package org.fiware.tmforum.storage.postgresql;

import io.micronaut.context.annotation.Property;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Pins the SQL and bindings per operator, value type, nesting and sort. Semantics are checked in
 * {@link PostgresQueryRenderingIT}.
 */
@MicronautTest(environments = PostgresStorage.ENVIRONMENT, startApplication = false)
@Property(name = "flyway.datasources.default.enabled", value = "false")
class JsonbQueryRendererTest {

    // parameters before the fragment, e.g. the type list of the list statement
    private static final int FIRST_PARAMETER = 3;

    private static final String MATCH =
            "EXISTS (SELECT 1 FROM jsonb_path_query(data, %s::jsonpath) AS v(value) WHERE %s)";
    private static final String TEXT =
            "(CASE WHEN jsonb_typeof(%1$s) = 'string' THEN %1$s #>> '{}' END) COLLATE \"C\"";
    private static final String NUMBER = "CASE WHEN jsonb_typeof(%1$s) = 'number' THEN (%1$s)::numeric END";
    private static final String BOOLEAN = "CASE WHEN jsonb_typeof(%1$s) = 'boolean' THEN (%1$s)::boolean END";
    private static final String REGEX = "CASE WHEN jsonb_typeof(v.value) = 'string' THEN v.value #>> '{}' END ~ $4";
    private static final String MATCHED = "v.value";
    private static final String FIRST = "jsonb_path_query_first(data, %s::jsonpath)";
    private static final String TIEBREAKER = "created_at, id";

    private static final Map<ComparisonOperator, String> SQL_OPERATORS = Map.of(
            ComparisonOperator.EQUALS, "=",
            ComparisonOperator.GREATER_THAN, ">",
            ComparisonOperator.GREATER_THAN_EQUALS, ">=",
            ComparisonOperator.LESS_THAN, "<",
            ComparisonOperator.LESS_THAN_EQUALS, "<=");

    private static final String NAME_PATH = "$.\"name\"[*]";
    private static final String COUNT_PATH = "$.\"count\"[*]";
    private static final String ACTIVE_PATH = "$.\"active\"[*]";
    private static final String LAST_UPDATE_PATH = "$.\"lastUpdate\"[*]";

    @Inject
    private PostgresDocumentMapper documentMapper;

    private JsonbQueryRenderer renderer;

    @BeforeEach
    void setUp() {
        renderer = new JsonbQueryRenderer(new DocumentPathResolver(documentMapper.storage()));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource({"comparisons", "structures", "dateTimes"})
    void rendersTheFilter(String description, QueryFilter filter, String expectedSql, List<Object> expectedBindings) {
        SqlFragment fragment = renderer.filter(new EntityQuery(PathRoot.class, filter, List.of()), FIRST_PARAMETER);

        assertEquals(expectedSql, fragment.sql());
        assertEquals(expectedBindings, comparable(fragment.bindings()));
    }

    static Stream<Arguments> comparisons() {
        Stream<Arguments> ordered = SQL_OPERATORS.entrySet().stream().flatMap(operator -> Stream.of(
                Arguments.of("string " + operator.getKey(),
                        comparison("name", operator.getKey(), ValueType.STRING, "a", "b"),
                        match(text(MATCHED) + " " + operator.getValue() + " ANY($4::text[])"),
                        List.of(NAME_PATH, List.of("a", "b"))),
                Arguments.of("number " + operator.getKey(),
                        comparison("count", operator.getKey(), ValueType.NUMBER, "1.5", "-2e3"),
                        match(String.format(NUMBER, MATCHED) + " " + operator.getValue() + " ANY($4::numeric[])"),
                        List.of(COUNT_PATH, List.of(new BigDecimal("1.5"), new BigDecimal("-2e3"))))));
        return Stream.concat(ordered, Stream.of(
                Arguments.of("boolean EQUALS",
                        comparison("active", ComparisonOperator.EQUALS, ValueType.BOOLEAN, "true", "false"),
                        match(String.format(BOOLEAN, MATCHED) + " = ANY($4::boolean[])"),
                        List.of(ACTIVE_PATH, List.of(true, false))),
                Arguments.of("regex with lookahead",
                        comparison("name", ComparisonOperator.REGEX, ValueType.STRING, "^(?!a\"b$).*$"),
                        match(REGEX), List.of(NAME_PATH, "^(?!a\"b$).*$"))));
    }

    static Stream<Arguments> structures() {
        Comparison name = comparison("name", ComparisonOperator.EQUALS, ValueType.STRING, "a");
        Comparison count = comparison("count", ComparisonOperator.LESS_THAN, ValueType.NUMBER, "3");
        Comparison active = comparison("active", ComparisonOperator.EQUALS, ValueType.BOOLEAN, "true");
        return Stream.of(
                Arguments.of("AND inside OR", new AnyOf(List.of(new AllOf(List.of(name, count)), active)),
                        "((" + String.format(MATCH, "$3", text(MATCHED) + " = ANY($4::text[])") + " AND "
                                + String.format(MATCH, "$5", String.format(NUMBER, MATCHED) + " < ANY($6::numeric[])")
                                + ") OR "
                                + String.format(MATCH, "$7", String.format(BOOLEAN, MATCHED) + " = ANY($8::boolean[])")
                                + ")",
                        List.of(NAME_PATH, List.of("a"), COUNT_PATH, List.of(new BigDecimal("3")), ACTIVE_PATH,
                                List.of(true))),
                Arguments.of("nested path through a bridge getter",
                        comparison("children.value", ComparisonOperator.EQUALS, ValueType.STRING, "x"),
                        match(text(MATCHED) + " = ANY($4::text[])"),
                        List.of("$.\"children\"[*].\"tmfValue\"[*]", List.of("x"))),
                Arguments.of("extension attribute",
                        undeclared(List.of("customField", "value"), "x"),
                        match(text(MATCHED) + " = ANY($4::text[])"),
                        List.of("$.\"customField\"[*].\"value\"[*]", List.of("x"))),
                Arguments.of("segments with quote, dot and backslash",
                        undeclared(List.of("a\"b", "c.d", "e\\f"), "x"),
                        match(text(MATCHED) + " = ANY($4::text[])"),
                        List.of("$.\"a\\\"b\"[*].\"c.d\"[*].\"e\\\\f\"[*]", List.of("x"))),
                Arguments.of("control character in a segment",
                        undeclared(List.of("a\nb"), "x"),
                        match(text(MATCHED) + " = ANY($4::text[])"),
                        List.of("$.\"a\\u000ab\"[*]", List.of("x"))));
    }

    // string parity with the brokers: every literal form is bound as written and compared in byte order
    static Stream<Arguments> dateTimes() {
        return Stream.of("2024-01-01T10:00:00Z", "2024-01-01T10:00:00.500Z", "2024-01-01T11:00:00+01:00",
                        "2024-01-01T10:00:00", "2024-01-01")
                .flatMap(literal -> Stream.of(ComparisonOperator.EQUALS, ComparisonOperator.GREATER_THAN)
                        .map(operator -> Arguments.of("date-time " + operator + " " + literal,
                                comparison("lastUpdate", operator, ValueType.DATE_TIME, literal),
                                match(text(MATCHED) + " " + SQL_OPERATORS.get(operator) + " ANY($4::text[])"),
                                List.of(LAST_UPDATE_PATH, List.of(literal)))));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("sorts")
    void rendersTheOrder(String description, List<SortKey> sort, String expectedSql, List<Object> expectedBindings) {
        SqlFragment fragment = renderer.orderBy(new EntityQuery(PathRoot.class, null, sort), FIRST_PARAMETER);

        assertEquals(expectedSql, fragment.sql());
        assertEquals(expectedBindings, fragment.bindings());
    }

    static Stream<Arguments> sorts() {
        return Stream.of(
                Arguments.of("no sort", List.of(), "ORDER BY " + TIEBREAKER, List.of()),
                Arguments.of("string ascending", List.of(sortKey("name", ValueType.STRING, false)),
                        "ORDER BY " + text(String.format(FIRST, "$3")) + " ASC NULLS LAST, " + TIEBREAKER,
                        List.of(NAME_PATH)),
                Arguments.of("date-time as string", List.of(sortKey("lastUpdate", ValueType.DATE_TIME, true)),
                        "ORDER BY " + text(String.format(FIRST, "$3")) + " DESC NULLS LAST, " + TIEBREAKER,
                        List.of(LAST_UPDATE_PATH)),
                Arguments.of("number and boolean",
                        List.of(sortKey("count", ValueType.NUMBER, true), sortKey("active", ValueType.BOOLEAN, false)),
                        "ORDER BY " + String.format(NUMBER, String.format(FIRST, "$3")) + " DESC NULLS LAST, "
                                + String.format(BOOLEAN, String.format(FIRST, "$4")) + " ASC NULLS LAST, "
                                + TIEBREAKER,
                        List.of(COUNT_PATH, ACTIVE_PATH)),
                Arguments.of("undeclared key by raw jsonb",
                        List.of(new SortKey(new AttributePath(List.of("customField"), false), null, false)),
                        "ORDER BY " + String.format(FIRST, "$3") + " ASC NULLS LAST, " + TIEBREAKER,
                        List.of("$.\"customField\"[*]")),
                Arguments.of("nested key through a bridge getter",
                        List.of(sortKey("otherNamedSub.value", ValueType.STRING, false)),
                        "ORDER BY " + text(String.format(FIRST, "$3")) + " ASC NULLS LAST, " + TIEBREAKER,
                        List.of("$.\"otherNamedSub\"[*].\"tmfValue\"[*]")));
    }

    @ParameterizedTest(name = "first parameter {0}")
    @MethodSource("noFilters")
    void rendersNoFilterAsTrue(int firstParameter) {
        SqlFragment fragment = renderer.filter(new EntityQuery(PathRoot.class, null, List.of()), firstParameter);

        assertEquals(new SqlFragment(JsonbQueryRenderer.NO_FILTER, List.of()), fragment);
    }

    static Stream<Integer> noFilters() {
        return Stream.of(1, FIRST_PARAMETER);
    }

    private static Comparison comparison(String path, ComparisonOperator operator, ValueType valueType,
                                         String... values) {
        return new Comparison(new AttributePath(List.of(path.split("\\.")), true), operator, valueType,
                List.of(values));
    }

    private static Comparison undeclared(List<String> segments, String value) {
        return new Comparison(new AttributePath(segments, false), ComparisonOperator.EQUALS, ValueType.STRING,
                List.of(value));
    }

    private static SortKey sortKey(String path, ValueType valueType, boolean descending) {
        return new SortKey(new AttributePath(List.of(path.split("\\.")), true), valueType, descending);
    }

    private static String match(String predicate) {
        return String.format(MATCH, "$3", predicate);
    }

    private static String text(String value) {
        return String.format(TEXT, value);
    }

    // arrays compare by identity
    private static List<Object> comparable(List<Object> bindings) {
        return bindings.stream()
                .map(binding -> binding instanceof Object[] array ? (Object) Arrays.asList(array) : binding)
                .toList();
    }
}
