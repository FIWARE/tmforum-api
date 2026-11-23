package org.fiware.tmforum.migration.postgresql;

/**
 * The migrated hubs in PostgreSQL; {@link PostgresStoredHubs} in production, a fake in unit tests.
 */
interface StoredHubs {

	/**
	 * Removes the {@code subscription} key from every stored hub, or on a dry run only counts the rows holding one.
	 * Throws on a failure, after which nothing was written.
	 */
	Outcome dropSubscriptions(boolean dryRun);

	/**
	 * {@code rowsUpdated} is 0 on a dry run.
	 */
	record Outcome(long rowsWithSubscription, long rowsUpdated) {
	}
}
