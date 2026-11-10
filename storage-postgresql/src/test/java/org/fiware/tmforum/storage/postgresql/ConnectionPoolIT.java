package org.fiware.tmforum.storage.postgresql;

import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import io.r2dbc.pool.ConnectionPool;
import io.r2dbc.pool.PoolMetrics;
import io.r2dbc.spi.ConnectionFactory;
import jakarta.inject.Inject;
import org.fiware.tmforum.storage.postgresql.DocumentTestEntities.TestCatalog;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;

import java.net.URI;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The configured {@code r2dbc:pool:} URL puts a bounded pool in front of the driver, and the repository reuses it.
 */
@MicronautTest(environments = PostgresStorage.ENVIRONMENT)
class ConnectionPoolIT {

    // max-size of the test application-postgresql.yaml
    private static final int CONFIGURED_MAX_SIZE = 10;
    private static final int CONCURRENT_READS = CONFIGURED_MAX_SIZE * 3;
    private static final URI MISSING_ID = URI.create("urn:ngsi-ld:" + TestCatalog.TYPE + ":missing");

    @Inject
    private ConnectionFactory connectionFactory;

    @Inject
    private PostgresEntityRepository repository;

    @Test
    void concurrentReadsStayWithinThePool() {
        ConnectionPool pool = assertInstanceOf(ConnectionPool.class, connectionFactory,
                "The r2dbc:pool: URL should produce a connection pool.");
        PoolMetrics metrics = pool.getMetrics().orElseThrow();
        assertEquals(CONFIGURED_MAX_SIZE, metrics.getMaxAllocatedSize(), "The configured max-size should apply.");

        Flux.range(0, CONCURRENT_READS)
                .flatMap(i -> repository.get(MISSING_ID, TestCatalog.class), CONCURRENT_READS)
                .blockLast();

        assertTrue(metrics.allocatedSize() <= CONFIGURED_MAX_SIZE,
                "Concurrent reads should never open more connections than the pool allows.");
        assertEquals(0, metrics.acquiredSize(), "Every connection should be back in the pool.");
    }
}
