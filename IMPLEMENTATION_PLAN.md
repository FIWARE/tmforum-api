# Implementation Plan: TMForum - DB Backend - Query Translation

## Overview
Split `common/.../querying/QueryParser` at an explicit AST boundary. A backend-neutral query model in `storage-api` (filter tree, value types, sort keys) is produced by the TMForum-syntax front half and rendered by two permanent renderers: the existing NGSI-LD `q`/`orderBy` renderer, and a new JSONB/SQL renderer in `storage-postgresql`. The storage port then takes the model instead of NGSI-LD strings, and `PostgresEntityRepository` gains filtering, ordering and pagination (always `WHERE is_current`). The steps end with the filter, sort and paging ITs passing on PostgreSQL, Orion-LD and Scorpio.

Ground rules for every step:
- Keep the NGSI-LD ITs green after every step. The NGSI-LD renderer stays permanent and is tested on equal footing (decision 7).
- Do not change TMForum filter syntax or semantics. `SubscriptionQueryResolver` stays out of scope.
- No index tuning (decision 12). Bind every user value as an SQL parameter and never concatenate one into SQL.
- A behaviour difference between the backends either reaches parity or is recorded: a `KnownDeviation` entry (contract tests) plus a line in `release-notes/RELEASE_NOTES_POSTGRESQL_ADAPTER.md`.
- Doc density stays sparse (see AGENTS.md). Use named constants and parameterized tests.

## Steps

### Step 1: Backend-neutral query model in `storage-api`
Add the package `org.fiware.tmforum.storage.query` in `storage-api/src/main/java`. It holds plain Java records and enums with no dependencies, because the enforcer bans `io.github.wistefan`, `io.micronaut` and `org.fiware.tmforum` there:
- `ValueType` {`STRING`, `NUMBER`, `BOOLEAN`, `DATE_TIME`}. The first three are the neutral twins of the mapping library's `QueryAttributeType`, which `storage-api` must not see. `DATE_TIME` has no twin there (the library reports dates as `STRING`); it exists because byte order of ISO strings is wrong across precisions and offsets (`…00.500Z` < `…00Z`, since `.` < `Z`).
- `DateTimeLiterals`, a small `java.time`-only helper: `Optional<OffsetDateTime> parse(String)` accepts an ISO-8601 date-time with offset or `Z`, a local date-time (read as UTC) and a date (start of day UTC). Both halves use it: the front half to pick `DATE_TIME` (Step 2), the JSONB renderer to bind the literals (Step 4).
- `ComparisonOperator` {`EQUALS`, `GREATER_THAN`, `GREATER_THAN_EQUALS`, `LESS_THAN`, `LESS_THAN_EQUALS`, `REGEX`}. There is no not-equals: the regex not-equals rewrite is an NGSI-LD renderer optimisation (Step 2).
- `AttributePath(List<String> segments, boolean declared)`. The segments are the TMForum path after JSON-LD reserved-token translation (`@type` → `atType`, ...). `declared == false` marks a free-form extension attribute.
- `QueryFilter`, a sealed interface with `Comparison(AttributePath attribute, ComparisonOperator operator, ValueType valueType, List<String> values)` (values OR-ed, already validated and trimmed for their type; each value is one exact literal that no renderer splits further; a `REGEX` comparison carries exactly one pattern, see Step 2), `AllOf(List<QueryFilter>)` and `AnyOf(List<QueryFilter>)`.
- `SortKey(AttributePath attribute, ValueType valueType, boolean descending)`. `valueType` is null for an undeclared (extension) attribute: a sort has no literal to infer a type from.
- `EntityQuery(Class<?> queryClass, QueryFilter filter, List<SortKey> sort)`. `filter` may be null, and `sort` is empty when there is no sort. `queryClass` is the class paths were resolved against; the NGSI-LD renderer needs it for `findEntitiesPolymorphic`, which has no entity class.

The compact constructors reject null or empty lists where they make no sense, and copy lists immutably. Records give `equals`/`hashCode`, which matters if a call ever becomes a cache key.

Do not change the port yet. Add a unit test in `storage-api/src/test/java/.../query/` (parameterized) for the constructor invariants.
Acceptance: `mvn -pl storage-api verify` passes, enforcer included.

### Step 2: Split `QueryParser` into parse and NGSI-LD render (NGSI-LD output unchanged for every existing test)
In `common/src/main/java/org/fiware/tmforum/common/querying/`:
- **Front half, `QueryParser`.** Add `ParsedQuery parse(Class<?> queryClass, String queryString)` returning `ParsedQuery(String ids, String types, QueryFilter filter)`, and `List<SortKey> parseSort(Class<?> queryClass, Map<String, List<String>> parameters)`. The parse moves here unchanged:
  - well-known-parameter removal and the AND(`&`)/OR(`;`) split, including the "combining is not supported" `QueryException`;
  - `parseParameter`/operator detection, OR-grouping per attribute and operator (`combineParts`), and `,`-value splitting (never for `.regex`);
  - JSON-LD reserved-token translation, the `id`/`type` shortcuts into `ids`/`types`, and attribute type resolution through `JavaObjectMapper.getNGSIAttributePath`. An empty path means `declared = false`, with the type inferred from the literal as in today's `getPathToAdditionalAttributes`;
  - all validation (`validateOperator`, the boolean/number literal checks), so both backends answer the same 400s.
- **Semantics the front half fixes** (so the renderers cannot drift apart):
  - *Date-time.* When the resolved path ends at a declared field of a date-time Java type (`Instant`, `OffsetDateTime`, `ZonedDateTime`, `Date`), the type is `DATE_TIME`, resolved from the getter's return type, not from `QueryAttributeType`. It is `DATE_TIME` only if every literal parses through `DateTimeLiterals` (offset or `Z`, local date-time, date); otherwise the comparison stays `STRING`, as today. No new 400: the ticket keeps TMForum filter semantics out of scope, and `lastUpdate.gt=2024-01-01` keeps working on the brokers. The model keeps the literal as given, and the NGSI-LD renderer renders `DATE_TIME` exactly like `STRING`, so its `q` output is unchanged. `.regex` on a date-time field yields a `STRING` comparison that matches the stored string. `validateOperator` keeps checking against the mapping library's `QueryAttributeType`, which reports dates as `STRING`, so `.regex` stays allowed there; do not move that check onto `ValueType`.
  - *Regex OR.* `combineParts` keeps joining OR-ed regex values with `,`, and regex values are never split. So `a.regex=x;a.regex=y` stays one literal pattern `x,y` on every backend, as on the brokers today. A `REGEX` comparison therefore always has exactly one value. Do not change this here.
  - *Separators inside a value.* The split in `encodeStringValue` moves to the front half. A parsed string value that contains `ngsildOrQueryValue` becomes separate OR-ed values, so `name=a|b` (with `|` configured) matches `a` or `b` on every backend, PostgreSQL included. With the default `,` this never happens, because the front half has already split on `,`. The `;` branch is unreachable from parsed input, since `;` already separated the parameters. After this, a model value is always one exact literal, and the NGSI-LD renderer quotes it without splitting. Callers that build a `Comparison` directly (the hub check, Step 3) can then pass a URL containing `,`, `;`, `|` or `&` safely. No `QueryParserTest` case has a separator inside a value, so they all stay byte-identical. Only a value containing a non-default separator under `includeAttributeInList=true` renders differently (`(name=="a"|name=="b")` instead of `name==("a"|"b")`), with the same meaning; add a renderer test for it.
  - *Relationship without `.id`.* A filter path that is exactly a relationship attribute (`productSpecification=urn:...`, `isRelationship` on a one-segment path) gets `id` appended, so the model always points at the referenced id. The NGSI-LD renderer strips a trailing `.id` on relationships anyway, so its output is unchanged, and the JSONB renderer needs no relationship flag. Sort paths are not normalised, because today's `orderBy` keeps them as given.
  - *Undeclared sort keys* get `valueType == null` (see Step 1).
- **Back half, new `NgsiLdQueryRenderer` (`@Singleton`, takes `GeneralProperties`).** It provides `String renderFilter(EntityQuery)` and `String renderOrderBy(EntityQuery)`. Everything NGSI-LD moves here: the NGSI path through `getNGSIAttributePath`, `isRelationship`, `escapePath`/reserved-word escaping, the `.id` stripping for relationships, `mapPathPart` (`useDotSeperator`), `encodeValue`/`encodeStringValue`, `includeAttributeInList`/`encloseQuery`/`ngsildOrQueryKey`/`ngsildOrQueryValue`, the operator symbols (`Operator.getNgsiLdOperator`), the `;desc` suffix and `applyRegexNotEqualsOptimization`.
- Keep `toNgsiLdQuery(...)` and `toOrderBy(...)` for now as thin façades (`parse` + render, still returning `QueryParams`), so no caller changes in this step.
- **Pitfall: output order.** Today's OR grouping uses `Collectors.toMap` (HashMap iteration order), and the `QueryParserTest` expectations depend on it (e.g. `color==\"Red\"|(status...)`). Keep the same grouping so every existing `QueryParserTest` case stays byte-identical. Do not "fix" the order here.
- Tests:
  - `QueryParserTest` stays unchanged and green.
  - Add `QueryParserModelTest`, parameterized TMForum string → expected `EntityQuery`/`ParsedQuery`, covering reserved tokens, extension attributes, regex with `,` and with `;`-OR, `DATE_TIME` resolution (offset, `Z`, local date-time, date-only, and the `STRING` fallback for a non-date literal), the `ngsildOrQueryValue` split inside a value, relationship `id` normalisation, undeclared sort keys, and the 400 cases.
  - Add `NgsiLdQueryRenderer` tests over hand-built models for every `GeneralProperties` combination already covered by `QueryParserTest`, plus a value containing `,`, `;`, `|` and `&` that must come out as one quoted literal.

Acceptance: `mvn -pl common -am test` is green, and the NGSI-LD output of every existing test case is unchanged.

### Step 3: Migrate the storage port and its callers to the model
- `storage-api/.../EntityReadRepository`: replace `String query`/`String orderBy` with `EntityQuery query` (null means unfiltered and unordered):
  - `findEntities(Integer offset, Integer limit, Class<T> entityClass, EntityQuery query, String ids, String types)`;
  - the convenience overload `findEntities(offset, limit, entityType, entityClass, EntityQuery query)`;
  - `findEntitiesPolymorphic(offset, limit, types, EntityQuery query, ids, typeToClass)`.

  Drop the "transitional NGSI-LD coupling" paragraph from the Javadoc.
- `common/.../repository/NgsiLdEntityRepository`: inject `NgsiLdQueryRenderer` and render the filter and the orderBy right before `entitiesApi.queryEntities`. A null query passes null as today.
- `common/.../rest/AbstractApiController.list`/`listPolymorphic`: use `queryParser.parse(...)`/`parseSort(...)` and build the `EntityQuery` with the entity class or the query class. `ids`/`types` still come from `ParsedQuery`, and `hasFilter`'s mutation ordering is kept.
- `common/.../notification/TMForumEventHandler.findSubscriptions`: build the `entities`/`eventTypes` filter directly from model types (`Comparison`s on `AttributePath(["entities"])`/`["eventTypes"]`), not through a formatted query string.
- `common/.../rest/AbstractSubscriptionApiController.assureNotExistingTMForumSubscription`: remove the `String.format`-into-rendered-query hack and the NGSI-only `ForbiddenCharacterEscaper` coupling.
  - Filter on `callback` only, and compare `getRawQuery()` in Java while paging through the matches. Hubs sharing one callback are few, so this is neutral across backends: NGSI-LD stores `rawQuery` escaped, while the PostgreSQL document stores the raw value.
  - Build that filter directly as `Comparison(AttributePath(["callback"], true), EQUALS, STRING, [callback])`. Never pass the callback through `parse(...)`: a callback with a query string (`https://x/cb?a=1&b=2,3`) would be split on `&`, `,` and `;`. The renderer quotes the value unsplit (Step 2).
  - Keep the 409 behaviour. Add a hub test (`AbstractSubscriptionApiControllerTest` and an API IT on Orion-LD) whose callback is `…?a=1&b=2,3;c=4` and whose second registration answers 409. If a broker cannot match such a literal in `q`, record it as a deviation rather than escaping in the model.
- `storage-postgresql/.../PostgresEntityRepository`: adapt the signatures and keep rejecting a non-null filter or a non-empty sort with `LIST_NOT_SUPPORTED`. Implementation comes in Step 5.
- Delete the façades `QueryParser.toNgsiLdQuery`/`toOrderBy` and `QueryParams.query` if nothing uses them any more.
- Update every affected test:
  - `common`: `AbstractApiControllerTest`, `AbstractSubscriptionApiControllerTest`, `TMForumEventHandlerTest`, `NgsiLdEntityRepositoryTest`, `NgsiLdBaseRepositoryErrorTest`, `CharacterizationTestController`/`CharacterizationSubscriptionController`;
  - `storage-api`: `EntityReadRepositoryTest` and `AbstractEntityRepositoryContractTest` (pass `null` where they passed null strings);
  - `storage-postgresql`: `PostgresEntityRepositoryTest`/`IT`.

  Only `common` calls these methods in main code; the API modules just inject `QueryParser` and pass it to `super`.

Acceptance: `mvn clean install -DskipITs` is green, and the product-catalog and customer-management ITs (hub tests included) are green on Orion-LD.

### Step 4: JSONB/SQL renderer in `storage-postgresql`
Add `JsonbQueryRenderer` (package-private or a plain class; no bean needed) in `storage-postgresql/src/main/java/.../postgresql/`. It renders an `EntityQuery` into `SqlFragment(String sql, List<Object> bindings)`, starting at a given parameter index, for both the `WHERE` part and the `ORDER BY` part. It works against `data` of `tmforum.entity`. Keys are the domain property names, because `PostgresDocumentMapper` pins the naming; dates are ISO-8601 strings; extensions are top-level keys (`PostgresMappingModule`).

Suggested shape (the step agent may refine it, but must keep bind parameters and type-correct comparisons):
- **Path.**
  - Build a lax jsonpath with each segment as a quoted, escaped (`"` and `\`) member accessor followed by `[*]`, e.g. `$."relatedParty"[*]."role"[*]`. In lax mode `[*]` unwraps arrays and wraps scalars, so list-valued references (inline per ticket 5), lists of strings (`entities`, `eventTypes`) and plain values all behave as "any element matches", like NGSI-LD.
  - Bind the jsonpath text as a parameter (`$n::jsonpath`); it is never concatenated into the SQL.
  - Undeclared (extension) attributes use their segments as-is from the top level. Do not apply NGSI-LD reserved-word escaping (`tmfEscaped-...`); that is a broker-only artefact.
- **Comparison.** Use `EXISTS (SELECT 1 FROM jsonb_path_query(data, $n::jsonpath) AS v(value) WHERE <predicate>)`, with a predicate per `ValueType`:
  - `STRING`: `jsonb_typeof(v.value) = 'string'`, then `v.value #>> '{}' = ANY($m::text[])` for equality, or `(v.value #>> '{}') COLLATE "C" <op> ANY($m::text[])` for ordering. `COLLATE "C"` gives byte order, the closest match to the brokers.
  - `NUMBER`: `jsonb_typeof(v.value) = 'number' AND (v.value)::numeric <op> ANY($m::numeric[])`.
  - `BOOLEAN`: `v.value = ANY(...)` over `to_jsonb` booleans.
  - `DATE_TIME`: `<ts> <op> ANY($m::timestamptz[])`, with the literals parsed through `DateTimeLiterals` and bound as `OffsetDateTime`. Equality compares instants, so `…00Z` equals `…00.000Z` and `…01:00+01:00`.
    - `<ts>` is a guarded cast: `CASE WHEN jsonb_typeof(v.value) = 'string' AND pg_input_is_valid(v.value #>> '{}', 'timestamptz') THEN (v.value #>> '{}')::timestamptz END` (k3s runs `postgres:18`; `pg_input_is_valid` exists since 16). Use `CASE`, not `AND`, because PostgreSQL does not promise to evaluate `AND` operands in order.
    - The guard matters. Stored data is not only what this mapper wrote: ticket 8 migrates broker data, and a polymorphic query resolves the path against the query class while a subtype can hold another shape there. Without it, one unparseable value aborts the whole list with a 500. With it, such a value matches nothing and sorts as NULL. Cover it in `PostgresQueryRenderingIT`.
    - Pin the session `TimeZone` to `UTC` so stored strings without an offset read as UTC, the same rule `DateTimeLiterals` applies to literals.
    - `timestamptz` keeps microseconds, so a sub-microsecond `Instant` compares rounded. That is harmless; note it in the release notes (Step 7).
  - `REGEX`: `jsonb_typeof(v.value) = 'string' AND (v.value #>> '{}') ~ $m`. PostgreSQL ARE supports lookahead, so the not-equals idiom `^(?!X$).*$` works natively. There is exactly one pattern per comparison (Step 2).
  - `AllOf`/`AnyOf` become parenthesised `AND`/`OR`.
- **Sort.** Render `ORDER BY <expr> ASC|DESC` per key, with `<expr>` = `jsonb_path_query_first(data, $k::jsonpath)`, typed as above (`#>> '{}' COLLATE "C"` for strings, `::numeric` for numbers when `jsonb_typeof = 'number'`, the guarded `timestamptz` cast for `DATE_TIME`). A key with `valueType == null` (undeclared) orders by the raw `jsonb` value, which PostgreSQL orders across types with a defined rule; if the brokers order such keys differently, record a deviation in Step 6. Always append `created_at, id` as a tiebreaker so paging stays stable. Choose the NULLS placement to match the brokers once Step 7 observes them, defaulting to `NULLS LAST`.
- Put SQL fragments in named constants and reuse no magic strings.
- Tests: `JsonbQueryRendererTest` (parameterized: model → expected SQL plus bindings) for every operator × value type, AND/OR nesting, nested paths, extension attributes, quoting of segments containing `"`/`.`/`\`, `DATE_TIME` (sub-second vs whole-second, `Z` vs offset, date-only and local literals), and sort, including an undeclared sort key. Add a renderer-level `PostgresQueryRenderingIT` in `storage-postgresql/src/test` that executes the fragments against the k3s PostgreSQL over seeded raw documents (scalars, arrays, nested arrays, missing keys, mixed types, a non-date string and a number at a `DATE_TIME` path, an invalid regex). Check the document-level semantics there, independent of the domain mapping.

Acceptance: `mvn verify -pl storage-postgresql` is green.

### Step 5: Filtering, ordering and pagination in `PostgresEntityRepository`
- Remove `LIST_NOT_SUPPORTED` and wire `JsonbQueryRenderer` into `list(...)`. The predicate is `WHERE is_current AND type = ANY($1)` [`AND id = ANY($2)`] `AND <filter>`; then `ORDER BY <sort keys>, created_at, id` (only `created_at, id` without sort keys); then `LIMIT`/`OFFSET`. Renumber the parameters after the rendered bindings.
- `totalCount`'s fallback `COUNT` query must use the same predicate, filter and `is_current` included.
- A pattern PostgreSQL's ARE rejects raises SQLSTATE `2201B` (`invalid_regular_expression`), which `PostgresFailures` maps to nothing today, so the client would get a 500 where the brokers answer 400. Map `2201B` on the list and count path to `QueryException` (400 through `QueryExceptionHandler`; `storage-postgresql` already depends on `common`), as a named constant next to `UNIQUE_VIOLATION`. Test it in `PostgresFailuresTest` and with an unbalanced pattern (`(`) in `PostgresEntityRepositoryIT`.
- Keep `is_current` on every list and count. Add an IT that inserts a non-current row directly (same id, other `version`, `is_current = false`) and asserts it is neither listed nor counted.
- Update the class Javadoc (drop "a query or an order fails until ticket 7").
- Update `OutboxDispatcher`'s "every row fails until PG can look up hubs (ticket 7)" comment and log reasoning: the hub lookup (`TMForumEventHandler.findSubscriptions`) now works on PostgreSQL.
- Extend `PostgresEntityRepositoryIT` (parameterized) with filter, sort, paging-with-filter, count-past-the-end-with-filter and polymorphic-with-filter cases on its `TestCatalog`/`TestResource` types, and adapt `PostgresEntityRepositoryTest`.

Acceptance: `mvn verify -pl storage-postgresql` is green, and `mvn verify -pl product-catalog -Dbroker=postgresql` stays green.

### Step 6: Storage contract tests for query, sort and paging
- In `storage-api/src/test/.../contract/`, extend the fixtures with what a query case needs:
  - `EntityFixture`/`UpdateFixture` (or a new `QueryFixture`): a declared string attribute path, a numeric attribute path, a boolean (if the fixture type has one), a nested path, a reference-id path and an extension name, plus a way to create an entity with given values for them.
  - Build the `EntityQuery` directly from model types, because `storage-api` cannot see `QueryParser`.
- Add cases to `AbstractEntityRepositoryContractTest`, parameterized where possible:
  - equality, multi-value OR, `AllOf`/`AnyOf`;
  - `gt`/`gte`/`lt`/`lte` on numbers and on `DATE_TIME`. Seed stored values with and without a fraction (`…00Z`, `…00.500Z`) and use a literal with an offset (`+01:00`), so a byte-order comparison cannot pass;
  - regex, including the not-equals idiom;
  - nested path, reference id in a reference list, extension attribute;
  - sort ascending/descending, multi-key, paging stability across pages under a sort;
  - `totalCount` with a filter, an empty page past the last match with a filter, and polymorphic filter plus sort.
- Implement the fixture parts in `product-catalog/src/test/.../storage/NgsiLdEntityRepositoryContractIT` and `PostgresEntityRepositoryContractIT`.
- Record each backend difference you observe as a new `KnownDeviation` with a precise Javadoc (backend and version observed), asserting today's behaviour as the existing entries do. Candidate areas: sort placement of missing attributes, string collation, numeric-vs-string comparisons, regex dialect (Scorpio POSIX without lookahead, unless `optimizeRegexNotEquals`), `DATE_TIME` comparisons (instants on PostgreSQL, likely strings on the brokers: across precisions and offsets, and for date-only or local literals), and the order of undeclared sort keys.

Acceptance: `mvn verify -pl product-catalog -Dit.test='*ContractIT'` is green on `-Dbroker=orion-ld`, `scorpio` and `postgresql`.

### Step 7: API-level filter/sort/paging ITs on PostgreSQL, parity gaps, docs
- Remove `@RequiresQueryTranslation` from `product-catalog/.../ListParameterValidationIT`. Then delete `common/.../test/RequiresQueryTranslation.java` and `StorageTags.TICKET_7` once nothing references them.
- Remove the ticket-7 part of `@RequiresHubSupport`: drop `@Tag(StorageTags.TICKET_7)` and reword the Javadoc to "until ticket 1". Hub registration still creates an NGSI-LD subscription until ticket 1 lands.
- The existing API ITs barely filter or sort. Add an API-level `ListQueryIT` in `product-catalog/src/test/...` (extends `AbstractApiIT`, runs on every backend). It seeds a representative volume through the API (a named constant, e.g. a few hundred product offerings with varied `lifecycleStatus`, `version`, `lastUpdate`, `isBundle`, `validFor`, related parties and one extension attribute via `@schemaLocation`, as the existing extension ITs do). Then it asserts filtered, sorted and paged results plus the pagination headers (`X-Total-Count` etc.) against expectations computed in the test:
  - equality, multi-value, `;` OR and `&` AND;
  - `.gt/.gte/.lt/.lte` on numbers and dates, with `lastUpdate` seeded as a mix of whole-second and sub-second instants and literals in `Z`, offset and date-only form;
  - `.regex` and the not-equals idiom;
  - nested paths (`validFor.startDateTime`, `relatedParty.role`), `@type`, extension attributes, and a relationship filter both with and without `.id` (`productSpecification=urn:...`);
  - `sort` with `-` descending, and paging through a sorted, filtered result without duplicates or gaps.
- Add one polymorphic case with a filter (TMF730 sub-types, e.g. software-management `ResourceSpecification`).
- Run the list and hub-adjacent ITs of product-catalog, software-management and resource-catalog on `-Dbroker=postgresql`, `orion-ld` and `scorpio` (`-Dcache=in-memory` and `redis`). Fix parity gaps in the renderer. Record each accepted deviation in `KnownDeviation` (contract level) and in `release-notes/RELEASE_NOTES_POSTGRESQL_ADAPTER.md`.
- Docs:
  - `release-notes/RELEASE_NOTES_POSTGRESQL_ADAPTER.md`: replace "No list filters or sorting" in *Interim limits*, update the hub bullet (only ticket 1 remains), and add a short "Query translation" section on the semantics (instant comparison for date-time attributes with the accepted literal forms, microsecond precision, one regex pattern per attribute, `ngsildOrQueryValue` inside a value as OR on every backend) and the accepted deviations. An invalid regex is a 400 on PostgreSQL as on the brokers.
  - `storage-postgresql/README.md`: Tests section (tags), plus a short note that filters are jsonpath/`EXISTS` without indexes (decision 12).
  - `AGENTS.md`: the model package, `NgsiLdQueryRenderer`, `JsonbQueryRenderer`. Drop `RequiresQueryTranslation` from the `{StorageTags,RequiresQueryTranslation,RequiresHubSupport}` line, since this step deletes the class.
- Comments: resolve or reword `NgsiLdEventHandler`'s "(until ticket 7)" comment. Optionally filter `findSubscriptions` by the hub's `subscription` id so a hub beyond the first page is not missed, but only if this is cheap and both backends support it; otherwise reword the comment without the ticket reference.

Acceptance: the full IT suites of product-catalog and software-management are green on all three backends, every filter/sort/paging IT runs on PostgreSQL, and no `not-on-postgresql` tag remains for query translation.
