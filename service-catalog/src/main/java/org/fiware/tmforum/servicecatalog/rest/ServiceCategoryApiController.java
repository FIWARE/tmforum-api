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
import org.fiware.tmforum.common.exception.TmForumException;
import org.fiware.tmforum.common.exception.TmForumExceptionReason;
import org.fiware.tmforum.common.mapping.IdHelper;
import org.fiware.tmforum.common.notification.TMForumEventHandler;
import org.fiware.tmforum.common.querying.QueryParser;
import org.fiware.tmforum.common.repository.TmForumRepository;
import org.fiware.tmforum.common.rest.AbstractApiController;
import org.fiware.tmforum.common.validation.ReferenceValidationService;
import org.fiware.tmforum.common.validation.ReferencedEntity;
import org.fiware.tmforum.service.ServiceCategory;
import org.fiware.tmforum.servicecatalog.TMForumMapper;
import reactor.core.publisher.Mono;

import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Controller("${general.basepath:/}")
public class ServiceCategoryApiController extends AbstractApiController<ServiceCategory> implements ServiceCategoryApi {

	private final TMForumMapper tmForumMapper;
	private final Clock clock;

	public ServiceCategoryApiController(QueryParser queryParser, ReferenceValidationService validationService,
			TmForumRepository serviceCatalogRepository, TMForumMapper tmForumMapper,
			Clock clock, TMForumEventHandler eventHandler) {
		super(queryParser, validationService, serviceCatalogRepository, eventHandler);
		this.tmForumMapper = tmForumMapper;
		this.clock = clock;
	}

	@Override
	public Mono<HttpResponse<ServiceCategoryVO>> createServiceCategory(
			@NonNull ServiceCategoryCreateVO serviceCategoryCreateVO) {
		ServiceCategory serviceCategory = tmForumMapper.map(
				tmForumMapper.map(serviceCategoryCreateVO,
						IdHelper.toNgsiLd(UUID.randomUUID().toString(), ServiceCategory.TYPE_SERVICE_CATEGORY)));
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
						new TmForumException(
								String.format("Was not able to create service category %s", serviceCategory.getId()),
								throwable,
								TmForumExceptionReason.INVALID_RELATIONSHIP));
	}

	@Override
	public Mono<HttpResponse<Object>> deleteServiceCategory(@NonNull String id) {
		return delete(id);
	}

	@Override
	public Mono<HttpResponse<List<ServiceCategoryVO>>> listServiceCategory(@Nullable String fields,
			@Nullable Integer offset, @Nullable Integer limit) {
		return list(offset, limit, ServiceCategory.TYPE_SERVICE_CATEGORY, ServiceCategory.class)
				.map(serviceCategoryStream -> serviceCategoryStream.map(tmForumMapper::map).toList())
				.switchIfEmpty(Mono.just(List.of()))
				.map(HttpResponse::ok);
	}

	@Override
	public Mono<HttpResponse<ServiceCategoryVO>> patchServiceCategory(@NonNull String id,
			@NonNull ServiceCategoryUpdateVO serviceCategoryUpdateVO) {
		// non-ngsi-ld ids cannot exist.
		if (!IdHelper.isNgsiLdId(id)) {
			throw new TmForumException("Did not receive a valid id, such service category cannot exist.",
					TmForumExceptionReason.NOT_FOUND);
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
				.switchIfEmpty(Mono.error(new TmForumException("No such services category exists.",
						TmForumExceptionReason.NOT_FOUND)))
				.map(tmForumMapper::map)
				.map(HttpResponse::ok);
	}
}
