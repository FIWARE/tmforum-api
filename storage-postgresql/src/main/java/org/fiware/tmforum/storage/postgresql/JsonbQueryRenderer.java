package org.fiware.tmforum.storage.postgresql;

import org.fiware.tmforum.common.exception.QueryException;
import org.fiware.tmforum.storage.query.AttributePath;
import org.fiware.tmforum.storage.query.ComparisonOperator;
import org.fiware.tmforum.storage.query.EntityQuery;
import org.fiware.tmforum.storage.query.QueryFilter;
import org.fiware.tmforum.storage.query.SortKey;
import org.fiware.tmforum.storage.query.ValueType;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Renders an {@link EntityQuery} into SQL over the {@code data} document of {@code tmforum.entity}. Every user value
 * and every jsonpath is a bound parameter. A value of the wrong JSON type at a path matches nothing and sorts as NULL,
 * so mixed or migrated data can not abort a list. {@code DATE_TIME} compares as the stored strings, as on the brokers.
 */
final class JsonbQueryRenderer {

    /**
     * The filter of a query without one.
     */
    static final String NO_FILTER = "TRUE";

    private static final String ORDER_BY = "ORDER BY ";
    // keeps pages stable for equal sort values
    private static final String TIEBREAKER = "created_at, id";
    private static final String ASCENDING = " ASC NULLS LAST";
    private static final String DESCENDING = " DESC NULLS LAST";
    private static final String LIST_SEPARATOR = ", ";
    private static final String AND = " AND ";
    private static final String OR = " OR ";
    private static final String GROUP = "(%s)";

    private static final String PARAMETER = "$%d";
    private static final String MATCH_ANY_VALUE =
            "EXISTS (SELECT 1 FROM jsonb_path_query(data, %s::jsonpath) AS v(value) WHERE %s)";
    private static final String MATCHED_VALUE = "v.value";
    private static final String FIRST_VALUE = "jsonb_path_query_first(data, %s::jsonpath)";
    private static final String COMPARE_TO_ANY = "%s %s ANY(%s::%s[])";
    private static final String REGEX_MATCH = "%s ~ %s";

    // CASE, not AND: PostgreSQL does not promise to evaluate the type check before the cast
    private static final String TEXT_VALUE = "CASE WHEN jsonb_typeof(%1$s) = 'string' THEN %1$s #>> '{}' END";
    private static final String NUMBER_VALUE = "CASE WHEN jsonb_typeof(%1$s) = 'number' THEN (%1$s)::numeric END";
    private static final String BOOLEAN_VALUE = "CASE WHEN jsonb_typeof(%1$s) = 'boolean' THEN (%1$s)::boolean END";
    // byte order, the closest to the brokers' string order
    private static final String BYTE_ORDER = "(%s) COLLATE \"C\"";
    private static final String TEXT_TYPE = "text";
    private static final String NUMBER_TYPE = "numeric";
    private static final String BOOLEAN_TYPE = "boolean";

    private static final String JSONPATH_ROOT = "$";
    // in lax mode [*] unwraps an array and wraps a scalar, so a list matches when any element does
    private static final String JSONPATH_MEMBER = ".\"%s\"[*]";
    private static final char QUOTE = '"';
    private static final char BACKSLASH = '\\';
    private static final char FIRST_PRINTABLE = ' ';
    private static final String UNICODE_ESCAPE = "\\u%04x";

    // PostgreSQL's numeric limits; a literal beyond them fails the whole statement (SQLSTATE 22003)
    private static final long MAX_NUMERIC_INTEGER_DIGITS = 131_072;
    private static final int MAX_NUMERIC_FRACTION_DIGITS = 16_383;
    private static final String NUMBER_OUT_OF_RANGE = "Value '%s' of '%s' is outside the supported number range.";
    // neither jsonpath nor text input takes a NUL, and NullCharacterValidator keeps it out of every document
    private static final String NUL_IN_QUERY = "Filter or sort on '%s' contains the NUL character ("
            + NullCharacterValidator.NUL_ESCAPED + "), which no stored value can hold.";
    private static final String NUL = String.valueOf(NullCharacterValidator.NUL);
    private static final String PATH_SEPARATOR = ".";

    private final DocumentPathResolver pathResolver;

    JsonbQueryRenderer(DocumentPathResolver pathResolver) {
        this.pathResolver = pathResolver;
    }

    /**
     * The condition on a row, {@link #NO_FILTER} without a filter. Its parameters start at {@code firstParameter}.
     *
     * @throws QueryException for a NUL character or a number PostgreSQL cannot compare
     */
    SqlFragment filter(EntityQuery query, int firstParameter) {
        if (query.filter() == null) {
            return new SqlFragment(NO_FILTER, List.of());
        }
        Parameters parameters = new Parameters(firstParameter);
        String sql = render(query.queryClass(), query.filter(), parameters);
        return new SqlFragment(sql, parameters.bindings);
    }

    /**
     * The ORDER BY clause, ending in the creation-order tiebreaker, which alone orders a null query. Its parameters
     * start at {@code firstParameter}.
     *
     * @throws QueryException for a NUL character in a sort key
     */
    SqlFragment orderBy(EntityQuery query, int firstParameter) {
        Parameters parameters = new Parameters(firstParameter);
        List<String> keys = new ArrayList<>();
        List<SortKey> sort = query == null ? List.of() : query.sort();
        for (SortKey key : sort) {
            rejectNul(key.attribute(), List.of());
            String value = String.format(FIRST_VALUE, parameters.add(jsonPath(query.queryClass(), key.attribute())));
            keys.add(sortValue(key.valueType(), value) + (key.descending() ? DESCENDING : ASCENDING));
        }
        keys.add(TIEBREAKER);
        return new SqlFragment(ORDER_BY + String.join(LIST_SEPARATOR, keys), parameters.bindings);
    }

    private String render(Class<?> queryClass, QueryFilter filter, Parameters parameters) {
        return switch (filter) {
            case QueryFilter.Comparison comparison -> comparison(queryClass, comparison, parameters);
            case QueryFilter.AllOf allOf -> join(queryClass, allOf.filters(), AND, parameters);
            case QueryFilter.AnyOf anyOf -> join(queryClass, anyOf.filters(), OR, parameters);
        };
    }

    private String join(Class<?> queryClass, List<QueryFilter> filters, String operator, Parameters parameters) {
        return String.format(GROUP, filters.stream()
                .map(child -> render(queryClass, child, parameters))
                .collect(Collectors.joining(operator)));
    }

    private String comparison(Class<?> queryClass, QueryFilter.Comparison comparison, Parameters parameters) {
        rejectNul(comparison.attribute(), comparison.values());
        String path = parameters.add(jsonPath(queryClass, comparison.attribute()));
        return String.format(MATCH_ANY_VALUE, path, predicate(comparison, parameters));
    }

    private static String predicate(QueryFilter.Comparison comparison, Parameters parameters) {
        if (comparison.operator() == ComparisonOperator.REGEX) {
            // no COLLATE: under "C" the regex character classes would only know ASCII
            return String.format(REGEX_MATCH, String.format(TEXT_VALUE, MATCHED_VALUE),
                    parameters.add(comparison.values().getFirst()));
        }
        List<String> values = comparison.values();
        return switch (comparison.valueType()) {
            case STRING, DATE_TIME -> compareToAny(textValue(MATCHED_VALUE), comparison.operator(),
                    parameters.add(values.toArray(String[]::new)), TEXT_TYPE);
            case NUMBER -> compareToAny(String.format(NUMBER_VALUE, MATCHED_VALUE), comparison.operator(),
                    parameters.add(values.stream().map(value -> number(comparison.attribute(), value))
                            .toArray(BigDecimal[]::new)), NUMBER_TYPE);
            case BOOLEAN -> compareToAny(String.format(BOOLEAN_VALUE, MATCHED_VALUE), comparison.operator(),
                    parameters.add(values.stream().map(Boolean::valueOf).toArray(Boolean[]::new)), BOOLEAN_TYPE);
        };
    }

    private static BigDecimal number(AttributePath attribute, String literal) {
        try {
            // trailing zeros do not change the value but count against the fraction digits
            BigDecimal number = new BigDecimal(literal).stripTrailingZeros();
            if ((long) number.precision() - number.scale() <= MAX_NUMERIC_INTEGER_DIGITS
                    && number.scale() <= MAX_NUMERIC_FRACTION_DIGITS) {
                return number;
            }
        } catch (NumberFormatException | ArithmeticException e) {
            // an exponent beyond the int range of BigDecimal
        }
        throw new QueryException(String.format(NUMBER_OUT_OF_RANGE, literal, name(attribute)));
    }

    private static void rejectNul(AttributePath attribute, List<String> values) {
        if (attribute.segments().stream().anyMatch(segment -> segment.contains(NUL))
                || values.stream().anyMatch(value -> value.contains(NUL))) {
            throw new QueryException(String.format(NUL_IN_QUERY, name(attribute)));
        }
    }

    private static String name(AttributePath attribute) {
        return String.join(PATH_SEPARATOR, attribute.segments()).replace(NUL, NullCharacterValidator.NUL_ESCAPED);
    }

    private static String compareToAny(String value, ComparisonOperator operator, String parameter, String type) {
        return String.format(COMPARE_TO_ANY, value, sqlOperator(operator), parameter, type);
    }

    private static String sqlOperator(ComparisonOperator operator) {
        return switch (operator) {
            case EQUALS -> "=";
            case GREATER_THAN -> ">";
            case GREATER_THAN_EQUALS -> ">=";
            case LESS_THAN -> "<";
            case LESS_THAN_EQUALS -> "<=";
            case REGEX -> throw new IllegalArgumentException("A regex is no comparison operator.");
        };
    }

    // an untyped (undeclared) key orders by the raw jsonb, by PostgreSQL's cross-type jsonb order
    private static String sortValue(ValueType valueType, String value) {
        if (valueType == null) {
            return value;
        }
        return switch (valueType) {
            case STRING, DATE_TIME -> textValue(value);
            case NUMBER -> String.format(NUMBER_VALUE, value);
            case BOOLEAN -> String.format(BOOLEAN_VALUE, value);
        };
    }

    private static String textValue(String value) {
        return String.format(BYTE_ORDER, String.format(TEXT_VALUE, value));
    }

    private String jsonPath(Class<?> queryClass, AttributePath attribute) {
        StringBuilder path = new StringBuilder(JSONPATH_ROOT);
        for (String key : pathResolver.resolve(queryClass, attribute)) {
            path.append(String.format(JSONPATH_MEMBER, escape(key)));
        }
        return path.toString();
    }

    // a jsonpath string literal takes JSON escapes
    private static String escape(String key) {
        StringBuilder escaped = new StringBuilder(key.length());
        for (char c : key.toCharArray()) {
            if (c == QUOTE || c == BACKSLASH) {
                escaped.append(BACKSLASH).append(c);
            } else if (c < FIRST_PRINTABLE) {
                escaped.append(String.format(UNICODE_ESCAPE, (int) c));
            } else {
                escaped.append(c);
            }
        }
        return escaped.toString();
    }

    private static final class Parameters {

        private final List<Object> bindings = new ArrayList<>();
        private final int first;

        private Parameters(int first) {
            this.first = first;
        }

        private String add(Object binding) {
            bindings.add(binding);
            return String.format(PARAMETER, first + bindings.size() - 1);
        }
    }
}
