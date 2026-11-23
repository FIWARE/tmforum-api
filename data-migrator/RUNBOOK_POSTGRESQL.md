# Runbook: migrating a broker-backed deployment to PostgreSQL

This runbook moves a TMForum deployment from an NGSI-LD broker (Orion-LD or Scorpio) onto the PostgreSQL backend,
keeps a rollback path open for a while, and closes it. Every step uses
[postgresql-migrator](./README.md#postgresql-migrator). Rehearse the whole runbook on a copy of production first.
`MigrationRehearsalIT` runs it in CI on a small dataset.

The commands below assume these shell variables, set for the deployment:

```shell
BROKER="--broker-url http://orion-ld:1026 --broker-type orion-ld"   # add --tenant <t> if general.tenant is set
ESCAPED="--source-escaped true"           # general.escape-forbidden-characters of the broker deployment
export POSTGRES_HOST=... POSTGRES_PORT=5432 POSTGRES_DB=tmforum POSTGRES_USER=... POSTGRES_PASSWORD=...
MIGRATOR="java -jar postgresql-migrator-0.1.jar"   # or: docker run --env POSTGRES_... quay.io/fiware/tmforum-postgresql-migrator
```

Every command takes `--dry-run`. Run each step with it first and read the report. Every command exits non-zero on
any failure, so a script can stop at the first one.

## 0. Prepare

1. Deploy PostgreSQL and the PostgreSQL-backed deployment (environment `postgresql`, see
   [storage-postgresql/README.md](../storage-postgresql/README.md)), but keep the ingress on the broker deployment.
   The PostgreSQL deployment creates the schema on startup. The migrator does too, through Flyway.
2. Capture real API requests for `replay`, in the [capture format](./README.md#replay-capture-format): the list and
   get requests of every API in use, with their filters and paging. Do not put writes in the file unless the replay
   target is a rehearsal environment.
3. Note the broker deployment's notification endpoint, `<general.serverHost><general.basepath>/callback` (one per
   API module for split deployments). The rollback's `export` needs it.

## 1. Freeze writes

Block `POST`, `PATCH`, `PUT` and `DELETE` at the ingress, and keep reads open. The import pages through the broker
without a stable order (Orion-LD rejects `orderBy`, Scorpio ignores it), so its id accounting and `verify`'s counts
are only exact while nothing is written. `verify` is the check for skipped or duplicated pages.

## 2. Import

```shell
$MIGRATOR import $BROKER $ESCAPED
```

- Copies every mapped type into `tmforum.entity`, keeping the broker's `createdAt`/`modifiedAt`.
- Fires no TMForum event: `tmforum.event_outbox` stays empty, so no hub gets notified of the bulk copy.
- Versioned types (catalogs, offerings, specifications, ...) without a `version` get `0.0.1`.
- Decodes the broker's escaping: no `tmfEscaped-` key and no URL-encoded hub query ends up in PostgreSQL.
- A non-empty target fails the type. After an interrupted import, re-run with `--resume`, which skips the stored ids.
  A distinct count below the broker's count fails the type and names the gap; `--resume` picks up the missing ids.

## 3. Backfill references

```shell
$MIGRATOR backfill-references --target postgresql
```

Before inline references, the broker filled `name`, `href`, `version`, ... of 81 relationships from the target on
every read. PostgreSQL stores references as value copies, so this one SQL transaction fills the absent fields of
those references from their local targets once. A field the reference already carries is never replaced, and a
missing or external target leaves the reference as it is. Re-runs change nothing. `import --backfill-references`
runs steps 2 and 3 in one go.

## 4. Verify

```shell
$MIGRATOR verify $BROKER $ESCAPED --report verify-report.json
```

It must exit `0`. It checks:

- **Counts.** The entity count per type matches.
- **Attributes.** Every top-level attribute of every broker entity exists in PostgreSQL.
- **Field-level diff.** All entities of types with at most `--full-check-threshold` (1000) entities, and a sample
  (`--sample-size`, `--seed`) of larger types. The only allowed differences are the backfilled reference fields, the
  seeded `0.0.1` and, after step 9, the dropped `subscription` of hubs. They are listed separately.
- **Escaping.** No `tmfEscaped-` key at any depth, and every hub `query`/`rawQuery` decoded exactly once.

## 5. Replay

Start the PostgreSQL deployment next to the broker deployment, if it is not running yet. Then:

```shell
$MIGRATOR replay --requests captured.jsonl \
    --broker-base-url http://tmforum-broker:8080 --postgres-base-url http://tmforum-postgresql:8080 \
    --unordered-paths '/tmf-api/**' --external-host <references.external-hosts> --report replay-report.json
```

List order is not stable on the broker, so compare lists as sets (`--unordered-paths`). Every difference left in
the report must be one of the expected ones listed in
[RELEASE_NOTES_DATA_MIGRATION.md](../release-notes/RELEASE_NOTES_DATA_MIGRATION.md#expected-differences):
- backfilled reference fields;
- the seeded `0.0.1`;
- reference fields of external targets.

Anything else stops the migration.

## 6. Switch to PostgreSQL

Switch the backend selector to `postgresql` (ticket 92) and redeploy, or point the ingress at the PostgreSQL
deployment. Then lift the write freeze. From here on, PostgreSQL takes the writes, and hubs are notified through its
outbox.

Keep the broker and its NGSI-LD subscriptions as they are: they are the rollback path.

## 7. Rollback window

Choose how long the rollback stays possible, e.g. a few days. Within it:

- **No write on PostgreSQL yet.** Freeze writes, switch the selector back to the broker and redeploy. The broker is
  unchanged, including its hub subscriptions.
- **After writes on PostgreSQL.** Freeze writes, then export PostgreSQL into an empty broker:

  ```shell
  $MIGRATOR export $BROKER $ESCAPED --notification-endpoint <serverHost><basepath>/callback
  $MIGRATOR verify $BROKER $ESCAPED --direction export
  ```

  Then switch the selector back to the broker deployment, redeploy, and lift the freeze.

  - `export` needs a broker that holds no entity of the exported types: a fresh broker, or the old one emptied of
    them.
  - It registers a new NGSI-LD subscription for every hub, notifying `--notification-endpoint`. On the broker path
    that subscription carries the hub's DeleteEvents; create and update events are sent in-process from the hub
    entities. Use
    `--notification-endpoint-for <entityType>=<url>` for deployments split per API module. Hubs are exported last,
    so restoring the other entities fires no notification.
  - A broker that still holds subscriptions notifying that endpoint, or the old subscription of an exported hub,
    makes the export fail before it writes anything, since both would duplicate notifications. Either delete them
    first (on the old broker: `cleanup-subscriptions --dry-run` lists them), or pass `--keep-broker-subscriptions`
    to keep them. Kept subscriptions are live during the export and notify for every exported entity they watch.
  - An interrupted export continues with `--resume`. This implies `--keep-broker-subscriptions`, skips the stored
    ids and never overwrites them. A hub reuses the subscription the interrupted run registered for it only if that
    subscription notifies the same `--notification-endpoint`; with another endpoint, the export fails. Use
    `--resume` only on a broker that holds nothing but an earlier export's output.
  - **Not restored on the broker:** empty lists and empty values that PostgreSQL stores (`[]`, `""`). The NGSI-LD
    mapping omits them on write, so the broker deployment reads them back as absent. `verify --direction export`
    allows this difference.

  A replay between the restored broker deployment and PostgreSQL must come back without differences.

## 8. Broker deployments that stay on NGSI-LD

Skip everything above. To give stored references back the fields the broker used to fill on read, run once:

```shell
$MIGRATOR backfill-references --target broker $BROKER $ESCAPED --replace-on-update <true|false> --dry-run
$MIGRATOR backfill-references --target broker $BROKER $ESCAPED --replace-on-update <true|false>
```

- `--replace-on-update` is the deployment's `general.replace-on-update`: `true` for Scorpio >= 6.0.0, `false` for
  older Scorpio. It is required on Scorpio and must be `false` (or left out) on Orion-LD.
- It also rewrites old `productOrderItem` instances of products to the `<productOrderId>:<orderItemId>` datasetId,
  so that a later PATCH no longer duplicates them.
- Re-runs change nothing.

## 9. End of the rollback window

Once the cutover is final, delete the broker subscriptions of the hubs and drop the hubs' `subscription` attribute in
PostgreSQL:

```shell
$MIGRATOR cleanup-subscriptions $BROKER $ESCAPED --dry-run
$MIGRATOR cleanup-subscriptions $BROKER $ESCAPED --confirm-cutover-final \
    [--include-orphans --callback-endpoint <serverHost><basepath>/callback]
```

This breaks hub DeleteEvent notifications on the broker path, so a rollback after it needs `export`, which
registers new subscriptions. `--include-orphans` also deletes subscriptions that notify a given callback endpoint
but that no hub references. Then decommission the broker deployment.

## What each step guarantees

| Requirement                                          | Where |
|------------------------------------------------------|-------|
| Every entity is copied, with its timestamps          | `import` report, `verify` counts and attributes |
| Stored documents equal what the application writes   | `verify` field-level diff |
| No `tmfEscaped-` key, no URL-encoded hub query       | `verify` escape check |
| No TMForum notification for the bulk copy            | `import` writes no outbox row |
| References carry the fields the broker used to fill  | `backfill-references` (both targets) |
| The API answers the same on both backends            | `replay` |
| Hubs keep notifying after the cutover and a rollback | outbox on PostgreSQL; restored hubs on the broker, with `export --notification-endpoint` for DeleteEvents |
| Rollback before and after writes on PostgreSQL       | step 7 |
| No broker subscription left behind                   | `cleanup-subscriptions` |

## Left to humans

The tooling and `MigrationRehearsalIT` cover the procedure, but not these tasks:

- rehearsing on a production-size copy of the data, including the timing of the write freeze;
- capturing real API requests for `replay`;
- running the 6-way IT matrix on CI (Orion-LD, Scorpio, PostgreSQL × in-memory, Redis), and checking it before
  the cutover;
- switching the backend selector (ticket 92).
