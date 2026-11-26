package org.fiware.tmforum.accesspolicy;

import java.util.Map;

/**
 * Cache configuration an enabled test context needs, since {@link DecisionCache} fails the startup without it.
 */
final class TestCaches {

	static final int DECISIONS_MAXIMUM_SIZE = 1000;
	static final Map<String, Object> DECISIONS = Map.of(
			"micronaut.caches." + DecisionCache.CACHE_NAME + ".maximum-size", DECISIONS_MAXIMUM_SIZE);

	private TestCaches() {
	}
}
