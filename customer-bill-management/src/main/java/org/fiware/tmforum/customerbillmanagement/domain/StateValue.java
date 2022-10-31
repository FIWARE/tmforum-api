package org.fiware.tmforum.customerbillmanagement.domain;

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
}
