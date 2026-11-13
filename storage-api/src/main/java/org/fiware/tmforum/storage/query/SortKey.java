package org.fiware.tmforum.storage.query;

import java.util.Objects;

/**
 * One sort criterion. A null {@code valueType} means untyped: renderers order by the raw stored value. It is always
 * null for an undeclared (extension) attribute, since a sort has no literal to infer a type from.
 */
public record SortKey(AttributePath attribute, ValueType valueType, boolean descending) {

    public SortKey {
        Objects.requireNonNull(attribute, "attribute");
        if (!attribute.declared() && valueType != null) {
            throw new IllegalArgumentException("An undeclared sort attribute has no value type, was " + valueType);
        }
    }
}
