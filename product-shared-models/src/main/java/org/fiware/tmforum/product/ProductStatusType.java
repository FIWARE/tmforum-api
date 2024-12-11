package org.fiware.tmforum.product;

import com.fasterxml.jackson.annotation.JsonValue;

public enum ProductStatusType {

	CREATED("created"),
	PENDING_ACTIVE("pendingActive"),
	CANCELLED("cancelled"),
	ACTIVE("active"),
	PENDING_TERMINATE("pendingTerminate"),
	TERMINATED("terminated"),
	SUSPENDED("suspended"),
	ABORTED_("aborted ");

	private final String value;

	ProductStatusType(String value) {
		this.value = value;
	}

	@JsonValue
	public String getValue() {
		return value;
	}
}
