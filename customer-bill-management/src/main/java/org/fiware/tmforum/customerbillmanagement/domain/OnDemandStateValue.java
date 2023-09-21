package org.fiware.tmforum.customerbillmanagement.domain;

import com.fasterxml.jackson.annotation.JsonValue;

public enum OnDemandStateValue {

	INPROGRESS("inProgress"),
	REJECTED("rejected"),
	DONE("done"),
	TERMINATEDWITHERROR("terminatedWithError");

	private final String value;

	OnDemandStateValue(String value) {
		this.value = value;
	}

	@JsonValue
	public String getValue() {
		return value;
	}
}
