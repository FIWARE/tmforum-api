package org.fiware.tmforum.storage.postgresql;

import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import io.r2dbc.spi.Connection;
import io.r2dbc.spi.ConnectionFactory;
import io.r2dbc.spi.Readable;
import jakarta.inject.Inject;
import org.fiware.tmforum.storage.EntityRepository;
import org.fiware.tmforum.storage.PagedResult;
import org.fiware.tmforum.storage.StorageErrorReason;
import org.fiware.tmforum.storage.StorageException;
import org.fiware.tmforum.storage.StorageOperation;
import org.fiware.tmforum.storage.postgresql.DocumentTestEntities.TestCatalog;
import org.fiware.tmforum.storage.postgresql.DocumentTestEntities.TestLogicalResource;
import org.fiware.tmforum.storage.postgresql.DocumentTestEntities.TestResource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static org.fiware.tmforum.storage.postgresql.PostgresStorage.QUALIFIED_ENTITY_TABLE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Get, create, update, delete and unfiltered list of the PostgreSQL repository against the k3s PostgreSQL.
 */
@MicronautTest(environments = PostgresStorage.ENVIRONMENT)
class PostgresEntityRepositoryIT {

    private static final String CATALOG_ID_PREFIX = "urn:ngsi-ld:" + TestCatalog.TYPE + ":catalog-";
    private static final String RESOURCE_ID = "urn:ngsi-ld:" + TestResource.TYPE + ":resource-1";
    private static final String LOGICAL_RESOURCE_ID = "urn:ngsi-ld:" + TestLogicalResource.TYPE + ":resource-2";
    private static final String NAME = "catalog name";
    private static final String UPDATED_NAME = "updated catalog name";
    private static final String DESCRIPTION = "catalog description";
    private static final List<String> STORED_KEYWORDS = List.of("stored");
    private static final List<String> FIRST_KEYWORDS = List.of("first-1", "first-2");
    private static final List<String> SECOND_KEYWORDS = List.of("second-1", "second-2", "second-3");
    // each round races two updates once; several rounds make an interleaving likely if one were possible
    private static final int CONCURRENT_UPDATE_ROUNDS = 20;
    private static final String NUL = "\u0000";
    private static final String UNKNOWN_TYPE = "unknown-type";
    private static final List<String> TEST_TYPES =
            List.of(TestCatalog.TYPE, TestResource.TYPE, TestLogicalResource.TYPE);
    private static final int CATALOG_COUNT = 3;
    private static final int PAGE_SIZE = 2;
    private static final Instant IMPORTED_CREATED_AT = Instant.parse("2020-01-02T03:04:05.123456Z");
    private static final Instant IMPORTED_UPDATED_AT = Instant.parse("2021-06-07T08:09:10.654321Z");

    @Inject
    private PostgresEntityRepository repository;

    @Inject
    private EntityRepository port;

    @Inject
    private ConnectionFactory connectionFactory;

    @BeforeEach
    void cleanUp() {
        query("DELETE FROM " + QUALIFIED_ENTITY_TABLE + " WHERE type = ANY($1)", row -> row,
                (Object) TEST_TYPES.toArray(String[]::new));
    }

    @Test
    void postgresRepositoryIsThePrimaryPort() {
        assertInstanceOf(PostgresEntityRepository.class, port,
                "The postgresql environment should select the PostgreSQL adapter.");
    }

    @Test
    void createdEntityIsRead() {
        TestCatalog catalog = catalog(1);
        catalog.setName(NAME);

        repository.createDomainEntity(catalog).block();

        assertEquals(catalog, repository.get(catalog.getId(), TestCatalog.class).block(),
                "The created entity should be read back unchanged.");
    }

    @Test
    void missingEntityIsEmpty() {
        assertNull(repository.get(catalog(1).getId(), TestCatalog.class).block(),
                "An entity that was never created should not be found.");
    }

    static Stream<Arguments> createFailures() {
        TestCatalog withNul = catalog(2);
        withNul.setName(NAME + NUL);
        return Stream.of(
                Arguments.of(catalog(1), StorageErrorReason.CONFLICT),
                Arguments.of(withNul, StorageErrorReason.INVALID_DATA));
    }

    @ParameterizedTest(name = "{1}")
    @MethodSource("createFailures")
    void createFailureIsReported(TestCatalog entity, StorageErrorReason expectedReason) {
        repository.createDomainEntity(catalog(1)).block();

        StorageException failure = assertThrows(StorageException.class,
                () -> repository.createDomainEntity(entity).block(), "The create should fail.");

        assertEquals(StorageOperation.CREATE, failure.operation(), "The failed operation should be reported.");
        assertEquals(expectedReason, failure.reason(), "The failure should be mapped to the expected reason.");
    }

    @Test
    void updateMergesOnlyTheCarriedAttributes() {
        TestCatalog stored = catalog(1);
        stored.setName(NAME);
        stored.setDescription(DESCRIPTION);
        stored.setKeywords(STORED_KEYWORDS);
        repository.createDomainEntity(stored).block();
        TestCatalog patch = catalog(1);
        patch.setName(UPDATED_NAME);
        patch.setKeywords(List.of());

        repository.updateDomainEntity(stored.getId().toString(), patch).block();

        TestCatalog expected = catalog(1);
        expected.setName(UPDATED_NAME);
        expected.setDescription(DESCRIPTION);
        assertEquals(expected, repository.get(stored.getId(), TestCatalog.class).block(),
                "Carried attributes should replace the stored ones, an emptied list is removed, the others stay.");
    }

    @Test
    void updateOfMissingEntityCreatesNothing() {
        TestCatalog patch = catalog(1);

        StorageException failure = assertThrows(StorageException.class,
                () -> repository.updateDomainEntity(patch.getId().toString(), patch).block(),
                "The update should fail.");

        assertEquals(StorageErrorReason.NOT_FOUND, failure.reason(), "A missing entity should not be found.");
        assertNull(repository.get(patch.getId(), TestCatalog.class).block(), "The update should not create.");
    }

    @Test
    void updateCarryingNulIsInvalidAndKeepsTheStoredEntity() {
        TestCatalog stored = catalog(1);
        stored.setName(NAME);
        repository.createDomainEntity(stored).block();
        TestCatalog patch = catalog(1);
        patch.setName(UPDATED_NAME + NUL);

        StorageException failure = assertThrows(StorageException.class,
                () -> repository.updateDomainEntity(stored.getId().toString(), patch).block(),
                "The update should fail.");

        assertEquals(StorageOperation.UPDATE, failure.operation(), "The failed operation should be reported.");
        assertEquals(StorageErrorReason.INVALID_DATA, failure.reason(), "A NUL character is invalid data.");
        assertEquals(stored, repository.get(stored.getId(), TestCatalog.class).block(),
                "The stored entity should be unchanged.");
    }

    @Test
    void concurrentListReplacementsKeepExactlyOneList() {
        TestCatalog stored = catalog(1);
        stored.setKeywords(STORED_KEYWORDS);
        repository.createDomainEntity(stored).block();
        String id = stored.getId().toString();

        for (int round = 0; round < CONCURRENT_UPDATE_ROUNDS; round++) {
            Mono.when(repository.updateDomainEntity(id, withKeywords(FIRST_KEYWORDS)),
                    repository.updateDomainEntity(id, withKeywords(SECOND_KEYWORDS))).block();

            List<String> keywords = repository.get(stored.getId(), TestCatalog.class).block().getKeywords();
            assertTrue(FIRST_KEYWORDS.equals(keywords) || SECOND_KEYWORDS.equals(keywords),
                    "Round " + round + " should end with one of the two lists, but was " + keywords);
        }
    }

    @Test
    void deletedEntityIsGone() {
        TestCatalog catalog = catalog(1);
        repository.createDomainEntity(catalog).block();

        repository.deleteDomainEntity(catalog.getId()).block();

        assertNull(repository.get(catalog.getId(), TestCatalog.class).block(), "The entity should be deleted.");
        StorageException failure = assertThrows(StorageException.class,
                () -> repository.deleteDomainEntity(catalog.getId()).block(), "A second delete should fail.");
        assertEquals(StorageErrorReason.NOT_FOUND, failure.reason(), "A missing entity should not be found.");
    }

    static Stream<Arguments> pages() {
        // offset, limit, ids, types, expected catalog numbers, expected total count
        String allIds = String.join(",", catalogId(1), catalogId(3));
        return Stream.of(
                Arguments.of(0, PAGE_SIZE, null, TestCatalog.TYPE, List.of(1, 2), CATALOG_COUNT),
                Arguments.of(PAGE_SIZE, PAGE_SIZE, null, TestCatalog.TYPE, List.of(3), CATALOG_COUNT),
                Arguments.of(null, null, null, TestCatalog.TYPE, List.of(1, 2, 3), CATALOG_COUNT),
                Arguments.of(CATALOG_COUNT + PAGE_SIZE, PAGE_SIZE, null, TestCatalog.TYPE, List.of(),
                        CATALOG_COUNT),
                Arguments.of(0, PAGE_SIZE, allIds, TestCatalog.TYPE, List.of(1, 3), 2),
                Arguments.of(PAGE_SIZE, PAGE_SIZE, allIds, TestCatalog.TYPE, List.of(), 2),
                Arguments.of(0, 0, null, TestCatalog.TYPE, List.of(), CATALOG_COUNT),
                Arguments.of(null, 0, allIds, TestCatalog.TYPE, List.of(), 2),
                Arguments.of(0, PAGE_SIZE, null, UNKNOWN_TYPE, List.of(), 0));
    }

    @ParameterizedTest(name = "offset {0}, limit {1}, ids {2}, types {3}")
    @MethodSource("pages")
    void listReturnsThePageInCreationOrder(Integer offset, Integer limit, String ids, String types,
                                           List<Integer> expectedNumbers, int expectedTotalCount) {
        IntStream.rangeClosed(1, CATALOG_COUNT).forEach(number -> repository.createDomainEntity(catalog(number))
                .block());

        PagedResult<TestCatalog> page = repository.findEntities(offset, limit, TestCatalog.class, null, ids, types,
                null).block();

        assertEquals(expectedNumbers.stream().map(PostgresEntityRepositoryIT::catalog).toList(), page.items(),
                "The page should hold the expected entities in creation order.");
        assertEquals(expectedTotalCount, page.totalCount(), "The total count should cover all matches.");
    }

    @Test
    void polymorphicListMapsEachRowByItsType() {
        TestResource resource = new TestResource(RESOURCE_ID);
        TestLogicalResource logicalResource = new TestLogicalResource(LOGICAL_RESOURCE_ID);
        repository.createDomainEntity(resource).block();
        repository.createDomainEntity(logicalResource).block();
        Map<String, Class<? extends TestResource>> classes = Map.of(
                TestResource.TYPE, TestResource.class, TestLogicalResource.TYPE, TestLogicalResource.class);

        PagedResult<TestResource> page = repository.<TestResource>findEntitiesPolymorphic(null, null,
                TestResource.TYPE + "," + TestLogicalResource.TYPE, null, null, null, classes::get).block();

        assertEquals(List.of(resource, logicalResource), page.items(), "Each row should be read as its own type.");
        assertEquals(TestLogicalResource.class, page.items().get(1).getClass(), "The sub-type should be kept.");
    }

    @Test
    void importKeepsBothTimestamps() {
        TestCatalog created = catalog(1);
        TestCatalog imported = catalog(2);
        repository.createDomainEntity(created).block();

        repository.importDomainEntity(imported, IMPORTED_CREATED_AT, IMPORTED_UPDATED_AT).block();

        assertEquals(List.of(List.of(IMPORTED_CREATED_AT, IMPORTED_UPDATED_AT)),
                query("SELECT created_at, updated_at FROM " + QUALIFIED_ENTITY_TABLE + " WHERE id = $1",
                        row -> List.of(row.get("created_at", Instant.class), row.get("updated_at", Instant.class)),
                        imported.getId().toString()),
                "The given timestamps should be stored.");
        assertEquals(List.of(imported, created),
                repository.findEntities(null, null, TestCatalog.class, null, null, TestCatalog.TYPE, null)
                        .block().items(),
                "An imported entity should list by its original creation time.");
    }

    private static TestCatalog withKeywords(List<String> keywords) {
        TestCatalog patch = catalog(1);
        patch.setKeywords(keywords);
        return patch;
    }

    private static String catalogId(int number) {
        return CATALOG_ID_PREFIX + number;
    }

    private static TestCatalog catalog(int number) {
        return new TestCatalog(catalogId(number));
    }

    private <T> List<T> query(String sql, Function<Readable, T> rowMapper, Object... bindings) {
        return Flux.usingWhen(
                        Mono.from(connectionFactory.create()),
                        connection -> {
                            var statement = connection.createStatement(sql);
                            for (int i = 0; i < bindings.length; i++) {
                                statement.bind(i, bindings[i]);
                            }
                            return Flux.from(statement.execute())
                                    .flatMap(result -> result.map(rowMapper::apply));
                        },
                        Connection::close)
                .collectList()
                .block();
    }
}
