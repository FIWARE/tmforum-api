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
import org.fiware.tmforum.common.notification.EventHandler;
import org.fiware.tmforum.common.exception.TmForumException;
import org.fiware.tmforum.common.exception.TmForumExceptionReason;
import org.fiware.tmforum.common.mapping.IdHelper;
import org.fiware.tmforum.common.repository.TmForumRepository;
import org.fiware.tmforum.common.rest.AbstractApiController;
import org.fiware.tmforum.common.validation.ReferenceValidationService;
import org.fiware.tmforum.common.validation.ReferencedEntity;
import org.fiware.tmforum.servicecatalog.TMForumMapper;
import org.fiware.tmforum.servicecatalog.domain.ServiceCatalog;
import reactor.core.publisher.Mono;

import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
@Controller("${general.basepath:/}")
public class ServiceCatalogApiController extends AbstractApiController<ServiceCatalog> implements ServiceCatalogApi {

	private final TMForumMapper tmForumMapper;
	private final Clock clock;

	public ServiceCatalogApiController(ReferenceValidationService validationService,
			TmForumRepository serviceCatalogRepository, TMForumMapper tmForumMapper,
			Clock clock, EventHandler eventHandler) {
		super(validationService, serviceCatalogRepository, eventHandler);
		this.tmForumMapper = tmForumMapper;
		this.clock = clock;
	}

	@Override
	public Mono<HttpResponse<ServiceCatalogVO>> createServiceCatalog(
			@NonNull ServiceCatalogCreateVO serviceCatalogCreateVO) {
		ServiceCatalog serviceCatalog = tmForumMapper.map(
				tmForumMapper.map(serviceCatalogCreateVO,
						IdHelper.toNgsiLd(UUID.randomUUID().toString(), ServiceCatalog.TYPE_SERVICE_CATALOG)));
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
						new TmForumException(
								String.format("Was not able to create service catalog %s", serviceCatalog.getId()),
								throwable,
								TmForumExceptionReason.INVALID_RELATIONSHIP));
	}

	@Override
	public Mono<HttpResponse<Object>> deleteServiceCatalog(@NonNull String id) {
		return delete(id);
	}

	@Override
	public Mono<HttpResponse<List<ServiceCatalogVO>>> listServiceCatalog(@Nullable String fields,
			@Nullable Integer offset, @Nullable Integer limit) {
		return list(offset, limit, ServiceCatalog.TYPE_SERVICE_CATALOG, ServiceCatalog.class)
				.map(serviceCatalogStream -> serviceCatalogStream.map(tmForumMapper::map).toList())
				.switchIfEmpty(Mono.just(List.of()))
				.map(HttpResponse::ok);
	}

	@Override
	public Mono<HttpResponse<ServiceCatalogVO>> patchServiceCatalog(@NonNull String id,
			@NonNull ServiceCatalogUpdateVO serviceCatalogUpdateVO) {
		// non-ngsi-ld ids cannot exist.
		if (!IdHelper.isNgsiLdId(id)) {
			throw new TmForumException("Did not receive a valid id, such resource catalog cannot exist.",
					TmForumExceptionReason.NOT_FOUND);
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
				.switchIfEmpty(Mono.error(new TmForumException("No such resources catalog exists.",
						TmForumExceptionReason.NOT_FOUND)))
				.map(tmForumMapper::map)
				.map(HttpResponse::ok);
	}
}
