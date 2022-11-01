package org.fiware.tmforum.servicecatalog.rest;

import io.micronaut.core.annotation.NonNull;
import io.micronaut.core.annotation.Nullable;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.annotation.Controller;
import lombok.extern.slf4j.Slf4j;
import org.fiware.servicecatalog.api.ServiceSpecificationApi;
import org.fiware.servicecatalog.model.ServiceSpecificationCreateVO;
import org.fiware.servicecatalog.model.ServiceSpecificationUpdateVO;
import org.fiware.servicecatalog.model.ServiceSpecificationVO;
import org.fiware.tmforum.common.domain.ConstraintRef;
import org.fiware.tmforum.common.exception.TmForumException;
import org.fiware.tmforum.common.exception.TmForumExceptionReason;
import org.fiware.tmforum.common.mapping.IdHelper;
import org.fiware.tmforum.common.repository.TmForumRepository;
import org.fiware.tmforum.common.rest.AbstractApiController;
import org.fiware.tmforum.common.validation.ReferenceValidationService;
import org.fiware.tmforum.common.validation.ReferencedEntity;
import org.fiware.tmforum.resource.FeatureSpecificationCharacteristicRelationship;
import org.fiware.tmforum.resource.ResourceSpecificationRef;
import org.fiware.tmforum.servicecatalog.TMForumMapper;
import org.fiware.tmforum.servicecatalog.domain.AssociationSpecificationRef;
import org.fiware.tmforum.servicecatalog.domain.CharacteristicSpecification;
import org.fiware.tmforum.servicecatalog.domain.CharacteristicSpecificationRelationship;
import org.fiware.tmforum.servicecatalog.domain.EntitySpecificationRelationship;
import org.fiware.tmforum.servicecatalog.domain.FeatureSpecification;
import org.fiware.tmforum.servicecatalog.domain.FeatureSpecificationCharacteristic;
import org.fiware.tmforum.servicecatalog.domain.FeatureSpecificationRelationship;
import org.fiware.tmforum.servicecatalog.domain.ServiceSpecification;
import reactor.core.publisher.Mono;

import java.time.Clock;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Slf4j
@Controller("${general.basepath:/}")
public class ServiceSpecificationApiController extends AbstractApiController<ServiceSpecification>
		implements ServiceSpecificationApi {

	private final TMForumMapper tmForumMapper;
	private final Clock clock;

	public ServiceSpecificationApiController(ReferenceValidationService validationService,
			TmForumRepository serviceCatalogRepository, TMForumMapper tmForumMapper,
			Clock clock) {
		super(validationService, serviceCatalogRepository);
		this.tmForumMapper = tmForumMapper;
		this.clock = clock;
	}

	@Override
	public Mono<HttpResponse<ServiceSpecificationVO>> createServiceSpecification(
			@NonNull ServiceSpecificationCreateVO serviceSpecificationCreateVO) {
		if (serviceSpecificationCreateVO.getName() == null) {
			throw new TmForumException(
					String.format("The specification create does not contain all mandatory values: %s.",
							serviceSpecificationCreateVO), TmForumExceptionReason.INVALID_DATA);
		}
		if (serviceSpecificationCreateVO.getIsBundle() == null) {
			// set default required by the conformance
			serviceSpecificationCreateVO.isBundle(false);
		}
		if (serviceSpecificationCreateVO.getLifecycleStatus() == null) {
			// set default required by the conformance
			serviceSpecificationCreateVO.lifecycleStatus("created");
		}

		ServiceSpecification serviceSpecification = tmForumMapper.map(
				tmForumMapper.map(serviceSpecificationCreateVO, IdHelper.toNgsiLd(UUID.randomUUID().toString(),
						ServiceSpecification.TYPE_SERVICE_SPECIFICATION)));
		serviceSpecification.setLastUpdate(clock.instant());

		return create(validateSpec(serviceSpecification), ServiceSpecification.class)
				.map(tmForumMapper::map)
				.map(HttpResponse::created);
	}

	private Mono<ServiceSpecification> validateSpec(ServiceSpecification serviceSpecification) {
		Mono<ServiceSpecification> checkingMono = getCheckingMono(serviceSpecification);

		// no external calls -> no mono
		validateCharacteristicSpecifications(serviceSpecification);

		checkingMono = validateFeatureSpecification(serviceSpecification, checkingMono);
		checkingMono = validateEntitySpecRelationship(serviceSpecification, checkingMono);

		return checkingMono
				.switchIfEmpty(Mono.just(serviceSpecification))
				.onErrorMap(throwable ->
						new TmForumException(
								String.format("Was not able to validate service service specification %s",
										serviceSpecification.getId()),
								throwable,
								TmForumExceptionReason.INVALID_RELATIONSHIP));
	}

	private void validateCharacteristicSpecifications(ServiceSpecification serviceSpecification) {
		if (serviceSpecification.getSpecCharacteristic() == null || serviceSpecification.getSpecCharacteristic()
				.isEmpty()) {
			// early exit
			return;
		}
		List<String> charIds = serviceSpecification.getSpecCharacteristic().stream()
				.map(CharacteristicSpecification::getId).toList();
		if (charIds.size() != new HashSet<>(charIds).size()) {
			throw new TmForumException("Duplicate characteristic ids are not allowed.",
					TmForumExceptionReason.INVALID_DATA);
		}
		serviceSpecification.getSpecCharacteristic()
				.stream()
				.map(CharacteristicSpecification::getCharSpecRelationship)
				.filter(Objects::nonNull)
				.flatMap(List::stream)
				.map(CharacteristicSpecificationRelationship::getCharacteristicSpecificationId)
				.filter(rId -> !charIds.contains(rId))
				.findFirst()
				.ifPresent(rId -> {
					throw new TmForumException(
							String.format("Referenced characteristic %s does not exist.", rId),
							TmForumExceptionReason.INVALID_DATA);
				});
	}

	private Mono<ServiceSpecification> validateEntitySpecRelationship(ServiceSpecification serviceSpecification,
			Mono<ServiceSpecification> checkingMono) {
		if (serviceSpecification.getEntitySpecRelationship() == null || serviceSpecification.getEntitySpecRelationship()
				.isEmpty()) {
			// early exit
			return checkingMono;
		}
		List<AssociationSpecificationRef> associationSpecificationRefs = serviceSpecification.getEntitySpecRelationship()
				.stream()
				.map(EntitySpecificationRelationship::getAssociationSpec)
				.toList();
		if (!associationSpecificationRefs.isEmpty()) {
			checkingMono = Mono.zip(checkingMono,
					getCheckingMono(serviceSpecification, List.of(associationSpecificationRefs)),
					(p1, p2) -> serviceSpecification);
		}
		return checkingMono;
	}

	private Mono<ServiceSpecification> validateFeatureSpecification(ServiceSpecification serviceSpecification,
			Mono<ServiceSpecification> checkingMono) {
		if (serviceSpecification.getFeatureSpecification() == null || serviceSpecification.getFeatureSpecification()
				.isEmpty()) {
			// early exit
			return checkingMono;
		}
		// feature spec constraints
		checkingMono = validateFeatureSpecConstraints(serviceSpecification, checkingMono);

		// feature specs ids
		List<String> featureSpecIds = serviceSpecification
				.getFeatureSpecification()
				.stream()
				.map(FeatureSpecification::getId)
				.toList();
		if (featureSpecIds.size() != new HashSet<>(featureSpecIds).size()) {
			throw new TmForumException("Duplicate featureSpec ids are not allowed.",
					TmForumExceptionReason.INVALID_DATA);
		}

		validateFeatureSpecRelationships(serviceSpecification, featureSpecIds);

		// feature spec chars
		checkingMono = validateFeatureSpecCharacteristics(serviceSpecification, checkingMono, featureSpecIds);

		return checkingMono;
	}

	private Mono<ServiceSpecification> validateFeatureSpecCharacteristics(ServiceSpecification serviceSpecification,
			Mono<ServiceSpecification> checkingMono, List<String> featureSpecIds) {
		List<FeatureSpecificationCharacteristic> featureSpecCharList = serviceSpecification
				.getFeatureSpecification()
				.stream()
				.map(FeatureSpecification::getFeatureSpecCharacteristic)
				.filter(Objects::nonNull)
				.flatMap(List::stream)
				.toList();

		List<String> featureSpecCharIds = featureSpecCharList.stream().map(FeatureSpecificationCharacteristic::getId)
				.toList();
		if (featureSpecCharIds.size() != new HashSet<>(featureSpecCharIds).size()) {
			throw new TmForumException("Duplicate featureSpecChar ids are not allowed.",
					TmForumExceptionReason.INVALID_DATA);
		}
		// validate feature and char refs
		List<FeatureSpecificationCharacteristicRelationship> featureSpecificationCharacteristicRelationships = featureSpecCharList
				.stream()
				.map(FeatureSpecificationCharacteristic::getFeatureSpecCharRelationship)
				.flatMap(List::stream)
				.toList();

		featureSpecificationCharacteristicRelationships
				.forEach(fscr -> validateFeatureSpecRelRefs(featureSpecIds, featureSpecCharIds, fscr));

		List<ResourceSpecificationRef> resourceSpecificationRefs = featureSpecificationCharacteristicRelationships
				.stream()
				.map(FeatureSpecificationCharacteristicRelationship::getResourceSpecificationId)
				.toList();
		if (!resourceSpecificationRefs.isEmpty()) {
			checkingMono = Mono.zip(checkingMono,
					getCheckingMono(serviceSpecification, List.of(resourceSpecificationRefs)),
					(p1, p2) -> serviceSpecification);
		}
		return checkingMono;
	}

	private void validateFeatureSpecRelationships(ServiceSpecification serviceSpecification,
			List<String> featureSpecIds) {
		List<FeatureSpecificationRelationship> featureSpecificationRelationships = serviceSpecification
				.getFeatureSpecification()
				.stream()
				.filter(fs -> fs.getFeatureSpecRelationship() != null)
				.flatMap(fs -> fs.getFeatureSpecRelationship().stream())
				.toList();

		featureSpecificationRelationships
				.stream()
				.map(FeatureSpecificationRelationship::getFeatureId)
				.filter(referencedId -> !featureSpecIds.contains(referencedId))
				.findFirst()
				.ifPresent(rId -> {
					throw new TmForumException(String.format("Referenced feature %s does not exist.", rId),
							TmForumExceptionReason.INVALID_DATA);
				});
	}

	private Mono<ServiceSpecification> validateFeatureSpecConstraints(ServiceSpecification serviceSpecification,
			Mono<ServiceSpecification> checkingMono) {
		List<ConstraintRef> constraintRefs = serviceSpecification
				.getFeatureSpecification()
				.stream()
				.map(FeatureSpecification::getConstraint)
				.filter(Objects::nonNull)
				.flatMap(List::stream)
				.toList();
		return Mono.zip(checkingMono, getCheckingMono(serviceSpecification, List.of(constraintRefs)),
				(p1, p2) -> serviceSpecification);
	}

	private void validateFeatureSpecRelRefs(List<String> featureSpecIds, List<String> featureSpecCharIds,
			FeatureSpecificationCharacteristicRelationship fscr) {
		if (!featureSpecIds.contains(fscr.getFeatureId())) {
			throw new TmForumException(
					String.format("Feature %s referenced by feature spec char does not exist.", fscr.getFeatureId()),
					TmForumExceptionReason.INVALID_DATA);
		}
		if (!featureSpecCharIds.contains(fscr.getCharacteristicId())) {
			throw new TmForumException(
					String.format("Characteristic %s referenced by feature spec char does not exist.",
							fscr.getFeatureId()),
					TmForumExceptionReason.INVALID_DATA);
		}
	}

	private Mono<ServiceSpecification> getCheckingMono(ServiceSpecification serviceSpecification) {
		List<List<? extends ReferencedEntity>> references = new ArrayList<>();
		references.add(serviceSpecification.getResourceSpecification());
		references.add(serviceSpecification.getRelatedParty());
		references.add(serviceSpecification.getServiceLevelSpecification());
		references.add(serviceSpecification.getEntitySpecRelationship());
		references.add(serviceSpecification.getServiceSpecRelationship());

		return getCheckingMono(serviceSpecification, references)
				.onErrorMap(throwable ->
						new TmForumException(
								String.format("Was not able to validate service specification %s",
										serviceSpecification.getId()),
								throwable,
								TmForumExceptionReason.INVALID_RELATIONSHIP));
	}

	@Override
	public Mono<HttpResponse<Object>> deleteServiceSpecification(@NonNull String id) {
		return delete(id);
	}

	@Override
	public Mono<HttpResponse<List<ServiceSpecificationVO>>> listServiceSpecification(@Nullable String fields,
			@Nullable Integer offset, @Nullable Integer limit) {
		return list(offset, limit, ServiceSpecification.TYPE_SERVICE_SPECIFICATION, ServiceSpecification.class)
				.map(serviceSpecificationStream -> serviceSpecificationStream
						.map(tmForumMapper::map)
						.toList())
				.switchIfEmpty(Mono.just(List.of()))
				.map(HttpResponse::ok);
	}

	@Override
	public Mono<HttpResponse<ServiceSpecificationVO>> patchServiceSpecification(@NonNull String id,
			@NonNull ServiceSpecificationUpdateVO serviceSpecificationUpdateVO) {
		// non-ngsi-ld ids cannot exist.
		if (!IdHelper.isNgsiLdId(id)) {
			throw new TmForumException("Did not receive a valid id, such service specification cannot exist.",
					TmForumExceptionReason.NOT_FOUND);
		}
		ServiceSpecification serviceSpecification = tmForumMapper.map(serviceSpecificationUpdateVO, id);
		serviceSpecification.setLastUpdate(clock.instant());

		return patch(id, serviceSpecification, validateSpec(serviceSpecification), ServiceSpecification.class)
				.map(tmForumMapper::map)
				.map(HttpResponse::ok);
	}

	@Override
	public Mono<HttpResponse<ServiceSpecificationVO>> retrieveServiceSpecification(@NonNull String id,
			@Nullable String fields) {
		return retrieve(id, ServiceSpecification.class)
				.switchIfEmpty(Mono.error(new TmForumException("No such service specification exists.",
						TmForumExceptionReason.NOT_FOUND)))
				.map(tmForumMapper::map)
				.map(HttpResponse::ok);
	}
}
