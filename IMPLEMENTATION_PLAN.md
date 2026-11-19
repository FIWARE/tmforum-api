# Implementation Plan: TMForum - DB Backend - Notifications

## Overview
Keep the NGSI-LD notification path (broker subscriptions, `NgsiLdEventHandler`, `POST /callback`), but stop wiring it
when the `postgresql` environment is active. The TMForum event path stays active on both backends. Nothing is deleted:
broker-only beans get `@Requires(notEnv = CommonConstants.POSTGRESQL_ENVIRONMENT)`, the same mechanism as
`DirectEntityEventPublisher`. `AbstractSubscriptionApiController` skips the broker calls when that path is not wired.

Design decisions (apply to all steps):
- **Byte-identical broker behaviour.** Methods that move do so verbatim, together with their `@Cacheable`/`@CacheInvalidate`
  annotations, and they stay together in one class so the internal call `deleteDomainSubscriptionByTmForumSubscription`
  → `deleteDomainSubscription` keeps the same interception. The broker request sequence for `create`, `delete` and
  `callback` must not change.
- **The callback route stays per module.** `@Post(EventConstants.SUBSCRIPTION_CALLBACK_PATH)` stays on
  `AbstractSubscriptionApiController`, so on the broker it still sits under each module's `api.<module>.basepath` (and
  under every module's basepath in all-in-one). It is not moved into one common controller, because that would change
  the broker routes.
- **Module constructors keep their signature.** All 18 `EventSubscriptionApiController` (the ticket says 17) constructors still take
  `NgsiLdEventHandler`. The only edit is a `@Nullable` (`io.micronaut.core.annotation.Nullable`) on that parameter,
  because Micronaut cannot inject a bean that is absent. No parameter is added or removed.
- `NgsiLdEntityRepository` itself stays unconditional: the PostgreSQL adapter shadows it as `@Primary`. Making it
  conditional belongs to ticket 9. Only its subscription role becomes broker-only.

## Steps

### Step 1: Broker-only subscription repository
Move the NGSI-LD subscription operations out of the unconditional repository into a broker-only bean.

- New `common/src/main/java/org/fiware/tmforum/common/repository/NgsiLdSubscriptionRepository.java`:
  `@Singleton`, `@Requires(notEnv = CommonConstants.POSTGRESQL_ENVIRONMENT)`, `implements SubscriptionRepository`.
  Its constructor takes `SubscriptionsApiClient`, `NgsiLdEntityRepository` (for `retrieveEntityById`, which keeps the
  `entities` cache) and `EntityVOMapper`.
- Move these verbatim from `NgsiLdBaseRepository`, annotations included: `createSubscription`,
  `retrieveSubscriptionById` (`@Cacheable(SUBSCRIPTIONS_CACHE_NAME)`), `deleteDomainSubscriptionByTmForumSubscription`,
  `deleteDomainSubscription` (`@CacheInvalidate(SUBSCRIPTIONS_CACHE_NAME)`). Keep their Javadoc and do not add more.
  Also move the private helper `handleClientSubscriptionException` (only `retrieveSubscriptionById` uses it) and remove
  it from `NgsiLdBaseRepository`, so the error mapping (404 → empty, otherwise `NgsiLdRepositoryException`) stays identical.
- Remove the `SubscriptionsApiClient subscriptionsApi` field and constructor argument from `NgsiLdBaseRepository`. Update
  the constructors of `NgsiLdEntityRepository` and `BrokerBackedReferencesRepository`, and every test that builds them
  (e.g. `ScriptedBroker`, `ErrorResponseCharacterizationTest` and the repository tests; let `mvn -pl common -am test-compile`
  find them).
- Remove `SubscriptionRepository` from `NgsiLdEntityRepository`'s `implements` list.
- Update the Javadoc on `SubscriptionRepository` to one line: broker-only port, absent on PostgreSQL.
- `StoragePortWiringTest`: `SubscriptionRepository` now resolves to `NgsiLdSubscriptionRepository`. Add a second
  parameterized case that runs an `ApplicationContext` with the `postgresql` environment and asserts that
  `context.getBeanDefinitions(SubscriptionRepository.class)` is empty. Do not assert on `SubscriptionsApiClient`: it is
  an unconditional declarative `@Client` from ngsi-ld-java-mapping, so its definition exists on PostgreSQL too; it is
  merely no longer injected anywhere there.
- Move any existing unit tests of the moved methods along with them.
- `AbstractSubscriptionApiController` still requires the bean in this step, which is fine because the PostgreSQL ITs for
  hubs are still gated by `@RequiresHubSupport`.

Acceptance: `mvn -pl common -am test` is green. On the default (broker) context exactly one `SubscriptionRepository`
exists. On the `postgresql` context there is none.

### Step 2: Make the controller and handler side broker-only
Make `NgsiLdEventHandler` broker-only, and let `AbstractSubscriptionApiController` work without the NGSI-LD path.

- `common/.../notification/NgsiLdEventHandler.java`: add `@Requires(notEnv = CommonConstants.POSTGRESQL_ENVIRONMENT)`.
  The four checkers in `notification/checkers/ngsild/` are plain `new` instances inside the handler, so they need no
  change.
- `common/.../rest/AbstractSubscriptionApiController.java`:
  - mark the `NgsiLdEventHandler` constructor parameter `@Nullable`;
  - mark the setter `setSubscriptionRepository(@Nullable SubscriptionRepository)`;
  - add one private `boolean brokerNotifications()` (both collaborators present) as the single switch;
  - `buildSubscription`: call `buildNgsiLdSubscription` only when `brokerNotifications()` is true, otherwise leave
    `subscription` null. `buildNotificationParams` and `getCallbackURI` stay unchanged;
  - `create(TMForumSubscription)`: duplicate check, then `subscriptionRepository.createSubscription` only on the
    broker, then store. On PostgreSQL it duplicate-checks and stores, nothing else;
  - `delete`: call `deleteDomainSubscriptionByTmForumSubscription` only on the broker, otherwise only `super.delete(id)`;
  - `callback`: make the route unmatched on PostgreSQL so that Micronaut answers its normal 404. Preferred approach:
    `@RouteCondition` (micronaut-http, since 4.0) with an expression that is false when the broker path is not wired.
    First check that Micronaut 5.2.3's expression context can evaluate it, for example through the environment's
    active names via `ctx[...]`. If it cannot, fall back to returning `HttpResponse.notFound()` from the method when
    `!brokerNotifications()`, and record the choice in a one-line comment. The fallback still matches the route and
    binds `@Body NotificationVO` first, so on PostgreSQL an empty or invalid body gets 400/415, not 404; if the
    fallback is used, state that limitation in the comment and in the PR. Either way the broker branch stays
    unchanged.
- All 18 module `rest/EventSubscriptionApiController.java` (account, agreement, customer-bill-management,
  customer-management, party-catalog, product-catalog, product-inventory, product-ordering-management, quote,
  resource-catalog, resource-function-activation, resource-inventory, resource-order-management, service-catalog,
  service-inventory, service-order-management, software-management, usage-management; find the full list with
  `grep -rl "NgsiLdEventHandler ngsiLdEventHandler" --include=EventSubscriptionApiController.java`): add `@Nullable`
  to the `NgsiLdEventHandler` parameter. Nothing else changes. Do the same for the test controller
  `common/src/test/.../rest/CharacterizationSubscriptionController.java`, which otherwise cannot be created under
  `postgresql` (the same grep without `--include` finds 20 files: these 19 plus the abstract controller).
- Tests in `common/src/test/.../rest/`:
  - `AbstractSubscriptionApiControllerTest`: parameterize over broker (mocked `SubscriptionRepository` +
    `NgsiLdEventHandler`) and PostgreSQL (both null). Assert that `create` and `delete` call the subscription
    repository only in the broker case, that the PostgreSQL case stores the `TMForumSubscription` with
    `getSubscription() == null`, and that the broker case still builds the NGSI-LD subscription;
  - a route test (next to `ErrorResponseCharacterizationTest`, reusing `CharacterizationSubscriptionController` or a
    small test subclass): `POST /callback` with a valid `NotificationVO` body gives 404 under the `postgresql`
    environment and is routed to the controller under the default environment. The valid body makes the test check
    the gating, not body binding. With `@RouteCondition`, add a case with an invalid body that must also give 404
    under `postgresql`;
  - `NgsiLdEventHandlerTest` stays as it is.
- `StoragePortWiringTest` (or a sibling `NotificationWiringTest`): assert `NgsiLdEventHandler` is present by default and
  absent under `postgresql`. `TMForumEventHandler` must be present in both.

Acceptance: `mvn -pl common -am test` is green and `mvn install -DskipTests -DskipITs` compiles every module.
Broker-side behaviour is unchanged (broker branches identical, existing broker unit tests untouched).

### Step 3: Document the subscriptions cache as broker-only
The `subscriptions` cache (14d) is only used by `NgsiLdSubscriptionRepository`. It cannot be removed from the
PostgreSQL configuration: the environments are `${cache},${broker}`, so `application-in-memory.yaml` and
`application-redis.yaml` are loaded on PostgreSQL too, and Micronaut has no per-combination config file. Moving the
block into `application-orion-ld.yaml`/`application-scorpio.yaml` would also register the `redis.caches.subscriptions`
cache in in-memory deployments, where it has no Redis connection. On PostgreSQL the block is inert, because nothing
reads or writes the cache.

- In every `application-in-memory.yaml` and `application-redis.yaml` that defines `subscriptions` (main and test
  resources of all modules plus `all-in-one/src/main/resources`; `grep -rln "subscriptions:" --include="application-*.yaml"`),
  add a one-line YAML comment above the block: `# broker-only: NGSI-LD subscriptions (NgsiLdSubscriptionRepository), unused on postgresql`.
  The `tmforum-subscriptions` cache stays unchanged.
- `CommonConstants.SUBSCRIPTIONS_CACHE_NAME`: add a short trailing comment that it is broker-only, if the file uses that
  style; otherwise leave it.
- Verify that a `postgresql` context starts with both `in-memory` and `redis` cache configs. Step 4's IT matrix covers
  this, so run at least `mvn integration-test -pl product-catalog -Dbroker=postgresql -Dcache=redis` locally.

Acceptance: only comments change in the YAML files, and the PostgreSQL contexts still start.

### Step 4: Enable the hub ITs on PostgreSQL and prove event delivery
- Remove `@RequiresHubSupport` (and its import) from the 17 `EventSubscriptionApiIT`s. Keep each class's
  `@Requires(notEnv = "scorpio")`. Then delete `common/.../test/RequiresHubSupport.java` and `StorageTags.TICKET_1`,
  since nothing uses them any more. These are test gating only; no production code is deleted. Update the
  `RequiresHubSupport` mention in `AGENTS.md`.
- Update the user-facing docs that still describe the limit this ticket removes:
  - `release-notes/RELEASE_NOTES_POSTGRESQL_ADAPTER.md`: rewrite the intro sentence that says a PostgreSQL deployment
    needs a broker for hub registration "until ticket 1" (it links the interim-limits anchor); drop the
    "Interim limits (until ticket 1)" section, or keep the heading only if another limit remains; state that hub
    registration and TMForum event delivery work without a broker, and that `POST /callback` and NGSI-LD
    subscriptions are broker-only (404 on PostgreSQL); remove the `@RequiresHubSupport` bullet in "Tests and CI".
  - `storage-postgresql/README.md`: remove `@RequiresHubSupport` (ticket 1) from the excluded-tags sentence, leaving
    `@BrokerOnly`.
- New `product-catalog/src/test/java/org/fiware/tmforum/productcatalog/HubEventDeliveryIT.java`, `@PostgresqlOnly`,
  with `@Property(name = RecordingHubController.ENABLED_PROPERTY, value = "true")`. It registers hubs through
  `EventsSubscriptionApiTestClient` with callbacks on `RecordingHubController`, creates and deletes a catalog, and
  awaits (awaitility, named timeout constants as in `DeleteEventBrokerIT`) one `CatalogCreateEvent`/`CatalogDeleteEvent`
  on the matching hubs and none on the others. It also asserts that the stored `TMForumSubscription` has no
  `subscription`. Use a `@ParameterizedTest` over the hub/event cases. Broker delivery is already pinned by
  `DeleteEventBrokerIT`.
- `all-in-one/src/test/.../PostgresStartupIT`: add an assertion that `POST /callback` with a valid `NotificationVO`
  body under a module basepath gives 404, which checks the real wiring of all controllers (the valid body keeps the
  check on the gating, not on body binding).
- Run the full 6-way IT matrix (`-Dbroker=orion-ld|scorpio|postgresql`, `-Dcache=in-memory|redis`) for `common`,
  `product-catalog` and at least two more hub modules (for example `party-catalog`, `service-inventory`), and the
  all-in-one job (`.github/workflows/test-brokers.yaml`). Delegate the runs and report only failures. Fix what
  fails within this ticket's scope.

Acceptance: on PostgreSQL, `POST /hub` stores the listener and TMForum events reach it with no broker interaction
(there is no broker in `k3s/postgresql`). `POST /callback` gives 404 on PostgreSQL and works on the broker. The broker
ITs, including `DeleteEventBrokerIT`, are unchanged and green. The 6-way matrix is green.
