package org.fiware.tmforum.common.querying;

/**
 * A single raw TMForum query token paired with the {@link LogicalOperator} that connected it to
 * the previous token in the original left-to-right query string. The first token in a query has
 * no predecessor; its connector is conventionally {@link LogicalOperator#AND} but must not be
 * consulted by callers.
 */
public record ConnectedQueryPart(String rawParameter, LogicalOperator connectorToPrevious) {
}
