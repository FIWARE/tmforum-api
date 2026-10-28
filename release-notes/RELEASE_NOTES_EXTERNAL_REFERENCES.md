# Release Notes: validation of references to other deployments

## Overview

References (e.g. relationships) can point at entities held by another TMForum deployment. Until now every reference
was resolved against local storage, so an entity carrying such a reference was rejected with `400`. Reference
validation now tells local and external references apart: references to the deployments an operator trusts are
accepted without a local lookup, and the read path returns them instead of failing.

External references are an opt-in: `references.external-hosts` is empty by default, which keeps the previous,
strict behaviour for every reference.

## What changed

* [ReferenceClassifier](../common/src/main/java/org/fiware/tmforum/common/validation/ReferenceClassifier.java)
  classifies each reference as local or external, by the host of its `href` against the operator-configured
  `references.external-hosts`. The configured hosts are logged on startup.
* External references are not resolved remotely (no HTTP call to the other deployment). Their id is looked up in
  local storage only, so a trusted `href` cannot smuggle in a local entity of another type; a reference to no local
  entity is skipped and logged at `info`.
* Reference ids that are not absolute URIs (e.g. `1234`) are rejected with `400` and a warning, instead of being
  handed to the broker, which answered them with an error that surfaced as `500`.
* Every validated reference is counted in the Micrometer counter `tmforum.references` (Prometheus:
  `tmforum_references_total`), tagged with `scope` (`local`, `external`) and `result` (`resolved`, `unresolved`,
  `skipped`).
* The read path tolerates relationship targets that do not exist: they are read back id-only instead of failing the
  `GET`. This also covers a local target that was deleted after the write.
* Validation of a list of references now has explicit `all` semantics: every non-null local reference must exist
  with an accepted type, and an empty, null, all-null or all-external list passes.

See [common/README.md](../common/README.md#local-and-external-references) for the full policy.

## Configuration

| Property                      | Env var                     | Default | Description                                                                                                       |
|-------------------------------|-----------------------------|---------|-------------------------------------------------------------------------------------------------------------------|
| `references.external-hosts`   | `REFERENCES_EXTERNAL_HOSTS` | empty   | Hosts whose entities may be referenced without being validated. Comma-separated host names, without scheme or port. |
| `references.local-hosts`      | `REFERENCES_LOCAL_HOSTS`    | empty   | Further hosts this deployment is reachable under, besides `general.serverHost`. They are never external.            |
| `mapping.strictRelationships` | `MAPPING_STRICT_RELATIONSHIPS` | `false` | Whether an unresolvable relationship target fails the read. See below.                                            |

## Behaviour changes

* **`mapping.strictRelationships` defaults to `false`.** The mapping library defaults it to `true`, and
  `StrictRelationshipsDefault` now sets it to `false` unless a deployment configures it, so the read path tolerates
  the references the write path accepts. A `GET` of an entity whose relationship target no longer exists therefore
  returns `200` with an id-only reference instead of failing. A deployment that sets the flag to `true` keeps the
  previous behaviour and is warned about on startup. Every module's `application.yaml` carries the flag explicitly.
* **The error message of a failed reference check changed** from `Not all references exist. References: <objects>`
  to `References do not exist: <ids>`. It names only the unresolved ids instead of the `toString` of every
  reference. The `400` and its `INVALID_RELATIONSHIP` error details (`At least one of the references does not exist
  - ...`) are unchanged.

## Breaking changes for consumers of `common`

Both are source-incompatible for code outside this repository; nothing inside it is affected.

* `ReferencesRepository` has a new method `Mono<EntityVO> findReference(String id)`, which returns the entity
  regardless of its type. Any other implementation of the interface (e.g. a future PostgreSQL adapter) has to
  provide it.
* The constructor of `ReferenceValidationService` takes a `ReferenceClassifier` and a (nullable) `MeterRegistry`
  next to the `ReferencesRepository`.
