package org.fiware.tmforum.resourcecandidate;

import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import io.micronaut.test.annotation.MockBean;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import lombok.RequiredArgsConstructor;
import org.fiware.resourcecatalog.api.ResourceCandidateApiTestClient;
import org.fiware.resourcecatalog.api.ResourceCandidateApiTestSpec;
import org.fiware.resourcecatalog.model.RelatedPartyVOTestExample;
import org.fiware.resourcecatalog.model.ResourceCandidateCreateVO;
import org.fiware.resourcecatalog.model.ResourceCandidateCreateVOTestExample;
import org.fiware.resourcecatalog.model.ResourceCandidateUpdateVO;
import org.fiware.resourcecatalog.model.ResourceCandidateUpdateVOTestExample;
import org.fiware.resourcecatalog.model.ResourceCandidateVO;
import org.fiware.resourcecatalog.model.ResourceCandidateVOTestExample;
import org.fiware.resourcecatalog.model.ResourceCategoryRefVOTestExample;
import org.fiware.resourcecatalog.model.ResourceSpecificationRefVOTestExample;
import org.fiware.resourcecatalog.model.TimePeriodVO;
import org.fiware.resourcecatalog.model.TimePeriodVOTestExample;
import org.fiware.tmforum.common.exception.ErrorDetails;
import org.fiware.tmforum.common.test.AbstractApiIT;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RequiredArgsConstructor
@MicronautTest(packages = {"org.fiware.tmforum.resourcecandidate"})
public class ResourceCandidateApiIT extends AbstractApiIT implements ResourceCandidateApiTestSpec {

    public final ResourceCandidateApiTestClient resourceCandidateApiTestClient;

    private String message;
    private ResourceCandidateCreateVO resourceCandidateCreateVO;
    private ResourceCandidateUpdateVO resourceCandidateUpdateVO;
    private ResourceCandidateVO expectedResourceCandidate;

    private Clock clock = mock(Clock.class);

    @MockBean(Clock.class)
    public Clock clock() {
        return clock;
    }

    @ParameterizedTest
    @MethodSource("provideValidResourceCandidates")
    public void createResourceCandidate201(String message, ResourceCandidateCreateVO resourceCandidateCreateVO, ResourceCandidateVO expectedResourceCandidate) throws Exception {
        this.message = message;
        this.resourceCandidateCreateVO = resourceCandidateCreateVO;
        this.expectedResourceCandidate = expectedResourceCandidate;
        createResourceCandidate201();
    }

    @Override
    public void createResourceCandidate201() throws Exception {

        Instant currentTimeInstant = Instant.ofEpochSecond(10000);
        when(clock.instant()).thenReturn(currentTimeInstant);

        HttpResponse<ResourceCandidateVO> resourceCandidateVOHttpResponse = callAndCatch(() -> resourceCandidateApiTestClient.createResourceCandidate(resourceCandidateCreateVO));
        assertEquals(HttpStatus.CREATED, resourceCandidateVOHttpResponse.getStatus(), message);
        String rfId = resourceCandidateVOHttpResponse.body().getId();
        expectedResourceCandidate.setId(rfId);
        expectedResourceCandidate.setHref(rfId);
        expectedResourceCandidate.setLastUpdate(currentTimeInstant);
        expectedResourceCandidate.resourceSpecification(null);

        assertEquals(expectedResourceCandidate, resourceCandidateVOHttpResponse.body(), message);
    }

    private static Stream<Arguments> provideValidResourceCandidates() {
        List<Arguments> testEntries = new ArrayList<>();

        ResourceCandidateCreateVO emptyCreate = ResourceCandidateCreateVOTestExample.build().lifecycleStatus("created").resourceSpecification(null);
        ResourceCandidateVO expectedEmpty = ResourceCandidateVOTestExample.build().lifecycleStatus("created").resourceSpecification(null);
        testEntries.add(Arguments.of("An empty resource candidate should have been created.", emptyCreate, expectedEmpty));

        TimePeriodVO timePeriodVO = TimePeriodVOTestExample.build().endDateTime(Instant.now()).startDateTime(Instant.now());
        ResourceCandidateCreateVO createValidFor = ResourceCandidateCreateVOTestExample.build().validFor(timePeriodVO).lifecycleStatus("created").resourceSpecification(null);
        ResourceCandidateVO expectedValidFor = ResourceCandidateVOTestExample.build().validFor(timePeriodVO).lifecycleStatus("created").resourceSpecification(null);
        testEntries.add(Arguments.of("An resource candidate with a validFor should have been created.", createValidFor, expectedValidFor));


        return testEntries.stream();
    }


    @ParameterizedTest
    @MethodSource("provideInvalidResourceCandidates")
    public void createResourceCandidate400(String message, ResourceCandidateCreateVO invalidCreateVO) throws Exception {
        this.message = message;
        this.resourceCandidateCreateVO = invalidCreateVO;
        createResourceCandidate400();
    }

    @Override
    public void createResourceCandidate400() throws Exception {
        HttpResponse<ResourceCandidateVO> creationResponse = callAndCatch(() -> resourceCandidateApiTestClient.createResourceCandidate(resourceCandidateCreateVO));
        assertEquals(HttpStatus.BAD_REQUEST, creationResponse.getStatus(), message);
        Optional<ErrorDetails> optionalErrorDetails = creationResponse.getBody(ErrorDetails.class);
        assertTrue(optionalErrorDetails.isPresent(), "Error details should be provided.");
    }

    private static Stream<Arguments> provideInvalidResourceCandidates() {
        List<Arguments> testEntries = new ArrayList<>();

        testEntries.add(Arguments.of("A resource candidate with an spec ref should not be created.",
                ResourceCandidateCreateVOTestExample.build().resourceSpecification(ResourceSpecificationRefVOTestExample.build())));
        testEntries.add(Arguments.of("A resource candidate with a non-existent spec ref should not be created.",
                ResourceCandidateCreateVOTestExample.build().resourceSpecification(ResourceSpecificationRefVOTestExample.build().id("urn:ngsi-ld:resource-specification:non-existent"))));

        testEntries.add(Arguments.of("A resource candidate with an invalid resource category should not be created.",
                ResourceCandidateCreateVOTestExample.build().category(List.of(ResourceCategoryRefVOTestExample.build()))));
        testEntries.add(Arguments.of("A resource candidate with a non-existent resource category should not be created.",
                ResourceCandidateCreateVOTestExample.build().category(List.of(ResourceCategoryRefVOTestExample.build().id("urn:ngsi-ld:category:non-existent")))));

        return testEntries.stream();
    }

    @Disabled("Security is handled externally, thus 401 and 403 cannot happen.")
    @Test
    @Override
    public void createResourceCandidate401() throws Exception {

    }

    @Disabled("Security is handled externally, thus 401 and 403 cannot happen.")
    @Test
    @Override
    public void createResourceCandidate403() throws Exception {

    }

    @Disabled("Prohibited by the framework.")
    @Test
    @Override
    public void createResourceCandidate405() throws Exception {
    }

    @Disabled("No implicit creation, impossible state.")
    @Test
    @Override
    public void createResourceCandidate409() throws Exception {

    }


    @Override
    public void createResourceCandidate500() throws Exception {

    }

    @Test
    @Override
    public void deleteResourceCandidate204() throws Exception {
        ResourceCandidateCreateVO emptyCreate = ResourceCandidateCreateVOTestExample.build().resourceSpecification(null);

        HttpResponse<ResourceCandidateVO> createResponse = resourceCandidateApiTestClient.createResourceCandidate(emptyCreate);
        assertEquals(HttpStatus.CREATED, createResponse.getStatus(), "The resource candidate should have been created first.");

        String rfId = createResponse.body().getId();


        assertEquals(HttpStatus.NO_CONTENT,
                callAndCatch(() -> resourceCandidateApiTestClient.deleteResourceCandidate(rfId)).getStatus(),
                "The resource candidate should have been deleted.");

        assertEquals(HttpStatus.NOT_FOUND,
                callAndCatch(() -> resourceCandidateApiTestClient.retrieveResourceCandidate(rfId, null)).status(),
                "The resource candidate should not exist anymore.");
    }

    @Disabled("400 is impossible to happen on deletion with the current implementation.")
    @Test
    @Override
    public void deleteResourceCandidate400() throws Exception {

    }

    @Disabled("Security is handled externally, thus 401 and 403 cannot happen.")
    @Test
    @Override
    public void deleteResourceCandidate401() throws Exception {

    }

    @Disabled("Security is handled externally, thus 401 and 403 cannot happen.")
    @Test
    @Override
    public void deleteResourceCandidate403() throws Exception {

    }

    @Test
    @Override
    public void deleteResourceCandidate404() throws Exception {
        HttpResponse<?> notFoundResponse = callAndCatch(() -> resourceCandidateApiTestClient.deleteResourceCandidate("urn:ngsi-ld:resource-candidate:no-pop"));
        assertEquals(HttpStatus.NOT_FOUND,
                notFoundResponse.getStatus(),
                "No such resource-candidate should exist.");

        Optional<ErrorDetails> optionalErrorDetails = notFoundResponse.getBody(ErrorDetails.class);
        assertTrue(optionalErrorDetails.isPresent(), "Error details should be provided.");

        notFoundResponse = callAndCatch(() -> resourceCandidateApiTestClient.deleteResourceCandidate("invalid-id"));
        assertEquals(HttpStatus.NOT_FOUND,
                notFoundResponse.getStatus(),
                "No such resource-candidate should exist.");

        optionalErrorDetails = notFoundResponse.getBody(ErrorDetails.class);
        assertTrue(optionalErrorDetails.isPresent(), "Error details should be provided.");
    }

    @Disabled("Prohibited by the framework.")
    @Test
    @Override
    public void deleteResourceCandidate405() throws Exception {

    }

    @Disabled("Impossible status.")
    @Test
    @Override
    public void deleteResourceCandidate409() throws Exception {

    }

    @Override
    public void deleteResourceCandidate500() throws Exception {

    }

    @Disabled("Cleanup has to be solved")
    @Test
    @Override
    public void listResourceCandidate200() throws Exception {
        List<ResourceCandidateVO> expectedResourceCandidates = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            ResourceCandidateCreateVO resourceCandidateCreateVO = ResourceCandidateCreateVOTestExample.build().resourceSpecification(null);
            String id = resourceCandidateApiTestClient.createResourceCandidate(resourceCandidateCreateVO).body().getId();
            ResourceCandidateVO resourceCandidateVO = ResourceCandidateVOTestExample.build();
            resourceCandidateVO.id(id).href(id);
            expectedResourceCandidates.add(resourceCandidateVO);
        }

        HttpResponse<List<ResourceCandidateVO>> candidateListResponse = callAndCatch(() -> resourceCandidateApiTestClient.listResourceCandidate(null, null, null));
        assertEquals(HttpStatus.OK, candidateListResponse.getStatus(), "The list should be accessible.");

        // ignore order
        List<ResourceCandidateVO> resourceCandidateVOS = candidateListResponse.body();
        assertEquals(expectedResourceCandidates.size(), expectedResourceCandidates.size(), "All categories should be returned.");
        expectedResourceCandidates
                .forEach(resourceCandidateVO ->
                        assertTrue(resourceCandidateVOS.contains(resourceCandidateVO),
                                String.format("All product specs  should be contained. Missing: %s", resourceCandidateVO)));

        // get with pagination
        Integer limit = 5;
        HttpResponse<List<ResourceCandidateVO>> firstPartResponse = callAndCatch(() -> resourceCandidateApiTestClient.listResourceCandidate(null, 0, limit));
        assertEquals(limit, firstPartResponse.body().size(), "Only the requested number of entries should be returend.");
        HttpResponse<List<ResourceCandidateVO>> secondPartResponse = callAndCatch(() -> resourceCandidateApiTestClient.listResourceCandidate(null, 0 + limit, limit));
        assertEquals(limit, secondPartResponse.body().size(), "Only the requested number of entries should be returend.");

        List<ResourceCandidateVO> retrievedCatalogs = firstPartResponse.body();
        retrievedCatalogs.addAll(secondPartResponse.body());
        expectedResourceCandidates
                .forEach(resourceCandidateVO ->
                        assertTrue(retrievedCatalogs.contains(resourceCandidateVO),
                                String.format("All resource candidates should be contained. Missing: %s", resourceCandidateVO)));
    }

    @Test
    @Override
    public void listResourceCandidate400() throws Exception {
        HttpResponse<List<ResourceCandidateVO>> badRequestResponse = callAndCatch(() -> resourceCandidateApiTestClient.listResourceCandidate(null, -1, null));
        assertEquals(HttpStatus.BAD_REQUEST,
                badRequestResponse.getStatus(),
                "Negative offsets are impossible.");

        Optional<ErrorDetails> optionalErrorDetails = badRequestResponse.getBody(ErrorDetails.class);
        assertTrue(optionalErrorDetails.isPresent(), "Error details should be provided.");

        badRequestResponse = callAndCatch(() -> resourceCandidateApiTestClient.listResourceCandidate(null, null, -1));
        assertEquals(HttpStatus.BAD_REQUEST,
                badRequestResponse.getStatus(),
                "Negative limits are impossible.");
        optionalErrorDetails = badRequestResponse.getBody(ErrorDetails.class);
        assertTrue(optionalErrorDetails.isPresent(), "Error details should be provided.");
    }

    @Disabled("Security is handled externally, thus 401 and 403 cannot happen.")
    @Test
    @Override
    public void listResourceCandidate401() throws Exception {

    }

    @Disabled("Security is handled externally, thus 401 and 403 cannot happen.")
    @Test
    @Override
    public void listResourceCandidate403() throws Exception {

    }

    @Disabled("Not found is not possible here, will be answered with an empty list instead.")
    @Test
    @Override
    public void listResourceCandidate404() throws Exception {

    }

    @Disabled("Prohibited by the framework.")
    @Test
    @Override
    public void listResourceCandidate405() throws Exception {

    }

    @Disabled("Impossible status.")
    @Test
    @Override
    public void listResourceCandidate409() throws Exception {

    }

    @Override
    public void listResourceCandidate500() throws Exception {

    }

    @ParameterizedTest
    @MethodSource("provideResourceCandidateUpdates")
    public void patchResourceCandidate200(String message, ResourceCandidateUpdateVO resourceCandidateUpdateVO, ResourceCandidateVO expectedResourceCandidate) throws Exception {
        this.message = message;
        this.resourceCandidateUpdateVO = resourceCandidateUpdateVO;
        this.expectedResourceCandidate = expectedResourceCandidate;
        patchResourceCandidate200();
    }

    @Override
    public void patchResourceCandidate200() throws Exception {
        //first create
        ResourceCandidateCreateVO resourceCandidateCreateVO = ResourceCandidateCreateVOTestExample.build().resourceSpecification(null);

        HttpResponse<ResourceCandidateVO> createResponse = callAndCatch(() -> resourceCandidateApiTestClient.createResourceCandidate(resourceCandidateCreateVO));
        assertEquals(HttpStatus.CREATED, createResponse.getStatus(), "The resource function should have been created first.");

        String resourceId = createResponse.body().getId();

        HttpResponse<ResourceCandidateVO> updateResponse = callAndCatch(() -> resourceCandidateApiTestClient.patchResourceCandidate(resourceId, resourceCandidateUpdateVO));
        assertEquals(HttpStatus.OK, updateResponse.getStatus(), message);

        ResourceCandidateVO updatedResourceCandidate = updateResponse.body();
        expectedResourceCandidate.href(resourceId).id(resourceId);

        expectedResourceCandidate.category(null);

        assertEquals(expectedResourceCandidate, updatedResourceCandidate, message);
    }

    private static Stream<Arguments> provideResourceCandidateUpdates() {
        List<Arguments> testEntries = new ArrayList<>();

        ResourceCandidateUpdateVO lifecycleStatusUpdate = ResourceCandidateUpdateVOTestExample.build()
                .resourceSpecification(null)
                .lifecycleStatus("dead");
        ResourceCandidateVO expectedLifecycleStatus = ResourceCandidateVOTestExample.build()
                .resourceSpecification(null)
                .lifecycleStatus("dead");
        testEntries.add(Arguments.of("The lifecycle state should have been updated.", lifecycleStatusUpdate, expectedLifecycleStatus));

        ResourceCandidateUpdateVO descriptionUpdate = ResourceCandidateUpdateVOTestExample.build()
                .resourceSpecification(null)
                .description("new-description");
        ResourceCandidateVO expectedDescriptionUpdate = ResourceCandidateVOTestExample.build()
                .resourceSpecification(null)
                .description("new-description");
        testEntries.add(Arguments.of("The description should have been updated.", descriptionUpdate, expectedDescriptionUpdate));

        ResourceCandidateUpdateVO nameUpdate = ResourceCandidateUpdateVOTestExample.build()
                .resourceSpecification(null)
                .name("new-name");
        ResourceCandidateVO expectedNameUpdate = ResourceCandidateVOTestExample.build()
                .resourceSpecification(null)
                .name("new-name");
        testEntries.add(Arguments.of("The name should have been updated.", nameUpdate, expectedNameUpdate));

        ResourceCandidateUpdateVO versionUpdate = ResourceCandidateUpdateVOTestExample.build()
                .resourceSpecification(null)
                .version("v0.0.2");
        ResourceCandidateVO expectedVersionUpdate = ResourceCandidateVOTestExample.build()
                .resourceSpecification(null)
                .version("v0.0.2");
        testEntries.add(Arguments.of("The version should have been updated.", versionUpdate, expectedVersionUpdate));

        TimePeriodVO timePeriodVO = TimePeriodVOTestExample.build().endDateTime(Instant.now()).startDateTime(Instant.now());
        ResourceCandidateUpdateVO validForUpdate = ResourceCandidateUpdateVOTestExample.build().validFor(timePeriodVO).resourceSpecification(null);
        ResourceCandidateVO expectedValidForUpdate = ResourceCandidateVOTestExample.build().validFor(timePeriodVO).resourceSpecification(null);
        testEntries.add(Arguments.of("The validFor should have been updated.", validForUpdate, expectedValidForUpdate));

        return testEntries.stream();
    }

    @ParameterizedTest
    @MethodSource("provideInvalidUpdates")
    public void patchResourceCandidate400(String message, ResourceCandidateUpdateVO invalidUpdateVO) throws Exception {
        this.message = message;
        this.resourceCandidateUpdateVO = invalidUpdateVO;
        patchResourceCandidate400();
    }

    @Override
    public void patchResourceCandidate400() throws Exception {
        //first create
        ResourceCandidateCreateVO resourceCandidateCreateVO = ResourceCandidateCreateVOTestExample.build().resourceSpecification(null);

        HttpResponse<ResourceCandidateVO> createResponse = callAndCatch(() -> resourceCandidateApiTestClient.createResourceCandidate(resourceCandidateCreateVO));
        assertEquals(HttpStatus.CREATED, createResponse.getStatus(), "The resource function should have been created first.");

        String resourceId = createResponse.body().getId();

        HttpResponse<ResourceCandidateVO> updateResponse = callAndCatch(() -> resourceCandidateApiTestClient.patchResourceCandidate(resourceId, resourceCandidateUpdateVO));
        assertEquals(HttpStatus.BAD_REQUEST, updateResponse.getStatus(), message);

        Optional<ErrorDetails> optionalErrorDetails = updateResponse.getBody(ErrorDetails.class);
        assertTrue(optionalErrorDetails.isPresent(), "Some error details should be present.");
    }

    private static Stream<Arguments> provideInvalidUpdates() {
        List<Arguments> testEntries = new ArrayList<>();

        testEntries.add(Arguments.of("An update with an invalid resource spe ref is not allowed.",
                ResourceCandidateUpdateVOTestExample.build()
                        .resourceSpecification(ResourceSpecificationRefVOTestExample.build())));
        testEntries.add(Arguments.of("An update with an non existent related party is not allowed.",
                ResourceCandidateUpdateVOTestExample.build()
                        .resourceSpecification(ResourceSpecificationRefVOTestExample.build().id("urn:ngsi-ld:resource-specification:non-existent"))));

        testEntries.add(Arguments.of("An update with an invalid category ref is not allowed.",
                ResourceCandidateUpdateVOTestExample.build()
                        .category(List.of(ResourceCategoryRefVOTestExample.build()))));
        testEntries.add(Arguments.of("An update with an non existent category is not allowed.",
                ResourceCandidateUpdateVOTestExample.build()
                        .category(List.of(ResourceCategoryRefVOTestExample.build().id("urn:ngsi-ld:resource-category:non-existent")))));

        return testEntries.stream();
    }

    @Disabled("Security is handled externally, thus 401 and 403 cannot happen.")
    @Test
    @Override
    public void patchResourceCandidate401() throws Exception {

    }

    @Disabled("Security is handled externally, thus 401 and 403 cannot happen.")
    @Test
    @Override
    public void patchResourceCandidate403() throws Exception {

    }

    @Test
    @Override
    public void patchResourceCandidate404() throws Exception {
        ResourceCandidateUpdateVO resourceCandidateUpdateVO = ResourceCandidateUpdateVOTestExample.build();
        assertEquals(
                HttpStatus.NOT_FOUND,
                callAndCatch(() -> resourceCandidateApiTestClient.patchResourceCandidate("urn:ngsi-ld:resource-candidate:not-existent", resourceCandidateUpdateVO)).getStatus(),
                "Non existent resource candidate should not be updated.");
    }

    @Disabled("Prohibited by the framework.")
    @Test
    @Override
    public void patchResourceCandidate405() throws Exception {

    }

    @Override
    public void patchResourceCandidate409() throws Exception {
        // TODO: can this happen?
    }

    @Override
    public void patchResourceCandidate500() throws Exception {

    }

    @Test
    @Override
    public void retrieveResourceCandidate200() throws Exception {

        ResourceCandidateCreateVO resourceCandidateCreateVO = ResourceCandidateCreateVOTestExample.build().resourceSpecification(null);
        HttpResponse<ResourceCandidateVO> createResponse = callAndCatch(() -> resourceCandidateApiTestClient.createResourceCandidate(resourceCandidateCreateVO));
        assertEquals(HttpStatus.CREATED, createResponse.getStatus(), "The productSpecification should have been created first.");
        String id = createResponse.body().getId();

        ResourceCandidateVO expectedResourceCandidateVO = ResourceCandidateVOTestExample.build();
        expectedResourceCandidateVO
                .id(id)
                .href(id)
                .resourceSpecification(null)
                .category(null);

        //then retrieve
        HttpResponse<ResourceCandidateVO> retrievedRF = callAndCatch(() -> resourceCandidateApiTestClient.retrieveResourceCandidate(id, null));
        assertEquals(HttpStatus.OK, retrievedRF.getStatus(), "The retrieval should be ok.");
        assertEquals(expectedResourceCandidateVO, retrievedRF.body(), "The correct resource function should be returned.");
    }

    @Disabled("400 cannot happen, only 404")
    @Test
    @Override
    public void retrieveResourceCandidate400() throws Exception {

    }

    @Disabled("Security is handled externally, thus 401 and 403 cannot happen.")
    @Test
    @Override
    public void retrieveResourceCandidate401() throws Exception {

    }

    @Disabled("Security is handled externally, thus 401 and 403 cannot happen.")
    @Test
    @Override
    public void retrieveResourceCandidate403() throws Exception {

    }

    @Test
    @Override
    public void retrieveResourceCandidate404() throws Exception {
        HttpResponse<ResourceCandidateVO> response = callAndCatch(() -> resourceCandidateApiTestClient.retrieveResourceCandidate("urn:ngsi-ld:resource-function:non-existent", null));
        assertEquals(HttpStatus.NOT_FOUND, response.getStatus(), "No such resource-candidate should exist.");

        Optional<ErrorDetails> optionalErrorDetails = response.getBody(ErrorDetails.class);
        assertTrue(optionalErrorDetails.isPresent(), "Error details should have been provided.");
    }

    @Disabled("Prohibited by the framework.")
    @Test
    @Override
    public void retrieveResourceCandidate405() throws Exception {

    }

    @Disabled("Conflict not possible on retrieval")
    @Test
    @Override
    public void retrieveResourceCandidate409() throws Exception {

    }

    @Override
    public void retrieveResourceCandidate500() throws Exception {

    }
}
