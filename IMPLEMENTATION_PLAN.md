# Implementation Plan: TMForum - Access Policies - 06 - The Policy Decision Point client

## Overview
This adds AP-05 from `doc/access-policies/TICKETS.md` to the existing `access-policy` module. It covers three things:
- the PDP wire records (`ValidationRequest`, `AccessDecision`);
- a reactive `@Client(id = "pdp")`-backed `PolicyDecisionPoint`, which turns a timeout, a non-200 answer or an unparsable body into a denial tagged as a failure;
- `StubPolicyDecisionPoint`, a reusable HTTP stub that counts requests and is exported through the module's test-jar for every later IT.

There is no caching (AP-06) and no caller in this ticket.

All new code goes in `access-policy/src/main/java/org/fiware/tmforum/accesspolicy/`. Tests go in `access-policy/src/test/java/org/fiware/tmforum/accesspolicy/` and fixtures in `access-policy/src/test/resources/pdp/`. The module may depend only on `storage-api` (enforcer `below-common`). Every bean is gated by `@Requires(property = AccessPolicyProperties.ENABLED_PROPERTY, value = "true")`. No `block()` anywhere. The module's files use tabs and sparse Javadoc, so match that.

Decisions this plan settles (record them in the PR descriptions):
1. **Endpoint configuration.** The client is `@Client(id = "pdp") HttpClient`, so `micronaut.http.services.pdp.*` (url, read-timeout, ssl, pool) applies the way it does for `ngsi`. AP-03's `access-policy.pdp.url` stays as the short form:
   - When it is set, the request URI is the absolute `url + path`. Micronaut uses an absolute URI as is and never consults the service's load balancer.
   - Otherwise the URI is the relative `access-policy.pdp.path`, resolved against `micronaut.http.services.pdp.url`.
   - While the feature is enabled, exactly one of the two must be set. The guard rejects neither and both at startup.
2. **Hard bound.** `access-policy.pdp.timeout` is always applied as a Reactor `.timeout(...)` on the call, whatever the HTTP client's own timeouts are. That makes "a tagged failure within `pdp.timeout`" true by construction.
3. **The failure tag** is a field of `AccessDecision`, not just a reason text: `enum Source { POLICY, PDP_FAILURE }`. Later tickets (AP-09 bypass, AP-18 audit) may add constants. `fail-open` flips `permitted` and keeps `Source.PDP_FAILURE`.
4. **`subject.claims.vc`** is the list of credentials presented in the token's `verifiableCredential` claim, used as is:
   - a list is kept;
   - any other value (an object, or a string such as a JWT-encoded credential) becomes a singleton list;
   - a missing claim gives an empty list.

   The client never interprets a credential (AP-1).
5. **The stub is an HTTP server, not an alternative bean.** Later ITs then exercise the real client and its configuration. It is built on the JDK's `com.sun.net.httpserver.HttpServer`, as `HttpJwkSetProviderHttpTest` already does, and is not a Micronaut bean.

## Steps

### Step 1: PDP wire records and the JSON fixture test
Add the request and response model, with no client yet.

- `ValidationRequest.java` is a record tree that mirrors `POLICY_EXAMPLES.md` § What the PDP receives field for field:
  - `ValidationRequest(Map<String, Object> policy, JsonInput jsonInput)`
  - `JsonInput(Payload payload, RequestSubject subject)`
  - `Payload(String scope, String protocol, String action, Resource resource)`
  - `Resource(String type, String id, String catalog, List<String> category, List<RelatedParty> relatedParty)`
  - `RelatedParty(String role, String did)`
  - `RequestSubject(String identity, Map<String, Object> claims)`

  Nested records may live inside `ValidationRequest`. `policy` is opaque: it is passed through exactly as the resolver (AP-07) hands it over.
- Add `public static final String PROTOCOL = "tmf"` and a factory `ValidationRequest.of(Map<String, Object> policy, String scope, String action, Resource resource, Subject subject)`. It fixes `protocol` and builds `RequestSubject` from `Subject`:
  - `identity` comes from `Subject.identity()`;
  - `claims` is `{"vc": [...]}` (decision 4), with named constants `VERIFIABLE_CREDENTIAL_CLAIM = "verifiableCredential"` and `VC_CLAIMS_KEY = "vc"`.

  `Subject.ANONYMOUS` gives no identity and an empty `vc` list.
- Absent values (`catalog`, `category`, `relatedParty`, an anonymous `identity`) are omitted, not written as `null`. Empty lists are written as `[]`, so an anonymous subject sends `"claims": {"vc": []}`. Put `@JsonInclude(JsonInclude.Include.NON_NULL)` from `com.fasterxml.jackson.annotation` on every record of the `ValidationRequest` tree, not only on those with optional fields. A class-level annotation overrides the mapper's global inclusion, so the wire shape does not depend on the deploying app's `jackson.serialization-inclusion`. Add `com.fasterxml.jackson.core:jackson-annotations` (BOM-managed, no version) at compile scope to `access-policy/pom.xml` if it is not already on the compile classpath. No `BeanCreatedEventListener`, and no custom `JacksonModule` unless one turns out to be strictly needed.
- `AccessDecision.java` is `record AccessDecision(boolean permitted, String reason, String policyUid, Source source)` with nested `enum Source { POLICY, PDP_FAILURE }`. It gets a one-line Javadoc stating that `source` tells a policy denial from a failed PDP call, and that `fail-open` flips `permitted` but never `source`. Give it static factories `policy(...)` and `failure(uid, reason, failOpen)`. It must round-trip through Jackson (AP-06 caches it in Redis), so it gets no constructor logic beyond null checks.
- Fixtures go in `access-policy/src/test/resources/pdp/`:
  - `validation-request-full.json`: the document's example with a concrete policy and one credential;
  - `validation-request-anonymous.json`: no catalog, category or relatedParty, and an anonymous subject;
  - `validation-request-empty-lists.json`: empty `category` and `relatedParty` lists, which must appear as `[]`;
  - `validation-request-jwt-credential.json`: a `verifiableCredential` claim that is a plain JWT string, which must appear as a one-element `vc` list holding that string unchanged.
- `ValidationRequestSerializationTest` is a `@ParameterizedTest` over (fixture, request built through `ValidationRequest.of`). It serialises with the `io.micronaut.json.JsonMapper` bean of an `ApplicationContext.run(Map.of("jackson.serialization-inclusion", "non_null"))`, the setting every deployed module uses (Micronaut's default is `NON_EMPTY`), so the test sees the mapper the deployed HTTP client uses. Keep that property in a named constant. It compares JSON trees parsed from both sides, so key order does not matter, against the committed fixture and never against the builder. One more case asserts that `protocol` is `"tmf"`.
- `AccessDecisionTest` is parameterized over `(failOpen, expectedPermitted)` for `failure(...)` and asserts that `source` stays `PDP_FAILURE`. It also round-trips an `AccessDecision` through the same configured `JsonMapper`.

Acceptance: `mvn test -pl access-policy` is green, and the enforcer still passes (`mvn verify -pl access-policy -DskipTests`).

### Step 2: The `PolicyDecisionPoint` client with tagged-failure handling
- `PolicyDecisionPoint.java` is `@Slf4j @Singleton @Requires(property = AccessPolicyProperties.ENABLED_PROPERTY, value = "true")`.
  - Public `@Inject` constructor `(AccessPolicyProperties properties, @Client(id = "pdp") HttpClient httpClient, JsonMapper jsonMapper)`.
  - Public method `Mono<AccessDecision> decide(String policyUid, ValidationRequest request)`. Its Javadoc says it never errors: every failure comes back as a `PDP_FAILURE` decision.
  - Flow:
    1. `POST` the request as `application/json` to the URI from decision 1, through `httpClient.exchange(request, String.class)`.
    2. Status exactly `200` → parse the body with the `JsonMapper` into a package-private wire record `PdpResponse(Boolean permitted, String reason)`. A missing or non-boolean `permitted` counts as unparsable, so `{}` never reads as a policy denial. Then map to `AccessDecision.policy(...)`.
    3. Any other status (including other 2xx), an `HttpClientResponseException`, a connect error, a `TimeoutException` from `.timeout(properties.getPdp().getTimeout())`, or a parse error → `onErrorResume` gives `AccessDecision.failure(policyUid, <reason>, properties.isFailOpen())`.
  - Failure reasons are short named-constant formats, e.g. `"PDP timed out after %s"`, `"PDP answered %d"`, `"PDP answer unparsable"`, `"PDP unreachable: %s"` (exception class only). Never the body, the subject or claims.
  - Log each failure once at `warn`, with policy uid and reason only.
- `AccessPolicyConfigurationGuard`: while enabled, require exactly one PDP endpoint (decision 1):
  - `access-policy.pdp.url` (must be an absolute http(s) URI; reuse `isAbsoluteHttpUri`), or
  - `micronaut.http.services.pdp.url` / `micronaut.http.services.pdp.urls`.

  Neither or both is a `ConfigurationException` naming both keys. Update every existing test that runs a context with `access-policy.enabled=true` so it also sets `access-policy.pdp.url`: `AccessPolicyPropertiesTest`, `SubjectResolutionFilterContextTest`, `HttpJwkSetProviderHttpTest`. Add guard cases (neither / both / relative url / only `micronaut.http.services.pdp.url`) to the existing parameterized guard tests in `AccessPolicyPropertiesTest`.
- `PolicyDecisionPointTest` uses a Mockito-mocked `HttpClient` (`exchange(any(HttpRequest.class), eq(String.class))`) and a real `JsonMapper`.
  - A `@ParameterizedTest` over the failure modes × `failOpen ∈ {false, true}`:
    - timeout (`Mono.never()` with a short configured `pdp.timeout`, verified with `StepVerifier.withVirtualTime`);
    - connect error;
    - `HttpClientResponseException` 500 and 404;
    - 204 with no body;
    - invalid JSON;
    - `{}`;
    - `{"permitted":"yes"}`.

    Each case asserts `source == PDP_FAILURE`, `permitted == failOpen`, the `policyUid`, and that the Mono completes without error.
  - A second parameterized test covers policy answers: `permitted` true or false, with the reason propagated and `source == POLICY`.
  - One test captures the sent `HttpRequest` and asserts method, content type and URI for both endpoint configurations (absolute `access-policy.pdp.url + path`, and relative `path`).
- Add `PolicyDecisionPoint` to the existing "disabled context has no feature bean" assertion in `AccessPolicyPropertiesTest` (beside `AccessPolicyConfigurationGuard`), and assert it exists once enabled.
- Docs: in `doc/access-policies/ARCHITECTURE.md` § Components, extend the `AccessDecision` line with `Source source`. In § Configuration, add one line noting that `micronaut.http.services.pdp.*` is the alternative to `pdp.url`. Nothing more.

Acceptance: `mvn test -pl access-policy` is green, and `grep -rn "block()" access-policy/src/main` is empty.

### Step 3: `StubPolicyDecisionPoint`, test-jar export and the real-HTTP tests
- `access-policy/src/test/java/.../StubPolicyDecisionPoint.java` is a plain `AutoCloseable` class, not a bean, wrapping a JDK `HttpServer` on the loopback address and an ephemeral port.
  - `static StubPolicyDecisionPoint start()`
  - `String url()`: base URL, for `access-policy.pdp.url`
  - `permit(String policyUid, String identity)` and `deny(String policyUid, String identity, String reason)`: rules keyed by `(policy uid, subject identity)`, with `identity == null` matching the anonymous subject
  - `defaultAnswer(boolean permitted)`: unmatched requests deny by default with a named reason constant
  - `failWith(int status)` and `delay(Duration)`: to simulate failures
  - `int requestCount()`: an `AtomicInteger`; UC-8 asserts `0`
  - `List<Map<String, Object>> requests()`: the received bodies, for assertions on scope, action and resource
  - `reset()`
  - `close()`

  It serves `POST` on `AccessPolicyProperties.DEFAULT_PDP_PATH` and answers `200 {"permitted":…,"reason":…}`. It parses and writes JSON with nimbus `JSONObjectUtils`, which is already a compile dependency, so consumers of the test-jar need nothing extra. The policy uid is read from the request's `policy` under the first present key among named constants `http://www.w3.org/ns/odrl/2/uid` (expanded JSON-LD), `odrl:uid`, `uid`, `@id`. The value is unwrapped to a string: a string is used as is, an object yields its `@id`, and an array yields its first element, unwrapped the same way. So `[{"@id": "urn:p"}]`, `{"@id": "urn:p"}` and `"urn:p"` all give `urn:p`. A request with no uid that unwraps to a string is answered with a deny under its own named reason (e.g. `NO_POLICY_UID_REASON`), not the default answer, so a mis-shaped policy shows up in the failing assertion. Thread-safe: concurrent maps and atomics.
- `access-policy/pom.xml`: add a `maven-jar-plugin` `test-jar` execution, as in `storage-api/pom.xml` (execution id e.g. `pdp-stub`). Restrict it with `<includes>` to `org/fiware/tmforum/accesspolicy/StubPolicyDecisionPoint*.class`. Otherwise the test bean definitions (`GateProbes` and its `META-INF/micronaut` references), fixtures and logback config would leak into every consumer. No consumer is added in this ticket.
- `PolicyDecisionPointHttpTest` uses the real `PolicyDecisionPoint` from an `ApplicationContext.run(...)` with the feature enabled (JWT keys as in `HttpJwkSetProviderHttpTest`) against the stub. Parameterized where the cases differ only in data:
  - permit and deny by `(uid, identity)` → `Source.POLICY`, with `requestCount()` incremented once per call;
  - the stub's `failWith(503)` → `PDP_FAILURE`;
  - `delay` beyond a short `pdp.timeout` → `PDP_FAILURE`, finishing within `pdp.timeout` plus a small named slack (`StepVerifier...verify(Duration)`);
  - an unreachable PDP: a closed loopback port (bind and close a socket to obtain a free port) → `PDP_FAILURE` within the same bound, never hanging;
  - both endpoint configurations from decision 1 (`access-policy.pdp.url` alone, and `micronaut.http.services.pdp.url` alone) reach the stub;
  - the body the stub received equals the request fixture from step 1, which shows the serialisation on the wire, not only in the mapper.
- `StubPolicyDecisionPointTest`: a short self-test of the rule lookup (exact match, the anonymous rule, the default answer), `reset()` and counting. A `@ParameterizedTest` covers the uid shapes: expanded IRI key with `[{"@id": …}]`, `odrl:uid` as `{"@id": …}`, `odrl:uid` as a string, plain `uid`, top-level `@id`, and a policy without a uid (→ `NO_POLICY_UID_REASON`). It talks to the stub through the JDK `java.net.http.HttpClient` so it does not depend on the client under test.
- `mvn install -pl access-policy -DskipITs` produces `access-policy-0.1-tests.jar` containing only the stub classes. Check with `jar tf`.

Acceptance: `mvn verify -pl access-policy -DskipITs` is green, and `mvn install -pl common -am -DskipTests -DskipITs` still builds.
