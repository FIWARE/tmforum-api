# Implementation Plan: TMForum - DB Backend - Relationships

## Overview
Store each relationship's client-supplied reference payload (`id`, `href`, `name`, `@referredType`, plus the
extra fields of `RefEntity` subclasses such as `role` or `version`) inline, and stop resolving reference targets
at read time. On the NGSI-LD adapter this removes the `EntitiesRepository.getEntities` fan-out, which is the N+1.
The PostgreSQL adapter (ticket 6) does not exist yet. "Both backends" is pinned here by a backend-neutral case in
the `storage-api` port contract that ticket 6 has to pass. The broker backfill for existing data is ticket 8.

## Background (verified on `main`, 9570e548)

- `io.github.wistefan:ngsi-ld-java-mapping` 1.6.31, `EntityVOMapper`:
  - `fromEntityVO` → `getRelationshipMap` calls `entitiesRepository.getEntities(ids)` for every
    `RELATIONSHIP`/`RELATIONSHIP_LIST` setter with `fromProperties = false`.
  - `getObjectFromRelationship` then maps the **fetched target**, with the relationship's own sub-properties
    merged over it. A field the client omitted (e.g. `name`, `href`, `version`) is therefore filled from the
    target's current state.
  - `fromProperties = true` takes the `relationshipFromProperties` path instead: it builds the ref from the
    relationship's sub-properties alone and makes no fetch.
- `JavaObjectMapper.getAdditionalPropertyEntryFromMethod` writes only getters that are **`PROPERTY` and
  `embedProperty = true`** as sub-properties of a relationship. `PROPERTY_LIST`/`RELATIONSHIP` sub-attributes are
  silently dropped on write.
- Counts (non-test sources): 141 relationship setters, 58 of them `fromProperties = true`, 83 `false`. Per
  module, `false`: product-shared-models 23, resource-function-activation 13, service-shared-models 10,
  resource-shared-models 8, party-role 4, agreement 4, account 3, customer-bill-management 3, usage-management 3,
  customer-management 2, document-management 2, party-catalog 2, service-catalog 2, service-inventory 2,
  resource-catalog 1, product-ordering-management 1.
- `RefEntity` subclasses whose extra fields are **not** stored inline today (getter missing
  `embedProperty = true`, or a type the library does not embed):
  - `common/.../domain/ConstraintRef` (`version`)
  - `usage-management/.../UsageSpecificationRef`: redeclares `href`/`name` without embedding, which shadows
    `RefEntity`'s getters
  - `agreement/.../AgreementSpecificationRelationship` (`relationshipType`, `validFor`)
  - `account/.../FinancialAccountRef` (`accountBalance`) and `account/.../AccountRef` (`description`)
  - `resource-function-activation/.../ConnectionPointRef` (`version`), `ResourceFunctionRef` (`version`),
    `EndpointRef` (`isRoot`, and `connectionPoint`, which is a `RELATIONSHIP`)
  - `service-shared-models/.../EntitySpecificationRelationship` (`associationSpec`, embedded but a
    `RELATIONSHIP`, so dropped)
  - `customer-bill-management/.../FinancialAccountRef` (`accountBalance`, embedded but a `PROPERTY_LIST`, so
    dropped)
- Read-time resolution lives in `common/.../repository/BrokerBackedEntitiesRepository` (one cached
  `retrieveEntityById` per id) and `common/.../mapping/TolerantEntityVOMapper` (stub for a 404 target). The
  `entities` cache (`expire-after-write: 2s`) is **also** used by `NgsiLdEntityRepository.get`,
  `BrokerBackedReferencesRepository` and the update/delete invalidation. It stays.
- `product-catalog/.../ExternalReferenceIT` (ticket 4) already round-trips external category/service-candidate
  references. Today that works only because the fan-out 404s and the stub keeps the sub-properties.

## Decisions taken by this plan (reviewer: object here if you disagree)

1. **Store exactly what the client sent.** A field the client omits reads back absent. No write-time enrichment
   from the target. Write-time enrichment was rejected because it would add a target read to every write and
   make the stored payload depend on target state on both adapters.
   This is the documented behaviour change: an IT that posts `{id}` only and expects the target's `name`/`href`
   gets its assertion changed to the posted payload.
2. **Non-`PROPERTY` sub-attributes of a ref** are made embeddable without changing the mapping library.
   `EndpointRef.connectionPoint` and `EntitySpecificationRelationship.associationSpec` are single objects and are
   re-mapped as embedded `PROPERTY` (getter and setter) with a structured value. Customer-bill
   `FinancialAccountRef.accountBalance` is a list. Only its **getter** becomes `PROPERTY, embedProperty = true`,
   because the library writes a `List` as a `PropertyListVO`. Its setter stays `PROPERTY_LIST` with
   `targetClass = AccountBalance.class`, because `relationshipFromProperties` reads a `PROPERTY_LIST` setter
   through `handlePropertyList`, while a `PROPERTY` setter rejects a list.
3. **Unflagged setters fail loudly.** After the flip, the NGSI-LD `EntitiesRepository` returns an empty list for
   an empty request and **errors** for a non-empty one, so a setter that was missed fails instead of reading back
   id-only. A build-time guard test (Step 5) keeps that path unreachable.
4. `mapping.strictRelationships`/`StrictRelationshipsDefault` stay. Once nothing resolves they have no effect.
   They are not removed here, which avoids touching every `application.yaml`.

## Steps

### Step 1: Store every `RefEntity` subclass field inline on write
Make the write side complete before any read stops consulting the target. On its own this step changes no read:
when a relationship is still resolved, its sub-properties already override the target's.
- In each class listed under Background, add `embedProperty = true` to the `PROPERTY` getters that lack it. Add
  `fromProperties = true` to their `PROPERTY` setters too, for consistency with `RefEntity`. The library ignores
  that flag on properties, so this is cosmetic.
- `UsageSpecificationRef`: delete the shadowing `href`/`name` fields so `RefEntity`'s embedded ones apply. Check
  the usage-management MapStruct mapper still compiles.
- `EndpointRef.connectionPoint` and `EntitySpecificationRelationship.associationSpec`: re-map getter and setter
  as `AttributeType.PROPERTY`, `embedProperty = true`, with a `targetClass` on the setter where the library needs
  one to deserialize the structured value.
- Customer-bill `FinancialAccountRef.accountBalance` (Decision 2): change only the getter to
  `AttributeType.PROPERTY, embedProperty = true`. Keep the `PROPERTY_LIST` setter with
  `targetClass = AccountBalance.class`, and assert the read gives `List<AccountBalance>`, not `List<Map>`.
  A relationship sub-attribute with several datasetId instances is unusual NGSI-LD, so add an IT in
  `customer-bill-management` that round-trips a `FinancialAccountRef` with one and with several balances on
  orion-ld **and** scorpio. If a broker drops or merges the instances, stop and report it on the ticket before
  Step 4.
  - **Result (open, blocks Step 4):** scorpio keeps the instances, orion-ld does not. Orion-LD stores the
    written array of instances (`datasetId` `list-item:0..n`) as **one** `Property` whose `value` is the array of
    the instance objects (`value`, `datasetId`, `type`, sub-properties). A single balance collapses to one plain
    instance and reads fine. With several, the read maps the instance objects to `AccountBalance` and the API
    answers 400 "Invalid JSON". `CustomerBill.financialAccount` already reads from properties, so this would turn
    today's silent drop into an unreadable bill. The getter change was therefore **not** made: `accountBalance`
    is still dropped on write. `customer-bill-management/.../FinancialAccountRefIT` is `@Disabled`. At this
    state it fails on **both** brokers, because nothing is stored inline. It shows the Orion-LD merge only once the
    getter is `PROPERTY, embedProperty = true`, so re-enable it together with that change. A reviewer has to choose an encoding (e.g. one `PROPERTY` whose value is the plain JSON array,
    which needs a getter the library writes as a single property) before Step 4.
- Unit tests in `common/src/test/.../domain/` and in each touched module's `src/test`: a parameterized
  round-trip through `JavaObjectMapper` (`toEntityVO`) and `EntityVOMapper.fromEntityVO`, with a mocked
  `EntitiesRepository` that returns nothing. Per touched ref class, assert every field survives. Follow
  `RefEntityAtReferredTypeTest` and `TolerantEntityVOMapperTest`.
- Acceptance: `mvn test -pl common,usage-management,agreement,account,resource-function-activation,service-shared-models,customer-bill-management -am`
  passes, the ITs of those modules still pass on orion-ld, and the `accountBalance` IT passes on both brokers.
  - **Result:** `mvn install -DskipITs` of those modules is green. The orion-ld ITs (account, agreement,
    usage-management, resource-function-activation, service-catalog, service-inventory, document-management,
    customer-bill-management) did **not** run in the agent container: the k3s cluster cannot resolve DNS, so no
    pod (not even coredns) starts. They must run in CI or locally before merge. The resolving merge path is pinned
    broker-less by `AccountRefRoundTripTest#writtenFieldsWinOverTheResolvedTarget` (`BillingAccount` over a stored
    `FinancialAccount`).

### Step 2: Read product-family references from properties
Flip the relationship setters of the product family to `fromProperties = true`: `product-shared-models` (23,
e.g. `ProductOffering` `agreement`/`bundledProductOffering`/`category`/`channel`/`marketSegment`/`place`/
`productOfferingPrice`/`productOfferingRelationship`/`resourceCandidate`/`serviceCandidate`/
`serviceLevelAgreement`) and `product-ordering-management` (1).
- `product-catalog`, `product-inventory`, `product-ordering-management` and `quote` (all consumers of the shared
  models): run their ITs. Where an assertion expected a value the request did not carry (target `name`/`href`/
  `version` filled in by resolution), change the expectation to the posted payload. Add a one-line comment
  naming this ticket. Do not change any other assertion. A failure for any other reason is a bug to fix.
- Extend `product-catalog/.../ExternalReferenceIT`: a `ProductOffering` whose 12 relationship attributes (the 11
  listed above plus `productSpecification`, which is already `fromProperties`) all point at non-existent
  external ids. Each carries `href`, `name`, `@referredType` and any subclass extras, and
  must round-trip intact through create, `GET` and `PATCH`. Use a parameterized test over the attributes.
- Acceptance: `mvn integration-test` for the four modules passes with `-Dbroker=orion-ld` and
  `-Dbroker=scorpio`.

### Step 3: Read resource- and service-family references from properties
Same procedure as Step 2 for `resource-shared-models` (8), `resource-function-activation` (13),
`resource-catalog` (1), `service-shared-models` (10), `service-catalog` (2) and `service-inventory` (2).
- Run the ITs of every module that consumes these models (`resource-catalog`, `resource-inventory`,
  `resource-function-activation`, `resource-order-management`, `service-catalog`, `service-inventory`,
  `service-order-management`, and `document-management`, whose `DocumentSpecification.entitySpecRelationship` uses
  `EntitySpecificationRelationship`) on both brokers. Adjust assertions that depended on resolution, as in Step 2.
- Pre-existing failure, fixed by this step: `EntitySpecificationRelationship` has no `@MappingEnabled`, so today a
  `ServiceSpecification`/`DocumentSpecification` whose `entitySpecRelationship` points at a **stored** target fails
  the read with `MappingException: Mapping is not enabled` (checked broker-less, same before and after Step 1).
  Add an IT that references a local specification.
- Check `EntitySpecificationRelationship.associationSpec` round-trips through the API, using the property mapping
  from Step 1. `EndpointRef` is never a relationship value: it is only reached through
  `ResourceFunction.resourceGraph` (`PROPERTY_LIST`) → `ResourceGraph.connection` → `Connection.endpoint`, all
  plain Jackson. Its Step 1 annotations have no runtime effect and exist for consistency and the Step 5 guard, so
  an API check of `connectionPoint` there exercises Jackson only.

### Step 4: Read the remaining modules' references from properties
**Blocked** until the customer-bill `FinancialAccountRef.accountBalance` encoding is decided (see Step 1 result).
Same procedure for `account` (3), `agreement` (4), `customer-bill-management` (3), `customer-management` (2),
`document-management` (2), `party-catalog` (2), `party-role` (4) and `usage-management` (3).
- Look at `party-catalog/.../TMForumMapper` (the organization parent/child relationship hrefs) and
  `document-management/.../S3AttachmentService` (copies `href`). Check that nothing relies on a resolved target.
- Run these modules' ITs and the `all-in-one` ITs on both brokers, and adjust assertions as in Step 2.
- Account: today a `financialAccount` ref to a local `FinancialAccount` with 2+ balances fails the read with a
  `MappingException`, because the resolved target's balance list cannot map to the ref's single `accountBalance`.
  This is unchanged by Step 1, which was checked against the annotations before and after. With
  `fromProperties = true` the target is no longer read. Add an account IT for it.
- Agreement: same pre-existing failure as `EntitySpecificationRelationship` in Step 3.
  `AgreementSpecificationRelationship` has no `@MappingEnabled`, so an `AgreementSpecification.specificationRelationship`
  to a stored specification fails the read today. Add an agreement IT that references a local specification.
- Acceptance: re-run the setter scan (any `RELATIONSHIP`/`RELATIONSHIP_LIST` `@AttributeSetter` without
  `fromProperties = true` in non-test sources). It finds 0 of 141.

### Step 5: Remove read-time resolution from the NGSI-LD adapter
- Replace `common/.../repository/BrokerBackedEntitiesRepository` with a non-resolving `EntitiesRepository`
  (e.g. `common/.../mapping/InlineReferencesEntitiesRepository`). It returns `Mono.just(List.of())` for an empty
  id list and `Mono.error(MappingException)` naming the ids otherwise (Decision 3). Delete
  `TolerantEntityVOMapper` and its stub, and `BrokerBackedEntitiesRepositoryTest`. Fold the cases of
  `TolerantEntityVOMapperTest` that still apply into a test of the new class. Update `StorageBoundaryTest`
  (`common/src/test/.../architecture/`), which lists `BrokerBackedEntitiesRepository`.
  Also replace `NgsiLdMappingRegressionTest#injectsTheTolerantMapper` (`common/src/test/.../mapping/`), which
  asserts the `TolerantEntityVOMapper` bean type, with a check that the new `EntitiesRepository` is the injected
  bean. Remove the `{@link TolerantEntityVOMapper}` from the `StrictRelationshipsDefault` Javadoc.
- Guard test in `all-in-one/src/test` (the module that sees every domain class), reflection- or ArchUnit-based
  next to `ControllerStorageBoundaryTest`. It fails when:
  - a `RELATIONSHIP`/`RELATIONSHIP_LIST` `@AttributeSetter` lacks `fromProperties = true`;
  - a relationship target class has an `@AttributeGetter` that is not `PROPERTY` with `embedProperty = true`.
    Target classes are every `targetClass` of a `RELATIONSHIP`/`RELATIONSHIP_LIST` setter plus all `RefEntity`
    subclasses. That covers non-`RefEntity` targets such as `RelatedProductOrderItemRef` (extends `Entity`) and
    `OrganizationParent/ChildRelationship`. Generated `*VO` classes (e.g. `PaymentMethodRefVO`) are skipped,
    since the getter side does not use them.
  - Target exemption: `Subscription` (`TMForumSubscription.subscription`) is excluded by an explicit,
    commented allow-list entry. It is a pointer to the broker subscription, not a reference payload: reads use
    only its id (`NgsiLdBaseRepository` delete path), and the full object is needed only on the create path
    before persisting (`AbstractSubscriptionApiController.create`). Its getters are non-embedded `PROPERTY` and
    `PROPERTY_LIST` (including the `Set`-typed `watchedAttributes`), and they must not be changed to embed it.
    Its setter still falls under the first rule, which it already satisfies.
  - Getter exemption: a `List`-typed getter may be `PROPERTY, embedProperty = true` with a `PROPERTY_LIST` setter
    (Decision 2, customer-bill `accountBalance`).
- Fan-out IT in `product-catalog`: an `HttpClientFilter` test bean (or a counter on `BrokerRequestLoggingFilter`)
  counts the broker requests of one `GET /productOffering/{id}` whose offering references several local and
  external targets. Assert that no broker request targets any referenced id, and that the offering itself is
  retrieved at most once. Do not assert exactly one retrieve: `createEntity` is `@CachePut` on the `entities`
  cache, so a `GET` right after the create may be a cache hit with zero broker requests.
- Keep the `entities` cache, because `get`, reference checks and updates still use it. Reword its "not call
  twice in one call" comment in the `application-in-memory.yaml`/`application-redis.yaml` files if it no longer
  fits.
- Acceptance: `mvn test` and `mvn integration-test` pass on both brokers, and with `-Dcache=redis`.

### Step 6: Pin inline references in the backend-neutral port contract
- `storage-api/src/test/.../contract/`: add an optional `ReferenceFixture<T>` (or default methods on
  `EntityFixture`). It builds an entity whose references point at ids the backend does not hold, all payload
  fields set, and extracts the reference payloads for comparison. In `AbstractEntityRepositoryContractTest`, add:
  - `createdReferencesRoundTripIntact`;
  - `updateKeepsUntouchedReferences` (a write carrying only one reference leaves the others as stored);
  - `getDoesNotResolveReferences` (the read payload equals the written one, even after a same-id target is
    created and renamed in between).
  The cases skip when the fixture provides no references.
- Implement the fixture in `product-catalog/.../storage/NgsiLdEntityRepositoryContractIT` with a `Catalog` (or
  `ProductOffering`) carrying external `CategoryRef`s, including an extra-bearing subclass.
- Document the rule in `storage-api/README.md` and in the Javadoc of `EntityReadRepository#get`: references are
  stored and returned as written and never resolved. Ticket 6 has to pass these cases.

### Step 7: Document the behaviour change
- Add `release-notes/RELEASE_NOTES_INLINE_REFERENCES.md`, in the style of `RELEASE_NOTES_EXTERNAL_REFERENCES.md`.
  It covers:
  - references are value copies; a field the client omits reads back absent; a renamed target leaves cached
    `name`s stale;
  - data written before this release reads back with only the fields that were embedded at the time until the
    ticket 8 backfill runs;
  - the list of IT assertions changed in Steps 2–4 and why.
- `AGENTS.md` Key Conventions: every relationship setter must be `fromProperties = true`, and every field of a
  relationship target class (except the `Subscription` pointer) an embedded `PROPERTY` getter, enforced by the Step 5 guard test. Reword the `strictRelationships`
  bullet, which no longer matters for reads.
