package org.fiware.tmforum.serviceordering.domain;

import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Actions that can be performed on a service order item.
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

	/**
	 * Returns the JSON-serialized value of this action.
	 *
	 * @return the string value used for JSON serialization
	 */
	@JsonValue
	public String getValue() {
		return value;
	}
}
