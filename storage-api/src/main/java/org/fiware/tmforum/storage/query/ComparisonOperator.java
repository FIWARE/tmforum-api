package org.fiware.tmforum.storage.query;

/**
 * Operator of a {@link QueryFilter.Comparison}. TMForum has no not-equals parameter; clients write it as the regex
 * {@code ^(?!X$).*$}, which {@code QueryParser} normalizes into {@link #NOT_EQUALS} when X is a literal, so that
 * every backend reads it alike instead of each interpreting the pattern with its own regex engine.
 */
public enum ComparisonOperator {
    EQUALS,
    NOT_EQUALS,
    GREATER_THAN,
    GREATER_THAN_EQUALS,
    LESS_THAN,
    LESS_THAN_EQUALS,
    REGEX
}
