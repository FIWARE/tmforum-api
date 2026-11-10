package org.fiware.tmforum.storage.postgresql;

import io.micronaut.context.BeanContext;
import io.micronaut.context.event.BeanPreDestroyEvent;
import io.micronaut.inject.BeanDefinition;
import io.r2dbc.spi.Closeable;
import io.r2dbc.spi.ConnectionFactory;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

class ConnectionPoolCloserTest {

    private final ConnectionPoolCloser closer = new ConnectionPoolCloser();

    @Test
    void closeableFactoryIsClosed() {
        ConnectionFactory pool = mock(ConnectionFactory.class, withSettings().extraInterfaces(Closeable.class));
        when(((Closeable) pool).close()).thenReturn(Mono.empty());

        assertSame(pool, closer.onPreDestroy(event(pool)), "The destroyed bean should be handed back.");
        verify((Closeable) pool).close();
    }

    @Test
    void plainFactoryIsHandedBack() {
        ConnectionFactory factory = mock(ConnectionFactory.class);

        assertSame(factory, closer.onPreDestroy(event(factory)), "A factory that cannot close should pass through.");
    }

    @SuppressWarnings("unchecked")
    private static BeanPreDestroyEvent<ConnectionFactory> event(ConnectionFactory factory) {
        return new BeanPreDestroyEvent<>(mock(BeanContext.class), mock(BeanDefinition.class), factory);
    }
}
