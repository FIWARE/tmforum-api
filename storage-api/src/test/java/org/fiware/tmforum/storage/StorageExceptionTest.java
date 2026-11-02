package org.fiware.tmforum.storage;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.Arrays;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

class StorageExceptionTest {

    private static final String MESSAGE = "storage failed";

    static Stream<Arguments> operationsAndReasons() {
        return Arrays.stream(StorageOperation.values())
                .flatMap(operation -> Arrays.stream(StorageErrorReason.values())
                        .map(reason -> Arguments.of(operation, reason)));
    }

    @ParameterizedTest
    @MethodSource("operationsAndReasons")
    void carriesOperationReasonAndCause(StorageOperation operation, StorageErrorReason reason) {
        Throwable cause = new IllegalStateException("backend error");

        StorageException exception = new StorageException(operation, reason, MESSAGE, cause);

        assertEquals(operation, exception.operation());
        assertEquals(reason, exception.reason());
        assertEquals(MESSAGE, exception.getMessage());
        assertSame(cause, exception.getCause());
    }

    @ParameterizedTest
    @MethodSource("operationsAndReasons")
    void hasNoCauseWhenNoneIsGiven(StorageOperation operation, StorageErrorReason reason) {
        StorageException exception = new StorageException(operation, reason, MESSAGE);

        assertEquals(operation, exception.operation());
        assertEquals(reason, exception.reason());
        assertNull(exception.getCause());
    }

    @Test
    void requiresOperationAndReason() {
        assertThrows(NullPointerException.class,
                () -> new StorageException(null, StorageErrorReason.UNKNOWN, MESSAGE));
        assertThrows(NullPointerException.class,
                () -> new StorageException(StorageOperation.READ, null, MESSAGE));
    }
}
