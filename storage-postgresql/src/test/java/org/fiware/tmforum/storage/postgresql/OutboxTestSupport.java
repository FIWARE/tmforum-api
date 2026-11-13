package org.fiware.tmforum.storage.postgresql;

import io.r2dbc.spi.Connection;
import io.r2dbc.spi.ConnectionFactory;
import io.r2dbc.spi.Readable;
import org.fiware.tmforum.common.mapping.EventMapping;
import org.fiware.tmforum.common.notification.EventMapper;
import org.fiware.tmforum.storage.postgresql.DocumentTestEntities.TestCatalog;
import org.fiware.tmforum.storage.postgresql.DocumentTestEntities.TestNotStoredHolder;
import org.fiware.tmforum.storage.postgresql.DocumentTestEntities.TestResource;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import static org.fiware.tmforum.storage.postgresql.PostgresStorage.QUALIFIED_ENTITY_TABLE;
import static org.fiware.tmforum.storage.postgresql.PostgresStorage.QUALIFIED_OUTBOX_TABLE;

/**
 * Shared fixtures of the outbox ITs: plain SQL access, the test event mapping and the per-test cleanup.
 */
final class OutboxTestSupport {

    static final List<String> TEST_TYPES = List.of(TestCatalog.TYPE, TestNotStoredHolder.TYPE, TestResource.TYPE);
    static final OutboxProperties PROPERTIES = new OutboxProperties(true, Duration.ofSeconds(1), 20,
            Duration.ofMinutes(5), Duration.ofHours(1), Duration.ofDays(7), Duration.ofMinutes(1));

    private OutboxTestSupport() {
    }

    /**
     * An event mapper over the given types, the domain class as raw and target class, like a module's mapper.
     */
    static EventMapper eventMapper(Map<String, Class<?>> classes) {
        Map<String, EventMapping> mapping = new HashMap<>();
        classes.forEach((type, clazz) -> mapping.put(type, new EventMapping(clazz, clazz)));
        return new EventMapper() {
            @Override
            public Map<String, EventMapping> getEntityClassMapping() {
                return mapping;
            }

            @Override
            public Object mapPayload(Object rawPayload, Class<?> targetClass) {
                return rawPayload;
            }
        };
    }

    static void cleanUp(ConnectionFactory connectionFactory) {
        Object types = TEST_TYPES.toArray(String[]::new);
        query(connectionFactory, "DELETE FROM " + QUALIFIED_ENTITY_TABLE + " WHERE type = ANY($1)", row -> row,
                types);
        query(connectionFactory, "DELETE FROM " + QUALIFIED_OUTBOX_TABLE + " WHERE entity_type = ANY($1)",
                row -> row, types);
    }

    static <T> List<T> query(ConnectionFactory connectionFactory, String sql, Function<Readable, T> rowMapper,
                             Object... bindings) {
        return Flux.usingWhen(
                        Mono.from(connectionFactory.create()),
                        connection -> {
                            var statement = connection.createStatement(sql);
                            for (int i = 0; i < bindings.length; i++) {
                                statement.bind(i, bindings[i]);
                            }
                            return Flux.from(statement.execute())
                                    .flatMap(result -> result.map(rowMapper::apply));
                        },
                        Connection::close)
                .collectList()
                .block();
    }
}
