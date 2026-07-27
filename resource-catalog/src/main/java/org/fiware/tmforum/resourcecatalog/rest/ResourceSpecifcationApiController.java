package org.fiware.tmforum.resourcecatalog.rest;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.micronaut.core.annotation.NonNull;
import io.micronaut.core.annotation.Nullable;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.annotation.Controller;
import lombok.extern.slf4j.Slf4j;
import org.fiware.resourcecatalog.api.ResourceSpecificationApi;
import org.fiware.resourcecatalog.model.APISpecificationVO;
import org.fiware.resourcecatalog.model.HostingPlatformRequirementSpecificationVO;
import org.fiware.resourcecatalog.model.LogicalResourceSpecificationVO;
import org.fiware.resourcecatalog.model.PhysicalResourceSpecificationVO;
import org.fiware.resourcecatalog.model.ResourceSpecificationCreateVO;
import org.fiware.resourcecatalog.model.ResourceSpecificationUpdateVO;
import org.fiware.resourcecatalog.model.ResourceSpecificationVO;
import org.fiware.resourcecatalog.model.SoftwareResourceSpecificationVO;
import org.fiware.resourcecatalog.model.SoftwareSpecificationVO;
import org.fiware.resourcecatalog.model.SoftwareSupportPackageSpecificationVO;
import org.fiware.tmforum.common.exception.TmForumException;
import org.fiware.tmforum.common.exception.TmForumExceptionReason;
import org.fiware.tmforum.common.mapping.IdHelper;
import org.fiware.tmforum.common.notification.TMForumEventHandler;
import org.fiware.tmforum.common.querying.QueryParser;
import org.fiware.tmforum.common.repository.TmForumRepository;
import org.fiware.tmforum.common.rest.AbstractApiController;
import org.fiware.tmforum.common.validation.ReferenceValidationService;
import org.fiware.tmforum.common.validation.ReferencedEntity;
import org.fiware.tmforum.resource.ApiSpecification;
import org.fiware.tmforum.resource.FeatureSpecification;
import org.fiware.tmforum.resource.FeatureSpecificationCharacteristicRelationship;
import org.fiware.tmforum.resource.HostingPlatformRequirementSpecification;
import org.fiware.tmforum.resource.LogicalResourceSpecification;
import org.fiware.tmforum.resource.PhysicalResourceSpecification;
import org.fiware.tmforum.resource.ResourceSpecification;
import org.fiware.tmforum.resource.ResourceSpecificationCharacteristic;
import org.fiware.tmforum.resource.ResourceTypeRegistry;
import org.fiware.tmforum.resource.SoftwareResourceSpecification;
import org.fiware.tmforum.resource.SoftwareSpecification;
import org.fiware.tmforum.resource.SoftwareSupportPackageSpecification;
import org.fiware.tmforum.resourcecatalog.TMForumMapper;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * REST controller for the ResourceSpecification API within the Resource Catalog module (TMF634).
 * Provides CRUD operations for ResourceSpecification entities and all sub-types
 * (LogicalResourceSpecification, SoftwareResourceSpecification, APISpecification,
 * SoftwareSpecification, HostingPlatformRequirementSpecification,
 * PhysicalResourceSpecification, SoftwareSupportPackageSpecification).
 *
 * <p>Polymorphic dispatch based on the {@code @type} field in request payloads and the NGSI-LD
 * entity type embedded in entity IDs. Mirrors the pattern in the software-management module so
 * that {@code /resourceCatalog/v4/resourceSpecification} and
 * {@code /softwareCompute/v4/resourceSpecification} produce identical responses for the same
 * entity.</p>
 */
@Slf4j
@Controller("${api.resource-catalog.basepath:/}")
public class ResourceSpecifcationApiController extends AbstractApiController<ResourceSpecification>
		implements ResourceSpecificationApi {

	private final TMForumMapper tmForumMapper;
	private final Clock clock;
	private final ObjectMapper objectMapper;

	public ResourceSpecifcationApiController(QueryParser queryParser, ReferenceValidationService validationService,
			TmForumRepository resourceCatalogRepository, TMForumMapper tmForumMapper,
			Clock clock, TMForumEventHandler eventHandler,
			ObjectMapper objectMapper) {
		super(queryParser, validationService, resourceCatalogRepository, eventHandler);
		this.tmForumMapper = tmForumMapper;
		this.clock = clock;
		this.objectMapper = objectMapper;
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
			resourceSpecificationCreateVO.isBundle(false);
		}
		if (resourceSpecificationCreateVO.getLifecycleStatus() == null) {
			resourceSpecificationCreateVO.lifecycleStatus("created");
		}

		String atType = resourceSpecificationCreateVO.getAtType();
		if (atType != null && ResourceTypeRegistry.SPEC_TYPES.containsKey(atType)) {
			String entityType = ResourceTypeRegistry.getSpecEntityType(atType);
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

	@Override
	public Mono<HttpResponse<Object>> deleteResourceSpecification(@NonNull String id) {
		return delete(id);
	}

	@Override
	public Mono<HttpResponse<List<ResourceSpecificationVO>>> listResourceSpecification(@Nullable String fields,
			@Nullable Integer offset, @Nullable Integer limit) {
		// Polymorphic listing: query all registered NGSI-LD entity types in a single broker call so
		// offset/limit/count are correct against the combined result set, then dispatch each returned
		// entity to its own concrete domain class so sub-type fields round-trip with full fidelity.
		return listPolymorphic(offset, limit, ResourceTypeRegistry.ALL_SPEC_TYPES,
				ResourceSpecification.class, ResourceTypeRegistry::getSpecClass)
				.map(stream -> stream.map(this::mapSpecToVO).toList())
				.map(HttpResponse::ok);
	}

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
