package org.fiware.tmforum.storage.contract;

import org.fiware.tmforum.storage.EntityRepository;
import org.fiware.tmforum.storage.PagedResult;
import org.fiware.tmforum.storage.StorageErrorReason;
import org.fiware.tmforum.storage.StorageException;
import org.fiware.tmforum.storage.StorageOperation;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The {@link EntityRepository} contract from its Javadoc, run by every adapter against its live backend; ticket 6
 * reuses it for PostgreSQL. Backend failures (UNKNOWN, UNAVAILABLE) cannot be provoked here, the adapters' unit
 * tests cover them. Update cases run in whatever update mode the backend is configured with.
 */
public abstract class AbstractEntityRepositoryContractTest<T, O> {

    static final Duration TIMEOUT = Duration.ofMinutes(1);
    static final String LIST_SEPARATOR = ",";

    private static final int LISTED_ENTITIES = 3;
    private static final int PAGE_SIZE = LISTED_ENTITIES - 1;
    private static final int FIRST_OFFSET = 0;
    // high enough to return every entity a single case creates
    private static final int LIST_LIMIT = 100;

    private final List<URI> createdIds = new ArrayList<>();

    protected abstract EntityRepository repository();

    /**
     * The class most cases run against.
     */
    protected abstract UpdateFixture<T> fixture();

    /**
     * A second class with another stored type, for the type-mismatch, type-filter and polymorphic cases.
     */
    protected abstract EntityFixture<O> otherFixture();

    /**
     * Deviations of the backend under test as configured; the affected cases assert them instead of the contract.
     */
    protected Set<KnownDeviation> knownDeviations() {
        return Set.of();
    }

    enum AbsentId {UNKNOWN, OTHER_TYPE, NO_TYPE_SEGMENT}

    @AfterEach
    void deleteCreatedEntities() {
        createdIds.forEach(id -> repository().deleteDomainEntity(id)
                .onErrorResume(StorageException.class, e -> Mono.empty())
                .block(TIMEOUT));
        createdIds.clear();
    }

    @Test
    void getReturnsTheCreatedEntity() {
        T created = fixture().newEntity(fixture().newId());
        URI id = create(fixture(), created);

        T read = read(id);

        assertEquals(id, fixture().id(read));
        assertEquals(fixture().mutableValue(created), fixture().mutableValue(read));
        assertEquals(fixture().untouchedValue(created), fixture().untouchedValue(read));
        assertEquals(fixture().listValue(created).size(), fixture().listValue(read).size());
    }

    @ParameterizedTest
    @EnumSource(AbsentId.class)
    void getIsEmpty(AbsentId absentId) {
        URI id = switch (absentId) {
            case UNKNOWN -> fixture().newId();
            case OTHER_TYPE -> create(otherFixture());
            case NO_TYPE_SEGMENT -> URI.create("urn:" + UUID.randomUUID());
        };

        assertNull(repository().get(id, fixture().entityClass()).block(TIMEOUT));
    }

    @Test
    void createFailsWithConflictForAnExistingId() {
        URI id = create(fixture());

        assertFailure(StorageOperation.CREATE, StorageErrorReason.CONFLICT,
                () -> repository().createDomainEntity(fixture().newEntity(id)));
    }

    @Test
    void updateWritesOnlyTheCarriedAttributes() {
        T created = fixture().newEntity(fixture().newId());
        URI id = create(fixture(), created);
        T mutation = fixture().mutate(id);

        repository().updateDomainEntity(id.toString(), mutation).block(TIMEOUT);

        T read = read(id);
        assertEquals(fixture().mutableValue(mutation), fixture().mutableValue(read));
        assertEquals(fixture().untouchedValue(created), fixture().untouchedValue(read));
        assertEquals(fixture().listValue(created).size(), fixture().listValue(read).size());
    }

    @Test
    void updateClearsAnEmptiedList() {
        T created = fixture().newEntity(fixture().newId());
        URI id = create(fixture(), created);

        repository().updateDomainEntity(id.toString(), fixture().emptiedList(id)).block(TIMEOUT);

        if (knownDeviations().contains(KnownDeviation.EMPTIED_LIST_BREAKS_READS)) {
            assertThrows(RuntimeException.class, () -> repository().get(id, fixture().entityClass()).block(TIMEOUT),
                    "Reading after emptying a list works now, drop " + KnownDeviation.EMPTIED_LIST_BREAKS_READS);
            return;
        }
        T read = read(id);
        List<?> list = fixture().listValue(read);
        assertTrue(list == null || list.isEmpty(), "An emptied list reads back absent or empty, but was " + list);
        assertEquals(fixture().mutableValue(created), fixture().mutableValue(read));
        assertEquals(fixture().untouchedValue(created), fixture().untouchedValue(read));
    }

    @Test
    void updateOfAnUnknownIdFailsWithNotFoundAndCreatesNothing() {
        URI id = fixture().newId();
        // registered for cleanup in case an adapter creates it after all
        createdIds.add(id);

        StorageException failure = assertThrows(StorageException.class,
                () -> repository().updateDomainEntity(id.toString(), fixture().mutate(id)).block(TIMEOUT));

        assertEquals(StorageErrorReason.NOT_FOUND, failure.reason());
        // READ is the recorded NGSI-LD replace-mode pre-read, see StorageOperation
        assertTrue(Set.of(StorageOperation.UPDATE, StorageOperation.READ).contains(failure.operation()),
                "Unexpected operation " + failure.operation());
        assertNull(repository().get(id, fixture().entityClass()).block(TIMEOUT));
    }

    @Test
    void deleteRemovesTheEntityAndFailsWithNotFoundOnceGone() {
        URI id = create(fixture());

        repository().deleteDomainEntity(id).block(TIMEOUT);

        assertNull(repository().get(id, fixture().entityClass()).block(TIMEOUT));
        assertFailure(StorageOperation.DELETE, StorageErrorReason.NOT_FOUND,
                () -> repository().deleteDomainEntity(id));
    }

    static Stream<Arguments> pages() {
        return Stream.of(
                Arguments.of(FIRST_OFFSET, LIST_LIMIT, LISTED_ENTITIES),
                Arguments.of(FIRST_OFFSET, PAGE_SIZE, PAGE_SIZE),
                Arguments.of(PAGE_SIZE, PAGE_SIZE, LISTED_ENTITIES - PAGE_SIZE),
                Arguments.of(LISTED_ENTITIES, PAGE_SIZE, 0));
    }

    @ParameterizedTest
    @MethodSource("pages")
    void findEntitiesPages(int offset, int limit, int expectedSize) {
        List<URI> ids = createListed();

        PagedResult<T> page = repository().findEntities(offset, limit, fixture().entityClass(), null, join(ids),
                fixture().storedType(), null).block(TIMEOUT);

        if (page == null) {
            assertEquals(0, expectedSize, "Only a result without matches may complete empty.");
            return;
        }
        assertEquals(expectedSize, page.items().size());
        assertTrue(ids.containsAll(idsOf(page)), "The page holds only matching entities.");
        assertEquals(offset, page.offset());
        assertEquals(limit, page.limit());
        assertTrue(page.totalCount() == null || page.totalCount() == LISTED_ENTITIES,
                "The total count is null or counts all matches, but was " + page.totalCount());
    }

    @Test
    void findEntitiesFiltersByIds() {
        List<URI> requested = createListed().subList(0, PAGE_SIZE);

        PagedResult<T> page = repository().findEntities(FIRST_OFFSET, LIST_LIMIT, fixture().entityClass(), null,
                join(requested), fixture().storedType(), null).block(TIMEOUT);

        assertNotNull(page);
        assertEquals(Set.copyOf(requested), Set.copyOf(idsOf(page)));
    }

    @Test
    void findEntitiesFiltersByType() {
        URI own = create(fixture());
        URI other = create(otherFixture());

        PagedResult<T> page = repository().findEntities(FIRST_OFFSET, LIST_LIMIT, fixture().entityClass(), null,
                join(List.of(own, other)), fixture().storedType(), null).block(TIMEOUT);

        assertNotNull(page);
        assertEquals(List.of(own), idsOf(page));
    }

    @Test
    void findEntitiesPolymorphicMapsEachEntityByItsStoredType() {
        URI own = create(fixture());
        URI other = create(otherFixture());

        // one id per call, so the case holds regardless of SEVERAL_IDS_WITH_SEVERAL_TYPES_FAIL
        List<Object> ownItems = itemsOf(findPolymorphic(List.of(own)));
        List<Object> otherItems = itemsOf(findPolymorphic(List.of(other)));

        assertEquals(List.of(own), idsOf(fixture(), ownItems), "The own type maps to its class.");
        assertEquals(List.of(other), idsOf(otherFixture(), otherItems), "The other type maps to its class.");
    }

    @Test
    void findEntitiesPolymorphicFiltersSeveralIdsWithSeveralTypes() {
        URI own = create(fixture());
        URI other = create(otherFixture());

        if (knownDeviations().contains(KnownDeviation.SEVERAL_IDS_WITH_SEVERAL_TYPES_FAIL)) {
            assertFailure(StorageOperation.LIST, StorageErrorReason.UNKNOWN,
                    () -> findPolymorphic(List.of(own, other)));
            return;
        }
        List<Object> items = itemsOf(findPolymorphic(List.of(own, other)));

        assertEquals(List.of(own), idsOf(fixture(), items));
        assertEquals(List.of(other), idsOf(otherFixture(), items));
    }

    // lists both fixtures' types, mapping each stored type to its fixture's class
    private Mono<PagedResult<Object>> findPolymorphic(List<URI> ids) {
        Map<String, Class<?>> classes = Map.of(
                fixture().storedType(), fixture().entityClass(),
                otherFixture().storedType(), otherFixture().entityClass());
        return repository().findEntitiesPolymorphic(FIRST_OFFSET, LIST_LIMIT,
                String.join(LIST_SEPARATOR, classes.keySet()), null, join(ids), null, classes::get);
    }

    private static List<Object> itemsOf(Mono<PagedResult<Object>> result) {
        PagedResult<Object> page = result.block(TIMEOUT);
        assertNotNull(page);
        return page.items();
    }

    private T read(URI id) {
        T read = repository().get(id, fixture().entityClass()).block(TIMEOUT);
        assertNotNull(read, "Entity " + id + " should exist.");
        return read;
    }

    private <E> URI create(EntityFixture<E> entityFixture) {
        return create(entityFixture, entityFixture.newEntity(entityFixture.newId()));
    }

    private <E> URI create(EntityFixture<E> entityFixture, E entity) {
        URI id = entityFixture.id(entity);
        createdIds.add(id);
        repository().createDomainEntity(entity).block(TIMEOUT);
        return id;
    }

    private List<URI> createListed() {
        return IntStream.range(0, LISTED_ENTITIES).mapToObj(i -> create(fixture())).toList();
    }

    private List<URI> idsOf(PagedResult<T> page) {
        return page.items().stream().map(fixture()::id).toList();
    }

    private static <E> List<URI> idsOf(EntityFixture<E> entityFixture, List<?> items) {
        return items.stream()
                .filter(entityFixture.entityClass()::isInstance)
                .map(entityFixture.entityClass()::cast)
                .map(entityFixture::id)
                .toList();
    }

    static String join(List<URI> ids) {
        return ids.stream().map(URI::toString).collect(Collectors.joining(LIST_SEPARATOR));
    }

    static void assertFailure(StorageOperation operation, StorageErrorReason reason, Supplier<Mono<?>> call) {
        StorageException failure = assertThrows(StorageException.class, () -> call.get().block(TIMEOUT));
        assertEquals(reason, failure.reason());
        assertEquals(operation, failure.operation());
    }
}
