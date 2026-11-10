package org.fiware.tmforum.storage.postgresql;

import io.micronaut.context.annotation.Requires;
import io.micronaut.context.event.BeanPreDestroyEvent;
import io.micronaut.context.event.BeanPreDestroyEventListener;
import io.r2dbc.spi.Closeable;
import io.r2dbc.spi.ConnectionFactory;
import jakarta.inject.Singleton;
import reactor.core.publisher.Mono;

/**
 * Closes the R2DBC connection pool on shutdown. Micronaut only closes {@link AutoCloseable} beans, and the pool is an
 * R2DBC {@link Closeable}, so without this its connections stay open after the context stops.
 */
@Singleton
@Requires(env = PostgresStorage.ENVIRONMENT)
public class ConnectionPoolCloser implements BeanPreDestroyEventListener<ConnectionFactory> {

    @Override
    public ConnectionFactory onPreDestroy(BeanPreDestroyEvent<ConnectionFactory> event) {
        ConnectionFactory connectionFactory = event.getBean();
        if (connectionFactory instanceof Closeable closeable) {
            Mono.from(closeable.close()).block();
        }
        return connectionFactory;
    }
}
