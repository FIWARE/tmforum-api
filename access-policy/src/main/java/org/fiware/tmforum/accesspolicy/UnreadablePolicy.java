package org.fiware.tmforum.accesspolicy;

/** A carried policy resolution could not interpret; {@code policyUid} is null when none could be read. */
public record UnreadablePolicy(String location, String policyUid, String reason) {

	/** The decision standing in for a PDP verdict; {@code failOpen} decides whether it permits. */
	public AccessDecision decision(boolean failOpen) {
		return AccessDecision.unreadable(policyUid, location + ": " + reason, failOpen);
	}
}
