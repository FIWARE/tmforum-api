package org.fiware.tmforum.quote.domain;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum QuoteState {

	IN_PROGRESS("inProgress"),
	APPROVED("approved"),
	PENDING("rejected"),
	CANCELLED("cancelled"),
	ACCEPTED("accepted"),
	REJECTED("rejected");

	private final String value;

	QuoteState(String value) {
		this.value = value;
	}

	@JsonValue
	public String getValue() {
		return value;
	}

	@JsonCreator
	public static QuoteState toEnum(String value) {
		return java.util.Arrays
				.stream(values())
				.filter(e -> e.value.equals(value))
				.findAny()
				.orElseThrow(() -> new IllegalArgumentException("Unknown value '" + value + "'."));
	}
}
