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
- `ValueType` {`STRING`, `NUMBER`, `BOOLEAN`}. It is the neutral twin of the mapping library's `QueryAttributeType`, which `storage-api` must not see.
- `ComparisonOperator` {`EQUALS`, `GREATER_THAN`, `GREATER_THAN_EQUALS`, `LESS_THAN`, `LESS_THAN_EQUALS`, `REGEX`}. There is no not-equals: the regex not-equals rewrite is an NGSI-LD renderer optimisation (Step 2).
- `AttributePath(List<String> segments, boolean declared)`. The segments are the TMForum path after JSON-LD reserved-token translation (`@type` → `atType`, ...). `declared == false` marks a free-form extension attribute.
- `QueryFilter`, a sealed interface with `Comparison(AttributePath attribute, ComparisonOperator operator, ValueType valueType, List<String> values)` (values OR-ed, already validated and trimmed for their type), `AllOf(List<QueryFilter>)` and `AnyOf(List<QueryFilter>)`.
- `SortKey(AttributePath attribute, ValueType valueType, boolean descending)`.
- `EntityQuery(Class<?> queryClass, QueryFilter filter, List<SortKey> sort)`. `filter` may be null, and `sort` is empty when there is no sort. `queryClass` is the class paths were resolved against; the NGSI-LD renderer needs it for `findEntitiesPolymorphic`, which has no entity class.

The compact constructors reject null or empty lists where they make no sense, and copy lists immutably. Records give `equals`/`hashCode`, which matters if a call ever becomes a cache key.

Do not change the port yet. Add a unit test in `storage-api/src/test/java/.../query/` (parameterized) for the constructor invariants.
Acceptance: `mvn -pl storage-api verify` passes, enforcer included.

### Step 2: Split `QueryParser` into parse and NGSI-LD render (no behaviour change)
In `common/src/main/java/org/fiware/tmforum/common/querying/`:
- **Front half, `QueryParser`.** Add `ParsedQuery parse(Class<?> queryClass, String queryString)` returning `ParsedQuery(String ids, String types, QueryFilter filter)`, and `List<SortKey> parseSort(Class<?> queryClass, Map<String, List<String>> parameters)`. The parse moves here unchanged:
  - well-known-parameter removal and the AND(`&`)/OR(`;`) split, including the "combining is not supported" `QueryException`;
  - `parseParameter`/operator detection, OR-grouping per attribute and operator (`combineParts`), and `,`-value splitting (never for `.regex`);
  - JSON-LD reserved-token translation, the `id`/`type` shortcuts into `ids`/`types`, and attribute type resolution through `JavaObjectMapper.getNGSIAttributePath`. An empty path means `declared = false`, with the type inferred from the literal as in today's `getPathToAdditionalAttributes`;
  - all validation (`validateOperator`, the boolean/number literal checks), so both backends answer the same 400s.
- **Back half, new `NgsiLdQueryRenderer` (`@Singleton`, takes `GeneralProperties`).** It provides `String renderFilter(EntityQuery)` and `String renderOrderBy(EntityQuery)`. Everything NGSI-LD moves here: the NGSI path through `getNGSIAttributePath`, `isRelationship`, `escapePath`/reserved-word escaping, the `.id` stripping for relationships, `mapPathPart` (`useDotSeperator`), `encodeValue`/`encodeStringValue`, `includeAttributeInList`/`encloseQuery`/`ngsildOrQueryKey`/`ngsildOrQueryValue`, the operator symbols (`Operator.getNgsiLdOperator`), the `;desc` suffix and `applyRegexNotEqualsOptimization`.
- Keep `toNgsiLdQuery(...)` and `toOrderBy(...)` for now as thin façades (`parse` + render, still returning `QueryParams`), so no caller changes in this step.
- **Pitfall: output order.** Today's OR grouping uses `Collectors.toMap` (HashMap iteration order), and the `QueryParserTest` expectations depend on it (e.g. `color==\"Red\"|(status...)`). Keep the same grouping so every existing `QueryParserTest` case stays byte-identical. Do not "fix" the order here.
- Tests:
  - `QueryParserTest` stays unchanged and green.
  - Add `QueryParserModelTest`, parameterized TMForum string → expected `EntityQuery`/`ParsedQuery`, covering reserved tokens, extension attributes, regex with `,`, and the 400 cases.
  - Add `NgsiLdQueryRenderer` tests over hand-built models for every `GeneralProperties` combination already covered by `QueryParserTest`.

Acceptance: `mvn -pl common -am test` is green, and the NGSI-LD output of every existing test case is unchanged.

### Step 3: Migrate the storage port and its callers to the model
- `storage-api/.../EntityReadRepository`: replace `String query`/`String orderBy` with `EntityQuery query` (null means unfiltered and unordered):
  - `findEntities(Integer offset, Integer limit, Class<T> entityClass, EntityQuery query, String ids, String types)`;
  - the convenience overload `findEntities(offset, limit, entityType, entityClass, EntityQuery query)`;
  - `findEntitiesPolymorphic(offset, limit, types, EntityQuery query, ids, typeToClass)`.

  Drop the "transitional NGSI-LD coupling" paragraph from the Javadoc.
- `common/.../repository/NgsiLdEntityRepository`: inject `NgsiLdQueryRenderer` and render the filter and the orderBy right before `entitiesApi.queryEntities`. A null query passes null as today.
- `common/.../rest/AbstractApiController.list`/`listPolymorphic`: use `queryParser.parse(...)`/`parseSort(...)` and build the `EntityQuery` with the entity class or the query class. `ids`/`types` still come from `ParsedQuery`, and `hasFilter`'s mutation ordering is kept.
- `common/.../notification/TMForumEventHandler.findSubscriptions`: build the `entities`/`eventTypes` filter through `parse(TMForumSubscription.class, ...)`.
- `common/.../rest/AbstractSubscriptionApiController.assureNotExistingTMForumSubscription`: remove the `String.format`-into-rendered-query hack and the NGSI-only `ForbiddenCharacterEscaper` coupling.
  - Recommended: filter on `callback` only through the model, page through the matches, and compare `getRawQuery()` in Java. Hubs sharing one callback are few, so this is neutral across backends: NGSI-LD stores `rawQuery` escaped, while the PostgreSQL document stores the raw value.
  - Keep the 409 behaviour.
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
  - `REGEX`: `jsonb_typeof(v.value) = 'string' AND (v.value #>> '{}') ~ $m`. PostgreSQL ARE supports lookahead, so the not-equals idiom `^(?!X$).*$` works natively. Multiple OR-ed regex values become an OR of patterns.
  - `AllOf`/`AnyOf` become parenthesised `AND`/`OR`.
- **Sort.** Render `ORDER BY <expr> ASC|DESC` per key, with `<expr>` = `jsonb_path_query_first(data, $k::jsonpath)`, typed as above (`#>> '{}' COLLATE "C"` for strings, `::numeric` for numbers when `jsonb_typeof = 'number'`). Always append `created_at, id` as a tiebreaker so paging stays stable. Choose the NULLS placement to match the brokers once Step 7 observes them, defaulting to `NULLS LAST`.
- Put SQL fragments in named constants and reuse no magic strings.
- Tests: `JsonbQueryRendererTest` (parameterized: model → expected SQL plus bindings) for every operator × value type, AND/OR nesting, nested paths, extension attributes, quoting of segments containing `"`/`.`/`\`, and sort. Add a renderer-level `PostgresQueryRenderingIT` in `storage-postgresql/src/test` that executes the fragments against the k3s PostgreSQL over seeded raw documents (scalars, arrays, nested arrays, missing keys, mixed types). Check the document-level semantics there, independent of the domain mapping.

Acceptance: `mvn verify -pl storage-postgresql` is green.

### Step 5: Filtering, ordering and pagination in `PostgresEntityRepository`
- Remove `LIST_NOT_SUPPORTED` and wire `JsonbQueryRenderer` into `list(...)`. The predicate is `WHERE is_current AND type = ANY($1)` [`AND id = ANY($2)`] `AND <filter>`; then `ORDER BY <sort keys>, created_at, id` (only `created_at, id` without sort keys); then `LIMIT`/`OFFSET`. Renumber the parameters after the rendered bindings.
- `totalCount`'s fallback `COUNT` query must use the same predicate, filter and `is_current` included.
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
  - `gt`/`gte`/`lt`/`lte` on numbers and on ISO date-time strings;
  - regex, including the not-equals idiom;
  - nested path, reference id in a reference list, extension attribute;
  - sort ascending/descending, multi-key, paging stability across pages under a sort;
  - `totalCount` with a filter, an empty page past the last match with a filter, and polymorphic filter plus sort.
- Implement the fixture parts in `product-catalog/src/test/.../storage/NgsiLdEntityRepositoryContractIT` and `PostgresEntityRepositoryContractIT`.
- Record each backend difference you observe as a new `KnownDeviation` with a precise Javadoc (backend and version observed), asserting today's behaviour as the existing entries do. Candidate areas: sort placement of missing attributes, string collation, numeric-vs-string comparisons, regex dialect (Scorpio POSIX without lookahead, unless `optimizeRegexNotEquals`).

Acceptance: `mvn verify -pl product-catalog -Dit.test='*ContractIT'` is green on `-Dbroker=orion-ld`, `scorpio` and `postgresql`.

### Step 7: API-level filter/sort/paging ITs on PostgreSQL, parity gaps, docs
- Remove `@RequiresQueryTranslation` from `product-catalog/.../ListParameterValidationIT`. Then delete `common/.../test/RequiresQueryTranslation.java` and `StorageTags.TICKET_7` once nothing references them.
- Remove the ticket-7 part of `@RequiresHubSupport`: drop `@Tag(StorageTags.TICKET_7)` and reword the Javadoc to "until ticket 1". Hub registration still creates an NGSI-LD subscription until ticket 1 lands.
- The existing API ITs barely filter or sort. Add an API-level `ListQueryIT` in `product-catalog/src/test/...` (extends `AbstractApiIT`, runs on every backend). It seeds a representative volume through the API (a named constant, e.g. a few hundred product offerings with varied `lifecycleStatus`, `version`, `lastUpdate`, `isBundle`, `validFor`, related parties and one extension attribute via `@schemaLocation`, as the existing extension ITs do). Then it asserts filtered, sorted and paged results plus the pagination headers (`X-Total-Count` etc.) against expectations computed in the test:
  - equality, multi-value, `;` OR and `&` AND;
  - `.gt/.gte/.lt/.lte` on numbers and dates;
  - `.regex` and the not-equals idiom;
  - nested paths (`validFor.startDateTime`, `relatedParty.role`), `@type`, extension attributes;
  - `sort` with `-` descending, and paging through a sorted, filtered result without duplicates or gaps.
- Add one polymorphic case with a filter (TMF730 sub-types, e.g. software-management `ResourceSpecification`).
- Run the list and hub-adjacent ITs of product-catalog, software-management and resource-catalog on `-Dbroker=postgresql`, `orion-ld` and `scorpio` (`-Dcache=in-memory` and `redis`). Fix parity gaps in the renderer. Record each accepted deviation in `KnownDeviation` (contract level) and in `release-notes/RELEASE_NOTES_POSTGRESQL_ADAPTER.md`.
- Docs:
  - `release-notes/RELEASE_NOTES_POSTGRESQL_ADAPTER.md`: replace "No list filters or sorting" in *Interim limits*, update the hub bullet (only ticket 1 remains), and add a short "Query translation" section on the semantics and accepted deviations.
  - `storage-postgresql/README.md`: Tests section (tags), plus a short note that filters are jsonpath/`EXISTS` without indexes (decision 12).
  - `AGENTS.md`: the model package, `NgsiLdQueryRenderer`, `JsonbQueryRenderer`.
- Comments: resolve or reword `NgsiLdEventHandler`'s "(until ticket 7)" comment. Optionally filter `findSubscriptions` by the hub's `subscription` id so a hub beyond the first page is not missed, but only if this is cheap and both backends support it; otherwise reword the comment without the ticket reference.

Acceptance: the full IT suites of product-catalog and software-management are green on all three backends, every filter/sort/paging IT runs on PostgreSQL, and no `not-on-postgresql` tag remains for query translation.
