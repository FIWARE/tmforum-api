package org.fiware.tmforum.storage.query;

import org.fiware.tmforum.storage.query.QueryFilter.AllOf;
import org.fiware.tmforum.storage.query.QueryFilter.AnyOf;
import org.fiware.tmforum.storage.query.QueryFilter.Comparison;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class QueryModelTest {

    private static final AttributePath NAME = new AttributePath(List.of("name"), true);
    private static final AttributePath EXTENSION = new AttributePath(List.of("custom", "field"), false);
    private static final String VALUE = "value";
    private static final Comparison NAME_EQUALS =
            new Comparison(NAME, ComparisonOperator.EQUALS, ValueType.STRING, List.of(VALUE));

    static Stream<Arguments> invalidConstructions() {
        return Stream.of(
                Arguments.of("path without segments", NullPointerException.class,
                        (Executable) () -> new AttributePath(null, true)),
                Arguments.of("empty path", IllegalArgumentException.class,
                        (Executable) () -> new AttributePath(List.of(), true)),
                Arguments.of("blank path segment", IllegalArgumentException.class,
                        (Executable) () -> new AttributePath(List.of("a", " "), true)),
                Arguments.of("null path segment", NullPointerException.class,
                        (Executable) () -> new AttributePath(Arrays.asList("a", null), true)),
                Arguments.of("comparison without attribute", NullPointerException.class,
                        (Executable) () -> new Comparison(null, ComparisonOperator.EQUALS, ValueType.STRING,
                                List.of(VALUE))),
                Arguments.of("comparison without operator", NullPointerException.class,
                        (Executable) () -> new Comparison(NAME, null, ValueType.STRING, List.of(VALUE))),
                Arguments.of("comparison without value type", NullPointerException.class,
                        (Executable) () -> new Comparison(NAME, ComparisonOperator.EQUALS, null, List.of(VALUE))),
                Arguments.of("comparison without values", NullPointerException.class,
                        (Executable) () -> new Comparison(NAME, ComparisonOperator.EQUALS, ValueType.STRING, null)),
                Arguments.of("comparison with empty values", IllegalArgumentException.class,
                        (Executable) () -> new Comparison(NAME, ComparisonOperator.EQUALS, ValueType.STRING,
                                List.of())),
                Arguments.of("comparison with null value", NullPointerException.class,
                        (Executable) () -> new Comparison(NAME, ComparisonOperator.EQUALS, ValueType.STRING,
                                Arrays.asList(VALUE, null))),
                Arguments.of("regex with two patterns", IllegalArgumentException.class,
                        (Executable) () -> new Comparison(NAME, ComparisonOperator.REGEX, ValueType.STRING,
                                List.of("a.*", "b.*"))),
                Arguments.of("regex on a number", IllegalArgumentException.class,
                        (Executable) () -> new Comparison(NAME, ComparisonOperator.REGEX, ValueType.NUMBER,
                                List.of("1.*"))),
                Arguments.of("regex on a date-time", IllegalArgumentException.class,
                        (Executable) () -> new Comparison(NAME, ComparisonOperator.REGEX, ValueType.DATE_TIME,
                                List.of("2024.*"))),
                Arguments.of("allOf without children", NullPointerException.class,
                        (Executable) () -> new AllOf(null)),
                Arguments.of("allOf with empty children", IllegalArgumentException.class,
                        (Executable) () -> new AllOf(List.of())),
                Arguments.of("allOf with null child", NullPointerException.class,
                        (Executable) () -> new AllOf(Arrays.asList(NAME_EQUALS, null))),
                Arguments.of("anyOf without children", NullPointerException.class,
                        (Executable) () -> new AnyOf(null)),
                Arguments.of("anyOf with empty children", IllegalArgumentException.class,
                        (Executable) () -> new AnyOf(List.of())),
                Arguments.of("anyOf with null child", NullPointerException.class,
                        (Executable) () -> new AnyOf(Arrays.asList(NAME_EQUALS, null))),
                Arguments.of("sort key without attribute", NullPointerException.class,
                        (Executable) () -> new SortKey(null, ValueType.STRING, false)),
                Arguments.of("typed sort key on an extension", IllegalArgumentException.class,
                        (Executable) () -> new SortKey(EXTENSION, ValueType.STRING, false)),
                Arguments.of("query without class", NullPointerException.class,
                        (Executable) () -> new EntityQuery(null, NAME_EQUALS, List.of())),
                Arguments.of("query with null sort", NullPointerException.class,
                        (Executable) () -> new EntityQuery(Object.class, NAME_EQUALS, null)),
                Arguments.of("query with null sort key", NullPointerException.class,
                        (Executable) () -> new EntityQuery(Object.class, null,
                                Arrays.asList(new SortKey(NAME, ValueType.STRING, true), null))));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("invalidConstructions")
    void rejectsInvalidConstruction(String description, Class<? extends Throwable> expected, Executable construction) {
        assertThrows(expected, construction, description);
    }

    static Stream<Arguments> validConstructions() {
        return Stream.of(
                Arguments.of("single-value regex", (Executable) () -> new Comparison(NAME, ComparisonOperator.REGEX,
                        ValueType.STRING, List.of("^a.*$"))),
                Arguments.of("empty string value", (Executable) () -> new Comparison(NAME,
                        ComparisonOperator.EQUALS, ValueType.STRING, List.of(""))),
                Arguments.of("untyped sort key on an extension",
                        (Executable) () -> new SortKey(EXTENSION, null, true)),
                Arguments.of("untyped sort key on a declared attribute",
                        (Executable) () -> new SortKey(NAME, null, false)),
                Arguments.of("query without filter and sort",
                        (Executable) () -> new EntityQuery(Object.class, null, List.of())),
                Arguments.of("nested filter", (Executable) () -> new AllOf(List.of(NAME_EQUALS,
                        new AnyOf(List.of(NAME_EQUALS, NAME_EQUALS))))));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("validConstructions")
    void acceptsValidConstruction(String description, Executable construction) {
        assertDoesNotThrow(construction, description);
    }

    private static final Map<ValueType, List<String>> VALID_LITERALS = Map.of(
            ValueType.STRING, List.of("a", "", "1", "true"),
            ValueType.NUMBER, List.of("0", "-1", "1.5", "2e10", "-3.2E-4"),
            ValueType.BOOLEAN, List.of("true", "false"),
            ValueType.DATE_TIME, List.of("2024-01-01T00:00:00Z", "2024-01-01T00:00:00.500+01:00",
                    "2024-01-01T00:00:00", "2024-01-01"));

    static Stream<Arguments> validLiterals() {
        return VALID_LITERALS.entrySet().stream().map(entry -> Arguments.of(entry.getKey(), entry.getValue()));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("validLiterals")
    void acceptsSeveralValidLiteralsOfTheType(ValueType valueType, List<String> values) {
        assertEquals(values, new Comparison(NAME, ComparisonOperator.EQUALS, valueType, values).values());
    }

    @ParameterizedTest(name = "{0} {1}")
    @MethodSource("invalidLiterals")
    void rejectsLiteralNotOfTheType(ValueType valueType, String literal) {
        assertThrows(IllegalArgumentException.class,
                () -> new Comparison(NAME, ComparisonOperator.EQUALS, valueType, List.of(literal)));
    }

    static Stream<Arguments> invalidLiterals() {
        return Stream.of(
                Arguments.of(ValueType.NUMBER, "abc"),
                Arguments.of(ValueType.NUMBER, "01"),
                Arguments.of(ValueType.NUMBER, " 1"),
                Arguments.of(ValueType.NUMBER, "1,2"),
                Arguments.of(ValueType.NUMBER, ".5"),
                Arguments.of(ValueType.NUMBER, "NaN"),
                Arguments.of(ValueType.NUMBER, "Infinity"),
                Arguments.of(ValueType.NUMBER, "0x1p1"),
                Arguments.of(ValueType.BOOLEAN, "TRUE"),
                Arguments.of(ValueType.BOOLEAN, "yes"),
                Arguments.of(ValueType.DATE_TIME, "abc"),
                Arguments.of(ValueType.DATE_TIME, "2024-13-01"));
    }

    @ParameterizedTest
    @EnumSource(value = ComparisonOperator.class, mode = EnumSource.Mode.EXCLUDE, names = {"EQUALS", "REGEX"})
    void rejectsOrderingOnBooleans(ComparisonOperator operator) {
        assertThrows(IllegalArgumentException.class,
                () -> new Comparison(NAME, operator, ValueType.BOOLEAN, List.of("true")));
    }

    @ParameterizedTest
    @EnumSource(value = ComparisonOperator.class, mode = EnumSource.Mode.EXCLUDE, names = "REGEX")
    void orderedTypesTakeEveryNonRegexOperator(ComparisonOperator operator) {
        for (ValueType valueType : List.of(ValueType.STRING, ValueType.NUMBER, ValueType.DATE_TIME)) {
            assertDoesNotThrow(() -> new Comparison(NAME, operator, valueType, VALID_LITERALS.get(valueType)));
        }
    }

    static Stream<Arguments> listCopies() {
        return Stream.of(
                Arguments.of("path segments",
                        (Function<List<Object>, List<?>>) list -> new AttributePath(strings(list), true).segments(),
                        "segment"),
                Arguments.of("comparison values",
                        (Function<List<Object>, List<?>>) list -> new Comparison(NAME, ComparisonOperator.EQUALS,
                                ValueType.STRING, strings(list)).values(),
                        VALUE),
                Arguments.of("allOf children",
                        (Function<List<Object>, List<?>>) list -> new AllOf(filters(list)).filters(), NAME_EQUALS),
                Arguments.of("anyOf children",
                        (Function<List<Object>, List<?>>) list -> new AnyOf(filters(list)).filters(), NAME_EQUALS),
                Arguments.of("sort keys",
                        (Function<List<Object>, List<?>>) list -> new EntityQuery(Object.class, null,
                                sortKeys(list)).sort(),
                        new SortKey(NAME, ValueType.STRING, false)));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("listCopies")
    void copiesListsImmutably(String description, Function<List<Object>, List<?>> construct, Object element) {
        List<Object> source = new ArrayList<>(List.of(element));

        List<?> stored = construct.apply(source);
        source.add(element);

        assertEquals(1, stored.size(), description + " must not follow the caller's list");
        assertThrows(UnsupportedOperationException.class, () -> stored.clear(), description + " must be immutable");
    }

    @Test
    void equalModelsAreEqual() {
        EntityQuery query = new EntityQuery(Object.class,
                new AllOf(List.of(NAME_EQUALS, new Comparison(EXTENSION, ComparisonOperator.REGEX, ValueType.STRING,
                        List.of("x.*")))),
                List.of(new SortKey(NAME, ValueType.STRING, true)));
        EntityQuery sameQuery = new EntityQuery(Object.class,
                new AllOf(new ArrayList<>(List.of(
                        new Comparison(new AttributePath(new ArrayList<>(List.of("name")), true),
                                ComparisonOperator.EQUALS, ValueType.STRING, new ArrayList<>(List.of(VALUE))),
                        new Comparison(new AttributePath(List.of("custom", "field"), false), ComparisonOperator.REGEX,
                                ValueType.STRING, List.of("x.*"))))),
                new ArrayList<>(List.of(new SortKey(NAME, ValueType.STRING, true))));

        assertEquals(query, sameQuery);
        assertEquals(query.hashCode(), sameQuery.hashCode());
    }

    @SuppressWarnings("unchecked")
    private static List<String> strings(List<Object> list) {
        return (List<String>) (List<?>) list;
    }

    @SuppressWarnings("unchecked")
    private static List<QueryFilter> filters(List<Object> list) {
        return (List<QueryFilter>) (List<?>) list;
    }

    @SuppressWarnings("unchecked")
    private static List<SortKey> sortKeys(List<Object> list) {
        return (List<SortKey>) (List<?>) list;
    }
}
