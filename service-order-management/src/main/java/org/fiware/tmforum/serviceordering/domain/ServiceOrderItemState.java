package org.fiware.tmforum.serviceordering.domain;

import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Possible states for a service order item.
 */
public enum ServiceOrderItemState {
	ACKNOWLEDGED("acknowledged"),
	REJECTED("rejected"),
	PENDING("pending"),
	HELD("held"),
	IN_PROGRESS("inProgress"),
	CANCELLED("cancelled"),
	COMPLETED("completed"),
	FAILED("failed"),
	ASSESSING_CANCELLATION("assessingCancellation"),
	PENDING_CANCELLATION("pendingCancellation"),
	PARTIAL("partial");

	private final String value;

	ServiceOrderItemState(String value) {
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
