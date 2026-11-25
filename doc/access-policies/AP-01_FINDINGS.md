# AP-01 finding: the policy round-trip

Result of [AP-01](./TICKETS.md#ap-01--prove-the-policy-survives-the-entity-round-trip), which resolves
design correction [D4](./TICKETS.md#blocking-design-corrections).

**Verdict.** Every carrier **preserves** the policy on every storage, at the API, on a store read
through the storage port and on an `entities`-cache hit (Redis included). The one condition is on the
offering term: it must carry a `@schemaLocation` whose schema admits `accessPolicy`/`contractPolicy`.
Without one, the API rejects the term with `400`, so nothing is silently dropped. No carrier is
dropped or lossy, [AP-2](./DECISIONS.md#ap-2--read-the-policies-the-entities-already-carry) stands
unchanged, and [AP-07](./TICKETS.md#ap-07--the-policycarrier-spi-and-direct-policy-resolution) is
**not blocked**.

| Carrier | Verdict |
|---|---|
| `ProductOffering.productOfferingTerm[]`, `accessPolicy`/`contractPolicy` | preserved everywhere **with** a term `@schemaLocation`; rejected (`400`) without one |
| `ProductSpecification.productSpecCharacteristic[].productSpecCharacteristicValue[].value` | preserved everywhere |
| `ServiceSpecification.specCharacteristic[].characteristicValueSpecification[].value` | preserved everywhere |

## How it was measured

* Tests:
  * [`ProductOfferingPolicyRoundTripIT`](../../product-catalog/src/test/java/org/fiware/tmforum/productcatalog/ProductOfferingPolicyRoundTripIT.java)
    covers the offering term and the product specification.
  * [`ServiceSpecificationPolicyRoundTripIT`](../../service-catalog/src/test/java/org/fiware/tmforum/servicecatalog/ServiceSpecificationPolicyRoundTripIT.java)
    covers the service specification.
  * Each case pins its outcome per run and level, so a change in behaviour fails the test.
* Payloads are taken verbatim from [POLICY_EXAMPLES.md](./POLICY_EXAMPLES.md):
  * the UC-1 term as it read before this finding corrected it, without `@schemaLocation`
    ([`uc1-product-offering-term.json`](../../product-catalog/src/test/resources/access-policy/uc1-product-offering-term.json)).
    `OFFERING_TERM_WITH_SCHEMA` adds the test schema to it;
  * the UC-6 service specification ([`uc6-service-specification.json`](../../service-catalog/src/test/resources/access-policy/uc6-service-specification.json));
  * the UC-6 policy object placed in a product specification characteristic
    ([`uc6-policy.json`](../../product-catalog/src/test/resources/access-policy/uc6-policy.json)),
    because the examples have no product-spec case.
* Each case is checked at three levels:
  * **API**: the raw `GET /{id}` body, read as a string and not through the generated VOs.
  * **Store read**: `EntityReadRepository.get(id, <DomainClass>)`, after the `entities` cache entry
    for the id is invalidated.
  * **Cached read**: the same call again, once the entry is confirmed to be in the cache. This level
    exists on orion-ld and scorpio only. The PostgreSQL adapter does not read through the cache.
* Equality is Jackson `JsonNode.equals` between the request subtree and the response or domain
  subtree. Key order and whitespace are ignored. Array order, value types, `@`-prefixed keys and URL
  strings must match exactly.
* Runs: `orion-ld`, `scorpio`, `postgresql` (each with the in-memory cache), plus `orion-ld+redis` and
  `scorpio+redis`. `general.escape-forbidden-characters` stayed at its default, `true`.

## Outcome matrix

`P` preserved, `R` rejected with `400`, `–` level not reached. The lossy and dropped outcomes did not
occur. The columns are the runs: orion-ld (OL), scorpio (SC), postgresql (PG), and the two Redis runs.

| Case | Level | OL | SC | PG | OL+redis | SC+redis |
|---|---|---|---|---|---|---|
| `OFFERING_TERM_VERBATIM` (UC-1 as documented) | API | R | R | R | R | R |
| `OFFERING_TERM_WITH_SCHEMA` (UC-1 + `@schemaLocation`) | API | P | P | P | P | P |
| | store read | P | P | P | P | P |
| | cached read | P | P | – | P | P |
| `PRODUCT_SPEC_CHARACTERISTIC_VALUE` | API | P | P | P | P | P |
| | store read | P | P | P | P | P |
| | cached read | P | P | – | P | P |
| `PRODUCT_SPEC_WRONG_CONTAINER` | API | R | R | R | R | R |
| `SERVICE_SPEC_CHARACTERISTIC_VALUE` | API | P | P | P | P | P |
| | store read | P | P | P | P | P |
| | cached read | P | P | – | P | P |
| `SERVICE_SPEC_WRONG_CONTAINER` | API | R | R | R | R | R |
| `SERVICE_SPEC_WRONG_CHARACTERISTIC_CONTAINER` | API | R | R | R | R | R |

The offering-term rows hold for `accessPolicy` and `contractPolicy` separately.

What the payloads put at risk, and what survived:
* `@context` as an array that mixes a URL and an object (UC-1), and as a bare object (UC-6).
* `@id`, `@type` and `@value` keys.
* Single-element arrays (`permission` in UC-1). Scorpio did not compact them.
* A single permission object that is not wrapped in an array (`odrl:permission` in UC-6). No backend
  wrapped it.
* `did:web:` and `https://` strings.

All of these round-trip unchanged on every run, including after `EntityVOSerializer` on a Redis hit.

## The accessor paths for `PolicyCarrier` (AP-07)

| Carrier | Accessor on the object `EntityReadRepository.get` returns |
|---|---|
| Offering term | `ProductOffering.getProductOfferingTerm().get(i).getAdditionalProperties()`: a `List<UnmappedProperty>`. Select by `getName()` equal to `accessPolicy` or `contractPolicy`, then take `getValue()` |
| Product spec | `ProductSpecification.getProductSpecCharacteristic().get(i).getProductSpecCharacteristicValue().get(j).getTmfValue()` |
| Service spec | `ServiceSpecification.getSpecCharacteristic().get(i).getCharacteristicValueSpecification().get(j).getTmfValue()` |

* **Both specification carriers end at one holder,**
  [`org.fiware.tmforum.service.CharacteristicValueSpecification`](../../service-shared-models/src/main/java/org/fiware/tmforum/service/CharacteristicValueSpecification.java)`.getTmfValue()`.
  `ProductSpecificationCharacteristic` imports the service class. The similarly named
  `org.fiware.tmforum.productcatalog.domain.CharacteristicValueSpecification` in `product-catalog` is
  not on this path. One value accessor
  therefore serves both specification types, and only the walk down to the holder differs.
  `ProductOfferingTerm` (in `product-shared-models`) has no policy field of its own.
* **Unwrapping.**
  * `UnmappedProperty.getValue()` and `getTmfValue()` are untyped `Object`s that hold the
    deserialised JSON tree.
  * Convert them with `ObjectMapper.valueToTree`/`convertValue`, as the ITs do, and do not cast to a
    concrete type.
  * No re-wrapping of compacted arrays and no `@context` re-expansion is needed. The value is
    JSON-equal to what the provider wrote.
  * A characteristic is a policy carrier when its `valueType` is `authorizationPolicy`. The value
    container can hold several entries, so iterate over them.
* **The domain object is the unreshaped store shape.** On the API path, `BaseMapper.coerceToSchema`
  reshapes additional properties to the term's schema. The domain object the port returns does not go
  through `BaseMapper`. A `PolicyCarrier` therefore sees what was stored, even when a schema would
  change the API rendering.

## What a provider must write

* **Offering term:** a `@schemaLocation` on the `productOfferingTerm` entry, pointing to a schema that
  admits `accessPolicy` and `contractPolicy` as objects. This is the same rule as for every other
  extension in this project (`ValidatingDeserializer`, `BaseMapper`). The Dataspace Protocol side
  already writes the term this way: the DSC's
  [DSP integration guide](https://github.com/FIWARE/data-space-connector/blob/main/doc/DSP_INTEGRATION.md#prepare-the-offering)
  points it at `contract-definition.json`. Only the UC-1 example lacked it, and
  [POLICY_EXAMPLES.md](./POLICY_EXAMPLES.md#an-entity-restricted-by-a-subject-attribute) is corrected.
  The ITs use the minimal test schema `product-offering-term-policy-schema.json` from
  [`static-fileserver.yaml`](../../k3s/schema-provider/static-fileserver.yaml), which declares both
  keys as `{"type":"object"}` and nothing else.
* **Admitting is not enough when the schema requires.** `ValidatingDeserializer.validateWithSchema`
  enforces the whole schema on write, `required` included. `contract-definition.json` requires both
  `accessPolicy` and `contractPolicy`, so a term that points at it and leaves one out gets `400`. A
  UC-2 term ("no access policy") must then carry a permit-all `accessPolicy`, or the deployment must
  host a schema that does not require it.
* **Specifications:** nothing beyond the documented examples, as long as the container names match the
  type. The values need no schema.
* **The term rejection does not name the key.** A term without `@schemaLocation` gets `400` with
  `No additional properties are allowed without a @schemaLocation` and the Jackson reference chain
  `ProductOfferingCreateVO["productOfferingTerm"]`. The body never says `accessPolicy`, so the
  documentation has to say it.

## Wrong containers

Every wrong-container case is rejected with `400` on every run. None of them is stored and silently
dropped.

| Case | Request | Response body contains |
|---|---|---|
| `PRODUCT_SPEC_WRONG_CONTAINER` | `characteristicValueSpecification` inside `productSpecCharacteristic[]` | the unknown-property message, chain `ProductSpecificationCreateVO["productSpecCharacteristic"]` |
| `SERVICE_SPEC_WRONG_CONTAINER` | `productSpecCharacteristicValue` inside `specCharacteristic[]` | the unknown-property message, chain `ServiceSpecificationCreateVO["specCharacteristic"]` |
| `SERVICE_SPEC_WRONG_CHARACTERISTIC_CONTAINER` | `productSpecCharacteristic` at the top level of a service specification | the unknown-property message, and from schema validation `Additional properties not allowed: [productSpecCharacteristic]` |

Only a wrong top-level key is named in the response. A wrong nested container shows up only as the
reference chain to its parent.

## Not covered

* **List reads** (`findEntities`). Only reads by id are tested, so AP-11/AP-12 must not assume
  that a list read keeps the policy.
* **`ProductOfferingPrice.productOfferingTerm`**, which is a second term carrier and is not tested.
* **PATCH**: both a policy written by PATCH and a PATCH that replaces or removes one.
* **Redis on postgresql.** The PostgreSQL adapter does not read through the `entities` cache, so there
  is no cached level to test.
* **A real-world term schema.** No IT runs with the DSC's `contract-definition.json`. Reading it:
  * it gives each key as `{"$ref": "policy.json"}` with no `type`. `BaseMapper.coerceToSchema` only
    acts on a `type` and does not follow `$ref`, so the API response is not reshaped;
  * `policy.json` declares only `permissions: {"type":"array"}`, has no `required` and allows
    additional properties, so the UC-1 policy (`permission`, singular) validates;
  * the constraint that does bite is the `required` pair (see
    [What a provider must write](#what-a-provider-must-write)).
* **A policy without an explicit `@context`**, and characteristics whose `valueType` is not
  `authorizationPolicy`.

## Reproducing

```bash
mvn install -pl product-catalog,service-catalog -am -DskipTests -DskipITs
mvn integration-test -pl product-catalog -Dit.test=ProductOfferingPolicyRoundTripIT -DskipTests -Dbroker=<orion-ld|scorpio|postgresql> [-Dcache=redis]
mvn integration-test -pl service-catalog -Dit.test=ServiceSpecificationPolicyRoundTripIT -DskipTests -Dbroker=<orion-ld|scorpio|postgresql> [-Dcache=redis]
```

Run `mvn clean` on the module between runs. Otherwise the K3S cluster from the previous run is reused.
