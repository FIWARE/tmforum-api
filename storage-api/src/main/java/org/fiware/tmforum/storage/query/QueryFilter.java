package org.fiware.tmforum.storage.query;

import java.util.List;
import java.util.Objects;

/**
 * Filter tree of an {@link EntityQuery}.
 */
public sealed interface QueryFilter {

    /**
     * Matches when the attribute compares true against any of the OR-ed values. Each value is one exact literal
     * that no renderer splits further and that must be a valid literal of {@code valueType}. A {@code REGEX}
     * comparison carries exactly one pattern and is always of type {@code STRING}; a {@code BOOLEAN} one only
     * supports {@code EQUALS}.
     */
    record Comparison(AttributePath attribute, ComparisonOperator operator, ValueType valueType, List<String> values)
            implements QueryFilter {

        public Comparison {
            Objects.requireNonNull(attribute, "attribute");
            Objects.requireNonNull(operator, "operator");
            Objects.requireNonNull(valueType, "valueType");
            values = requireNonEmpty(values, "values");
            if (operator == ComparisonOperator.REGEX) {
                if (values.size() != 1) {
                    throw new IllegalArgumentException("A regex comparison carries exactly one pattern: " + values);
                }
                if (valueType != ValueType.STRING) {
                    throw new IllegalArgumentException("A regex comparison must be of type STRING, was " + valueType);
                }
            }
            if (valueType == ValueType.BOOLEAN && operator != ComparisonOperator.EQUALS) {
                throw new IllegalArgumentException("A boolean comparison only supports EQUALS, was " + operator);
            }
            for (String value : values) {
                if (!Literals.isLiteralOf(valueType, value)) {
                    throw new IllegalArgumentException(value + " is not a valid " + valueType + " literal.");
                }
            }
        }
    }

    /**
     * Matches when every child matches.
     */
    record AllOf(List<QueryFilter> filters) implements QueryFilter {

        public AllOf {
            filters = requireNonEmpty(filters, "filters");
        }
    }

    /**
     * Matches when at least one child matches.
     */
    record AnyOf(List<QueryFilter> filters) implements QueryFilter {

        public AnyOf {
            filters = requireNonEmpty(filters, "filters");
        }
    }

    private static <T> List<T> requireNonEmpty(List<T> list, String name) {
        Objects.requireNonNull(list, name);
        List<T> copy = List.copyOf(list);
        if (copy.isEmpty()) {
            throw new IllegalArgumentException(name + " must not be empty.");
        }
        return copy;
    }
}
