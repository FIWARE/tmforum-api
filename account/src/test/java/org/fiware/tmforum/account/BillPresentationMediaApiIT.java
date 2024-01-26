package org.fiware.tmforum.account;


import com.fasterxml.jackson.databind.ObjectMapper;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.fiware.ngsi.api.EntitiesApiClient;
import io.micronaut.test.annotation.MockBean;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import org.fiware.account.api.BillPresentationMediaApiTestSpec;
import org.fiware.account.api.BillPresentationMediaApiTestClient;
import org.fiware.tmforum.account.domain.BillPresentationMedia;
import org.fiware.tmforum.common.configuration.GeneralProperties;
import org.fiware.tmforum.common.exception.ErrorDetails;
import org.fiware.tmforum.common.notification.EventHandler;
import org.fiware.tmforum.common.test.AbstractApiIT;
import org.fiware.account.model.*;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import reactor.core.publisher.Mono;

import java.time.Instant;
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


@MicronautTest(packages = { "org.fiware.tmforum.account" })
public class BillPresentationMediaApiIT extends AbstractApiIT implements BillPresentationMediaApiTestSpec {

    public final BillPresentationMediaApiTestClient billPresentationMediaApiTestClient;

    private String message;
    private BillPresentationMediaCreateVO billPresentationMediaCreateVO;
    private BillPresentationMediaUpdateVO billPresentationMediaUpdateVO;
    private BillPresentationMediaVO expectedBillPresentationMedia;

    public BillPresentationMediaApiIT(BillPresentationMediaApiTestClient billPresentationMediaApiTestClient, EntitiesApiClient entitiesApiClient,
                           ObjectMapper objectMapper, GeneralProperties generalProperties) {
        super(entitiesApiClient, objectMapper, generalProperties);
        this.billPresentationMediaApiTestClient = billPresentationMediaApiTestClient;
    }

    @Override
    protected String getEntityType() {
        return BillPresentationMedia.TYPE_BILLPM;
    }

    @MockBean(EventHandler.class)
    public EventHandler eventHandler() {
        EventHandler eventHandler = mock(EventHandler.class);

        when(eventHandler.handleCreateEvent(any())).thenReturn(Mono.empty());
        when(eventHandler.handleUpdateEvent(any(), any())).thenReturn(Mono.empty());
        when(eventHandler.handleDeleteEvent(any())).thenReturn(Mono.empty());

        return eventHandler;
    }

    @ParameterizedTest
    @MethodSource("provideValidBillPresentationMedias")
    public void createBillPresentationMedia201(String message, BillPresentationMediaCreateVO billPresentationMediaCreateVO, BillPresentationMediaVO expectedBillPresentationMedia)
            throws Exception {
        this.message = message;
        this.billPresentationMediaCreateVO = billPresentationMediaCreateVO;
        this.expectedBillPresentationMedia = expectedBillPresentationMedia;
        createBillPresentationMedia201();
    }

    @Override
    @Test
    public void createBillPresentationMedia201() throws Exception {

        HttpResponse<BillPresentationMediaVO> billPresentationMediaVOHttpResponse = callAndCatch(
                () -> billPresentationMediaApiTestClient.createBillPresentationMedia(billPresentationMediaCreateVO));
        assertEquals(HttpStatus.CREATED, billPresentationMediaVOHttpResponse.getStatus(), message);
        String billPresentationMediaId = billPresentationMediaVOHttpResponse.body().getId();
        expectedBillPresentationMedia.setId(billPresentationMediaId);
        expectedBillPresentationMedia.setHref(billPresentationMediaId);

        assertEquals(expectedBillPresentationMedia, billPresentationMediaVOHttpResponse.body(), message);


    }

    private static Stream<Arguments> provideValidBillPresentationMedias() {
        List<Arguments> testEntries = new ArrayList<>();

        BillPresentationMediaCreateVO billPresentationMediaCreateVO = BillPresentationMediaCreateVOTestExample.build();
        BillPresentationMediaVO expectedBillPresentationMedia = BillPresentationMediaVOTestExample.build();
        testEntries.add(Arguments.of("An empty billPresentationMedia should have been created.", billPresentationMediaCreateVO, expectedBillPresentationMedia));

        return testEntries.stream();
    }

    @Disabled("Cannot add invalid references, there isn't one")
    @Test
    @Override
    public void createBillPresentationMedia400() throws Exception {

    }

    @Disabled
    @Test
    @Override
    public void createBillPresentationMedia401() throws Exception {

    }

    @Disabled
    @Override
    public void createBillPresentationMedia403() throws Exception {

    }

    @Disabled
    @Override
    public void createBillPresentationMedia405() throws Exception {

    }

    @Disabled
    @Override
    public void createBillPresentationMedia409() throws Exception {

    }

    @Override
    public void createBillPresentationMedia500() throws Exception {

    }

    @Test
    @Override
    public void deleteBillPresentationMedia204() throws Exception {

        BillPresentationMediaCreateVO billPresentationMediaCreateVO = BillPresentationMediaCreateVOTestExample.build();
        HttpResponse<BillPresentationMediaVO> createResponse = callAndCatch(
                () -> billPresentationMediaApiTestClient.createBillPresentationMedia(billPresentationMediaCreateVO));
        assertEquals(HttpStatus.CREATED, createResponse.getStatus(), "The billPresentationMedia should have been created first.");

        String billPresentationMediaId = createResponse.body().getId();

        assertEquals(HttpStatus.NO_CONTENT,
                callAndCatch(() -> billPresentationMediaApiTestClient.deleteBillPresentationMedia(billPresentationMediaId)).getStatus(),
                "The billPresentationMedia should have been deleted.");

        assertEquals(HttpStatus.NOT_FOUND,
                callAndCatch(() -> billPresentationMediaApiTestClient.retrieveBillPresentationMedia(billPresentationMediaId, null)).status(),
                "The billPresentationMedia should not exist anymore.");

    }

    @Disabled
    @Override
    public void deleteBillPresentationMedia400() throws Exception {

    }

    @Disabled
    @Override
    public void deleteBillPresentationMedia401() throws Exception {

    }

    @Disabled
    @Override
    public void deleteBillPresentationMedia403() throws Exception {

    }

    @Test
    @Override
    public void deleteBillPresentationMedia404() throws Exception {
        HttpResponse<?> notFoundResponse = callAndCatch(
                () -> billPresentationMediaApiTestClient.deleteBillPresentationMedia("urn:ngsi-ld:billPresentationMedia:no-billPresentationMedia"));
        assertEquals(HttpStatus.NOT_FOUND,
                notFoundResponse.getStatus(),
                "No such billPresentationMedia should exist.");

        Optional<ErrorDetails> optionalErrorDetails = notFoundResponse.getBody(ErrorDetails.class);
        assertTrue(optionalErrorDetails.isPresent(), "Error details should be provided.");

        notFoundResponse = callAndCatch(() -> billPresentationMediaApiTestClient.deleteBillPresentationMedia("invalid-id"));
        assertEquals(HttpStatus.NOT_FOUND,
                notFoundResponse.getStatus(),
                "No such billPresentationMedia should exist.");

        optionalErrorDetails = notFoundResponse.getBody(ErrorDetails.class);
        assertTrue(optionalErrorDetails.isPresent(), "Error details should be provided.");
    }

    @Disabled
    @Override
    public void deleteBillPresentationMedia405() throws Exception {

    }

    @Disabled
    @Override
    public void deleteBillPresentationMedia409() throws Exception {

    }

    @Override
    public void deleteBillPresentationMedia500() throws Exception {

    }

    @Test
    @Override
    public void listBillPresentationMedia200() throws Exception {
        List<BillPresentationMediaVO> expectedBillPresentationMedias = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            BillPresentationMediaCreateVO billPresentationMediaCreateVO = BillPresentationMediaCreateVOTestExample.build();
            String id = billPresentationMediaApiTestClient.createBillPresentationMedia(billPresentationMediaCreateVO).body().getId();
            BillPresentationMediaVO billPresentationMediaVO = BillPresentationMediaVOTestExample.build();
            billPresentationMediaVO
                    .id(id)
                    .href(id);
            expectedBillPresentationMedias.add(billPresentationMediaVO);
        }

        HttpResponse<List<BillPresentationMediaVO>> billPresentationMediaResponse = callAndCatch(
                () -> billPresentationMediaApiTestClient.listBillPresentationMedia(null, null, null));

        assertEquals(HttpStatus.OK, billPresentationMediaResponse.getStatus(), "The list should be accessible.");
        assertEquals(expectedBillPresentationMedias.size(), billPresentationMediaResponse.getBody().get().size(),
                "All billPresentationMedias should have been returned.");
        List<BillPresentationMediaVO> retrievedBillPresentationMedias = billPresentationMediaResponse.getBody().get();

        Map<String, BillPresentationMediaVO> retrievedMap = retrievedBillPresentationMedias.stream()
                .collect(Collectors.toMap(billPresentationMedia -> billPresentationMedia.getId(), billPresentationMedia -> billPresentationMedia));

        expectedBillPresentationMedias.stream()
                .forEach(expectedBillPresentationMedia -> assertTrue(retrievedMap.containsKey(expectedBillPresentationMedia.getId()),
                        String.format("All created billPresentationMedias should be returned - Missing: %s.", expectedBillPresentationMedia,
                                retrievedBillPresentationMedias)));
        expectedBillPresentationMedias.stream().forEach(
                expectedBillPresentationMedia -> assertEquals(expectedBillPresentationMedia, retrievedMap.get(expectedBillPresentationMedia.getId()),
                        "The correct billPresentationMedias should be retrieved."));

        // get with pagination
        Integer limit = 5;
        HttpResponse<List<BillPresentationMediaVO>> firstPartResponse = callAndCatch(
                () -> billPresentationMediaApiTestClient.listBillPresentationMedia(null, 0, limit));
        assertEquals(limit, firstPartResponse.body().size(),
                "Only the requested number of entries should be returend.");
        HttpResponse<List<BillPresentationMediaVO>> secondPartResponse = callAndCatch(
                () -> billPresentationMediaApiTestClient.listBillPresentationMedia(null, 0 + limit, limit));
        assertEquals(limit, secondPartResponse.body().size(),
                "Only the requested number of entries should be returend.");

        retrievedBillPresentationMedias.clear();
        retrievedBillPresentationMedias.addAll(firstPartResponse.body());
        retrievedBillPresentationMedias.addAll(secondPartResponse.body());
        expectedBillPresentationMedias.stream()
                .forEach(expectedBillPresentationMedia -> assertTrue(retrievedMap.containsKey(expectedBillPresentationMedia.getId()),
                        String.format("All created billPresentationMedias should be returned - Missing: %s.", expectedBillPresentationMedia)));
        expectedBillPresentationMedias.stream().forEach(
                expectedBillPresentationMedia -> assertEquals(expectedBillPresentationMedia, retrievedMap.get(expectedBillPresentationMedia.getId()),
                        "The correct billPresentationMedias should be retrieved."));
    }

    @Override
    @Test
    public void listBillPresentationMedia400() throws Exception {
        HttpResponse<List<BillPresentationMediaVO>> badRequestResponse = callAndCatch(
                () -> billPresentationMediaApiTestClient.listBillPresentationMedia(null, -1, null));
        assertEquals(HttpStatus.BAD_REQUEST,
                badRequestResponse.getStatus(),
                "Negative offsets are impossible.");

        Optional<ErrorDetails> optionalErrorDetails = badRequestResponse.getBody(ErrorDetails.class);
        assertTrue(optionalErrorDetails.isPresent(), "Error details should be provided.");

        badRequestResponse = callAndCatch(() -> billPresentationMediaApiTestClient.listBillPresentationMedia(null, null, -1));
        assertEquals(HttpStatus.BAD_REQUEST,
                badRequestResponse.getStatus(),
                "Negative limits are impossible.");
        optionalErrorDetails = badRequestResponse.getBody(ErrorDetails.class);
        assertTrue(optionalErrorDetails.isPresent(), "Error details should be provided.");

    }

    @Disabled
    @Override
    public void listBillPresentationMedia401() throws Exception {

    }

    @Disabled
    @Override
    public void listBillPresentationMedia403() throws Exception {

    }

    @Disabled
    @Override
    public void listBillPresentationMedia404() throws Exception {

    }

    @Disabled
    @Override
    public void listBillPresentationMedia405() throws Exception {

    }

    @Disabled
    @Override
    public void listBillPresentationMedia409() throws Exception {

    }

    @Override
    public void listBillPresentationMedia500() throws Exception {

    }

    @ParameterizedTest
    @MethodSource("provideBillPresentationMediaUpdates")
    public void patchBillPresentationMedia200(String message, BillPresentationMediaUpdateVO billPresentationMediaUpdateVO, BillPresentationMediaVO expectedBillPresentationMedia)
            throws Exception {
        this.message = message;
        this.billPresentationMediaUpdateVO = billPresentationMediaUpdateVO;
        this.expectedBillPresentationMedia = expectedBillPresentationMedia;
        patchBillPresentationMedia200();
    }

    @Override
    public void patchBillPresentationMedia200() throws Exception {
        //first create
        BillPresentationMediaCreateVO billPresentationMediaCreateVO = BillPresentationMediaCreateVOTestExample.build();
        HttpResponse<BillPresentationMediaVO> createResponse = callAndCatch(
                () -> billPresentationMediaApiTestClient.createBillPresentationMedia(billPresentationMediaCreateVO));
        assertEquals(HttpStatus.CREATED, createResponse.getStatus(), "The billPresentationMedia should have been created first.");

        String billPresentationMediaId = createResponse.body().getId();

        HttpResponse<BillPresentationMediaVO> updateResponse = callAndCatch(
                () -> billPresentationMediaApiTestClient.patchBillPresentationMedia(billPresentationMediaId, billPresentationMediaUpdateVO));
        assertEquals(HttpStatus.OK, updateResponse.getStatus(), message);

        BillPresentationMediaVO updatedBillPresentationMedia = updateResponse.body();
        expectedBillPresentationMedia.setHref(billPresentationMediaId);
        expectedBillPresentationMedia.setId(billPresentationMediaId);
        assertEquals(expectedBillPresentationMedia, updatedBillPresentationMedia, message);
    }

    private static Stream<Arguments> provideBillPresentationMediaUpdates() {
        List<Arguments> testEntries = new ArrayList<>();

        BillPresentationMediaUpdateVO newDesc = BillPresentationMediaUpdateVOTestExample.build();
        newDesc.setDescription("New description");
        BillPresentationMediaVO expectedNewDesc = BillPresentationMediaVOTestExample.build();
        expectedNewDesc.setDescription("New description");
        testEntries.add(Arguments.of("The description should have been updated.", newDesc, expectedNewDesc));

        BillPresentationMediaUpdateVO newName = BillPresentationMediaUpdateVOTestExample.build();
        newName.setName("New name");
        BillPresentationMediaVO expectedNewName = BillPresentationMediaVOTestExample.build();
        expectedNewName.setName("New name");
        testEntries.add(Arguments.of("The name should have been updated.", newName, expectedNewName));

        return testEntries.stream();
    }

    @Disabled
    @ParameterizedTest
    @MethodSource("provideInvalidUpdates")
    public void patchBillPresentationMedia400(String message, BillPresentationMediaUpdateVO invalidUpdateVO) throws Exception {
        this.message = message;
        this.billPresentationMediaUpdateVO = invalidUpdateVO;
        patchBillPresentationMedia400();
    }

    @Override
    public void patchBillPresentationMedia400() throws Exception {

    }

    @Disabled
    @Override
    public void patchBillPresentationMedia401() throws Exception {

    }

    @Disabled
    @Override
    public void patchBillPresentationMedia403() throws Exception {

    }

    @Override
    public void patchBillPresentationMedia404() throws Exception {
        BillPresentationMediaUpdateVO billPresentationMediaUpdateVO = BillPresentationMediaUpdateVOTestExample.build();
        assertEquals(
                HttpStatus.NOT_FOUND,
                callAndCatch(() -> billPresentationMediaApiTestClient.patchBillPresentationMedia("urn:ngsi-ld:billPresentationMedia:not-existent",
                        billPresentationMediaUpdateVO)).getStatus(),
                "Non existent billPresentationMedias should not be updated.");
    }

    @Disabled
    @Override
    public void patchBillPresentationMedia405() throws Exception {

    }

    @Disabled
    @Override
    public void patchBillPresentationMedia409() throws Exception {

    }

    @Override
    public void patchBillPresentationMedia500() throws Exception {

    }

    @Test
    @Override
    public void retrieveBillPresentationMedia200() throws Exception {
        //first create
        BillPresentationMediaCreateVO billPresentationMediaCreateVO = BillPresentationMediaCreateVOTestExample.build();
        HttpResponse<BillPresentationMediaVO> createResponse = callAndCatch(
                () -> billPresentationMediaApiTestClient.createBillPresentationMedia(billPresentationMediaCreateVO));
        assertEquals(HttpStatus.CREATED, createResponse.getStatus(), "The billPresentationMedia should have been created first.");
        String id = createResponse.body().getId();

        BillPresentationMediaVO expectedBillPresentationMedia = BillPresentationMediaVOTestExample.build();
        expectedBillPresentationMedia.setId(id);
        expectedBillPresentationMedia.setHref(id);

        //then retrieve
        HttpResponse<BillPresentationMediaVO> retrievedBillPresentationMedia = callAndCatch(() -> billPresentationMediaApiTestClient.retrieveBillPresentationMedia(id, null));
        assertEquals(HttpStatus.OK, retrievedBillPresentationMedia.getStatus(), "The retrieval should be ok.");
        assertEquals(expectedBillPresentationMedia, retrievedBillPresentationMedia.body(), "The correct billPresentationMedia should be returned.");
    }

    @Disabled
    @Override
    public void retrieveBillPresentationMedia400() throws Exception {

    }

    @Disabled
    @Override
    public void retrieveBillPresentationMedia401() throws Exception {

    }

    @Disabled
    @Override
    public void retrieveBillPresentationMedia403() throws Exception {

    }

    @Test
    @Override
    public void retrieveBillPresentationMedia404() throws Exception {
        HttpResponse<BillPresentationMediaVO> response = callAndCatch(
                () -> billPresentationMediaApiTestClient.retrieveBillPresentationMedia("urn:ngsi-ld:billPresentationMedia:non-existent", null));
        assertEquals(HttpStatus.NOT_FOUND, response.getStatus(), "No such billPresentationMedia should exist.");

        Optional<ErrorDetails> optionalErrorDetails = response.getBody(ErrorDetails.class);
        assertTrue(optionalErrorDetails.isPresent(), "Error details should have been provided.");
    }

    @Disabled
    @Override
    public void retrieveBillPresentationMedia405() throws Exception {

    }

    @Disabled
    @Override
    public void retrieveBillPresentationMedia409() throws Exception {

    }

    @Override
    public void retrieveBillPresentationMedia500() throws Exception {

    }

}
