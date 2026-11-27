package org.fiware.tmforum.common.accesspolicy;

import org.fiware.tmforum.accesspolicy.CarriedPolicy;
import tools.jackson.databind.ObjectMapper;

import java.util.Map;

/**
 * Turns a domain field holding a policy into a {@link CarriedPolicy}, so carriers never cast the stored value.
 */
public final class PolicyValues {

	private PolicyValues() {
	}

	/**
	 * Converts {@code value} to a JSON map through {@code objectMapper}; never throws. A value that does not convert
	 * stays raw, so resolution reports it as not a JSON object.
	 */
	public static CarriedPolicy carried(String location, Object value, ObjectMapper objectMapper) {
		if (value == null || value instanceof Map<?, ?>) {
			return new CarriedPolicy(location, value);
		}
		try {
			Map<?, ?> converted = objectMapper.convertValue(value, Map.class);
			return new CarriedPolicy(location, converted == null ? value : converted);
		} catch (RuntimeException e) {
			// a value Jackson cannot shape into an object is malformed authoring, reported downstream
			return new CarriedPolicy(location, value);
		}
	}
}
