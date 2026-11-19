# Release Notes: reference version semantics, PostgreSQL schema ready for versioning

## Overview

A TMForum reference (`*Ref`) can carry the `version` of the entity it points to, but that `version` was never
checked: a `ProductOffering` could point to version `2.0` of a `ProductSpecification` whose only version is `1.0`.
A reference that carries a `version` is now accepted only when it names the target's current version. The API is
unchanged, and Orion-LD, Scorpio and PostgreSQL behave the same.

The PostgreSQL `tmforum.entity` table already had the columns for versioning (`version`, `revision`, `is_current`,
`PRIMARY KEY (id, version)`), but `version` was always `''`. It now holds the entity's TMForum `version` label.

## What changed

* **Reference version rule.** A reference is *pinned* when its `version` is non-null and non-empty. A pinned
  reference resolves only when it equals the target's current version exactly (case-sensitive, no trimming). An
  unpinned reference resolves whatever version the target has.
* **400 on mismatch.** A pinned reference whose target has a different version is rejected like a missing one, on
  create and on `PATCH`: HTTP `400` with `At least one of the references does not exist`. The underlying
  `NonExistentReferenceException` says `Reference version mismatch: <id> (version <pinned>, current <current>)`, and
  the reference counts as `unresolved` in the `tmforum.references` metric.
* **Unversioned targets.** Only types implementing
  [VersionedEntity](../common/src/main/java/org/fiware/tmforum/common/domain/VersionedEntity.java) have a version:
  the 18 versioned TMForum resources (agreement, catalog, category, product offering, product specification, quote,
  resource/service specification, ...) and their subclasses, such as the 7 TMF730 `ResourceSpecification` sub-types.
  Every other type, and a versioned entity created without `version`, has the version `''`. A pinned reference to it
  is rejected. An extension property called `version` on a non-versioned type is ignored.
* **External references.** The rule also applies to an `EXTERNAL` reference whose id names a local entity, so a
  wrong version cannot be smuggled in through an external `href`. An external reference with no local target is
  still skipped, version-agnostic.
* **PostgreSQL `version` column.** It is filled on every write and updated in place by `PATCH`.
  `V3__entity_version_backfill.sql` backfills rows written before this release from their `data`. `revision` stays
  `1` and `is_current` stays `true`. See [storage-postgresql/README.md](../storage-postgresql/README.md#schema).
* **Storage port.** `StoredReference` carries the target's current `version`, filled the same way by the NGSI-LD and
  PostgreSQL adapters. See [storage-api/README.md](../storage-api/README.md#references).

## Unchanged API

No endpoint, query parameter or response field was added. `PATCH` still updates an entity in place, so a `GET`
returns one entity with its latest version, and a reference follows the target's current version: after a `PATCH`
to `2.0`, a reference pinned to `1.0` is rejected and one pinned to `2.0` is accepted.

## Behaviour changes

* A create or `PATCH` that carries a pinned reference to a different or unset target version now fails with `400`.
  Clients that copied an arbitrary `version` into a reference (for example a placeholder like `"string"`) must drop
  it or send the target's current version.

## Not in this release

* Keeping superseded versions: a `PATCH` does not create a new row, `revision` is never incremented and
  `is_current` is never moved. The PostgreSQL table is ready for it, and it would be a PostgreSQL-only extension
  that needs no migration.
* Version history, retrieving a specific version, and version-specific endpoints or query parameters.
* Resolving a pinned reference to a superseded version.
* Versioning on the NGSI-LD brokers (Orion-LD, Scorpio), which keep one entity per id.

## New tests

* `product-catalog` `ReferenceVersionIT`: the rule on create and `PATCH`, a reference following a patched version, a
  pinned reference to a category without version, case sensitivity, and an external reference to a local entity.
* `resource-catalog` `ResourceSpecificationVersionIT`: a `logical-resource-specification` sub-type is versioned.

Both pass on Orion-LD, Scorpio and PostgreSQL.
