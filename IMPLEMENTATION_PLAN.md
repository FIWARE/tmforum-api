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
  - **Result (open, blocks Step 4):** observed by running `FinancialAccountRefIT` with the getter change on the
    k3s IT setup (`quay.io/fiware/orion-ld:1.9.0`, `scorpiobroker/all-in-one-runner:java-5.0.3` from `k3s/`). To
    reproduce, change the getter as above, drop `@Disabled` and run
    `mvn verify -pl customer-bill-management -Dbroker=orion-ld -Dit.test=FinancialAccountRefIT -DskipTests`.
    Scorpio keeps the instances, orion-ld does not. Orion-LD stores the
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
  - **Result:** `mvn install -DskipITs` of those modules is green. The orion-ld ITs pass at 3dfe1edd
    (`mvn verify -pl <module> -Dbroker=orion-ld -DskipTests`, orion-ld 1.9.0), with run/failed/skipped counts:
    account 237/0/119, agreement 85/0/44, usage-management 80/0/27, resource-function-activation 191/0/63,
    service-catalog 238/0/89, service-inventory 71/0/22, document-management 40/0/23, customer-bill-management
    120/0/53 (`FinancialAccountRefIT` disabled). In a container whose Docker network overlaps the k3s defaults, pass
    `-Dk3s.serviceCidr=10.143.0.0/16 -Dk3s.clusterDns=10.143.0.10 -Dk3s.clusterCidr=10.142.0.0/16`, otherwise no
    pod resolves DNS. The resolving merge path is also pinned broker-less by
    `AccountRefRoundTripTest#writtenFieldsWinOverTheResolvedTarget` (`BillingAccount` over a stored
    `FinancialAccount`).
  - **Result (open, blocks list setters in Steps 2–4 on orion-ld):** `service-catalog/.../EntitySpecificationRelationshipIT`
    writes a `RELATIONSHIP_LIST` of `EntitySpecificationRelationship` (all fields, nested `associationSpec`) with
    the production mappers, retrieves it from the broker and reads it with `fromProperties = true`. Scorpio
    (java-5.0.3) returns everything for one and for several relationships. Orion-LD 1.9.0 stores everything, but
    when the attribute has **several instances** it returns their sub-attributes **expanded**
    (`https://uri.etsi.org/ngsi-ld/default-context/validFor`, `https://uri.etsi.org/ngsi-ld/name`), so the read
    gets id-only refs. One instance comes back compacted and reads fine. The IT therefore skips the several-instances
    case on orion-ld. This hits every `RELATIONSHIP_LIST` read from properties with 2+ entries: the ones flipped in
    Steps 2–4, and by inference the existing `fromProperties` lists such as `relatedParty` (no IT posts 2+ of those,
    not checked through the API). A maintainer has to choose a remedy before Steps 2–4 flip any list setter, e.g.
    compacting expanded sub-attribute names on read in the mapping library, or an Orion-LD fix.
  - **Resolved in Step 2:** `ngsi-ld-java-mapping` 1.6.33 compacts expanded sub-attribute names on read
    (`NgsiLdKeyCompactor`, against `mapping.contextUrl`). `TolerantEntityVOMapper` passes the injected compactor on,
    and the orion-ld skip in `EntitySpecificationRelationshipIT` is gone. List setters are no longer blocked.

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
- **Known defect, pinned in Step 2 (open):** on Orion-LD without `replaceOnUpdate` (the production default, and forced
  for every IT by the root pom's failsafe `argLine`), a PATCH that swaps the ids of a reference list keeps the old
  entries, because the broker merges the instances by datasetId. Scorpio replaces the whole attribute and drops them. It predates this ticket (resolved lists merged the same
  way). `ExternalReferenceIT#patchReplacingTheIdsOfAReferenceList` asserts the current behaviour, so a fix has to
  change that expectation on purpose. Ticket 6 (PostgreSQL) will replace the list, which is a parity gap to settle
  there.

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
- Deferred from Step 2: `resource-shared-models` `ResourceSpecificationRef` has no `version`, although every API's
  `ResourceSpecificationRef` schema has one, so a posted `version` is dropped (before and after Step 2). Add it as an
  embedded `PROPERTY` (getter `embedProperty = true`, setter `fromProperties = true`). While the resource setters
  still resolve, it would be filled from the target's `version`, which is why it goes in with this step's flips and
  IT runs. Add a `ProductSpecification.resourceSpecification` external round-trip to product-catalog's
  `ExternalReferenceIT`.
- **Result:** all 34 setters flipped (resource-function-activation has 12, not 13; the scan now finds the 23 of
  Step 4 and nothing else). `ResourceSpecificationRef` has `version`, pinned broker-less by
  `resource-shared-models/.../ResourceSpecificationRefRoundTripTest` (the module gained JUnit test dependencies) and
  through the API by `ExternalReferenceIT#externalResourceSpecificationsRoundTrip`. Every target class of the flipped
  setters already embeds all its fields, so no assertion needed changing.
  `service-catalog/.../EntitySpecRelationshipApiIT` posts minimal, full (with an external `associationSpec`) and
  two-entry `entitySpecRelationship` lists to stored specifications and checks create, `GET` and `PATCH`. On the
  unchanged work branch all 3 cases fail the retrieval with a 500. `DocumentSpecification.entitySpecRelationship`
  (and `UsageSpecification.entitySpecRelationship`) are document-management and usage-management setters, so they
  keep the failure until Step 4.
  ITs (`mvn verify -pl <module> -Dbroker=<broker> -DskipTests`, run/failed/skipped, orion-ld | scorpio):
  service-catalog 245/0/89 | 245/0/99, product-catalog 399/0/115 | 399/0/125, resource-catalog 224/0/89 |
  224/0/99, resource-inventory 101/0/22 | 101/0/32, resource-function-activation 191/0/63 | 191/0/73,
  service-inventory 71/0/22 | 71/0/32, service-order-management 28/0/0 | 28/0/8, document-management 40/0/23 |
  40/0/23, software-management (also a resource-shared-models consumer) 196/1/45 | 196/1/55,
  resource-order-management 29/5/0 | 29/5/8. The failures predate this step: on the unchanged work branch
  (orion-ld) resource-order-management fails the same 5 `ResourceOrderApiIT.createResourceOrder201` cases (`state`
  reads back null) and software-management fails 3. This step fixes 2 of those 3:
  `ResourceApiIT.createResource400`/`patchResource400` with a non-existent resource ref now answer 400 instead of
  500. `ResourceSpecificationApiIT.createResourceSpecification400` (a non-URI `resourceSpecRelationship` id, a
  `PROPERTY`, is accepted) still fails.

### Step 4: Read the remaining modules' references from properties
Not blocked by the customer-bill `FinancialAccountRef.accountBalance` encoding (Step 1 result), contrary to the
earlier note: the only setter that reaches that class is `CustomerBill.financialAccount`, which was already
`fromProperties = true` before this ticket, so none of this step's flips touches it. The open decision moves to Step 5,
whose guard test is the first place that rejects the class's `PROPERTY_LIST` getter.
Same procedure for `account` (3), `agreement` (4), `customer-bill-management` (3), `customer-management` (2),
`document-management` (2), `party-catalog` (2), `party-role` (4) and `usage-management` (3).
- Look at `party-catalog/.../TMForumMapper` (the organization parent/child relationship hrefs) and
  `document-management/.../S3AttachmentService` (copies `href`). Check that nothing relies on a resolved target.
- Run these modules' ITs and the `all-in-one` ITs on both brokers, and adjust assertions as in Step 2.
- Account: today a `financialAccount` ref to a local `FinancialAccount` with 2+ balances fails the read with a
  `MappingException`, because the resolved target's balance list cannot map to the ref's single `accountBalance`.
  This is unchanged by Step 1, which was checked against the annotations before and after. With
  `fromProperties = true` the target is no longer read. Add an account IT for it.
- Document-management and usage-management: `DocumentSpecification.entitySpecRelationship` and
  `UsageSpecification.entitySpecRelationship` still resolve and fail the read for a stored target (see Step 3
  result). Add a document-management IT like `service-catalog/.../EntitySpecRelationshipApiIT`.
- Agreement: same pre-existing failure as `EntitySpecificationRelationship` in Step 3.
  `AgreementSpecificationRelationship` has no `@MappingEnabled`, so an `AgreementSpecification.specificationRelationship`
  to a stored specification fails the read today. Add an agreement IT that references a local specification.
- Acceptance: re-run the setter scan (any `RELATIONSHIP`/`RELATIONSHIP_LIST` `@AttributeSetter` without
  `fromProperties = true` in non-test sources). It finds 0 of 141.
  - **Result:** the scan finds 0 of 139. The other 2 of the 141 are `EndpointRef.connectionPoint` and
    `EntitySpecificationRelationship.associationSpec`, which Step 1 re-mapped as embedded `PROPERTY`. `TMForumMapper`
    (party-catalog) maps the stored `href` of the organization parent/child refs, and `S3AttachmentService` copies the
    posted attachment's `href`. Neither relies on a resolved target. New ITs: `account/.../FinancialAccountRefApiIT`,
    `agreement/.../SpecificationRelationshipApiIT`, `document-management/.../EntitySpecRelationshipApiIT`. No existing
    assertion needed changing.
    ITs (`mvn verify -pl <module> -Dbroker=<broker> -DskipTests`, run/failed/skipped, orion-ld | scorpio):
    account 241/0/119 | 241/0/129, agreement 88/0/44 | 88/0/54, customer-bill-management 120/0/53 | 120/0/53,
    customer-management 71/0/22 | 71/0/32, document-management 43/0/23 | 43/0/23, party-catalog 111/0/49 |
    111/0/58, party-role 36/0/19 | 36/0/19, usage-management 80/0/27 | 80/0/37. `all-in-one` has no `*IT`. Its unit
    tests (`RelationshipsReadFromPropertiesTest` and the relationship-target tests) pass.
  - **Fixed in review (PR #12):** three relationships could not be read back at all, because the mapper builds a
    target through its String constructor and these targets had none. `defaultPaymentMethod` (BillingAccount,
    PartyAccount, SettlementAccount) and customer-bill `CustomerBill.paymentMethod` named the generated
    `PaymentMethodRefVO` as `targetClass`; they now name the domain `PaymentMethodRef`. `Product.productOrderItem`'s
    `RelatedProductOrderItemRef` gained a String constructor (the product order id) and a getter for
    `productOrderId`, which the VO mapping needs, and lost the `productOrderId` setter annotation that mapped
    `productOrderHref` onto the id. Pinned broker-less by `account/.../DefaultPaymentMethodRoundTripTest`,
    `customer-bill-management/.../PaymentMethodRoundTripTest` and
    `product-inventory/.../ProductOrderItemRoundTripTest`. `RelationshipTargetConstructorTest` has no allow-list now.
    The new `productOrderId` getter also makes Jackson write the id wherever the ref is embedded as a plain-JSON
    value (`ProductRefOrValue.productOrderItem` in product-ordering-management and quote, and the redis cache), where
    it was dropped before. `product-inventory/.../ProductOrderItemApiIT` round-trips minimal and full items of one
    and of two stored product orders through create, `GET` and `PATCH`. ITs with it (run/failed/skipped, orion-ld |
    scorpio | orion-ld with `-Dcache=redis`): product-inventory 122/0/22 | 122/0/32 | 122/0/22,
    product-ordering-management 149/0/36 | 149/0/46 | 149/0/36, quote 90/0/22 | 90/0/31 | 90/0/22.
    `RelatedProductOrderItemRef` used the product order id as its datasetId, so two items of the **same** order
    collided: on orion-ld the PATCH response lost one of them. The datasetId is now `<productOrderId>:<orderItemId>`
    (URL-encoded, `~none` when absent, which no encoded item id can equal); the relationship object stays the order
    id. A stored instance with the old datasetId (the bare order id) is **not** removed by a PATCH on Orion-LD:
    `updateDomainEntity` drops instances the update does not keep (`NgsiLdBaseRepository#replacingAttribute`) only
    with `replaceOnUpdate`, which is off on Orion-LD (the production default) and for every IT (root pom failsafe
    `argLine`). Without it the PATCH appends by datasetId, so the old instance survives next to the new one and the
    product reads back a duplicate item (the Step 2 PATCH defect's mechanism; not tested). The ticket 8 backfill
    therefore has to rewrite the datasetId of **every** stored `productOrderItem` instance to
    `<productOrderId>:<orderItemId>`, single-item products included.
    `ProductOrderItemApiIT` gained minimal and full two-items-of-one-order cases. Without the change, both fail on
    orion-ld. With it, product-inventory passes at 124/0/22 | 124/0/32 | 124/0/22. The datasetId getter
    (`getDatasetId`) is `@JsonIgnore`, so it adds nothing to the plain-JSON form in product-ordering-management and
    quote (unlike the `productOrderId` getter above).
    `RelatedProductOrderItemRef` also overrides `getReferenceHref` with `productOrderHref`, like `RefEntity` with
    `href`. Before, a `productOrderItem` was always classified as local, so an order held by a configured external
    host was rejected with a 400. `ProductOrderItemApiIT` gained external-order cases, which fail without the override
    (3 of 9 on orion-ld). With it, product-inventory passes at 127/0/22 | 127/0/32 (orion-ld | scorpio).
    `RelatedProductOrderItemDatasetIdTest` pins distinct datasetIds for items of one order, the fallback included.

### Step 5: Remove read-time resolution from the NGSI-LD adapter
No longer blocked by the customer-bill `FinancialAccountRef.accountBalance` encoding (Step 1 result). Removing
resolution does not change that field: its only setter (`CustomerBill.financialAccount`) already reads from
properties, and the field is still dropped on write. The guard therefore exempts the one getter
(`customerbillmanagement.domain.FinancialAccountRef#getAccountBalance`) by name, with a comment pointing at the open
decision. Whoever picks the encoding removes that entry together with the getter change and the `@Disabled` on
`FinancialAccountRefIT`.
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
    `OrganizationParent/ChildRelationship`. No setter names a generated `*VO` class any more (see the Step 4
    review fix), so fail on one instead of skipping it.
  - Target exemption: `Subscription` (`TMForumSubscription.subscription`) is excluded by an explicit,
    commented allow-list entry. It is a pointer to the broker subscription, not a reference payload: reads use
    only its id (`NgsiLdBaseRepository` delete path), and the full object is needed only on the create path
    before persisting (`AbstractSubscriptionApiController.create`). Its getters are non-embedded `PROPERTY` and
    `PROPERTY_LIST` (including the `Set`-typed `watchedAttributes`), and they must not be changed to embed it.
    Its setter still falls under the first rule, which it already satisfies.
  - Getter exemption: the customer-bill `FinancialAccountRef#getAccountBalance` (`PROPERTY_LIST`) by name, until
    its encoding is decided (see above). A `List`-typed `PROPERTY, embedProperty = true` getter (Decision 2) passes
    the rule as is.
- Fan-out IT in `product-catalog`: an `HttpClientFilter` test bean (or a counter on `BrokerRequestLoggingFilter`)
  counts the broker requests of one `GET /productOffering/{id}` whose offering references several local and
  external targets. Assert that no broker request targets any referenced id, and that the offering itself is
  retrieved at most once. Do not assert exactly one retrieve: `createEntity` is `@CachePut` on the `entities`
  cache, so a `GET` right after the create may be a cache hit with zero broker requests.
- Keep the `entities` cache, because `get`, reference checks and updates still use it. Reword its "not call
  twice in one call" comment in the `application-in-memory.yaml`/`application-redis.yaml` files if it no longer
  fits.
- Acceptance: `mvn test` and `mvn integration-test` pass on both brokers, and with `-Dcache=redis`.
  - **Result:** the `entities` cache comment in every `application-in-memory.yaml`/`application-redis.yaml` now says
    it dedups one request's reads of an entity (get after create/update, reference checks). ITs at ba689bee
    (`mvn verify -pl <module> -Dbroker=<broker> -Dcache=<cache> -DskipTests`, run/failed/skipped, orion-ld |
    scorpio | orion-ld with `-Dcache=redis`): product-catalog 400/0/115 | 400/0/125 | 400/0/115 (the new
    `ExternalReferenceIT#retrieveProductOfferingDoesNotFetchReferences` runs on all three), account 241/0/119 |
    241/0/129 | 241/0/119, customer-bill-management 120/0/53 | 120/0/53 | 120/0/53, product-inventory 127/0/22 |
    127/0/32 | 127/0/22. The counts match Step 4 plus the one new IT. The other modules' ITs were not re-run for this
    step.

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
- **Result:** `AbstractEntityRepositoryContractTest#referenceFixture()` returns a `ReferenceFixture<?>`. It is an
  `EntityFixture` of its own and may use a class other than the contract's main class. When it is empty, the three
  cases are skipped. `product-catalog`'s `ProductOfferingReferenceFixture` writes a `ProductOffering` with external
  `CategoryRef`s (`version` extra) and `ChannelRef`s. Both attributes were flipped in Step 2, so the cases also guard
  the flip on the NGSI-LD adapter. The update replaces only the categories. The same-id target is a `Category`.
  The fixture's reference to that `Category` omits `name`. A resolving read merges the written sub-properties over
  the target, so it would fill `name` from the target and fail `getDoesNotResolveReferences` (checked after the
  target is created, renamed and deleted). This is reasoned from the merge pinned by `AccountRefRoundTripTest`; it
  was not run against a resolving adapter, which the NGSI-LD adapter no longer is since Step 5. `updateKeepsUntouchedReferences` hit the Step 2 PATCH
  defect on Orion-LD: the replaced category list is merged with the stored one by datasetId. It is recorded as
  `KnownDeviation.REFERENCE_LIST_UPDATE_MERGES` (Orion-LD without `replaceOnUpdate`, next to
  `EMPTIED_LIST_BREAKS_READS`), and in that setup the case asserts the merge: the created list plus every carried entry, each once, in any order. The other comparisons ignore list order too but
  count entries, and the README and `EntityReadRepository#get` say that order is not part of the rule. The port rule (a
  carried list replaces the stored one) and the defect are in the `updateDomainEntity` Javadoc. `NgsiLd*ContractIT`
  (`mvn verify -pl product-catalog -Dit.test='NgsiLd*ContractIT' -DskipTests`): 27/0/0 on orion-ld, scorpio and
  scorpio with `-Dcache=redis`.

### Step 7: Document the behaviour change
- Add `release-notes/RELEASE_NOTES_INLINE_REFERENCES.md`, in the style of `RELEASE_NOTES_EXTERNAL_REFERENCES.md`.
  It covers:
  - references are value copies; a field the client omits reads back absent; a renamed target leaves cached
    `name`s stale;
  - data written before this release reads back with only the fields that were embedded at the time until the
    ticket 8 backfill runs;
  - the list of IT assertions changed in Steps 2–4 and why;
  - reads compact sub-attribute names against `mapping.contextUrl`, so a deployment with a custom
    `general.contextUrl` has to set `mapping.contextUrl` to the same URL (it already had to for writes);
    `common`'s `JsonLdContextPreloader` fetches that context at server startup off the event loop, but if it is
    unreachable then, the library retries the blocking fetch (up to `mapping.context-retrieval-timeout`) on the
    reading thread once per `mapping.context-retrieval-retry-delay` until it succeeds. The preload makes every
    start of every module issue a GET to `mapping.contextUrl` (default: the ETSI core context), so a deployment
    has to allow egress to that URL or self-host the context, otherwise each start spends up to the timeout and
    logs a WARN. While the preload is in flight, a read that needs compaction waits on the resolver's lock (its
    `retrieve()` is `synchronized`) for up to the same timeout;
  - the reference-list PATCH defect recorded under Step 2, if still open.
- `AGENTS.md` Key Conventions: every relationship setter must be `fromProperties = true`, and every field of a
  relationship target class (except the `Subscription` pointer) an embedded `PROPERTY` getter, enforced by the Step 5 guard test. Reword the `strictRelationships`
  bullet, which no longer matters for reads.
- **Result:** `release-notes/RELEASE_NOTES_INLINE_REFERENCES.md` added. Steps 3 and 4 changed no existing IT
  assertion; Step 2 changed one (`ExternalReferenceIT#assertCategoriesReadBack`, ids only → whole posted reference)
  and dropped the Orion-LD skip in `EntitySpecificationRelationshipIT`. The notes list only tests that changed
  against the previous release (`assertCategoriesReadBack`, the Step 5 unit-test rewrites, and the
  `RelationshipTarget*Test` scope change); tests added within this ticket (`EntitySpecificationRelationshipIT`,
  `AccountRefRoundTripTest`) are left out. They name no ticket or step numbers, and say the backfill is not part of
  the release. They say `mapping.compact-against-context` only gates resolving names outside the built-in NGSI-LD
  prefixes (stripped either way), so a deployment on the default core context can set it to `false` and skip the
  startup GET, while a custom context needs it `true` plus egress or a self-hosted context. The shipped
  `application.yaml`s leave it `true`, and the notes say so; shipping `false` is a possible follow-up. The `AGENTS.md` change is **not** made on this step branch (step agents must not edit it); it goes to a
  plan-mode agent through a `[plan-update]`. Proposed replacement for the `strictRelationships` bullet:
  "References are stored inline and never resolved on read: every `RELATIONSHIP`/`RELATIONSHIP_LIST`
  `@AttributeSetter` is `fromProperties = true`, every `@AttributeGetter` of a relationship target class (except the
  `Subscription` pointer) is `PROPERTY` with `embedProperty = true` (one getter, customer-bill `FinancialAccountRef#getAccountBalance`, is
  exempt by name until its encoding is decided), and the target class has a `String` constructor.
  `all-in-one`'s `RelationshipsReadFromPropertiesTest` and `RelationshipTargetConstructorTest` enforce this.
  `mapping.strictRelationships`/`StrictRelationshipsDefault` no longer affect reads; the `application.yaml` blocks
  stay."
