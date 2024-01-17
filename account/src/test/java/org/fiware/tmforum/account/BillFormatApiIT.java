package org.fiware.tmforum.account;


import com.fasterxml.jackson.databind.ObjectMapper;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import io.micronaut.test.annotation.MockBean;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import org.fiware.account.api.BillFormatApiTestClient;
import org.fiware.account.api.BillFormatApiTestSpec;
import org.fiware.account.model.*;
import org.fiware.ngsi.api.EntitiesApiClient;
import org.fiware.tmforum.account.domain.BillFormat;
import org.fiware.tmforum.common.configuration.GeneralProperties;
import org.fiware.tmforum.common.exception.ErrorDetails;
import org.fiware.tmforum.common.notification.EventHandler;
import org.fiware.tmforum.common.test.AbstractApiIT;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import reactor.core.publisher.Mono;

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
public class BillFormatApiIT extends AbstractApiIT implements BillFormatApiTestSpec {

    public final BillFormatApiTestClient billFormatApiTestClient;

    private String message;
    private BillFormatCreateVO billFormatCreateVO;
    private BillFormatUpdateVO billFormatUpdateVO;
    private BillFormatVO expectedBillFormat;

    public BillFormatApiIT(BillFormatApiTestClient billFormatApiTestClient, EntitiesApiClient entitiesApiClient,
                        ObjectMapper objectMapper, GeneralProperties generalProperties) {
        super(entitiesApiClient, objectMapper, generalProperties);
        this.billFormatApiTestClient = billFormatApiTestClient;
    }

    @Override
    protected String getEntityType() {
        return BillFormat.TYPE_BILLF;
    }

    @MockBean(EventHandler.class)
    public EventHandler eventHandler() {
        EventHandler eventHandler = mock(EventHandler.class);

        when(eventHandler.handleCreateEvent(any())).thenReturn(Mono.empty());
        when(eventHandler.handleUpdateEvent(any(), any())).thenReturn(Mono.empty());

        return eventHandler;
    }

    @ParameterizedTest
    @MethodSource("provideValidBillFormats")
    public void createBillFormat201(String message, BillFormatCreateVO billFormatCreateVO, BillFormatVO expectedBillFormat)
            throws Exception {
        this.message = message;
        this.billFormatCreateVO = billFormatCreateVO;
        this.expectedBillFormat = expectedBillFormat;
        createBillFormat201();
    }
    
    @Override
    public void createBillFormat201() throws Exception {

        HttpResponse<BillFormatVO> billFormatVOHttpResponse = callAndCatch(
                () -> billFormatApiTestClient.createBillFormat(billFormatCreateVO));
        assertEquals(HttpStatus.CREATED, billFormatVOHttpResponse.getStatus(), message);
        String billFormatId = billFormatVOHttpResponse.body().getId();
        expectedBillFormat.setId(billFormatId);
        expectedBillFormat.setHref(billFormatId);

        assertEquals(expectedBillFormat, billFormatVOHttpResponse.body(), message);


    }

    private static Stream<Arguments> provideValidBillFormats() {
        List<Arguments> testEntries = new ArrayList<>();

        BillFormatCreateVO billFormatCreateVO = BillFormatCreateVOTestExample.build();
        BillFormatVO expectedBillFormat = BillFormatVOTestExample.build();
        testEntries.add(Arguments.of("An empty billFormat should have been created.", billFormatCreateVO, expectedBillFormat));

        return testEntries.stream();
    }

    @Disabled("Cannot add invalid references, there isn't one")
    @Test
    @Override
    public void createBillFormat400() throws Exception {
    }


    @Disabled
    @Test
    @Override
    public void createBillFormat401() throws Exception {

    }

    @Disabled
    @Override
    public void createBillFormat403() throws Exception {

    }

    @Disabled
    @Override
    public void createBillFormat405() throws Exception {

    }

    @Disabled
    @Override
    public void createBillFormat409() throws Exception {

    }

    @Override
    public void createBillFormat500() throws Exception {

    }

    @Test
    @Override
    public void deleteBillFormat204() throws Exception {

        BillFormatCreateVO billFormatCreateVO = BillFormatCreateVOTestExample.build();
        HttpResponse<BillFormatVO> createResponse = callAndCatch(
                () -> billFormatApiTestClient.createBillFormat(billFormatCreateVO));
        assertEquals(HttpStatus.CREATED, createResponse.getStatus(), "The billFormat should have been created first.");

        String billFormatId = createResponse.body().getId();

        assertEquals(HttpStatus.NO_CONTENT,
                callAndCatch(() -> billFormatApiTestClient.deleteBillFormat(billFormatId)).getStatus(),
                "The billFormat should have been deleted.");

        assertEquals(HttpStatus.NOT_FOUND,
                callAndCatch(() -> billFormatApiTestClient.retrieveBillFormat(billFormatId, null)).status(),
                "The billFormat should not exist anymore.");
        
    }

    @Disabled
    @Override
    public void deleteBillFormat400() throws Exception {

    }

    @Disabled
    @Override
    public void deleteBillFormat401() throws Exception {

    }

    @Disabled
    @Override
    public void deleteBillFormat403() throws Exception {

    }

    @Test
    @Override
    public void deleteBillFormat404() throws Exception {
        HttpResponse<?> notFoundResponse = callAndCatch(
                () -> billFormatApiTestClient.deleteBillFormat("urn:ngsi-ld:billFormat:no-billFormat"));
        assertEquals(HttpStatus.NOT_FOUND,
                notFoundResponse.getStatus(),
                "No such billFormat should exist.");

        Optional<ErrorDetails> optionalErrorDetails = notFoundResponse.getBody(ErrorDetails.class);
        assertTrue(optionalErrorDetails.isPresent(), "Error details should be provided.");

        notFoundResponse = callAndCatch(() -> billFormatApiTestClient.deleteBillFormat("invalid-id"));
        assertEquals(HttpStatus.NOT_FOUND,
                notFoundResponse.getStatus(),
                "No such billFormat should exist.");

        optionalErrorDetails = notFoundResponse.getBody(ErrorDetails.class);
        assertTrue(optionalErrorDetails.isPresent(), "Error details should be provided.");
    }

    @Disabled
    @Override
    public void deleteBillFormat405() throws Exception {

    }

    @Disabled
    @Override
    public void deleteBillFormat409() throws Exception {

    }

    @Override
    public void deleteBillFormat500() throws Exception {

    }

    @Test
    @Override
    public void listBillFormat200() throws Exception {
        List<BillFormatVO> expectedBillFormats = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            BillFormatCreateVO billFormatCreateVO = BillFormatCreateVOTestExample.build();
            String id = billFormatApiTestClient.createBillFormat(billFormatCreateVO).body().getId();
            BillFormatVO billFormatVO = BillFormatVOTestExample.build();
            billFormatVO
                    .id(id)
                    .href(id);
            expectedBillFormats.add(billFormatVO);
        }

        HttpResponse<List<BillFormatVO>> billFormatResponse = callAndCatch(
                () -> billFormatApiTestClient.listBillFormat(null, null, null));

        assertEquals(HttpStatus.OK, billFormatResponse.getStatus(), "The list should be accessible.");
        assertEquals(expectedBillFormats.size(), billFormatResponse.getBody().get().size(),
                "All billFormats should have been returned.");
        List<BillFormatVO> retrievedBillFormats = billFormatResponse.getBody().get();

        Map<String, BillFormatVO> retrievedMap = retrievedBillFormats.stream()
                .collect(Collectors.toMap(billFormat -> billFormat.getId(), billFormat -> billFormat));

        expectedBillFormats.stream()
                .forEach(expectedBillFormat -> assertTrue(retrievedMap.containsKey(expectedBillFormat.getId()),
                        String.format("All created billFormats should be returned - Missing: %s.", expectedBillFormat,
                                retrievedBillFormats)));
        expectedBillFormats.stream().forEach(
                expectedBillFormat -> assertEquals(expectedBillFormat, retrievedMap.get(expectedBillFormat.getId()),
                        "The correct billFormats should be retrieved."));

        // get with pagination
        Integer limit = 5;
        HttpResponse<List<BillFormatVO>> firstPartResponse = callAndCatch(
                () -> billFormatApiTestClient.listBillFormat(null, 0, limit));
        assertEquals(limit, firstPartResponse.body().size(),
                "Only the requested number of entries should be returend.");
        HttpResponse<List<BillFormatVO>> secondPartResponse = callAndCatch(
                () -> billFormatApiTestClient.listBillFormat(null, 0 + limit, limit));
        assertEquals(limit, secondPartResponse.body().size(),
                "Only the requested number of entries should be returend.");

        retrievedBillFormats.clear();
        retrievedBillFormats.addAll(firstPartResponse.body());
        retrievedBillFormats.addAll(secondPartResponse.body());
        expectedBillFormats.stream()
                .forEach(expectedBillFormat -> assertTrue(retrievedMap.containsKey(expectedBillFormat.getId()),
                        String.format("All created billFormats should be returned - Missing: %s.", expectedBillFormat)));
        expectedBillFormats.stream().forEach(
                expectedBillFormat -> assertEquals(expectedBillFormat, retrievedMap.get(expectedBillFormat.getId()),
                        "The correct billFormats should be retrieved."));
    }

    @Override
    public void listBillFormat400() throws Exception {
        HttpResponse<List<BillFormatVO>> badRequestResponse = callAndCatch(
                () -> billFormatApiTestClient.listBillFormat(null, -1, null));
        assertEquals(HttpStatus.BAD_REQUEST,
                badRequestResponse.getStatus(),
                "Negative offsets are impossible.");

        Optional<ErrorDetails> optionalErrorDetails = badRequestResponse.getBody(ErrorDetails.class);
        assertTrue(optionalErrorDetails.isPresent(), "Error details should be provided.");

        badRequestResponse = callAndCatch(() -> billFormatApiTestClient.listBillFormat(null, null, -1));
        assertEquals(HttpStatus.BAD_REQUEST,
                badRequestResponse.getStatus(),
                "Negative limits are impossible.");
        optionalErrorDetails = badRequestResponse.getBody(ErrorDetails.class);
        assertTrue(optionalErrorDetails.isPresent(), "Error details should be provided.");

    }

    @Disabled
    @Override
    public void listBillFormat401() throws Exception {

    }

    @Disabled
    @Override
    public void listBillFormat403() throws Exception {

    }

    @Disabled
    @Override
    public void listBillFormat404() throws Exception {

    }

    @Disabled
    @Override
    public void listBillFormat405() throws Exception {

    }

    @Disabled
    @Override
    public void listBillFormat409() throws Exception {

    }

    @Override
    public void listBillFormat500() throws Exception {

    }

    @ParameterizedTest
    @MethodSource("provideBillFormatUpdates")
    public void patchBillFormat200(String message, BillFormatUpdateVO billFormatUpdateVO, BillFormatVO expectedBillFormat)
            throws Exception {
        this.message = message;
        this.billFormatUpdateVO = billFormatUpdateVO;
        this.expectedBillFormat = expectedBillFormat;
        patchBillFormat200();
    }
    
    @Override
    public void patchBillFormat200() throws Exception {
        //first create
        BillFormatCreateVO billFormatCreateVO = BillFormatCreateVOTestExample.build();
        HttpResponse<BillFormatVO> createResponse = callAndCatch(
                () -> billFormatApiTestClient.createBillFormat(billFormatCreateVO));
        assertEquals(HttpStatus.CREATED, createResponse.getStatus(), "The billFormat should have been created first.");

        String billFormatId = createResponse.body().getId();

        HttpResponse<BillFormatVO> updateResponse = callAndCatch(
                () -> billFormatApiTestClient.patchBillFormat(billFormatId, billFormatUpdateVO));
        assertEquals(HttpStatus.OK, updateResponse.getStatus(), message);

        BillFormatVO updatedBillFormat = updateResponse.body();
        expectedBillFormat.setHref(billFormatId);
        expectedBillFormat.setId(billFormatId);
        assertEquals(expectedBillFormat, updatedBillFormat, message);
    }

    private static Stream<Arguments> provideBillFormatUpdates() {
        List<Arguments> testEntries = new ArrayList<>();

        BillFormatUpdateVO newDesc = BillFormatUpdateVOTestExample.build();
        newDesc.setDescription("New description");
        BillFormatVO expectedNewDesc = BillFormatVOTestExample.build();
        expectedNewDesc.setDescription("New description");
        testEntries.add(Arguments.of("The description should have been updated.", newDesc, expectedNewDesc));

        BillFormatUpdateVO newName = BillFormatUpdateVOTestExample.build();
        newName.setName("New name");
        BillFormatVO expectedNewName = BillFormatVOTestExample.build();
        expectedNewName.setName("New name");
        testEntries.add(Arguments.of("The name should have been updated.", newName, expectedNewName));
        
        return testEntries.stream();
    }

    @Disabled
    @ParameterizedTest
    @MethodSource("provideInvalidUpdates")
    public void patchBillFormat400(String message, BillFormatUpdateVO invalidUpdateVO) throws Exception {
        this.message = message;
        this.billFormatUpdateVO = invalidUpdateVO;
        patchBillFormat400();
    }
    
    @Override
    public void patchBillFormat400() throws Exception {

    }

    @Disabled
    @Override
    public void patchBillFormat401() throws Exception {

    }

    @Disabled
    @Override
    public void patchBillFormat403() throws Exception {

    }

    @Override
    public void patchBillFormat404() throws Exception {
        BillFormatUpdateVO billFormatUpdateVO = BillFormatUpdateVOTestExample.build();
        assertEquals(
                HttpStatus.NOT_FOUND,
                callAndCatch(() -> billFormatApiTestClient.patchBillFormat("urn:ngsi-ld:billFormat:not-existent",
                        billFormatUpdateVO)).getStatus(),
                "Non existent billFormats should not be updated.");
    }

    @Disabled
    @Override
    public void patchBillFormat405() throws Exception {

    }

    @Disabled
    @Override
    public void patchBillFormat409() throws Exception {

    }

    @Override
    public void patchBillFormat500() throws Exception {

    }

    @Test
    @Override
    public void retrieveBillFormat200() throws Exception {
        //first create
        BillFormatCreateVO billFormatCreateVO = BillFormatCreateVOTestExample.build();
        HttpResponse<BillFormatVO> createResponse = callAndCatch(
                () -> billFormatApiTestClient.createBillFormat(billFormatCreateVO));
        assertEquals(HttpStatus.CREATED, createResponse.getStatus(), "The billFormat should have been created first.");
        String id = createResponse.body().getId();

        BillFormatVO expectedBillFormat = BillFormatVOTestExample.build();
        expectedBillFormat.setId(id);
        expectedBillFormat.setHref(id);

        //then retrieve
        HttpResponse<BillFormatVO> retrievedBillFormat = callAndCatch(() -> billFormatApiTestClient.retrieveBillFormat(id, null));
        assertEquals(HttpStatus.OK, retrievedBillFormat.getStatus(), "The retrieval should be ok.");
        assertEquals(expectedBillFormat, retrievedBillFormat.body(), "The correct billFormat should be returned.");
    }

    @Disabled
    @Override
    public void retrieveBillFormat400() throws Exception {

    }

    @Disabled
    @Override
    public void retrieveBillFormat401() throws Exception {

    }

    @Disabled
    @Override
    public void retrieveBillFormat403() throws Exception {

    }

    @Test
    @Override
    public void retrieveBillFormat404() throws Exception {
        HttpResponse<BillFormatVO> response = callAndCatch(
                () -> billFormatApiTestClient.retrieveBillFormat("urn:ngsi-ld:billFormat:non-existent", null));
        assertEquals(HttpStatus.NOT_FOUND, response.getStatus(), "No such billFormat should exist.");

        Optional<ErrorDetails> optionalErrorDetails = response.getBody(ErrorDetails.class);
        assertTrue(optionalErrorDetails.isPresent(), "Error details should have been provided.");
    }

    @Disabled
    @Override
    public void retrieveBillFormat405() throws Exception {

    }

    @Disabled
    @Override
    public void retrieveBillFormat409() throws Exception {

    }

    @Override
    public void retrieveBillFormat500() throws Exception {

    }

}
