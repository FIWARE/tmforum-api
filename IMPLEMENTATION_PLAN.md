# Implementation Plan: TMForum - Access Policies - 04 - The access-policy module

## Overview
Add a new library module `access-policy/` (AP-03 in `doc/access-policies/TICKETS.md`, rollout phase 1)
that contains only the `access-policy.*` configuration surface from `doc/access-policies/ARCHITECTURE.md`
§ Configuration. It is wired into `common` and `all-in-one`. It must have no effect when the feature is
off, and a value that does not convert (unknown enum, duration without a unit) must fail startup
instead of being silently replaced by the default.

## Decisions (apply to every step)

- **Module placement: before `common`**, right after `storage-api` in the root `<modules>`. `common`
  depends on `access-policy`, never the reverse, so listing it first matches the dependency direction.
  The `storage-postgresql` style (after `common`, depends on `common`) does not fit, because a module
  that `common` depends on cannot depend on `common`. State this reason in the PR body.
- **Package:** `org.fiware.tmforum.accesspolicy` (the path in ARCHITECTURE.md § Components).
- **Lenient binding is the known risk.** The plan assumed Micronaut skips a value it cannot convert.
  Step 1's tests showed that in Micronaut 5.2 this holds only for booleans (next bullet). An unknown
  enum, a non-numeric int and a unit-less duration (string `"60"` or YAML integer `60`) already fail
  startup with a `DependencyInjectionException`, as long as `micronaut-context` (the Duration converters)
  is on the classpath. Without it every duration fails. The guard checks all typed keys anyway, so that
  the error is one clear `ConfigurationException` and a converter change cannot weaken binding. It reads
  the **raw** values from the `Environment`. It does not change the properties class or replace
  Micronaut's global converters, because doing so would change every other module's behaviour.
- **Booleans are lenient in a different way.** Micronaut's `CharSequence`→`Boolean` converter
  (micronaut-core 5.2.17, `DefaultMutableConversionService`) maps `yes`/`y`/`on`/`true` to `TRUE` and
  **every other string** to `FALSE`. It never gives an empty result, so a typo like `fail-open: ture`
  binds to `false`, and a conversion-service check cannot catch it. The guard therefore checks the six
  booleans against the raw string, case-insensitive `true` or `false` only.
- **Endpoint values have no default.** `pdp.url`, `jwt.jwks-uri`, `jwt.issuer` and `jwt.audience` default
  to `null`. The hosts in the ARCHITECTURE.md yaml are examples, and the defaults table does not list them.
  They are `String`s, so binding is lossless. Checking that they are set when `enabled = true` belongs to
  AP-04/AP-05, not to this ticket.
- **No yaml changes.** The acceptance criterion requires that no `access-policy` key exists in any yaml.
  Do not add one to any module's `application*.yaml`.

## Steps

### Step 1: `access-policy` module with `AccessPolicyProperties` and strict-binding guard

**Files**
- `pom.xml` (root): add `<module>access-policy</module>` between `storage-api` and `common`.
- `access-policy/pom.xml` (new): `org.fiware.tmforum:access-policy:0.1`, parent `org.fiware:tmforum:0.1`,
  plus a one-line `<description>` like `storage-api`'s.
  - Dependencies: `org.fiware.tmforum:storage-api:${project.version}` (the only in-build dependency),
    `lombok`, `micronaut-inject`/`micronaut-context` (BOM-managed, no version, scope as `common` uses for
    Micronaut core). Annotation processing already comes from the root pom's `annotationProcessorPaths`.
  - Test dependencies: `junit-jupiter` (including params), plus `micronaut-runtime` or anything else
    `ApplicationContext.run(...)` needs at test scope.
  - A `maven-enforcer-plugin` execution, modelled on `storage-api/pom.xml`'s `neutral-port` (keep its
    handling of the inherited `micronaut-enforce` execution). It bans `org.fiware.tmforum:*` except
    `org.fiware.tmforum:storage-api`, so the build enforces the "nothing else from this build" rule. Use
    a one-line XML comment for the reason, the way `storage-api` does.
  - No jib plugin. The root `oci` profile only puts jib in `pluginManagement`, so a module that does not
    declare jib gets no image.
- `access-policy/src/main/java/org/fiware/tmforum/accesspolicy/AccessPolicyProperties.java` (new):
  `@ConfigurationProperties(AccessPolicyProperties.PREFIX)` (`PREFIX = "access-policy"`), Lombok `@Data`,
  written in the style of `common/.../configuration/GeneralProperties.java`. Each field gets a short
  Javadoc of one to two lines giving its default and what it does (take the wording from ARCHITECTURE.md
  § Defaults and failure modes). Put the defaults in named constants instead of literals in the field
  initialisers, at least `DEFAULT_MAX_SCAN = 10000`, `DEFAULT_OVERFETCH_FACTOR = 4`,
  `DEFAULT_DECISION_TTL = Duration.ofSeconds(60)`, `DEFAULT_PDP_PATH = "/validate"`,
  `DEFAULT_PDP_TIMEOUT = Duration.ofSeconds(2)` and `DEFAULT_IDENTITY_CLAIM = "verifiableCredential.issuer"`.

  | property | type | default |
  |---|---|---|
  | `enabled` | `boolean` | `false` |
  | `mode` | `Mode {ENFORCE, OBSERVE}` | `ENFORCE` |
  | `default-decision` | `Decision {ALLOW, DENY}` | `ALLOW` |
  | `anonymous-decision` | `Decision` | `ALLOW` |
  | `bypass-unauthenticated` | `boolean` | `true` |
  | `bypass-roles` | `List<String>` | empty |
  | `bypass-subjects` | `List<String>` | empty |
  | `owner-exempt` | `boolean` | `true` |
  | `derive-catalog-visibility` | `boolean` | `false` |
  | `total-count` | `TotalCount {EXACT, OMIT, UPSTREAM}` | `EXACT` |
  | `max-scan` | `int` | `10000` |
  | `overfetch-factor` | `int` | `4` |
  | `decision-ttl` | `Duration` | `60s` |
  | `fail-open` | `boolean` | `false` |
  | `pdp.url` | `String` | `null` |
  | `pdp.path` | `String` | `/validate` |
  | `pdp.timeout` | `Duration` | `2s` |
  | `jwt.jwks-uri` / `jwt.issuer` / `jwt.audience` | `String` | `null` |
  | `jwt.identity-claim` | `String` | `verifiableCredential.issuer` |
  | `audit.enabled` | `boolean` | `false` |

  `Mode`, `Decision` and `TotalCount` are public nested enums. `Pdp`, `Jwt` and `Audit` are public static
  nested `@ConfigurationProperties("pdp"|"jwt"|"audit")` classes with `@Data`. Each is held in a field
  initialised to `new Pdp()` etc., so it is never `null` when unconfigured (Micronaut's nested
  configuration pattern). The class stays a plain, lazily created configuration bean. Do **not** gate it
  with `@Requires(property = "access-policy.enabled")`, because the acceptance criterion reads its
  defaults when no key exists.
- `access-policy/src/main/java/org/fiware/tmforum/accesspolicy/AccessPolicyConfigurationGuard.java` (new):
  - `@Context @Requires(property = AccessPolicyProperties.PREFIX)`, so it exists only when **some**
    `access-policy.*` key is configured. With no key, no bean from this module is ever created.
  - Its constructor takes the `Environment`. For each typed key it reads the raw value as a `String`. If
    the value is present and the target is not `Boolean`, it converts the value to the target type with the environment's conversion
    service. If conversion gives an empty result or throws, it throws
    `io.micronaut.context.exceptions.ConfigurationException` naming the key and the rejected value, for
    example `access-policy.decision-ttl: '60' is not a duration, a unit is mandatory (e.g. 60s)`.
  - A `Boolean` key does not go through the conversion service (see Decisions). The raw string must
    equal `true` or `false`, ignoring case, or the guard throws the same `ConfigurationException`. A YAML
    boolean (`enabled: true`) arrives as `"true"`, so it passes. `yes`/`on` are rejected on purpose,
    because only `true`/`false` read the same in YAML, env vars and system properties.
  - The keys and their target types go in one named constant (`Map<String, Class<?>>` or a small record
    list): `mode`→`Mode`, `default-decision`/`anonymous-decision`→`Decision`, `total-count`→`TotalCount`,
    `max-scan`/`overfetch-factor`→`Integer`, `decision-ttl`/`pdp.timeout`→`Duration`, and the six booleans
    `enabled`, `bypass-unauthenticated`, `owner-exempt`, `derive-catalog-visibility`, `fail-open`,
    `audit.enabled`→`Boolean`.
    Build the keys from `PREFIX`, not from repeated literals.
  - The class gets a one-line Javadoc saying why it exists: Micronaut drops a value it cannot convert, so
    the default would win silently.
- `access-policy/src/test/java/org/fiware/tmforum/accesspolicy/AccessPolicyPropertiesTest.java` (new),
  modelled on `common/src/test/.../configuration/SchemaCachePropertiesTest.java`, which uses
  `ApplicationContext.run(Map)` with no `@MicronautTest`. **Write the rejection cases first and confirm
  that they fail without the guard.**
  - `@ParameterizedTest @MethodSource`: with an empty context, each property has its default. Use one row
    per property, a getter as `Function<AccessPolicyProperties, Object>` and the expected default, covering
    every row of the table above, nested classes included.
  - Parameterized test: with an empty context, `AccessPolicyConfigurationGuard` is not a bean
    (`context.containsBean(...)` is false). This shows the module is inert when unconfigured.
  - Parameterized test: valid values bind. Cover `observe`/`deny`/`omit`/`upstream` in kebab/lower case,
    `decision-ttl` as `30s`, `5m` and `PT1M`, `pdp.timeout` as `500ms`, and `fail-open` as `TRUE` and
    `false`.
  - Parameterized characterization test of the converter behaviour the guard depends on: for each of
    `ture`, `1`, `enabled`, `ConversionService.SHARED.convert(value, Boolean.class)` is `Optional.of(FALSE)`
    (not empty). If a Micronaut upgrade changes this, the test shows that the raw boolean check may no
    longer be needed.
  - Parameterized test, the rejection cases: an unknown value for `mode`, `default-decision`,
    `anonymous-decision` and `total-count`, a unit-less `decision-ttl` (`60`) and `pdp.timeout` (`2`), and
    a non-numeric `max-scan`, and a misspelled boolean for `fail-open` (`ture`) and `audit.enabled`
    (`yes`). Each must make `ApplicationContext.run(Map)` throw, and the exception's cause
    chain must contain the offending key. Also include one case with `access-policy.enabled=false` plus a
    bad value: it still fails, because a bad value is a configuration error whether or not the feature is
    on.

**Acceptance**
- `mvn clean install -pl access-policy -am` passes, and `AccessPolicyPropertiesTest` is green.
- The enforcer fails if any `org.fiware.tmforum` artifact other than `storage-api` is added.
- No other module changes in this step.

### Step 2: Wire `access-policy` into `common` and `all-in-one`, full build

**Files**
- `common/pom.xml`: add `org.fiware.tmforum:access-policy:${project.version}` next to the existing
  `storage-api` dependency (compile scope, so every API module gets it transitively).
- `all-in-one/pom.xml`: add the same dependency explicitly next to `common`/`storage-postgresql`.
- No change to the `oci` profile or any jib configuration. Confirm that
  `mvn install -Poci -DskipTests -DskipITs -pl access-policy` builds no image (jib is not bound in the
  module), and say so in the PR.
- `AGENTS.md`:
  - Add `access-policy/` to Project Structure: "optional per-entity access-policy layer
    (`org.fiware.tmforum.accesspolicy`), depends only on `storage-api`, `common` depends on it; ticket
    182 holds only `AccessPolicyProperties` + `AccessPolicyConfigurationGuard`".
  - Add one Key Conventions line: `access-policy.*` values are strictly validated by the guard, so a new
    typed property must be added to its key table.

**Checks**
- `mvn clean install -DskipTests -DskipITs` passes for the whole reactor.
- `mvn test -pl common` passes. In particular, the ArchUnit `StorageBoundaryTest` and the existing wiring
  tests (`StoragePortWiringTest`, `StorageAdapterQualifierTest`) must stay green, which shows the new
  bean on the classpath does not disturb injection.
- `mvn test -pl all-in-one` passes (`ControllerStorageBoundaryTest`).
- No `application*.yaml` in the repo contains an `access-policy` key (`git grep -n "access-policy:" -- '*.yaml'`
  is empty). Together with the guard's `@Requires`, this means no module's runtime behaviour changes.

**Acceptance**
- All ticket acceptance criteria hold: the full build passes, the defaults apply when no key is set,
  unknown enum and unit-less duration values fail startup, and runtime behaviour is unchanged.
- The PR body states the module placement and why (see Decisions).
