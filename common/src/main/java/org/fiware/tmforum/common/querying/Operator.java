package org.fiware.tmforum.common.querying;

import lombok.Getter;
import org.fiware.tmforum.storage.query.ComparisonOperator;

import java.util.Arrays;

public enum Operator {
	GREATER_THAN(new TMForumOperator(".gt", ">"), ">", ComparisonOperator.GREATER_THAN),
	GREATER_THAN_EQUALS(new TMForumOperator(".gte", ">="), ">=", ComparisonOperator.GREATER_THAN_EQUALS),
	LESS_THAN(new TMForumOperator(".lt", "<"), "<", ComparisonOperator.LESS_THAN),
	LESS_THAN_EQUALS(new TMForumOperator(".lte", "<="), "<=", ComparisonOperator.LESS_THAN_EQUALS),
	EQUALS(new TMForumOperator(".eq", "="), "==", ComparisonOperator.EQUALS),
	// NGSI-LD's native pattern-match operator (ETSI GS CIM 009, Simple Query Language)
	REGEX(new TMForumOperator(".regex", "*="), "~=", ComparisonOperator.REGEX);

	@Getter
	private final TMForumOperator tmForumOperator;
	@Getter
	private final String ngsiLdOperator;
	@Getter
	private final ComparisonOperator comparisonOperator;

	Operator(TMForumOperator tmForumOperator, String ngsiLdOperator, ComparisonOperator comparisonOperator) {
		this.tmForumOperator = tmForumOperator;
		this.ngsiLdOperator = ngsiLdOperator;
		this.comparisonOperator = comparisonOperator;
	}

	public static Operator of(ComparisonOperator comparisonOperator) {
		return Arrays.stream(values())
				.filter(operator -> operator.comparisonOperator == comparisonOperator)
				.findFirst()
				.orElseThrow(() -> new IllegalArgumentException("No operator for " + comparisonOperator));
	}

	static Operator ofNgsiLdOperator(String ngsiLdOperator) {
		return Arrays.stream(values())
				.filter(operator -> operator.ngsiLdOperator.equals(ngsiLdOperator))
				.findFirst()
				.orElseThrow(() -> new IllegalArgumentException("No operator for " + ngsiLdOperator));
	}
}
