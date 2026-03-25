package org.fiware.tmforum.serviceordering.domain;

import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Possible states for a service order, following the TMF641 lifecycle.
 */
public enum ServiceOrderState {
	ACKNOWLEDGED("acknowledged"),
	REJECTED("rejected"),
	PENDING("pending"),
	HELD("held"),
	IN_PROGRESS("inProgress"),
	CANCELLED("cancelled"),
	COMPLETED("completed"),
	FAILED("failed"),
	PARTIAL("partial"),
	ASSESSING_CANCELLATION("assessingCancellation"),
	PENDING_CANCELLATION("pendingCancellation");

	private final String value;

	ServiceOrderState(String value) {
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
