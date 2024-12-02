package org.fiware.tmforum.account;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import io.micronaut.test.annotation.MockBean;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import org.fiware.account.api.BillingAccountApiTestClient;
import org.fiware.account.api.BillingAccountApiTestSpec;
import org.fiware.account.model.*;
import org.fiware.ngsi.api.EntitiesApiClient;
import org.fiware.tmforum.account.domain.BillingAccount;
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
public class BillingAccountApiIT extends AbstractApiIT implements BillingAccountApiTestSpec {

	public final BillingAccountApiTestClient billingAccountApiTestClient;

	private String message;
	private BillingAccountCreateVO billingAccountCreateVO;
	private BillingAccountUpdateVO billingAccountUpdateVO;
	private BillingAccountVO expectedBillingAccount;

	public BillingAccountApiIT(BillingAccountApiTestClient billingAccountApiTestClient, EntitiesApiClient entitiesApiClient,
							   ObjectMapper objectMapper, GeneralProperties generalProperties) {
		super(entitiesApiClient, objectMapper, generalProperties);
		this.billingAccountApiTestClient = billingAccountApiTestClient;
	}

	@Override
	protected String getEntityType() {
		return BillingAccount.TYPE_BILLINGAC;
	}

	@MockBean(TMForumEventHandler.class)
	public TMForumEventHandler eventHandler() {
		TMForumEventHandler eventHandler = mock(TMForumEventHandler.class);

		when(eventHandler.handleCreateEvent(any())).thenReturn(Mono.empty());
		when(eventHandler.handleUpdateEvent(any(), any())).thenReturn(Mono.empty());

		return eventHandler;
	}

	private static void fixExampleCreate(BillingAccountCreateVO billingAccount) {
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
		billingAccount.setBillStructure(billStructure);
	}

	private static void fixExampleUpdate(BillingAccountUpdateVO billingAccount) {
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
		billingAccount.setBillStructure(billStructure);
	}

	private static void fixExampleExpected(BillingAccountVO billingAccount) {
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
		billingAccount.setBillStructure(billStructure);
	}

	@ParameterizedTest
	@MethodSource("provideValidBillingAccounts")
	public void createBillingAccount201(String message, BillingAccountCreateVO billingAccountCreateVO, BillingAccountVO expectedBillingAccount)
			throws Exception {
		this.message = message;
		this.billingAccountCreateVO = billingAccountCreateVO;
		this.expectedBillingAccount = expectedBillingAccount;
		createBillingAccount201();
	}

	@Override
	public void createBillingAccount201() throws Exception {

		HttpResponse<BillingAccountVO> billingAccountVOHttpResponse = callAndCatch(
				() -> billingAccountApiTestClient.createBillingAccount(null, billingAccountCreateVO));
		assertEquals(HttpStatus.CREATED, billingAccountVOHttpResponse.getStatus(), message);
		String billingAccountId = billingAccountVOHttpResponse.body().getId();
		expectedBillingAccount.setId(billingAccountId);
		expectedBillingAccount.setHref(billingAccountId);

		assertEquals(expectedBillingAccount, billingAccountVOHttpResponse.body(), message);


	}

	private static Stream<Arguments> provideValidBillingAccounts() {
		List<Arguments> testEntries = new ArrayList<>();

		BillingAccountCreateVO billingAccountCreateVO = BillingAccountCreateVOTestExample.build().defaultPaymentMethod(null).financialAccount(null);
		BillingAccountVO expectedBillingAccount = BillingAccountVOTestExample.build().defaultPaymentMethod(null).financialAccount(null);

		fixExampleCreate(billingAccountCreateVO);
		fixExampleExpected(expectedBillingAccount);

		testEntries.add(Arguments.of("An empty billingAccount should have been created.", billingAccountCreateVO, expectedBillingAccount));

		return testEntries.stream();
	}

	@ParameterizedTest
	@MethodSource("provideInvalidBillingAccounts")
	public void createBillingAccount400(String message, BillingAccountCreateVO invalidCreateVO) throws Exception {
		this.message = message;
		this.billingAccountCreateVO = invalidCreateVO;
		createBillingAccount400();
	}

	@Override
	public void createBillingAccount400() throws Exception {
		HttpResponse<BillingAccountVO> creationResponse = callAndCatch(
				() -> billingAccountApiTestClient.createBillingAccount(null, billingAccountCreateVO));
		assertEquals(HttpStatus.BAD_REQUEST, creationResponse.getStatus(), message);
		Optional<ErrorDetails> optionalErrorDetails = creationResponse.getBody(ErrorDetails.class);
		assertTrue(optionalErrorDetails.isPresent(), "Error details should be provided.");
	}

	private static Stream<Arguments> provideInvalidBillingAccounts() {
		List<Arguments> testEntries = new ArrayList<>();

		BillingAccountCreateVO invalidRelatedPartyCreate = BillingAccountCreateVOTestExample.build()
				.defaultPaymentMethod(null)
				.financialAccount(null);
		// no valid id
		RelatedPartyVO invalidRelatedParty = RelatedPartyVOTestExample.build();
		invalidRelatedPartyCreate.setRelatedParty(List.of(invalidRelatedParty));
		testEntries.add(Arguments.of("A billingAccount with invalid related parties should not be created.",
				invalidRelatedPartyCreate));

		BillingAccountCreateVO nonExistentRelatedPartyCreate = BillingAccountCreateVOTestExample.build()
				.defaultPaymentMethod(null)
				.financialAccount(null);
		// no existent id
		RelatedPartyVO nonExistentRelatedParty = RelatedPartyVOTestExample.build();
		nonExistentRelatedParty.setId("urn:ngsi-ld:individual:non-existent");
		nonExistentRelatedPartyCreate.setRelatedParty(List.of(nonExistentRelatedParty));
		testEntries.add(Arguments.of("A billingAccount with non-existent related parties should not be created.",
				nonExistentRelatedPartyCreate));

		BillingAccountCreateVO nonExistentDefaultPaymentMethodCreate = BillingAccountCreateVOTestExample.build()
				.financialAccount(null);
		// no existent id
		nonExistentDefaultPaymentMethodCreate.setRelatedParty(List.of(nonExistentRelatedParty));
		testEntries.add(Arguments.of("A billingAccount with non-existent related parties should not be created.",
				nonExistentDefaultPaymentMethodCreate));

		BillingAccountCreateVO nonExistentFinancialAccountCreate = BillingAccountCreateVOTestExample.build()
				.defaultPaymentMethod(null);
		// no existent id
		nonExistentFinancialAccountCreate.setRelatedParty(List.of(nonExistentRelatedParty));
		testEntries.add(Arguments.of("A billingAccount with non-existent related parties should not be created.",
				nonExistentFinancialAccountCreate));

		fixExampleCreate(invalidRelatedPartyCreate);
		fixExampleCreate(nonExistentRelatedPartyCreate);

		return testEntries.stream();
	}

	@Disabled
	@Test
	@Override
	public void createBillingAccount401() throws Exception {

	}

	@Disabled
	@Test
	@Override
	public void createBillingAccount403() throws Exception {

	}

	@Disabled
	@Test
	@Override
	public void createBillingAccount405() throws Exception {

	}

	@Disabled
	@Test
	@Override
	public void createBillingAccount409() throws Exception {

	}

	@Override
	public void createBillingAccount500() throws Exception {

	}

	@Test
	@Override
	public void deleteBillingAccount204() throws Exception {
		//first create
		BillingAccountCreateVO billingAccountCreateVO = BillingAccountCreateVOTestExample.build()
				.defaultPaymentMethod(null)
				.financialAccount(null);

		fixExampleCreate(billingAccountCreateVO);

		HttpResponse<BillingAccountVO> createResponse = callAndCatch(
				() -> billingAccountApiTestClient.createBillingAccount(null, billingAccountCreateVO));
		assertEquals(HttpStatus.CREATED, createResponse.getStatus(), "The billingAccount should have been created first.");

		String billingAccountId = createResponse.body().getId();

		assertEquals(HttpStatus.NO_CONTENT,
				callAndCatch(() -> billingAccountApiTestClient.deleteBillingAccount(null, billingAccountId)).getStatus(),
				"The billingAccount should have been deleted.");

		assertEquals(HttpStatus.NOT_FOUND,
				callAndCatch(() -> billingAccountApiTestClient.retrieveBillingAccount(null, billingAccountId, null)).status(),
				"The billingAccount should not exist anymore.");

	}

	@Disabled
	@Test
	@Override
	public void deleteBillingAccount400() throws Exception {

	}

	@Disabled
	@Test
	@Override
	public void deleteBillingAccount401() throws Exception {

	}

	@Disabled
	@Test
	@Override
	public void deleteBillingAccount403() throws Exception {

	}

	@Test
	@Override
	public void deleteBillingAccount404() throws Exception {
		HttpResponse<?> notFoundResponse = callAndCatch(
				() -> billingAccountApiTestClient.deleteBillingAccount(null, "urn:ngsi-ld:billingAccount:no-billingAccount"));
		assertEquals(HttpStatus.NOT_FOUND,
				notFoundResponse.getStatus(),
				"No such billingAccount should exist.");

		Optional<ErrorDetails> optionalErrorDetails = notFoundResponse.getBody(ErrorDetails.class);
		assertTrue(optionalErrorDetails.isPresent(), "Error details should be provided.");

		notFoundResponse = callAndCatch(() -> billingAccountApiTestClient.deleteBillingAccount(null, "invalid-id"));
		assertEquals(HttpStatus.NOT_FOUND,
				notFoundResponse.getStatus(),
				"No such billingAccount should exist.");

		optionalErrorDetails = notFoundResponse.getBody(ErrorDetails.class);
		assertTrue(optionalErrorDetails.isPresent(), "Error details should be provided.");
	}

	@Disabled
	@Test
	@Override
	public void deleteBillingAccount405() throws Exception {

	}

	@Disabled
	@Test
	@Override
	public void deleteBillingAccount409() throws Exception {

	}

	@Override
	public void deleteBillingAccount500() throws Exception {

	}

	@Test
	@Override
	public void listBillingAccount200() throws Exception {
		List<BillingAccountVO> expectedBillingAccounts = new ArrayList<>();
		for (int i = 0; i < 10; i++) {
			BillingAccountCreateVO billingAccountCreateVO = BillingAccountCreateVOTestExample.build()
					.defaultPaymentMethod(null)
					.financialAccount(null);
			fixExampleCreate(billingAccountCreateVO);
			String id = billingAccountApiTestClient.createBillingAccount(null, billingAccountCreateVO).body().getId();
			BillingAccountVO billingAccountVO = BillingAccountVOTestExample.build();
			fixExampleExpected(billingAccountVO);
			BillingCycleSpecificationRefOrValueVO billingCycleRV = billingAccountVO.getBillStructure()
					.getCycleSpecification();
			BillStructureVO billStructure = billingAccountVO.getBillStructure()
					.cycleSpecification(billingCycleRV);
			billingAccountVO
					.id(id)
					.href(id)
					.billStructure(billStructure)
					.defaultPaymentMethod(null)
					.financialAccount(null);
			expectedBillingAccounts.add(billingAccountVO);
		}

		HttpResponse<List<BillingAccountVO>> billingAccountResponse = callAndCatch(
				() -> billingAccountApiTestClient.listBillingAccount(null, null, null, null));

		assertEquals(HttpStatus.OK, billingAccountResponse.getStatus(), "The list should be accessible.");
		assertEquals(expectedBillingAccounts.size(), billingAccountResponse.getBody().get().size(),
				"All billingAccounts should have been returned.");
		List<BillingAccountVO> retrievedBillingAccounts = billingAccountResponse.getBody().get();

		Map<String, BillingAccountVO> retrievedMap = retrievedBillingAccounts.stream()
				.collect(Collectors.toMap(billingAccount -> billingAccount.getId(), billingAccount -> billingAccount));

		expectedBillingAccounts.stream()
				.forEach(expectedBillingAccount -> assertTrue(retrievedMap.containsKey(expectedBillingAccount.getId()),
						String.format("All created billingAccounts should be returned - Missing: %s.", expectedBillingAccount,
								retrievedBillingAccounts)));
		expectedBillingAccounts.stream().forEach(
				expectedBillingAccount -> assertEquals(expectedBillingAccount, retrievedMap.get(expectedBillingAccount.getId()),
						"The correct billingAccounts should be retrieved."));

		// get with pagination
		Integer limit = 5;
		HttpResponse<List<BillingAccountVO>> firstPartResponse = callAndCatch(
				() -> billingAccountApiTestClient.listBillingAccount(null, null, 0, limit));
		assertEquals(limit, firstPartResponse.body().size(),
				"Only the requested number of entries should be returend.");
		HttpResponse<List<BillingAccountVO>> secondPartResponse = callAndCatch(
				() -> billingAccountApiTestClient.listBillingAccount(null, null, 0 + limit, limit));
		assertEquals(limit, secondPartResponse.body().size(),
				"Only the requested number of entries should be returend.");

		retrievedBillingAccounts.clear();
		retrievedBillingAccounts.addAll(firstPartResponse.body());
		retrievedBillingAccounts.addAll(secondPartResponse.body());
		expectedBillingAccounts.stream()
				.forEach(expectedBillingAccount -> assertTrue(retrievedMap.containsKey(expectedBillingAccount.getId()),
						String.format("All created billingAccounts should be returned - Missing: %s.", expectedBillingAccount)));
		expectedBillingAccounts.stream().forEach(
				expectedBillingAccount -> assertEquals(expectedBillingAccount, retrievedMap.get(expectedBillingAccount.getId()),
						"The correct billingAccounts should be retrieved."));
	}

	@Test
	@Override
	public void listBillingAccount400() throws Exception {
		HttpResponse<List<BillingAccountVO>> badRequestResponse = callAndCatch(
				() -> billingAccountApiTestClient.listBillingAccount(null, null, -1, null));
		assertEquals(HttpStatus.BAD_REQUEST,
				badRequestResponse.getStatus(),
				"Negative offsets are impossible.");

		Optional<ErrorDetails> optionalErrorDetails = badRequestResponse.getBody(ErrorDetails.class);
		assertTrue(optionalErrorDetails.isPresent(), "Error details should be provided.");

		badRequestResponse = callAndCatch(() -> billingAccountApiTestClient.listBillingAccount(null, null, null, -1));
		assertEquals(HttpStatus.BAD_REQUEST,
				badRequestResponse.getStatus(),
				"Negative limits are impossible.");
		optionalErrorDetails = badRequestResponse.getBody(ErrorDetails.class);
		assertTrue(optionalErrorDetails.isPresent(), "Error details should be provided.");

	}

	@Disabled
	@Test
	@Override
	public void listBillingAccount401() throws Exception {

	}

	@Disabled
	@Test
	@Override
	public void listBillingAccount403() throws Exception {

	}

	@Disabled
	@Test
	@Override
	public void listBillingAccount404() throws Exception {

	}

	@Disabled
	@Test
	@Override
	public void listBillingAccount405() throws Exception {

	}

	@Disabled
	@Test
	@Override
	public void listBillingAccount409() throws Exception {

	}

	@Override
	public void listBillingAccount500() throws Exception {

	}

	@ParameterizedTest
	@MethodSource("provideBillingAccountUpdates")
	public void patchBillingAccount200(String message, BillingAccountUpdateVO billingAccountUpdateVO, BillingAccountVO expectedBillingAccount)
			throws Exception {
		this.message = message;
		this.billingAccountUpdateVO = billingAccountUpdateVO;
		this.expectedBillingAccount = expectedBillingAccount;
		patchBillingAccount200();
	}

	@Override
	public void patchBillingAccount200() throws Exception {
		//first create
		BillingAccountCreateVO billingAccountCreateVO = BillingAccountCreateVOTestExample.build()
				.financialAccount(null)
				.defaultPaymentMethod(null);
		fixExampleCreate(billingAccountCreateVO);
		HttpResponse<BillingAccountVO> createResponse = callAndCatch(
				() -> billingAccountApiTestClient.createBillingAccount(null, billingAccountCreateVO));
		assertEquals(HttpStatus.CREATED, createResponse.getStatus(), "The billingAccount should have been created first.");

		String billingAccountId = createResponse.body().getId();

		HttpResponse<BillingAccountVO> updateResponse = callAndCatch(
				() -> billingAccountApiTestClient.patchBillingAccount(null, billingAccountId, billingAccountUpdateVO));
		assertEquals(HttpStatus.OK, updateResponse.getStatus(), message);

		BillingAccountVO updatedBillingAccount = updateResponse.body();
		fixExampleExpected(expectedBillingAccount);
		expectedBillingAccount.href(billingAccountId)
				.id(billingAccountId)
				.financialAccount(null)
				.defaultPaymentMethod(null);
		BillingCycleSpecificationRefOrValueVO billingCycleRV = expectedBillingAccount.getBillStructure()
				.getCycleSpecification();
		BillStructureVO billStructure = expectedBillingAccount.getBillStructure()
				.cycleSpecification(billingCycleRV);
		expectedBillingAccount.billStructure(billStructure);

		assertEquals(expectedBillingAccount, updatedBillingAccount, message);
	}

	private static Stream<Arguments> provideBillingAccountUpdates() {
		List<Arguments> testEntries = new ArrayList<>();

		BillingAccountUpdateVO newTypeBillingAccount = BillingAccountUpdateVOTestExample.build()
				.financialAccount(null)
				.defaultPaymentMethod(null);
		fixExampleUpdate(newTypeBillingAccount);
		newTypeBillingAccount.setAccountType("New-Type");
		BillingAccountVO expectedNewType = BillingAccountVOTestExample.build();
		expectedNewType.setAccountType("New-Type");
		testEntries.add(Arguments.of("The type should have been updated.", newTypeBillingAccount, expectedNewType));

		BillingAccountUpdateVO newDesc = BillingAccountUpdateVOTestExample.build()
				.financialAccount(null)
				.defaultPaymentMethod(null);
		fixExampleUpdate(newDesc);
		newDesc.setDescription("New description");
		BillingAccountVO expectedNewDesc = BillingAccountVOTestExample.build();
		expectedNewDesc.setDescription("New description");
		testEntries.add(Arguments.of("The description should have been updated.", newDesc, expectedNewDesc));

		BillingAccountUpdateVO newName = BillingAccountUpdateVOTestExample.build()
				.financialAccount(null)
				.defaultPaymentMethod(null);
		fixExampleUpdate(newName);
		newName.setName("New name");
		BillingAccountVO expectedNewName = BillingAccountVOTestExample.build();
		expectedNewName.setName("New name");
		testEntries.add(Arguments.of("The name should have been updated.", newName, expectedNewName));

		return testEntries.stream();
	}

	@ParameterizedTest
	@MethodSource("provideInvalidUpdates")
	public void patchBillingAccount400(String message, BillingAccountUpdateVO invalidUpdateVO) throws Exception {
		this.message = message;
		this.billingAccountUpdateVO = invalidUpdateVO;
		patchBillingAccount400();
	}

	@Override
	public void patchBillingAccount400() throws Exception {
		//first create
		BillingAccountCreateVO billingAccountCreateVO = BillingAccountCreateVOTestExample.build()
				.financialAccount(null)
				.defaultPaymentMethod(null);
		fixExampleCreate(billingAccountCreateVO);
		HttpResponse<BillingAccountVO> createResponse = callAndCatch(
				() -> billingAccountApiTestClient.createBillingAccount(null, billingAccountCreateVO));
		assertEquals(HttpStatus.CREATED, createResponse.getStatus(), "The billingAccount should have been created first.");

		String billingAccountId = createResponse.body().getId();

		HttpResponse<BillingAccountVO> updateResponse = callAndCatch(
				() -> billingAccountApiTestClient.patchBillingAccount(null, billingAccountId, billingAccountUpdateVO));
		assertEquals(HttpStatus.BAD_REQUEST, updateResponse.getStatus(), message);

		Optional<ErrorDetails> optionalErrorDetails = updateResponse.getBody(ErrorDetails.class);
		assertTrue(optionalErrorDetails.isPresent(), "Some error details should be present.");
	}

	private static Stream<Arguments> provideInvalidUpdates() {
		List<Arguments> testEntries = new ArrayList<>();

		BillingAccountUpdateVO invalidRelatedPartyUpdate = BillingAccountUpdateVOTestExample.build()
				.financialAccount(null)
				.defaultPaymentMethod(null);
		fixExampleUpdate(invalidRelatedPartyUpdate);
		// no valid id
		RelatedPartyVO invalidRelatedParty = RelatedPartyVOTestExample.build();
		invalidRelatedPartyUpdate.setRelatedParty(List.of(invalidRelatedParty));
		testEntries.add(Arguments.of("A billingAccount with invalid related parties should not be updated.",
				invalidRelatedPartyUpdate));

		BillingAccountUpdateVO nonExistentRelatedPartyUpdate = BillingAccountUpdateVOTestExample.build()
				.financialAccount(null)
				.defaultPaymentMethod(null);
		fixExampleUpdate(nonExistentRelatedPartyUpdate);
		// no existent id
		RelatedPartyVO nonExistentRelatedParty = RelatedPartyVOTestExample.build();
		nonExistentRelatedParty.setId("urn:ngsi-ld:individual:non-existent");
		nonExistentRelatedPartyUpdate.setRelatedParty(List.of(nonExistentRelatedParty));
		testEntries.add(Arguments.of("A billingAccount with non-existent related parties should not be updated.",
				nonExistentRelatedPartyUpdate));

		BillingAccountUpdateVO nonExistentFinancialAccountUpdate = BillingAccountUpdateVOTestExample.build()
				.defaultPaymentMethod(null);
		fixExampleUpdate(nonExistentFinancialAccountUpdate);
		testEntries.add(Arguments.of("A billingAccount with non-existent Financial Account should not be updated.",
				nonExistentFinancialAccountUpdate));

		BillingAccountUpdateVO nonExistentDefaultPaymentMethodUpdate = BillingAccountUpdateVOTestExample.build()
				.financialAccount(null);
		fixExampleUpdate(nonExistentDefaultPaymentMethodUpdate);
		testEntries.add(Arguments.of("A billingAccount with non-existent default payment method should not be updated.",
				nonExistentDefaultPaymentMethodUpdate));

		return testEntries.stream();
	}

	@Disabled
	@Test
	@Override
	public void patchBillingAccount401() throws Exception {

	}

	@Disabled
	@Test
	@Override
	public void patchBillingAccount403() throws Exception {

	}

	@Test
	@Override
	public void patchBillingAccount404() throws Exception {
		BillingAccountUpdateVO billingAccountUpdateVO = BillingAccountUpdateVOTestExample.build();
		assertEquals(
				HttpStatus.NOT_FOUND,
				callAndCatch(() -> billingAccountApiTestClient.patchBillingAccount(null, "urn:ngsi-ld:billingAccount:not-existent",
						billingAccountUpdateVO)).getStatus(),
				"Non existent billingAccounts should not be updated.");
	}

	@Disabled
	@Test
	@Override
	public void patchBillingAccount405() throws Exception {

	}

	@Disabled
	@Test
	@Override
	public void patchBillingAccount409() throws Exception {

	}

	@Override
	public void patchBillingAccount500() throws Exception {

	}

	@Test
	@Override
	public void retrieveBillingAccount200() throws Exception {

		//first create
		BillingAccountCreateVO billingAccountCreateVO = BillingAccountCreateVOTestExample.build()
				.financialAccount(null)
				.defaultPaymentMethod(null);
		fixExampleCreate(billingAccountCreateVO);
		HttpResponse<BillingAccountVO> createResponse = callAndCatch(
				() -> billingAccountApiTestClient.createBillingAccount(null, billingAccountCreateVO));
		assertEquals(HttpStatus.CREATED, createResponse.getStatus(), "The billingAccount should have been created first.");
		String id = createResponse.body().getId();

		BillingAccountVO expectedBillingAccount = BillingAccountVOTestExample.build();
		expectedBillingAccount.setId(id);
		expectedBillingAccount.setHref(id);
		expectedBillingAccount.financialAccount(null).defaultPaymentMethod(null);
		fixExampleExpected(expectedBillingAccount);
		BillingCycleSpecificationRefOrValueVO billingCycleRV = expectedBillingAccount.getBillStructure()
				.getCycleSpecification();
		BillStructureVO billStructure = expectedBillingAccount.getBillStructure()
				.cycleSpecification(billingCycleRV);
		expectedBillingAccount.billStructure(billStructure);

		//then retrieve
		HttpResponse<BillingAccountVO> retrievedBillingAccount = callAndCatch(() -> billingAccountApiTestClient.retrieveBillingAccount(null, id, null));
		assertEquals(HttpStatus.OK, retrievedBillingAccount.getStatus(), "The retrieval should be ok.");
		assertEquals(expectedBillingAccount, retrievedBillingAccount.body(), "The correct billingAccount should be returned.");
	}

	@Disabled
	@Test
	@Override
	public void retrieveBillingAccount400() throws Exception {

	}

	@Override
	public void retrieveBillingAccount401() throws Exception {

	}

	@Disabled
	@Test
	@Override
	public void retrieveBillingAccount403() throws Exception {

	}

	@Test
	@Override
	public void retrieveBillingAccount404() throws Exception {
		HttpResponse<BillingAccountVO> response = callAndCatch(
				() -> billingAccountApiTestClient.retrieveBillingAccount(null, "urn:ngsi-ld:billingAccount:non-existent", null));
		assertEquals(HttpStatus.NOT_FOUND, response.getStatus(), "No such billingAccount should exist.");

		Optional<ErrorDetails> optionalErrorDetails = response.getBody(ErrorDetails.class);
		assertTrue(optionalErrorDetails.isPresent(), "Error details should have been provided.");
	}

	@Disabled
	@Test
	@Override
	public void retrieveBillingAccount405() throws Exception {

	}

	@Disabled
	@Test
	@Override
	public void retrieveBillingAccount409() throws Exception {

	}

	@Override
	public void retrieveBillingAccount500() throws Exception {

	}
}
