package org.fiware.tmforum.accesspolicy;

import java.util.Objects;

/**
 * A PDP verdict. {@code source} tells a policy denial from a failed PDP call or an unreadable policy;
 * {@code fail-open} flips {@code permitted} but never {@code source}.
 */
public record AccessDecision(boolean permitted, String reason, String policyUid, Source source) {

	public AccessDecision {
		Objects.requireNonNull(source, "source");
	}

	/** A verdict the PDP actually reached on the policy. */
	public static AccessDecision policy(boolean permitted, String reason, String policyUid) {
		return new AccessDecision(permitted, reason, policyUid, Source.POLICY);
	}

	/** A PDP call that produced no verdict; {@code failOpen} decides whether it still permits. */
	public static AccessDecision failure(String policyUid, String reason, boolean failOpen) {
		return new AccessDecision(failOpen, reason, policyUid, Source.PDP_FAILURE);
	}

	/** A policy that could not be read, so the PDP was never asked; {@code failOpen} decides whether it permits. */
	public static AccessDecision unreadable(String policyUid, String reason, boolean failOpen) {
		return new AccessDecision(failOpen, reason, policyUid, Source.UNREADABLE_POLICY);
	}

	public enum Source {
		POLICY,
		PDP_FAILURE,
		UNREADABLE_POLICY
	}
}
