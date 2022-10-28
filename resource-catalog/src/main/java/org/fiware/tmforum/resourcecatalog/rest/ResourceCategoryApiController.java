package org.fiware.tmforum.resourcecatalog.rest;

import io.micronaut.core.annotation.NonNull;
import io.micronaut.core.annotation.Nullable;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.annotation.Controller;
import lombok.extern.slf4j.Slf4j;
import org.fiware.resourcecatalog.api.ResourceCategoryApi;
import org.fiware.resourcecatalog.model.ResourceCategoryCreateVO;
import org.fiware.resourcecatalog.model.ResourceCategoryUpdateVO;
import org.fiware.resourcecatalog.model.ResourceCategoryVO;
import org.fiware.tmforum.common.mapping.IdHelper;
import org.fiware.tmforum.common.validation.ReferenceValidationService;
import org.fiware.tmforum.common.validation.ReferencedEntity;
import org.fiware.tmforum.resourcecatalog.TMForumMapper;
import org.fiware.tmforum.resource.ResourceCategory;
import org.fiware.tmforum.resourcecatalog.exception.ResourceCatalogException;
import org.fiware.tmforum.resourcecatalog.exception.ResourceCatalogExceptionReason;
import org.fiware.tmforum.resourcecatalog.repository.ResourceCatalogRepository;
import reactor.core.publisher.Mono;

import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Controller("${general.basepath:/}")
public class ResourceCategoryApiController extends AbstractApiController implements ResourceCategoryApi {

    private final Clock clock;

    public ResourceCategoryApiController(TMForumMapper tmForumMapper, ReferenceValidationService validationService, ResourceCatalogRepository resourceCatalogRepository, Clock clock) {
        super(tmForumMapper, validationService, resourceCatalogRepository);
        this.clock = clock;
    }


    @Override
    public Mono<HttpResponse<ResourceCategoryVO>> createResourceCategory(@NonNull ResourceCategoryCreateVO resourceCategoryCreateVO) {
        ResourceCategory resourceCategory = tmForumMapper.map(
                tmForumMapper.map(resourceCategoryCreateVO, IdHelper.toNgsiLd(UUID.randomUUID().toString(), ResourceCategory.TYPE_RESOURCE_CATEGORY)));
        resourceCategory.setLastUpdate(clock.instant());

        return create(getCheckingMono(resourceCategory), ResourceCategory.class)
                .map(tmForumMapper::map)
                .map(HttpResponse::created);
    }

    private Mono<ResourceCategory> getCheckingMono(ResourceCategory resourceCategory) {
        List<List<? extends ReferencedEntity>> references = new ArrayList<>();
        references.add(resourceCategory.getCategory());
        references.add(resourceCategory.getRelatedParty());
        references.add(resourceCategory.getResourceCandidate());

        Optional.ofNullable(resourceCategory.getParentId()).map(List::of).ifPresent(references::add);

        return getCheckingMono(resourceCategory, references)
                .onErrorMap(throwable ->
                        new ResourceCatalogException(
                                String.format("Was not able to create resource category %s", resourceCategory.getId()),
                                throwable,
                                ResourceCatalogExceptionReason.INVALID_RELATIONSHIP));
    }

    @Override
    public Mono<HttpResponse<Object>> deleteResourceCategory(@NonNull String id) {
        return delete(id);
    }

    @Override
    public Mono<HttpResponse<List<ResourceCategoryVO>>> listResourceCategory(@Nullable String fields, @Nullable Integer offset, @Nullable Integer limit) {
        return list(offset, limit, ResourceCategory.TYPE_RESOURCE_CATEGORY, ResourceCategory.class)
                .map(resourceCategoryStream -> resourceCategoryStream.map(tmForumMapper::map).toList())
                .switchIfEmpty(Mono.just(List.of()))
                .map(HttpResponse::ok);
    }

    @Override
    public Mono<HttpResponse<ResourceCategoryVO>> patchResourceCategory(@NonNull String id, @NonNull ResourceCategoryUpdateVO resourceCategoryUpdateVO) {
        // non-ngsi-ld ids cannot exist.
        if (!IdHelper.isNgsiLdId(id)) {
            throw new ResourceCatalogException("Did not receive a valid id, such resource category cannot exist.", ResourceCatalogExceptionReason.NOT_FOUND);
        }
        ResourceCategory resourceCategory = tmForumMapper.map(resourceCategoryUpdateVO, id);
        resourceCategory.setLastUpdate(clock.instant());

        return patch(id, resourceCategory, getCheckingMono(resourceCategory), ResourceCategory.class)
                .map(tmForumMapper::map)
                .map(HttpResponse::ok);
    }

    @Override
    public Mono<HttpResponse<ResourceCategoryVO>> retrieveResourceCategory(@NonNull String id, @Nullable String fields) {
        return retrieve(id, ResourceCategory.class)
                .switchIfEmpty(Mono.error(new ResourceCatalogException("No such resources category exists.", ResourceCatalogExceptionReason.NOT_FOUND)))
                .map(tmForumMapper::map)
                .map(HttpResponse::ok);
    }
}
