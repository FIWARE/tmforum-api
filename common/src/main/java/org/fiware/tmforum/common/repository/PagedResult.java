package org.fiware.tmforum.common.repository;

import java.util.List;

/**
 * A page of entities together with the offset/limit that produced it and the total number of
 * entities matching the query, when the target broker reports it (see
 * {@link org.fiware.tmforum.common.configuration.GeneralProperties#getCountHeader()}).
 * {@code totalCount} is null when the broker profile has no count header configured, or the broker
 * didn't send a parsable value for it.
 */
public record PagedResult<T>(List<T> items, Integer offset, Integer limit, Integer totalCount) {
}
