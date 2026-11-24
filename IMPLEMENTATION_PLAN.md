# Implementation Plan: TMForum - DB Backend - Selectable backend

## Overview
Make the storage backend one documented setting, the Micronaut environment `postgresql`, `orion-ld` or `scorpio`,
with PostgreSQL as the default when none is given. Startup fails fast on a contradictory or incomplete selection.
The broker-only `general.*` keys move into their own properties class, still bound to the same keys. The README,
`AGENTS.md`, the root pom and the release notes stop describing NGSI-LD as the only storage layer, and the accepted
deviations are listed in one table. The broker adapter stays at full parity, and nothing on the ticket's "not deleted"
list is touched.

### Design decisions (apply to every step)
- **The selector is the Micronaut environment.** It already exists: the `@Requires(env/notEnv =
  CommonConstants.POSTGRESQL_ENVIRONMENT)` gates, the `application-{postgresql,orion-ld,scorpio}.yaml` files, the IT
  wiring `-Dmicronaut.environments=${cache},${broker}`, `test-brokers.yaml` and `MigrationContexts` all use it.
  No new property is added. The documented setting is `MICRONAUT_ENVIRONMENTS=<backend>,<cache>`.
- **The default is applied at the application entry point only.** Micronaut ignores `defaultEnvironments` as soon as
  any environment is set, and deployments always set the cache env. An `ApplicationContextConfigurer` runs for every
  context, including `@MicronautTest` unit tests and the migrator's broker source context, so it would add
  `postgresql` there too. Instead, the 21 `Application.main` methods call a shared runner. It adds `postgresql`
  only when neither `micronaut.environments` (system property) nor `MICRONAUT_ENVIRONMENTS` (env var) names a
  backend. Tests and the migrator keep setting their environments explicitly, so their behaviour does not change.
- **This is a breaking change for broker deployments that do not name their broker.** Today, no backend env means
  the broker. After this ticket it means PostgreSQL. Such a deployment must add `orion-ld` or `scorpio`. The release
  notes say this first, and the startup warning for broker-only keys (Step 2) points at it.
  Naming the broker also loads `application-<broker>.yaml`, which such a deployment never loaded. That changes the
  effective query dialect: `ngsildOrQueryKey` goes from `,` to `|`, and `encloseQuery` and `includeAttributeInList`
  go from `false` to `true`. On `scorpio`, `replaceOnUpdate` also goes from `false` to `true`, which breaks
  attribute creation on Scorpio < 6.0.0 (README `Scorpio < 6.0.0` row, failsafe argLine
  `-Dgeneral.replace-on-update=false`). The docs (Steps 4 and 5) must say so, and say that Scorpio < 6.0.0 sets
  `GENERAL_REPLACE_ON_UPDATE=false`.
- **Config keys do not change.** The broker-only fields move to a new `@ConfigurationProperties("general")` class,
  so `GENERAL_REPLACE_ON_UPDATE`, `general.tenant` and the other keys keep working.

## Steps

### Step 1: Backend selector, PostgreSQL default, fail-fast validation
Files: `common/src/main/java/org/fiware/tmforum/common/configuration/StorageBackend.java` (new),
`common/.../common/TmForumApplication.java` (new), `common/.../configuration/StorageBackendValidator.java` (new),
all 21 `*/src/main/java/**/Application.java` (20 API modules plus `all-in-one`; find them with
`grep -rl "Micronaut.run" --include=Application.java .`), `conformance-test/api.yaml`,
`data-migrator/postgresql-migrator/src/main/java/org/fiware/tmforum/migration/postgresql/BrokerType.java`, plus unit
tests in `common/src/test/java/.../configuration/`.

- `StorageBackend` enum: `POSTGRESQL("postgresql")`, `ORION_LD("orion-ld")`, `SCORPIO("scorpio")`, with
  `environment()` and `isBroker()`. `POSTGRESQL` uses `CommonConstants.POSTGRESQL_ENVIRONMENT`. Add named
  constants for the broker env names, either in `CommonConstants` or in the enum, but no string literals in the
  logic. Add a static helper that finds the backends among a set of environment names.
- `BrokerType` (migrator CLI) holds `"orion-ld"`/`"scorpio"` as literals today. Make its constants take their
  names from `StorageBackend.ORION_LD/SCORPIO.environment()` (or the new constants), so the names exist once.
  The CLI values, `Converter`, `Names` and `MigrationContexts` behave exactly as before; the existing
  `MigrationContextsTest`/`MigrationContextsIT` must pass unchanged.
- `TmForumApplication.run(Class<?> mainClass, String... args)` replaces `Micronaut.run(Application.class, args)`.
  It must start the same context `Micronaut.run` does. In micronaut-context 5.2.17 (platform 5.2.3) that is
  `new Micronaut().classes(cls).args(args).start()`: `classes(...)` registers the main class's package, and there
  is no `mainClass(...)` call, which would also set the class loader. So the runner is
  `Micronaut.build(args).classes(mainClass)` (`build(args)` is `new Micronaut().args(args)`), plus the conditional
  `.environments(...)`/`.properties(...)` below as the only addition, then `.start()`. Re-check this against the
  sources if the Micronaut version has changed. Put the decision in a pure, package-visible function that takes the raw
  `micronaut.environments` and `MICRONAUT_ENVIRONMENTS` values (comma-separated, may be null or blank), so it can be
  unit-tested without touching the JVM. Builder environments are merged with both of those, not replaced by them
  (`DefaultEnvironmentAndPackageDeducer.deduce()` concatenates them; `defaultEnvironments` would only apply when
  all are empty).
- The runner also sets a marker property (named constant, e.g. `tmforum.storage.backend-defaulted=true`, through
  the builder's `.properties(...)`) when it added `postgresql`, so the validator can tell a defaulted backend from
  a named one.
- `StorageBackendValidator` is a `@Context` bean that checks the configuration when the context starts and throws
  `io.micronaut.context.exceptions.ConfigurationException`. The message names the offending setting and what to
  set instead. Checks are limited to misconfigurations a shipped module can actually have. Every module's
  `application.yaml` sets `micronaut.http.services.ngsi.url: http://localhost:1026`, and every
  `application-postgresql.yaml` sets both datasource URLs with `${POSTGRES_HOST:localhost}` defaults, so "URL
  missing or blank" can never happen and is not checked. Reachability is left to Flyway, which aborts startup when
  PostgreSQL is unreachable, and to the existing readiness indicators for the broker.
  1. More than one backend env is active (for example `postgresql,orion-ld` or `orion-ld,scorpio`): fail.
  2. A broker deployment that did not name its broker (the upgrade case): `postgresql` was added by the runner
     (marker set) and `micronaut.http.services.ngsi.url` differs from the shipped default `http://localhost:1026`
     (named constant). Fail, and tell the operator to add `orion-ld` or `scorpio` to `MICRONAUT_ENVIRONMENTS`, or
     to name `postgresql` explicitly if PostgreSQL is intended. `conformance-test/api.yaml` today is exactly
     this case (`MICRONAUT_HTTP_SERVICES_NGSI_URL` set, no backend env). Step 2 extends the same check to the
     broker-only `general.*` keys.
  With no backend env at all (only possible outside the runner, e.g. a hand-built context), the validator does
  nothing, which keeps today's broker behaviour.
  Keep the checks in a pure method that takes the active env names and a `PropertyResolver`. The bean itself must
  not break the existing `@MicronautTest` unit tests in `common`, which run without a backend env and have no
  `src/test/resources/application.yaml`. Gate the bean with `@Requires(notEnv = Environment.TEST)` and unit-test
  the pure method instead. Also test one `ApplicationContext.builder().deduceEnvironment(false)` start that fails.
- `conformance-test/api.yaml` deploys the modules against `orion-ld` without naming a backend env. Add
  `MICRONAUT_ENVIRONMENTS` with `orion-ld` (and its cache env, if one is implied) so the conformance run stays on
  the broker. Check the `-Pdev` and `conformance-test` profiles in the root `pom.xml` for the same gap.
  This also loads `application-orion-ld.yaml` there for the first time, so the conformance run switches to the
  Orion-LD query dialect (`ngsildOrQueryKey: "|"`, `encloseQuery: true`, `includeAttributeInList: true`). That
  is the dialect the ITs already run with. Say this in the step's PR description, so that a later conformance
  failure in a filter test is traced to the dialect change and not to the selector.
- Unchanged, but verify: the root pom's failsafe `argLine`, `test-brokers.yaml`, `parallel-test.yml` (explicit
  `in-memory,orion-ld`), `MigrationContexts` (explicit envs, rejects inherited ones) and `UpdateWriter`'s own
  context. In each, either the runner is not involved or a backend is named explicitly.
- `all-in-one`'s `ControllerStorageBoundaryTest` (ArchUnit) must still pass with the new classes.
- Tests, parameterized: default resolution (sysprop and env var each null, blank, cache-only, naming each backend,
  naming a backend among others with whitespace), and validator cases 1–2 plus the valid combinations. Include the
  real deployment cases: `MICRONAUT_ENVIRONMENTS=redis` with `MICRONAUT_HTTP_SERVICES_NGSI_URL=http://orion-ld:1026`
  (defaulted, fails), the same with `postgresql` named explicitly (passes), and defaulted with the shipped URL
  (passes).
- Acceptance: `mvn test -pl common -am` and `mvn install -DskipTests -DskipITs` are green. Starting a module jar with
  only `MICRONAUT_ENVIRONMENTS=in-memory` activates `postgresql`. `orion-ld,postgresql` aborts startup with the
  validator message, and so does `MICRONAUT_ENVIRONMENTS=in-memory` plus `MICRONAUT_HTTP_SERVICES_NGSI_URL=http://broker:1026`.

### Step 2: Scope the broker-only `general.*` properties
Files: `common/.../configuration/GeneralProperties.java`, a new `common/.../configuration/NgsiLdBrokerProperties.java`,
the consumers listed below, `StorageBackendValidator` from Step 1, their tests, and the base
`*/src/main/resources/application.yaml` of the 20 API modules and `all-in-one` (only the `countHeader` line).

- `NgsiLdBrokerProperties`: `@ConfigurationProperties("general")`, `@Data`, with no `@Requires`. Broker beans
  such as `NgsiLdEntityRepository` are also instantiated on PostgreSQL as non-`@Primary` beans, so the class must
  always exist. Move these fields over, with their defaults and existing field Javadoc: `tenant`,
  `ngsildOrQueryValue`, `ngsildOrQueryKey`, `encloseQuery`, `includeAttributeInList`, `useDotSeperator`,
  `replaceOnUpdate`, `countHeader`. Give the class one line of Javadoc saying the fields are NGSI-LD broker dialect
  and ignored by the PostgreSQL backend.
- `GeneralProperties` keeps `contextUrl`, `basepath`, `serverHost`, `escapeForbiddenCharacters` and
  `optimizeRegexNotEquals`. The last two stay global on purpose: PostgreSQL's POSIX regex has no lookahead either,
  and migrated data may still be escaped. Say so in their Javadoc in one line.
- Switch the consumers to the new class (find them with `grep -rn "getTenant\|getCountHeader\|isReplaceOnUpdate\|
  getNgsildOrQuery\|getEncloseQuery\|getIncludeAttributeInList\|getUseDotSeperator" --include=*.java .`):
  - `common`: `NgsiLdQueryRenderer`, `NgsiLdBaseRepository`, `NgsiLdEntityRepository`,
    `AbstractSubscriptionApiController` (tenant for the NGSI-LD subscription), and the `PaginationFilter` Javadoc
    link.
  - `data-migrator`: `postgresql-migrator`'s `BrokerSource`, `BrokerExportTarget`, `BrokerReferenceBackfill`, and
    `update-writer`'s `UpdateWriter`.
  - Tests: `QueryParserTest`, `QueryParserModelTest`, `NgsiLdQueryRendererTest`, `NgsiLd*RepositoryTest`, the
    `NgsiLdBaseRepository*UpdateTest`s, `BrokerReferenceBackfillTest`, `MigrationContextsTest`, and the ITs
    `ExternalReferenceIT`, `NgsiLdEntityRepositoryContractIT`, `DateTimeFilterBehaviourIT` and the migrator ITs.
- `QueryParser` (storage-neutral) splits string values a second time on `ngsildOrQueryValue` (line ~309). Every
  shipped profile sets it to `,`, the same as `TMFORUM_OR_VALUE`, so the split does nothing in practice, and
  `NgsiLdQueryRenderer` already joins with that key (line ~178, it stays). Remove the split so the parser depends
  only on `TMFORUM_OR_VALUE`, and fix the `toValues` Javadoc, which still says it splits on `ngsildOrQueryValue`.
  Some existing tests assert the split on purpose. Rewrite them; do not put the split back to make them pass:
  - `QueryParserModelTest.testParseSplitsConfiguredOrValueSeparator` (sets `PIPE`): rename it, and assert that
    `color=a|b` parses to the one literal `a|b`, `color=a|b,c` to `a|b` and `c`, and the regex case is unchanged.
    Add the `,`-separated case (`color=a,b` -> `a`, `b`).
  - `NgsiLdQueryRendererTest.testOrValueSeparatorInsideValue` (`color=a|b`): the parser now yields one literal,
    so the expected render is a single comparison on `"a|b"` (escaped as the renderer escapes it), not two
    values. Rewrite its Javadoc as well. Keep a separate case where the parser yields two values (`color=a,b`)
    to show the renderer still joins them with `ngsildOrQueryValue`.
  - `QueryParserTest` sets `ngsildOrQueryValue("|")` at lines ~28, 43, 58 and 232. None of its inputs carries a
    `|` inside a value, so these only move to `NgsiLdBrokerProperties`. If one of them fails, the input relied
    on the split; change the expectation to the single literal.
  The release note goes in Step 5.
- Extend `StorageBackendValidator` for the broker-only `general.*` keys that are explicitly configured (check both
  camelCase and kebab-case through the `PropertyResolver`):
  - `postgresql` defaulted by the runner: add them to Step 1's check 2, so a defaulted backend with any of them set
    fails startup with the same "add `orion-ld`/`scorpio`" message.
  - `postgresql` named explicitly: log a WARN listing them, saying they are ignored. Do not fail.
  For this not to fire on every start, remove `general.countHeader: NGSILD-Results-Count` from every module's base
  `src/main/resources/application.yaml` in this step (the code default is identical and the broker yamls keep
  it). Cover both outcomes with a parameterized unit test, including `GENERAL_REPLACE_ON_UPDATE=true` with no
  backend env named, so the runner defaults to `postgresql` (fails), and with `postgresql` named (warns).
- Acceptance: `mvn test -pl common,data-migrator/postgresql-migrator,data-migrator/update-writer -am` is green.
  `general.*` keys bind as before; there is a test that sets `general.replace-on-update=true` and
  `general.tenant` and reads them from `NgsiLdBrokerProperties`.

### Step 3: Per-module `application*.yaml` documentation
Files: `*/src/main/resources/application.yaml`, `application-postgresql.yaml`, `application-orion-ld.yaml` and
`application-scorpio.yaml` of the 20 API modules and `all-in-one`. Test resources stay as they are, unless a test
breaks.

- `application.yaml`: add a short comment block at the top (3–4 lines, the same in every module) saying the backend
  is chosen with `MICRONAUT_ENVIRONMENTS=<postgresql|orion-ld|scorpio>,<in-memory|redis>` and that PostgreSQL is
  the default. Mark `micronaut.http.services.ngsi` as broker-only with a one-line comment. (`general.countHeader`
  was already removed in Step 2.)
- `application-postgresql.yaml`: replace the first comment line with "Default storage backend: PostgreSQL. The
  NGSI-LD broker and the broker-only `general.*` keys are not used."
- `application-orion-ld.yaml` and `application-scorpio.yaml`: add a one-line header saying this is a broker backend
  and the `general.*` keys here are broker-only (see README "Storage backends").
- Do the edit with a script, keep each file's indentation, and diff two modules by hand afterwards.
  `document-management` has no `application-orion-ld.yaml` or `application-scorpio.yaml`, so the script must skip
  missing files rather than create them.
- Acceptance: `mvn install -DskipTests -DskipITs` is green. `git grep -n "countHeader" -- '*/src/main/resources/application.yaml'`
  is empty. One module IT on `-Dbroker=orion-ld` and one on `-Dbroker=postgresql` pass, to prove the yaml still
  loads.

### Step 4: README "Storage backends", deviation table, `AGENTS.md`, root pom
Files: `README.md`, `AGENTS.md`, `pom.xml` (`project.description`, `project.summary`), and any other docs that
describe the broker as the default or the only storage. Find them with
`grep -rn -i "backed by NGSI-LD\|stays the default\|NGSI-LD adapter.*default\|the broker is the" --include=*.md .`;
expect `release-notes/RELEASE_NOTES_POSTGRESQL_ADAPTER.md`, `storage-postgresql/README.md`, `common/README.md`
and `data-migrator/RUNBOOK_POSTGRESQL.md`.

- README `## Structure`: the intro paragraph and the `k3s` bullet list now say the project stores entities through
  a storage port (`storage-api`) with two adapters, `storage-postgresql` and the NGSI-LD broker adapter in `common`.
  Add `postgresql-db` to the k3s list.
- Replace `## Broker compatibility` with `## Storage backends`:
  - **Selecting a backend**: the env names, the default, an example for each, and the fail-fast rules from
    Step 1. Warn that broker deployments must name their broker, and that naming it turns on the dialect keys of
    `application-<broker>.yaml` (`ngsildOrQueryKey: "|"`, `encloseQuery: true`, `includeAttributeInList: true`,
    plus `replaceOnUpdate: true` on `scorpio`), so Scorpio < 6.0.0 must set `GENERAL_REPLACE_ON_UPDATE=false`.
    Link the `replaceOnUpdate` table in the NGSI-LD broker subsection.
  - **PostgreSQL**: a setup summary (the `POSTGRES_*` variables, schema rights, Flyway on first start) that links
    to `storage-postgresql/README.md#configuration` rather than copying it.
  - **NGSI-LD broker**: one sentence saying everything in this subsection applies to the broker backend only. Then
    a table of the broker-only `general.*` keys (`NgsiLdBrokerProperties`) with a one-line meaning each, and the
    existing `### Attribute updates (replaceOnUpdate)` text and table moved here unchanged.
  - **Accepted deviations** (in `## Storage backends`, after the NGSI-LD broker subsection, not in
    "Dev-Environment"): one table with columns *Area | PostgreSQL | NGSI-LD broker | Decision*. It is the
    single place where deviations are recorded; say so in one sentence. Rows:
    1. Event delivery: at-least-once through `tmforum.event_outbox` vs fire-and-forget (decision 13; facts in
       `RELEASE_NOTES_POSTGRESQL_ADAPTER.md` "Notifications"/"Events").
    2. NGSI-LD subscriptions and `POST /callback`: broker-only, `404` on PostgreSQL (decision 2 as amended).
    3. `totalCount`/`X-Total-Count`: always exact on PostgreSQL vs only when the broker sends `countHeader`.
       Confirm in `PostgresEntityRepository.findEntities` and `PaginationFilter`.
    4. `NULLS` ordering on `sort`: the PostgreSQL cell is the actual ordering in `JsonbQueryRenderer`. The broker
       cell says the brokers do not sort at all and links the `SORT_IS_IGNORED` entry in the pending list below,
       so the row records only the PostgreSQL `NULLS` placement and does not claim a broker `NULLS` behaviour.
    5. Entity versioning: schema prepared, `version` filled and `*Ref` versions checked, no history kept
       (decision 11; `RELEASE_NOTES_ENTITY_VERSIONING.md`), and the same on both backends where that is the case.
    Take every cell from the code or the existing release notes, not from memory. Where a fact cannot be confirmed,
    write it down as an open question in the step's PR description and do not guess.
  - **Known backend differences, not accepted**: directly under the table, a short list with one line per
    `KnownDeviation` entry (`storage-api/src/test/java/org/fiware/tmforum/storage/contract/KnownDeviation.java`,
    11 entries today, e.g. `SORT_IS_IGNORED`, `MULTI_ELEMENT_LIST_NEVER_MATCHES`, `REFERENCE_LIST_UPDATE_MERGES`,
    `NUMBER_AND_BOOLEAN_FILTER_FAILS`, `STRING_COMPARISON_IS_LINGUISTIC`, `ANCHORED_REGEX_NEVER_MATCHES`). Each
    line names the enum constant, the affected backend(s) and versions as its Javadoc states them (e.g. Scorpio
    5.0.3, Orion-LD 1.9.0), and the behaviour in a few words. Link the enum, say the contract tests pin today's
    behaviour for each, and say an entry moves to the accepted table once ticket 0 accepts it. Take the list from
    the enum when the step runs, not from this plan. Also link the "Known limitations" of
    `RELEASE_NOTES_INLINE_REFERENCES.md`, without copying it.
- README "Dev-Environment": `-Pdev` deploys both the broker (`localhost:1026`) and PostgreSQL (`localhost:5432`).
  Say that a module started locally without a backend env now runs on PostgreSQL, and that the validator does
  not warn because the NGSI URL is the shipped default. To use the dev broker, start the module with
  `MICRONAUT_ENVIRONMENTS=orion-ld,in-memory` (`scorpio,in-memory` after `mvn install -Pdev -Dbroker=scorpio`).
- `RELEASE_NOTES_POSTGRESQL_ADAPTER.md`: change "The NGSI-LD adapter stays the default; ticket 9 …" to point at
  the README. Change "No accepted deviations yet …" so it points at both the accepted table and the "Known backend
  differences, not accepted" list, and keep the wording that those differences are pending acceptance in ticket 0. Change "Enabling it" step 1 to say it is the default.
- `AGENTS.md`: rewrite the Overview to describe the port and both adapters, with PostgreSQL as the default. Add
  `TmForumApplication`, `StorageBackendValidator` and `NgsiLdBrokerProperties` to Important Files in one line each.
  Rewrite the "Storage backend selection" line from the plan PR: no backend env now means `postgresql` for
  applications (added by `TmForumApplication`), tests and the migrator name their env explicitly, and
  `conformance-test/api.yaml` names `orion-ld`. Keep the file short.
- Root `pom.xml`: `project.description` becomes "Implementation of the TMForum APIs, backed by PostgreSQL or an
  NGSI-LD context broker", and `project.summary` becomes "TMForum APIs on PostgreSQL or NGSI-LD.".
- Acceptance: `git grep -n -i "backed by NGSI-LD\|Broker compatibility"` matches nothing outside release notes
  history. The README renders (check the table syntax). `mvn -q validate` is green.

### Step 5: Release notes for the epic, then verify the full matrix
Files: `release-notes/RELEASE_NOTES_STORAGE_BACKENDS.md` (new) and the README `## Release notes` table.

- The release notes summarise the whole epic in one page, each item 1–3 lines with a link to the detailed notes:
  - storage port (ticket 86) and external-reference policy (87, `RELEASE_NOTES_EXTERNAL_REFERENCES.md`);
  - inline references (88, `RELEASE_NOTES_INLINE_REFERENCES.md`);
  - PostgreSQL adapter and filter translation (89, 90, `RELEASE_NOTES_POSTGRESQL_ADAPTER.md`);
  - broker-only NGSI-LD notifications (85);
  - data migration (91, `RELEASE_NOTES_DATA_MIGRATION.md`);
  - versioning readiness (93, `RELEASE_NOTES_ENTITY_VERSIONING.md`);
  - this ticket: selector, default, validator, broker-only properties.
- An **Upgrading** section comes first and says:
  - **(a)** broker deployments must add `orion-ld` or `scorpio` to `MICRONAUT_ENVIRONMENTS`, otherwise they start
    on PostgreSQL;
  - **(b)** the `general.*` key names are unchanged, but their effective values can change: a deployment that did
    not name its broker ran on the code defaults, and naming it in (a) loads `application-<broker>.yaml`. List the
    keys that change (`ngsildOrQueryKey` `,` -> `|`, `encloseQuery` and `includeAttributeInList` `false` -> `true`,
    and on `scorpio` `replaceOnUpdate` `false` -> `true`), say that a deployment can override any of them through
    its `GENERAL_*` variable, and say that Scorpio < 6.0.0 must set `GENERAL_REPLACE_ON_UPDATE=false`;
  - **(c)** `QueryParser` no longer splits filter values on a custom `general.ngsildOrQueryValue`;
  - **(d)** to move existing broker data, see `data-migrator/RUNBOOK_POSTGRESQL.md`.
- Link the deviation table and the "Known backend differences, not accepted" list in the README instead of
  copying them. Add a row to the README release-notes table.
- Verification. The GitHub matrix cannot run here, so reproduce it locally and report the results in the PR
  description:
  - `mvn clean install -DskipTests -DskipITs`, then `mvn test` (all modules).
  - `mvn verify -pl product-catalog -Dbroker=<b> -Dcache=<c>` for all six {orion-ld, scorpio, postgresql} x
    {in-memory, redis} combinations.
  - `mvn verify -pl all-in-one -Dbroker=scorpio -Dcache=in-memory` (`PostgresStartupIT`) and
    `mvn verify -pl data-migrator/postgresql-migrator -Dbroker=orion-ld -Dcache=in-memory`.
  - If a run fails because of the environment (k3s or Docker) and not the code, record it as such and do not
    change the code to work around it.
- Acceptance: every acceptance criterion of the ticket can be traced to one of Steps 1–5, and the PR description
  has a checklist that maps them.
