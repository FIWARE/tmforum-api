package org.fiware.tmforum.servicecatalog.rest;

import io.micronaut.core.annotation.NonNull;
import io.micronaut.core.annotation.Nullable;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.annotation.Controller;
import lombok.extern.slf4j.Slf4j;
import org.fiware.servicecatalog.api.ServiceCategoryApi;
import org.fiware.servicecatalog.model.ServiceCategoryCreateVO;
import org.fiware.servicecatalog.model.ServiceCategoryUpdateVO;
import org.fiware.servicecatalog.model.ServiceCategoryVO;
import org.fiware.servicecatalog.api.ServiceCategoryApi;
import org.fiware.tmforum.common.mapping.IdHelper;
import org.fiware.tmforum.common.validation.ReferenceValidationService;
import org.fiware.tmforum.common.validation.ReferencedEntity;
import org.fiware.tmforum.servicecatalog.TMForumMapper;
import org.fiware.tmforum.servicecatalog.domain.ServiceCategory;
import org.fiware.tmforum.servicecatalog.exception.ServiceCatalogException;
import org.fiware.tmforum.servicecatalog.exception.ServiceCatalogExceptionReason;
import org.fiware.tmforum.servicecatalog.repository.ServiceCatalogRepository;
import org.fiware.tmforum.servicecatalog.repository.ServiceCatalogRepository;
import reactor.core.publisher.Mono;

import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Controller("${general.basepath:/}")
public class ServiceCategoryApiController extends AbstractApiController implements ServiceCategoryApi {

    private final Clock clock;

    public ServiceCategoryApiController(TMForumMapper tmForumMapper, ReferenceValidationService validationService, ServiceCatalogRepository serviceCatalogRepository, Clock clock) {
        super(tmForumMapper, validationService, serviceCatalogRepository);
        this.clock = clock;
    }


    @Override
    public Mono<HttpResponse<ServiceCategoryVO>> createServiceCategory(@NonNull ServiceCategoryCreateVO serviceCategoryCreateVO) {
        ServiceCategory serviceCategory = tmForumMapper.map(
                tmForumMapper.map(serviceCategoryCreateVO, IdHelper.toNgsiLd(UUID.randomUUID().toString(), ServiceCategory.TYPE_SERVICE_CATEGORY)));
        serviceCategory.setLastUpdate(clock.instant());

        return create(getCheckingMono(serviceCategory), ServiceCategory.class)
                .map(tmForumMapper::map)
                .map(HttpResponse::created);
    }

    private Mono<ServiceCategory> getCheckingMono(ServiceCategory serviceCategory) {
        List<List<? extends ReferencedEntity>> references = new ArrayList<>();
        references.add(serviceCategory.getCategory());
        references.add(serviceCategory.getServiceCandidate());

        Optional.ofNullable(serviceCategory.getParentId()).map(List::of).ifPresent(references::add);

        return getCheckingMono(serviceCategory, references)
                .onErrorMap(throwable ->
                        new ServiceCatalogException(
                                String.format("Was not able to create service category %s", serviceCategory.getId()),
                                throwable,
                                ServiceCatalogExceptionReason.INVALID_RELATIONSHIP));
    }

    @Override
    public Mono<HttpResponse<Object>> deleteServiceCategory(@NonNull String id) {
        return delete(id);
    }

    @Override
    public Mono<HttpResponse<List<ServiceCategoryVO>>> listServiceCategory(@Nullable String fields, @Nullable Integer offset, @Nullable Integer limit) {
        return list(offset, limit, ServiceCategory.TYPE_SERVICE_CATEGORY, ServiceCategory.class)
                .map(serviceCategoryStream -> serviceCategoryStream.map(tmForumMapper::map).toList())
                .switchIfEmpty(Mono.just(List.of()))
                .map(HttpResponse::ok);
    }

    @Override
    public Mono<HttpResponse<ServiceCategoryVO>> patchServiceCategory(@NonNull String id, @NonNull ServiceCategoryUpdateVO serviceCategoryUpdateVO) {
        // non-ngsi-ld ids cannot exist.
        if (!IdHelper.isNgsiLdId(id)) {
            throw new ServiceCatalogException("Did not receive a valid id, such service category cannot exist.", ServiceCatalogExceptionReason.NOT_FOUND);
        }
        ServiceCategory serviceCategory = tmForumMapper.map(serviceCategoryUpdateVO, id);
        serviceCategory.setLastUpdate(clock.instant());

        return patch(id, serviceCategory, getCheckingMono(serviceCategory), ServiceCategory.class)
                .map(tmForumMapper::map)
                .map(HttpResponse::ok);
    }

    @Override
    public Mono<HttpResponse<ServiceCategoryVO>> retrieveServiceCategory(@NonNull String id, @Nullable String fields) {
        return retrieve(id, ServiceCategory.class)
                .switchIfEmpty(Mono.error(new ServiceCatalogException("No such services category exists.", ServiceCatalogExceptionReason.NOT_FOUND)))
                .map(tmForumMapper::map)
                .map(HttpResponse::ok);
    }
}
