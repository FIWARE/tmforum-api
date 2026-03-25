package org.fiware.tmforum.resourceordering.domain;

import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Possible states for a task resource such as CancelResourceOrder.
 */
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
