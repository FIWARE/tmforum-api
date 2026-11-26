package org.fiware.tmforum.storage.postgresql;

import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import io.r2dbc.spi.ConnectionFactory;
import io.r2dbc.spi.Result;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * The {@code options} startup parameter of the datasource puts a {@code statement_timeout} on every session, so a
 * statement no request is waiting for any more cannot hold its connection indefinitely.
 */
@MicronautTest(environments = PostgresStorage.ENVIRONMENT)
class StatementTimeoutIT {

    // the test application-postgresql.yaml binds statement_timeout to 30000 ms, which the server reports as 30s
    private static final String CONFIGURED_TIMEOUT = "30s";
    private static final String READ_TIMEOUT = "SELECT current_setting('statement_timeout') AS timeout";
    private static final String TIMEOUT_COLUMN = "timeout";
    // SET LOCAL only holds for the transaction, so the pooled connection is handed back unchanged
    private static final String SHORT_TIMEOUT = "SET LOCAL statement_timeout = 250";
    // longer than any sane statement, so only the server's own cancellation can end it
    private static final String ENDLESS_STATEMENT = "SELECT pg_sleep(600)";
    // generous enough for a loaded CI machine, far below the statement's own duration
    private static final Duration CANCELLATION_DEADLINE = Duration.ofSeconds(30);

    @Inject
    private ConnectionFactory connectionFactory;

    @Test
    void everySessionCarriesTheConfiguredStatementTimeout() {
        String timeout = PostgresStatements.execute(connectionFactory, READ_TIMEOUT, statement -> statement,
                        result -> result.map(row -> row.get(TIMEOUT_COLUMN, String.class)))
                .blockFirst();

        assertEquals(CONFIGURED_TIMEOUT, timeout, "The configured statement_timeout should apply to the session.");
    }

    @Test
    void theServerCancelsAStatementThatOutlivesTheTimeout() {
        Throwable failure = assertThrows(Throwable.class,
                () -> PostgresStatements.inTransaction(connectionFactory, connection ->
                                PostgresStatements.execute(connection, SHORT_TIMEOUT, statement -> statement,
                                                Result::getRowsUpdated)
                                        .thenMany(PostgresStatements.execute(connection, ENDLESS_STATEMENT,
                                                statement -> statement, Result::getRowsUpdated))
                                        .then())
                        .block(CANCELLATION_DEADLINE),
                "A statement over the timeout should be cancelled by the server.");

        assertNotNull(failure.getMessage(), "The cancellation should be reported with a message.");
    }
}
