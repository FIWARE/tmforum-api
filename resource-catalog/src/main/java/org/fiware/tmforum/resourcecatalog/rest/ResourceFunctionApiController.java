package org.fiware.tmforum.resourcecatalog.rest;

import io.micronaut.http.HttpResponse;
import io.micronaut.http.annotation.Controller;
import lombok.extern.slf4j.Slf4j;
import org.fiware.resourcecatalog.api.ResourceFunctionApi;
import org.fiware.resourcecatalog.model.ResourceFunctionCreateVO;
import org.fiware.resourcecatalog.model.ResourceFunctionUpdateVO;
import org.fiware.resourcecatalog.model.ResourceFunctionVO;
import org.fiware.tmforum.common.mapping.IdHelper;
import org.fiware.tmforum.common.validation.ReferenceValidationService;
import org.fiware.tmforum.common.validation.ReferencedEntity;
import org.fiware.tmforum.resourcecatalog.TMForumMapper;
import org.fiware.tmforum.resourcecatalog.domain.Characteristic;
import org.fiware.tmforum.resourcecatalog.domain.Feature;
import org.fiware.tmforum.resourcecatalog.domain.Resource;
import org.fiware.tmforum.resourcecatalog.domain.ResourceFunction;
import org.fiware.tmforum.resourcecatalog.domain.ResourceGraph;
import org.fiware.tmforum.resourcecatalog.exception.ResourceCatalogException;
import org.fiware.tmforum.resourcecatalog.exception.ResourceCatalogExceptionReason;
import org.fiware.tmforum.resourcecatalog.repository.ResourceCatalogRepository;
import reactor.core.publisher.Mono;

import javax.annotation.Nullable;
import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Controller("${general.basepath:/}")
public class ResourceFunctionApiController extends AbstractApiController implements ResourceFunctionApi {

    private final Clock clock;

    public ResourceFunctionApiController(TMForumMapper tmForumMapper, ReferenceValidationService validationService, ResourceCatalogRepository resourceCatalogRepository, Clock clock) {
        super(tmForumMapper, validationService, resourceCatalogRepository);
        this.clock = clock;
    }

    @Override
    public Mono<HttpResponse<ResourceFunctionVO>> createResourceFunction(ResourceFunctionCreateVO resourceFunctionCreateVO) {
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
                        throw new ResourceCatalogException("A resource relationship without an ID cannot exist.", ResourceCatalogExceptionReason.INVALID_DATA);
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


        // check feature rels
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
                .onErrorMap(throwable -> new ResourceCatalogException(String.format("Was not able to create resource function %s", resourceFunction.getId()), throwable, ResourceCatalogExceptionReason.INVALID_RELATIONSHIP));
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
            throw new ResourceCatalogException("Did not receive a valid id, such resource cannot exist.", ResourceCatalogExceptionReason.NOT_FOUND);
        }
        ResourceFunction resourceFunction = tmForumMapper.map(resourceFunctionUpdateVO, id);

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

        return patch(id, resourceFunction, Mono.zip(connectivityHandlingMono, resourceRelationshipHandlingMono, (p1, p2) -> resourceFunction), ResourceFunction.class)
                .map(tmForumMapper::map)
                .map(HttpResponse::ok);
    }

    @Override
    public Mono<HttpResponse<ResourceFunctionVO>> retrieveResourceFunction(String id, @Nullable String fields) {
        return retrieve(id, ResourceFunction.class)
                .switchIfEmpty(Mono.error(new ResourceCatalogException("No such resources function exists.", ResourceCatalogExceptionReason.NOT_FOUND)))
                .map(tmForumMapper::map)
                .map(HttpResponse::ok);
    }
}
