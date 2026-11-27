package org.fiware.tmforum.accesspolicy;

/** The {@code dspace:scope} values this project evaluates; a rule at any other scope or none is ignored. */
public enum PolicyScope {
	CATALOG("catalog"),
	CONTRACT_NEGOTIATION("contract.negotiation");

	private final String value;

	PolicyScope(String value) {
		this.value = value;
	}

	/** The wire string, as written in a policy and sent to the PDP. */
	public String value() {
		return value;
	}
}
