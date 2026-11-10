package org.fiware.tmforum.common.validation;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micronaut.core.annotation.Nullable;
import lombok.extern.slf4j.Slf4j;
import org.fiware.tmforum.common.exception.NonExistentReferenceException;
import org.fiware.tmforum.storage.ReferenceReadRepository;
import org.fiware.tmforum.storage.StoredReference;
import reactor.core.publisher.Mono;

import jakarta.inject.Singleton;
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

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
     * Check if all local referenced entities exist with an accepted type, with one storage lookup for the whole list.
     * Null entries are ignored, as are external ones whose id names no local entity, so an empty, null, all-null or
     * all-external list emits true.
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
     * Always emits a (possibly empty) list; a null list counts as empty. At most one {@code findReferences} call per
     * subscription, none when no id can resolve.
     */
    public <T extends ReferencedEntity> Mono<List<T>> findUnresolved(List<T> references) {
        if (references == null) {
            return Mono.just(List.of());
        }
        // deferred, so each subscription classifies, looks up and counts afresh
        return Mono.defer(() -> {
            List<ClassifiedReference<T>> classified = references.stream()
                    .filter(Objects::nonNull)
                    .map(ref -> new ClassifiedReference<>(ref, referenceClassifier.classify(ref)))
                    .toList();
            return storedTypes(classified).map(storedTypes -> classified.stream()
                    .filter(reference -> !resolves(reference, storedTypes))
                    .map(ClassifiedReference::reference)
                    .toList());
        });
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

    // one lookup for all ids of the list that can resolve, keyed by id
    private Mono<Map<String, String>> storedTypes(List<? extends ClassifiedReference<?>> references) {
        Set<String> ids = references.stream()
                .map(ClassifiedReference::reference)
                .filter(ReferenceValidationService::hasLookupId)
                .map(ref -> ref.getEntityId().toString())
                .collect(Collectors.toSet());
        if (ids.isEmpty()) {
            return Mono.just(Map.of());
        }
        return referencesRepository.findReferences(ids).collectMap(StoredReference::id, StoredReference::type);
    }

    private boolean resolves(ClassifiedReference<?> classified, Map<String, String> storedTypes) {
        ReferencedEntity ref = classified.reference();
        URI entityId = ref.getEntityId();
        if (!hasLookupId(ref)) {
            // the broker would answer such an id with a 400, which would surface as a 500 instead of a 400
            log.warn("The reference id '{}' is not an absolute URI (e.g. urn:ngsi-ld:category:1), so it cannot "
                    + "resolve.", entityId);
            return count(scopeTag(classified.scope()), false);
        }
        String storedType = storedTypes.get(entityId.toString());
        return switch (classified.scope()) {
            // a local entity with that id must not be smuggled in under another type
            case EXTERNAL -> storedType == null
                    ? skipExternal(entityId)
                    : count(SCOPE_EXTERNAL, ref.getReferencedTypes().contains(storedType));
            case LOCAL -> count(SCOPE_LOCAL, storedType != null && ref.getReferencedTypes().contains(storedType));
        };
    }

    private static boolean hasLookupId(ReferencedEntity ref) {
        URI entityId = ref.getEntityId();
        return entityId != null && entityId.isAbsolute();
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

    private record ClassifiedReference<T extends ReferencedEntity>(T reference, ReferenceScope scope) {
    }
}
