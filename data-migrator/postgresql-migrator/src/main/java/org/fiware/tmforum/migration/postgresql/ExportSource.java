package org.fiware.tmforum.migration.postgresql;

import java.util.List;

/**
 * Where {@code export} reads entities from; {@link PostgresExportSource} in production, a fake in unit tests.
 */
interface ExportSource {

	/**
	 * One page of the type's current entities, mapped to {@code domainClass}, in a stable order. {@code totalCount} is
	 * the number of the type's current entities.
	 */
	Page page(String type, Class<?> domainClass, int offset, int limit);

	/**
	 * Offsets advance by {@code entities().size()}.
	 */
	record Page(List<Object> entities, long totalCount) {
	}
}
