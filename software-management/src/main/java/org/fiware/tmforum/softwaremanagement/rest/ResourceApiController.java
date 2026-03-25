package org.fiware.tmforum.softwaremanagement.rest;

import io.micronaut.core.annotation.NonNull;
import io.micronaut.core.annotation.Nullable;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.annotation.Controller;
import lombok.extern.slf4j.Slf4j;
import org.fiware.softwaremanagement.api.ResourceApi;
import org.fiware.softwaremanagement.model.ResourceCreateVO;
import org.fiware.softwaremanagement.model.ResourceUpdateVO;
import org.fiware.softwaremanagement.model.ResourceVO;
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

/**
 * REST controller for the Resource API within the Software Management module (TMF730).
 * Provides CRUD operations for Resource entities.
 */
@Slf4j
@Controller("${api.software-management.basepath:/}")
public class ResourceApiController extends AbstractApiController<Resource> implements ResourceApi {

	private final TMForumMapper tmForumMapper;

	/**
	 * Create a new ResourceApiController.
	 *
	 * @param queryParser             the query parser for filtering
	 * @param validationService       the reference validation service
	 * @param repository              the TM Forum repository
	 * @param tmForumMapper           the mapper for entity/VO conversions
	 * @param eventHandler            the event handler for notifications
	 */
	public ResourceApiController(QueryParser queryParser, ReferenceValidationService validationService,
			TmForumRepository repository,
			TMForumMapper tmForumMapper, TMForumEventHandler eventHandler) {
		super(queryParser, validationService, repository, eventHandler);
		this.tmForumMapper = tmForumMapper;
	}

	/**
	 * {@inheritDoc}
	 */
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
		return list(offset, limit, Resource.TYPE_RESOURCE, Resource.class)
				.map(resourceStream -> resourceStream
						.map(tmForumMapper::map)
						.toList())
				.switchIfEmpty(Mono.just(List.of()))
				.map(HttpResponse::ok);
	}

	/**
	 * {@inheritDoc}
	 */
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

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Mono<HttpResponse<ResourceVO>> retrieveResource(@NonNull String id, @Nullable String fields) {
		return retrieve(id, Resource.class)
				.switchIfEmpty(Mono.error(new TmForumException("No such resource exists.",
						TmForumExceptionReason.NOT_FOUND)))
				.map(tmForumMapper::map)
				.map(HttpResponse::ok);
	}
}
