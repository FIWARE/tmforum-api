package org.fiware.tmforum.customerbillmanagement.domain;

import com.fasterxml.jackson.annotation.JsonValue;

public enum StateValue {

	NEW("new"),
	ONHOLD("onHold"),
	VALIDATED("validated"),
	SENT("sent"),
	PARTIALLYPAID("partiallyPaid"),
	SETTLED("settled");

	private final String value;

	StateValue(String value) {
		this.value = value;
	}

	@JsonValue
	public String getValue() {
		return value;
	}
}
