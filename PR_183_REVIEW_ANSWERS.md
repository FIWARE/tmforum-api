# PR #183 — answers to the review comments

Two reviews on [PR #183](https://github.com/FIWARE/tmforum-api/pull/183):

- [`6071222210`](https://github.com/FIWARE/tmforum-api/pull/183#pullrequestreview-6071222210) (Mortega5) — 3 blocking, 11 should-fix, 6 minor, numbered `#1`–`#14` plus minor `M1`–`M6`.
- [`6071352382`](https://github.com/FIWARE/tmforum-api/pull/183#pullrequestreview-6071352382) (vramperez) — 7 inline comments, numbered `V1`–`V7`.

Each entry records the verdict and, where a fix was made, the commit it is in.

## Summary

Every comment of both reviews is answered below.

| Outcome | Comments |
|---------|----------|
| Behaviour fixed | #1, #2, #3, #4, #5, #7, #8.2, #8.3, #8.4, #8.5, #9, #11, #12 (the `NullNode` half), #14, M1, M3, M4, M5, V1, V2, V3, V5, V6, V7 |
| Answered; tests or documentation added, no behaviour change | #10, #13, V4 |
| Answered; nothing to change | #6, #12 (the PATCH-semantics half), M2 |
| Agreed, deliberately deferred | #8.1 |

Each entry quotes the comment, says what was decided and why, and names the commit and the tests. Where a comment
asked a question rather than reporting a defect, the entry answers it; where the answer is "no", it says so and
gives the reason rather than quietly skipping it.

The entries are in the order the work was done, which is roughly the order of risk: the three blocking comments
first, then the cheap and the far-reaching fixes, then the questions.


---

## The comments

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
`deleteWithEventRemovesAnEntityOfAnAcceptedType` pin the SQL predicate;
`OrganizationApiControllerTest.deleteOfAnIdOfAnotherTypeIsNotFound` pins it end to end on the controller the
comment singled out.

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
field and the constructor parameter were removed and the bean built by its implicit no-argument constructor.

**It came back two commits later, with a reader.** The fix for **V3** moves the not-equals normalization into the
parser, and that normalization is what `optimizeRegexNotEquals` switches, so `GeneralProperties` is a real
dependency of `QueryParser` again — `normalizeNotEquals` reads it. The net effect of the two commits is that the
field is used rather than carried, which is what this comment was about; the intermediate state is visible in the
history because the two were done as separate fixes.

---

### M5 — `IdHelper.hasTypeSegmentOf` can throw outside a `Mono`

**Agreed, fixed** in `Report an unmapped class on subscription, not on assembly`.

`get` ran the check before building the chain, so a class without `@MappingEnabled` threw
`IllegalArgumentException` from the call that only assembles the `Mono`. A caller that assembles now and subscribes
later — which is the normal shape here — never sees it through its own `onErrorMap`/`onErrorResume`, and on the
PostgreSQL adapter it escaped the `StorageException` wrapping that is supposed to hold for everything leaving the
adapter.

Both adapters now wrap the check in `Mono.defer`, so it arrives as an error signal. On PostgreSQL it is therefore
also wrapped as a `StorageException` like every other read failure, which is what
`PostgresEntityRepositoryTest.classWithoutMappingIsRejectedOnSubscription` now pins — it replaces the test that
pinned the throw-on-assembly behaviour rather than sitting next to it.

Tests: that one, plus `NgsiLdEntityRepositoryTest.getReportsAnUnmappedClassAsAnErrorSignal` for the broker adapter.
Both assert that assembling does not throw and that subscribing reports.

---

### M1 — Broker-only beans carry no `@Requires(notEnv = postgresql)`

> `NgsiLdEntityRepository` and the other broker-only beans carry no `@Requires(notEnv = postgresql)`; only
> `@Primary` on `PostgresEntityRepository` decides which one is injected. `AGENTS.md` lists this as the convention.

**Agreed, fixed** in `Gate the broker adapters on the broker backends`.

`NgsiLdSubscriptionRepository` already had the gate; `NgsiLdEntityRepository` and
`BrokerBackedReferencesRepository` did not, so on PostgreSQL they stayed wired, held their NGSI-LD client, and were
one forgotten `@Primary` away from being the injected bean. Both carry the gate now.

`@Primary` stays on the PostgreSQL adapters. With the gate there is only one bean to resolve, so it is redundant —
but it is also the thing that keeps a future broker-only bean that forgets its gate from silently winning, and it
costs nothing.

Tests: `StoragePortWiringTest.brokerAdapterAbsentOnPostgresql`, parameterized over all three broker adapters,
asserts they have no bean definition in a PostgreSQL context. The existing `portResolvesToSingleAdapter` still pins
that each port has exactly one bean on the broker side.

---

### #14 — `cleanup-subscriptions` is not all-or-nothing

> After `readHubs` (or the orphan listing) records an error, the loop still deletes the broker subscriptions found so
> far and `dropSubscriptions` still runs. A broker timeout in the middle of hub paging leaves a partial deletion and
> drops the PostgreSQL attribute, with a non-zero exit code.

**Agreed, fixed** in `Write nothing when the cleanup could not read what is referenced`, as suggested: the run stops
after the read phase when the report already holds an error.

The read phase is what establishes which subscriptions are referenced. With an incomplete picture, deleting the ones
that were found *and* dropping the attribute that records which hub owned which leaves a state the re-run cannot
reconstruct — the re-run reads the hubs again, but their subscription attribute is gone. The command now records
one more error saying nothing was written, and returns.

A failure of the deletion or of the drop still lets the other side run, unchanged: by then what is referenced is
known, so finishing is better than leaving half of it undone. The tests are split along that line
(`aFailedReadPhaseWritesNothing` and `recordsAWritePhaseFailureAndStillRunsTheOtherSide`), which is also the
distinction the comment draws.

The hub-shortfall case ("read N distinct hubs, but the broker counts M") is a read-phase error, so it now writes
nothing either; `countsDistinctHubsAndFailsOnAShortfall` asserts that. Its existing message already told the
operator to freeze writes and re-run.

---

### V3 — `^(?!X$).*$` → `!=` takes a regex fragment as a string literal

> This takes `group(1)` of the `^(?!X$).*$` idiom as a string literal for `!=`, but X is a regex fragment. For
> `foo.regex=^(?!a\.b$).*$` the broker renders `foo!="a\.b"` (excludes the backslash literal) while postgres runs
> the real regex (excludes `a.b`) — opposite results for `foo="a.b"`. Since the backend is meant to be transparent,
> normalize this in the shared parser (e.g. a model-level NOT_EQUALS operator) rather than only in the NGSI renderer.

**Agreed, fixed** in `Normalize the not-equals idiom into a model operator`, the way the comment asks: with a
model-level operator in the shared parser, not a patch in the NGSI renderer.

`ComparisonOperator.NOT_EQUALS` is new. `QueryParser.normalizeNotEquals` recognizes the idiom and rewrites it —
**only when X is a literal**, which is the part that makes the two backends agree. X holding a regex
metacharacter is a pattern, so it stays a `REGEX` and both backends run it as one; the comment's own
`^(?!a\.b$).*$` is exactly that case and no longer becomes a string comparison on either side.

The renderers now only render what the model says, and neither knows about the idiom:

- `NgsiLdQueryRenderer` renders `NOT_EQUALS` as `!=` and no longer rewrites anything. It lost its
  `GeneralProperties` dependency with the rewrite.
- `JsonbQueryRenderer` renders `NOT_EQUALS` as `<>` inside the existing per-value `EXISTS`, so PostgreSQL keeps the
  semantics the lookahead regex had there — "some value differs" — rather than gaining a new reading.

This also makes `optimizeRegexNotEquals` do what its own documentation claimed. The property said "Global, not
broker-only: PostgreSQL's POSIX regex has no lookahead either", but it was read only by the NGSI renderer, so on
PostgreSQL the idiom was always carried through as a raw lookahead pattern. Now the normalization happens before
either backend sees it, and neither depends on lookahead support. The property's Javadoc is corrected accordingly.

The storage contract's `REGEX_NOT_EQUALS` case built the idiom as a raw `REGEX` filter, which is no longer what the
parser produces; it is replaced by a `NOT_EQUALS` case, so the contract pins the operator both adapters now receive.

Tests: `QueryParserModelTest` gained four cases — a plain literal and a literal holding a comma normalize, while
`^(?!a\.b$).*$` and `^(?!a|b$).*$` stay regexes. `NgsiLdQueryRendererTest` and `JsonbQueryRendererTest` pin the two
renderings of the new operator.

---

### #13 — EXTERNAL references now fail on a local id collision

> An EXTERNAL-scope reference whose id matches a local entity of another type or version now fails with MISSING or
> VERSION_MISMATCH (400). Before it was skipped. The comment says this is deliberate, but it changes the contract
> for federated hrefs that reuse a local-looking urn. A unit test should pin the behaviour.

**Deliberate; kept, and now pinned by tests** in `Pin what an external reference colliding with a local id does`.

The behaviour stands. An external scope says where the entity is served from, not what its id means: an id that
names a local entity of another type or another version is a reference to *that* entity, and accepting it because
the href points elsewhere would let a caller point a `categoryRef` at a stored product-offering simply by adding an
external href. A federated href that reuses a local-looking urn and collides with a real local entity is a genuine
ambiguity, and the 400 says so rather than silently resolving to the wrong thing. Note the scope is narrow: when no
local entity carries the id, the external reference is still skipped as before.

The test table already covered the type collision. It now also covers the two version cases the comment names and
the no-collision case, so all four outcomes of an external reference are pinned:

| External reference | Outcome |
|--------------------|---------|
| id names no local entity | skipped, accepted |
| id names a local entity of an accepted type | resolved, accepted |
| id names a local entity of another type | unresolved, 400 |
| id names a local entity of the accepted type, another version | unresolved, 400 |
| id names a local entity of the accepted type and the pinned version | resolved, accepted |

---

### V4 — `toRegExp` rejects `|,;()"'<>!`, PostgreSQL accepts them

> `toRegExp` rejects any pattern containing `|,;()"'<>!` with a 400, but the postgres backend runs the same pattern
> fine as POSIX ARE. So `name.regex=(foo|bar)` is a 400 on the broker and a result set on postgres — the same query
> behaves differently per backend. Worth aligning (reject on both, or document the divergence).

**Documented, not aligned** — in `Document what the two backends do differently`.

Rejecting on both was the alternative, and it is the wrong one here: it would make this backend refuse perfectly
good POSIX regexes only because NGSI-LD's `q` cannot express them. That is a loss of function in the backend this PR
adds, traded for a uniformity nobody gains from — a client that sends `(foo|bar)` wants the alternation, and on
PostgreSQL it can have it.

The divergence was already recorded in `storage-api`'s `KnownDeviation.REGEX_HOLDING_QUERY_SYNTAX_IS_REJECTED`,
which the contract test cases that hit it are tagged with, so the contract already states per backend what each is
expected to do. What was missing is a place an operator reading about this backend would find it, so
`storage-postgresql/README.md` gained a **Divergences from the broker backends** section listing all of them: the
rejected regex characters, Scorpio's anchored-regex behaviour, the string values #5 now rejects, and the
`statement_timeout` from #1.

It also says what is *not* a divergence any more: after V3 the not-equals idiom is normalized before either backend
sees it.

---

### #8.2 — `findSubscriptions` reads one page of 100 and never continues

> So the 101st matching hub gets no event and the row is deleted as delivered. Page until the result is exhausted.

**Agreed, fixed** in `Read every page of the hubs taking an event`.

The lookup read offset 0, limit 100, and stopped. Past the hundredth hub on one event the notification was never
built, and because nothing failed, the outbox settled the row as delivered — the event was lost silently, which is
the worst shape this can take. It now reads page after page until one comes back short.

`MAX_SUBSCRIPTION_PAGES` (100) bounds the loop, so a backend that keeps answering with a full page cannot spin
forever; at the page size that is 10 000 hubs on a single event.

The comment in `NgsiLdEventHandler` that recorded the limitation ("findSubscriptions reads only the first page, so a
hub beyond it loses the DeleteEvent") is gone with the limitation.

Tests: `findSubscriptionsReadsEveryPage`, parameterized over 0, 1, one less than a page, exactly a page, one more
than a page, and two pages plus three — the exact-page case is the one that makes the loop ask for a page it will
find empty.

---

### #8.4 — a row nothing can deliver is retried until the retention

> A type without a mapping, a document that cannot be read, or a hub that always answers 404/410 is retried until
> retention. Treat mapping and deserialization failures as terminal and drop the row with a WARN.

**Agreed for the two the comment names**, in `Drop an outbox row no retry can deliver`.

A missing event mapping and a document that does not read back as its type both change only with a deployment, so
every one of the attempts up to the retention does the same work and fails the same way, while the row sits in the
table and in the pending gauge. Both now raise an `UndeliverableRow`, and the dispatcher settles such a row by
deleting it with a WARN naming the cause, counted as `tmforum.outbox.discarded{reason=undeliverable}` — the
retention sweep is now tagged `reason=expired`, so the two are told apart on the same counter.

**Not the hub answering 404/410.** That is a remote system's answer, and a hub that is being redeployed or
reconfigured answers 404 for a while; dropping its events on the first one would lose them for a transient
condition. Retrying with the backoff until the retention is the right behaviour there, and the retention is the
bound. The two cases this fixes are different in kind: they are facts about *this* deployment, which no amount of
waiting changes.

Tests: `anUndeliverableRowIsDroppedInsteadOfRetried`, parameterized over a type with no event mapping and a document
whose shape does not match its class, asserts the row is gone, the hub was never called, and the counter carries the
reason.

---

### #8.5 — `DISCARD_EXPIRED` and `STATISTICS` filter on the owned types

> Rows of a type no longer in `EventTypes` are never discarded or counted.

**Half agreed, fixed in the half that is safe**, in `Report the outbox rows no dispatcher owns`.

The scoping of the sweep itself stays, and deliberately: a module that swept every expired row would delete another
deployment's events while that deployment is still retrying them, which is worse than the leak. The existing IT
pins that ("A row of a type the dispatcher does not own should stay"), and that is the right contract.

What was wrong is that such rows were also invisible. A type dropped from `EventTypes` leaves rows no dispatcher
ever claims, delivers, discards or counts — the table grows and nothing says so. Housekeeping now also counts the
rows whose type this deployment does not own and publishes them as `tmforum.outbox.unowned`, with one WARN when the
count first becomes non-zero (it does not change by itself, so repeating the WARN every minute would be noise).

They are then removed deliberately, by whoever knows that no deployment owns the type any more — which is a fact no
single process can establish on its own.

Tests: `housekeepingDiscardsExpiredOwnedRowsAndReportsTheRest` asserts the new gauge next to the assertions that
already pinned the scoping.

---

### #12 — Shallow PATCH and `NullNode` in extensions

> `data || patch` replaces each top-level attribute whole, so a nested object in the patch replaces the stored one
> instead of being merged. Is that the intended PATCH semantics on both backends (NGSI-LD replaces per attribute)?
>
> `removeEmptyAttributes` [...] an extension attribute holding a `NullNode` is not removed there. [...] Worth a test
> with a `NullNode` extension value; if confirmed, drop `NullNode` values in the full-document mode.

Two separate points. The first is answered, the second was confirmed and fixed in
`Keep a null extension value out of the stored document`.

**The shallow merge is intended.** NGSI-LD replaces an attribute whole, and the PostgreSQL adapter matches it on
purpose, so the same PATCH against either backend produces the same stored entity. A deep merge would be a
divergence, and it would also make it impossible to shrink a nested object — a client could add keys to it but
never remove one. `storage-api`'s contract test runs the same partial-update cases against both adapters, which is
where that equivalence is held.

**The `NullNode` is a real defect.** Writing the test the comment asks for reproduced it immediately: with the
extension value built as a tree node rather than as a Java null, a create stored
`{"id":…,"name":…,"customField":null}`. That contradicts "a stored document never holds null", and the merge reads
a null value as "remove this key", so the next patch carrying the same extension would delete it.

It is fixed where the value is written rather than in `removeEmptyAttributes`: `ExtensionPropertiesWriter` already
skipped a Java null, and now treats a `NullNode` as one. That covers both document modes, so neither a create nor a
patch can put a null into the document.

(A JSON `null` in a request reaches the domain object as a Java null, which was already skipped and tested — the
`NullNode` only appears when a value is built as a tree node. The test now covers both, in both modes.)

---

### #6 — `RELATIONSHIP` to `PROPERTY` in two getters

> The other relationship changes in this PR add `fromProperties = true` on the setter. These two also change the
> getter to `PROPERTY` with `embedProperty = true`, so what is written changes. Existing Orion/Scorpio data holds
> these as Relationships, and the new setters read Properties. After the upgrade an old `EndpointRef` or
> `associationSpec` can read back as null or fail.

**Answered; no change needed.** The observation is right — I confirmed these are the only two getters in the PR that
move away from `RELATIONSHIP`, against eleven that stay — but the conclusion does not follow, for two reasons.

**They are required to be `PROPERTY`, and a test enforces it.** Both fields live on a *relationship target* class
(`EndpointRef`, `EntitySpecificationRelationship`). The whole point of this PR is that a relationship target is
stored inline, and `RelationshipsReadFromPropertiesTest.everyRelationshipTargetIsStoredInline` in `all-in-one`
fails the build for any getter on such a class that is not `PROPERTY` with `embedProperty = true`. A nested
`RELATIONSHIP` cannot be expressed inside an embedded property in the first place. The other eleven are getters on
the *owning* entity, pointing at a target — a different position, which is why they stay relationships.

**There is no stored Relationship to migrate.** These two fields were not written before this PR at all.
`RELEASE_NOTES_INLINE_REFERENCES.md` lists them by name among the "`RefEntity` subclasses whose extra fields were
silently dropped on write now store them": `EndpointRef.isRoot`/`connectionPoint` and
`EntitySpecificationRelationship.associationSpec`. A field that was dropped on write left nothing in Orion or
Scorpio, so there is no old shape for the new setter to fail on — an upgraded deployment reads them back absent,
which the release notes already state for every reference field under "Data written before this release reads back
with only the fields that were embedded at the time".

So neither a test with a legacy entity nor a migration note is owed: the test that would have caught the problem
already exists as a build-time guard, and the data the migration would convert was never written.

---

### M3 — `ControllerStorageBoundaryTest` exempts any method named `relatedEntityHandlingMono`

> It exempts any method named `relatedEntityHandlingMono` and only inspects `createDomainEntity`,
> `updateDomainEntity` and `deleteDomainEntity`. It passes with the `DELETE` gap from #2.

**Agreed on the exemption, fixed** in `Exempt one method, not every method of that name`. The `DELETE` gap itself is
closed by #2.

The exemption matched on the method name alone, so any class anywhere could have called a storage write from a
method it named `relatedEntityHandlingMono` and the rule would have passed it. It now also requires the declaring
class to be `AbstractApiController`, which is the one method the exemption is for.

The three write methods are the right set to inspect: they are the whole write surface of the storage port, and the
rule's job is that each of them goes through `EntityEventPublisher`. `delete()` not checking the type was never
something this rule could have seen — it is an argument the controller did not pass, not a call it made past the
port.

---

### #10 — `DATE_TIME` compares as text

> The Javadoc says this follows the brokers. With mixed offsets or fractional seconds the text order differs from the
> instant order (`...:00.5Z` sorts before `...:00Z`). Do Orion-LD and Scorpio compare these as text or as instants?
> […] If they compare text, this can stay, with a test that documents it.

**Kept as text and documented** in `Say what a date-time filter compares on PostgreSQL`, which is the outcome the
comment allows for — but two things in it need correcting, and one is left as a decision.

**The divergence is not limited to foreign writers.** I checked: a declared date-time field is written as
`Instant.toString()` (`keysAndDatesIgnoreTheApplicationMapperSettings` pins that), which is canonical UTC. So the
offset case cannot arise from this API's own writes — but the fractional-second case can, because
`Instant.toString()` omits the fraction when it is zero and prints it when it is not. `...T10:00:00.500Z` sorts
below `...T10:00:00Z` in byte order and above it in instant order, for data this API wrote itself.

**The "as on the brokers" claim is unverified.** I could not settle what Orion-LD 1.9.0 and Scorpio 5.0.3 do from
this repository, and guessing it into a contract expectation would be worse than saying so. The Javadoc no longer
asserts it.

**Why it stays text for now.** The cast the comment suggests needs a zone for the literal forms the model accepts
and `DateTimeLiterals` parses today — `2024-01-01T10:00:00` and `2024-01-01` carry none. Picking one (UTC? the
server's?) is a semantic decision that should be made once for both backends, together with the answer to the
broker question, rather than taken here as a side effect.

What is in place instead: `dateTimes()` in `JsonbQueryRendererTest` already covered every literal form
(`Z`, a fractional second, an offset, no offset, date-only) — its comment now states the consequence rather than
claiming parity, the renderer's Javadoc spells out both cases that diverge and what fixing it would take, and
`storage-postgresql/README.md` says it where an operator will read it.

---

### #8.1 — A row is delivered to every matching hub and fails unless all acknowledge

> With 50 hubs and one down, each retry (backoff up to 1h, retention 7d) resends to the 49 healthy hubs, each time
> with the same `eventId`. Keep per-row delivery state (for example an `acked_callbacks` column, or one row per
> subscription).

**Agreed as a real cost; not fixed here, and I would rather it were taken as its own change.** This is the one
sub-point of #8 left open.

The mechanics are as described: `deliverNotifications` attempts every callback (`flatMapDelayError`, so one dead
hub does not stop the others) and the row only settles as delivered when all of them answered 2xx. A single dead hub
therefore makes every healthy hub receive the event again on each retry.

What keeps it from being a correctness bug is the `eventId`: `outboxEventId` derives it from the row id and the
event type, so every resend of a row carries the same id and a consumer deduplicates on it. The guarantee the
outbox advertises is at-least-once, and this is the "at least" being exercised — loudly, but correctly.

Fixing it properly means per-row, per-callback delivery state, and that is a schema and a protocol decision rather
than a patch:

- an `acked_callbacks` column means the claim, the settle and the retention sweep all have to read and write it, and
  a hub deleted between two attempts has to stop blocking the row;
- one row per subscription means the writer has to know the hubs at write time — which it deliberately does not, so
  that a hub registered after the write still gets the event — or a fan-out step has to materialize them;
- either way the metrics, the discard path and `OutboxDispatcherIT`'s claim/settle cases change shape.

That is a bigger change than the rest of this review put together, it touches the part of the PR with the strongest
correctness guarantees, and it is not needed for correctness. The deployment shape it hurts (dozens of hubs on one
event type, one of them down for hours) also has a cheaper mitigation available today: lowering
`tmforum.outbox.max-backoff` is the wrong direction, but raising it toward the retention cuts the number of
resends.

Recommendation: a follow-up ticket, with per-callback state in `V5`.

---

### M2 — `PostgresTestEntityCleaner` runs `DELETE FROM tmforum.entity` and ships in the production jar

**Answered; no change.** The class is in `src/main`, but it cannot run outside a test: it carries
`@Requires(env = Environment.TEST)`, so Micronaut never creates the bean unless the test environment is active.

It is also where the project puts this kind of helper on purpose. The port it implements, `TestEntityCleaner`, and
the base class that uses it, `AbstractApiIT`, both live in `common/src/main/java/.../common/test/` — `AGENTS.md`
records that as the convention ("test helpers in main code"), because the 21 API modules consume them from the
published `common` artifact. `NgsiLdTestEntityCleaner`, the broker counterpart, sits right next to them with the
same `@Requires(env = Environment.TEST)` guard. Moving only the PostgreSQL one into a test-jar would make the two
implementations of one port live in different places and would break every module IT that resolves it.

If the guard is judged too thin for a `DELETE FROM`, the change worth making is to all three classes at once and to
the convention behind them, not to this one.

---
