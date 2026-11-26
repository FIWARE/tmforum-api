# PR #183 — answers to the review comments

Two reviews on [PR #183](https://github.com/FIWARE/tmforum-api/pull/183):

- [`6071222210`](https://github.com/FIWARE/tmforum-api/pull/183#pullrequestreview-6071222210) (Mortega5) — 3 blocking, 11 should-fix, 6 minor, numbered `#1`–`#14` plus minor `M1`–`M6`.
- [`6071352382`](https://github.com/FIWARE/tmforum-api/pull/183#pullrequestreview-6071352382) (vramperez) — 7 inline comments, numbered `V1`–`V7`.

Each entry records the verdict and, where a fix was made, the commit it is in.

---

## Addressed

### #1 — Unbounded `limit`, no `statement_timeout` (blocking)

> `list()` only rejects `limit < 1`. With `?limit=2147483647` the query scans every row with `count(*) OVER ()` and
> maps all of them to domain objects. The pool has `max-size: 10`, so a few such requests exhaust it. `regex` filters
> and sort keys also run inside PostgreSQL with no timeout.

**Agreed, fixed** in `Bound the page size of a list and the runtime of a statement`.

Both halves of the comment are real and they are independent, so both are closed:

- **The page size.** `CommonConstants.MAX_LIMIT` (1000) is now the upper bound of a served page.
  `AbstractApiController.boundedLimit` clamps anything above it, in `list()` and in `listPolymorphic()`. Clamping
  rather than rejecting keeps existing clients working, and the value is not silent: the applied limit is what goes
  into `PaginationFilter.LIMIT_ATTR`, so the response header reports the page that was actually served. The
  `offset`/`limit` range check was split into `validOffset`/`boundedLimit` so the 400 names the parameter that was
  wrong instead of both.
- **The statement runtime.** The datasource now carries the driver's own `options` option,
  `statement_timeout=${POSTGRES_STATEMENT_TIMEOUT:30000}`, in every module's main and test
  `application-postgresql.yaml`. r2dbc-postgresql sends those `name=value` pairs as PostgreSQL startup parameters, so
  every session of the pool has the server cancel a statement that outlives the timeout and hand its connection back,
  instead of a regex or sort holding it for the whole request. This also covers **V1** (ReDoS through a client
  `.regex` filter) — a catastrophically backtracking pattern is now cancelled by the server.

Tests: `AbstractApiControllerTest.listClampsTheLimitToTheMaximum` and `listRejectsAnOutOfRangePaginationParameter`
(parameterized, both list paths); `StatementTimeoutIT` asserts the session really carries the configured timeout and
that the server cancels a statement over it. `storage-postgresql/README.md` documents the option and
`POSTGRES_STATEMENT_TIMEOUT`.

### #2 — `DELETE` does not check the type segment of the id (blocking)

> `delete()` only checks `IdHelper.isNgsiLdId(id)` […] `DELETE /productOffering/urn:ngsi-ld:category:1` returns 204,
> deletes the category and writes a DELETE outbox row for that type. `get()` and `patch()` reject the wrong type, so
> the three operations disagree on the same URL.

**Agreed, fixed** in `Reject a delete of an entity another endpoint owns`.

Closed at both levels the comment names, but not with a single entity class: the polymorphic endpoints
(`ResourceSpecifcationApiController`, `ResourceApiController`, …) legitimately delete every sub-type of their
registry, so `hasTypeSegmentOf(id, T.class)` would have turned a valid delete into a 404. What the endpoint owns is
exactly the comma-separated type string it already lists with, so that is what is passed down:

- `AbstractApiController.delete(String id, String acceptedTypes)` replaces `delete(String id)`. `ownedEntityId`
  rejects a non-NGSI-LD id and an id whose type segment is none of the accepted types, both with 404, so DELETE now
  agrees with GET and PATCH on the same URL. Removing the one-argument overload rather than adding a second one is
  deliberate — no call site can silently keep the unchecked path. All 33 controllers were updated to pass the same
  type expression they list with; `AbstractSubscriptionApiController` passes
  `TMForumSubscription.TYPE_TM_FORUM_SUBSCRIPTION`.
- `EntityEventPublisher.deleteWithEvent(URI id, List<String> acceptedTypes)` carries the types to the backend.
  `PostgresEntityRepository.DELETE_CURRENT_RETURNING` now reads
  `WHERE id = $1 AND is_current AND type = ANY($2)`, so even a caller that bypassed the controller cannot delete
  another endpoint's row — it gets NOT_FOUND and no outbox row. `DirectEntityEventPublisher` ignores the argument
  and says so in its Javadoc: the broker deletes by id alone, so the controller check is the only one available
  there, exactly as before this PR.
- `OrganizationApiController.deleteOrganization` no longer hand-rolls its own check; it delegates to
  `delete(id, Organization.TYPE_ORGANIZATION)`. That also closes **M6** — it threw `DeletionException` where every
  other controller throws `TmForumException` — since the shared method throws the latter.

Tests: `AbstractApiControllerTest.deleteRejectsAnIdOfAnotherType` and
`deletePassesAnIdOfAnAcceptedTypeToThePublisher` (parameterized, including a polymorphic endpoint with two accepted
types); `PostgresEntityRepositoryIT.deleteWithEventLeavesAnEntityOfAnotherTypeAlone` and
`deleteWithEventRemovesAnEntityOfAnAcceptedType` pin the SQL predicate.

**M3** (`ControllerStorageBoundaryTest` passes with this gap) follows from this change and is listed as still open
only for the test's own exemption list, which is unrelated to the gap itself.

---

### #3 — With no backend named the default is now PostgreSQL (blocking)

> A broker deployment at the default URL (sidecar, host network) without `MICRONAUT_ENVIRONMENTS` starts on an empty
> database and serves empty data after the upgrade. Log a WARN at startup naming the selected backend, or fail when
> no `POSTGRES_*` setting is present. `AGENTS.md` needs the same correction.

**Agreed, fixed** in `Say which storage backend a start selected`.

Taken as the WARN rather than the hard failure. Requiring a `POSTGRES_*` setting would reject the deployment the
defaults are written for — a database reachable at `localhost:5432` with the shipped credentials — and the case the
comment is really about (a broker at the shipped `http://localhost:1026`) would still pass that check, because such
a deployment sets no `POSTGRES_*` either way. What distinguishes it is that nothing was named at all, and that is
exactly what is now logged.

`StorageBackendValidator` logs:

- a **WARN** on the defaulted path, naming `postgresql` as the backend in use, saying the data comes from the
  PostgreSQL database, and naming `orion-ld`/`scorpio` as what a broker deployment has to set — so an upgraded
  deployment that silently moved to an empty database says so in its first lines of log;
- an **INFO** naming the backend whenever one was selected explicitly, for either backend, so the log always answers
  "which storage is this process on".

The existing hard failure is unchanged: a defaulted start that *does* carry broker settings (a non-shipped ngsi URL
or a `general.*` key) still aborts with the `ConfigurationException`.

`AGENTS.md` L84 was wrong in two ways and is corrected: it said the broker beans are active when no backend env is
set (they are not — `TmForumApplication.run` adds `postgresql`), and it said `conformance-test/api.yaml` deploys
without `MICRONAUT_ENVIRONMENTS`, while that file now sets `orion-ld`.

---

### V7 — Broker URL with userinfo reaches an exception message and the startup log

> On the defaulted-to-postgres path this puts the broker URL into a ConfigurationException (and startup logs). If the
> URL carries userinfo (`http://user:pass@host:1026`) the password gets logged.

**Agreed, fixed** in `Keep a broker url's password out of the startup report`.

Confirmed: `StorageBackendValidator` interpolated `ngsiUrl` raw into `UNNAMED_BROKER_MESSAGE`, which becomes a
`ConfigurationException` message and therefore also a startup log line. `withoutUserInfo` now replaces the authority's
userinfo with `***` before the url goes into the message. It works on the string rather than `URI.create`, because a
url that fails to parse is exactly the kind this path has to report, and parsing it would throw instead.

Tests: `userInfoIsRedactedFromAReportedUrl` (parameterized over `user:pass@`, `user@`, a path, and urls with nothing
to redact, which stay unchanged) and `userInfoIsRedactedFromTheConfigurationException`, which asserts the password
is absent from the thrown message.

---

### #9 / V5 — No indexes on `tmforum.entity`, and #8.3 / V2 — no index on `event_outbox`

> Every list filters by `type = ANY(...)`, orders by `created_at, id` and runs `count(*) OVER ()`, which is a
> sequential scan plus a sort per request.
> The CLAIM query runs every poll (~1s) […] but event_outbox has no index, so each poll seq-scans and sorts the whole
> table.

**Agreed, fixed** in `Index the list filter and the outbox poll`. Both were verified against the migrations:
`V1__entity.sql` defined only the primary key and the partial unique index on `id`, and `V2__event_outbox.sql` only
its identity primary key.

A new `V4__list_and_outbox_indexes.sql` rather than an edit of V1 and V2. The two are unreleased, so editing them
would be defensible, but a database a developer already migrated (the persistent `-Pdev` cluster) would then fail to
start on a checksum mismatch, which is a confusing failure for no gain.

| Index | Statement it carries |
|-------|----------------------|
| `entity_current_type_created_idx (type, created_at, id) WHERE is_current` | every list: the `is_current AND type = ANY($1)` filter and the `created_at, id` order an unsorted list uses, which is the default. `count(*) OVER ()` runs over the same rows, so it is covered too. A sorted list still sorts, but over the rows the index found rather than the table. |
| `event_outbox_type_due_idx (entity_type, next_attempt_at, id)` | `OutboxDispatcher.CLAIM`, which runs every poll interval (1s by default) and selects the due rows of this deployment's types ordered by `id`. `STATISTICS` filters on `entity_type` alone and uses the same index. |
| `event_outbox_type_created_idx (entity_type, created_at)` | `DISCARD_EXPIRED`, the retention sweep. |

A GIN index on `data` is deliberately not added: the filters the renderer produces are `EXISTS` over a jsonpath
selection, which `jsonb_path_ops` would not serve without rewriting the rendered SQL, and that is a larger change
than this review calls for. The list index plus the `MAX_LIMIT` bound and the `statement_timeout` from #1 bound what
an unindexed `data` filter can cost.

Tests: `SchemaMigrationIT.entityIndexesAreTheKeysAndTheListIndex`, `listIndexIsPartialOnTheCurrentRows` and
`outboxIndexesCoverTheClaimAndTheRetentionSweep`. The old `noOtherIndexExists` was the assertion that pinned the
absence of exactly these indexes; it is replaced rather than deleted, so the set of indexes stays pinned.

---

### V6 — `getSubscriptions` is `@Cacheable` on a key that is unique per call

> `getSubscriptions` is `@Cacheable` keyed on the whole `EventDetails`, which carries `eventTime = Instant.now()`, so
> the key is unique per call and the cache never hits — it only adds overhead.

**Agreed, fixed** in `Key the hub cache on what the lookup depends on`.

Confirmed: the annotation sat on a method whose single parameter is the whole `EventDetails`, so the generated key
included the event time and no two calls ever shared one. The lookup itself only reads `entityType` and `eventType`
— `subscriptionsTaking` used nothing else — so those two are now the method's parameters and therefore the key:

- `getSubscriptions(String entityType, String eventType)` carries the `@Cacheable`.
- `getSubscriptions(EventDetails)` stays as the entry point the broker path uses and just unpacks the two values.
  Micronaut's AOP is compile-time, so this self-call still goes through the interceptor, unlike a proxy-based one.
- `findSubscriptions` gained the same two-argument form, with the `EventDetails` overload kept for the outbox path
  and `NgsiLdEventHandler`.

The comment notes this is pre-existing but that the new `findSubscriptions` split assumes the cached one caches —
which is why it is fixed here rather than deferred.

Tests: `theCachedLookupTakesOnlyTheEntityTypeAndTheEventType` pins the arguments the cached method is called with,
which is what the cache key is derived from. `brokerDeliveryLooksUpTheHubsThroughTheCache` and
`outboxDeliveryLooksUpTheHubsUncached` keep pinning that the two paths use the cached and the uncached lookup
respectively.

---

### #4 — Several `regex` filters on one attribute are merged into one pattern

> `name.regex=^a;name.regex=^b` becomes the single pattern `^a,^b`. On the broker `toRegExp` rejects the comma (400).
> On PostgreSQL there is no such check, so the literal `^a,^b` is matched and the result is wrong without an error.

**Agreed, fixed** in `Keep two regex filters on one attribute apart`.

`combineParts` joined the values of every part sharing an attribute and an operator, which is right for exact
matches — `color=red;color=blue` is one comparison carrying both literals — and wrong for a regex, because
`toValues` deliberately does not split a pattern on commas. The merge now skips `REGEX` parts: each pattern stays
its own `QueryPart`, becomes its own `Comparison`, and the OR of the query combines them into an `AnyOf` like any
other pair of filters. Both backends then see two patterns, which is what was asked for.

Tests: `regexFiltersOnOneAttributeStayApart`, parameterized over two patterns on one attribute (now an `AnyOf` of
two comparisons), one pattern (unchanged), and an exact-match OR list (still combined, so the fix did not take the
merge away from the case it is for).

---

### #5 — `encodeValue` does not escape `"`

> String values are wrapped in `"` without escaping. `name=a"|description=="x` renders `name=="a"|description=="x"`,
> which the broker reads as an OR with an extra condition. The regex path already rejects these characters […] so
> the equality path is inconsistent with it.

**Agreed, fixed** in `Reject a value NGSI-LD's q would read as syntax`, taking the comment's second option.

Rejecting rather than escaping, because escaping has nowhere to go: neither Orion-LD 1.9.0 nor Scorpio 5.0.3
honours a backslash escape inside a quoted `q` value, so an escaped quote would be forwarded as a quote and end the
string anyway. The comment names rejection as the alternative, and it is what keeps the equality path consistent
with the regex path it is measured against — `REGEX_QUERY_SYNTAX` already rejects `"` for the same reason.

`encodeValue` now runs a string value through `quotable`, which throws `QueryException` (400) when it holds `"` or
`\`. The `!=` rewrite feeds its extracted value through the same path, so the idiom is covered, as asked.

This is renderer-local and therefore broker-only: on PostgreSQL the value is a bound parameter and a quote in it has
never been a problem, so the backends do not diverge further — the broker simply reports what it cannot express.

Tests: `testStringValueHoldingQuerySyntaxIsRejected` (a bare quote, the comment's own
`a"|description=="x`, and a backslash) and `testNotEqualsValueHoldingQuerySyntaxIsRejected` for the `!=` group. A
`DATE_TIME` literal is validated by the model before it reaches the renderer, so only a string can carry these.

---

### #11 — A broker 400 on list becomes a 500

> `listFailure` maps any `HttpClientResponseException` to `UNKNOWN`. Writes map a 400 to `INVALID_DATA`, reads do
> not. A `q` that the renderer lets through and the broker rejects (for example a multi-value `>`) returns 500
> instead of 400.

**Agreed, fixed** in `Report a filter the broker rejects as the client's error`.

Two places had to change, because mapping the reason alone would not have shown:

- `BrokerFailures.unknownOrUnavailable` now resolves an `HttpClientResponseException` through the same `reasonOf`
  the write path uses, so a 400 becomes `INVALID_DATA`. It also maps 503 and 504 to `UNAVAILABLE`, which is what
  those statuses mean and what the write path was already missing.
- `StorageExceptionHandler` mapped **every** LIST failure to `TmForumExceptionReason.UNKNOWN` (500), so the reason
  would have been discarded one layer later. LIST now renders `INVALID_DATA` as a 400 and everything else as before.

The hub registration's duplicate check runs through the same list path, so its broker 400 is now a 400 as well. That
is right rather than collateral: the query it runs filters on the callback the client sent, so a rejection of it is
also the client's error.

Tests: `ErrorResponseCharacterizationTest.listFails` is now parameterized over the broker's status with the response
expected for each — 400 for `BAD_REQUEST`, 500 for not-found, conflict, 500 and no answer — and
`registerFailsOnTheDuplicateCheck` uses the same table. Both run again under `ReplaceOnUpdateErrorResponseCharacterizationTest`.

---

### #7 — Database failures map to wrong statuses and text

> With the database down, create and update end in `UNAVAILABLE`, which returns 502 with "Context broker is not
> reachable." Read, list and delete end in `UNKNOWN` (500). `UNAVAILABLE_STATE_PREFIXES` is `08`, `53`, `57P0`, so
> statement timeout (`57014`), serialization failure (`40001`) and deadlock (`40P01`) also return 500.

**Agreed, fixed** in `Answer an unreachable storage with 503, whichever backend`. All three parts are done.

**UNAVAILABLE is now 503 for every operation, with backend-neutral text.** `StorageExceptionHandler` checks the
reason before the operation and answers `503 Service Unavailable` with "The storage backend is currently not
available." A storage that gave no answer is the same failure whether it was a broker or a database, and the
response now says so instead of naming the context broker — which was wrong text on PostgreSQL and the wrong status
(502) everywhere, since nothing upstream sent a bad response.

This changes the broker path too, deliberately: a broker outage on create/update was 502 and is now 503, and on
read/list/delete it was 500 and is now 503. The NGSI-LD *subscription* calls do not go through the storage port and
keep their old rendering; the characterization tests now hold the two apart explicitly
(`failedBrokerReads`, `failedBrokerDeletes`, `rejectedBrokerWritesUncaught`).

**`40001`, `40P01` and `57014` are transient.** A new `UNAVAILABLE_STATES` set carries them next to the existing
prefix set. Serialization failure and deadlock are retryable by definition. `57014` (query_canceled) is what the
`statement_timeout` from #1 raises, so this comment and that one meet here: the timeout now has a status, and it is
503 rather than 400 — the server gave up on the statement, which says nothing about the request being invalid, and
it was not previously covered by the `57P0` prefix.

Tests: `PostgresFailuresTest` gained the three states (the `57014` case flipped from `UNKNOWN` to `UNAVAILABLE` and
is renamed from "unclassified state" accordingly). `ErrorResponseCharacterizationTest` pins the 503 across create,
update, read, list, delete and the related-entity writes, under both the append and the replace update.

---

### M4 — `QueryParser.generalProperties` is unused

**Agreed, fixed** in `Drop the query parser's unread properties`.

The field carried a comment saying it is "not read since the parser stopped splitting on ngsildOrQueryValue; kept so
the constructor stays stable". Keeping an unread dependency to keep a signature stable is the wrong trade in a
module that is compiled together: it makes every construction site state a dependency that does not exist. The
field and the constructor parameter are both gone; the bean is constructed by its implicit no-argument constructor
and every call site drops the argument.

---

## Still open

The remaining comments are triaged but not yet worked. Nothing below has been changed in the branch.

| # | Comment | Verdict |
|---|---------|---------|
| #6 | `RELATIONSHIP` → `PROPERTY` in two getters | Needs an answer, not necessarily a fix: confirm the mapping library reads a stored Relationship through a `PROPERTY` setter, add an IT with such an entity, or document the migration. |
| #8 | Outbox delivery and housekeeping (5 sub-points) | **8.3's index is fixed** (above). Still open: per-hub delivery state (8.1) and paging past 100 hubs (8.2), which are real but larger; terminal failures (8.4) and un-owned types (8.5), separate small fixes. |
| #10 | `DATE_TIME` compares as text | Open question to answer first: do Orion-LD and Scorpio compare these as instants or as text? |
| #12 | Shallow PATCH and `NullNode` in extensions | Question + likely fix. Confirm per-attribute replace is the intended PATCH semantics on both backends; add a `NullNode` extension-value test and drop such values in full-document mode if it reproduces. |
| #13 | EXTERNAL references now fail on a local id collision | Deliberate per the code comment; needs a unit test pinning the behaviour. |
| #14 | `cleanup-subscriptions` is not all-or-nothing | Agreed. Skip the deletion and the drop when the report already holds an error after the read phase. |
| M1 | Broker-only beans carry no `@Requires(notEnv = postgresql)` | Agreed, convention per `AGENTS.md`. |
| M2 | `PostgresTestEntityCleaner` ships in the production jar | Agreed. |
| M3 | `ControllerStorageBoundaryTest` exempts `relatedEntityHandlingMono` and inspects only three methods | The #2 gap it used to pass over is closed; the exemption list itself is still worth tightening. |
| M5 | `IdHelper.hasTypeSegmentOf` throws outside a `Mono` | Agreed, wrap in `Mono.defer`. |
| V1 | ReDoS through a client `.regex` filter | **Closed by #1** — the `statement_timeout` cancels a backtracking pattern server-side. Bounding pattern length is still possible on top, but the unbounded-CPU failure mode is gone. |
| V3 | `^(?!X$).*$` → `!=` takes a regex fragment as a string literal | Agreed. Normalize in the shared parser (a model-level `NOT_EQUALS`), not only in the NGSI renderer. |
| V4 | `toRegExp` rejects `|,;()"'<>!`, postgres accepts them | Agreed, related to #4. Align or document. |
