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

import static org.fiware.tmforum.storage.contract.AbstractEntityRepositoryContractTest.TIMEOUT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * The {@link ReferenceReadRepository} contract from its Javadoc, run by every adapter against its live backend;
 * ticket 6 reuses it for PostgreSQL.
 */
public abstract class AbstractReferenceReadRepositoryContractTest<T> {

    private static final String OTHER_TYPE = "contract-test-other-type";

    private final List<URI> createdIds = new ArrayList<>();

    protected abstract ReferenceReadRepository repository();

    /**
     * Writes the referenced entities; its own contract is {@link AbstractEntityRepositoryContractTest}'s.
     */
    protected abstract EntityWriteRepository writeRepository();

    protected abstract EntityFixture<T> fixture();

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
                ? new StoredReference(id.toString(), fixture().storedType())
                : null;

        assertEquals(expected, repository().referenceExists(id.toString(), types).block(TIMEOUT));
    }

    @Test
    void findReferenceReturnsIdAndStoredType() {
        URI id = create();

        assertEquals(new StoredReference(id.toString(), fixture().storedType()),
                repository().findReference(id.toString()).block(TIMEOUT));
    }

    @Test
    void unknownIdHasNoReference() {
        String id = fixture().newId().toString();

        assertNull(repository().findReference(id).block(TIMEOUT));
        assertNull(repository().referenceExists(id, List.of(fixture().storedType())).block(TIMEOUT));
    }

    private URI create() {
        T entity = fixture().newEntity(fixture().newId());
        URI id = fixture().id(entity);
        createdIds.add(id);
        writeRepository().createDomainEntity(entity).block(TIMEOUT);
        return id;
    }
}
