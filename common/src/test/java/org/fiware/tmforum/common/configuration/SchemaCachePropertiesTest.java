package org.fiware.tmforum.common.configuration;

import io.micronaut.context.ApplicationContext;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.time.Duration;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests the binding of the schema cache configuration, since the property names and the supported duration
 * formats are part of the documented configuration of the applications.
 */
class SchemaCachePropertiesTest {

	@DisplayName("The defaults are applied if nothing is configured.")
	@Test
	void defaultsAreApplied() {
		try (ApplicationContext context = ApplicationContext.run()) {
			SchemaCacheProperties properties = context.getBean(SchemaCacheProperties.class);

			assertTrue(properties.isEnabled(), "Caching should be enabled by default.");
			assertEquals(Duration.ofMinutes(30), properties.getTimeToLive(),
					"Schemas should be cached for 30 minutes by default.");
			assertEquals(100L, properties.getMaxSize(), "100 schemas should be cached by default.");
		}
	}

	@DisplayName("The configured values are bound, independent of the duration format.")
	@ParameterizedTest
	@CsvSource({
			"5m, PT5M",
			"30s, PT30S",
			"1h, PT1H",
			"PT15M, PT15M",
			"0s, PT0S"
	})
	void configuredValuesAreBound(String configuredTimeToLive, Duration expectedTimeToLive) {
		try (ApplicationContext context = ApplicationContext.run(
				Map.of("general.schema-cache.enabled", false,
						"general.schema-cache.time-to-live", configuredTimeToLive,
						"general.schema-cache.max-size", 7))) {
			SchemaCacheProperties properties = context.getBean(SchemaCacheProperties.class);

			assertEquals(false, properties.isEnabled(), "The configured state should be bound.");
			assertEquals(expectedTimeToLive, properties.getTimeToLive(),
					"The configured time-to-live should be bound.");
			assertEquals(7L, properties.getMaxSize(), "The configured maximum size should be bound.");
		}
	}
}
