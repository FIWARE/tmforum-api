package org.fiware.tmforum.product;

import io.github.wistefan.mapping.UnmappedProperty;
import io.micronaut.context.annotation.Requires;
import jakarta.inject.Singleton;
import org.fiware.tmforum.accesspolicy.AccessPolicyProperties;
import org.fiware.tmforum.accesspolicy.CarriedPolicy;
import org.fiware.tmforum.accesspolicy.PolicyCarrier;
import org.fiware.tmforum.common.accesspolicy.PolicyValues;
import tools.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Reads the policies a {@link ProductOffering} carries as {@code accessPolicy}/{@code contractPolicy} on its terms.
 * {@code ProductOfferingPrice.productOfferingTerm} is not read (AP-01).
 */
@Singleton
@Requires(property = AccessPolicyProperties.ENABLED_PROPERTY, value = "true")
public class ProductOfferingPolicyCarrier implements PolicyCarrier<ProductOffering> {

	/** The term extension holding the policy that governs catalog visibility. */
	public static final String ACCESS_POLICY = "accessPolicy";
	/** The term extension holding the policy offered for contract negotiation. */
	public static final String CONTRACT_POLICY = "contractPolicy";

	private static final Set<String> POLICY_PROPERTIES = Set.of(ACCESS_POLICY, CONTRACT_POLICY);
	private static final String TERM_LOCATION = "productOfferingTerm[%d].%s";

	private final ObjectMapper objectMapper;

	public ProductOfferingPolicyCarrier(ObjectMapper objectMapper) {
		this.objectMapper = objectMapper;
	}

	@Override
	public Class<ProductOffering> carriedType() {
		return ProductOffering.class;
	}

	@Override
	public List<CarriedPolicy> carriedPolicies(ProductOffering entity) {
		List<ProductOfferingTerm> terms = entity.getProductOfferingTerm();
		if (terms == null) {
			return List.of();
		}
		List<CarriedPolicy> carried = new ArrayList<>();
		for (int i = 0; i < terms.size(); i++) {
			ProductOfferingTerm term = terms.get(i);
			List<UnmappedProperty> properties = term == null ? null : term.getAdditionalProperties();
			if (properties == null) {
				continue;
			}
			for (UnmappedProperty property : properties) {
				if (property != null && POLICY_PROPERTIES.contains(property.getName())) {
					carried.add(PolicyValues.carried(TERM_LOCATION.formatted(i, property.getName()),
							property.getValue(), objectMapper));
				}
			}
		}
		return carried;
	}
}
