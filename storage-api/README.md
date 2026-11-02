# storage-api

The storage-neutral persistence port of the TMForum APIs. Controllers and services in `common` and in the API
modules depend only on the types in `org.fiware.tmforum.storage`. A backend provides an adapter that implements
them. Today the only adapter is the NGSI-LD one in `common`
(`org.fiware.tmforum.common.repository.NgsiLdEntityRepository`). Ticket 6 adds PostgreSQL. Both backends are
permanent, so the port is a public contract.

The Javadoc of the types is the contract. This file is only a map of it.

## Read/write split

| Interface                 | Purpose                                                                    |
|---------------------------|----------------------------------------------------------------------------|
| `EntityReadRepository`    | `get`, `findEntities`, `findEntitiesPolymorphic` (paged in a `PagedResult`) |
| `EntityWriteRepository`   | `createDomainEntity`, `updateDomainEntity`, `deleteDomainEntity`           |
| `EntityRepository`        | both of the above, what most controllers inject                            |
| `ReferenceReadRepository` | `referenceExists`/`findReference`, returning a neutral `StoredReference`   |

Inject the narrowest interface you need. ArchUnit fails the build in three cases. In `common`'s
`StorageBoundaryTest`, code outside the adapter package references one of the four concrete adapter classes, or
a `..rest..` class references `HttpClientResponseException`. In `all-in-one`'s `ControllerStorageBoundaryTest`,
API-module code (outside `common`) references a concrete class of `common.repository` or NGSI-LD types. `common`
itself is not NGSI-free: subscriptions, notifications, mapping and caching use NGSI-LD types on purpose.

NGSI-LD subscriptions (`SubscriptionRepository` in `common`) are broker-only and are not part of the port.

## Errors

An adapter signals every failure as a `StorageException` with a `StorageOperation` (the backend step that failed:
READ, LIST, CREATE, UPDATE, DELETE) and a `StorageErrorReason` (NOT_FOUND, CONFLICT, INVALID_DATA,
PARTIAL_UPDATE, UNAVAILABLE, UNKNOWN). The one exception is `get` for a class the backend cannot persist: it throws
`IllegalArgumentException` synchronously, which renders as a 400, so do not wrap it. `common` renders the HTTP
response at the call site or in `StorageExceptionHandler`. The cause is kept for logging only, and the response
depends on (operation, reason) alone.

## Update semantics

`updateDomainEntity` writes only the attributes the entity carries. Null fields stay untouched, and an emptied list
is cleared (it reads back absent or empty). An absent id fails with NOT_FOUND and operation UPDATE. The NGSI-LD
replace mode reports READ instead, which renders differently; ticket 6 has to decide this open parity question (see
the `updateDomainEntity` Javadoc). The update never creates the entity and never completes silently.

`query` and `orderBy` are still `QueryParser` output (NGSI-LD q syntax). A second adapter has to translate that
syntax until ticket 7 adds a neutral query model.

## Adding an adapter

1. Depend on `storage-api` and `common`, nothing else from this build.
2. Implement `EntityRepository` and `ReferenceReadRepository`, and throw `StorageException` on every failure,
   except the synchronous `IllegalArgumentException` of `get` described under *Errors*.
3. Run the contract against the live backend: depend on the `storage-api` test-jar (`<type>test-jar</type>`,
   scope `test`), then extend `AbstractEntityRepositoryContractTest` and
   `AbstractReferenceReadRepositoryContractTest`. `product-catalog`'s `NgsiLd*ContractIT` show how. A backend
   that cannot meet a rule yet declares it in `knownDeviations()`. The affected cases then assert today's
   behaviour, so a fix fails the case instead of passing silently.
4. Select the adapter per deployment through a Micronaut profile/`@Requires` (ticket 6), so exactly one
   implementation of each interface is in the context. `NgsiLdEntityRepository` is also the only
   `SubscriptionRepository` bean, and every subscription controller requires one through its `@Inject` setter.
   A profile that switches the NGSI-LD adapter off must still supply a `SubscriptionRepository`, or ticket 1 must
   split it out of `NgsiLdEntityRepository` first. Without one, every module with a subscription controller fails
   at startup.

The known deviations of the NGSI-LD adapter are listed in `KnownDeviation` and in the port Javadoc.
