package org.fiware.tmforum.storage;

import reactor.core.publisher.Mono;

import java.util.List;

/**
 * Existence checks for referenced entities. Failures carry {@link StorageOperation#READ}.
 */
public interface ReferenceReadRepository {

    /**
     * Emits the reference if the entity exists and its stored type is one of {@code acceptedTypes}, else empty.
     */
    Mono<StoredReference> referenceExists(String id, List<String> acceptedTypes);

    /**
     * Emits the reference regardless of its type, or empty if the entity does not exist.
     */
    Mono<StoredReference> findReference(String id);
}
