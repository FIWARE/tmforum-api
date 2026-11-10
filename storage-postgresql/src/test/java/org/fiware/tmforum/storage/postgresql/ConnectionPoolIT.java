package org.fiware.tmforum.storage.postgresql;

import io.micronaut.context.ApplicationContext;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import io.r2dbc.pool.ConnectionPool;
import io.r2dbc.pool.PoolMetrics;
import io.r2dbc.spi.ConnectionFactory;
import jakarta.inject.Inject;
import org.fiware.tmforum.storage.postgresql.DocumentTestEntities.TestCatalog;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import reactor.core.publisher.Flux;

import java.net.URI;
import java.time.Duration;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The configured {@code r2dbc:pool:} URL puts a bounded pool in front of the driver, and the repository reuses it.
 */
@MicronautTest(environments = PostgresStorage.ENVIRONMENT)
class ConnectionPoolIT {

    // initial-size and max-size of the test application-postgresql.yaml, both unlike r2dbc-pool's default of 10
    private static final int CONFIGURED_INITIAL_SIZE = 2;
    private static final int CONFIGURED_MAX_SIZE = 4;
    private static final int CONCURRENT_READS = CONFIGURED_MAX_SIZE * 3;
    // a get of an existing entity releases its connection asynchronously, on cancel
    private static final Duration RELEASE_TIMEOUT = Duration.ofSeconds(5);
    private static final String STORED_ID = "urn:ngsi-ld:" + TestCatalog.TYPE + ":pooled";
    private static final String MISSING_ID = "urn:ngsi-ld:" + TestCatalog.TYPE + ":missing";

    @Inject
    private ConnectionFactory connectionFactory;

    @Inject
    private PostgresEntityRepository repository;

    @Test
    void freshPoolAppliesTheConfiguredSizesAndClosesWithTheContext() {
        ConnectionPool pool;
        try (ApplicationContext context = ApplicationContext.run(PostgresStorage.ENVIRONMENT)) {
            pool = assertInstanceOf(ConnectionPool.class, context.getBean(ConnectionFactory.class),
                    "The r2dbc:pool: URL should produce a connection pool.");
            PoolMetrics metrics = pool.getMetrics().orElseThrow();
            pool.warmup().block();

            assertEquals(CONFIGURED_INITIAL_SIZE, metrics.allocatedSize(), "The configured initial-size should apply.");
            assertEquals(CONFIGURED_MAX_SIZE, metrics.getMaxAllocatedSize(), "The configured max-size should apply.");
            assertFalse(pool.isDisposed(), "The pool should be open while the context runs.");
        }
        assertTrue(pool.isDisposed(), "Stopping the context should close the pool.");
    }

    @ParameterizedTest(name = "stored entity: {0}")
    @ValueSource(booleans = {false, true})
    void concurrentReadsReturnEveryConnection(boolean stored) {
        PoolMetrics metrics = assertInstanceOf(ConnectionPool.class, connectionFactory).getMetrics().orElseThrow();
        URI id = URI.create(stored ? STORED_ID : MISSING_ID);
        if (stored) {
            repository.createDomainEntity(new TestCatalog(id.toString())).block();
        }
        try {
            Flux.range(0, CONCURRENT_READS)
                    .flatMap(i -> repository.get(id, TestCatalog.class), CONCURRENT_READS)
                    .blockLast();

            await().atMost(RELEASE_TIMEOUT).untilAsserted(() -> assertEquals(0, metrics.acquiredSize(),
                    "Every connection should be back in the pool."));
        } finally {
            if (stored) {
                repository.deleteDomainEntity(id).block();
            }
        }
    }
}
