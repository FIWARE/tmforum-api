package org.fiware.tmforum.storage;

import java.util.Objects;

/**
 * Failure of a port method, independent of the backend that produced it.
 */
public class StorageException extends RuntimeException {

    private final StorageOperation operation;
    private final StorageErrorReason reason;

    public StorageException(StorageOperation operation, StorageErrorReason reason, String message) {
        this(operation, reason, message, null);
    }

    /**
     * {@code cause} is the backend error, kept for logging only; rendering must not depend on its type.
     */
    public StorageException(StorageOperation operation, StorageErrorReason reason, String message, Throwable cause) {
        super(message, cause);
        this.operation = Objects.requireNonNull(operation, "operation");
        this.reason = Objects.requireNonNull(reason, "reason");
    }

    public StorageOperation operation() {
        return operation;
    }

    public StorageErrorReason reason() {
        return reason;
    }
}
