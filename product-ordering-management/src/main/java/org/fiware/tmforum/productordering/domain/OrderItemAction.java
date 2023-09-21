package org.fiware.tmforum.productordering.domain;

import com.fasterxml.jackson.annotation.JsonValue;

public enum OrderItemAction {
	ADD("add"),
	MODIFY("modify"),
	DELETE("delete"),
	NO_CHANGE("noChange");

	private final String value;

	OrderItemAction(String value) {
		this.value = value;
	}

	@JsonValue
	public String getValue() {
		return value;
	}
}
