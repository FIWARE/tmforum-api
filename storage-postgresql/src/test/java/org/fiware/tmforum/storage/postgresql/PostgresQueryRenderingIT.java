package org.fiware.tmforum.storage.postgresql;

import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import io.r2dbc.spi.ConnectionFactory;
import io.r2dbc.spi.R2dbcException;
import io.r2dbc.spi.Statement;
import jakarta.inject.Inject;
import org.fiware.tmforum.storage.postgresql.QueryTestTypes.PathRoot;
import org.fiware.tmforum.storage.query.AttributePath;
import org.fiware.tmforum.storage.query.ComparisonOperator;
import org.fiware.tmforum.storage.query.EntityQuery;
import org.fiware.tmforum.storage.query.QueryFilter;
import org.fiware.tmforum.storage.query.QueryFilter.AllOf;
import org.fiware.tmforum.storage.query.QueryFilter.AnyOf;
import org.fiware.tmforum.storage.query.QueryFilter.Comparison;
import org.fiware.tmforum.storage.query.SortKey;
import org.fiware.tmforum.storage.query.ValueType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Runs rendered filters and sorts on raw documents in PostgreSQL: lists, scalars, nested arrays, missing keys and
 * values of the wrong JSON type, which must match nothing and sort as NULL instead of failing the statement.
 */
@MicronautTest(environments = PostgresStorage.ENVIRONMENT)
class PostgresQueryRenderingIT {

    private static final String TYPE = "query-rendering-document";
    private static final int FIRST_FRAGMENT_PARAMETER = 2;
    private static final String INVALID_REGULAR_EXPRESSION = "2201B";

    // one statement, so all rows share created_at and the tiebreaker orders them by id
    private static final String SEED = "INSERT INTO tmforum.entity (id, type, data) "
            + "SELECT d.id, $1, d.data::jsonb FROM unnest($2::text[], $3::text[]) AS d(id, data)";
    private static final String DELETE = "DELETE FROM tmforum.entity WHERE type = $1";
    private static final String SELECT = "SELECT id FROM tmforum.entity WHERE type = $1 AND %s %s";

    private static final String D1 = "doc-1";
    private static final String D2 = "doc-2";
    private static final String D3 = "doc-3";
    private static final String D4 = "doc-4";
    private static final String D5 = "doc-5";
    private static final String D6 = "doc-6";
    private static final List<String> IDS = List.of(D1, D2, D3, D4, D5, D6);
    private static final List<String> DOCUMENTS = List.of(
            """
                    {"name": "alpha", "count": 1, "active": true, "lastUpdate": "2024-01-01T10:00:00Z",
                     "tags": ["x", "y"], "children": [{"name": "c1", "tmfValue": 5}, {"name": "c2", "tmfValue": "s"}]}""",
            // scalars where lists are declared, and a sub-second date-time
            """
                    {"name": "beta", "count": 2.5, "active": false, "lastUpdate": "2024-01-01T10:00:00.500Z",
                     "tags": "x", "children": {"name": "c3"}}""",
            // a list where a scalar is declared, a nested array, a string at a number and a non-date at a date-time
            """
                    {"name": ["gamma", "alpha"], "count": "abc", "lastUpdate": "not a date",
                     "children": [[{"name": "c4"}]]}""",
            """
                    {"name": 7, "count": {"n": 1}, "active": "true", "lastUpdate": 12}""",
            """
                    {"customField": "x"}""",
            // upper case sorts before lower case in byte order
            """
                    {"name": "Zulu", "customField": 5}""");

    @Inject
    private ConnectionFactory connectionFactory;

    @Inject
    private PostgresDocumentMapper documentMapper;

    private JsonbQueryRenderer renderer;

    @BeforeEach
    void seed() {
        renderer = new JsonbQueryRenderer(new DocumentPathResolver(documentMapper.storage()));
        delete();
        run(SEED, statement -> statement.bind(0, TYPE)
                .bind(1, IDS.toArray(String[]::new))
                .bind(2, DOCUMENTS.toArray(String[]::new)));
    }

    @AfterEach
    void delete() {
        run(DELETE, statement -> statement.bind(0, TYPE));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("filters")
    void filters(String description, QueryFilter filter, List<String> expectedIds) {
        assertEquals(expectedIds, ids(new EntityQuery(PathRoot.class, filter, List.of())));
    }

    static Stream<Arguments> filters() {
        return Stream.of(
                Arguments.of("any list element", string("name", ComparisonOperator.EQUALS, "alpha"), List.of(D1, D3)),
                Arguments.of("a number is no string", string("name", ComparisonOperator.EQUALS, "7"), List.of()),
                Arguments.of("byte order", string("name", ComparisonOperator.LESS_THAN, "a"), List.of(D6)),
                Arguments.of("string greater", string("name", ComparisonOperator.GREATER_THAN, "b"), List.of(D2, D3)),
                Arguments.of("number greater, wrong types skipped",
                        number("count", ComparisonOperator.GREATER_THAN, "1"), List.of(D2)),
                Arguments.of("number equals", number("count", ComparisonOperator.EQUALS, "1"), List.of(D1)),
                Arguments.of("number any of", number("count", ComparisonOperator.LESS_THAN_EQUALS, "0", "2.5"),
                        List.of(D1, D2)),
                Arguments.of("boolean true, not the string", bool("active", "true"), List.of(D1)),
                Arguments.of("boolean false", bool("active", "false"), List.of(D2)),
                Arguments.of("date-time greater in byte order",
                        dateTime(ComparisonOperator.GREATER_THAN, "2024-01-01T10:00:00Z"), List.of(D3)),
                Arguments.of("sub-second sorts before whole second",
                        dateTime(ComparisonOperator.LESS_THAN, "2024-01-01T10:00:00Z"), List.of(D2)),
                Arguments.of("an offset is not normalised",
                        dateTime(ComparisonOperator.EQUALS, "2024-01-01T11:00:00+01:00"), List.of()),
                Arguments.of("scalar where a list is declared", string("tags", ComparisonOperator.EQUALS, "x"),
                        List.of(D1, D2)),
                Arguments.of("list element", string("tags", ComparisonOperator.EQUALS, "y"), List.of(D1)),
                Arguments.of("bridge getter in a list", number("children.value", ComparisonOperator.EQUALS, "5"),
                        List.of(D1)),
                Arguments.of("object where a list is declared", string("children.name", ComparisonOperator.EQUALS,
                        "c3"), List.of(D2)),
                Arguments.of("nested array", string("children.name", ComparisonOperator.EQUALS, "c4"), List.of(D3)),
                Arguments.of("regex", string("name", ComparisonOperator.REGEX, "^al"), List.of(D1, D3)),
                Arguments.of("regex not-equals idiom", string("name", ComparisonOperator.REGEX, "^(?!alpha$).*$"),
                        List.of(D2, D3, D6)),
                Arguments.of("regex only on strings", string("lastUpdate", ComparisonOperator.REGEX, ".*"),
                        List.of(D1, D2, D3)),
                Arguments.of("AND of OR", new AllOf(List.of(
                                new AnyOf(List.of(number("count", ComparisonOperator.EQUALS, "1"),
                                        bool("active", "false"))),
                                string("name", ComparisonOperator.REGEX, "a"))),
                        List.of(D1, D2)),
                Arguments.of("extension string", extension("customField", ValueType.STRING, "x"), List.of(D5)),
                Arguments.of("extension number", extension("customField", ValueType.NUMBER, "5"), List.of(D6)));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("sorts")
    void sorts(String description, SortKey key, List<String> expectedIds) {
        assertEquals(expectedIds, ids(new EntityQuery(PathRoot.class, null, List.of(key))));
    }

    static Stream<Arguments> sorts() {
        return Stream.of(
                Arguments.of("string ascending in byte order", sortKey("name", ValueType.STRING, false),
                        List.of(D6, D1, D2, D3, D4, D5)),
                Arguments.of("string descending, nulls last", sortKey("name", ValueType.STRING, true),
                        List.of(D3, D2, D1, D6, D4, D5)),
                Arguments.of("number ascending", sortKey("count", ValueType.NUMBER, false),
                        List.of(D1, D2, D3, D4, D5, D6)),
                Arguments.of("number descending", sortKey("count", ValueType.NUMBER, true),
                        List.of(D2, D1, D3, D4, D5, D6)),
                Arguments.of("date-time as string", sortKey("lastUpdate", ValueType.DATE_TIME, false),
                        List.of(D2, D1, D3, D4, D5, D6)),
                Arguments.of("boolean descending", sortKey("active", ValueType.BOOLEAN, true),
                        List.of(D1, D2, D3, D4, D5, D6)),
                // jsonb orders a string before a number
                Arguments.of("undeclared by raw jsonb",
                        new SortKey(new AttributePath(List.of("customField"), false), null, false),
                        List.of(D5, D6, D1, D2, D3, D4)));
    }

    @Test
    void filterAndSortNumberTheirParametersInOneStatement() {
        EntityQuery query = new EntityQuery(PathRoot.class, string("name", ComparisonOperator.REGEX, "a"),
                List.of(sortKey("count", ValueType.NUMBER, true)));

        assertEquals(List.of(D2, D1, D3), ids(query));
    }

    @Test
    void invalidRegexFailsTheStatement() {
        EntityQuery query = new EntityQuery(PathRoot.class, string("name", ComparisonOperator.REGEX, "("), List.of());

        R2dbcException failure = assertThrows(R2dbcException.class, () -> ids(query));
        assertEquals(INVALID_REGULAR_EXPRESSION, failure.getSqlState());
    }

    private List<String> ids(EntityQuery query) {
        SqlFragment filter = renderer.filter(query, FIRST_FRAGMENT_PARAMETER);
        SqlFragment order = renderer.orderBy(query, FIRST_FRAGMENT_PARAMETER + filter.bindings().size());
        List<Object> bindings = new ArrayList<>(List.of(TYPE));
        bindings.addAll(filter.bindings());
        bindings.addAll(order.bindings());
        return PostgresStatements.execute(connectionFactory, String.format(SELECT, filter.sql(), order.sql()),
                        statement -> bindAll(statement, bindings),
                        result -> result.map((row, metadata) -> row.get(0, String.class)))
                .collectList().block();
    }

    private void run(String sql, Function<Statement, Statement> binder) {
        PostgresStatements.execute(connectionFactory, sql, binder, result -> result.getRowsUpdated()).blockLast();
    }

    private static Statement bindAll(Statement statement, List<Object> bindings) {
        for (int i = 0; i < bindings.size(); i++) {
            statement.bind(i, bindings.get(i));
        }
        return statement;
    }

    private static Comparison string(String path, ComparisonOperator operator, String value) {
        return comparison(path, operator, ValueType.STRING, value);
    }

    private static Comparison number(String path, ComparisonOperator operator, String... values) {
        return comparison(path, operator, ValueType.NUMBER, values);
    }

    private static Comparison bool(String path, String value) {
        return comparison(path, ComparisonOperator.EQUALS, ValueType.BOOLEAN, value);
    }

    private static Comparison dateTime(ComparisonOperator operator, String value) {
        return comparison("lastUpdate", operator, ValueType.DATE_TIME, value);
    }

    private static Comparison extension(String key, ValueType valueType, String value) {
        return new Comparison(new AttributePath(List.of(key), false), ComparisonOperator.EQUALS, valueType,
                List.of(value));
    }

    private static Comparison comparison(String path, ComparisonOperator operator, ValueType valueType,
                                         String... values) {
        return new Comparison(new AttributePath(List.of(path.split("\\.")), true), operator, valueType,
                List.of(values));
    }

    private static SortKey sortKey(String path, ValueType valueType, boolean descending) {
        return new SortKey(new AttributePath(List.of(path.split("\\.")), true), valueType, descending);
    }
}
