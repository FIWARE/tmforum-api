package org.fiware.tmforum.storage.postgresql;

import org.fiware.tmforum.storage.query.ComparisonOperator;
import org.fiware.tmforum.storage.query.EntityQuery;
import org.fiware.tmforum.storage.query.QueryFilter;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * The distinct {@code REGEX} patterns of a query's filter. {@code ~} only compiles a pattern once a row reaches it, so
 * the repository compiles them up front to reject an invalid one whatever the data.
 */
final class RegexPatterns {

    private RegexPatterns() {
    }

    /**
     * In filter order; empty for a null query or one without a filter.
     */
    static Set<String> of(EntityQuery query) {
        if (query == null || query.filter() == null) {
            return Set.of();
        }
        Set<String> patterns = new LinkedHashSet<>();
        collect(query.filter(), patterns);
        return Collections.unmodifiableSet(patterns);
    }

    private static void collect(QueryFilter filter, Set<String> patterns) {
        switch (filter) {
            case QueryFilter.Comparison comparison -> {
                if (comparison.operator() == ComparisonOperator.REGEX) {
                    patterns.add(comparison.values().getFirst());
                }
            }
            case QueryFilter.AllOf allOf -> allOf.filters().forEach(child -> collect(child, patterns));
            case QueryFilter.AnyOf anyOf -> anyOf.filters().forEach(child -> collect(child, patterns));
        }
    }
}
