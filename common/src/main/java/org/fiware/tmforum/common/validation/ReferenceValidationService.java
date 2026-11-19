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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Service to support validation of referential integrity; a reference with a version also has to match its target's.
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

    private static final String MISSING_FORMAT = "References do not exist: %s";
    private static final String VERSION_MISMATCH_FORMAT = "Reference version mismatch: %s";
    private static final String MISMATCH_ENTRY_FORMAT = "%s (version %s, current %s)";
    private static final String MESSAGE_SEPARATOR = "; ";

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
     * Check if all local referenced entities exist with an accepted type and, when pinned, the referenced version, with
     * one storage lookup for the whole list.
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
     * Emits the non-null references that do not resolve to an entity with an accepted type and, when pinned, the
     * referenced version, in input order. External references only count when their id names a local entity of another
     * type or version. A reference whose id is not an absolute URI never resolves, since NGSI-LD ids are URIs.
     * Always emits a (possibly empty) list; a null list counts as empty. At most one {@code findReferences} call per
     * subscription, none when no id can resolve.
     */
    public <T extends ReferencedEntity> Mono<List<T>> findUnresolved(List<T> references) {
        return findFailures(references).map(failures -> failures.stream().map(Failure::reference).toList());
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
        return findFailures(refs)
                .flatMap(failures -> failures.isEmpty()
                        ? Mono.just(returnObject)
                        : Mono.error(new NonExistentReferenceException(failureMessage(failures))));
    }

    private <T extends ReferencedEntity> Mono<List<Failure<T>>> findFailures(List<T> references) {
        if (references == null) {
            return Mono.just(List.of());
        }
        // deferred, so each subscription classifies, looks up and counts afresh
        return Mono.defer(() -> {
            List<ClassifiedReference<T>> classified = references.stream()
                    .filter(Objects::nonNull)
                    .map(ref -> new ClassifiedReference<>(ref, referenceClassifier.classify(ref)))
                    .toList();
            return storedReferences(classified).map(stored -> classified.stream()
                    .map(reference -> new Failure<>(reference.reference(), resolve(reference, stored),
                            stored.get(String.valueOf(reference.reference().getEntityId()))))
                    .filter(failure -> failure.resolution() != Resolution.RESOLVED)
                    .toList());
        });
    }

    private static String failureMessage(List<? extends Failure<?>> failures) {
        List<String> missing = failures.stream()
                .filter(failure -> failure.resolution() == Resolution.MISSING)
                .map(failure -> String.valueOf(failure.reference().getEntityId()))
                .toList();
        List<String> mismatched = failures.stream()
                .filter(failure -> failure.resolution() == Resolution.VERSION_MISMATCH)
                .map(failure -> String.format(MISMATCH_ENTRY_FORMAT, failure.reference().getEntityId(),
                        failure.reference().getReferenceVersion(), failure.stored().version()))
                .toList();
        List<String> parts = new ArrayList<>();
        if (!missing.isEmpty()) {
            parts.add(String.format(MISSING_FORMAT, missing));
        }
        if (!mismatched.isEmpty()) {
            parts.add(String.format(VERSION_MISMATCH_FORMAT, mismatched));
        }
        return String.join(MESSAGE_SEPARATOR, parts);
    }

    // one lookup for all ids of the list that can resolve, keyed by id
    private Mono<Map<String, StoredReference>> storedReferences(List<? extends ClassifiedReference<?>> references) {
        Set<String> ids = references.stream()
                .map(ClassifiedReference::reference)
                .filter(ReferenceValidationService::hasLookupId)
                .map(ref -> ref.getEntityId().toString())
                .collect(Collectors.toSet());
        if (ids.isEmpty()) {
            return Mono.just(Map.of());
        }
        return referencesRepository.findReferences(ids).collectMap(StoredReference::id, Function.identity());
    }

    private Resolution resolve(ClassifiedReference<?> classified, Map<String, StoredReference> storedReferences) {
        ReferencedEntity ref = classified.reference();
        URI entityId = ref.getEntityId();
        String scope = scopeTag(classified.scope());
        if (!hasLookupId(ref)) {
            // the broker would answer such an id with a 400, which would surface as a 500 instead of a 400
            log.warn("The reference id '{}' is not an absolute URI (e.g. urn:ngsi-ld:category:1), so it cannot "
                    + "resolve.", entityId);
            return count(scope, Resolution.MISSING);
        }
        StoredReference stored = storedReferences.get(entityId.toString());
        if (stored == null) {
            return classified.scope() == ReferenceScope.EXTERNAL
                    ? skipExternal(entityId)
                    : count(scope, Resolution.MISSING);
        }
        // a local entity with that id must not be smuggled in under another type or version, not even as external
        if (!ref.getReferencedTypes().contains(stored.type())) {
            return count(scope, Resolution.MISSING);
        }
        return count(scope, versionMatches(ref, stored) ? Resolution.RESOLVED : Resolution.VERSION_MISMATCH);
    }

    private static boolean versionMatches(ReferencedEntity ref, StoredReference stored) {
        String refVersion = ref.getReferenceVersion();
        return refVersion == null || refVersion.isEmpty() || refVersion.equals(stored.version());
    }

    private static boolean hasLookupId(ReferencedEntity ref) {
        URI entityId = ref.getEntityId();
        return entityId != null && entityId.isAbsolute();
    }

    private Resolution skipExternal(URI entityId) {
        log.info("Skipping validation of external reference {}.", entityId);
        count(SCOPE_EXTERNAL, RESULT_SKIPPED);
        return Resolution.RESOLVED;
    }

    private static String scopeTag(ReferenceScope scope) {
        return switch (scope) {
            case EXTERNAL -> SCOPE_EXTERNAL;
            case LOCAL -> SCOPE_LOCAL;
        };
    }

    private Resolution count(String scope, Resolution resolution) {
        count(scope, resolution == Resolution.RESOLVED ? RESULT_RESOLVED : RESULT_UNRESOLVED);
        return resolution;
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

    private enum Resolution { RESOLVED, MISSING, VERSION_MISMATCH }

    // stored is the found target, null when there is none
    private record Failure<T extends ReferencedEntity>(T reference, Resolution resolution,
                                                        @Nullable StoredReference stored) {
    }
}
