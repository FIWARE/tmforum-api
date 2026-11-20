package org.fiware.tmforum.migration.postgresql;

/**
 * Switches of the {@code import} command.
 *
 * @param resume   accept a non-empty target and count an id that is already stored as skipped
 * @param failFast stop at the first failure instead of finishing the run
 */
record ImportOptions(int pageSize, boolean resume, boolean failFast, boolean dryRun) {

	ImportOptions {
		if (pageSize < 1) {
			throw new IllegalArgumentException("The page size must be positive, was " + pageSize + ".");
		}
	}
}
