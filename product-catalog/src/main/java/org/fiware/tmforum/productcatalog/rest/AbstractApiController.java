package org.fiware.tmforum.productcatalog.rest;

import io.micronaut.http.HttpResponse;
import io.micronaut.http.client.exceptions.HttpClientResponseException;
import lombok.RequiredArgsConstructor;
import org.fiware.productcatalog.model.BundledProductOfferingVO;
import org.fiware.productcatalog.model.CategoryVO;
import org.fiware.tmforum.common.domain.EntityWithId;
import org.fiware.tmforum.common.mapping.IdHelper;
import org.fiware.tmforum.common.validation.ReferenceValidationService;
import org.fiware.tmforum.common.validation.ReferencedEntity;
import org.fiware.tmforum.party.domain.TaxExemptionCertificate;
import org.fiware.tmforum.productcatalog.TMForumMapper;
import org.fiware.tmforum.productcatalog.domain.BundleProductOffering;
import org.fiware.tmforum.productcatalog.domain.Catalog;
import org.fiware.tmforum.productcatalog.domain.Category;
import org.fiware.tmforum.productcatalog.exception.ProductCatalogException;
import org.fiware.tmforum.productcatalog.exception.ProductCatalogExceptionReason;
import org.fiware.tmforum.productcatalog.repository.ProductCatalogRepository;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Stream;

import static org.fiware.tmforum.common.CommonConstants.DEFAULT_LIMIT;
import static org.fiware.tmforum.common.CommonConstants.DEFAULT_OFFSET;

@RequiredArgsConstructor
public abstract class AbstractApiController {


    protected final TMForumMapper tmForumMapper;
    protected final ReferenceValidationService validationService;
    protected final ProductCatalogRepository productCatalogRepository;

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
                .flatMap(checkedResult -> productCatalogRepository.createDomainEntity(checkedResult).then(Mono.just(checkedResult)))
                .onErrorMap(t -> {
                    if (t instanceof HttpClientResponseException e) {
                        return switch (e.getStatus()) {
                            case CONFLICT -> new ProductCatalogException(String.format("Conflict on creating the entity: %s", e.getMessage()), ProductCatalogExceptionReason.CONFLICT);
                            case BAD_REQUEST -> new ProductCatalogException(String.format("Did not receive a valid entity: %s.", e.getMessage()), ProductCatalogExceptionReason.INVALID_DATA);
                            default -> new ProductCatalogException(String.format("Unspecified downstream error: %s", e.getMessage()), ProductCatalogExceptionReason.UNKNOWN);
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
            throw new ProductCatalogException("Did not receive a valid id, such entity cannot exist.", ProductCatalogExceptionReason.NOT_FOUND);
        }
        return productCatalogRepository.deleteDomainEntity(URI.create(id))
                .then(Mono.just(HttpResponse.noContent()));
    }

    protected <R> Mono<Stream<R>> list(Integer offset, Integer limit, String type, Class<R> entityClass) {
        offset = Optional.ofNullable(offset).orElse(DEFAULT_OFFSET);
        limit = Optional.ofNullable(limit).orElse(DEFAULT_LIMIT);

        if (offset < 0 || limit < 1) {
            throw new ProductCatalogException(String.format("Invalid offset %s or limit %s.", offset, limit), ProductCatalogExceptionReason.INVALID_DATA);
        }

        return productCatalogRepository
                .findEntities(offset, limit, type, entityClass)
                .map(List::stream);
    }

    protected <R> Mono<R> retrieve(String id, Class<R> entityClass) {
        // non-ngsi-ld ids cannot exist.
        if (!IdHelper.isNgsiLdId(id)) {
            throw new ProductCatalogException("Did not receive a valid id, such catalog cannot exist.", ProductCatalogExceptionReason.NOT_FOUND);
        }
        return productCatalogRepository
                .get(URI.create(id), entityClass);
    }

    protected <T> Mono<T> patch(String id, T updatedObject, Mono<T> checkingMono, Class<T> entityClass) {
        URI idUri = URI.create(id);

        return productCatalogRepository
                .get(idUri, entityClass)
                .switchIfEmpty(Mono.error(new ProductCatalogException("No such entity exists.", ProductCatalogExceptionReason.NOT_FOUND)))
                .flatMap(entity -> checkingMono)
                .flatMap(entity -> productCatalogRepository.updateDomainEntity(id, updatedObject)
                        .then(productCatalogRepository.get(idUri, entityClass)));
    }


    protected <T, R extends EntityWithId> Mono<T> relatedEntityHandlingMono(T offering, Mono<T> offeringMono, List<R> bundleProductOfferingsList, Consumer<List<R>> offeringUpdater, Class<R> relatedEntityClass) {
        List<R> bundleProductOfferings = Optional.ofNullable(bundleProductOfferingsList).orElseGet(List::of);
        if (!bundleProductOfferings.isEmpty()) {
            Mono<List<R>> bundleProductOfferingsMono = Mono.zip(
                    bundleProductOfferings
                            .stream()
                            .map(bpo ->
                                    productCatalogRepository
                                            .updateDomainEntity(bpo.getId().toString(), bpo)
                                            .onErrorResume(t -> productCatalogRepository.createDomainEntity(bpo))
                                            .then(Mono.just(bpo))
                            )
                            .toList(),
                    t -> Arrays.stream(t).map(relatedEntityClass::cast).toList());

            Mono<T> updatingMono = bundleProductOfferingsMono
                    .map(updatedTaxExemptions -> {
                        offeringUpdater.accept(updatedTaxExemptions);
                        return offering;
                    });
            offeringMono = Mono.zip(offeringMono, updatingMono, (offering1, offering2) -> offering1);
        }
        return offeringMono;
    }


}
