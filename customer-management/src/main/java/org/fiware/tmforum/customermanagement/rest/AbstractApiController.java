package org.fiware.tmforum.customermanagement.rest;

import io.micronaut.http.HttpResponse;
import io.micronaut.http.client.exceptions.HttpClientResponseException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.fiware.tmforum.common.domain.EntityWithId;
import org.fiware.tmforum.common.mapping.IdHelper;
import org.fiware.tmforum.common.validation.ReferenceValidationService;
import org.fiware.tmforum.common.validation.ReferencedEntity;
import org.fiware.tmforum.customermanagement.TMForumMapper;
import org.fiware.tmforum.customermanagement.exception.CustomerManagementException;
import org.fiware.tmforum.customermanagement.exception.CustomerManagementExceptionReason;
import org.fiware.tmforum.customermanagement.repository.CustomerManagementRepository;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Stream;

import static org.fiware.tmforum.common.CommonConstants.DEFAULT_LIMIT;
import static org.fiware.tmforum.common.CommonConstants.DEFAULT_OFFSET;

@Slf4j
@RequiredArgsConstructor
public abstract class AbstractApiController {


    protected final TMForumMapper tmForumMapper;
    protected final ReferenceValidationService validationService;
    protected final CustomerManagementRepository customerManagementRepository;

    protected <T> Mono<T> getCheckingMono(T entityToCheck, List<List<? extends ReferencedEntity>> referencedEntities) {
        Mono<T> checkingMono = Mono.just(entityToCheck);
        for (List<? extends ReferencedEntity> referencedEntitiesList : referencedEntities) {
            if (referencedEntitiesList != null && !referencedEntitiesList.isEmpty()) {
                checkingMono = Mono.zip(checkingMono, validationService.getCheckingMono(referencedEntitiesList, entityToCheck), (p1, p2) -> p1);
            }
        }
        return checkingMono;
    }

    protected <T> Mono<T> create(Mono<T> checkingMono, Class<T> entityClass) {
        return checkingMono
                .flatMap(checkedResult -> customerManagementRepository.createDomainEntity(checkedResult).then(Mono.just(checkedResult)))
                .onErrorMap(t -> {
                    if (t instanceof HttpClientResponseException e) {
                        return switch (e.getStatus()) {
                            case CONFLICT -> new CustomerManagementException(String.format("Conflict on creating the entity: %s", e.getMessage()), CustomerManagementExceptionReason.CONFLICT);
                            case BAD_REQUEST -> new CustomerManagementException(String.format("Did not receive a valid entity: %s.", e.getMessage()), CustomerManagementExceptionReason.INVALID_DATA);
                            default -> new CustomerManagementException(String.format("Unspecified downstream error: %s", e.getMessage()), CustomerManagementExceptionReason.UNKNOWN);
                        };
                    } else {
                        return t;
                    }
                })
                .cast(entityClass);
    }

    protected Mono<HttpResponse<Object>> delete(String id) {
        // non-ngsi-ld ids cannot exist.
        if (!IdHelper.isNgsiLdId(id)) {
            throw new CustomerManagementException("Did not receive a valid id, such entity cannot exist.", CustomerManagementExceptionReason.NOT_FOUND);
        }
        return customerManagementRepository.deleteDomainEntity(URI.create(id))
                .then(Mono.just(HttpResponse.noContent()));
    }

    protected <R> Mono<Stream<R>> list(Integer offset, Integer limit, String type, Class<R> entityClass) {
        offset = Optional.ofNullable(offset).orElse(DEFAULT_OFFSET);
        limit = Optional.ofNullable(limit).orElse(DEFAULT_LIMIT);

        if (offset < 0 || limit < 1) {
            throw new CustomerManagementException(String.format("Invalid offset %s or limit %s.", offset, limit), CustomerManagementExceptionReason.INVALID_DATA);
        }

        return customerManagementRepository
                .findEntities(offset, limit, type, entityClass)
                .map(List::stream);
    }

    protected <R> Mono<R> retrieve(String id, Class<R> entityClass) {
        // non-ngsi-ld ids cannot exist.
        if (!IdHelper.isNgsiLdId(id)) {
            throw new CustomerManagementException("Did not receive a valid id, such inventory cannot exist.", CustomerManagementExceptionReason.NOT_FOUND);
        }
        return customerManagementRepository
                .get(URI.create(id), entityClass);
    }

    protected <T> Mono<T> patch(String id, T updatedObject, Mono<T> checkingMono, Class<T> entityClass) {
        URI idUri = URI.create(id);

        return customerManagementRepository
                .get(idUri, entityClass)
                .switchIfEmpty(Mono.error(new CustomerManagementException("No such entity exists.", CustomerManagementExceptionReason.NOT_FOUND)))
                .flatMap(entity -> checkingMono)
                .flatMap(entity -> customerManagementRepository.updateDomainEntity(id, updatedObject)
                        .then(customerManagementRepository.get(idUri, entityClass)));
    }


    protected <T, R extends EntityWithId> Mono<T> relatedEntityHandlingMono(T entity, Mono<T> entityMono, List<R> relatedList, Consumer<List<R>> entityUpdater, Class<R> relatedEntityClass) {
        List<R> relatedEntities = Optional.ofNullable(relatedList).orElseGet(List::of);
        if (!relatedEntities.isEmpty()) {
            Mono<List<R>> relatedEntitiesMono = Mono.zip(
                    relatedEntities
                            .stream()
                            .map(re ->
                                    customerManagementRepository
                                            .updateDomainEntity(re.getId().toString(), re)
                                            .onErrorResume(t -> customerManagementRepository.createDomainEntity(re))
                                            .then(Mono.just(re))
                            )
                            .toList(),
                    t -> Arrays.stream(t).map(relatedEntityClass::cast).toList());

            Mono<T> updatingMono = relatedEntitiesMono
                    .map(updatedRelatedEntity -> {
                        entityUpdater.accept(updatedRelatedEntity);
                        return entity;
                    });
            entityMono = Mono.zip(entityMono, updatingMono, (e1, e2) -> e1);
        }
        return entityMono;
    }


}
