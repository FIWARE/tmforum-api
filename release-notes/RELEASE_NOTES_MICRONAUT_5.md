# Release Notes: Micronaut 5, Java 25 and Jackson 3

## Overview

All TMForum API modules, the all-in-one application and the data-migrator moved from Micronaut 4 to Micronaut 5.
Micronaut 5 requires Java 25 and replaces Jackson 2 with Jackson 3, so the runtime, the images and a few
configuration keys change as well.

| Component                                      | Before                                  | After                                   |
|------------------------------------------------|-----------------------------------------|-----------------------------------------|
| `io.micronaut.platform:micronaut-parent`       | 4.10.18                                 | 5.2.3                                   |
| Java (build and runtime)                       | 21                                      | 25                                      |
| Jackson (`jackson-databind`)                   | 2.x (`com.fasterxml.jackson.databind`)  | 3.x (`tools.jackson.databind`)          |
| `io.github.wistefan:ngsi-ld-java-mapping`      | 1.6.28                                  | 1.6.31 (Micronaut 5, Jackson 3)         |
| `io.kokuwa.micronaut:micronaut-openapi-codegen`| 4.5.0                                   | 4.5.1                                   |
| Service image base                             | `ubi9/openjdk-21-runtime:1.24-2`        | `ubi9/openjdk-25-runtime:1.24-3`        |
| `data-migrator` image base                     | `eclipse-temurin:21-jdk`                | `eclipse-temurin:25-jdk`                |

## Runtime requirements

* The service images ship their own Java 25 runtime
  (`registry.access.redhat.com/ubi9/openjdk-25-runtime:1.24-3`). Nothing changes for deployments that use them.
* If you run the jars outside the images, you need a Java 25 runtime.
* Building from source needs JDK 25. The build was verified with Maven 3.9.

## Configuration changes

These keys changed in every module's `application.yaml`. If you override them in your deployment, through a
mounted `application.yaml`, environment variables or Helm values, change your overrides too.

| Before                                                 | After                                                    | Note                                                                                     |
|--------------------------------------------------------|----------------------------------------------------------|------------------------------------------------------------------------------------------|
| `micronaut.http.services.ngsi.read-timeout: 30`        | `micronaut.http.services.ngsi.read-timeout: 30s`         | The old value disabled the timeout. Broker calls now time out after 30 s, see below.    |
| `micronaut.http.services.read-timeout: 30s`            | removed                                                  | It was never bound. Set the timeout on the `ngsi` service.                              |
| `jackson.serialization.writeDatesAsTimestamps: false`  | `jackson.date-time-features.write-dates-as-timestamps: false` | Jackson 3 moved the feature. Dates are still written as ISO-8601 strings.          |
| -                                                      | `jackson.serialization-inclusion: non_null`              | Replaces code that set this before. Micronaut's `non_empty` default would drop empty lists. |
| -                                                      | `jackson.mapper-features.sort-properties-alphabetically: false` | Keeps the Jackson 2 property order of the responses.                               |
| -                                                      | `jackson.deserialization-features.fail-on-null-for-primitives: false` | Keeps the Jackson 2 behaviour: `null` for a primitive is its default, not a 400. |

**Broker read timeout:** Micronaut 4 and 5 both reject a duration without a unit, such as `30`, and neither
fails at startup. The value is silently dropped, and the `ngsi` client then runs without a read timeout instead of the
10 s default. So far, broker requests had no time limit at all. With `30s`, a broker
request now fails after 30 s without data (read timeout), or after 31 s in total (request timeout). If you override
the value, give it a unit, e.g. `MICRONAUT_HTTP_SERVICES_NGSI_READ_TIMEOUT=30s` or `2m`. A unit-less override such
as `=30` is still accepted without a warning and turns the timeout off again.

The `micronaut.codec.json.additional-types`, `micronaut.caches.*`, `redis.*` (including
`redis.caches.*.value-serializer`), metrics and `endpoints.*` keys are unchanged.

## API behaviour changes

* **Empty lists on create:** a `POST` that contains an empty list, e.g. `"relatedParty": []`, no longer stores that
  attribute. The created entity reads back without the field instead of with `[]`. The mapping library 1.6.31 follows
  NGSI-LD semantics here: on create there is nothing to delete, so an empty list is not sent to the broker.
* **Emptied lists on update:** a `PATCH` that empties a list still removes its content. Orion-LD returns `[]` for
  it, Scorpio leaves the attribute out. Clients should treat a missing list and an empty list as the same.

* **Malformed request bodies:** a body that cannot be read, e.g. a string where an object is expected, still gets a
  `400`. Micronaut 5 words the error body differently: `"message":"Invalid JSON"` and the detail starts with
  `Invalid JSON: `, where Micronaut 4 sent `"message":"Bad Request"` and `Failed to convert argument [...]`.

Apart from that, the wire format of the TMForum responses is unchanged. The `product-catalog` image of release
`1.21.0` and the new image, run against the same Orion-LD, return byte-identical responses for the same entities
(`GET` by id, list, `fields=` and `PATCH`), whichever version wrote them. Entities stored by the previous release are
read without migration.

## data-migrator

* `legacy-loader` is no longer part of the Maven build. It needs Micronaut 3 to load the legacy mapping library
  1.2.11. The `migrator` image builds it from the frozen release tag `1.18.3` (property `legacy.loader.ref` in
  `data-migrator/pom.xml`). See [data-migrator/README.md](../data-migrator/README.md) for building it locally.
* The `-ll` option of the migrator (path to the `legacy-loader` jar) is now required.
* Building the `migrator` image (`mvn deploy -Poci`, or `docker build` of `data-migrator/migrator`) needs network
  access to the tmforum-api Git repository, because the Dockerfile clones the `legacy.loader.ref` tag and builds it
  with Maven on JDK 17. For mirrored or air-gapped builds, point it at a mirror with `-Dlegacy.loader.repo=<url>`
  (Docker build arg `LEGACY_LOADER_REPO`). That Maven build also needs a reachable Maven repository.

## Migration

* Deployments using the published images: check your configuration overrides against the table above.
  Nothing else is needed.
* Clients: accept a missing list wherever an empty list could come back (see above).
* Code that extends this project: Jackson imports move from `com.fasterxml.jackson.databind`/`core` to
  `tools.jackson.databind`/`core`. `com.fasterxml.jackson.annotation` keeps its package. The Jackson 3 `ObjectMapper`
  is immutable, so customizations are registered as `JacksonModule` beans instead of changing the mapper in a
  `BeanCreatedEventListener`.
