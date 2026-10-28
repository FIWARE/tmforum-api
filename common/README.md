# Common

This module is intended to provide functionality, required by most of the API-implementations.

## Referential integrity

For the api-implementation, it's important to contain [referential-integrity](https://en.wikipedia.org/wiki/Referential_integrity).
To support such checks on a Pojo-level the [ReferenceValidationService](src/main/java/org/fiware/tmforum/common/validation/ReferenceValidationService.java) can be used.
Any entity that implements the [ReferencedEntity](src/main/java/org/fiware/tmforum/common/validation/ReferencedEntity.java) interface, can be handed over to the ReferenceValidationService 
and checked for references.

### Local and external references

A reference can point at an entity held by another TMForum deployment. The [ReferenceClassifier](src/main/java/org/fiware/tmforum/common/validation/ReferenceClassifier.java)
decides per reference, against an operator-configured allow-list:

- **external**: the `href` is an absolute URL whose host is listed in `references.external-hosts` and is none of this
  deployment's own hosts
- **local**: everything else, i.e. no `href`, a relative `href`, a non-hierarchical one (e.g. an `urn:`), one on a
  local host, or one on a host that is not listed

The `href` is client-supplied, so the allow-list is what keeps a client from switching validation off by pointing the
`href` at an arbitrary host. `references.external-hosts` is empty by default, which validates every reference against
local storage, and the classifier logs the configured hosts (or their absence) on startup. With the list, a client can
at most declare a reference as held by a deployment the operator already trusts.

This deployment's own hosts are the host of `general.serverHost` plus `references.local-hosts`, e.g. the public host
behind an ingress. They are never external, so a host that is listed in both is treated as local and logged as a
warning on startup. Hosts are compared case-insensitively and without port or scheme, so entries are plain host names
(no scheme, no port, no path). Blank entries are ignored. An unparsable or scheme-less `general.serverHost` is logged
as a warning and not used, so an href on it is external if its host is listed in `references.external-hosts`.

```yaml
general:
  serverHost: http://tmf.example.org:8632
references:
  externalHosts:
    - partner-a.example.org
  localHosts:
    - tmf.public.example.org
```

The env vars are `GENERAL_SERVER_HOST`, `REFERENCES_EXTERNAL_HOSTS` and `REFERENCES_LOCAL_HOSTS` (comma-separated).
The `href` is read through `ReferencedEntity#getReferenceHref`, which every `ReferencedEntity` with an `href` overrides
(`RefEntity`, `ReferenceValue`, `ProductRefOrValue`, `ProductOfferingQualificationItemRef`, `Resource`,
`ServiceRefOrValue`). An implementation without an `href` is always local.

#### Validation on create and update

- **all semantics**: every non-null local reference must exist with an accepted type. Otherwise the request fails
  with `400` and `INVALID_RELATIONSHIP` (`At least one of the references does not exist - ...`). Empty, null, all-null
  and all-external lists pass.
- **external references are not resolved remotely**: there is no HTTP call to the other deployment, and live
  resolution is out of scope. The id is only looked up in local storage, regardless of type:
  - no local entity: the reference is skipped and logged at `info` (`Skipping validation of external reference <id>.`
    by `ReferenceValidationService`)
  - a local entity with an accepted type: resolved
  - a local entity with any other type: unresolved, `400`. Otherwise a client could reference e.g. a local
    `ProductOffering` as a `category` by adding an href on a trusted host, and the read path would build the
    `CategoryRef` from it.
- **ids must be absolute URIs**: NGSI-LD ids are URIs, local as well as external ones, so an id such as `1234` or a
  reference without an id cannot resolve. It counts as unresolved (`400`) and is logged as a warning, rather than being
  handed to the broker, which would answer it with a `400` that surfaces as a `500`. An external deployment with plain
  ids has to be referenced through a URI (e.g. `urn:ngsi-ld:category:partner-a-1234`).

For an id that does not exist locally, validation only guarantees that it does not point at a local entity of the
wrong type. Everything else is the operator's trust in the hosts of `references.external-hosts`.

Every validated reference is counted in the Micrometer counter `tmforum.references` (Prometheus:
`tmforum_references_total`) with the tags `scope` (`local`, `external`) and `result` (`resolved`, `unresolved`,
`skipped`), when a `MeterRegistry` is present. The scope is the classification, so an external reference whose id
names a local entity counts as `external` `resolved`/`unresolved`. No ids are tagged.

#### Read path

Relationships without `fromProperties = true` (e.g. `ProductOffering.category`) are fetched from local storage on
read. That includes external ones, because the read path only sees ids. A missing target must therefore not fail the
read, which is what the mapping library's `mapping.strictRelationships` would do (its default is `true`). Since the
write path accepts external references in every configuration,
[StrictRelationshipsDefault](src/main/java/org/fiware/tmforum/common/mapping/StrictRelationshipsDefault.java) sets the
flag to `false` unless a deployment configures it explicitly, so a replaced `application.yaml` cannot end up accepting
entities on write that it fails to read back. Every module's main and test `application.yaml` also carries

```yaml
mapping:
  strictRelationships: false
```

A deployment that sets it to `true` is warned about on startup and keeps the library behaviour, which fails the GET of
an entity with an unresolvable relationship target. With the flag `false`:

- a `404` reads back as an id-only reference. This also applies to a local target deleted after the write. A single
  relationship keeps the sub-properties stored on it (e.g. `name`), list entries do not.
- any other broker error fails the GET, so real outages are not hidden.

Fetched relationship target classes therefore need a `String` constructor, which `RelationshipTargetConstructorTest`
(in `all-in-one`) enforces. `RelationshipTargetSerializationTest` next to it keeps that constructor from changing how
the classes are (de)serialized, e.g. in the redis cache, by being picked up as a Jackson delegating creator. Until ticket 5 stores the reference payload inline (`fromProperties = true`), the `href`,
`name` and `@referredType` of an external reference do not round-trip on fetched relationships.
Relationships with `fromProperties = true` (e.g. `Catalog.category`) round-trip fully.
