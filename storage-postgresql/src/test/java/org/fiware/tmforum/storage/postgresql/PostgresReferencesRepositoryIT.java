package org.fiware.tmforum.storage.postgresql;

import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import io.r2dbc.spi.Connection;
import io.r2dbc.spi.ConnectionFactory;
import jakarta.inject.Inject;
import org.fiware.tmforum.storage.ReferenceReadRepository;
import org.fiware.tmforum.storage.StoredReference;
import org.fiware.tmforum.storage.postgresql.DocumentTestEntities.TestCatalog;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

import static org.fiware.tmforum.storage.postgresql.PostgresStorage.QUALIFIED_ENTITY_TABLE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

/**
 * Reference lookups of the PostgreSQL adapter on rows the entity port cannot write, against the k3s PostgreSQL.
 */
@MicronautTest(environments = PostgresStorage.ENVIRONMENT)
class PostgresReferencesRepositoryIT {

    private static final String CURRENT_ID = "urn:ngsi-ld:" + TestCatalog.TYPE + ":current";
    private static final String SUPERSEDED_ID = "urn:ngsi-ld:" + TestCatalog.TYPE + ":superseded";
    private static final String OLD_VERSION = "1";
    private static final String NEW_VERSION = "2";
    private static final String EMPTY_DOCUMENT = "{}";
    private static final String INSERT_VERSION = "INSERT INTO " + QUALIFIED_ENTITY_TABLE
            + " (id, version, is_current, type, data) VALUES ($1, $2, $3, $4, $5::jsonb)";

    @Inject
    private ReferenceReadRepository port;

    @Inject
    private ConnectionFactory connectionFactory;

    @BeforeEach
    void cleanUp() {
        execute("DELETE FROM " + QUALIFIED_ENTITY_TABLE + " WHERE type = $1", TestCatalog.TYPE);
    }

    @Test
    void postgresRepositoryIsThePrimaryPort() {
        assertInstanceOf(PostgresReferencesRepository.class, port,
                "The postgresql environment should select the PostgreSQL adapter.");
    }

    // versioning is inert today, but a non-current row must never count as an existing entity
    @Test
    void onlyCurrentRowsAreFound() {
        execute(INSERT_VERSION, CURRENT_ID, OLD_VERSION, false, TestCatalog.TYPE, EMPTY_DOCUMENT);
        execute(INSERT_VERSION, CURRENT_ID, NEW_VERSION, true, TestCatalog.TYPE, EMPTY_DOCUMENT);
        execute(INSERT_VERSION, SUPERSEDED_ID, OLD_VERSION, false, TestCatalog.TYPE, EMPTY_DOCUMENT);

        assertEquals(List.of(new StoredReference(CURRENT_ID, TestCatalog.TYPE)),
                port.findReferences(List.of(CURRENT_ID, SUPERSEDED_ID)).collectList().block(),
                "One reference per current row, none for an id without one.");
    }

    private void execute(String sql, Object... bindings) {
        Flux.usingWhen(Mono.from(connectionFactory.create()),
                        connection -> {
                            var statement = connection.createStatement(sql);
                            for (int i = 0; i < bindings.length; i++) {
                                statement.bind(i, bindings[i]);
                            }
                            return Flux.from(statement.execute()).flatMap(result -> result.getRowsUpdated());
                        },
                        Connection::close)
                .blockLast();
    }
}
