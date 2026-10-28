# Common

This module is intended to provide functionality, required by most of the API-implementations.

## Referential integrity

For the api-implementation, it's important to contain [referential-integrity](https://en.wikipedia.org/wiki/Referential_integrity).
To support such checks on a Pojo-level the [ReferenceValidationService](src/main/java/org/fiware/tmforum/common/validation/ReferenceValidationService.java) can be used.
Any entity that implements the [ReferencedEntity](src/main/java/org/fiware/tmforum/common/validation/ReferencedEntity.java) interface, can be handed over to the ReferenceValidationService 
and checked for references.

### Local and external references

A reference can point at an entity held by another TMForum deployment. The [ReferenceClassifier](src/main/java/org/fiware/tmforum/common/validation/ReferenceClassifier.java)
decides per reference:

- **external**: the `href` is an absolute URL whose host is not one of this deployment's hosts
- **local**: everything else, i.e. no `href`, a relative `href`, a non-hierarchical one (e.g. an `urn:`) or one on a local host

This deployment's hosts are the host of `general.serverHost` plus `references.local-hosts`, e.g. the public host
behind an ingress. Hosts are compared case-insensitively and without port, so entries are plain host names (no scheme,
no port). Blank entries are ignored. An unparsable or scheme-less `general.serverHost` is logged as a warning and
not used, so every absolute `href` is then external unless its host is listed in `references.local-hosts`.

```yaml
general:
  serverHost: http://tmf.example.org:8632
references:
  localHosts:
    - tmf.public.example.org
```

The env vars are `GENERAL_SERVER_HOST` and `REFERENCES_LOCAL_HOSTS` (comma-separated).
The `href` is read through `ReferencedEntity#getReferenceHref`, which every `ReferencedEntity` with an `href` overrides
(`RefEntity`, `ReferenceValue`, `ProductRefOrValue`, `ProductOfferingQualificationItemRef`, `Resource`,
`ServiceRefOrValue`). An implementation without an `href` is always local.

#### Validation on create and update

- **all semantics**: every non-null local reference must exist with an accepted type. Otherwise the request fails
  with `400` and `INVALID_RELATIONSHIP` (`At least one of the references does not exist - ...`). Empty, null, all-null
  and all-external lists pass.
- **external references are not resolved remotely**: there is no HTTP call to the other deployment, and live
  resolution is out of scope. The id is only looked up in local storage, regardless of type:
  - no local entity: the reference is skipped and debug-logged (`Skipping validation of external reference <id>.`
    by `ReferenceValidationService`)
  - a local entity with an accepted type: resolved
  - a local entity with any other type: unresolved, `400`. Otherwise a client could reference e.g. a local
    `ProductOffering` as a `category` by adding a foreign `href`, and the read path would build the `CategoryRef` from it.

The `href` is client-supplied, so a client can declare any reference external. For an id that does not exist
locally, validation then only guarantees that it does not point at a local entity of the wrong type.

Every validated reference is counted in the Micrometer counter `tmforum.references` (Prometheus:
`tmforum_references_total`) with the tags `scope` (`local`, `external`) and `result` (`resolved`, `unresolved`,
`skipped`), when a `MeterRegistry` is present. The scope is the classification, so an external reference whose id
names a local entity counts as `external` `resolved`/`unresolved`. No ids are tagged.

#### Read path

Relationships without `fromProperties = true` (e.g. `ProductOffering.category`) are fetched from local storage on
read. That includes external ones, because the read path only sees ids. Every module sets

```yaml
mapping:
  strictRelationships: false
```

in its main and test `application.yaml`, so a missing target does not fail the read:

- a `404` reads back as an id-only reference. This also applies to a local target deleted after the write. A single
  relationship keeps the sub-properties stored on it (e.g. `name`), list entries do not.
- any other broker error fails the GET, so real outages are not hidden.

Fetched relationship target classes therefore need a `String` constructor, which `RelationshipTargetConstructorTest`
(in `all-in-one`) enforces. Until ticket 5 stores the reference payload inline (`fromProperties = true`), the `href`,
`name` and `@referredType` of an external reference do not round-trip on fetched relationships.
Relationships with `fromProperties = true` (e.g. `Catalog.category`) round-trip fully.
