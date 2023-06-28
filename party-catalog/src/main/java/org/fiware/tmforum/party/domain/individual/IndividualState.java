package org.fiware.tmforum.party.domain.individual;

import com.fasterxml.jackson.annotation.JsonValue;

public enum IndividualState {

	INITIALIZED("initialized"),
	VALIDATED("validated"),
	DECEADED("deceaded");

	private final String value;

	IndividualState(String value) {
		this.value = value;
	}

	@JsonValue
	public String getValue() {
		return value;
	}
}
