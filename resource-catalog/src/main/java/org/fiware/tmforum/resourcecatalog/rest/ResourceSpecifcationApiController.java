package org.fiware.tmforum.resourcecatalog.rest;

import io.micronaut.http.HttpResponse;
import org.fiware.resourcecatalog.api.ResourceSpecificationApi;
import org.fiware.resourcecatalog.model.ResourceSpecificationCreateVO;
import org.fiware.resourcecatalog.model.ResourceSpecificationUpdateVO;
import org.fiware.resourcecatalog.model.ResourceSpecificationVO;
import org.fiware.tmforum.common.validation.ReferenceValidationService;
import org.fiware.tmforum.resourcecatalog.TMForumMapper;
import org.fiware.tmforum.resourcecatalog.repository.ResourceCatalogRepository;
import reactor.core.publisher.Mono;

import java.util.List;

public class ResourceSpecifcationApiController extends AbstractApiController implements ResourceSpecificationApi {
    public ResourceSpecifcationApiController(TMForumMapper tmForumMapper, ReferenceValidationService validationService, ResourceCatalogRepository resourceCatalogRepository) {
        super(tmForumMapper, validationService, resourceCatalogRepository);
    }

    @Override
    public Mono<HttpResponse<ResourceSpecificationVO>> createResourceSpecification(ResourceSpecificationCreateVO resourceSpecification) {

        // TODO validate the object
        return null;
    }

    @Override
    public Mono<HttpResponse<Object>> deleteResourceSpecification(String id) {
        return null;
    }

    @Override
    public Mono<HttpResponse<List<ResourceSpecificationVO>>> listResourceSpecification(String fields, Integer offset, Integer limit) {
        return null;
    }

    @Override
    public Mono<HttpResponse<ResourceSpecificationVO>> patchResourceSpecification(String id, ResourceSpecificationUpdateVO resourceSpecification) {
        return null;
    }

    @Override
    public Mono<HttpResponse<ResourceSpecificationVO>> retrieveResourceSpecification(String id, String fields) {
        return null;
    }
}
