package org.fiware.tmforum.productcatalog.rest;

import io.micronaut.http.HttpResponse;
import io.micronaut.http.annotation.Controller;
import lombok.extern.slf4j.Slf4j;
import org.fiware.productcatalog.api.CatalogApi;
import org.fiware.productcatalog.model.CatalogCreateVO;
import org.fiware.productcatalog.model.CatalogUpdateVO;
import org.fiware.productcatalog.model.CatalogVO;
import org.fiware.tmforum.common.mapping.IdHelper;
import org.fiware.tmforum.common.validation.ReferenceValidationService;
import org.fiware.tmforum.common.validation.ReferencedEntity;
import org.fiware.tmforum.productcatalog.TMForumMapper;
import org.fiware.tmforum.productcatalog.domain.Catalog;
import org.fiware.tmforum.productcatalog.exception.ProductCatalogException;
import org.fiware.tmforum.productcatalog.exception.ProductCatalogExceptionReason;
import org.fiware.tmforum.productcatalog.repository.ProductCatalogRepository;
import reactor.core.publisher.Mono;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
@Controller("${general.basepath:/}")
public class CatalogApiController extends AbstractApiController implements CatalogApi {

    public CatalogApiController(TMForumMapper tmForumMapper, ReferenceValidationService validationService, ProductCatalogRepository productCatalogRepository) {
        super(tmForumMapper, validationService, productCatalogRepository);
    }

    @Override
    public Mono<HttpResponse<CatalogVO>> createCatalog(CatalogCreateVO catalogVo) {
        Catalog catalog = tmForumMapper.map(tmForumMapper.map(catalogVo, IdHelper.toNgsiLd(UUID.randomUUID().toString(), Catalog.TYPE_CATALOGUE)));
        Mono<Catalog> checkingMono = getCheckingMono(catalog);

        return create(checkingMono, Catalog.class)
                .map(tmForumMapper::map)
                .map(HttpResponse::created);
    }

    private Mono<Catalog> getCheckingMono(Catalog catalog) {
        List<List<? extends ReferencedEntity>> references = new ArrayList<>();
        references.add(catalog.getCategory());
        references.add(catalog.getRelatedParty());
        return getCheckingMono(catalog, references)
                .onErrorMap(throwable -> new ProductCatalogException(String.format("Was not able to create category %s", catalog.getId()), throwable, ProductCatalogExceptionReason.INVALID_RELATIONSHIP));
    }


    @Override
    public Mono<HttpResponse<Object>> deleteCatalog(String id) {
        return delete(id);
    }

    @Override
    public Mono<HttpResponse<List<CatalogVO>>> listCatalog(@Nullable String fields, @Nullable Integer offset, @Nullable Integer limit) {
        return list(offset, limit, Catalog.TYPE_CATALOGUE, Catalog.class)
                .map(categoryStream -> categoryStream.map(tmForumMapper::map).toList())
                .switchIfEmpty(Mono.just(List.of()))
                .map(HttpResponse::ok);
    }

    @Override
    public Mono<HttpResponse<CatalogVO>> patchCatalog(String id, CatalogUpdateVO catalogUpdateVO) {
        // non-ngsi-ld ids cannot exist.
        if (!IdHelper.isNgsiLdId(id)) {
            throw new ProductCatalogException("Did not receive a valid id, such catalog cannot exist.", ProductCatalogExceptionReason.NOT_FOUND);
        }
        Catalog updatedCatalog = tmForumMapper.map(tmForumMapper.map(catalogUpdateVO, id));

        Mono<Catalog> checkingMono = getCheckingMono(updatedCatalog);
        return patch(id, updatedCatalog, checkingMono, Catalog.class)
                .map(tmForumMapper::map)
                .map(HttpResponse::ok);
    }

    @Override
    public Mono<HttpResponse<CatalogVO>> retrieveCatalog(String id, @Nullable String fields) {
        return retrieve(id, Catalog.class)
                .switchIfEmpty(Mono.error(new ProductCatalogException("No such catalog exists.", ProductCatalogExceptionReason.NOT_FOUND)))
                .map(tmForumMapper::map)
                .map(HttpResponse::ok);
    }
}

