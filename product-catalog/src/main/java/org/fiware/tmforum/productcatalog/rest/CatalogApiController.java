package org.fiware.tmforum.productcatalog.rest;

import io.micronaut.http.HttpResponse;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.client.exceptions.HttpClientResponseException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.fiware.productcatalog.api.CatalogApi;
import org.fiware.productcatalog.model.CatalogCreateVO;
import org.fiware.productcatalog.model.CatalogUpdateVO;
import org.fiware.productcatalog.model.CatalogVO;
import org.fiware.tmforum.common.mapping.IdHelper;
import org.fiware.tmforum.common.validation.ReferenceValidationService;
import org.fiware.tmforum.productcatalog.TMForumMapper;
import org.fiware.tmforum.productcatalog.domain.Catalog;
import org.fiware.tmforum.productcatalog.exception.CatalogException;
import org.fiware.tmforum.productcatalog.exception.CatalogExceptionReason;
import org.fiware.tmforum.productcatalog.repository.ProductCatalogRepository;
import reactor.core.publisher.Mono;

import javax.annotation.Nullable;
import java.net.URI;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.fiware.tmforum.common.CommonConstants.DEFAULT_LIMIT;
import static org.fiware.tmforum.common.CommonConstants.DEFAULT_OFFSET;

@Slf4j
@RequiredArgsConstructor
@Controller("${general.basepath:/}")
public class CatalogApiController implements CatalogApi {

    private final TMForumMapper tmForumMapper;
    private final ReferenceValidationService validationService;
    private final ProductCatalogRepository productCatalogRepository;

    @Override
    public Mono<HttpResponse<CatalogVO>> createCatalog(CatalogCreateVO catalogVo) {
        Catalog catalog = tmForumMapper.map(tmForumMapper.map(catalogVo, IdHelper.toNgsiLd(UUID.randomUUID().toString(), Catalog.TYPE_CATALOGUE)));
        Mono<Catalog> checkingMono = getCheckingMono(catalog);

        return checkingMono
                .flatMap(catalogToBeCreated -> productCatalogRepository.createDomainEntity(catalogToBeCreated).then(Mono.just(catalogToBeCreated)))
                .onErrorMap(t -> {
                    if (t instanceof HttpClientResponseException e) {
                        return switch (e.getStatus()) {
                            case CONFLICT -> new CatalogException(String.format("Conflict on creating the catalog: %s", e.getMessage()), CatalogExceptionReason.CONFLICT);
                            case BAD_REQUEST -> new CatalogException(String.format("Did not receive a valid catalog: %s.", e.getMessage()), CatalogExceptionReason.INVALID_DATA);
                            default -> new CatalogException(String.format("Unspecified downstream error: %s", e.getMessage()), CatalogExceptionReason.UNKNOWN);
                        };
                    } else {
                        return t;
                    }
                })
                .cast(Catalog.class)
                .map(tmForumMapper::map)
                .map(HttpResponse::created);
    }

    private Mono<Catalog> getCheckingMono(Catalog catalog) {
        Mono<Catalog> catalogMono = Mono.just(catalog);
        Mono<Catalog> checkingMono;

        if (catalog.getRelatedParty() != null && !catalog.getRelatedParty().isEmpty()) {
            checkingMono = validationService.getCheckingMono(catalog.getRelatedParty(), catalog)
                    .onErrorMap(throwable -> new CatalogException(String.format("Was not able to create individual %s", catalog.getId()), throwable, CatalogExceptionReason.INVALID_RELATIONSHIP));
            catalogMono = Mono.zip(catalogMono, checkingMono, (p1, p2) -> p1);
        }
        if (catalog.getCategory() != null && !catalog.getCategory().isEmpty()) {
            checkingMono = validationService.getCheckingMono(catalog.getCategory(), catalog)
                    .onErrorMap(throwable -> new CatalogException(String.format("Was not able to create individual %s", catalog.getId()), throwable, CatalogExceptionReason.INVALID_RELATIONSHIP));
            catalogMono = Mono.zip(catalogMono, checkingMono, (p1, p2) -> p1);
        }
        return catalogMono;
    }


    @Override
    public Mono<HttpResponse<Object>> deleteCatalog(String id) {
        // non-ngsi-ld ids cannot exist.
        if (!IdHelper.isNgsiLdId(id)) {
            throw new CatalogException("Did not receive a valid id, such catalog cannot exist.", CatalogExceptionReason.NOT_FOUND);
        }
        return productCatalogRepository.deleteDomainEntity(URI.create(id))
                .then(Mono.just(HttpResponse.noContent()));
    }

    @Override
    public Mono<HttpResponse<List<CatalogVO>>> listCatalog(@Nullable String fields, @Nullable Integer offset, @Nullable Integer limit) {
        offset = Optional.ofNullable(offset).orElse(DEFAULT_OFFSET);
        limit = Optional.ofNullable(limit).orElse(DEFAULT_LIMIT);

        if (offset < 0 || limit < 1) {
            throw new CatalogException(String.format("Invalid offset %s or limit %s.", offset, limit), CatalogExceptionReason.INVALID_DATA);
        }

        return productCatalogRepository
                .findCatalogs(offset, limit)
                .map(List::stream)
                .map(catalogStream -> catalogStream.map(tmForumMapper::map).toList())
                .map(HttpResponse::ok);
    }

    @Override
    public Mono<HttpResponse<CatalogVO>> patchCatalog(String id, CatalogUpdateVO catalogUpdateVO) {
        // non-ngsi-ld ids cannot exist.
        if (!IdHelper.isNgsiLdId(id)) {
            throw new CatalogException("Did not receive a valid id, such catalog cannot exist.", CatalogExceptionReason.NOT_FOUND);
        }
        Catalog updatedCatalog = tmForumMapper.map(tmForumMapper.map(catalogUpdateVO, id));

        URI idUri = URI.create(id);

        return productCatalogRepository
                .getCatalog(idUri)
                .switchIfEmpty(Mono.error(new CatalogException("No such catalog exists.", CatalogExceptionReason.NOT_FOUND)))
                .flatMap(catalog -> getCheckingMono(updatedCatalog))
                .flatMap(catalog -> productCatalogRepository.updateDomainEntity(id, updatedCatalog)
                        .then(productCatalogRepository.getCatalog(idUri)))
                .map(tmForumMapper::map)
                .map(HttpResponse::ok);
    }

    @Override
    public Mono<HttpResponse<CatalogVO>> retrieveCatalog(String id, @Nullable String fields) {
        // non-ngsi-ld ids cannot exist.
        if (!IdHelper.isNgsiLdId(id)) {
            throw new CatalogException("Did not receive a valid id, such catalog cannot exist.", CatalogExceptionReason.NOT_FOUND);
        }
        return productCatalogRepository
                .getCatalog(URI.create(id))
                .map(tmForumMapper::map)
                .map(HttpResponse::ok)
                .switchIfEmpty(Mono.error(new CatalogException("No such catalog exists.", CatalogExceptionReason.NOT_FOUND)))
                .map(HttpResponse.class::cast);
    }
}

