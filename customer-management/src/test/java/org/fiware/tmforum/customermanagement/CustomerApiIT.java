package org.fiware.tmforum.customermanagement;

import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import lombok.RequiredArgsConstructor;
import org.fiware.customermanagement.api.CustomerApiTestClient;
import org.fiware.customermanagement.api.CustomerApiTestSpec;
import org.fiware.customermanagement.model.AccountRefVOTestExample;
import org.fiware.customermanagement.model.AgreementRefVOTestExample;
import org.fiware.customermanagement.model.CharacteristicVOTestExample;
import org.fiware.customermanagement.model.ContactMediumVOTestExample;
import org.fiware.customermanagement.model.CreditProfileVOTestExample;
import org.fiware.customermanagement.model.CustomerCreateVO;
import org.fiware.customermanagement.model.CustomerCreateVOTestExample;
import org.fiware.customermanagement.model.CustomerUpdateVO;
import org.fiware.customermanagement.model.CustomerUpdateVOTestExample;
import org.fiware.customermanagement.model.CustomerVO;
import org.fiware.customermanagement.model.CustomerVOTestExample;
import org.fiware.customermanagement.model.PaymentMethodRefVOTestExample;
import org.fiware.customermanagement.model.RelatedPartyVOTestExample;
import org.fiware.customermanagement.model.TimePeriodVO;
import org.fiware.customermanagement.model.TimePeriodVOTestExample;
import org.fiware.tmforum.common.exception.ErrorDetails;
import org.fiware.tmforum.common.test.AbstractApiIT;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@RequiredArgsConstructor
@MicronautTest(packages = { "org.fiware.tmforum.customermanagement" })
public class CustomerApiIT extends AbstractApiIT implements CustomerApiTestSpec {

	public final CustomerApiTestClient customerApiTestClient;

	private String message;
	private String fieldsParameter;
	private CustomerCreateVO customerCreateVO;
	private CustomerUpdateVO customerUpdateVO;
	private CustomerVO expectedCustomer;

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
				() -> customerApiTestClient.createCustomer(customerCreateVO));
		assertEquals(HttpStatus.CREATED, customerVOHttpResponse.getStatus(), message);
		String rfId = customerVOHttpResponse.body().getId();
		expectedCustomer.id(rfId)
				.href(rfId);

		assertEquals(expectedCustomer, customerVOHttpResponse.body(), message);
	}

	private static Stream<Arguments> provideValidCustomers() {
		List<Arguments> testEntries = new ArrayList<>();

		CustomerCreateVO emptyCreate = CustomerCreateVOTestExample.build().engagedParty(null);
		CustomerVO expectedEmpty = CustomerVOTestExample.build().engagedParty(null);
		testEntries.add(Arguments.of("An empty customer should have been created.", emptyCreate, expectedEmpty));

		TimePeriodVO timePeriodVO = TimePeriodVOTestExample.build().endDateTime(Instant.now())
				.startDateTime(Instant.now());
		CustomerCreateVO createValidFor = CustomerCreateVOTestExample.build().validFor(timePeriodVO).engagedParty(null);
		CustomerVO expectedValidFor = CustomerVOTestExample.build().validFor(timePeriodVO).engagedParty(null);
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
				() -> customerApiTestClient.createCustomer(customerCreateVO));
		assertEquals(HttpStatus.BAD_REQUEST, creationResponse.getStatus(), message);
		Optional<ErrorDetails> optionalErrorDetails = creationResponse.getBody(ErrorDetails.class);
		assertTrue(optionalErrorDetails.isPresent(), "Error details should be provided.");
	}

	private static Stream<Arguments> provideInvalidCustomers() {
		List<Arguments> testEntries = new ArrayList<>();

		testEntries.add(Arguments.of("A customer with an invalid engaged party should not be created.",
				CustomerCreateVOTestExample.build().engagedParty(RelatedPartyVOTestExample.build())));
		testEntries.add(Arguments.of("A customer with a non-existent engaged party should not be created.",
				CustomerCreateVOTestExample.build()
						.engagedParty(RelatedPartyVOTestExample.build().id("urn:ngsi-ld:organization:non-existent"))));

		testEntries.add(Arguments.of("A customer with an invalid account should not be created.",
				CustomerCreateVOTestExample.build().engagedParty(null)
						.account(List.of(AccountRefVOTestExample.build()))));
		testEntries.add(Arguments.of("A customer with a non-existent account should not be created.",
				CustomerCreateVOTestExample.build().engagedParty(null)
						.account(List.of(AccountRefVOTestExample.build().id("urn:ngsi-ld:account:non-existent")))));

		testEntries.add(Arguments.of("A customer with an invalid agreement should not be created.",
				CustomerCreateVOTestExample.build().engagedParty(null)
						.agreement(List.of(AgreementRefVOTestExample.build()))));
		testEntries.add(Arguments.of("A customer with a non-existent agreement should not be created.",
				CustomerCreateVOTestExample.build().engagedParty(null).agreement(
						List.of(AgreementRefVOTestExample.build().id("urn:ngsi-ld:agreement:non-existent")))));

		testEntries.add(Arguments.of("A customer with an invalid paymentMethod should not be created.",
				CustomerCreateVOTestExample.build().engagedParty(null)
						.paymentMethod(List.of(PaymentMethodRefVOTestExample.build()))));
		testEntries.add(Arguments.of("A customer with a non-existent paymentMethod should not be created.",
				CustomerCreateVOTestExample.build().engagedParty(null).paymentMethod(
						List.of(PaymentMethodRefVOTestExample.build().id("urn:ngsi-ld:payment-method:non-existent")))));

		testEntries.add(Arguments.of("A customer with an invalid related party should not be created.",
				CustomerCreateVOTestExample.build().engagedParty(null)
						.relatedParty(List.of(RelatedPartyVOTestExample.build()))));
		testEntries.add(Arguments.of("A customer with a non-existent related party should not be created.",
				CustomerCreateVOTestExample.build().engagedParty(null).relatedParty(
						List.of(RelatedPartyVOTestExample.build().id("urn:ngsi-ld:organization:non-existent")))));

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
		CustomerCreateVO emptyCreate = CustomerCreateVOTestExample.build().engagedParty(null);

		HttpResponse<CustomerVO> createResponse = customerApiTestClient.createCustomer(emptyCreate);
		assertEquals(HttpStatus.CREATED, createResponse.getStatus(), "The customer should have been created first.");

		String rfId = createResponse.body().getId();

		assertEquals(HttpStatus.NO_CONTENT,
				callAndCatch(() -> customerApiTestClient.deleteCustomer(rfId)).getStatus(),
				"The customer should have been deleted.");

		assertEquals(HttpStatus.NOT_FOUND,
				callAndCatch(() -> customerApiTestClient.retrieveCustomer(rfId, null)).status(),
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
				() -> customerApiTestClient.deleteCustomer("urn:ngsi-ld:service-category:no-pop"));
		assertEquals(HttpStatus.NOT_FOUND,
				notFoundResponse.getStatus(),
				"No such service-category should exist.");

		Optional<ErrorDetails> optionalErrorDetails = notFoundResponse.getBody(ErrorDetails.class);
		assertTrue(optionalErrorDetails.isPresent(), "Error details should be provided.");

		notFoundResponse = callAndCatch(() -> customerApiTestClient.deleteCustomer("invalid-id"));
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

	@Disabled("Cleanup has to be solved")
	@Test
	@Override
	public void listCustomer200() throws Exception {
		List<CustomerVO> expectedCustomers = new ArrayList<>();
		for (int i = 0; i < 10; i++) {
			CustomerCreateVO customerCreateVO = CustomerCreateVOTestExample.build().engagedParty(null);
			String id = customerApiTestClient.createCustomer(customerCreateVO).body().getId();
			CustomerVO customerVO = CustomerVOTestExample.build();
			customerVO.id(id).href(id);
			expectedCustomers.add(customerVO);
		}

		HttpResponse<List<CustomerVO>> categoryListResponse = callAndCatch(
				() -> customerApiTestClient.listCustomer(null, null, null));
		assertEquals(HttpStatus.OK, categoryListResponse.getStatus(), "The list should be accessible.");

		// ignore order
		List<CustomerVO> customerVOS = categoryListResponse.body();
		assertEquals(expectedCustomers.size(), expectedCustomers.size(), "All categories should be returned.");
		expectedCustomers
				.forEach(customerVO ->
						assertTrue(customerVOS.contains(customerVO),
								String.format("All product specs  should be contained. Missing: %s", customerVO)));

		// get with pagination
		Integer limit = 5;
		HttpResponse<List<CustomerVO>> firstPartResponse = callAndCatch(
				() -> customerApiTestClient.listCustomer(null, 0, limit));
		assertEquals(limit, firstPartResponse.body().size(),
				"Only the requested number of entries should be returend.");
		HttpResponse<List<CustomerVO>> secondPartResponse = callAndCatch(
				() -> customerApiTestClient.listCustomer(null, 0 + limit, limit));
		assertEquals(limit, secondPartResponse.body().size(),
				"Only the requested number of entries should be returend.");

		List<CustomerVO> retrievedCatalogs = firstPartResponse.body();
		retrievedCatalogs.addAll(secondPartResponse.body());
		expectedCustomers
				.forEach(customerVO ->
						assertTrue(retrievedCatalogs.contains(customerVO),
								String.format("All customers should be contained. Missing: %s", customerVO)));
	}

	@Test
	@Override
	public void listCustomer400() throws Exception {
		HttpResponse<List<CustomerVO>> badRequestResponse = callAndCatch(
				() -> customerApiTestClient.listCustomer(null, -1, null));
		assertEquals(HttpStatus.BAD_REQUEST,
				badRequestResponse.getStatus(),
				"Negative offsets are impossible.");

		Optional<ErrorDetails> optionalErrorDetails = badRequestResponse.getBody(ErrorDetails.class);
		assertTrue(optionalErrorDetails.isPresent(), "Error details should be provided.");

		badRequestResponse = callAndCatch(() -> customerApiTestClient.listCustomer(null, null, -1));
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
		CustomerCreateVO customerCreateVO = CustomerCreateVOTestExample.build().engagedParty(null);

		HttpResponse<CustomerVO> createResponse = callAndCatch(
				() -> customerApiTestClient.createCustomer(customerCreateVO));
		assertEquals(HttpStatus.CREATED, createResponse.getStatus(), "The customer should have been created first.");

		String resourceId = createResponse.body().getId();

		HttpResponse<CustomerVO> updateResponse = callAndCatch(
				() -> customerApiTestClient.patchCustomer(resourceId, customerUpdateVO));
		assertEquals(HttpStatus.OK, updateResponse.getStatus(), message);

		CustomerVO updatedCustomer = updateResponse.body();
		expectedCustomer.href(resourceId).id(resourceId);

		assertEquals(expectedCustomer, updatedCustomer, message);
	}

	private static Stream<Arguments> provideCustomerUpdates() {
		List<Arguments> testEntries = new ArrayList<>();

		testEntries.add(Arguments.of("The name should have been updated.",
				CustomerUpdateVOTestExample.build()
						.engagedParty(null)
						.name("Max Mustermann"),
				CustomerVOTestExample.build()
						.engagedParty(null)
						.agreement(null)
						.account(null)
						.paymentMethod(null)
						.relatedParty(null)
						.name("Max Mustermann")));

		testEntries.add(Arguments.of("The status should have been updated.",
				CustomerUpdateVOTestExample.build()
						.engagedParty(null)
						.name("pending"),
				CustomerVOTestExample.build()
						.engagedParty(null)
						.agreement(null)
						.account(null)
						.paymentMethod(null)
						.relatedParty(null)
						.name("pending")));

		testEntries.add(Arguments.of("The statusReason should have been updated.",
				CustomerUpdateVOTestExample.build()
						.engagedParty(null)
						.statusReason("not signed"),
				CustomerVOTestExample.build()
						.engagedParty(null)
						.agreement(null)
						.account(null)
						.paymentMethod(null)
						.relatedParty(null)
						.statusReason("not signed")));

		testEntries.add(Arguments.of("Characteristics should have been updated.",
				CustomerUpdateVOTestExample.build()
						.engagedParty(null)
						.characteristic(List.of(CharacteristicVOTestExample.build())),
				CustomerVOTestExample.build()
						.engagedParty(null)
						.agreement(null)
						.account(null)
						.paymentMethod(null)
						.relatedParty(null)
						.characteristic(List.of(CharacteristicVOTestExample.build()))));

		testEntries.add(Arguments.of("ContactMedium should have been updated.",
				CustomerUpdateVOTestExample.build()
						.engagedParty(null)
						.contactMedium(List.of(ContactMediumVOTestExample.build()
								.validFor(TimePeriodVOTestExample.build().endDateTime(Instant.MAX)
										.startDateTime(Instant.MIN)))),
				CustomerVOTestExample.build()
						.engagedParty(null)
						.agreement(null)
						.account(null)
						.paymentMethod(null)
						.relatedParty(null)
						.contactMedium(List.of(ContactMediumVOTestExample.build()
								.validFor(TimePeriodVOTestExample.build().endDateTime(Instant.MAX)
										.startDateTime(Instant.MIN))))));

		testEntries.add(Arguments.of("CreditProfile should have been updated.",
				CustomerUpdateVOTestExample.build()
						.engagedParty(null)
						.creditProfile(List.of(CreditProfileVOTestExample.build()
								.validFor(TimePeriodVOTestExample.build().endDateTime(Instant.MAX)
										.startDateTime(Instant.MIN)))),
				CustomerVOTestExample.build()
						.engagedParty(null)
						.agreement(null)
						.account(null)
						.paymentMethod(null)
						.relatedParty(null)
						.creditProfile(List.of(CreditProfileVOTestExample.build()
								.validFor(TimePeriodVOTestExample.build().endDateTime(Instant.MAX)
										.startDateTime(Instant.MIN))))));

		TimePeriodVO timePeriodVO = TimePeriodVOTestExample.build().endDateTime(Instant.now())
				.startDateTime(Instant.now());
		testEntries.add(Arguments.of("The validFor should have been updated.",
				CustomerUpdateVOTestExample.build().validFor(timePeriodVO)
						.engagedParty(null),
				CustomerVOTestExample.build().validFor(timePeriodVO)
						.engagedParty(null)
						.agreement(null)
						.account(null)
						.paymentMethod(null)
						.relatedParty(null)));

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
		CustomerCreateVO customerCreateVO = CustomerCreateVOTestExample.build().engagedParty(null);

		HttpResponse<CustomerVO> createResponse = callAndCatch(
				() -> customerApiTestClient.createCustomer(customerCreateVO));
		assertEquals(HttpStatus.CREATED, createResponse.getStatus(), "The customer should have been created first.");

		String resourceId = createResponse.body().getId();

		HttpResponse<CustomerVO> updateResponse = callAndCatch(
				() -> customerApiTestClient.patchCustomer(resourceId, customerUpdateVO));
		assertEquals(HttpStatus.BAD_REQUEST, updateResponse.getStatus(), message);

		Optional<ErrorDetails> optionalErrorDetails = updateResponse.getBody(ErrorDetails.class);
		assertTrue(optionalErrorDetails.isPresent(), "Some error details should be present.");
	}

	private static Stream<Arguments> provideInvalidUpdates() {
		List<Arguments> testEntries = new ArrayList<>();

		testEntries.add(Arguments.of("An update with an invalid engaged party is not allowed.",
				CustomerUpdateVOTestExample.build()
						.engagedParty(RelatedPartyVOTestExample.build().id("invalid"))));
		testEntries.add(Arguments.of("An update with an non existent engaged party is not allowed.",
				CustomerUpdateVOTestExample.build()
						.engagedParty(RelatedPartyVOTestExample.build().id("urn:ngsi-ld:organization:non-existent"))));

		testEntries.add(Arguments.of("An update with an invalid account is not allowed.",
				CustomerUpdateVOTestExample.build()
						.account(List.of(AccountRefVOTestExample.build().id("invalid")))));
		testEntries.add(Arguments.of("An update with an non existent account is not allowed.",
				CustomerUpdateVOTestExample.build()
						.account(List.of(AccountRefVOTestExample.build().id("urn:ngsi-ld:account:non-existent")))));

		testEntries.add(Arguments.of("An update with an invalid agreement is not allowed.",
				CustomerUpdateVOTestExample.build()
						.agreement(List.of(AgreementRefVOTestExample.build().id("invalid")))));
		testEntries.add(Arguments.of("An update with an non existent agreement is not allowed.",
				CustomerUpdateVOTestExample.build()
						.agreement(
								List.of(AgreementRefVOTestExample.build().id("urn:ngsi-ld:agreement:non-existent")))));

		testEntries.add(Arguments.of("An update with an invalid paymentMethod is not allowed.",
				CustomerUpdateVOTestExample.build()
						.paymentMethod(List.of(PaymentMethodRefVOTestExample.build().id("invalid")))));
		testEntries.add(Arguments.of("An update with an non existent paymentMethod is not allowed.",
				CustomerUpdateVOTestExample.build()
						.paymentMethod(List.of(PaymentMethodRefVOTestExample.build()
								.id("urn:ngsi-ld:payment-method:non-existent")))));

		testEntries.add(Arguments.of("An update with an invalid relatedParty is not allowed.",
				CustomerUpdateVOTestExample.build()
						.relatedParty(List.of(RelatedPartyVOTestExample.build().id("invalid")))));
		testEntries.add(Arguments.of("An update with an non existent relatedParty is not allowed.",
				CustomerUpdateVOTestExample.build()
						.relatedParty(List.of(RelatedPartyVOTestExample.build()
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
		CustomerUpdateVO customerUpdateVO = CustomerUpdateVOTestExample.build();
		assertEquals(
				HttpStatus.NOT_FOUND,
				callAndCatch(() -> customerApiTestClient.patchCustomer("urn:ngsi-ld:service-category:not-existent",
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

		CustomerCreateVO customerCreateVO = CustomerCreateVOTestExample.build().engagedParty(null);
		HttpResponse<CustomerVO> createResponse = callAndCatch(
				() -> customerApiTestClient.createCustomer(customerCreateVO));
		assertEquals(HttpStatus.CREATED, createResponse.getStatus(), message);
		String id = createResponse.body().getId();

		expectedCustomer
				.id(id)
				.href(id);

		//then retrieve
		HttpResponse<CustomerVO> retrievedRF = callAndCatch(
				() -> customerApiTestClient.retrieveCustomer(id, fieldsParameter));
		assertEquals(HttpStatus.OK, retrievedRF.getStatus(), message);
		assertEquals(expectedCustomer, retrievedRF.body(), message);
	}

	private static Stream<Arguments> provideFieldParameters() {
		return Stream.of(
				Arguments.of("Without a fields parameter everything should be returned.", null,
						CustomerVOTestExample.build()
								// get nulled without values
								.engagedParty(null)
								.account(null)
								.agreement(null)
								.paymentMethod(null)
								.relatedParty(null)),
				Arguments.of("Only name and the mandatory parameters should have been included.", "name",
						CustomerVOTestExample.build()
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
						"nothingToSeeHere", CustomerVOTestExample.build()
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
						"name,status,statusReason", CustomerVOTestExample.build()
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
				() -> customerApiTestClient.retrieveCustomer("urn:ngsi-ld:resource-function:non-existent", null));
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
}
