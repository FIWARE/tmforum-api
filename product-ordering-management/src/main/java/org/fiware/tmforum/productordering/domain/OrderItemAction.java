package org.fiware.tmforum.productordering.domain;

import lombok.Getter;

public enum OrderItemAction {
	ADD("add"),
	MODIFY("modify"),
	DELETE("delete"),
	NO_CHANGE("noChange");

	@Getter
	private final String value;

	OrderItemAction(String value) {
		this.value = value;
	}
}
