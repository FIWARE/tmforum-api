package org.fiware.tmforum.storage.postgresql;

import io.micronaut.context.annotation.Requires;
import io.micronaut.context.env.Environment;
import io.r2dbc.spi.ConnectionFactory;
import jakarta.inject.Singleton;

import java.util.List;

import static org.fiware.tmforum.storage.postgresql.PostgresStorage.QUALIFIED_OUTBOX_TABLE;

/**
 * Test-only read access to the event outbox, so module ITs can assert what an API write enqueued.
 */
@Singleton
@Requires(env = Environment.TEST)
@Requires(env = PostgresStorage.ENVIRONMENT)
public class PostgresOutboxInspector {

    private static final String SELECT_OUTBOX_KINDS =
            "SELECT event_kind FROM " + QUALIFIED_OUTBOX_TABLE + " WHERE entity_id = $1 ORDER BY id";

    private final ConnectionFactory connectionFactory;

    public PostgresOutboxInspector(ConnectionFactory connectionFactory) {
        this.connectionFactory = connectionFactory;
    }

    /**
     * Kinds of the outbox rows of an entity, oldest first.
     */
    public List<EventKind> outboxEventKinds(String entityId) {
        return PostgresStatements.execute(connectionFactory, SELECT_OUTBOX_KINDS,
                        statement -> statement.bind(0, entityId),
                        result -> result.map(row -> EventKind.valueOf(row.get(0, String.class))))
                .collectList()
                .block();
    }
}
