package org.fiware.tmforum.resourcefunction.rest;

import io.micronaut.http.HttpResponse;
import io.micronaut.http.annotation.Controller;
import lombok.extern.slf4j.Slf4j;
import org.fiware.resourcefunction.api.ResourceFunctionApi;
import org.fiware.resourcefunction.model.ResourceFunctionCreateVO;
import org.fiware.resourcefunction.model.ResourceFunctionUpdateVO;
import org.fiware.resourcefunction.model.ResourceFunctionVO;
import org.fiware.tmforum.common.mapping.IdHelper;
import org.fiware.tmforum.common.validation.ReferenceValidationService;
import org.fiware.tmforum.common.validation.ReferencedEntity;
import org.fiware.tmforum.resourcefunction.TMForumMapper;
import org.fiware.tmforum.resourcefunction.domain.Characteristic;
import org.fiware.tmforum.resourcefunction.domain.Feature;
import org.fiware.tmforum.resourcefunction.domain.Resource;
import org.fiware.tmforum.resourcefunction.domain.ResourceFunction;
import org.fiware.tmforum.resourcefunction.domain.ResourceGraph;
import org.fiware.tmforum.resourcefunction.exception.ResourceFunctionException;
import org.fiware.tmforum.resourcefunction.exception.ResourceFunctionExceptionReason;
import org.fiware.tmforum.resourcefunction.repository.ResourceFunctionRepository;
import reactor.core.publisher.Mono;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Controller("${general.basepath:/}")
public class ResourceFunctionApiController extends AbstractApiController implements ResourceFunctionApi {

    public ResourceFunctionApiController(TMForumMapper tmForumMapper, ReferenceValidationService validationService, ResourceFunctionRepository resourceCatalogRepository) {
        super(tmForumMapper, validationService, resourceCatalogRepository);
    }

    @Override
    public Mono<HttpResponse<ResourceFunctionVO>> createResourceFunction(ResourceFunctionCreateVO resourceFunctionCreateVO) {
        if (resourceFunctionCreateVO.getLifecycleState() == null) {
            throw new ResourceFunctionException("No lifecycleState was set.", ResourceFunctionExceptionReason.INVALID_DATA);
        }
        ResourceFunction resourceFunction = tmForumMapper.map(
                tmForumMapper.map(resourceFunctionCreateVO, IdHelper.toNgsiLd(UUID.randomUUID().toString(), ResourceFunction.TYPE_RESOURCE_FUNCTION)));

        Mono<ResourceFunction> checkingMono = getCheckingMono(resourceFunction);

        Mono<ResourceFunction> activationFeatureHandlingMono = relatedEntityHandlingMono(
                resourceFunction,
                checkingMono,
                resourceFunction.getActivationFeature(),
                resourceFunction::setActivationFeature,
                Feature.class);
        Mono<ResourceFunction> autoModificationHandlingMono = relatedEntityHandlingMono(
                resourceFunction,
                activationFeatureHandlingMono,
                resourceFunction.getAutoModification(),
                resourceFunction::setAutoModification,
                Characteristic.class);
        Mono<ResourceFunction> characteristicHandlingMono = relatedEntityHandlingMono(
                resourceFunction,
                autoModificationHandlingMono,
                resourceFunction.getResourceCharacteristic(),
                resourceFunction::setResourceCharacteristic,
                Characteristic.class);
        Mono<ResourceFunction> connectivityHandlingMono = relatedEntityHandlingMono(
                resourceFunction,
                characteristicHandlingMono,
                resourceFunction.getConnectivity(),
                resourceFunction::setConnectivity,
                ResourceGraph.class);

        Mono<ResourceFunction> resourceRelationshipHandlingMono = handleResourceRelationship(resourceFunction);

        return create(Mono.zip(connectivityHandlingMono, resourceRelationshipHandlingMono, (p1, p2) -> resourceFunction), ResourceFunction.class)
                .map(tmForumMapper::map)
                .map(HttpResponse::created);

    }

    private Mono<ResourceFunction> handleResourceRelationship(ResourceFunction resourceFunction) {
        if (resourceFunction.getResourceRelationship() == null || resourceFunction.getResourceRelationship().isEmpty()) {
            return Mono.just(resourceFunction);
        }
        List<Mono<Resource>> relationshipHandlingMonos = resourceFunction.getResourceRelationship()
                .stream()
                .map(resourceRelationship -> {
                    if (resourceRelationship.getId() == null) {
                        throw new ResourceFunctionException("A resource relationship without an ID cannot exist.", ResourceFunctionExceptionReason.INVALID_DATA);
                    }

                    return validationService.checkReferenceExists(List.of(resourceRelationship))
                            .flatMap(res -> {
                                if (res) {
                                    return resourceCatalogRepository.get(resourceRelationship.getId(), Resource.class);
                                } else {
                                    return resourceCatalogRepository
                                            .createDomainEntity(resourceRelationship.getResource())
                                            .then(Mono.just(resourceRelationship.getResource()));
                                }
                            });
                })
                .toList();
        return Mono.zip(relationshipHandlingMonos, (m1) -> resourceFunction);

    }

    private Mono<ResourceFunction> getCheckingMono(ResourceFunction resourceFunction) {
        List<List<? extends ReferencedEntity>> references = new ArrayList<>();
        references.add(resourceFunction.getConnectionPoint());
        references.add(resourceFunction.getRelatedParty());
        references.add(resourceFunction.getSchedule());

        Optional.ofNullable(resourceFunction.getPlace()).ifPresent(placeRef -> references.add(List.of(placeRef)));
        Optional.ofNullable(resourceFunction.getResourceSpecification()).ifPresent(resourceSpecificationRef -> references.add(List.of(resourceSpecificationRef)));

        Mono<ResourceFunction> checkingMono = getCheckingMono(resourceFunction, references);

        //Feature handling
        if (resourceFunction.getActivationFeature() != null && !resourceFunction.getActivationFeature().isEmpty()) {
            List<Mono<Feature>> constraintCheckingMonos = resourceFunction
                    .getActivationFeature()
                    .stream()
                    .filter(af -> af.getConstraint() != null)
                    .map(af -> getCheckingMono(af, List.of(af.getConstraint())))
                    .toList();
            Mono<ResourceFunction> constraintCheckingMono = Mono.zip(constraintCheckingMonos, (m1) -> resourceFunction);
            checkingMono = Mono.zip(constraintCheckingMono, checkingMono, (p1, p2) -> resourceFunction);
            List<Mono<Feature>> relCheckingMonos = resourceFunction
                    .getActivationFeature()
                    .stream()
                    .filter(af -> af.getFeatureRelationship() != null)
                    .map(af -> getCheckingMono(af, List.of(af.getFeatureRelationship())))
                    .toList();
            Mono<ResourceFunction> relCheckingMono = Mono.zip(relCheckingMonos, (m1) -> resourceFunction);
            checkingMono = Mono.zip(relCheckingMono, checkingMono, (p1, p2) -> resourceFunction);
        }

        // auto modification handling
        if (resourceFunction.getAutoModification() != null && !resourceFunction.getAutoModification().isEmpty()) {
            List<Mono<Characteristic>> autoModCheckingMonos = resourceFunction
                    .getAutoModification()
                    .stream()
                    .filter(rc -> rc.getCharacteristicRelationship() != null)
                    .map(rc -> getCheckingMono(rc, List.of(rc.getCharacteristicRelationship())))
                    .toList();
            Mono<ResourceFunction> autoModCheckingMono = Mono.zip(autoModCheckingMonos, (m1) -> resourceFunction);
            checkingMono = Mono.zip(autoModCheckingMono, checkingMono, (p1, p2) -> resourceFunction);
        }

        // characteristic handling
        if (resourceFunction.getResourceCharacteristic() != null && !resourceFunction.getResourceCharacteristic().isEmpty()) {
            List<Mono<Characteristic>> characteristicCheckingMonos = resourceFunction
                    .getResourceCharacteristic()
                    .stream()
                    .filter(rc -> rc.getCharacteristicRelationship() != null)
                    .map(rc -> getCheckingMono(rc, List.of(rc.getCharacteristicRelationship())))
                    .toList();
            Mono<ResourceFunction> characteristicCheckingMono = Mono.zip(characteristicCheckingMonos, (m1) -> resourceFunction);
            checkingMono = Mono.zip(characteristicCheckingMono, checkingMono, (p1, p2) -> resourceFunction);
        }

        // resource graph handling
        if (resourceFunction.getConnectivity() != null && !resourceFunction.getConnectivity().isEmpty()) {

            List<Mono<ResourceGraph>> connectionCheckingMonos = resourceFunction
                    .getConnectivity()
                    .stream()
                    .filter(rg -> rg.getConnection() != null)
                    .map(rg -> getCheckingMono(rg, List.of(rg.getConnection())))
                    .toList();
            Mono<ResourceFunction> connectionCheckingMono = Mono.zip(connectionCheckingMonos, (m1) -> resourceFunction);
            checkingMono = Mono.zip(connectionCheckingMono, checkingMono, (p1, p2) -> resourceFunction);

            List<Mono<ResourceGraph>> graphRelCheckingMonos = resourceFunction
                    .getConnectivity()
                    .stream()
                    .filter(rg -> rg.getGraphRelationship() != null)
                    .map(rg -> getCheckingMono(rg, List.of(rg.getGraphRelationship())))
                    .toList();
            Mono<ResourceFunction> graphRelCheckingMono = Mono.zip(graphRelCheckingMonos, (m1) -> resourceFunction);
            checkingMono = Mono.zip(graphRelCheckingMono, checkingMono, (p1, p2) -> resourceFunction);
        }
        return checkingMono
                .onErrorMap(throwable -> new ResourceFunctionException(String.format("Was not able to create resource function %s", resourceFunction.getId()), throwable, ResourceFunctionExceptionReason.INVALID_RELATIONSHIP));
    }

    @Override
    public Mono<HttpResponse<Object>> deleteResourceFunction(String id) {
        return delete(id);
    }

    @Override
    public Mono<HttpResponse<List<ResourceFunctionVO>>> listResourceFunction(@Nullable String fields, @Nullable Integer offset, @Nullable Integer limit) {
        return list(offset, limit, ResourceFunction.TYPE_RESOURCE_FUNCTION, ResourceFunction.class)
                .map(resourceFunctionStream -> resourceFunctionStream.map(tmForumMapper::map).toList())
                .map(HttpResponse::ok);
    }

    @Override
    public Mono<HttpResponse<ResourceFunctionVO>> patchResourceFunction(String id, ResourceFunctionUpdateVO resourceFunctionUpdateVO) {
        // non-ngsi-ld ids cannot exist.
        if (!IdHelper.isNgsiLdId(id)) {
            throw new ResourceFunctionException("Did not receive a valid id, such resource cannot exist.", ResourceFunctionExceptionReason.NOT_FOUND);
        }
        ResourceFunction updatedResourceFunction = tmForumMapper.map(resourceFunctionUpdateVO, id);

        Mono<ResourceFunction> checkingMono = getCheckingMono(updatedResourceFunction);


        Mono<ResourceFunction> activationFeatureHandlingMono = relatedEntityHandlingMono(
                updatedResourceFunction,
                checkingMono,
                updatedResourceFunction.getActivationFeature(),
                updatedResourceFunction::setActivationFeature,
                Feature.class);
        Mono<ResourceFunction> autoModificationHandlingMono = relatedEntityHandlingMono(
                updatedResourceFunction,
                checkingMono,
                updatedResourceFunction.getAutoModification(),
                updatedResourceFunction::setAutoModification,
                Characteristic.class);
        Mono<ResourceFunction> characteristicHandlingMono = relatedEntityHandlingMono(
                updatedResourceFunction,
                checkingMono,
                updatedResourceFunction.getResourceCharacteristic(),
                updatedResourceFunction::setResourceCharacteristic,
                Characteristic.class);
        Mono<ResourceFunction> connectivityHandlingMono = relatedEntityHandlingMono(
                updatedResourceFunction,
                checkingMono,
                updatedResourceFunction.getConnectivity(),
                updatedResourceFunction::setConnectivity,
                ResourceGraph.class);

        Mono<ResourceFunction> relatedEntityHandlingMono = Mono.zip(List.of(activationFeatureHandlingMono, autoModificationHandlingMono, characteristicHandlingMono, connectivityHandlingMono), m1 -> updatedResourceFunction);

        Mono<ResourceFunction> resourceRelationshipHandlingMono = handleResourceRelationship(updatedResourceFunction);

        return patch(id, updatedResourceFunction, Mono.zip(relatedEntityHandlingMono, resourceRelationshipHandlingMono, (p1, p2) -> updatedResourceFunction), ResourceFunction.class)
                .map(tmForumMapper::map)
                .map(HttpResponse::ok);
    }

    @Override
    public Mono<HttpResponse<ResourceFunctionVO>> retrieveResourceFunction(String id, @Nullable String fields) {
        return retrieve(id, ResourceFunction.class)
                .switchIfEmpty(Mono.error(new ResourceFunctionException("No such resources function exists.", ResourceFunctionExceptionReason.NOT_FOUND)))
                .map(tmForumMapper::map)
                .map(HttpResponse::ok);
    }
}
