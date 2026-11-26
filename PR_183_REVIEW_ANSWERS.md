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

---

## Still open

The remaining comments are triaged but not yet worked. Nothing below has been changed in the branch.

| # | Comment | Verdict |
|---|---------|---------|
| #2 | `DELETE` does not check the type segment of the id | Agreed, blocking. Needs care: `delete()` has ~60 call sites, and the polymorphic controllers (`ResourceSpecifcationApiController`, `ResourceApiController`, …) must keep accepting every sub-type of their registry, so the check cannot be a single `hasTypeSegmentOf(id, T.class)`. Plan: pass the accepted types (the same string the controller lists with) into `delete()`, 404 on a mismatch, and add `AND type = ANY($2)` to `DELETE_CURRENT` / `DELETE_CURRENT_RETURNING` as defence in depth. `deleteOrganization` bypasses `delete()` and needs the same check. |
| #3 | With no backend named the default is now PostgreSQL | Agreed, blocking. A broker deployment at the default URL and without `MICRONAUT_ENVIRONMENTS` silently starts on an empty database. Plan: WARN at startup naming the selected backend, and correct `AGENTS.md` L84, which still says the broker beans are active when no backend env is set. |
| #4 | Several `regex` filters on one attribute merged into one pattern | Agreed. `combineParts` must not join `REGEX` parts; emit one `Comparison` each and combine with `AnyOf`. |
| #5 | `encodeValue` does not escape `"` | Agreed. Escape `"` and `\`, or throw `QueryException`; same for the `!=` group. |
| #6 | `RELATIONSHIP` → `PROPERTY` in two getters | Needs an answer, not necessarily a fix: confirm the mapping library reads a stored Relationship through a `PROPERTY` setter, add an IT with such an entity, or document the migration. |
| #7 | Database failures map to wrong statuses and text | Agreed. `UNAVAILABLE` → 503 for every operation with backend-neutral text; `40001`/`40P01` as transient; `57014` (now reachable, see #1) needs its own status. |
| #8 | Outbox delivery and housekeeping (5 sub-points) | Partly agreed. The index (8.3) is the same as **V2** and is cheap. Per-hub delivery state (8.1) and paging past 100 hubs (8.2) are real but larger; terminal failures (8.4) and un-owned types (8.5) are separate small fixes. |
| #9 | No indexes on `tmforum.entity` | Agreed, same as **V5**. A partial `(type, created_at, id) WHERE is_current` covers the default list. |
| #10 | `DATE_TIME` compares as text | Open question to answer first: do Orion-LD and Scorpio compare these as instants or as text? |
| #11 | A broker 400 on list becomes a 500 | Agreed. `listFailure` should map a 400 to `INVALID_DATA` like the write path. |
| #12 | Shallow PATCH and `NullNode` in extensions | Question + likely fix. Confirm per-attribute replace is the intended PATCH semantics on both backends; add a `NullNode` extension-value test and drop such values in full-document mode if it reproduces. |
| #13 | EXTERNAL references now fail on a local id collision | Deliberate per the code comment; needs a unit test pinning the behaviour. |
| #14 | `cleanup-subscriptions` is not all-or-nothing | Agreed. Skip the deletion and the drop when the report already holds an error after the read phase. |
| M1 | Broker-only beans carry no `@Requires(notEnv = postgresql)` | Agreed, convention per `AGENTS.md`. |
| M2 | `PostgresTestEntityCleaner` ships in the production jar | Agreed. |
| M3 | `ControllerStorageBoundaryTest` exemption passes with the #2 gap | Follows from #2. |
| M4 | `QueryParser.generalProperties` unused | Agreed, remove. |
| M5 | `IdHelper.hasTypeSegmentOf` throws outside a `Mono` | Agreed, wrap in `Mono.defer`. |
| M6 | `OrganizationApiController` throws `DeletionException` | Agreed, align with the other controllers. |
| V1 | ReDoS through a client `.regex` filter | **Closed by #1** — the `statement_timeout` cancels a backtracking pattern server-side. Bounding pattern length is still possible on top, but the unbounded-CPU failure mode is gone. |
| V2 | No index on `event_outbox` for the CLAIM poll | Agreed, same as #8.3. Verified: `V2__event_outbox.sql` defines only the identity primary key. |
| V3 | `^(?!X$).*$` → `!=` takes a regex fragment as a string literal | Agreed. Normalize in the shared parser (a model-level `NOT_EQUALS`), not only in the NGSI renderer. |
| V4 | `toRegExp` rejects `|,;()"'<>!`, postgres accepts them | Agreed, related to #4. Align or document. |
| V5 | No index for `type` on `tmforum.entity` | Agreed, same as #9. Verified against `V1__entity.sql`. |
| V6 | `getSubscriptions` `@Cacheable` key is unique per call | Agreed, verified: the key is the whole `EventDetails`, which carries `eventTime = Instant.now()`, so the cache never hits. Key on `(entityType, eventType)`. |
| V7 | Broker URL with userinfo logged in a `ConfigurationException` | Agreed, verified: `StorageBackendValidator` interpolates `ngsiUrl` raw into `UNNAMED_BROKER_MESSAGE`. Redact. |
