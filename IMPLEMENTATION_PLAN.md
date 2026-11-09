# Implementation Plan: TMForum - DB Backend - PSQL Adapter

## Overview
Add a PostgreSQL 18 adapter behind the existing `storage-api` port, in a new `storage-postgresql` module: a Flyway
schema (`tmforum.entity` exactly as in the ticket, with inert versioning columns), R2DBC connectivity, Jackson-3 JSONB
persistence of the domain POJOs, CRUD with a single-statement partial-merge update, a one-query reference lookup, a
transactional event outbox, and a health indicator. PostgreSQL is added to the k3s IT harness next to Orion-LD and
Scorpio, and the CI matrix grows to {orion-ld, scorpio, postgresql} x {in-memory, redis}. The NGSI-LD adapter stays
the default here; ticket 9 makes the backend selectable and flips the default.

## Decisions fixed by this plan (apply to every step)
- **Driver ("ticket 3 threading decision"):** R2DBC on the data path (`io.micronaut.r2dbc:micronaut-r2dbc-core`,
  `org.postgresql:r2dbc-postgresql`), so the adapter stays non-blocking behind the `Mono`/`Flux` port. Flyway needs
  JDBC: `io.micronaut.flyway:micronaut-flyway` + `org.flywaydb:flyway-database-postgresql` + `org.postgresql:postgresql`
  run the migrations at startup through `flyway.datasources.default.url/user/password` (check the micronaut-flyway
  "R2DBC" docs for the exact keys; fall back to a JDBC datasource used only by Flyway if needed). No Micronaut Data,
  no ORM. All versions BOM-managed; anything not in the BOM gets a `version.<groupId>.<artifactId>` root property.
- **Transactions:** a small helper over `io.r2dbc.spi.ConnectionFactory` (`Mono.usingWhen` + begin/commit/rollback).
  JSONB is bound as `io.r2dbc.postgresql.codec.Json` with `$n::jsonb`.
- **Module & package:** `storage-postgresql/` (root `<module>` after `common`), package
  `org.fiware.tmforum.storage.postgresql`. Depends on `storage-api` and `common`; `common` never depends on it.
- **Selection:** Micronaut environment `postgresql`. Every PG bean is `@Requires(env = "postgresql")`; the PG
  repository is additionally `@Primary` for `EntityRepository`/`EntityReadRepository`/`EntityWriteRepository`/
  `ReferenceReadRepository`, so `NgsiLdEntityRepository` (also the `SubscriptionRepository`, broker-only, ticket 1)
  stays a bean and the app starts without changes to the NGSI-LD wiring. Name the environment in one constant
  (`PostgresStorage.ENVIRONMENT`).
- **Stored document:** `data` is the domain POJO serialized by Jackson, keys are the domain property names (the names
  `QueryParser` resolves today), so ticket 7 can translate filters to jsonb paths. `id` and `type` columns come from
  `EntityWithId`; the id keeps the `urn:ngsi-ld:<type>:<id>` format. No `EntityVO`, no NGSI-LD types on this path.
- **Interim list support (until ticket 7):** `findEntities`/`findEntitiesPolymorphic` support `ids`, `types`,
  `offset`, `limit` and a total count. A non-null `query` or `orderBy` fails with
  `StorageException(LIST, UNKNOWN, "... not supported by the PostgreSQL backend before ticket 7")`. This covers the
  port contract tests; filtered list endpoints, TMForum hub registration (`AbstractSubscriptionApiController`
  duplicate check) and event subscription lookup (`TMForumEventHandler.getSubscriptions`) need ticket 7. Hub
  registration and deletion also need ticket 1: `AbstractSubscriptionApiController.create` always calls
  `subscriptionRepository.createSubscription`, which creates an NGSI-LD subscription on a broker a PG deployment
  does not have.
- **IT harness:** PostgreSQL is provisioned in every IT run (like minio). `-Dbroker=postgresql` selects PG as the
  storage under test (property name kept so the existing `-Dmicronaut.environments=${cache},${broker}` wiring and CI
  matrix work; ticket 9 may rename it).

## Steps

### Step 1: `storage-postgresql` module, Flyway schema and k3s PostgreSQL
- New module `storage-postgresql/pom.xml` (parent root pom; deps per the decisions above, test deps JUnit 5,
  Mockito, `micronaut-test-junit5`). Add `<module>storage-postgresql</module>` after `common` in the root pom.
- `storage-postgresql/src/main/resources/db/migration/tmforum/V1__entity.sql`: `tmforum.entity` and
  `entity_current_idx` exactly as in the ticket (columns, defaults, `PRIMARY KEY (id, version)`, partial unique
  index). No other index. Flyway owns the schema (`schemas: tmforum`, `create-schemas: true`, history table in
  `tmforum`), so the SQL file does not need `CREATE SCHEMA`.
- `PostgresStorage` (constants: environment name, schema/table names) and a `postgresql` config block documented in
  `storage-postgresql/README.md` (`r2dbc.datasources.default.*`, `flyway.datasources.default.*`, env overrides
  `POSTGRES_HOST/PORT/DB/USER/PASSWORD`).
- k3s: `k3s/postgresql-db/postgresql.yaml` (Deployment + Service, image `postgres:18`, db/user/password `tmforum`,
  port 5432). Root pom: new execution `apply-postgresql` (like `apply-minio`) in the `integration-test` and `dev`
  profiles, plus `<portBinding>5432:5432</portBinding>` in both `prepare-test-cluster` executions.
- `k3s/postgresql/redis.yaml` (copy of `k3s/orion-ld/redis.yaml`): the manifests dir used when `-Dbroker=postgresql`
  (Redis only; PG comes from `postgresql-db`, no broker).
- Test: `storage-postgresql/src/test/.../SchemaMigrationIT.java` (`@MicronautTest(environments = "postgresql")`,
  ITs need the k3s plugin wired in this module's pom like the API modules) asserting the migration ran, the column
  set/defaults, the PK, `entity_current_idx` being unique+partial, and that no other index exists on the table.
- Acceptance: `mvn clean install -pl storage-postgresql -am` and `mvn integration-test -pl storage-postgresql` pass.

### Step 2: JSONB mapping of domain POJOs (Jackson 3) and `\u0000` rejection
- `PostgresMappingModule` (a `JacksonModule` bean, `@Requires(env = "postgresql")`; never a
  `BeanCreatedEventListener`) plus a `PostgresDocumentMapper` that turns a domain object into a jsonb document and
  back for a given class:
  - instantiation through the domain classes' `(String id)` constructor (they have no no-arg constructor and a final
    `type`); `type` is written but ignored on read;
  - unknown/additional properties (`Entity`'s `@UnmappedPropertiesSetter` map) written as top-level keys and read
    back into the map, so extension fields round-trip;
  - references (`RefEntity` and subclasses) stored inline with every field the writer sent (ticket 5 semantics);
  - two write modes: full document (create) and "carried attributes only" (update: null fields omitted, an emptied
    list kept as `[]`).
  Use the application `ObjectMapper` (it carries `TmForumMappingModule`/`FieldCleaningModule`); do not create a
  private mapper unless a module above conflicts, and then document why.
- Not-stored fields (parity with the port contract): the `EntityReadRepository.get` Javadoc makes customer-bill
  `FinancialAccountRef.accountBalance` "not stored yet, reads back absent" part of the port contract, and the ticket
  requires identical response payloads on every backend, so PG drops it too. Add a marker annotation
  `@NotStored` to `storage-api` (Javadoc: not persisted by any adapter until its encoding is decided, see
  `EntityReadRepository.get`), put it on that field, and have `PostgresDocumentMapper` use a mapper derived from
  the application one (`rebuild()` with an annotation introspector that ignores `@NotStored` properties) for the
  stored document only; API serialization, caches and events are unaffected. The NGSI-LD adapter already drops it,
  so this is the contract, not a deviation: `knownDeviations()` stays empty. Test: a round-trip case asserting the
  field reads back absent; `FinancialAccountRefIT` (ported in Step 8) asserts the same on every backend.
- `NullCharacterValidator`: walks the produced `JsonNode`, finds the first string value or key containing `\u0000`
  and throws `StorageException(<op>, INVALID_DATA, "Field '<json.path[2]>' contains the NUL character (\\u0000), which
  cannot be stored.")`. `AbstractApiController.rejectedWrite` already renders INVALID_DATA as 400 with the message.
- Tests (`storage-postgresql/src/test`): parameterized round-trip tests (object -> json -> object equals) over
  representative classes (an `EntityWithId` with lists, nested values, references, additional properties, a TMF730
  sub-type-like class with `atType`/`atBaseType`); update-mode serialization (null omitted, `[]` kept);
  parameterized `\u0000` cases (top-level, nested, in a list, in a key) asserting the reported path.
- Acceptance: unit tests green; no NGSI-LD types referenced in the module except `MappingEnabled`.

### Step 3: PostgreSQL entity repository: get, create, delete, unfiltered list
- `PostgresEntityRepository implements EntityRepository` (`@Singleton @Primary @Requires(env = "postgresql")`):
  - `get`: same id/type-segment rules as `NgsiLdEntityRepository.get` (empty for malformed id or wrong segment,
    `IllegalArgumentException` for a class without `@MappingEnabled`); extract the shared check into a helper in
    `common` (e.g. `IdHelper`) and use it from both adapters. One statement:
    `SELECT data FROM tmforum.entity WHERE id = $1 AND is_current`.
  - `createDomainEntity`: `INSERT (id, type, data)` with `is_current = true` (defaults); unique violation (SQLState
    `23505`) -> CONFLICT; `\u0000` -> INVALID_DATA (Step 2).
  - `deleteDomainEntity`: `DELETE ... WHERE id = $1 AND is_current`, 0 rows -> NOT_FOUND.
  - `findEntities`/`findEntitiesPolymorphic`: `type = ANY($types)`, optional `id = ANY($ids)`,
    `ORDER BY created_at, id LIMIT/OFFSET`, `count(*) OVER ()` as `totalCount`; always emits a page (also empty);
    polymorphic maps each row by its `type` column through `typeToClass`. `query`/`orderBy` -> the interim failure
    defined above. A page with no rows has no window count, so when it comes back empty and `offset > 0` a second
    `SELECT count(*)` with the same predicate supplies `totalCount` (never report 0 for "past the last match": the
    contract accepts only null or the real count, and the total-count header must stay correct). Add a unit/IT
    case for an offset past the last match.
  - `PostgresFailures` (mirrors `BrokerFailures`): connection/timeout errors -> UNAVAILABLE, the rest UNKNOWN, with
    the step's `StorageOperation`.
  - `importDomainEntity(T entity, Instant updatedAt)`: public adapter method (not on the port) for ticket 8, which
    writes `updated_at` (and `created_at`) from the broker's `modifiedAt`.
- Extend `common`'s `StorageBoundaryTest` (and `all-in-one`'s `ControllerStorageBoundaryTest` if it lists adapter
  packages) so nothing outside `org.fiware.tmforum.storage.postgresql` references the PG adapter classes.
- Add `storage-postgresql` as a dependency of `product-catalog` (for the ITs from Step 4 on).
- Tests: unit tests with a mocked `ConnectionFactory` for error mapping (parameterized over exception -> reason);
  `PostgresEntityRepositoryIT` in `storage-postgresql` (or `product-catalog/.../storage/`) covering get/create/
  delete/list against the k3s PostgreSQL, plus `importDomainEntity` keeping the given timestamp.

### Step 4: Partial-merge update and the entity contract IT
- `updateDomainEntity(id, entity)` as one statement:
  `UPDATE tmforum.entity SET data = data || $2::jsonb, updated_at = now() WHERE id = $1 AND is_current RETURNING id`;
  0 rows -> `StorageException(UPDATE, NOT_FOUND)` (never creates). `$2` is the update-mode document from Step 2:
  only carried attributes, each replaces its stored top-level value (per-attribute replace, which covers both
  broker strategies); an emptied list is written as `[]`. `id`/`type` keys are never overwritten (strip them from
  the patch document).
- No `replaceOnUpdate`, no read-merge-write, no entity cache on this path (they stay for the broker adapter).
- `product-catalog/src/test/.../storage/PostgresEntityRepositoryContractIT` extending
  `AbstractEntityRepositoryContractTest` with the same fixtures as `NgsiLdEntityRepositoryContractIT`,
  `@MicronautTest(environments = "postgresql")`, empty `knownDeviations()`. All contract cases pass, including
  `updateWritesOnlyTheCarriedAttributes`, `updateClearsAnEmptiedList`, `updateKeepsUntouchedReferences`.
- Add a PG-only IT: two concurrent updates replacing the same list end with exactly one of the two lists (the #179
  race does not exist here).

### Step 5: One-query reference lookup
- Port: add `Flux<StoredReference> findReferences(Collection<String> ids)` to `ReferenceReadRepository` with a
  `default` that fans out to `findReference` (NGSI-LD behaviour unchanged), Javadoc stating the contract (only
  existing ids are emitted, order unspecified, failures carry READ). Update `storage-api/README.md`.
- `PostgresReferencesRepository` (`@Primary @Requires(env = "postgresql")`): `findReferences` as
  `SELECT id, type FROM tmforum.entity WHERE id = ANY($1) AND is_current`; `referenceExists`/`findReference` on top
  of the same query.
- `ReferenceValidationService.findUnresolved`/`getCheckingMono`: classify references first (`ReferenceClassifier`),
  look up all LOCAL and EXTERNAL-but-resolvable ids of one list with a single `findReferences` call, then apply the
  existing per-reference rules (type check for LOCAL, metrics tags unchanged). Existing unit tests must still pass;
  add cases proving one call per list.
- `AbstractReferenceReadRepositoryContractTest`: add `findReferences` cases;
  `PostgresReferenceReadRepositoryContractIT` in `product-catalog/.../storage/` next to the NGSI-LD one.

### Step 6: Transactional event outbox and dispatcher
- `V2__event_outbox.sql`: `tmforum.event_outbox (id bigint generated always as identity primary key, event_kind
  text not null, entity_id text not null, entity_type text not null, new_state jsonb, old_state jsonb,
  created_at timestamptz not null default now(), attempts int not null default 0, next_attempt_at timestamptz not
  null default now())`. `event_kind` is CREATE, UPDATE or DELETE; `new_state` is null for DELETE, `old_state` null
  for CREATE. `entity_type` (the NGSI-LD type segment, not a class name) scopes which deployment owns a row, see the
  dispatcher. No extra index (decision 12).
- `common`: `EntityEventPublisher` interface:
  `<T> Mono<Void> createWithEvent(T)`, `<T> Mono<T> updateWithEvent(String id, T patch, Class<T>)` (returns the
  new state), `Mono<Void> deleteWithEvent(URI id)`. There is no `oldState` parameter: every implementation reads
  the old state itself, immediately before its write, so both carry the same "state the write replaced".
  - `DirectEntityEventPublisher` (`@Requires(notEnv = "postgresql")`) holds today's controller behaviour: create,
    then fire-and-forget `handleCreateEvent`; update = `get` (old state, served from the entity cache the
    controller's existence check just filled) -> `updateDomainEntity` -> `get` -> fire-and-forget
    `handleUpdateEvent`; delete = `deleteDomainEntity` only, because on the broker DeleteEvents keep coming from
    `NgsiLdEventHandler` via the NGSI-LD subscriptions (firing one here would duplicate them).
  - `AbstractApiController.create`/`patch`/`delete` call the publisher instead of `repository` + `eventHandler`.
    `patch` keeps its existence check (404 before reference validation); `relatedEntityHandlingMono` keeps calling
    the repository (no events, as today).
- `storage-postgresql`: `OutboxEntityEventPublisher` uses new transactional methods on `PostgresEntityRepository`
  that, in one transaction, write the entity and insert the outbox row (`create`: new_state = written document;
  `update`: `SELECT data ... FOR UPDATE` as old_state, merge as in Step 4, `RETURNING data` as new_state;
  `delete`: `DELETE ... RETURNING type, data` as old_state, 0 rows -> NOT_FOUND and no outbox row). Rollback of
  either statement rolls back both. Write the outbox row only when the entity's type is in the app's
  `EventTypes` (below), the set the dispatcher claims; any other type (e.g. all of party-role, whose
  `PartyRoleEventMapper` maps nothing, and `TMForumSubscription`) gets no row, matching today's silent no-op, so
  no row is left that no deployment would ever claim.
  On PG the API is the only writer, so API create/patch/delete are every change there is (no broker-originated
  events to replace).
- `NotificationSender`: new `deliverNotifications(List<TMForumNotification>)` returning a `Mono<Void>` that
  completes only when every POST got a 2xx and errors otherwise (one attempt per callback, no resilience4j retry:
  the outbox backoff is the retry, which keeps the lease short). `sendNotifications` keeps its fire-and-forget
  behaviour for the broker path.
- `TMForumEventHandler`: outbox overloads `handleCreateEvent`/`handleUpdateEvent`/`handleDeleteEvent` taking the
  outbox `created_at` as event time and a deterministic event id (`UUID.nameUUIDFromBytes(outboxId + eventType)`),
  so retries carry the same `eventId`. They go through `deliverNotifications`, so their `Mono` fails when any
  delivery fails. `handleDeleteEvent` builds the same `<Type>DeleteEvent` payload `NgsiLdEventHandler` produces
  today (reuse its event construction, do not fork it). The existing signatures keep their behaviour.
- `OutboxDispatcher` (`@Scheduled(fixedDelay = "${tmforum.outbox.poll-interval:1s}")`, config record
  `OutboxProperties` with `poll-interval`, `batch-size`, `lease`, `max-backoff`, all with units):
  - Ownership: the claim only takes rows whose `entity_type` is in `EventTypes` (a `common` singleton holding the
    union of `getEntityClassMapping()` keys of the app's `EventMapper` beans, shared with the publisher above), so
    product-catalog never claims party-catalog rows (all-in-one owns all types). The state documents are
    deserialized into that type's `EventMapping.rawClass()`, the domain class (`NgsiLdEventHandler` and
    `EventHandler.createEvent` use it the same way; `targetClass` is the VO).
  - Transaction boundaries: (1) claim + lease in one short auto-committed statement:
    `UPDATE tmforum.event_outbox SET attempts = attempts + 1, next_attempt_at = now() + $lease WHERE id IN
    (SELECT id FROM tmforum.event_outbox WHERE next_attempt_at <= now() AND entity_type = ANY($types) ORDER BY id
    FOR UPDATE SKIP LOCKED LIMIT $batch) RETURNING *`. No transaction or lock is held during delivery; the lease
    keeps other instances off the row, and a crashed instance's rows become due again when the lease expires.
    (2) deliver through the outbox overloads. (3) on success `DELETE` the row; on failure `UPDATE ...
    next_attempt_at = now() + least(poll-interval * 2^attempts, max-backoff)`. `lease` must exceed the time one
    batch can take (HTTP client read timeout x callbacks per batch); document that in `OutboxProperties`.
  - At-least-once: a row is deleted only after every callback acknowledged it. A partly failed row is redelivered
    to all its subscribers with the same `eventId` (consumers deduplicate on it).
- Note: delivery on PG needs subscription lookup with a query (ticket 7) and hub registration without a broker (tickets 1 and 7); until then rows stay pending and retry.
  Tests therefore cover:
  - unit tests of `DirectEntityEventPublisher` (same events, same order as before, no event on delete);
  - `NotificationSender.deliverNotifications` (all 2xx completes, one failing callback errors, parameterized);
  - the dispatcher with a mocked `TMForumEventHandler`, parameterized over CREATE/UPDATE/DELETE: success deletes
    the row; a failing callback leaves the row with `attempts + 1` and a pushed `next_attempt_at`; the backoff is
    capped;
  - PG ITs: entity + outbox row commit together, and a failing outbox insert leaves no entity row (also for delete);
    two dispatchers with disjoint type sets each claim only their own rows; a leased row is not claimed again
    before the lease expires; create/patch/delete of an unmapped type (a party-role entity) leave no outbox row
    (assert on the table directly, before any test cleanup).

### Step 7: Health indicator, module rollout and storage-neutral IT cleanup
- `PostgresReadinessIndicator` (`@Readiness @Requires(env = "postgresql")`, `SELECT 1` with a configured timeout),
  unless micronaut-r2dbc already ships an equivalent indicator, in which case enable and test that.
  `NgsiLdBrokerReadinessIndicator` gets `@Requires(notEnv = "postgresql")` (no broker in a PG deployment's
  readiness; ticket 1 refines the broker-only path).
- Add the `storage-postgresql` dependency to all 21 API modules and `all-in-one`, and an
  `application-postgresql.yaml` to each module's `src/main/resources` and `src/test/resources` (datasource,
  Flyway, outbox settings; the test one points at `localhost:5432`).
- Verify every module (and `all-in-one`) starts in `postgresql` env without a reachable broker (e.g.
  `JsonLdContextPreloader`, `ForbiddenCharacterEscapingConfigurer`, NGSI-LD subscription beans); gate anything that
  contacts the broker at startup with `@Requires(notEnv = "postgresql")`.
- `common/.../test`: `TestEntityCleaner` interface; `AbstractApiIT.cleanUp` delegates to it. NGSI-LD implementation
  = today's broker query+delete; `PostgresTestEntityCleaner` (main code of `storage-postgresql`, like `common.test`)
  = `DELETE FROM tmforum.entity WHERE type = $1` plus `DELETE FROM tmforum.event_outbox WHERE entity_type = $1`
  (the latter only cleans up rows of mapped types; Step 6's IT proves unmapped types never get one). Keep the
  `AbstractApiIT` constructor signature so the 73 ITs need no changes.
- Tests: health indicator unit test (up/down, parameterized); a startup smoke IT in `all-in-one` with
  `environments = "postgresql"`.

### Step 8: PostgreSQL in the IT matrix, one-query GET, CI and release notes
- `-Dbroker=postgresql`: verify the full IT suite of each module runs against PG. Inventory the ITs that cannot run
  on PG yet and exclude them only in a `broker=postgresql`-activated profile (prefer JUnit 5 tags + failsafe
  `excludedGroups`; a precise name pattern is acceptable): (a) list/filter/sort tests (ticket 7), (b) hub
  registration/deletion and event delivery tests (tickets 1 and 7: both must land before this exclusion is
  lifted), (c) broker-only tests: the NGSI-LD contract ITs and the NGSI-LD subscription ITs. Each exclusion names
  every ticket that has to land before it is lifted. CRUD tests (create/get/patch/delete, references, TMF730 sub-types,
  extensions) must run and pass unchanged. Passing an `EntitiesApiClient` to the `AbstractApiIT` constructor does
  not make an IT broker-only.
- Port the 6 ITs that call `EntitiesApiClient` directly (`ProductOrderItemApiIT`,
  `EntitySpecificationRelationshipIT`, `CustomerBillApiIT`, `AppliedCustomerBillingRateApiIT`,
  `ExtendedAppliedCustomerBillingRateApiIT`, `FinancialAccountRefIT`) to seed through the injected
  `EntityRepository` (`createDomainEntity`), so the read-only resources (CustomerBill, AppliedCustomerBillingRate)
  keep their GET coverage on every backend. Only an assertion that inspects the broker's own representation stays
  in bucket (c), split into its own test method.
- One-query GET: `GetIssuesOneQueryIT` (product-catalog, `environments = "postgresql"`) wrapping the
  `ConnectionFactory` with `io.r2dbc:r2dbc-proxy` (test scope) to count statements; `GET /productOffering/{id}` with
  references issues exactly one statement. Also assert the id format `urn:ngsi-ld:product-offering:<id>` in the
  response.
- CI: `.github/workflows/test-brokers.yaml` matrix `broker: [scorpio, orion-ld, postgresql]` x `cache` (6 jobs),
  `--projects` gains `storage-postgresql`; `parallel-test.yml`/`conformance-test.yaml` build lists gain
  `storage-postgresql` where they list `storage-api`.
- Docs: `release-notes/RELEASE_NOTES_POSTGRESQL_ADAPTER.md` (new adapter and how to enable it; schema; atomic
  PATCH, so the #179 concurrent-merge race no longer happens, callers relying on merged lists must not;
  at-least-once TMForum events with stable `eventId` on PG vs fire-and-forget on the broker (accepted deviation,
  ticket 0/9); DeleteEvents on PG come from the outbox, on the broker from `NgsiLdEventHandler`; `\u0000` rejected
  with 400 and the broker behaviour observed for it; interim limits until ticket 7);
  update `storage-api/README.md` ("Today the only adapter..."), `storage-postgresql/README.md`, and `AGENTS.md`
  (module, `-Dbroker=postgresql`, `k3s/postgresql-db`; unless the plan-mode `[plan-update]` already did it,
  replace the "only place TMForum events are fired" line with:
  API create/patch/delete events go through `EntityEventPublisher` (direct on the broker, outbox on PG);
  broker-originated events, including all DeleteEvents on the broker, come from `NgsiLdEventHandler` via NGSI-LD
  subscriptions created in `AbstractSubscriptionApiController`).
- Acceptance: all 6 matrix combinations green; contract ITs green for NGSI-LD (broker jobs) and PostgreSQL (every
  job).
