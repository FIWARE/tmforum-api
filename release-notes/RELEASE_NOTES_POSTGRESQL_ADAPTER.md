# Release Notes: PostgreSQL storage adapter

## Overview

The TMForum APIs can store their entities in PostgreSQL 18 instead of an NGSI-LD context broker. The new module
[storage-postgresql](../storage-postgresql/README.md) implements the backend-neutral port of
[storage-api](../storage-api/README.md) with R2DBC (non-blocking) and migrates its schema with Flyway at startup.
The NGSI-LD adapter stays the default; ticket 9 makes the backend a regular setting and flips the default.

A PostgreSQL deployment needs no context broker for entity storage. It still needs one for TMForum hub
registration until ticket 1 lands (see [Interim limits](#interim-limits-until-tickets-1-and-7)).

## Enabling it

1. Add `postgresql` to the Micronaut environments: `MICRONAUT_ENVIRONMENTS=postgresql,<cache>`.
2. Point the module at the database. Every API module ships an `application-postgresql.yaml`; its variables are
   `POSTGRES_HOST`, `POSTGRES_PORT`, `POSTGRES_DB`, `POSTGRES_USER`, `POSTGRES_PASSWORD` and the `POSTGRES_POOL_*`
   pool bounds. The full block and the defaults are in the [storage-postgresql README](../storage-postgresql/README.md#configuration).
3. Give the user the right to create the schema `tmforum`, or create it beforehand. Flyway creates
   `tmforum.entity`, `tmforum.event_outbox` and its history table `tmforum.flyway_schema_history` on the first start.

Readiness then checks the database (`SELECT 1`, bounded by `endpoints.health.postgresql.timeout`, default `5s`)
instead of the broker.

## Schema

`tmforum.entity` holds one row per entity: `id` (`urn:ngsi-ld:<type>:<id>`, the same id format as on the broker),
`type`, `data` (the domain object as `jsonb`, keyed by the TMForum property names), `created_at`, `updated_at`, and
the versioning columns `version`, `revision`, `is_current`, which keep their defaults until versioning lands.
`tmforum.event_outbox` holds the TMForum events not delivered yet. Both are described in the
[storage-postgresql README](../storage-postgresql/README.md#schema).

## Behaviour on PostgreSQL

* **GET is one statement.** References are stored inline and never resolved on read, so a GET costs one query
  whatever the number of references (`GetIssuesOneQueryIT` pins it).
* **PATCH is atomic.** An update is a single statement that merges the carried attributes into the stored document;
  nothing is read first. Each carried attribute replaces its stored value whole, a carried list replaces the stored
  list. Two concurrent PATCHes of the same list end with exactly one of the two lists, so the concurrent-merge race
  of #179 no longer happens. A client that relied on concurrent PATCHes merging their lists must not.
* **`\u0000` is rejected.** A create or PATCH carrying the NUL character in any string or key answers 400 with the
  path of the field, because PostgreSQL cannot store it in `jsonb`. Orion-LD 1.9.0 accepts it (201) but stores the
  six characters `\u0000` instead of the NUL, so the value reads back changed. Scorpio was not checked.
* **Empty values** of declared properties behave like on the broker: an emptied list removes the attribute, a
  create stores no empty list or object, and an update carrying an empty object keeps the stored one. Extension
  (unknown) properties are stored as sent, so an empty list or object reads back empty. The port contract pins all
  of this for both adapters. One difference: on Orion-LD a PATCH that empties an extension list makes it read back
  as a map from the expanded attribute name to `[]`; PostgreSQL returns `[]`.

## TMForum events

* **At-least-once on PostgreSQL.** Create, update and delete write their event into `tmforum.event_outbox` in the
  same transaction as the entity. The `OutboxDispatcher` delivers it to the registered hubs and retries with an
  exponential backoff (`tmforum.outbox.poll-interval` up to `tmforum.outbox.max-backoff`) until every hub answered
  2xx. Retries carry the same `eventId` and `eventTime`, so a hub can drop duplicates by `eventId`. Several
  instances share the outbox safely (a claimed row is leased for `tmforum.outbox.lease`).
* **Fire-and-forget on the broker,** unchanged: a failed delivery is logged and lost. The difference between the
  backends is an accepted deviation (tickets 0 and 9).
* **No per-entity ordering on either backend.** Two events of the same entity can arrive in any order; order them
  by `eventTime`.
* **Retention.** A row still undelivered after `tmforum.outbox.retention` (default `7d`) is discarded, logged with
  its event and counted in `tmforum.outbox.discarded`.
* **Metrics:** `tmforum.outbox.pending` (rows waiting), `tmforum.outbox.oldest.age` (age of the oldest waiting row)
  and `tmforum.outbox.discarded` (rows dropped by retention). The `tmforum.outbox.*` properties are listed in the
  [storage-postgresql README](../storage-postgresql/README.md#configuration).
* **DeleteEvents.** On PostgreSQL they come from the outbox and go to every hub taking `<Type>DeleteEvent` whose
  query matched the deleted entity (TMF630). On the broker they come from the broker's deletion notification
  through `NgsiLdEventHandler`, and go to every hub taking `<Type>DeleteEvent`, whatever its query: the
  notification carries only the id, so the query cannot be evaluated. Both send the same id-only payload.
  `DeleteEventBrokerIT` pins the broker side on Orion-LD; its first CI run is the first observation of that
  behaviour.
* **Broker DeleteEvents are delivered at all only since this release.** Before, the deleted entity was mapped to its
  VO twice, and the second mapping failed for every DeleteEvent. Now they are delivered, and only to hubs that take
  `<Type>DeleteEvent`; a hub registered for other event types no longer receives them.
* **TMF730 sub-types:** a PATCH of a sub-type (`@type` of a ResourceSpecification or LogicalResource sub-type) in
  software-management now fires an update event on every backend, the broker included. The resource-inventory and
  resource-catalog sub-types still fire no events, because their `EventMapper`s do not map them.

## Interim limits (until tickets 1 and 7)

* **No list filters or sorting.** `GET /<resource>` supports `offset`, `limit`, `fields` and the total count, but a
  filter or `sort` answers 500 until ticket 7 translates queries to `jsonb`.
* **No TMForum hubs.** Registering a hub (`POST /hub`) needs a broker until ticket 1, because it also creates an
  NGSI-LD subscription, and its duplicate check needs ticket 7. Looking up the hubs of an event also needs ticket 7,
  so the outbox keeps retrying such events until retention discards them.

## Tests and CI

* PostgreSQL (`k3s/postgresql-db`) is provisioned in every IT run next to the broker. `-Dbroker=postgresql` makes it
  the storage under test and starts no broker:
  `mvn verify -pl product-catalog -Dbroker=postgresql -Dcache=redis`.
* The port contract ITs run against PostgreSQL in every job and against NGSI-LD in the broker jobs.
* `test-brokers.yaml` runs the matrix {scorpio, orion-ld, postgresql} x {in-memory, redis}.
* ITs the storage under test cannot run yet carry a JUnit tag, and failsafe excludes `not-on-${broker}`
  (`common`'s `StorageTags`):
  * `@RequiresQueryTranslation` (list filters): lifted with ticket 7.
  * `@RequiresHubSupport` (hub registration/deletion and event delivery through the API): lifted when both
    tickets 1 and 7 have landed.
  * `@BrokerOnly` (NGSI-LD adapter, NGSI-LD subscriptions, `DeleteEventBrokerIT`): never runs on PostgreSQL.
  * `@PostgresqlOnly` (PostgreSQL-pinned ITs such as the outbox and one-query ITs): runs only with
    `-Dbroker=postgresql`.
* The ITs that seeded read-only resources (CustomerBill, AppliedCustomerBillingRate, ...) through the broker client
  now seed through the `EntityRepository`, so they cover GET on every backend.

## Known issues

* `software-management`'s `ResourceSpecificationApiIT#createResourceSpecification400` case "invalid
  resourceSpecRelationship id" fails on every backend, Orion-LD included, and fails on `main` as well: the base
  `ResourceSpecification` domain class has no `resourceSpecRelationship`, so the field is dropped before the id
  check in `TMForumMapper` runs, and the create answers 201. It is unrelated to the storage backend. The case is
  `@Disabled` (`createResourceSpecificationWithInvalidRelationshipId400`) until a follow-up ticket fixes the mapping,
  so the software-management matrix jobs stay a usable signal.
* `resource-order-management`'s `ResourceOrderApiIT#createResourceOrder201` fails on every backend, Orion-LD
  included, and on `main` as well: the expected order comes from the generated example with `state` `"string"`,
  which `TMForumMapper.mapResourceOrderState` maps to `null` because it is no `ResourceOrderState` value. It is
  unrelated to the storage backend, and the module is not in the `test-brokers` matrix.
