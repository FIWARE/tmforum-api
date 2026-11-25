# Implementation Plan: TMForum - Access Policies - 02 - Storage adapter qualifier

## Overview
Add a `@StorageAdapter` qualifier to `storage-api` and put it on the four adapter beans. Move each adapter's
environment choice entirely onto `@Requires`, so no adapter needs `@Primary` and a decorator can take it. The
decorator injects `@StorageAdapter EntityRepository` and is `@Primary`. `ARCHITECTURE.md` and `DECISIONS.md`
already assume a `@Primary` decorator but say it injects its delegate by concrete type; Step 1 aligns both docs. This
ticket adds no decorator. The one behaviour change is that a `postgresql` context without `storage-postgresql` on the
classpath no longer falls back to the broker adapters (see Step 1). A context test proves the mechanism on the
default storage and on `postgresql`.

### Decided selection mechanism (settled here, once)
| Bean | Today | After this ticket |
|---|---|---|
| `NgsiLdEntityRepository` (`common/.../repository`) | `@Singleton` | `@Singleton @StorageAdapter @Requires(notEnv = CommonConstants.POSTGRESQL_ENVIRONMENT)` |
| `BrokerBackedReferencesRepository` (`common/.../repository`) | `@Singleton` | same as above |
| `PostgresEntityRepository` (`storage-postgresql`) | `@Singleton @Primary @Requires(env = postgresql)` | `@Singleton @StorageAdapter @Requires(env = PostgresStorage.ENVIRONMENT)`, **no `@Primary`** |
| `PostgresReferencesRepository` (`storage-postgresql`) | same as above | same as above |
| future decorator (e.g. AP's `PolicyEnforcingEntityRepository`) | n/a | `@Singleton @Primary @Requires(property = ...)`, injects `@StorageAdapter EntityRepository`, never carries `@StorageAdapter` itself |

Why this and not `@Replaces`: in Micronaut, `@Replaces(X)` removes X from the candidate set for every lookup.
That includes the replacing bean's own `@StorageAdapter` injection point, so the decorator could not reach the
adapter. Moving the env choice onto `@Requires` leaves exactly one adapter per port interface in every context.
`@Primary` is then free for at most one decorator per interface. Chained decorators are out of scope. The broker
adapters get the same `notEnv` gate that other broker-only beans already carry.

Without a decorator, every lookup still resolves to the single adapter. With one, unqualified lookups resolve to the
`@Primary` decorator, and `@StorageAdapter` lookups resolve to the adapter, because the decorator lacks the qualifier.
Lookups by concrete class keep working: `getBean(NgsiLdEntityRepository.class)` in the migrator's broker context,
`getBean(PostgresEntityRepository.class)` in its PostgreSQL context, and the `@Inject PostgresEntityRepository` ITs.

## Steps

### Step 1: `@StorageAdapter` qualifier, adapter annotations and env-only adapter selection
Production change and docs only. No test classes are added here; Step 2 proves the mechanism.

Behaviour change to state in the step summary: a `postgresql` context *without* `storage-postgresql` on the classpath
used to fall back silently to the broker adapters, and now has no `EntityRepository`/`ReferenceReadRepository` bean.
No shipped deployment hits this (all 21 API modules and all-in-one depend on `storage-postgresql`). Only `common`'s
own tests start such a context (`CallbackRouteTest`, `NotificationWiringTest`, `StoragePortWiringTest`); they appear
to use only `containsBean` or lazy wiring, and `mvn test -pl common` must prove it. If one fails, fix the test's
expectation (the broker adapter is now absent there by design), not the gate.

- `storage-api/pom.xml`: add `jakarta.inject:jakarta.inject-api` (compile scope). The `neutral-port` enforcer rule
  bans `io.micronaut*:*`, `io.github.wistefan:*` and `org.fiware.tmforum:*`, but `jakarta.inject` is allowed. It
  should be BOM-managed through `micronaut-parent`, so check with
  `mvn help:effective-pom -pl storage-api | grep -A2 jakarta.inject-api`. If it is not managed, add a
  `version.jakarta.inject.jakarta.inject-api` root-pom property, following the repo convention.
- New `storage-api/src/main/java/org/fiware/tmforum/storage/StorageAdapter.java`: `@jakarta.inject.Qualifier`,
  `@Documented`, `@Retention(RUNTIME)`, `@Target({TYPE, METHOD, FIELD, PARAMETER})`. Give it a Javadoc of 2 lines
  at most, along the lines of: "Marks the bean that actually talks to a backend; put it on every adapter. A decorator
  injects the port with this qualifier and never names an adapter class."
- `common/src/main/java/org/fiware/tmforum/common/repository/NgsiLdEntityRepository.java` and
  `BrokerBackedReferencesRepository.java`: add `@StorageAdapter` and
  `@Requires(notEnv = CommonConstants.POSTGRESQL_ENVIRONMENT)`. Note that `BrokerBackedReferencesRepository` is
  indented with tabs.
- `storage-postgresql/src/main/java/org/fiware/tmforum/storage/postgresql/PostgresEntityRepository.java` and
  `PostgresReferencesRepository.java`: add `@StorageAdapter`, remove `@Primary` and its now-unused import, and keep
  `@Requires(env = PostgresStorage.ENVIRONMENT)`.
- `storage-api/README.md` § *Adding an adapter*: rewrite item 4. An adapter carries `@StorageAdapter` and is chosen
  by `@Requires` alone: env for the PostgreSQL adapter, `notEnv = postgresql` for the broker adapter. An adapter is
  never `@Primary`. Add one sentence for decorators: a decorator is `@Primary`, injects `@StorageAdapter
  EntityRepository` (or `ReferenceReadRepository`), never names an adapter class, and does not carry the qualifier
  itself. Mention the `@Replaces` pitfall in half a sentence.
- `doc/access-policies/ARCHITECTURE.md` (§ `PolicyEnforcingEntityRepository`, ~line 196-214): change the snippet's
  delegate to `@StorageAdapter EntityRepository delegate`, and change the "injected by concrete type" sentence to
  "injected by the `@StorageAdapter` qualifier". The reasoning about the composition walk stays. In the flowchart
  (~line 67), rename the `ngsi[NgsiLdEntityRepository]` node to a generic `adapter["@StorageAdapter EntityRepository<br/>
  (NGSI-LD or PostgreSQL)"]`, since the decorator now sits in front of either adapter. At ~line 104-105, change
  "Off, it does not exist, and `NgsiLdEntityRepository` is injected directly as it is today" to "Off, it does not
  exist, and the active `@StorageAdapter` bean is injected directly as it is today".
- `doc/access-policies/TICKETS.md:440` (a later ticket's acceptance criterion) carries the same stale wording: change
  "`NgsiLdEntityRepository` is injected directly" to "the active `@StorageAdapter` bean is injected directly". The D1
  row (l.61) and the AP-02 notes (l.165-183) describe the old design on purpose and stay.
- `doc/access-policies/DECISIONS.md:105`: change "the delegate must be injected by concrete type" to "the delegate is
  injected by the `@StorageAdapter` qualifier", so the two design docs agree. The rest of that bullet stays.
- `AGENTS.md` already records the convention (Key Conventions, `@StorageAdapter` bullet). Update it only if the
  final shape differs.

Acceptance:
- `mvn clean install -DskipTests -DskipITs` builds, and the `storage-api` enforcer rule passes.
- `mvn test -pl storage-api,common,storage-postgresql,product-catalog,all-in-one` is green. This includes all three
  ArchUnit tests, unmodified: `common/.../architecture/StorageBoundaryTest`,
  `product-catalog/.../architecture/StorageBoundaryTest` and `all-in-one/.../ControllerStorageBoundaryTest`.
- `git grep -n "@Primary" -- storage-postgresql/src/main` returns nothing.
- No `application*.yaml` changes anywhere.

### Step 2: `StorageAdapterQualifierTest` proving the decorator mechanism on all storages
Add a plain JUnit test that starts a real Micronaut context, with no live DB or broker. It goes in
`storage-postgresql` test scope, because only that module sees both adapters: `common` is a compile dependency
there, and the test scope already has `micronaut-runtime`, `micronaut-http-client`, `micronaut-jackson-databind`
and Mockito.

Files, in a package that is neither `org.fiware.tmforum.common.repository` nor `org.fiware.tmforum.storage.postgresql`,
e.g. `org.fiware.tmforum.decoration`:
- `storage-postgresql/src/test/java/org/fiware/tmforum/decoration/StorageAdapterQualifierTest.java`
- `storage-postgresql/src/test/java/org/fiware/tmforum/decoration/RecordingEntityRepositoryDecorator.java`: `@Singleton @Primary
  @Requires(property = DECORATE_PROPERTY, value = "true")`, `implements EntityRepository`. The constructor takes
  `@StorageAdapter EntityRepository delegate`, every method forwards to it, and a `delegate()` accessor exposes it.
- `.../RecordingReferenceReadRepositoryDecorator.java`: the same for `ReferenceReadRepository`.
- `DECORATE_PROPERTY` is a named constant, e.g. `storage-adapter-qualifier-test.decorate`. The property gate keeps
  the decorators from leaking into the other contexts and ITs of `storage-postgresql`, for example
  `PostgresEntityRepositoryIT`'s `assertInstanceOf(PostgresEntityRepository.class, port)`.
- First check that test sources run the Micronaut annotation processor. Test beans need generated bean definitions.
  If they are not generated, add `micronaut-inject-java` to the test annotation processor path.

Context set-up, copying `OutboxDispatcherSwitchTest`:
`ApplicationContext.builder(environments).overrideConfigLocations("classpath:storage-adapter-qualifier-test/")
.properties(...).singletons(...).start()` in try-with-resources. The non-existent config location keeps the test
`application-postgresql.yaml` and its datasource out.
- PostgreSQL context: register `mock(ConnectionFactory.class)` through `.singletons(...)`. With no r2dbc datasource
  configured, there is no competing bean. Set `OutboxProperties.ENABLED_PROPERTY=false` so no scheduled dispatcher
  touches the mock.
- Broker contexts (`orion-ld`, `scorpio`): set `micronaut.http.services.ngsi.url` to a dummy URL if the declarative
  `EntitiesApiClient` needs one to be created. Adapter constructors do no I/O, and nothing is invoked.

Parameterized cases (`@MethodSource`): environments `orion-ld`, `scorpio`, `postgresql` (use
`PostgresStorage.ENVIRONMENT`, and named constants for the broker names), each × decorate `false|true`. Each case
gives the expected entity adapter class and the expected reference adapter class.
- decorate=false, i.e. resolution unchanged:
  - `getBean(EntityRepository)`, `getBean(EntityReadRepository)`, `getBean(EntityWriteRepository)` and
    `getBean(ReferenceReadRepository)` are instances of the expected adapter.
  - The `@StorageAdapter`-qualified lookup (`Qualifiers.byStereotype(StorageAdapter.class)`) returns the same
    instance.
  - `getBeansOfType(EntityRepository.class)` has exactly one bean.
- decorate=true:
  - Unqualified `EntityRepository`, `EntityReadRepository` and `EntityWriteRepository` resolve to
    `RecordingEntityRepositoryDecorator`. This pins down that the write path also goes through a decorator that
    implements `EntityRepository`.
  - Unqualified `ReferenceReadRepository` resolves to its decorator.
  - Each decorator's `delegate()` is the expected adapter, and is the same instance as the `@StorageAdapter`
    lookup.
  - The qualified lookup has exactly one candidate.
  - The NGSI-LD adapters are proxied by `@Cacheable`, so assert with `assertInstanceOf`, not class equality.

Optionally, add one `postgresql` case asserting that `findBeanDefinition(NgsiLdEntityRepository.class)` is empty.
That documents the new `notEnv` gate.

Acceptance:
- `mvn test -pl storage-postgresql -Dtest=StorageAdapterQualifierTest` is green.
- The full unit suite is green: `mvn test` from the root, or at least
  `-pl storage-api,common,storage-postgresql,product-catalog,all-in-one,data-migrator/postgresql-migrator -am`.
- If K3S is available, run `mvn integration-test -pl storage-postgresql`; `PostgresEntityRepositoryIT` and
  `PostgresReferencesRepositoryIT` assert the adapter type. Also run one broker IT, e.g.
  `mvn integration-test -pl product-catalog -Dit.test=ProductOfferingApiIT -DskipTests`, as a sanity check. If K3S
  cannot run in the container, say so in the completion summary instead of skipping silently.
- Every public test helper method has a terse doc comment, or none where the file style has none (sparse, see
  AGENTS.md).
