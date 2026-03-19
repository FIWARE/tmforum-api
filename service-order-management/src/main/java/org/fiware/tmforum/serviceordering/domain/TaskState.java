package org.fiware.tmforum.serviceordering.domain;

import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Possible states for a task resource such as CancelServiceOrder.
 */
public enum TaskState {

	ACCEPTED("accepted"),
	TERMINATED_WITH_ERROR("terminatedWithError"),
	IN_PROGRESS("inProgress"),
	DONE("done");

	private final String value;

	TaskState(String value) {
		this.value = value;
	}

	/**
	 * Returns the JSON-serialized value of this state.
	 *
	 * @return the string value used for JSON serialization
	 */
	@JsonValue
	public String getValue() {
		return value;
	}
}
