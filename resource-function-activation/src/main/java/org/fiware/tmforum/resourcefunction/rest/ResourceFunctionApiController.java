package org.fiware.tmforum.resourcefunction.rest;

import io.micronaut.http.HttpResponse;
import io.micronaut.http.annotation.Controller;
import lombok.extern.slf4j.Slf4j;
import org.fiware.resourcefunction.api.ResourceFunctionApi;
import org.fiware.resourcefunction.model.ResourceFunctionCreateVO;
import org.fiware.resourcefunction.model.ResourceFunctionUpdateVO;
import org.fiware.resourcefunction.model.ResourceFunctionVO;
import org.fiware.tmforum.common.notification.EventHandler;
import org.fiware.tmforum.common.exception.TmForumException;
import org.fiware.tmforum.common.exception.TmForumExceptionReason;
import org.fiware.tmforum.common.mapping.IdHelper;
import org.fiware.tmforum.common.repository.TmForumRepository;
import org.fiware.tmforum.common.rest.AbstractApiController;
import org.fiware.tmforum.common.validation.ReferenceValidationService;
import org.fiware.tmforum.common.validation.ReferencedEntity;
import org.fiware.tmforum.resourcefunction.TMForumMapper;
import org.fiware.tmforum.resourcefunction.domain.ResourceFunction;
import org.fiware.tmforum.resourcefunction.domain.ResourceGraphRelationship;
import reactor.core.publisher.Mono;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Controller("${general.basepath:/}")
public class ResourceFunctionApiController extends AbstractApiController<ResourceFunction>
		implements ResourceFunctionApi {
	public final TMForumMapper tmForumMapper;

	public ResourceFunctionApiController(ReferenceValidationService validationService,
			TmForumRepository resourceCatalogRepository,
			TMForumMapper tmForumMapper, EventHandler eventHandler) {
		super(validationService, resourceCatalogRepository, eventHandler);
		this.tmForumMapper = tmForumMapper;
	}

	@Override
	public Mono<HttpResponse<ResourceFunctionVO>> createResourceFunction(
			ResourceFunctionCreateVO resourceFunctionCreateVO) {
		if (resourceFunctionCreateVO.getLifecycleState() == null) {
			throw new TmForumException("No lifecycleState was set.", TmForumExceptionReason.INVALID_DATA);
		}
		ResourceFunction resourceFunction = tmForumMapper.map(
				tmForumMapper.map(resourceFunctionCreateVO,
						IdHelper.toNgsiLd(UUID.randomUUID().toString(), ResourceFunction.TYPE_RESOURCE_FUNCTION)));

		return create(getCheckingMono(resourceFunction), ResourceFunction.class)
				.map(tmForumMapper::map)
				.map(HttpResponse::created);

	}

	private Mono<ResourceFunction> getCheckingMono(ResourceFunction resourceFunction) {
		List<List<? extends ReferencedEntity>> references = new ArrayList<>();
		references.add(resourceFunction.getConnectionPoint());
		references.add(resourceFunction.getRelatedParty());
		references.add(resourceFunction.getSchedule());

		Optional.ofNullable(resourceFunction.getPlace()).ifPresent(placeRef -> references.add(List.of(placeRef)));
		Optional.ofNullable(resourceFunction.getResourceSpecification())
				.ifPresent(resourceSpecificationRef -> references.add(List.of(resourceSpecificationRef)));

		Mono<ResourceFunction> checkingMono = getCheckingMono(resourceFunction, references);

		//Feature handling
		if (resourceFunction.getActivationFeature() != null && !resourceFunction.getActivationFeature().isEmpty()) {
			List<Mono<ResourceFunction>> constraintCheckingMonos = resourceFunction
					.getActivationFeature()
					.stream()
					.filter(af -> af.getConstraint() != null)
					.map(af -> getCheckingMono(resourceFunction, List.of(af.getConstraint())))
					.toList();
			Mono<ResourceFunction> constraintCheckingMono = Mono.zip(constraintCheckingMonos, (m1) -> resourceFunction);
			checkingMono = Mono.zip(constraintCheckingMono, checkingMono, (p1, p2) -> resourceFunction);
		}

		// resource graph handling
		if (resourceFunction.getConnectivity() != null && !resourceFunction.getConnectivity().isEmpty()) {

			List<Mono<ResourceFunction>> endpointRefCheckingMonos = resourceFunction
					.getConnectivity()
					.stream()
					.filter(rg -> rg.getConnection() != null)
					.flatMap(resourceGraph -> resourceGraph.getConnection().stream())
					.filter(connection -> connection.getEndpoint() != null)
					.map(connection -> getCheckingMono(resourceFunction, List.of(connection.getEndpoint())))
					.toList();
			if (!endpointRefCheckingMonos.isEmpty()) {
				Mono<ResourceFunction> endpointRefCheckingMono = Mono.zip(endpointRefCheckingMonos,
						(m1) -> resourceFunction);
				checkingMono = Mono.zip(endpointRefCheckingMono, checkingMono, (p1, p2) -> resourceFunction);
			}
			List<Mono<ResourceFunction>> connectionPointCheckingMonos = resourceFunction
					.getConnectivity()
					.stream()
					.filter(rg -> rg.getConnection() != null)
					.flatMap(resourceGraph -> resourceGraph.getConnection().stream())
					.filter(connection -> connection.getEndpoint() != null)
					.flatMap(connection -> connection.getEndpoint().stream())
					.map(endpointRef -> getCheckingMono(resourceFunction,
							List.of(List.of(endpointRef.getConnectionPoint()))))
					.toList();
			if (!connectionPointCheckingMonos.isEmpty()) {
				Mono<ResourceFunction> connectionPointCheckingMono = Mono.zip(connectionPointCheckingMonos,
						(m1) -> resourceFunction);
				checkingMono = Mono.zip(connectionPointCheckingMono, checkingMono, (p1, p2) -> resourceFunction);
			}
			List<Mono<ResourceFunction>> graphRelCheckingMonos = resourceFunction
					.getConnectivity()
					.stream()
					.filter(rg -> rg.getGraphRelationship() != null)
					.flatMap(rg -> rg.getGraphRelationship().stream())
					.map(ResourceGraphRelationship::getResourceGraph)
					.map(rgr -> getCheckingMono(resourceFunction, List.of(List.of(rgr))))
					.toList();
			if (!graphRelCheckingMonos.isEmpty()) {
				Mono<ResourceFunction> graphRelCheckingMono = Mono.zip(graphRelCheckingMonos, (m1) -> resourceFunction);
				checkingMono = Mono.zip(graphRelCheckingMono, checkingMono, (p1, p2) -> resourceFunction);
			}
		}
		if (resourceFunction.getResourceRelationship() != null && !resourceFunction.getResourceRelationship()
				.isEmpty()) {
			List<Mono<ResourceFunction>> resourceRelCheckingMonos = resourceFunction
					.getResourceRelationship()
					.stream()
					.filter(af -> af.getResource() != null)
					.map(af -> getCheckingMono(resourceFunction, List.of(List.of(af.getResource()))))
					.toList();
			Mono<ResourceFunction> resourceRelCheckingMono = Mono.zip(resourceRelCheckingMonos,
					(m1) -> resourceFunction);
			checkingMono = Mono.zip(resourceRelCheckingMono, checkingMono, (p1, p2) -> resourceFunction);
		}

		return checkingMono
				.onErrorMap(throwable -> new TmForumException(
						String.format("Was not able to create resource function %s", resourceFunction.getId()),
						throwable, TmForumExceptionReason.INVALID_RELATIONSHIP));
	}

	@Override
	public Mono<HttpResponse<Object>> deleteResourceFunction(String id) {
		return delete(id);
	}

	@Override
	public Mono<HttpResponse<List<ResourceFunctionVO>>> listResourceFunction(@Nullable String fields,
			@Nullable Integer offset, @Nullable Integer limit) {
		return list(offset, limit, ResourceFunction.TYPE_RESOURCE_FUNCTION, ResourceFunction.class)
				.map(resourceFunctionStream -> resourceFunctionStream.map(tmForumMapper::map).toList())
				.switchIfEmpty(Mono.just(List.of()))
				.map(HttpResponse::ok);
	}

	@Override
	public Mono<HttpResponse<ResourceFunctionVO>> patchResourceFunction(String id,
			ResourceFunctionUpdateVO resourceFunctionUpdateVO) {
		// non-ngsi-ld ids cannot exist.
		if (!IdHelper.isNgsiLdId(id)) {
			throw new TmForumException("Did not receive a valid id, such resource cannot exist.",
					TmForumExceptionReason.NOT_FOUND);
		}
		ResourceFunction updatedResourceFunction = tmForumMapper.map(resourceFunctionUpdateVO, id);

		return patch(id, updatedResourceFunction, getCheckingMono(updatedResourceFunction), ResourceFunction.class)
				.map(tmForumMapper::map)
				.map(HttpResponse::ok);
	}

	@Override
	public Mono<HttpResponse<ResourceFunctionVO>> retrieveResourceFunction(String id, @Nullable String fields) {
		return retrieve(id, ResourceFunction.class)
				.switchIfEmpty(Mono.error(new TmForumException("No such resources function exists.",
						TmForumExceptionReason.NOT_FOUND)))
				.map(tmForumMapper::map)
				.map(HttpResponse::ok);
	}
}
