# Implementation Plan: Update TMForum Micronaut to 5.1.3

## Overview
Move the build from `io.micronaut.platform:micronaut-parent` 4.10.18 to **5.1.3**, which the ticket names, even though 5.2.x already exists. Micronaut 5 needs **Java 25**, moves `micronaut-jackson-databind` to **Jackson 3** (`com.fasterxml.jackson.databind` → `tools.jackson.databind`) and drops RxJava 2. That makes this a toolchain, dependency and API migration rather than a version bump. The work goes in small steps. Prep that is safe on Micronaut 4 lands first, then the bump with the compile fixes, then runtime and test fixes, then docs.

### Findings that shape the plan
- **Java baseline:** the project is on JDK 21 (root `pom.xml` `jdk.version`/`release.version`, jib base `ubi9/openjdk-21-runtime:1.24-2`, `data-migrator/migrator/Dockerfile` `eclipse-temurin:21-jdk`, CI `setup-java` 21, and `check.yml` still on 11). Some build plugins are too old for Java 25 class files: jacoco 0.8.12 and spotbugs-maven-plugin 4.5.2.0.
- **Jackson 3:** 104 Java files import `com.fasterxml.jackson.databind`/`core`. Hotspots: `common/.../mapping/FieldCleaningSerializer.java`, `common/.../filter/BrokerRequestLoggingFilter.java`, `common/.../test/AbstractApiIT.java`, and the ITs and unit tests of every module. `jackson-annotations` (`com.fasterxml.jackson.annotation`) keeps its package in Jackson 3, so generated VOs and `UnknownPreservingBase` annotations stay as they are.
- **`ngsi-ld-java-mapping` 1.6.x** (latest 1.6.29) is built on Micronaut 4.6/4.10 with Jackson 2 and Java 17. It ships Micronaut-4-compiled bean definitions: `EntityVOMapper`, `JavaObjectMapper`, `CacheSerdeableObjectMapper extends com.fasterxml.jackson.databind.ObjectMapper`, `EscapeCleaningJsonFactory`, and the generated declarative clients `EntitiesApiClient`/`SubscriptionsApiClient` (client id `ngsi`). It is the **main external risk**. Either its Micronaut 4 beans load and work under Micronaut 5, with Jackson 2 kept internal to the library, or a Micronaut-5/Jackson-3 release of that library (in `wistefan/ngsi-ld-java-mapping`) is a **prerequisite**. Step 3 decides this first.
- **`micronaut-openapi-codegen`:** 4.5.1 is the latest release, and no 5.x line exists. It only generates sources, so it can stay as long as the generated controllers, VOs, example builders and test interfaces compile against Micronaut 5 and Jackson 3. If they don't, use template overrides (`templateDirectory`) in this repo.
- **RxJava 2:** only `data-migrator/update-writer/pom.xml` declares `io.reactivex.rxjava2:rxjava`, and no Java code imports it.
- **`data-migrator/legacy-loader`** depends on the legacy mapping library 1.2.11, which will not run on Micronaut 5. It runs in its own `URLClassLoader` inside `migrator`, so it can stay on Micronaut 4.
- **Micronaut internals that may break at runtime:** `common/.../caching/ClientTrackingRedisCache.java` (extends lettuce `RedisCache` and uses `@Replaces`, `@EachBean`, `ConversionService`, `BeanLocator`), the old-style filters `PaginationFilter`, `ForwardedForFilter` and `BrokerRequestLoggingFilter` (`@Filter` + `HttpServerFilter`/`HttpClientFilter`), the 6 `ExceptionHandler`s in `common/.../exception/`, the health indicators in `common/.../health/`, and the YAML keys `micronaut.codec.json.additional-types`, `micronaut.http.services.ngsi.read-timeout: 30` (no unit) and `redis.caches.*.value-serializer: io.github.wistefan.mapping.EntityVOSerializer`.
- The unused property `version.io.micronaut.configuration.micronaut-micrometer-registry-prometheus` (5.13.2) is dead and can be deleted.

## Steps

### Step 1: Raise the toolchain to Java 25 while still on Micronaut 4.10.18
Make the build and runtime Java 25 without touching Micronaut. Micronaut 4.10 runs on 25, so this lands green on its own and the bump step only has to deal with API changes.
- Root `pom.xml`:
  - Set `jdk.version` and `release.version` to `25`.
  - Bump `version.org.jacoco.maven-plugin` to ≥ 0.8.14 and `version.com.github.spotbugs.maven-plugin` to a current 4.9.x. Both need to read Java 25 class files.
  - Check that lombok 1.18.42, mapstruct 1.6.3 and mockito 5.22.0 work on JDK 25, and bump any that don't.
  - Update the jib base image in the `oci` and `conformance-test` profiles from `ubi9/openjdk-21-runtime:1.24-2` to a Java 25 runtime image. Check that a `ubi9`/`ubi10` `openjdk-25-runtime` tag exists in `registry.access.redhat.com`. Otherwise use `eclipse-temurin:25-jre`. Put the image in a named property instead of repeating the literal.
- `data-migrator/migrator/Dockerfile`: move to `eclipse-temurin:25-jdk`.
- `.github/workflows/*.y*ml`: every `setup-java` `java-version` becomes `'25'`, including `check.yml`, which is still on `'11'`.
- Install JDK 25 locally (`sudo apt-get install` or a Temurin tarball). If `micronaut-parent` 5.1.3 or its plugins need it, also install Maven ≥ 3.9. Check the enforcer rules in the 5.1.3 parent POM and record the result in `AGENTS.md`.
- In `AGENTS.md`, change the Java version and base image entries.
- **Acceptance:** `mvn clean install -DskipTests -DskipITs` and `mvn test` pass on JDK 25 with Micronaut 4.10.18. `mvn install -Poci -DskipTests -DskipITs -pl common,product-catalog -am` builds an image that starts.

### Step 2: Remove Micronaut-5 blockers that can go away while still on Micronaut 4
Cleanups that are no-ops on Micronaut 4 but are needed before the bump.
- `data-migrator/update-writer/pom.xml`: remove the unused `io.reactivex.rxjava2:rxjava` dependency, since Micronaut 5 dropped RxJava 2. Remove `version.io.reactivex.rxjava2.rxjava` from the root `pom.xml` unless something else still references it (`grep` first).
- Root `pom.xml`: delete the unused `version.io.micronaut.configuration.micronaut-micrometer-registry-prometheus` property.
- `data-migrator/legacy-loader`: pin this module to Micronaut 4 explicitly so the later parent bump does not affect it. It only reads legacy data with the old mapping library 1.2.11 and runs isolated in its own classloader under `migrator`.
  - Preferred: override `micronaut.version` in `legacy-loader/pom.xml` and import `io.micronaut.platform:micronaut-platform:4.10.18` in its `dependencyManagement`. Then run `mvn -pl data-migrator/legacy-loader dependency:tree` and confirm that only 4.x Micronaut artifacts resolve, including the annotation processor paths that use `${micronaut.version}`.
  - Fallback: give `legacy-loader` `micronaut-parent:4.10.18` as its own `<parent>` (with `relativePath` empty) and copy over the few root properties it needs.
  - Use a named property such as `legacy.micronaut.version` for the pinned version, not a literal.
- **Acceptance:** the full build and `mvn test` stay green. `dependency:tree` for `legacy-loader` shows only Micronaut 4.10.18. The migrator still packages (`mvn -pl data-migrator/migrator -am package -DskipTests`).

### Step 3: Bump to Micronaut 5.1.3, migrate to Jackson 3, and make every module compile
This is the core bump. **Start with the compatibility probe**, and stop if it fails.
1. **Probe `ngsi-ld-java-mapping`:**
   - Bump the parent and `micronaut.version` to `5.1.3` (root `pom.xml` lines `<parent>` and `<micronaut.version>`). Set `version.io.github.wistefan.ngsi-ld-mapping-java` to the newest release (≥ 1.6.29), and `version.io.kokuwa.micronaut.codegen` to 4.5.1.
   - Run `mvn clean install -pl common -am -DskipTests`.
   - Write a minimal `@MicronautTest` in `common/src/test/java` that injects `EntitiesApiClient`, `EntityVOMapper` and `JavaObjectMapper` and maps a sample domain object to an `EntityVO`. This tells you whether the library's Micronaut-4 bean definitions and declarative clients load and run on Micronaut 5.
   - **If they don't**, write `completion-status.json` with status `blocked`. Name the requirement exactly: a release of `io.github.wistefan:ngsi-ld-java-mapping` built on `micronaut-parent` 5.1.x, Java 25 and Jackson 3, with regenerated NGSI-LD clients. Do not work around it with reflection or copied library code.
   - **If they do**, keep the test as a regression guard and continue.
2. **Jackson 3 migration:**
   - Run OpenRewrite once, ad hoc, and do not commit the plugin config: `mvn org.openrewrite.maven:rewrite-maven-plugin:run -Drewrite.recipeArtifactCoordinates=org.openrewrite.recipe:rewrite-jackson:RELEASE -Drewrite.activeRecipes=org.openrewrite.java.jackson.UpgradeJackson_2_3`.
   - If that doesn't work, rename by hand: `com.fasterxml.jackson.databind` → `tools.jackson.databind`, `com.fasterxml.jackson.core` → `tools.jackson.core`, `JsonProcessingException` → `JacksonException`, `JsonMappingException` → `DatabindException`, `JsonSerializer`/`SerializerProvider` → `ValueSerializer`/`SerializationContext`.
   - In `common/pom.xml`, replace the explicit `com.fasterxml.jackson.core:jackson-databind` and `jackson-datatype-jsr310` deps with the Jackson 3 equivalents managed by the BOM (`tools.jackson.core:jackson-databind`). java.time support is built into Jackson 3. Keep `com.fasterxml.jackson.core:jackson-annotations`.
   - Hand-check `FieldCleaningSerializer`, `BrokerRequestLoggingFilter` (it must serialize with the same primary mapper the HTTP codec uses) and `AbstractApiIT`.
3. **Other Micronaut 5 API breaks:**
   - Fix what the compiler reports in `common` first, then `*-shared-models`, the API modules, `all-in-one` and `data-migrator/update-writer`. Follow the official guide: https://github.com/micronaut-projects/micronaut-core/wiki/Update-to-Micronaut-5 and the per-module breaking-change pages linked from it (cache, redis, micrometer, validation, reactor, test).
   - Expected hotspots: `ClientTrackingRedisCache`, the filters, the `ExceptionHandler`s, the health indicators, and the `testcontainers`/`micronaut-test` artifact renames, if any are used.
   - Migrating `io.micronaut.core.annotation.Nullable/NonNull` to JSpecify is **optional**. Only do it where the compiler or deprecation-as-error forces it.
4. **Generated code:** check that the kokuwa-generated sources in each module's `target/generated-sources` and `target/generated-test-sources` compile. If generated test code uses Jackson 2 databind APIs, add minimal template overrides in this repo and point the generator at them with `templateDirectory` in the root `pom.xml` `pluginManagement`.
- Leave runtime behaviour (YAML config, cache, filters at runtime) to Step 4 unless it blocks compilation.
- **Acceptance:** `mvn clean install -DskipTests -DskipITs` passes with `micronaut-parent` 5.1.3. `mvn dependency:tree` shows no Micronaut 4.x artifacts and no `com.fasterxml.jackson.databind` outside `ngsi-ld-java-mapping` and `legacy-loader`. The probe test passes.

### Step 4: Get the unit tests green and fix runtime configuration
- Run `mvn test` across all modules and fix the failures.
- Go through every `*/src/main/resources/application*.yaml` (171 files) and check each key against the Micronaut 5 configuration reference. Rename or adjust what changed. Make the change in every module and in `all-in-one`, not just one. Keys to check:
  - `micronaut.codec.json.additional-types` (`application/ld+json` must still be (de)serialized by the JSON codec)
  - `micronaut.http.services.ngsi.{url,path,read-timeout}`, including the unit-less `read-timeout: 30` and the stray `micronaut.http.services.read-timeout`
  - `micronaut.caches.*`
  - `redis.uri` and `redis.caches.*.value-serializer`
  - `jackson.serialization.*`. Jackson 3 changes some defaults, for example dates as timestamps. Keep the current wire format of TMForum responses.
  - `micronaut.metrics.export.prometheus.*`
  - `endpoints.{health,metrics}.*`
- Check behaviour that compiles but may have changed:
  - `ClientTrackingRedisCache` still replaces the default Redis cache.
  - `PaginationFilter` and `ForwardedForFilter` still run on server requests, and `BrokerRequestLoggingFilter` on `ngsi` client requests.
  - The exception handlers still map to the same HTTP statuses.
  - `NgsiLdBrokerReadinessIndicator` (`@Client(id="ngsihealth")`) still reports readiness.
- Add or extend unit tests (parameterized where possible) in `common/src/test/java` for anything you had to change semantically. Examples: the Jackson 3 serializer in `FieldCleaningSerializer`, and the filter ordering.
- **Acceptance:** `mvn test` is green for all modules. Running one module with `-Dmicronaut.environments=in-memory,orion-ld` against `mvn install -Pdev` serves `GET` and `POST` requests correctly.

### Step 5: Get the integration tests green across the broker/cache matrix
- Run the ITs per module (`mvn integration-test -pl <module>`) with the default `orion-ld`/`in-memory` combination, then `-Dbroker=scorpio` and `-Dcache=redis`. These are the combinations in `.github/workflows/test-brokers.yaml` and `parallel-test.yml`.
- Fix the failures in `common/.../test/AbstractApiIT.java`, `TestServerHostConfigurer.java`, the module `*IT.java` classes (for example `product-catalog/.../ListParameterValidationIT.java` with `@Client("/")`), and the k3s wiring in the root `pom.xml`. Check the surefire/failsafe `argLine` that sets `-Dmicronaut.environments` and the `--add-opens` flags, which may need changes on Java 25.
- Run `all-in-one` ITs, if any, and smoke-test the `all-in-one` app: every API answers a list request.
- `data-migrator`: build `update-writer`, `legacy-loader` and `migrator`, and run one migration against the dev broker. This confirms the isolated classloaders work with a Micronaut 4 `legacy-loader` next to a Micronaut 5 `update-writer`.
- If the CI workflows need new flags (for example the Maven version or `MAVEN_OPTS`), update them.
- **Acceptance:** all ITs pass for orion-ld+in-memory and scorpio+redis, at least on the modules CI covers. The `data-migrator` run succeeds.

### Step 6: Images, conformance smoke test and documentation
- `mvn install -Poci -DskipTests -DskipITs`: build all images, including `ui`, which is unaffected but must still build. Start `all-in-one` and check `/health` and one API.
- Run the conformance test for one module (`mvn clean install -Pconformance-test --projects common,mapping,product-catalog -DskipTests -DskipITs`) to confirm the wire format did not change with Jackson 3.
- Docs:
  - `AGENTS.md`: Micronaut 5.1.3, Java 25, Jackson 3, the codegen and mapping-library versions, and the pinned Micronaut 4 `legacy-loader`.
  - `README.md`: any Java or Micronaut version requirements.
  - Add a release note under `release-notes/` that follows the existing format. It should cover the Java 25 runtime requirement, the image base change and any changed config keys.
- **Acceptance:** the images build and start, the conformance smoke test passes, and the docs match the build.
