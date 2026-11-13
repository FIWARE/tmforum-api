package org.fiware.tmforum.storage.query;

/**
 * Operator of a {@link QueryFilter.Comparison}. There is no not-equals: TMForum expresses it as a regex.
 */
public enum ComparisonOperator {
    EQUALS,
    GREATER_THAN,
    GREATER_THAN_EQUALS,
    LESS_THAN,
    LESS_THAN_EQUALS,
    REGEX
}
