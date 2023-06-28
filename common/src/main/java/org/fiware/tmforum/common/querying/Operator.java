package org.fiware.tmforum.common.querying;

import lombok.Getter;

public enum Operator {
	GREATER_THAN(new TMForumOperator(".gt", ">"), ">"),
	GREATER_THAN_EQUALS(new TMForumOperator(".gte", ">="), ">="),
	LESS_THAN(new TMForumOperator(".lt", "<"), "<"),
	LESS_THAN_EQUALS(new TMForumOperator(".lte", "<="), "<="),
	EQUALS(new TMForumOperator(".eq", "="), "=="),
	// NGSI-LD allows regex for every string, so no specific operator exists, it's just an equals
	REGEX(new TMForumOperator(".regex", "*="), "==");

	@Getter
	private final TMForumOperator tmForumOperator;
	@Getter
	private final String ngsiLdOperator;

	Operator(TMForumOperator tmForumOperator, String ngsiLdOperator) {
		this.tmForumOperator = tmForumOperator;
		this.ngsiLdOperator = ngsiLdOperator;
	}
}
