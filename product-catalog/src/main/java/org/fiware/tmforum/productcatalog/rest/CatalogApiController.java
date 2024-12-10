package org.fiware.tmforum.productcatalog.rest;

import io.micronaut.http.HttpResponse;
import io.micronaut.http.annotation.Controller;
import lombok.extern.slf4j.Slf4j;
import org.fiware.productcatalog.api.CatalogApi;
import org.fiware.productcatalog.model.CatalogCreateVO;
import org.fiware.productcatalog.model.CatalogUpdateVO;
import org.fiware.productcatalog.model.CatalogVO;
import org.fiware.tmforum.common.exception.TmForumException;
import org.fiware.tmforum.common.exception.TmForumExceptionReason;
import org.fiware.tmforum.common.mapping.IdHelper;
import org.fiware.tmforum.common.notification.TMForumEventHandler;
import org.fiware.tmforum.common.querying.QueryParser;
import org.fiware.tmforum.common.repository.TmForumRepository;
import org.fiware.tmforum.common.rest.AbstractApiController;
import org.fiware.tmforum.common.validation.ReferenceValidationService;
import org.fiware.tmforum.common.validation.ReferencedEntity;
import org.fiware.tmforum.productcatalog.TMForumMapper;
import org.fiware.tmforum.productcatalog.domain.Catalog;
import reactor.core.publisher.Mono;

import javax.annotation.Nullable;
import java.util.*;

@Slf4j
@Controller("${general.basepath:/}")
public class CatalogApiController extends AbstractApiController<Catalog> implements CatalogApi {

    private final TMForumMapper tmForumMapper;

    public CatalogApiController(QueryParser queryParser, ReferenceValidationService validationService,
                                TmForumRepository productCatalogRepository, TMForumMapper tmForumMapper, TMForumEventHandler eventHandler) {
        super(queryParser, validationService, productCatalogRepository, eventHandler);
        this.tmForumMapper = tmForumMapper;
    }

    @Override
    public Mono<HttpResponse<CatalogVO>> createCatalog(CatalogCreateVO catalogVo) {
        Catalog catalog = tmForumMapper.map(
                tmForumMapper.map(catalogVo, IdHelper.toNgsiLd(UUID.randomUUID().toString(), Catalog.TYPE_CATALOG)));

        return create(getCheckingMono(catalog), Catalog.class)
                .map(tmForumMapper::map)
                .map(HttpResponse::created);
    }

    private Mono<Catalog> getCheckingMono(Catalog catalog) {
        List<List<? extends ReferencedEntity>> references = new ArrayList<>();
        references.add(catalog.getCategory());
        references.add(catalog.getRelatedParty());
        return getCheckingMono(catalog, references)
                .onErrorMap(throwable -> new TmForumException(
                        String.format("Was not able to create catalog %s", catalog.getId()), throwable,
                        TmForumExceptionReason.INVALID_RELATIONSHIP));
    }

    @Override
    public Mono<HttpResponse<Object>> deleteCatalog(String id) {
        return delete(id);
    }

    @Override
    public Mono<HttpResponse<List<CatalogVO>>> listCatalog(@Nullable String fields, @Nullable Integer offset,
                                                           @Nullable Integer limit) {
        return list(offset, limit, Catalog.TYPE_CATALOG, Catalog.class)
                .map(categoryStream -> categoryStream.map(tmForumMapper::map).toList())
                .switchIfEmpty(Mono.just(List.of()))
                .map(HttpResponse::ok);
    }

    @Override
    public Mono<HttpResponse<CatalogVO>> patchCatalog(String id, CatalogUpdateVO catalogUpdateVO) {
        // non-ngsi-ld ids cannot exist.
        if (!IdHelper.isNgsiLdId(id)) {
            throw new TmForumException("Did not receive a valid id, such catalog cannot exist.",
                    TmForumExceptionReason.NOT_FOUND);
        }
        Catalog updatedCatalog = tmForumMapper.map(tmForumMapper.map(catalogUpdateVO, id));

        return patch(id, updatedCatalog, getCheckingMono(updatedCatalog), Catalog.class)
                .map(tmForumMapper::map)
                .map(HttpResponse::ok);
    }

    @Override
    public Mono<HttpResponse<CatalogVO>> retrieveCatalog(String id, @Nullable String fields) {
        return retrieve(id, Catalog.class)
                .switchIfEmpty(Mono.error(new TmForumException("No such catalog exists.",
                        TmForumExceptionReason.NOT_FOUND)))
                .map(tmForumMapper::map)
                .map(HttpResponse::ok);
    }
}

