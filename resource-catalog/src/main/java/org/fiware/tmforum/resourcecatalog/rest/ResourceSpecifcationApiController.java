package org.fiware.tmforum.resourcecatalog.rest;

import io.micronaut.core.annotation.NonNull;
import io.micronaut.core.annotation.Nullable;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.context.ServerRequestContext;
import lombok.extern.slf4j.Slf4j;
import org.fiware.resourcecatalog.api.ResourceSpecificationApi;
import org.fiware.resourcecatalog.model.ResourceSpecificationCreateVO;
import org.fiware.resourcecatalog.model.ResourceSpecificationUpdateVO;
import org.fiware.resourcecatalog.model.ResourceSpecificationVO;
import org.fiware.tmforum.common.exception.TmForumException;
import org.fiware.tmforum.common.exception.TmForumExceptionReason;
import org.fiware.tmforum.common.mapping.IdHelper;
import org.fiware.tmforum.common.notification.TMForumEventHandler;
import org.fiware.tmforum.common.querying.QueryParams;
import org.fiware.tmforum.common.querying.QueryParser;
import org.fiware.tmforum.common.repository.TmForumRepository;
import org.fiware.tmforum.common.rest.AbstractApiController;
import org.fiware.tmforum.common.validation.ReferenceValidationService;
import org.fiware.tmforum.common.validation.ReferencedEntity;
import org.fiware.tmforum.resource.FeatureSpecification;
import org.fiware.tmforum.resource.FeatureSpecificationCharacteristicRelationship;
import org.fiware.tmforum.resource.ResourceSpecification;
import org.fiware.tmforum.resource.ResourceSpecificationCharacteristic;
import org.fiware.tmforum.resourcecatalog.TMForumMapper;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.time.Clock;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.fiware.tmforum.common.CommonConstants.DEFAULT_LIMIT;
import static org.fiware.tmforum.common.CommonConstants.DEFAULT_OFFSET;

@Slf4j
@Controller("${api.resource-catalog.basepath:/}")
public class ResourceSpecifcationApiController extends AbstractApiController<ResourceSpecification>
		implements ResourceSpecificationApi {

	private final TMForumMapper tmForumMapper;
	private final Clock clock;

	public ResourceSpecifcationApiController(QueryParser queryParser, ReferenceValidationService validationService,
			TmForumRepository resourceCatalogRepository, TMForumMapper tmForumMapper,
			Clock clock, TMForumEventHandler eventHandler) {
		super(queryParser, validationService, resourceCatalogRepository, eventHandler);
		this.tmForumMapper = tmForumMapper;
		this.clock = clock;
	}

	@Override
	public Mono<HttpResponse<ResourceSpecificationVO>> createResourceSpecification(
			@NonNull ResourceSpecificationCreateVO resourceSpecificationCreateVO) {
		if (resourceSpecificationCreateVO.getName() == null) {
			throw new TmForumException(
					String.format("The specification create does not contain all mandatory values: %s.",
							resourceSpecificationCreateVO), TmForumExceptionReason.INVALID_DATA);
		}
		if (resourceSpecificationCreateVO.getIsBundle() == null) {
			// set default required by the conformance
			resourceSpecificationCreateVO.isBundle(false);
		}
		if (resourceSpecificationCreateVO.getLifecycleStatus() == null) {
			// set default required by the conformance
			resourceSpecificationCreateVO.lifecycleStatus("created");
		}

		ResourceSpecification resourceSpecification = tmForumMapper.map(
				tmForumMapper.map(resourceSpecificationCreateVO, IdHelper.toNgsiLd(UUID.randomUUID().toString(),
						ResourceSpecification.TYPE_RESOURCE_SPECIFICATION)));
		resourceSpecification.setLastUpdate(clock.instant());

		Mono<ResourceSpecification> checkingMono = getCheckingMono(resourceSpecification);
		checkingMono = Mono.zip(checkingMono, validateSpec(resourceSpecification), (p1, p2) -> resourceSpecification);

		return create(checkingMono, ResourceSpecification.class)
				.map(tmForumMapper::map)
				.map(HttpResponse::created);
	}

	private Mono<ResourceSpecification> validateSpec(ResourceSpecification resourceSpecification) {
		Mono<ResourceSpecification> validatingMono = Mono.just(resourceSpecification);

		if (resourceSpecification.getFeatureSpecification() != null && !resourceSpecification.getFeatureSpecification()
				.isEmpty()) {

			List<Mono<ResourceSpecification>> fsCheckingMonos = resourceSpecification.getFeatureSpecification()
					.stream()
					.map(featureSpecification -> validateFeatureSpecification(resourceSpecification,
							featureSpecification))
					.toList();
			if (!fsCheckingMonos.isEmpty()) {
				Mono<ResourceSpecification> fsCheckingMono = Mono.zip(fsCheckingMonos, p1 -> resourceSpecification);
				validatingMono = Mono.zip(validatingMono, fsCheckingMono, (p1, p2) -> resourceSpecification);
			}
		}

		if (resourceSpecification.getResourceSpecCharacteristic() != null && !resourceSpecification.getResourceSpecCharacteristic()
				.isEmpty()) {

			List<Mono<ResourceSpecification>> rscCheckingMonos = resourceSpecification.getResourceSpecCharacteristic()
					.stream()
					.map(resourceSpecificationCharacteristic -> validateResourceSpecChar(resourceSpecification,
							resourceSpecificationCharacteristic))
					.toList();
			if (!rscCheckingMonos.isEmpty()) {
				Mono<ResourceSpecification> rscCheckingMono = Mono.zip(rscCheckingMonos, p1 -> resourceSpecification);
				validatingMono = Mono.zip(validatingMono, rscCheckingMono, (p1, p2) -> resourceSpecification);

			}
		}

		return validatingMono;
	}

	private Mono<ResourceSpecification> validateResourceSpecChar(
			ResourceSpecification resourceSpecification,
			ResourceSpecificationCharacteristic resourceSpecificationCharacteristic) {
		if (resourceSpecificationCharacteristic.getResourceSpecCharRelationship() != null) {
			List<List<? extends ReferencedEntity>> references = new ArrayList<>();
			resourceSpecificationCharacteristic.getResourceSpecCharRelationship().forEach(rscr ->
					Optional.ofNullable(rscr.getResourceSpecificationId())
							.map(List::of)
							.ifPresent(references::add)
			);
			return getCheckingMono(resourceSpecification, references)
					.onErrorMap(throwable ->
							new TmForumException(
									String.format("Resource spec char contains invalid references: %s",
											resourceSpecificationCharacteristic),
									throwable,
									TmForumExceptionReason.INVALID_RELATIONSHIP));
		} else {
			return Mono.just(resourceSpecification);
		}
	}

	private Mono<ResourceSpecification> validateFeatureSpecification(ResourceSpecification resourceSpecification,
			FeatureSpecification featureSpecification) {
		List<List<? extends ReferencedEntity>> references = new ArrayList<>();
		references.add(featureSpecification.getConstraint());

		if (featureSpecification.getFeatureSpecRelationship() != null) {
			featureSpecification.getFeatureSpecRelationship().forEach(fsr ->
					Optional.ofNullable(fsr.getParentSpecificationId())
							.map(List::of)
							.ifPresent(references::add));
		}

		if (featureSpecification.getFeatureSpecCharacteristic() != null) {
			featureSpecification.getFeatureSpecCharacteristic().forEach(fsc -> {
				if (fsc.getFeatureSpecCharRelationship() != null) {
					fsc.getFeatureSpecCharRelationship().forEach(fscr -> addReferencesForFSCR(fscr, references));
				}
			});
		}

		return getCheckingMono(resourceSpecification, references)
				.onErrorMap(throwable ->
						new TmForumException(
								String.format("Feature spec contains invalid references: %s", featureSpecification),
								throwable,
								TmForumExceptionReason.INVALID_RELATIONSHIP));
	}

	private void addReferencesForFSCR(FeatureSpecificationCharacteristicRelationship fscr,
			List<List<? extends ReferencedEntity>> references) {
		Optional.ofNullable(fscr.getResourceSpecificationId())
				.map(List::of)
				.ifPresent(references::add);
	}

	private Mono<ResourceSpecification> getCheckingMono(ResourceSpecification resourceSpecification) {

		if (resourceSpecification.getRelatedParty() != null && !resourceSpecification.getRelatedParty().isEmpty()) {
			return getCheckingMono(resourceSpecification, List.of(resourceSpecification.getRelatedParty()))
					.onErrorMap(throwable ->
							new TmForumException(
									String.format("Was not able to create resource specification %s",
											resourceSpecification.getId()),
									throwable,
									TmForumExceptionReason.INVALID_RELATIONSHIP));
		} else {
			return Mono.just(resourceSpecification);
		}

	}

	@Override
	public Mono<HttpResponse<Object>> deleteResourceSpecification(@NonNull String id) {
		return delete(id);
	}

	/**
	 * Polymorphic listing per the TMF spec: returns the union of entities natively stored with
	 * NGSI-LD type {@code resource-specification} and entities declaring
	 * {@code "@baseType": "ResourceSpecification"} regardless of which module owns their
	 * concrete {@code @type} (e.g. {@code SoftwareSupportPackageSpecification},
	 * {@code SoftwareSpecification}, {@code LogicalResourceSpecification}). Same shape as
	 * {@link org.fiware.tmforum.resourceinventory.rest.ResourceApiController#listResource}.
	 */
	@Override
	public Mono<HttpResponse<List<ResourceSpecificationVO>>> listResourceSpecification(@Nullable String fields,
			@Nullable Integer offset, @Nullable Integer limit) {
		int effectiveOffset = Optional.ofNullable(offset).orElse(DEFAULT_OFFSET);
		int effectiveLimit = Optional.ofNullable(limit).orElse(DEFAULT_LIMIT);

		if (effectiveOffset < 0 || effectiveLimit < 1) {
			throw new TmForumException(
					String.format("Invalid offset %s or limit %s.", effectiveOffset, effectiveLimit),
					TmForumExceptionReason.INVALID_DATA);
		}

		QueryParams clientParams = parseClientQuery();
		String clientQ = clientParams != null ? clientParams.query() : null;
		String clientIds = clientParams != null ? clientParams.id() : null;
		String clientType = clientParams != null && clientParams.type() != null
				? clientParams.type() : ResourceSpecification.TYPE_RESOURCE_SPECIFICATION;

		int fetchUpTo = effectiveOffset + effectiveLimit;

		// Branch A: entities natively stored under NGSI-LD type "resource-specification".
		Mono<List<ResourceSpecification>> byType =
				repository.findEntities(0, fetchUpTo, ResourceSpecification.class, clientQ, clientIds, clientType)
						.switchIfEmpty(Mono.just(List.of()));

		// Branch B: entities declaring @baseType matching this controller's base class.
		String baseTypeFilter = String.format("atBaseType==\"%s\"", ResourceSpecification.class.getSimpleName());
		String combinedQ = (clientQ == null || clientQ.isEmpty())
				? baseTypeFilter
				: "(" + clientQ + ");" + baseTypeFilter;
		Mono<List<ResourceSpecification>> byBaseType =
				repository.findEntities(0, fetchUpTo, ResourceSpecification.class, combinedQ, clientIds, null)
						.switchIfEmpty(Mono.just(List.of()));

		return Mono.zip(byType, byBaseType)
				.map(tuple -> mergeAndPage(tuple.getT1(), tuple.getT2(), effectiveOffset, effectiveLimit))
				.map(HttpResponse::ok);
	}

	private QueryParams parseClientQuery() {
		Optional<HttpRequest<Object>> optionalHttpRequest = ServerRequestContext.currentRequest();
		if (optionalHttpRequest.isEmpty()) {
			log.warn("The original request is not available, no filters will be applied.");
			return null;
		}
		HttpRequest<Object> request = optionalHttpRequest.get();
		Map<String, List<String>> parameters = request.getParameters().asMap();
		if (!QueryParser.hasFilter(parameters)) {
			return null;
		}
		return queryParser.toNgsiLdQuery(ResourceSpecification.class, request.getUri().getQuery());
	}

	private List<ResourceSpecificationVO> mergeAndPage(List<ResourceSpecification> byType,
			List<ResourceSpecification> byBaseType, int offset, int limit) {
		Map<URI, ResourceSpecification> dedup = new LinkedHashMap<>();
		for (ResourceSpecification rs : byType) {
			dedup.putIfAbsent(rs.getId(), rs);
		}
		for (ResourceSpecification rs : byBaseType) {
			dedup.putIfAbsent(rs.getId(), rs);
		}
		return dedup.values().stream()
				.skip(offset)
				.limit(limit)
				.map(tmForumMapper::map)
				.toList();
	}

	@Override
	public Mono<HttpResponse<ResourceSpecificationVO>> patchResourceSpecification(@NonNull String id,
			@NonNull ResourceSpecificationUpdateVO resourceSpecificationUpdateVO) {
		// non-ngsi-ld ids cannot exist.
		if (!IdHelper.isNgsiLdId(id)) {
			throw new TmForumException("Did not receive a valid id, such resource spec cannot exist.",
					TmForumExceptionReason.NOT_FOUND);
		}

		ResourceSpecification resourceSpecification = tmForumMapper.map(resourceSpecificationUpdateVO, id);
		resourceSpecification.setLastUpdate(clock.instant());

		Mono<ResourceSpecification> checkingMono = getCheckingMono(resourceSpecification);
		checkingMono = Mono.zip(checkingMono, validateSpec(resourceSpecification), (p1, p2) -> resourceSpecification);

		return patch(id, resourceSpecification, checkingMono, ResourceSpecification.class)
				.map(tmForumMapper::map)
				.map(HttpResponse::ok);
	}

	@Override
	public Mono<HttpResponse<ResourceSpecificationVO>> retrieveResourceSpecification(@NonNull String id,
			@Nullable String fields) {
		return retrieve(id, ResourceSpecification.class)
				.switchIfEmpty(Mono.error(new TmForumException("No such resources specification exists.",
						TmForumExceptionReason.NOT_FOUND)))
				.map(tmForumMapper::map)
				.map(HttpResponse::ok);
	}

}
