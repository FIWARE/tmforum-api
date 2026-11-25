# Implementation tickets

The work needed to build what [ARCHITECTURE.md](./ARCHITECTURE.md) proposes, cut into tickets that can
each be built, reviewed and merged on their own. Ticket numbers are stable; phases are the ones in
[ARCHITECTURE.md § Rollout](./ARCHITECTURE.md#rollout).

> :construction: **Status: proposal.** Nothing here is implemented. The tickets were written against
> `9d309bf2` and **re-validated against `origin/ticket-88/work` at `9b4a6b00`**, which is the base the
> feature will actually be built on. Six of them exist because the design as written does not compile
> against it — see [Blocking design corrections](#blocking-design-corrections).
>
> That branch carries tickets 89, 90, 93, 85 and 91 on top of `main`: a second storage adapter
> ([`storage-postgresql`](../../storage-postgresql/README.md), the `postgresql` Micronaut environment),
> a backend-neutral query model (`org.fiware.tmforum.storage.query.EntityQuery`, which replaced the
> `query`/`orderBy` strings on the port), entity versioning, a broker-only notification path behind
> `EntityEventPublisher`, and a broker-to-PostgreSQL migration. Every one of those touches this
> feature; the places where it does are called out in the tickets. The `access-policy` branch carrying
> this folder is rebased onto `9b4a6b00`, so every path linked here resolves against the base the
> feature will be built on.

- [How to read a ticket](#how-to-read-a-ticket)
- [Blocking design corrections](#blocking-design-corrections)
- [The tickets](#the-tickets)
- [Phase 0 — prove the premise](#phase-0--prove-the-premise)
- [Phase 1 — the module, the subject, the PDP](#phase-1--the-module-the-subject-the-pdp)
- [Phase 2 — enforce on `get`](#phase-2--enforce-on-get)
- [Phase 3 — enforce on lists](#phase-3--enforce-on-lists)
- [Phase 4 — ordering and quoting](#phase-4--ordering-and-quoting)
- [Phase 5 — audit and cost control](#phase-5--audit-and-cost-control)
- [Phase 6 — the target state](#phase-6--the-target-state)
- [Documentation](#documentation)
- [Outside this project](#outside-this-project)
- [Sequencing notes](#sequencing-notes)

## How to read a ticket

Each ticket states its **goal**, what is **in scope**, the **files** it touches, its **acceptance
criteria** and the **tests** that prove it. "Done" means the acceptance criteria hold and the named
tests exist and pass; a ticket that cannot meet a criterion records why rather than dropping it.

Sizes are rough: **S** a day or less, **M** a few days, **L** a week or more. Branch names follow
[the global workflow](../../AGENTS.md): one topic branch `topic/access-policies`, one step branch and
one PR per ticket.

"All three storages" below means the three a ticket has to be run against on this base:
`-Dbroker=orion-ld`, `-Dbroker=scorpio` and `-Dbroker=postgresql`. ITs are gated per storage with the
JUnit tags in
[`StorageTags`](../../common/src/main/java/org/fiware/tmforum/common/test/StorageTags.java) and the
`@BrokerOnly`/`@PostgresqlOnly` annotations beside it; failsafe excludes `not-on-${broker}`. A case
that cannot hold on one storage carries the tag rather than being left out.

## Blocking design corrections

Six things in the architecture do not hold against the code as it stands: the four found against
`9d309bf2`, all still valid, plus two that `ticket-88/work` introduced. Four are cheap to fix, one
(D6) is a hole in the feature rather than in the build-out, and one (D4) may invalidate the premise.
None of them should be discovered during Phase 2.

| # | What the design says | What the code says | Ticket |
|---|---|---|---|
| **D1** | `PolicyEnforcingEntityRepository` is `@Primary` and injects `NgsiLdEntityRepository` by concrete type | Three ways wrong on `ticket-88/work`. `access-policy` cannot see that class, and **both ArchUnit rules forbid it anyway**: [`StorageBoundaryTest`](../../common/src/test/java/org/fiware/tmforum/common/architecture/StorageBoundaryTest.java) and [`ControllerStorageBoundaryTest`](../../all-in-one/src/test/java/org/fiware/tmforum/architecture/ControllerStorageBoundaryTest.java). There are now **two** adapters, so naming one leaves the other undecorated. And **`@Primary` is already taken**: `PostgresEntityRepository` and `PostgresReferencesRepository` are both `@Singleton @Primary @Requires(env = postgresql)`, which is how an adapter wins over the NGSI-LD one — a third `@Primary` bean makes the injection ambiguous | [AP-02](#ap-02--a-qualifier-for-the-storage-adapter-so-a-decorator-is-possible-at-all) |
| **D2** | "One seam covers everything … and reads from `ReferenceValidationService` — all of them" ([AP-3](./DECISIONS.md#ap-3--enforce-at-the-storage-port)) | `ReferenceValidationService` reads through [`ReferenceReadRepository`](../../storage-api/src/main/java/org/fiware/tmforum/storage/ReferenceReadRepository.java), a **different port**. A decorator on `EntityRepository` does not see it, which leaves an existence oracle open against [C7](./ANALYSIS.md#constraints-that-shape-any-solution). On `ticket-88/work` the port grew a bulk `findReferences` and `StoredReference.version`, so the oracle now leaks the current version label of a hidden entity too — every governed type is in `VersionedEntity.VERSIONED_TYPES` | [AP-17](#ap-17--close-the-reference-port-existence-oracle) |
| **D3** | A denied order answers `403` | [`TmForumExceptionReason`](../../common/src/main/java/org/fiware/tmforum/common/exception/TmForumExceptionReason.java) has no `FORBIDDEN`, and [`TmForumExceptionHandler`](../../common/src/main/java/org/fiware/tmforum/common/exception/TmForumExceptionHandler.java)'s `default ->` renders `500` | [AP-15](#ap-15--a-forbidden-exception-reason) |
| **D4** | "The policy arrives with the entity … `productOfferingTerm.accessPolicy` is deserialised with the offering" | [`ProductOfferingTerm`](../../product-shared-models/src/main/java/org/fiware/tmforum/product/ProductOfferingTerm.java) has four fields and none of them is a policy. The pair survives in `Entity.additionalProperties` as `UnmappedProperty`, through a nested `PROPERTY_LIST` mapping. **Resolved by AP-01: preserved** on all three storages and through the Redis cache, provided the term carries a `@schemaLocation` (without one the POST is a `400`). Both specification carriers are preserved too ([finding](./AP-01_FINDINGS.md)) | [AP-01](#ap-01--prove-the-policy-survives-the-entity-round-trip) |
| **D5** | "Writes pass straight through" | New on `ticket-88/work`: `ControllerStorageBoundaryTest` now fails any class outside the two adapter packages that calls `createDomainEntity`/`updateDomainEntity`/`deleteDomainEntity` without implementing `EntityEventPublisher`. A decorator of `EntityRepository` has to implement those three methods, and delegating them **violates that rule** | [AP-08](#ap-08--the-decorator-get-only) |
| **D6** | A denied read answers `404`, and the architecture's action model covers `delete` | [`AbstractApiController#delete`](../../common/src/main/java/org/fiware/tmforum/common/rest/AbstractApiController.java) goes straight to `eventPublisher.deleteWithEvent`, which reads nothing first on either backend (`DirectEntityEventPublisher.deleteWithEvent` is `repository.deleteDomainEntity`; the PostgreSQL one delegates straight to its own repository). A read-side decorator therefore lets anyone who learns an id delete a restricted entity. `patch` does read first, so it is covered | [AP-23](#ap-23--deny-writes-to-entities-the-subject-cannot-read) |

D4 is still the one that matters. Every later ticket assumes the policy is readable from the domain object
the storage port returns. [AP-01](#ap-01--prove-the-policy-survives-the-entity-round-trip) is
therefore a gate, not a warm-up: if the pair is dropped on write or on read, the feature needs either
a typed field on `ProductOfferingTerm` or a second read, and
[AP-2](./DECISIONS.md#ap-2--read-the-policies-the-entities-already-carry) has to be revisited before
anything else is built.

**AP-01 result:** the gate is passed. All three carriers are preserved on orion-ld, scorpio and
postgresql, at the API and on the domain object the port returns, cached or not. AP-2 stands
unchanged and AP-07 is not blocked. The one authoring rule is that the offering term needs a
`@schemaLocation`, which the UC-1 example now carries. See [AP-01_FINDINGS.md](./AP-01_FINDINGS.md).

## The tickets

| ID | Title | Phase | Depends on | Size |
|---|---|---|---|---|
| [AP-01](#ap-01--prove-the-policy-survives-the-entity-round-trip) | Prove the policy survives the entity round-trip | 0 | — | S |
| [AP-02](#ap-02--a-qualifier-for-the-storage-adapter-so-a-decorator-is-possible-at-all) | A qualifier for the storage adapter, so a decorator is possible at all | 0 | — | S |
| [AP-03](#ap-03--the-access-policy-module-and-its-configuration) | The `access-policy` module and its configuration | 1 | AP-02 | M |
| [AP-04](#ap-04--subject-resolution-from-a-bearer-token) | Subject resolution from a bearer token | 1 | AP-03 | M |
| [AP-05](#ap-05--the-policy-decision-point-client) | The Policy Decision Point client | 1 | AP-03 | M |
| [AP-06](#ap-06--the-decision-cache) | The decision cache | 1 | AP-05 | M |
| [AP-07](#ap-07--the-policycarrier-spi-and-direct-policy-resolution) | The `PolicyCarrier` SPI and direct policy resolution | 2 | AP-01, AP-03 | M |
| [AP-08](#ap-08--the-decorator-get-only) | The decorator, `get` only | 2 | AP-04, AP-05, AP-07 | M |
| [AP-09](#ap-09--bypass-owner-exemption-and-observe-mode) | Bypass, owner exemption and `observe` mode | 2 | AP-08 | M |
| [AP-23](#ap-23--deny-writes-to-entities-the-subject-cannot-read) | Deny writes to entities the subject cannot read | 2 | AP-08 | M |
| [AP-10](#ap-10--the-decorator-is-still-a-storage-adapter) | The decorator is still a storage adapter | 2 | AP-08, AP-23 | S |
| [AP-11](#ap-11--composition-walk-and-policy-union) | Composition walk and policy union | 3 | AP-07 | L |
| [AP-12](#ap-12--page-refill-for-findentities) | Page refill for `findEntities` | 3 | AP-08 | L |
| [AP-13](#ap-13--total-count-modes) | `total-count` modes | 3 | AP-12 | M |
| [AP-14](#ap-14--catalog-and-category-policies) | Catalog and category policies | 3 | AP-12 | L |
| [AP-15](#ap-15--a-forbidden-exception-reason) | A `FORBIDDEN` exception reason | 4 | — | S |
| [AP-16](#ap-16--the-contract-policy-check-on-order-and-quote-creation) | The contract-policy check on order and quote creation | 4 | AP-11, AP-15 | M |
| [AP-17](#ap-17--close-the-reference-port-existence-oracle) | Close the reference-port existence oracle | 4 | AP-08 | M |
| [AP-18](#ap-18--the-access-audit-log) | The access audit log | 5 | AP-09 | M |
| [AP-19](#ap-19--the-cursor-memo) | The cursor memo | 5 | AP-12 | M |
| [AP-20](#ap-20--predicate-push-down) | Predicate push-down | 6 | AP-12 | L |
| [AP-21](#ap-21--bypass-by-identity) | Bypass by identity | 6 | AP-09, AP-18 | M |
| [AP-22](#ap-22--documentation-and-release-notes) | Documentation and release notes | — | all | S |

---

## Phase 0 — prove the premise

### AP-01 — Prove the policy survives the entity round-trip

**Goal.** Establish, with a test and not an argument, that a `ProductOffering` written with an
`accessPolicy`/`contractPolicy` pair in `productOfferingTerm` reads back with that pair intact and
reachable from the domain object, and the same for an `authorizationPolicy` characteristic on a
`ProductSpecification` and a `ServiceSpecification`. This is [D4](#blocking-design-corrections) and it
gates every other ticket.

**In scope.**
* An IT that POSTs the offering from
  [POLICY_EXAMPLES.md](./POLICY_EXAMPLES.md#an-entity-restricted-by-a-subject-attribute) verbatim,
  GETs it back, and asserts the pair is byte-equivalent in the response body.
* A repository-level test that asserts the same pair is reachable from the
  `ProductOffering` the storage port returns, and records *where*: `ProductOfferingTerm` extends
  [`Entity`](../../common/src/main/java/org/fiware/tmforum/common/domain/Entity.java), so the
  candidate is `additionalProperties` (`List<UnmappedProperty>`, via
  `@UnmappedPropertiesGetter`) on the nested `PROPERTY_LIST` object.
* The same for the specification path, where the carrier is a typed `Object`:
  `ProductSpecificationCharacteristic.productSpecCharacteristicValue[].tmfValue`, and for the service
  path, `specCharacteristic[].characteristicValueSpecification[]` — the container names differ and
  [POLICY_EXAMPLES.md](./POLICY_EXAMPLES.md#a-visibility-policy-on-a-service-specification) says so.
* All three storages. NGSI-LD attribute names and the escaping in
  `general.escapeForbiddenCharacters` both touch JSON-LD payloads carrying `@context`, `@type` and
  URLs; a policy is exactly the kind of value that gets mangled.
* A written finding, in this ticket's PR, stating for each of the three carriers: preserved / lossy /
  dropped, and if lossy, what is lost.

**Out of scope.** Reading the policy for a decision. Nothing is enforced here.

**Acceptance criteria.**
* For each of the three carriers, a test asserts the outcome that was actually observed — a carrier
  that turns out lossy gets a test pinning the lossy behaviour, not a skipped test.
* The finding names the exact accessor path a `PolicyCarrier` implementation will use.
* If any carrier drops the policy, the ticket's PR proposes the correction to
  [AP-2](./DECISIONS.md#ap-2--read-the-policies-the-entities-already-carry) and the follow-up ticket,
  and [AP-07](#ap-07--the-policycarrier-spi-and-direct-policy-resolution) is blocked until that is
  agreed.

**Tests.** `ProductOfferingPolicyRoundTripIT` in `product-catalog`,
`ServiceSpecificationPolicyRoundTripIT` in `service-catalog`, parameterized over the carriers.

**Risk.** This is the ticket most likely to change the design. Schedule it first and alone.

---

### AP-02 — A qualifier for the storage adapter, so a decorator is possible at all

**Goal.** Make it possible for any module to decorate the storage port without naming a concrete
adapter class, and without competing with the mechanism that already selects one. This is
[D1](#blocking-design-corrections).

**The problem, in two halves.**

*Naming the delegate.* The design has the decorator hold `private final NgsiLdEntityRepository
delegate` so that injecting `EntityRepository` does not bind the decorator to itself. That is
impossible from any module outside `common`: `StorageBoundaryTest` fails any class outside
`org.fiware.tmforum.common.repository..` that depends on it, and `ControllerStorageBoundaryTest` fails
any class outside `org.fiware.tmforum.common..` that depends on a non-interface type in that package.
On `ticket-88/work` it is also pointless: the PostgreSQL adapter ticket 6 promised has landed, so
naming the NGSI-LD one would leave `postgresql` deployments silently unenforced.

*Winning the injection.* `@Primary` is no longer free. `PostgresEntityRepository` and
`PostgresReferencesRepository` are `@Singleton @Primary @Requires(env = PostgresStorage.ENVIRONMENT)`,
and `NgsiLdEntityRepository`/`BrokerBackedReferencesRepository` are plain `@Singleton` — `@Primary` is
exactly how the active adapter wins. A decorator that is also `@Primary` makes two primary beans in a
`postgresql` context, which is a `NonUniqueBeanException`, not a decoration.

**In scope.**
* A `@StorageAdapter` qualifier annotation in `storage-api`
  (`@Qualifier @Retention(RUNTIME)`), documented as "the bean that actually talks to a backend; put
  it on every adapter".
* `@StorageAdapter` on all four adapter beans: `NgsiLdEntityRepository`,
  `BrokerBackedReferencesRepository`, `PostgresEntityRepository` and `PostgresReferencesRepository`
  — the last two for [AP-17](#ap-17--close-the-reference-port-existence-oracle)'s benefit and so a
  decorator is backend-agnostic by construction.
* A selection mechanism for the decorator that does **not** collide with the adapters' `@Primary`.
  The straightforward one: the adapters keep `@Primary`, and the decorator injects
  `@StorageAdapter EntityRepository` while being selected over them by `@Replaces`, or by moving the
  adapters' env-based choice onto the qualifier. Settle it here, in one place, rather than in the
  decorator ticket.
* A note in [`storage-api/README.md`](../../storage-api/README.md) § *Adding an adapter*: an adapter
  carries the qualifier; a decorator injects `@StorageAdapter` and never names an adapter class.
* Confirmation that injection of an unqualified `EntityRepository` still resolves to the active
  adapter when no decorator exists, in every storage environment, so nothing changes with the feature
  off.

**Out of scope.** The decorator itself. This ticket ships with no behaviour change at all.

**Acceptance criteria.**
* Every existing injection point of `EntityRepository`, `EntityReadRepository` and
  `ReferenceReadRepository` resolves exactly as before, **on all three storages**; no module's
  `application.yaml` changes.
* Both ArchUnit tests still pass, unmodified.
* A test proves a decorator in a *different* package can inject the active adapter by qualifier and be
  injected in its place, in a `postgresql` context as well as the default one — it is the whole point
  of the ticket and the only thing in it that can be wrong.

**Tests.** `StorageAdapterQualifierTest` in `storage-api` or `common`, plus the existing suite green.

---

## Phase 1 — the module, the subject, the PDP

### AP-03 — The `access-policy` module and its configuration

**Goal.** A new Maven module, in the build, wired into `common` and `all-in-one`, containing nothing
but configuration and inert when the feature is off.

**In scope.**
* `access-policy/` as a module in the root [`pom.xml`](../../pom.xml), depending on `storage-api` and
  nothing else from this build. The architecture puts it before `common`; the house style set by
  [`storage-postgresql`](../../storage-postgresql/README.md) — a storage-participating module placed
  *after* `common` and depending on it — is the alternative. Either works, because nothing in `common`
  needs to call into this module; pick one and say why in the PR.
* `AccessPolicyProperties` as `@ConfigurationProperties("access-policy")`, in the style of
  [`GeneralProperties`](../../common/src/main/java/org/fiware/tmforum/common/configuration/GeneralProperties.java):
  every field Javadoc'd with its default and its consequence. The full surface is in
  [ARCHITECTURE.md § Configuration](./ARCHITECTURE.md#configuration).
* Nested `pdp` and `jwt` property classes.
* The dependency from `common` on `access-policy`, and from `all-in-one`.
* The `oci` profile: no new image, the module is a library.

**Watch for.** `decision-ttl` and `pdp.timeout` are `Duration`s, and
[AGENTS.md](../../AGENTS.md) records that a unit-less duration in `application.yaml` is *silently
dropped*. Both need a unit in every yaml and a test that a missing unit is rejected rather than
defaulted.

**Acceptance criteria.**
* `mvn clean install -DskipTests -DskipITs` passes with the new module.
* With no `access-policy` key in any yaml, `AccessPolicyProperties` reports
  `enabled = false` and every other default from the table in
  [ARCHITECTURE.md](./ARCHITECTURE.md#defaults-and-failure-modes).
* An enum-valued property (`mode`, `default-decision`, `total-count`) with an unknown value fails
  startup rather than falling back.
* No module's runtime behaviour changes.

**Tests.** `AccessPolicyPropertiesTest`, parameterized over the properties and their defaults.

---

### AP-04 — Subject resolution from a bearer token

**Goal.** A `Subject` is resolvable for every request when the feature is on, and nothing at all
happens when it is off.

**In scope.**
* `Subject`: identity, the presented credentials as claims, token expiry, and a credential
  fingerprint — the fingerprint is part of the decision cache key and keying on identity alone would
  let a weak presentation inherit a strong one's decision
  ([ARCHITECTURE.md § Caching](./ARCHITECTURE.md#caching)).
* `SubjectResolutionFilter`: `@Filter(MATCH_ALL_PATTERN)`, `HttpServerFilter`, `Ordered`,
  `@Requires(property = "access-policy.enabled", value = "true")`, modelled on
  [`ForwardedForFilter`](../../common/src/main/java/org/fiware/tmforum/common/filter/ForwardedForFilter.java) —
  compute, publish as a request attribute, proceed.
* JWT validation against `access-policy.jwt.jwks-uri`: signature, issuer, audience, expiry, with the
  key set cached and refreshed.
* `identity-claim` resolution, default `verifiableCredential.issuer`, including the dotted-path case.
* A request with no token resolves to an explicit *anonymous* subject, not to `null` — the difference
  between "no identity" and "not resolved" has to be representable, because
  [AP-6](./DECISIONS.md#ap-6--optional-default-allow-and-bypass-unauthenticated-callers-at-first)
  switches on it.

**Decision needed.** [Open question 1](./ARCHITECTURE.md#open-questions): `micronaut-security` or
`nimbus-jose-jwt`. Neither is on the dependency tree today. Resolve it **in this ticket's PR**, with
a decision record appended to [DECISIONS.md](./DECISIONS.md), because it is the only new third-party
dependency the feature adds and it sits on the request path of 21 services. The brief favours
`nimbus-jose-jwt`: `micronaut-security` brings a security filter chain that a feature which must be
inert when off should not be installing.

**Ordering.** The filter must run before anything reading the subject and after
`ForwardedForFilter` (which is `HIGHEST_PRECEDENCE`). `PaginationFilter` is `LOWEST_PRECEDENCE`.
Pick a value between them and state it in a comment, the way `PaginationFilter` states its own.

**Acceptance criteria.**
* A valid token yields a `Subject` with the configured identity claim and every credential claim.
* An expired, wrongly-signed, wrong-issuer or wrong-audience token is rejected, and the request is
  treated as anonymous rather than failing — a malformed token must not turn a `GET` into a `500`.
* With `access-policy.enabled` unset, the filter bean does not exist; assert on the application
  context.
* No log line, metric or audit record contains the raw token.

**Tests.** `SubjectResolutionFilterTest` (parameterized over token defects),
`AccessPolicySubjectResolutionIT` ([UC-5](./USE_CASES.md#uc-5--a-caller-acting-for-a-user)) — its
`bypass-unauthenticated` and `mode` dimensions land with
[AP-09](#ap-09--bypass-owner-exemption-and-observe-mode).

---

### AP-05 — The Policy Decision Point client

**Goal.** One reactive call to an external PDP, against the contract in
[ARCHITECTURE.md § The PDP contract](./ARCHITECTURE.md#the-pdp-contract), with a stub to test
everything else against.

**In scope.**
* `ValidationRequest` / `AccessDecision` wire records, serialised exactly as
  [POLICY_EXAMPLES.md § What the PDP receives](./POLICY_EXAMPLES.md#what-the-pdp-receives) shows,
  including `protocol: "tmf"`.
* A `@Client(id = "pdp")`-backed `PolicyDecisionPoint`, configured through
  `micronaut.http.services.pdp.*` the way `ngsi` already is, returning `Mono<AccessDecision>`. No
  `block()` anywhere ([C9](./ANALYSIS.md#constraints-that-shape-any-solution)).
* Failure handling: timeout, non-200 and unparsable body all resolve to a denial that is **tagged as
  a failure**, distinct from a policy denial, because
  [ARCHITECTURE.md](./ARCHITECTURE.md#defaults-and-failure-modes) requires the audit record to tell
  them apart. `fail-open: true` inverts the decision, not the tag.
* A reusable stub PDP for tests, able to answer by `(policy uid, subject)` and to count requests —
  [UC-8](./USE_CASES.md#uc-8--an-entity-without-a-policy-stays-public) asserts it receives *zero*.

**Out of scope.** Caching ([AP-06](#ap-06--the-decision-cache)) and any caller.

**Acceptance criteria.**
* The serialised request matches the documented shape field for field; assert against a committed
  JSON fixture rather than against the builder.
* An unreachable PDP produces a tagged failure denial within `pdp.timeout`, and the request does not
  hang.
* Jackson 3: no `BeanCreatedEventListener` on the `ObjectMapper`; any customisation is a
  `JacksonModule` bean ([AGENTS.md](../../AGENTS.md)).

**Tests.** `PolicyDecisionPointTest` with a mocked client, parameterized over the failure modes;
`StubPolicyDecisionPoint` as a test fixture in `access-policy`'s test-jar so every later IT can use it.

---

### AP-06 — The decision cache

**Goal.** Deduplication, which is what makes the whole design affordable: at most one `/validate` per
distinct policy uid per subject, cached across requests.

**In scope.**
* `@Cacheable` over the already-wired cache backends, keyed on policy uid + subject identity +
  credential fingerprint, and on resource facts **only** for policies that constrain on them — which
  the resolver detects from the policy's own target refinements
  ([ARCHITECTURE.md § The PDP contract](./ARCHITECTURE.md#the-pdp-contract)).
* TTL `min(token exp, decision-ttl)`.
* A `decisions` cache in every `application-in-memory.yaml` and `application-redis.yaml`: on
  `ticket-88/work` that is **72 files** — 20 modules under `src/main/resources` (`all-in-one`
  included) and 16 more under `src/test/resources`. The Redis ones need a `value-serializer` that can
  handle `AccessDecision`; the existing entries all name
  `io.github.wistefan.mapping.EntityVOSerializer`, which cannot. The `postgresql` environment is a
  *storage* profile, orthogonal to the cache profile, so it adds no cache files.
* Confirmation that the cache key is built from **method parameters**: the subject cannot be read
  from `ServerRequestContext` inside a `@Cacheable` method, so it has to be passed in.

**Acceptance criteria.**
* Listing 100 entities that share 6 policy uids issues 6 PDP calls, asserted on the stub's counter.
* A subject presenting a different credential set gets a different decision, not the cached one.
* Narrowing a policy takes effect within `decision-ttl`, and no cache path makes a revocation slower
  than a grant ([UC-9](./USE_CASES.md#uc-9--a-policy-change-takes-effect)).
* Both cache profiles work; the Redis profile round-trips an `AccessDecision`.

**Tests.** `DecisionCacheTest`, `AccessPolicyCacheIT` with a short `decision-ttl`, run under
`-Dcache=redis` as well as in-memory.

---

## Phase 2 — enforce on `get`

### AP-07 — The `PolicyCarrier` SPI and direct policy resolution

**Goal.** Given a domain object, produce the policies it carries and the scope each applies at,
without the `access-policy` module knowing a single domain class.

**In scope.**
* `PolicyCarrier`: the SPI, taking an entity and a scope, returning resolved policies keyed by
  `odrl:uid`.
* `dspace:scope` as the discriminator. A permission **without** a scope is ignored here and keeps its
  meaning elsewhere ([AP-2](./DECISIONS.md#ap-2--read-the-policies-the-entities-already-carry)) —
  this is what makes the feature additive, and it needs a test of its own.
* Implementations for `ProductOffering` (the `productOfferingTerm` pair) and the two specification
  types (the `authorizationPolicy` characteristic), reading through the accessor paths
  [AP-01](#ap-01--prove-the-policy-survives-the-entity-round-trip) established
  ([finding](./AP-01_FINDINGS.md#the-accessor-paths-for-policycarrier-ap-07)). Both specification
  types end at `service.CharacteristicValueSpecification.getTmfValue()`, so they share one value
  accessor.
* A governed-type short-circuit that runs **before** anything else, so `party-catalog`,
  `usage-management` and the other 18 modules cost nothing. In `all-in-one` the decorator is `@Primary`
  for every API at once, so this is a hot path, not a nicety.

**Open question to resolve here.** [Open question 2](./ARCHITECTURE.md#open-questions): where the
implementations live. They need to see both the SPI and the domain classes;
`product-shared-models` and `service-shared-models` are the candidates, and the module graph has to be
checked rather than assumed — `service-shared-models` carries `CharacteristicValueSpecification`, which
`product-shared-models` already imports.

**Acceptance criteria.**
* `access-policy` has no compile dependency on any domain class; assert it with an ArchUnit rule in
  the module, alongside the two existing boundary tests.
* An ungoverned type resolves to "no policies" without touching a field.
* A permission with no `dspace:scope` resolves to no policy at that scope.
* Resolution never throws on malformed policy JSON — a policy that cannot be read is a tagged failure
  decision, which `fail-open` governs, not an exception on a read path.

**Tests.** `PolicyCarrierTest` per carrier, parameterized over well-formed, scope-less and malformed
policies.

---

### AP-08 — The decorator, `get` only

**Goal.** A restricted entity read by id answers `404`, indistinguishably from one that does not
exist. Lists are untouched by this ticket.

**In scope.**
* `PolicyEnforcingEntityRepository`: `@Primary`, `@Singleton`,
  `@Requires(property = "access-policy.enabled", value = "true")`, implementing `EntityRepository`,
  injecting the adapter via `@StorageAdapter` from
  [AP-02](#ap-02--a-qualifier-for-the-storage-adapter-so-a-decorator-is-possible-at-all).
* `get` evaluates at scope `catalog` and returns `Mono.empty()` on denial — empty, not an exception,
  so the existing `switchIfEmpty` in
  [`AbstractApiController#retrieve`](../../common/src/main/java/org/fiware/tmforum/common/rest/AbstractApiController.java)
  and `patch` produce the standard `404` body with no new code path. That is what makes the `404`
  byte-identical to a genuine miss ([C7](./ANALYSIS.md#constraints-that-shape-any-solution)).
* `findEntities` and `findEntitiesPolymorphic` delegate unchanged.
* The write side delegates unchanged — but see [D5](#blocking-design-corrections): on
  `ticket-88/work` `ControllerStorageBoundaryTest` fails any class outside the two adapter packages
  that calls `createDomainEntity`/`updateDomainEntity`/`deleteDomainEntity` without implementing
  `EntityEventPublisher`, which a pass-through decorator does by definition. Resolve it by exempting
  the decorator in that rule — it writes nothing of its own and fires no events, so the rule's intent
  ("each backend can attach its events") is untouched. Do **not** make the decorator implement
  `EntityEventPublisher` to get around the rule.
* Reaching the subject through `ServerRequestContext.currentRequest()`, with the no-request case
  (the data-migrator, notification handling, anything off the request path) handled explicitly and
  documented.

**Acceptance criteria.**
* A denied `GET /{id}` and a `GET` of a nonexistent id produce identical status, headers and body.
* `default-decision: allow` plus an entity with no policy makes **zero** PDP calls
  ([UC-8](./USE_CASES.md#uc-8--an-entity-without-a-policy-stays-public)).
* With the feature off, no `access-policy` bean exists in the context and the active `@StorageAdapter` bean is
  injected directly — asserted on the context, not only on behaviour.
* A `PATCH` of a denied entity answers `404`, because
  [`AbstractApiController#patch`](../../common/src/main/java/org/fiware/tmforum/common/rest/AbstractApiController.java)
  reads through the port first. **`DELETE` does not read first** and is therefore *not* covered by this
  ticket — that is [D6](#blocking-design-corrections) and
  [AP-23](#ap-23--deny-writes-to-entities-the-subject-cannot-read). Assert the gap here so the next
  ticket closes a documented hole rather than discovering one.
* No `block()` on any path.

**Tests.** `PolicyEnforcingEntityRepositoryTest`;
`AccessPolicyProductOfferingIT` (the `get` half of
[UC-1](./USE_CASES.md#uc-1--an-entity-restricted-by-a-subject-attribute)); `AccessPolicyDefaultsIT`
([UC-8](./USE_CASES.md#uc-8--an-entity-without-a-policy-stays-public)), parameterized over
`default-decision`.

---

### AP-09 — Bypass, owner exemption and `observe` mode

**Goal.** The three switches that make the feature deployable into the existing ecosystem without
changing four other components first.

**In scope.**
* `bypass-unauthenticated` (default `true`): a request with no bearer token is not filtered, so
  `contract-management`, the EDC connector and `consent-facade` keep working unchanged
  ([C2](./ANALYSIS.md#constraints-that-shape-any-solution)).
* `anonymous-decision`: what an unauthenticated request gets once the bypass is off.
* `owner-exempt` (default `true`): a subject matching a `relatedParty` with role `provider` is never
  filtered from its own entities, decided **before** any PDP call. Without it a provider is locked out
  of the offering whose policy locks them out, including out of fixing it
  ([UC-7](./USE_CASES.md#uc-7--a-provider-manages-its-own-restricted-entity)).
* `mode: observe`: decide, record, filter nothing.
* Every bypass is distinguishable in the decision outcome from a policy permit, so
  [AP-18](#ap-18--the-access-audit-log) can log "bypassed" rather than "permitted"
  ([UC-10](./USE_CASES.md#uc-10--callers-without-a-token-are-not-filtered)).

**Note on sequencing.** The architecture puts `observe` in phase 5. It belongs here. `observe` is a
branch at the point where the decorator drops an entity, it is a few lines next to the code that does
the dropping, and [UC-5](./USE_CASES.md#uc-5--a-caller-acting-for-a-user) and
[UC-10](./USE_CASES.md#uc-10--callers-without-a-token-are-not-filtered) both call it the honest answer
for a deployment whose UI backend does not yet forward tokens. Building it in phase 2 makes the
feature measurable from the first enforcing release. The audit *log* stays in phase 5; `observe` with
nothing but a log line at `INFO` is still useful.

**Acceptance criteria.**
* Owner exemption costs no PDP call, asserted on the stub's counter.
* `owner-exempt: false` locks the owner out — tested as the documented behaviour, not fixed.
* A provider does **not** see another provider's restricted entities.
* `observe` produces the same decisions as `enforce` and an identical response body.

**Tests.** `AccessPolicyBypassIT` (parameterized over `bypass-unauthenticated` ×
`anonymous-decision`), `AccessPolicyOwnerExemptionIT` (parameterized over the setting),
`AccessPolicySubjectResolutionIT` completed.

---

### AP-23 — Deny writes to entities the subject cannot read

**Goal.** A subject who cannot read an entity cannot delete or update it either. This is
[D6](#blocking-design-corrections), and it is a hole that opens the moment
[AP-08](#ap-08--the-decorator-get-only) ships.

**The problem.** `AbstractApiController#delete` calls `eventPublisher.deleteWithEvent(id)`, and
`DirectEntityEventPublisher.deleteWithEvent` is `repository.deleteDomainEntity(id)` — no read, so
nothing consults a policy. A consumer who learns the id of an offering that is invisible to them, and
answers `404` on `GET`, can still `DELETE` it. The architecture's own decision model lists `delete`
among the actions and maps it from the HTTP method, so this is an omission in the build-out, not a
deliberate non-goal.

`updateWithEvent` has a quieter version of the same shape: it reads the old state through the injected
`EntityRepository`, which with a decorator in place is the decorator. For a denied entity the old state
comes back empty, and the code then updates anyway and fires no update event. Today
`AbstractApiController#patch` 404s before that is reached, so it is latent rather than live — but it is
one refactor away from being live, and it should be pinned by a test either way.

**In scope.**
* Evaluate the governing policy at scope `catalog` before a `delete` and before an `update`, and
  answer `404` on denial, consistent with the read path and with
  [AP-5](./DECISIONS.md#ap-5--a-denied-read-answers-404-a-denied-order-answers-403): the caller may not
  know the entity exists.
* The owner exemption applies, so a provider still manages its own restricted entities
  ([UC-7](./USE_CASES.md#uc-7--a-provider-manages-its-own-restricted-entity)).
* Decide where the check goes. The decorator's `deleteDomainEntity`/`updateDomainEntity` is the
  smallest change and keeps everything in one bean, but it collides with the eventless-write ArchUnit
  rule the same way [AP-08](#ap-08--the-decorator-get-only) does; a read before the write in the
  controller's `delete` is more explicit and matches what `patch` already does. Pick one and record it.
* A test that `updateWithEvent` cannot write with an empty old state for a denied entity.

**Acceptance criteria.**
* `DELETE` of a denied entity answers `404` and the entity is still there afterwards.
* `PATCH` of a denied entity answers `404` and nothing is written — asserted by reading the entity back
  as the owner, not only by the status code.
* The owner can still delete and update its own restricted entities.
* `observe` mode does not block the write, and records what it would have blocked.

**Tests.** `AccessPolicyWriteDenialIT`, parameterized over `DELETE`/`PATCH` × owner/non-owner.

---

### AP-10 — The decorator is still a storage adapter

**Goal.** The decorator satisfies the port's contract, on by default and off by default, so a later
ticket cannot quietly break it.

**In scope.**
* Extend
  [`AbstractEntityRepositoryContractTest`](../../storage-api/src/test/java/org/fiware/tmforum/storage/contract/AbstractEntityRepositoryContractTest.java)
  for the decorator, in both configurations: feature off (it must be transparent) and feature on with
  a permit-all policy (it must still satisfy every rule).
* Particular attention to *"Always emits a page, an empty one when nothing matches"* — the rule a
  refilling decorator is most likely to break. On `ticket-88/work` the NGSI-LD adapter's old
  "completes empty instead" defect is gone from the port Javadoc and from `KnownDeviation`, so this is
  now a rule the decorator must actually hold rather than a deviation it inherits.
* A `ReferenceFixture` through `referenceFixture()`, which the contract now requires of every adapter,
  and the reference round-trip cases (`createdReferencesRoundTripIntact`,
  `updateKeepsUntouchedReferences`, `getDoesNotResolveReferences`).
* Any rule the decorator genuinely cannot meet goes in `knownDeviations()`, so a later fix fails the
  case instead of passing silently.

**Acceptance criteria.**
* Both configurations pass the contract suite, on all three storages.
* Deviations are declared, not skipped.

**Tests.** `PolicyEnforcingEntityRepositoryContractIT`, parameterized over the feature flag.

---

## Phase 3 — enforce on lists

### AP-11 — Composition walk and policy union

**Goal.** An offering inherits the policies of its specification and of that specification's services,
so a restriction is written once where the restricted thing lives
([UC-6](./USE_CASES.md#uc-6--a-restriction-inherited-from-a-shared-service-specification)).

**In scope.**
* `PolicyResolver`: offering → `ProductSpecification` → `ServiceSpecification`s, unioning policies
  keyed by `odrl:uid`.
* **The walk reads through the delegate.** Routing it through the decorator makes the filter censor
  its own inputs: a restricted specification becomes unresolvable to the resolver trying to read it,
  and hides every product built from it from everyone. This is the single most likely bug in the
  feature and deserves a test named after it.
* Two different policies claiming one `odrl:uid` is an **error**, reported as such, not silently
  resolved to one of them — the same way it fails order activation in the surrounding deployment
  today.
* A depth limit, and a cache of the walk keyed by specification id.
* Union, never override: an offering's own policy is combined with the inherited ones.

**Acceptance criteria.**
* Adding a product composed of a restricted service hides it immediately, with nothing authored on it.
* A uid collision is reported with both policy sources named.
* The depth limit is hit rather than recursed past, and is configurable only if a reason appears.
* A restricted specification does not hide its own products from the owner.

**Tests.** `PolicyResolverTest` (parameterized over composition shapes, collisions, depth),
`AccessPolicyCompositionIT`.

---

### AP-12 — Page refill for `findEntities`

**Goal.** The property the whole design exists to preserve: `offset` and `limit` index the
*permitted* subsequence, so a filtered collection pages exactly like an unfiltered one.

**In scope.**
* `findEntities` and `findEntitiesPolymorphic`: fetch a window from the delegate, decide each entity,
  advance the storage offset, repeat until `offset` permitted entities have been skipped and `limit`
  collected, or the delegate returns a short page. Note the signatures on `ticket-88/work`: the
  `query`/`orderBy` string pair became one
  [`EntityQuery`](../../storage-api/src/main/java/org/fiware/tmforum/storage/query/EntityQuery.java),
  which the decorator passes through untouched.
* Return `new PagedResult<>(items, offset, limit, total)` with the **client's** offset and limit, so
  [`AbstractApiController#list`](../../common/src/main/java/org/fiware/tmforum/common/rest/AbstractApiController.java)
  sets its request attributes and
  [`PaginationFilter`](../../common/src/main/java/org/fiware/tmforum/common/filter/PaginationFilter.java)
  builds `Link` and `X-Result-Count` with no change to either.
* `overfetch-factor`: fetch `limit × factor` per delegate call, so cost tracks the hidden fraction
  rather than the collection size.
* `max-scan` as a hard bound on the work one request may do.
* Sort preservation: the backend applies the sort over the unfiltered set and filtering preserves
  relative order, so a filtered page of a sorted collection is still sorted
  ([open question 6](./ARCHITECTURE.md#open-questions)). On `ticket-88/work` this is only *testable* on
  PostgreSQL: `KnownDeviation.SORT_IS_IGNORED` records that both Orion-LD 1.9.0 and Scorpio 5.0.3
  return a sorted list in the order it would have had without the sort, so there is no order for the
  decorator to preserve there. Assert it on the `postgresql` environment and state the rest as a
  deviation rather than claiming coverage.

**Acceptance criteria.**
* Every full page holds exactly `limit` items while items remain.
* Consecutive pages are disjoint and complete; `offset` past the end gives an empty array and `200`.
* The refill arithmetic is unit-tested over offsets, limits, hidden fractions and short pages — this
  is where the bugs will be, and an IT will not find them.
* The number of delegate calls for a sparsely-permitted collection is bounded and asserted.

**Tests.** `PageRefillTest`, heavily parameterized; the body half of
`AccessPolicyPaginationIT` ([UC-12](./USE_CASES.md#uc-12--paging-a-filtered-collection)).

---

### AP-13 — `total-count` modes

**Goal.** `X-Total-Count`, the `last` link and the `200`/`206` status describe the collection **as the
caller sees it**, or say nothing at all — never something unverified.

**In scope.**
* `exact` (default): decide the match set, bounded by `max-scan`; `PagedResult.totalCount` becomes the
  permitted count. Above the bound, degrade to `omit`.
* `omit`: pass `null`, so `PaginationFilter` omits `X-Total-Count` and `last`, keeps `200`, and infers
  `next` from `returnedCount == limit` — correct precisely because
  [AP-12](#ap-12--page-refill-for-findentities) refills pages.
* `upstream`: pass the broker's unfiltered total through. It **leaks the size of the hidden set** and
  produces links into it; the test asserts that it does, so nobody enables it by accident.
* The case where the backend reports no total at all (`general.countHeader` unset or not sent on the
  NGSI-LD adapter): nothing to correct, result stays `null` as today. The PostgreSQL adapter always
  computes a total with a real `COUNT` query, so on that environment `exact` is always meaningful and
  `omit` is the only way to get no total — which also means `exact`'s scan-and-decide replaces a query
  the backend could have answered in one shot, and is the strongest argument for
  [AP-20](#ap-20--predicate-push-down).

**Acceptance criteria.**
* `exact` over 100 offerings with 40 hidden yields `X-Total-Count: 60`, a `last` link inside the 60
  and `206`.
* Past `max-scan`, `X-Total-Count` and `last` are omitted, the status is `200`, and paging still works.
* The headers and status are asserted, not only the body — a test that checks the array and not the
  `Link` header passes while the feature is broken.

**Tests.** `AccessPolicyPaginationIT`, parameterized over `total-count` and run on **all three**
storages, since `totalCount` availability differs between them: configured-and-sent on orion-ld and
scorpio, always present on postgresql.

---

### AP-14 — Catalog and category policies

**Goal.** One policy on a catalog governs every offering in it, without extending TMF620
([UC-3](./USE_CASES.md#uc-3--a-private-catalog-for-a-named-partner)).

**In scope.**
* Matching policies registered at the PDP whose `odrl:target` names the entity by id — TMF620 gives
  `Catalog` and `Category` no characteristic array, so there is no carrier on the entity
  ([open question 3](./ARCHITECTURE.md#open-questions)).
* **Downward derivation:** an offering is visible only if its `Catalog` and `Category` are.
* **Upward derivation** behind `derive-catalog-visibility` (default `false`): a catalog with no policy
  of its own is visible if at least one of its offerings is. It costs an extra query per candidate,
  which is why it is off.
* An offering's own policy is combined with its catalog's, never overridden.

**Acceptance criteria.**
* A non-partner sees neither the preview catalog nor its offerings, and gets `404` on an offering id.
* With `derive-catalog-visibility: false` a catalog that is empty for a subject stays visible and
  empty; with `true` it is hidden.
* The extra queries the upward rule costs are counted in a test, so the default is justified by a
  number.

**Tests.** `AccessPolicyCatalogIT`, parameterized over `derive-catalog-visibility`.

**Risk.** This is the ticket that tests whether target-id matching is sufficient in practice. If it is
not, the outcome is a schema proposal, not a workaround.

---

## Phase 4 — ordering and quoting

### AP-15 — A `FORBIDDEN` exception reason

**Goal.** `403` is expressible. Today it is not: `TmForumExceptionReason` has five constants and
`TmForumExceptionHandler`'s `default ->` renders `500`. This is
[D3](#blocking-design-corrections).

**In scope.** A `FORBIDDEN` constant, its branch in the handler with the standard `ErrorDetails`
shape, and a check that no other handler or `switch` over the enum is left non-exhaustive.

**Acceptance criteria.** A thrown `TmForumException(FORBIDDEN)` renders `403` with the standard body;
the existing four reasons are unchanged. The ticket is independent of everything else in the feature
and can merge on its own.

**Tests.** `TmForumExceptionHandlerTest`, parameterized over the reasons.

---

### AP-16 — The contract-policy check on order and quote creation

**Goal.** A `ProductOrder` or `Quote` referencing an offering the subject may not contract is refused
with `403` before anything is written
([UC-2](./USE_CASES.md#uc-2--visible-but-not-orderable)).

**In scope.**
* The check in the create path of
  [`ProductOrderingApiController`](../../product-ordering-management/src/main/java/org/fiware/tmforum/productordering/rest/ProductOrderingApiController.java)
  and `QuoteApiController`, where the reference is already resolved through `getCheckingMono`.
* Load the referenced offering **through the delegate**, so an offering that is invisible at scope
  `catalog` still resolves and the decision is the contract policy's to make.
* Evaluate at `contract.negotiation`; on denial throw `FORBIDDEN` naming the offering and the failing
  policy uid, so the consumer can work out which credential to obtain.
* Nothing is persisted on denial — no order in an intermediate state.
* These are the **only two modules** that change. If a third needs touching, the design is wrong and
  the ticket says so rather than spreading.

**Acceptance criteria.**
* Visibility and contractability are independently decidable: denying one does not deny the other.
* An offering readable at `catalog` and denied at `contract.negotiation` answers `200` on `GET` and
  `403` on order.
* The product-order collection is unchanged after a refused create.
* A `403` names the offering and the policy uid; a `404` from the read path still says nothing
  ([AP-5](./DECISIONS.md#ap-5--a-denied-read-answers-404-a-denied-order-answers-403)).

**Tests.** `AccessPolicyOrderingIT`, and the ordering half of `AccessPolicyProductOfferingIT`.

---

### AP-17 — Close the reference-port existence oracle

**Goal.** A consumer cannot establish that a hidden entity exists by referencing it. This is
[D2](#blocking-design-corrections).

**The problem.** `ReferenceValidationService` reads through `ReferenceReadRepository`, not through
`EntityReadRepository`, so the decorator never sees it. POSTing any entity that references a hidden
offering therefore answers `400 INVALID_RELATIONSHIP` when the id is unknown and succeeds when it
exists — a clean yes/no oracle over every restricted id, which is exactly what
[C7](./ANALYSIS.md#constraints-that-shape-any-solution) and
[AP-5](./DECISIONS.md#ap-5--a-denied-read-answers-404-a-denied-order-answers-403) exist to prevent.

**What `ticket-88/work` adds to it.** The port grew a bulk `findReferences(Collection<String>)` — which
`ReferenceValidationService` now uses to check a whole list in one call, and which the PostgreSQL
adapter answers with a single query — so a decorator has to cover three methods, not two. And
`StoredReference` now carries a `version` label, non-empty for every type in
`VersionedEntity.VERSIONED_TYPES`, which includes `product-offering`, `catalog`, `category` and
`product-specification`. The oracle therefore no longer leaks only existence but the current version of
a hidden entity, and the version-pinning rule turns a version mismatch into a distinguishable failure.

**In scope.** Decide and implement one of:
1. A decorator on `ReferenceReadRepository` that hides references to entities the subject may not read,
   so the oracle answers "does not exist". Consistent, and it makes a referencing create fail for the
   same reason a direct read does. Costs a decision per validated reference, and must cover
   `referenceExists`, `findReference` and `findReferences` — the bulk one especially, since dropping
   entries from its `Flux` is how the leak is actually closed. Note that `@Primary` is taken here too
   ([AP-02](#ap-02--a-qualifier-for-the-storage-adapter-so-a-decorator-is-possible-at-all)).
2. Leave the port undecorated and document the oracle as a known limitation of the feature, with the
   deployment obligation that the API is not reachable by untrusted callers.

Option 1 is the one consistent with the stated requirement; option 2 is honest but weakens the feature
to a visibility nicety against a determined caller. Either way, the outcome is recorded as a decision
in [DECISIONS.md](./DECISIONS.md) and the claim in
[AP-3](./DECISIONS.md#ap-3--enforce-at-the-storage-port) that the one seam covers reference validation
is corrected.

**Acceptance criteria.**
* A test probes a hidden offering by POSTing a referencing entity and asserts the chosen behaviour.
* Reference validation for permitted and ungoverned references is unaffected, and the
  `tmforum.references` metric still counts what it counted.
* The version label of a hidden entity is not observable, and a reference pinned to a hidden entity's
  version fails the same way as one to a nonexistent entity.
* If option 1: the per-reference decision reuses the cache, and bulk `findReferences` validation does
  not multiply PDP calls.

**Tests.** `AccessPolicyReferenceValidationIT`.

---

## Phase 5 — audit and cost control

### AP-18 — The access audit log

**Goal.** Every decision is explainable afterwards
([UC-11](./USE_CASES.md#uc-11--a-denied-access-is-auditable)).

**In scope.**
* `AccessAuditPublisher`: one record per decision — decision, reason, policy uid, resource type and
  id, subject identity, method, path, request id — emitted asynchronously, never changing the
  decision and never blocking the response.
* The reason distinguishes "denied by policy X" from "denied because the PDP was unreachable" and from
  "bypassed". An operator hunting for the policy that hid an entity, when the real cause was an
  unreachable PDP, is a bad afternoon.
* `audit.enabled`, default `false`, and an OTLP-friendly structured log — Micrometer and the OTEL
  agent are already part of how this project is run.
* No raw token, and no credential claim beyond the identity, in a record.

**Acceptance criteria.**
* `observe` mode produces records identical to `enforce` and leaves the body untouched.
* A slow or failing audit sink does not slow or fail a request.
* Listing a large filtered collection does not emit one record per scanned entity unless asked to —
  volume control is part of the ticket, not a follow-up.

**Tests.** `AccessPolicyAuditIT`.

---

### AP-19 — The cursor memo

**Goal.** Sequential paging — what every UI does — costs one window per page instead of rescanning
from offset zero.

**In scope.** A cache, keyed by subject + query fingerprint, of the storage offset at which each
filtered offset begins, populated as a client pages forward; short TTL, size-bounded. A deep random
`offset` without a memo entry stays the expensive case and stays bounded by `max-scan`.

**Acceptance criteria.** Paging a 10 000-entity filtered collection front to back issues a bounded,
asserted number of delegate calls; a stale memo entry produces a correct page, never a wrong one — the
memo is an optimisation and must be verifiable, so a wrong entry costs a rescan rather than a wrong
answer.

**Tests.** `CursorMemoTest`, and a paging IT that asserts the call count.

---

## Phase 6 — the target state

### AP-20 — Predicate push-down

**Goal.** When every governing policy for a type reduces to an attribute predicate
(`relatedParty`, `category`, `lifecycleStatus`), translate it into the NGSI-LD query and let the broker
do offset, limit and count. Exact, O(1), and the proper endgame for large catalogs.

**In scope.** Detecting the reducible shape, building the predicate as an
[`EntityQuery`](../../storage-api/src/main/java/org/fiware/tmforum/storage/query/EntityQuery.java)
filter and AND-ing it into the caller's, and falling back to per-entity decisions for anything else. A
benchmark against [open question 5](./ARCHITECTURE.md#open-questions): whether resource-constrained
policies, which weaken deduplication, should force `total-count: omit`.

**This got easier.** The architecture has the push-down reusing `QueryParser`/`QueryParams` — NGSI-LD
`q` syntax. On `ticket-88/work` ticket 7 landed `org.fiware.tmforum.storage.query`: a backend-neutral
filter tree (`QueryFilter`, `AttributePath`, `ComparisonOperator`, `SortKey`, `ValueType`) that each
adapter renders itself. A policy predicate is built once against that tree and works on every backend,
instead of being emitted as broker syntax a second adapter has to re-parse. Target the tree, not `q`.

**Acceptance criteria.** A pushed-down query returns the same items as the per-entity path for the same
policy — asserted by running both and comparing, not by trusting the translation. A policy that does
not reduce is detected and not pushed.

**Tests.** `PredicatePushDownTest`, plus an equivalence IT over both paths.

---

### AP-21 — Bypass by identity

**Goal.** Infrastructure callers are exempt because of who they are, not because they sent no token,
so `bypass-unauthenticated` can go to `false`.

**In scope.** `bypass-roles` and `bypass-subjects`; an audit record that names which rule exempted a
request; documentation of the migration — enable with the bypass on, give infrastructure callers
tokens, turn it off — with each step independently verifiable.

**Acceptance criteria.** With `bypass-unauthenticated: false`, a caller with a bypass role reads
unfiltered and an anonymous caller is decided by `anonymous-decision`. The audit distinguishes the two.

**Tests.** `AccessPolicyBypassIT` extended with the identity dimension.

---

## Documentation

### AP-22 — Documentation and release notes

**In scope.**
* `access-policy/README.md` in the style of
  [`storage-api/README.md`](../../storage-api/README.md): the Javadoc is the contract, the README is a
  map of it.
* The deployment obligation, stated plainly and not softened: while `bypass-unauthenticated` is
  `true`, the API must not be published unauthenticated, or dropping an `Authorization` header defeats
  the feature.
* Update [AGENTS.md](../../AGENTS.md) — project structure, key conventions — and the root
  [README.md](../../README.md).
* Fold the decisions taken in [AP-04](#ap-04--subject-resolution-from-a-bearer-token) (JWT library),
  [AP-07](#ap-07--the-policycarrier-spi-and-direct-policy-resolution) (where the carriers live) and
  [AP-17](#ap-17--close-the-reference-port-existence-oracle) (the reference port) into
  [DECISIONS.md](./DECISIONS.md), and correct the claims
  [D1–D4](#blocking-design-corrections) falsify.
* `CLAUDE.md` was dropped on this base in favour of `AGENTS.md` alone, so every pointer added here goes
  into `AGENTS.md` — its *Project Structure* and *Important Files* blocks, beside the `storage-api` and
  `storage-postgresql` entries.
* Remove the `:construction:` banners from this folder once the feature ships.
* `RELEASE_NOTES_ACCESS_POLICIES.md` in the project root, per the global workflow.

---

## Outside this project

Not tickets here, but the feature does not work without them. [Phase 0 in
ARCHITECTURE.md](./ARCHITECTURE.md#rollout) names the first; the second is the feature's one hard
external dependency.

| # | What | Who |
|---|---|---|
| X-1 | The PDP learns the instance-level vocabulary (`tmf:resourceId`, `tmf:relatedParty`, `tmf:catalog`, `tmf:category`, generic `vc:<claim>`) and both scopes — see [POLICY_EXAMPLES.md § Vocabulary](./POLICY_EXAMPLES.md#vocabulary) | the ODRL-PAP, or whichever PDP a deployment runs |
| X-2 | The marketplace UI backend forwards the end user's access token on its outbound calls. Until it does, its calls are anonymous and `observe` mode plus the audit log is the honest answer | the UI backend |
| X-3 | The cross-component half of [UC-4](./USE_CASES.md#uc-4--one-policy-one-answer-on-every-path) — one policy, same answer on both paths — belongs in the Data Space Connector's integration tests | DSC |

## Sequencing notes

* **[AP-01](#ap-01--prove-the-policy-survives-the-entity-round-trip) goes first and alone.** It is the
  only ticket that can invalidate the design, and it is a day's work. Done: it did not invalidate
  the design ([finding](./AP-01_FINDINGS.md)).
* **[AP-02](#ap-02--a-qualifier-for-the-storage-adapter-so-a-decorator-is-possible-at-all) and
  [AP-15](#ap-15--a-forbidden-exception-reason) can merge immediately.** Both are small, both are pure
  groundwork, and neither changes any behaviour. They need no decision from anybody. AP-02 is also the
  one ticket that has grown since it was written — it now has to settle how a decorator wins the
  injection against two `@Primary` adapters, which is a design decision, not a mechanical change.
* **[AP-23](#ap-23--deny-writes-to-entities-the-subject-cannot-read) ships with phase 2 or phase 2 does
  not ship.** An enforcing release in which a hidden entity answers `404` on `GET` and deletes happily
  on `DELETE` is worse than no release, because it reads as a guarantee it does not give.
* **`observe` mode moves from phase 5 to phase 2** ([AP-09](#ap-09--bypass-owner-exemption-and-observe-mode)).
  It is a branch next to the code that drops an entity, and two use cases depend on it as the honest
  answer while a UI backend still sends no token.
* **Phase 3 is the bulk of the work.** [AP-12](#ap-12--page-refill-for-findentities),
  [AP-13](#ap-13--total-count-modes) and [AP-14](#ap-14--catalog-and-category-policies) are the three
  largest tickets and they are sequential.
* **A release is possible after phase 2** — `get` enforcement, bypass, owner exemption, `observe` — and
  that release is useful on its own: it closes the id-probing path and makes the policy's impact
  measurable. Lists stay unfiltered until phase 3, which has to be said in the release note, because a
  half-enforced feature that nobody warned about is worse than none.
* **[AP-17](#ap-17--close-the-reference-port-existence-oracle) only matters once lists and `get` are
  enforced**, but it is listed in phase 4 rather than later because the oracle it closes makes the
  phase-2 `404` guarantee conditional until it is resolved. Pull it forward if the deployment that
  enables the feature is exposed to untrusted callers.
* **The base is still moving.** `ticket-88/work` gained five substantial tickets between `main` and
  `9b4a6b00`, three of which changed the storage port's signatures or its bean wiring. Re-check
  [D1](#blocking-design-corrections) and the `EntityQuery` signatures before
  [AP-02](#ap-02--a-qualifier-for-the-storage-adapter-so-a-decorator-is-possible-at-all) and
  [AP-12](#ap-12--page-refill-for-findentities) start; the pagination argument the whole design rests
  on, by contrast, is unaffected —
  [`PaginationFilter`](../../common/src/main/java/org/fiware/tmforum/common/filter/PaginationFilter.java)
  and the request-attribute handoff in `AbstractApiController#list` are byte-identical to `main`.
