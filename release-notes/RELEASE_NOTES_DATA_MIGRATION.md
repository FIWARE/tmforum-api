# Release Notes: data migration from an NGSI-LD broker to PostgreSQL

## Overview

A broker-backed deployment (Orion-LD or Scorpio) can now be moved onto the PostgreSQL backend, and back while the
rollback window is open. The new CLI [postgresql-migrator](../data-migrator/README.md#postgresql-migrator) does the
work, and [RUNBOOK_POSTGRESQL.md](../data-migrator/RUNBOOK_POSTGRESQL.md) is the procedure. The same CLI also backfills
inline references for deployments that stay on the broker. The API modules themselves are unchanged.

## What changed

* **New module `data-migrator/postgresql-migrator`** (image `quay.io/fiware/tmforum-postgresql-migrator`), with
  these commands:
  * `import`: copies every entity from the broker into `tmforum.entity`. It keeps the broker's timestamps, decodes
    the broker's escaping (`tmfEscaped-` keys, URL-encoded hub queries) and writes no outbox row, so no hub is
    notified.
  * `backfill-references --target postgresql|broker`: fills the absent fields of the 81 references that the broker
    resolved on read before references became value copies (see
    [RELEASE_NOTES_INLINE_REFERENCES.md](./RELEASE_NOTES_INLINE_REFERENCES.md)). On the broker, it also rewrites
    old `productOrderItem` instances to the `<productOrderId>:<orderItemId>` datasetId.
  * `verify`: compares counts, attributes, fields and escaping between both stores.
  * `replay`: diffs the API responses of both deployments for captured requests.
  * `export`: the rollback. It copies PostgreSQL back into an empty broker and registers a new NGSI-LD subscription
    per hub.
  * `cleanup-subscriptions`: removes the broker subscriptions of the hubs once the cutover is final.
* **The migrator reuses the application's own mapping.** It runs a broker context and a PostgreSQL context of
  `all-in-one` in one JVM, so the stored documents are exactly what the PostgreSQL deployment writes.
* **The reference backfill is not a Flyway migration.** It runs once, from the migrator. A fresh PostgreSQL
  deployment keeps references as the client sent them.
* **CI** runs the migrator's unit tests, and its ITs on Orion-LD and Scorpio with both caches. The ITs include
  `MigrationRehearsalIT`, an end-to-end rehearsal of the runbook with a rollback.

## Expected differences

These differences between the broker-backed and the PostgreSQL-backed deployment come from the
migration itself or, for the last one, from the backends. `verify` allows the first two and lists them separately;
`verify --direction export` allows the empty values after a rollback.
`replay` reports them like any other difference, so check each one against this list:

* **Backfilled reference fields.** After `backfill-references --target postgresql`, references that the broker
  stored with the id only carry the `name`, `href`, `version`, ... of their target on PostgreSQL. The broker
  deployment reads them back with the id only, since references are no longer resolved on read. After
  `backfill-references --target broker`, the broker reads back the same fields, and the broker's own responses
  change in the same way.
* **The seeded `0.0.1`.** A versioned entity (catalog, category, offering, specification, quote, ...) stored without
  a `version` gets `"version": "0.0.1"` on import, since PostgreSQL keys the versions of an entity by it. The broker
  deployment reads it back without a `version`.
* **Reference fields of external targets.** The backfill fills only from local targets, so a reference to an entity
  on a `references.external-hosts` host keeps exactly the fields it was stored with. If the broker deployment still
  resolved that reference on read (a release before inline references), its `name` and other fields differ. Pass
  the hosts to `replay --external-host`, which then compares only `id` and `href` of such references.
* **Empty values after a rollback.** `export` cannot restore empty lists and empty values that PostgreSQL stores:
  the NGSI-LD mapping omits them on write, so the restored broker deployment reads them back as absent.
  `verify --direction export` allows this.
* **The status of a page past the end.** A list request whose `offset` lies beyond the last match (e.g.
  `?offset=100&limit=10` on 20 entities) returns an empty list on both backends. PostgreSQL still counts the matches
  and answers `206 Partial Content` with `X-Total-Count`. Orion-LD reports no usable total for an empty page, so the
  broker deployment answers `200 OK`. `replay` reports this as `status: broker 200, postgresql 206`. This is a
  backend difference, not a data one.

## Known limitations

* The import pages through the broker without a stable order, so it is only exact while writes are frozen. The
  runbook freezes writes, and `verify`'s counts catch skipped or duplicated pages.
* `replay` compares list order only for paths outside `--unordered-paths`. The broker's list order is not stable,
  so captured list requests normally go into `--unordered-paths`.
* These tasks are left to humans: rehearsing on production-size data, capturing real requests, running the 6-way IT
  matrix on CI, and switching the backend selector (ticket 92).
