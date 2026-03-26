package org.fiware.tmforum.resourceordering.domain;

import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Actions that can be performed on an order item.
 */
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
