package org.fiware.tmforum.servicecatalog.rest;

import io.micronaut.core.annotation.NonNull;
import io.micronaut.core.annotation.Nullable;
import io.micronaut.http.HttpResponse;
import org.fiware.servicecatalog.api.ServiceSpecificationApi;
import org.fiware.servicecatalog.model.ServiceSpecificationCreateVO;
import org.fiware.servicecatalog.model.ServiceSpecificationUpdateVO;
import org.fiware.servicecatalog.model.ServiceSpecificationVO;
import org.fiware.tmforum.common.mapping.IdHelper;
import org.fiware.tmforum.common.validation.ReferenceValidationService;
import org.fiware.tmforum.servicecatalog.TMForumMapper;
import org.fiware.tmforum.servicecatalog.domain.ServiceSpecification;
import org.fiware.tmforum.servicecatalog.exception.ServiceCatalogException;
import org.fiware.tmforum.servicecatalog.exception.ServiceCatalogExceptionReason;
import org.fiware.tmforum.servicecatalog.repository.ServiceCatalogRepository;
import reactor.core.publisher.Mono;

import java.time.Clock;
import java.util.List;
import java.util.UUID;

public class ServiceSpecificationApiController extends AbstractApiController implements ServiceSpecificationApi {

    private final Clock clock;

    public ServiceSpecificationApiController(TMForumMapper tmForumMapper, ReferenceValidationService validationService, ServiceCatalogRepository serviceCatalogRepository, Clock clock) {
        super(tmForumMapper, validationService, serviceCatalogRepository);
        this.clock = clock;
    }

    @Override
    public Mono<HttpResponse<ServiceSpecificationVO>> createServiceSpecification(@NonNull ServiceSpecificationCreateVO serviceSpecificationCreateVO) {
        if (serviceSpecificationCreateVO.getName() == null) {
            throw new ServiceCatalogException(String.format("The specification create does not contain all mandatory values: %s.", serviceSpecificationCreateVO), ServiceCatalogExceptionReason.INVALID_DATA);
        }
        if (serviceSpecificationCreateVO.getIsBundle() == null) {
            // set default required by the conformance
            serviceSpecificationCreateVO.isBundle(false);
        }
        if (serviceSpecificationCreateVO.getLifecycleStatus() == null) {
            // set default required by the conformance
            serviceSpecificationCreateVO.lifecycleStatus("created");
        }

        ServiceSpecification serviceSpecification = tmForumMapper.map(
                tmForumMapper.map(serviceSpecificationCreateVO, IdHelper.toNgsiLd(UUID.randomUUID().toString(), ServiceSpecification.TYPE_SERVICE_SPECIFICATION)));
        serviceSpecification.setLastUpdate(clock.instant());

        return null;
    }


    private Mono<ServiceSpecification> validateSpec(ServiceSpecification serviceSpecification) {
        return null;
    }

    @Override
    public Mono<HttpResponse<Object>> deleteServiceSpecification(@NonNull String id) {
        return delete(id);
    }

    @Override
    public Mono<HttpResponse<List<ServiceSpecificationVO>>> listServiceSpecification(@Nullable String fields, @Nullable Integer offset, @Nullable Integer limit) {
        return null;
    }

    @Override
    public Mono<HttpResponse<ServiceSpecificationVO>> patchServiceSpecification(@NonNull String id, @NonNull ServiceSpecificationUpdateVO serviceSpecification) {
        return null;
    }

    @Override
    public Mono<HttpResponse<ServiceSpecificationVO>> retrieveServiceSpecification(@NonNull String id, @Nullable String fields) {
        return null;
    }
}
