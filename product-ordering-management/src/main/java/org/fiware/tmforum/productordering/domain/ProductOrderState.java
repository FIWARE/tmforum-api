package org.fiware.tmforum.productordering.domain;

import lombok.Getter;

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

	@Getter
	private final String value;

	ProductOrderState(String value) {
		this.value = value;
	}
}
