package org.fiware.tmforum.accesspolicy;

import io.micronaut.context.annotation.Context;
import io.micronaut.context.annotation.Requires;
import io.micronaut.context.env.Environment;
import io.micronaut.context.exceptions.ConfigurationException;
import io.micronaut.core.type.Argument;
import org.fiware.tmforum.accesspolicy.AccessPolicyProperties.Decision;
import org.fiware.tmforum.accesspolicy.AccessPolicyProperties.Mode;
import org.fiware.tmforum.accesspolicy.AccessPolicyProperties.TotalCount;

import java.net.URI;
import java.net.URISyntaxException;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Fails the startup on an access-policy value that does not convert, since Micronaut binds any unknown boolean
 * string as {@code false} and the default would win silently, and on an enabled feature without the token settings,
 * without exactly one PDP endpoint, or with a PDP path or timeout every call would fail on.
 */
@Context
@Requires(property = AccessPolicyProperties.PREFIX)
public class AccessPolicyConfigurationGuard {

	private static final String TRUE = Boolean.TRUE.toString();
	private static final String FALSE = Boolean.FALSE.toString();

	private static final String JWKS_URI_KEY = AccessPolicyProperties.PREFIX + ".jwt.jwks-uri";
	private static final String ISSUER_KEY = AccessPolicyProperties.PREFIX + ".jwt.issuer";
	private static final String AUDIENCE_KEY = AccessPolicyProperties.PREFIX + ".jwt.audience";
	private static final String PDP_URL_KEY = AccessPolicyProperties.PREFIX + ".pdp.url";
	private static final String PDP_PATH_KEY = AccessPolicyProperties.PREFIX + ".pdp.path";
	private static final String PDP_TIMEOUT_KEY = AccessPolicyProperties.PREFIX + ".pdp.timeout";
	private static final String PDP_SERVICE_URL_KEY = PolicyDecisionPoint.SERVICE_PREFIX + ".url";
	private static final String PDP_SERVICE_URLS_KEY = PolicyDecisionPoint.SERVICE_PREFIX + ".urls";
	private static final String PDP_SERVICE_KEYS = PDP_SERVICE_URL_KEY + " / " + PDP_SERVICE_URLS_KEY;
	private static final Set<String> HTTP_SCHEMES = Set.of("http", "https");

	private static final Map<String, Class<?>> TYPED_KEYS = Map.ofEntries(
			typedKey("enabled", Boolean.class),
			typedKey("mode", Mode.class),
			typedKey("default-decision", Decision.class),
			typedKey("anonymous-decision", Decision.class),
			typedKey("bypass-unauthenticated", Boolean.class),
			typedKey("owner-exempt", Boolean.class),
			typedKey("derive-catalog-visibility", Boolean.class),
			typedKey("total-count", TotalCount.class),
			typedKey("max-scan", Integer.class),
			typedKey("overfetch-factor", Integer.class),
			typedKey("decision-ttl", Duration.class),
			typedKey("fail-open", Boolean.class),
			Map.entry(PDP_TIMEOUT_KEY, Duration.class),
			typedKey("audit.enabled", Boolean.class));

	public AccessPolicyConfigurationGuard(Environment environment) {
		Optional<String> enabled = environment.getProperty(AccessPolicyProperties.ENABLED_PROPERTY, String.class);
		enabled.ifPresent(AccessPolicyConfigurationGuard::checkEnabled);
		TYPED_KEYS.forEach((key, type) -> environment.getProperty(key, String.class)
				.ifPresent(raw -> check(environment, key, raw, type)));
		if (enabled.filter(TRUE::equals).isPresent()) {
			checkJwt(environment);
			checkPdpEndpoint(environment);
			checkPdpPath(environment);
			checkPdpTimeout(environment);
		}
	}

	// @Requires(value = "true") matches the raw string exactly, so a "TRUE" would bind as enabled while no gated bean exists
	private static void checkEnabled(String raw) {
		if (!TRUE.equals(raw) && !FALSE.equals(raw)) {
			throw invalid(AccessPolicyProperties.ENABLED_PROPERTY, raw, "must be lowercase true or false");
		}
	}

	// without a key set and an issuer, every token would degrade to anonymous and the feature would bite nobody
	private static void checkJwt(Environment environment) {
		String jwksUri = environment.getProperty(JWKS_URI_KEY, String.class).orElse("");
		if (!isAbsoluteHttpUri(jwksUri)) {
			throw invalid(JWKS_URI_KEY, jwksUri, "is required while enabled and must be an absolute http(s) URI");
		}
		String issuer = environment.getProperty(ISSUER_KEY, String.class).orElse("");
		if (issuer.isBlank()) {
			throw invalid(ISSUER_KEY, issuer, "is required while enabled");
		}
		// a blank audience is never in a token's aud, so every token would degrade to anonymous
		environment.getProperty(AUDIENCE_KEY, String.class).filter(String::isBlank).ifPresent(audience -> {
			throw invalid(AUDIENCE_KEY, audience, "must not be blank, leave it unset to skip the audience check");
		});
	}

	// pdp.url is used as an absolute URI that bypasses the service, so setting both would silently ignore one of them
	private static void checkPdpEndpoint(Environment environment) {
		String url = environment.getProperty(PDP_URL_KEY, String.class).orElse("");
		boolean serviceUrlSet = checkServiceUrls(PDP_SERVICE_URL_KEY,
				environment.getProperty(PDP_SERVICE_URL_KEY, String.class).map(List::of).orElse(List.of()));
		boolean serviceUrlsSet = checkServiceUrls(PDP_SERVICE_URLS_KEY,
				environment.getProperty(PDP_SERVICE_URLS_KEY, Argument.listOf(String.class)).orElse(List.of()));
		boolean serviceConfigured = serviceUrlSet || serviceUrlsSet;
		if (url.isBlank() && !serviceConfigured) {
			throw invalid(PDP_URL_KEY, url, String.format("or %s is required while enabled", PDP_SERVICE_KEYS));
		}
		if (!url.isBlank() && serviceConfigured) {
			throw invalid(PDP_URL_KEY, url, String.format("and %s are mutually exclusive, set only one", PDP_SERVICE_KEYS));
		}
		if (!url.isBlank() && !isAbsoluteHttpUri(url)) {
			throw invalid(PDP_URL_KEY, url, "must be an absolute http(s) URI");
		}
	}

	// blank counts as unset, like pdp.url, since Helm charts default env values to empty strings
	private static boolean checkServiceUrls(String key, List<String> urls) {
		List<String> configured = urls.stream().filter(url -> !url.isBlank()).toList();
		configured.stream().filter(url -> !isAbsoluteHttpUri(url)).findFirst().ifPresent(url -> {
			throw invalid(key, url, "must be an absolute http(s) URI");
		});
		return !configured.isEmpty();
	}

	// the path is parsed only on the first call, so a malformed one would otherwise surface on every request
	private static void checkPdpPath(Environment environment) {
		environment.getProperty(PDP_PATH_KEY, String.class).ifPresent(path -> {
			if (!isRelativePath(path)) {
				throw invalid(PDP_PATH_KEY, path, "must be a path, e.g. " + AccessPolicyProperties.DEFAULT_PDP_PATH);
			}
		});
	}

	// a non-positive timeout fails every call, which fail-open would turn into a silent permit-all
	private static void checkPdpTimeout(Environment environment) {
		environment.getProperty(PDP_TIMEOUT_KEY, String.class).ifPresent(raw -> {
			Duration timeout = environment.getConversionService().convert(raw, Duration.class).orElseThrow();
			if (timeout.isZero() || timeout.isNegative()) {
				throw invalid(PDP_TIMEOUT_KEY, raw, "must be positive while enabled");
			}
		});
	}

	private static boolean isRelativePath(String raw) {
		try {
			URI uri = new URI(raw);
			return !raw.isBlank() && uri.getScheme() == null && uri.getRawAuthority() == null;
		} catch (URISyntaxException e) {
			return false;
		}
	}

	private static boolean isAbsoluteHttpUri(String raw) {
		try {
			URI uri = new URI(raw);
			return uri.isAbsolute() && uri.getHost() != null
					&& HTTP_SCHEMES.contains(uri.getScheme().toLowerCase(Locale.ROOT));
		} catch (URISyntaxException e) {
			return false;
		}
	}

	private static Map.Entry<String, Class<?>> typedKey(String suffix, Class<?> type) {
		return Map.entry(AccessPolicyProperties.PREFIX + "." + suffix, type);
	}

	private static void check(Environment environment, String key, String raw, Class<?> type) {
		if (type == Boolean.class) {
			// the conversion service maps every unknown string to false, so only the raw value tells a typo
			if (!TRUE.equalsIgnoreCase(raw) && !FALSE.equalsIgnoreCase(raw)) {
				throw invalid(key, raw, "is not a boolean, use true or false");
			}
			return;
		}
		Optional<?> converted;
		try {
			converted = environment.getConversionService().convert(raw, type);
		} catch (RuntimeException e) {
			converted = Optional.empty();
		}
		if (converted.isEmpty()) {
			throw invalid(key, raw, describe(type));
		}
	}

	private static String describe(Class<?> type) {
		if (type == Duration.class) {
			return "is not a duration, a unit is mandatory (e.g. 60s)";
		}
		if (type.isEnum()) {
			return String.format("is not one of %s", Arrays.toString(type.getEnumConstants()));
		}
		return String.format("is not a valid %s", type.getSimpleName());
	}

	private static ConfigurationException invalid(String key, String raw, String reason) {
		return new ConfigurationException(String.format("%s: '%s' %s", key, raw, reason));
	}
}
