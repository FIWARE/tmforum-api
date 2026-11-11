package org.fiware.tmforum.common.test;

/**
 * JUnit tags that gate ITs per storage under test. Failsafe excludes {@code not-on-${broker}}, so a test tagged
 * {@link #NOT_ON_POSTGRESQL} is skipped by {@code -Dbroker=postgresql}. The ticket tags only document what has to land.
 */
public final class StorageTags {

	public static final String NOT_ON_POSTGRESQL = "not-on-postgresql";
	public static final String NOT_ON_ORION_LD = "not-on-orion-ld";
	public static final String NOT_ON_SCORPIO = "not-on-scorpio";
	public static final String BROKER_ONLY = "broker-only";
	public static final String TICKET_1 = "ticket-1";
	public static final String TICKET_7 = "ticket-7";

	private StorageTags() {
	}
}
