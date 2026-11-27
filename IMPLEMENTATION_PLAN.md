# Implementation Plan: TMForum - Access Policies - 08 - PolicyCarrier SPI and policy resolution

## Overview
Add a domain-agnostic `PolicyCarrier` SPI to `access-policy`. Given an entity and a scope, it returns the
ODRL policies the entity carries at that scope, keyed by `odrl:uid`, with `dspace:scope` as the
discriminator. A registry short-circuits ungoverned types before any field is read. The carriers for
`ProductOffering`, `ProductSpecification` and `ServiceSpecification` live in the shared-models modules
and use the accessor paths from `doc/access-policies/AP-01_FINDINGS.md` § "The accessor paths for
`PolicyCarrier`". Turning resolved policies into PDP calls is AP-08 and out of scope here.

## Decisions (resolving the ticket's open points)

- **Where the implementations live.** The module graph is `access-policy` ← `common` ←
  `service-shared-models` ← `product-shared-models`, so both shared-models modules see the SPI.
  - `common` gets the domain-free value conversion (`Object` → JSON map, through the app's `ObjectMapper`).
  - `service-shared-models` gets the shared `authorizationPolicy` characteristic walk and the
    `ServiceSpecification` carrier.
  - `product-shared-models` gets the `ProductOffering` and `ProductSpecification` carriers.
  - `customer-bill-management` is the only consumer of `product-shared-models` that does not also
    depend on `service-shared-models`. Step 3 adds that dependency so the carrier classes load there.
- **Split of responsibilities.** A carrier only walks its domain object to raw policy values
  (`carriedPolicies(entity)`). Scope filtering, uid keying, de-duplication and malformed handling are
  implemented once, in `access-policy`. The SPI still offers `resolve(entity, scope)` as a default
  method, as the ticket asks.
- **Scope semantics.**
  - A policy resolves at scope `S` when at least one of its ODRL rules (`permission`, `prohibition`,
    `obligation`) declares `dspace:scope` = `S`.
  - The resolved policy keeps every policy-level key (`@context`, `@type`, `odrl:uid`, `assigner`, …).
    Each rule key keeps only the rules that declare `S`. A rule key left with no rules is removed.
  - A rule without a scope is never part of the result at any scope.
  - The key the policy sits under (`accessPolicy`/`contractPolicy`) is not a scope. Only
    `dspace:scope` decides.
- **Malformed policies.** Resolution never throws. A policy that cannot be read becomes an
  `UnreadablePolicy(location, policyUid?, reason)` in the result. It turns into a decision through a
  new `AccessDecision.Source.UNREADABLE_POLICY`, whose `permitted` follows `fail-open`, like
  `PDP_FAILURE`. A separate source lets audit (AP-18) tell bad authoring from a PDP outage.

## Steps

### Step 1: PolicyCarrier SPI, scope resolution and governed-type short-circuit in `access-policy`
All files are under `access-policy/src/main/java/org/fiware/tmforum/accesspolicy/` unless a path says otherwise.

**SPI and result types (public):**
- `PolicyScope` (enum): `CATALOG("catalog")`, `CONTRACT_NEGOTIATION("contract.negotiation")`, with
  `value()` for the wire string that `ValidationRequest.of` takes later.
- `CarriedPolicy` (record): `String location, Object value`. The location is a path used in
  diagnostics, such as `productOfferingTerm[0].accessPolicy`. The value is the raw JSON tree.
- `UnreadablePolicy` (record): `String location, String policyUid` (nullable), `String reason`. It
  has `AccessDecision decision(boolean failOpen)`.
- `ResolvedPolicies` (record):
  - `Map<String, Map<String, Object>> policies`: unmodifiable, in insertion order, keyed by uid.
  - `List<UnreadablePolicy> unreadable`.
  - A shared `NONE` constant and `isEmpty()`.
- `PolicyCarrier<T>` (interface):
  - `Class<T> carriedType()`.
  - `List<CarriedPolicy> carriedPolicies(T entity)`.
  - Default `ResolvedPolicies resolve(T entity, PolicyScope scope)`, which delegates to the
    resolution below.

**`AccessDecision`:**
- Add `Source.UNREADABLE_POLICY` and the factory `unreadable(policyUid, reason, failOpen)`.
- In `DecisionCache`, change the no-cache guard from `== PDP_FAILURE` to `!= POLICY`. An unreadable
  decision never reaches the PDP, but if one did arrive it must not be cached.
- `CachedDecisionSerializationTest` already iterates `Source.values()`, so it covers the new value.

**`ScopedPolicyResolution` (package-private final class, static `resolve(List<CarriedPolicy>, PolicyScope)`):**
- **JSON-LD key handling.** Reuse the key expansion in `ResourceConstraintDetector`: `Bindings`,
  `expandKey`, `collectContexts` and `DEFAULT_PREFIXES`.
  - Extract them into a package-private `JsonLdKeys` helper that both classes use.
  - Do not change the detector's behaviour. `ResourceConstraintDetectorTest` must stay green
    unchanged.
- **Key recognition:**
  - The uid is the key that expands to `http://www.w3.org/ns/odrl/2/uid`. If there is none, use
    `@id`. It accepts a string, `{"@id": …}` or a single-element list.
  - The rules are the keys that expand to the ODRL `permission`/`prohibition`/`obligation` IRIs. A
    bare term (`permission`) counts only when the policy's `@context` includes the trusted
    `http://www.w3.org/ns/odrl.jsonld`, as in UC-1.
  - The scope is the key that expands to `https://w3id.org/dspace/2024/1/scope`. It accepts a
    string, `{"@value": …}` or a single-element list of either.
  - Put every IRI and keyword in a named constant. Reuse the namespace constants the detector
    already defines.
- **What makes a policy unreadable** (tagged result, never an exception):
  - the value is not a JSON object;
  - it has no uid;
  - a rule value is neither an object nor a list of objects;
  - a scope value has an unexpected shape;
  - a relevant key does not expand (`expandKey` returns null);
  - any `RuntimeException` during the walk (catch it around each policy).
- **Duplicates.** Two carried policies with the same uid and equal content collapse to one entry.
  When the content differs, drop both and add one `UnreadablePolicy(uid, "conflicting policies share
  odrl:uid")`. POLICY_EXAMPLES.md says such a conflict fails resolution.

**`PolicyCarriers` (`@Singleton`, `@Requires(property = AccessPolicyProperties.ENABLED_PROPERTY, value = "true")`):**
- Takes `List<PolicyCarrier<?>>` in its constructor and fails at startup if two carriers claim the
  same type.
- `boolean isGoverned(Class<?> type)`. AP-08 can then short-circuit `get(id, Class)` before it reads.
- `ResolvedPolicies resolve(Object entity, PolicyScope scope)`.
  - Lookup goes through a `ClassValue<PolicyCarrier<?>>`. It is computed once per class, walks
    `isAssignableFrom` for subclasses and caches a miss.
  - A null or ungoverned entity returns `ResolvedPolicies.NONE` and calls no method on the entity.
  - An exception thrown by a carrier's `carriedPolicies` becomes one `UnreadablePolicy` with the
    entity's type name as its location.
- Javadoc: one line per public type and method, matching the module's density.

**ArchUnit:**
- Add `com.tngtech.archunit:archunit` (test scope, version managed in the root pom) to
  `access-policy/pom.xml`.
- New `access-policy/src/test/java/org/fiware/tmforum/accesspolicy/architecture/DomainBoundaryTest.java`,
  modelled on `common/.../architecture/StorageBoundaryTest.java` (parameterized over `Named<ArchRule>`).
  The rule: no main class in `org.fiware.tmforum.accesspolicy..` depends on classes in
  `org.fiware.tmforum..` outside `org.fiware.tmforum.accesspolicy..` and `org.fiware.tmforum.storage..`,
  nor on `io.github.wistefan..` or `org.fiware.ngsi..`.

**Tests (JUnit 5, parameterized):**
- `ScopedPolicyResolutionTest`. Fixtures are maps built inline or JSON parsed with nimbus
  `JSONObjectUtils.parse`, which is already a compile dependency.
  - Well-formed policies in the UC-1 shape (bare terms plus `odrl.jsonld`) and the UC-6 shape
    (`odrl:` prefixes, single unwrapped `odrl:permission`), at both scopes.
  - A scope-less policy resolves to nothing at either scope.
  - A mixed policy (one `catalog` permission and one scope-less permission) keeps only the
    `catalog` rule.
  - A scope given as `@value` or as a list.
  - Every malformed case above yields `UnreadablePolicy` and never throws.
  - Duplicate uids, equal and conflicting.
- `PolicyCarriersTest`:
  - Mock an ungoverned entity, then `verifyNoInteractions(entity)` and verify no carrier is called.
  - A subclass of a governed type resolves through the parent's carrier.
  - Two carriers for one type fail construction.
  - A throwing carrier gives an unreadable result.
- `AccessDecisionTest`: add a case for `unreadable(...)` under both `failOpen` values.
- `DecisionCacheTest`: an `UNREADABLE_POLICY` decision is not cached.

**Acceptance:**
- `mvn test -pl access-policy` is green, the ArchUnit rule passes and the enforcer `below-common` rule
  still holds.
- An ungoverned type resolves to `NONE` without touching a field.
- A scope-less permission resolves to nothing.
- Malformed JSON yields a tagged `UnreadablePolicy`.

### Step 2: Policy value conversion in `common` and the `ServiceSpecification` carrier in `service-shared-models`
**`common`:**
- Add `common/src/main/java/org/fiware/tmforum/common/accesspolicy/PolicyValues.java`, a small final
  utility class.
- Static `CarriedPolicy carried(String location, Object value, ObjectMapper objectMapper)`:
  - A `Map` value is passed through.
  - Any other non-null value is converted with `objectMapper.convertValue(value, Map.class)`, as
    AP-01 recommends instead of a cast.
  - A failed conversion keeps the raw value, so `access-policy` tags it "not a JSON object".
  - It never throws.
- `common` already has Jackson 3 and depends on `access-policy`.
- Unit test `PolicyValuesTest`, parameterized over a map, a `JsonNode`, a string, a list and an
  unconvertible value.

**`service-shared-models` (`org.fiware.tmforum.service`):**
- `CharacteristicPolicyValues`: the one value accessor both specification types share (AP-01).
  - Static `List<CarriedPolicy> fromValues(String locationPrefix,
    List<CharacteristicValueSpecification> values, ObjectMapper)`.
  - It takes `getTmfValue()` of each non-null entry, null-safe, with locations such as
    `<prefix>[j].value`.
  - Put the `authorizationPolicy` value type in a public constant `AUTHORIZATION_POLICY_VALUE_TYPE`.
- `ServiceSpecificationPolicyCarrier` (`@Singleton`, `@Requires(property =
  AccessPolicyProperties.ENABLED_PROPERTY, value = "true")`, injects `ObjectMapper`).
  - Walks `getSpecCharacteristic()`, keeps the characteristics whose `getValueType()` equals the
    constant, and delegates to `CharacteristicPolicyValues`.
  - Location: `specCharacteristic[i].characteristicValueSpecification[j].value`.
  - Null lists or entries yield an empty list. It never throws.

**Tests:**
- `ServiceSpecificationPolicyCarrierTest`, parameterized over:
  - the well-formed UC-6 specification (copy the policy from
    `service-catalog/src/test/resources/access-policy/uc6-service-specification.json` into a test
    resource of this module);
  - a scope-less permission;
  - a malformed policy (a string value, a policy without a uid);
  - a characteristic whose `valueType` is not `authorizationPolicy`, which is ignored;
  - null containers.
- Each case asserts on `carrier.resolve(spec, PolicyScope.CATALOG)`: the uids, the empty result and
  the unreadable entries.
- Use a real `JsonMapper` from `tools.jackson.databind.json`. Add the test dependencies the module
  lacks (e.g. `mockito-junit-jupiter`, if needed). JUnit params is already present.

**Acceptance:** `mvn test -pl common,service-shared-models -am` is green.

### Step 3: `ProductOffering` and `ProductSpecification` carriers in `product-shared-models`
**Carriers (`org.fiware.tmforum.product`, same gating, inject `ObjectMapper`):**
- `ProductOfferingPolicyCarrier`:
  - Walks `getProductOfferingTerm()`. In each term's `getAdditionalProperties()`
    (`List<UnmappedProperty>`), it selects by `getName()` equal to the constants `ACCESS_POLICY`
    (`accessPolicy`) and `CONTRACT_POLICY` (`contractPolicy`).
  - Each value goes through `PolicyValues.carried(...)`, with location
    `productOfferingTerm[i].<name>`.
  - It does not read `ProductOfferingPrice.productOfferingTerm`, which AP-01 lists as not covered.
- `ProductSpecificationPolicyCarrier`:
  - Walks `getProductSpecCharacteristic()` and keeps the characteristics whose `getAtValueType()`
    equals `CharacteristicPolicyValues.AUTHORIZATION_POLICY_VALUE_TYPE`. The domain field is
    `atValueType`, aliased from `valueType`.
  - Delegates `getProductSpecCharacteristicValue()` to `CharacteristicPolicyValues`, with location
    `productSpecCharacteristic[i].productSpecCharacteristicValue[j].value`.

**Build:**
- Add the test dependencies to `product-shared-models/pom.xml`: `junit-jupiter-api`, `-engine` and
  `-params`, the same as `service-shared-models`.
- Add `service-shared-models` (compile scope, like its `product-shared-models` entry) to
  `customer-bill-management/pom.xml`, so the `ProductSpecification` carrier loads there when the
  feature is on.

**Tests:**
- `ProductOfferingPolicyCarrierTest`, parameterized over:
  - the UC-1 term pair: `accessPolicy` resolves at `CATALOG` and `contractPolicy` at
    `CONTRACT_NEGOTIATION`. Take the policies from POLICY_EXAMPLES.md or
    `product-catalog/src/test/resources/access-policy/uc1-product-offering-term.json`;
  - a scope-less term policy;
  - a malformed policy (no uid, a non-object value);
  - a term without policy keys;
  - null terms or additional properties.
- `ProductSpecificationPolicyCarrierTest`, parameterized the same way (well-formed with the UC-6
  policy in a product characteristic, scope-less, malformed, wrong `valueType`, null containers).
- Build the domain objects directly. `UnmappedProperty` comes from ngsi-ld-java-mapping through
  `common`.

**Acceptance:**
- `mvn test -pl product-shared-models -am` is green.
- `mvn clean install -DskipTests -DskipITs` builds the whole reactor, including
  `customer-bill-management` and `all-in-one`.

### Step 4: Document the SPI in the design docs
- `doc/access-policies/ARCHITECTURE.md`:
  - Close open question 2, recording where the carriers live and why (module graph,
    `customer-bill-management`).
  - Update the `access-policy` component listing with `PolicyCarriers`, `PolicyScope`,
    `ResolvedPolicies` and the `UNREADABLE_POLICY` source.
- `doc/access-policies/TICKETS.md`:
  - Mark AP-07 resolved.
  - Note for AP-08: use `PolicyCarriers.isGoverned` before reading, and map `UnreadablePolicy`
    through `decision(failOpen)`.
- `POLICY_EXAMPLES.md` § Scoping: one sentence each on rule narrowing (only the rules at the asked
  scope are sent) and the accepted uid and scope key forms.
- `AGENTS.md`: add the carrier locations to the `access-policy` line.
- No code changes.
