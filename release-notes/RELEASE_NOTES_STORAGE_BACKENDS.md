# Release Notes: selectable storage backend, PostgreSQL by default

## Overview

The TMForum APIs no longer depend on an NGSI-LD context broker. Every module stores its entities through a
backend-neutral storage port, which has two adapters: PostgreSQL 18 and the NGSI-LD broker (Orion-LD or Scorpio).
The backend is one setting, the Micronaut environment `postgresql`, `orion-ld` or `scorpio`. PostgreSQL is the
default. This page summarises the whole change and links the detailed notes. How to select and configure a backend
is in the README's [Storage backends](../README.md#storage-backends).

## Upgrading

Read this first if you run a broker deployment.

* **(a) Name your broker.** A deployment that names no backend in `MICRONAUT_ENVIRONMENTS` (or
  `micronaut.environments`) used to run on the broker. It now runs on PostgreSQL. Add `orion-ld` or `scorpio`,
  e.g. `MICRONAUT_ENVIRONMENTS=orion-ld,redis`. If such a deployment points `MICRONAUT_HTTP_SERVICES_NGSI_URL`
  anywhere but the shipped `http://localhost:1026`, or sets a broker-only `general.*` key, startup fails and the
  message says what to add.
* **(b) The `general.*` keys keep their names, but their effective values can change.** A deployment that did not
  name its broker ran on the code defaults. Naming it in (a) also loads the module's `application-<broker>.yaml`,
  which changes:

  | Key                      | Before (code default) | After `orion-ld` | After `scorpio` |
  |--------------------------|-----------------------|------------------|-----------------|
  | `ngsildOrQueryKey`       | `,`                   | `\|`             | `\|`            |
  | `encloseQuery`           | `false`               | `true`           | `true`          |
  | `includeAttributeInList` | `false`               | `true`           | `true`          |
  | `replaceOnUpdate`        | `false`               | `false`          | `true`          |

  Every key can be overridden through its `GENERAL_*` variable (e.g. `GENERAL_ENCLOSE_QUERY=false`). Scorpio < 6.0.0
  does not create attributes with `replaceOnUpdate: true` and must set `GENERAL_REPLACE_ON_UPDATE=false`, see
  [Attribute updates](../README.md#attribute-updates-replaceonupdate). `document-management` ships no broker yaml,
  so nothing changes there.
* **(c) Filter values are split on `,` only.** `QueryParser` no longer splits a filter value a second time on a
  custom `general.ngsildOrQueryValue`. With `ngsildOrQueryValue: "|"`, `status=a|b` used to mean `a` OR `b`. It now
  matches the single value `a|b`. Use `status=a,b`. Every shipped profile sets `,`, so only a custom value is
  affected. The broker still joins the values with `ngsildOrQueryValue` in its `q`.
* **(d) Moving data.** To move an existing broker deployment onto PostgreSQL, follow
  [RUNBOOK_POSTGRESQL.md](../data-migrator/RUNBOOK_POSTGRESQL.md).

## What changed

* **Storage port (ticket 86).** Controllers and services depend only on the interfaces of
  [storage-api](../storage-api/README.md). The NGSI-LD code in `common` is one adapter behind it, and a contract test
  suite runs against every adapter.
* **References to other deployments (ticket 87).** References to the hosts in `references.external-hosts` are
  accepted without a local lookup. The list is empty by default. See
  [RELEASE_NOTES_EXTERNAL_REFERENCES.md](RELEASE_NOTES_EXTERNAL_REFERENCES.md).
* **Inline references (ticket 88).** A reference is stored as the client sent it and read back without fetching
  its target, so a read makes no extra request per reference. See
  [RELEASE_NOTES_INLINE_REFERENCES.md](RELEASE_NOTES_INLINE_REFERENCES.md).
* **PostgreSQL adapter and filter translation (tickets 89, 90).** The new module
  [storage-postgresql](../storage-postgresql/README.md) stores entities as `jsonb` (R2DBC, Flyway), translates
  TMForum filters, `sort` and paging to SQL, and delivers TMForum events at least once through an outbox. See
  [RELEASE_NOTES_POSTGRESQL_ADAPTER.md](RELEASE_NOTES_POSTGRESQL_ADAPTER.md).
* **NGSI-LD notifications are broker-only (ticket 85).** NGSI-LD subscriptions and `POST /callback` exist on the
  broker only. On PostgreSQL `POST /callback` answers `404`, and TMForum hubs (`POST /hub`) work on both backends.
  See [Notifications](RELEASE_NOTES_POSTGRESQL_ADAPTER.md#notifications).
* **Data migration (ticket 91).** The `postgresql-migrator` CLI imports a broker deployment into PostgreSQL,
  verifies it, and backfills inline references for deployments that stay on the broker. See
  [RELEASE_NOTES_DATA_MIGRATION.md](RELEASE_NOTES_DATA_MIGRATION.md).
* **Versioning readiness (ticket 93).** A `*Ref` that carries a `version` must name the target's current version.
  PostgreSQL fills the `version` column, but no history is kept. See
  [RELEASE_NOTES_ENTITY_VERSIONING.md](RELEASE_NOTES_ENTITY_VERSIONING.md).
* **Selectable backend (ticket 92).**
  * **Selector and default:** the Micronaut environment selects the backend. When none is named, the application
    entry point (`TmForumApplication`) adds `postgresql`. Tests and the migrator always name their backend.
  * **Fail fast:** `StorageBackendValidator` stops startup when more than one backend is named, or when no backend
    is named but broker settings are configured. With `postgresql` named, configured broker-only keys are ignored
    with a warning.
  * **Broker-only properties:** the broker dialect keys (`tenant`, `ngsildOrQueryKey`, `ngsildOrQueryValue`,
    `encloseQuery`, `includeAttributeInList`, `useDotSeperator`, `replaceOnUpdate`, `countHeader`) moved to
    `NgsiLdBrokerProperties`, still bound to `general.*`. `general.countHeader` is gone from the base
    `application.yaml`, and the broker yamls keep it.
  * **Docs:** every module's `application*.yaml` says how the backend is selected. The README has the new
    [Storage backends](../README.md#storage-backends) section, and `conformance-test/api.yaml` names `orion-ld`.

## Backend differences

The accepted differences between PostgreSQL and the brokers are recorded in one place, the README's
[Accepted deviations](../README.md#accepted-deviations) table. The differences that are observed but not accepted yet
are listed under [Known backend differences, not accepted](../README.md#known-backend-differences-not-accepted).
