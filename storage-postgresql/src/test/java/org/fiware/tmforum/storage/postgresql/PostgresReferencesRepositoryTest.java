package org.fiware.tmforum.storage.postgresql;

import io.r2dbc.spi.Connection;
import io.r2dbc.spi.ConnectionFactory;
import io.r2dbc.spi.R2dbcBadGrammarException;
import io.r2dbc.spi.R2dbcNonTransientResourceException;
import io.r2dbc.spi.R2dbcTimeoutException;
import io.r2dbc.spi.Statement;
import org.fiware.tmforum.storage.StorageErrorReason;
import org.fiware.tmforum.storage.StorageException;
import org.fiware.tmforum.storage.StorageOperation;
import org.fiware.tmforum.storage.postgresql.DocumentTestEntities.TestCatalog;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Named;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.RETURNS_SELF;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

/**
 * Failure handling of the PostgreSQL reference repository against a mocked connection factory.
 */
class PostgresReferencesRepositoryTest {

    private static final String CATALOG_ID = "urn:ngsi-ld:" + TestCatalog.TYPE + ":catalog-1";
    private static final String DRIVER_MESSAGE = "driver failure";
    private static final String UNDEFINED_TABLE_STATE = "42P01";
    private static final Duration TIMEOUT = Duration.ofSeconds(5);

    private ConnectionFactory connectionFactory;
    private Connection connection;
    private Statement statement;
    private PostgresReferencesRepository repository;

    @BeforeEach
    void setUp() {
        connectionFactory = mock(ConnectionFactory.class);
        connection = mock(Connection.class);
        statement = mock(Statement.class, RETURNS_SELF);
        doReturn(Mono.just(connection)).when(connectionFactory).create();
        doReturn(statement).when(connection).createStatement(anyString());
        doReturn(Mono.empty()).when(connection).close();
        repository = new PostgresReferencesRepository(connectionFactory);
    }

    static Stream<Arguments> statementFailures() {
        Function<PostgresReferencesRepository, Mono<?>> exists = repository -> repository.referenceExists(CATALOG_ID,
                List.of(TestCatalog.TYPE));
        Function<PostgresReferencesRepository, Mono<?>> find = repository -> repository.findReference(CATALOG_ID);
        Function<PostgresReferencesRepository, Mono<?>> findAll = repository -> repository
                .findReferences(List.of(CATALOG_ID)).collectList();
        return Stream.of(
                Arguments.of(Named.of("referenceExists", exists), new R2dbcTimeoutException(DRIVER_MESSAGE),
                        StorageErrorReason.UNAVAILABLE),
                Arguments.of(Named.of("findReference", find), new R2dbcNonTransientResourceException(DRIVER_MESSAGE),
                        StorageErrorReason.UNAVAILABLE),
                Arguments.of(Named.of("findReferences", findAll),
                        new R2dbcBadGrammarException(DRIVER_MESSAGE, UNDEFINED_TABLE_STATE),
                        StorageErrorReason.UNKNOWN));
    }

    @ParameterizedTest(name = "{0}: {1}")
    @MethodSource("statementFailures")
    void statementFailureIsAReadFailure(Function<PostgresReferencesRepository, Mono<?>> call, Throwable failure,
                                        StorageErrorReason expectedReason) {
        doReturn(Mono.error(failure)).when(statement).execute();

        Throwable thrown = assertThrows(Throwable.class, () -> call.apply(repository).block(TIMEOUT),
                "The call should fail.");

        StorageException exception = assertInstanceOf(StorageException.class, thrown,
                "Only storage exceptions should leave the adapter.");
        assertEquals(StorageOperation.READ, exception.operation(), "Reference lookups are reads.");
        assertEquals(expectedReason, exception.reason(), "The failure should be mapped to the expected reason.");
        verify(connection).close();
    }

    @Test
    void noIdsNeedNoConnection() {
        assertEquals(List.of(), repository.findReferences(List.of()).collectList().block(TIMEOUT));

        verifyNoInteractions(connectionFactory);
    }
}
