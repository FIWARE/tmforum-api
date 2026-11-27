package org.fiware.tmforum.service;

import org.fiware.tmforum.accesspolicy.CarriedPolicy;
import org.fiware.tmforum.common.accesspolicy.PolicyValues;
import tools.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.List;

/**
 * Reads the policies held in the values of an {@code authorizationPolicy} characteristic, the shape service and product
 * specifications share (AP-01).
 */
public final class CharacteristicPolicyValues {

	/** The characteristic {@code valueType} marking its values as ODRL policies. */
	public static final String AUTHORIZATION_POLICY_VALUE_TYPE = "authorizationPolicy";

	private static final String VALUE_LOCATION = "%s[%d].value";

	private CharacteristicPolicyValues() {
	}

	/** One carried policy per non-null entry of {@code values}, located as {@code <prefix>[j].value}; null-safe. */
	public static List<CarriedPolicy> fromValues(String locationPrefix, List<CharacteristicValueSpecification> values,
			ObjectMapper objectMapper) {
		if (values == null) {
			return List.of();
		}
		List<CarriedPolicy> carried = new ArrayList<>();
		for (int j = 0; j < values.size(); j++) {
			CharacteristicValueSpecification value = values.get(j);
			if (value != null) {
				carried.add(PolicyValues.carried(VALUE_LOCATION.formatted(locationPrefix, j), value.getTmfValue(),
						objectMapper));
			}
		}
		return carried;
	}
}
