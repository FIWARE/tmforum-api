package org.fiware.tmforum.storage.query;

import java.util.List;
import java.util.Objects;

/**
 * Filter and sort of a list request. {@code queryClass} is the class the attribute paths were resolved against;
 * {@code filter} is null for no filter, and {@code sort} is empty for no sort.
 */
public record EntityQuery(Class<?> queryClass, QueryFilter filter, List<SortKey> sort) {

    public EntityQuery {
        Objects.requireNonNull(queryClass, "queryClass");
        sort = List.copyOf(Objects.requireNonNull(sort, "sort"));
    }
}
