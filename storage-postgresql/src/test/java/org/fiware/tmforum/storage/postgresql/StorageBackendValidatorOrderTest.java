package org.fiware.tmforum.storage.postgresql;

import io.micronaut.context.ApplicationContext;
import io.micronaut.context.exceptions.ConfigurationException;
import org.fiware.tmforum.common.TmForumApplication;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * The backend validator must abort the start before Flyway tries to reach PostgreSQL.
 */
class StorageBackendValidatorOrderTest {

    private static final String NGSI_URL_PROPERTY = "micronaut.http.services.ngsi.url";
    private static final String BROKER_URL = "http://orion-ld:1026";
    private static final String FLYWAY_URL_PROPERTY = "flyway.datasources.default.url";
    // nothing listens on port 1, so a Flyway migration that runs first fails with a connection error
    private static final String UNREACHABLE_FLYWAY_URL = "jdbc:postgresql://localhost:1/tmforum";

    @Test
    void unnamedBrokerFailsWithTheValidatorMessageNotAFlywayError() {
        Exception exception = assertThrows(Exception.class, () -> ApplicationContext.builder()
                .deduceEnvironment(false)
                .deduceCloudEnvironment(false)
                .environments(PostgresStorage.ENVIRONMENT)
                .properties(Map.of(
                        TmForumApplication.BACKEND_DEFAULTED_PROPERTY, true,
                        NGSI_URL_PROPERTY, BROKER_URL,
                        FLYWAY_URL_PROPERTY, UNREACHABLE_FLYWAY_URL))
                .start()
                .close(), "The upgrade case must not start.");
        assertInstanceOf(ConfigurationException.class, rootCause(exception),
                "The validator, not Flyway, should abort the start.");
    }

    private static Throwable rootCause(Throwable throwable) {
        Throwable cause = throwable;
        while (cause.getCause() != null && cause.getCause() != cause) {
            cause = cause.getCause();
        }
        return cause;
    }
}
