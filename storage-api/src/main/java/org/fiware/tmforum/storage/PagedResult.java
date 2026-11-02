package org.fiware.tmforum.storage;

import java.util.List;

/**
 * A page of entities together with the offset/limit that produced it and the total number of entities matching
 * the query. {@code totalCount} is null when the backend cannot report it.
 */
public record PagedResult<T>(List<T> items, Integer offset, Integer limit, Integer totalCount) {
}
