package org.fiware.tmforum.storage.postgresql;

import io.micronaut.context.ApplicationContext;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.Map;
import java.util.stream.Stream;

import static org.fiware.tmforum.storage.postgresql.OutboxProperties.ENABLED_PROPERTY;
import static org.junit.jupiter.api.Assertions.assertEquals;

class OutboxDispatcherSwitchTest {

    // a location without config files, so the test application-postgresql.yaml (switch off) is not loaded
    private static final String NO_CONFIG_FILES = "classpath:outbox-switch-test/";

    static Stream<Arguments> switches() {
        return Stream.of(
                Arguments.of(Map.of(), true),
                Arguments.of(Map.of(ENABLED_PROPERTY, "true"), true),
                Arguments.of(Map.of(ENABLED_PROPERTY, "false"), false));
    }

    @ParameterizedTest
    @MethodSource("switches")
    void dispatcherIsOnByDefaultAndOffOnlyWhenDisabled(Map<String, Object> properties, boolean expectedPresent) {
        try (ApplicationContext context = ApplicationContext.builder(PostgresStorage.ENVIRONMENT)
                .overrideConfigLocations(NO_CONFIG_FILES)
                .properties(properties)
                .start()) {
            assertEquals(expectedPresent, context.findBeanDefinition(OutboxDispatcher.class).isPresent(),
                    "The dispatcher should be present unless tmforum.outbox.enabled is false.");
        }
    }
}
