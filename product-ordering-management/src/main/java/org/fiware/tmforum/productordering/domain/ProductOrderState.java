package org.fiware.tmforum.productordering.domain;

import com.fasterxml.jackson.annotation.JsonValue;

public enum ProductOrderState {
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

	ProductOrderState(String value) {
		this.value = value;
	}

	@JsonValue
	public String getValue() {
		return value;
	}
}
