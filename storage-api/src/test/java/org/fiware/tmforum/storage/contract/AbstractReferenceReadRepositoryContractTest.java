package org.fiware.tmforum.storage.contract;

import org.fiware.tmforum.storage.EntityWriteRepository;
import org.fiware.tmforum.storage.ReferenceReadRepository;
import org.fiware.tmforum.storage.StorageException;
import org.fiware.tmforum.storage.StoredReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static org.fiware.tmforum.storage.contract.AbstractEntityRepositoryContractTest.TIMEOUT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * The {@link ReferenceReadRepository} contract from its Javadoc, run by every adapter against its live backend;
 * ticket 6 reuses it for PostgreSQL.
 */
public abstract class AbstractReferenceReadRepositoryContractTest<T> {

    private static final String OTHER_TYPE = "contract-test-other-type";
    private static final String NO_VERSION = "";
    private static final String VERSION = "1.0";
    private static final String UPDATED_VERSION = "2.0";

    private final List<URI> createdIds = new ArrayList<>();

    protected abstract ReferenceReadRepository repository();

    /**
     * Writes the referenced entities; its own contract is {@link AbstractEntityRepositoryContractTest}'s.
     */
    protected abstract EntityWriteRepository writeRepository();

    protected abstract EntityFixture<T> fixture();

    /**
     * Entities of a type that is not versionable for the extension-version case, which skips when empty.
     */
    protected Optional<UnversionedFixture<?>> unversionedFixture() {
        return Optional.empty();
    }

    enum AcceptedTypes {OWN, OWN_AMONG_OTHERS, OTHERS_ONLY, NONE}

    @AfterEach
    void deleteCreatedEntities() {
        createdIds.forEach(id -> writeRepository().deleteDomainEntity(id)
                .onErrorResume(StorageException.class, e -> Mono.empty())
                .block(TIMEOUT));
        createdIds.clear();
    }

    @ParameterizedTest
    @EnumSource(AcceptedTypes.class)
    void referenceExistsOnlyForAnAcceptedStoredType(AcceptedTypes acceptedTypes) {
        URI id = create();
        List<String> types = switch (acceptedTypes) {
            case OWN -> List.of(fixture().storedType());
            case OWN_AMONG_OTHERS -> List.of(OTHER_TYPE, fixture().storedType());
            case OTHERS_ONLY -> List.of(OTHER_TYPE);
            case NONE -> List.of();
        };
        StoredReference expected = types.contains(fixture().storedType())
                ? new StoredReference(id.toString(), fixture().storedType(), NO_VERSION)
                : null;

        assertEquals(expected, repository().referenceExists(id.toString(), types).block(TIMEOUT));
    }

    @Test
    void findReferenceReturnsIdAndStoredType() {
        URI id = create();

        assertEquals(new StoredReference(id.toString(), fixture().storedType(), NO_VERSION),
                repository().findReference(id.toString()).block(TIMEOUT));
    }

    @Test
    void unknownIdHasNoReference() {
        String id = fixture().newId().toString();

        assertNull(repository().findReference(id).block(TIMEOUT));
        assertNull(repository().referenceExists(id, List.of(fixture().storedType())).block(TIMEOUT));
    }

    enum Lookup {NONE, UNKNOWN_ONLY, ONE, MIXED_WITH_UNKNOWN, DUPLICATES}

    @ParameterizedTest
    @EnumSource(Lookup.class)
    void findReferencesEmitsEachExistingIdOnce(Lookup lookup) {
        String first = create().toString();
        String second = create().toString();
        String unknown = fixture().newId().toString();
        List<String> ids = switch (lookup) {
            case NONE -> List.of();
            case UNKNOWN_ONLY -> List.of(unknown);
            case ONE -> List.of(first);
            case MIXED_WITH_UNKNOWN -> List.of(unknown, first, second);
            case DUPLICATES -> List.of(second, first, second, first);
        };
        Set<StoredReference> expected = ids.stream()
                .filter(id -> !id.equals(unknown))
                .map(id -> new StoredReference(id, fixture().storedType(), NO_VERSION))
                .collect(Collectors.toSet());

        List<StoredReference> found = repository().findReferences(ids).collectList().block(TIMEOUT);

        assertEquals(expected, Set.copyOf(found));
        assertEquals(expected.size(), found.size(), "Each existing id is emitted once.");
    }

    @Test
    void findReferencesSkipsDeletedEntities() {
        URI deleted = create();
        String kept = create().toString();
        writeRepository().deleteDomainEntity(deleted).block(TIMEOUT);

        assertEquals(List.of(new StoredReference(kept, fixture().storedType(), NO_VERSION)),
                repository().findReferences(List.of(deleted.toString(), kept)).collectList().block(TIMEOUT));
    }

    enum CreatedVersion {
        LABELLED(VERSION), NONE(NO_VERSION);

        private final String expected;

        CreatedVersion(String expected) {
            this.expected = expected;
        }
    }

    @ParameterizedTest
    @EnumSource(CreatedVersion.class)
    void referencesReportTheStoredVersion(CreatedVersion createdVersion) {
        VersionFixture<T> versions = versionFixture();
        URI id = versions.newId();
        create(id, createdVersion == CreatedVersion.LABELLED
                ? versions.withVersion(id, VERSION)
                : versions.newEntity(id));
        StoredReference expected = new StoredReference(id.toString(), versions.storedType(), createdVersion.expected);

        assertEquals(expected, repository().findReference(id.toString()).block(TIMEOUT));
        assertEquals(List.of(expected),
                repository().findReferences(List.of(id.toString())).collectList().block(TIMEOUT));
    }

    @Test
    void findReferenceReportsTheUpdatedVersion() {
        VersionFixture<T> versions = versionFixture();
        URI id = versions.newId();
        create(id, versions.withVersion(id, VERSION));
        writeRepository().updateDomainEntity(id.toString(), versions.withVersion(id, UPDATED_VERSION)).block(TIMEOUT);

        assertEquals(new StoredReference(id.toString(), versions.storedType(), UPDATED_VERSION),
                repository().findReference(id.toString()).block(TIMEOUT));
    }

    // an extension must not make a type versionable, otherwise a pinned reference would resolve on one backend only
    @Test
    void extensionVersionOfAnUnversionedTypeIsNotReported() {
        Optional<UnversionedFixture<?>> unversioned = unversionedFixture();
        assumeTrue(unversioned.isPresent(), "The adapter provides no unversioned fixture.");
        UnversionedFixture<?> fixture = unversioned.get();
        URI id = fixture.newId();
        create(id, fixture.withExtensionVersion(id, VERSION));
        StoredReference expected = new StoredReference(id.toString(), fixture.storedType(), NO_VERSION);

        assertEquals(expected, repository().findReference(id.toString()).block(TIMEOUT));
        assertEquals(List.of(expected),
                repository().findReferences(List.of(id.toString())).collectList().block(TIMEOUT));
    }

    @SuppressWarnings("unchecked")
    private VersionFixture<T> versionFixture() {
        assumeTrue(fixture() instanceof VersionFixture, "The fixture's class is not versionable.");
        return (VersionFixture<T>) fixture();
    }

    private URI create() {
        T entity = fixture().newEntity(fixture().newId());
        URI id = fixture().id(entity);
        create(id, entity);
        return id;
    }

    private void create(URI id, Object entity) {
        createdIds.add(id);
        writeRepository().createDomainEntity(entity).block(TIMEOUT);
    }
}
