package org.fiware.tmforum.party.domain.organization;

public enum OrganizationState {

	INITIALIZED("initialized"),
	VALIDATED("validated"),
	CLOSED("closed");

	private final String value;

	OrganizationState(String value) {
		this.value = value;
	}
}
