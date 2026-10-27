# tmforum-api

## Overview
FIWARE implementation of the TMForum APIs. Each TMForum API is a Micronaut service that stores its resources as NGSI-LD entities in a context broker (Orion-LD or Scorpio).

## Tech Stack
- Language: Java 25 (`jdk.version`/`release.version` in the root `pom.xml`)
- Build: Maven multi-module. The parent is `io.micronaut.platform:micronaut-parent` 5.2.3, and the same version is in the `micronaut.version` property.
- Framework: Micronaut 5, Project Reactor (`Mono`/`Flux` everywhere), Jackson 3 (`tools.jackson.*`, annotations stay `com.fasterxml.jackson.annotation`) through `micronaut-jackson-databind` (no serde), `micronaut-validation`, Lombok, MapStruct.
- NGSI-LD: `io.github.wistefan:ngsi-ld-java-mapping` (1.6.31). It ships `EntitiesApiClient`/`SubscriptionsApiClient` (`org.fiware.ngsi.api`, client id `ngsi`), `EntityVOMapper`, `JavaObjectMapper` and its own Jackson-3 beans (`CacheSerdeableObjectMapper`, `EscapeCleaningJsonFactory`).
- Codegen: `openapi-generator-maven-plugin` 7.20.0 with the `io.kokuwa.micronaut:micronaut-openapi-codegen` 4.5.1 generator (`generatorName=micronaut`).
- Test: JUnit 5, Mockito, Micronaut Test (`@MicronautTest`), K3S through `io.kokuwa.maven:k3s-maven-plugin`.
- Images: jib (`oci` profile), base image `registry.access.redhat.com/ubi9/openjdk-25-runtime`.

## Project Structure
```
pom.xml                    root: versions, codegen config (pluginManagement), profiles oci/dev/conformance-test, k3s IT setup
api/tm-forum/<module>/     OpenAPI specs fed to the generator
common/                    shared base code (all modules depend on it)
  .../common/repository/   TmForumRepository, NgsiLdBaseRepository (@Cacheable etc.)
  .../common/rest/         AbstractApiController and subscription/hub controllers
  .../common/caching/      ClientTrackingRedisCache (extends lettuce RedisCache), EntityIdKeyGenerator
  .../common/filter/       PaginationFilter, ForwardedForFilter (@Filter/HttpServerFilter), BrokerRequestLoggingFilter (HttpClientFilter)
  .../common/exception/    ExceptionHandler implementations
  .../common/health/       readiness/liveness indicators (@Client(id="ngsihealth"))
  .../common/mapping/      UnknownPreservingBase (VO base class), FieldCleaningSerializer and TmForumMappingModule (Jackson 3 modules)
  .../common/validation/   ReferenceValidationService (referential integrity on create/update), ReferencedEntity
  .../common/test/         AbstractApiIT, TestServerHostConfigurer, FirstMatchConfigContextBuilder (test helpers in main code)
*-shared-models/           domain classes shared between API modules
<api-module>/              21 API modules, e.g. product-catalog, party-catalog, customer-management
  src/main/java/.../Application.java   Micronaut.run + @Factory
  src/main/resources/application*.yaml profiles: orion-ld|scorpio, in-memory|redis
all-in-one/                one app containing all APIs (24 codegen executions)
ui/                        read-only browser for all-in-one; Node server + static page, jib-only, no Java
data-migrator/             update-writer, migrator (picocli, separate classloaders); legacy-loader (old mapping lib 1.2.11) is NOT in this build, it is built from the frozen tag in `legacy.loader.ref`
k3s/                       manifests for orion-ld, scorpio, minio, schema-provider used by ITs/dev
.github/workflows/         CI (Java 25; check.yml still on 11)
```

## Build & Test
```bash
mvn clean install -DskipTests -DskipITs                  # full build
mvn test                                                 # unit tests (*Test.java)
mvn test -pl product-catalog -Dtest=ProductOfferingTest  # one unit test class
mvn integration-test -pl product-catalog                 # ITs (*IT.java); starts K3S + broker automatically
mvn integration-test -pl product-catalog -Dit.test=ProductOfferingApiIT -DskipTests
mvn integration-test -Dbroker=scorpio -Dcache=redis      # broker: orion-ld|scorpio, cache: in-memory|redis
mvn install -Pdev                                        # persistent dev cluster, broker on localhost:1026; cleanup: mvn clean k3s:rm -Pdev
mvn install -Poci -DskipTests -DskipITs                  # images (quay.io/fiware/tmforum-<module>)
mvn clean install -Pconformance-test --projects common,mapping,<MODULE> -DskipTests -DskipITs
mvn spotbugs:spotbugs
cd ui && npm run gen-catalog                             # regenerate ui catalog.js after API/resource/TYPE_* changes
```
Use `-pl common -am` or `-pl <module> -am` to build just the parts you need.

## Key Conventions
- Generated code (controllers, `*VO` models, example builders, test interfaces) lives in `target/`. Never edit it. Extend the generated interfaces in `src/main/java`.
- Everything is reactive: repositories and controllers return `Mono`/`Flux`.
- Domain ↔ VO mapping uses MapStruct mappers next to the domain classes. Referential integrity goes through `ReferenceValidationService`, and extension validation through `@schemaLocation`.
- Versions are root-pom properties named `version.<groupId>.<artifactId>`. Micronaut artifacts are BOM-managed and carry no explicit version.
- Jackson 3: the `ObjectMapper` is immutable. Customizations are `JacksonModule` beans (e.g. `TmForumMappingModule`, `FieldCleaningModule`), never a `BeanCreatedEventListener` on the mapper. Durations in `application.yaml` need a unit (`30s`), a unit-less value is silently dropped.
- Doc density is sparse. Some non-obvious public methods in `common` have a short Javadoc. Many public methods and most module classes have none. Match the file you edit. Indentation mixes tabs and spaces by file, so keep the file's style.
- `mapping.strictRelationships` (ngsi-ld-java-mapping) defaults to `true`: an unresolvable relationship without `fromProperties = true` fails the read.
- Tests: `*Test.java` are unit tests (JUnit5/Mockito). `*IT.java` extend `AbstractApiIT` and use `@MicronautTest(packages=...)`. Use parameterized tests where possible.

## Important Files
- `pom.xml`: Micronaut parent/version, all version properties, openapi-generator `configOptions` (`useGenericResponse`, `supportAsync`, `serdeable=false`, `modelBaseClass=...UnknownPreservingBase`), jib base image, k3s IT wiring (`-Dmicronaut.environments=${cache},${broker}`).
- `common/pom.xml`: Micronaut, redis-lettuce, micrometer-prometheus, jackson deps (`tools.jackson.core:jackson-databind` plus `com.fasterxml.jackson.core:jackson-annotations`).
- `*/src/main/resources/application*.yaml`: `micronaut.http.services.ngsi.*`, `micronaut.codec.json.additional-types`, `micronaut.caches.*`, `redis.caches.*.value-serializer`, `jackson.*`, `endpoints.*`.
- `data-migrator/*/src/main/java/**/{UpdateWriter,LegacyLoader}.java`: build their own `ApplicationContext`.
- `IMPLEMENTATION_PLAN.md`: the current ticket's plan, when one exists.
