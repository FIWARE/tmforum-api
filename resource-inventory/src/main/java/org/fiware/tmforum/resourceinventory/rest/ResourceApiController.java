package org.fiware.tmforum.resourceinventory.rest;

import io.micronaut.http.HttpResponse;
import io.micronaut.http.annotation.Controller;
import lombok.extern.slf4j.Slf4j;
import org.fiware.resourceinventory.api.ResourceApi;
import org.fiware.resourceinventory.model.ResourceCreateVO;
import org.fiware.resourceinventory.model.ResourceUpdateVO;
import org.fiware.resourceinventory.model.ResourceVO;
import org.fiware.tmforum.common.validation.ReferenceValidationService;
import org.fiware.tmforum.resourceinventory.TMForumMapper;
import org.fiware.tmforum.resourceinventory.repository.ResourceInventoryRepository;
import reactor.core.publisher.Mono;

import java.util.List;

@Slf4j
@Controller("${general.basepath:/}")
public class ResourceApiController extends AbstractApiController implements ResourceApi {

    public ResourceApiController(TMForumMapper tmForumMapper, ReferenceValidationService validationService, ResourceInventoryRepository resourceInventoryRepository) {
        super(tmForumMapper, validationService, resourceInventoryRepository);
    }

    @Override
    public Mono<HttpResponse<ResourceVO>> createResource(ResourceCreateVO resource) {
        return null;
    }

    @Override
    public Mono<HttpResponse<Object>> deleteResource(String id) {
        return null;
    }

    @Override
    public Mono<HttpResponse<List<ResourceVO>>> listResource(String fields, Integer offset, Integer limit) {
        return null;
    }

    @Override
    public Mono<HttpResponse<ResourceVO>> patchResource(String id, ResourceUpdateVO resource) {
        return null;
    }

    @Override
    public Mono<HttpResponse<ResourceVO>> retrieveResource(String id, String fields) {
        return null;
    }
}
