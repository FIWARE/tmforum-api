package org.fiware.tmforum.storage.postgresql;

import io.r2dbc.spi.Connection;
import io.r2dbc.spi.ConnectionFactory;
import io.r2dbc.spi.R2dbcBadGrammarException;
import io.r2dbc.spi.R2dbcDataIntegrityViolationException;
import io.r2dbc.spi.R2dbcNonTransientResourceException;
import io.r2dbc.spi.R2dbcTimeoutException;
import io.r2dbc.spi.Result;
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
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import tools.jackson.databind.json.JsonMapper;

import java.net.URI;
import java.util.function.Function;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.RETURNS_SELF;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

/**
 * Failure handling of the PostgreSQL repository against a mocked connection factory.
 */
class PostgresEntityRepositoryTest {

    private static final String CATALOG_ID = "urn:ngsi-ld:" + TestCatalog.TYPE + ":catalog-1";
    private static final String DRIVER_MESSAGE = "driver failure";
    private static final String UNDEFINED_TABLE_STATE = "42P01";
    private static final String NUL_IN_JSONB_STATE = "22P05";
    private static final String QUERY = "name==catalog";
    private static final String ORDER_BY = "name";
    private static final int OFFSET = 0;
    private static final int LIMIT = 10;
    private static final long NO_ROWS = 0L;

    private ConnectionFactory connectionFactory;
    private Connection connection;
    private Statement statement;
    private PostgresEntityRepository repository;

    @BeforeEach
    void setUp() {
        connectionFactory = mock(ConnectionFactory.class);
        connection = mock(Connection.class);
        statement = mock(Statement.class, RETURNS_SELF);
        doReturn(Mono.just(connection)).when(connectionFactory).create();
        doReturn(statement).when(connection).createStatement(anyString());
        doReturn(Mono.empty()).when(connection).close();
        repository = new PostgresEntityRepository(connectionFactory,
                new PostgresDocumentMapper(JsonMapper.builder().build()));
    }

    static Stream<Arguments> statementFailures() {
        Function<PostgresEntityRepository, Mono<?>> get = repository -> repository.get(URI.create(CATALOG_ID),
                TestCatalog.class);
        Function<PostgresEntityRepository, Mono<?>> create = repository -> repository.createDomainEntity(
                new TestCatalog(CATALOG_ID));
        Function<PostgresEntityRepository, Mono<?>> delete = repository -> repository.deleteDomainEntity(
                URI.create(CATALOG_ID));
        Function<PostgresEntityRepository, Mono<?>> list = repository -> repository.findEntities(OFFSET, LIMIT,
                TestCatalog.class, null, null, TestCatalog.TYPE, null);
        Function<PostgresEntityRepository, Mono<?>> listPolymorphic = repository -> repository
                .findEntitiesPolymorphic(OFFSET, LIMIT, TestCatalog.TYPE, null, CATALOG_ID, null,
                        type -> TestCatalog.class);
        return Stream.of(
                Arguments.of(Named.of("get", get), new R2dbcNonTransientResourceException(DRIVER_MESSAGE),
                        StorageOperation.READ, StorageErrorReason.UNAVAILABLE),
                Arguments.of(Named.of("create", create),
                        new R2dbcDataIntegrityViolationException(DRIVER_MESSAGE, PostgresFailures.UNIQUE_VIOLATION),
                        StorageOperation.CREATE, StorageErrorReason.CONFLICT),
                Arguments.of(Named.of("create", create),
                        new R2dbcBadGrammarException(DRIVER_MESSAGE, NUL_IN_JSONB_STATE),
                        StorageOperation.CREATE, StorageErrorReason.INVALID_DATA),
                Arguments.of(Named.of("delete", delete), new R2dbcTimeoutException(DRIVER_MESSAGE),
                        StorageOperation.DELETE, StorageErrorReason.UNAVAILABLE),
                Arguments.of(Named.of("list", list),
                        new R2dbcBadGrammarException(DRIVER_MESSAGE, UNDEFINED_TABLE_STATE),
                        StorageOperation.LIST, StorageErrorReason.UNKNOWN),
                Arguments.of(Named.of("polymorphic list", listPolymorphic),
                        new R2dbcNonTransientResourceException(DRIVER_MESSAGE),
                        StorageOperation.LIST, StorageErrorReason.UNAVAILABLE));
    }

    @ParameterizedTest(name = "{0}: {1}")
    @MethodSource("statementFailures")
    void statementFailureIsTranslated(Function<PostgresEntityRepository, Mono<?>> call, Throwable failure,
                                      StorageOperation expectedOperation, StorageErrorReason expectedReason) {
        doReturn(Mono.error(failure)).when(statement).execute();

        StorageException exception = assertStorageFailure(call.apply(repository));

        assertEquals(expectedOperation, exception.operation(), "The failed operation should be reported.");
        assertEquals(expectedReason, exception.reason(), "The failure should be mapped to the expected reason.");
        verify(connection).close();
    }

    @Test
    void unreachableDatabaseIsUnavailable() {
        doReturn(Mono.error(new R2dbcNonTransientResourceException(DRIVER_MESSAGE))).when(connectionFactory).create();

        StorageException exception = assertStorageFailure(repository.get(URI.create(CATALOG_ID), TestCatalog.class));

        assertEquals(StorageOperation.READ, exception.operation(), "The failed operation should be reported.");
        assertEquals(StorageErrorReason.UNAVAILABLE, exception.reason(), "No connection means unavailable.");
    }

    @Test
    void deletingMissingEntityIsNotFound() {
        Result result = mock(Result.class);
        doReturn(Mono.just(NO_ROWS)).when(result).getRowsUpdated();
        doReturn(Flux.just(result)).when(statement).execute();

        StorageException exception = assertStorageFailure(repository.deleteDomainEntity(URI.create(CATALOG_ID)));

        assertEquals(StorageOperation.DELETE, exception.operation(), "The failed operation should be reported.");
        assertEquals(StorageErrorReason.NOT_FOUND, exception.reason(), "No deleted row means not found.");
    }

    static Stream<Arguments> unsupportedListArguments() {
        return Stream.of(
                Arguments.of(Named.of("query", QUERY), null),
                Arguments.of(null, Named.of("orderBy", ORDER_BY)),
                Arguments.of(Named.of("query", QUERY), Named.of("orderBy", ORDER_BY)));
    }

    @ParameterizedTest(name = "{0} {1}")
    @MethodSource("unsupportedListArguments")
    void filteredListIsRejected(String query, String orderBy) {
        StorageException exception = assertStorageFailure(repository.findEntities(OFFSET, LIMIT, TestCatalog.class,
                query, null, TestCatalog.TYPE, orderBy));

        assertEquals(StorageOperation.LIST, exception.operation(), "The failed operation should be reported.");
        assertEquals(PostgresEntityRepository.LIST_NOT_SUPPORTED, exception.getMessage(),
                "The interim limitation should be named.");
        verifyNoInteractions(connectionFactory);
    }

    static Stream<String> unreadableIds() {
        return Stream.of("not-a-urn", "urn:ngsi-ld", "urn:ngsi-ld:other-type:catalog-1");
    }

    @ParameterizedTest
    @MethodSource("unreadableIds")
    void idWithoutTheClassTypeIsEmpty(String id) {
        assertNull(repository.get(URI.create(id), TestCatalog.class).block(),
                "An id of another or no type should not be found.");
        verifyNoInteractions(connectionFactory);
    }

    @Test
    void classWithoutMappingIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> repository.get(URI.create(CATALOG_ID), Object.class),
                "Reading requires a mapping-enabled class.");
    }

    private static StorageException assertStorageFailure(Mono<?> call) {
        Throwable failure = assertThrows(Throwable.class, call::block, "The call should fail.");
        return assertInstanceOf(StorageException.class, failure, "Only storage exceptions should leave the adapter.");
    }
}
