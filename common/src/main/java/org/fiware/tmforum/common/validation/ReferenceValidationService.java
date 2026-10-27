package org.fiware.tmforum.common.validation;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.fiware.tmforum.common.exception.NonExistentReferenceException;
import org.fiware.tmforum.common.repository.ReferencesRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import jakarta.inject.Singleton;
import java.util.List;
import java.util.Objects;

/**
 * Service to support validation of referential integrity.
 */
@Slf4j
@Singleton
@RequiredArgsConstructor
public class ReferenceValidationService {

    private final ReferencesRepository referencesRepository;

    /**
     * Check if all referenced entities exist with an accepted type. The checks will be executed asynchronously to not
     * block each other on sequential calls to the NGSI-LD api. Null entries are ignored, so an empty, null or all-null
     * list emits true.
     *
     * @param references a list of referenced objects to be checked for existence
     * @param <T>        concrete type of the referenced entity
     * @return a single, emitting the check result
     */
    public <T extends ReferencedEntity> Mono<Boolean> checkReferenceExists(List<T> references) {
        return findUnresolved(references).map(List::isEmpty);
    }

    /**
     * Emits the non-null references that do not resolve to an entity with an accepted type, in input order.
     * Always emits a (possibly empty) list; a null list counts as empty.
     */
    public <T extends ReferencedEntity> Mono<List<T>> findUnresolved(List<T> references) {
        if (references == null) {
            return Mono.just(List.of());
        }
        // Flux.fromIterable rejects null elements; the supplier gives each subscription a fresh stream
        return Flux.fromStream(() -> references.stream().filter(Objects::nonNull))
                .flatMapSequential(ref -> referenceResolves(ref)
                        .filter(resolves -> !resolves)
                        .map(resolves -> ref))
                .collectList();
    }

    /**
     * Convenience method for checking and returning a given object for easier integration in an async-chain
     *
     * @param refs         the references to check
     * @param returnObject the object to be returned in case all references exist
     * @param <T>          actual type of the references to check
     * @param <R>          type of the object to be returened
     * @return a mono emitting the returnObject
     * @throws NonExistentReferenceException will be thrown in case not all references exist
     */
    public <T extends ReferencedEntity, R> Mono<R> getCheckingMono(List<T> refs, R returnObject) {
        return findUnresolved(refs)
                .flatMap(unresolved -> {
                    if (unresolved.isEmpty()) {
                        return Mono.just(returnObject);
                    }
                    List<String> unresolvedIds = unresolved.stream()
                            .map(ref -> String.valueOf(ref.getEntityId()))
                            .toList();
                    return Mono.error(new NonExistentReferenceException(
                            String.format("References do not exist: %s", unresolvedIds)));
                });
    }

    private Mono<Boolean> referenceResolves(ReferencedEntity ref) {
        return referencesRepository.referenceExists(ref.getEntityId().toString(), ref.getReferencedTypes())
                .map(entityVO -> true)
                .defaultIfEmpty(false);
    }

}
