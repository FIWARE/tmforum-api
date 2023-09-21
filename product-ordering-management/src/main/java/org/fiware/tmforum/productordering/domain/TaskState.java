package org.fiware.tmforum.productordering.domain;

import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

public enum TaskState {
	
	ACKNOWLEDGED("acknowledged"),
	TERMINATED_WITH_ERROR("terminatedWithError"),
	IN_PROGRESS("inProgress"),
	DONE("done");

	private final String value;

	TaskState(String value) {
		this.value = value;
	}

	@JsonValue
	public String getValue() {
		return value;
	}
}
