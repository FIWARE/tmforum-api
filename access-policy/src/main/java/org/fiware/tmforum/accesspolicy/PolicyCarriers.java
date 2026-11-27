package org.fiware.tmforum.accesspolicy;

import io.micronaut.context.annotation.Context;
import io.micronaut.context.annotation.Requires;
import io.micronaut.context.exceptions.ConfigurationException;

import java.util.List;

/**
 * The registry of every {@link PolicyCarrier} bean. Answers per class whether it is governed, so an ungoverned entity
 * is never read; eager ({@code @Context}) so two carriers whose types are equal or related fail the boot.
 */
@Context
@Requires(property = AccessPolicyProperties.ENABLED_PROPERTY, value = "true")
public class PolicyCarriers {

	private static final String RELATED_CARRIERS = "Policy carriers %s and %s govern related types %s and %s";
	private static final String AMBIGUOUS_CARRIERS = "ambiguous policy carriers: ";

	private final ClassValue<List<PolicyCarrier<?>>> carriersByType;

	public PolicyCarriers(List<PolicyCarrier<?>> carriers) {
		List<PolicyCarrier<?>> registered = List.copyOf(carriers);
		for (int i = 0; i < registered.size(); i++) {
			for (int j = i + 1; j < registered.size(); j++) {
				requireUnrelated(registered.get(i), registered.get(j));
			}
		}
		this.carriersByType = new ClassValue<>() {
			@Override
			protected List<PolicyCarrier<?>> computeValue(Class<?> type) {
				return registered.stream().filter(carrier -> carrier.carriedType().isAssignableFrom(type)).toList();
			}
		};
	}

	private static void requireUnrelated(PolicyCarrier<?> first, PolicyCarrier<?> second) {
		Class<?> firstType = first.carriedType();
		Class<?> secondType = second.carriedType();
		if (firstType.isAssignableFrom(secondType) || secondType.isAssignableFrom(firstType)) {
			throw new ConfigurationException(RELATED_CARRIERS.formatted(first.getClass().getName(),
					second.getClass().getName(), firstType.getName(), secondType.getName()));
		}
	}

	/** True when a carrier governs {@code type} or one of its supertypes. */
	public boolean isGoverned(Class<?> type) {
		return !carriersByType.get(type).isEmpty();
	}

	/** The policies {@code entity} carries at {@code scope}; {@link ResolvedPolicies#NONE} for null or ungoverned. */
	public ResolvedPolicies resolve(Object entity, PolicyScope scope) {
		if (entity == null) {
			return ResolvedPolicies.NONE;
		}
		List<PolicyCarrier<?>> matching = carriersByType.get(entity.getClass());
		if (matching.isEmpty()) {
			return ResolvedPolicies.NONE;
		}
		if (matching.size() > 1) {
			return ResolvedPolicies.unreadable(new UnreadablePolicy(entity.getClass().getName(), null,
					AMBIGUOUS_CARRIERS + matching.stream().map(carrier -> carrier.carriedType().getName()).toList()));
		}
		return resolve(matching.getFirst(), entity, scope);
	}

	private static <T> ResolvedPolicies resolve(PolicyCarrier<T> carrier, Object entity, PolicyScope scope) {
		return carrier.resolve(carrier.carriedType().cast(entity), scope);
	}
}
