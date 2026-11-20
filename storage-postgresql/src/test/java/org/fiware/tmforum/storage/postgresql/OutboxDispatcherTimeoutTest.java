package org.fiware.tmforum.storage.postgresql;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import io.r2dbc.spi.ConnectionFactory;
import org.fiware.tmforum.common.notification.EventMapper;
import org.fiware.tmforum.common.notification.EventTypes;
import org.fiware.tmforum.common.notification.TMForumEventHandler;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

class OutboxDispatcherTimeoutTest {

    private static final Duration SHORT_LEASE = Duration.ofMillis(200);
    private static final Duration TEST_BOUND = Duration.ofSeconds(10);
    private static final String TYPE = "catalog";

    static Stream<Arguments> scheduledRuns() {
        return Stream.of(
                Arguments.of("poll", (Consumer<OutboxDispatcher>) OutboxDispatcher::poll),
                Arguments.of("housekeeping", (Consumer<OutboxDispatcher>) OutboxDispatcher::scheduledHousekeeping));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("scheduledRuns")
    void hungRunEndsAfterTheLease(String name, Consumer<OutboxDispatcher> run) {
        ConnectionFactory hungDatabase = mock(ConnectionFactory.class);
        doReturn(Mono.never()).when(hungDatabase).create();
        OutboxDispatcher dispatcher = new OutboxDispatcher(hungDatabase, mock(PostgresDocumentMapper.class),
                new EventTypes(Set.of(TYPE)), mock(EventMapper.class), mock(TMForumEventHandler.class),
                withLease(SHORT_LEASE), new SimpleMeterRegistry());

        assertTimeoutPreemptively(TEST_BOUND, () -> run.accept(dispatcher),
                "A scheduled run on a hung database should end once the lease passed.");
    }

    private static OutboxProperties withLease(Duration lease) {
        OutboxProperties defaults = OutboxTestSupport.PROPERTIES;
        return new OutboxProperties(defaults.enabled(), defaults.pollInterval(), defaults.batchSize(), lease,
                defaults.maxBackoff(), defaults.retention(), defaults.housekeepingInterval());
    }
}
