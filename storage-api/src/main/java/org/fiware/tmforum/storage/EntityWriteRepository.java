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
     * untouched, an emptied list is cleared and reads back absent or empty, so callers must accept both.
     * Fails with {@link StorageErrorReason#NOT_FOUND} for an absent id and never creates or completes silently
     * (update-else-create callers rely on it), and with {@link StorageErrorReason#PARTIAL_UPDATE} when the
     * backend wrote only some attributes. Failures carry UPDATE, except the recorded NGSI-LD replace-mode
     * pre-read (see {@link StorageOperation}).
     * <p>
     * Known NGSI-LD dialect: in append mode (Orion-LD, no {@code replaceOnUpdate}) an emptied list whose instances
     * carry a datasetId is not cleared; the broker keeps them and adds an empty instance, so later reads fail.
     */
    <T> Mono<Void> updateDomainEntity(String id, T domainEntity);

    /**
     * Fails with {@link StorageErrorReason#NOT_FOUND} when the id is absent.
     */
    Mono<Void> deleteDomainEntity(URI id);
}
