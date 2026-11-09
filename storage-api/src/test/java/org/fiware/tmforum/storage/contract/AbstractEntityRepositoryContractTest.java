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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

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
    private static final String TARGET_NAME = "contract-reference-target";
    private static final String RENAMED_TARGET_NAME = "renamed-contract-reference-target";

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

    /**
     * Entities with references for the inline-reference cases, which skip when empty. Pick a class whose references
     * a resolving adapter would fill in from their targets, so the cases discriminate.
     */
    protected Optional<ReferenceFixture<?>> referenceFixture() {
        return Optional.empty();
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
            assertFailure(StorageOperation.READ, StorageErrorReason.UNKNOWN,
                    () -> repository().get(id, fixture().entityClass()));
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

    @Test
    void createdReferencesRoundTripIntact() {
        assertCreatedReferencesRoundTripIntact(requireReferenceFixture());
    }

    @Test
    void updateKeepsUntouchedReferences() {
        assertUpdateKeepsUntouchedReferences(requireReferenceFixture());
    }

    @Test
    void getDoesNotResolveReferences() {
        assertGetDoesNotResolveReferences(requireReferenceFixture());
    }

    private <R> void assertCreatedReferencesRoundTripIntact(ReferenceFixture<R> references) {
        R created = references.newEntity(references.newId());
        URI id = create(references, created);

        assertSameReferences(references.references(created), references.references(read(references, id)),
                "Read after the create.");
    }

    private <R> void assertUpdateKeepsUntouchedReferences(ReferenceFixture<R> references) {
        R created = references.newEntity(references.newId());
        URI id = create(references, created);
        R update = references.withOneReferenceReplaced(id);
        Map<String, List<?>> expected = new HashMap<>(references.references(created));
        expected.putAll(references.references(update));
        assertTrue(expected.size() > references.references(update).size(),
                "The update has to leave at least one reference attribute out.");

        repository().updateDomainEntity(id.toString(), update).block(TIMEOUT);

        Map<String, List<?>> read = references.references(read(references, id));
        if (knownDeviations().contains(KnownDeviation.REFERENCE_LIST_UPDATE_MERGES)) {
            // the carried references point at other ids, so the merge appends every one of them
            Map<String, List<?>> merged = new HashMap<>(references.references(created));
            references.references(update).forEach((attribute, carried) -> merged.merge(attribute, carried,
                    (stored, appended) -> Stream.concat(stored.stream(), appended.stream()).toList()));
            assertSameReferences(merged, read,
                    "The update replaces the list now, drop " + KnownDeviation.REFERENCE_LIST_UPDATE_MERGES);
            return;
        }
        assertSameReferences(expected, read, "Read after the update.");
    }

    // the reference at targetId omits the name its target carries, so that a resolving read would fill it in
    private <R> void assertGetDoesNotResolveReferences(ReferenceFixture<R> references) {
        R created = references.newEntity(references.newId());
        URI id = create(references, created);
        Map<String, List<?>> written = references.references(created);
        URI targetId = references.targetId(created);
        createdIds.add(targetId);

        repository().createDomainEntity(references.target(targetId, TARGET_NAME)).block(TIMEOUT);
        assertSameReferences(written, references.references(read(references, id)),
                "Read after the target was created.");

        repository().updateDomainEntity(targetId.toString(), references.target(targetId, RENAMED_TARGET_NAME))
                .block(TIMEOUT);
        assertSameReferences(written, references.references(read(references, id)),
                "Read after the target was renamed.");

        repository().deleteDomainEntity(targetId).block(TIMEOUT);
        assertSameReferences(written, references.references(read(references, id)),
                "Read after the target was deleted.");
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

        if (expectedSize == 0 && knownDeviations().contains(KnownDeviation.EMPTY_PAGE_COMPLETES_EMPTY)) {
            assertNull(page, "An empty page is emitted now, drop " + KnownDeviation.EMPTY_PAGE_COMPLETES_EMPTY);
            return;
        }
        assertNotNull(page, "A page is always emitted, an empty one included.");
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

    @Test
    void findEntitiesPolymorphicEmitsAnEmptyPagePastTheLastMatch() {
        List<URI> matches = List.of(create(fixture()));

        PagedResult<Object> page = findPolymorphic(matches.size(), matches).block(TIMEOUT);

        assertNotNull(page, "A page is always emitted, an empty one included.");
        assertTrue(page.items().isEmpty(), "The page past the last match is empty, but was " + page.items());
        assertEquals(matches.size(), page.offset());
        assertTrue(page.totalCount() == null || page.totalCount() == matches.size(),
                "The total count is null or counts all matches, but was " + page.totalCount());
    }

    private Mono<PagedResult<Object>> findPolymorphic(List<URI> ids) {
        return findPolymorphic(FIRST_OFFSET, ids);
    }

    // lists both fixtures' types, mapping each stored type to its fixture's class
    private Mono<PagedResult<Object>> findPolymorphic(int offset, List<URI> ids) {
        Map<String, Class<?>> classes = Map.of(
                fixture().storedType(), fixture().entityClass(),
                otherFixture().storedType(), otherFixture().entityClass());
        return repository().findEntitiesPolymorphic(offset, LIST_LIMIT,
                String.join(LIST_SEPARATOR, classes.keySet()), null, join(ids), null, classes::get);
    }

    private static List<Object> itemsOf(Mono<PagedResult<Object>> result) {
        PagedResult<Object> page = result.block(TIMEOUT);
        assertNotNull(page);
        return page.items();
    }

    private ReferenceFixture<?> requireReferenceFixture() {
        Optional<ReferenceFixture<?>> references = referenceFixture();
        assumeTrue(references.isPresent(), "The adapter provides no reference fixture.");
        return references.get();
    }

    // list order is not part of the port contract (NGSI-LD does not define the order of attribute instances),
    // multiplicity is: a duplicated or lost reference fails
    private static void assertSameReferences(Map<String, List<?>> expected, Map<String, List<?>> actual,
                                             String message) {
        assertEquals(asCounts(expected), asCounts(actual), message);
    }

    private static Map<String, Map<Object, Long>> asCounts(Map<String, List<?>> references) {
        Map<String, Map<Object, Long>> counts = new HashMap<>();
        references.forEach((attribute, payloads) -> counts.put(attribute,
                payloads.stream().collect(Collectors.groupingBy(Function.identity(), Collectors.counting()))));
        return counts;
    }

    private T read(URI id) {
        return read(fixture(), id);
    }

    private <E> E read(EntityFixture<E> entityFixture, URI id) {
        E read = repository().get(id, entityFixture.entityClass()).block(TIMEOUT);
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
