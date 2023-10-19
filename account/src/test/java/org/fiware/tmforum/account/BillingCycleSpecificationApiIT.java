package org.fiware.tmforum.account;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import io.micronaut.test.annotation.MockBean;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import org.fiware.account.model.*;
import org.fiware.ngsi.api.EntitiesApiClient;
import org.fiware.account.api.BillingCycleSpecificationApiTestClient;
import org.fiware.account.api.BillingCycleSpecificationApiTestSpec;
import org.fiware.tmforum.common.notification.EventHandler;
import org.fiware.tmforum.common.configuration.GeneralProperties;
import org.fiware.tmforum.common.exception.ErrorDetails;
import org.fiware.tmforum.common.test.AbstractApiIT;
import org.fiware.tmforum.account.domain.BillingCycleSpecification;
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
public class BillingCycleSpecificationApiIT extends AbstractApiIT implements BillingCycleSpecificationApiTestSpec {

    public final BillingCycleSpecificationApiTestClient billingCycleSpecificationApiTestClient;

    private String message;
    private BillingCycleSpecificationCreateVO billingCycleSpecificationCreateVO;
    private BillingCycleSpecificationUpdateVO billingCycleSpecificationUpdateVO;
    private BillingCycleSpecificationVO expectedBillingCycleSpecification;

    public BillingCycleSpecificationApiIT(BillingCycleSpecificationApiTestClient billingCycleSpecificationApiTestClient, EntitiesApiClient entitiesApiClient,
                        ObjectMapper objectMapper, GeneralProperties generalProperties) {
        super(entitiesApiClient, objectMapper, generalProperties);
        this.billingCycleSpecificationApiTestClient = billingCycleSpecificationApiTestClient;
    }
    
    @Override
    protected String getEntityType() {
        return BillingCycleSpecification.TYPE_BILLCL;
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
    @MethodSource("provideValidBillingCycleSpecifications")
    public void createBillingCycleSpecification201(String message, BillingCycleSpecificationCreateVO billingCycleSpecificationCreateVO, BillingCycleSpecificationVO expectedBillingCycleSpecification)
            throws Exception {
        this.message = message;
        this.billingCycleSpecificationCreateVO = billingCycleSpecificationCreateVO;
        this.expectedBillingCycleSpecification = expectedBillingCycleSpecification;
        createBillingCycleSpecification201();
    }

    @Override
    public void createBillingCycleSpecification201() throws Exception {

        HttpResponse<BillingCycleSpecificationVO> billingCycleSpecificationVOHttpResponse = callAndCatch(
                () -> billingCycleSpecificationApiTestClient.createBillingCycleSpecification(billingCycleSpecificationCreateVO));
        assertEquals(HttpStatus.CREATED, billingCycleSpecificationVOHttpResponse.getStatus(), message);
        String billingCycleSpecificationId = billingCycleSpecificationVOHttpResponse.body().getId();
        expectedBillingCycleSpecification.setId(billingCycleSpecificationId);
        expectedBillingCycleSpecification.setHref(billingCycleSpecificationId);

        assertEquals(expectedBillingCycleSpecification, billingCycleSpecificationVOHttpResponse.body(), message);


    }

    private static Stream<Arguments> provideValidBillingCycleSpecifications() {
        List<Arguments> testEntries = new ArrayList<>();

        BillingCycleSpecificationCreateVO billingCycleSpecificationCreateVO = BillingCycleSpecificationCreateVOTestExample.build();
        BillingCycleSpecificationVO expectedBillingCycleSpecification = BillingCycleSpecificationVOTestExample.build();
        testEntries.add(Arguments.of("An empty billingCycleSpecification should have been created.", billingCycleSpecificationCreateVO, expectedBillingCycleSpecification));

        return testEntries.stream();
    }

    @Disabled("Cannot add invalid references, there isn't one")
    @Test
    @Override
    public void createBillingCycleSpecification400() throws Exception {
    }

    @Disabled
    @Test
    @Override
    public void createBillingCycleSpecification401() throws Exception {

    }

    @Disabled
    @Test
    @Override
    public void createBillingCycleSpecification403() throws Exception {

    }

    @Disabled
    @Test
    @Override
    public void createBillingCycleSpecification405() throws Exception {

    }

    @Disabled
    @Test
    @Override
    public void createBillingCycleSpecification409() throws Exception {

    }

    @Override
    public void createBillingCycleSpecification500() throws Exception {

    }

    @Test
    @Override
    public void deleteBillingCycleSpecification204() throws Exception {
        //first create
        BillingCycleSpecificationCreateVO billingCycleSpecificationCreateVO = BillingCycleSpecificationCreateVOTestExample.build();
        HttpResponse<BillingCycleSpecificationVO> createResponse = callAndCatch(
                () -> billingCycleSpecificationApiTestClient.createBillingCycleSpecification(billingCycleSpecificationCreateVO));
        assertEquals(HttpStatus.CREATED, createResponse.getStatus(), "The billingCycleSpecification should have been created first.");

        String billingCycleSpecificationId = createResponse.body().getId();

        assertEquals(HttpStatus.NO_CONTENT,
                callAndCatch(() -> billingCycleSpecificationApiTestClient.deleteBillingCycleSpecification(billingCycleSpecificationId)).getStatus(),
                "The billingCycleSpecification should have been deleted.");

        assertEquals(HttpStatus.NOT_FOUND,
                callAndCatch(() -> billingCycleSpecificationApiTestClient.retrieveBillingCycleSpecification(billingCycleSpecificationId, null)).status(),
                "The billingCycleSpecification should not exist anymore.");

    }

    @Disabled
    @Test
    @Override
    public void deleteBillingCycleSpecification400() throws Exception {

    }

    @Disabled
    @Test
    @Override
    public void deleteBillingCycleSpecification401() throws Exception {

    }

    @Disabled
    @Test
    @Override
    public void deleteBillingCycleSpecification403() throws Exception {

    }

    @Test
    @Override
    public void deleteBillingCycleSpecification404() throws Exception {
        HttpResponse<?> notFoundResponse = callAndCatch(
                () -> billingCycleSpecificationApiTestClient.deleteBillingCycleSpecification("urn:ngsi-ld:billingCycleSpecification:no-billingCycleSpecification"));
        assertEquals(HttpStatus.NOT_FOUND,
                notFoundResponse.getStatus(),
                "No such billingCycleSpecification should exist.");

        Optional<ErrorDetails> optionalErrorDetails = notFoundResponse.getBody(ErrorDetails.class);
        assertTrue(optionalErrorDetails.isPresent(), "Error details should be provided.");

        notFoundResponse = callAndCatch(() -> billingCycleSpecificationApiTestClient.deleteBillingCycleSpecification("invalid-id"));
        assertEquals(HttpStatus.NOT_FOUND,
                notFoundResponse.getStatus(),
                "No such billingCycleSpecification should exist.");

        optionalErrorDetails = notFoundResponse.getBody(ErrorDetails.class);
        assertTrue(optionalErrorDetails.isPresent(), "Error details should be provided.");
    }

    @Disabled
    @Test
    @Override
    public void deleteBillingCycleSpecification405() throws Exception {

    }

    @Disabled
    @Test
    @Override
    public void deleteBillingCycleSpecification409() throws Exception {

    }

    @Override
    public void deleteBillingCycleSpecification500() throws Exception {

    }

    @Test
    @Override
    public void listBillingCycleSpecification200() throws Exception {
        List<BillingCycleSpecificationVO> expectedBillingCycleSpecifications = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            BillingCycleSpecificationCreateVO billingCycleSpecificationCreateVO = BillingCycleSpecificationCreateVOTestExample.build();
            String id = billingCycleSpecificationApiTestClient.createBillingCycleSpecification(billingCycleSpecificationCreateVO).body().getId();
            BillingCycleSpecificationVO billingCycleSpecificationVO = BillingCycleSpecificationVOTestExample.build();
            billingCycleSpecificationVO
                    .id(id)
                    .href(id);
            expectedBillingCycleSpecifications.add(billingCycleSpecificationVO);
        }

        HttpResponse<List<BillingCycleSpecificationVO>> billingCycleSpecificationResponse = callAndCatch(
                () -> billingCycleSpecificationApiTestClient.listBillingCycleSpecification(null, null, null));

        assertEquals(HttpStatus.OK, billingCycleSpecificationResponse.getStatus(), "The list should be accessible.");
        assertEquals(expectedBillingCycleSpecifications.size(), billingCycleSpecificationResponse.getBody().get().size(),
                "All billingCycleSpecifications should have been returned.");
        List<BillingCycleSpecificationVO> retrievedBillingCycleSpecifications = billingCycleSpecificationResponse.getBody().get();

        Map<String, BillingCycleSpecificationVO> retrievedMap = retrievedBillingCycleSpecifications.stream()
                .collect(Collectors.toMap(billingCycleSpecification -> billingCycleSpecification.getId(), billingCycleSpecification -> billingCycleSpecification));

        expectedBillingCycleSpecifications.stream()
                .forEach(expectedBillingCycleSpecification -> assertTrue(retrievedMap.containsKey(expectedBillingCycleSpecification.getId()),
                        String.format("All created billingCycleSpecifications should be returned - Missing: %s.", expectedBillingCycleSpecification,
                                retrievedBillingCycleSpecifications)));
        expectedBillingCycleSpecifications.stream().forEach(
                expectedBillingCycleSpecification -> assertEquals(expectedBillingCycleSpecification, retrievedMap.get(expectedBillingCycleSpecification.getId()),
                        "The correct billingCycleSpecifications should be retrieved."));

        // get with pagination
        Integer limit = 5;
        HttpResponse<List<BillingCycleSpecificationVO>> firstPartResponse = callAndCatch(
                () -> billingCycleSpecificationApiTestClient.listBillingCycleSpecification(null, 0, limit));
        assertEquals(limit, firstPartResponse.body().size(),
                "Only the requested number of entries should be returend.");
        HttpResponse<List<BillingCycleSpecificationVO>> secondPartResponse = callAndCatch(
                () -> billingCycleSpecificationApiTestClient.listBillingCycleSpecification(null, 0 + limit, limit));
        assertEquals(limit, secondPartResponse.body().size(),
                "Only the requested number of entries should be returend.");

        retrievedBillingCycleSpecifications.clear();
        retrievedBillingCycleSpecifications.addAll(firstPartResponse.body());
        retrievedBillingCycleSpecifications.addAll(secondPartResponse.body());
        expectedBillingCycleSpecifications.stream()
                .forEach(expectedBillingCycleSpecification -> assertTrue(retrievedMap.containsKey(expectedBillingCycleSpecification.getId()),
                        String.format("All created billingCycleSpecifications should be returned - Missing: %s.", expectedBillingCycleSpecification)));
        expectedBillingCycleSpecifications.stream().forEach(
                expectedBillingCycleSpecification -> assertEquals(expectedBillingCycleSpecification, retrievedMap.get(expectedBillingCycleSpecification.getId()),
                        "The correct billingCycleSpecifications should be retrieved."));
    }

    @Test
    @Override
    public void listBillingCycleSpecification400() throws Exception {
        HttpResponse<List<BillingCycleSpecificationVO>> badRequestResponse = callAndCatch(
                () -> billingCycleSpecificationApiTestClient.listBillingCycleSpecification(null, -1, null));
        assertEquals(HttpStatus.BAD_REQUEST,
                badRequestResponse.getStatus(),
                "Negative offsets are impossible.");

        Optional<ErrorDetails> optionalErrorDetails = badRequestResponse.getBody(ErrorDetails.class);
        assertTrue(optionalErrorDetails.isPresent(), "Error details should be provided.");

        badRequestResponse = callAndCatch(() -> billingCycleSpecificationApiTestClient.listBillingCycleSpecification(null, null, -1));
        assertEquals(HttpStatus.BAD_REQUEST,
                badRequestResponse.getStatus(),
                "Negative limits are impossible.");
        optionalErrorDetails = badRequestResponse.getBody(ErrorDetails.class);
        assertTrue(optionalErrorDetails.isPresent(), "Error details should be provided.");

    }

    @Disabled
    @Test
    @Override
    public void listBillingCycleSpecification401() throws Exception {

    }

    @Disabled
    @Test
    @Override
    public void listBillingCycleSpecification403() throws Exception {

    }

    @Disabled
    @Test
    @Override
    public void listBillingCycleSpecification404() throws Exception {

    }

    @Disabled
    @Test
    @Override
    public void listBillingCycleSpecification405() throws Exception {

    }

    @Disabled
    @Test
    @Override
    public void listBillingCycleSpecification409() throws Exception {

    }

    @Override
    public void listBillingCycleSpecification500() throws Exception {

    }

    @ParameterizedTest
    @MethodSource("provideBillingCycleSpecificationUpdates")
    public void patchBillingCycleSpecification200(String message, BillingCycleSpecificationUpdateVO billingCycleSpecificationUpdateVO, BillingCycleSpecificationVO expectedBillingCycleSpecification)
            throws Exception {
        this.message = message;
        this.billingCycleSpecificationUpdateVO = billingCycleSpecificationUpdateVO;
        this.expectedBillingCycleSpecification = expectedBillingCycleSpecification;
        patchBillingCycleSpecification200();
    }

    @Override
    public void patchBillingCycleSpecification200() throws Exception {
        //first create
        BillingCycleSpecificationCreateVO billingCycleSpecificationCreateVO = BillingCycleSpecificationCreateVOTestExample.build();
        HttpResponse<BillingCycleSpecificationVO> createResponse = callAndCatch(
                () -> billingCycleSpecificationApiTestClient.createBillingCycleSpecification(billingCycleSpecificationCreateVO));
        assertEquals(HttpStatus.CREATED, createResponse.getStatus(), "The billingCycleSpecification should have been created first.");

        String billingCycleSpecificationId = createResponse.body().getId();

        HttpResponse<BillingCycleSpecificationVO> updateResponse = callAndCatch(
                () -> billingCycleSpecificationApiTestClient.patchBillingCycleSpecification(billingCycleSpecificationId, billingCycleSpecificationUpdateVO));
        assertEquals(HttpStatus.OK, updateResponse.getStatus(), message);

        BillingCycleSpecificationVO updatedBillingCycleSpecification = updateResponse.body();
        expectedBillingCycleSpecification.setHref(billingCycleSpecificationId);
        expectedBillingCycleSpecification.setId(billingCycleSpecificationId);

        assertEquals(expectedBillingCycleSpecification, updatedBillingCycleSpecification, message);
    }

    private static Stream<Arguments> provideBillingCycleSpecificationUpdates() {
        List<Arguments> testEntries = new ArrayList<>();

        BillingCycleSpecificationUpdateVO newBillingDateShift = BillingCycleSpecificationUpdateVOTestExample.build();
        newBillingDateShift.setBillingDateShift(10);
        BillingCycleSpecificationVO expectedNewBillingDateShift = BillingCycleSpecificationVOTestExample.build();
        expectedNewBillingDateShift.setBillingDateShift(10);
        testEntries.add(Arguments.of("The billing date shift should have been updated.", newBillingDateShift, expectedNewBillingDateShift));

        BillingCycleSpecificationUpdateVO newBillingPeriod = BillingCycleSpecificationUpdateVOTestExample.build();
        newBillingPeriod.setBillingPeriod("New billingPeriod");
        BillingCycleSpecificationVO expectedNewBillingPeriod = BillingCycleSpecificationVOTestExample.build();
        expectedNewBillingPeriod.setBillingPeriod("New billingPeriod");
        testEntries.add(Arguments.of("The billingPeriod should have been updated.", newBillingPeriod, expectedNewBillingPeriod));

        BillingCycleSpecificationUpdateVO newChargeDateOffset = BillingCycleSpecificationUpdateVOTestExample.build();
        newChargeDateOffset.setChargeDateOffset(10);
        BillingCycleSpecificationVO expectedChargeDateOffset = BillingCycleSpecificationVOTestExample.build();
        expectedChargeDateOffset.setChargeDateOffset(10);
        testEntries.add(Arguments.of("The chargeDateOffset should have been updated.", newChargeDateOffset, expectedChargeDateOffset));

        BillingCycleSpecificationUpdateVO newCreditDateOffset = BillingCycleSpecificationUpdateVOTestExample.build();
        newCreditDateOffset.setCreditDateOffset(10);
        BillingCycleSpecificationVO expectedCreditDateOffset = BillingCycleSpecificationVOTestExample.build();
        expectedCreditDateOffset.setCreditDateOffset(10);
        testEntries.add(Arguments.of("The creditDateOffset should have been updated.", newCreditDateOffset, expectedCreditDateOffset));

        BillingCycleSpecificationUpdateVO newDesc = BillingCycleSpecificationUpdateVOTestExample.build();
        newDesc.setDescription("New description");
        BillingCycleSpecificationVO expectedNewDesc = BillingCycleSpecificationVOTestExample.build();
        expectedNewDesc.setDescription("New description");
        testEntries.add(Arguments.of("The description should have been updated.", newDesc, expectedNewDesc));

        BillingCycleSpecificationUpdateVO newFrequency = BillingCycleSpecificationUpdateVOTestExample.build();
        newFrequency.setFrequency("New frequency");
        BillingCycleSpecificationVO expectedNewFrequency = BillingCycleSpecificationVOTestExample.build();
        expectedNewFrequency.setFrequency("New frequency");
        testEntries.add(Arguments.of("The frequency should have been updated.", newFrequency, expectedNewFrequency));

        BillingCycleSpecificationUpdateVO newMailingDateOffset = BillingCycleSpecificationUpdateVOTestExample.build();
        newMailingDateOffset.setMailingDateOffset(10);
        BillingCycleSpecificationVO expectedMailingDateOffset = BillingCycleSpecificationVOTestExample.build();
        expectedMailingDateOffset.setMailingDateOffset(10);
        testEntries.add(Arguments.of("The mailingDateOffset should have been updated.", newMailingDateOffset, expectedMailingDateOffset));

        BillingCycleSpecificationUpdateVO newName = BillingCycleSpecificationUpdateVOTestExample.build();
        newName.setName("New name");
        BillingCycleSpecificationVO expectedNewName = BillingCycleSpecificationVOTestExample.build();
        expectedNewName.setName("New name");
        testEntries.add(Arguments.of("The name should have been updated.", newName, expectedNewName));

        BillingCycleSpecificationUpdateVO newPaymentDateOffset = BillingCycleSpecificationUpdateVOTestExample.build();
        newPaymentDateOffset.setPaymentDueDateOffset(10);
        BillingCycleSpecificationVO expectedPaymentDateOffset = BillingCycleSpecificationVOTestExample.build();
        expectedPaymentDateOffset.setPaymentDueDateOffset(10);
        testEntries.add(Arguments.of("The paymentDueDateOffset should have been updated.", newPaymentDateOffset, expectedPaymentDateOffset));

        BillingCycleSpecificationUpdateVO newValidFor = BillingCycleSpecificationUpdateVOTestExample.build();
        TimePeriodVO timePeriodVO = TimePeriodVOTestExample.build();
        timePeriodVO.setEndDateTime(Instant.now());
        timePeriodVO.setStartDateTime(Instant.now());
        newValidFor.setValidFor(timePeriodVO);
        BillingCycleSpecificationVO expectedNewValidFor = BillingCycleSpecificationVOTestExample.build();
        expectedNewValidFor.setValidFor(timePeriodVO);
        testEntries.add(Arguments.of("The validFor should have been updated.", newValidFor, expectedNewValidFor));

        return testEntries.stream();
    }

    @Disabled //No puede haber un 400 porque no hay referencia a otras clases
    @Test
    @Override
    public void patchBillingCycleSpecification400() throws Exception {

    }

    @Disabled
    @Test
    @Override
    public void patchBillingCycleSpecification401() throws Exception {

    }

    @Disabled
    @Test
    @Override
    public void patchBillingCycleSpecification403() throws Exception {

    }

    @Test
    @Override
    public void patchBillingCycleSpecification404() throws Exception {
        BillingCycleSpecificationUpdateVO billingCycleSpecificationUpdateVO = BillingCycleSpecificationUpdateVOTestExample.build();
        assertEquals(
                HttpStatus.NOT_FOUND,
                callAndCatch(() -> billingCycleSpecificationApiTestClient.patchBillingCycleSpecification("urn:ngsi-ld:billingCycleSpecification:not-existent",
                        billingCycleSpecificationUpdateVO)).getStatus(),
                "Non existent billingCycleSpecifications should not be updated.");
    }

    @Disabled
    @Test
    @Override
    public void patchBillingCycleSpecification405() throws Exception {

    }

    @Disabled
    @Test
    @Override
    public void patchBillingCycleSpecification409() throws Exception {

    }

    @Override
    public void patchBillingCycleSpecification500() throws Exception {

    }

    @Test
    @Override
    public void retrieveBillingCycleSpecification200() throws Exception {

        //first create
        BillingCycleSpecificationCreateVO billingCycleSpecificationCreateVO = BillingCycleSpecificationCreateVOTestExample.build();
        HttpResponse<BillingCycleSpecificationVO> createResponse = callAndCatch(
                () -> billingCycleSpecificationApiTestClient.createBillingCycleSpecification(billingCycleSpecificationCreateVO));
        assertEquals(HttpStatus.CREATED, createResponse.getStatus(), "The billingCycleSpecification should have been created first.");
        String id = createResponse.body().getId();

        BillingCycleSpecificationVO expectedBillingCycleSpecification = BillingCycleSpecificationVOTestExample.build();
        expectedBillingCycleSpecification.setId(id);
        expectedBillingCycleSpecification.setHref(id);

        //then retrieve
        HttpResponse<BillingCycleSpecificationVO> retrievedBillingCycleSpecification = callAndCatch(() -> billingCycleSpecificationApiTestClient.retrieveBillingCycleSpecification(id, null));
        assertEquals(HttpStatus.OK, retrievedBillingCycleSpecification.getStatus(), "The retrieval should be ok.");
        assertEquals(expectedBillingCycleSpecification, retrievedBillingCycleSpecification.body(), "The correct billingCycleSpecification should be returned.");
    }

    @Disabled
    @Test
    @Override
    public void retrieveBillingCycleSpecification400() throws Exception {

    }

    @Disabled
    @Test
    @Override
    public void retrieveBillingCycleSpecification401() throws Exception {

    }

    @Disabled
    @Test
    @Override
    public void retrieveBillingCycleSpecification403() throws Exception {

    }

    @Test
    @Override
    public void retrieveBillingCycleSpecification404() throws Exception {
        HttpResponse<BillingCycleSpecificationVO> response = callAndCatch(
                () -> billingCycleSpecificationApiTestClient.retrieveBillingCycleSpecification("urn:ngsi-ld:billingCycleSpecification:non-existent", null));
        assertEquals(HttpStatus.NOT_FOUND, response.getStatus(), "No such billingCycleSpecification should exist.");

        Optional<ErrorDetails> optionalErrorDetails = response.getBody(ErrorDetails.class);
        assertTrue(optionalErrorDetails.isPresent(), "Error details should have been provided.");
    }

    @Disabled
    @Test
    @Override
    public void retrieveBillingCycleSpecification405() throws Exception {

    }

    @Disabled
    @Test
    @Override
    public void retrieveBillingCycleSpecification409() throws Exception {

    }

    @Override
    public void retrieveBillingCycleSpecification500() throws Exception {

    }

}
