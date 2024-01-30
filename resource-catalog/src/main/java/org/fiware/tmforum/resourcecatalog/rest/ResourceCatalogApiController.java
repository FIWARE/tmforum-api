package org.fiware.tmforum.resourcecatalog.rest;

import io.micronaut.core.annotation.NonNull;
import io.micronaut.core.annotation.Nullable;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.annotation.Controller;
import lombok.extern.slf4j.Slf4j;
import org.fiware.resourcecatalog.api.ResourceCatalogApi;
import org.fiware.resourcecatalog.model.ResourceCatalogCreateVO;
import org.fiware.resourcecatalog.model.ResourceCatalogUpdateVO;
import org.fiware.resourcecatalog.model.ResourceCatalogVO;
import org.fiware.tmforum.common.exception.TmForumException;
import org.fiware.tmforum.common.exception.TmForumExceptionReason;
import org.fiware.tmforum.common.mapping.IdHelper;
import org.fiware.tmforum.common.notification.TMForumEventHandler;
import org.fiware.tmforum.common.querying.QueryParser;
import org.fiware.tmforum.common.repository.TmForumRepository;
import org.fiware.tmforum.common.rest.AbstractApiController;
import org.fiware.tmforum.common.validation.ReferenceValidationService;
import org.fiware.tmforum.common.validation.ReferencedEntity;
import org.fiware.tmforum.resourcecatalog.TMForumMapper;
import org.fiware.tmforum.resourcecatalog.domain.ResourceCatalog;
import reactor.core.publisher.Mono;

import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
@Controller("${general.basepath:/}")
public class ResourceCatalogApiController extends AbstractApiController<ResourceCatalog> implements ResourceCatalogApi {

	private final TMForumMapper tmForumMapper;
	private final Clock clock;

	public ResourceCatalogApiController(QueryParser queryParser, ReferenceValidationService validationService,
			TmForumRepository resourceCatalogRepository, TMForumMapper tmForumMapper,
			Clock clock, TMForumEventHandler eventHandler) {
		super(queryParser, validationService, resourceCatalogRepository, eventHandler);
		this.tmForumMapper = tmForumMapper;
		this.clock = clock;
	}

	@Override
	public Mono<HttpResponse<ResourceCatalogVO>> createResourceCatalog(
			@NonNull ResourceCatalogCreateVO resourceCatalogCreateVO) {
		ResourceCatalog resourceCatalog = tmForumMapper.map(
				tmForumMapper.map(resourceCatalogCreateVO,
						IdHelper.toNgsiLd(UUID.randomUUID().toString(), ResourceCatalog.TYPE_RESOURCE_CATALOG)));
		resourceCatalog.setLastUpdate(clock.instant());

		return create(getCheckingMono(resourceCatalog), ResourceCatalog.class)
				.map(tmForumMapper::map)
				.map(HttpResponse::created);
	}

	private Mono<ResourceCatalog> getCheckingMono(ResourceCatalog resourceCatalog) {
		List<List<? extends ReferencedEntity>> references = new ArrayList<>();
		references.add(resourceCatalog.getCategory());
		references.add(resourceCatalog.getRelatedParty());

		return getCheckingMono(resourceCatalog, references)
				.onErrorMap(throwable ->
						new TmForumException(
								String.format("Was not able to create resource catalog %s", resourceCatalog.getId()),
								throwable,
								TmForumExceptionReason.INVALID_RELATIONSHIP));
	}

	@Override
	public Mono<HttpResponse<Object>> deleteResourceCatalog(@NonNull String id) {
		return delete(id);
	}

	@Override
	public Mono<HttpResponse<List<ResourceCatalogVO>>> listResourceCatalog(@Nullable String fields,
			@Nullable Integer offset, @Nullable Integer limit) {
		return list(offset, limit, ResourceCatalog.TYPE_RESOURCE_CATALOG, ResourceCatalog.class)
				.map(resourceCatalogStream -> resourceCatalogStream.map(tmForumMapper::map).toList())
				.switchIfEmpty(Mono.just(List.of()))
				.map(HttpResponse::ok);
	}

	@Override
	public Mono<HttpResponse<ResourceCatalogVO>> patchResourceCatalog(@NonNull String id,
			@NonNull ResourceCatalogUpdateVO resourceCatalogUpdateVO) {
		// non-ngsi-ld ids cannot exist.
		if (!IdHelper.isNgsiLdId(id)) {
			throw new TmForumException("Did not receive a valid id, such resource catalog cannot exist.",
					TmForumExceptionReason.NOT_FOUND);
		}
		ResourceCatalog updatedResourceCatalog = tmForumMapper.map(resourceCatalogUpdateVO, id);
		updatedResourceCatalog.setLastUpdate(clock.instant());

		return patch(id, updatedResourceCatalog, getCheckingMono(updatedResourceCatalog), ResourceCatalog.class)
				.map(tmForumMapper::map)
				.map(HttpResponse::ok);
	}

	@Override
	public Mono<HttpResponse<ResourceCatalogVO>> retrieveResourceCatalog(@NonNull String id, @Nullable String fields) {
		return retrieve(id, ResourceCatalog.class)
				.switchIfEmpty(Mono.error(new TmForumException("No such resources catalog exists.",
						TmForumExceptionReason.NOT_FOUND)))
				.map(tmForumMapper::map)
				.map(HttpResponse::ok);
	}
}
