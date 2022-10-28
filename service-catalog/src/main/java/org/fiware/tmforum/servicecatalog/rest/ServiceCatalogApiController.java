package org.fiware.tmforum.servicecatalog.rest;

import io.micronaut.core.annotation.NonNull;
import io.micronaut.core.annotation.Nullable;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.annotation.Controller;
import lombok.extern.slf4j.Slf4j;
import org.fiware.servicecatalog.api.ServiceCatalogApi;
import org.fiware.servicecatalog.model.ServiceCatalogCreateVO;
import org.fiware.servicecatalog.model.ServiceCatalogUpdateVO;
import org.fiware.servicecatalog.model.ServiceCatalogVO;
import org.fiware.tmforum.common.mapping.IdHelper;
import org.fiware.tmforum.common.validation.ReferenceValidationService;
import org.fiware.tmforum.common.validation.ReferencedEntity;
import org.fiware.tmforum.servicecatalog.TMForumMapper;
import org.fiware.tmforum.servicecatalog.domain.ServiceCatalog;
import org.fiware.tmforum.servicecatalog.exception.ServiceCatalogException;
import org.fiware.tmforum.servicecatalog.exception.ServiceCatalogExceptionReason;
import org.fiware.tmforum.servicecatalog.repository.ServiceCatalogRepository;
import reactor.core.publisher.Mono;

import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
@Controller("${general.basepath:/}")
public class ServiceCatalogApiController extends AbstractApiController implements ServiceCatalogApi {

    private final Clock clock;

    public ServiceCatalogApiController(TMForumMapper tmForumMapper, ReferenceValidationService validationService, ServiceCatalogRepository serviceCatalogRepository, Clock clock) {
        super(tmForumMapper, validationService, serviceCatalogRepository);
        this.clock = clock;
    }

    @Override
    public Mono<HttpResponse<ServiceCatalogVO>> createServiceCatalog(@NonNull ServiceCatalogCreateVO serviceCatalogCreateVO) {
        ServiceCatalog serviceCatalog = tmForumMapper.map(
                tmForumMapper.map(serviceCatalogCreateVO, IdHelper.toNgsiLd(UUID.randomUUID().toString(), ServiceCatalog.TYPE_SERVICE_CATALOG)));
        serviceCatalog.setLastUpdate(clock.instant());


        return create(getCheckingMono(serviceCatalog), ServiceCatalog.class)
                .map(tmForumMapper::map)
                .map(HttpResponse::created);
    }

    private Mono<ServiceCatalog> getCheckingMono(ServiceCatalog serviceCatalog) {
        List<List<? extends ReferencedEntity>> references = new ArrayList<>();
        references.add(serviceCatalog.getCategory());
        references.add(serviceCatalog.getRelatedParty());

        return getCheckingMono(serviceCatalog, references)
                .onErrorMap(throwable ->
                        new ServiceCatalogException(
                                String.format("Was not able to create service catalog %s", serviceCatalog.getId()),
                                throwable,
                                ServiceCatalogExceptionReason.INVALID_RELATIONSHIP));
    }

    @Override
    public Mono<HttpResponse<Object>> deleteServiceCatalog(@NonNull String id) {
        return delete(id);
    }

    @Override
    public Mono<HttpResponse<List<ServiceCatalogVO>>> listServiceCatalog(@Nullable String fields, @Nullable Integer offset, @Nullable Integer limit) {
        return list(offset, limit, ServiceCatalog.TYPE_SERVICE_CATALOG, ServiceCatalog.class)
                .map(serviceCatalogStream -> serviceCatalogStream.map(tmForumMapper::map).toList())
                .switchIfEmpty(Mono.just(List.of()))
                .map(HttpResponse::ok);
    }

    @Override
    public Mono<HttpResponse<ServiceCatalogVO>> patchServiceCatalog(@NonNull String id, @NonNull ServiceCatalogUpdateVO serviceCatalogUpdateVO) {
        // non-ngsi-ld ids cannot exist.
        if (!IdHelper.isNgsiLdId(id)) {
            throw new ServiceCatalogException("Did not receive a valid id, such resource catalog cannot exist.", ServiceCatalogExceptionReason.NOT_FOUND);
        }
        ServiceCatalog updatedResourceCatalog = tmForumMapper.map(serviceCatalogUpdateVO, id);
        updatedResourceCatalog.setLastUpdate(clock.instant());

        return patch(id, updatedResourceCatalog, getCheckingMono(updatedResourceCatalog), ServiceCatalog.class)
                .map(tmForumMapper::map)
                .map(HttpResponse::ok);
    }

    @Override
    public Mono<HttpResponse<ServiceCatalogVO>> retrieveServiceCatalog(@NonNull String id, @Nullable String fields) {
        return retrieve(id, ServiceCatalog.class)
                .switchIfEmpty(Mono.error(new ServiceCatalogException("No such resources catalog exists.", ServiceCatalogExceptionReason.NOT_FOUND)))
                .map(tmForumMapper::map)
                .map(HttpResponse::ok);
    }
}
