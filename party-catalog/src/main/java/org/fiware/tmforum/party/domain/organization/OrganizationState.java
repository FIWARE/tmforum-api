package org.fiware.tmforum.party.domain.organization;

import com.fasterxml.jackson.annotation.JsonValue;

public enum OrganizationState {

	INITIALIZED("initialized"),
	VALIDATED("validated"),
	CLOSED("closed");

	private final String value;

	OrganizationState(String value) {
		this.value = value;
	}

	@JsonValue
	public String getValue() {
		return value;
	}
}
