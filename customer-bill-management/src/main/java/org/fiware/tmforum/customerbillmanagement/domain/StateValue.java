package org.fiware.tmforum.customerbillmanagement.domain;

import lombok.Getter;

public enum StateValue {

	NEW("new"),
	ONHOLD("onHold"),
	VALIDATED("validated"),
	SENT("sent"),
	PARTIALLYPAID("partiallyPaid"),
	SETTLED("settled");

	@Getter
	private final String value;

	StateValue(String value) {
		this.value = value;
	}
}
