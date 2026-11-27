package org.fiware.tmforum.accesspolicy;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The policies an entity carries at one scope, keyed by {@code odrl:uid} in carried order, plus the ones that could
 * not be read. Not empty while anything is unreadable, so such an entity is never served unchecked.
 */
public record ResolvedPolicies(Map<String, Map<String, Object>> policies, List<UnreadablePolicy> unreadable) {

	public static final ResolvedPolicies NONE = new ResolvedPolicies(Map.of(), List.of());

	public ResolvedPolicies {
		policies = Collections.unmodifiableMap(new LinkedHashMap<>(policies));
		unreadable = List.copyOf(unreadable);
	}

	static ResolvedPolicies unreadable(UnreadablePolicy policy) {
		return new ResolvedPolicies(Map.of(), List.of(policy));
	}

	public boolean isEmpty() {
		return policies.isEmpty() && unreadable.isEmpty();
	}
}
