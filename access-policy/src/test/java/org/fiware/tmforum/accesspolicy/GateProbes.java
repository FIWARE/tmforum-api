package org.fiware.tmforum.accesspolicy;

import io.micronaut.context.annotation.Requires;
import jakarta.inject.Singleton;

/**
 * Beans gated like the feature's beans, to observe how {@code @Requires(value = "true")} reads a property.
 */
final class GateProbes {

	static final String CASE_PROBE_PROPERTY = "gate-probe.enabled";

	private GateProbes() {
	}

	/**
	 * Gated exactly as every access-policy bean is.
	 */
	@Singleton
	@Requires(property = AccessPolicyProperties.ENABLED_PROPERTY, value = "true")
	static class EnabledProbe {
	}

	/**
	 * Same gate on a key outside the feature, so a non-canonical value reaches it without the guard failing startup.
	 */
	@Singleton
	@Requires(property = CASE_PROBE_PROPERTY, value = "true")
	static class CaseProbe {
	}
}
