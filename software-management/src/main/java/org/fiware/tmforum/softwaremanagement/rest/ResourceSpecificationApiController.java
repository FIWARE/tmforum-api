package org.fiware.tmforum.softwaremanagement.rest;

import io.micronaut.core.annotation.NonNull;
import io.micronaut.core.annotation.Nullable;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.annotation.Controller;
import lombok.extern.slf4j.Slf4j;
import org.fiware.softwaremanagement.api.ResourceSpecificationApi;
import org.fiware.softwaremanagement.model.ResourceSpecificationCreateVO;
import org.fiware.softwaremanagement.model.ResourceSpecificationUpdateVO;
import org.fiware.softwaremanagement.model.ResourceSpecificationVO;
import org.fiware.tmforum.common.exception.TmForumException;
import org.fiware.tmforum.common.exception.TmForumExceptionReason;
import org.fiware.tmforum.common.mapping.IdHelper;
import org.fiware.tmforum.common.notification.TMForumEventHandler;
import org.fiware.tmforum.common.querying.QueryParser;
import org.fiware.tmforum.common.repository.TmForumRepository;
import org.fiware.tmforum.common.rest.AbstractApiController;
import org.fiware.tmforum.common.validation.ReferenceValidationService;
import org.fiware.tmforum.common.validation.ReferencedEntity;
import org.fiware.tmforum.resource.FeatureSpecification;
import org.fiware.tmforum.resource.FeatureSpecificationCharacteristicRelationship;
import org.fiware.tmforum.resource.ResourceSpecification;
import org.fiware.tmforum.resource.ResourceSpecificationCharacteristic;
import org.fiware.tmforum.softwaremanagement.TMForumMapper;
import reactor.core.publisher.Mono;

import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * REST controller for the ResourceSpecification API within the Software Management module (TMF730).
 * Provides CRUD operations for ResourceSpecification entities.
 */
@Slf4j
@Controller("${api.software-management.basepath:/}")
public class ResourceSpecificationApiController extends AbstractApiController<ResourceSpecification>
		implements ResourceSpecificationApi {

	private final TMForumMapper tmForumMapper;
	private final Clock clock;

	/**
	 * Create a new ResourceSpecificationApiController.
	 *
	 * @param queryParser             the query parser for filtering
	 * @param validationService       the reference validation service
	 * @param repository              the TM Forum repository
	 * @param tmForumMapper           the mapper for entity/VO conversions
	 * @param clock                   the clock for lastUpdate timestamps
	 * @param eventHandler            the event handler for notifications
	 */
	public ResourceSpecificationApiController(QueryParser queryParser, ReferenceValidationService validationService,
			TmForumRepository repository, TMForumMapper tmForumMapper,
			Clock clock, TMForumEventHandler eventHandler) {
		super(queryParser, validationService, repository, eventHandler);
		this.tmForumMapper = tmForumMapper;
		this.clock = clock;
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

	/**
	 * Validate the specification by checking feature specifications and resource spec characteristics.
	 *
	 * @param resourceSpecification the resource specification to validate
	 * @return a Mono that completes with the specification if validation passes
	 */
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

		if (resourceSpecification.getResourceSpecCharacteristic() != null
				&& !resourceSpecification.getResourceSpecCharacteristic().isEmpty()) {

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

	/**
	 * Validate a resource specification characteristic and its relationships.
	 *
	 * @param resourceSpecification              the parent resource specification
	 * @param resourceSpecificationCharacteristic the characteristic to validate
	 * @return a Mono that completes with the specification if validation passes
	 */
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

	/**
	 * Validate a feature specification and its constraints, relationships, and characteristics.
	 *
	 * @param resourceSpecification the parent resource specification
	 * @param featureSpecification  the feature specification to validate
	 * @return a Mono that completes with the specification if validation passes
	 */
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

	/**
	 * Add references from a feature specification characteristic relationship to the reference list.
	 *
	 * @param fscr       the feature specification characteristic relationship
	 * @param references the accumulated list of references to validate
	 */
	private void addReferencesForFSCR(FeatureSpecificationCharacteristicRelationship fscr,
			List<List<? extends ReferencedEntity>> references) {
		Optional.ofNullable(fscr.getResourceSpecificationId())
				.map(List::of)
				.ifPresent(references::add);
	}

	/**
	 * Build a checking Mono that validates all external references of a resource specification.
	 *
	 * @param resourceSpecification the resource specification to validate
	 * @return a Mono that completes with the specification if validation passes
	 */
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
		return list(offset, limit, ResourceSpecification.TYPE_RESOURCE_SPECIFICATION, ResourceSpecification.class)
				.map(resourceFunctionStream -> resourceFunctionStream
						.map(tmForumMapper::map)
						.toList())
				.switchIfEmpty(Mono.just(List.of()))
				.map(HttpResponse::ok);
	}

	/**
	 * {@inheritDoc}
	 */
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

	/**
	 * {@inheritDoc}
	 */
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
