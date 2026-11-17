package org.fiware.tmforum.storage.query;

import java.util.regex.Pattern;

/**
 * Literal grammars per {@link ValueType}, the single source for the TMForum filter parser and the model's own checks.
 */
public final class Literals {

    /**
     * JSON number syntax. Unlike {@link Double#parseDouble} it rejects "NaN", "Infinity", "1d" and hex floats like
     * "0x1p1", none of which are valid in a query.
     */
    public static final Pattern NUMBER = Pattern.compile("-?(0|[1-9]\\d*)(\\.\\d+)?([eE][-+]?\\d+)?");

    /**
     * Lower-case only: the brokers read e.g. "True" as an unquoted string, which breaks the query.
     */
    public static final Pattern BOOLEAN = Pattern.compile("true|false");

    private Literals() {
    }

    /**
     * Whether the untrimmed value is a valid literal of the type; every string is a {@code STRING} literal.
     */
    public static boolean isLiteralOf(ValueType valueType, String value) {
        return switch (valueType) {
            case STRING -> true;
            case NUMBER -> NUMBER.matcher(value).matches();
            case BOOLEAN -> BOOLEAN.matcher(value).matches();
            case DATE_TIME -> DateTimeLiterals.parse(value).isPresent();
        };
    }
}
