package org.fiware.tmforum.storage.postgresql;

import io.r2dbc.spi.R2dbcException;
import io.r2dbc.spi.R2dbcNonTransientResourceException;
import io.r2dbc.spi.R2dbcTimeoutException;
import io.r2dbc.spi.R2dbcTransientResourceException;
import org.fiware.tmforum.storage.StorageErrorReason;
import org.fiware.tmforum.storage.StorageException;
import org.fiware.tmforum.storage.StorageOperation;

import java.io.IOException;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeoutException;

/**
 * Translates database failures into {@link StorageException}s, so that no R2DBC error leaves the adapter.
 */
final class PostgresFailures {

    static final String UNIQUE_VIOLATION = "23505";
    // NUL in the document: 22P05 for an escaped \u0000 in jsonb, 22021 for a raw NUL byte in a text parameter
    static final Set<String> INVALID_CHARACTER_STATES = Set.of("22P05", "22021");
    static final String CONNECTION_EXCEPTION_CLASS = "08";

    private PostgresFailures() {
    }

    /**
     * Wraps {@code failure} with {@code operation} and {@code message}; a {@link StorageException} passes unchanged.
     */
    static StorageException failure(StorageOperation operation, String message, Throwable failure) {
        if (failure instanceof StorageException storageException) {
            return storageException;
        }
        return new StorageException(operation, reasonOf(failure), message, failure);
    }

    /**
     * UNAVAILABLE when the database gave no answer, CONFLICT for a unique violation, INVALID_DATA for content
     * PostgreSQL cannot store, UNKNOWN otherwise.
     */
    static StorageErrorReason reasonOf(Throwable failure) {
        if (isUnavailable(failure)) {
            return StorageErrorReason.UNAVAILABLE;
        }
        String sqlState = sqlStateOf(failure).orElse("");
        if (sqlState.equals(UNIQUE_VIOLATION)) {
            return StorageErrorReason.CONFLICT;
        }
        if (INVALID_CHARACTER_STATES.contains(sqlState)) {
            return StorageErrorReason.INVALID_DATA;
        }
        return StorageErrorReason.UNKNOWN;
    }

    // the driver wraps transport errors, so the whole cause chain counts
    private static boolean isUnavailable(Throwable failure) {
        for (Throwable cause = failure; cause != null; cause = cause.getCause()) {
            if (cause instanceof StorageException storageException) {
                return storageException.reason() == StorageErrorReason.UNAVAILABLE;
            }
            if (cause instanceof R2dbcNonTransientResourceException
                    || cause instanceof R2dbcTransientResourceException
                    || cause instanceof R2dbcTimeoutException
                    || cause instanceof TimeoutException
                    || cause instanceof IOException) {
                return true;
            }
            if (cause instanceof R2dbcException r2dbcException && r2dbcException.getSqlState() != null
                    && r2dbcException.getSqlState().startsWith(CONNECTION_EXCEPTION_CLASS)) {
                return true;
            }
        }
        return false;
    }

    private static Optional<String> sqlStateOf(Throwable failure) {
        for (Throwable cause = failure; cause != null; cause = cause.getCause()) {
            if (cause instanceof R2dbcException r2dbcException && r2dbcException.getSqlState() != null) {
                return Optional.of(r2dbcException.getSqlState());
            }
        }
        return Optional.empty();
    }
}
