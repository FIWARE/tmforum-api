package org.fiware.tmforum.common.test;

/**
 * JUnit tags that gate ITs per storage under test. Failsafe excludes {@code not-on-${broker}}, so a test tagged
 * {@link #NOT_ON_POSTGRESQL} is skipped by {@code -Dbroker=postgresql}.
 */
public final class StorageTags {

	public static final String NOT_ON_POSTGRESQL = "not-on-postgresql";
	public static final String NOT_ON_ORION_LD = "not-on-orion-ld";
	public static final String NOT_ON_SCORPIO = "not-on-scorpio";
	public static final String BROKER_ONLY = "broker-only";

	private StorageTags() {
	}
}
