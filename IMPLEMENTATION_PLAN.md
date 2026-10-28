# Implementation Plan: TMForum - DB Backend - Extract Repo

## Overview
Put a storage-neutral port in front of persistence. It lives in a new `storage-api` Maven module and is split into read and write interfaces, with a neutral reference type and neutral error semantics. Today's NGSI-LD code (`TmForumRepository`, `NgsiLdBaseRepository`, `BrokerBackedReferencesRepository`) becomes the adapter that implements the port. Controllers and services then depend only on the interfaces. Both backends are permanent (decision 7), so the port is a documented, tested public contract. The ticket ships with **zero behaviour change**: the ITs and the HTTP responses stay byte-identical.

### Facts from the survey (main @ `f63b9b42`)
- `TmForumRepository` (`@Singleton`, extends `NgsiLdBaseRepository`) is referenced by 6 main files in `common` and by about 65 files in the 20 API modules. All of the module files are controllers, and they pass it to the `AbstractApiController`/`AbstractSubscriptionApiController` super constructors. 19 classes extend `AbstractSubscriptionApiController`. `all-in-one` compiles the module sources through `build-helper-maven-plugin` and has no references of its own. `data-migrator` does not use it.
- The only callers of `extractTotalCount` are in `common/src/test/.../TmForumRepositoryTest.java`. It stays package-private inside the adapter.
- `ReferencesRepository.referenceExists`/`findReference` return `Mono<EntityVO>`. They are used only by `ReferenceValidationService`, which reads `getType()`. Their result comes from the `@Cacheable` `retrieveEntityById` (`Mono<EntityVO>`), so the cached value must stay `EntityVO` and be mapped to the neutral type *after* the cache.
- The broker leaks through the errors: `AbstractApiController.create`/`patch` and `AbstractSubscriptionApiController.create(Subscription)` switch on `HttpClientResponseException` status (CONFLICT/BAD_REQUEST/other). `NgsiLdBaseRepository` throws `DeletionException` (NOT_FOUND/UNKNOWN), `NgsiLdRepositoryException` and `TmForumException`. When an unmapped `HttpClientResponseException` reaches `HttpClientExceptionHandler`, the client gets a 502.
- Port calls also happen outside `common`, with no error mapping of their own: `party-catalog` `OrganizationApiController.deleteOrganization` calls `deleteDomainEntity` (it relies on the adapter's `DeletionException`), and the `resource-catalog`, `resource-inventory` and `software-management` controllers call `updateDomainEntity` directly (a raw broker error reaches the client as 502). That is why Step 3 renders errors in a handler instead of at each call site.
- Only `AbstractSubscriptionApiController` uses the subscription methods `createSubscription(SubscriptionVO, String)`, `createDomainSubscription`, `retrieveSubscriptionById` and `deleteDomainSubscriptionByTmForumSubscription`. They are NGSI-typed, and ticket 1 owns them, so they stay **out of** the entity port.
- There are 73 `*IT.java` files; the ticket text says 72. All of them must pass.
- CI builds `common` with explicit lists such as `--projects common,...-shared-models,...` (`.github/workflows/test-brokers.yaml`, `conformance-test.yaml`, and `README.md`). Those lists must gain `storage-api`, or the reactor cannot resolve it.

### Port shape (target, package `org.fiware.tmforum.storage`)
```java
public interface EntityReadRepository {
    <T> Mono<T> get(URI id, Class<T> entityClass);
    <T> Mono<PagedResult<T>> findEntities(Integer offset, Integer limit, Class<T> entityClass,
                                          String query, String ids, String types, String orderBy);
    default <T> Mono<PagedResult<T>> findEntities(Integer offset, Integer limit, String entityType,
                                                  Class<T> entityClass, String query) { /* delegates */ }
    <T> Mono<PagedResult<T>> findEntitiesPolymorphic(Integer offset, Integer limit, String types, String query,
                                                     String ids, String orderBy,
                                                     Function<String, Class<? extends T>> typeToClass);
}
public interface EntityWriteRepository {
    <T> Mono<Void> createDomainEntity(T domainEntity);
    <T> Mono<Void> updateDomainEntity(String id, T domainEntity);
    Mono<Void> deleteDomainEntity(URI id);
}
public interface EntityRepository extends EntityReadRepository, EntityWriteRepository {}
public interface ReferenceReadRepository {
    Mono<StoredReference> referenceExists(String id, List<String> acceptedTypes);
    Mono<StoredReference> findReference(String id);
}
public record StoredReference(String id, String type) {}
public record PagedResult<T>(List<T> items, Integer offset, Integer limit, Integer totalCount) {}  // moved from common
public class StorageException extends RuntimeException { StorageErrorReason reason(); StorageOperation operation(); }
public enum StorageErrorReason { NOT_FOUND, CONFLICT, INVALID_DATA, PARTIAL_UPDATE, UNAVAILABLE, UNKNOWN }
public enum StorageOperation { READ, LIST, CREATE, UPDATE, DELETE }
```
The method signatures are today's, kept verbatim to keep the rewiring mechanical. The only changes are the `EntityVO` → `StoredReference` swap and the default-method overload. `StorageOperation` names the backend step that failed. Together with the reason, it is the only signal `common` uses to render today's responses (Step 3), so no rendering decision depends on the type of the cause. `query`/`orderBy` remain the `QueryParser` output (NGSI-LD q syntax). That is documented as transitional coupling, which ticket 7 removes, so do not change `QueryParser` here.

## Steps

### Step 1: Create the `storage-api` module with the documented port types
- Add `storage-api/pom.xml` (artifact `storage-api`, parent = root). Its only compile dependency is `io.projectreactor:reactor-core`, version managed by the Micronaut BOM, and its tests use `junit-jupiter`. Add a `maven-enforcer-plugin` `bannedDependencies` rule that bans `io.github.wistefan:*`, `io.micronaut*:*` and `org.fiware.tmforum:*` from this module. That makes "no NGSI-LD/framework types in the port" a build failure instead of a convention. If the enforcer plugin is not yet in the root `pluginManagement`, add its version as a root property.
- Register `<module>storage-api</module>` in the root `pom.xml` **before** `common`. Add the `storage-api` dependency (`${project.version}`) to `common/pom.xml`.
- Create the types listed under *Port shape* in `storage-api/src/main/java/org/fiware/tmforum/storage/`: `EntityReadRepository`, `EntityWriteRepository`, `EntityRepository`, `ReferenceReadRepository`, `StoredReference`, `PagedResult`, `StorageException`, `StorageErrorReason`, `StorageOperation`, plus a `package-info.java`.
- Move `PagedResult` from `common/.../repository/PagedResult.java` to the new package and update every import in `common`. No module outside `common` imports it. Also update the Javadoc `{@link}` in `PaginationFilter`.
- The Javadoc is the contract, so it is denser than the rest of the repo by design. Keep each item short and cover only what the signature cannot show:
  - `get`: returns empty, not an error, when the id is unknown or the stored type is not one the class is persisted as. Today that is `@MappingEnabled.entityType`; name the annotation in prose and do not depend on it. Returns empty for an id that is not an entity id.
  - `findEntities`/`findEntitiesPolymorphic`: `types` and `ids` are comma-separated with OR semantics. `query`/`orderBy` are `QueryParser` output (transitional, ticket 7). `totalCount` is null when the backend cannot report it. `typeToClass` receives the stored type.
  - `updateDomainEntity`: writes only the attributes the entity carries (null fields are untouched, an emptied list is cleared), never a whole-document overwrite. Fails with `NOT_FOUND` when the id is absent and never creates or completes silently, because `AbstractApiController.relatedEntityHandlingMono` relies on that failure for its update-else-create. Fails with `PARTIAL_UPDATE` when the backend wrote only some attributes.
  - `createDomainEntity`: `CONFLICT` when the id exists, `INVALID_DATA` when the backend rejects the content.
  - `deleteDomainEntity`: `NOT_FOUND` when the id is absent.
  - The interfaces carry the type-level notes: a `Mono<Void>` completes empty on success, failures are signalled as `StorageException` with a reason and the failing operation, and references are inline, so no foreign-key semantics apply. `UNAVAILABLE` means the backend gave no answer. `UNKNOWN` means it answered with an error that has no other reason.
- Update the `--projects` lists in `.github/workflows/test-brokers.yaml` (both jobs), `.github/workflows/conformance-test.yaml` and `README.md` (conformance command) to include `storage-api`.
- Add unit tests in `storage-api/src/test/java`: parameterized tests for the `findEntities` default overload (it delegates with `ids=null`, `types=entityType`, `orderBy=null`, using a tiny in-test stub) and for `StorageException` reason, operation and cause propagation.
- **Acceptance:** `mvn clean install -DskipTests -DskipITs` is green. `mvn test -pl storage-api,common` is green. Nothing implements the port yet, so behaviour is unchanged.

### Step 2: Make the NGSI-LD code implement the port
- Make `common/.../repository/TmForumRepository.java` `implements EntityRepository`. Remove its own copy of the 5-arg `findEntities` overload in favour of the interface default, which keeps the same semantics, and add `@Override` where it applies. `createDomainEntity`/`updateDomainEntity`/`deleteDomainEntity` stay in `NgsiLdBaseRepository` and satisfy the interface by inheritance. Keep every `@Cacheable`/`@CachePut`/`@CacheInvalidate` exactly where it is.
- Delete `common/.../repository/ReferencesRepository.java`. Make `BrokerBackedReferencesRepository` `implements ReferenceReadRepository` and return `Mono<StoredReference>`, built with `retrieveEntityById(...).map(e -> new StoredReference(e.getId().toString(), e.getType()))`. The cached value stays `EntityVO`. `referenceExists` keeps filtering on `acceptedTypes`.
- Switch `ReferenceValidationService` to `ReferenceReadRepository`/`StoredReference` (`.type()` replaces `getType()`). Update `ReferenceValidationServiceTest`, keeping its parameterized cases.
- Add `common/.../repository/SubscriptionRepository.java`, an interface that stays in `common` because it carries NGSI types. It holds `createSubscription(SubscriptionVO, String)`, `createDomainSubscription(Subscription)`, `retrieveSubscriptionById(URI)` and `deleteDomainSubscriptionByTmForumSubscription(URI)`, and `TmForumRepository` implements it. Give it a one-line Javadoc saying it is the broker-only notification path owned by ticket 1 and not part of the storage port.
- Bean wiring: `TmForumRepository` is the single bean for `EntityRepository`, `EntityReadRepository`, `EntityWriteRepository` and `SubscriptionRepository`. `BrokerBackedReferencesRepository` is the single bean for `ReferenceReadRepository`. Plain `@Singleton` is enough. Ticket 6 adds profile-based selection, so do not add `@Requires`/`@Primary` now.
- **Acceptance:** `mvn test -pl common` is green. One IT module, for example `mvn integration-test -pl product-catalog`, is green, which shows that caching and reference validation are unchanged. The bean graph resolves: add a small `@MicronautTest` or `ApplicationContext` unit test in `common` that resolves each port interface to exactly one bean, only if that can be done without a broker. Otherwise, the IT covers it.

### Step 3: Make error semantics storage-neutral (zero HTTP change)
Today's error responses depend on *where* a failure comes from, not only on the broker status. Every origin below keeps exactly its response. (`TF-x` = `TmForumExceptionHandler` with reason x, `DEL` = `DeletionExceptionHandler`, `CA` = the `CatchAllExceptionHandler` 500 body, `502` = the `HttpClientExceptionHandler` body. "Call site" = the `onErrorMap` in `AbstractApiController.create`/`patch` and `AbstractSubscriptionApiController.create(Subscription)`, which covers the whole chain, so it includes the checking mono's reference validation and related-entity writes. "Uncaught" = every other caller, e.g. `OrganizationApiController.deleteOrganization` and the module controllers that call `updateDomainEntity` directly.)

| Origin today | Thrown today | Response at call site / uncaught | New `StorageException` (operation, reason) |
|---|---|---|---|
| `get`/`retrieveEntityById`/`findReference`, non-404 or no answer | `NgsiLdRepositoryException` | CA 500 / CA 500 | READ, UNKNOWN or UNAVAILABLE |
| `findEntities*` failure | `TmForumException` UNKNOWN "Was not able to list entities." | TF-UNKNOWN 500 / same | LIST, UNKNOWN or UNAVAILABLE |
| `deleteDomainEntity` 404 / other | `DeletionException` NOT_FOUND / UNKNOWN | DEL 404 / DEL 500 | DELETE, NOT_FOUND / UNKNOWN or UNAVAILABLE |
| create (incl. `createDomainSubscription`), broker 409 / 400 / other status | raw `HttpClientResponseException` | TF-CONFLICT / TF-INVALID_DATA / TF-UNKNOWN "Unspecified downstream error" ; uncaught 502 | CREATE, CONFLICT / INVALID_DATA / UNKNOWN (404 → NOT_FOUND, mapped like UNKNOWN) |
| create, no answer | raw `HttpClientException` | 502 / 502 (call site passes it through) | CREATE, UNAVAILABLE |
| update append path (`patchEntity`) and replace path `updateAttributes`, broker 409 / 400 / 404 / other | raw `HttpClientResponseException` | as create, "updating" wording / 502 | UPDATE, CONFLICT / INVALID_DATA / NOT_FOUND / UNKNOWN |
| update, no answer | raw `HttpClientException` | 502 / 502 | UPDATE, UNAVAILABLE |
| update replace path, pre-read 404 ("Entity … does not exist.") or pre-read failure | `NgsiLdRepositoryException` | CA 500 / CA 500 | READ, NOT_FOUND / UNKNOWN or UNAVAILABLE (the failing step is the read) |
| update replace path, broker 207 | `TmForumException` INVALID_DATA "Not all attributes could be updated: …" | TF-INVALID_DATA 400 (passed through) / same | UPDATE, PARTIAL_UPDATE, same message |

- **First, before any refactoring, write characterization tests** against the current code and commit them. They drive the real adapter (`TmForumRepository` over a mocked `EntitiesApiClient`/`EntityAttributesApiClient`, constructed the way `TmForumRepositoryTest` already does it) and the real controllers, and they render the final error through the real `ExceptionHandler` beans. They assert **only** the HTTP status and the `ErrorDetails` code/reason/message, never the exception type, so they stay unchanged and green through the whole step. Cover every table row, parameterized:
  - {create, patch, delete, list, retrieve} × {broker 400, 404, 409, 500, no answer} for the final port call (`AbstractApiControllerTest`, `AbstractSubscriptionApiControllerTest`).
  - The same create/patch status set when the failure comes from the checking mono: reference validation (`findReference` failing → CA 500) and the related-entity write in `relatedEntityHandlingMono` (update fails, then the fallback create fails with 409 / 400 / 500 / no answer).
  - get → CA 500 and list → TF-UNKNOWN 500, explicitly through the real handlers.
  - Both update modes (`replaceOnUpdate` false and true), including the pre-read 404 and the 207.
  - A direct, uncaught `updateDomainEntity` caller (e.g. `resource-catalog` `ResourceSpecifcationApiController`) → 502.
  - `party-catalog`: a new `OrganizationApiControllerTest` with delete 404 → DEL 404 and delete 500 → DEL 500.
- Translate broker failures inside the adapter (`NgsiLdBaseRepository`/`TmForumRepository`) into `StorageException` exactly as in the last column. The message stays what the broker body or today's message is, and the original exception is the cause, kept for logging only. Keep the "Broker rejected …" log lines, moved with the logic. `NgsiLdRepositoryException` and the adapter's `DeletionException`/`TmForumException` throws are replaced; `DeletionException` stays only for the subscription delete path (`SubscriptionRepository`, ticket 1).
- In `common`, render it back:
  - Call site: switch only on `StorageException` with operation CREATE/UPDATE and reason CONFLICT/INVALID_DATA/NOT_FOUND/UNKNOWN (the backend answered with an error), using today's messages, with NOT_FOUND and UNKNOWN both mapping to the "Unspecified downstream error" case. Everything else passes through unchanged, as it does today.
  - A new `StorageExceptionHandler` renders every `StorageException` that is not mapped, keyed on (operation, reason) only: DELETE → DEL 404 for NOT_FOUND, otherwise DEL 500. LIST → TF-UNKNOWN 500 with the stored message. READ → CA 500. CREATE/UPDATE → TF-INVALID_DATA 400 with the stored message for PARTIAL_UPDATE, otherwise 502. It reuses the existing handlers' body builders so the bytes cannot drift. Its Javadoc carries the table in one short line per operation.
  - `AbstractApiController.delete` needs no mapping. The handler covers it and `OrganizationApiController.deleteOrganization` alike.
- After this step, no code in `common` outside `common/.../repository` may inspect `HttpClientResponseException` that came from a port call. `BrokerRequestLoggingFilter` and `AbstractApiIT` are not port callers and stay as they are.
- If any case cannot be kept identical, stop and report it as a blocker instead of accepting a deviation. Accepted deviations must be recorded in ticket 0.
- **Acceptance:** the characterization tests are unchanged and green, `mvn test -pl common,party-catalog,resource-catalog` is green, and `mvn integration-test -pl product-catalog,party-catalog` is green.

### Step 4: Switch `common`'s own consumers to the interfaces
- `common/.../rest/AbstractApiController.java`: the field and constructor parameter `TmForumRepository repository` becomes `EntityRepository repository`. This is source-compatible, because the module controllers still pass a `TmForumRepository`, which implements the interface, so the step merges on its own.
- `common/.../notification/TMForumEventHandler.java`: the dependency becomes `EntityReadRepository`, since it only calls `findEntities`.
- `AbstractSubscriptionApiController` keeps its constructor signature in this step; it changes in Step 5. Its calls into the entity port go through the inherited `EntityRepository` field.
- Update `AbstractApiControllerTest` and the `TMForumEventHandler` tests, if any, to mock the interfaces instead of the concrete class.
- **Acceptance:** `mvn test -pl common` and `mvn clean install -DskipTests -DskipITs` are green, and `grep -n TmForumRepository common/src/main/java/org/fiware/tmforum/common/{rest/AbstractApiController,notification/TMForumEventHandler}.java` finds nothing.

### Step 5: Rewire `AbstractSubscriptionApiController` and every API module to the interfaces
- `AbstractSubscriptionApiController`: the constructor takes `EntityRepository repository, SubscriptionRepository subscriptionRepository` instead of `TmForumRepository`, and the four subscription calls go to `subscriptionRepository`. Update `AbstractSubscriptionApiControllerTest`.
- In all 20 API modules (`account`, `agreement`, `customer-bill-management`, `customer-management`, `document-management`, `party-catalog`, `party-role`, `product-catalog`, `product-inventory`, `product-ordering-management`, `quote`, `resource-catalog`, `resource-function-activation`, `resource-inventory`, `resource-order-management`, `service-catalog`, `service-inventory`, `service-order-management`, `software-management`, `usage-management`), replace every `TmForumRepository` constructor parameter, field and import with `EntityRepository`. In the 19 `AbstractSubscriptionApiController` subclasses, also add the `SubscriptionRepository` parameter.
- Mechanical rule: `grep -rl TmForumRepository --include=*.java <module>/src` must return nothing afterwards. Do not change signatures or behaviour beyond the type swap. Doc density stays as found, so do not add Javadoc to untouched classes.
- `all-in-one` compiles the same sources and needs no source change. Verify that it still builds and starts its context.
- **Acceptance:** `mvn clean install -DskipTests -DskipITs` is green, the `grep -rl TmForumRepository --include=*.java . | grep -v '^./common/'` check prints nothing, and `mvn test` is green.

### Step 6: Rename the concrete adapter and guard the boundaries
- Rename `TmForumRepository` → `NgsiLdEntityRepository` with `git mv` in `common/.../repository`, together with `TmForumRepositoryTest` → `NgsiLdEntityRepositoryTest`. This makes accidental injection of the concrete class visible. After Step 5, only `common` references it.
- Add ArchUnit (`com.tngtech.archunit:archunit-junit5`, root property `version.com.tngtech.archunit.archunit-junit5`, test scope) and two rule sets:
  - `common/src/test/.../architecture/StorageBoundaryTest.java`: outside `org.fiware.tmforum.common.repository..` (and `..common.test..`, the IT helpers), no class depends on `NgsiLdEntityRepository`, `NgsiLdBaseRepository`, `BrokerBackedReferencesRepository` or `BrokerBackedEntitiesRepository`. No `..rest..` class depends on `HttpClientResponseException`.
  - `all-in-one/src/test/.../architecture/ControllerStorageBoundaryTest.java`: all-in-one sees every module's sources. No class in `org.fiware.tmforum..` outside `org.fiware.tmforum.common..` depends on any concrete repository in `org.fiware.tmforum.common.repository`, nor on `org.fiware.ngsi..`. Before adding the second rule, confirm that no module legitimately imports `org.fiware.ngsi` (`grep -rl 'org.fiware.ngsi' --include=*.java */src/main | grep -v '^common/'`). If one does, scope that rule to controllers only and note why.
  - Keep the rule definitions in a parameterized test where the cases share a shape.
- **Acceptance:** `mvn test -pl storage-api,common,all-in-one -am` is green. Deliberately breaking a rule locally, for example by injecting `NgsiLdEntityRepository` in one controller, makes the test fail. Do not commit that experiment.

### Step 7: Reusable port contract tests and their NGSI-LD run
- In `storage-api`, publish a test-jar (`maven-jar-plugin` `test-jar` goal) containing `AbstractEntityRepositoryContractTest` and `AbstractReferenceReadRepositoryContractTest`. They are abstract JUnit 5 classes whose subclasses supply the repository under test and a fixture factory (entity class, its stored type, `newEntity(URI id)`, `mutate(entity)`). The cases cover the Step 1 Javadoc, parameterized where possible:
  - `get`: found, unknown id, type mismatch, malformed id.
  - create: then create again gives `CONFLICT`.
  - update: changes only the carried attributes, and an emptied list is cleared. An unknown id gives `NOT_FOUND` and creates nothing (run with `replaceOnUpdate` false and true on NGSI-LD).
  - delete: then delete again gives `NOT_FOUND`.
  - `findEntities`: paging (offset/limit), id and type filters, `totalCount` null or correct.
  - `findEntitiesPolymorphic`: dispatches per stored type.
  - `referenceExists`: accepted and rejected types. `findReference`: id and type.
- Run them against NGSI-LD in an API module that already has the K3S IT wiring. Add `product-catalog/src/test/java/org/fiware/tmforum/productcatalog/storage/NgsiLdEntityRepositoryContractIT.java` and a matching `...ReferenceReadRepositoryContractIT.java`. Use `@MicronautTest` and `AbstractApiIT` conventions, a simple domain class such as `Category`, and a second type for the polymorphic case. Add the `storage-api` `test-jar` dependency (test scope) to `product-catalog/pom.xml`.
- Ticket 6 reuses these abstract classes for PostgreSQL. Say so in their class Javadoc in one line.
- **Acceptance:** `mvn integration-test -pl product-catalog -Dit.test='NgsiLd*ContractIT' -DskipTests` is green on `-Dbroker=orion-ld` and on `-Dbroker=scorpio`. Any case that exposes broker-dialect behaviour is documented in the port Javadoc or fixed in the adapter. The fix must not change behaviour; otherwise, stop and report.

### Step 8: Full verification and context docs
- Run the full gate: `mvn clean install -DskipTests -DskipITs`, `mvn test`, and `mvn integration-test` over all modules (all 73 `*IT.java`) with the default `in-memory,orion-ld`. Also run at least `product-catalog` and `party-catalog` with `-Dbroker=scorpio -Dcache=redis`. Run `mvn spotbugs:spotbugs` on `storage-api` and `common`.
- Update `AGENTS.md`:
  - Project Structure: add `storage-api/` and list `NgsiLdEntityRepository` and `SubscriptionRepository` under `common/.../repository`.
  - Key Conventions: controllers and services inject `EntityRepository`/`EntityReadRepository`/`ReferenceReadRepository`, never the NGSI-LD classes, and ArchUnit enforces this. Adapters signal failures as `StorageException` (operation + reason), and `StorageExceptionHandler` renders them.
  - Build & Test: CI `--projects` lists include `storage-api`.
- Add a short `storage-api/README.md`. It covers what the port is, the read/write split, error and update semantics, and how a second adapter plugs in: implement the interfaces, extend the contract test-jar, and select it by profile in ticket 6. It points to the Javadoc and does not duplicate it.
- **Acceptance:** all 73 ITs pass with unchanged assertions. No controller references a concrete repository (ArchUnit). A second adapter needs only `storage-api` and `common` (documented in the README).
