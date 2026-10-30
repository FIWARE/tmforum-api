package org.fiware.tmforum.common.validation;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micronaut.core.annotation.Nullable;
import lombok.extern.slf4j.Slf4j;
import org.fiware.tmforum.common.exception.NonExistentReferenceException;
import org.fiware.tmforum.storage.ReferenceReadRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import jakarta.inject.Singleton;
import java.net.URI;
import java.util.List;
import java.util.Objects;

/**
 * Service to support validation of referential integrity.
 */
@Slf4j
@Singleton
public class ReferenceValidationService {

    /**
     * Counter of validated references, tagged by scope and result. Scope is the classification, so an external
     * reference whose id names a local entity counts as external resolved/unresolved, not as local.
     */
    public static final String REFERENCES_METRIC = "tmforum.references";
    public static final String SCOPE_TAG = "scope";
    public static final String RESULT_TAG = "result";
    public static final String SCOPE_LOCAL = "local";
    public static final String SCOPE_EXTERNAL = "external";
    public static final String RESULT_RESOLVED = "resolved";
    public static final String RESULT_UNRESOLVED = "unresolved";
    public static final String RESULT_SKIPPED = "skipped";

    private final ReferenceReadRepository referencesRepository;
    private final ReferenceClassifier referenceClassifier;
    @Nullable
    private final MeterRegistry meterRegistry;

    /**
     * The registry is optional, so contexts with metrics disabled still start; references are then not counted.
     */
    public ReferenceValidationService(ReferenceReadRepository referencesRepository,
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
     * references only count when their id names a local entity of another type. A reference whose id is not an
     * absolute URI never resolves, since NGSI-LD ids are URIs.
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
        ReferenceScope scope = referenceClassifier.classify(ref);
        URI entityId = ref.getEntityId();
        if (entityId == null || !entityId.isAbsolute()) {
            // the broker would answer such an id with a 400, which would surface as a 500 instead of a 400
            log.warn("The reference id '{}' is not an absolute URI (e.g. urn:ngsi-ld:category:1), so it cannot "
                    + "resolve.", entityId);
            return Mono.just(count(scopeTag(scope), false));
        }
        return switch (scope) {
            case EXTERNAL -> referencesRepository.findReference(entityId.toString())
                    // a local entity with that id must not be smuggled in under another type
                    .map(reference -> count(SCOPE_EXTERNAL, ref.getReferencedTypes().contains(reference.type())))
                    .switchIfEmpty(Mono.fromSupplier(() -> skipExternal(entityId)));
            case LOCAL -> referencesRepository.referenceExists(entityId.toString(), ref.getReferencedTypes())
                    .map(reference -> true)
                    .defaultIfEmpty(false)
                    .map(resolves -> count(SCOPE_LOCAL, resolves));
        };
    }

    private boolean skipExternal(URI entityId) {
        log.info("Skipping validation of external reference {}.", entityId);
        count(SCOPE_EXTERNAL, RESULT_SKIPPED);
        return true;
    }

    private static String scopeTag(ReferenceScope scope) {
        return switch (scope) {
            case EXTERNAL -> SCOPE_EXTERNAL;
            case LOCAL -> SCOPE_LOCAL;
        };
    }

    private boolean count(String scope, boolean resolves) {
        count(scope, resolves ? RESULT_RESOLVED : RESULT_UNRESOLVED);
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
