package org.fiware.tmforum.accesspolicy;

import java.util.List;

/**
 * Finds the ODRL policies an entity type carries. Implementations only walk their domain object; scope filtering,
 * uid keying and malformed handling live in {@link #resolve}, which implementations do not override.
 */
public interface PolicyCarrier<T> {

	/** The governed type; subclasses resolve through this carrier too. */
	Class<T> carriedType();

	/** Every raw policy on {@code entity}, at any scope, in a stable order. */
	List<CarriedPolicy> carriedPolicies(T entity);

	/** The policies {@code entity} carries at {@code scope}; never throws, a failing walk comes back unreadable. */
	default ResolvedPolicies resolve(T entity, PolicyScope scope) {
		try {
			List<CarriedPolicy> carried = carriedPolicies(entity);
			if (carried == null) {
				return ResolvedPolicies.unreadable(new UnreadablePolicy(carriedType().getName(), null,
						ScopedPolicyResolution.NO_CARRIED_POLICIES));
			}
			return ScopedPolicyResolution.resolve(carried, scope);
		} catch (RuntimeException e) {
			return ResolvedPolicies.unreadable(new UnreadablePolicy(carriedType().getName(), null,
					ScopedPolicyResolution.CARRIER_FAILED + e));
		}
	}
}
