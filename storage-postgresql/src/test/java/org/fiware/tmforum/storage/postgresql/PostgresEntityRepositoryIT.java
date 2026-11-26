package org.fiware.tmforum.storage.postgresql;

import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import io.r2dbc.spi.Connection;
import io.r2dbc.spi.ConnectionFactory;
import io.r2dbc.spi.Readable;
import jakarta.inject.Inject;
import org.fiware.tmforum.common.domain.EntityWithId;
import org.fiware.tmforum.common.domain.Money;
import org.fiware.tmforum.common.exception.QueryException;
import org.fiware.tmforum.storage.EntityRepository;
import org.fiware.tmforum.storage.PagedResult;
import org.fiware.tmforum.storage.StorageErrorReason;
import org.fiware.tmforum.storage.StorageException;
import org.fiware.tmforum.storage.StorageOperation;
import org.fiware.tmforum.storage.postgresql.DocumentTestEntities.TestCatalog;
import org.fiware.tmforum.storage.postgresql.DocumentTestEntities.TestLogicalResource;
import org.fiware.tmforum.storage.postgresql.DocumentTestEntities.TestResource;
import org.fiware.tmforum.storage.postgresql.DocumentTestEntities.TestVersionedCatalog;
import org.fiware.tmforum.storage.query.AttributePath;
import org.fiware.tmforum.storage.query.ComparisonOperator;
import org.fiware.tmforum.storage.query.EntityQuery;
import org.fiware.tmforum.storage.query.QueryFilter;
import org.fiware.tmforum.storage.query.QueryFilter.AllOf;
import org.fiware.tmforum.storage.query.QueryFilter.AnyOf;
import org.fiware.tmforum.storage.query.QueryFilter.Comparison;
import org.fiware.tmforum.storage.query.SortKey;
import org.fiware.tmforum.storage.query.ValueType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static org.fiware.tmforum.storage.postgresql.PostgresStorage.QUALIFIED_ENTITY_TABLE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Get, create, update, delete and list (filtered, sorted and paged) of the PostgreSQL repository against the k3s
 * PostgreSQL.
 */
@MicronautTest(environments = PostgresStorage.ENVIRONMENT)
class PostgresEntityRepositoryIT {

    private static final String CATALOG_ID_PREFIX = "urn:ngsi-ld:" + TestCatalog.TYPE + ":catalog-";
    private static final String RESOURCE_ID = "urn:ngsi-ld:" + TestResource.TYPE + ":resource-1";
    private static final String LOGICAL_RESOURCE_ID = "urn:ngsi-ld:" + TestLogicalResource.TYPE + ":resource-2";
    private static final String NAME = "catalog name";
    private static final String UPDATED_NAME = "updated catalog name";
    private static final String DESCRIPTION = "catalog description";
    private static final String EXTENSION_KEY = "customField";
    private static final List<String> STORED_KEYWORDS = List.of("stored");
    private static final List<String> FIRST_KEYWORDS = List.of("first-1", "first-2");
    private static final List<String> SECOND_KEYWORDS = List.of("second-1", "second-2", "second-3");
    // each round races two updates once; several rounds make an interleaving likely if one were possible
    private static final int CONCURRENT_UPDATE_ROUNDS = 20;
    private static final String NUL = "\u0000";
    private static final String UNKNOWN_TYPE = "unknown-type";
    private static final List<String> TEST_TYPES =
            List.of(TestCatalog.TYPE, TestResource.TYPE, TestLogicalResource.TYPE, TestVersionedCatalog.TYPE);
    private static final int CATALOG_COUNT = 3;
    private static final int PAGE_SIZE = 2;
    private static final Instant IMPORTED_CREATED_AT = Instant.parse("2020-01-02T03:04:05.123456Z");
    private static final Instant IMPORTED_UPDATED_AT = Instant.parse("2021-06-07T08:09:10.654321Z");

    // seeded by seedQueryCatalogs, numbered in creation order; upper case sorts first in byte order
    private static final List<String> QUERY_NAMES = List.of("alpha", "Bravo", "charlie", "delta");
    private static final List<List<String>> QUERY_KEYWORDS =
            List.of(List.of("a", "shared"), List.of("b"), List.of("shared"), List.of("d"));
    // the fourth catalog has no price
    private static final List<Float> QUERY_PRICES = List.of(10f, 2.5f, 30f);
    private static final String PRICE_UNIT = "EUR";
    private static final String NAME_ATTRIBUTE = "name";
    private static final String KEYWORDS_ATTRIBUTE = "keywords";
    private static final String PRICE_ATTRIBUTE = "price.tmfValue";
    private static final String INVALID_PATTERN = "(";
    private static final int PAST_THE_END = 10;
    private static final String NON_CURRENT_VERSION = "previous";
    private static final String NON_CURRENT_NAME = "previous name";
    private static final String INSERT_NON_CURRENT = "INSERT INTO " + QUALIFIED_ENTITY_TABLE
            + " (id, version, is_current, type, data)"
            + " SELECT id, $2, false, type, data || jsonb_build_object('name', $3::text) FROM " + QUALIFIED_ENTITY_TABLE + " WHERE id = $1";
    private static final String RESOURCE_NAME = "resource";
    private static final String VERSIONED_ID = "urn:ngsi-ld:" + TestVersionedCatalog.TYPE + ":versioned-1";
    private static final String FIRST_VERSION = "1.0";
    private static final String SECOND_VERSION = "2.0";
    private static final String VERSION_ATTRIBUTE = "version";
    private static final long INITIAL_REVISION = 1L;
    private static final String SELECT_VERSION_COLUMNS =
            "SELECT version, revision, is_current FROM " + QUALIFIED_ENTITY_TABLE + " WHERE id = $1";
    private static final String LOGICAL_RESOURCE_NAME = "logical resource";

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
    void updateKeepsAnEmptiedExtensionList() {
        TestCatalog stored = catalog(1);
        stored.addAdditionalProperties(EXTENSION_KEY, STORED_KEYWORDS);
        repository.createDomainEntity(stored).block();
        TestCatalog patch = catalog(1);
        patch.addAdditionalProperties(EXTENSION_KEY, List.of());

        repository.updateDomainEntity(stored.getId().toString(), patch).block();

        TestCatalog expected = catalog(1);
        expected.addAdditionalProperties(EXTENSION_KEY, List.of());
        assertEquals(expected, repository.get(stored.getId(), TestCatalog.class).block(),
                "An emptied extension list is stored as [], like the NGSI-LD adapter stores it.");
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

    /**
     * The delete carries the types the calling endpoint owns, so a matching id of another type leaves its row alone
     * instead of deleting another endpoint's entity and writing its DELETE event.
     */
    @Test
    void deleteWithEventLeavesAnEntityOfAnotherTypeAlone() {
        TestCatalog catalog = catalog(1);
        repository.createDomainEntity(catalog).block();

        StorageException failure = assertThrows(StorageException.class,
                () -> repository.deleteWithEvent(catalog.getId(), List.of(TestResource.TYPE), type -> false).block(),
                "A delete of another endpoint's type should not find the entity.");

        assertEquals(StorageErrorReason.NOT_FOUND, failure.reason(),
                "An entity of a type the caller does not own should read as missing.");
        assertNotNull(repository.get(catalog.getId(), TestCatalog.class).block(),
                "The entity should still be stored.");
    }

    @Test
    void deleteWithEventRemovesAnEntityOfAnAcceptedType() {
        TestCatalog catalog = catalog(1);
        repository.createDomainEntity(catalog).block();

        repository.deleteWithEvent(catalog.getId(), List.of(TestCatalog.TYPE, TestResource.TYPE), type -> false)
                .block();

        assertNull(repository.get(catalog.getId(), TestCatalog.class).block(), "The entity should be deleted.");
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

        PagedResult<TestCatalog> page = repository.findEntities(offset, limit, TestCatalog.class, null, ids, types)
                .block();

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
                TestResource.TYPE + "," + TestLogicalResource.TYPE, null, null, classes::get).block();

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
                repository.findEntities(null, null, TestCatalog.class, null, null, TestCatalog.TYPE)
                        .block().items(),
                "An imported entity should list by its original creation time.");
    }

    static Stream<Arguments> filters() {
        return Stream.of(
                Arguments.of("equals", string(NAME_ATTRIBUTE, ComparisonOperator.EQUALS, "alpha"), List.of(1)),
                Arguments.of("any of several values",
                        string(NAME_ATTRIBUTE, ComparisonOperator.EQUALS, "alpha", "charlie"), List.of(1, 3)),
                Arguments.of("any list element", string(KEYWORDS_ATTRIBUTE, ComparisonOperator.EQUALS, "shared"),
                        List.of(1, 3)),
                Arguments.of("regex", string(NAME_ATTRIBUTE, ComparisonOperator.REGEX, "^[a-c]"), List.of(1, 3)),
                Arguments.of("regex not-equals idiom",
                        string(NAME_ATTRIBUTE, ComparisonOperator.REGEX, "^(?!alpha$).*$"), List.of(2, 3, 4)),
                Arguments.of("nested number", number(ComparisonOperator.GREATER_THAN, "5"), List.of(1, 3)),
                Arguments.of("and", new AllOf(List.of(string(KEYWORDS_ATTRIBUTE, ComparisonOperator.EQUALS, "shared"),
                        number(ComparisonOperator.GREATER_THAN, "15"))), List.of(3)),
                Arguments.of("or", new AnyOf(List.of(string(NAME_ATTRIBUTE, ComparisonOperator.EQUALS, "delta"),
                        number(ComparisonOperator.LESS_THAN, "5"))), List.of(2, 4)),
                Arguments.of("no match", string(NAME_ATTRIBUTE, ComparisonOperator.EQUALS, "echo"), List.of()));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("filters")
    void listIsFiltered(String description, QueryFilter filter, List<Integer> expectedNumbers) {
        List<TestCatalog> catalogs = seedQueryCatalogs();

        PagedResult<TestCatalog> page = listCatalogs(null, null, new EntityQuery(TestCatalog.class, filter,
                List.of()));

        assertEquals(numbered(catalogs, expectedNumbers), page.items(),
                "Only matches should be listed, in creation order.");
        assertEquals(expectedNumbers.size(), page.totalCount(), "The total count should cover the matches only.");
    }

    static Stream<Arguments> sorts() {
        QueryFilter shared = string(KEYWORDS_ATTRIBUTE, ComparisonOperator.EQUALS, "shared");
        return Stream.of(
                Arguments.of("string ascending, byte order", null, sortKey(NAME_ATTRIBUTE, ValueType.STRING, false),
                        List.of(2, 1, 3, 4)),
                Arguments.of("string descending", null, sortKey(NAME_ATTRIBUTE, ValueType.STRING, true),
                        List.of(4, 3, 1, 2)),
                Arguments.of("number ascending, missing last", null, sortKey(PRICE_ATTRIBUTE, ValueType.NUMBER, false),
                        List.of(2, 1, 3, 4)),
                Arguments.of("number descending, missing last", null,
                        sortKey(PRICE_ATTRIBUTE, ValueType.NUMBER, true), List.of(3, 1, 2, 4)),
                Arguments.of("filtered and sorted", shared, sortKey(PRICE_ATTRIBUTE, ValueType.NUMBER, true),
                        List.of(3, 1)));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("sorts")
    void listIsSorted(String description, QueryFilter filter, SortKey sortKey, List<Integer> expectedNumbers) {
        List<TestCatalog> catalogs = seedQueryCatalogs();

        PagedResult<TestCatalog> page = listCatalogs(null, null, new EntityQuery(TestCatalog.class, filter,
                List.of(sortKey)));

        assertEquals(numbered(catalogs, expectedNumbers), page.items(), "The matches should be in sort order.");
    }

    static Stream<Arguments> filteredPages() {
        // offset, limit, expected catalog numbers; three catalogs have a price, sorted 2, 1, 3
        return Stream.of(
                Arguments.of(0, PAGE_SIZE, List.of(2, 1)),
                Arguments.of(PAGE_SIZE, PAGE_SIZE, List.of(3)),
                Arguments.of(1, 1, List.of(1)),
                Arguments.of(PAST_THE_END, PAGE_SIZE, List.of()),
                Arguments.of(0, 0, List.of()),
                Arguments.of(null, 0, List.of()));
    }

    @ParameterizedTest(name = "offset {0}, limit {1}")
    @MethodSource("filteredPages")
    void filteredListIsPagedAndCountsEveryMatch(Integer offset, Integer limit, List<Integer> expectedNumbers) {
        List<TestCatalog> catalogs = seedQueryCatalogs();
        EntityQuery query = new EntityQuery(TestCatalog.class, number(ComparisonOperator.GREATER_THAN, "0"),
                List.of(sortKey(PRICE_ATTRIBUTE, ValueType.NUMBER, false)));

        PagedResult<TestCatalog> page = listCatalogs(offset, limit, query);

        assertEquals(numbered(catalogs, expectedNumbers), page.items(), "The page should hold the expected matches.");
        assertEquals(QUERY_PRICES.size(), page.totalCount(), "The total count should cover all matches.");
    }

    @Test
    void filteredListKeepsTheIds() {
        List<TestCatalog> catalogs = seedQueryCatalogs();
        EntityQuery query = new EntityQuery(TestCatalog.class,
                string(KEYWORDS_ATTRIBUTE, ComparisonOperator.EQUALS, "shared"), List.of());

        PagedResult<TestCatalog> page = repository.findEntities(null, null, TestCatalog.class, query,
                String.join(",", catalogId(2), catalogId(3)), TestCatalog.TYPE).block();

        assertEquals(numbered(catalogs, List.of(3)), page.items(), "Ids and filter should both apply.");
        assertEquals(1, page.totalCount(), "The total count should cover ids and filter.");
    }

    static Stream<Arguments> polymorphicFilters() {
        return Stream.of(
                Arguments.of(string(NAME_ATTRIBUTE, ComparisonOperator.EQUALS, LOGICAL_RESOURCE_NAME), List.of(1)),
                Arguments.of(string(NAME_ATTRIBUTE, ComparisonOperator.REGEX, "resource$"), List.of(0, 1)));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("polymorphicFilters")
    void polymorphicListIsFiltered(QueryFilter filter, List<Integer> expectedIndexes) {
        TestResource resource = new TestResource(RESOURCE_ID);
        resource.setName(RESOURCE_NAME);
        TestLogicalResource logicalResource = new TestLogicalResource(LOGICAL_RESOURCE_ID);
        logicalResource.setName(LOGICAL_RESOURCE_NAME);
        repository.createDomainEntity(resource).block();
        repository.createDomainEntity(logicalResource).block();
        List<TestResource> resources = List.of(resource, logicalResource);
        Map<String, Class<? extends TestResource>> classes = Map.of(
                TestResource.TYPE, TestResource.class, TestLogicalResource.TYPE, TestLogicalResource.class);

        PagedResult<TestResource> page = repository.<TestResource>findEntitiesPolymorphic(null, null,
                TestResource.TYPE + "," + TestLogicalResource.TYPE, new EntityQuery(TestResource.class, filter,
                        List.of()), null, classes::get).block();

        assertEquals(expectedIndexes.stream().map(resources::get).toList(), page.items(),
                "Each type should be filtered and read as its own type.");
    }

    @ParameterizedTest(name = "seeded: {0}")
    @ValueSource(booleans = {false, true})
    void invalidPatternIsAQueryExceptionWhateverTheData(boolean seeded) {
        if (seeded) {
            seedQueryCatalogs();
        }
        EntityQuery query = new EntityQuery(TestCatalog.class,
                string(NAME_ATTRIBUTE, ComparisonOperator.REGEX, INVALID_PATTERN), List.of());

        assertThrows(QueryException.class, () -> listCatalogs(0, PAGE_SIZE, query),
                "A pattern PostgreSQL rejects should be the client's error, even when no row reaches it.");
    }

    @Test
    void nonCurrentRowIsNeitherListedNorCounted() {
        TestCatalog current = catalog(1);
        current.setName(NAME);
        repository.createDomainEntity(current).block();
        query(INSERT_NON_CURRENT, row -> row, current.getId().toString(), NON_CURRENT_VERSION, NON_CURRENT_NAME);
        EntityQuery previousName = new EntityQuery(TestCatalog.class,
                string(NAME_ATTRIBUTE, ComparisonOperator.EQUALS, NON_CURRENT_NAME), List.of());

        assertEquals(List.of(current), listCatalogs(null, null, null).items(), "Only the current row should list.");
        assertEquals(1, listCatalogs(PAST_THE_END, PAGE_SIZE, null).totalCount(),
                "Only the current row should count.");
        assertEquals(List.of(), listCatalogs(null, null, previousName).items(),
                "A non-current row should not match a filter.");
        assertEquals(0, listCatalogs(PAST_THE_END, PAGE_SIZE, previousName).totalCount(),
                "A non-current row should not be counted by a filter.");
    }

    private PagedResult<TestCatalog> listCatalogs(Integer offset, Integer limit, EntityQuery query) {
        return repository.findEntities(offset, limit, TestCatalog.class, query, null, TestCatalog.TYPE).block();
    }

    private List<TestCatalog> seedQueryCatalogs() {
        List<TestCatalog> catalogs = IntStream.range(0, QUERY_NAMES.size()).mapToObj(index -> {
            TestCatalog catalog = catalog(index + 1);
            catalog.setName(QUERY_NAMES.get(index));
            catalog.setKeywords(QUERY_KEYWORDS.get(index));
            if (index < QUERY_PRICES.size()) {
                Money price = new Money();
                price.setUnit(PRICE_UNIT);
                price.setTmfValue(QUERY_PRICES.get(index));
                catalog.setPrice(price);
            }
            return catalog;
        }).toList();
        catalogs.forEach(catalog -> repository.createDomainEntity(catalog).block());
        return catalogs;
    }

    private static List<TestCatalog> numbered(List<TestCatalog> catalogs, List<Integer> numbers) {
        return numbers.stream().map(number -> catalogs.get(number - 1)).toList();
    }

    private static Comparison string(String path, ComparisonOperator operator, String... values) {
        return comparison(path, operator, ValueType.STRING, values);
    }

    private static Comparison number(ComparisonOperator operator, String value) {
        return comparison(PRICE_ATTRIBUTE, operator, ValueType.NUMBER, value);
    }

    private static Comparison comparison(String path, ComparisonOperator operator, ValueType valueType,
                                         String... values) {
        return new Comparison(attribute(path), operator, valueType, List.of(values));
    }

    private static SortKey sortKey(String path, ValueType valueType, boolean descending) {
        return new SortKey(attribute(path), valueType, descending);
    }

    private static AttributePath attribute(String path) {
        return new AttributePath(List.of(path.split("\\.")), true);
    }

    // the three ways an entity is first written
    static Stream<Arguments> writePaths() {
        return Stream.of(
                Arguments.of("createDomainEntity", (BiFunction<PostgresEntityRepository, Object, Mono<Void>>)
                        PostgresEntityRepository::createDomainEntity),
                Arguments.of("createWithEvent", (BiFunction<PostgresEntityRepository, Object, Mono<Void>>)
                        (repository, entity) -> repository.createWithEvent(entity, type -> true)),
                Arguments.of("importDomainEntity", (BiFunction<PostgresEntityRepository, Object, Mono<Void>>)
                        (repository, entity) -> repository.importDomainEntity(entity, IMPORTED_CREATED_AT,
                                IMPORTED_UPDATED_AT)));
    }

    static Stream<Arguments> versionColumnCases() {
        TestCatalog unversionedWithExtension = catalog(1);
        unversionedWithExtension.addAdditionalProperties(VERSION_ATTRIBUTE, FIRST_VERSION);
        Stream<Arguments> entities = Stream.of(
                Arguments.of("versioned", versioned(FIRST_VERSION), FIRST_VERSION),
                Arguments.of("versioned without version", versioned(null), PostgresEntityRepository.UNVERSIONED),
                Arguments.of("unversioned with an extension version", unversionedWithExtension,
                        PostgresEntityRepository.UNVERSIONED));
        List<Arguments> paths = writePaths().toList();
        return entities.flatMap(entity -> paths.stream().map(path -> Arguments.of(entity.get()[0], path.get()[0],
                entity.get()[1], path.get()[1], entity.get()[2])));
    }

    @ParameterizedTest(name = "{0} through {1}")
    @MethodSource("versionColumnCases")
    void writeFillsTheVersionColumns(String description, String path, EntityWithId entity,
                                     BiFunction<PostgresEntityRepository, Object, Mono<Void>> write,
                                     String expectedVersion) {
        write.apply(repository, entity).block();

        assertEquals(List.of(new VersionColumns(expectedVersion, INITIAL_REVISION, true)),
                versionColumns(entity.getId()),
                "The row should carry the entity's version, the first revision and be current.");
    }

    static Stream<Arguments> versionUpdates() {
        BiFunction<PostgresEntityRepository, TestVersionedCatalog, Mono<?>> update =
                (repository, patch) -> repository.updateDomainEntity(VERSIONED_ID, patch);
        BiFunction<PostgresEntityRepository, TestVersionedCatalog, Mono<?>> updateWithEvent =
                (repository, patch) -> repository.updateWithEvent(VERSIONED_ID, patch, TestVersionedCatalog.class,
                        type -> true);
        return Stream.of(
                Arguments.of("updateDomainEntity", update, SECOND_VERSION, SECOND_VERSION),
                Arguments.of("updateDomainEntity", update, null, FIRST_VERSION),
                Arguments.of("updateWithEvent", updateWithEvent, SECOND_VERSION, SECOND_VERSION),
                Arguments.of("updateWithEvent", updateWithEvent, null, FIRST_VERSION));
    }

    @ParameterizedTest(name = "{0} carrying version {2}")
    @MethodSource("versionUpdates")
    void updateMovesTheVersionOnlyWhenItCarriesOne(String path,
                                                   BiFunction<PostgresEntityRepository, TestVersionedCatalog,
                                                           Mono<?>> update,
                                                   String patchVersion, String expectedVersion) {
        repository.createDomainEntity(versioned(FIRST_VERSION)).block();
        TestVersionedCatalog patch = versioned(patchVersion);
        patch.setName(UPDATED_NAME);

        update.apply(repository, patch).block();

        assertEquals(List.of(new VersionColumns(expectedVersion, INITIAL_REVISION, true)),
                versionColumns(URI.create(VERSIONED_ID)),
                "The single current row should hold the expected version.");
        TestVersionedCatalog stored = repository.get(URI.create(VERSIONED_ID), TestVersionedCatalog.class).block();
        assertEquals(expectedVersion, stored.getVersion(), "The document's version should match the column.");
        assertEquals(UPDATED_NAME, stored.getName(), "The update should be applied.");
    }

    // with version bound the primary key (id, version) admits the second row; only entity_current_idx rejects it
    @ParameterizedTest(name = "{0}")
    @MethodSource("writePaths")
    void sameIdWithAnotherVersionConflicts(String path,
                                           BiFunction<PostgresEntityRepository, Object, Mono<Void>> write) {
        write.apply(repository, versioned(FIRST_VERSION)).block();

        StorageException failure = assertThrows(StorageException.class,
                () -> write.apply(repository, versioned(SECOND_VERSION)).block(), "The second write should fail.");

        assertEquals(StorageErrorReason.CONFLICT, failure.reason(), "An existing id should be a conflict.");
        assertEquals(List.of(new VersionColumns(FIRST_VERSION, INITIAL_REVISION, true)),
                versionColumns(URI.create(VERSIONED_ID)), "The first row should be the only one of the id.");
    }

    private List<VersionColumns> versionColumns(URI id) {
        return query(SELECT_VERSION_COLUMNS, row -> new VersionColumns(row.get("version", String.class),
                row.get("revision", Long.class), row.get("is_current", Boolean.class)), id.toString());
    }

    private static TestVersionedCatalog versioned(String version) {
        TestVersionedCatalog catalog = new TestVersionedCatalog(VERSIONED_ID);
        catalog.setName(NAME);
        catalog.setVersion(version);
        return catalog;
    }

    private record VersionColumns(String version, long revision, boolean current) {
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
