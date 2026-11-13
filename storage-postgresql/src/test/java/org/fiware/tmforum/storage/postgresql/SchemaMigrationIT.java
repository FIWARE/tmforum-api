package org.fiware.tmforum.storage.postgresql;

import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import io.r2dbc.spi.Connection;
import io.r2dbc.spi.ConnectionFactory;
import io.r2dbc.spi.Readable;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Arrays;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Stream;

import static org.fiware.tmforum.storage.postgresql.PostgresStorage.ENTITY_CURRENT_INDEX;
import static org.fiware.tmforum.storage.postgresql.PostgresStorage.ENTITY_TABLE;
import static org.fiware.tmforum.storage.postgresql.PostgresStorage.QUALIFIED_ENTITY_TABLE;
import static org.fiware.tmforum.storage.postgresql.PostgresStorage.SCHEMA;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Verifies the Flyway migration against the k3s PostgreSQL: the entity table matches the ticket's schema exactly.
 */
@MicronautTest(environments = PostgresStorage.ENVIRONMENT)
class SchemaMigrationIT {

    private static final String FLYWAY_HISTORY_TABLE = SCHEMA + ".flyway_schema_history";
    private static final String INITIAL_VERSION = "1";
    private static final String PRIMARY_KEY_INDEX = ENTITY_TABLE + "_pkey";
    private static final String NOT_NULL = "NO";

    @Inject
    private ConnectionFactory connectionFactory;

    @Test
    void initialMigrationSucceeded() {
        List<Boolean> success = query(
                "SELECT success FROM " + FLYWAY_HISTORY_TABLE + " WHERE version = '" + INITIAL_VERSION + "'",
                row -> row.get("success", Boolean.class));

        assertEquals(List.of(true), success, "The initial migration should be recorded exactly once, successful.");
    }

    static Stream<Arguments> expectedColumns() {
        // column name, data type, default (null if none); every column is NOT NULL
        return Stream.of(
                Arguments.of("id", "text", null),
                Arguments.of("version", "text", "''::text"),
                Arguments.of("revision", "bigint", "1"),
                Arguments.of("is_current", "boolean", "true"),
                Arguments.of("type", "text", null),
                Arguments.of("data", "jsonb", null),
                Arguments.of("created_at", "timestamp with time zone", "now()"),
                Arguments.of("updated_at", "timestamp with time zone", "now()"));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("expectedColumns")
    void columnIsDefinedAsInTheSchema(String column, String dataType, String columnDefault) {
        List<List<String>> definitions = query(
                "SELECT data_type::text, is_nullable::text, column_default::text FROM information_schema.columns "
                        + "WHERE table_schema = $1 AND table_name = $2 AND column_name = $3",
                row -> Arrays.asList(
                        row.get("data_type", String.class),
                        row.get("is_nullable", String.class),
                        row.get("column_default", String.class)),
                SCHEMA, ENTITY_TABLE, column);

        assertEquals(List.of(Arrays.asList(dataType, NOT_NULL, columnDefault)), definitions,
                "Column " + column + " should have the expected type, nullability and default.");
    }

    @Test
    void noAdditionalColumns() {
        List<String> columns = query(
                "SELECT column_name::text FROM information_schema.columns WHERE table_schema = $1 AND table_name = $2",
                row -> row.get("column_name", String.class),
                SCHEMA, ENTITY_TABLE);

        assertEquals(expectedColumns().map(arguments -> (String) arguments.get()[0]).sorted().toList(),
                columns.stream().sorted().toList(), "The table should have exactly the specified columns.");
    }

    @Test
    void primaryKeyIsIdAndVersion() {
        List<String> keyColumns = query(
                "SELECT a.attname::text FROM pg_index i "
                        + "JOIN LATERAL unnest(i.indkey) WITH ORDINALITY AS k(attnum, position) ON true "
                        + "JOIN pg_attribute a ON a.attrelid = i.indrelid AND a.attnum = k.attnum "
                        + "WHERE i.indrelid = $1::regclass AND i.indisprimary ORDER BY k.position",
                row -> row.get("attname", String.class),
                QUALIFIED_ENTITY_TABLE);

        assertEquals(List.of("id", "version"), keyColumns, "The primary key should be (id, version).");
    }

    @Test
    void currentIndexIsUniqueAndPartialOnId() {
        List<List<Object>> indexes = query(
                "SELECT i.indisunique, pg_get_expr(i.indpred, i.indrelid) AS predicate, a.attname::text FROM pg_index i "
                        + "JOIN pg_attribute a ON a.attrelid = i.indrelid AND a.attnum = ANY(i.indkey) "
                        + "WHERE i.indexrelid = $1::regclass",
                row -> List.of(
                        row.get("indisunique", Boolean.class),
                        row.get("predicate", String.class),
                        row.get("attname", String.class)),
                SCHEMA + "." + ENTITY_CURRENT_INDEX);

        assertEquals(List.of(List.of(true, "is_current", "id")), indexes,
                ENTITY_CURRENT_INDEX + " should be a unique index on id, restricted to the current rows.");
    }

    @Test
    void noOtherIndexExists() {
        List<String> indexes = query(
                "SELECT indexname::text FROM pg_indexes WHERE schemaname = $1 AND tablename = $2",
                row -> row.get("indexname", String.class),
                SCHEMA, ENTITY_TABLE);

        assertEquals(List.of(ENTITY_CURRENT_INDEX, PRIMARY_KEY_INDEX), indexes.stream().sorted().toList(),
                "Only the primary key and " + ENTITY_CURRENT_INDEX + " should exist.");
    }

    private <T> List<T> query(String sql, Function<Readable, T> rowMapper, Object... bindings) {
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
