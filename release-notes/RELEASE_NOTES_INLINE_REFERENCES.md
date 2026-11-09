# Release Notes: references stored inline, never resolved on read

## Overview

Until now most relationships (83 of 141 relationship setters) were resolved on read: for every referenced id the
NGSI-LD adapter fetched the target entity from the broker and merged the relationship's own sub-properties over it.
A list of N references cost N extra broker requests per read (N+1), and a field the client had omitted (`name`,
`href`, `version`, ...) came back filled from the target's current state.

Every reference is now stored inline as the client sent it, including the extra fields of `RefEntity` subclasses
(`role`, `version`, `relationshipType`, `validFor`, ...), and is read back from those stored fields alone. A read
makes no request for any referenced id, whatever the number of references.

## What changed

* Every `RELATIONSHIP`/`RELATIONSHIP_LIST` `@AttributeSetter` is `fromProperties = true`, and every field of a
  relationship target class is written as an embedded `PROPERTY` sub-attribute. The
  [RelationshipsReadFromPropertiesTest](../all-in-one/src/test/java/org/fiware/tmforum/RelationshipsReadFromPropertiesTest.java)
  guard in `all-in-one` enforces both on every build. It exempts the `Subscription` pointer and one getter (see
  [Known limitations](#known-limitations)).
* `RefEntity` subclasses whose extra fields were silently dropped on write now store them: `ConstraintRef.version`,
  `UsageSpecificationRef.href`/`name`, `AgreementSpecificationRelationship.relationshipType`/`validFor`,
  account `FinancialAccountRef.accountBalance`, `AccountRef.description`, the `version` of `ConnectionPointRef` and
  `ResourceFunctionRef`, `EndpointRef.isRoot`/`connectionPoint`, and
  `EntitySpecificationRelationship.associationSpec`. `ResourceSpecificationRef` gained the `version` every API's
  schema already had, so a posted `version` is no longer dropped.
* `BrokerBackedEntitiesRepository` and `TolerantEntityVOMapper` are gone. The mapping library's
  `EntitiesRepository` is now
  [InlineReferencesEntitiesRepository](../common/src/main/java/org/fiware/tmforum/common/mapping/InlineReferencesEntitiesRepository.java):
  it answers an empty request with an empty list and fails any non-empty one, so a relationship that is not read
  from its properties fails loudly instead of reading back id-only.
* `ngsi-ld-java-mapping` is raised to 1.6.33. Orion-LD returns the sub-attributes of a relationship with several
  instances expanded (`https://uri.etsi.org/ngsi-ld/name`), which read back as id-only references. The library now
  compacts such names on read: it strips the built-in NGSI-LD core and default-context prefixes, and resolves any
  other expanded name against `mapping.contextUrl`.
  [JsonLdContextPreloader](../common/src/main/java/org/fiware/tmforum/common/mapping/JsonLdContextPreloader.java)
  fetches that context once the server is up.
* The `entities` cache stays. It now only dedups one request's reads of an entity (get after create/update,
  reference checks).
* The backend-neutral port contract in `storage-api` pins the rule with `createdReferencesRoundTripIntact`,
  `updateKeepsUntouchedReferences` and `getDoesNotResolveReferences`, so the upcoming PostgreSQL adapter has to
  behave the same. See [storage-api/README.md](../storage-api/README.md#references).

## Behaviour changes

* **References are value copies.** A reference reads back exactly as written. A field the client omitted reads back
  absent and is no longer filled from the target. Creating, renaming or deleting the target changes nothing: after a
  target is renamed, every reference that carried its old `name` keeps returning that old `name`. A client that needs
  the current state of the target has to fetch it.
* **Data written before this release reads back with only the fields that were embedded at the time.** Fields that
  were filled in from the target (e.g. `name`/`href` the client never sent, or the extra fields listed above, which
  were dropped on write) are absent. **This release does not migrate stored data.** A
  later data-migration release will rewrite the stored references; until it is applied, existing data stays as
  described here. In the meantime a client can restore the fields of a reference by PATCHing its attribute with the
  full reference payload. A `productOrderItem` (product-inventory) stored before this release is the exception: its
  datasetId changed from the product order id to `<productOrderId>:<orderItemId>`, and on Orion-LD a PATCH without
  `replaceOnUpdate` keeps the old instance next to the new one, so a re-PATCH makes the product read back a duplicate
  item. Leave such products untouched until the data migration has rewritten them.
* **Reads that used to fail now succeed.** A reference to a stored target is no longer fetched, so these reads,
  which answered `500` or `400` before, return the entity:
  * `ServiceSpecification`, `DocumentSpecification` and `UsageSpecification` whose `entitySpecRelationship` points at a
    stored specification, and `AgreementSpecification.specificationRelationship` to a stored specification (the target
    classes had no `@MappingEnabled`);
  * an account `financialAccount` to a `FinancialAccount` with two or more balances;
  * `defaultPaymentMethod` (BillingAccount, PartyAccount, SettlementAccount), customer-bill
    `CustomerBill.paymentMethod`, and product-inventory `Product.productOrderItem` (the target classes had no `String`
    constructor);
  * a `GET` whose referenced entity could not be fetched because of a broker error: it returns `200` with the
    stored reference instead of failing.
* **Two `productOrderItem`s of the same product order are both kept.** They shared a datasetId, so on Orion-LD one
  of them was lost on a PATCH.
* **A `productOrderItem` whose `productOrderHref` names a configured external host is accepted.** It was always
  classified as local, so such an order was rejected with `400`.
* **`productOrderId` now appears in the `productOrderItem`s of an embedded product value.** In product-ordering-management
  (`ProductOrder.productOrderItem[].product.productOrderItem`) and quote (`Quote.quoteItem[].product.productOrderItem`)
  the product is stored as a plain JSON value. That value, and the redis cache entry, used to drop `productOrderId`,
  so responses omitted it. Data stored before this release still reads back without it.
* **software-management** answers `400` instead of `500` when a resource is created or patched with a reference to a
  non-existent resource.
* **`mapping.strictRelationships` has no effect any more.** Nothing is resolved on read, so there is no unresolvable
  target to fail on. The flag, `StrictRelationshipsDefault` and the `application.yaml` blocks stay for now.

## Configuration

No new properties in this repository. The last three below are new in `ngsi-ld-java-mapping` 1.6.33, which this
release upgrades to; `mapping.contextUrl` existed before and now matters for reads too:

| Property                                  | Env var                                   | Default                                                      | Description                                                                                  |
|-------------------------------------------|-------------------------------------------|--------------------------------------------------------------|----------------------------------------------------------------------------------------------|
| `mapping.contextUrl`                      | `MAPPING_CONTEXT_URL`                     | `https://uri.etsi.org/ngsi-ld/v1/ngsi-ld-core-context.jsonld` | Context written with every entity, and now also the one expanded names outside the built-in prefixes are resolved against. |
| `mapping.context-retrieval-timeout`       | `MAPPING_CONTEXT_RETRIEVAL_TIMEOUT`       | `10s`                                                        | Timeout of one retrieval of that context.                                                    |
| `mapping.context-retrieval-retry-delay`   | `MAPPING_CONTEXT_RETRIEVAL_RETRY_DELAY`   | `60s`                                                        | Time after a failed retrieval before the next one is attempted.                              |
| `mapping.compact-against-context`         | `MAPPING_COMPACT_AGAINST_CONTEXT`         | `true`                                                       | Resolve expanded names that are not under the built-in NGSI-LD core/default-context prefixes against `mapping.contextUrl`. `false` skips retrieving the context; names under the built-in prefixes are compacted either way. |

* **A deployment with a custom `general.contextUrl` has to set `mapping.contextUrl` to the same URL.** It already had
  to for writes. Reads now compact sub-attribute names against `mapping.contextUrl`, so a mismatch turns
  multi-instance references on Orion-LD into id-only ones.
* **Every start of every module issues a GET to `mapping.contextUrl`** (the preload), by default to the ETSI core
  context, unless `mapping.compact-against-context` is `false`. If the GET fails, the background preload takes up to
  `mapping.context-retrieval-timeout` and logs a WARN; startup itself is not delayed.
  * **With the default (core) context, set `mapping.compact-against-context=false`.** That skips the startup GET and
    compacts nothing less than `true` does. The default context maps every term under the built-in prefixes, which
    are compacted without the context, so it cannot compact any other name either (see
    [Known limitations](#known-limitations) for `description`). The shipped `application.yaml`s use the core context
    but leave the flag at `true`, so operators have to set it themselves.
  * **With a custom context, keep it `true`** and allow egress to `mapping.contextUrl` or self-host the context.
* The preload runs on a blocking-capable thread, off the event loop. If the context is unreachable then, a read that
  meets an expanded name outside the built-in prefixes makes the library retry the blocking fetch (again up to
  `mapping.context-retrieval-timeout`), at most once per `mapping.context-retrieval-retry-delay`, until it succeeds.
  That fetch runs on the thread mapping the broker response, typically an HTTP event-loop thread, so it stalls every
  other request served on that loop. Until it succeeds, such names are not compacted. Reads that meet no such name
  never fetch.
* While the preload is in flight, a read that needs the context (an expanded name outside the built-in prefixes)
  waits on the resolver's lock (its `retrieve()` is `synchronized`) for up to `mapping.context-retrieval-timeout`, on
  the same event-loop thread and with the same effect.

## Known limitations

* **A PATCH that swaps the ids of a reference list keeps the old entries on Orion-LD.** Without `replaceOnUpdate`
  (the Orion-LD production default, and forced for every IT by the root pom), the broker merges the carried list into
  the stored one by datasetId, so the read returns the old and the new entries. Scorpio replaces the whole
  attribute. The defect predates this release (resolved lists merged the same way).
  `ExternalReferenceIT#patchReplacingTheIdsOfAReferenceList` and the contract's
  `KnownDeviation.REFERENCE_LIST_UPDATE_MERGES` pin the current behaviour. The datasetId of a list entry is the
  reference id, except where a subclass extends it: `RelatedParty` `<id>:<role>`,
  `ProductSpecificationRelationship` `<id>:<relationshipType>`, `RelatedProductOrderItemRef`
  `<productOrderId>:<orderItemId>`.
* **A PATCH that shortens a property list presumably keeps the stored tail on Orion-LD** by the same merge, because
  a property list's datasetId is the position (`list-item:n`). This is inferred, not checked against a broker.
* **On Orion-LD, the `description` of a reference in a list of two or more reads back absent.** Orion-LD 1.9.0
  expands names against its built-in core context v1.8, which maps `description` and `title` to
  `http://purl.org/dc/terms/` and the GeoJSON terms to `https://purl.org/geojson/vocab#`, outside the built-in
  prefixes. In a reference list of two or more entries (a one-entry list is written without datasetId and comes back
  compact), Orion-LD returns such a sub-attribute as `http://purl.org/dc/terms/description`. The shipped
  `mapping.contextUrl` (`https://uri.etsi.org/ngsi-ld/v1/ngsi-ld-core-context.jsonld`) maps `description` to
  `https://uri.etsi.org/ngsi-ld/description`, so the name is not compacted, whatever `mapping.compact-against-context`
  says. The only reference field affected today is the customer `AccountRef.description`, in `Customer.account`
  (customer-management) and `PartyRole.account` (party-role). No reference field is named `title` or after a GeoJSON
  term. The broker output was checked against Orion-LD 1.9.0, and the read result follows from the compactor's code;
  it was not run through the API. Pointing `general.contextUrl` and `mapping.contextUrl` at the broker's versioned core
  context with the flag `true` should compact the name, but this has not been tested. Scorpio is not affected.
* **Customer-bill `FinancialAccountRef.accountBalance` is still not stored** and reads back absent. Writing it as an
  embedded list makes Orion-LD merge several balances into one property that no longer reads back. Its encoding is an
  open decision. The guard test exempts that getter by name, and `FinancialAccountRefIT` is `@Disabled` until the
  decision is made.

## Changed test expectations

Against the previous release, only these existing tests changed their expectations. No IT assertion had to change
from a target-filled value to the posted payload: every IT already posted the fields it asserted.

* `product-catalog` `ExternalReferenceIT` (`assertCategoriesReadBack`): compared only the ids of the read-back
  categories, because an external `ProductOffering.category` was fetched, 404'd and came back id-only. It now
  compares the whole posted reference, which is what the read returns.
* `ErrorResponseCharacterizationTest`: a list or retrieve whose related entity fails to fetch used to fail. It now
  reads back `200` with the stored relationship, and the related entity is never fetched.
* `NgsiLdMappingRegressionTest`: the assertion that `TolerantEntityVOMapper` is the injected mapper became one that
  `InlineReferencesEntitiesRepository` is the injected `EntitiesRepository`.
* `RelationshipTargetConstructorTest` and `RelationshipTargetSerializationTest`: they covered only the targets that
  were fetched on read, and now cover every relationship target.
* `TolerantEntityVOMapperTest` and `BrokerBackedEntitiesRepositoryTest` were removed together with their classes;
  the cases that still apply are in `InlineReferencesEntitiesRepositoryTest`.

## Breaking changes for consumers of `common`

* `BrokerBackedEntitiesRepository` and `TolerantEntityVOMapper` were removed. Code outside this repository that
  depends on a resolving read has to fetch the target itself.
* A domain class with a relationship setter that is not `fromProperties = true` now fails every read that carries
  such a relationship. Mark the setter `fromProperties = true` and give every field of the target class an embedded
  `PROPERTY` getter; the target class also needs a `String` constructor.
