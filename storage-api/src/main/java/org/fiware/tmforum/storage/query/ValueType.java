package org.fiware.tmforum.storage.query;

/**
 * Type a comparison or sort key is evaluated as. {@code DATE_TIME} marks a declared date-time attribute whose
 * literals all parse through {@link DateTimeLiterals}; the literal itself stays as the client wrote it.
 */
public enum ValueType {
    STRING,
    NUMBER,
    BOOLEAN,
    DATE_TIME
}
