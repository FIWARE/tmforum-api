package org.fiware.tmforum.storage;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Collection;
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

    /**
     * Emits one reference per distinct id of {@code ids} (no nulls) that exists, in no particular order; unknown ids
     * are skipped; a failure carries {@link StorageOperation#READ}. The default fans out to {@link #findReference};
     * an adapter overrides it with a single lookup.
     */
    default Flux<StoredReference> findReferences(Collection<String> ids) {
        return Flux.fromIterable(ids).distinct().flatMap(this::findReference);
    }
}
