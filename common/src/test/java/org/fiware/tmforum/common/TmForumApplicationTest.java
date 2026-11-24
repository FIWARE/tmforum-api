package org.fiware.tmforum.common;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Tests when the runner adds PostgreSQL as the default backend; the default is part of the documented configuration.
 */
class TmForumApplicationTest {

	@ParameterizedTest(name = "property: [{0}], variable: [{1}] -> default: {2}")
	@CsvSource(delimiter = ';', value = {
			";;true",
			"'';'';true",
			"'  ';;true",
			"in-memory;;true",
			";redis;true",
			"in-memory;redis;true",
			"postgresql-ha;;true",
			"postgresql;;false",
			";orion-ld;false",
			";scorpio;false",
			"'redis , scorpio ';;false",
			"in-memory;redis,postgresql;false",
			"redis;' orion-ld,in-memory';false",
			"orion-ld;postgresql;false"
	})
	void defaultIsAddedOnlyWithoutNamedBackend(String environmentsProperty, String environmentsVariable,
			boolean expectedDefault) {
		assertEquals(expectedDefault,
				TmForumApplication.needsDefaultBackend(environmentsProperty, environmentsVariable),
				"PostgreSQL should be added exactly when no backend is named.");
	}
}
