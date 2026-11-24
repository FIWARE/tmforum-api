package org.fiware.tmforum.storage.contract;

import org.fiware.tmforum.storage.EntityRepository;
import org.fiware.tmforum.storage.PagedResult;
import org.fiware.tmforum.storage.StorageErrorReason;
import org.fiware.tmforum.storage.StorageException;
import org.fiware.tmforum.storage.StorageOperation;
import org.fiware.tmforum.storage.contract.QueryFixture.Attribute;
import org.fiware.tmforum.storage.query.AttributePath;
import org.fiware.tmforum.storage.query.ComparisonOperator;
import org.fiware.tmforum.storage.query.EntityQuery;
import org.fiware.tmforum.storage.query.QueryFilter;
import org.fiware.tmforum.storage.query.QueryFilter.AllOf;
import org.fiware.tmforum.storage.query.QueryFilter.AnyOf;
import org.fiware.tmforum.storage.query.QueryFilter.Comparison;
import org.fiware.tmforum.storage.query.SortKey;
import org.fiware.tmforum.storage.query.ValueType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.function.IntPredicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
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
    private static final String EXTENSION_NAME = "contractExtension";
    private static final String EXTENSION_ENTRY = "contract-extension-entry";

    private static final String ALPHA = "alpha";
    private static final String ALPHABET = "alphabet";
    private static final String BETA = "beta";
    private static final String GAMMA = "gamma";
    private static final List<String> WORDS = List.of(ALPHA, BETA, ALPHABET);
    private static final String CAPITAL_BETA = "Beta";
    // byte order puts the capital first (Beta < alpha < beta), a linguistic collation puts it after alpha
    private static final List<String> CASE_DIFFERING_WORDS = List.of(CAPITAL_BETA, ALPHA, BETA);
    // inside BETA and ALPHABET, at the start of neither ALPHA nor ALPHABET
    private static final String UNANCHORED_PATTERN = "bet";
    // each branch matches one word only: "eta" BETA, "lphab" ALPHABET
    private static final String ALTERNATION_PATTERN = "eta|lphab";
    // matches ALPHABET only, while the bare UNANCHORED_PATTERN also matches BETA
    private static final String GROUP_PATTERN = "a(" + UNANCHORED_PATTERN + ")";
    // seeds for patterns holding characters that look like q syntax; each pattern matches the first seed only
    private static final List<String> SPACED_WORDS = List.of("two words", "twowords");
    private static final List<String> EQUATIONS = List.of("a=b", "ab");
    private static final List<String> BRACKETED_WORDS = List.of("a[b]", "ab");
    private static final List<String> QUOTED_WORDS = List.of("say \"hi\"", "say hi");
    // seeds for patterns the q lexer could read as another literal; each pattern matches the first two seeds only
    private static final List<String> NUMBER_LIKE_WORDS = List.of("2.0", "2x0", "20");
    private static final List<String> BOOLEAN_LIKE_WORDS = List.of("true", "untrue", "false");
    private static final List<String> DATE_LIKE_WORDS = List.of("2024-01-01T10:00:00Z", "x2024-01-01", "2024-02-01");
    private static final List<String> TIME_LIKE_WORDS = List.of("T10:00:00Z 25:61:00", "xT10:00:00 25:61:00", "10:00:01");
    // creation order differs from both sort orders
    private static final List<String> EXTENSION_SORT_SEEDS = List.of(BETA, GAMMA, ALPHA);
    // string order ("10" < "2" < "3") differs from numeric order
    private static final List<Integer> NUMBERS = List.of(2, 10, 3);
    private static final String NUMBER_LITERAL = "3";
    private static final String HIGH_NUMBER_LITERAL = "5";
    // the string seed is under a NUMBER filter on the same key, as when an extension is typed by its literal
    private static final List<Object> NUMBERS_AND_A_STRING = List.of(2, 10, ALPHA, 3);
    private static final List<Integer> TIED_NUMBERS = List.of(2, 1, 2, 1, 2);
    // creation order differs from every sort order the sort cases ask for
    private static final List<Map.Entry<String, Integer>> NAMED_AND_NUMBERED = List.of(
            Map.entry(BETA, 2), Map.entry(ALPHA, 10), Map.entry(GAMMA, 3), Map.entry(ALPHA, 1));
    private static final List<Map.Entry<Integer, Boolean>> NUMBERED_AND_FLAGGED = List.of(
            Map.entry(2, true), Map.entry(10, true), Map.entry(10, false), Map.entry(2, false));
    private static final URI REFERENCE_A = URI.create("urn:ngsi-ld:contract-reference:a");
    private static final URI REFERENCE_B = URI.create("urn:ngsi-ld:contract-reference:b");
    private static final URI REFERENCE_C = URI.create("urn:ngsi-ld:contract-reference:c");
    private static final List<List<URI>> REFERENCE_LISTS = List.of(List.of(REFERENCE_A, REFERENCE_B),
            List.of(REFERENCE_C));
    private static final List<List<String>> STRING_LISTS = List.of(List.of(ALPHA, BETA), List.of(GAMMA));
    // Instant.toString() leaves out a zero fraction, so EARLY and EXACT are stored without one, FRACTION with one
    private static final Instant EARLY = Instant.parse("2024-01-01T09:00:00Z");
    private static final Instant EXACT = Instant.parse("2024-01-01T10:00:00Z");
    private static final Instant FRACTION = Instant.parse("2024-01-01T10:00:00.500Z");
    private static final List<Instant> DATE_TIME_SEEDS = List.of(EXACT, EARLY, FRACTION);

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
     * Whether a list failure is the client's bad query (a 400), as opposed to a storage failure or a bug.
     */
    protected abstract boolean isClientQueryError(Throwable failure);

    /**
     * Fixtures for the query, sort and paging cases; a case runs on the first one holding its attributes and skips
     * when none does.
     */
    protected List<QueryFixture<?>> queryFixtures() {
        return List.of();
    }

    /**
     * Entities with references for the inline-reference cases, which skip when empty. Pick a class whose references
     * a resolving adapter would fill in from their targets, so the cases discriminate.
     */
    protected Optional<ReferenceFixture<?>> referenceFixture() {
        return Optional.empty();
    }

    enum AbsentId {UNKNOWN, OTHER_TYPE, NO_TYPE_SEGMENT}

    // enum rather than Object arguments, which Micronaut's parameter resolver would try to inject as beans
    enum EmptyValue {
        LIST(List.of()), OBJECT(Map.of());

        private final Object value;

        EmptyValue(Object value) {
            this.value = value;
        }
    }

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
    void updateKeepsTheStoredObjectWhenCarryingAnEmptyOne() {
        T created = fixture().newEntity(fixture().newId());
        URI id = create(fixture(), created);

        repository().updateDomainEntity(id.toString(), fixture().emptiedObject(id)).block(TIMEOUT);

        T read = read(id);
        assertNotNull(fixture().objectValue(created));
        assertEquals(fixture().objectValue(created), fixture().objectValue(read));
        assertEquals(fixture().mutableValue(created), fixture().mutableValue(read));
    }

    @ParameterizedTest
    @EnumSource(EmptyValue.class)
    void anEmptyExtensionValueRoundTrips(EmptyValue empty) {
        URI id = create(fixture(),
                fixture().withExtension(fixture().newEntity(fixture().newId()), EXTENSION_NAME, empty.value));

        assertEquals(empty.value, fixture().extensionValue(read(id), EXTENSION_NAME));
    }

    @Test
    void updateKeepsAnEmptiedExtensionListAsEmpty() {
        URI id = create(fixture(), fixture().withExtension(fixture().newEntity(fixture().newId()), EXTENSION_NAME,
                List.of(EXTENSION_ENTRY)));

        repository().updateDomainEntity(id.toString(),
                fixture().withExtension(fixture().mutate(id), EXTENSION_NAME, List.of())).block(TIMEOUT);

        Object read = fixture().extensionValue(read(id), EXTENSION_NAME);
        if (knownDeviations().contains(KnownDeviation.EMPTIED_EXTENSION_LIST_READS_BACK_EXPANDED)) {
            assertTrue(read instanceof Map<?, ?> expanded && expanded.size() == 1
                            && List.of().equals(expanded.values().iterator().next()),
                    "Expected a map to an empty list while "
                            + KnownDeviation.EMPTIED_EXTENSION_LIST_READS_BACK_EXPANDED + " is recorded, but was " + read);
            return;
        }
        assertEquals(List.of(), read);
    }

    @Test
    void aDeclaredPropertyWinsOverAnExtensionOfTheSameName() {
        T created = fixture().withExtensionNamedLikeTheMutableAttribute(fixture().newEntity(fixture().newId()));
        URI id = create(fixture(), created);

        assertDeclaredValueWon(fixture().mutableValue(created), read(id));
    }

    @Test
    void anUpdatedDeclaredPropertyWinsOverAnExtensionOfTheSameName() {
        URI id = create(fixture());
        T mutation = fixture().withExtensionNamedLikeTheMutableAttribute(fixture().mutate(id));

        repository().updateDomainEntity(id.toString(), mutation).block(TIMEOUT);

        assertDeclaredValueWon(fixture().mutableValue(mutation), read(id));
    }

    private void assertDeclaredValueWon(Object declaredValue, T read) {
        assertEquals(Set.of(), fixture().extensionNames(read));
        if (knownDeviations().contains(KnownDeviation.EXTENSION_OVERRIDES_DECLARED_PROPERTY)) {
            assertEquals(fixture().extensionValue(), fixture().mutableValue(read),
                    "Expected the extension value while " + KnownDeviation.EXTENSION_OVERRIDES_DECLARED_PROPERTY
                            + " is listed");
            return;
        }
        assertEquals(declaredValue, fixture().mutableValue(read));
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
                fixture().storedType()).block(TIMEOUT);

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
                join(requested), fixture().storedType()).block(TIMEOUT);

        assertNotNull(page);
        assertEquals(Set.copyOf(requested), Set.copyOf(idsOf(page)));
    }

    @Test
    void findEntitiesFiltersByType() {
        URI own = create(fixture());
        URI other = create(otherFixture());

        PagedResult<T> page = repository().findEntities(FIRST_OFFSET, LIST_LIMIT, fixture().entityClass(), null,
                join(List.of(own, other)), fixture().storedType()).block(TIMEOUT);

        assertNotNull(page);
        assertEquals(List.of(own), idsOf(page));
    }

    // the flag pins SEPARATOR_IN_LITERAL_NEVER_MATCHES per literal: the list fails instead of returning empty
    static Stream<Arguments> literalsHoldingFilterSeparators() {
        return Stream.of(
                Arguments.of("a&b", false),
                Arguments.of("a,b", true),
                Arguments.of("a;b", false),
                Arguments.of("https://example.org/cb?a=1&b=2,3;c=4", false));
    }

    // a comparison value is one literal, even when it holds TMForum's filter separators
    @ParameterizedTest
    @MethodSource("literalsHoldingFilterSeparators")
    void findEntitiesMatchesALiteralHoldingFilterSeparators(String literal, boolean deviationFailsTheList) {
        URI match = create(fixture(), fixture().withMutableValue(fixture().newEntity(fixture().newId()), literal));
        URI other = create(fixture());
        Class<T> entityClass = fixture().entityClass();
        Comparison equalsLiteral = new Comparison(new AttributePath(List.of(fixture().mutableAttribute()), true),
                ComparisonOperator.EQUALS, ValueType.STRING, List.of(literal));

        Mono<PagedResult<T>> result = repository().findEntities(FIRST_OFFSET, LIST_LIMIT, entityClass,
                new EntityQuery(entityClass, equalsLiteral, List.of()), join(List.of(match, other)),
                fixture().storedType());

        if (knownDeviations().contains(KnownDeviation.SEPARATOR_IN_LITERAL_NEVER_MATCHES)) {
            String changed = "The backend behaves differently now, revisit "
                    + KnownDeviation.SEPARATOR_IN_LITERAL_NEVER_MATCHES;
            if (deviationFailsTheList) {
                assertThrows(StorageException.class, () -> result.block(TIMEOUT), changed);
                return;
            }
            PagedResult<T> page = result.block(TIMEOUT);
            assertNotNull(page, changed);
            assertTrue(page.items().isEmpty(), changed);
            return;
        }
        PagedResult<T> page = result.block(TIMEOUT);
        assertNotNull(page);
        assertEquals(List.of(match), idsOf(page));
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

    // one entity per seed value; matches holds the indexes of the seeds the filter selects
    enum FilterCase {
        STRING_EQUALS(Attribute.STRING, WORDS, ComparisonOperator.EQUALS, List.of(BETA), Set.of(1)),
        STRING_EQUALS_ANY_VALUE(Attribute.STRING, WORDS, ComparisonOperator.EQUALS, List.of(ALPHA, ALPHABET),
                Set.of(0, 2)),
        NUMBER_EQUALS(Attribute.NUMBER, NUMBERS, ComparisonOperator.EQUALS, List.of(NUMBER_LITERAL), Set.of(2)),
        NUMBER_EQUALS_ANY_VALUE(Attribute.NUMBER, NUMBERS, ComparisonOperator.EQUALS,
                List.of(String.valueOf(NUMBERS.get(0)), String.valueOf(NUMBERS.get(1))), Set.of(0, 1)),
        NUMBER_GREATER_THAN(Attribute.NUMBER, NUMBERS, ComparisonOperator.GREATER_THAN, List.of(NUMBER_LITERAL),
                Set.of(1)),
        NUMBER_GREATER_THAN_EQUALS(Attribute.NUMBER, NUMBERS, ComparisonOperator.GREATER_THAN_EQUALS,
                List.of(NUMBER_LITERAL), Set.of(1, 2)),
        NUMBER_LESS_THAN(Attribute.NUMBER, NUMBERS, ComparisonOperator.LESS_THAN, List.of(NUMBER_LITERAL), Set.of(0)),
        NUMBER_LESS_THAN_EQUALS(Attribute.NUMBER, NUMBERS, ComparisonOperator.LESS_THAN_EQUALS,
                List.of(NUMBER_LITERAL), Set.of(0, 2)),
        BOOLEAN_EQUALS(Attribute.BOOLEAN, List.of(true, false), ComparisonOperator.EQUALS,
                List.of(Boolean.TRUE.toString()), Set.of(0)),
        REGEX(Attribute.STRING, WORDS, ComparisonOperator.REGEX, List.of("^" + ALPHA), Set.of(0, 2),
                KnownDeviation.ANCHORED_REGEX_NEVER_MATCHES),
        REGEX_UNANCHORED(Attribute.STRING, WORDS, ComparisonOperator.REGEX, List.of(UNANCHORED_PATTERN),
                Set.of(1, 2)),
        REGEX_NOT_EQUALS(Attribute.STRING, WORDS, ComparisonOperator.REGEX, List.of("^(?!" + ALPHA + "$).*$"),
                Set.of(1, 2)),
        REGEX_ALTERNATION(Attribute.STRING, WORDS, ComparisonOperator.REGEX,
                List.of(ALTERNATION_PATTERN), Set.of(1, 2),
                KnownDeviation.REGEX_HOLDING_QUERY_SYNTAX_IS_REJECTED),
        REGEX_GROUP(Attribute.STRING, WORDS, ComparisonOperator.REGEX, List.of(GROUP_PATTERN),
                Set.of(2), KnownDeviation.REGEX_HOLDING_QUERY_SYNTAX_IS_REJECTED),
        REGEX_HOLDING_A_SPACE(Attribute.STRING, SPACED_WORDS, ComparisonOperator.REGEX, List.of("o w"), Set.of(0)),
        REGEX_HOLDING_AN_EQUALS_SIGN(Attribute.STRING, EQUATIONS, ComparisonOperator.REGEX, List.of("a=b"),
                Set.of(0)),
        REGEX_HOLDING_ESCAPED_BRACKETS(Attribute.STRING, BRACKETED_WORDS, ComparisonOperator.REGEX,
                List.of("a\\[b\\]"), Set.of(0)),
        REGEX_HOLDING_A_QUOTE(Attribute.STRING, QUOTED_WORDS, ComparisonOperator.REGEX, List.of("y \"h"), Set.of(0),
                KnownDeviation.REGEX_HOLDING_QUERY_SYNTAX_IS_REJECTED),
        REGEX_LIKE_A_NUMBER(Attribute.STRING, NUMBER_LIKE_WORDS, ComparisonOperator.REGEX, List.of("2.0"),
                Set.of(0, 1)),
        REGEX_LIKE_A_BOOLEAN(Attribute.STRING, BOOLEAN_LIKE_WORDS, ComparisonOperator.REGEX,
                List.of(Boolean.TRUE.toString()), Set.of(0, 1)),
        REGEX_LIKE_A_DATE(Attribute.STRING, DATE_LIKE_WORDS, ComparisonOperator.REGEX, List.of("2024-01-01"),
                Set.of(0, 1)),
        REGEX_LIKE_AN_INVALID_DATE(Attribute.STRING, DATE_LIKE_WORDS, ComparisonOperator.REGEX, List.of("2024-01"),
                Set.of(0, 1)),
        REGEX_LIKE_A_TIME(Attribute.STRING, TIME_LIKE_WORDS, ComparisonOperator.REGEX, List.of("10:00:00"),
                Set.of(0, 1)),
        REGEX_LIKE_AN_INVALID_TIME(Attribute.STRING, TIME_LIKE_WORDS, ComparisonOperator.REGEX, List.of("25:61:00"),
                Set.of(0, 1)),
        REGEX_LIKE_A_PREFIXED_TIME(Attribute.STRING, TIME_LIKE_WORDS, ComparisonOperator.REGEX, List.of("T10:00"),
                Set.of(0, 1)),
        NESTED_STRING_EQUALS(Attribute.NESTED_STRING, List.of(ALPHA, BETA), ComparisonOperator.EQUALS,
                List.of(BETA), Set.of(1)),
        REFERENCE_ID_IN_A_LONGER_LIST(Attribute.REFERENCE_ID, REFERENCE_LISTS, ComparisonOperator.EQUALS,
                List.of(REFERENCE_B.toString()), Set.of(0)),
        REFERENCE_ID_IN_A_SINGLE_ELEMENT_LIST(Attribute.REFERENCE_ID, REFERENCE_LISTS, ComparisonOperator.EQUALS,
                List.of(REFERENCE_C.toString()), Set.of(1)),
        STRING_IN_A_LONGER_LIST(Attribute.STRING_LIST, STRING_LISTS, ComparisonOperator.EQUALS, List.of(BETA),
                Set.of(0)),
        STRING_IN_A_SINGLE_ELEMENT_LIST(Attribute.STRING_LIST, STRING_LISTS, ComparisonOperator.EQUALS,
                List.of(GAMMA), Set.of(1)),
        EXTENSION_EQUALS(Attribute.EXTENSION, List.of(ALPHA, BETA), ComparisonOperator.EQUALS, List.of(BETA),
                Set.of(1)),
        EXTENSION_NUMBER_GREATER_THAN(Attribute.EXTENSION, ValueType.NUMBER, NUMBERS_AND_A_STRING,
                ComparisonOperator.GREATER_THAN, List.of(NUMBER_LITERAL), Set.of(1));

        private final Attribute attribute;
        private final ValueType valueType;
        private final List<?> seeds;
        private final ComparisonOperator operator;
        private final List<String> literals;
        private final Set<Integer> matches;
        // each one makes the filter match nothing
        private final Set<KnownDeviation> deviations;

        FilterCase(Attribute attribute, List<?> seeds, ComparisonOperator operator, List<String> literals,
                   Set<Integer> matches, KnownDeviation... deviations) {
            this(attribute, attribute.valueType(), seeds, operator, literals, matches, deviations);
        }

        FilterCase(Attribute attribute, ValueType valueType, List<?> seeds, ComparisonOperator operator,
                   List<String> literals, Set<Integer> matches, KnownDeviation... deviations) {
            this.attribute = attribute;
            this.valueType = valueType;
            this.seeds = seeds;
            this.operator = operator;
            this.literals = literals;
            this.matches = matches;
            this.deviations = Set.of(deviations);
        }
    }

    @ParameterizedTest
    @EnumSource(FilterCase.class)
    void findEntitiesFilters(FilterCase filterCase) {
        assertFilterMatches(queryFixture(filterCase.attribute), filterCase);
    }

    private <Q> void assertFilterMatches(QueryFixture<Q> queries, FilterCase filterCase) {
        List<URI> ids = filterCase.seeds.stream()
                .map(seed -> create(queries, queries.newEntity(queries.newId(), Map.of(filterCase.attribute, seed))))
                .toList();
        Comparison comparison = new Comparison(queries.path(filterCase.attribute), filterCase.operator,
                filterCase.valueType, filterCase.literals);
        boolean deviates = filterCase.deviations.stream().anyMatch(knownDeviations()::contains);
        if (deviates && filterCase.deviations.contains(KnownDeviation.REGEX_HOLDING_QUERY_SYNTAX_IS_REJECTED)) {
            Mono<PagedResult<Q>> rejected = assertDoesNotThrow(
                    () -> find(queries, comparison, List.of(), ids, FIRST_OFFSET, LIST_LIMIT),
                    "A rejected filter is an error signal of the Mono, not a synchronous throw.");
            RuntimeException failure = assertThrows(RuntimeException.class, () -> rejected.block(TIMEOUT),
                    "The pattern is accepted now, drop " + KnownDeviation.REGEX_HOLDING_QUERY_SYNTAX_IS_REJECTED);
            assertTrue(isClientQueryError(failure), "Expected the client's query error, but was " + failure);
            return;
        }
        Set<Integer> matches = deviates ? Set.of() : filterCase.matches;
        Set<URI> expected = matches.stream()
                .filter(match -> reachable(filterCase.attribute, filterCase.seeds.get(match)))
                .map(ids::get)
                .collect(Collectors.toSet());

        assertEquals(expected, Set.copyOf(findIds(queries, comparison, List.of(), ids, FIRST_OFFSET, LIST_LIMIT)));
    }

    @Test
    void findEntitiesComparesStringsInByteOrder() {
        assertStringsCompareInByteOrder(queryFixture(Attribute.STRING));
    }

    private <Q> void assertStringsCompareInByteOrder(QueryFixture<Q> queries) {
        Map<String, URI> idsByValue = new HashMap<>();
        CASE_DIFFERING_WORDS.forEach(value -> idsByValue.put(value,
                create(queries, queries.newEntity(queries.newId(), Map.of(Attribute.STRING, value)))));
        Comparison greaterThanAlpha = new Comparison(queries.path(Attribute.STRING), ComparisonOperator.GREATER_THAN,
                ValueType.STRING, List.of(ALPHA));
        Set<String> greater = knownDeviations().contains(KnownDeviation.STRING_COMPARISON_IS_LINGUISTIC)
                ? Set.of(CAPITAL_BETA, BETA)
                : Set.of(BETA);

        assertEquals(greater.stream().map(idsByValue::get).collect(Collectors.toSet()),
                Set.copyOf(findIds(queries, greaterThanAlpha, List.of(), List.copyOf(idsByValue.values()),
                        FIRST_OFFSET, LIST_LIMIT)));
    }

    // MULTI_ELEMENT_LIST_NEVER_MATCHES hides the elements of a longer list from every filter
    private boolean reachable(Attribute attribute, Object seed) {
        return !(attribute.isList() && knownDeviations().contains(KnownDeviation.MULTI_ELEMENT_LIST_NEVER_MATCHES)
                && seed instanceof List<?> list && list.size() > 1);
    }

    /**
     * A date-time literal in the forms the cases use, both meaning {@link #EXACT}.
     */
    enum DateTimeLiteral {
        ZULU("2024-01-01T10:00:00Z"),
        OFFSET("2024-01-01T11:00:00+01:00");

        private final String literal;

        DateTimeLiteral(String literal) {
            this.literal = literal;
        }
    }

    /**
     * The ordering operators, with the outcome of a {@code compareTo(literal)} that matches.
     */
    enum Ordering {
        EQUALS(ComparisonOperator.EQUALS, comparison -> comparison == 0),
        GREATER_THAN(ComparisonOperator.GREATER_THAN, comparison -> comparison > 0),
        GREATER_THAN_EQUALS(ComparisonOperator.GREATER_THAN_EQUALS, comparison -> comparison >= 0),
        LESS_THAN(ComparisonOperator.LESS_THAN, comparison -> comparison < 0),
        LESS_THAN_EQUALS(ComparisonOperator.LESS_THAN_EQUALS, comparison -> comparison <= 0);

        private final ComparisonOperator operator;
        private final IntPredicate matches;

        Ordering(ComparisonOperator operator, IntPredicate matches) {
            this.operator = operator;
            this.matches = matches;
        }
    }

    static Stream<Arguments> dateTimeComparisons() {
        return Arrays.stream(DateTimeLiteral.values())
                .flatMap(literal -> Arrays.stream(Ordering.values()).map(ordering -> Arguments.of(literal, ordering)));
    }

    // The date-time gate of ticket 90 chose string parity: every backend compares the stored ISO strings, so
    // "...00.500Z" sorts before "...00Z" and an offset literal compares by its characters. The seeds pin that choice.
    @ParameterizedTest
    @MethodSource("dateTimeComparisons")
    void findEntitiesComparesDateTimesAsTheStoredStrings(DateTimeLiteral literal, Ordering ordering) {
        assertDateTimeComparison(queryFixture(Attribute.DATE_TIME), literal, ordering);
    }

    private <Q> void assertDateTimeComparison(QueryFixture<Q> queries, DateTimeLiteral literal, Ordering ordering) {
        Map<URI, Instant> seeds = new HashMap<>();
        DATE_TIME_SEEDS.forEach(seed -> seeds.put(
                create(queries, queries.newEntity(queries.newId(), Map.of(Attribute.DATE_TIME, seed))), seed));
        Comparison comparison = new Comparison(queries.path(Attribute.DATE_TIME), ordering.operator,
                ValueType.DATE_TIME, List.of(literal.literal));
        // Instant.toString() is the stored form: ISO-8601 in UTC, a zero fraction left out
        Set<URI> expected = seeds.entrySet().stream()
                .filter(seed -> ordering.matches.test(seed.getValue().toString().compareTo(literal.literal)))
                .map(Map.Entry::getKey)
                .collect(Collectors.toSet());

        assertEquals(expected, Set.copyOf(findIds(queries, comparison, List.of(),
                List.copyOf(seeds.keySet()), FIRST_OFFSET, LIST_LIMIT)));
    }

    // indexes of NAMED_AND_NUMBERED
    enum CompositeCase {
        ALL_OF(Set.of(1)),
        ANY_OF(Set.of(1, 3)),
        ANY_OF_ALL_OFS(Set.of(0, 3));

        private final Set<Integer> matches;

        CompositeCase(Set<Integer> matches) {
            this.matches = matches;
        }
    }

    @ParameterizedTest
    @EnumSource(CompositeCase.class)
    void findEntitiesCombinesFilters(CompositeCase compositeCase) {
        assertCombinedFilterMatches(queryFixture(Attribute.STRING, Attribute.NUMBER), compositeCase);
    }

    private <Q> void assertCombinedFilterMatches(QueryFixture<Q> queries, CompositeCase compositeCase) {
        List<URI> ids = createNamedAndNumbered(queries);
        QueryFilter filter = switch (compositeCase) {
            case ALL_OF -> new AllOf(List.of(nameIs(queries, ALPHA),
                    numberIs(queries, ComparisonOperator.GREATER_THAN, NUMBER_LITERAL)));
            case ANY_OF -> new AnyOf(List.of(nameIs(queries, ALPHA),
                    numberIs(queries, ComparisonOperator.GREATER_THAN, HIGH_NUMBER_LITERAL)));
            case ANY_OF_ALL_OFS -> new AnyOf(List.of(
                    new AllOf(List.of(nameIs(queries, ALPHA),
                            numberIs(queries, ComparisonOperator.LESS_THAN, HIGH_NUMBER_LITERAL))),
                    new AllOf(List.of(nameIs(queries, BETA),
                            numberIs(queries, ComparisonOperator.LESS_THAN, HIGH_NUMBER_LITERAL)))));
        };

        assertEquals(compositeCase.matches.stream().map(ids::get).collect(Collectors.toSet()),
                Set.copyOf(findIds(queries, filter, List.of(), ids, FIRST_OFFSET, LIST_LIMIT)));
    }

    // indexes of NUMBERED_AND_FLAGGED
    enum NumberAndBooleanCase {
        ALL_OF(Set.of(1)),
        ANY_OF(Set.of(0, 1, 2));

        private final Set<Integer> matches;

        NumberAndBooleanCase(Set<Integer> matches) {
            this.matches = matches;
        }
    }

    @ParameterizedTest
    @EnumSource(NumberAndBooleanCase.class)
    void findEntitiesCombinesNumberAndBooleanFilters(NumberAndBooleanCase numberAndBooleanCase) {
        assertNumberAndBooleanFilterMatches(queryFixture(Attribute.NUMBER, Attribute.BOOLEAN), numberAndBooleanCase);
    }

    private <Q> void assertNumberAndBooleanFilterMatches(QueryFixture<Q> queries,
                                                         NumberAndBooleanCase numberAndBooleanCase) {
        List<URI> ids = NUMBERED_AND_FLAGGED.stream()
                .map(seed -> create(queries, queries.newEntity(queries.newId(),
                        Map.of(Attribute.NUMBER, seed.getKey(), Attribute.BOOLEAN, seed.getValue()))))
                .toList();
        List<QueryFilter> comparisons = List.of(numberIs(queries, ComparisonOperator.GREATER_THAN, NUMBER_LITERAL),
                new Comparison(queries.path(Attribute.BOOLEAN), ComparisonOperator.EQUALS, ValueType.BOOLEAN,
                        List.of(Boolean.TRUE.toString())));
        QueryFilter filter = switch (numberAndBooleanCase) {
            case ALL_OF -> new AllOf(comparisons);
            case ANY_OF -> new AnyOf(comparisons);
        };

        if (knownDeviations().contains(KnownDeviation.NUMBER_AND_BOOLEAN_FILTER_FAILS)) {
            assertFailure(StorageOperation.LIST, StorageErrorReason.UNKNOWN,
                    () -> find(queries, filter, List.of(), ids, FIRST_OFFSET, LIST_LIMIT));
            return;
        }
        assertEquals(numberAndBooleanCase.matches.stream().map(ids::get).collect(Collectors.toSet()),
                Set.copyOf(findIds(queries, filter, List.of(), ids, FIRST_OFFSET, LIST_LIMIT)));
    }

    // order holds the indexes of NAMED_AND_NUMBERED in the expected order
    enum SortCase {
        NUMBER_ASCENDING(List.of(Attribute.NUMBER), List.of(false), List.of(3, 0, 2, 1)),
        NUMBER_DESCENDING(List.of(Attribute.NUMBER), List.of(true), List.of(1, 2, 0, 3)),
        STRING_ASCENDING_THEN_NUMBER_DESCENDING(List.of(Attribute.STRING, Attribute.NUMBER), List.of(false, true),
                List.of(1, 3, 0, 2)),
        STRING_DESCENDING_THEN_NUMBER_ASCENDING(List.of(Attribute.STRING, Attribute.NUMBER), List.of(true, false),
                List.of(2, 0, 3, 1));

        private final List<Attribute> attributes;
        private final List<Boolean> descending;
        private final List<Integer> order;

        SortCase(List<Attribute> attributes, List<Boolean> descending, List<Integer> order) {
            this.attributes = attributes;
            this.descending = descending;
            this.order = order;
        }
    }

    @ParameterizedTest
    @EnumSource(SortCase.class)
    void findEntitiesSorts(SortCase sortCase) {
        assertSorted(queryFixture(Attribute.STRING, Attribute.NUMBER), sortCase);
    }

    private <Q> void assertSorted(QueryFixture<Q> queries, SortCase sortCase) {
        List<URI> ids = createNamedAndNumbered(queries);
        List<SortKey> sort = IntStream.range(0, sortCase.attributes.size())
                .mapToObj(i -> sortKey(queries, sortCase.attributes.get(i), sortCase.descending.get(i)))
                .toList();

        assertOrder(sortCase.order.stream().map(ids::get).toList(),
                () -> find(queries, null, sort, ids, FIRST_OFFSET, LIST_LIMIT)
                        .map(page -> idsOf(queries, page.items())));
    }

    @Test
    void pagingUnderASortReturnsEveryEntityOnceInOrder() {
        assertPagingIsStable(queryFixture(Attribute.NUMBER));
    }

    // ties in the sort key have to keep their order across pages too
    private <Q> void assertPagingIsStable(QueryFixture<Q> queries) {
        Map<URI, Integer> numbers = new HashMap<>();
        TIED_NUMBERS.forEach(number -> numbers.put(
                create(queries, queries.newEntity(queries.newId(), Map.of(Attribute.NUMBER, number))), number));
        List<URI> ids = List.copyOf(numbers.keySet());
        List<SortKey> sort = List.of(sortKey(queries, Attribute.NUMBER, false));

        List<URI> paged = new ArrayList<>();
        for (int offset = FIRST_OFFSET; offset < ids.size(); offset += PAGE_SIZE) {
            paged.addAll(findIds(queries, null, sort, ids, offset, PAGE_SIZE));
        }

        assertEquals(ids.size(), paged.size(), "Every entity is on exactly one page: " + paged);
        assertEquals(Set.copyOf(ids), Set.copyOf(paged));
        if (!knownDeviations().contains(KnownDeviation.SORT_IS_IGNORED)) {
            List<Integer> pagedNumbers = paged.stream().map(numbers::get).toList();
            assertEquals(pagedNumbers.stream().sorted().toList(), pagedNumbers);
        }
    }

    // LISTED_ENTITIES match the filter, one more entity of the type does not
    @ParameterizedTest
    @MethodSource("pages")
    void findEntitiesPagesAndCountsAFilteredList(int offset, int limit, int expectedSize) {
        assertFilteredPage(queryFixture(Attribute.STRING), offset, limit, expectedSize);
    }

    private <Q> void assertFilteredPage(QueryFixture<Q> queries, int offset, int limit, int expectedSize) {
        List<URI> matches = IntStream.range(0, LISTED_ENTITIES)
                .mapToObj(i -> create(queries, queries.newEntity(queries.newId(), Map.of(Attribute.STRING, ALPHA))))
                .toList();
        URI other = create(queries, queries.newEntity(queries.newId(), Map.of(Attribute.STRING, BETA)));
        List<URI> ids = Stream.concat(matches.stream(), Stream.of(other)).toList();

        PagedResult<Q> page = find(queries, nameIs(queries, ALPHA), List.of(), ids, offset, limit).block(TIMEOUT);

        assertNotNull(page, "A page is always emitted, an empty one included.");
        assertEquals(expectedSize, page.items().size());
        assertTrue(matches.containsAll(idsOf(queries, page.items())), "The page holds only matching entities.");
        assertEquals(offset, page.offset());
        assertEquals(limit, page.limit());
        assertTrue(page.totalCount() == null || page.totalCount() == LISTED_ENTITIES,
                "The total count is null or counts the matches only, but was " + page.totalCount());
    }

    @Test
    void findEntitiesPolymorphicFilters() {
        assertPolymorphicFilter(queryFixture(Attribute.STRING, Attribute.NUMBER));
    }

    // without a sort, so the brokers that fail or ignore one still verify the filter
    private <Q> void assertPolymorphicFilter(QueryFixture<Q> queries) {
        String name = UUID.randomUUID().toString();
        List<URI> matches = createPolymorphicSeeds(queries, name);

        List<URI> actual = findPolymorphic(queries, new EntityQuery(queries.entityClass(), nameIs(queries, name),
                List.of())).block(TIMEOUT);

        assertNotNull(actual);
        assertEquals(matches.size(), actual.size(), "Only the matches are listed: " + actual);
        assertEquals(Set.copyOf(matches), Set.copyOf(actual));
    }

    @Test
    void findEntitiesPolymorphicFiltersAndSorts() {
        assertPolymorphicFilterAndSort(queryFixture(Attribute.STRING, Attribute.NUMBER));
    }

    private <Q> void assertPolymorphicFilterAndSort(QueryFixture<Q> queries) {
        String name = UUID.randomUUID().toString();
        List<URI> lowAndHigh = createPolymorphicSeeds(queries, name);
        EntityQuery query = new EntityQuery(queries.entityClass(), nameIs(queries, name),
                List.of(sortKey(queries, Attribute.NUMBER, true)));

        assertOrder(lowAndHigh.reversed(), () -> findPolymorphic(queries, query));
    }

    // two matches (low, then high number), a near miss of the class and an entity of the other class; no ids,
    // which SEVERAL_IDS_WITH_SEVERAL_TYPES_FAIL would reject, so the unique name keeps other entities out
    private <Q> List<URI> createPolymorphicSeeds(QueryFixture<Q> queries, String name) {
        URI low = create(queries, queries.newEntity(queries.newId(), Map.of(Attribute.STRING, name,
                Attribute.NUMBER, NUMBERS.get(0))));
        URI high = create(queries, queries.newEntity(queries.newId(), Map.of(Attribute.STRING, name,
                Attribute.NUMBER, NUMBERS.get(1))));
        create(queries, queries.newEntity(queries.newId(), Map.of(Attribute.STRING, name + BETA,
                Attribute.NUMBER, NUMBERS.get(2))));
        create(otherFixture());
        return List.of(low, high);
    }

    private <Q> Mono<List<URI>> findPolymorphic(QueryFixture<Q> queries, EntityQuery query) {
        Map<String, Class<?>> classes = Map.of(
                queries.storedType(), queries.entityClass(),
                otherFixture().storedType(), otherFixture().entityClass());
        return repository().findEntitiesPolymorphic(FIRST_OFFSET, LIST_LIMIT,
                        String.join(LIST_SEPARATOR, classes.keySet()), query, null, classes::get)
                .map(page -> page.items().stream().map(item -> idOf(queries, item)).toList());
    }

    // pins PostgreSQL's NULLS LAST in both directions; the brokers fail or ignore every sort (pending Step 7)
    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void findEntitiesSortsAnEntityWithoutTheKeyLast(boolean descending) {
        assertMissingKeySortsLast(queryFixture(Attribute.NUMBER), descending);
    }

    // created first, so a creation-order tiebreak would list the entity without the key first
    private <Q> void assertMissingKeySortsLast(QueryFixture<Q> queries, boolean descending) {
        URI missing = create(queries, queries.newEntity(queries.newId()));
        URI low = create(queries, queries.newEntity(queries.newId(), Map.of(Attribute.NUMBER, NUMBERS.get(0))));
        URI high = create(queries, queries.newEntity(queries.newId(), Map.of(Attribute.NUMBER, NUMBERS.get(1))));
        List<URI> ids = List.of(missing, low, high);
        List<SortKey> sort = List.of(sortKey(queries, Attribute.NUMBER, descending));
        List<URI> expected = descending ? List.of(high, low, missing) : List.of(low, high, missing);

        assertOrder(expected, () -> find(queries, null, sort, ids, FIRST_OFFSET, LIST_LIMIT)
                .map(page -> idsOf(queries, page.items())));
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void findEntitiesSortsOnAnExtension(boolean descending) {
        assertSortedOnExtension(queryFixture(Attribute.EXTENSION), descending);
    }

    private <Q> void assertSortedOnExtension(QueryFixture<Q> queries, boolean descending) {
        Map<String, URI> idsByValue = new HashMap<>();
        EXTENSION_SORT_SEEDS.forEach(value -> idsByValue.put(value,
                create(queries, queries.newEntity(queries.newId(), Map.of(Attribute.EXTENSION, value)))));
        // an undeclared key has no value type, as the parser leaves it
        List<SortKey> sort = List.of(new SortKey(queries.path(Attribute.EXTENSION), null, descending));
        List<String> order = descending ? List.of(GAMMA, BETA, ALPHA) : List.of(ALPHA, BETA, GAMMA);

        assertOrder(order.stream().map(idsByValue::get).toList(),
                () -> find(queries, null, sort, List.copyOf(idsByValue.values()), FIRST_OFFSET, LIST_LIMIT)
                        .map(page -> idsOf(queries, page.items())));
    }

    // a sort the backend ignores is asserted as such
    private void assertOrder(List<URI> expected, Supplier<Mono<List<URI>>> call) {
        List<URI> actual = call.get().block(TIMEOUT);
        assertNotNull(actual);
        if (knownDeviations().contains(KnownDeviation.SORT_IS_IGNORED)) {
            assertEquals(Set.copyOf(expected), Set.copyOf(actual));
            return;
        }
        assertEquals(expected, actual);
    }

    // null for an item of another class, which then shows up in the assertion
    private static <Q> URI idOf(QueryFixture<Q> queries, Object item) {
        return queries.entityClass().isInstance(item) ? queries.id(queries.entityClass().cast(item)) : null;
    }

    private <Q> List<URI> createNamedAndNumbered(QueryFixture<Q> queries) {
        return NAMED_AND_NUMBERED.stream()
                .map(seed -> create(queries, queries.newEntity(queries.newId(),
                        Map.of(Attribute.STRING, seed.getKey(), Attribute.NUMBER, seed.getValue()))))
                .toList();
    }

    private static Comparison nameIs(QueryFixture<?> queries, String name) {
        return new Comparison(queries.path(Attribute.STRING), ComparisonOperator.EQUALS, ValueType.STRING,
                List.of(name));
    }

    private static Comparison numberIs(QueryFixture<?> queries, ComparisonOperator operator, String literal) {
        return new Comparison(queries.path(Attribute.NUMBER), operator, ValueType.NUMBER, List.of(literal));
    }

    private static SortKey sortKey(QueryFixture<?> queries, Attribute attribute, boolean descending) {
        return new SortKey(queries.path(attribute), attribute.valueType(), descending);
    }

    private QueryFixture<?> queryFixture(Attribute... attributes) {
        Optional<QueryFixture<?>> queries = queryFixtures().stream()
                .filter(candidate -> candidate.attributes().containsAll(List.of(attributes)))
                .findFirst();
        assumeTrue(queries.isPresent(), "No query fixture holds " + List.of(attributes));
        return queries.get();
    }

    // the ids restrict the list to the case's own entities
    private <Q> Mono<PagedResult<Q>> find(QueryFixture<Q> queries, QueryFilter filter, List<SortKey> sort,
                                          List<URI> ids, int offset, int limit) {
        return repository().findEntities(offset, limit, queries.entityClass(),
                new EntityQuery(queries.entityClass(), filter, sort), join(ids), queries.storedType());
    }

    private <Q> List<URI> findIds(QueryFixture<Q> queries, QueryFilter filter, List<SortKey> sort, List<URI> ids,
                                  int offset, int limit) {
        PagedResult<Q> page = find(queries, filter, sort, ids, offset, limit).block(TIMEOUT);
        assertNotNull(page, "A page is always emitted, an empty one included.");
        return idsOf(queries, page.items());
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
                String.join(LIST_SEPARATOR, classes.keySet()), null, join(ids), classes::get);
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
