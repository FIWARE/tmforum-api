package org.fiware.tmforum.common.configuration;

import io.micronaut.context.ApplicationContext;
import io.micronaut.context.env.PropertySource;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.Map;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests that the broker-only {@code general.*} keys still bind after moving out of {@link GeneralProperties}.
 */
class NgsiLdBrokerPropertiesTest {

	private static final String TENANT = "t1";
	private static final String BASEPATH = "/tmf";
	private static final String SOURCE_NAME = "test";

	static Stream<PropertySource> configurations() {
		return Stream.of(
				PropertySource.of(SOURCE_NAME, Map.of("general.replace-on-update", true, "general.tenant", TENANT,
						"general.basepath", BASEPATH)),
				PropertySource.of(SOURCE_NAME, Map.of("general.replaceOnUpdate", true, "general.tenant", TENANT,
						"general.basepath", BASEPATH)),
				PropertySource.of(SOURCE_NAME, Map.of("GENERAL_REPLACE_ON_UPDATE", "true", "GENERAL_TENANT", TENANT,
						"GENERAL_BASEPATH", BASEPATH), PropertySource.PropertyConvention.ENVIRONMENT_VARIABLE));
	}

	@DisplayName("The broker keys bind to NgsiLdBrokerProperties, the shared keys still to GeneralProperties.")
	@ParameterizedTest
	@MethodSource("configurations")
	void brokerKeysAreBound(PropertySource configuration) {
		try (ApplicationContext context = ApplicationContext.builder().propertySources(configuration).start()) {
			NgsiLdBrokerProperties brokerProperties = context.getBean(NgsiLdBrokerProperties.class);

			assertTrue(brokerProperties.isReplaceOnUpdate(), "general.replace-on-update should be bound.");
			assertEquals(TENANT, brokerProperties.getTenant(), "general.tenant should be bound.");
			assertEquals(BASEPATH, context.getBean(GeneralProperties.class).getBasepath(),
					"general.basepath should still be bound to GeneralProperties.");
		}
	}
}
