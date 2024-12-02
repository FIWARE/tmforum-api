package org.fiware.tmforum.usagemanagement;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import io.micronaut.test.annotation.MockBean;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import org.fiware.ngsi.api.EntitiesApiClient;
import org.fiware.tmforum.common.configuration.GeneralProperties;
import org.fiware.tmforum.common.exception.ErrorDetails;
import org.fiware.tmforum.common.notification.TMForumEventHandler;
import org.fiware.tmforum.common.test.AbstractApiIT;
import org.fiware.tmforum.usagemanagement.domain.Usage;
import org.fiware.usagemanagement.api.UsageApiTestClient;
import org.fiware.usagemanagement.api.UsageApiTestSpec;
import org.fiware.usagemanagement.model.*;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@MicronautTest(packages = {"org.fiware.tmforum.usagemanagement"})
public class UsageApiIT extends AbstractApiIT implements UsageApiTestSpec {

    private final UsageApiTestClient usageApiTestClient;
    private String message;
    private UsageCreateVO usageCreateVO;
    private UsageUpdateVO usageUpdateVO;
    private UsageVO expectedUsage;

    public UsageApiIT(UsageApiTestClient usageApiTestClient, EntitiesApiClient entitiesApiClient,
                      ObjectMapper objectMapper, GeneralProperties generalProperties) {
        super(entitiesApiClient, objectMapper, generalProperties);
        this.usageApiTestClient = usageApiTestClient;
    }

    @MockBean(TMForumEventHandler.class)
    public TMForumEventHandler eventHandler() {
        TMForumEventHandler eventHandler = mock(TMForumEventHandler.class);

        when(eventHandler.handleCreateEvent(any())).thenReturn(Mono.empty());
        when(eventHandler.handleUpdateEvent(any(), any())).thenReturn(Mono.empty());

        return eventHandler;
    }

    @Override
    protected String getEntityType() {
        return Usage.TYPE_U;
    }

    // createUsage

    @ParameterizedTest
    @MethodSource("provideValidUsages")
    public void createUsage201(String message, UsageCreateVO usageCreateVO,
                               UsageVO expectedUsage) throws Exception {

        this.message = message;
        this.usageCreateVO = usageCreateVO;
        this.expectedUsage = expectedUsage;
        createUsage201();

    }

    @Override
    public void createUsage201() throws Exception {

        HttpResponse<UsageVO> usageVOHttpResponse = callAndCatch(
                () -> usageApiTestClient.createUsage(null, usageCreateVO));

        assertEquals(HttpStatus.CREATED, usageVOHttpResponse.getStatus(), message);
        UsageVO createdUsageVO = usageVOHttpResponse.body();
        String usageId = usageVOHttpResponse.body().getId();
        expectedUsage.setId(usageId);
        expectedUsage.setHref(new URI(usageId));
        assertEquals(expectedUsage, createdUsageVO, message);

    }

    private static Stream<Arguments> provideValidUsages() {

        List<Arguments> testEntries = new ArrayList<>();

        UsageCreateVO usageCreateVO = UsageCreateVOTestExample.build().usageSpecification(null);
        UsageVO expectedUsage = UsageVOTestExample.build().usageSpecification(null);
        testEntries.add(
                Arguments.of("Empty usage should have been created.", usageCreateVO, expectedUsage));

        return testEntries.stream();

    }

    @ParameterizedTest
    @MethodSource("provideInvalidUsages")
    public void createUsage400(String message, UsageCreateVO invalidCreateVO
    ) throws Exception {

        this.message = message;
        this.usageCreateVO = invalidCreateVO;
        createUsage400();

    }

    @Override
    public void createUsage400() throws Exception {

        HttpResponse<UsageVO> usageCreateResponse = callAndCatch(
                () -> usageApiTestClient.createUsage(null, usageCreateVO));
        assertEquals(HttpStatus.BAD_REQUEST, usageCreateResponse.getStatus(), message);

        Optional<ErrorDetails> optionalErrorDetails = usageCreateResponse.getBody(ErrorDetails.class);
        assertTrue(optionalErrorDetails.isPresent(), "Error details should be provided.");

    }

    private static Stream<Arguments> provideInvalidUsages() {

        List<Arguments> testEntries = new ArrayList<>();

        UsageCreateVO invalidRelatedPartyCreate = UsageCreateVOTestExample.build();
        // no valid id
        RelatedPartyVO invalidRelatedParty = RelatedPartyVOTestExample.build();
        invalidRelatedPartyCreate.setRelatedParty(List.of(invalidRelatedParty));
        testEntries.add(Arguments.of("A usage with invalid related parties should not be created.",
                invalidRelatedPartyCreate));

        UsageCreateVO nonExistentRelatedPartyCreate = UsageCreateVOTestExample.build();
        // no existent id
        RelatedPartyVO nonExistentRelatedParty = RelatedPartyVOTestExample.build();
        nonExistentRelatedParty.setId("urn:ngsi-ld:usage:non-existent");
        nonExistentRelatedPartyCreate.setRelatedParty(List.of(nonExistentRelatedParty));
        testEntries.add(Arguments.of("A usage with non-existent related parties should not be created.",
                nonExistentRelatedPartyCreate));

        UsageCreateVO invalidUsageCharacteristicCreate = UsageCreateVOTestExample.build();
        // no valid id
        UsageCharacteristicVO usageCharacteristic = UsageCharacteristicVOTestExample.build();
        invalidUsageCharacteristicCreate.setUsageCharacteristic(List.of(usageCharacteristic));
        testEntries.add(
                Arguments.of("A usage with invalid categories should not be created.", invalidUsageCharacteristicCreate));

        UsageCreateVO nonExistentUsageCharacteristicCreate = UsageCreateVOTestExample.build();
        // no existent id
        UsageCharacteristicVO nonExistentUsageCharacteristic = UsageCharacteristicVOTestExample.build();
        nonExistentUsageCharacteristic.setId("urn:ngsi-ld:usage:non-existent");
        nonExistentUsageCharacteristicCreate.setUsageCharacteristic(List.of(nonExistentUsageCharacteristic));
        testEntries.add(Arguments.of("A usage with non-existent categories should not be created.",
                nonExistentUsageCharacteristicCreate));

        return testEntries.stream();

    }

    @Disabled("Security is handled externally, thus 401 and 403 cannot happen.")
    @Test
    @Override
    public void createUsage401() throws Exception {
    }

    @Disabled("Security is handled externally, thus 401 and 403 cannot happen.")
    @Test
    @Override
    public void createUsage403() throws Exception {
    }

    @Disabled("Prohibited by the framework.")
    @Test
    @Override
    public void createUsage405() throws Exception {

    }

    @Disabled("Usage doesn't have 'implicit' entities and id is generated by the implementation, no conflict possible.")
    @Test
    @Override
    public void createUsage409() throws Exception {

    }

    @Override
    public void createUsage500() throws Exception {

    }

    // deleteUsage

    @Test
    @Override
    public void deleteUsage204() throws Exception {
        // first create one
        UsageCreateVO usageCreateVO = UsageCreateVOTestExample.build().usageSpecification(null);
        HttpResponse<UsageVO> usageCreateResponse = callAndCatch(
                () -> usageApiTestClient.createUsage(null, usageCreateVO));
        assertEquals(HttpStatus.CREATED, usageCreateResponse.getStatus(),
                "The Usage should have been created first.");

        String usageId = usageCreateResponse.body().getId();

        assertEquals(HttpStatus.NO_CONTENT,
                callAndCatch(() -> usageApiTestClient.deleteUsage(null, usageId)).getStatus(),
                "The Usage should have been deleted.");

        assertEquals(HttpStatus.NOT_FOUND,
                callAndCatch(() -> usageApiTestClient.retrieveUsage(null, usageId, null)).status(),
                "The Usage should not exist anymore.");
    }

    @Disabled("400 is impossible to happen on deletion with the current implementation.")
    @Test
    @Override
    public void deleteUsage400() throws Exception {

    }

    @Disabled("Security is handled externally, thus 401 and 403 cannot happen.")
    @Test
    @Override
    public void deleteUsage401() throws Exception {
    }

    @Disabled("Security is handled externally, thus 401 and 403 cannot happen.")
    @Test
    @Override
    public void deleteUsage403() throws Exception {
    }

    @Test
    @Override
    public void deleteUsage404() throws Exception {
        HttpResponse<?> notFoundResponse = callAndCatch(
                () -> usageApiTestClient.deleteUsage(null, "urn:ngsi-ld:usage:no-usage"));
        assertEquals(HttpStatus.NOT_FOUND,
                notFoundResponse.getStatus(),
                "No such usage should exist.");

        Optional<ErrorDetails> optionalErrorDetails = notFoundResponse.getBody(ErrorDetails.class);
        assertTrue(optionalErrorDetails.isPresent(), "Error details should be provided.");

        notFoundResponse = callAndCatch(() -> usageApiTestClient.deleteUsage(null, "invalid-id"));
        assertEquals(HttpStatus.NOT_FOUND,
                notFoundResponse.getStatus(),
                "No such usage should exist.");

        optionalErrorDetails = notFoundResponse.getBody(ErrorDetails.class);
        assertTrue(optionalErrorDetails.isPresent(), "Error details should be provided.");
    }

    @Disabled("Prohibited by the framework.")
    @Test
    @Override
    public void deleteUsage405() throws Exception {

    }

    @Disabled("Usage doesn't have 'implicit' entities and id is generated by the implementation, no conflict possible.")
    @Test
    @Override
    public void deleteUsage409() throws Exception {

    }

    @Override
    public void deleteUsage500() throws Exception {

    }

    // listUsage

    @Test
    @Override
    public void listUsage200() throws Exception {

        List<UsageVO> expectedUsages = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            UsageCreateVO usageCreateVO = UsageCreateVOTestExample.build().usageSpecification(null);
            String id = usageApiTestClient.createUsage(null, usageCreateVO).body().getId();
            UsageVO usageVO = UsageVOTestExample.build();
            usageVO
                    .id(id)
                    .href(new URI(id))
                    .usageSpecification(null);
            expectedUsages.add(usageVO);
        }


        HttpResponse<List<UsageVO>> usageResponse = callAndCatch(
                () -> usageApiTestClient.listUsage(null, null, null, null));
        assertEquals(HttpStatus.OK, usageResponse.getStatus(), "The list should be accessible.");
        assertEquals(expectedUsages.size(), usageResponse.getBody().get().size(),
                "All usages should have been returned.");

        List<UsageVO> retrievedUsages = usageResponse.getBody().get();

        Map<String, UsageVO> retrievedMap = retrievedUsages.stream()
                .collect(Collectors.toMap((usage) -> usage.getId(), usage -> usage));

        expectedUsages.stream()
                .forEach(expectedUsage -> assertTrue(retrievedMap.containsKey(expectedUsage.getId()),
                        String.format("All created usages should be returned - Missing: %s.", expectedUsage,
                                retrievedUsages)));
        expectedUsages.stream().forEach(
                expectedUsage -> assertEquals(expectedUsage, retrievedMap.get(expectedUsage.getId()),
                        "The correct usages should be retrieved."));

        // get with pagination
        Integer limit = 5;
        HttpResponse<List<UsageVO>> firstPartResponse = callAndCatch(
                () -> usageApiTestClient.listUsage(null, null, 0, limit));
        assertEquals(limit, firstPartResponse.body().size(),
                "Only the requested number of entries should be returend.");
        HttpResponse<List<UsageVO>> secondPartResponse = callAndCatch(
                () -> usageApiTestClient.listUsage(null, null, 0 + limit, limit));
        assertEquals(limit, secondPartResponse.body().size(),
                "Only the requested number of entries should be returend.");

        retrievedUsages.clear();
        retrievedUsages.addAll(firstPartResponse.body());
        retrievedUsages.addAll(secondPartResponse.body());
        expectedUsages.stream()
                .forEach(expectedUsage -> assertTrue(retrievedMap.containsKey(expectedUsage.getId()),
                        String.format("All created usages should be returned - Missing: %s.", expectedUsage)));
        expectedUsages.stream().forEach(
                expectedUsage -> assertEquals(expectedUsage, retrievedMap.get(expectedUsage.getId()),
                        "The correct usages should be retrieved."));

    }

    @Test
    @Override
    public void listUsage400() throws Exception {
        HttpResponse<List<UsageVO>> badRequestResponse = callAndCatch(
                () -> usageApiTestClient.listUsage(null, null, -1, null));
        assertEquals(HttpStatus.BAD_REQUEST,
                badRequestResponse.getStatus(),
                "Negative offsets are impossible.");

        Optional<ErrorDetails> optionalErrorDetails = badRequestResponse.getBody(ErrorDetails.class);
        assertTrue(optionalErrorDetails.isPresent(), "Error details should be provided.");

        badRequestResponse = callAndCatch(() -> usageApiTestClient.listUsage(null, null, null, -1));
        assertEquals(HttpStatus.BAD_REQUEST,
                badRequestResponse.getStatus(),
                "Negative limits are impossible.");
        optionalErrorDetails = badRequestResponse.getBody(ErrorDetails.class);
        assertTrue(optionalErrorDetails.isPresent(), "Error details should be provided.");

    }

    @Disabled("Security is handled externally, thus 401 and 403 cannot happen.")
    @Test
    @Override
    public void listUsage401() throws Exception {

    }

    @Disabled("Security is handled externally, thus 401 and 403 cannot happen.")
    @Test
    @Override
    public void listUsage403() throws Exception {

    }

    @Disabled("Not found is not possible here, will be answerd with an empty list instead.")
    @Test
    @Override
    public void listUsage404() throws Exception {

    }

    @Disabled("Prohibited by the framework.")
    @Test
    @Override
    public void listUsage405() throws Exception {

    }

    @Disabled("Impossible status.")
    @Test
    @Override
    public void listUsage409() throws Exception {

    }

    @Override
    public void listUsage500() throws Exception {

    }

    // patchUsage


    @ParameterizedTest
    @MethodSource("provideUsageUpdates")
    public void patchUsage200(String message, UsageUpdateVO usageUpdateVO, UsageVO expectedUsage)
            throws Exception {
        this.message = message;
        this.usageUpdateVO = usageUpdateVO;
        this.expectedUsage = expectedUsage;
        patchUsage200();
    }

    @Override
    public void patchUsage200() throws Exception {
        //first create
        UsageCreateVO usageCreateVO = UsageCreateVOTestExample.build().usageSpecification(null);
        HttpResponse<UsageVO> createResponse = callAndCatch(
                () -> usageApiTestClient.createUsage(null, usageCreateVO));
        assertEquals(HttpStatus.CREATED, createResponse.getStatus(), "The usage should have been created first.");

        String usageId = createResponse.body().getId();

        HttpResponse<UsageVO> updateResponse = callAndCatch(
                () -> usageApiTestClient.patchUsage(null, usageId, usageUpdateVO));
        assertEquals(HttpStatus.OK, updateResponse.getStatus(), message);

        UsageVO updatedUsage = updateResponse.body();
        expectedUsage.setId(usageId);
        expectedUsage.setHref(new URI(usageId));

        assertEquals(expectedUsage, updatedUsage, message);
    }

    private static Stream<Arguments> provideUsageUpdates() {

        List<Arguments> testEntries = new ArrayList<>();

        UsageUpdateVO newTypeUsage = UsageUpdateVOTestExample.build().usageSpecification(null);
        newTypeUsage.setUsageType("New-Type");
        UsageVO expectedNewType = UsageVOTestExample.build().usageSpecification(null);
        expectedNewType.setUsageType("New-Type");
        testEntries.add(Arguments.of("The type should have been updated.", newTypeUsage, expectedNewType));

        UsageUpdateVO newDesc = UsageUpdateVOTestExample.build().usageSpecification(null);
        newDesc.setDescription("New description");
        UsageVO expectedNewDesc = UsageVOTestExample.build().usageSpecification(null);
        expectedNewDesc.setDescription("New description");
        testEntries.add(Arguments.of("The description should have been updated.", newDesc, expectedNewDesc));

        return testEntries.stream();
    }

    @ParameterizedTest
    @MethodSource("provideInvalidUpdates")
    public void patchUsage400(String message, UsageUpdateVO usageUpdateVO) throws Exception {
        this.message = message;
        this.usageUpdateVO = usageUpdateVO;
        patchUsage400();
    }

    @Override
    public void patchUsage400() throws Exception {
        //first create
        UsageCreateVO usageCreateVO = UsageCreateVOTestExample.build().usageSpecification(null);
        HttpResponse<UsageVO> createResponse = callAndCatch(
                () -> usageApiTestClient.createUsage(null, usageCreateVO));
        assertEquals(HttpStatus.CREATED, createResponse.getStatus(), "The usage should have been created first.");

        String usageId = createResponse.body().getId();

        HttpResponse<UsageVO> updateResponse = callAndCatch(
                () -> usageApiTestClient.patchUsage(null, usageId, usageUpdateVO));
        assertEquals(HttpStatus.BAD_REQUEST, updateResponse.getStatus(), message);

        Optional<ErrorDetails> optionalErrorDetails = updateResponse.getBody(ErrorDetails.class);
        assertTrue(optionalErrorDetails.isPresent(), "Some error details should be present.");
    }

    private static Stream<Arguments> provideInvalidUpdates() {

        List<Arguments> testEntries = new ArrayList<>();

        UsageUpdateVO invalidRelatedPartyUpdate = UsageUpdateVOTestExample.build();
        // no valid id
        RelatedPartyVO invalidRelatedParty = RelatedPartyVOTestExample.build();
        invalidRelatedPartyUpdate.setRelatedParty(List.of(invalidRelatedParty));
        testEntries.add(Arguments.of("A usage with invalid related parties should not be Updated.",
                invalidRelatedPartyUpdate));

        UsageUpdateVO nonExistentRelatedPartyUpdate = UsageUpdateVOTestExample.build();
        // no existent id
        RelatedPartyVO nonExistentRelatedParty = RelatedPartyVOTestExample.build();
        nonExistentRelatedParty.setId("urn:ngsi-ld:usage:non-existent");
        nonExistentRelatedPartyUpdate.setRelatedParty(List.of(nonExistentRelatedParty));
        testEntries.add(Arguments.of("A usage with non-existent related parties should not be Updated.",
                nonExistentRelatedPartyUpdate));

        UsageUpdateVO invalidUsageCharacteristicUpdate = UsageUpdateVOTestExample.build();
        // no valid id
        UsageCharacteristicVO usageCharacteristic = UsageCharacteristicVOTestExample.build();
        invalidUsageCharacteristicUpdate.setUsageCharacteristic(List.of(usageCharacteristic));
        testEntries.add(
                Arguments.of("A usage with invalid categories should not be Updated.", invalidUsageCharacteristicUpdate));

        UsageUpdateVO nonExistentUsageCharacteristicUpdate = UsageUpdateVOTestExample.build();
        // no existent id
        UsageCharacteristicVO nonExistentUsageCharacteristic = UsageCharacteristicVOTestExample.build();
        nonExistentUsageCharacteristic.setId("urn:ngsi-ld:usage:non-existent");
        nonExistentUsageCharacteristicUpdate.setUsageCharacteristic(List.of(nonExistentUsageCharacteristic));
        testEntries.add(Arguments.of("A usage with non-existent categories should not be Updated.",
                nonExistentUsageCharacteristicUpdate));

        return testEntries.stream();
    }

    @Disabled("Security is handled externally, thus 401 and 403 cannot happen.")
    @Test
    @Override
    public void patchUsage401() throws Exception {

    }

    @Disabled("Security is handled externally, thus 401 and 403 cannot happen.")
    @Test
    @Override
    public void patchUsage403() throws Exception {

    }


    @Test
    @Override
    public void patchUsage404() throws Exception {
        UsageUpdateVO usageUpdateVO = UsageUpdateVOTestExample.build();
        assertEquals(
                HttpStatus.NOT_FOUND,
                callAndCatch(() -> usageApiTestClient.patchUsage(null, "urn:ngsi-ld:usage:not-existent",
                        usageUpdateVO)).getStatus(),
                "Non existent usages should not be updated.");
    }

    @Disabled("Prohibited by the framework.")
    @Test
    @Override
    public void patchUsage405() throws Exception {

    }

    @Disabled("No implicit creations, cannot happen.")
    @Test
    @Override
    public void patchUsage409() throws Exception {

    }

    @Override
    public void patchUsage500() throws Exception {

    }

    // retrieveUsage

    @Test
    @Override
    public void retrieveUsage200() throws Exception {

        //first create
        UsageCreateVO usageCreateVO = UsageCreateVOTestExample.build().usageSpecification(null);
        HttpResponse<UsageVO> createResponse = callAndCatch(
                () -> usageApiTestClient.createUsage(null, usageCreateVO));
        assertEquals(HttpStatus.CREATED, createResponse.getStatus(), "The usage should have been created first.");
        String id = createResponse.body().getId();

        UsageVO expectedUsage = UsageVOTestExample.build();
        expectedUsage.setId(id);
        expectedUsage.setHref(new URI(id));
        expectedUsage.usageSpecification(null);

        //then retrieve
        HttpResponse<UsageVO> retrievedUsage = callAndCatch(() -> usageApiTestClient.retrieveUsage(null, id, null));
        assertEquals(HttpStatus.OK, retrievedUsage.getStatus(), "The retrieval should be ok.");
        assertEquals(expectedUsage, retrievedUsage.body(), "The correct usage should be returned.");
    }

    @Disabled("400 cannot happen, only 404")
    @Test
    @Override
    public void retrieveUsage400() throws Exception {

    }

    @Disabled("Security is handled externally, thus 401 and 403 cannot happen.")
    @Test
    @Override
    public void retrieveUsage401() throws Exception {

    }

    @Disabled("Security is handled externally, thus 401 and 403 cannot happen.")
    @Test
    @Override
    public void retrieveUsage403() throws Exception {
    }

    @Test
    @Override
    public void retrieveUsage404() throws Exception {
        HttpResponse<UsageVO> response = callAndCatch(
                () -> usageApiTestClient.retrieveUsage(null, "urn:ngsi-ld:usage:non-existent", null));
        assertEquals(HttpStatus.NOT_FOUND, response.getStatus(), "No such usage should exist.");

        Optional<ErrorDetails> optionalErrorDetails = response.getBody(ErrorDetails.class);
        assertTrue(optionalErrorDetails.isPresent(), "Error details should have been provided.");
    }

    @Disabled("Prohibited by the framework.")
    @Test
    @Override
    public void retrieveUsage405() throws Exception {

    }

    @Disabled("Conflict not possible on retrieval")
    @Test
    @Override
    public void retrieveUsage409() throws Exception {

    }

    @Override
    public void retrieveUsage500() throws Exception {

    }

}
