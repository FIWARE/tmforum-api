package org.fiware.tmforum.storage.postgresql;

import io.r2dbc.spi.R2dbcBadGrammarException;
import io.r2dbc.spi.R2dbcDataIntegrityViolationException;
import io.r2dbc.spi.R2dbcNonTransientResourceException;
import io.r2dbc.spi.R2dbcPermissionDeniedException;
import io.r2dbc.spi.R2dbcTimeoutException;
import io.r2dbc.spi.R2dbcTransientResourceException;
import org.fiware.tmforum.storage.StorageErrorReason;
import org.fiware.tmforum.storage.StorageException;
import org.fiware.tmforum.storage.StorageOperation;
import org.junit.jupiter.api.Named;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.IOException;
import java.util.concurrent.TimeoutException;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

class PostgresFailuresTest {

    private static final String MESSAGE = "Was not able to do it.";
    private static final String DRIVER_MESSAGE = "driver failure";
    private static final String CONNECTION_FAILURE_STATE = "08006";
    private static final String UNDEFINED_TABLE_STATE = "42P01";
    private static final String NUL_IN_JSONB_STATE = "22P05";
    private static final String NUL_IN_TEXT_STATE = "22021";

    static Stream<Arguments> failures() {
        return Stream.of(
                Arguments.of(Named.of("non-transient resource",
                        new R2dbcNonTransientResourceException(DRIVER_MESSAGE)), StorageErrorReason.UNAVAILABLE),
                Arguments.of(Named.of("transient resource",
                        new R2dbcTransientResourceException(DRIVER_MESSAGE)), StorageErrorReason.UNAVAILABLE),
                Arguments.of(Named.of("driver timeout", new R2dbcTimeoutException(DRIVER_MESSAGE)),
                        StorageErrorReason.UNAVAILABLE),
                Arguments.of(Named.of("reactor timeout", new TimeoutException(DRIVER_MESSAGE)),
                        StorageErrorReason.UNAVAILABLE),
                Arguments.of(Named.of("wrapped io failure",
                        new IllegalStateException(new IOException(DRIVER_MESSAGE))), StorageErrorReason.UNAVAILABLE),
                Arguments.of(Named.of("connection exception state",
                        new R2dbcPermissionDeniedException(DRIVER_MESSAGE, CONNECTION_FAILURE_STATE)),
                        StorageErrorReason.UNAVAILABLE),
                Arguments.of(Named.of("unique violation", new R2dbcDataIntegrityViolationException(DRIVER_MESSAGE,
                        PostgresFailures.UNIQUE_VIOLATION)), StorageErrorReason.CONFLICT),
                Arguments.of(Named.of("NUL in jsonb",
                        new R2dbcBadGrammarException(DRIVER_MESSAGE, NUL_IN_JSONB_STATE)),
                        StorageErrorReason.INVALID_DATA),
                Arguments.of(Named.of("NUL in text",
                        new R2dbcBadGrammarException(DRIVER_MESSAGE, NUL_IN_TEXT_STATE)),
                        StorageErrorReason.INVALID_DATA),
                Arguments.of(Named.of("other state",
                        new R2dbcBadGrammarException(DRIVER_MESSAGE, UNDEFINED_TABLE_STATE)),
                        StorageErrorReason.UNKNOWN),
                Arguments.of(Named.of("no state", new IllegalStateException(DRIVER_MESSAGE)),
                        StorageErrorReason.UNKNOWN));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("failures")
    void failureIsMappedToReason(Throwable failure, StorageErrorReason expectedReason) {
        StorageException mapped = PostgresFailures.failure(StorageOperation.CREATE, MESSAGE, failure);

        assertEquals(expectedReason, mapped.reason(), "The failure should be mapped to the expected reason.");
        assertEquals(StorageOperation.CREATE, mapped.operation(), "The operation should be kept.");
        assertSame(failure, mapped.getCause(), "The original failure should be the cause.");
    }

    @Test
    void storageExceptionPassesUnchanged() {
        StorageException original = new StorageException(StorageOperation.READ, StorageErrorReason.INVALID_DATA,
                MESSAGE);

        assertSame(original, PostgresFailures.failure(StorageOperation.LIST, MESSAGE, original),
                "A storage exception should not be wrapped again.");
    }
}
