# FIWARE implementation of the TMFORUM-APIs

## Structure

The project is setup as a maven multi-module project, to ease the development of independent implementations for each
API.

Functionality required by most of the implementations is contained in the [common-module](common). The APIs store
their entities through a backend-neutral storage port, [storage-api](storage-api), which has two adapters: the
PostgreSQL adapter in [storage-postgresql](storage-postgresql) (the default) and the NGSI-LD context broker adapter in
[common](common). The broker adapter relies on the [ngsi-ld-java-mapping](https://github.com/wistefan/ngsi-ld-java-mapping)
library for an annotation based mapping between NGSI-LD objects and Java-Pojos. See [Storage backends](#storage-backends)
for how a deployment selects one.

The api-implementations are inside modules, producing jar-files and oci-containers according to the definitions in
the [parent-pom](pom.xml). Since some models are used in multiple apis, there are modules for ```shared-models```. They
should only include models that are used by other modules, too.

The [ui-module](ui) is a read-only browser for the APIs of the all-in-one deployment: it lists the entities and
follows the references between them as hyperlinks. It is an image of its own (`tmforum-ui`), not part of the
all-in-one: run it next to it as a sidecar, or on its own to debug. See [ui/README.md](ui/README.md).

The project also contains 3 non-module folders:

- [api](api) - contains the [OpenApi-Specifications](https://spec.openapis.org/oas/v3.1.0) of the NGSI-LD API used by
  the project. It does not contain the TMForum-API specs, since they are downloaded on built-time.
- [k3s](k3s) - contains kubernetes manifests to be used in the integration-test environments or for setting up a
  dev-environment. Currently includes:
    - [postgresql-db](k3s/postgresql-db): [PostgreSQL](https://www.postgresql.org/) 18, for the PostgreSQL backend
    - [orion-ld](k3s/orion-ld): [Orion-LD Context Broker](https://github.com/FIWARE/context.Orion-LD) with
      [MongoDB](https://www.mongodb.com/)
    - [scorpio](k3s/scorpio): [Scorpio Broker](https://github.com/ScorpioBroker/ScorpioBroker)
    - [postgresql](k3s/postgresql): only the Redis cache, for `-Dbroker=postgresql` runs, which start no broker (the
      broker folders hold their own Redis)
    - [minio](k3s/minio) and [schema-provider](k3s/schema-provider): test dependencies
- [conformance-test](conformance-test) - contains a dockerfile to be used for running the TMForum-Conformance Tests
  inside the K3S setup. Also contains a kubernetes manifest for running the api-implementations.

## Building

The project is built with [Micronaut 5](https://micronaut.io/) and needs JDK 25 (Jackson 3 is used for JSON). Build
all modules with ```mvn clean install -DskipTests -DskipITs -Dk3s.skip=true```. ```-DskipITs``` does not stop the k3s
cluster for the integration-tests from starting, ```-Dk3s.skip=true``` does. The ```oci``` profile builds the container
images in the ```deploy``` phase and pushes them (amd64 and arm64) to ```${image.registry}/${image.repository}```:
```mvn deploy -Poci -DskipTests -DskipITs -Dk3s.skip=true -Dimage.registry=<REGISTRY> -Dimage.tag=<TAG>```. The images ship
their own Java 25 runtime.

## Testing

The current implementation supports multiple layers of testing:

- Unit-Tests, using [JUnit5](https://junit.org/junit5/docs/current/user-guide/) and [Mockito](https://site.mockito.org/)
  to test on a function-level
- Unit-Tests,
  using [JUnit5](https://junit.org/junit5/docs/current/user-guide/),[Micronaut-Test](https://micronaut-projects.github.io/micronaut-test/latest/guide/)
  and [Mockito](https://site.mockito.org/) to test on component level(e.g. running parts of the application, while
  mocking all external dependencies)
- Integration-Tests,
  using [JUnit5](https://junit.org/junit5/docs/current/user-guide/),[Micronaut-Test](https://micronaut-projects.github.io/micronaut-test/latest/guide/)
  and [k3s](https://k3s.io/) to test the application with its real external dependencies running
- Conformance-Test, using the CTK-Implementations provided by TMForum and [k3s](https://k3s.io/) to test conformance of
  the implementations

In order to reduce the overhead of test implementation, the OpenAPI-Generator provides the following test-helpers:

- Test-Interface: an interface, defining a test for all endpoint-response combinations possible from the OpenAPI-spec -
  see [OrganizationApiIT](party/src/test/java/org.fiware.tmforum.party/OrganizationApiIT.java) as an example
- Test-Examples: for each model generated(f.e. ```Organization```), a test example builder is generated, that provides
  pre-filled(from the spec-examples and default values) test-objects -
  see   [OrganizationApiIT](party/src/test/java/org.fiware.tmforum.party/OrganizationApiIT.java) as an example

### Running unit-tests

The unit-tests are integrated into
the [maven-lifecycle's](https://maven.apache.org/guides/introduction/introduction-to-the-lifecycle.html)
```test-phase```
via the [surfire-plugin](https://maven.apache.org/surefire/maven-surefire-plugin/). They can be executed
via ```mvn test```. Since they don't have any external dependencies, they also can be run via the IDEs(only tested
with [IntelliJ](https://www.jetbrains.com/idea/)) integrated test runners. All tests with suffix ```*Test``` will be
executed.

### Running integration-tests

The integration-tests are integrated into
the [maven-lifecycle's](https://maven.apache.org/guides/introduction/introduction-to-the-lifecycle.html)
```integration-test phase```
via the [failsafe-plugin](https://maven.apache.org/surefire/maven-failsafe-plugin/). Since the tests require a running
broker at ```localhost:1026```, the [k3s-plugin](https://github.com/kokuwaio/k3s-maven-plugin) is integrated in
the ```pre-integration-test``` and ```post-intgeration-test``` phases to spin-up a k3s-cluster inside docker and deploy
the components. The tests can be executed via: ```mvn integration-test```. After running the tests, the k3s-cluster will
be automatically destroyed. All tests with suffix ```*IT``` will be executed.

In order to run the tests from the IDE, you have to setup an environment before that. This can be achieved via
the ```dev```-[profile](https://maven.apache.org/guides/introduction/introduction-to-profiles.html).
With ```mvn install -Pdev```, a k3s-cluster will be setup but not destroyed after running the tests. In order to connect
to the cluster, install the [kubectl-client](https://kubernetes.io/docs/tasks/tools/#kubectl) and
run ```export KUBECONFIG=/tmp/k3s-maven-plugin/mount/kubeconfig.yaml && kubectl get all --all-namespaces```. To clean up
the environment, use ```mvn clean k3s:rm -Pdev```.

### Running conformance-tests

> **Note:** the CTK downloads referenced by `module.ctk.url` currently return 404 for every module, so the profile
> fails until TMForum publishes them again or the URLs are updated.

TMForum does provide conformance-test tooling for the
API-implementations: [Open API Conformance](https://projects.tmforum.org/wiki/display/API/Open+API+Conformance)
The tests are integrated into the maven lifecycle with the "conformance-test" profile. To execute them, run "mvn clean
install -Pconformance-test --projects storage-api,common,resource-shared-models,customer-shared-models,service-shared-models,product-shared-models,<THE_MODULE> -DskipTests -DskipITs".
The tests are mounted into a
docker container and executed using them, after the API-implementation and Orion-LD are started. Currently, they need to
run individually for each API-Module, since the tests expect them under the same port on localhost. To integrate a new
module into the tests:

- set find the test url of the module(at
  the [TMForum OpenAPI table](https://projects.tmforum.org/wiki/display/API/Open+API+Table) in the column "CTK")
- insert it to the property <module.ctk.url> in your module
- check the structure of the zip-file, containing the test and set the following properties accordingly(example from the
  parties-api)

```xml

<module.ctk.script-folder>TMF632-Party</module.ctk.script-folder>
<module.ctk.run-script>Mac-Linux-RUNCTK.sh</module.ctk.run-script>
<module.ctk.base-path>/tmf-api/party/v4</module.ctk.base-path>
```

- add the module to the matrix input of the [conformance-test](.github/workflows/conformance-test.yaml) workflow

## Dev-Environment

To get a dev-environment, the [k3s-plugin](https://github.com/kokuwaio/k3s-maven-plugin) can be
used: ```mvn install -Pdev```
After running this, the context broker is available at ```http://localhost:1026``` and PostgreSQL at
```localhost:5432``` (database, user and password `tmforum`). ```-Dbroker=scorpio``` deploys Scorpio instead of
Orion-LD.
See the logs via:

```
  export KUBECONFIG=$(pwd)/target/k3s.yaml && kubectl get all --all-namespaces
  kubectl logs <BROKER_POD>
```

A module started locally without a backend environment runs on PostgreSQL, the [default](#selecting-a-backend), and
uses the dev database with its shipped settings. The startup check does not complain, because the NGSI-LD URL is the
shipped default. To use the dev broker instead, start the module with `MICRONAUT_ENVIRONMENTS=orion-ld,in-memory`
(`scorpio,in-memory` after `mvn install -Pdev -Dbroker=scorpio`).

## Storage backends

Each module stores its entities in exactly one backend, selected through the Micronaut environment:

| Environment  | Backend                          |
|--------------|----------------------------------|
| `postgresql` | PostgreSQL 18 (default)          |
| `orion-ld`   | Orion-LD NGSI-LD context broker  |
| `scorpio`    | Scorpio NGSI-LD context broker   |

### Selecting a backend

Name the backend next to the cache (`in-memory` or `redis`) in `MICRONAUT_ENVIRONMENTS`:

```shell
MICRONAUT_ENVIRONMENTS=postgresql,redis   # PostgreSQL
MICRONAUT_ENVIRONMENTS=orion-ld,redis     # Orion-LD
MICRONAUT_ENVIRONMENTS=scorpio,redis      # Scorpio
MICRONAUT_ENVIRONMENTS=redis              # no backend named: PostgreSQL
```

When neither `MICRONAUT_ENVIRONMENTS` nor the system property `micronaut.environments` names a backend, the
application adds `postgresql`. Tests and the [data-migrator](data-migrator) always name their backend and get no
default. Startup fails, naming the offending setting, when:

- more than one backend is named, e.g. `MICRONAUT_ENVIRONMENTS=orion-ld,postgresql`;
- no backend is named, but broker settings are configured: `micronaut.http.services.ngsi.url`
  (`MICRONAUT_HTTP_SERVICES_NGSI_URL`) differs from the shipped `http://localhost:1026`, or one of the
  [broker-only `general.*` keys](#ngsi-ld-broker) is set. Name the broker to keep it, or name `postgresql` to confirm
  PostgreSQL.

With `postgresql` named explicitly, configured broker-only `general.*` keys are ignored and logged as a warning.

> **Broker deployments must name their broker.** A deployment without a backend environment used to run on the
> broker and now runs on PostgreSQL. Add `orion-ld` or `scorpio` to `MICRONAUT_ENVIRONMENTS`.
>
> Naming the broker also loads the module's `application-<broker>.yaml`, which such a deployment did not load
> before, and changes the effective broker dialect: `ngsildOrQueryKey` goes from `,` to `|`, `encloseQuery` and
> `includeAttributeInList` go from `false` to `true`, and on `scorpio` `replaceOnUpdate` goes from `false` to `true`.
> Each of them can be overridden through its `GENERAL_*` variable. Scorpio < 6.0.0 does not create attributes with
> `replaceOnUpdate: true` and must set `GENERAL_REPLACE_ON_UPDATE=false`, see
> [Attribute updates](#attribute-updates-replaceonupdate).
>
> `document-management` is the exception: it ships no `application-orion-ld.yaml` or `application-scorpio.yaml`, so
> naming a broker changes nothing there and it keeps the code defaults, including `replaceOnUpdate: false` on
> `scorpio`.

### PostgreSQL

The default backend; it needs no context broker. Every module ships an `application-postgresql.yaml`. To set it up:

1. Point the module at the database with `POSTGRES_HOST`, `POSTGRES_PORT`, `POSTGRES_DB`, `POSTGRES_USER` and
   `POSTGRES_PASSWORD`, and bound the connection pool with the `POSTGRES_POOL_*` variables.
2. Give the user the right to create the schema `tmforum`, or create it beforehand.
3. Start the module. Flyway creates `tmforum.entity`, `tmforum.event_outbox` and Flyway's schema history table on the first start.

Readiness then checks the database instead of the broker. The full configuration block, the defaults and the
`tmforum.outbox.*` properties are in [storage-postgresql/README.md](storage-postgresql/README.md#configuration). To
move the data of a broker deployment, see [data-migrator/RUNBOOK_POSTGRESQL.md](data-migrator/RUNBOOK_POSTGRESQL.md).

### NGSI-LD broker

Everything in this subsection applies to the broker backends (`orion-ld`, `scorpio`) only. The broker is reached
through `micronaut.http.services.ngsi.url` (`MICRONAUT_HTTP_SERVICES_NGSI_URL`). Its query dialect is set by the
broker-only `general.*` keys ([NgsiLdBrokerProperties](common/src/main/java/org/fiware/tmforum/common/configuration/NgsiLdBrokerProperties.java)),
each also settable through its `GENERAL_*` variable (e.g. `GENERAL_REPLACE_ON_UPDATE`). The `orion-ld` and `scorpio`
columns are the values of the modules' `application-<broker>.yaml`; `document-management` has neither file and runs
on the code defaults:

| Property                         | Code default           | `orion-ld`             | `scorpio`              | Meaning                                                                                               |
|----------------------------------|------------------------|------------------------|------------------------|-------------------------------------------------------------------------------------------------------|
| `general.tenant`                 | none                   | none                   | none                   | NGSI-LD tenant the APIs use                                                                           |
| `general.ngsildOrQueryValue`     | `,`                    | `,`                    | `,`                    | joins the values of one filter in the NGSI-LD `q` (`status=="a","b"`)                                 |
| `general.ngsildOrQueryKey`       | `,`                    | `\|`                   | `\|`                   | joins OR-ed comparisons: `;` filters, and the values of one filter with `includeAttributeInList`      |
| `general.encloseQuery`           | `false`                | `true`                 | `true`                 | encloses the values of one filter in parentheses                                                      |
| `general.includeAttributeInList` | `false`                | `true`                 | `true`                 | renders each value of one filter as its own comparison (`(status=="a"\|status=="b")`)                 |
| `general.useDotSeperator`        | `true`                 | `true`                 | `true`                 | writes attribute paths as `a.b.c` instead of `a[b][c]`                                                |
| `general.replaceOnUpdate`        | `false`                | `false`                | `true`                 | how a PATCH writes its attributes, see [Attribute updates](#attribute-updates-replaceonupdate)        |
| `general.countHeader`            | `NGSILD-Results-Count` | `NGSILD-Results-Count` | `NGSILD-Results-Count` | broker response header with the total count; without it, lists carry no `X-Total-Count`               |

#### Attribute updates (`replaceOnUpdate`)

A PATCH only writes the attributes it contains. Every other attribute of the entity stays as it is stored, so
concurrent PATCHes of different attributes do not interfere with each other. The property `general.replaceOnUpdate`
controls how the attributes of a PATCH are written:

- `false` (default): the attributes are appended as they are, with `POST /entities/{id}/attrs`.
- `true`: each attribute of the PATCH replaces the stored one as a whole:
  - a list ends up with exactly the items of the PATCH, items that are no longer part of it are removed
  - an empty list removes the attribute
  - an object value is replaced, not merged

  To do so, the API reads the stored entity and sends a single `PATCH /entities/{id}/attrs` with the new attribute
  instances and an NGSI-LD Null (`urn:ngsi-ld:null`) for every stored instance (identified by its `datasetId`) that
  is not part of the update. A PATCH therefore results in one read and one write at the broker, independent of the
  number of attributes or list items. If the broker does not write all attributes, the PATCH is answered with `400`.
  Two concurrent PATCHes that replace the same list can end with the items of both.

| Broker | `replaceOnUpdate` | Behaviour |
|--------|-------------------|-----------|
| **Orion-LD** | `false` (default) | Orion-LD neither creates attributes with `PATCH /attrs` nor accepts the NGSI-LD Null to delete a single attribute instance. Attributes and list items of the PATCH are written, but list items that are no longer part of the PATCH stay stored, and empty lists are not supported. |
| **Scorpio < 6.0.0** | `false` (default) | Appending replaces array attributes in place. Scorpio < 6.0.0 does not create attributes with `PATCH /attrs`, so with the `scorpio` profile, which sets `true`, it has to be set back to `false` (e.g. `GENERAL_REPLACE_ON_UPDATE=false`). |
| **Scorpio >= 6.0.0** | `true` | Required. Appending keeps the existing items of array attributes. Each module's `application-scorpio.yaml` already sets it. |

```yaml
# application-scorpio.yaml (already included in each module)
general:
  replaceOnUpdate: true
```

### Accepted deviations

This table is the single place where accepted differences between the backends are recorded.

| Area | PostgreSQL | NGSI-LD broker | Decision |
|------|------------|----------------|----------|
| TMForum event delivery | At-least-once: create, update and delete write their event into `tmforum.event_outbox` in the entity's transaction, and the `OutboxDispatcher` retries with the same `eventId` until every hub answered 2xx or `tmforum.outbox.retention` (default `7d`) expires, then discards the row (see [TMForum events](release-notes/RELEASE_NOTES_POSTGRESQL_ADAPTER.md#tmforum-events)) | Fire-and-forget: a failed delivery is logged and lost | Accepted (decision 13) |
| NGSI-LD subscriptions and `POST /callback` | Not wired, `POST /callback` answers `404`; TMForum hubs (`POST /hub`) work | Wired, the broker notifies `POST /callback` | Accepted, broker-only (decision 2 as amended) |
| `X-Total-Count` and the `last` link | Always present: the total is counted with the page | Present only when the broker sends the `general.countHeader` header (`NGSILD-Results-Count`); otherwise `next` is inferred from a full page | Accepted |
| `NULLS` ordering on `sort` | An entity without the sort key comes last, ascending and descending (`NULLS LAST`); ties break by creation and id | The brokers do not sort at all, see [`SORT_IS_IGNORED`](#known-backend-differences-not-accepted) | Accepted for PostgreSQL; the broker side is pending |
| Entity versioning | `version` column filled from the entity, `revision` stays `1`, `is_current` stays `true`; no history is kept | No version column; no history is kept | Accepted (decision 11), see [RELEASE_NOTES_ENTITY_VERSIONING.md](release-notes/RELEASE_NOTES_ENTITY_VERSIONING.md). A pinned `*Ref` `version` is checked against the target's current version on both backends |

### Known backend differences, not accepted

Each entry of [KnownDeviation](storage-api/src/test/java/org/fiware/tmforum/storage/contract/KnownDeviation.java) is a
difference the port contract tests pin with today's behaviour, so a fix shows up as a failing test. They are pending
acceptance in ticket 0; an entry moves to the [accepted table](#accepted-deviations) once it is accepted.

- `EMPTIED_LIST_BREAKS_READS`: Orion-LD in append mode (`replaceOnUpdate: false`); an update that empties a list
  leaves the entity unreadable.
- `EMPTIED_EXTENSION_LIST_READS_BACK_EXPANDED`: Orion-LD 1.9.0 in append mode; an emptied extension list reads back
  as a map from the expanded attribute name to `[]`.
- `EXTENSION_OVERRIDES_DECLARED_PROPERTY`: Orion-LD 1.9.0 and Scorpio 5.0.3; an extension property named like a
  declared one overwrites it.
- `REFERENCE_LIST_UPDATE_MERGES`: Orion-LD in append mode; an update of a reference list keeps the stored references
  it does not carry.
- `SEPARATOR_IN_LITERAL_NEVER_MATCHES`: Scorpio 5.0.3; an equality literal holding `&` or `;` matches nothing, one
  holding `,` fails the list.
- `MULTI_ELEMENT_LIST_NEVER_MATCHES`: Orion-LD 1.9.0; an equality filter on a list only matches a one-element list.
- `ANCHORED_REGEX_NEVER_MATCHES`: Scorpio 5.0.3; a regex anchored with `^` or `$` matches nothing.
- `REGEX_HOLDING_QUERY_SYNTAX_IS_REJECTED`: Orion-LD 1.9.0 and Scorpio 5.0.3; a regex holding any of `|,;()"'<>!`
  answers `400`.
- `STRING_COMPARISON_IS_LINGUISTIC`: Scorpio 5.0.3; `gt`/`lt` on strings follow a linguistic collation, not byte
  order.
- `SORT_IS_IGNORED`: Scorpio 5.0.3 and Orion-LD 1.9.0; a sorted list comes back unsorted.
- `NUMBER_AND_BOOLEAN_FILTER_FAILS`: Scorpio 5.0.3; a filter combining a number and a boolean comparison answers
  `500`.
- `SEVERAL_IDS_WITH_SEVERAL_TYPES_FAIL`: Orion-LD; a polymorphic lookup with several ids and several types fails.

Outside the port contract, TMForum DeleteEvents differ as well: on PostgreSQL they go only to the hubs taking
`<Type>DeleteEvent` whose query matched the deleted entity, on the broker to every hub taking `<Type>DeleteEvent`,
whatever its query, because the broker's notification carries only the id (see
[TMForum events](release-notes/RELEASE_NOTES_POSTGRESQL_ADAPTER.md#tmforum-events)). Decision 13 covers the delivery
guarantee only, so this difference is pending acceptance as well.

The inline-reference limitations are listed in
[RELEASE_NOTES_INLINE_REFERENCES.md](release-notes/RELEASE_NOTES_INLINE_REFERENCES.md#known-limitations).

## References to other deployments

A reference whose `href` names one of the hosts in `references.external-hosts` (`REFERENCES_EXTERNAL_HOSTS`) is
external and is not validated against local storage. The list is empty by default, so every reference is validated.
This deployment's own hosts (`general.serverHost`, `references.local-hosts`/`REFERENCES_LOCAL_HOSTS`) stay local
even when they are listed. See [common/README.md](common/README.md#local-and-external-references).

## Extension with @schemaLocation

TMForum supports the extension of API entities through the ```@schemaLocation``` property. In order to extend an entity,
the property should contain a valid(and reachable) URL to a [JSON-Schema](https://json-schema.org/), defining the
extension.
When a schema is provided, the entity is validated against it during deserialization on insertion or update.
For example, in order to extend the ```ProductOfferingTerm``` with a policy, the following schema could be used:

```json
{
  "$schema": "https://json-schema.org/draft/2020-12/schema",
  "$id": "http://localhost:3000/ProductOfferingTerm-schema.json",
  "title": "Test Schema - Extension with a policy",
  "description": "Test Schema - Extension with a policy",
  "type": "object",
  "properties": {
    "policy": {
      "type": "object"
    }
  },
  "required": [
    "policy"
  ]
}
```

With that schema, a product offering could be created as following:

```json
{
  "id": "string",
  "href": "string",
  "description": "string",
  "productOfferingTerm": [
    {
      "description": "string",
      "name": "string",
      "validFor": {
        "endDateTime": "1985-04-12T23:20:50.52Z",
        "startDateTime": "1985-04-12T23:20:50.52Z"
      },
      "policy": {
        "the": "policy"
      },
      "@baseType": "ProductOfferingTerm",
      "@schemaLocation": "http://localhost:3000/ProductOfferingTerm-schema.json",
      "@type": "ProductOfferingTermWithPolicy"
    }
  ]
}
```

The API will validate the policy is present and actually an object. Its possible to define in detail the properties of
the policy itself and let them be validated, too.

Notes:

* All schemas need to define there meta-schema. When using the meta-schemas from ```json-schema.org```, only
  ```draft/2020-12/schema```, ```draft/2019-09/schema```, ```draft-04/schema```, ```draft-06/schema``` and
  ```draft-07/schema``` are allowed.
* It is possible to set ```additionalProperties: false``` for the object to be extended. However, in this case the
  schema needs to contain the full definition of the object, including the properties from the baseType. Else, they will
  be considered as additional properties and validation will fail.  

### Caching of the schemas

Schemas are downloaded from the address given in ```@schemaLocation```. To prevent a download on every single request,
they are cached in-memory. The cache is bound by a time-to-live - e.g. changes at the schema provider are picked up
after at most that time - and by the number of cached schemas, to prevent clients from filling up the memory through
arbitrary schema addresses. The least recently used schemas are evicted first and failed downloads are not cached.

| Property                            | Default | Description                                                                                            |
|-------------------------------------|---------|--------------------------------------------------------------------------------------------------------|
| ```general.schema-cache.enabled```     | true    | Enables the caching of downloaded schemas. If disabled, every validation downloads the schema again.     |
| ```general.schema-cache.time-to-live```| 30m     | Time a downloaded schema is kept, before it is downloaded again. A duration of ```0s``` disables caching. |
| ```general.schema-cache.max-size```    | 100     | Maximum number of schemas to be kept in the cache.                                                       |

```yaml
general:
  schema-cache:
    enabled: true
    time-to-live: 30m
    max-size: 100
```

## Release notes

|Version| Notes link                                                                                 |
|------|--------------------------------------------------------------------------------------------|
| 1.5.0 | [Add TMF652, TMF641, TMF730](./release-notes/RELEASE_NOTES_ADD_NEW_TMFORUM_API_MODULES.md) |
| next  | [Selectable storage backend, PostgreSQL by default](./release-notes/RELEASE_NOTES_STORAGE_BACKENDS.md) |
