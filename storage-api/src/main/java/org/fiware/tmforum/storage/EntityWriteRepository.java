package org.fiware.tmforum.storage;

import reactor.core.publisher.Mono;

import java.net.URI;

/**
 * Write side of the entity port. Each method completes empty on success and fails with a
 * {@link StorageException} carrying its own {@link StorageOperation}.
 */
public interface EntityWriteRepository {

    /**
     * Fails with {@link StorageErrorReason#CONFLICT} when the id exists and {@link StorageErrorReason#INVALID_DATA}
     * when the backend rejects the content.
     */
    <T> Mono<Void> createDomainEntity(T domainEntity);

    /**
     * Writes only the attributes the entity carries, never a whole-document overwrite: null fields stay
     * untouched, an emptied list is cleared and reads back absent or empty, so callers must accept both. A carried
     * list replaces the stored one.
     * Fails with {@link StorageErrorReason#NOT_FOUND} for an absent id and never creates or completes silently
     * (update-else-create callers rely on it), and with {@link StorageErrorReason#PARTIAL_UPDATE} when the
     * backend wrote only some attributes. Failures carry UPDATE, except the recorded NGSI-LD replace-mode
     * pre-read (see {@link StorageOperation}).
     * <p>
     * Known NGSI-LD adapter defect, not a contract rule (ticket 0): on Orion-LD in append mode (no
     * {@code replaceOnUpdate}) an emptied list whose instances carry a datasetId is not cleared; the broker keeps
     * them and adds an empty instance, so later reads fail. For the same reason any carried list whose instances
     * carry a datasetId is merged into the stored one by datasetId instead of replacing it: a stored instance whose
     * datasetId the update does not carry survives next to the carried ones. For a reference list the key is the
     * reference's datasetId, not its id; for a property list it is the position ({@code list-item:n}), so shortening
     * the list keeps the stored tail. Only the reference-list case is pinned by a contract case.
     */
    <T> Mono<Void> updateDomainEntity(String id, T domainEntity);

    /**
     * Fails with {@link StorageErrorReason#NOT_FOUND} when the id is absent.
     */
    Mono<Void> deleteDomainEntity(URI id);
}
