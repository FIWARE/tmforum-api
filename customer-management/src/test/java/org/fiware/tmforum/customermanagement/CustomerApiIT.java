package org.fiware.tmforum.customermanagement;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import io.micronaut.test.annotation.MockBean;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import org.fiware.customermanagement.api.CustomerApiTestClient;
import org.fiware.customermanagement.api.CustomerApiTestSpec;
import org.fiware.customermanagement.model.*;
import org.fiware.ngsi.api.EntitiesApiClient;
import org.fiware.tmforum.common.configuration.GeneralProperties;
import org.fiware.tmforum.common.exception.ErrorDetails;
import org.fiware.tmforum.common.notification.TMForumEventHandler;
import org.fiware.tmforum.common.test.AbstractApiIT;
import org.fiware.tmforum.customermanagement.domain.Customer;
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

@MicronautTest(packages = {"org.fiware.tmforum.customermanagement"})
public class CustomerApiIT extends AbstractApiIT implements CustomerApiTestSpec {

	public final CustomerApiTestClient customerApiTestClient;
	private final EntitiesApiClient entitiesApiClient;
	private final ObjectMapper objectMapper;

	private String message;
	private String fieldsParameter;
	private CustomerCreateVO customerCreateVO;
	private CustomerUpdateVO customerUpdateVO;
	private CustomerVO expectedCustomer;

	public CustomerApiIT(CustomerApiTestClient customerApiTestClient, EntitiesApiClient entitiesApiClient,
						 ObjectMapper objectMapper, GeneralProperties generalProperties) {
		super(entitiesApiClient, objectMapper, generalProperties);
		this.customerApiTestClient = customerApiTestClient;
		this.entitiesApiClient = entitiesApiClient;
		this.objectMapper = objectMapper;
	}

	@MockBean(TMForumEventHandler.class)
	public TMForumEventHandler eventHandler() {
		TMForumEventHandler eventHandler = mock(TMForumEventHandler.class);

		when(eventHandler.handleCreateEvent(any())).thenReturn(Mono.empty());
		when(eventHandler.handleUpdateEvent(any(), any())).thenReturn(Mono.empty());

		return eventHandler;
	}

	@ParameterizedTest
	@MethodSource("provideValidCustomers")
	public void createCustomer201(String message, CustomerCreateVO customerCreateVO, CustomerVO expectedCustomer)
			throws Exception {
		this.message = message;
		this.customerCreateVO = customerCreateVO;
		this.expectedCustomer = expectedCustomer;
		createCustomer201();
	}

	@Override
	public void createCustomer201() throws Exception {

		HttpResponse<CustomerVO> customerVOHttpResponse = callAndCatch(
				() -> customerApiTestClient.createCustomer(null, customerCreateVO));
		assertEquals(HttpStatus.CREATED, customerVOHttpResponse.getStatus(), message);
		String rfId = customerVOHttpResponse.body().getId();
		expectedCustomer.id(rfId)
				.href(rfId);

		assertEquals(expectedCustomer, customerVOHttpResponse.body(), message);
	}

	private static Stream<Arguments> provideValidCustomers() {
		List<Arguments> testEntries = new ArrayList<>();

		CustomerCreateVO emptyCreate = CustomerCreateVOTestExample.build().atSchemaLocation(null).engagedParty(null);
		CustomerVO expectedEmpty = CustomerVOTestExample.build().atSchemaLocation(null).engagedParty(null);
		testEntries.add(Arguments.of("An empty customer should have been created.", emptyCreate, expectedEmpty));

		TimePeriodVO timePeriodVO = TimePeriodVOTestExample.build().endDateTime(Instant.now())
				.startDateTime(Instant.now());
		CustomerCreateVO createValidFor = CustomerCreateVOTestExample.build().atSchemaLocation(null).validFor(timePeriodVO).engagedParty(null);
		CustomerVO expectedValidFor = CustomerVOTestExample.build().atSchemaLocation(null).validFor(timePeriodVO).engagedParty(null);
		testEntries.add(Arguments.of("An customer with a validFor should have been created.", createValidFor,
				expectedValidFor));

		return testEntries.stream();
	}

	@ParameterizedTest
	@MethodSource("provideInvalidCustomers")
	public void createCustomer400(String message, CustomerCreateVO invalidCreateVO) throws Exception {
		this.message = message;
		this.customerCreateVO = invalidCreateVO;
		createCustomer400();
	}

	@Override
	public void createCustomer400() throws Exception {
		HttpResponse<CustomerVO> creationResponse = callAndCatch(
				() -> customerApiTestClient.createCustomer(null, customerCreateVO));
		assertEquals(HttpStatus.BAD_REQUEST, creationResponse.getStatus(), message);
		Optional<ErrorDetails> optionalErrorDetails = creationResponse.getBody(ErrorDetails.class);
		assertTrue(optionalErrorDetails.isPresent(), "Error details should be provided.");
	}

	private static Stream<Arguments> provideInvalidCustomers() {
		List<Arguments> testEntries = new ArrayList<>();

		testEntries.add(Arguments.of("A customer with an invalid engaged party should not be created.",
				CustomerCreateVOTestExample.build().atSchemaLocation(null).engagedParty(RelatedPartyVOTestExample.build().atSchemaLocation(null))));
		testEntries.add(Arguments.of("A customer with a non-existent engaged party should not be created.",
				CustomerCreateVOTestExample.build().atSchemaLocation(null)
						.engagedParty(RelatedPartyVOTestExample.build().atSchemaLocation(null).id("urn:ngsi-ld:organization:non-existent"))));

		testEntries.add(Arguments.of("A customer with an invalid account should not be created.",
				CustomerCreateVOTestExample.build().atSchemaLocation(null).engagedParty(null)
						.account(List.of(AccountRefVOTestExample.build().atSchemaLocation(null)))));
		testEntries.add(Arguments.of("A customer with a non-existent account should not be created.",
				CustomerCreateVOTestExample.build().atSchemaLocation(null).engagedParty(null)
						.account(List.of(AccountRefVOTestExample.build().atSchemaLocation(null).id("urn:ngsi-ld:account:non-existent")))));

		testEntries.add(Arguments.of("A customer with an invalid agreement should not be created.",
				CustomerCreateVOTestExample.build().atSchemaLocation(null).engagedParty(null)
						.agreement(List.of(AgreementRefVOTestExample.build().atSchemaLocation(null)))));
		testEntries.add(Arguments.of("A customer with a non-existent agreement should not be created.",
				CustomerCreateVOTestExample.build().atSchemaLocation(null).engagedParty(null).agreement(
						List.of(AgreementRefVOTestExample.build().atSchemaLocation(null).id("urn:ngsi-ld:agreement:non-existent")))));

		testEntries.add(Arguments.of("A customer with an invalid paymentMethod should not be created.",
				CustomerCreateVOTestExample.build().atSchemaLocation(null).engagedParty(null)
						.paymentMethod(List.of(PaymentMethodRefVOTestExample.build().atSchemaLocation(null)))));
		testEntries.add(Arguments.of("A customer with a non-existent paymentMethod should not be created.",
				CustomerCreateVOTestExample.build().atSchemaLocation(null).engagedParty(null).paymentMethod(
						List.of(PaymentMethodRefVOTestExample.build().atSchemaLocation(null).id("urn:ngsi-ld:payment-method:non-existent")))));

		testEntries.add(Arguments.of("A customer with an invalid related party should not be created.",
				CustomerCreateVOTestExample.build().atSchemaLocation(null).engagedParty(null)
						.relatedParty(List.of(RelatedPartyVOTestExample.build().atSchemaLocation(null)))));
		testEntries.add(Arguments.of("A customer with a non-existent related party should not be created.",
				CustomerCreateVOTestExample.build().atSchemaLocation(null).engagedParty(null).relatedParty(
						List.of(RelatedPartyVOTestExample.build().atSchemaLocation(null).id("urn:ngsi-ld:organization:non-existent")))));

		return testEntries.stream();
	}

	@Disabled("Security is handled externally, thus 401 and 403 cannot happen.")
	@Test
	@Override
	public void createCustomer401() throws Exception {

	}

	@Disabled("Security is handled externally, thus 401 and 403 cannot happen.")
	@Test
	@Override
	public void createCustomer403() throws Exception {

	}

	@Disabled("Prohibited by the framework.")
	@Test
	@Override
	public void createCustomer405() throws Exception {
	}

	@Disabled("No implicit creation, impossible state.")
	@Test
	@Override
	public void createCustomer409() throws Exception {

	}

	@Override
	public void createCustomer500() throws Exception {

	}

	@Test
	@Override
	public void deleteCustomer204() throws Exception {
		CustomerCreateVO emptyCreate = CustomerCreateVOTestExample.build().atSchemaLocation(null).engagedParty(null);

		HttpResponse<CustomerVO> createResponse = customerApiTestClient.createCustomer(null, emptyCreate);
		assertEquals(HttpStatus.CREATED, createResponse.getStatus(), "The customer should have been created first.");

		String rfId = createResponse.body().getId();

		assertEquals(HttpStatus.NO_CONTENT,
				callAndCatch(() -> customerApiTestClient.deleteCustomer(null, rfId)).getStatus(),
				"The customer should have been deleted.");

		assertEquals(HttpStatus.NOT_FOUND,
				callAndCatch(() -> customerApiTestClient.retrieveCustomer(null, rfId, null)).status(),
				"The customer should not exist anymore.");
	}

	@Disabled("400 is impossible to happen on deletion with the current implementation.")
	@Test
	@Override
	public void deleteCustomer400() throws Exception {

	}

	@Disabled("Security is handled externally, thus 401 and 403 cannot happen.")
	@Test
	@Override
	public void deleteCustomer401() throws Exception {

	}

	@Disabled("Security is handled externally, thus 401 and 403 cannot happen.")
	@Test
	@Override
	public void deleteCustomer403() throws Exception {

	}

	@Test
	@Override
	public void deleteCustomer404() throws Exception {
		HttpResponse<?> notFoundResponse = callAndCatch(
				() -> customerApiTestClient.deleteCustomer(null, "urn:ngsi-ld:service-category:no-pop"));
		assertEquals(HttpStatus.NOT_FOUND,
				notFoundResponse.getStatus(),
				"No such service-category should exist.");

		Optional<ErrorDetails> optionalErrorDetails = notFoundResponse.getBody(ErrorDetails.class);
		assertTrue(optionalErrorDetails.isPresent(), "Error details should be provided.");

		notFoundResponse = callAndCatch(() -> customerApiTestClient.deleteCustomer(null, "invalid-id"));
		assertEquals(HttpStatus.NOT_FOUND,
				notFoundResponse.getStatus(),
				"No such service-category should exist.");

		optionalErrorDetails = notFoundResponse.getBody(ErrorDetails.class);
		assertTrue(optionalErrorDetails.isPresent(), "Error details should be provided.");
	}

	@Disabled("Prohibited by the framework.")
	@Test
	@Override
	public void deleteCustomer405() throws Exception {

	}

	@Disabled("Impossible status.")
	@Test
	@Override
	public void deleteCustomer409() throws Exception {

	}

	@Override
	public void deleteCustomer500() throws Exception {

	}

	@Test
	@Override
	public void listCustomer200() throws Exception {
		List<CustomerVO> expectedCustomers = new ArrayList<>();
		for (int i = 0; i < 10; i++) {
			CustomerCreateVO customerCreateVO = CustomerCreateVOTestExample.build().atSchemaLocation(null)
					.engagedParty(null);
			String id = customerApiTestClient.createCustomer(null, customerCreateVO).body().getId();
			CustomerVO customerVO = CustomerVOTestExample.build().atSchemaLocation(null);
			customerVO
					.id(id)
					.href(id)
					.engagedParty(null)
					.account(null)
					.paymentMethod(null)
					.validFor(null);
			expectedCustomers.add(customerVO);
		}

		HttpResponse<List<CustomerVO>> customerResponse = callAndCatch(
				() -> customerApiTestClient.listCustomer(null, null, null, null));

		assertEquals(HttpStatus.OK, customerResponse.getStatus(), "The list should be accessible.");
		assertEquals(expectedCustomers.size(), customerResponse.getBody().get().size(),
				"All bills should have been returend.");
		List<CustomerVO> retrievedBills = customerResponse.getBody().get();

		Map<String, CustomerVO> retrievedMap = retrievedBills.stream()
				.collect(Collectors.toMap(bill -> bill.getId(), bill -> bill));

		expectedCustomers.stream()
				.forEach(expectedCustomer -> assertTrue(retrievedMap.containsKey(expectedCustomer.getId()),
						String.format("All created customers should be returned - Missing: %s.", expectedCustomer,
								retrievedBills)));
		expectedCustomers.stream().forEach(
				expectedCustomer -> assertEquals(expectedCustomer, retrievedMap.get(expectedCustomer.getId()),
						"The correct customers should be retrieved."));

		// get with pagination
		Integer limit = 5;
		HttpResponse<List<CustomerVO>> firstPartResponse = callAndCatch(
				() -> customerApiTestClient.listCustomer(null, null, 0, limit));
		assertEquals(limit, firstPartResponse.body().size(),
				"Only the requested number of entries should be returend.");
		HttpResponse<List<CustomerVO>> secondPartResponse = callAndCatch(
				() -> customerApiTestClient.listCustomer(null, null, 0 + limit, limit));
		assertEquals(limit, secondPartResponse.body().size(),
				"Only the requested number of entries should be returend.");

		retrievedBills.clear();
		retrievedBills.addAll(firstPartResponse.body());
		retrievedBills.addAll(secondPartResponse.body());
		expectedCustomers.stream().forEach(expectedBill -> assertTrue(retrievedMap.containsKey(expectedBill.getId()),
				String.format("All created customers should be returned - Missing: %s.", expectedBill)));
		expectedCustomers.stream().forEach(
				expectedBill -> assertEquals(expectedBill, retrievedMap.get(expectedBill.getId()),
						"The correct customers should be retrieved."));
	}

	@Test
	@Override
	public void listCustomer400() throws Exception {
		HttpResponse<List<CustomerVO>> badRequestResponse = callAndCatch(
				() -> customerApiTestClient.listCustomer(null, null, -1, null));
		assertEquals(HttpStatus.BAD_REQUEST,
				badRequestResponse.getStatus(),
				"Negative offsets are impossible.");

		Optional<ErrorDetails> optionalErrorDetails = badRequestResponse.getBody(ErrorDetails.class);
		assertTrue(optionalErrorDetails.isPresent(), "Error details should be provided.");

		badRequestResponse = callAndCatch(() -> customerApiTestClient.listCustomer(null, null, null, -1));
		assertEquals(HttpStatus.BAD_REQUEST,
				badRequestResponse.getStatus(),
				"Negative limits are impossible.");
		optionalErrorDetails = badRequestResponse.getBody(ErrorDetails.class);
		assertTrue(optionalErrorDetails.isPresent(), "Error details should be provided.");
	}

	@Disabled("Security is handled externally, thus 401 and 403 cannot happen.")
	@Test
	@Override
	public void listCustomer401() throws Exception {

	}

	@Disabled("Security is handled externally, thus 401 and 403 cannot happen.")
	@Test
	@Override
	public void listCustomer403() throws Exception {

	}

	@Disabled("Not found is not possible here, will be answered with an empty list instead.")
	@Test
	@Override
	public void listCustomer404() throws Exception {

	}

	@Disabled("Prohibited by the framework.")
	@Test
	@Override
	public void listCustomer405() throws Exception {

	}

	@Disabled("Impossible status.")
	@Test
	@Override
	public void listCustomer409() throws Exception {

	}

	@Override
	public void listCustomer500() throws Exception {

	}

	@ParameterizedTest
	@MethodSource("provideCustomerUpdates")
	public void patchCustomer200(String message, CustomerUpdateVO customerUpdateVO, CustomerVO expectedCustomer)
			throws Exception {
		this.message = message;
		this.customerUpdateVO = customerUpdateVO;
		this.expectedCustomer = expectedCustomer;
		patchCustomer200();
	}

	@Override
	public void patchCustomer200() throws Exception {
		//first create
		CustomerCreateVO customerCreateVO = CustomerCreateVOTestExample.build().atSchemaLocation(null).engagedParty(null);

		HttpResponse<CustomerVO> createResponse = callAndCatch(
				() -> customerApiTestClient.createCustomer(null, customerCreateVO));
		assertEquals(HttpStatus.CREATED, createResponse.getStatus(), "The customer should have been created first.");

		String resourceId = createResponse.body().getId();

		HttpResponse<CustomerVO> updateResponse = callAndCatch(
				() -> customerApiTestClient.patchCustomer(null, resourceId, customerUpdateVO));
		assertEquals(HttpStatus.OK, updateResponse.getStatus(), message);

		CustomerVO updatedCustomer = updateResponse.body();
		expectedCustomer.href(resourceId).id(resourceId);

		assertEquals(expectedCustomer, updatedCustomer, message);
	}

	private static Stream<Arguments> provideCustomerUpdates() {
		List<Arguments> testEntries = new ArrayList<>();

		testEntries.add(Arguments.of("The name should have been updated.",
				CustomerUpdateVOTestExample.build().atSchemaLocation(null)
						.engagedParty(null)
						.name("Max Mustermann"),
				CustomerVOTestExample.build().atSchemaLocation(null)
						.engagedParty(null)
						.account(null)
						.paymentMethod(null)
						.validFor(null)
						.name("Max Mustermann")));

		testEntries.add(Arguments.of("The status should have been updated.",
				CustomerUpdateVOTestExample.build().atSchemaLocation(null)
						.engagedParty(null)
						.name("pending"),
				CustomerVOTestExample.build().atSchemaLocation(null)
						.engagedParty(null)
						.account(null)
						.paymentMethod(null)
						.validFor(null)
						.name("pending")));

		testEntries.add(Arguments.of("The statusReason should have been updated.",
				CustomerUpdateVOTestExample.build().atSchemaLocation(null)
						.engagedParty(null)
						.statusReason("not signed"),
				CustomerVOTestExample.build().atSchemaLocation(null)
						.engagedParty(null)
						.account(null)
						.paymentMethod(null)
						.validFor(null)
						.statusReason("not signed")));

		testEntries.add(Arguments.of("Characteristics should have been updated.",
				CustomerUpdateVOTestExample.build().atSchemaLocation(null)
						.engagedParty(null)
						.characteristic(List.of(CharacteristicVOTestExample.build().atSchemaLocation(null))),
				CustomerVOTestExample.build().atSchemaLocation(null)
						.engagedParty(null)
						.account(null)
						.paymentMethod(null)
						.validFor(null)
						.characteristic(List.of(CharacteristicVOTestExample.build().atSchemaLocation(null)))));

		testEntries.add(Arguments.of("ContactMedium should have been updated.",
				CustomerUpdateVOTestExample.build().atSchemaLocation(null)
						.engagedParty(null)
						.contactMedium(List.of(ContactMediumVOTestExample.build().characteristic(null).atSchemaLocation(null)
								.validFor(TimePeriodVOTestExample.build().endDateTime(Instant.MAX)
										.startDateTime(Instant.MIN)))),
				CustomerVOTestExample.build().atSchemaLocation(null)
						.engagedParty(null)
						.account(null)
						.paymentMethod(null)
						.validFor(null)
						.contactMedium(List.of(ContactMediumVOTestExample.build().characteristic(null).atSchemaLocation(null)
								.validFor(TimePeriodVOTestExample.build().endDateTime(Instant.MAX)
										.startDateTime(Instant.MIN))))));

		testEntries.add(Arguments.of("CreditProfile should have been updated.",
				CustomerUpdateVOTestExample.build().atSchemaLocation(null)
						.engagedParty(null)
						.creditProfile(List.of(CreditProfileVOTestExample.build().atSchemaLocation(null)
								.validFor(TimePeriodVOTestExample.build().endDateTime(Instant.MAX)
										.startDateTime(Instant.MIN)))),
				CustomerVOTestExample.build().atSchemaLocation(null)
						.engagedParty(null)
						.account(null)
						.paymentMethod(null)
						.validFor(null)
						.creditProfile(List.of(CreditProfileVOTestExample.build().atSchemaLocation(null)
								.validFor(TimePeriodVOTestExample.build().endDateTime(Instant.MAX)
										.startDateTime(Instant.MIN))))));

		TimePeriodVO timePeriodVO = TimePeriodVOTestExample.build().endDateTime(Instant.now())
				.startDateTime(Instant.now());
		testEntries.add(Arguments.of("The validFor should have been updated.",
				CustomerUpdateVOTestExample.build().atSchemaLocation(null).validFor(timePeriodVO)
						.engagedParty(null),
				CustomerVOTestExample.build().atSchemaLocation(null).validFor(timePeriodVO)
						.engagedParty(null)
						.account(null)
						.paymentMethod(null)));

		return testEntries.stream();
	}

	@ParameterizedTest
	@MethodSource("provideInvalidUpdates")
	public void patchCustomer400(String message, CustomerUpdateVO invalidUpdateVO) throws Exception {
		this.message = message;
		this.customerUpdateVO = invalidUpdateVO;
		patchCustomer400();
	}

	@Override
	public void patchCustomer400() throws Exception {
		//first create
		CustomerCreateVO customerCreateVO = CustomerCreateVOTestExample.build().atSchemaLocation(null).engagedParty(null);

		HttpResponse<CustomerVO> createResponse = callAndCatch(
				() -> customerApiTestClient.createCustomer(null, customerCreateVO));
		assertEquals(HttpStatus.CREATED, createResponse.getStatus(), "The customer should have been created first.");

		String resourceId = createResponse.body().getId();

		HttpResponse<CustomerVO> updateResponse = callAndCatch(
				() -> customerApiTestClient.patchCustomer(null, resourceId, customerUpdateVO));
		assertEquals(HttpStatus.BAD_REQUEST, updateResponse.getStatus(), message);

		Optional<ErrorDetails> optionalErrorDetails = updateResponse.getBody(ErrorDetails.class);
		assertTrue(optionalErrorDetails.isPresent(), "Some error details should be present.");
	}

	private static Stream<Arguments> provideInvalidUpdates() {
		List<Arguments> testEntries = new ArrayList<>();

		testEntries.add(Arguments.of("An update with an invalid engaged party is not allowed.",
				CustomerUpdateVOTestExample.build().atSchemaLocation(null)
						.engagedParty(RelatedPartyVOTestExample.build().atSchemaLocation(null).id("invalid"))));
		testEntries.add(Arguments.of("An update with an non existent engaged party is not allowed.",
				CustomerUpdateVOTestExample.build().atSchemaLocation(null)
						.engagedParty(RelatedPartyVOTestExample.build().atSchemaLocation(null).id("urn:ngsi-ld:organization:non-existent"))));

		testEntries.add(Arguments.of("An update with an invalid account is not allowed.",
				CustomerUpdateVOTestExample.build().atSchemaLocation(null)
						.account(List.of(AccountRefVOTestExample.build().atSchemaLocation(null).id("invalid")))));
		testEntries.add(Arguments.of("An update with an non existent account is not allowed.",
				CustomerUpdateVOTestExample.build().atSchemaLocation(null)
						.account(List.of(AccountRefVOTestExample.build().atSchemaLocation(null).id("urn:ngsi-ld:account:non-existent")))));

		testEntries.add(Arguments.of("An update with an invalid agreement is not allowed.",
				CustomerUpdateVOTestExample.build().atSchemaLocation(null)
						.agreement(List.of(AgreementRefVOTestExample.build().atSchemaLocation(null).id("invalid")))));
		testEntries.add(Arguments.of("An update with an non existent agreement is not allowed.",
				CustomerUpdateVOTestExample.build().atSchemaLocation(null)
						.agreement(
								List.of(AgreementRefVOTestExample.build().atSchemaLocation(null).id("urn:ngsi-ld:agreement:non-existent")))));

		testEntries.add(Arguments.of("An update with an invalid paymentMethod is not allowed.",
				CustomerUpdateVOTestExample.build().atSchemaLocation(null)
						.paymentMethod(List.of(PaymentMethodRefVOTestExample.build().atSchemaLocation(null).id("invalid")))));
		testEntries.add(Arguments.of("An update with an non existent paymentMethod is not allowed.",
				CustomerUpdateVOTestExample.build().atSchemaLocation(null)
						.paymentMethod(List.of(PaymentMethodRefVOTestExample.build().atSchemaLocation(null)
								.id("urn:ngsi-ld:payment-method:non-existent")))));

		testEntries.add(Arguments.of("An update with an invalid relatedParty is not allowed.",
				CustomerUpdateVOTestExample.build().atSchemaLocation(null)
						.relatedParty(List.of(RelatedPartyVOTestExample.build().atSchemaLocation(null).id("invalid")))));
		testEntries.add(Arguments.of("An update with an non existent relatedParty is not allowed.",
				CustomerUpdateVOTestExample.build().atSchemaLocation(null)
						.relatedParty(List.of(RelatedPartyVOTestExample.build().atSchemaLocation(null)
								.id("urn:ngsi-ld:organization:non-existent")))));

		return testEntries.stream();
	}

	@Disabled("Security is handled externally, thus 401 and 403 cannot happen.")
	@Test
	@Override
	public void patchCustomer401() throws Exception {

	}

	@Disabled("Security is handled externally, thus 401 and 403 cannot happen.")
	@Test
	@Override
	public void patchCustomer403() throws Exception {

	}

	@Test
	@Override
	public void patchCustomer404() throws Exception {
		CustomerUpdateVO customerUpdateVO = CustomerUpdateVOTestExample.build().atSchemaLocation(null).engagedParty(null);
		assertEquals(
				HttpStatus.NOT_FOUND,
				callAndCatch(() -> customerApiTestClient.patchCustomer(null, "urn:ngsi-ld:service-category:not-existent",
						customerUpdateVO)).getStatus(),
				"Non existent customer should not be updated.");
	}

	@Disabled("Prohibited by the framework.")
	@Test
	@Override
	public void patchCustomer405() throws Exception {

	}

	@Override
	public void patchCustomer409() throws Exception {
		// TODO: can this happen?
	}

	@Override
	public void patchCustomer500() throws Exception {

	}

	@ParameterizedTest
	@MethodSource("provideFieldParameters")
	public void retrieveCustomer200(String message, String fields, CustomerVO expectedCustomer) throws Exception {
		this.fieldsParameter = fields;
		this.message = message;
		this.expectedCustomer = expectedCustomer;
		retrieveCustomer200();
	}

	@Override
	public void retrieveCustomer200() throws Exception {

		CustomerCreateVO customerCreateVO = CustomerCreateVOTestExample.build()
				.atSchemaLocation(null)
				.engagedParty(null);
		HttpResponse<CustomerVO> createResponse = callAndCatch(
				() -> customerApiTestClient.createCustomer(null, customerCreateVO));
		assertEquals(HttpStatus.CREATED, createResponse.getStatus(), message);
		String id = createResponse.body().getId();

		expectedCustomer
				.id(id)
				.href(id);

		//then retrieve
		HttpResponse<CustomerVO> retrievedRF = callAndCatch(
				() -> customerApiTestClient.retrieveCustomer(null, id, fieldsParameter));
		assertEquals(HttpStatus.OK, retrievedRF.getStatus(), message);
		assertEquals(expectedCustomer, retrievedRF.body(), message);
	}

	private static Stream<Arguments> provideFieldParameters() {
		return Stream.of(
				Arguments.of("Without a fields parameter everything should be returned.", null,
						CustomerVOTestExample.build().atSchemaLocation(null)
								// get nulled without values
								.engagedParty(null)
								.account(null)
								.paymentMethod(null)
								//empty objects are ignored
								.validFor(null)),
				Arguments.of("Only name and the mandatory parameters should have been included.", "name",
						CustomerVOTestExample.build().atSchemaLocation(null)
								.status(null)
								.statusReason(null)
								.characteristic(null)
								.contactMedium(null)
								.creditProfile(null)
								.validFor(null)
								.engagedParty(null)
								.account(null)
								.agreement(null)
								.paymentMethod(null)
								.relatedParty(null)
								.atType(null)
								.atBaseType(null)
								.atSchemaLocation(null)),
				Arguments.of(
						"Only the mandatory parameters should have been included when a non-existent field was requested.",
						"nothingToSeeHere", CustomerVOTestExample.build().atSchemaLocation(null)
								.name(null)
								.status(null)
								.statusReason(null)
								.characteristic(null)
								.contactMedium(null)
								.creditProfile(null)
								.validFor(null)
								.engagedParty(null)
								.account(null)
								.agreement(null)
								.paymentMethod(null)
								.relatedParty(null)
								.atType(null)
								.atBaseType(null)
								.atSchemaLocation(null)),
				Arguments.of("Only name, status, statusReason and the mandatory parameters should have been included.",
						"name,status,statusReason", CustomerVOTestExample.build().atSchemaLocation(null)
								.validFor(null)
								.characteristic(null)
								.contactMedium(null)
								.creditProfile(null)
								.engagedParty(null)
								.account(null)
								.agreement(null)
								.paymentMethod(null)
								.relatedParty(null)
								.atType(null)
								.atBaseType(null)
								.atSchemaLocation(null)));
	}

	@Disabled("400 cannot happen, only 404")
	@Test
	@Override
	public void retrieveCustomer400() throws Exception {

	}

	@Disabled("Security is handled externally, thus 401 and 403 cannot happen.")
	@Test
	@Override
	public void retrieveCustomer401() throws Exception {

	}

	@Disabled("Security is handled externally, thus 401 and 403 cannot happen.")
	@Test
	@Override
	public void retrieveCustomer403() throws Exception {

	}

	@Test
	@Override
	public void retrieveCustomer404() throws Exception {
		HttpResponse<CustomerVO> response = callAndCatch(
				() -> customerApiTestClient.retrieveCustomer(null, "urn:ngsi-ld:resource-function:non-existent", null));
		assertEquals(HttpStatus.NOT_FOUND, response.getStatus(), "No such service-category should exist.");

		Optional<ErrorDetails> optionalErrorDetails = response.getBody(ErrorDetails.class);
		assertTrue(optionalErrorDetails.isPresent(), "Error details should have been provided.");
	}

	@Disabled("Prohibited by the framework.")
	@Test
	@Override
	public void retrieveCustomer405() throws Exception {

	}

	@Disabled("Conflict not possible on retrieval")
	@Test
	@Override
	public void retrieveCustomer409() throws Exception {

	}

	@Override
	public void retrieveCustomer500() throws Exception {

	}

	@Override
	protected String getEntityType() {
		return Customer.TYPE_CUSTOMER;
	}
}
