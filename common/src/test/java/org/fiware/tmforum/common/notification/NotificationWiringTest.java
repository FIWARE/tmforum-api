package org.fiware.tmforum.common.notification;

import io.micronaut.context.ApplicationContext;
import org.fiware.tmforum.common.CommonConstants;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Guards that the NGSI-LD notification path is broker-only while the TMForum one is wired on both backends.
 */
class NotificationWiringTest {

	private static final String[] BROKER = {};
	private static final String[] POSTGRESQL = {CommonConstants.POSTGRESQL_ENVIRONMENT};

	static Stream<Arguments> handlers() {
		return Stream.of(
				Arguments.of(NgsiLdEventHandler.class, BROKER, true),
				Arguments.of(NgsiLdEventHandler.class, POSTGRESQL, false),
				Arguments.of(TMForumEventHandler.class, BROKER, true),
				Arguments.of(TMForumEventHandler.class, POSTGRESQL, true));
	}

	@ParameterizedTest(name = "{0} in {1} -> {2}")
	@MethodSource("handlers")
	void handlerWiring(Class<?> handler, String[] environments, boolean expected) {
		try (ApplicationContext context = ApplicationContext.run(environments)) {
			assertEquals(expected, context.containsBean(handler), "Unexpected wiring of " + handler.getSimpleName());
		}
	}
}
