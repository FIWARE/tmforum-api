package org.fiware.tmforum.migration.postgresql;

/**
 * Switches of the {@code verify} command.
 *
 * @param fullCheckThreshold a type with at most this many entities is diffed completely, a larger one by sample
 * @param sourceEscaped      whether the broker's hub queries are URL-encoded, as {@code --source-escaped}
 */
record VerifyOptions(int pageSize, int sampleSize, long seed, int fullCheckThreshold, boolean sourceEscaped) {

	VerifyOptions {
		if (pageSize < 1) {
			throw new IllegalArgumentException("The page size must be positive, was " + pageSize + ".");
		}
		if (sampleSize < 0) {
			throw new IllegalArgumentException("The sample size must not be negative, was " + sampleSize + ".");
		}
		if (fullCheckThreshold < 0) {
			throw new IllegalArgumentException(
					"The full-check threshold must not be negative, was " + fullCheckThreshold + ".");
		}
	}
}
