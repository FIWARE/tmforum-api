package org.fiware.tmforum.service;

import io.micronaut.context.annotation.Requires;
import jakarta.inject.Singleton;
import org.fiware.tmforum.accesspolicy.AccessPolicyProperties;
import org.fiware.tmforum.accesspolicy.CarriedPolicy;
import org.fiware.tmforum.accesspolicy.PolicyCarrier;
import org.fiware.tmforum.servicecatalog.domain.ServiceSpecification;
import tools.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.List;

/** Reads the policies a {@link ServiceSpecification} carries in its {@code authorizationPolicy} characteristics. */
@Singleton
@Requires(property = AccessPolicyProperties.ENABLED_PROPERTY, value = "true")
public class ServiceSpecificationPolicyCarrier implements PolicyCarrier<ServiceSpecification> {

	private static final String VALUES_LOCATION = "specCharacteristic[%d].characteristicValueSpecification";

	private final ObjectMapper objectMapper;

	public ServiceSpecificationPolicyCarrier(ObjectMapper objectMapper) {
		this.objectMapper = objectMapper;
	}

	@Override
	public Class<ServiceSpecification> carriedType() {
		return ServiceSpecification.class;
	}

	@Override
	public List<CarriedPolicy> carriedPolicies(ServiceSpecification entity) {
		List<CharacteristicSpecification> characteristics = entity.getSpecCharacteristic();
		if (characteristics == null) {
			return List.of();
		}
		List<CarriedPolicy> carried = new ArrayList<>();
		for (int i = 0; i < characteristics.size(); i++) {
			CharacteristicSpecification characteristic = characteristics.get(i);
			if (characteristic != null && CharacteristicPolicyValues.AUTHORIZATION_POLICY_VALUE_TYPE.equals(
					characteristic.getValueType())) {
				carried.addAll(CharacteristicPolicyValues.fromValues(VALUES_LOCATION.formatted(i),
						characteristic.getCharacteristicValueSpecification(), objectMapper));
			}
		}
		return carried;
	}
}
