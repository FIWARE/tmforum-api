# Implementation Plan: TMForum - Access Policies - 05 - Subject resolution from a bearer token

## Overview
Ticket AP-04 (`doc/access-policies/TICKETS.md`). Inside the existing `access-policy` module, resolve a `Subject` for every
request from an optional bearer token. The token is validated with `nimbus-jose-jwt` against the configured JWKS (signature,
issuer, audience, expiry), and the result is published as a request attribute by `SubjectResolutionFilter`. Any invalid
token degrades to an explicit anonymous subject. With `access-policy.enabled` unset, no bean exists. The raw token is never
logged.

Fixed facts every step relies on:
- `access-policy/` sits below `common`. Its `below-common` enforcer rule bans every `org.fiware.tmforum:*` dependency
  except `storage-api`. Add third-party and Micronaut deps only. Micronaut/Reactor artifacts are BOM-managed and carry no
  `<version>`.
- Config already exists in `AccessPolicyProperties.Jwt`: `jwks-uri`, `issuer`, `audience` (unchecked when unset),
  `identity-claim` (default `verifiableCredential.issuer`). Do not rename these keys.
- Every new bean carries `@Requires(property = AccessPolicyProperties.PREFIX + ".enabled", value = "true")`. Use a shared
  constant if one exists, or add `AccessPolicyProperties.ENABLED_PROPERTY`.
- File style: tabs, sparse Javadoc (one line per class/public method, only for what the signature can't show), no
  magic constants, parameterized tests.
- No `block()`. Validating against a cached key set is CPU-only. Only the JWKS fetch is I/O, and it is reactive.

## Steps

### Step 1: Library decision, dependency and mandatory JWT settings
Record the decision and wire the dependency. This step adds no runtime behaviour.
- Root `pom.xml`: add the property `version.com.nimbusds.nimbus-jose-jwt`, using the current 10.x release from Maven
  Central. Add a `dependencyManagement` entry only if the root pom manages other third-party libs there; otherwise put
  the version on the dependency.
- `access-policy/pom.xml`: add these dependencies:
  - `com.nimbusds:nimbus-jose-jwt`
  - `io.micronaut:micronaut-http` (filter API, `ServerRequestContext`)
  - `io.micronaut:micronaut-http-client` (reactive JWKS fetch)
  - `io.projectreactor:reactor-core`
  - test scope: `org.mockito:mockito-junit-jupiter`, `io.projectreactor:reactor-test`, and `ch.qos.logback:logback-classic` (needed by the log-capture test in step 4; check how `common` declares it)

  The `below-common` rule must still pass.
- `doc/access-policies/DECISIONS.md`: append **AP-7 — Validate tokens with `nimbus-jose-jwt`, not `micronaut-security`**
  (Context / Decision / Consequences / Alternatives, same shape as AP-4) and add its row to the index table. Points to
  cover:
  - `micronaut-security` installs a `SecurityFilter` chain and its own beans once on the classpath. That conflicts with
    "off means the beans do not exist" (AP-6), and the dependency would sit in all 21 services.
  - nimbus has no transitive runtime deps.
  - The cost is a hand-written validator (step 3).
  - Only asymmetric algorithms (RSA/EC families) are accepted, never `none` or HMAC.
- `doc/access-policies/ARCHITECTURE.md`: in § Identity › Validation and § Open questions item 1, mark the question as
  resolved by AP-7.
- `AccessPolicyConfigurationGuard`: when `access-policy.enabled` is `true`, fail startup with `ConfigurationException`
  in these cases:
  - `jwt.jwks-uri` is missing, or is not an absolute `http`/`https` URI;
  - `jwt.issuer` is missing.

  Keep the existing message format `key: 'value' reason`.
- Tests: extend `AccessPolicyPropertiesTest` with parameterized cases: enabled with a missing or relative JWKS URI, and
  enabled with a missing issuer, fail; disabled with nothing set passes; enabled with both set passes.
- Acceptance: `mvn test -pl access-policy -am` is green, and `mvn verify -pl access-policy` passes the enforcer.

### Step 2: The `Subject` model
New file `access-policy/src/main/java/org/fiware/tmforum/accesspolicy/Subject.java`. It is pure Java with no Micronaut
dependency.
- Declaration: `public record Subject(String identity, Map<String, Object> claims, Instant expiresAt, String credentialFingerprint)`.
  - `public static final Subject ANONYMOUS` has `null` identity, empty claims, `null` expiry and `null` fingerprint.
  - `boolean isAnonymous()`.
  - The compact constructor copies `claims` deeply into unmodifiable maps and lists.
  - The Javadoc states that `null` identity and expiry occur only on `ANONYMOUS`. "Not resolved" is never a `Subject`;
    it is the absence of the request attribute (step 4).
- `claims` holds every claim of the token except the registered JWT claims (`iss sub aud exp nbf iat jti`, see
  `JWTClaimsSet.getRegisteredNames()`). These are the presented credentials that a policy constrains on.
- `static Subject of(String identity, Map<String, Object> claims, Instant expiresAt)` computes the fingerprint:
  lowercase SHA-256 hex (`HexFormat`) over a canonical JSON of `claims`. Canonical means object keys sorted recursively
  (`TreeMap`), serialized with nimbus `JSONObjectUtils.toJSONString`.
  - The fingerprint never includes the raw token or time-varying claims, so a refreshed token presenting the same
    credentials keeps its fingerprint, while a different credential set changes it.
  - Name the digest algorithm as a constant.
- New class `IdentityClaimResolver`, package-private, static, with a single method
  `Optional<String> resolve(Map<String, Object> claims, String path)`:
  1. If a top-level claim named exactly `path` exists, use it. This handles claim names that contain dots.
  2. Otherwise walk the `.`-separated segments through nested maps. A one-element list is descended. A longer list is
     unresolvable.
  3. The terminal value must be a non-blank string, or a map with a non-blank string `id`. A VC `issuer` may be either.
     Anything else is empty.
- Tests:
  - `SubjectTest`, parameterized: equal claims in a different key order give the same fingerprint. A changed nested
    value, an added credential, or a different credential list gives a different fingerprint. `ANONYMOUS.isAnonymous()`.
    Claims are immutable.
  - `IdentityClaimResolverTest`, parameterized: top-level claim, dotted path, issuer as a string and as `{id}`, dotted
    top-level name, missing segment, multi-element list, non-string terminal, blank value.

### Step 3: JWKS cache and the bearer-token validator
- `JwkSetProvider`: an interface with `Mono<JWKSet> current()` and `Mono<JWKSet> refresh()`. It exists so that the
  validator and filter tests can supply an in-memory key set.
- `HttpJwkSetProvider`: `@Singleton`, gated by `enabled`, implements `JwkSetProvider`.
  - Fetches `jwt.jwks-uri` through an injected Micronaut reactive `HttpClient` (`@Client`, with the request carrying the
    absolute URI) and parses it with `JWKSet.parse`.
  - Holds the set in an `AtomicReference<Mono<JWKSet>>` built with `Mono.cache(ttlForValue, ttlForError, ttlForEmpty)`.
    A good set lives for `JWKS_REFRESH_INTERVAL` (5 min). A failure is cached for `JWKS_MIN_REFRESH_INTERVAL` (30 s) so a
    down endpoint is not hammered.
  - When a refresh fails, the last good set stays in use.
  - `refresh()`, which the validator calls on an unknown `kid`, is rate-limited by `JWKS_MIN_REFRESH_INTERVAL`.
  - Requests time out after `JWKS_FETCH_TIMEOUT`.
  - All of these durations are named constants, not new config keys.
  - Nothing blocks. The fetch is lazy on first use, so startup is not delayed.
- `TokenRejection`: an enum `MALFORMED, UNSUPPORTED_ALGORITHM, UNKNOWN_KEY, BAD_SIGNATURE, EXPIRED, NOT_YET_VALID, WRONG_ISSUER, WRONG_AUDIENCE, NO_IDENTITY, KEYS_UNAVAILABLE`.
  `TokenRejectedException` is a `RuntimeException` that carries a `TokenRejection` and has no stack trace. Its message
  is only the reason, never the token.
- `BearerTokenValidator`: `@Singleton`, gated by `enabled`, takes `AccessPolicyProperties` and `JwkSetProvider`.
  - Package-private `Mono<Subject> validate(String token)` either emits a `Subject` or errors with `TokenRejectedException`.
    It runs these checks in order:
    1. `SignedJWT.parse`, else `MALFORMED`.
    2. The header algorithm must be in the RSA or EC families (`JWSAlgorithm.Family`), else `UNSUPPORTED_ALGORITHM`.
       This rejects `alg: none` and HS*.
    3. Select the key by `kid` (or the single matching key when `kid` is absent) from `current()`. If none matches,
       call `refresh()` once and retry. If there is still no key, `UNKNOWN_KEY`. A provider error gives `KEYS_UNAVAILABLE`.
    4. Verify with `DefaultJWSVerifierFactory`, else `BAD_SIGNATURE`.
    5. Run the claim checks by hand so that each one gets a distinct reason:
       - `exp` is required and must be after now minus `MAX_CLOCK_SKEW` (60 s), else `EXPIRED`;
       - `nbf`, if present, must be before now plus the skew, else `NOT_YET_VALID`;
       - `iss` must equal `jwt.issuer`, else `WRONG_ISSUER`;
       - when `jwt.audience` is set, `aud` must contain it, else `WRONG_AUDIENCE`.
    6. Resolve the identity with `IdentityClaimResolver` and `jwt.identity-claim`. If it is empty, `NO_IDENTITY`.
    7. Return `Subject.of(identity, nonRegisteredClaims, exp)`.
  - Public `Mono<Subject> resolve(String token)` is `validate(token)`, mapping any error to `Subject.ANONYMOUS`. It logs
    at debug level only the `TokenRejection`, or the exception class for an unexpected error. It never logs the token or
    a nimbus exception message.
  - Inject a `Clock`, or a package-private constructor taking one, so the tests are deterministic.
- `BearerTokenValidatorTest` (Mockito/StepVerifier, in-memory `JwkSetProvider`, keys generated with `RSAKeyGenerator`/`ECKeyGenerator`):
  - Parameterized over the defects, each expecting its exact `TokenRejection`: garbage string, two-part string, `alg: none`,
    HS256 signed with the public key bytes, signed by a foreign key with a known kid, unknown kid (refresh is called
    exactly once), expired, future `nbf`, wrong issuer, wrong audience, missing `exp`, no identity claim, and the
    provider erroring.
  - Parameterized valid cases: RSA and EC keys; audience unset and set; the default dotted `verifiableCredential.issuer`
    and a top-level custom claim. Each asserts identity, claims, expiry and fingerprint.
  - A test that `resolve` maps every defect to `Subject.ANONYMOUS`.
  - `HttpJwkSetProvider` test: the cache reuses the set, `refresh()` is rate-limited, and a failed refresh keeps the last
    good set. Stub the fetch through a package-private constructor taking a `Supplier<Mono<String>>`, or use a local
    `com.sun.net.httpserver.HttpServer`.

### Step 4: `SubjectResolutionFilter`
New file `access-policy/.../SubjectResolutionFilter.java`, modelled on `common/.../filter/ForwardedForFilter.java`.
- Annotations and interfaces: `@Filter(Filter.MATCH_ALL_PATTERN)`, `@Requires(property = "access-policy.enabled", value = "true")`,
  `implements HttpServerFilter, Ordered`.
- Constants: `public static final String SUBJECT_ATTR = "access-policy.subject"` and a constant for the `Bearer` scheme.
- `doFilter` handles three cases:
  - No `Authorization` header, a non-Bearer scheme, or an empty token: set `SUBJECT_ATTR` to `Subject.ANONYMOUS` and
    `return chain.proceed(request)` synchronously, with no reactive hop.
  - Otherwise: `Mono.from(validator.resolve(token)).flatMap(s -> Mono.from(chain.proceed(request.setAttribute(SUBJECT_ATTR, s))))`.
  - The token is never stored in an attribute or in `Subject`.
- `public static Optional<Subject> subjectOf(HttpRequest<?> request)` and `public static Optional<Subject> current()`
  (via `ServerRequestContext.currentRequest()`). An empty result means "not resolved", which is distinct from
  `ANONYMOUS`. The AP-08 decorator will read the subject through `current()`.
- `getOrder()` returns `ServerFilterPhase.SECURITY.order()`. Add a comment in the style of `PaginationFilter`: the filter
  must run after `ForwardedForFilter` (`HIGHEST_PRECEDENCE`) and before anything reading the subject, including
  `PaginationFilter` (`LOWEST_PRECEDENCE`), and the security phase is where Micronaut places authentication. Assert the
  ordering in a test.
- `SubjectResolutionFilterTest`:
  - A mock `ServerFilterChain` captures the proceeded request. Use a real `BearerTokenValidator` over an in-memory
    `JwkSetProvider`.
  - Parameterized over token defects at the header level: no header, `Basic …`, `Bearer` with an empty token,
    lowercase `bearer` with a valid token (which must resolve), malformed, wrong signature, expired, wrong issuer,
    wrong audience, unknown kid.
  - Each case asserts that the chain proceeded exactly once with the expected subject (anonymous or the identity), and
    that no error propagates.
  - A valid token yields the configured identity claim and every credential claim.
  - Order: `ForwardedForFilter` cannot be referenced from here, so assert `HIGHEST_PRECEDENCE < getOrder() < LOWEST_PRECEDENCE`.
  - No raw token in the logs: attach a logback `ListAppender` at TRACE to the `org.fiware.tmforum.accesspolicy` logger,
    run every defect case, and assert that no formatted message, argument or throwable message contains the token or
    its signature segment.
- `SubjectResolutionFilterContextTest`, parameterized:
  - `ApplicationContext.run(props)` with `access-policy.enabled` unset, or `false`, asserts that none of
    `SubjectResolutionFilter`, `BearerTokenValidator` and `HttpJwkSetProvider` exists.
  - `enabled=true` with `jwt.jwks-uri` and `jwt.issuer` set asserts that all of them exist. No JWKS fetch happens at
    startup.

### Step 5: `AccessPolicySubjectResolutionIT` (UC-5, token dimension)
Host module: `product-catalog`. It reaches `access-policy` through `common` and already has test controllers.
- `product-catalog/src/test/java/org/fiware/tmforum/productcatalog/SubjectEchoController.java`:
  - Annotated `@Controller("/test-subject")` and `@Requires(property = "test.subject-echo.enabled", value = "true")`,
    the same pattern as `RecordingHubController`.
  - `GET` returns `{resolved, anonymous, identity, fingerprint}`.
  - It reads the subject both through the request attribute and inside a `Mono` through `SubjectResolutionFilter.current()`.
    This proves that the propagated request context survives the filter's async hop on a cold JWKS cache. If it does
    not, fix that in the filter, not in the test.
- `AccessPolicySubjectResolutionIT extends AbstractApiIT`:
  - Follow the `@MicronautTest(packages = ..., contextBuilder = FirstMatchConfigContextBuilder.class)` style of the
    existing ITs, with `@Property` for `access-policy.enabled=true` and `test.subject-echo.enabled=true`.
  - `@TestInstance(PER_CLASS)` with `TestPropertyProvider`: start a JDK `com.sun.net.httpserver.HttpServer` on an
    ephemeral port that serves a test-generated RSA JWKS. Supply `access-policy.jwt.jwks-uri`, `issuer` and `audience`
    from it. This needs no k3s manifest change. Stop the server in `@AfterAll`.
  - Parameterized cases: token A, token B, no token, expired token, wrongly signed token.
    - Against `/test-subject`: A and B resolve to their own identities, with different fingerprints. No token and the
      defect cases resolve to anonymous.
    - Against `GET /productCatalogManagement/v4/productOffering`: `200` for every case. A bad token must never give `500`.
  - Assert that the JWKS server was hit once across the whole run, which proves the cache.
  - Leave out the `bypass-unauthenticated` and `mode` dimensions. Add a class Javadoc line saying they land with AP-09.
  - No storage-tag gating is needed; the IT is backend-neutral.
- Docs:
  - `AGENTS.md`: add the nimbus line to Tech Stack, and add `SubjectResolutionFilter`/`SUBJECT_ATTR` to Important Files.
  - `doc/access-policies/ARCHITECTURE.md` § Components: change the `Subject` record sketch to the implemented shape.
- Acceptance: run `mvn integration-test -pl product-catalog -Dit.test=AccessPolicySubjectResolutionIT -DskipTests`
  and confirm it is green on orion-ld. The other ITs of the module are unchanged, since the feature is off in their
  context.
