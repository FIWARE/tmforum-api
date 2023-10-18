package org.fiware.tmforum.account;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import io.micronaut.test.annotation.MockBean;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import org.fiware.account.api.SettlementAccountApiTestClient;
import org.fiware.account.api.SettlementAccountApiTestSpec;
import org.fiware.account.model.*;
import org.fiware.ngsi.api.EntitiesApiClient;
import org.fiware.tmforum.account.domain.BillStructure;
import org.fiware.tmforum.account.domain.SettlementAccount;
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
public class SettlementAccountApiIT extends AbstractApiIT implements SettlementAccountApiTestSpec {

    public final SettlementAccountApiTestClient settlementAccountApiTestClient;

    private String message;
    private SettlementAccountCreateVO settlementAccountCreateVO;
    private SettlementAccountUpdateVO settlementAccountUpdateVO;
    private SettlementAccountVO expectedSettlementAccount;

    public SettlementAccountApiIT(SettlementAccountApiTestClient settlementAccountApiTestClient, EntitiesApiClient entitiesApiClient,
                             ObjectMapper objectMapper, GeneralProperties generalProperties) {
        super(entitiesApiClient, objectMapper, generalProperties);
        this.settlementAccountApiTestClient = settlementAccountApiTestClient;
    }

    @Override
    protected String getEntityType() {
        return SettlementAccount.TYPE_PARTYAC;
    }

    @MockBean(EventHandler.class)
    public EventHandler eventHandler() {
        EventHandler eventHandler = mock(EventHandler.class);

        when(eventHandler.handleCreateEvent(any())).thenReturn(Mono.empty());
        when(eventHandler.handleUpdateEvent(any(), any())).thenReturn(Mono.empty());
        when(eventHandler.handleDeleteEvent(any())).thenReturn(Mono.empty());

        return eventHandler;
    }

    private static void fixExampleCreate(SettlementAccountCreateVO settlementAccount) {
        // fix the example
        BillingCycleSpecificationRefOrValueVO billingCycleSpecificationRefOrValueVO = BillingCycleSpecificationRefOrValueVOTestExample.build();
        billingCycleSpecificationRefOrValueVO.setHref("http://my-ref.de");
        billingCycleSpecificationRefOrValueVO.setId("http://my-url.de");

        BillFormatRefOrValueVO billFormatRefOrValueVO = BillFormatRefOrValueVOTestExample.build();
        billFormatRefOrValueVO.setHref("http://my-ref.de");
        billFormatRefOrValueVO.setId("http://my-url.de");

        BillStructureVO billStructure = BillStructureVOTestExample.build();
        billStructure.setCycleSpecification(billingCycleSpecificationRefOrValueVO);
        billStructure.setFormat(billFormatRefOrValueVO);
        settlementAccount.setBillStructure(billStructure);
        // igual o podo meter nunha función a parte
    }

    private static void fixExampleUpdate(SettlementAccountUpdateVO settlementAccount) {
        // fix the example
        BillingCycleSpecificationRefOrValueVO billingCycleSpecificationRefOrValueVO = BillingCycleSpecificationRefOrValueVOTestExample.build();
        billingCycleSpecificationRefOrValueVO.setHref("http://my-ref.de");
        billingCycleSpecificationRefOrValueVO.setId("http://my-url.de");

        BillFormatRefOrValueVO billFormatRefOrValueVO = BillFormatRefOrValueVOTestExample.build();
        billFormatRefOrValueVO.setHref("http://my-ref.de");
        billFormatRefOrValueVO.setId("http://my-url.de");

        BillStructureVO billStructure = BillStructureVOTestExample.build();
        billStructure.setCycleSpecification(billingCycleSpecificationRefOrValueVO);
        billStructure.setFormat(billFormatRefOrValueVO);
        settlementAccount.setBillStructure(billStructure);
        // igual o podo meter nunha función a parte
    }

    private static void fixExampleExpected(SettlementAccountVO settlementAccount) {
        // fix the example
        BillingCycleSpecificationRefOrValueVO billingCycleSpecificationRefOrValueVO = BillingCycleSpecificationRefOrValueVOTestExample.build();
        billingCycleSpecificationRefOrValueVO.setHref("http://my-ref.de");
        billingCycleSpecificationRefOrValueVO.setId("http://my-url.de");

        BillFormatRefOrValueVO billFormatRefOrValueVO = BillFormatRefOrValueVOTestExample.build();
        billFormatRefOrValueVO.setHref("http://my-ref.de");
        billFormatRefOrValueVO.setId("http://my-url.de");

        BillStructureVO billStructure = BillStructureVOTestExample.build();
        billStructure.setCycleSpecification(billingCycleSpecificationRefOrValueVO);
        billStructure.setFormat(billFormatRefOrValueVO);
        settlementAccount.setBillStructure(billStructure);
        // igual o podo meter nunha función a parte
    }

    //Preguntar o tema do do Checking Mono do Controller, porque non me ten sentido

    @ParameterizedTest
    @MethodSource("provideValidSettlementAccounts")
    public void createSettlementAccount201(String message, SettlementAccountCreateVO settlementAccountCreateVO, SettlementAccountVO expectedSettlementAccount)
            throws Exception {
        this.message = message;
        this.settlementAccountCreateVO = settlementAccountCreateVO;
        this.expectedSettlementAccount = expectedSettlementAccount;
        createSettlementAccount201();
    }

    @Override
    public void createSettlementAccount201() throws Exception {

        HttpResponse<SettlementAccountVO> settlementAccountVOHttpResponse = callAndCatch(
                () -> settlementAccountApiTestClient.createSettlementAccount(settlementAccountCreateVO));
        assertEquals(HttpStatus.CREATED, settlementAccountVOHttpResponse.getStatus(), message);
        String settlementAccountId = settlementAccountVOHttpResponse.body().getId();
        expectedSettlementAccount.setId(settlementAccountId);
        expectedSettlementAccount.setHref(settlementAccountId);

        assertEquals(expectedSettlementAccount, settlementAccountVOHttpResponse.body(), message);


    }

    private static Stream<Arguments> provideValidSettlementAccounts() {
        List<Arguments> testEntries = new ArrayList<>();

        SettlementAccountCreateVO settlementAccountCreateVO = SettlementAccountCreateVOTestExample.build().defaultPaymentMethod(null).financialAccount(null);
        SettlementAccountVO expectedSettlementAccount = SettlementAccountVOTestExample.build().defaultPaymentMethod(null).financialAccount(null);

        fixExampleCreate(settlementAccountCreateVO);
        fixExampleExpected(expectedSettlementAccount);

        testEntries.add(Arguments.of("An empty settlementAccount should have been created.", settlementAccountCreateVO, expectedSettlementAccount));

        return testEntries.stream();
    }

    @ParameterizedTest
    @MethodSource("provideInvalidSettlementAccounts")
    public void createSettlementAccount400(String message, SettlementAccountCreateVO invalidCreateVO) throws Exception {
        this.message = message;
        this.settlementAccountCreateVO = invalidCreateVO;
        createSettlementAccount400();
    }

    @Override
    public void createSettlementAccount400() throws Exception {
        HttpResponse<SettlementAccountVO> creationResponse = callAndCatch(
                () -> settlementAccountApiTestClient.createSettlementAccount(settlementAccountCreateVO));
        assertEquals(HttpStatus.BAD_REQUEST, creationResponse.getStatus(), message);
        Optional<ErrorDetails> optionalErrorDetails = creationResponse.getBody(ErrorDetails.class);
        assertTrue(optionalErrorDetails.isPresent(), "Error details should be provided.");
    }

    private static Stream<Arguments> provideInvalidSettlementAccounts() {
        List<Arguments> testEntries = new ArrayList<>();

        SettlementAccountCreateVO invalidRelatedPartyCreate = SettlementAccountCreateVOTestExample.build()
                .defaultPaymentMethod(null)
                .financialAccount(null);
        // no valid id
        RelatedPartyVO invalidRelatedParty = RelatedPartyVOTestExample.build();
        invalidRelatedPartyCreate.setRelatedParty(List.of(invalidRelatedParty));
        testEntries.add(Arguments.of("A settlementAccount with invalid related parties should not be created.",
                invalidRelatedPartyCreate));

        SettlementAccountCreateVO nonExistentRelatedPartyCreate = SettlementAccountCreateVOTestExample.build()
                .defaultPaymentMethod(null)
                .financialAccount(null);
        // no existent id
        RelatedPartyVO nonExistentRelatedParty = RelatedPartyVOTestExample.build();
        nonExistentRelatedParty.setId("urn:ngsi-ld:individual:non-existent");
        nonExistentRelatedPartyCreate.setRelatedParty(List.of(nonExistentRelatedParty));
        testEntries.add(Arguments.of("A settlementAccount with non-existent related parties should not be created.",
                nonExistentRelatedPartyCreate));

        SettlementAccountCreateVO nonExistentDefaultPaymentMethodCreate = SettlementAccountCreateVOTestExample.build()
                .financialAccount(null);
        // no existent id
        nonExistentDefaultPaymentMethodCreate.setRelatedParty(List.of(nonExistentRelatedParty));
        testEntries.add(Arguments.of("A settlementAccount with non-existent related parties should not be created.",
                nonExistentDefaultPaymentMethodCreate));

        SettlementAccountCreateVO nonExistentFinancialAccountCreate = SettlementAccountCreateVOTestExample.build()
                .defaultPaymentMethod(null);
        // no existent id
        nonExistentFinancialAccountCreate.setRelatedParty(List.of(nonExistentRelatedParty));
        testEntries.add(Arguments.of("A settlementAccount with non-existent related parties should not be created.",
                nonExistentFinancialAccountCreate));

        fixExampleCreate(invalidRelatedPartyCreate);
        fixExampleCreate(nonExistentRelatedPartyCreate);

        return testEntries.stream();
    }

    @Disabled
    @Test
    @Override
    public void createSettlementAccount401() throws Exception {

    }

    @Disabled
    @Test
    @Override
    public void createSettlementAccount403() throws Exception {

    }

    @Disabled
    @Test
    @Override
    public void createSettlementAccount405() throws Exception {

    }

    @Disabled
    @Test
    @Override
    public void createSettlementAccount409() throws Exception {

    }

    @Override
    public void createSettlementAccount500() throws Exception {

    }

    @Test
    @Override
    public void deleteSettlementAccount204() throws Exception {
        //first create
        SettlementAccountCreateVO settlementAccountCreateVO = SettlementAccountCreateVOTestExample.build()
                .defaultPaymentMethod(null)
                .financialAccount(null);

        fixExampleCreate(settlementAccountCreateVO);

        HttpResponse<SettlementAccountVO> createResponse = callAndCatch(
                () -> settlementAccountApiTestClient.createSettlementAccount(settlementAccountCreateVO));
        assertEquals(HttpStatus.CREATED, createResponse.getStatus(), "The settlementAccount should have been created first.");

        String settlementAccountId = createResponse.body().getId();

        assertEquals(HttpStatus.NO_CONTENT,
                callAndCatch(() -> settlementAccountApiTestClient.deleteSettlementAccount(settlementAccountId)).getStatus(),
                "The settlementAccount should have been deleted.");

        assertEquals(HttpStatus.NOT_FOUND,
                callAndCatch(() -> settlementAccountApiTestClient.retrieveSettlementAccount(settlementAccountId, null)).status(),
                "The settlementAccount should not exist anymore.");

    }

    @Disabled
    @Test
    @Override
    public void deleteSettlementAccount400() throws Exception {

    }

    @Disabled
    @Test
    @Override
    public void deleteSettlementAccount401() throws Exception {

    }

    @Disabled
    @Test
    @Override
    public void deleteSettlementAccount403() throws Exception {

    }

    @Test
    @Override
    public void deleteSettlementAccount404() throws Exception {
        HttpResponse<?> notFoundResponse = callAndCatch(
                () -> settlementAccountApiTestClient.deleteSettlementAccount("urn:ngsi-ld:settlementAccount:no-settlementAccount"));
        assertEquals(HttpStatus.NOT_FOUND,
                notFoundResponse.getStatus(),
                "No such settlementAccount should exist.");

        Optional<ErrorDetails> optionalErrorDetails = notFoundResponse.getBody(ErrorDetails.class);
        assertTrue(optionalErrorDetails.isPresent(), "Error details should be provided.");

        notFoundResponse = callAndCatch(() -> settlementAccountApiTestClient.deleteSettlementAccount("invalid-id"));
        assertEquals(HttpStatus.NOT_FOUND,
                notFoundResponse.getStatus(),
                "No such settlementAccount should exist.");

        optionalErrorDetails = notFoundResponse.getBody(ErrorDetails.class);
        assertTrue(optionalErrorDetails.isPresent(), "Error details should be provided.");
    }

    @Disabled
    @Test
    @Override
    public void deleteSettlementAccount405() throws Exception {

    }

    @Disabled
    @Test
    @Override
    public void deleteSettlementAccount409() throws Exception {

    }

    @Override
    public void deleteSettlementAccount500() throws Exception {

    }

    @Test
    @Override
    public void listSettlementAccount200() throws Exception {
        List<SettlementAccountVO> expectedSettlementAccounts = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            SettlementAccountCreateVO settlementAccountCreateVO = SettlementAccountCreateVOTestExample.build()
                    .defaultPaymentMethod(null)
                    .financialAccount(null);
            fixExampleCreate(settlementAccountCreateVO);
            String id = settlementAccountApiTestClient.createSettlementAccount(settlementAccountCreateVO).body().getId();
            SettlementAccountVO settlementAccountVO = SettlementAccountVOTestExample.build();
            fixExampleExpected(settlementAccountVO);
            BillingCycleSpecificationRefOrValueVO billingCycleRV = settlementAccountVO.getBillStructure()
                    .getCycleSpecification().validFor(null);
            BillStructureVO billStructure = settlementAccountVO.getBillStructure()
                    .cycleSpecification(billingCycleRV)
                    .presentationMedia(null);
            settlementAccountVO
                    .id(id)
                    .href(id)
                    .billStructure(billStructure)
                    .defaultPaymentMethod(null)
                    .financialAccount(null)
                    .relatedParty(null);
            expectedSettlementAccounts.add(settlementAccountVO);
        }

        HttpResponse<List<SettlementAccountVO>> settlementAccountResponse = callAndCatch(
                () -> settlementAccountApiTestClient.listSettlementAccount(null, null, null));

        assertEquals(HttpStatus.OK, settlementAccountResponse.getStatus(), "The list should be accessible.");
        assertEquals(expectedSettlementAccounts.size(), settlementAccountResponse.getBody().get().size(),
                "All settlementAccounts should have been returned.");
        List<SettlementAccountVO> retrievedSettlementAccounts = settlementAccountResponse.getBody().get();

        Map<String, SettlementAccountVO> retrievedMap = retrievedSettlementAccounts.stream()
                .collect(Collectors.toMap(settlementAccount -> settlementAccount.getId(), settlementAccount -> settlementAccount));

        expectedSettlementAccounts.stream()
                .forEach(expectedSettlementAccount -> assertTrue(retrievedMap.containsKey(expectedSettlementAccount.getId()),
                        String.format("All created settlementAccounts should be returned - Missing: %s.", expectedSettlementAccount,
                                retrievedSettlementAccounts)));
        expectedSettlementAccounts.stream().forEach(
                expectedSettlementAccount -> assertEquals(expectedSettlementAccount, retrievedMap.get(expectedSettlementAccount.getId()),
                        "The correct settlementAccounts should be retrieved."));

        // get with pagination
        Integer limit = 5;
        HttpResponse<List<SettlementAccountVO>> firstPartResponse = callAndCatch(
                () -> settlementAccountApiTestClient.listSettlementAccount(null, 0, limit));
        assertEquals(limit, firstPartResponse.body().size(),
                "Only the requested number of entries should be returend.");
        HttpResponse<List<SettlementAccountVO>> secondPartResponse = callAndCatch(
                () -> settlementAccountApiTestClient.listSettlementAccount(null, 0 + limit, limit));
        assertEquals(limit, secondPartResponse.body().size(),
                "Only the requested number of entries should be returend.");

        retrievedSettlementAccounts.clear();
        retrievedSettlementAccounts.addAll(firstPartResponse.body());
        retrievedSettlementAccounts.addAll(secondPartResponse.body());
        expectedSettlementAccounts.stream()
                .forEach(expectedSettlementAccount -> assertTrue(retrievedMap.containsKey(expectedSettlementAccount.getId()),
                        String.format("All created settlementAccounts should be returned - Missing: %s.", expectedSettlementAccount)));
        expectedSettlementAccounts.stream().forEach(
                expectedSettlementAccount -> assertEquals(expectedSettlementAccount, retrievedMap.get(expectedSettlementAccount.getId()),
                        "The correct settlementAccounts should be retrieved."));
    }

    @Test
    @Override
    public void listSettlementAccount400() throws Exception {
        HttpResponse<List<SettlementAccountVO>> badRequestResponse = callAndCatch(
                () -> settlementAccountApiTestClient.listSettlementAccount(null, -1, null));
        assertEquals(HttpStatus.BAD_REQUEST,
                badRequestResponse.getStatus(),
                "Negative offsets are impossible.");

        Optional<ErrorDetails> optionalErrorDetails = badRequestResponse.getBody(ErrorDetails.class);
        assertTrue(optionalErrorDetails.isPresent(), "Error details should be provided.");

        badRequestResponse = callAndCatch(() -> settlementAccountApiTestClient.listSettlementAccount(null, null, -1));
        assertEquals(HttpStatus.BAD_REQUEST,
                badRequestResponse.getStatus(),
                "Negative limits are impossible.");
        optionalErrorDetails = badRequestResponse.getBody(ErrorDetails.class);
        assertTrue(optionalErrorDetails.isPresent(), "Error details should be provided.");

    }

    @Disabled
    @Test
    @Override
    public void listSettlementAccount401() throws Exception {

    }

    @Disabled
    @Test
    @Override
    public void listSettlementAccount403() throws Exception {

    }

    @Disabled
    @Test
    @Override
    public void listSettlementAccount404() throws Exception {

    }

    @Disabled
    @Test
    @Override
    public void listSettlementAccount405() throws Exception {

    }

    @Disabled
    @Test
    @Override
    public void listSettlementAccount409() throws Exception {

    }

    @Override
    public void listSettlementAccount500() throws Exception {

    }

    @ParameterizedTest
    @MethodSource("provideSettlementAccountUpdates")
    public void patchSettlementAccount200(String message, SettlementAccountUpdateVO settlementAccountUpdateVO, SettlementAccountVO expectedSettlementAccount)
            throws Exception {
        this.message = message;
        this.settlementAccountUpdateVO = settlementAccountUpdateVO;
        this.expectedSettlementAccount = expectedSettlementAccount;
        patchSettlementAccount200();
    }

    @Override
    public void patchSettlementAccount200() throws Exception {
        //first create
        SettlementAccountCreateVO settlementAccountCreateVO = SettlementAccountCreateVOTestExample.build()
                .financialAccount(null)
                .defaultPaymentMethod(null);
        fixExampleCreate(settlementAccountCreateVO);
        HttpResponse<SettlementAccountVO> createResponse = callAndCatch(
                () -> settlementAccountApiTestClient.createSettlementAccount(settlementAccountCreateVO));
        assertEquals(HttpStatus.CREATED, createResponse.getStatus(), "The settlementAccount should have been created first.");

        String settlementAccountId = createResponse.body().getId();

        HttpResponse<SettlementAccountVO> updateResponse = callAndCatch(
                () -> settlementAccountApiTestClient.patchSettlementAccount(settlementAccountId, settlementAccountUpdateVO));
        assertEquals(HttpStatus.OK, updateResponse.getStatus(), message);

        SettlementAccountVO updatedSettlementAccount = updateResponse.body();
        fixExampleExpected(expectedSettlementAccount);
        expectedSettlementAccount.setHref(settlementAccountId);
        expectedSettlementAccount.setId(settlementAccountId);
        expectedSettlementAccount.setRelatedParty(null);
        expectedSettlementAccount.setFinancialAccount(null);
        expectedSettlementAccount.setDefaultPaymentMethod(null);
        BillingCycleSpecificationRefOrValueVO billingCycleRV = expectedSettlementAccount.getBillStructure()
                .getCycleSpecification().validFor(null);
        BillStructureVO billStructure = expectedSettlementAccount.getBillStructure()
                .cycleSpecification(billingCycleRV)
                .presentationMedia(null);
        expectedSettlementAccount.billStructure(billStructure);

        assertEquals(expectedSettlementAccount, updatedSettlementAccount, message);
    }

    private static Stream<Arguments> provideSettlementAccountUpdates() {
        List<Arguments> testEntries = new ArrayList<>();

        SettlementAccountUpdateVO newTypeSettlementAccount = SettlementAccountUpdateVOTestExample.build()
                .financialAccount(null)
                .defaultPaymentMethod(null);
        fixExampleUpdate(newTypeSettlementAccount);
        newTypeSettlementAccount.setAccountType("New-Type");
        SettlementAccountVO expectedNewType = SettlementAccountVOTestExample.build();
        expectedNewType.setAccountType("New-Type");
        testEntries.add(Arguments.of("The type should have been updated.", newTypeSettlementAccount, expectedNewType));

        SettlementAccountUpdateVO newDesc = SettlementAccountUpdateVOTestExample.build()
                .financialAccount(null)
                .defaultPaymentMethod(null);
        fixExampleUpdate(newDesc);
        newDesc.setDescription("New description");
        SettlementAccountVO expectedNewDesc = SettlementAccountVOTestExample.build();
        expectedNewDesc.setDescription("New description");
        testEntries.add(Arguments.of("The description should have been updated.", newDesc, expectedNewDesc));

        SettlementAccountUpdateVO newName = SettlementAccountUpdateVOTestExample.build()
                .financialAccount(null)
                .defaultPaymentMethod(null);
        fixExampleUpdate(newName);
        newName.setName("New name");
        SettlementAccountVO expectedNewName = SettlementAccountVOTestExample.build();
        expectedNewName.setName("New name");
        testEntries.add(Arguments.of("The name should have been updated.", newName, expectedNewName));

        return testEntries.stream();
    }

    @ParameterizedTest
    @MethodSource("provideInvalidUpdates")
    public void patchSettlementAccount400(String message, SettlementAccountUpdateVO invalidUpdateVO) throws Exception {
        this.message = message;
        this.settlementAccountUpdateVO = invalidUpdateVO;
        patchSettlementAccount400();
    }

    @Override
    public void patchSettlementAccount400() throws Exception {
        //first create
        SettlementAccountCreateVO settlementAccountCreateVO = SettlementAccountCreateVOTestExample.build()
                .financialAccount(null)
                .defaultPaymentMethod(null);
        fixExampleCreate(settlementAccountCreateVO);
        HttpResponse<SettlementAccountVO> createResponse = callAndCatch(
                () -> settlementAccountApiTestClient.createSettlementAccount(settlementAccountCreateVO));
        assertEquals(HttpStatus.CREATED, createResponse.getStatus(), "The settlementAccount should have been created first.");

        String settlementAccountId = createResponse.body().getId();

        HttpResponse<SettlementAccountVO> updateResponse = callAndCatch(
                () -> settlementAccountApiTestClient.patchSettlementAccount(settlementAccountId, settlementAccountUpdateVO));
        assertEquals(HttpStatus.BAD_REQUEST, updateResponse.getStatus(), message);

        Optional<ErrorDetails> optionalErrorDetails = updateResponse.getBody(ErrorDetails.class);
        assertTrue(optionalErrorDetails.isPresent(), "Some error details should be present.");
    }

    private static Stream<Arguments> provideInvalidUpdates() {
        List<Arguments> testEntries = new ArrayList<>();

        SettlementAccountUpdateVO invalidRelatedPartyUpdate = SettlementAccountUpdateVOTestExample.build()
                .financialAccount(null)
                .defaultPaymentMethod(null);
        fixExampleUpdate(invalidRelatedPartyUpdate);
        // no valid id
        RelatedPartyVO invalidRelatedParty = RelatedPartyVOTestExample.build();
        invalidRelatedPartyUpdate.setRelatedParty(List.of(invalidRelatedParty));
        testEntries.add(Arguments.of("A settlementAccount with invalid related parties should not be updated.",
                invalidRelatedPartyUpdate));

        SettlementAccountUpdateVO nonExistentRelatedPartyUpdate = SettlementAccountUpdateVOTestExample.build()
                .financialAccount(null)
                .defaultPaymentMethod(null);
        fixExampleUpdate(nonExistentRelatedPartyUpdate);
        // no existent id
        RelatedPartyVO nonExistentRelatedParty = RelatedPartyVOTestExample.build();
        nonExistentRelatedParty.setId("urn:ngsi-ld:individual:non-existent");
        nonExistentRelatedPartyUpdate.setRelatedParty(List.of(nonExistentRelatedParty));
        testEntries.add(Arguments.of("A settlementAccount with non-existent related parties should not be updated.",
                nonExistentRelatedPartyUpdate));

        SettlementAccountUpdateVO nonExistentFinancialAccountUpdate = SettlementAccountUpdateVOTestExample.build()
                .defaultPaymentMethod(null);
        fixExampleUpdate(nonExistentFinancialAccountUpdate);
        testEntries.add(Arguments.of("A settlementAccount with non-existent Financial Account should not be updated.",
                nonExistentFinancialAccountUpdate));

        SettlementAccountUpdateVO nonExistentDefaultPaymentMethodUpdate = SettlementAccountUpdateVOTestExample.build()
                .financialAccount(null);
        fixExampleUpdate(nonExistentDefaultPaymentMethodUpdate);
        testEntries.add(Arguments.of("A settlementAccount with non-existent default payment method should not be updated.",
                nonExistentDefaultPaymentMethodUpdate));

        return testEntries.stream();
    }

    @Disabled
    @Test
    @Override
    public void patchSettlementAccount401() throws Exception {

    }

    @Disabled
    @Test
    @Override
    public void patchSettlementAccount403() throws Exception {

    }

    @Test
    @Override
    public void patchSettlementAccount404() throws Exception {
        SettlementAccountUpdateVO settlementAccountUpdateVO = SettlementAccountUpdateVOTestExample.build();
        assertEquals(
                HttpStatus.NOT_FOUND,
                callAndCatch(() -> settlementAccountApiTestClient.patchSettlementAccount("urn:ngsi-ld:settlementAccount:not-existent",
                        settlementAccountUpdateVO)).getStatus(),
                "Non existent settlementAccounts should not be updated.");
    }

    @Disabled
    @Test
    @Override
    public void patchSettlementAccount405() throws Exception {

    }

    @Disabled
    @Test
    @Override
    public void patchSettlementAccount409() throws Exception {

    }

    @Override
    public void patchSettlementAccount500() throws Exception {

    }

    @Test
    @Override
    public void retrieveSettlementAccount200() throws Exception {

        //first create
        SettlementAccountCreateVO settlementAccountCreateVO = SettlementAccountCreateVOTestExample.build()
                .financialAccount(null)
                .defaultPaymentMethod(null);
        fixExampleCreate(settlementAccountCreateVO);
        HttpResponse<SettlementAccountVO> createResponse = callAndCatch(
                () -> settlementAccountApiTestClient.createSettlementAccount(settlementAccountCreateVO));
        assertEquals(HttpStatus.CREATED, createResponse.getStatus(), "The settlementAccount should have been created first.");
        String id = createResponse.body().getId();

        SettlementAccountVO expectedSettlementAccount = SettlementAccountVOTestExample.build();
        expectedSettlementAccount.setId(id);
        expectedSettlementAccount.setHref(id);
        // empty list is mapped to null
        expectedSettlementAccount.setFinancialAccount(null);
        expectedSettlementAccount.setDefaultPaymentMethod(null);
        expectedSettlementAccount.setRelatedParty(null);
        fixExampleExpected(expectedSettlementAccount);
        BillingCycleSpecificationRefOrValueVO billingCycleRV = expectedSettlementAccount.getBillStructure()
                .getCycleSpecification().validFor(null);
        BillStructureVO billStructure = expectedSettlementAccount.getBillStructure()
                .cycleSpecification(billingCycleRV)
                .presentationMedia(null);
        expectedSettlementAccount.billStructure(billStructure);

        //then retrieve
        HttpResponse<SettlementAccountVO> retrievedSettlementAccount = callAndCatch(() -> settlementAccountApiTestClient.retrieveSettlementAccount(id, null));
        assertEquals(HttpStatus.OK, retrievedSettlementAccount.getStatus(), "The retrieval should be ok.");
        assertEquals(expectedSettlementAccount, retrievedSettlementAccount.body(), "The correct settlementAccount should be returned.");
    }

    @Disabled
    @Test
    @Override
    public void retrieveSettlementAccount400() throws Exception {

    }

    @Override
    public void retrieveSettlementAccount401() throws Exception {

    }

    @Disabled
    @Test
    @Override
    public void retrieveSettlementAccount403() throws Exception {

    }

    @Test
    @Override
    public void retrieveSettlementAccount404() throws Exception {
        HttpResponse<SettlementAccountVO> response = callAndCatch(
                () -> settlementAccountApiTestClient.retrieveSettlementAccount("urn:ngsi-ld:settlementAccount:non-existent", null));
        assertEquals(HttpStatus.NOT_FOUND, response.getStatus(), "No such settlementAccount should exist.");

        Optional<ErrorDetails> optionalErrorDetails = response.getBody(ErrorDetails.class);
        assertTrue(optionalErrorDetails.isPresent(), "Error details should have been provided.");
    }

    @Disabled
    @Test
    @Override
    public void retrieveSettlementAccount405() throws Exception {

    }

    @Disabled
    @Test
    @Override
    public void retrieveSettlementAccount409() throws Exception {

    }

    @Override
    public void retrieveSettlementAccount500() throws Exception {

    }
}
