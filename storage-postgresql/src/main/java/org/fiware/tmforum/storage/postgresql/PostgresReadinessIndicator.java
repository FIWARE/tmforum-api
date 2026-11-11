package org.fiware.tmforum.storage.postgresql;

import io.micronaut.context.annotation.Requires;
import io.micronaut.context.annotation.Value;
import io.micronaut.health.HealthStatus;
import io.micronaut.management.endpoint.health.HealthEndpoint;
import io.micronaut.management.health.indicator.HealthIndicator;
import io.micronaut.management.health.indicator.HealthResult;
import io.micronaut.management.health.indicator.annotation.Readiness;
import io.r2dbc.spi.ConnectionFactory;
import jakarta.inject.Singleton;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.function.Function;

/**
 * Readiness of a PostgreSQL deployment: UP when {@code SELECT 1} answers within
 * {@value #TIMEOUT_PROPERTY} (default {@value #DEFAULT_TIMEOUT}), DOWN otherwise.
 * micronaut-r2dbc's own indicator is neither a readiness check nor bounded in time.
 */
@Singleton
@Readiness
@Requires(env = PostgresStorage.ENVIRONMENT)
@Requires(beans = HealthEndpoint.class)
public class PostgresReadinessIndicator implements HealthIndicator {

    static final String NAME = "postgresql";
    static final String HEALTH_QUERY = "SELECT 1";
    static final String TIMEOUT_PROPERTY = "endpoints.health.postgresql.timeout";
    static final String DEFAULT_TIMEOUT = "5s";

    private final ConnectionFactory connectionFactory;
    private final Duration timeout;

    public PostgresReadinessIndicator(ConnectionFactory connectionFactory,
                                      @Value("${" + TIMEOUT_PROPERTY + ":" + DEFAULT_TIMEOUT + "}") Duration timeout) {
        this.connectionFactory = connectionFactory;
        this.timeout = timeout;
    }

    @Override
    public Publisher<HealthResult> getResult() {
        return PostgresStatements.execute(connectionFactory, HEALTH_QUERY, Function.identity(),
                        result -> result.map(row -> row))
                .then(Mono.fromCallable(() -> HealthResult.builder(NAME).status(HealthStatus.UP).build()))
                .timeout(timeout)
                .onErrorResume(e -> Mono.just(HealthResult.builder(NAME).status(HealthStatus.DOWN).exception(e).build()));
    }
}
