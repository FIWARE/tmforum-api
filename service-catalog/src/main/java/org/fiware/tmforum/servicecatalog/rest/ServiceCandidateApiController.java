package org.fiware.tmforum.servicecatalog.rest;

import io.micronaut.core.annotation.NonNull;
import io.micronaut.core.annotation.Nullable;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.annotation.Controller;
import lombok.extern.slf4j.Slf4j;
import org.fiware.servicecatalog.api.ServiceCandidateApi;
import org.fiware.servicecatalog.model.ServiceCandidateCreateVO;
import org.fiware.servicecatalog.model.ServiceCandidateUpdateVO;
import org.fiware.servicecatalog.model.ServiceCandidateVO;
import org.fiware.tmforum.common.notification.EventHandler;
import org.fiware.tmforum.common.exception.TmForumException;
import org.fiware.tmforum.common.exception.TmForumExceptionReason;
import org.fiware.tmforum.common.mapping.IdHelper;
import org.fiware.tmforum.common.repository.TmForumRepository;
import org.fiware.tmforum.common.rest.AbstractApiController;
import org.fiware.tmforum.common.validation.ReferenceValidationService;
import org.fiware.tmforum.common.validation.ReferencedEntity;
import org.fiware.tmforum.service.ServiceCandidate;
import org.fiware.tmforum.servicecatalog.TMForumMapper;
import reactor.core.publisher.Mono;

import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Controller("${general.basepath:/}")
public class ServiceCandidateApiController extends AbstractApiController<ServiceCandidate>
		implements ServiceCandidateApi {

	private final TMForumMapper tmForumMapper;
	private final Clock clock;

	public ServiceCandidateApiController(ReferenceValidationService validationService,
			TmForumRepository resourceCatalogRepository, TMForumMapper tmForumMapper,
			Clock clock, EventHandler eventHandler) {
		super(validationService, resourceCatalogRepository, eventHandler);
		this.tmForumMapper = tmForumMapper;
		this.clock = clock;
	}

	@Override
	public Mono<HttpResponse<ServiceCandidateVO>> createServiceCandidate(
			@NonNull ServiceCandidateCreateVO resourceCandidateCreateVO) {
		ServiceCandidate resourceCandidate = tmForumMapper.map(
				tmForumMapper.map(resourceCandidateCreateVO,
						IdHelper.toNgsiLd(UUID.randomUUID().toString(), ServiceCandidate.TYPE_SERVICE_CANDIDATE)));
		resourceCandidate.setLastUpdate(clock.instant());

		return create(getCheckingMono(resourceCandidate), ServiceCandidate.class)
				.map(tmForumMapper::map)
				.map(HttpResponse::created);
	}

	private Mono<ServiceCandidate> getCheckingMono(ServiceCandidate resourceCandidate) {
		List<List<? extends ReferencedEntity>> references = new ArrayList<>();
		references.add(resourceCandidate.getCategory());

		Optional.ofNullable(resourceCandidate.getServiceSpecification())
				.map(List::of)
				.ifPresent(references::add);

		return getCheckingMono(resourceCandidate, references)
				.onErrorMap(throwable ->
						new TmForumException(
								String.format("Was not able to create service candidate %s", resourceCandidate.getId()),
								throwable,
								TmForumExceptionReason.INVALID_RELATIONSHIP));
	}

	@Override
	public Mono<HttpResponse<Object>> deleteServiceCandidate(@NonNull String id) {
		return delete(id);
	}

	@Override
	public Mono<HttpResponse<List<ServiceCandidateVO>>> listServiceCandidate(@Nullable String fields,
			@Nullable Integer offset, @Nullable Integer limit) {
		return list(offset, limit, ServiceCandidate.TYPE_SERVICE_CANDIDATE, ServiceCandidate.class)
				.map(resourceFunctionStream -> resourceFunctionStream.map(tmForumMapper::map).toList())
				.switchIfEmpty(Mono.just(List.of()))
				.map(HttpResponse::ok);
	}

	@Override
	public Mono<HttpResponse<ServiceCandidateVO>> patchServiceCandidate(@NonNull String id,
			@NonNull ServiceCandidateUpdateVO resourceCandidateUpdateVO) {
		// non-ngsi-ld ids cannot exist.
		if (!IdHelper.isNgsiLdId(id)) {
			throw new TmForumException("Did not receive a valid id, such service candidate cannot exist.",
					TmForumExceptionReason.NOT_FOUND);
		}
		ServiceCandidate resourceCandidate = tmForumMapper.map(resourceCandidateUpdateVO, id);
		resourceCandidate.setLastUpdate(clock.instant());

		return patch(id, resourceCandidate, getCheckingMono(resourceCandidate), ServiceCandidate.class)
				.map(tmForumMapper::map)
				.map(HttpResponse::ok);
	}

	@Override
	public Mono<HttpResponse<ServiceCandidateVO>> retrieveServiceCandidate(@NonNull String id,
			@Nullable String fields) {
		return retrieve(id, ServiceCandidate.class)
				.switchIfEmpty(Mono.error(new TmForumException("No such service candidate exists.",
						TmForumExceptionReason.NOT_FOUND)))
				.map(tmForumMapper::map)
				.map(HttpResponse::ok);
	}
}