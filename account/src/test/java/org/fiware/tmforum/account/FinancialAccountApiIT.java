package org.fiware.tmforum.account;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import io.micronaut.test.annotation.MockBean;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import org.fiware.account.api.FinancialAccountApiTestClient;
import org.fiware.account.api.FinancialAccountApiTestSpec;
import org.fiware.account.model.*;
import org.fiware.ngsi.api.EntitiesApiClient;
import org.fiware.tmforum.account.domain.FinancialAccount;
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
public class FinancialAccountApiIT extends AbstractApiIT implements FinancialAccountApiTestSpec {

    public final FinancialAccountApiTestClient financialAccountApiTestClient;

    private String message;
    private FinancialAccountCreateVO financialAccountCreateVO;
    private FinancialAccountUpdateVO financialAccountUpdateVO;
    private FinancialAccountVO expectedFinancialAccount;

    public FinancialAccountApiIT(FinancialAccountApiTestClient financialAccountApiTestClient, EntitiesApiClient entitiesApiClient,
                           ObjectMapper objectMapper, GeneralProperties generalProperties) {
        super(entitiesApiClient, objectMapper, generalProperties);
        this.financialAccountApiTestClient = financialAccountApiTestClient;
    }

    @Override
    protected String getEntityType() {
        return FinancialAccount.TYPE_FINANCIALAC;
    }

    @MockBean(EventHandler.class)
    public EventHandler eventHandler() {
        EventHandler eventHandler = mock(EventHandler.class);

        when(eventHandler.handleCreateEvent(any())).thenReturn(Mono.empty());
        when(eventHandler.handleUpdateEvent(any(), any())).thenReturn(Mono.empty());

        return eventHandler;
    }

    @ParameterizedTest
    @MethodSource("provideValidFinancialAccounts")
    public void createFinancialAccount201(String message, FinancialAccountCreateVO financialAccountCreateVO, FinancialAccountVO expectedFinancialAccount)
            throws Exception {
        this.message = message;
        this.financialAccountCreateVO = financialAccountCreateVO;
        this.expectedFinancialAccount = expectedFinancialAccount;
        createFinancialAccount201();
    }

    @Override
    public void createFinancialAccount201() throws Exception {

        HttpResponse<FinancialAccountVO> financialAccountVOHttpResponse = callAndCatch(
                () -> financialAccountApiTestClient.createFinancialAccount(financialAccountCreateVO));
        assertEquals(HttpStatus.CREATED, financialAccountVOHttpResponse.getStatus(), message);
        String financialAccountId = financialAccountVOHttpResponse.body().getId();
        expectedFinancialAccount.setId(financialAccountId);
        expectedFinancialAccount.setHref(financialAccountId);

        assertEquals(expectedFinancialAccount, financialAccountVOHttpResponse.body(), message);


    }

    private static Stream<Arguments> provideValidFinancialAccounts() {
        List<Arguments> testEntries = new ArrayList<>();

        FinancialAccountCreateVO financialAccountCreateVO = FinancialAccountCreateVOTestExample.build();
        FinancialAccountVO expectedFinancialAccount = FinancialAccountVOTestExample.build();
        testEntries.add(Arguments.of("An empty financialAccount should have been created.", financialAccountCreateVO, expectedFinancialAccount));

        return testEntries.stream();
    }

    @ParameterizedTest
    @MethodSource("provideInvalidFinancialAccounts")
    public void createFinancialAccount400(String message, FinancialAccountCreateVO invalidCreateVO) throws Exception {
        this.message = message;
        this.financialAccountCreateVO = invalidCreateVO;
        createFinancialAccount400();
    }

    @Override
    public void createFinancialAccount400() throws Exception {
        HttpResponse<FinancialAccountVO> creationResponse = callAndCatch(
                () -> financialAccountApiTestClient.createFinancialAccount(financialAccountCreateVO));
        assertEquals(HttpStatus.BAD_REQUEST, creationResponse.getStatus(), message);
        Optional<ErrorDetails> optionalErrorDetails = creationResponse.getBody(ErrorDetails.class);
        assertTrue(optionalErrorDetails.isPresent(), "Error details should be provided.");
    }

    private static Stream<Arguments> provideInvalidFinancialAccounts() {
        List<Arguments> testEntries = new ArrayList<>();

        FinancialAccountCreateVO invalidRelatedPartyCreate = FinancialAccountCreateVOTestExample.build();
        // no valid id
        RelatedPartyVO invalidRelatedParty = RelatedPartyVOTestExample.build();
        invalidRelatedPartyCreate.setRelatedParty(List.of(invalidRelatedParty));
        testEntries.add(Arguments.of("A financialAccount with invalid related parties should not be created.",
                invalidRelatedPartyCreate));

        FinancialAccountCreateVO nonExistentRelatedPartyCreate = FinancialAccountCreateVOTestExample.build();
        // no existent id
        RelatedPartyVO nonExistentRelatedParty = RelatedPartyVOTestExample.build();
        nonExistentRelatedParty.setId("urn:ngsi-ld:individual:non-existent");
        nonExistentRelatedPartyCreate.setRelatedParty(List.of(nonExistentRelatedParty));
        testEntries.add(Arguments.of("A financialAccount with non-existent related parties should not be created.",
                nonExistentRelatedPartyCreate));

        return testEntries.stream();
    }

    @Disabled
    @Test
    @Override
    public void createFinancialAccount401() throws Exception {

    }

    @Disabled
    @Test
    @Override
    public void createFinancialAccount403() throws Exception {

    }

    @Disabled
    @Test
    @Override
    public void createFinancialAccount405() throws Exception {

    }

    @Disabled
    @Test
    @Override
    public void createFinancialAccount409() throws Exception {

    }

    @Override
    public void createFinancialAccount500() throws Exception {

    }

    @Test
    @Override
    public void deleteFinancialAccount204() throws Exception {
        //first create
        FinancialAccountCreateVO financialAccountCreateVO = FinancialAccountCreateVOTestExample.build();
        HttpResponse<FinancialAccountVO> createResponse = callAndCatch(
                () -> financialAccountApiTestClient.createFinancialAccount(financialAccountCreateVO));
        assertEquals(HttpStatus.CREATED, createResponse.getStatus(), "The financialAccount should have been created first.");

        String financialAccountId = createResponse.body().getId();

        assertEquals(HttpStatus.NO_CONTENT,
                callAndCatch(() -> financialAccountApiTestClient.deleteFinancialAccount(financialAccountId)).getStatus(),
                "The financialAccount should have been deleted.");

        assertEquals(HttpStatus.NOT_FOUND,
                callAndCatch(() -> financialAccountApiTestClient.retrieveFinancialAccount(financialAccountId, null)).status(),
                "The financialAccount should not exist anymore.");

    }

    @Disabled
    @Test
    @Override
    public void deleteFinancialAccount400() throws Exception {

    }

    @Disabled
    @Test
    @Override
    public void deleteFinancialAccount401() throws Exception {

    }

    @Disabled
    @Test
    @Override
    public void deleteFinancialAccount403() throws Exception {

    }

    @Test
    @Override
    public void deleteFinancialAccount404() throws Exception {
        HttpResponse<?> notFoundResponse = callAndCatch(
                () -> financialAccountApiTestClient.deleteFinancialAccount("urn:ngsi-ld:financialAccount:no-financialAccount"));
        assertEquals(HttpStatus.NOT_FOUND,
                notFoundResponse.getStatus(),
                "No such financialAccount should exist.");

        Optional<ErrorDetails> optionalErrorDetails = notFoundResponse.getBody(ErrorDetails.class);
        assertTrue(optionalErrorDetails.isPresent(), "Error details should be provided.");

        notFoundResponse = callAndCatch(() -> financialAccountApiTestClient.deleteFinancialAccount("invalid-id"));
        assertEquals(HttpStatus.NOT_FOUND,
                notFoundResponse.getStatus(),
                "No such financialAccount should exist.");

        optionalErrorDetails = notFoundResponse.getBody(ErrorDetails.class);
        assertTrue(optionalErrorDetails.isPresent(), "Error details should be provided.");
    }

    @Disabled
    @Test
    @Override
    public void deleteFinancialAccount405() throws Exception {

    }

    @Disabled
    @Test
    @Override
    public void deleteFinancialAccount409() throws Exception {

    }

    @Override
    public void deleteFinancialAccount500() throws Exception {

    }

    @Test
    @Override
    public void listFinancialAccount200() throws Exception {
        List<FinancialAccountVO> expectedFinancialAccounts = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            FinancialAccountCreateVO financialAccountCreateVO = FinancialAccountCreateVOTestExample.build();
            String id = financialAccountApiTestClient.createFinancialAccount(financialAccountCreateVO).body().getId();
            FinancialAccountVO financialAccountVO = FinancialAccountVOTestExample.build();
            financialAccountVO
                    .id(id)
                    .href(id)
                    .relatedParty(null);
            expectedFinancialAccounts.add(financialAccountVO);
        }

        HttpResponse<List<FinancialAccountVO>> financialAccountResponse = callAndCatch(
                () -> financialAccountApiTestClient.listFinancialAccount(null, null, null));

        assertEquals(HttpStatus.OK, financialAccountResponse.getStatus(), "The list should be accessible.");
        assertEquals(expectedFinancialAccounts.size(), financialAccountResponse.getBody().get().size(),
                "All financialAccounts should have been returned.");
        List<FinancialAccountVO> retrievedFinancialAccounts = financialAccountResponse.getBody().get();

        Map<String, FinancialAccountVO> retrievedMap = retrievedFinancialAccounts.stream()
                .collect(Collectors.toMap(financialAccount -> financialAccount.getId(), financialAccount -> financialAccount));

        expectedFinancialAccounts.stream()
                .forEach(expectedFinancialAccount -> assertTrue(retrievedMap.containsKey(expectedFinancialAccount.getId()),
                        String.format("All created financialAccounts should be returned - Missing: %s.", expectedFinancialAccount,
                                retrievedFinancialAccounts)));
        expectedFinancialAccounts.stream().forEach(
                expectedFinancialAccount -> assertEquals(expectedFinancialAccount, retrievedMap.get(expectedFinancialAccount.getId()),
                        "The correct financialAccounts should be retrieved."));

        // get with pagination
        Integer limit = 5;
        HttpResponse<List<FinancialAccountVO>> firstPartResponse = callAndCatch(
                () -> financialAccountApiTestClient.listFinancialAccount(null, 0, limit));
        assertEquals(limit, firstPartResponse.body().size(),
                "Only the requested number of entries should be returend.");
        HttpResponse<List<FinancialAccountVO>> secondPartResponse = callAndCatch(
                () -> financialAccountApiTestClient.listFinancialAccount(null, 0 + limit, limit));
        assertEquals(limit, secondPartResponse.body().size(),
                "Only the requested number of entries should be returend.");

        retrievedFinancialAccounts.clear();
        retrievedFinancialAccounts.addAll(firstPartResponse.body());
        retrievedFinancialAccounts.addAll(secondPartResponse.body());
        expectedFinancialAccounts.stream()
                .forEach(expectedFinancialAccount -> assertTrue(retrievedMap.containsKey(expectedFinancialAccount.getId()),
                        String.format("All created financialAccounts should be returned - Missing: %s.", expectedFinancialAccount)));
        expectedFinancialAccounts.stream().forEach(
                expectedFinancialAccount -> assertEquals(expectedFinancialAccount, retrievedMap.get(expectedFinancialAccount.getId()),
                        "The correct financialAccounts should be retrieved."));
    }

    @Test
    @Override
    public void listFinancialAccount400() throws Exception {
        HttpResponse<List<FinancialAccountVO>> badRequestResponse = callAndCatch(
                () -> financialAccountApiTestClient.listFinancialAccount(null, -1, null));
        assertEquals(HttpStatus.BAD_REQUEST,
                badRequestResponse.getStatus(),
                "Negative offsets are impossible.");

        Optional<ErrorDetails> optionalErrorDetails = badRequestResponse.getBody(ErrorDetails.class);
        assertTrue(optionalErrorDetails.isPresent(), "Error details should be provided.");

        badRequestResponse = callAndCatch(() -> financialAccountApiTestClient.listFinancialAccount(null, null, -1));
        assertEquals(HttpStatus.BAD_REQUEST,
                badRequestResponse.getStatus(),
                "Negative limits are impossible.");
        optionalErrorDetails = badRequestResponse.getBody(ErrorDetails.class);
        assertTrue(optionalErrorDetails.isPresent(), "Error details should be provided.");

    }

    @Disabled
    @Test
    @Override
    public void listFinancialAccount401() throws Exception {

    }

    @Disabled
    @Test
    @Override
    public void listFinancialAccount403() throws Exception {

    }

    @Disabled
    @Test
    @Override
    public void listFinancialAccount404() throws Exception {

    }

    @Disabled
    @Test
    @Override
    public void listFinancialAccount405() throws Exception {

    }

    @Disabled
    @Test
    @Override
    public void listFinancialAccount409() throws Exception {

    }

    @Override
    public void listFinancialAccount500() throws Exception {

    }

    @ParameterizedTest
    @MethodSource("provideFinancialAccountUpdates")
    public void patchFinancialAccount200(String message, FinancialAccountUpdateVO financialAccountUpdateVO, FinancialAccountVO expectedFinancialAccount)
            throws Exception {
        this.message = message;
        this.financialAccountUpdateVO = financialAccountUpdateVO;
        this.expectedFinancialAccount = expectedFinancialAccount;
        patchFinancialAccount200();
    }

    @Override
    public void patchFinancialAccount200() throws Exception {
        //first create
        FinancialAccountCreateVO financialAccountCreateVO = FinancialAccountCreateVOTestExample.build();
        HttpResponse<FinancialAccountVO> createResponse = callAndCatch(
                () -> financialAccountApiTestClient.createFinancialAccount(financialAccountCreateVO));
        assertEquals(HttpStatus.CREATED, createResponse.getStatus(), "The financialAccount should have been created first.");

        String financialAccountId = createResponse.body().getId();

        HttpResponse<FinancialAccountVO> updateResponse = callAndCatch(
                () -> financialAccountApiTestClient.patchFinancialAccount(financialAccountId, financialAccountUpdateVO));
        assertEquals(HttpStatus.OK, updateResponse.getStatus(), message);

        FinancialAccountVO updatedFinancialAccount = updateResponse.body();
        expectedFinancialAccount.setHref(financialAccountId);
        expectedFinancialAccount.setId(financialAccountId);
        expectedFinancialAccount.setRelatedParty(null);

        assertEquals(expectedFinancialAccount, updatedFinancialAccount, message);
    }

    private static Stream<Arguments> provideFinancialAccountUpdates() {
        List<Arguments> testEntries = new ArrayList<>();

        FinancialAccountUpdateVO newTypeFinancialAccount = FinancialAccountUpdateVOTestExample.build();
        newTypeFinancialAccount.setAccountType("New-Type");
        FinancialAccountVO expectedNewType = FinancialAccountVOTestExample.build();
        expectedNewType.setAccountType("New-Type");
        testEntries.add(Arguments.of("The type should have been updated.", newTypeFinancialAccount, expectedNewType));

        FinancialAccountUpdateVO newDesc = FinancialAccountUpdateVOTestExample.build();
        newDesc.setDescription("New description");
        FinancialAccountVO expectedNewDesc = FinancialAccountVOTestExample.build();
        expectedNewDesc.setDescription("New description");
        testEntries.add(Arguments.of("The description should have been updated.", newDesc, expectedNewDesc));

        FinancialAccountUpdateVO newName = FinancialAccountUpdateVOTestExample.build();
        newName.setName("New name");
        FinancialAccountVO expectedNewName = FinancialAccountVOTestExample.build();
        expectedNewName.setName("New name");
        testEntries.add(Arguments.of("The name should have been updated.", newName, expectedNewName));

        FinancialAccountUpdateVO newState = FinancialAccountUpdateVOTestExample.build();
        newState.setState("New state");
        FinancialAccountVO expectedNewState = FinancialAccountVOTestExample.build();
        expectedNewState.setState("New state");
        testEntries.add(Arguments.of("The state should have been updated.", newState, expectedNewState));


        return testEntries.stream();
    }

    @ParameterizedTest
    @MethodSource("provideInvalidUpdates")
    public void patchFinancialAccount400(String message, FinancialAccountUpdateVO invalidUpdateVO) throws Exception {
        this.message = message;
        this.financialAccountUpdateVO = invalidUpdateVO;
        patchFinancialAccount400();
    }

    @Override
    public void patchFinancialAccount400() throws Exception {
        //first create
        FinancialAccountCreateVO financialAccountCreateVO = FinancialAccountCreateVOTestExample.build();
        HttpResponse<FinancialAccountVO> createResponse = callAndCatch(
                () -> financialAccountApiTestClient.createFinancialAccount(financialAccountCreateVO));
        assertEquals(HttpStatus.CREATED, createResponse.getStatus(), "The financialAccount should have been created first.");

        String financialAccountId = createResponse.body().getId();

        HttpResponse<FinancialAccountVO> updateResponse = callAndCatch(
                () -> financialAccountApiTestClient.patchFinancialAccount(financialAccountId, financialAccountUpdateVO));
        assertEquals(HttpStatus.BAD_REQUEST, updateResponse.getStatus(), message);

        Optional<ErrorDetails> optionalErrorDetails = updateResponse.getBody(ErrorDetails.class);
        assertTrue(optionalErrorDetails.isPresent(), "Some error details should be present.");
    }

    private static Stream<Arguments> provideInvalidUpdates() {
        List<Arguments> testEntries = new ArrayList<>();

        FinancialAccountUpdateVO invalidRelatedPartyUpdate = FinancialAccountUpdateVOTestExample.build();
        // no valid id
        RelatedPartyVO invalidRelatedParty = RelatedPartyVOTestExample.build();
        invalidRelatedPartyUpdate.setRelatedParty(List.of(invalidRelatedParty));
        testEntries.add(Arguments.of("A financialAccount with invalid related parties should not be updated.",
                invalidRelatedPartyUpdate));

        FinancialAccountUpdateVO nonExistentRelatedPartyUpdate = FinancialAccountUpdateVOTestExample.build();
        // no existent id
        RelatedPartyVO nonExistentRelatedParty = RelatedPartyVOTestExample.build();
        nonExistentRelatedParty.setId("urn:ngsi-ld:individual:non-existent");
        nonExistentRelatedPartyUpdate.setRelatedParty(List.of(nonExistentRelatedParty));
        testEntries.add(Arguments.of("A financialAccount with non-existent related parties should not be updated.",
                nonExistentRelatedPartyUpdate));

        return testEntries.stream();
    }

    @Disabled
    @Test
    @Override
    public void patchFinancialAccount401() throws Exception {

    }

    @Disabled
    @Test
    @Override
    public void patchFinancialAccount403() throws Exception {

    }

    @Test
    @Override
    public void patchFinancialAccount404() throws Exception {
        FinancialAccountUpdateVO financialAccountUpdateVO = FinancialAccountUpdateVOTestExample.build();
        assertEquals(
                HttpStatus.NOT_FOUND,
                callAndCatch(() -> financialAccountApiTestClient.patchFinancialAccount("urn:ngsi-ld:financialAccount:not-existent",
                        financialAccountUpdateVO)).getStatus(),
                "Non existent financialAccounts should not be updated.");
    }

    @Disabled
    @Test
    @Override
    public void patchFinancialAccount405() throws Exception {

    }

    @Disabled
    @Test
    @Override
    public void patchFinancialAccount409() throws Exception {

    }

    @Override
    public void patchFinancialAccount500() throws Exception {

    }

    @Test
    @Override
    public void retrieveFinancialAccount200() throws Exception {

        //first create
        FinancialAccountCreateVO financialAccountCreateVO = FinancialAccountCreateVOTestExample.build();
        HttpResponse<FinancialAccountVO> createResponse = callAndCatch(
                () -> financialAccountApiTestClient.createFinancialAccount(financialAccountCreateVO));
        assertEquals(HttpStatus.CREATED, createResponse.getStatus(), "The financialAccount should have been created first.");
        String id = createResponse.body().getId();

        FinancialAccountVO expectedFinancialAccount = FinancialAccountVOTestExample.build();
        expectedFinancialAccount.setId(id);
        expectedFinancialAccount.setHref(id);
        // empty list is mapped to null
        expectedFinancialAccount.setRelatedParty(null);

        //then retrieve
        HttpResponse<FinancialAccountVO> retrievedFinancialAccount = callAndCatch(() -> financialAccountApiTestClient.retrieveFinancialAccount(id, null));
        assertEquals(HttpStatus.OK, retrievedFinancialAccount.getStatus(), "The retrieval should be ok.");
        assertEquals(expectedFinancialAccount, retrievedFinancialAccount.body(), "The correct financialAccount should be returned.");
    }

    @Disabled
    @Test
    @Override
    public void retrieveFinancialAccount400() throws Exception {

    }

    @Disabled
    @Test
    @Override
    public void retrieveFinancialAccount401() throws Exception {

    }

    @Disabled
    @Test
    @Override
    public void retrieveFinancialAccount403() throws Exception {

    }

    @Test
    @Override
    public void retrieveFinancialAccount404() throws Exception {
        HttpResponse<FinancialAccountVO> response = callAndCatch(
                () -> financialAccountApiTestClient.retrieveFinancialAccount("urn:ngsi-ld:financialAccount:non-existent", null));
        assertEquals(HttpStatus.NOT_FOUND, response.getStatus(), "No such financialAccount should exist.");

        Optional<ErrorDetails> optionalErrorDetails = response.getBody(ErrorDetails.class);
        assertTrue(optionalErrorDetails.isPresent(), "Error details should have been provided.");
    }

    @Disabled
    @Test
    @Override
    public void retrieveFinancialAccount405() throws Exception {

    }

    @Disabled
    @Test
    @Override
    public void retrieveFinancialAccount409() throws Exception {

    }

    @Override
    public void retrieveFinancialAccount500() throws Exception {

    }

}
