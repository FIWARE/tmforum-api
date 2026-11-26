package org.fiware.tmforum.storage.postgresql;

import io.r2dbc.spi.Connection;
import io.r2dbc.spi.ConnectionFactory;
import io.r2dbc.spi.R2dbcBadGrammarException;
import io.r2dbc.spi.R2dbcDataIntegrityViolationException;
import io.r2dbc.spi.R2dbcNonTransientResourceException;
import io.r2dbc.spi.R2dbcTimeoutException;
import io.r2dbc.spi.Result;
import io.r2dbc.spi.Statement;
import org.fiware.tmforum.common.exception.QueryException;
import org.fiware.tmforum.storage.StorageErrorReason;
import org.fiware.tmforum.storage.StorageException;
import org.fiware.tmforum.storage.StorageOperation;
import org.fiware.tmforum.storage.postgresql.DocumentTestEntities.TestCatalog;
import org.fiware.tmforum.storage.postgresql.DocumentTestEntities.TestVersionedCatalog;
import org.fiware.tmforum.storage.query.AttributePath;
import org.fiware.tmforum.storage.query.ComparisonOperator;
import org.fiware.tmforum.storage.query.EntityQuery;
import org.fiware.tmforum.storage.query.QueryFilter;
import org.fiware.tmforum.storage.query.SortKey;
import org.fiware.tmforum.storage.query.ValueType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Named;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import java.lang.reflect.Type;
import java.net.URI;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.RETURNS_SELF;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
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
    private static final String NUL = "\u0000";
    private static final AttributePath NAME = new AttributePath(List.of("name"), true);
    private static final QueryFilter NAME_FILTER = new QueryFilter.Comparison(NAME, ComparisonOperator.EQUALS,
            ValueType.STRING, List.of("catalog"));
    private static final List<SortKey> NAME_SORT = List.of(new SortKey(NAME, ValueType.STRING, false));
    private static final QueryFilter NAME_REGEX = new QueryFilter.Comparison(NAME, ComparisonOperator.REGEX,
            ValueType.STRING, List.of("("));
    private static final AttributePath PRICE = new AttributePath(List.of("price", "value"), true);
    private static final String INVALID_REGULAR_EXPRESSION_STATE = "2201B";
    private static final String COMPILE_PATTERN = "SELECT '' ~ $1";
    private static final String LIST_STATEMENT_PREFIX = "SELECT type, data::text";
    private static final String OUT_OF_RANGE_NUMBER = "1e200000";
    // types, ids, the filter's path and values, the sort's path
    private static final int LIMIT_INDEX = 5;
    private static final int OFFSET_INDEX = 6;
    private static final String FILTERED_PAGE_SUFFIX = " LIMIT $6 OFFSET $7";
    private static final int PAST_THE_END = 20;
    private static final int OFFSET = 0;
    private static final int LIMIT = 10;
    private static final long NO_ROWS = 0L;
    private static final String VERSIONED_ID = "urn:ngsi-ld:" + TestVersionedCatalog.TYPE + ":catalog-1";
    private static final String VERSION = "1.0";
    private static final String VERSION_ATTRIBUTE = "version";
    // id, type, data, created_at, updated_at, version
    private static final int INSERT_VERSION_INDEX = 5;
    // id, patch, version
    private static final int UPDATE_VERSION_INDEX = 2;

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
        Function<PostgresEntityRepository, Mono<?>> update = repository -> repository.updateDomainEntity(CATALOG_ID,
                new TestCatalog(CATALOG_ID));
        Function<PostgresEntityRepository, Mono<?>> delete = repository -> repository.deleteDomainEntity(
                URI.create(CATALOG_ID));
        Function<PostgresEntityRepository, Mono<?>> list = repository -> repository.findEntities(OFFSET, LIMIT,
                TestCatalog.class, null, null, TestCatalog.TYPE);
        Function<PostgresEntityRepository, Mono<?>> listPolymorphic = repository -> repository
                .findEntitiesPolymorphic(OFFSET, LIMIT, TestCatalog.TYPE, null, CATALOG_ID,
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
                Arguments.of(Named.of("update", update), new R2dbcTimeoutException(DRIVER_MESSAGE),
                        StorageOperation.UPDATE, StorageErrorReason.UNAVAILABLE),
                Arguments.of(Named.of("update", update),
                        new R2dbcBadGrammarException(DRIVER_MESSAGE, NUL_IN_JSONB_STATE),
                        StorageOperation.UPDATE, StorageErrorReason.INVALID_DATA),
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

    @Test
    void updatingMissingEntityIsNotFound() {
        Result result = mock(Result.class);
        doReturn(Flux.empty()).when(result).map(any(Function.class));
        doReturn(Flux.just(result)).when(statement).execute();

        StorageException exception = assertStorageFailure(repository.updateDomainEntity(CATALOG_ID,
                new TestCatalog(CATALOG_ID)));

        assertEquals(StorageOperation.UPDATE, exception.operation(), "The failed operation should be reported.");
        assertEquals(StorageErrorReason.NOT_FOUND, exception.reason(), "No updated row means not found.");
    }

    @Test
    void updateCarryingNulIsRejectedBeforeTheStatement() {
        TestCatalog patch = new TestCatalog(CATALOG_ID);
        patch.setName(NUL);

        StorageException exception = assertStorageFailure(repository.updateDomainEntity(CATALOG_ID, patch));

        assertEquals(StorageOperation.UPDATE, exception.operation(), "The failed operation should be reported.");
        assertEquals(StorageErrorReason.INVALID_DATA, exception.reason(), "A NUL character is invalid data.");
        verifyNoInteractions(connectionFactory);
    }

    static Stream<Arguments> invalidPatternQueries() {
        return Stream.of(
                Arguments.of(Named.of("pattern compiled up front", new EntityQuery(TestCatalog.class, NAME_REGEX,
                        List.of())), COMPILE_PATTERN),
                Arguments.of(Named.of("failing page", new EntityQuery(TestCatalog.class, NAME_FILTER, NAME_SORT)),
                        LIST_STATEMENT_PREFIX));
    }

    // The first statement fails, so it must be the only one: an up-front failure never reaches the page.
    @ParameterizedTest(name = "{0}")
    @MethodSource("invalidPatternQueries")
    void invalidPatternIsAQueryException(EntityQuery query, String failingStatementPrefix) {
        doReturn(Mono.error(new R2dbcBadGrammarException(DRIVER_MESSAGE, INVALID_REGULAR_EXPRESSION_STATE)))
                .when(statement).execute();

        assertThrows(QueryException.class, () -> repository.findEntities(OFFSET, LIMIT, TestCatalog.class, query,
                null, TestCatalog.TYPE).block(), "A pattern PostgreSQL rejects is the client's error.");
        ArgumentCaptor<String> sql = ArgumentCaptor.forClass(String.class);
        verify(connection).createStatement(sql.capture());
        assertTrue(sql.getValue().startsWith(failingStatementPrefix),
                "The rejected pattern should fail the expected statement: " + sql.getValue());
        verify(connection).close();
    }

    static Stream<Arguments> unrenderableQueries() {
        return Stream.of(
                Arguments.of(Named.of("NUL in a value", new QueryFilter.Comparison(NAME, ComparisonOperator.EQUALS,
                        ValueType.STRING, List.of(NUL)))),
                Arguments.of(Named.of("number out of range", new QueryFilter.Comparison(PRICE,
                        ComparisonOperator.EQUALS, ValueType.NUMBER, List.of(OUT_OF_RANGE_NUMBER)))));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("unrenderableQueries")
    void unrenderableQueryFailsBeforeAnyStatement(QueryFilter filter) {
        EntityQuery query = new EntityQuery(TestCatalog.class, filter, List.of());

        Mono<?> list = assertDoesNotThrow(() -> repository.findEntities(OFFSET, LIMIT, TestCatalog.class, query,
                null, TestCatalog.TYPE), "Rendering should fail as an error signal, not on assembly.");
        assertThrows(QueryException.class, list::block, "A query PostgreSQL can not run is the client's error.");
        verifyNoInteractions(connectionFactory);
    }

    @Test
    void introspectionFailureIsAStorageError() {
        ObjectMapper failingMapper = mock(ObjectMapper.class);
        doThrow(new IllegalStateException(DRIVER_MESSAGE)).when(failingMapper).constructType(any(Type.class));
        DocumentMapper failingDocuments = mock(DocumentMapper.class);
        doReturn(failingMapper).when(failingDocuments).objectMapper();
        PostgresDocumentMapper documentMapper = mock(PostgresDocumentMapper.class);
        doReturn(failingDocuments).when(documentMapper).storage();
        PostgresEntityRepository failingRepository = new PostgresEntityRepository(connectionFactory, documentMapper);
        EntityQuery query = new EntityQuery(TestCatalog.class, NAME_FILTER, List.of());

        Mono<?> list = assertDoesNotThrow(() -> failingRepository.findEntities(OFFSET, LIMIT, TestCatalog.class,
                query, null, TestCatalog.TYPE), "Rendering should fail as an error signal, not on assembly.");
        StorageException failure = assertThrows(StorageException.class, list::block,
                "A rendering failure should be mapped like any other list failure.");
        assertEquals(StorageOperation.LIST, failure.operation(), "The failed operation should be reported.");
        verifyNoInteractions(connectionFactory);
    }

    @Test
    void filteredPageNumbersPagingAfterIdsAndQuery() {
        doReturn(Flux.empty()).when(statement).execute();
        EntityQuery query = new EntityQuery(TestCatalog.class, NAME_FILTER, NAME_SORT);

        repository.findEntities(OFFSET, LIMIT, TestCatalog.class, query, CATALOG_ID, TestCatalog.TYPE).block();

        ArgumentCaptor<String> sql = ArgumentCaptor.forClass(String.class);
        verify(connection).createStatement(sql.capture());
        assertTrue(sql.getValue().endsWith(FILTERED_PAGE_SUFFIX),
                "LIMIT and OFFSET should follow the parameters of the ids, the filter and the sort: " + sql.getValue());
        verify(statement).bind(LIMIT_INDEX, LIMIT);
        verify(statement).bind(OFFSET_INDEX, OFFSET);
    }

    @Test
    void countPastTheEndKeepsTheFilter() {
        doReturn(Flux.empty()).when(statement).execute();
        EntityQuery query = new EntityQuery(TestCatalog.class, NAME_FILTER, NAME_SORT);

        repository.findEntities(PAST_THE_END, LIMIT, TestCatalog.class, query, null, TestCatalog.TYPE).block();

        ArgumentCaptor<String> sql = ArgumentCaptor.forClass(String.class);
        verify(connection, times(2)).createStatement(sql.capture());
        String page = sql.getAllValues().getFirst();
        String count = sql.getAllValues().getLast();
        String pagePredicate = page.substring(page.indexOf(" WHERE "), page.indexOf(" ORDER BY "));
        assertTrue(count.endsWith(pagePredicate), "The count should use the page's predicate: " + count);
        assertTrue(pagePredicate.contains("is_current"), "Only current rows should count: " + pagePredicate);
        verify(connection).close();
    }

    static Stream<Arguments> boundVersions() {
        TestCatalog unversioned = new TestCatalog(CATALOG_ID);
        unversioned.addAdditionalProperties(VERSION_ATTRIBUTE, VERSION);
        Function<Object, Function<PostgresEntityRepository, Mono<?>>> create =
                entity -> repository -> repository.createDomainEntity(entity);
        Function<Object, Function<PostgresEntityRepository, Mono<?>>> update =
                entity -> repository -> repository.updateDomainEntity(CATALOG_ID, entity);
        Consumer<Statement> insertsVersion = statement -> verify(statement)
                .bind(INSERT_VERSION_INDEX, VERSION);
        Consumer<Statement> insertsUnversioned = statement -> verify(statement)
                .bind(INSERT_VERSION_INDEX, PostgresEntityRepository.UNVERSIONED);
        Consumer<Statement> updatesVersion = statement -> verify(statement)
                .bind(UPDATE_VERSION_INDEX, VERSION);
        Consumer<Statement> keepsVersion = statement -> verify(statement)
                .bindNull(UPDATE_VERSION_INDEX, String.class);
        return Stream.of(
                Arguments.of("create versioned", create.apply(versioned(VERSION)), insertsVersion),
                Arguments.of("create without version", create.apply(versioned(null)), insertsUnversioned),
                Arguments.of("create unversioned", create.apply(unversioned), insertsUnversioned),
                Arguments.of("update versioned", update.apply(versioned(VERSION)), updatesVersion),
                Arguments.of("update without version", update.apply(versioned(null)), keepsVersion),
                Arguments.of("update unversioned", update.apply(unversioned), keepsVersion));
    }

    // an extension property called version must not label the row of an unversioned type
    @ParameterizedTest(name = "{0}")
    @MethodSource("boundVersions")
    void versionIsBoundOnlyForVersionedEntities(String description, Function<PostgresEntityRepository, Mono<?>> write,
                                                Consumer<Statement> verification) {
        doReturn(Flux.empty()).when(statement).execute();

        // an update that matches no row fails; only its bindings matter here
        write.apply(repository).onErrorComplete().block();

        verification.accept(statement);
    }

    private static TestVersionedCatalog versioned(String version) {
        TestVersionedCatalog catalog = new TestVersionedCatalog(VERSIONED_ID);
        catalog.setVersion(version);
        return catalog;
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

    /**
     * The type-segment check throws on a class without {@code @MappingEnabled}. Assembling a chain must not throw,
     * or the failure bypasses the error handling of every caller that only subscribes later; it arrives on
     * subscription, wrapped like every other failure of the adapter.
     */
    @Test
    void classWithoutMappingIsRejectedOnSubscription() {
        Mono<Object> read = assertDoesNotThrow(() -> repository.get(URI.create(CATALOG_ID), Object.class),
                "Assembling the read should not throw.");

        assertStorageFailure(read);
    }

    private static StorageException assertStorageFailure(Mono<?> call) {
        Throwable failure = assertThrows(Throwable.class, call::block, "The call should fail.");
        return assertInstanceOf(StorageException.class, failure, "Only storage exceptions should leave the adapter.");
    }
}
