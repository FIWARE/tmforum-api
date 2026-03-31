package org.fiware.tmforum.softwaremanagement.rest;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.micronaut.core.annotation.NonNull;
import io.micronaut.core.annotation.Nullable;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.annotation.Controller;
import lombok.extern.slf4j.Slf4j;
import org.fiware.softwaremanagement.api.ResourceSpecificationApi;
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
import java.time.Clock;
import java.util.*;
import java.util.stream.Stream;

/**
 * REST controller for the ResourceSpecification API within the Software Management module (TMF730).
 * Provides CRUD operations for ResourceSpecification entities and all sub-types
 * (LogicalResourceSpecification, SoftwareResourceSpecification, APISpecification,
 * SoftwareSpecification, HostingPlatformRequirementSpecification,
 * PhysicalResourceSpecification, SoftwareSupportPackageSpecification).
 *
 * <p>Polymorphic dispatch is based on the {@code @type} field in request payloads and
 * the NGSI-LD entity type embedded in entity IDs.</p>
 */
@Slf4j
@Controller("${api.software-management.basepath:/}")
public class ResourceSpecificationApiController extends AbstractApiController<ResourceSpecification>
		implements ResourceSpecificationApi {

	private final TMForumMapper tmForumMapper;
	private final Clock clock;
	private final ObjectMapper objectMapper;

	/**
	 * Create a new ResourceSpecificationApiController.
	 *
	 * @param queryParser       the query parser for filtering
	 * @param validationService the reference validation service
	 * @param repository        the TM Forum repository
	 * @param tmForumMapper     the mapper for entity/VO conversions
	 * @param clock             the clock for lastUpdate timestamps
	 * @param eventHandler      the event handler for notifications
	 * @param objectMapper      the Jackson object mapper for sub-type VO conversion
	 */
	public ResourceSpecificationApiController(QueryParser queryParser, ReferenceValidationService validationService,
			TmForumRepository repository, TMForumMapper tmForumMapper,
			Clock clock, TMForumEventHandler eventHandler,
			ObjectMapper objectMapper) {
		super(queryParser, validationService, repository, eventHandler);
		this.tmForumMapper = tmForumMapper;
		this.clock = clock;
		this.objectMapper = objectMapper;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Mono<HttpResponse<ResourceSpecificationVO>> createResourceSpecification(
			@NonNull ResourceSpecificationCreateVO resourceSpecificationCreateVO) {
		if (resourceSpecificationCreateVO.getName() == null) {
			throw new TmForumException(
					String.format("The specification create does not contain all mandatory values: %s.",
							resourceSpecificationCreateVO), TmForumExceptionReason.INVALID_DATA);
		}
		if (resourceSpecificationCreateVO.getIsBundle() == null) {
			resourceSpecificationCreateVO.isBundle(false);
		}
		if (resourceSpecificationCreateVO.getLifecycleStatus() == null) {
			resourceSpecificationCreateVO.lifecycleStatus("created");
		}

		String atType = resourceSpecificationCreateVO.getAtType();
		String entityType = ResourceTypeRegistry.getSpecEntityType(atType);

		if (ResourceTypeRegistry.SPEC_TYPES.containsKey(atType)) {
			return createSubTypeSpec(resourceSpecificationCreateVO, entityType, atType);
		}

		// Default: create base ResourceSpecification
		ResourceSpecification resourceSpecification = tmForumMapper.map(
				tmForumMapper.map(resourceSpecificationCreateVO, IdHelper.toNgsiLd(UUID.randomUUID().toString(),
						ResourceSpecification.TYPE_RESOURCE_SPECIFICATION)));
		resourceSpecification.setLastUpdate(clock.instant());

		Mono<ResourceSpecification> checkingMono = getCheckingMono(resourceSpecification);
		checkingMono = Mono.zip(checkingMono, validateSpec(resourceSpecification),
				(p1, p2) -> resourceSpecification);

		return create(checkingMono, ResourceSpecification.class)
				.map(tmForumMapper::map)
				.map(HttpResponse::created);
	}

	/**
	 * Create a sub-type ResourceSpecification entity.
	 */
	@SuppressWarnings("unchecked")
	private Mono<HttpResponse<ResourceSpecificationVO>> createSubTypeSpec(
			ResourceSpecificationCreateVO createVO, String entityType, String atType) {
		URI id = IdHelper.toNgsiLd(UUID.randomUUID().toString(), entityType);
		Class<? extends ResourceSpecification> domainClass = ResourceTypeRegistry.SPEC_TYPES.get(atType);

		Map<String, Object> map = objectMapper.convertValue(createVO, Map.class);
		map.put("id", id.toString());
		map.put("href", id.toString());

		Object subTypeVO = objectMapper.convertValue(map, getSpecVOClass(domainClass));
		ResourceSpecification spec = mapSpecVOToDomain(subTypeVO, domainClass);
		spec.setLastUpdate(clock.instant());

		Mono<ResourceSpecification> checkingMono = getCheckingMono(spec);
		checkingMono = Mono.zip(checkingMono, validateSpec(spec), (p1, p2) -> spec);

		return create(checkingMono, ResourceSpecification.class)
				.map(this::mapSpecToVO)
				.map(HttpResponse::created);
	}

	/**
	 * Map a ResourceSpecification domain entity to a ResourceSpecificationVO.
	 */
	private ResourceSpecificationVO mapSpecToVO(ResourceSpecification spec) {
		// Order matters: check leaf types before parent types
		if (spec instanceof ApiSpecification as) {
			return objectMapper.convertValue(tmForumMapper.mapToApiSpecificationVO(as),
					ResourceSpecificationVO.class);
		} else if (spec instanceof SoftwareSpecification ss) {
			return objectMapper.convertValue(tmForumMapper.mapToSoftwareSpecificationVO(ss),
					ResourceSpecificationVO.class);
		} else if (spec instanceof SoftwareResourceSpecification srs) {
			return objectMapper.convertValue(tmForumMapper.mapToSoftwareResourceSpecificationVO(srs),
					ResourceSpecificationVO.class);
		} else if (spec instanceof HostingPlatformRequirementSpecification hprs) {
			return objectMapper.convertValue(
					tmForumMapper.mapToHostingPlatformRequirementSpecificationVO(hprs),
					ResourceSpecificationVO.class);
		} else if (spec instanceof LogicalResourceSpecification lrs) {
			return objectMapper.convertValue(tmForumMapper.mapToLogicalResourceSpecificationVO(lrs),
					ResourceSpecificationVO.class);
		} else if (spec instanceof SoftwareSupportPackageSpecification ssps) {
			return objectMapper.convertValue(
					tmForumMapper.mapToSoftwareSupportPackageSpecificationVO(ssps),
					ResourceSpecificationVO.class);
		} else if (spec instanceof PhysicalResourceSpecification prs) {
			return objectMapper.convertValue(tmForumMapper.mapToPhysicalResourceSpecificationVO(prs),
					ResourceSpecificationVO.class);
		}
		return tmForumMapper.map(spec);
	}

	/**
	 * Get the generated VO class for a given specification domain class.
	 */
	private Class<?> getSpecVOClass(Class<? extends ResourceSpecification> domainClass) {
		if (domainClass == LogicalResourceSpecification.class) return LogicalResourceSpecificationVO.class;
		if (domainClass == SoftwareResourceSpecification.class) return SoftwareResourceSpecificationVO.class;
		if (domainClass == ApiSpecification.class) return APISpecificationVO.class;
		if (domainClass == SoftwareSpecification.class) return SoftwareSpecificationVO.class;
		if (domainClass == HostingPlatformRequirementSpecification.class) {
			return HostingPlatformRequirementSpecificationVO.class;
		}
		if (domainClass == PhysicalResourceSpecification.class) return PhysicalResourceSpecificationVO.class;
		if (domainClass == SoftwareSupportPackageSpecification.class) {
			return SoftwareSupportPackageSpecificationVO.class;
		}
		return ResourceSpecificationVO.class;
	}

	/**
	 * Map a sub-type specification VO to its domain entity.
	 */
	private ResourceSpecification mapSpecVOToDomain(Object vo,
			Class<? extends ResourceSpecification> domainClass) {
		if (domainClass == LogicalResourceSpecification.class) {
			return tmForumMapper.map((LogicalResourceSpecificationVO) vo);
		}
		if (domainClass == SoftwareResourceSpecification.class) {
			return tmForumMapper.map((SoftwareResourceSpecificationVO) vo);
		}
		if (domainClass == ApiSpecification.class) return tmForumMapper.map((APISpecificationVO) vo);
		if (domainClass == SoftwareSpecification.class) return tmForumMapper.map((SoftwareSpecificationVO) vo);
		if (domainClass == HostingPlatformRequirementSpecification.class) {
			return tmForumMapper.map((HostingPlatformRequirementSpecificationVO) vo);
		}
		if (domainClass == PhysicalResourceSpecification.class) {
			return tmForumMapper.map((PhysicalResourceSpecificationVO) vo);
		}
		if (domainClass == SoftwareSupportPackageSpecification.class) {
			return tmForumMapper.map((SoftwareSupportPackageSpecificationVO) vo);
		}
		throw new TmForumException("Unknown spec sub-type: " + domainClass.getSimpleName(),
				TmForumExceptionReason.INVALID_DATA);
	}

	/**
	 * Validate the specification by checking feature specifications and resource spec characteristics.
	 */
	private Mono<ResourceSpecification> validateSpec(ResourceSpecification resourceSpecification) {
		Mono<ResourceSpecification> validatingMono = Mono.just(resourceSpecification);

		if (resourceSpecification.getFeatureSpecification() != null
				&& !resourceSpecification.getFeatureSpecification().isEmpty()) {
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

		if (resourceSpecification.getResourceSpecCharacteristic() != null
				&& !resourceSpecification.getResourceSpecCharacteristic().isEmpty()) {
			List<Mono<ResourceSpecification>> rscCheckingMonos = resourceSpecification.getResourceSpecCharacteristic()
					.stream()
					.map(rsc -> validateResourceSpecChar(resourceSpecification, rsc))
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

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Mono<HttpResponse<Object>> deleteResourceSpecification(@NonNull String id) {
		return delete(id);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Mono<HttpResponse<List<ResourceSpecificationVO>>> listResourceSpecification(@Nullable String fields,
			@Nullable Integer offset, @Nullable Integer limit) {
		List<Mono<List<ResourceSpecificationVO>>> typeQueries = new ArrayList<>();

		for (Map.Entry<String, Class<? extends ResourceSpecification>> entry :
				ResourceTypeRegistry.SPEC_ENTITY_TYPES.entrySet()) {
			String entityType = entry.getKey();
			Class<? extends ResourceSpecification> entityClass = entry.getValue();
			Mono<List<ResourceSpecificationVO>> query = list(offset, limit, entityType, entityClass)
					.map(stream -> stream.map(this::mapSpecToVO).toList())
					.switchIfEmpty(Mono.just(List.of()));
			typeQueries.add(query);
		}

		return Mono.zip(typeQueries, results -> {
			List<ResourceSpecificationVO> combined = new ArrayList<>();
			for (Object result : results) {
				@SuppressWarnings("unchecked")
				List<ResourceSpecificationVO> typed = (List<ResourceSpecificationVO>) result;
				combined.addAll(typed);
			}
			return combined;
		}).map(HttpResponse::ok);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Mono<HttpResponse<ResourceSpecificationVO>> patchResourceSpecification(@NonNull String id,
			@NonNull ResourceSpecificationUpdateVO resourceSpecificationUpdateVO) {
		if (!IdHelper.isNgsiLdId(id)) {
			throw new TmForumException("Did not receive a valid id, such resource spec cannot exist.",
					TmForumExceptionReason.NOT_FOUND);
		}

		String entityType = ResourceTypeRegistry.extractTypeFromId(id);
		Class<? extends ResourceSpecification> entityClass = ResourceTypeRegistry.getSpecClass(entityType);

		if (entityClass != ResourceSpecification.class) {
			return patchSubTypeSpec(id, resourceSpecificationUpdateVO, entityClass);
		}

		ResourceSpecification resourceSpecification = tmForumMapper.map(resourceSpecificationUpdateVO, id);
		resourceSpecification.setLastUpdate(clock.instant());

		Mono<ResourceSpecification> checkingMono = getCheckingMono(resourceSpecification);
		checkingMono = Mono.zip(checkingMono, validateSpec(resourceSpecification),
				(p1, p2) -> resourceSpecification);

		return patch(id, resourceSpecification, checkingMono, ResourceSpecification.class)
				.map(tmForumMapper::map)
				.map(HttpResponse::ok);
	}

	@SuppressWarnings("unchecked")
	private Mono<HttpResponse<ResourceSpecificationVO>> patchSubTypeSpec(String id,
			ResourceSpecificationUpdateVO updateVO,
			Class<? extends ResourceSpecification> entityClass) {
		Map<String, Object> map = objectMapper.convertValue(updateVO, Map.class);
		map.put("id", id);
		map.put("href", id);

		Object subTypeVO = objectMapper.convertValue(map, getSpecVOClass(entityClass));
		ResourceSpecification spec = mapSpecVOToDomain(subTypeVO, entityClass);
		spec.setLastUpdate(clock.instant());

		URI idUri = URI.create(id);
		Mono<ResourceSpecification> validatedMono = Mono.zip(
				getCheckingMono(spec), validateSpec(spec), (p1, p2) -> spec);

		return repository.get(idUri, entityClass)
				.switchIfEmpty(Mono.error(new TmForumException("No such resource specification exists.",
						TmForumExceptionReason.NOT_FOUND)))
				.flatMap(existing -> validatedMono)
				.flatMap(checked -> repository.updateDomainEntity(id, spec)
						.then(repository.get(idUri, entityClass)))
				.map(this::mapSpecToVO)
				.map(HttpResponse::ok);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Mono<HttpResponse<ResourceSpecificationVO>> retrieveResourceSpecification(@NonNull String id,
			@Nullable String fields) {
		if (!IdHelper.isNgsiLdId(id)) {
			throw new TmForumException("Did not receive a valid id, such resource spec cannot exist.",
					TmForumExceptionReason.NOT_FOUND);
		}

		String entityType = ResourceTypeRegistry.extractTypeFromId(id);
		Class<? extends ResourceSpecification> entityClass = ResourceTypeRegistry.getSpecClass(entityType);

		return retrieve(id, entityClass)
				.switchIfEmpty(Mono.error(new TmForumException("No such resource specification exists.",
						TmForumExceptionReason.NOT_FOUND)))
				.map(this::mapSpecToVO)
				.map(HttpResponse::ok);
	}
}
