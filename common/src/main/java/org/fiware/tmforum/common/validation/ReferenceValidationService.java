package org.fiware.tmforum.common.validation;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.fiware.tmforum.common.exception.NonExistentReferenceException;
import org.fiware.tmforum.common.repository.ReferencesRepository;
import reactor.core.publisher.Mono;

import javax.inject.Singleton;
import java.util.Arrays;
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
     * Check if all referenced entities exist. The checks will be executed asynchronously to not block each other
     * on sequential calls to the NGSI-LD api
     *
     * @param references a list of referenced objects to be checked for existence
     * @param <T>        concrete type of the referenced entity
     * @return a single, emitting the check result
     */
    public <T extends ReferencedEntity> Mono<Boolean> checkReferenceExists(List<T> references) {

        return Mono.zip(
                references.stream()
                        .filter(Objects::nonNull)
                        .map(ref -> referencesRepository.referenceExists(ref.getEntityId().toString(), ref.getReferencedTypes())
                                .map(eVo -> true)
                                .defaultIfEmpty(false))
                        .toList(),
                t -> Arrays.stream(t).anyMatch(Boolean.class::cast));
    }

    /**
     * Convenience method for checking and returning a given object for easier integration in an async-chain
     *
     * @param refs         the references to check
     * @param returnObject the object to be returned in case all references exist
     * @param <T>          actual type of the references to check
     * @param <R>          type of the object to be returened
     * @return a mono emitting the returnObject
     * @throws NonExistentReferenceException will be thrown in case a reference does not exist
     */
    public <T extends ReferencedEntity, R> Mono<R> getCheckingMono(List<T> refs, R returnObject) {
        return checkReferenceExists(refs)
                .flatMap(res -> {
                    if (res) {
                        return Mono.just(returnObject);
                    }
                    return Mono.error(new NonExistentReferenceException(String.format("Not all references exist. References: %s", refs)));
                });
    }

}
