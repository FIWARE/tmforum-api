package org.fiware.tmforum.accesspolicy;

import io.micronaut.context.annotation.Context;
import io.micronaut.context.annotation.Requires;
import io.micronaut.context.env.Environment;
import io.micronaut.context.exceptions.ConfigurationException;
import org.fiware.tmforum.accesspolicy.AccessPolicyProperties.Decision;
import org.fiware.tmforum.accesspolicy.AccessPolicyProperties.Mode;
import org.fiware.tmforum.accesspolicy.AccessPolicyProperties.TotalCount;

import java.time.Duration;
import java.util.Arrays;
import java.util.Map;
import java.util.Optional;

/**
 * Fails the startup on an access-policy value that does not convert, since Micronaut binds any unknown boolean
 * string as {@code false} and the default would win silently.
 */
@Context
@Requires(property = AccessPolicyProperties.PREFIX)
public class AccessPolicyConfigurationGuard {

	private static final String TRUE = Boolean.TRUE.toString();
	private static final String FALSE = Boolean.FALSE.toString();

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
			typedKey("pdp.timeout", Duration.class),
			typedKey("audit.enabled", Boolean.class));

	public AccessPolicyConfigurationGuard(Environment environment) {
		TYPED_KEYS.forEach((key, type) -> environment.getProperty(key, String.class)
				.ifPresent(raw -> check(environment, key, raw, type)));
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
