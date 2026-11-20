package org.fiware.tmforum.storage.postgresql;

import io.micronaut.context.annotation.Requires;
import io.micronaut.context.event.BeanPreDestroyEvent;
import io.micronaut.context.event.BeanPreDestroyEventListener;
import io.r2dbc.spi.Closeable;
import io.r2dbc.spi.ConnectionFactory;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;

import java.time.Duration;

/**
 * Closes the R2DBC connection pool on shutdown. Micronaut only closes {@link AutoCloseable} beans, and the pool is an
 * R2DBC {@link Closeable}, so without this its connections stay open after the context stops.
 * Closing is bounded by {@link #SHUTDOWN_TIMEOUT}; a failure is logged, never thrown, so shutdown cannot hang on it.
 */
@Singleton
@Requires(env = PostgresStorage.ENVIRONMENT)
public class ConnectionPoolCloser implements BeanPreDestroyEventListener<ConnectionFactory> {

    static final Duration SHUTDOWN_TIMEOUT = Duration.ofSeconds(10);

    private static final Logger LOGGER = LoggerFactory.getLogger(ConnectionPoolCloser.class);

    private final Duration shutdownTimeout;

    @Inject
    public ConnectionPoolCloser() {
        this(SHUTDOWN_TIMEOUT);
    }

    ConnectionPoolCloser(Duration shutdownTimeout) {
        this.shutdownTimeout = shutdownTimeout;
    }

    @Override
    public ConnectionFactory onPreDestroy(BeanPreDestroyEvent<ConnectionFactory> event) {
        ConnectionFactory connectionFactory = event.getBean();
        if (connectionFactory instanceof Closeable closeable) {
            try {
                Mono.from(closeable.close()).timeout(shutdownTimeout).block();
            } catch (RuntimeException e) {
                LOGGER.warn("Could not close the R2DBC connection pool within {}.", shutdownTimeout, e);
            }
        }
        return connectionFactory;
    }
}
