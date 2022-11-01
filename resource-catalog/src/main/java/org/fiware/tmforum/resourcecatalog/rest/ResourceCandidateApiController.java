package org.fiware.tmforum.resourcecatalog.rest;

import io.micronaut.core.annotation.NonNull;
import io.micronaut.core.annotation.Nullable;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.annotation.Controller;
import lombok.extern.slf4j.Slf4j;
import org.fiware.resourcecatalog.api.ResourceCandidateApi;
import org.fiware.resourcecatalog.model.ResourceCandidateCreateVO;
import org.fiware.resourcecatalog.model.ResourceCandidateUpdateVO;
import org.fiware.resourcecatalog.model.ResourceCandidateVO;
import org.fiware.tmforum.common.exception.TmForumException;
import org.fiware.tmforum.common.exception.TmForumExceptionReason;
import org.fiware.tmforum.common.mapping.IdHelper;
import org.fiware.tmforum.common.repository.TmForumRepository;
import org.fiware.tmforum.common.rest.AbstractApiController;
import org.fiware.tmforum.common.validation.ReferenceValidationService;
import org.fiware.tmforum.common.validation.ReferencedEntity;
import org.fiware.tmforum.resource.ResourceCandidate;
import org.fiware.tmforum.resourcecatalog.TMForumMapper;
import reactor.core.publisher.Mono;

import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Controller("${general.basepath:/}")
public class ResourceCandidateApiController extends AbstractApiController<ResourceCandidate>
		implements ResourceCandidateApi {

	private final TMForumMapper tmForumMapper;
	private final Clock clock;

	public ResourceCandidateApiController(ReferenceValidationService validationService,
			TmForumRepository resourceCatalogRepository, TMForumMapper tmForumMapper,
			Clock clock) {
		super(validationService, resourceCatalogRepository);
		this.tmForumMapper = tmForumMapper;
		this.clock = clock;
	}

	@Override
	public Mono<HttpResponse<ResourceCandidateVO>> createResourceCandidate(
			@NonNull ResourceCandidateCreateVO resourceCandidateCreateVO) {
		ResourceCandidate resourceCandidate = tmForumMapper.map(
				tmForumMapper.map(resourceCandidateCreateVO,
						IdHelper.toNgsiLd(UUID.randomUUID().toString(), ResourceCandidate.TYPE_RESOURCE_CANDIDATE)));
		resourceCandidate.setLastUpdate(clock.instant());

		return create(getCheckingMono(resourceCandidate), ResourceCandidate.class)
				.map(tmForumMapper::map)
				.map(HttpResponse::created);
	}

	private Mono<ResourceCandidate> getCheckingMono(ResourceCandidate resourceCandidate) {
		List<List<? extends ReferencedEntity>> references = new ArrayList<>();
		references.add(resourceCandidate.getCategory());

		Optional.ofNullable(resourceCandidate.getResourceSpecification())
				.map(List::of)
				.ifPresent(references::add);

		return getCheckingMono(resourceCandidate, references)
				.onErrorMap(throwable ->
						new TmForumException(
								String.format("Was not able to create resource candidate %s",
										resourceCandidate.getId()),
								throwable,
								TmForumExceptionReason.INVALID_RELATIONSHIP));
	}

	@Override
	public Mono<HttpResponse<Object>> deleteResourceCandidate(@NonNull String id) {
		return delete(id);
	}

	@Override
	public Mono<HttpResponse<List<ResourceCandidateVO>>> listResourceCandidate(@Nullable String fields,
			@Nullable Integer offset, @Nullable Integer limit) {
		return list(offset, limit, ResourceCandidate.TYPE_RESOURCE_CANDIDATE, ResourceCandidate.class)
				.map(resourceFunctionStream -> resourceFunctionStream.map(tmForumMapper::map).toList())
				.switchIfEmpty(Mono.just(List.of()))
				.map(HttpResponse::ok);
	}

	@Override
	public Mono<HttpResponse<ResourceCandidateVO>> patchResourceCandidate(@NonNull String id,
			@NonNull ResourceCandidateUpdateVO resourceCandidateUpdateVO) {
		// non-ngsi-ld ids cannot exist.
		if (!IdHelper.isNgsiLdId(id)) {
			throw new TmForumException("Did not receive a valid id, such resource candidate cannot exist.",
					TmForumExceptionReason.NOT_FOUND);
		}
		ResourceCandidate resourceCandidate = tmForumMapper.map(resourceCandidateUpdateVO, id);
		resourceCandidate.setLastUpdate(clock.instant());

		return patch(id, resourceCandidate, getCheckingMono(resourceCandidate), ResourceCandidate.class)
				.map(tmForumMapper::map)
				.map(HttpResponse::ok);
	}

	@Override
	public Mono<HttpResponse<ResourceCandidateVO>> retrieveResourceCandidate(@NonNull String id,
			@Nullable String fields) {
		return retrieve(id, ResourceCandidate.class)
				.switchIfEmpty(Mono.error(new TmForumException("No such resources candidate exists.",
						TmForumExceptionReason.NOT_FOUND)))
				.map(tmForumMapper::map)
				.map(HttpResponse::ok);
	}
}
