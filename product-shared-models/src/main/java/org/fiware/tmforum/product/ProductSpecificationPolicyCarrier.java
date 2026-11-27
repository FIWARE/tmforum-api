package org.fiware.tmforum.product;

import io.micronaut.context.annotation.Requires;
import jakarta.inject.Singleton;
import org.fiware.tmforum.accesspolicy.AccessPolicyProperties;
import org.fiware.tmforum.accesspolicy.CarriedPolicy;
import org.fiware.tmforum.accesspolicy.PolicyCarrier;
import org.fiware.tmforum.service.CharacteristicPolicyValues;
import tools.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.List;

/** Reads the policies a {@link ProductSpecification} carries in its {@code authorizationPolicy} characteristics. */
@Singleton
@Requires(property = AccessPolicyProperties.ENABLED_PROPERTY, value = "true")
public class ProductSpecificationPolicyCarrier implements PolicyCarrier<ProductSpecification> {

	private static final String VALUES_LOCATION = "productSpecCharacteristic[%d].productSpecCharacteristicValue";

	private final ObjectMapper objectMapper;

	public ProductSpecificationPolicyCarrier(ObjectMapper objectMapper) {
		this.objectMapper = objectMapper;
	}

	@Override
	public Class<ProductSpecification> carriedType() {
		return ProductSpecification.class;
	}

	@Override
	public List<CarriedPolicy> carriedPolicies(ProductSpecification entity) {
		List<ProductSpecificationCharacteristic> characteristics = entity.getProductSpecCharacteristic();
		if (characteristics == null) {
			return List.of();
		}
		List<CarriedPolicy> carried = new ArrayList<>();
		for (int i = 0; i < characteristics.size(); i++) {
			ProductSpecificationCharacteristic characteristic = characteristics.get(i);
			if (characteristic != null && CharacteristicPolicyValues.AUTHORIZATION_POLICY_VALUE_TYPE.equals(
					characteristic.getAtValueType())) {
				carried.addAll(CharacteristicPolicyValues.fromValues(VALUES_LOCATION.formatted(i),
						characteristic.getProductSpecCharacteristicValue(), objectMapper));
			}
		}
		return carried;
	}
}
