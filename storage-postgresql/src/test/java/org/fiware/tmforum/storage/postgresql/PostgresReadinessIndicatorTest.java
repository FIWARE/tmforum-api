package org.fiware.tmforum.storage.postgresql;

import io.micronaut.health.HealthStatus;
import io.micronaut.management.health.indicator.HealthResult;
import io.r2dbc.spi.Connection;
import io.r2dbc.spi.ConnectionFactory;
import io.r2dbc.spi.Result;
import io.r2dbc.spi.Statement;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.function.Function;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

class PostgresReadinessIndicatorTest {

    private static final Duration TIMEOUT = Duration.ofMillis(200);

    private static Stream<Arguments> databaseStates() {
        return Stream.of(
                Arguments.of("query answers", Mono.just(1), HealthStatus.UP),
                Arguments.of("query fails", Mono.error(new IllegalStateException("broken")), HealthStatus.DOWN),
                Arguments.of("query hangs past the timeout", Mono.never(), HealthStatus.DOWN));
    }

    @ParameterizedTest(name = "{0} -> {2}")
    @MethodSource("databaseStates")
    void reportsDatabaseReadiness(String description, Publisher<Integer> rows, HealthStatus expectedStatus) {
        Result result = mock(Result.class);
        doReturn(rows).when(result).map(any(Function.class));
        Statement statement = mock(Statement.class);
        doReturn(Mono.just(result)).when(statement).execute();
        Connection connection = mock(Connection.class);
        doReturn(statement).when(connection).createStatement(PostgresReadinessIndicator.HEALTH_QUERY);
        doReturn(Mono.empty()).when(connection).close();
        ConnectionFactory connectionFactory = mock(ConnectionFactory.class);
        doReturn(Mono.just(connection)).when(connectionFactory).create();

        HealthResult health = Mono.from(new PostgresReadinessIndicator(connectionFactory, TIMEOUT).getResult()).block();

        assertEquals(PostgresReadinessIndicator.NAME, health.getName());
        assertEquals(expectedStatus, health.getStatus(), description);
    }

    @ParameterizedTest(name = "unreachable database -> DOWN")
    @MethodSource("unreachableDatabases")
    void reportsUnreachableDatabaseAsDown(Publisher<Connection> connections) {
        ConnectionFactory connectionFactory = mock(ConnectionFactory.class);
        doReturn(connections).when(connectionFactory).create();

        HealthResult health = Mono.from(new PostgresReadinessIndicator(connectionFactory, TIMEOUT).getResult()).block();

        assertEquals(HealthStatus.DOWN, health.getStatus());
    }

    private static Stream<Arguments> unreachableDatabases() {
        return Stream.of(
                Arguments.of(Mono.error(new IllegalStateException("connection refused"))),
                Arguments.of(Flux.never()));
    }
}
