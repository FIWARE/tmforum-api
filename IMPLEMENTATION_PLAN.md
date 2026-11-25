# Implementation Plan: TMForum - Access Policies - 01 - Prove the policy round-trip

## Overview
Correction D4 in `doc/access-policies/TICKETS.md` (AP-01). This ticket adds tests only, no enforcement. They show whether an ODRL policy written on each of the three carriers survives POST -> storage -> GET, and whether it can be reached from the domain object the `storage-api` port returns. The tests run on orion-ld, scorpio and postgresql. They pin the outcome each backend actually produces, and a finding document records it per carrier along with the accessor path a `PolicyCarrier` will use.

## Facts established while planning (read before starting)

- **Carriers and their domain accessors**
  1. Offering term:
     - Domain: `ProductOffering.getProductOfferingTerm().get(i).getAdditionalProperties()`. That is a `List<UnmappedProperty>` (`getName()` is `accessPolicy` or `contractPolicy`, `getValue()` holds the policy).
     - `ProductOfferingTerm` (product-shared-models) extends `Entity` and has no policy field.
     - Stored as `PROPERTY_LIST` `productOfferingTerm`.
  2. Product spec:
     - Domain: `ProductSpecification.getProductSpecCharacteristic().get(i).getProductSpecCharacteristicValue().get(j).getTmfValue()`. The value is an `Object`, held by `org.fiware.tmforum.service.CharacteristicValueSpecification` (imported by `product.ProductSpecificationCharacteristic`, l.31), which is not an `Entity`. `productcatalog.domain.CharacteristicValueSpecification` is not on this path.
     - API path: `productSpecCharacteristic[].productSpecCharacteristicValue[].value`. MapStruct maps `value` to `tmfValue`.
  3. Service spec:
     - Domain: `ServiceSpecification.getSpecCharacteristic().get(i).getCharacteristicValueSpecification().get(j).getTmfValue()`. Same holder class as the product spec: `service.CharacteristicValueSpecification` (`service.CharacteristicSpecification`, l.23). Both specification carriers therefore end at one `getTmfValue()`, and the finding must say so.
     - API path: `specCharacteristic[].characteristicValueSpecification[].value`.
- **Unknown fields need `@schemaLocation`.** `ProductOfferingTerm` in `api/tm-forum/product-catalog/api.json` has `@schemaLocation`. For such VOs, `common/.../mapping/ValidatingDeserializer` rejects unknown properties with **400** ("No additional properties are allowed without a @schemaLocation"). `BaseMapper.afterMappingToEntity`/`afterMappingFromEntity` copy unknown properties to and from `additionalProperties` only when `@schemaLocation != null`.
  - Expected result: the UC-1 term from `POLICY_EXAMPLES.md` sent **verbatim** (no `@schemaLocation`) is rejected. It is only stored once the term carries a `@schemaLocation`.
  - Both variants are tested. Do not "fix" either one, only pin what happens.
- **Read-side reshaping.** On read, `BaseMapper.coerceToSchema` reshapes additional properties to the linked schema. Two consequences:
  - The test schema must declare `accessPolicy` and `contractPolicy` only as `{"type":"object"}`, with no nested `properties` and no `array` types. Otherwise the reshaping could hide broker-side compaction in the API response.
  - The repository-level check bypasses `BaseMapper`, so it sees the raw stored shape. The API result and the domain result can differ, and the finding must report both.
- **Payloads.** In `doc/access-policies/POLICY_EXAMPLES.md`:
  - §"An entity restricted by a subject attribute" (the code block at lines ~39-91) is **one `ProductOfferingTerm` object**, not a whole offering. The IT wraps it as `{"name": ..., "productOfferingTerm": [ <term> ]}`.
  - §"A visibility policy on a service specification" (lines ~184-223) is a complete `ServiceSpecification` create body.
  - There is no product-spec example. Build one from the same UC-6 policy object, placed under `productSpecCharacteristic[0].productSpecCharacteristicValue[0].value`.
- **Test infrastructure.**
  - Raw HTTP: `@Client("/") HttpClient` with `exchange(..., String.class)` and `callAndCatch` (pattern: `product-catalog/.../ListQueryIT`). The bodies must be compared raw, not through the generated test clients, because VO deserialization would hide what the server actually returns.
  - Port bean: inject `org.fiware.tmforum.storage.EntityReadRepository` and call `get(URI, Class)`. The active backend follows `-Dbroker`.
  - Cleanup: `AbstractApiIT` cleans the single type from `getEntityType()`. For any other type, inject `TestEntityCleaner` and call `deleteAll(type)` in a `@BeforeEach`.
  - Backend detection: `Environment.getActiveNames()` contains `orion-ld`, `scorpio` or `CommonConstants.POSTGRESQL_ENVIRONMENT`. orion-ld and scorpio have no constants, so define them in the IT.
  - Schemas: keys of the `schema-provider` ConfigMap in `k3s/schema-provider/static-fileserver.yaml`, served at `http://localhost:3000/schemas/<key>` (see existing usage in `ProductOfferingApiIT` ~l.955).
  - `general.escape-forbidden-characters` defaults to `true` (`GeneralProperties`). Keep the default.
  - The "pin today's behaviour so a fix fails loudly" pattern exists in `storage-api/src/test/.../contract/KnownDeviation.java`. Follow that idea, not the class itself.
- **Equivalence criterion for "byte-equivalent".**
  - Rule: Jackson `JsonNode.equals` between the subtree from the request fixture and the same JSON-pointer subtree of the GET body. Object key order and whitespace are ignored. Array order, value types (`"1"` vs `1`), `@`-prefixed keys and URL strings must all match exactly.
  - Do **not** use `AbstractApiIT.assertEqualsIgnoringEmptyLists`/`assertEqualsForFields`, because they mask differences.
- **Entity cache on the NGSI-LD backends.** `NgsiLdBaseRepository.retrieveEntityById` is `@Cacheable("entities")` (2s in the test config). `createEntity` carries `@CachePut`, but it returns `Mono<Void>`: Micronaut's `CacheInterceptor` builds no put operations for a void-valued publisher (`CacheOperation`: `putOperations = isVoid ? null : ...`), and the put would sit in a `flatMap` that never fires. Create therefore caches nothing, and the GET after the POST is a miss that reads the broker. The cached entry is then the broker's `EntityVO`, but with `-Dcache=redis` it also went through `EntityVOSerializer`. So the IT must:
  - never compare the POST response body, which is built from the client-side object;
  - invalidate the entry before the port-level read, so it reads the store: inject `CacheManager` and call `getCache(CommonConstants.ENTITIES_CACHE_NAME).invalidate(id)` when `getCacheNames()` contains it. On postgresql the cache exists too (`application-in-memory.yaml` declares `micronaut.caches.entities`), but the PostgreSQL adapter never fills it, so the invalidation is a harmless no-op there.
  - also test the cached read, because AP-07 reads through this port and gets cache hits in production: after the store read, assert the entry is now in the cache (`getCache(...).get(id, Object.class)` is present, so the check cannot silently become a second miss when the 2s TTL lapses), then read through the port again. That read is a hit, and with `-Dcache=redis` its `EntityVO` went through `EntityVOSerializer` and back. Gate this on the detected backend (orion-ld or scorpio), **not** on `getCacheNames()`: the name is present on postgresql as well, where the entry never appears and the presence assertion would fail. On postgresql the cached-read level is absent from the expected maps.
- **Cache dimension.** The in-memory cache keeps the `EntityVO` instance, so only `-Dcache=redis` can change the value on a hit. Outcomes are therefore keyed by storage run, not only broker: `orion-ld`, `scorpio`, `postgresql` (all `-Dcache=in-memory`, the default) plus `orion-ld+redis` and `scorpio+redis`. postgresql gets no redis run, because its adapter does not read through the `entities` cache (the cache bean exists but stays empty).
- Build/run:
  - `mvn install -pl product-catalog,service-catalog -am -DskipTests -DskipITs`
  - `mvn integration-test -pl product-catalog -Dit.test=ProductOfferingPolicyRoundTripIT -DskipTests -Dbroker=<orion-ld|scorpio|postgresql> [-Dcache=redis]`. The default broker is `orion-ld`, the default cache `in-memory`; the run key comes from `Environment.getActiveNames()` (`redis` is active with `-Dcache=redis`). Run `mvn clean` on the module between runs, or the K3S cluster from the previous run is reused.

## Steps

### Step 1: Product-catalog round-trip IT (offering term + product-spec characteristic), pinned on all three storages
**Files**
- `k3s/schema-provider/static-fileserver.yaml`: add the ConfigMap key `product-offering-term-policy-schema.json`. Use draft 2020-12, with `$id` following the existing entries' pattern, `"type":"object"`, and `properties.accessPolicy`/`properties.contractPolicy` each `{"type":"object"}`. Nothing more (see "Read-side reshaping" above).
- `product-catalog/src/test/resources/access-policy/`:
  - `uc1-product-offering-term.json`: the UC-1 term, copied verbatim from `POLICY_EXAMPLES.md`.
  - `uc6-policy.json`: the UC-6 `value` object, verbatim.
  - The IT builds the request bodies from these two files. Pinned observed shapes for lossy cases go next to them as `*-observed-<run>.json`.
- `product-catalog/src/test/java/org/fiware/tmforum/productcatalog/ProductOfferingPolicyRoundTripIT.java`: new, `@MicronautTest(packages = {"org.fiware.tmforum.productcatalog"}, contextBuilder = FirstMatchConfigContextBuilder.class)`, extends `AbstractApiIT`, `getEntityType()` = `ProductOffering.TYPE_PRODUCT_OFFERING`.
  - Also clean `ProductSpecification.TYPE_PRODUCT_SPECIFICATION`.

**Test cases** (one `@ParameterizedTest` over a case enum/record with a `@MethodSource`)

| Case | Request | JSON pointer(s) checked | Domain accessor |
|---|---|---|---|
| `OFFERING_TERM_VERBATIM` | POST `/tmf-api/productCatalogManagement/v4/productOffering` with the term exactly as documented | `/productOfferingTerm/0/accessPolicy`, `/productOfferingTerm/0/contractPolicy` | term `additionalProperties` by name |
| `OFFERING_TERM_WITH_SCHEMA` | same, plus `"@schemaLocation": "http://localhost:3000/schemas/product-offering-term-policy-schema.json"` on the term | same | same |
| `PRODUCT_SPEC_CHARACTERISTIC_VALUE` | POST `.../productSpecification` with `productSpecCharacteristic[{id:"visibility", name, valueType:"authorizationPolicy", productSpecCharacteristicValue:[{isDefault:true, value:<uc6>}]}]` | `/productSpecCharacteristic/0/productSpecCharacteristicValue/0/value` | `getProductSpecCharacteristic().get(0).getProductSpecCharacteristicValue().get(0).getTmfValue()` |
| `PRODUCT_SPEC_WRONG_CONTAINER` | the same characteristic, but with the service names `characteristicValueSpecification[].value` | n/a | n/a |

**What each case asserts**
1. The POST status.
2. When the POST succeeds, the GET-by-id body subtree at each pointer against the fixture subtree (API level).
3. `objectMapper.valueToTree(<domain accessor>)` on the object from `EntityReadRepository.get(id, ProductOffering.class | ProductSpecification.class)` against the same fixture subtree (port level, store read). Invalidate the `entities` cache entry for the id first (see "Entity cache" above).
4. On the NGSI-LD backends, the same comparison on a second port read that is a confirmed cache hit (port level, cached read; see "Entity cache" above).
5. The outcome per run (see "Cache dimension"). Each case carries one `Map<run, Outcome>` per level (API, store read, cached read), where `Outcome` ∈ {`PRESERVED`, `LOSSY`, `DROPPED`, `REJECTED`}:
   - `PRESERVED`: the subtrees are equal.
   - `LOSSY`: the actual subtree equals the pinned `*-observed-<run>.json`, and differs from the fixture.
   - `DROPPED`: the stored value is absent.
   - `REJECTED`: 4xx on POST, with the status pinned.

The expected map holds what was **observed**. A carrier that turns out lossy gets a test that pins the lossy shape, not a skipped test.

**Procedure**
1. Write every case expecting `PRESERVED`, except the two expected 400s (`OFFERING_TERM_VERBATIM`, `PRODUCT_SPEC_WRONG_CONTAINER`).
2. Run on orion-ld, scorpio and postgresql, then on orion-ld and scorpio with `-Dcache=redis`, and pin each result.
3. When a value is lost, put a one-line comment on the case naming what is lost, e.g. "scorpio expands `@context` / compacts single-element arrays".

**Acceptance**
- `ProductOfferingPolicyRoundTripIT` is green on `-Dbroker=orion-ld`, `-Dbroker=scorpio` and `-Dbroker=postgresql`, and on orion-ld and scorpio with `-Dcache=redis`.
- No production code changed.
- The step's completion summary lists the observed outcome matrix (case × run × level).

### Step 2: Service-catalog round-trip IT, pinned on all three storages
**Files**
- `service-catalog/src/test/resources/access-policy/uc6-service-specification.json`: the UC-6 body, verbatim from `POLICY_EXAMPLES.md`.
- `service-catalog/src/test/java/org/fiware/tmforum/servicecatalog/ServiceSpecificationPolicyRoundTripIT.java`: new. It has the same structure, `Outcome` model, run detection, store read after cache invalidation and cached second read as step 1; copy the small helpers, do not move them into `common`. `getEntityType()` = `ServiceSpecification.TYPE_SERVICE_SPECIFICATION`.

**Test cases** (parameterized)

| Case | Request | JSON pointer checked | Domain accessor |
|---|---|---|---|
| `SERVICE_SPEC_CHARACTERISTIC_VALUE` | POST `/tmf-api/serviceCatalogManagement/v4/serviceSpecification` with the verbatim body | `/specCharacteristic/0/characteristicValueSpecification/0/value` | `getSpecCharacteristic().get(0).getCharacteristicValueSpecification().get(0).getTmfValue()` via `EntityReadRepository.get(id, ServiceSpecification.class)` |
| `SERVICE_SPEC_WRONG_CONTAINER` | the same body with the product value container name `productSpecCharacteristicValue` | n/a | n/a |
| `SERVICE_SPEC_WRONG_CHARACTERISTIC_CONTAINER` | the variant with both product names, `productSpecCharacteristic` at the top level | n/a | n/a |

The wrong-container case pins whether the most common authoring mistake is rejected (expected 400) or silently dropped.

- Both base paths come from the `basePath` in `api/tm-forum/*/api.json` and are confirmed.
- Run and pin on the five runs exactly as in step 1.

**Acceptance**
- `ServiceSpecificationPolicyRoundTripIT` is green on orion-ld, scorpio and postgresql, and on orion-ld and scorpio with `-Dcache=redis`.
- No production code changed.
- The outcome matrix is in the completion summary.

### Step 3: Written finding and, if needed, the AP-2 correction
**Files**
- New `doc/access-policies/AP-01_FINDINGS.md`. For each of the three carriers:
  - The outcome per run at the API level and at the domain level (store read and cached read, so it says whether a Redis-served read keeps the policy): preserved / lossy (exactly what is lost) / dropped / rejected. Copy the numbers from the pinned tests of steps 1 and 2, not from memory.
  - The **exact accessor path** the `PolicyCarrier` (AP-07) will use, from the facts list above, plus any unwrapping it needs. Examples of unwrapping: `UnmappedProperty.getValue()` is a `Map`, a compacted array must be re-wrapped. Record that both specification carriers share the holder `service.CharacteristicValueSpecification.getTmfValue()`, so one accessor serves both.
  - What a provider must write. Expected: the offering term needs a `@schemaLocation` whose schema admits `accessPolicy`/`contractPolicy`. If confirmed, correct the UC-1 example in `POLICY_EXAMPLES.md` to carry it and to name the schema.
  - The wrong-container results.
  - What is not covered: list reads (`findEntities`), `ProductOfferingPrice.productOfferingTerm`, PATCH, Redis on postgresql (its adapter does not read through the `entities` cache).
- `doc/access-policies/TICKETS.md`: set the D4 row status to the result and link the finding.
- If any carrier is **dropped** on any backend, or is unusable in the form providers write it:
  - Add a "Correction proposed by AP-01" subsection to AP-2 in `doc/access-policies/DECISIONS.md`. Options to weigh: a typed `accessPolicy`/`contractPolicy` on `ProductOfferingTerm`, a mandatory extension schema, or resolution at the PAP by target.
  - Add a follow-up ticket entry to `TICKETS.md` that implements the chosen correction.
  - Mark AP-07 as blocked on agreeing it.

**Acceptance**
- The finding covers all three carriers on all three storages and names the accessor paths.
- The completion summary, which becomes the PR text and Taiga comment, repeats the per-carrier verdict in one line each, and states whether AP-07 is blocked.
