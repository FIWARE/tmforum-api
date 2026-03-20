package org.fiware.tmforum.resourceordering.domain;

import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Possible states for a resource order item.
 */
public enum ResourceOrderItemState {
	ACKNOWLEDGED("acknowledged"),
	REJECTED("rejected"),
	PENDING("pending"),
	HELD("held"),
	IN_PROGRESS("inProgress"),
	CANCELLED("cancelled"),
	COMPLETED("completed"),
	FAILED("failed"),
	ASSESSING_CANCELLATION("assessingCancellation"),
	PENDING_CANCELLATION("pendingCancellation");

	private final String value;

	ResourceOrderItemState(String value) {
		this.value = value;
	}

	@JsonValue
	public String getValue() {
		return value;
	}
}
