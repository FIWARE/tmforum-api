package org.fiware.tmforum.common.mapping;

import io.github.wistefan.mapping.MappingProperties;
import io.micronaut.context.ApplicationContext;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * The write path accepts references this deployment cannot resolve, so the read path has to tolerate them. The
 * library's default would fail the read, also for a deployment that ships its own application.yaml.
 */
class StrictRelationshipsDefaultTest {

	@DisplayName("Relationships are non-strict if nothing is configured.")
	@Test
	void nonStrictByDefault() {
		try (ApplicationContext context = ApplicationContext.run()) {
			assertFalse(context.getBean(MappingProperties.class).isStrictRelationships(),
					"Unresolvable relationship targets should be tolerated by default.");
		}
	}

	@DisplayName("A configured value is kept, in either spelling.")
	@ParameterizedTest
	@CsvSource({
			"mapping.strict-relationships, true, true",
			"mapping.strict-relationships, false, false",
			"mapping.strictRelationships, true, true",
			"mapping.strictRelationships, false, false"
	})
	void configuredValueIsKept(String property, boolean configured, boolean expected) {
		try (ApplicationContext context = ApplicationContext.run(Map.of(property, configured))) {
			assertEquals(expected, context.getBean(MappingProperties.class).isStrictRelationships(),
					"The configured value should not be overwritten.");
		}
	}
}
