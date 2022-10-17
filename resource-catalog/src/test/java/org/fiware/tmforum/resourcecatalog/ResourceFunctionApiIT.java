package org.fiware.tmforum.resourcecatalog;

import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import lombok.RequiredArgsConstructor;
import org.fiware.resourcecatalog.api.ResourceFunctionApiTestClient;
import org.fiware.resourcecatalog.api.ResourceFunctionApiTestSpec;
import org.fiware.resourcecatalog.model.CharacteristicRelationshipVOTestExample;
import org.fiware.resourcecatalog.model.CharacteristicVOTestExample;
import org.fiware.resourcecatalog.model.ConnectionPointRefVOTestExample;
import org.fiware.resourcecatalog.model.ConstraintRefVO;
import org.fiware.resourcecatalog.model.ConstraintRefVOTestExample;
import org.fiware.resourcecatalog.model.FeatureRelationshipVOTestExample;
import org.fiware.resourcecatalog.model.FeatureVOTestExample;
import org.fiware.resourcecatalog.model.PlaceRefVOTestExample;
import org.fiware.resourcecatalog.model.RelatedPartyVOTestExample;
import org.fiware.resourcecatalog.model.RelatedPlaceRefOrValueVOTestExample;
import org.fiware.resourcecatalog.model.ResourceFunctionCreateVO;
import org.fiware.resourcecatalog.model.ResourceFunctionCreateVOTestExample;
import org.fiware.resourcecatalog.model.ResourceFunctionUpdateVO;
import org.fiware.resourcecatalog.model.ResourceFunctionVO;
import org.fiware.resourcecatalog.model.ResourceFunctionVOTestExample;
import org.fiware.resourcecatalog.model.ResourceGraphRelationshipVOTestExample;
import org.fiware.resourcecatalog.model.ResourceGraphVOTestExample;
import org.fiware.resourcecatalog.model.ResourceRelationshipVO;
import org.fiware.resourcecatalog.model.ResourceRelationshipVOTestExample;
import org.fiware.resourcecatalog.model.ResourceSpecificationRefVOTestExample;
import org.fiware.resourcecatalog.model.ResourceVO;
import org.fiware.resourcecatalog.model.ScheduleRefVOTestExample;
import org.fiware.tmforum.common.exception.ErrorDetails;
import org.fiware.tmforum.common.test.AbstractApiIT;
import org.fiware.tmforum.resourcecatalog.domain.ResourceFunction;
import org.fiware.tmforum.resourcecatalog.domain.ResourceGraph;
import org.fiware.tmforum.resourcecatalog.domain.ResourceRelationship;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@RequiredArgsConstructor
@MicronautTest(packages = {"org.fiware.tmforum.resourcecatalog"})
public class ResourceFunctionApiIT extends AbstractApiIT implements ResourceFunctionApiTestSpec {

    public final ResourceFunctionApiTestClient resourceFunctionApiTestClient;

    private String message;
    private ResourceFunctionCreateVO resourceFunctionCreateVO;
    private ResourceFunctionUpdateVO resourceFunctionUpdateVO;
    private ResourceFunctionVO expectedResourceFunction;


    @ParameterizedTest
    @MethodSource("provideValidResourceFunctions")
    public void createResourceFunction201(String message, ResourceFunctionCreateVO resourceFunctionCreateVO, ResourceFunctionVO expectedResourceFunction) throws Exception {
        this.message = message;
        this.resourceFunctionCreateVO = resourceFunctionCreateVO;
        this.expectedResourceFunction = expectedResourceFunction;
        createResourceFunction201();
    }


    @Override
    public void createResourceFunction201() throws Exception {

        HttpResponse<ResourceFunctionVO> resourceFunctionVOHttpResponse = callAndCatch(() -> resourceFunctionApiTestClient.createResourceFunction(resourceFunctionCreateVO));
        assertEquals(HttpStatus.CREATED, resourceFunctionVOHttpResponse.getStatus(), message);
        String rfId = resourceFunctionVOHttpResponse.body().getId();
        expectedResourceFunction.setId(rfId);
        expectedResourceFunction.setHref(URI.create(rfId));

        assertEquals(expectedResourceFunction, resourceFunctionVOHttpResponse.body(), message);
    }

    private static Stream<Arguments> provideValidResourceFunctions() {
        List<Arguments> testEntries = new ArrayList<>();

        ResourceFunctionCreateVO emptyCreate = ResourceFunctionCreateVOTestExample.build();
        emptyCreate.place(null).resourceSpecification(null);
        ResourceFunctionVO expectedEmptyRF = ResourceFunctionVOTestExample.build();
        expectedEmptyRF.place(null).resourceSpecification(null);
        testEntries.add(Arguments.of("An empty resource function should have been created.", emptyCreate, expectedEmptyRF));

        ResourceFunctionCreateVO withResRel = ResourceFunctionCreateVOTestExample.build();
        ResourceRelationshipVO resourceRelationshipVO = ResourceRelationshipVOTestExample.build();
        resourceRelationshipVO.getResource().resourceSpecification(null).place(null);
        withResRel.setResourceRelationship(List.of(resourceRelationshipVO));
        withResRel.place(null).resourceSpecification(null);

        ResourceFunctionVO expectedWithResRel = ResourceFunctionVOTestExample.build();
        expectedWithResRel.place(null).resourceSpecification(null);
        expectedWithResRel.resourceRelationship(List.of(resourceRelationshipVO));
        testEntries.add(Arguments.of("A resource function with a related resource should have been created.", withResRel, expectedWithResRel));

        return testEntries.stream();
    }


    @ParameterizedTest
    @MethodSource("provideInvalidResourceFunctions")
    public void createResourceFunction400(String message, ResourceFunctionCreateVO invalidCreateVO) throws Exception {
        this.message = message;
        this.resourceFunctionCreateVO = invalidCreateVO;
        createResourceFunction400();
    }

    @Override
    public void createResourceFunction400() throws Exception {
        HttpResponse<ResourceFunctionVO> creationResponse = callAndCatch(() -> resourceFunctionApiTestClient.createResourceFunction(resourceFunctionCreateVO));
        assertEquals(HttpStatus.BAD_REQUEST, creationResponse.getStatus(), message);
        Optional<ErrorDetails> optionalErrorDetails = creationResponse.getBody(ErrorDetails.class);
        assertTrue(optionalErrorDetails.isPresent(), "Error details should be provided.");
    }

    private static Stream<Arguments> provideInvalidResourceFunctions() {
        List<Arguments> testEntries = new ArrayList<>();

        testEntries.add(Arguments.of("A resource functions with invalid connection points should not be created.",
                ResourceFunctionCreateVOTestExample.build().connectionPoint(List.of(ConnectionPointRefVOTestExample.build()))));
        testEntries.add(Arguments.of("A resource functions with non-existent connection point should not be created.",
                ResourceFunctionCreateVOTestExample.build().connectionPoint(List.of(ConnectionPointRefVOTestExample.build().id("urn:ngsi-ld:connection:non-existent")))));

        testEntries.add(Arguments.of("A resource functions with invalid related parties should not be created.",
                ResourceFunctionCreateVOTestExample.build().relatedParty(List.of(RelatedPartyVOTestExample.build()))));
        testEntries.add(Arguments.of("A resource functions with non-existent related parties should not be created.",
                ResourceFunctionCreateVOTestExample.build().relatedParty(List.of(RelatedPartyVOTestExample.build().id("urn:ngsi-ld:organisation:non-existent")))));

        testEntries.add(Arguments.of("A resource functions with invalid schedules should not be created.",
                ResourceFunctionCreateVOTestExample.build().schedule(List.of(ScheduleRefVOTestExample.build()))));
        testEntries.add(Arguments.of("A resource functions with non-existent schedules should not be created.",
                ResourceFunctionCreateVOTestExample.build().schedule(List.of(ScheduleRefVOTestExample.build().id("urn:ngsi-ld:schedule:non-existent")))));

        testEntries.add(Arguments.of("A resource functions with an invalid place should not be created.",
                ResourceFunctionCreateVOTestExample.build().place(RelatedPlaceRefOrValueVOTestExample.build())));
        testEntries.add(Arguments.of("A resource functions with a non-existent place should not be created.",
                ResourceFunctionCreateVOTestExample.build().place(RelatedPlaceRefOrValueVOTestExample.build().id("urn:ngsi-ld:place:non-existent"))));

        testEntries.add(Arguments.of("A resource functions with an invalid resource spec should not be created.",
                ResourceFunctionCreateVOTestExample.build().resourceSpecification(ResourceSpecificationRefVOTestExample.build())));
        testEntries.add(Arguments.of("A resource functions with a non-existent resource spec  should not be created.",
                ResourceFunctionCreateVOTestExample.build().resourceSpecification(ResourceSpecificationRefVOTestExample.build().id("urn:ngsi-ld:resource-spec:non-existent"))));

        testEntries.add(Arguments.of("A resource function with an invalid activation feature should not be created.",
                ResourceFunctionCreateVOTestExample.build().activationFeature(
                        List.of(FeatureVOTestExample.build()
                                .constraint(List.of(ConstraintRefVOTestExample.build()))))));

        testEntries.add(Arguments.of("A resource functions with an invalid activation feature should not be created.",
                ResourceFunctionCreateVOTestExample.build().activationFeature(
                        List.of(FeatureVOTestExample.build()
                                .featureRelationship(List.of(FeatureRelationshipVOTestExample.build()))))));

        testEntries.add(Arguments.of("A resource functions with an invalid auto modification should not be created.",
                ResourceFunctionCreateVOTestExample.build().autoModification(
                        List.of(CharacteristicVOTestExample.build()
                                .characteristicRelationship(List.of(CharacteristicRelationshipVOTestExample.build()))))));

        testEntries.add(Arguments.of("A resource functions with an invalid resource characteristic should not be created.",
                ResourceFunctionCreateVOTestExample.build().resourceCharacteristic(
                        List.of(CharacteristicVOTestExample.build()
                                .characteristicRelationship(List.of(CharacteristicRelationshipVOTestExample.build()))))));

        testEntries.add(Arguments.of("A resource functions with an invalid connectivity should not be created.",
                ResourceFunctionCreateVOTestExample.build().connectivity(
                        List.of(ResourceGraphVOTestExample.build()
                                .graphRelationship(List.of(ResourceGraphRelationshipVOTestExample.build()))))));

        return testEntries.stream();
    }

    @Override
    public void createResourceFunction401() throws Exception {

    }

    @Override
    public void createResourceFunction403() throws Exception {

    }

    @Override
    public void createResourceFunction405() throws Exception {

    }

    @Override
    public void createResourceFunction409() throws Exception {

    }

    @Override
    public void createResourceFunction500() throws Exception {

    }

    @Override
    public void deleteResourceFunction204() throws Exception {

    }

    @Override
    public void deleteResourceFunction400() throws Exception {

    }

    @Override
    public void deleteResourceFunction401() throws Exception {

    }

    @Override
    public void deleteResourceFunction403() throws Exception {

    }

    @Override
    public void deleteResourceFunction404() throws Exception {

    }

    @Override
    public void deleteResourceFunction405() throws Exception {

    }

    @Override
    public void deleteResourceFunction409() throws Exception {

    }

    @Override
    public void deleteResourceFunction500() throws Exception {

    }

    @Override
    public void listResourceFunction200() throws Exception {

    }

    @Override
    public void listResourceFunction400() throws Exception {

    }

    @Override
    public void listResourceFunction401() throws Exception {

    }

    @Override
    public void listResourceFunction403() throws Exception {

    }

    @Override
    public void listResourceFunction404() throws Exception {

    }

    @Override
    public void listResourceFunction405() throws Exception {

    }

    @Override
    public void listResourceFunction409() throws Exception {

    }

    @Override
    public void listResourceFunction500() throws Exception {

    }

    @Override
    public void patchResourceFunction200() throws Exception {

    }

    @Override
    public void patchResourceFunction400() throws Exception {

    }

    @Override
    public void patchResourceFunction401() throws Exception {

    }

    @Override
    public void patchResourceFunction403() throws Exception {

    }

    @Override
    public void patchResourceFunction404() throws Exception {

    }

    @Override
    public void patchResourceFunction405() throws Exception {

    }

    @Override
    public void patchResourceFunction409() throws Exception {

    }

    @Override
    public void patchResourceFunction500() throws Exception {

    }

    @Override
    public void retrieveResourceFunction200() throws Exception {

    }

    @Override
    public void retrieveResourceFunction400() throws Exception {

    }

    @Override
    public void retrieveResourceFunction401() throws Exception {

    }

    @Override
    public void retrieveResourceFunction403() throws Exception {

    }

    @Override
    public void retrieveResourceFunction404() throws Exception {

    }

    @Override
    public void retrieveResourceFunction405() throws Exception {

    }

    @Override
    public void retrieveResourceFunction409() throws Exception {

    }

    @Override
    public void retrieveResourceFunction500() throws Exception {

    }
}
