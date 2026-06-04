package org.fiware.tmforum.resourceinventory.rest;

import io.micronaut.core.annotation.NonNull;
import io.micronaut.core.annotation.Nullable;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.context.ServerRequestContext;
import lombok.extern.slf4j.Slf4j;
import org.fiware.resourceinventory.api.ResourceApi;
import org.fiware.resourceinventory.model.ResourceCreateVO;
import org.fiware.resourceinventory.model.ResourceUpdateVO;
import org.fiware.resourceinventory.model.ResourceVO;
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
import org.fiware.tmforum.resource.*;
import org.fiware.tmforum.resourceinventory.TMForumMapper;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.util.*;

import static org.fiware.tmforum.common.CommonConstants.DEFAULT_LIMIT;
import static org.fiware.tmforum.common.CommonConstants.DEFAULT_OFFSET;

@Slf4j
@Controller("${api.resource-inventory.basepath:/}")
public class ResourceApiController extends AbstractApiController<Resource> implements ResourceApi {

	private final TMForumMapper tmForumMapper;

	public ResourceApiController(QueryParser queryParser, ReferenceValidationService validationService,
			TmForumRepository resourceInventoryRepository,
			TMForumMapper tmForumMapper, TMForumEventHandler eventHandler) {
		super(queryParser, validationService, resourceInventoryRepository, eventHandler);
		this.tmForumMapper = tmForumMapper;
	}

	@Override
	public Mono<HttpResponse<ResourceVO>> createResource(@NonNull ResourceCreateVO resourceCreateVO) {
		Resource resource = tmForumMapper.map(
				tmForumMapper.map(resourceCreateVO,
						IdHelper.toNgsiLd(UUID.randomUUID().toString(), Resource.TYPE_RESOURCE)));

		validateInternalRefs(resource);

		return create(getCheckingMono(resource), Resource.class)
				.map(tmForumMapper::map)
				.map(HttpResponse::created);
	}

	private Mono<Resource> getCheckingMono(Resource resource) {
		List<List<? extends ReferencedEntity>> references = new ArrayList<>();
		references.add(resource.getRelatedParty());
		Optional.ofNullable(resource.getPlace()).ifPresent(place -> references.add(List.of(place)));
		Optional.ofNullable(resource.getResourceSpecification())
				.ifPresent(resourceSpecificationRef -> references.add(List.of(resourceSpecificationRef)));

		Mono<Resource> checkingMono = getCheckingMono(resource, references);

		// check resource refs
		if (resource.getResourceRelationship() != null && !resource.getResourceRelationship().isEmpty()) {
			List<Mono<Resource>> resourceRelCheckingMonos = resource.getResourceRelationship()
					.stream()
					.map(ResourceRelationship::getResource)
					.map(resourceRef -> getCheckingMono(resource, List.of(List.of(resourceRef))))
					.toList();
			if (!resourceRelCheckingMonos.isEmpty()) {
				Mono<Resource> resourceRelCheckingMono = Mono.zip(resourceRelCheckingMonos, p -> resource);
				checkingMono = Mono.zip(resourceRelCheckingMono, checkingMono, (p1, p2) -> resource);
			}
		}

		// check features
		if (resource.getActivationFeature() != null && !resource.getActivationFeature().isEmpty()) {
			List<Mono<Resource>> featureConstraintsCheckingMonos = resource.getActivationFeature()
					.stream()
					.peek(feature -> validateInternalFeatureRefs(feature, resource))
					.filter(feature -> feature.getConstraint() != null)
					.map(feature -> getCheckingMono(resource, List.of(feature.getConstraint())))
					.toList();
			if (!featureConstraintsCheckingMonos.isEmpty()) {
				Mono<Resource> featureConstraintsCheckingMono = Mono.zip(featureConstraintsCheckingMonos,
						p -> resource);
				checkingMono = Mono.zip(featureConstraintsCheckingMono, checkingMono, (p1, p2) -> resource);
			}
		}
		return checkingMono
				.onErrorMap(throwable ->
						new TmForumException(
								String.format("Was not able to create resource %s", resource.getId()),
								throwable,
								TmForumExceptionReason.INVALID_RELATIONSHIP));
	}

	private void validateInternalRefs(Resource resource) {
		if (resource.getNote() != null) {
			List<URI> noteIds = resource.getNote().stream().map(Note::getTmfId).filter(Objects::nonNull).toList();
			if (noteIds.size() != new HashSet<>(noteIds).size()) {
				throw new TmForumException(
						String.format("Duplicate note ids are not allowed: %s", noteIds),
						TmForumExceptionReason.INVALID_DATA);
			}
		}
		if (resource.getResourceCharacteristic() != null) {
			resource.getResourceCharacteristic()
					.forEach(characteristic -> validateInternalCharacteristicRefs(characteristic,
							resource.getResourceCharacteristic()));
		}

	}

	private void validateInternalCharacteristicRefs(Characteristic characteristic,
			List<Characteristic> characteristics) {
		List<String> charIds = characteristics
				.stream()
				.map(Characteristic::getTmfId)
				.filter(Objects::nonNull)
				.toList();
		if (charIds.size() != new HashSet<>(charIds).size()) {
			throw new TmForumException(
					String.format("Duplicate characteristic ids are not allowed: %s", charIds),
					TmForumExceptionReason.INVALID_DATA);
		}

		if (characteristic.getCharacteristicRelationship() != null) {
			characteristic.getCharacteristicRelationship()
					.stream()
					.map(CharacteristicRelationship::getTmfId)
					.filter(charRef -> !charIds.contains(charRef))
					.findFirst()
					.ifPresent(missingId -> {
						throw new TmForumException(
								String.format("Referenced characteristic %s does not exist", missingId),
								TmForumExceptionReason.INVALID_DATA);
					});
		}
	}

	private void validateInternalFeatureRefs(Feature feature, Resource resource) {
		List<String> featureIds = resource.getActivationFeature()
				.stream()
				.map(Feature::getTmfId)
				.toList();
		// check for duplicate ids
		if (featureIds.size() != new HashSet<>(featureIds).size()) {
			throw new TmForumException(String.format("Duplicate feature ids are not allowed: %s", featureIds),
					TmForumExceptionReason.INVALID_DATA);
		}
		if (feature.getFeatureRelationship() != null) {
			feature.getFeatureRelationship()
					.stream()
					.map(FeatureRelationship::getTmfId)
					.filter(featureRef -> !featureIds.contains(featureRef))
					.findFirst()
					.ifPresent(missingId -> {
						throw new TmForumException(
								String.format("Referenced feature %s does not exist", missingId),
								TmForumExceptionReason.INVALID_DATA);
					});
		}
		if (feature.getFeatureCharacteristic() != null) {
			feature.getFeatureCharacteristic()
					.forEach(characteristic -> validateInternalCharacteristicRefs(characteristic,
							feature.getFeatureCharacteristic()));
		}
	}

	@Override
	public Mono<HttpResponse<Object>> deleteResource(@NonNull String id) {
		return delete(id);
	}

	/**
	 * Polymorphic listing per the TMF spec: returns the union of entities natively stored with
	 * NGSI-LD type {@code resource} (pure Resources and entities created before {@code @baseType}
	 * was persisted) and entities declaring {@code "@baseType": "Resource"} regardless of which
	 * module owns their concrete {@code @type} (e.g. {@code SoftwareSupportPackage},
	 * {@code InstalledSoftware}, {@code LogicalResource}). The hierarchy lives in the broker
	 * data, not in shared code, so this works in both all-in-one and split deployments —
	 * subtypes not known to this module are returned as base {@link Resource} with the
	 * subtype-specific fields preserved via the framework's {@code additionalProperties}
	 * mechanism.
	 */
	@Override
	public Mono<HttpResponse<List<ResourceVO>>> listResource(@Nullable String fields, @Nullable Integer offset,
			@Nullable Integer limit) {
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
				? clientParams.type() : Resource.TYPE_RESOURCE;

		// Each branch fetches up to (offset + limit) so that after dedup-and-skip we still have
		// enough to fill the requested page in the worst case where the two result sets are disjoint.
		int fetchUpTo = effectiveOffset + effectiveLimit;

		// Branch A: entities natively stored under NGSI-LD type "resource".
		// switchIfEmpty is mandatory: TmForumRepository.findEntities returns Mono.empty() (not
		// Mono.just([])) when the broker returns no entities, which would propagate through
		// Mono.zip and surface as a 404 from Micronaut.
		Mono<List<Resource>> byType =
				repository.findEntities(0, fetchUpTo, Resource.class, clientQ, clientIds, clientType)
						.switchIfEmpty(Mono.just(List.of()));

		// Branch B: entities declaring @baseType matching this controller's base class.
		// atBaseType stores the TMF PascalCase form (e.g. "Resource"), which by convention
		// matches the Java class's simple name — so we derive the filter value from the class
		// rather than hardcoding the string. NGSI-LD AND is ";".
		String baseTypeFilter = String.format("atBaseType==\"%s\"", Resource.class.getSimpleName());
		String combinedQ = (clientQ == null || clientQ.isEmpty())
				? baseTypeFilter
				: "(" + clientQ + ");" + baseTypeFilter;
		Mono<List<Resource>> byBaseType =
				repository.findEntities(0, fetchUpTo, Resource.class, combinedQ, clientIds, null)
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
		return queryParser.toNgsiLdQuery(Resource.class, request.getUri().getQuery());
	}

	private List<ResourceVO> mergeAndPage(List<Resource> byType, List<Resource> byBaseType,
			int offset, int limit) {
		Map<URI, Resource> dedup = new LinkedHashMap<>();
		for (Resource r : byType) {
			dedup.putIfAbsent(r.getId(), r);
		}
		for (Resource r : byBaseType) {
			dedup.putIfAbsent(r.getId(), r);
		}
		return dedup.values().stream()
				.skip(offset)
				.limit(limit)
				.map(tmForumMapper::map)
				.toList();
	}

	@Override
	public Mono<HttpResponse<ResourceVO>> patchResource(@NonNull String id,
			@NonNull ResourceUpdateVO resourceUpdateVO) {
		// non-ngsi-ld ids cannot exist.
		if (!IdHelper.isNgsiLdId(id)) {
			throw new TmForumException("Did not receive a valid id, such resource cannot exist.",
					TmForumExceptionReason.NOT_FOUND);
		}

		Resource resource = tmForumMapper.map(resourceUpdateVO, id);
		validateInternalRefs(resource);

		return patch(id, resource, getCheckingMono(resource), Resource.class)
				.map(tmForumMapper::map)
				.map(HttpResponse::ok);
	}

	@Override
	public Mono<HttpResponse<ResourceVO>> retrieveResource(@NonNull String id, @Nullable String fields) {
		return retrieve(id, Resource.class)
				.switchIfEmpty(Mono.error(new TmForumException("No such resource exists.",
						TmForumExceptionReason.NOT_FOUND)))
				.map(tmForumMapper::map)
				.map(HttpResponse::ok);
	}
}
