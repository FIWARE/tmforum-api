package org.fiware.tmforum.customerbillmanagement.domain;

import com.fasterxml.jackson.annotation.JsonValue;

public enum OnDemandStateValue {

	IN_PROGRESS("inProgress"),
	REJECTED("rejected"),
	DONE("done"),
	TERMINATED_WITH_ERROR("terminatedWithError");

	private final String value;

	OnDemandStateValue(String value) {
		this.value = value;
	}

	@JsonValue
	public String getValue() {
		return value;
	}
}
