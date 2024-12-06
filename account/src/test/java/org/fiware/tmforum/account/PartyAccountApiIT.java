package org.fiware.tmforum.account;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import io.micronaut.test.annotation.MockBean;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import org.fiware.account.api.PartyAccountApiTestClient;
import org.fiware.account.api.PartyAccountApiTestSpec;
import org.fiware.account.model.*;
import org.fiware.ngsi.api.EntitiesApiClient;
import org.fiware.tmforum.account.domain.PartyAccount;
import org.fiware.tmforum.common.configuration.GeneralProperties;
import org.fiware.tmforum.common.exception.ErrorDetails;
import org.fiware.tmforum.common.notification.TMForumEventHandler;
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

@MicronautTest(packages = {"org.fiware.tmforum.account"})
public class PartyAccountApiIT extends AbstractApiIT implements PartyAccountApiTestSpec {

	public final PartyAccountApiTestClient partyAccountApiTestClient;

	private String message;
	private PartyAccountCreateVO partyAccountCreateVO;
	private PartyAccountUpdateVO partyAccountUpdateVO;
	private PartyAccountVO expectedPartyAccount;

	public PartyAccountApiIT(PartyAccountApiTestClient partyAccountApiTestClient, EntitiesApiClient entitiesApiClient,
							 ObjectMapper objectMapper, GeneralProperties generalProperties) {
		super(entitiesApiClient, objectMapper, generalProperties);
		this.partyAccountApiTestClient = partyAccountApiTestClient;
	}

	@Override
	protected String getEntityType() {
		return PartyAccount.TYPE_PARTYAC;
	}

	@MockBean(TMForumEventHandler.class)
	public TMForumEventHandler eventHandler() {
		TMForumEventHandler eventHandler = mock(TMForumEventHandler.class);

		when(eventHandler.handleCreateEvent(any())).thenReturn(Mono.empty());
		when(eventHandler.handleUpdateEvent(any(), any())).thenReturn(Mono.empty());

		return eventHandler;
	}

	private static void fixExampleCreate(PartyAccountCreateVO partyAccount) {
		// fix the example
		BillingCycleSpecificationRefOrValueVO billingCycleSpecificationRefOrValueVO = BillingCycleSpecificationRefOrValueVOTestExample.build().atSchemaLocation(null);
		billingCycleSpecificationRefOrValueVO.setHref("http://my-ref.de");
		billingCycleSpecificationRefOrValueVO.setId("http://my-url.de");

		BillFormatRefOrValueVO billFormatRefOrValueVO = BillFormatRefOrValueVOTestExample.build().atSchemaLocation(null);
		billFormatRefOrValueVO.setHref("http://my-ref.de");
		billFormatRefOrValueVO.setId("http://my-url.de");

		BillStructureVO billStructure = BillStructureVOTestExample.build().atSchemaLocation(null);
		billStructure.setCycleSpecification(billingCycleSpecificationRefOrValueVO);
		billStructure.setFormat(billFormatRefOrValueVO);
		partyAccount.setBillStructure(billStructure);
	}

	private static void fixExampleUpdate(PartyAccountUpdateVO partyAccount) {
		// fix the example
		BillingCycleSpecificationRefOrValueVO billingCycleSpecificationRefOrValueVO = BillingCycleSpecificationRefOrValueVOTestExample.build().atSchemaLocation(null);
		billingCycleSpecificationRefOrValueVO.setHref("http://my-ref.de");
		billingCycleSpecificationRefOrValueVO.setId("http://my-url.de");

		BillFormatRefOrValueVO billFormatRefOrValueVO = BillFormatRefOrValueVOTestExample.build().atSchemaLocation(null);
		billFormatRefOrValueVO.setHref("http://my-ref.de");
		billFormatRefOrValueVO.setId("http://my-url.de");

		BillStructureVO billStructure = BillStructureVOTestExample.build().atSchemaLocation(null);
		billStructure.setCycleSpecification(billingCycleSpecificationRefOrValueVO);
		billStructure.setFormat(billFormatRefOrValueVO);
		partyAccount.setBillStructure(billStructure);
	}

	private static void fixExampleExpected(PartyAccountVO partyAccount) {
		// fix the example
		BillingCycleSpecificationRefOrValueVO billingCycleSpecificationRefOrValueVO = BillingCycleSpecificationRefOrValueVOTestExample.build().atSchemaLocation(null);
		billingCycleSpecificationRefOrValueVO.setHref("http://my-ref.de");
		billingCycleSpecificationRefOrValueVO.setId("http://my-url.de");

		BillFormatRefOrValueVO billFormatRefOrValueVO = BillFormatRefOrValueVOTestExample.build().atSchemaLocation(null);
		billFormatRefOrValueVO.setHref("http://my-ref.de");
		billFormatRefOrValueVO.setId("http://my-url.de");

		BillStructureVO billStructure = BillStructureVOTestExample.build().atSchemaLocation(null);
		billStructure.setCycleSpecification(billingCycleSpecificationRefOrValueVO);
		billStructure.setFormat(billFormatRefOrValueVO);
		partyAccount.setBillStructure(billStructure);
	}

	@ParameterizedTest
	@MethodSource("provideValidPartyAccounts")
	public void createPartyAccount201(String message, PartyAccountCreateVO partyAccountCreateVO, PartyAccountVO expectedPartyAccount)
			throws Exception {
		this.message = message;
		this.partyAccountCreateVO = partyAccountCreateVO;
		this.expectedPartyAccount = expectedPartyAccount;
		createPartyAccount201();
	}

	@Override
	public void createPartyAccount201() throws Exception {

		HttpResponse<PartyAccountVO> partyAccountVOHttpResponse = callAndCatch(
				() -> partyAccountApiTestClient.createPartyAccount(null, partyAccountCreateVO));
		assertEquals(HttpStatus.CREATED, partyAccountVOHttpResponse.getStatus(), message);
		String partyAccountId = partyAccountVOHttpResponse.body().getId();
		expectedPartyAccount.setId(partyAccountId);
		expectedPartyAccount.setHref(partyAccountId);

		assertEquals(expectedPartyAccount, partyAccountVOHttpResponse.body(), message);


	}

	private static Stream<Arguments> provideValidPartyAccounts() {
		List<Arguments> testEntries = new ArrayList<>();

		PartyAccountCreateVO partyAccountCreateVO = PartyAccountCreateVOTestExample.build().atSchemaLocation(null).defaultPaymentMethod(null).financialAccount(null);
		PartyAccountVO expectedPartyAccount = PartyAccountVOTestExample.build().atSchemaLocation(null).defaultPaymentMethod(null).financialAccount(null);

		fixExampleCreate(partyAccountCreateVO);
		fixExampleExpected(expectedPartyAccount);

		testEntries.add(Arguments.of("An empty partyAccount should have been created.", partyAccountCreateVO, expectedPartyAccount));

		return testEntries.stream();
	}

	@ParameterizedTest
	@MethodSource("provideInvalidPartyAccounts")
	public void createPartyAccount400(String message, PartyAccountCreateVO invalidCreateVO) throws Exception {
		this.message = message;
		this.partyAccountCreateVO = invalidCreateVO;
		createPartyAccount400();
	}

	@Override
	public void createPartyAccount400() throws Exception {
		HttpResponse<PartyAccountVO> creationResponse = callAndCatch(
				() -> partyAccountApiTestClient.createPartyAccount(null, partyAccountCreateVO));
		assertEquals(HttpStatus.BAD_REQUEST, creationResponse.getStatus(), message);
		Optional<ErrorDetails> optionalErrorDetails = creationResponse.getBody(ErrorDetails.class);
		assertTrue(optionalErrorDetails.isPresent(), "Error details should be provided.");
	}

	private static Stream<Arguments> provideInvalidPartyAccounts() {
		List<Arguments> testEntries = new ArrayList<>();

		PartyAccountCreateVO invalidRelatedPartyCreate = PartyAccountCreateVOTestExample.build().atSchemaLocation(null)
				.defaultPaymentMethod(null)
				.financialAccount(null);
		// no valid id
		RelatedPartyVO invalidRelatedParty = RelatedPartyVOTestExample.build().atSchemaLocation(null);
		invalidRelatedPartyCreate.setRelatedParty(List.of(invalidRelatedParty));
		testEntries.add(Arguments.of("A partyAccount with invalid related parties should not be created.",
				invalidRelatedPartyCreate));

		PartyAccountCreateVO nonExistentRelatedPartyCreate = PartyAccountCreateVOTestExample.build().atSchemaLocation(null)
				.defaultPaymentMethod(null)
				.financialAccount(null);
		// no existent id
		RelatedPartyVO nonExistentRelatedParty = RelatedPartyVOTestExample.build().atSchemaLocation(null);
		nonExistentRelatedParty.setId("urn:ngsi-ld:individual:non-existent");
		nonExistentRelatedPartyCreate.setRelatedParty(List.of(nonExistentRelatedParty));
		testEntries.add(Arguments.of("A partyAccount with non-existent related parties should not be created.",
				nonExistentRelatedPartyCreate));

		PartyAccountCreateVO nonExistentDefaultPaymentMethodCreate = PartyAccountCreateVOTestExample.build().atSchemaLocation(null)
				.financialAccount(null);
		// no existent id
		nonExistentDefaultPaymentMethodCreate.setRelatedParty(List.of(nonExistentRelatedParty));
		testEntries.add(Arguments.of("A partyAccount with non-existent related parties should not be created.",
				nonExistentDefaultPaymentMethodCreate));

		PartyAccountCreateVO nonExistentFinancialAccountCreate = PartyAccountCreateVOTestExample.build().atSchemaLocation(null)
				.defaultPaymentMethod(null);
		// no existent id
		nonExistentFinancialAccountCreate.setRelatedParty(List.of(nonExistentRelatedParty));
		testEntries.add(Arguments.of("A partyAccount with non-existent related parties should not be created.",
				nonExistentFinancialAccountCreate));

		fixExampleCreate(invalidRelatedPartyCreate);
		fixExampleCreate(nonExistentRelatedPartyCreate);

		return testEntries.stream();
	}

	@Disabled
	@Test
	@Override
	public void createPartyAccount401() throws Exception {

	}

	@Disabled
	@Test
	@Override
	public void createPartyAccount403() throws Exception {

	}

	@Disabled
	@Test
	@Override
	public void createPartyAccount405() throws Exception {

	}

	@Disabled
	@Test
	@Override
	public void createPartyAccount409() throws Exception {

	}

	@Override
	public void createPartyAccount500() throws Exception {

	}

	@Test
	@Override
	public void deletePartyAccount204() throws Exception {
		//first create
		PartyAccountCreateVO partyAccountCreateVO = PartyAccountCreateVOTestExample.build().atSchemaLocation(null)
				.defaultPaymentMethod(null)
				.financialAccount(null);

		fixExampleCreate(partyAccountCreateVO);

		HttpResponse<PartyAccountVO> createResponse = callAndCatch(
				() -> partyAccountApiTestClient.createPartyAccount(null, partyAccountCreateVO));
		assertEquals(HttpStatus.CREATED, createResponse.getStatus(), "The partyAccount should have been created first.");

		String partyAccountId = createResponse.body().getId();

		assertEquals(HttpStatus.NO_CONTENT,
				callAndCatch(() -> partyAccountApiTestClient.deletePartyAccount(null, partyAccountId)).getStatus(),
				"The partyAccount should have been deleted.");

		assertEquals(HttpStatus.NOT_FOUND,
				callAndCatch(() -> partyAccountApiTestClient.retrievePartyAccount(null, partyAccountId, null)).status(),
				"The partyAccount should not exist anymore.");

	}

	@Disabled
	@Test
	@Override
	public void deletePartyAccount400() throws Exception {

	}

	@Disabled
	@Test
	@Override
	public void deletePartyAccount401() throws Exception {

	}

	@Disabled
	@Test
	@Override
	public void deletePartyAccount403() throws Exception {

	}

	@Test
	@Override
	public void deletePartyAccount404() throws Exception {
		HttpResponse<?> notFoundResponse = callAndCatch(
				() -> partyAccountApiTestClient.deletePartyAccount(null, "urn:ngsi-ld:partyAccount:no-partyAccount"));
		assertEquals(HttpStatus.NOT_FOUND,
				notFoundResponse.getStatus(),
				"No such partyAccount should exist.");

		Optional<ErrorDetails> optionalErrorDetails = notFoundResponse.getBody(ErrorDetails.class);
		assertTrue(optionalErrorDetails.isPresent(), "Error details should be provided.");

		notFoundResponse = callAndCatch(() -> partyAccountApiTestClient.deletePartyAccount(null, "invalid-id"));
		assertEquals(HttpStatus.NOT_FOUND,
				notFoundResponse.getStatus(),
				"No such partyAccount should exist.");

		optionalErrorDetails = notFoundResponse.getBody(ErrorDetails.class);
		assertTrue(optionalErrorDetails.isPresent(), "Error details should be provided.");
	}

	@Disabled
	@Test
	@Override
	public void deletePartyAccount405() throws Exception {

	}

	@Disabled
	@Test
	@Override
	public void deletePartyAccount409() throws Exception {

	}

	@Override
	public void deletePartyAccount500() throws Exception {

	}

	@Test
	@Override
	public void listPartyAccount200() throws Exception {
		List<PartyAccountVO> expectedPartyAccounts = new ArrayList<>();
		for (int i = 0; i < 10; i++) {
			PartyAccountCreateVO partyAccountCreateVO = PartyAccountCreateVOTestExample.build().atSchemaLocation(null)
					.defaultPaymentMethod(null)
					.financialAccount(null);
			fixExampleCreate(partyAccountCreateVO);
			String id = partyAccountApiTestClient.createPartyAccount(null, partyAccountCreateVO).body().getId();
			PartyAccountVO partyAccountVO = PartyAccountVOTestExample.build().atSchemaLocation(null);
			fixExampleExpected(partyAccountVO);
			BillingCycleSpecificationRefOrValueVO billingCycleRV = partyAccountVO.getBillStructure()
					.getCycleSpecification();
			BillStructureVO billStructure = partyAccountVO.getBillStructure()
					.cycleSpecification(billingCycleRV);
			partyAccountVO
					.id(id)
					.href(id)
					.billStructure(billStructure)
					.defaultPaymentMethod(null)
					.financialAccount(null);
			expectedPartyAccounts.add(partyAccountVO);
		}

		HttpResponse<List<PartyAccountVO>> partyAccountResponse = callAndCatch(
				() -> partyAccountApiTestClient.listPartyAccount(null, null, null, null));

		assertEquals(HttpStatus.OK, partyAccountResponse.getStatus(), "The list should be accessible.");
		assertEquals(expectedPartyAccounts.size(), partyAccountResponse.getBody().get().size(),
				"All partyAccounts should have been returned.");
		List<PartyAccountVO> retrievedPartyAccounts = partyAccountResponse.getBody().get();

		Map<String, PartyAccountVO> retrievedMap = retrievedPartyAccounts.stream()
				.collect(Collectors.toMap(partyAccount -> partyAccount.getId(), partyAccount -> partyAccount));

		expectedPartyAccounts.stream()
				.forEach(expectedPartyAccount -> assertTrue(retrievedMap.containsKey(expectedPartyAccount.getId()),
						String.format("All created partyAccounts should be returned - Missing: %s.", expectedPartyAccount,
								retrievedPartyAccounts)));
		expectedPartyAccounts.stream().forEach(
				expectedPartyAccount -> assertEquals(expectedPartyAccount, retrievedMap.get(expectedPartyAccount.getId()),
						"The correct partyAccounts should be retrieved."));

		// get with pagination
		Integer limit = 5;
		HttpResponse<List<PartyAccountVO>> firstPartResponse = callAndCatch(
				() -> partyAccountApiTestClient.listPartyAccount(null, null, 0, limit));
		assertEquals(limit, firstPartResponse.body().size(),
				"Only the requested number of entries should be returend.");
		HttpResponse<List<PartyAccountVO>> secondPartResponse = callAndCatch(
				() -> partyAccountApiTestClient.listPartyAccount(null, null, 0 + limit, limit));
		assertEquals(limit, secondPartResponse.body().size(),
				"Only the requested number of entries should be returend.");

		retrievedPartyAccounts.clear();
		retrievedPartyAccounts.addAll(firstPartResponse.body());
		retrievedPartyAccounts.addAll(secondPartResponse.body());
		expectedPartyAccounts.stream()
				.forEach(expectedPartyAccount -> assertTrue(retrievedMap.containsKey(expectedPartyAccount.getId()),
						String.format("All created partyAccounts should be returned - Missing: %s.", expectedPartyAccount)));
		expectedPartyAccounts.stream().forEach(
				expectedPartyAccount -> assertEquals(expectedPartyAccount, retrievedMap.get(expectedPartyAccount.getId()),
						"The correct partyAccounts should be retrieved."));
	}

	@Test
	@Override
	public void listPartyAccount400() throws Exception {
		HttpResponse<List<PartyAccountVO>> badRequestResponse = callAndCatch(
				() -> partyAccountApiTestClient.listPartyAccount(null, null, -1, null));
		assertEquals(HttpStatus.BAD_REQUEST,
				badRequestResponse.getStatus(),
				"Negative offsets are impossible.");

		Optional<ErrorDetails> optionalErrorDetails = badRequestResponse.getBody(ErrorDetails.class);
		assertTrue(optionalErrorDetails.isPresent(), "Error details should be provided.");

		badRequestResponse = callAndCatch(() -> partyAccountApiTestClient.listPartyAccount(null, null, null, -1));
		assertEquals(HttpStatus.BAD_REQUEST,
				badRequestResponse.getStatus(),
				"Negative limits are impossible.");
		optionalErrorDetails = badRequestResponse.getBody(ErrorDetails.class);
		assertTrue(optionalErrorDetails.isPresent(), "Error details should be provided.");

	}

	@Disabled
	@Test
	@Override
	public void listPartyAccount401() throws Exception {

	}

	@Disabled
	@Test
	@Override
	public void listPartyAccount403() throws Exception {

	}

	@Disabled
	@Test
	@Override
	public void listPartyAccount404() throws Exception {

	}

	@Disabled
	@Test
	@Override
	public void listPartyAccount405() throws Exception {

	}

	@Disabled
	@Test
	@Override
	public void listPartyAccount409() throws Exception {

	}

	@Override
	public void listPartyAccount500() throws Exception {

	}

	@ParameterizedTest
	@MethodSource("providePartyAccountUpdates")
	public void patchPartyAccount200(String message, PartyAccountUpdateVO partyAccountUpdateVO, PartyAccountVO expectedPartyAccount)
			throws Exception {
		this.message = message;
		this.partyAccountUpdateVO = partyAccountUpdateVO;
		this.expectedPartyAccount = expectedPartyAccount;
		patchPartyAccount200();
	}

	@Override
	public void patchPartyAccount200() throws Exception {
		//first create
		PartyAccountCreateVO partyAccountCreateVO = PartyAccountCreateVOTestExample.build().atSchemaLocation(null)
				.financialAccount(null)
				.defaultPaymentMethod(null);
		fixExampleCreate(partyAccountCreateVO);
		HttpResponse<PartyAccountVO> createResponse = callAndCatch(
				() -> partyAccountApiTestClient.createPartyAccount(null, partyAccountCreateVO));
		assertEquals(HttpStatus.CREATED, createResponse.getStatus(), "The partyAccount should have been created first.");

		String partyAccountId = createResponse.body().getId();

		HttpResponse<PartyAccountVO> updateResponse = callAndCatch(
				() -> partyAccountApiTestClient.patchPartyAccount(null, partyAccountId, partyAccountUpdateVO));
		assertEquals(HttpStatus.OK, updateResponse.getStatus(), message);

		PartyAccountVO updatedPartyAccount = updateResponse.body();
		fixExampleExpected(expectedPartyAccount);
		expectedPartyAccount.setHref(partyAccountId);
		expectedPartyAccount.setId(partyAccountId);
		expectedPartyAccount.defaultPaymentMethod(null);
		expectedPartyAccount.financialAccount(null);
		BillingCycleSpecificationRefOrValueVO billingCycleRV = expectedPartyAccount.getBillStructure()
				.getCycleSpecification();
		BillStructureVO billStructure = expectedPartyAccount.getBillStructure()
				.cycleSpecification(billingCycleRV);
		expectedPartyAccount.billStructure(billStructure);

		assertEquals(expectedPartyAccount, updatedPartyAccount, message);
	}

	private static Stream<Arguments> providePartyAccountUpdates() {
		List<Arguments> testEntries = new ArrayList<>();

		PartyAccountUpdateVO newTypePartyAccount = PartyAccountUpdateVOTestExample.build().atSchemaLocation(null)
				.financialAccount(null)
				.defaultPaymentMethod(null);
		fixExampleUpdate(newTypePartyAccount);
		newTypePartyAccount.setAccountType("New-Type");
		PartyAccountVO expectedNewType = PartyAccountVOTestExample.build().atSchemaLocation(null);
		expectedNewType.setAccountType("New-Type");
		testEntries.add(Arguments.of("The type should have been updated.", newTypePartyAccount, expectedNewType));

		PartyAccountUpdateVO newDesc = PartyAccountUpdateVOTestExample.build().atSchemaLocation(null)
				.financialAccount(null)
				.defaultPaymentMethod(null);
		fixExampleUpdate(newDesc);
		newDesc.setDescription("New description");
		PartyAccountVO expectedNewDesc = PartyAccountVOTestExample.build().atSchemaLocation(null);
		expectedNewDesc.setDescription("New description");
		testEntries.add(Arguments.of("The description should have been updated.", newDesc, expectedNewDesc));

		PartyAccountUpdateVO newName = PartyAccountUpdateVOTestExample.build().atSchemaLocation(null)
				.financialAccount(null)
				.defaultPaymentMethod(null);
		fixExampleUpdate(newName);
		newName.setName("New name");
		PartyAccountVO expectedNewName = PartyAccountVOTestExample.build().atSchemaLocation(null);
		expectedNewName.setName("New name");
		testEntries.add(Arguments.of("The name should have been updated.", newName, expectedNewName));

		return testEntries.stream();
	}

	@ParameterizedTest
	@MethodSource("provideInvalidUpdates")
	public void patchPartyAccount400(String message, PartyAccountUpdateVO invalidUpdateVO) throws Exception {
		this.message = message;
		this.partyAccountUpdateVO = invalidUpdateVO;
		patchPartyAccount400();
	}

	@Override
	public void patchPartyAccount400() throws Exception {
		//first create
		PartyAccountCreateVO partyAccountCreateVO = PartyAccountCreateVOTestExample.build().atSchemaLocation(null)
				.financialAccount(null)
				.defaultPaymentMethod(null);
		fixExampleCreate(partyAccountCreateVO);
		HttpResponse<PartyAccountVO> createResponse = callAndCatch(
				() -> partyAccountApiTestClient.createPartyAccount(null, partyAccountCreateVO));
		assertEquals(HttpStatus.CREATED, createResponse.getStatus(), "The partyAccount should have been created first.");

		String partyAccountId = createResponse.body().getId();

		HttpResponse<PartyAccountVO> updateResponse = callAndCatch(
				() -> partyAccountApiTestClient.patchPartyAccount(null, partyAccountId, partyAccountUpdateVO));
		assertEquals(HttpStatus.BAD_REQUEST, updateResponse.getStatus(), message);

		Optional<ErrorDetails> optionalErrorDetails = updateResponse.getBody(ErrorDetails.class);
		assertTrue(optionalErrorDetails.isPresent(), "Some error details should be present.");
	}

	private static Stream<Arguments> provideInvalidUpdates() {
		List<Arguments> testEntries = new ArrayList<>();

		PartyAccountUpdateVO invalidRelatedPartyUpdate = PartyAccountUpdateVOTestExample.build().atSchemaLocation(null)
				.financialAccount(null)
				.defaultPaymentMethod(null);
		fixExampleUpdate(invalidRelatedPartyUpdate);
		// no valid id
		RelatedPartyVO invalidRelatedParty = RelatedPartyVOTestExample.build().atSchemaLocation(null);
		invalidRelatedPartyUpdate.setRelatedParty(List.of(invalidRelatedParty));
		testEntries.add(Arguments.of("A partyAccount with invalid related parties should not be updated.",
				invalidRelatedPartyUpdate));

		PartyAccountUpdateVO nonExistentRelatedPartyUpdate = PartyAccountUpdateVOTestExample.build().atSchemaLocation(null)
				.financialAccount(null)
				.defaultPaymentMethod(null);
		fixExampleUpdate(nonExistentRelatedPartyUpdate);
		// no existent id
		RelatedPartyVO nonExistentRelatedParty = RelatedPartyVOTestExample.build().atSchemaLocation(null);
		nonExistentRelatedParty.setId("urn:ngsi-ld:individual:non-existent");
		nonExistentRelatedPartyUpdate.setRelatedParty(List.of(nonExistentRelatedParty));
		testEntries.add(Arguments.of("A partyAccount with non-existent related parties should not be updated.",
				nonExistentRelatedPartyUpdate));

		PartyAccountUpdateVO nonExistentFinancialAccountUpdate = PartyAccountUpdateVOTestExample.build().atSchemaLocation(null)
				.defaultPaymentMethod(null);
		fixExampleUpdate(nonExistentFinancialAccountUpdate);
		testEntries.add(Arguments.of("A partyAccount with non-existent Financial Account should not be updated.",
				nonExistentFinancialAccountUpdate));

		PartyAccountUpdateVO nonExistentDefaultPaymentMethodUpdate = PartyAccountUpdateVOTestExample.build().atSchemaLocation(null)
				.financialAccount(null);
		fixExampleUpdate(nonExistentDefaultPaymentMethodUpdate);
		testEntries.add(Arguments.of("A partyAccount with non-existent default payment method should not be updated.",
				nonExistentDefaultPaymentMethodUpdate));

		return testEntries.stream();
	}

	@Disabled
	@Test
	@Override
	public void patchPartyAccount401() throws Exception {

	}

	@Disabled
	@Test
	@Override
	public void patchPartyAccount403() throws Exception {

	}

	@Test
	@Override
	public void patchPartyAccount404() throws Exception {
		PartyAccountUpdateVO partyAccountUpdateVO = PartyAccountUpdateVOTestExample.build().atSchemaLocation(null).billStructure(null).defaultPaymentMethod(null).financialAccount(null);
		assertEquals(
				HttpStatus.NOT_FOUND,
				callAndCatch(() -> partyAccountApiTestClient.patchPartyAccount(null, "urn:ngsi-ld:partyAccount:not-existent",
						partyAccountUpdateVO)).getStatus(),
				"Non existent partyAccounts should not be updated.");
	}

	@Disabled
	@Test
	@Override
	public void patchPartyAccount405() throws Exception {

	}

	@Disabled
	@Test
	@Override
	public void patchPartyAccount409() throws Exception {

	}

	@Override
	public void patchPartyAccount500() throws Exception {

	}

	@Test
	@Override
	public void retrievePartyAccount200() throws Exception {

		//first create
		PartyAccountCreateVO partyAccountCreateVO = PartyAccountCreateVOTestExample.build().atSchemaLocation(null)
				.financialAccount(null)
				.defaultPaymentMethod(null);
		fixExampleCreate(partyAccountCreateVO);
		HttpResponse<PartyAccountVO> createResponse = callAndCatch(
				() -> partyAccountApiTestClient.createPartyAccount(null, partyAccountCreateVO));
		assertEquals(HttpStatus.CREATED, createResponse.getStatus(), "The partyAccount should have been created first.");
		String id = createResponse.body().getId();

		PartyAccountVO expectedPartyAccount = PartyAccountVOTestExample.build().atSchemaLocation(null);
		expectedPartyAccount.setId(id);
		expectedPartyAccount.setHref(id);
		expectedPartyAccount.financialAccount(null);
		expectedPartyAccount.defaultPaymentMethod(null);
		fixExampleExpected(expectedPartyAccount);
		BillingCycleSpecificationRefOrValueVO billingCycleRV = expectedPartyAccount.getBillStructure()
				.getCycleSpecification();
		BillStructureVO billStructure = expectedPartyAccount.getBillStructure()
				.cycleSpecification(billingCycleRV);
		expectedPartyAccount.billStructure(billStructure);

		//then retrieve
		HttpResponse<PartyAccountVO> retrievedPartyAccount = callAndCatch(() -> partyAccountApiTestClient.retrievePartyAccount(null, id, null));
		assertEquals(HttpStatus.OK, retrievedPartyAccount.getStatus(), "The retrieval should be ok.");
		assertEquals(expectedPartyAccount, retrievedPartyAccount.body(), "The correct partyAccount should be returned.");
	}

	@Disabled
	@Test
	@Override
	public void retrievePartyAccount400() throws Exception {

	}

	@Override
	public void retrievePartyAccount401() throws Exception {

	}

	@Disabled
	@Test
	@Override
	public void retrievePartyAccount403() throws Exception {

	}

	@Test
	@Override
	public void retrievePartyAccount404() throws Exception {
		HttpResponse<PartyAccountVO> response = callAndCatch(
				() -> partyAccountApiTestClient.retrievePartyAccount(null, "urn:ngsi-ld:partyAccount:non-existent", null));
		assertEquals(HttpStatus.NOT_FOUND, response.getStatus(), "No such partyAccount should exist.");

		Optional<ErrorDetails> optionalErrorDetails = response.getBody(ErrorDetails.class);
		assertTrue(optionalErrorDetails.isPresent(), "Error details should have been provided.");
	}

	@Disabled
	@Test
	@Override
	public void retrievePartyAccount405() throws Exception {

	}

	@Disabled
	@Test
	@Override
	public void retrievePartyAccount409() throws Exception {

	}

	@Override
	public void retrievePartyAccount500() throws Exception {

	}
}
