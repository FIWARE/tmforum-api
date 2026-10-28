package org.fiware.tmforum.common.validation;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micronaut.core.annotation.Nullable;
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
public class ReferenceValidationService {

    /** Counter of validated references, tagged by scope and result. */
    public static final String REFERENCES_METRIC = "tmforum.references";
    public static final String SCOPE_TAG = "scope";
    public static final String RESULT_TAG = "result";
    public static final String SCOPE_LOCAL = "local";
    public static final String SCOPE_EXTERNAL = "external";
    public static final String RESULT_RESOLVED = "resolved";
    public static final String RESULT_UNRESOLVED = "unresolved";
    public static final String RESULT_SKIPPED = "skipped";

    private final ReferencesRepository referencesRepository;
    private final ReferenceClassifier referenceClassifier;
    @Nullable
    private final MeterRegistry meterRegistry;

    /**
     * The registry is optional, so contexts with metrics disabled still start; references are then not counted.
     */
    public ReferenceValidationService(ReferencesRepository referencesRepository,
                                      ReferenceClassifier referenceClassifier,
                                      @Nullable MeterRegistry meterRegistry) {
        this.referencesRepository = referencesRepository;
        this.referenceClassifier = referenceClassifier;
        this.meterRegistry = meterRegistry;
    }

    /**
     * Check if all local referenced entities exist with an accepted type. The checks will be executed asynchronously to
     * not block each other on sequential calls to the NGSI-LD api. Null entries are ignored, as are external ones whose
     * id names no local entity, so an empty, null, all-null or all-external list emits true.
     *
     * @param references a list of referenced objects to be checked for existence
     * @param <T>        concrete type of the referenced entity
     * @return a single, emitting the check result
     */
    public <T extends ReferencedEntity> Mono<Boolean> checkReferenceExists(List<T> references) {
        return findUnresolved(references).map(List::isEmpty);
    }

    /**
     * Emits the non-null references that do not resolve to an entity with an accepted type, in input order. External
     * references only count when their id names a local entity of another type.
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
        return switch (referenceClassifier.classify(ref)) {
            case EXTERNAL -> referencesRepository.findReference(ref.getEntityId().toString())
                    // a local entity with that id must not be smuggled in under another type
                    .map(entityVO -> countLocal(ref.getReferencedTypes().contains(entityVO.getType())))
                    .switchIfEmpty(Mono.fromSupplier(() -> skipExternal(ref)));
            case LOCAL -> referencesRepository.referenceExists(ref.getEntityId().toString(), ref.getReferencedTypes())
                    .map(entityVO -> true)
                    .defaultIfEmpty(false)
                    .map(this::countLocal);
        };
    }

    private boolean skipExternal(ReferencedEntity ref) {
        log.debug("Skipping validation of external reference {}.", ref.getEntityId());
        count(SCOPE_EXTERNAL, RESULT_SKIPPED);
        return true;
    }

    private boolean countLocal(boolean resolves) {
        count(SCOPE_LOCAL, resolves ? RESULT_RESOLVED : RESULT_UNRESOLVED);
        return resolves;
    }

    private void count(String scope, String result) {
        if (meterRegistry == null) {
            return;
        }
        Counter.builder(REFERENCES_METRIC)
                .tag(SCOPE_TAG, scope)
                .tag(RESULT_TAG, result)
                .register(meterRegistry)
                .increment();
    }

}
