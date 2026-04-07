package org.fiware.tmforum.softwaremanagement.rest;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.micronaut.core.annotation.NonNull;
import io.micronaut.core.annotation.Nullable;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.annotation.Controller;
import lombok.extern.slf4j.Slf4j;
import org.fiware.softwaremanagement.api.ResourceApi;
import org.fiware.softwaremanagement.model.*;
import org.fiware.tmforum.common.exception.TmForumException;
import org.fiware.tmforum.common.exception.TmForumExceptionReason;
import org.fiware.tmforum.common.mapping.IdHelper;
import org.fiware.tmforum.common.notification.TMForumEventHandler;
import org.fiware.tmforum.common.querying.QueryParser;
import org.fiware.tmforum.common.repository.TmForumRepository;
import org.fiware.tmforum.common.rest.AbstractApiController;
import org.fiware.tmforum.common.validation.ReferenceValidationService;
import org.fiware.tmforum.common.validation.ReferencedEntity;
import org.fiware.tmforum.resource.*;
import org.fiware.tmforum.softwaremanagement.TMForumMapper;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.util.*;
import java.util.stream.Stream;

/**
 * REST controller for the Resource API within the Software Management module (TMF730).
 * Provides CRUD operations for Resource entities and all sub-types
 * (LogicalResource, SoftwareResource, API, InstalledSoftware, HostingPlatformRequirement,
 * PhysicalResource, SoftwareSupportPackage).
 *
 * <p>Polymorphic dispatch is based on the {@code @type} field in request payloads and
 * the NGSI-LD entity type embedded in entity IDs.</p>
 */
@Slf4j
@Controller("${api.software-management.basepath:/}")
public class ResourceApiController extends AbstractApiController<Resource> implements ResourceApi {

	private final TMForumMapper tmForumMapper;
	private final ObjectMapper objectMapper;

	/**
	 * Create a new ResourceApiController.
	 *
	 * @param queryParser       the query parser for filtering
	 * @param validationService the reference validation service
	 * @param repository        the TM Forum repository
	 * @param tmForumMapper     the mapper for entity/VO conversions
	 * @param eventHandler      the event handler for notifications
	 * @param objectMapper      the Jackson object mapper for sub-type VO conversion
	 */
	public ResourceApiController(QueryParser queryParser, ReferenceValidationService validationService,
			TmForumRepository repository,
			TMForumMapper tmForumMapper, TMForumEventHandler eventHandler,
			ObjectMapper objectMapper) {
		super(queryParser, validationService, repository, eventHandler);
		this.tmForumMapper = tmForumMapper;
		this.objectMapper = objectMapper;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Mono<HttpResponse<ResourceVO>> createResource(@NonNull ResourceCreateVO resourceCreateVO) {
		String atType = resourceCreateVO.getAtType();
		String entityType = ResourceTypeRegistry.getResourceEntityType(atType);

		if (ResourceTypeRegistry.RESOURCE_TYPES.containsKey(atType)) {
			return createSubTypeResource(resourceCreateVO, entityType, atType);
		}

		// Default: create base Resource
		Resource resource = tmForumMapper.map(
				tmForumMapper.map(resourceCreateVO,
						IdHelper.toNgsiLd(UUID.randomUUID().toString(), Resource.TYPE_RESOURCE)));

		validateInternalRefs(resource);

		return create(getCheckingMono(resource), Resource.class)
				.map(tmForumMapper::map)
				.map(HttpResponse::created);
	}

	/**
	 * Create a sub-type Resource entity. Uses Jackson ObjectMapper to convert the base create VO
	 * to the sub-type VO (capturing unknown properties as typed fields), then MapStruct to convert
	 * to the domain entity.
	 *
	 * @param createVO   the base resource create value object
	 * @param entityType the NGSI-LD entity type
	 * @param atType     the TMForum @type string
	 * @return the created resource as a Mono of HttpResponse
	 */
	@SuppressWarnings("unchecked")
	private Mono<HttpResponse<ResourceVO>> createSubTypeResource(ResourceCreateVO createVO,
			String entityType, String atType) {
		URI id = IdHelper.toNgsiLd(UUID.randomUUID().toString(), entityType);
		Class<? extends Resource> domainClass = ResourceTypeRegistry.RESOURCE_TYPES.get(atType);

		Resource resource = convertCreateVOToDomain(createVO, id, domainClass);
		validateInternalRefs(resource);

		return create(getCheckingMono(resource), Resource.class)
				.map(r -> mapResourceToVO(r))
				.map(HttpResponse::created);
	}

	/**
	 * Convert a ResourceCreateVO to the appropriate sub-type domain entity.
	 * Uses Jackson to serialize to a Map (capturing unknownProperties), adds id/href,
	 * then deserializes to the sub-type VO and maps to the domain class.
	 *
	 * @param createVO    the create VO (with sub-type fields in unknownProperties)
	 * @param id          the generated NGSI-LD ID
	 * @param domainClass the target domain class
	 * @return the domain entity
	 */
	@SuppressWarnings("unchecked")
	private Resource convertCreateVOToDomain(ResourceCreateVO createVO, URI id,
			Class<? extends Resource> domainClass) {
		Map<String, Object> map = objectMapper.convertValue(createVO, Map.class);
		map.put("id", id.toString());
		map.put("href", id.toString());

		Object subTypeVO = objectMapper.convertValue(map, getVOClass(domainClass));
		return mapVOToDomain(subTypeVO, domainClass);
	}

	/**
	 * Map a Resource domain entity to a ResourceVO. For sub-types, maps to the sub-type VO first,
	 * then converts to ResourceVO using Jackson (sub-type fields become unknownProperties).
	 *
	 * @param resource the resource domain entity
	 * @return the mapped ResourceVO
	 */
	private ResourceVO mapResourceToVO(Resource resource) {
		// Order matters: check leaf types before parent types
		if (resource instanceof InstalledSoftware is) {
			return objectMapper.convertValue(tmForumMapper.mapToInstalledSoftwareVO(is), ResourceVO.class);
		} else if (resource instanceof ApiResource ar) {
			return objectMapper.convertValue(tmForumMapper.mapToApiVO(ar), ResourceVO.class);
		} else if (resource instanceof SoftwareResource sr) {
			return objectMapper.convertValue(tmForumMapper.mapToSoftwareResourceVO(sr), ResourceVO.class);
		} else if (resource instanceof HostingPlatformRequirement hpr) {
			return objectMapper.convertValue(
					tmForumMapper.mapToHostingPlatformRequirementVO(hpr), ResourceVO.class);
		} else if (resource instanceof LogicalResource lr) {
			return objectMapper.convertValue(tmForumMapper.mapToLogicalResourceVO(lr), ResourceVO.class);
		} else if (resource instanceof SoftwareSupportPackage ssp) {
			return objectMapper.convertValue(
					tmForumMapper.mapToSoftwareSupportPackageVO(ssp), ResourceVO.class);
		} else if (resource instanceof PhysicalResource pr) {
			return objectMapper.convertValue(tmForumMapper.mapToPhysicalResourceVO(pr), ResourceVO.class);
		}
		return tmForumMapper.map(resource);
	}

	/**
	 * Get the generated VO class for a given domain class.
	 */
	private Class<?> getVOClass(Class<? extends Resource> domainClass) {
		if (domainClass == LogicalResource.class) return LogicalResourceVO.class;
		if (domainClass == SoftwareResource.class) return SoftwareResourceVO.class;
		if (domainClass == ApiResource.class) return APIVO.class;
		if (domainClass == InstalledSoftware.class) return InstalledSoftwareVO.class;
		if (domainClass == HostingPlatformRequirement.class) return HostingPlatformRequirementVO.class;
		if (domainClass == PhysicalResource.class) return PhysicalResourceVO.class;
		if (domainClass == SoftwareSupportPackage.class) return SoftwareSupportPackageVO.class;
		return ResourceVO.class;
	}

	/**
	 * Map a sub-type VO to its domain entity using the TMForumMapper.
	 */
	private Resource mapVOToDomain(Object vo, Class<? extends Resource> domainClass) {
		if (domainClass == LogicalResource.class) return tmForumMapper.map((LogicalResourceVO) vo);
		if (domainClass == SoftwareResource.class) return tmForumMapper.map((SoftwareResourceVO) vo);
		if (domainClass == ApiResource.class) return tmForumMapper.map((APIVO) vo);
		if (domainClass == InstalledSoftware.class) return tmForumMapper.map((InstalledSoftwareVO) vo);
		if (domainClass == HostingPlatformRequirement.class) return tmForumMapper.map((HostingPlatformRequirementVO) vo);
		if (domainClass == PhysicalResource.class) return tmForumMapper.map((PhysicalResourceVO) vo);
		if (domainClass == SoftwareSupportPackage.class) return tmForumMapper.map((SoftwareSupportPackageVO) vo);
		throw new TmForumException("Unknown resource sub-type: " + domainClass.getSimpleName(),
				TmForumExceptionReason.INVALID_DATA);
	}

	/**
	 * Build a checking Mono that validates all external references of a resource.
	 *
	 * @param resource the resource to validate
	 * @return a Mono that completes with the resource if validation passes
	 */
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

	/**
	 * Validate internal references within a resource, including note uniqueness
	 * and characteristic relationship consistency.
	 *
	 * @param resource the resource to validate
	 */
	private void validateInternalRefs(Resource resource) {
		if (resource.getNote() != null) {
			List<URI> noteIds = resource.getNote().stream().map(Note::getTmfId).toList();
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

	/**
	 * Validate that characteristic relationships reference existing characteristics.
	 *
	 * @param characteristic  the characteristic to validate
	 * @param characteristics the list of all characteristics in the resource
	 */
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

	/**
	 * Validate that feature relationships reference existing features within the resource.
	 *
	 * @param feature  the feature to validate
	 * @param resource the resource containing the features
	 */
	private void validateInternalFeatureRefs(Feature feature, Resource resource) {
		List<String> featureIds = resource.getActivationFeature()
				.stream()
				.map(Feature::getTmfId)
				.toList();
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

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Mono<HttpResponse<Object>> deleteResource(@NonNull String id) {
		return delete(id);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Mono<HttpResponse<List<ResourceVO>>> listResource(@Nullable String fields, @Nullable Integer offset,
			@Nullable Integer limit) {
		// Query each registered type and merge results
		List<Mono<List<ResourceVO>>> typeQueries = new ArrayList<>();

		for (Map.Entry<String, Class<? extends Resource>> entry :
				ResourceTypeRegistry.RESOURCE_ENTITY_TYPES.entrySet()) {
			String entityType = entry.getKey();
			Class<? extends Resource> entityClass = entry.getValue();
			Mono<List<ResourceVO>> query = list(offset, limit, entityType, entityClass)
					.map(stream -> stream.map(this::mapResourceToVO).toList())
					.switchIfEmpty(Mono.just(List.of()));
			typeQueries.add(query);
		}

		return Mono.zip(typeQueries, results -> {
			List<ResourceVO> combined = new ArrayList<>();
			for (Object result : results) {
				@SuppressWarnings("unchecked")
				List<ResourceVO> typed = (List<ResourceVO>) result;
				combined.addAll(typed);
			}
			return combined;
		}).map(HttpResponse::ok);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Mono<HttpResponse<ResourceVO>> patchResource(@NonNull String id,
			@NonNull ResourceUpdateVO resourceUpdateVO) {
		if (!IdHelper.isNgsiLdId(id)) {
			throw new TmForumException("Did not receive a valid id, such resource cannot exist.",
					TmForumExceptionReason.NOT_FOUND);
		}

		String entityType = ResourceTypeRegistry.extractTypeFromId(id);
		Class<? extends Resource> entityClass = ResourceTypeRegistry.getResourceClass(entityType);

		if (entityClass != Resource.class) {
			return patchSubTypeResource(id, resourceUpdateVO, entityClass);
		}

		Resource resource = tmForumMapper.map(resourceUpdateVO, id);
		validateInternalRefs(resource);

		return patch(id, resource, getCheckingMono(resource), Resource.class)
				.map(tmForumMapper::map)
				.map(HttpResponse::ok);
	}

	/**
	 * Patch a sub-type Resource entity.
	 */
	@SuppressWarnings("unchecked")
	private Mono<HttpResponse<ResourceVO>> patchSubTypeResource(String id,
			ResourceUpdateVO updateVO, Class<? extends Resource> entityClass) {
		Map<String, Object> map = objectMapper.convertValue(updateVO, Map.class);
		map.put("id", id);
		map.put("href", id);

		Object subTypeVO = objectMapper.convertValue(map, getVOClass(entityClass));
		Resource resource = mapVOToDomain(subTypeVO, entityClass);
		validateInternalRefs(resource);

		URI idUri = URI.create(id);
		return repository.get(idUri, entityClass)
				.switchIfEmpty(Mono.error(new TmForumException("No such resource exists.",
						TmForumExceptionReason.NOT_FOUND)))
				.flatMap(existing -> getCheckingMono(resource))
				.flatMap(checked -> repository.updateDomainEntity(id, resource)
						.then(repository.get(idUri, entityClass)))
				.map(this::mapResourceToVO)
				.map(HttpResponse::ok);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Mono<HttpResponse<ResourceVO>> retrieveResource(@NonNull String id, @Nullable String fields) {
		if (!IdHelper.isNgsiLdId(id)) {
			throw new TmForumException("Did not receive a valid id, such resource cannot exist.",
					TmForumExceptionReason.NOT_FOUND);
		}

		String entityType = ResourceTypeRegistry.extractTypeFromId(id);
		Class<? extends Resource> entityClass = ResourceTypeRegistry.getResourceClass(entityType);

		return retrieve(id, entityClass)
				.switchIfEmpty(Mono.error(new TmForumException("No such resource exists.",
						TmForumExceptionReason.NOT_FOUND)))
				.map(this::mapResourceToVO)
				.map(HttpResponse::ok);
	}
}
