package org.fiware.tmforum.storage.postgresql;

import io.micronaut.context.annotation.Requires;
import io.micronaut.context.env.Environment;
import io.r2dbc.spi.ConnectionFactory;
import jakarta.inject.Singleton;
import org.fiware.tmforum.common.test.TestEntityCleaner;

import static org.fiware.tmforum.storage.postgresql.PostgresStorage.QUALIFIED_ENTITY_TABLE;
import static org.fiware.tmforum.storage.postgresql.PostgresStorage.QUALIFIED_OUTBOX_TABLE;

/**
 * Test-only cleaner of the PostgreSQL storage: deletes the entity rows of a type, every version, and their outbox rows.
 */
@Singleton
@Requires(env = Environment.TEST)
@Requires(env = PostgresStorage.ENVIRONMENT)
public class PostgresTestEntityCleaner implements TestEntityCleaner {

    private static final String TYPE_SEPARATOR = ",";
    private static final String DELETE_ENTITIES = "DELETE FROM " + QUALIFIED_ENTITY_TABLE + " WHERE type = ANY($1)";
    private static final String DELETE_OUTBOX_ROWS =
            "DELETE FROM " + QUALIFIED_OUTBOX_TABLE + " WHERE entity_type = ANY($1)";

    private final ConnectionFactory connectionFactory;

    public PostgresTestEntityCleaner(ConnectionFactory connectionFactory) {
        this.connectionFactory = connectionFactory;
    }

    @Override
    public void deleteAll(String entityTypes) {
        String[] types = entityTypes.split(TYPE_SEPARATOR);
        delete(DELETE_ENTITIES, types);
        delete(DELETE_OUTBOX_ROWS, types);
    }

    private void delete(String sql, String[] entityTypes) {
        PostgresStatements.execute(connectionFactory, sql, statement -> statement.bind(0, entityTypes),
                        result -> result.getRowsUpdated())
                .blockLast();
    }
}
