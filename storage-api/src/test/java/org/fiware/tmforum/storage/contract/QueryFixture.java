package org.fiware.tmforum.storage.contract;

import org.fiware.tmforum.storage.query.AttributePath;
import org.fiware.tmforum.storage.query.ValueType;

import java.net.URI;
import java.util.Map;
import java.util.Set;

/**
 * Builds entities with chosen values at the attributes the query, sort and paging cases filter and order on. The
 * cases build the {@code EntityQuery} from {@link #path} themselves, since {@code storage-api} cannot see the parser.
 */
public interface QueryFixture<T> extends EntityFixture<T> {

    /**
     * The kinds of attribute a query case addresses, with the value type a parsed filter would give them and the
     * Java type {@link #newEntity(URI, Map)} takes for each.
     */
    enum Attribute {
        /** A declared string; takes a {@code String}. */
        STRING(ValueType.STRING),
        /** A declared integer; takes an {@code Integer}. */
        NUMBER(ValueType.NUMBER),
        /** Takes a {@code Boolean}. */
        BOOLEAN(ValueType.BOOLEAN),
        /** A declared date-time; takes an {@code Instant}. */
        DATE_TIME(ValueType.DATE_TIME),
        /** A string inside an object attribute; takes a {@code String}. */
        NESTED_STRING(ValueType.STRING),
        /** The id of each reference in a reference list; takes a {@code List<URI>}. */
        REFERENCE_ID(ValueType.STRING),
        /** A list of plain strings; takes a {@code List<String>}. */
        STRING_LIST(ValueType.STRING),
        /** An extension (undeclared) property; takes a {@code String} or an {@code Integer}. */
        EXTENSION(ValueType.STRING);

        private final ValueType valueType;

        Attribute(ValueType valueType) {
            this.valueType = valueType;
        }

        public ValueType valueType() {
            return valueType;
        }

        public boolean isList() {
            return this == REFERENCE_ID || this == STRING_LIST;
        }
    }

    /**
     * The attributes the class holds; a case needing another one runs on another fixture or skips.
     */
    Set<Attribute> attributes();

    AttributePath path(Attribute attribute);

    /**
     * An entity holding the given values and no other of the {@link #attributes}.
     */
    T newEntity(URI id, Map<Attribute, Object> values);

    @Override
    default T newEntity(URI id) {
        return newEntity(id, Map.of());
    }
}
