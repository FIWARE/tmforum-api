# Implementation Plan: TMForum - Access Policies - 07 - The decision cache

## Overview
Add a `decisions` cache in front of `PolicyDecisionPoint.decide` in `access-policy`. Each distinct
(policy, scope, action, subject identity, credential fingerprint) pair then costs at most one PDP
`/validate` across requests. Resource facts join the key only for resource-constrained policies. An
entry lives for `min(token exp, decision-ttl)`. The cache runs over the already-wired in-memory
(Caffeine) and Redis backends and is configured in all 72 `application-{in-memory,redis}.yaml` files.

### Design decisions (binding for every step)
- **Programmatic cache, not the `@Cacheable` annotation.** `DecisionCache` takes the
  `decisions` cache from the `CacheManager` and uses its `AsyncCache`. The annotation cannot do three
  things this needs: a per-entry expiry (`min(token exp, decision-ttl)`), not caching PDP failures,
  and coalescing concurrent misses. The cache backends are still the wired ones. As the ticket
  requires, the key is built **only from method parameters**: the `Subject` is passed in and never
  read from `ServerRequestContext`.
- **Key.** It is the SHA-256 hex of a canonical map with fixed names: `uid`, `scope`, `action`,
  `identity`, `fingerprint`, `policy` (= `digest(policy)`) and, only for resource-constrained
  policies, `resource` (= `digest(resourceMap)`). Null entries are dropped before digesting, so the
  digest never depends on how the JSON writer renders `null`, and the fixed names keep an absent
  entry unambiguous. `resourceMap` is built by hand, never by reflection: `type`, `id`, `catalog`,
  `category` (list order kept) and `relatedParty` as a list of `{role, did}` maps. Scope and action are in the key
  so that a `catalog/read` verdict never answers an `order` check. The policy-body digest makes an
  edited policy a new key at once, while reuse of the same policy still deduplicates. The resource
  digest is only included when `ResourceConstraintDetector` flags the policy. A hex key also keeps
  identities out of the Redis key space.
- **Resource-constrained detection (fail-safe, allowlist).** A policy counts as subject-only, and so
  shareable across resources, only when every rule below says so. Anything else counts as
  resource-constrained. A false positive only costs deduplication, while a false negative would
  share one verdict across resources, which is a leak.
  - The walk visits every map and list at any depth: permissions, prohibitions, duties, refinements
    and logical constraints (`and`, `or`, `xone`, `andSequence`). Keys match by local name, so
    `target`, `odrl:target` and `http://www.w3.org/ns/odrl/2/target` all count.
  - Any `target` or any `rightOperandReference` makes the policy resource-constrained.
  - Every `leftOperand` must expand to a **subject-side** IRI. The allowlist comes from
    POLICY_EXAMPLES.md § Vocabulary: any IRI in the `vc` namespace `https://w3id.org/fiware/dsc/vc#`,
    plus `https://w3id.org/dspace/2024/1/membershipType`, `https://w3id.org/dspace/2024/1/scope` and
    `http://www.w3.org/ns/odrl/2/spatial`. Any other operand counts as resource-side: every `tmf:`
    operand, a tmf operand the PDP adds later, and an operand this project has never seen.
  - Expansion. A `leftOperand` is a string or `{"@id": "…"}`. Anything else (a list, a number, a
    `@value` object) cannot be read and counts as resource-side. An absolute IRI (`http://` or
    `https://`) is compared as is. A compact IRI is split at the **first** `:` and its prefix is
    resolved, first through the prefixes that the policy's inline `@context` objects bind (at any
    depth), then through the defaults `vc`, `dspace` and `odrl` with the namespaces above. So
    `cred:country` with `"cred": "https://w3id.org/fiware/dsc/vc#"` is subject-side, and
    `t:catalog` with `"t": "<tmf-ns>"` or a `vc` rebound to the tmf namespace is resource-side. The
    following cannot be resolved and count as resource-side: a prefix bound to two different IRIs
    in the same policy, a prefix that is neither bound nor a default, a bare term without `:`, and
    any compact IRI in a policy whose `@context` holds a remote context other than
    `http://www.w3.org/ns/odrl.jsonld`, because that context could rebind a prefix this code never
    sees. Remote contexts are never fetched.
  - The POLICY_EXAMPLES.md policies on `vc:country` (UC-1), `odrl:spatial` and
    `dspace:membershipType` (UC-2) have no target and only subject-side operands, so they stay
    shareable.
- **Expiry.** The cached value is `CachedDecision(AccessDecision decision, long validUntilEpochMilli)`
  with `validUntil = min(now + decision-ttl, subject.expiresAt)`. `ANONYMOUS` gets `now + ttl`. A hit
  that is past `validUntil` counts as a miss. In YAML, the cache's own `expire-after-write` is
  `${access-policy.decision-ttl:60s}` and only does housekeeping. There is **no**
  `expire-after-access`, because that would let an entry outlive a revocation.
- **UC-9 symmetry.** Policy permits and policy denials are cached under the same TTL.
  `Source.PDP_FAILURE` decisions are never cached: under `fail-open`, a cached failure would be a
  grant that outlives a revocation. A subject whose token has already expired is not cached.
- **Never errors.** A failing cache `get` or `put` (for example Redis down) is logged at `warn` and
  treated as a miss or ignored. `decide` keeps the PDP client's contract: it never errors and never
  completes empty.
- **In-flight coalescing.** A `ConcurrentHashMap<DecisionKey, Mono<AccessDecision>>` holds `.cache()`d
  lookups and removes each in `doFinally`. Without it, a list that decides 100 entities concurrently
  issues more than 6 calls for 6 uids.

## Steps

### Step 1: Canonical digest, resource-constraint detection and the decision key
Pure, bean-free groundwork in `access-policy/src/main/java/org/fiware/tmforum/accesspolicy/`:
- `CanonicalJson` (package-private, final). `static String digest(Map<String, ?> value)`: a SHA-256
  hex of the canonical JSON, with maps sorted recursively and lists kept in order, through
  `JSONObjectUtils.toJSONString`, which only takes a `Map<String, ?>`. Nested values are what JSON
  parsing yields (maps, lists, strings, numbers, booleans); callers never pass records or other
  beans, so nimbus' shaded Gson never reflects over a type. Move `Subject.fingerprint`/`canonicalMap`/`canonicalValue` into it
  and have `Subject` delegate. The fingerprints must stay byte-identical, so the existing
  `SubjectTest` passes unchanged.
- `ResourceConstraintDetector` (package-private, final). `static boolean constrainsResource(Map<String, Object> policy)`
  follows the allowlist rules in the Overview. Name the subject-side IRI set, the default prefix
  map, the trusted remote context and the local names (`target`, `rightOperandReference`,
  `leftOperand`, `@context`, `@id`) as constants. It never throws, and a `null` or non-map policy
  returns `true`.
- `DecisionKey` record (`String value`), with `toString()` returning `value` because Redis
  serialises keys through their string form. Add `static DecisionKey of(String policyUid,
  Map<String, Object> policy, String scope, String action, ValidationRequest.Resource resource,
  Subject subject)` built as described in the Overview: the key parts go into a `LinkedHashMap` with
  the fixed names, and a private `resourceMap(Resource)` / `relatedPartyMap(RelatedParty)` turn the
  records into plain maps before `CanonicalJson.digest`.
- Tests, parameterized. `ResourceConstraintDetectorTest` maps each policy to its expected result.
  - Subject-only (`false`): UC-1 compact, UC-1 expanded, `odrl:spatial`, `dspace:membershipType`,
    `cred:country` with `cred` bound to the vc namespace, and a policy with no constraint at all.
  - Resource-constrained (`true`): the UC-3 refinement policy, a plain `odrl:target` id,
    `tmf:relatedParty` in a permission constraint, an **expanded unknown tmf operand**
    (`https://<tmf-ns>#productSpecification`), an **aliased prefix** (`t:catalog` with
    `"t": "<tmf-ns>"`), `vc` rebound to the tmf namespace, a prefix bound twice, an unbound prefix, a
    bare term, a compact `vc:country` under an unknown remote context, a subject-side operand next
    to a tmf one inside an `and`, a `rightOperandReference`, an unreadable `leftOperand`, and a
    `null` policy. `DecisionKeyTest` checks these cases:
  - Different fingerprint, scope, action, identity, policy body or uid each give a different key.
  - A refreshed token with the same claims gives the same key.
  - A different resource gives the same key for a subject-only policy and a different key for a
    constrained one.
  - `ANONYMOUS` is accepted.
  - A `null` resource, `null` catalog, `null` category or `null` relatedParty is accepted.
  - The record components of `Resource` and `RelatedParty` equal the field sets `resourceMap` and
    `relatedPartyMap` handle, so a new field fails this test and has to be added to the key on purpose.

Acceptance: `mvn test -pl access-policy` is green and `SubjectTest` is unchanged.

### Step 2: `DecisionCache` bean with TTL, failure exclusion and coalescing
- `access-policy/pom.xml`: add `io.micronaut.cache:micronaut-cache-core` (compile) and
  `io.micronaut.cache:micronaut-cache-caffeine` (test), both BOM-managed with no version. Check that
  the `below-common` enforcer still passes.
- `CachedDecision` record (`AccessDecision decision`, `long validUntilEpochMilli`), with
  `boolean isValidAt(Instant now)`. Use plain fields that Jackson can bind; Step 3 relies on this.
- `DecisionCache`: `@Context` (eager, not `@Singleton`) and `@Requires(property =
  AccessPolicyProperties.ENABLED_PROPERTY, value = "true")`, with `public static final String
  CACHE_NAME = "decisions"`.
  - The `@Inject` constructor takes `(AccessPolicyConfigurationGuard, AccessPolicyProperties,
    PolicyDecisionPoint, CacheManager<?>)`. The guard comes first so that an invalid configuration
    fails on the guard's message before the eager bean builds the PDP client.
    A package-private constructor adds a `java.time.Clock` for tests.
  - Resolve `cacheManager.getCache(CACHE_NAME).async()` once at construction. Because the bean is
    `@Context`, a missing `decisions` entry fails `ApplicationContext.run()` with a
    `ConfigurationException` ("No cache configured for name: decisions"), never the first filtered
    request. Caffeine does register a dynamic cache manager (`DefaultDynamicCacheManager`, on unless
    `micronaut.cache.dynamic=false`), which would create the cache silently, so the constructor first
    checks that `cacheManager.getCacheNames()` holds `decisions` and throws otherwise. This matters most for deployments that bring their own
    config (Helm values) instead of the profile YAMLs.
  - The existing enabled-context tests in `access-policy` (`PolicyDecisionPointHttpTest`,
    `SubjectResolutionFilterContextTest`, `HttpJwkSetProviderHttpTest`, and any other test that sets
    `ENABLED_PROPERTY` to `true`) now need a `decisions` cache. Add one shared test constant (for
    example `TestCaches.DECISIONS = Map.of("micronaut.caches.decisions.maximum-size", ...)`) and
    merge it into their properties. Product-catalog's `AccessPolicySubjectResolutionIT` gets the
    cache from the profile YAMLs in Step 4.
  - Public `Mono<AccessDecision> decide(String policyUid, Map<String, Object> policy, String scope,
    String action, ValidationRequest.Resource resource, Subject subject)`. It builds the
    `ValidationRequest` with `ValidationRequest.of(...)` only on a miss, and follows every rule in the
    Overview: expiry, no caching of failures or expired tokens, coalescing, swallowing cache errors,
    and awaiting the `put` before emitting. Give it a terse Javadoc that states the never-errors
    contract and the TTL.
- `AccessPolicyPropertiesTest` line ~114: add `DecisionCache.class` to the `@ValueSource` of beans
  that must not exist while disabled.
- `DecisionCacheTest`: an `ApplicationContext` with access-policy enabled, Caffeine, and a
  `micronaut.caches.decisions` entry, using the real `PolicyDecisionPoint` against
  `StubPolicyDecisionPoint`. Parameterize where it fits. It covers:
  - 100 resources sharing 6 policy uids, decided concurrently (`Flux.flatMap`), give
    `stub.requestCount() == 6`.
  - The same identity with a different `verifiableCredential` set makes a new call and gets its own
    verdict. Flip the stub rule between the calls and check that the old credential still gets the
    cached verdict.
  - A resource-constrained policy makes one call per distinct resource, and a subject-only policy
    makes one call in total.
  - Expiry is driven by the `Clock`:
    - after `decision-ttl` a new call is made;
    - when the token expires before `decision-ttl`, the token expiry bounds the entry;
    - a widen (deny→permit) and a narrow (permit→deny) each take effect at the same bound.
  - A `PDP_FAILURE` is not cached, under both `fail-open` true and false.
  - A cache whose `get`/`put` errors still yields the PDP verdict. Use a mocked `CacheManager` for
    this case only.
  - `decide` runs with no `ServerRequestContext`, which confirms the key comes from parameters.
  - Enabled with no `micronaut.caches.decisions` entry: `ApplicationContext.run()` throws, and the
    cause chain holds a `ConfigurationException` that names `decisions`.

Acceptance: `mvn test -pl access-policy` is green.

### Step 3: Redis value serialization of `CachedDecision`
- Pick the Redis `value-serializer` for `decisions`. First choice:
  `io.micronaut.jackson.serialize.JacksonObjectSerializer` from `micronaut-jackson-databind`, which is
  already a bean in every app, so `access-policy` gains no bean while disabled.
  - First confirm the class exists under that name in the Micronaut 5.2.3 BOM's
    `micronaut-jackson-databind`. Use `mvn dependency:copy` or unzip the jar from `~/.m2`.
  - Also confirm that `RedisCache.get(key, Argument)` hands it the `CachedDecision` type, since the
    `@Cacheable` path is not used.
  - Checked during plan review: `io.micronaut.jackson.serialize.JacksonObjectSerializer` is in
    `micronaut-jackson-databind` 5.2.17 (the core version in the 5.2.3 BOM). Re-check it on the
    build's resolved jar.
  - If the class cannot bind the record, add a fallback:
    `CachedDecisionSerializer implements io.micronaut.core.serialize.ObjectSerializer` in
    `access-policy`, backed by `io.micronaut.json.JsonMapper`. Do **not** gate it with
    `@Requires(access-policy.enabled)`: the YAMLs name it in every redis profile, and a bean that does
    not exist is silently replaced (see the next bullet).
- Never use `JdkSerializer`. Java deserialization of data read from Redis is a gadget risk. Note the
  silent fallback: micronaut-redis 7.2.0 `AbstractRedisCache` resolves `value-serializer` through
  `beanLocator.findOrInstantiateBean(...)` and, if that is empty, falls back to the default cache's
  serializer and then to `new JdkSerializer(...)`. A typo or a missing bean therefore brings back
  `JdkSerializer` with no error. Step 4 and Step 5 check for it.
- Add `CachedDecisionSerializationTest` in `access-policy` (adding `micronaut-jackson-databind` with
  test scope if needed). Run the chosen serializer from a real `ApplicationContext`, so the app's
  `ObjectMapper` modules apply, and round-trip `CachedDecision` with each `Source`, a `null` reason
  and a `null` policyUid. Parameterize it.
- Record the chosen class name as a constant or Javadoc reference on `DecisionCache`, so Step 4
  copies it from one place.

Acceptance: `mvn test -pl access-policy` is green and the serializer class name is settled.

Settled: `DecisionCache.VALUE_SERIALIZER` = `io.micronaut.jackson.serialize.JacksonObjectSerializer` (resolved
jar 5.2.17), no fallback class. It injects the `@Primary @Named("json")` mapper, so the mapping library's
non-primary `CacheSerdeableObjectMapper` is not used. The app's `EscapeCleaningJsonFactory` only rewrites
reserved property names, and `ValidatingDeserializer` passes through types without `@schemaLocation`. Both
`RedisCache.get` (sync and async) hand `Argument<CachedDecision>` to `valueSerializer.deserialize`.

### Step 4: A `decisions` cache entry in all 72 profile YAMLs
- Every file in `git ls-files '*application-in-memory.yaml' '*application-redis.yaml'` gets the
  entry. That is 20 modules under `src/main/resources` (`all-in-one` included) and 16 under
  `src/test/resources`, 72 files in total. Keep each file's indentation and comment style.
  - in-memory, under `micronaut.caches`:
    ```yaml
    # access-policy PDP verdicts (DecisionCache); entries also carry min(token exp, decision-ttl)
    decisions:
      maximumSize: 10000
      expire-after-write: ${access-policy.decision-ttl:60s}
    ```
  - redis, under `redis.caches`: the same `expire-after-write` and the Step 3 `value-serializer`,
    with no `maximumSize`.
- Do not add `expire-after-access`. Do not touch any `postgresql` profile file, since it is a storage
  profile.
- Do not add `decisions` to `ClientTrackingRedisCache`'s tracking prefixes. Nothing keeps a local
  copy, so the reads go to Redis directly.
- Check: `git ls-files '*application-in-memory.yaml' '*application-redis.yaml' | xargs grep -L
  'decisions:'` prints nothing, and `git ls-files '*application-redis.yaml' | xargs grep -L
  '<Step 3 serializer class>'` prints nothing, which catches a typo in any redis file. `mvn clean install -DskipTests -DskipITs` passes. A disabled app
  (for example `mvn test -pl party-catalog`) still starts, because a configured but unused cache is
  harmless.
- `AGENTS.md`: one line in Key Conventions saying every cache profile YAML carries
  `entities`, `subscriptions` and `decisions`, and a new module needs all three.

### Step 5: `AccessPolicyCacheIT` on both cache profiles, and the documented staleness bound
- `product-catalog/pom.xml`: add `org.fiware.tmforum:access-policy` `test-jar` with test scope, for
  `StubPolicyDecisionPoint`.
- `product-catalog/src/test/java/org/fiware/tmforum/productcatalog/AccessPolicyCacheIT.java`:
  `@MicronautTest(packages = "org.fiware.tmforum.productcatalog", contextBuilder =
  FirstMatchConfigContextBuilder.class)`, with access-policy enabled through `TestPropertyProvider`.
  - The provider sets `pdp.url` to the stub, `decision-ttl: 2s`, and `jwt.issuer`/`jwt.jwks-uri` to
    satisfy `AccessPolicyConfigurationGuard`. Copy that setup from `AccessPolicySubjectResolutionIT`.
  - The test injects `DecisionCache` and `CacheManager`. The k3s wiring
    (`-Dmicronaut.environments=${cache},${broker}`) runs it under both cache profiles.
  - Isolation. Under `-Dcache=redis` an entry outlives its test method and its ApplicationContext
    until `decision-ttl` runs out, and a persistent `-Pdev` Redis keeps it across runs. The key has
    no test-specific part, so every test method builds its policy uids and its subject identity with
    a `UUID.randomUUID()` suffix, set in `@BeforeEach`. The uid goes into the policy body as well, so
    it reaches the key twice. Resetting the stub alone is not enough. `DecisionCacheTest` (Step 2)
    does the same, so the Caffeine cases do not depend on test order either.
  - It covers:
    - 100 resources and 6 uids give 6 stub calls.
    - A different credential set gets its own decision.
    - UC-9 with Awaitility (`org.awaitility`, already in the root pom): after a narrow and after a
      widen, the new verdict shows within `decision-ttl` plus a small named slack constant. The two
      wall-clock durations are not compared with each other, because they differ only by poll
      jitter. The equal bound is proven by the `Clock`-driven `DecisionCacheTest` in Step 2.
    - Round-trip: read the `decisions` entry back through `cacheManager.getCache("decisions")` and
      check it equals the stored `CachedDecision`. When the environment is active for `redis`:
      - assert that the native cache is a Redis one, so a silent fallback to Caffeine cannot pass;
      - read the raw bytes of the entry through the native connection (`decisions:<key>`) and
        assert they are the chosen serializer's JSON (they parse as a JSON object and do not start
        with the Java serialization magic `0xACED`). A round trip alone would also pass under
        `JdkSerializer`.
  - The test touches no storage, so it needs no storage tag.
- `doc/access-policies/ARCHITECTURE.md`:
  - § Caching: replace "Micronaut `@Cacheable` over the already-wired in-memory or Redis backend"
    (line ~382) with the programmatic `DecisionCache` over the same backends, and state why the
    annotation was not used: per-entry expiry `min(token exp, decision-ttl)`, no caching of PDP
    failures, coalescing of concurrent misses.
  - § Caching: two or three lines that document the staleness bound (`decision-ttl`, symmetric for
    grant and revocation, failures uncached), the key composition, and the fail-safe allowlist
    resource-constraint rule. That satisfies UC-9's "documented" criterion.
  - The file tree (line ~191): `PolicyDecisionPoint.java` loses `@Cacheable`, and `DecisionCache.java`
    is added with a short description.
  - `git grep -n Cacheable doc/access-policies` must only show the sentence that explains why the
    annotation is not used.
- PR descriptions: the Step 2 PR and the Step 5 PR each state the departure from the ticket's
  `@Cacheable` and the three reasons. They also point to `DecisionCache.decide(...)` taking the
  `Subject` as a parameter, which meets the scope bullet "key built from method parameters".
- Run:
  - `mvn integration-test -pl product-catalog -Dit.test=AccessPolicyCacheIT -DskipTests`
  - the same command with `-Dcache=redis`
  - `mvn test -pl access-policy,common`

Acceptance: the IT is green under both cache profiles, and existing access-policy ITs still pass.
