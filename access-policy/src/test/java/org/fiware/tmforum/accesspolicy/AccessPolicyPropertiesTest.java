package org.fiware.tmforum.accesspolicy;

import io.micronaut.context.ApplicationContext;
import io.micronaut.context.exceptions.ConfigurationException;
import io.micronaut.core.convert.ConversionService;
import org.fiware.tmforum.accesspolicy.AccessPolicyProperties.Decision;
import org.fiware.tmforum.accesspolicy.AccessPolicyProperties.Mode;
import org.fiware.tmforum.accesspolicy.AccessPolicyProperties.TotalCount;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests the binding of the access-policy configuration, since a silently dropped value would weaken the access
 * control without notice.
 */
class AccessPolicyPropertiesTest {

	static final String VALID_PDP_URL = "http://odrl-pap.example.org:8080";

	private static final String PDP_SERVICE_URL_KEY = "micronaut.http.services.pdp.url";
	private static final String PDP_SERVICE_URLS_KEY = "micronaut.http.services.pdp.urls";

	private static String key(String suffix) {
		return AccessPolicyProperties.PREFIX + "." + suffix;
	}

	private static Stream<Arguments> defaults() {
		return Stream.of(
				Arguments.of("enabled", (Function<AccessPolicyProperties, Object>) AccessPolicyProperties::isEnabled,
						false),
				Arguments.of("mode", (Function<AccessPolicyProperties, Object>) AccessPolicyProperties::getMode,
						Mode.ENFORCE),
				Arguments.of("default-decision",
						(Function<AccessPolicyProperties, Object>) AccessPolicyProperties::getDefaultDecision,
						Decision.ALLOW),
				Arguments.of("anonymous-decision",
						(Function<AccessPolicyProperties, Object>) AccessPolicyProperties::getAnonymousDecision,
						Decision.ALLOW),
				Arguments.of("bypass-unauthenticated",
						(Function<AccessPolicyProperties, Object>) AccessPolicyProperties::isBypassUnauthenticated,
						true),
				Arguments.of("bypass-roles",
						(Function<AccessPolicyProperties, Object>) AccessPolicyProperties::getBypassRoles, List.of()),
				Arguments.of("bypass-subjects",
						(Function<AccessPolicyProperties, Object>) AccessPolicyProperties::getBypassSubjects,
						List.of()),
				Arguments.of("owner-exempt",
						(Function<AccessPolicyProperties, Object>) AccessPolicyProperties::isOwnerExempt, true),
				Arguments.of("derive-catalog-visibility",
						(Function<AccessPolicyProperties, Object>) AccessPolicyProperties::isDeriveCatalogVisibility,
						false),
				Arguments.of("total-count",
						(Function<AccessPolicyProperties, Object>) AccessPolicyProperties::getTotalCount,
						TotalCount.EXACT),
				Arguments.of("max-scan", (Function<AccessPolicyProperties, Object>) AccessPolicyProperties::getMaxScan,
						10000),
				Arguments.of("overfetch-factor",
						(Function<AccessPolicyProperties, Object>) AccessPolicyProperties::getOverfetchFactor, 4),
				Arguments.of("decision-ttl",
						(Function<AccessPolicyProperties, Object>) AccessPolicyProperties::getDecisionTtl,
						Duration.ofSeconds(60)),
				Arguments.of("fail-open",
						(Function<AccessPolicyProperties, Object>) AccessPolicyProperties::isFailOpen, false),
				Arguments.of("pdp.url",
						(Function<AccessPolicyProperties, Object>) p -> p.getPdp().getUrl(), null),
				Arguments.of("pdp.path",
						(Function<AccessPolicyProperties, Object>) p -> p.getPdp().getPath(), "/validate"),
				Arguments.of("pdp.timeout",
						(Function<AccessPolicyProperties, Object>) p -> p.getPdp().getTimeout(),
						Duration.ofSeconds(2)),
				Arguments.of("jwt.jwks-uri",
						(Function<AccessPolicyProperties, Object>) p -> p.getJwt().getJwksUri(), null),
				Arguments.of("jwt.issuer",
						(Function<AccessPolicyProperties, Object>) p -> p.getJwt().getIssuer(), null),
				Arguments.of("jwt.audience",
						(Function<AccessPolicyProperties, Object>) p -> p.getJwt().getAudience(), null),
				Arguments.of("jwt.identity-claim",
						(Function<AccessPolicyProperties, Object>) p -> p.getJwt().getIdentityClaim(),
						"verifiableCredential.issuer"),
				Arguments.of("audit.enabled",
						(Function<AccessPolicyProperties, Object>) p -> p.getAudit().isEnabled(), false));
	}

	@DisplayName("The defaults are applied if nothing is configured.")
	@ParameterizedTest(name = "{0}")
	@MethodSource("defaults")
	void defaultsAreApplied(String property, Function<AccessPolicyProperties, Object> getter, Object expected) {
		try (ApplicationContext context = ApplicationContext.run()) {
			assertEquals(expected, getter.apply(context.getBean(AccessPolicyProperties.class)),
					String.format("%s should default to %s.", property, expected));
		}
	}

	@DisplayName("Without any access-policy key, neither the guard nor the PDP client exists.")
	@ParameterizedTest(name = "{0}")
	@ValueSource(classes = {AccessPolicyConfigurationGuard.class, PolicyDecisionPoint.class})
	void featureBeansAreAbsentWhenUnconfigured(Class<?> bean) {
		try (ApplicationContext context = ApplicationContext.run()) {
			assertFalse(context.containsBean(bean), "Without configuration, the module must not create any bean.");
		}
	}

	private static Stream<Arguments> validValues() {
		return Stream.of(
				Arguments.of("mode", "observe",
						(Function<AccessPolicyProperties, Object>) AccessPolicyProperties::getMode, Mode.OBSERVE),
				Arguments.of("default-decision", "deny",
						(Function<AccessPolicyProperties, Object>) AccessPolicyProperties::getDefaultDecision,
						Decision.DENY),
				Arguments.of("anonymous-decision", "DENY",
						(Function<AccessPolicyProperties, Object>) AccessPolicyProperties::getAnonymousDecision,
						Decision.DENY),
				Arguments.of("total-count", "omit",
						(Function<AccessPolicyProperties, Object>) AccessPolicyProperties::getTotalCount,
						TotalCount.OMIT),
				Arguments.of("total-count", "upstream",
						(Function<AccessPolicyProperties, Object>) AccessPolicyProperties::getTotalCount,
						TotalCount.UPSTREAM),
				Arguments.of("max-scan", "500",
						(Function<AccessPolicyProperties, Object>) AccessPolicyProperties::getMaxScan, 500),
				Arguments.of("decision-ttl", "30s",
						(Function<AccessPolicyProperties, Object>) AccessPolicyProperties::getDecisionTtl,
						Duration.ofSeconds(30)),
				Arguments.of("decision-ttl", "5m",
						(Function<AccessPolicyProperties, Object>) AccessPolicyProperties::getDecisionTtl,
						Duration.ofMinutes(5)),
				Arguments.of("decision-ttl", "PT1M",
						(Function<AccessPolicyProperties, Object>) AccessPolicyProperties::getDecisionTtl,
						Duration.ofMinutes(1)),
				Arguments.of("pdp.timeout", "500ms",
						(Function<AccessPolicyProperties, Object>) p -> p.getPdp().getTimeout(),
						Duration.ofMillis(500)),
				Arguments.of("fail-open", "TRUE",
						(Function<AccessPolicyProperties, Object>) AccessPolicyProperties::isFailOpen, true),
				Arguments.of("fail-open", "false",
						(Function<AccessPolicyProperties, Object>) AccessPolicyProperties::isFailOpen, false),
				Arguments.of("audit.enabled", "true",
						(Function<AccessPolicyProperties, Object>) p -> p.getAudit().isEnabled(), true),
				Arguments.of("pdp.url", "http://odrl-pap:8080",
						(Function<AccessPolicyProperties, Object>) p -> p.getPdp().getUrl(), "http://odrl-pap:8080"));
	}

	@DisplayName("Valid values are bound.")
	@ParameterizedTest(name = "{0}: {1}")
	@MethodSource("validValues")
	void validValuesAreBound(String property, String value, Function<AccessPolicyProperties, Object> getter,
			Object expected) {
		try (ApplicationContext context = ApplicationContext.run(Map.of(key(property), value))) {
			assertEquals(expected, getter.apply(context.getBean(AccessPolicyProperties.class)),
					String.format("%s: '%s' should bind to %s.", property, value, expected));
		}
	}

	@DisplayName("Micronaut converts any unknown string to false instead of failing, which is why the guard checks booleans on the raw value.")
	@ParameterizedTest
	@ValueSource(strings = {"ture", "1", "enabled"})
	void conversionServiceTurnsUnknownBooleansIntoFalse(String value) {
		assertEquals(Optional.of(Boolean.FALSE), ConversionService.SHARED.convert(value, Boolean.class),
				"If this changes, the raw boolean check of the guard may no longer be needed.");
	}

	private static Stream<Arguments> invalidValues() {
		return Stream.of(
				Arguments.of(Map.of(key("mode"), "audit"), "mode"),
				Arguments.of(Map.of(key("default-decision"), "permit"), "default-decision"),
				Arguments.of(Map.of(key("anonymous-decision"), "maybe"), "anonymous-decision"),
				Arguments.of(Map.of(key("total-count"), "approximate"), "total-count"),
				Arguments.of(Map.of(key("decision-ttl"), "60"), "decision-ttl"),
				Arguments.of(Map.of(key("pdp.timeout"), "2"), "pdp.timeout"),
				Arguments.of(Map.of(key("decision-ttl"), 60), "decision-ttl"),
				Arguments.of(Map.of(key("pdp.timeout"), 2), "pdp.timeout"),
				Arguments.of(Map.of(key("fail-open"), 1), "fail-open"),
				Arguments.of(Map.of(key("max-scan"), "many"), "max-scan"),
				Arguments.of(Map.of(key("fail-open"), "ture"), "fail-open"),
				Arguments.of(Map.of(key("audit.enabled"), "yes"), "audit.enabled"),
				Arguments.of(Map.of(key("enabled"), "false", key("decision-ttl"), "60"), "decision-ttl"));
	}

	@DisplayName("A value that does not convert fails the startup instead of falling back to the default.")
	@ParameterizedTest(name = "{1}: {0}")
	@MethodSource("invalidValues")
	void invalidValuesFailStartup(Map<String, Object> configuration, String property) {
		Throwable thrown = assertThrows(Throwable.class, () -> {
			try (ApplicationContext context = ApplicationContext.run(configuration)) {
				context.getBean(AccessPolicyProperties.class);
			}
		}, String.format("An invalid %s must fail the startup.", property));
		assertTrue(guardRejected(thrown, key(property)),
				String.format("The guard should reject %s, but the failure was: %s", key(property), thrown));
	}

	private static final String VALID_JWKS_URI = "https://keycloak.example.org/realms/test/protocol/openid-connect/certs";
	private static final String VALID_ISSUER = "https://keycloak.example.org/realms/test";

	private static Stream<Arguments> incompleteJwtSettings() {
		return Stream.of(
				Arguments.of(Map.of(key("enabled"), "true", key("jwt.issuer"), VALID_ISSUER), "jwt.jwks-uri"),
				Arguments.of(Map.of(key("enabled"), "true", key("jwt.jwks-uri"), "/certs",
						key("jwt.issuer"), VALID_ISSUER), "jwt.jwks-uri"),
				Arguments.of(Map.of(key("enabled"), "true", key("jwt.jwks-uri"), "keycloak:8080/certs",
						key("jwt.issuer"), VALID_ISSUER), "jwt.jwks-uri"),
				Arguments.of(Map.of(key("enabled"), "true", key("jwt.jwks-uri"), "file:///etc/jwks.json",
						key("jwt.issuer"), VALID_ISSUER), "jwt.jwks-uri"),
				Arguments.of(Map.of(key("enabled"), "true", key("jwt.jwks-uri"), "https://in valid",
						key("jwt.issuer"), VALID_ISSUER), "jwt.jwks-uri"),
				Arguments.of(Map.of(key("enabled"), "true", key("jwt.jwks-uri"), VALID_JWKS_URI), "jwt.issuer"),
				Arguments.of(Map.of(key("enabled"), "true", key("jwt.jwks-uri"), VALID_JWKS_URI,
						key("jwt.issuer"), " "), "jwt.issuer"),
				Arguments.of(Map.of(key("enabled"), "true", key("jwt.jwks-uri"), VALID_JWKS_URI,
						key("jwt.issuer"), VALID_ISSUER, key("jwt.audience"), " "), "jwt.audience"));
	}

	@DisplayName("An enabled access control without a JWKS URI or an issuer, or with a blank audience, fails the startup.")
	@ParameterizedTest(name = "{1}: {0}")
	@MethodSource("incompleteJwtSettings")
	void incompleteJwtSettingsFailStartup(Map<String, Object> configuration, String property) {
		invalidValuesFailStartup(configuration, property);
	}

	private static Stream<Arguments> acceptedJwtSettings() {
		return Stream.of(
				Arguments.of(Map.of(key("enabled"), "false")),
				Arguments.of(Map.of(key("mode"), "observe")),
				Arguments.of(Map.of(key("enabled"), "true", key("jwt.jwks-uri"), VALID_JWKS_URI,
						key("jwt.issuer"), VALID_ISSUER, key("pdp.url"), VALID_PDP_URL)),
				Arguments.of(Map.of(key("enabled"), "true", key("jwt.jwks-uri"), "http://keycloak:8080/certs",
						key("jwt.issuer"), VALID_ISSUER, key("pdp.url"), VALID_PDP_URL)),
				Arguments.of(Map.of(key("enabled"), "true", key("jwt.jwks-uri"), VALID_JWKS_URI,
						key("jwt.issuer"), VALID_ISSUER, key("jwt.audience"), "tmforum-api", key("pdp.url"), VALID_PDP_URL)));
	}

	@DisplayName("A disabled access control needs no token settings, an enabled one starts with both set.")
	@ParameterizedTest(name = "{0}")
	@MethodSource("acceptedJwtSettings")
	void acceptedJwtSettingsStart(Map<String, Object> configuration) {
		try (ApplicationContext context = ApplicationContext.run(configuration)) {
			assertTrue(context.containsBean(AccessPolicyConfigurationGuard.class),
					"The guard should exist and accept the configuration.");
		}
	}

	private static Map<String, Object> enabledWith(Map<String, Object> pdpEndpoint) {
		Map<String, Object> configuration = new HashMap<>(pdpEndpoint);
		configuration.putAll(Map.of(key("enabled"), "true", key("jwt.jwks-uri"), VALID_JWKS_URI,
				key("jwt.issuer"), VALID_ISSUER));
		return configuration;
	}

	private static Stream<Arguments> invalidPdpEndpoints() {
		return Stream.of(
				Arguments.of("neither", enabledWith(Map.of())),
				Arguments.of("blank url", enabledWith(Map.of(key("pdp.url"), " "))),
				Arguments.of("relative url", enabledWith(Map.of(key("pdp.url"), "/validate"))),
				Arguments.of("url without scheme", enabledWith(Map.of(key("pdp.url"), "odrl-pap:8080"))),
				Arguments.of("both, service url", enabledWith(Map.of(key("pdp.url"), VALID_PDP_URL,
						PDP_SERVICE_URL_KEY, VALID_PDP_URL))),
				Arguments.of("both, service urls", enabledWith(Map.of(key("pdp.url"), VALID_PDP_URL,
						PDP_SERVICE_URLS_KEY, List.of(VALID_PDP_URL)))));
	}

	@DisplayName("An enabled access control needs exactly one PDP endpoint, pdp.url being an absolute http(s) URI.")
	@ParameterizedTest(name = "{0}")
	@MethodSource("invalidPdpEndpoints")
	void invalidPdpEndpointFailsStartup(String name, Map<String, Object> configuration) {
		invalidValuesFailStartup(configuration, "pdp.url");
	}

	private static Stream<Arguments> acceptedPdpEndpoints() {
		return Stream.of(
				Arguments.of("pdp.url", enabledWith(Map.of(key("pdp.url"), VALID_PDP_URL))),
				Arguments.of("service url", enabledWith(Map.of(PDP_SERVICE_URL_KEY, VALID_PDP_URL))),
				Arguments.of("service urls", enabledWith(Map.of(PDP_SERVICE_URLS_KEY, List.of(VALID_PDP_URL)))));
	}

	@DisplayName("Either PDP endpoint starts an enabled access control with a PDP client.")
	@ParameterizedTest(name = "{0}")
	@MethodSource("acceptedPdpEndpoints")
	void acceptedPdpEndpointStarts(String name, Map<String, Object> configuration) {
		try (ApplicationContext context = ApplicationContext.run(configuration)) {
			assertNotNull(context.getBean(PolicyDecisionPoint.class), "The PDP client should exist once enabled.");
		}
	}

	private static Stream<Arguments> nonCanonicalEnabledValues() {
		return Stream.of(
				Arguments.of("TRUE"),
				Arguments.of("True"),
				Arguments.of("FALSE"),
				Arguments.of("ture"));
	}

	@DisplayName("Only lowercase true or false is accepted for enabled, since the bean gate matches the raw string exactly.")
	@ParameterizedTest(name = "{0}")
	@MethodSource("nonCanonicalEnabledValues")
	void nonCanonicalEnabledFailsStartup(String value) {
		invalidValuesFailStartup(Map.of(key("enabled"), value, key("jwt.jwks-uri"), VALID_JWKS_URI,
				key("jwt.issuer"), VALID_ISSUER), "enabled");
	}

	@DisplayName("Micronaut's @Requires(value = \"true\") does not match a differently cased true, which is why the guard rejects it.")
	@ParameterizedTest(name = "{0} -> {1}")
	@CsvSource({"true, true", "TRUE, false", "True, false"})
	void requiresGateMatchesExactly(String value, boolean expected) {
		try (ApplicationContext context = ApplicationContext.run(Map.of(GateProbes.CASE_PROBE_PROPERTY, value))) {
			assertEquals(expected, context.containsBean(GateProbes.CaseProbe.class),
					"If this changes, the guard may accept any casing of enabled.");
		}
	}

	private static Stream<Arguments> acceptedEnabledValues() {
		return Stream.of(
				Arguments.of(Map.of(key("mode"), "observe")),
				Arguments.of(Map.of(key("enabled"), "false")),
				Arguments.of(Map.of(key("enabled"), "true", key("jwt.jwks-uri"), VALID_JWKS_URI,
						key("jwt.issuer"), VALID_ISSUER, key("pdp.url"), VALID_PDP_URL)));
	}

	@DisplayName("Every enabled value the guard accepts is read alike by the bound properties and by the bean gate.")
	@ParameterizedTest(name = "{0}")
	@MethodSource("acceptedEnabledValues")
	void guardAndGateAgreeOnEnabled(Map<String, Object> configuration) {
		try (ApplicationContext context = ApplicationContext.run(configuration)) {
			assertEquals(context.getBean(AccessPolicyProperties.class).isEnabled(),
					context.containsBean(GateProbes.EnabledProbe.class),
					"A bean gated on enabled must exist exactly when the properties report the feature as enabled.");
		}
	}

	private static boolean guardRejected(Throwable thrown, String key) {
		for (Throwable cause = thrown; cause != null; cause = cause.getCause()) {
			if (cause instanceof ConfigurationException && cause.getMessage().startsWith(key + ":")) {
				return true;
			}
		}
		return false;
	}
}
