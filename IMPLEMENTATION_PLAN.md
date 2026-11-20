# Implementation Plan: TMForum - DB Backend - Data migration

## Overview
This adds a supported, reversible way to move a broker-backed deployment onto PostgreSQL, and a broker-side
inline-reference backfill (decision 10) for deployments that stay on NGSI-LD. All of it is delivered as one new CLI
module, `data-migrator/postgresql-migrator`, with the commands `import`, `backfill-references`, `verify`,
`cleanup-subscriptions`, `replay` and `export`. The module comes with ITs in the existing K3S setup, a runbook and a
release note.

### Design decisions (apply to every step)
- **Read and write through the domain model, not the raw broker JSON.** The new module depends on `all-in-one`, which
  holds every domain class and both adapters. It starts two `ApplicationContext`s in one JVM:
  - a **source** context in a broker environment (`orion-ld`|`scorpio`, `in-memory`), which reads `EntityVO`s and maps
    them with the library's `EntityVOMapper.fromEntityVO`, as `NgsiLdEntityRepository` does;
  - a **target** context in the `postgresql` environment, which writes through
    `PostgresEntityRepository.importDomainEntity(entity, createdAt, updatedAt)`.

  This reuses the application's own document mapping, so `data` comes out exactly as the app writes it. Reading
  through the mapping library also reverses the `tmfEscaped-` reserved-key prefix (`ReservedWordHandler`), and the
  `TMForumSubscription.setEscapedQuery`/`setEscapedRawQuery` setters URL-decode `query`/`rawQuery`.
  `PostgresMappingModule` ignores the escaped getters, so the plain value is stored.
- **No legacy-loader / `ChildFirstClassLoader`.** The source broker is written by the current mapping library, so
  this direction needs one classloader only. The existing `migrator`/`update-writer` (legacy broker → broker) stay
  untouched.
- **No events.** `importDomainEntity` writes no outbox row (only `createWithEvent` does), so a bulk import fires no
  TMForum notification. The target context sets `tmforum.outbox.enabled=false`.
- **`ForbiddenCharacterEscaper` has a static flag.** Both contexts' `ForbiddenCharacterEscapingConfigurer`s write it.
  After both contexts start, the migrator sets it explicitly to the source's
  `general.escape-forbidden-characters` (CLI option, default `true`). Data written while the flag was `false` is stored
  plain, and decoding it would be lossy (`%`, `+`).
- **Configuration is passed programmatically** (`ApplicationContext.builder().environments(..).properties(..)`), like
  `UpdateWriter`. The module ships no `application*.yaml`: Micronaut 5 fails on duplicate configuration resources,
  and `all-in-one`'s files are already on the classpath.
- **The reference backfill fills absent fields only, and only for the 83 relationship setters that resolved on read
  before ticket 88** (`fromProperties = false` at commit `9570e548`). The other 58 always stored their payload
  inline, so filling them would invent data the client omitted. A field the stored reference already carries is
  never replaced. A missing or external target leaves the reference unchanged.
- **The PostgreSQL backfill is not a Flyway migration.** A Flyway step would run on every PostgreSQL deployment,
  including fresh ones whose references are value copies by design (ticket 88). It runs once, from the migrator,
  after the import.
- Every command takes `--dry-run`, prints a per-type report, and exits non-zero on any failure.

### Human / operational tasks (not automatable here)
The plan delivers the tooling, runbook and an in-cluster rollback rehearsal (Step 9). Humans keep these tasks:
- capturing real API requests for `replay`;
- rehearsing on a production-size dataset;
- the 6-way IT matrix run on CI;
- switching the backend selector, which belongs to ticket 92.

## Steps

### Step 1: `postgresql-migrator` module, dual-context bootstrap and entity-type registry
- **Module.** Add `data-migrator/postgresql-migrator` to `data-migrator/pom.xml`:
  - packaging `jar`, main class `org.fiware.tmforum.migration.postgresql.Application`;
  - picocli (same version as `migrator`) with a top-level command and empty subcommand stubs;
  - depends on `org.fiware:all-in-one` (`${project.version}`) and `storage-postgresql`;
  - surefire, failsafe and `k3s-maven-plugin` like `product-catalog/pom.xml`;
  - a jib image `tmforum-postgresql-migrator` in the `oci` profile.
- **`MigrationContexts`.** Builds and closes the source and target contexts, with these CLI options:
  - `--broker-url`, `--broker-type orion-ld|scorpio`, `--tenant`, `--source-escaped` (default `true`);
  - `--pg-host/--pg-port/--pg-db/--pg-user/--pg-password`, defaulting to the `POSTGRES_*` env vars of
    `storage-postgresql/README.md`.

  Fixed properties:
  - outbox off;
  - `jackson.serialization-inclusion=non_null` and `write-dates-as-timestamps=false`, as `UpdateWriter` sets them;
  - `mapping.strictRelationships=false`.

  Flyway runs in the target context, so the schema exists. It then applies the escaper rule from the Overview.
- **`EntityTypeRegistry`.** Maps each stored NGSI-LD type to its domain class. It scans `org.fiware.tmforum` for
  `@MappingEnabled(entityType=...)` with classgraph (already used by `update-writer`). An ambiguous type fails fast
  unless it has an explicit override entry. It also reports which source types it cannot map (e.g. NGSI-LD
  `Subscription`).
- **Tests (unit, parameterized):**
  - the registry resolves every `VersionedEntity.VERSIONED_TYPES` entry and every `TYPE_*` constant of the
    `@MappingEnabled` classes;
  - an ambiguous type fails;
  - the contexts start with the expected environments and properties (no network: verify the built configuration).
- **Acceptance.** `mvn -pl data-migrator/postgresql-migrator -am package -DskipITs` builds and the unit tests pass. No
  existing module changes behaviour.

### Step 2: `import` command — broker → PostgreSQL raw import
- **`ImportCommand` / `BrokerSource`.** For each registry type (or `--types`), the command:
  1. pages through `EntitiesApiClient.queryEntities` with `options=sysAttrs` (`--page-size`, default 100, below
     Orion-LD's 1000 limit);
  2. maps every `EntityVO` with `EntityVOMapper.fromEntityVO`;
  3. writes it with `importDomainEntity(entity, createdAt, modifiedAt)`, keeping the broker's creation order, which
     is the PostgreSQL default sort (`created_at, id`).
- **Q6 seed.** A `VersionedEntity` with a blank `version` gets `"0.0.1"` before the write, so both the document and
  the `version` column carry it. Every other type keeps `version = ''`, `revision = 1`, `is_current = true` (column
  defaults).
- **Re-runs.**
  - A non-empty target for a requested type fails, unless `--resume` is set.
  - With `--resume`, a `StorageException` with reason `CONFLICT` counts as `skipped`.
  - A mapping failure is logged with the id and counted as `failed`. It does not abort the run unless `--fail-fast`
    is set.
- **Report.** Per type: read / written / skipped / failed / seeded-version. Unmappable broker types are listed.
- **Tests:**
  - Unit tests with a mocked source and writer, parameterized:
    - version seeding (versioned without version, versioned with version, unversioned);
    - conflict handling with and without `--resume`;
    - timestamp pass-through;
    - paging termination.
  - IT `ImportIT` (tagged so it is skipped on `-Dbroker=postgresql`). Seeds the broker through the source context's
    `EntityRepository`, plus raw `EntityVO`s for these cases:
    - a `TMForumSubscription` whose escaped `query`/`rawQuery` contain `=`, `;`, `(`, `'`;
    - a nested object carrying a reserved key (`valueType`, nested `id`);
    - a pre-ticket-88 id-only relationship;
    - a versionable entity without a version.

    It then runs `import` and asserts:
    - rows, `version` and timestamps;
    - the unescaped values;
    - no `tmfEscaped-` key;
    - the id-only reference stored as id-only.
- **Acceptance.** The import reproduces every seeded entity in `tmforum.entity` and fires no outbox row
  (`SELECT count(*) FROM tmforum.event_outbox` = 0).

### Step 3: reference backfill catalog (the 83 formerly-resolving relationships)
- **The catalog.** Check in `ReferenceBackfillCatalog` (a resource `reference-backfill-catalog.json`, loaded into
  records). It has one entry per relationship setter that was not `fromProperties = true` at commit `9570e548`
  (83 setters; re-derive with `git grep "@AttributeSetter.*RELATIONSHIP" 9570e548 -- '*/src/main/java/*.java'`).
  Each entry holds:
  - the owning stored types;
  - the attribute name (NGSI-LD `targetName` = top-level document key);
  - single or list;
  - the reference class;
  - the **fillable fields**: the reference-class fields that the pre-88 read (`BrokerBackedEntitiesRepository` /
    `TolerantEntityVOMapper` at `9570e548`) filled from the target. Each is a `(reference key, target document key)`
    pair. `id` is excluded, and so is anything that describes the target's own JSON-LD typing (`atType`) or that
    pre-88 did not fill.

  Document in the class Javadoc how the field set was derived. Whether `version` is filled follows pre-88 behaviour,
  which is what clients saw. Call this out in the PR for review.
- **`productOrderItem`.** Record the product-inventory `productOrderItem` datasetId rewrite
  (`<productOrderId>` → `<productOrderId>:<orderItemId>`) as a catalog flag that the broker backfill (Step 5) acts on.
- **Guard test** (`ReferenceBackfillCatalogTest`, parameterized over the entries). It runs against the current domain
  classes (`all-in-one` test classpath, like `RelationshipTargets`) and checks:
  - each attribute still exists as a `RELATIONSHIP`/`RELATIONSHIP_LIST` setter;
  - every fillable field exists on the reference class;
  - the entry count is 83.
- **Acceptance.** The catalog is complete, matches the pre-88 code, and is pinned by the guard test.

### Step 4: `backfill-references --target postgresql` — one SQL pass
- **`PostgresReferenceBackfill`** generates, from the catalog, one script that runs in **one transaction**:
  - A `pg_temp` helper `fill_ref(ref jsonb, target jsonb, pairs text[][]) returns jsonb`. It returns `ref` plus each
    target key that is absent from `ref` and non-null in `target`, so `ref` keys always win.
  - Per catalog entry, one `UPDATE tmforum.entity e SET data = jsonb_set(...)`:
    - joined locally to `tmforum.entity t ON t.id = ref->>'id' AND t.is_current`;
    - for lists, the array is rebuilt with `jsonb_agg(... ORDER BY ordinality)`;
    - limited to `e.type = ANY(owning types) AND e.is_current AND e.data ? attribute`;
    - a reference without a local target is kept as is.

  `updated_at` is left unchanged, because this is a migration and not a client write. No outbox row is written.
  `--dry-run` reports, per attribute, how many references would change and how many targets are missing (external
  or deleted), without writing.
- **Command wiring.** `import --backfill-references` runs it after a successful import. The runbook (Step 9) runs it
  explicitly.
- **Tests:**
  - Unit test of the SQL generation for a single and a list entry (stable text, parameters bound, no string
    interpolation of values).
  - `PostgresReferenceBackfillIT` (also runs on `-Dbroker=postgresql`, it only needs the database), parameterized
    over these cases:
    - absent `name` gets filled;
    - a client-sent `name` is kept;
    - a missing target leaves the reference unchanged;
    - a list keeps its order and fills mixed entries;
    - a non-catalog attribute is untouched;
    - a re-run is a no-op (idempotent).
- **Acceptance.** It is one transaction with no per-reference round trip, and never overwrites a stored field.

### Step 5: `backfill-references --target broker` — decision 10, for deployments staying on NGSI-LD
- **`BrokerReferenceBackfill`.** Uses only the source context (no PostgreSQL needed). Per catalog owner type, it pages
  through the entities. Per page, it collects every referenced id and fetches the targets in batches with one
  `queryEntities(id=a,b,c,…)` per batch (no N+1), mapped to domain objects. It then computes the filled references
  with the same fill-absent rule as Step 4, shared as a small Java `ReferenceFiller` that is unit-tested on its own.
  Only entities with a change are written, through the NGSI-LD adapter's `updateDomainEntity`, carrying only the
  changed attributes. The datasetId stays the reference id, so Orion-LD's merge-by-datasetId updates in place.
- **`productOrderItem`.** Reads the raw `EntityVO`, deletes the whole attribute (all instances) and re-appends it with
  datasetId `<productOrderId>:<orderItemId>`. That avoids the duplicate-item problem described in
  `release-notes/RELEASE_NOTES_INLINE_REFERENCES.md`. It is a no-op when the instances already carry the new
  datasetId.
- **Re-runs and dry-run.** `--dry-run` and the report work as in Step 4. Re-runs are idempotent.
- **Tests:**
  - Unit tests of `ReferenceFiller`, parameterized and shared with Step 4's cases.
  - `BrokerReferenceBackfillIT` on orion-ld and scorpio (skipped on postgresql). It seeds pre-88 id-only references
    and a product with two old-format `productOrderItem` instances, runs the backfill, and asserts:
    - the API-level read shows the filled fields;
    - the client-sent fields are unchanged;
    - the product has exactly the two items with the new datasetIds;
    - a second run changes nothing.
- **Acceptance.** A broker-backed deployment that does not migrate reads back backfilled reference payloads.

### Step 6: `verify` command — counts, field-level diff, escape check
- **Counts.** Per type, source `count=true&limit=1` against target
  `SELECT type, count(*) FROM tmforum.entity WHERE is_current GROUP BY type`. Any mismatch fails.
- **Field-level diff.** Takes a random sample per type (`--sample-size`, default 50, `--seed` for reproducibility),
  and **every** entity of types with at most `--full-check-threshold` entities (default 1000). For each, it reads the
  domain object from both contexts and compares their documents (target `PostgresDocumentMapper` serialization). Only
  the migration's own intended changes are allowed:
  - reference fields absent in the source but filled by the backfill;
  - a seeded `version = "0.0.1"`;
  - the dropped `subscription` attribute after Step 7.

  Every allowed difference is listed separately, and anything else fails.
- **Escape check** (SQL over the whole table):
  - any key starting with `tmfEscaped-` at any depth (recursive `jsonb_each`/`jsonb_array_elements` CTE);
  - any `tm-forum-subscription` `query`/`rawQuery` that still contains an encoded forbidden character (`%3C %3E %22
    %27 %3D %3B %28 %29`, case-insensitive).

  Both must be empty.
- **Output.** A JSON report (`--report <file>`) plus a summary. Exit code `0` only if everything passes.
- **Tests:**
  - Unit tests of the diff normalizer, parameterized over allowed and disallowed differences.
  - `VerifyIT`: after an import, `verify` passes. After a manual SQL corruption (a deleted row, a changed field, a
    re-escaped query, a `tmfEscaped-` key) it fails, naming the cause.
- **Acceptance.** These cover the "counts match", "field-level diff" and "no `tmfEscaped-`/URL-encoded value"
  criteria.

### Step 7: `cleanup-subscriptions` command — broker subscriptions and the `subscription` attribute
- **Broker side.** Deletes the NGSI-LD subscriptions this service registered: the ids held in the `subscription`
  relationship of the source `tm-forum-subscription` entities (`SubscriptionsApiClient.removeSubscription`).
  `--include-orphans` additionally lists, and with confirmation deletes, broker subscriptions whose notification
  endpoint is `<serverHost><basepath>/callback` (`AbstractSubscriptionApiController`, `EventConstants`) but that no
  entity references. A missing subscription (404) counts as already deleted.
- **PostgreSQL side.** `UPDATE tmforum.entity SET data = data - 'subscription' WHERE type = 'tm-forum-subscription'
  AND data ? 'subscription'`, in one transaction. First check whether `PostgresDocumentMapper` writes `subscription`
  at all. If it never does, this half becomes a verified no-op and the report says so.
- **Rollback.** The command refuses to run without `--confirm-cutover-final`. Deleting broker subscriptions breaks
  hub notifications on the broker path, so it belongs after the rollback window (runbook, Step 9). `--dry-run` lists
  what would be deleted.
- **Tests.** `CleanupSubscriptionsIT` on orion-ld and scorpio. It creates hubs through the source context so that
  broker subscriptions exist, imports, runs the cleanup, and asserts that the broker subscriptions are gone, that the
  rows have no `subscription` key, and that the other hub fields are unchanged.
- **Acceptance.** No subscription this service registered remains on the broker. No migrated `TMForumSubscription`
  carries `subscription`.

### Step 8: `export` (PostgreSQL → broker, rollback path) and `replay` (response-diff harness)
- **`export`.** This is the reverse of `import`, used to roll back after the PostgreSQL instance has taken writes. It
  pages through the target per type (`findEntities`), and writes each entity into an **empty** broker through the
  NGSI-LD adapter's `createDomainEntity`. References stay inline, so nothing is lost. A non-empty broker fails unless
  `--resume` is set, in which case a conflict counts as skipped. It reports per type, and `verify` can run against
  the pair in reverse (`verify --direction export`).
- **`replay`.** Input is a JSON-lines file with one captured request per line (`method`, `path`, optional `headers`,
  `body`). The harness sends each request to `--broker-base-url` and `--postgres-base-url`, then diffs status and
  JSON body. A diff ignores:
  - object key order;
  - list order only for set-like endpoints (`--unordered-paths`);
  - configured JSON pointers (`--ignore`). The defaults cover the server-host part of `href` and the reference fields
    of external targets.

  Writes (`POST`/`PATCH`/`DELETE`) are skipped unless `--include-writes`, meant for rehearsal environments only. The
  output is a report file with per-request diffs and a summary exit code. The capture format is documented in
  `data-migrator/README.md`.
- **Tests:**
  - Unit tests of the response diff, parameterized over equal, reordered, ignored and real diffs.
  - A `replay` test against two Micronaut embedded stub servers.
  - `ExportIT` (orion-ld, scorpio): import, then export into the same cluster's broker under a different tenant (or
    after a cleanup of the source), then verify that the reverse diff is empty.
- **Acceptance.** Rollback works both before cutover (switch back, the broker is untouched) and after it (export).

### Step 9: end-to-end rehearsal IT, CI, runbook, docs and release note
- **`MigrationRehearsalIT`** (orion-ld and scorpio, both caches). It starts two in-process `all-in-one` servers on
  random ports, one in the broker environment and one in `postgresql`. Then it:
  1. seeds data through the broker server's API (plus raw pre-88 / escaped records);
  2. runs `import --backfill-references`, `verify`, and `replay` of a checked-in request set
     (`src/test/resources/replay/requests.jsonl`, covering each API module's list and get, filters and pagination);
  3. asserts no unexplained diff;
  4. writes through the PostgreSQL server, runs `export` into a fresh broker, and switches reads back to that broker
     server;
  5. asserts that the replay diff is still empty. This is the rollback rehearsal.
- **CI.** Add the module to `.github/workflows/test-brokers.yaml` for `broker ∈ {orion-ld, scorpio}` × both caches
  (no PostgreSQL-only job, since the migration needs a broker), and its unit tests to the `unit` job.
- **Docs:**
  - `data-migrator/README.md`: a new section for `postgresql-migrator` covering commands, options, the image and the
    capture format. Keep the legacy section.
  - `data-migrator/RUNBOOK_POSTGRESQL.md`: step-by-step migration:
    - freeze writes at the ingress, keeping reads;
    - `import` → `backfill-references` → `verify` → `replay`;
    - switch the selector (ticket 92) and redeploy;
    - the rollback window (switch back, or `export` after writes);
    - after the window, `cleanup-subscriptions`.

    Add a separate short section for broker-only deployments: `backfill-references --target broker`.
  - `release-notes/RELEASE_NOTES_DATA_MIGRATION.md`, with these expected and explained diffs:
    - reference `name`s of external targets;
    - the seeded `0.0.1`;
    - the broker-side backfill.

    Also add a pointer to it from `RELEASE_NOTES_INLINE_REFERENCES.md`.
  - `AGENTS.md`: the module entry and its commands.
- **Acceptance.**
  - `mvn integration-test -pl data-migrator/postgresql-migrator -Dbroker=orion-ld` and `-Dbroker=scorpio` pass.
  - The runbook covers every ticket acceptance criterion.
  - It names the remaining human tasks: production-data rehearsal, real request capture, the 6-way matrix, the
    selector switch.
