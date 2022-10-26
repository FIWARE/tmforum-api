package org.fiware.tmforum.resourcefunction;

import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import lombok.RequiredArgsConstructor;
import org.fiware.resourcefunction.api.ResourceFunctionApiTestClient;
import org.fiware.resourcefunction.api.ResourceFunctionApiTestSpec;
import org.fiware.resourcefunction.model.AttachmentRefOrValueVOTestExample;
import org.fiware.resourcefunction.model.CharacteristicRelationshipVOTestExample;
import org.fiware.resourcefunction.model.CharacteristicVOTestExample;
import org.fiware.resourcefunction.model.ConnectionPointRefVOTestExample;
import org.fiware.resourcefunction.model.ConnectionVOTestExample;
import org.fiware.resourcefunction.model.ConstraintRefVOTestExample;
import org.fiware.resourcefunction.model.FeatureRelationshipVOTestExample;
import org.fiware.resourcefunction.model.FeatureVOTestExample;
import org.fiware.resourcefunction.model.NoteVOTestExample;
import org.fiware.resourcefunction.model.RelatedPartyVOTestExample;
import org.fiware.resourcefunction.model.RelatedPlaceRefOrValueVOTestExample;
import org.fiware.resourcefunction.model.ResourceAdministrativeStateTypeVO;
import org.fiware.resourcefunction.model.ResourceFunctionCreateVO;
import org.fiware.resourcefunction.model.ResourceFunctionCreateVOTestExample;
import org.fiware.resourcefunction.model.ResourceFunctionUpdateVO;
import org.fiware.resourcefunction.model.ResourceFunctionUpdateVOTestExample;
import org.fiware.resourcefunction.model.ResourceFunctionVO;
import org.fiware.resourcefunction.model.ResourceFunctionVOTestExample;
import org.fiware.resourcefunction.model.ResourceGraphRefVOTestExample;
import org.fiware.resourcefunction.model.ResourceGraphRelationshipVOTestExample;
import org.fiware.resourcefunction.model.ResourceGraphVOTestExample;
import org.fiware.resourcefunction.model.ResourceOperationalStateTypeVO;
import org.fiware.resourcefunction.model.ResourceRelationshipVO;
import org.fiware.resourcefunction.model.ResourceRelationshipVOTestExample;
import org.fiware.resourcefunction.model.ResourceSpecificationRefVOTestExample;
import org.fiware.resourcefunction.model.ResourceStatusTypeVO;
import org.fiware.resourcefunction.model.ResourceUsageStateTypeVO;
import org.fiware.resourcefunction.model.ScheduleRefVOTestExample;
import org.fiware.tmforum.common.exception.ErrorDetails;
import org.fiware.tmforum.common.test.AbstractApiIT;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.net.URI;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@RequiredArgsConstructor
@MicronautTest(packages = {"org.fiware.tmforum.resourcefunction"})
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
        expectedResourceFunction.setHref(rfId);

        assertEquals(expectedResourceFunction, resourceFunctionVOHttpResponse.body(), message);
    }

    private static Stream<Arguments> provideValidResourceFunctions() {
        List<Arguments> testEntries = new ArrayList<>();

        ResourceFunctionCreateVO emptyCreate = ResourceFunctionCreateVOTestExample.build();
        emptyCreate.place(null).resourceSpecification(null);
        ResourceFunctionVO expectedEmptyRF = ResourceFunctionVOTestExample.build();
        expectedEmptyRF.place(null).resourceSpecification(null);
        testEntries.add(Arguments.of("An empty resource function should have been created.", emptyCreate, expectedEmptyRF));

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

        testEntries.add(Arguments.of("A resource function without a lifecycleState should not be created.",
                ResourceFunctionCreateVOTestExample.build().lifecycleState(null)));

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

    @Disabled("Security is handled externally, thus 401 and 403 cannot happen.")
    @Test
    @Override
    public void createResourceFunction401() throws Exception {

    }

    @Disabled("Security is handled externally, thus 401 and 403 cannot happen.")
    @Test
    @Override
    public void createResourceFunction403() throws Exception {

    }

    @Disabled("Prohibited by the framework.")
    @Test
    @Override
    public void createResourceFunction405() throws Exception {
    }

    @Override
    public void createResourceFunction409() throws Exception {
        // TODO: check if that can happen
    }


    @Override
    public void createResourceFunction500() throws Exception {

    }

    @Test
    @Override
    public void deleteResourceFunction204() throws Exception {
        ResourceFunctionCreateVO emptyCreate = ResourceFunctionCreateVOTestExample.build();
        emptyCreate.place(null).resourceSpecification(null);

        HttpResponse<ResourceFunctionVO> createResponse = resourceFunctionApiTestClient.createResourceFunction(emptyCreate);
        assertEquals(HttpStatus.CREATED, createResponse.getStatus(), "The resource function should have been created first.");

        String rfId = createResponse.body().getId();


        assertEquals(HttpStatus.NO_CONTENT,
                callAndCatch(() -> resourceFunctionApiTestClient.deleteResourceFunction(rfId)).getStatus(),
                "The resource function should have been deleted.");

        assertEquals(HttpStatus.NOT_FOUND,
                callAndCatch(() -> resourceFunctionApiTestClient.retrieveResourceFunction(rfId, null)).status(),
                "The resource function should not exist anymore.");
    }

    @Disabled("400 is impossible to happen on deletion with the current implementation.")
    @Test
    @Override
    public void deleteResourceFunction400() throws Exception {

    }

    @Disabled("Security is handled externally, thus 401 and 403 cannot happen.")
    @Test
    @Override
    public void deleteResourceFunction401() throws Exception {

    }

    @Disabled("Security is handled externally, thus 401 and 403 cannot happen.")
    @Test
    @Override
    public void deleteResourceFunction403() throws Exception {

    }

    @Test
    @Override
    public void deleteResourceFunction404() throws Exception {
        HttpResponse<?> notFoundResponse = callAndCatch(() -> resourceFunctionApiTestClient.deleteResourceFunction("urn:ngsi-ld:resource-function:no-pop"));
        assertEquals(HttpStatus.NOT_FOUND,
                notFoundResponse.getStatus(),
                "No such resource-function should exist.");

        Optional<ErrorDetails> optionalErrorDetails = notFoundResponse.getBody(ErrorDetails.class);
        assertTrue(optionalErrorDetails.isPresent(), "Error details should be provided.");

        notFoundResponse = callAndCatch(() -> resourceFunctionApiTestClient.deleteResourceFunction("invalid-id"));
        assertEquals(HttpStatus.NOT_FOUND,
                notFoundResponse.getStatus(),
                "No such resource-function should exist.");

        optionalErrorDetails = notFoundResponse.getBody(ErrorDetails.class);
        assertTrue(optionalErrorDetails.isPresent(), "Error details should be provided.");
    }

    @Disabled("Prohibited by the framework.")
    @Test
    @Override
    public void deleteResourceFunction405() throws Exception {

    }

    @Disabled("Impossible status.")
    @Test
    @Override
    public void deleteResourceFunction409() throws Exception {

    }

    @Override
    public void deleteResourceFunction500() throws Exception {

    }

    @Disabled("Cleanup has to be solved")
    @Test
    @Override
    public void listResourceFunction200() throws Exception {
        List<ResourceFunctionVO> expectedResourceFunctions = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            ResourceFunctionCreateVO resourceFunctionCreateVO = ResourceFunctionCreateVOTestExample.build();
            resourceFunctionCreateVO.place(null).resourceSpecification(null);
            String id = resourceFunctionApiTestClient.createResourceFunction(resourceFunctionCreateVO).body().getId();
            ResourceFunctionVO resourceFunctionVO = ResourceFunctionVOTestExample.build();
            resourceFunctionVO.id(id).href(id).place(null).resourceSpecification(null);
            expectedResourceFunctions.add(resourceFunctionVO);
        }

        HttpResponse<List<ResourceFunctionVO>> catalogListResponse = callAndCatch(() -> resourceFunctionApiTestClient.listResourceFunction(null, null, null));
        assertEquals(HttpStatus.OK, catalogListResponse.getStatus(), "The list should be accessible.");

        // ignore order
        List<ResourceFunctionVO> resourceFunctionVOS = catalogListResponse.body();
        assertEquals(expectedResourceFunctions.size(), expectedResourceFunctions.size(), "All categories should be returned.");
        expectedResourceFunctions
                .forEach(resourceFunctionVO ->
                        assertTrue(resourceFunctionVOS.contains(resourceFunctionVO),
                                String.format("All product specs  should be contained. Missing: %s", resourceFunctionVO)));

        // get with pagination
        Integer limit = 5;
        HttpResponse<List<ResourceFunctionVO>> firstPartResponse = callAndCatch(() -> resourceFunctionApiTestClient.listResourceFunction(null, 0, limit));
        assertEquals(limit, firstPartResponse.body().size(), "Only the requested number of entries should be returend.");
        HttpResponse<List<ResourceFunctionVO>> secondPartResponse = callAndCatch(() -> resourceFunctionApiTestClient.listResourceFunction(null, 0 + limit, limit));
        assertEquals(limit, secondPartResponse.body().size(), "Only the requested number of entries should be returend.");

        List<ResourceFunctionVO> retrievedCatalogs = firstPartResponse.body();
        retrievedCatalogs.addAll(secondPartResponse.body());
        expectedResourceFunctions
                .forEach(resourceFunctionVO ->
                        assertTrue(retrievedCatalogs.contains(resourceFunctionVO),
                                String.format("All product specs should be contained. Missing: %s", resourceFunctionVO)));
    }

    @Test
    @Override
    public void listResourceFunction400() throws Exception {
        HttpResponse<List<ResourceFunctionVO>> badRequestResponse = callAndCatch(() -> resourceFunctionApiTestClient.listResourceFunction(null, -1, null));
        assertEquals(HttpStatus.BAD_REQUEST,
                badRequestResponse.getStatus(),
                "Negative offsets are impossible.");

        Optional<ErrorDetails> optionalErrorDetails = badRequestResponse.getBody(ErrorDetails.class);
        assertTrue(optionalErrorDetails.isPresent(), "Error details should be provided.");

        badRequestResponse = callAndCatch(() -> resourceFunctionApiTestClient.listResourceFunction(null, null, -1));
        assertEquals(HttpStatus.BAD_REQUEST,
                badRequestResponse.getStatus(),
                "Negative limits are impossible.");
        optionalErrorDetails = badRequestResponse.getBody(ErrorDetails.class);
        assertTrue(optionalErrorDetails.isPresent(), "Error details should be provided.");
    }

    @Disabled("Security is handled externally, thus 401 and 403 cannot happen.")
    @Test
    @Override
    public void listResourceFunction401() throws Exception {

    }

    @Disabled("Security is handled externally, thus 401 and 403 cannot happen.")
    @Test
    @Override
    public void listResourceFunction403() throws Exception {

    }

    @Disabled("Not found is not possible here, will be answered with an empty list instead.")
    @Test
    @Override
    public void listResourceFunction404() throws Exception {

    }

    @Disabled("Prohibited by the framework.")
    @Test
    @Override
    public void listResourceFunction405() throws Exception {

    }

    @Disabled("Impossible status.")
    @Test
    @Override
    public void listResourceFunction409() throws Exception {

    }

    @Override
    public void listResourceFunction500() throws Exception {

    }

    @ParameterizedTest
    @MethodSource("provideResourceFunctionUpdates")
    public void patchResourceFunction200(String message, ResourceFunctionUpdateVO resourceFunctionUpdateVO, ResourceFunctionVO expectedResourceFunction) throws Exception {
        this.message = message;
        this.resourceFunctionUpdateVO = resourceFunctionUpdateVO;
        this.expectedResourceFunction = expectedResourceFunction;
        patchResourceFunction200();
    }

    @Override
    public void patchResourceFunction200() throws Exception {
        //first create
        ResourceFunctionCreateVO resourceFunctionCreateVO = ResourceFunctionCreateVOTestExample.build().place(null).resourceSpecification(null);

        HttpResponse<ResourceFunctionVO> createResponse = callAndCatch(() -> resourceFunctionApiTestClient.createResourceFunction(resourceFunctionCreateVO));
        assertEquals(HttpStatus.CREATED, createResponse.getStatus(), "The resource function should have been created first.");

        String resourceId = createResponse.body().getId();

        HttpResponse<ResourceFunctionVO> updateResponse = callAndCatch(() -> resourceFunctionApiTestClient.patchResourceFunction(resourceId, resourceFunctionUpdateVO));
        assertEquals(HttpStatus.OK, updateResponse.getStatus(), message);

        ResourceFunctionVO updatedResourceFunction = updateResponse.body();
        expectedResourceFunction.href(resourceId).id(resourceId);

        if (expectedResourceFunction.getConnectionPoint() != null && expectedResourceFunction.getConnectionPoint().isEmpty()) {
            expectedResourceFunction.connectionPoint(null);
        }
        if (expectedResourceFunction.getRelatedParty() != null && expectedResourceFunction.getRelatedParty().isEmpty()) {
            expectedResourceFunction.relatedParty(null);
        }
        if (expectedResourceFunction.getSchedule() != null && expectedResourceFunction.getSchedule().isEmpty()) {
            expectedResourceFunction.schedule(null);
        }

        assertEquals(expectedResourceFunction, updatedResourceFunction, message);
    }

    private static Stream<Arguments> provideResourceFunctionUpdates() {
        List<Arguments> testEntries = new ArrayList<>();

        ResourceFunctionUpdateVO functionUpdate = ResourceFunctionUpdateVOTestExample.build()
                .functionType("new-function")
                .place(null)
                .resourceSpecification(null);
        ResourceFunctionVO expectedFunctionUpdate = ResourceFunctionVOTestExample.build()
                .functionType("new-function")
                .place(null)
                .resourceSpecification(null);
        testEntries.add(Arguments.of("The function type should have been updated.", functionUpdate, expectedFunctionUpdate));

        ResourceFunctionUpdateVO categoryUpdate = ResourceFunctionUpdateVOTestExample.build().category("new-category")
                .place(null)
                .resourceSpecification(null);
        ResourceFunctionVO expectedCategoryUpdate = ResourceFunctionVOTestExample.build()
                .category("new-category")
                .place(null)
                .resourceSpecification(null);
        testEntries.add(Arguments.of("The category should have been updated.", categoryUpdate, expectedCategoryUpdate));

        ResourceFunctionUpdateVO descriptionUpdate = ResourceFunctionUpdateVOTestExample.build()
                .description("new-description")
                .place(null)
                .resourceSpecification(null);
        ResourceFunctionVO expectedDescriptionUpdate = ResourceFunctionVOTestExample.build()
                .description("new-description")
                .place(null)
                .resourceSpecification(null);
        testEntries.add(Arguments.of("The description should have been updated.", descriptionUpdate, expectedDescriptionUpdate));

        Instant endTime = Instant.now();
        ResourceFunctionUpdateVO endTimeUpdate = ResourceFunctionUpdateVOTestExample.build()
                .endOperatingDate(endTime)
                .place(null)
                .resourceSpecification(null);
        ResourceFunctionVO expectedEndTimeUpdate = ResourceFunctionVOTestExample.build()
                .place(null)
                .resourceSpecification(null)
                .endOperatingDate(endTime);
        testEntries.add(Arguments.of("The endTime should have been updated.", endTimeUpdate, expectedEndTimeUpdate));

        ResourceFunctionUpdateVO nameUpdate = ResourceFunctionUpdateVOTestExample.build()
                .name("new-name")
                .place(null)
                .resourceSpecification(null);
        ResourceFunctionVO expectedNameUpdate = ResourceFunctionVOTestExample.build()
                .name("new-name")
                .place(null)
                .resourceSpecification(null);
        testEntries.add(Arguments.of("The name should have been updated.", nameUpdate, expectedNameUpdate));

        ResourceFunctionUpdateVO priorityUpdate = ResourceFunctionUpdateVOTestExample.build()
                .priority(10)
                .place(null)
                .resourceSpecification(null);
        ResourceFunctionVO expectedPriorityUpdate = ResourceFunctionVOTestExample.build()
                .priority(10)
                .place(null)
                .resourceSpecification(null);
        testEntries.add(Arguments.of("The priority should have been updated.", priorityUpdate, expectedPriorityUpdate));

        ResourceFunctionUpdateVO resourceVersionUpdate = ResourceFunctionUpdateVOTestExample.build()
                .resourceVersion("10")
                .place(null)
                .resourceSpecification(null);
        ResourceFunctionVO expectedResourceVersionUpdate = ResourceFunctionVOTestExample.build()
                .resourceVersion("10")
                .place(null)
                .resourceSpecification(null);
        testEntries.add(Arguments.of("The resourceVersion should have been updated.", resourceVersionUpdate, expectedResourceVersionUpdate));

        ResourceFunctionUpdateVO roleUpdate = ResourceFunctionUpdateVOTestExample.build()
                .role("new-role")
                .place(null)
                .resourceSpecification(null);
        ResourceFunctionVO expectedRoleUpdate = ResourceFunctionVOTestExample.build()
                .role("new-role")
                .place(null)
                .resourceSpecification(null);
        testEntries.add(Arguments.of("The role should have been updated.", roleUpdate, expectedRoleUpdate));

        Instant starTime = Instant.now();
        ResourceFunctionUpdateVO startimeUpdate = ResourceFunctionUpdateVOTestExample.build()
                .startOperatingDate(starTime)
                .place(null)
                .resourceSpecification(null);
        ResourceFunctionVO expectedStartimeUpdate = ResourceFunctionVOTestExample.build()
                .startOperatingDate(starTime)
                .place(null)
                .resourceSpecification(null);
        testEntries.add(Arguments.of("The startime should have been updated.", startimeUpdate, expectedStartimeUpdate));

        ResourceFunctionUpdateVO valueUpdate = ResourceFunctionUpdateVOTestExample.build()
                .value("new-value")
                .place(null)
                .resourceSpecification(null);
        ResourceFunctionVO expectedValueUpdate = ResourceFunctionVOTestExample.build()
                .value("new-value")
                .place(null)
                .resourceSpecification(null);
        testEntries.add(Arguments.of("The value should have been updated.", valueUpdate, expectedValueUpdate));

        ResourceFunctionUpdateVO adminStateUpdate = ResourceFunctionUpdateVOTestExample.build()
                .administrativeState(ResourceAdministrativeStateTypeVO.LOCKED)
                .place(null)
                .resourceSpecification(null);
        ResourceFunctionVO expectedAdminStateUpdate = ResourceFunctionVOTestExample.build()
                .administrativeState(ResourceAdministrativeStateTypeVO.LOCKED)
                .place(null)
                .resourceSpecification(null);
        testEntries.add(Arguments.of("The administrative state should have been updated.", adminStateUpdate, expectedAdminStateUpdate));

        ResourceFunctionUpdateVO attachmentUpdate = ResourceFunctionUpdateVOTestExample.build()
                .attachment(List.of(AttachmentRefOrValueVOTestExample.build()
                        .id("urn:ngsi-ld:attachment:a")
                        .url("http://my-attachment.com")
                        .href("urn:ngsi-ld:attachment:a")))
                .place(null)
                .resourceSpecification(null);
        ResourceFunctionVO expectedAttachmentUpdate = ResourceFunctionVOTestExample.build()
                .attachment(List.of(AttachmentRefOrValueVOTestExample.build()
                        .id("urn:ngsi-ld:attachment:a")
                        .url("http://my-attachment.com")
                        .href("urn:ngsi-ld:attachment:a")
                        .validFor(null)))
                .place(null)
                .resourceSpecification(null);
        testEntries.add(Arguments.of("The attachments should have been updated.", attachmentUpdate, expectedAttachmentUpdate));

        String featureId = UUID.randomUUID().toString();
        ResourceFunctionUpdateVO featureUpdate = ResourceFunctionUpdateVOTestExample.build()
                .activationFeature(List.of(FeatureVOTestExample.build().id(featureId)))
                .place(null)
                .resourceSpecification(null);
        ResourceFunctionVO expectedFeatureUpdate = ResourceFunctionVOTestExample.build()
                .activationFeature(List.of(FeatureVOTestExample.build().id(featureId).constraint(null).featureCharacteristic(null).featureRelationship(null)))
                .place(null)
                .resourceSpecification(null);
        testEntries.add(Arguments.of("The activation feature should have been updated.", featureUpdate, expectedFeatureUpdate));

        String charId = UUID.randomUUID().toString();
        ResourceFunctionUpdateVO autoModificationUpdate = ResourceFunctionUpdateVOTestExample.build()
                .autoModification(List.of(CharacteristicVOTestExample.build().id(charId)))
                .place(null)
                .resourceSpecification(null);
        ResourceFunctionVO expectedAutoModificationUpdate = ResourceFunctionVOTestExample.build()
                .autoModification(List.of(CharacteristicVOTestExample.build().id(charId).characteristicRelationship(null)))
                .place(null)
                .resourceSpecification(null);
        testEntries.add(Arguments.of("The autoModification should have been updated.", autoModificationUpdate, expectedAutoModificationUpdate));

        String resourceCharId = UUID.randomUUID().toString();
        ResourceFunctionUpdateVO resourceCharacteristicUpdate = ResourceFunctionUpdateVOTestExample.build()
                .resourceCharacteristic(List.of(CharacteristicVOTestExample.build().id(resourceCharId)))
                .place(null)
                .resourceSpecification(null);
        ResourceFunctionVO expectedResourceCharacteristicUpdate = ResourceFunctionVOTestExample.build()
                .resourceCharacteristic(List.of(CharacteristicVOTestExample.build().characteristicRelationship(null).id(resourceCharId)))
                .place(null)
                .resourceSpecification(null);
        testEntries.add(Arguments.of("The resourceCharacteristic should have been updated.", resourceCharacteristicUpdate, expectedResourceCharacteristicUpdate));

        String graphId = UUID.randomUUID().toString();
        ResourceFunctionUpdateVO connectivityUpdate = ResourceFunctionUpdateVOTestExample.build()
                .connectivity(List.of(ResourceGraphVOTestExample.build().id(graphId)))
                .place(null)
                .resourceSpecification(null);
        ResourceFunctionVO expectedConnectivityUpdate = ResourceFunctionVOTestExample.build()
                .connectivity(List.of(ResourceGraphVOTestExample.build().id(graphId).connection(null).graphRelationship(null)))
                .place(null)
                .resourceSpecification(null);
        testEntries.add(Arguments.of("The connectivity should have been updated.", connectivityUpdate, expectedConnectivityUpdate));

        ResourceFunctionUpdateVO notesUpdate = ResourceFunctionUpdateVOTestExample.build()
                .note(List.of(NoteVOTestExample.build().author("My author").id("author-note").text("This is my note.")))
                .place(null)
                .resourceSpecification(null);
        ResourceFunctionVO expectedNotesUpdate = ResourceFunctionVOTestExample.build()
                .note(List.of(NoteVOTestExample.build().author("My author").id("author-note").text("This is my note.")))
                .place(null)
                .resourceSpecification(null);
        testEntries.add(Arguments.of("The notes should have been updated.", notesUpdate, expectedNotesUpdate));

        ResourceFunctionUpdateVO opStateUpdate = ResourceFunctionUpdateVOTestExample.build()
                .operationalState(ResourceOperationalStateTypeVO.ENABLE)
                .place(null)
                .resourceSpecification(null);
        ResourceFunctionVO expectedOpStateUpdate = ResourceFunctionVOTestExample.build()
                .operationalState(ResourceOperationalStateTypeVO.ENABLE)
                .place(null)
                .resourceSpecification(null);
        testEntries.add(Arguments.of("The operational state should have been updated.", opStateUpdate, expectedOpStateUpdate));

        ResourceFunctionUpdateVO statusUpdate = ResourceFunctionUpdateVOTestExample.build()
                .resourceStatus(ResourceStatusTypeVO.AVAILABLE)
                .place(null)
                .resourceSpecification(null);
        ResourceFunctionVO expectedStatusUpdate = ResourceFunctionVOTestExample.build()
                .resourceStatus(ResourceStatusTypeVO.AVAILABLE)
                .place(null)
                .resourceSpecification(null);
        testEntries.add(Arguments.of("The resource status should have been updated.", statusUpdate, expectedStatusUpdate));

        ResourceFunctionUpdateVO usageStateUpdate = ResourceFunctionUpdateVOTestExample.build()
                .usageState(ResourceUsageStateTypeVO.ACTIVE)
                .place(null)
                .resourceSpecification(null);
        ResourceFunctionVO expectedUsageStateUpdate = ResourceFunctionVOTestExample.build()
                .usageState(ResourceUsageStateTypeVO.ACTIVE)
                .place(null)
                .resourceSpecification(null);
        testEntries.add(Arguments.of("The usageState should have been updated.", usageStateUpdate, expectedUsageStateUpdate));

        return testEntries.stream();
    }

    @ParameterizedTest
    @MethodSource("provideInvalidUpdates")
    public void patchResourceFunction400(String message, ResourceFunctionUpdateVO invalidUpdateVO) throws Exception {
        this.message = message;
        this.resourceFunctionUpdateVO = invalidUpdateVO;
        patchResourceFunction400();
    }

    @Override
    public void patchResourceFunction400() throws Exception {
        //first create
        ResourceFunctionCreateVO resourceFunctionCreateVO = ResourceFunctionCreateVOTestExample.build().place(null).resourceSpecification(null);

        HttpResponse<ResourceFunctionVO> createResponse = callAndCatch(() -> resourceFunctionApiTestClient.createResourceFunction(resourceFunctionCreateVO));
        assertEquals(HttpStatus.CREATED, createResponse.getStatus(), "The resource function should have been created first.");

        String resourceId = createResponse.body().getId();

        HttpResponse<ResourceFunctionVO> updateResponse = callAndCatch(() -> resourceFunctionApiTestClient.patchResourceFunction(resourceId, resourceFunctionUpdateVO));
        assertEquals(HttpStatus.BAD_REQUEST, updateResponse.getStatus(), message);

        Optional<ErrorDetails> optionalErrorDetails = updateResponse.getBody(ErrorDetails.class);
        assertTrue(optionalErrorDetails.isPresent(), "Some error details should be present.");
    }

    private static Stream<Arguments> provideInvalidUpdates() {
        List<Arguments> testEntries = new ArrayList<>();

        testEntries.add(Arguments.of("An update with an invalid connection point ref is not allowed.",
                ResourceFunctionUpdateVOTestExample.build()
                        .connectionPoint(List.of(ConnectionPointRefVOTestExample.build()))));
        testEntries.add(Arguments.of("An update with an non existent connection point ref is not allowed.",
                ResourceFunctionUpdateVOTestExample.build()
                        .connectionPoint(List.of(ConnectionPointRefVOTestExample.build().id("urn:ngsi-ld:connection-point:non-existent")))));

        testEntries.add(Arguments.of("An update with an invalid related party ref is not allowed.",
                ResourceFunctionUpdateVOTestExample.build()
                        .relatedParty(List.of(RelatedPartyVOTestExample.build()))));
        testEntries.add(Arguments.of("An update with an non existent related party is not allowed.",
                ResourceFunctionUpdateVOTestExample.build()
                        .relatedParty(List.of(RelatedPartyVOTestExample.build().id("urn:ngsi-ld:organisation:non-existent")))));

        testEntries.add(Arguments.of("An update with an invalid schedule ref is not allowed.",
                ResourceFunctionUpdateVOTestExample.build()
                        .schedule(List.of(ScheduleRefVOTestExample.build()))));
        testEntries.add(Arguments.of("An update with an non existent schedule is not allowed.",
                ResourceFunctionUpdateVOTestExample.build()
                        .schedule(List.of(ScheduleRefVOTestExample.build().id("urn:ngsi-ld:schedule:non-existent")))));

        // TODO: build place handling like the one for resource relationship ref-or-value
        testEntries.add(Arguments.of("An update with an invalid place ref is not allowed.",
                ResourceFunctionUpdateVOTestExample.build()
                        .place(RelatedPlaceRefOrValueVOTestExample.build())));
        testEntries.add(Arguments.of("An update with an non existent schedule is not allowed.",
                ResourceFunctionUpdateVOTestExample.build()
                        .place(RelatedPlaceRefOrValueVOTestExample.build().id("urn:ngsi-ld:place:non-existent"))));

        testEntries.add(Arguments.of("An update with an invalid resource spec ref is not allowed.",
                ResourceFunctionUpdateVOTestExample.build()
                        .resourceSpecification(ResourceSpecificationRefVOTestExample.build())));
        testEntries.add(Arguments.of("An update with an non existent schedule is not allowed.",
                ResourceFunctionUpdateVOTestExample.build()
                        .resourceSpecification(ResourceSpecificationRefVOTestExample.build().id("urn:ngsi-ld:resource:non-existent"))));

        testEntries.add(Arguments.of("An update with an invalid activation constraint spec ref is not allowed.",
                ResourceFunctionUpdateVOTestExample.build()
                        .activationFeature(List.of(FeatureVOTestExample.build().constraint(List.of(ConstraintRefVOTestExample.build()))))));
        testEntries.add(Arguments.of("An update with an non existent activation constraint is not allowed.",
                ResourceFunctionUpdateVOTestExample.build()
                        .activationFeature(List.of(FeatureVOTestExample.build().constraint(List.of(ConstraintRefVOTestExample.build().id("urn:ngsi-ld:constraint:non-existent")))))));

        testEntries.add(Arguments.of("An update with an invalid activation feature ref is not allowed.",
                ResourceFunctionUpdateVOTestExample.build()
                        .activationFeature(List.of(FeatureVOTestExample.build().featureRelationship(List.of(FeatureRelationshipVOTestExample.build()))))));
        testEntries.add(Arguments.of("An update with an non existent activation feature ref is not allowed.",
                ResourceFunctionUpdateVOTestExample.build()
                        .activationFeature(List.of(FeatureVOTestExample.build().featureRelationship(List.of(FeatureRelationshipVOTestExample.build().id("urn:ngsi-ld:feature:non-existent")))))));

        testEntries.add(Arguments.of("An update with an invalid auto modification char ref is not allowed.",
                ResourceFunctionUpdateVOTestExample.build()
                        .autoModification(List.of(CharacteristicVOTestExample.build().characteristicRelationship(List.of(CharacteristicRelationshipVOTestExample.build()))))));
        testEntries.add(Arguments.of("An update with an non existent auto modification char ref is not allowed.",
                ResourceFunctionUpdateVOTestExample.build()
                        .autoModification(List.of(CharacteristicVOTestExample.build().characteristicRelationship(List.of(CharacteristicRelationshipVOTestExample.build().id("urn:ngsi-ld:char:non-existent")))))));

        testEntries.add(Arguments.of("An update with an invalid resource char char ref is not allowed.",
                ResourceFunctionUpdateVOTestExample.build()
                        .resourceCharacteristic(List.of(CharacteristicVOTestExample.build().characteristicRelationship(List.of(CharacteristicRelationshipVOTestExample.build()))))));
        testEntries.add(Arguments.of("An update with an non existent resource char char ref is not allowed.",
                ResourceFunctionUpdateVOTestExample.build()
                        .resourceCharacteristic(List.of(CharacteristicVOTestExample.build().characteristicRelationship(List.of(CharacteristicRelationshipVOTestExample.build().id("urn:ngsi-ld:char:non-existent")))))));

        testEntries.add(Arguments.of("An update with an invalid connectivity connection is not allowed.",
                ResourceFunctionUpdateVOTestExample.build()
                        .connectivity(List.of(ResourceGraphVOTestExample.build().connection(List.of(ConnectionVOTestExample.build()))))));
        testEntries.add(Arguments.of("An update with an non existent connectivity connection is not allowed.",
                ResourceFunctionUpdateVOTestExample.build()
                        .connectivity(List.of(ResourceGraphVOTestExample.build().connection(List.of(ConnectionVOTestExample.build().id("urn:ngsi-ld:connection:non-existent")))))));

        testEntries.add(Arguments.of("An update with an invalid connectivity graph rel is not allowed.",
                ResourceFunctionUpdateVOTestExample.build()
                        .connectivity(List.of(ResourceGraphVOTestExample.build().graphRelationship(List.of(ResourceGraphRelationshipVOTestExample.build().resourceGraph(ResourceGraphRefVOTestExample.build())))))));
        testEntries.add(Arguments.of("An update with an non existent connectivity graph rel connection is not allowed.",
                ResourceFunctionUpdateVOTestExample.build()
                        .connectivity(List.of(ResourceGraphVOTestExample.build().graphRelationship(List.of(ResourceGraphRelationshipVOTestExample.build().resourceGraph(ResourceGraphRefVOTestExample.build().id("urn:ngsi-ld:graph:non-existent"))))))));

        return testEntries.stream();
    }

    @Disabled("Security is handled externally, thus 401 and 403 cannot happen.")
    @Test
    @Override
    public void patchResourceFunction401() throws Exception {

    }

    @Disabled("Security is handled externally, thus 401 and 403 cannot happen.")
    @Test
    @Override
    public void patchResourceFunction403() throws Exception {

    }

    @Test
    @Override
    public void patchResourceFunction404() throws Exception {
        ResourceFunctionUpdateVO resourceFunctionUpdateVO = ResourceFunctionUpdateVOTestExample.build();
        assertEquals(
                HttpStatus.NOT_FOUND,
                callAndCatch(() -> resourceFunctionApiTestClient.patchResourceFunction("urn:ngsi-ld:resource-function:not-existent", resourceFunctionUpdateVO)).getStatus(),
                "Non existent resource functions should not be updated.");
    }

    @Disabled("Prohibited by the framework.")
    @Test
    @Override
    public void patchResourceFunction405() throws Exception {

    }

    @Override
    public void patchResourceFunction409() throws Exception {
        // TODO: can this happen?
    }

    @Override
    public void patchResourceFunction500() throws Exception {

    }

    @Test
    @Override
    public void retrieveResourceFunction200() throws Exception {

        ResourceFunctionCreateVO resourceFunctionCreateVO = ResourceFunctionCreateVOTestExample.build().place(null).resourceSpecification(null);
        HttpResponse<ResourceFunctionVO> createResponse = callAndCatch(() -> resourceFunctionApiTestClient.createResourceFunction(resourceFunctionCreateVO));
        assertEquals(HttpStatus.CREATED, createResponse.getStatus(), "The productSpecification should have been created first.");
        String id = createResponse.body().getId();

        ResourceFunctionVO expectedResourceFunctionVO = ResourceFunctionVOTestExample.build();
        expectedResourceFunctionVO
                .id(id)
                .href(id)
                .place(null)
                .resourceSpecification(null)
                .connectionPoint(null)
                .relatedParty(null)
                .schedule(null);

        //then retrieve
        HttpResponse<ResourceFunctionVO> retrievedRF = callAndCatch(() -> resourceFunctionApiTestClient.retrieveResourceFunction(id, null));
        assertEquals(HttpStatus.OK, retrievedRF.getStatus(), "The retrieval should be ok.");
        assertEquals(expectedResourceFunctionVO, retrievedRF.body(), "The correct resource function should be returned.");
    }

    @Disabled("400 cannot happen, only 404")
    @Test
    @Override
    public void retrieveResourceFunction400() throws Exception {

    }

    @Disabled("Security is handled externally, thus 401 and 403 cannot happen.")
    @Test
    @Override
    public void retrieveResourceFunction401() throws Exception {

    }

    @Disabled("Security is handled externally, thus 401 and 403 cannot happen.")
    @Test
    @Override
    public void retrieveResourceFunction403() throws Exception {

    }

    @Test
    @Override
    public void retrieveResourceFunction404() throws Exception {
        HttpResponse<ResourceFunctionVO> response = callAndCatch(() -> resourceFunctionApiTestClient.retrieveResourceFunction("urn:ngsi-ld:resource-function:non-existent", null));
        assertEquals(HttpStatus.NOT_FOUND, response.getStatus(), "No such resource-function should exist.");

        Optional<ErrorDetails> optionalErrorDetails = response.getBody(ErrorDetails.class);
        assertTrue(optionalErrorDetails.isPresent(), "Error details should have been provided.");
    }

    @Disabled("Prohibited by the framework.")
    @Test
    @Override
    public void retrieveResourceFunction405() throws Exception {

    }

    @Disabled("Conflict not possible on retrieval")
    @Test
    @Override
    public void retrieveResourceFunction409() throws Exception {

    }

    @Override
    public void retrieveResourceFunction500() throws Exception {

    }
}
