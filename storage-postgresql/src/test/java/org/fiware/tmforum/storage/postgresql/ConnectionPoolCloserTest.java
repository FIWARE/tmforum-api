package org.fiware.tmforum.storage.postgresql;

import io.micronaut.context.BeanContext;
import io.micronaut.context.event.BeanPreDestroyEvent;
import io.micronaut.inject.BeanDefinition;
import io.r2dbc.spi.Closeable;
import io.r2dbc.spi.ConnectionFactory;
import org.junit.jupiter.api.Named;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

class ConnectionPoolCloserTest {

    private static final Duration TEST_SHUTDOWN_TIMEOUT = Duration.ofMillis(100);
    private static final Duration TEST_DEADLINE = Duration.ofSeconds(5);

    private final ConnectionPoolCloser closer = new ConnectionPoolCloser(TEST_SHUTDOWN_TIMEOUT);

    @Test
    void closeableFactoryIsClosed() {
        ConnectionFactory pool = mock(ConnectionFactory.class, withSettings().extraInterfaces(Closeable.class));
        when(((Closeable) pool).close()).thenReturn(Mono.empty());

        assertSame(pool, closer.onPreDestroy(event(pool)), "The destroyed bean should be handed back.");
        verify((Closeable) pool).close();
    }

    static Stream<Named<Publisher<Void>>> failingCloses() {
        return Stream.of(
                Named.of("close never completes", Mono.never()),
                Named.of("close fails", Mono.error(new IllegalStateException("connection reset"))));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("failingCloses")
    void failingCloseDoesNotBlockShutdown(Publisher<Void> close) {
        ConnectionFactory pool = mock(ConnectionFactory.class, withSettings().extraInterfaces(Closeable.class));
        when(((Closeable) pool).close()).thenAnswer(invocation -> close);

        ConnectionFactory handedBack = assertTimeoutPreemptively(TEST_DEADLINE, () -> closer.onPreDestroy(event(pool)),
                "Closing the pool should give up after the shutdown timeout.");
        assertSame(pool, handedBack, "The destroyed bean should be handed back even if closing failed.");
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
