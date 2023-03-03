package org.fiware.tmforum.productordering.domain;

public enum ProductOrderItemState {

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

	ProductOrderItemState(String value) {
		this.value = value;
	}
}
