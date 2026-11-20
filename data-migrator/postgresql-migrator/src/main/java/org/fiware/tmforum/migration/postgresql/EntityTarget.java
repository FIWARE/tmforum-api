package org.fiware.tmforum.migration.postgresql;

import java.time.Instant;

/**
 * Where the import writes entities to; {@link PostgresTarget} in production, a fake in unit tests.
 */
interface EntityTarget {

	/**
	 * Rows of the given type, current or not.
	 */
	long count(String type);

	/**
	 * Inserts the entity with the source's timestamps and no event. Throws a
	 * {@link org.fiware.tmforum.storage.StorageException} with reason {@code CONFLICT} when the id is taken.
	 */
	void importEntity(Object entity, Instant createdAt, Instant updatedAt);
}
