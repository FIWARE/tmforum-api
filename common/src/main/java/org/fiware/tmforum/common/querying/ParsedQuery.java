package org.fiware.tmforum.common.querying;

import org.fiware.tmforum.storage.query.QueryFilter;

/**
 * Front-half output of a TMForum filter. {@code ids} and {@code types} are the comma-joined values of the
 * {@code id}/{@code type} shortcuts, or null; {@code filter} is null when nothing else is filtered.
 */
public record ParsedQuery(String ids, String types, QueryFilter filter) {
}
