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
- `PostgresDocumentMapper` (`@Singleton @Requires(env = "postgresql")`) turns a domain object into a jsonb document
  and back for a given class. It holds a document mapper derived from the application `ObjectMapper` (`rebuild()`,
  so it keeps `TmForumMappingModule`/`FieldCleaningModule`) with `PostgresMappingModule` added. That module is a plain
  Jackson module registered only on the derived mapper, not a `JacksonModule` bean, so API serialization, the Redis
  caches (e.g. `TMForumSubscription` lists) and events keep today's `Entity` serialization in every environment:
  - instantiation through the domain classes' `(String id)` constructor (they have no no-arg constructor and a final
    `type`); `type` is written but ignored on read;
  - unknown/additional properties: `Entity.additionalProperties` is a `List<UnmappedProperty>` (name/value pairs)
    behind the mapping library's `@UnmappedPropertiesGetter/Setter`, which Jackson ignores, so plain Jackson would
    write an `additionalProperties` array. A mix-in cannot add members, and `Entity` has no `Map` getter for
    `@JsonAnyGetter`, so the two directions use different mechanisms. Write: `PostgresMappingModule` registers a
    `ValueSerializerModifier` (Jackson 3's `BeanSerializerModifier`) that, for every `Entity` subclass, removes the
    `additionalProperties` bean property and appends a writer emitting each `UnmappedProperty` as a top-level field.
    Read: a mix-in for `Entity` puts `@JsonIgnore` on `getAdditionalProperties`/`setAdditionalProperties` and
    `@JsonAnySetter` on the existing `addAdditionalProperties(String, Object)`, so each key no declared property
    takes becomes an `UnmappedProperty`. Extension fields round-trip and an update patch replaces them per key like
    any other attribute. Collision rule: a declared property name always wins; the serializer modifier knows the
    class's declared bean properties and skips an extension entry with such a name (whether or not that property
    is null), so the document never has a duplicate key and a read puts the stored value into the declared
    field. No domain class gains a member. Add a contract case to `AbstractEntityRepositoryContractTest`
    (an extension entry named like a declared property; the declared value reads back, the entry is gone), so both
    adapters must agree; if the NGSI-LD adapter differs, its contract IT records it in `knownDeviations()` and
    in ticket 0;
  - references (`RefEntity` and subclasses) stored inline with every field the writer sent (ticket 5 semantics);
  - two write modes: full document (create) and "carried attributes only" (update: null fields omitted, an emptied
    list kept as `[]`).
  Never build a mapper from scratch: the document mapper is always derived from the application one, so a module
  added to the application mapper later reaches the stored document too.
- Not-stored fields (parity with the port contract): the `EntityReadRepository.get` Javadoc makes customer-bill
  `FinancialAccountRef.accountBalance` "not stored yet, reads back absent" part of the port contract, and the ticket
  requires identical response payloads on every backend, so PG drops it too. Add a marker annotation
  `@NotStored` to `storage-api` (Javadoc: not persisted by any adapter until its encoding is decided, see
  `EntityReadRepository.get`), put it on that field, and give the derived document mapper above an annotation
  introspector that ignores `@NotStored` properties; API serialization, caches and events are unaffected. The NGSI-LD adapter already drops it,
  so this is the contract, not a deviation: `knownDeviations()` stays empty. Test: a round-trip case asserting the
  field reads back absent; `FinancialAccountRefIT` (ported in Step 8) asserts the same on every backend.
- `NullCharacterValidator`: walks the produced `JsonNode`, finds the first string value or key containing `\u0000`
  and throws `StorageException(<op>, INVALID_DATA, "Field '<json.path[2]>' contains the NUL character (\\u0000), which
  cannot be stored.")`. `AbstractApiController.rejectedWrite` already renders INVALID_DATA as 400 with the message.
  Every PG write path runs it on the document it writes, with that path's `StorageOperation`: create, update (the
  patch document), `importDomainEntity`, and the transactional outbox create/update of Step 6 (their outbox
  `new_state` is the validated document). PostgreSQL itself rejects `\u0000` in jsonb (SQLState `22P05`);
  `PostgresFailures` (Step 3) maps that SQLState to INVALID_DATA as well, so a path that missed the validator still
  answers 400, not 500, only without the field path.
- Tests (`storage-postgresql/src/test`): parameterized round-trip tests (object -> json -> object equals) over
  representative classes (an `EntityWithId` with lists, nested values, references, additional properties, a TMF730
  sub-type-like class with `atType`/`atBaseType`; additional properties as top-level keys, including one colliding
  with a declared property name); update-mode serialization (null omitted, `[]` kept);
  parameterized `\u0000` cases (top-level, nested, in a list, in a key) x write mode (create, update) asserting the
  reported path and operation.
- Tests also cover that the application `ObjectMapper` in the `postgresql` env still serializes an `Entity`'s
  `additionalProperties` as today (the module is not global).
- Acceptance: unit tests green; no NGSI-LD types referenced in the module except `MappingEnabled` and the mapping
  library's `UnmappedProperty`.

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
  - `PostgresFailures` (mirrors `BrokerFailures`): connection/timeout errors -> UNAVAILABLE, SQLState `22P05`
    (NUL in jsonb) -> INVALID_DATA, the rest UNKNOWN, with the step's `StorageOperation`.
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
  the patch document). The patch document goes through `NullCharacterValidator` (UPDATE) before the statement, so
  a PATCH carrying `\u0000` is a 400; add a PG IT for it next to the create case.
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
    `patch` keeps its existence check (404 before reference validation) and is generalized to
    `<S extends T> Mono<S> patch(String id, S patch, Mono<?> checkingMono, Class<S> entityClass)` so sub-type
    patches can use it.
  - Writers that bypass `AbstractApiController` today. On the broker, only their deletes still produce an event
    (through `NgsiLdEventHandler`; its Create/AttributeValueChange/StateChange checkers always return `false`), so
    on PG the delete must go through the publisher. Routing the sub-type patches through `patch` adds update events
    on both backends:
    - `OrganizationApiController.deleteOrganization` (party-catalog) -> `deleteWithEvent` (keeps its
      `DeletionException` for an invalid id);
    - the sub-type patch paths that call `repository.updateDomainEntity` themselves: resource-inventory
      `ResourceApiController.patchSubTypeResource`, software-management `ResourceApiController` and
      `ResourceSpecificationApiController`, resource-catalog `ResourceSpecifcationApiController` -> the generalized
      `patch` (same existence check, checking mono and re-read, now with the event). Sub-type patches emit no
      update event on either backend today; afterwards they fire the direct update event every other patch already
      fires. This is a broker behaviour change, listed in the Step 8 release notes.
    - `relatedEntityHandlingMono` has no production caller (only `CharacterizationTestController` in `common`'s
      tests); it keeps calling the repository without events, as today. Its Javadoc gets a line saying so.
    - `importDomainEntity` (ticket 8 migration) writes without events by design.
    Step 6 ends with a grep check (in the PR description) that no `src/main` code outside the adapters and
    `EntityEventPublisher` implementations calls `createDomainEntity`/`updateDomainEntity`/`deleteDomainEntity`
    except `relatedEntityHandlingMono`; extend `StorageBoundaryTest` with that rule if it can express it.
- `storage-postgresql`: `OutboxEntityEventPublisher` uses new transactional methods on `PostgresEntityRepository`
  that, in one transaction, write the entity and insert the outbox row (`create`: new_state = the request object
  serialized with the application mapper, not the stored document, so `@NotStored` fields such as `accountBalance`
  are in the CreateEvent as on the broker, whose direct CreateEvent carries the request object; it goes through
  `NullCharacterValidator` like the entity document;
  `update`: `SELECT data ... FOR UPDATE` as old_state, merge as in Step 4, `RETURNING data` as new_state;
  `delete`: `DELETE ... RETURNING type, data` as old_state, 0 rows -> NOT_FOUND and no outbox row). Rollback of
  either statement rolls back both. Write the outbox row only when the entity's type is in the app's
  `EventTypes` (below), the set the dispatcher claims; any other type (e.g. all of party-role, whose
  `PartyRoleEventMapper` maps nothing, and `TMForumSubscription`) gets no row, matching today's silent no-op, so
  no row is left that no deployment would ever claim.
  On PG the API is the only writer, so once the bypassing writers above go through the publisher, the API's
  writes are every change there is (no broker-originated events to replace).
- `NotificationSender`: new `deliverNotifications(List<TMForumNotification>)` returning a `Mono<Void>` that
  completes only when every POST got a 2xx and errors otherwise (one attempt per callback, no resilience4j retry:
  the outbox backoff is the retry, which keeps the lease short). `sendNotifications` keeps its fire-and-forget
  behaviour for the broker path.
- `TMForumEventHandler`: outbox overloads `handleCreateEvent`/`handleUpdateEvent`/`handleDeleteEvent` taking the
  outbox `created_at` as event time and a deterministic event id (`UUID.nameUUIDFromBytes(outboxId + eventType)`),
  so retries carry the same `eventId`. They go through `deliverNotifications`, so their `Mono` fails when any
  delivery fails. The existing signatures keep their behaviour.
- DeleteEvents on PG (decided here; the broker path is not changed):
  - Broker today: `AbstractSubscriptionApiController` registers the NGSI-LD subscription with only `entities` and
    `q`, no event-type filter, and `NgsiLdEventHandler` fires `DeleteNgsiLdEventChecker` for every notification
    with `deletedAt`, so recipients and payload are whatever the broker's deletion notification gives. No IT pins
    either today. Step 6 first adds a broker-only IT (`DeleteEventBrokerIT`, product-catalog, bucket (c) in Step 8)
    that registers hubs with and without `eventType=CatalogDeleteEvent` and with a `q`, deletes a Catalog and
    records which hubs got a DeleteEvent and with which payload fields, on Orion-LD and Scorpio.
  - Recipients on PG: the TMForum-correct rule every Create/Change event already follows on both backends:
    `getSubscriptions` with `eventTypes=<Type>DeleteEvent` (a hub without an `eventType` filter gets the group's
    default event types, which include `DeleteEvent` where `ALLOWED_EVENT_TYPES` allows it), then a new
    `checkers/tmforum/DeleteTmForumEventChecker` evaluating the hub's `query` against `old_state` through
    `SubscriptionQueryResolver.doesQueryMatchCreateEvent` (the deleted entity matched the hub's filter). Where
    the broker IT shows the broker also notifies hubs that excluded DeleteEvents or whose `q` did not match, that
    difference is an accepted deviation (PG follows TMF630), recorded in ticket 0 and the Step 8 release notes.
  - Payload on PG: the same shape as the broker's, so a consumer sees no difference. `handleDeleteEvent` builds it
    through the calls `NgsiLdEventHandler.createNotification` makes after `fromEntityVO`
    (`makeEventDetails`, `eventMapper.mapPayload(entity, rawClass)`, `createEvent` with the fields filter), moved
    into `EventHandler` and shared, not forked. The entity is the domain object with only the fields the broker IT
    shows in the broker's payload (id-only if it carries only id/type), taken from `old_state`. A PG IT asserts
    the payload against the field set the broker IT recorded.
- `OutboxDispatcher` (`@Scheduled(fixedDelay = "${tmforum.outbox.poll-interval:1s}")`, config record
  `OutboxProperties` with `enabled`, `poll-interval`, `batch-size`, `lease`, `max-backoff`, durations with units):
  - On/off switch: the bean is `@Requires(env = "postgresql")` and
    `@Requires(property = "tmforum.outbox.enabled", notEquals = "false")` (on by default). The test
    `application-postgresql.yaml` of every module sets it to `false`, so no scheduled dispatcher races the outbox
    ITs or calls the `@MockBean(TMForumEventHandler)` of the 57 ITs that declare one (its outbox overloads would
    return null). The poll body is a public `dispatchOnce()` (claim, deliver, settle one batch) that the outbox ITs
    call directly on a dispatcher they construct with their own `EventTypes` and handler; the `@Scheduled` method
    only calls it.
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
    next_attempt_at = now() + $delay`. `attempts` is unbounded (rows keep failing until tickets 1 and 7, or for a
    dead callback forever), so the delay must not grow with it: `OutboxBackoff.delay(attempts)` computes it in Java
    as `poll-interval * 2^min(attempts, MAX_BACKOFF_EXPONENT)`, then `min(..., max-backoff)`, where
    `MAX_BACKOFF_EXPONENT` is a named constant small enough that the product cannot overflow `Duration` or a
    PostgreSQL interval (e.g. 20). It is bound as milliseconds (`$delay * interval '1 millisecond'`); no exponent is
    evaluated in SQL. `lease` must exceed the time one batch can take (HTTP client read timeout x callbacks per
    batch); document that in `OutboxProperties`.
  - At-least-once: a row is deleted only after every callback acknowledged it. A partly failed row is redelivered
    to all its subscribers with the same `eventId` (consumers deduplicate on it).
- Note: delivery on PG needs subscription lookup with a query (ticket 7) and hub registration without a broker (tickets 1 and 7); until then rows stay pending and retry.
  Tests therefore cover:
  - unit tests of `DirectEntityEventPublisher` (same events, same order as before, no event on delete);
  - `DeleteTmForumEventChecker`, parameterized over hub queries (none, matching, not matching `old_state`);
    `TMForumEventHandler.handleDeleteEvent` with a mocked `getSubscriptions`: a hub without `<Type>DeleteEvent`
    gets nothing, and the payload has exactly the broker's field set;
  - `NotificationSender.deliverNotifications` (all 2xx completes, one failing callback errors, parameterized);
  - the dispatcher with a mocked `TMForumEventHandler`, parameterized over CREATE/UPDATE/DELETE: success deletes
    the row; a failing callback leaves the row with `attempts + 1` and a pushed `next_attempt_at`;
  - `OutboxBackoff.delay`, parameterized over `attempts` (0, 1, a value just below the cap, and 100, 10_000,
    `Integer.MAX_VALUE`): grows exponentially, then stays at `max-backoff` without overflowing;
  - PG ITs (all with `tmforum.outbox.enabled: false`, dispatchers driven through `dispatchOnce()`): entity + outbox
    row commit together, and a failing outbox insert leaves no entity row (also for delete); two dispatchers with
    disjoint type sets each claim only their own rows; a leased row is not claimed again before the lease expires;
    create/patch/delete of an unmapped type (a party-role entity) leave no outbox row (assert on the table
    directly, before any test cleanup); a sub-type Resource patch (resource-inventory) and an Organization delete
    (party-catalog) each leave exactly one outbox row of the right kind;
  - a unit test that the dispatcher bean is absent with `tmforum.outbox.enabled=false` and present by default.
- `AGENTS.md`: replace the TMForum events line with: API create/patch/delete events go through
  `EntityEventPublisher` (direct on the broker, outbox on PG; `OutboxDispatcher` delivers, off in tests via
  `tmforum.outbox.enabled`); broker-originated events, including all DeleteEvents on the broker, come from
  `NgsiLdEventHandler` via NGSI-LD subscriptions created in `AbstractSubscriptionApiController`.

### Step 7: Health indicator, module rollout and storage-neutral IT cleanup
- `PostgresReadinessIndicator` (`@Readiness @Requires(env = "postgresql")`, `SELECT 1` with a configured timeout),
  unless micronaut-r2dbc already ships an equivalent indicator, in which case enable and test that.
  `NgsiLdBrokerReadinessIndicator` gets `@Requires(notEnv = "postgresql")` (no broker in a PG deployment's
  readiness; ticket 1 refines the broker-only path).
- Add the `storage-postgresql` dependency to all 21 API modules and `all-in-one`, and an
  `application-postgresql.yaml` to each module's `src/main/resources` and `src/test/resources` (datasource,
  Flyway, outbox settings; the test one points at `localhost:5432` and sets `tmforum.outbox.enabled: false`, see
  Step 6).
- Verify every module (and `all-in-one`) starts in `postgresql` env without a reachable broker (e.g.
  `JsonLdContextPreloader`, `ForbiddenCharacterEscapingConfigurer`, NGSI-LD subscription beans); gate anything that
  contacts the broker at startup with `@Requires(notEnv = "postgresql")`.
- `common/.../test`: `TestEntityCleaner` interface; `AbstractApiIT.cleanUp` delegates to it. NGSI-LD implementation
  = today's broker query+delete; `PostgresTestEntityCleaner` (main code of `storage-postgresql`, like `common.test`)
  = `DELETE FROM tmforum.entity WHERE type = $1` plus `DELETE FROM tmforum.event_outbox WHERE entity_type = $1`
  (the latter only cleans up rows of mapped types; Step 6's IT proves unmapped types never get one). Keep the
  `AbstractApiIT` constructor signature so the 73 ITs need no changes. Update the `AbstractApiIT` line in
  `AGENTS.md`: per-test cleanup goes through `TestEntityCleaner` (broker query+delete, or SQL delete on PG).
- Tests: health indicator unit test (up/down, parameterized); a startup smoke IT in `all-in-one` with
  `environments = "postgresql"`.

### Step 8: PostgreSQL in the IT matrix, one-query GET, CI and release notes
- `-Dbroker=postgresql`: verify the full IT suite of each module runs against PG. Inventory the ITs that cannot run
  on PG yet and exclude them only in a `broker=postgresql`-activated profile (prefer JUnit 5 tags + failsafe
  `excludedGroups`; a precise name pattern is acceptable): (a) list/filter/sort tests (ticket 7), (b) hub
  registration/deletion and event delivery tests (tickets 1 and 7: both must land before this exclusion is
  lifted), (c) broker-only tests: the NGSI-LD contract ITs, the NGSI-LD subscription ITs and `DeleteEventBrokerIT` (Step 6). Each exclusion names
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
  ticket 0/9); DeleteEvents on PG come from the outbox, on the broker from `NgsiLdEventHandler`, and the
  recipient difference the Step 6 broker IT found (PG honours `eventType` and the hub's query); TMF730 sub-type
  patches (resource-inventory, software-management, resource-catalog) now fire update events on every backend,
  the broker included (Step 6); `\u0000` rejected
  with 400 and the broker behaviour observed for it; interim limits until ticket 7);
  update `storage-api/README.md` ("Today the only adapter..."), `storage-postgresql/README.md`, and `AGENTS.md`
  (module, `-Dbroker=postgresql`, `k3s/postgresql-db`; the event line and the IT cleanup line are updated in
  Steps 6 and 7).
- Acceptance: all 6 matrix combinations green; contract ITs green for NGSI-LD (broker jobs) and PostgreSQL (every
  job).
