package org.fiware.tmforum.customerbillmanagement;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import io.micronaut.test.annotation.MockBean;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import org.fiware.customerbillmanagement.api.CustomerBillOnDemandApiTestClient;
import org.fiware.customerbillmanagement.api.CustomerBillOnDemandApiTestSpec;
import org.fiware.customerbillmanagement.model.BillRefVOTestExample;
import org.fiware.customerbillmanagement.model.BillingAccountRefVOTestExample;
import org.fiware.customerbillmanagement.model.CustomerBillOnDemandCreateVO;
import org.fiware.customerbillmanagement.model.CustomerBillOnDemandCreateVOTestExample;
import org.fiware.customerbillmanagement.model.CustomerBillOnDemandVO;
import org.fiware.customerbillmanagement.model.CustomerBillOnDemandVOTestExample;
import org.fiware.customerbillmanagement.model.RelatedPartyRefVOTestExample;
import org.fiware.ngsi.api.EntitiesApiClient;
import org.fiware.tmforum.common.EventHandler;
import org.fiware.tmforum.common.configuration.GeneralProperties;
import org.fiware.tmforum.common.exception.ErrorDetails;
import org.fiware.tmforum.common.test.AbstractApiIT;
import org.fiware.tmforum.customerbillmanagement.domain.CustomerBillOnDemand;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import reactor.core.publisher.Mono;

import java.time.Clock;
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

@MicronautTest(packages = { "org.fiware.tmforum.customerbillmanagement" })
public class CustomerBillOnDemandApiIT extends AbstractApiIT implements CustomerBillOnDemandApiTestSpec {

	public final CustomerBillOnDemandApiTestClient customerBillOnDemandApiTestClient;

	private String message;
	private String fieldsParameter;
	private CustomerBillOnDemandCreateVO customerBillOnDemandCreateVO;
	private CustomerBillOnDemandVO expectedCustomerBillOnDemand;

	public CustomerBillOnDemandApiIT(CustomerBillOnDemandApiTestClient customerBillOnDemandApiTestClient,
			EntitiesApiClient entitiesApiClient,
			ObjectMapper objectMapper, GeneralProperties generalProperties) {
		super(entitiesApiClient, objectMapper, generalProperties);
		this.customerBillOnDemandApiTestClient = customerBillOnDemandApiTestClient;
	}

	@MockBean(EventHandler.class)
	public EventHandler eventHandler() {
		EventHandler eventHandler = mock(EventHandler.class);

		Mono<List<HttpResponse<String>>> response = Mono.just(Stream.of("ok")
				.map(HttpResponse::ok).collect(Collectors.toList()));
		when(eventHandler.handleCreateEvent(any())).thenReturn(response);
		when(eventHandler.handleUpdateEvent(any(), any())).thenReturn(response);
		when(eventHandler.handleDeleteEvent(any())).thenReturn(response);

		return eventHandler;
	}

	@ParameterizedTest
	@MethodSource("provideValidCustomerBillOnDemands")
	public void createCustomerBillOnDemand201(String message,
			CustomerBillOnDemandCreateVO customerBillOnDemandCreateVO,
			CustomerBillOnDemandVO expectedCustomerBillOnDemand)
			throws Exception {
		this.message = message;
		this.customerBillOnDemandCreateVO = customerBillOnDemandCreateVO;
		this.expectedCustomerBillOnDemand = expectedCustomerBillOnDemand;
		createCustomerBillOnDemand201();
	}

	@Override
	public void createCustomerBillOnDemand201() throws Exception {

		HttpResponse<CustomerBillOnDemandVO> customerBillOnDemandVOHttpResponse = callAndCatch(
				() -> customerBillOnDemandApiTestClient.createCustomerBillOnDemand(
						customerBillOnDemandCreateVO));
		assertEquals(HttpStatus.CREATED, customerBillOnDemandVOHttpResponse.getStatus(), message);
		String rfId = customerBillOnDemandVOHttpResponse.body().getId();
		expectedCustomerBillOnDemand
				.id(rfId)
				.href(rfId)
				.lastUpdate(Instant.MAX.toString());

		assertEquals(expectedCustomerBillOnDemand, customerBillOnDemandVOHttpResponse.body(), message);
	}

	private static Stream<Arguments> provideValidCustomerBillOnDemands() {
		List<Arguments> testEntries = new ArrayList<>();

		CustomerBillOnDemandCreateVO emptyCreate = CustomerBillOnDemandCreateVOTestExample.build()
				.lastUpdate(Instant.MAX.toString())
				.billingAccount(null)
				.relatedParty(null)
				.customerBill(null);
		CustomerBillOnDemandVO expectedEmpty = CustomerBillOnDemandVOTestExample.build()
				.lastUpdate(Instant.MAX.toString())
				.billingAccount(null)
				.relatedParty(null)
				.customerBill(null);
		testEntries.add(Arguments.of("An empty customerBillOnDemand should have been created.", emptyCreate,
				expectedEmpty));

		return testEntries.stream();
	}

	@ParameterizedTest
	@MethodSource("provideInvalidCustomerBillOnDemands")
	public void createCustomerBillOnDemand400(String message, CustomerBillOnDemandCreateVO invalidCreateVO)
			throws Exception {
		this.message = message;
		this.customerBillOnDemandCreateVO = invalidCreateVO;
		createCustomerBillOnDemand400();
	}

	@Override
	public void createCustomerBillOnDemand400() throws Exception {
		HttpResponse<CustomerBillOnDemandVO> creationResponse = callAndCatch(
				() -> customerBillOnDemandApiTestClient.createCustomerBillOnDemand(
						customerBillOnDemandCreateVO));
		assertEquals(HttpStatus.BAD_REQUEST, creationResponse.getStatus(), message);
		Optional<ErrorDetails> optionalErrorDetails = creationResponse.getBody(ErrorDetails.class);
		assertTrue(optionalErrorDetails.isPresent(), "Error details should be provided.");
	}

	private static Stream<Arguments> provideInvalidCustomerBillOnDemands() {
		List<Arguments> testEntries = new ArrayList<>();

		testEntries.add(
				Arguments.of("A customerBillOnDemand with an invalid billing account should not be created.",
						CustomerBillOnDemandCreateVOTestExample.build()
								.lastUpdate(null)
								.relatedParty(null)
								.customerBill(null)
								.billingAccount(BillingAccountRefVOTestExample.build())));
		testEntries.add(Arguments.of(
				"A customerBillOnDemand with a non-existent billing account should not be created.",
				CustomerBillOnDemandCreateVOTestExample.build()
						.lastUpdate(null)
						.relatedParty(null)
						.customerBill(null)
						.billingAccount(
								BillingAccountRefVOTestExample.build()
										.id("urn:ngsi-ld:billing-account:non-existent"))));

		testEntries.add(
				Arguments.of("A customerBillOnDemand with an invalid related party should not be created.",
						CustomerBillOnDemandCreateVOTestExample.build()
								.lastUpdate(null)
								.relatedParty(RelatedPartyRefVOTestExample.build())
								.customerBill(null)
								.billingAccount(null)));
		testEntries.add(
				Arguments.of("A customerBillOnDemand with a non-existent related party should not be created.",
						CustomerBillOnDemandCreateVOTestExample.build()
								.lastUpdate(null)
								.relatedParty(
										RelatedPartyRefVOTestExample.build().id("urn:ngsi-ld:organition:non-existent"))
								.customerBill(null)
								.billingAccount(null)));

		testEntries.add(
				Arguments.of("A customerBillOnDemand with an invalid bill ref should not be created.",
						CustomerBillOnDemandCreateVOTestExample.build()
								.lastUpdate(null)
								.relatedParty(null)
								.customerBill(BillRefVOTestExample.build())
								.billingAccount(null)));
		testEntries.add(
				Arguments.of("A customerBillOnDemand with a non-existent bill ref should not be created.",
						CustomerBillOnDemandCreateVOTestExample.build()
								.lastUpdate(null)
								.relatedParty(null)
								.customerBill(
										BillRefVOTestExample.build().id("urn:ngsi-ld:customer-bill:non-existent"))));

		testEntries.add(
				Arguments.of("A customerBillOnDemand with an invalid lastUpdate should not be created.",
						CustomerBillOnDemandCreateVOTestExample.build()
								.lastUpdate("no-date")
								.relatedParty(null)
								.customerBill(null)
								.billingAccount(null)));

		return testEntries.stream();
	}

	@Disabled("Security is handled externally, thus 401 and 403 cannot happen.")
	@Test
	@Override
	public void createCustomerBillOnDemand401() throws Exception {

	}

	@Disabled("Security is handled externally, thus 401 and 403 cannot happen.")
	@Test
	@Override
	public void createCustomerBillOnDemand403() throws Exception {

	}

	@Disabled("Prohibited by the framework.")
	@Test
	@Override
	public void createCustomerBillOnDemand405() throws Exception {
	}

	@Disabled("No implicit creation, impossible state.")
	@Test
	@Override
	public void createCustomerBillOnDemand409() throws Exception {

	}

	@Override
	public void createCustomerBillOnDemand500() throws Exception {

	}

	@Test
	@Override
	public void listCustomerBillOnDemand200() throws Exception {
		List<CustomerBillOnDemandVO> expectedCustomerBillOnDemands = new ArrayList<>();
		for (int i = 0; i < 10; i++) {
			CustomerBillOnDemandCreateVO customerBillOnDemandCreateVO = CustomerBillOnDemandCreateVOTestExample.build()
					.customerBill(null)
					.relatedParty(null)
					.billingAccount(null)
					.lastUpdate(Instant.MAX.toString());
			String id = customerBillOnDemandApiTestClient.createCustomerBillOnDemand(
					customerBillOnDemandCreateVO).body().getId();
			CustomerBillOnDemandVO customerBillOnDemandVO = CustomerBillOnDemandVOTestExample.build();
			customerBillOnDemandVO
					.id(id)
					.href(id)
					.billingAccount(null)
					.customerBill(null)
					.relatedParty(null)
					.lastUpdate(Instant.MAX.toString());
			expectedCustomerBillOnDemands.add(customerBillOnDemandVO);
		}

		HttpResponse<List<CustomerBillOnDemandVO>> customerBillOnDemandResponse = callAndCatch(
				() -> customerBillOnDemandApiTestClient.listCustomerBillOnDemand(null, null, null));

		assertEquals(HttpStatus.OK, customerBillOnDemandResponse.getStatus(),
				"The list should be accessible.");
		assertEquals(expectedCustomerBillOnDemands.size(),
				customerBillOnDemandResponse.getBody().get().size(),
				"All bills should have been returend.");
		List<CustomerBillOnDemandVO> retrievedBills = customerBillOnDemandResponse.getBody().get();

		Map<String, CustomerBillOnDemandVO> retrievedMap = retrievedBills.stream()
				.collect(Collectors.toMap(bill -> bill.getId(), bill -> bill));

		expectedCustomerBillOnDemands.stream()
				.forEach(expectedBill -> assertTrue(retrievedMap.containsKey(expectedBill.getId()),
						String.format("All created customerBillOnDemands should be returned - Missing: %s.",
								expectedBill,
								retrievedBills)));
		expectedCustomerBillOnDemands.stream().forEach(
				expectedBill -> assertEquals(expectedBill, retrievedMap.get(expectedBill.getId()),
						"The correct customerBillOnDemands should be retrieved."));

		// get with pagination
		Integer limit = 5;
		HttpResponse<List<CustomerBillOnDemandVO>> firstPartResponse = callAndCatch(
				() -> customerBillOnDemandApiTestClient.listCustomerBillOnDemand(null, 0, limit));
		assertEquals(limit, firstPartResponse.body().size(),
				"Only the requested number of entries should be returend.");
		HttpResponse<List<CustomerBillOnDemandVO>> secondPartResponse = callAndCatch(
				() -> customerBillOnDemandApiTestClient.listCustomerBillOnDemand(null, 0 + limit, limit));
		assertEquals(limit, secondPartResponse.body().size(),
				"Only the requested number of entries should be returend.");

		retrievedBills.clear();
		retrievedBills.addAll(firstPartResponse.body());
		retrievedBills.addAll(secondPartResponse.body());
		expectedCustomerBillOnDemands.stream()
				.forEach(expectedBill -> assertTrue(retrievedMap.containsKey(expectedBill.getId()),
						String.format("All created customerBillOnDemands should be returned - Missing: %s.",
								expectedBill)));
		expectedCustomerBillOnDemands.stream().forEach(
				expectedBill -> assertEquals(expectedBill, retrievedMap.get(expectedBill.getId()),
						"The correct customerBillOnDemands should be retrieved."));
	}

	@Test
	@Override
	public void listCustomerBillOnDemand400() throws Exception {
		HttpResponse<List<CustomerBillOnDemandVO>> badRequestResponse = callAndCatch(
				() -> customerBillOnDemandApiTestClient.listCustomerBillOnDemand(null, -1, null));
		assertEquals(HttpStatus.BAD_REQUEST,
				badRequestResponse.getStatus(),
				"Negative offsets are impossible.");

		Optional<ErrorDetails> optionalErrorDetails = badRequestResponse.getBody(ErrorDetails.class);
		assertTrue(optionalErrorDetails.isPresent(), "Error details should be provided.");

		badRequestResponse = callAndCatch(
				() -> customerBillOnDemandApiTestClient.listCustomerBillOnDemand(null, null, -1));
		assertEquals(HttpStatus.BAD_REQUEST,
				badRequestResponse.getStatus(),
				"Negative limits are impossible.");
		optionalErrorDetails = badRequestResponse.getBody(ErrorDetails.class);
		assertTrue(optionalErrorDetails.isPresent(), "Error details should be provided.");
	}

	@Disabled("Security is handled externally, thus 401 and 403 cannot happen.")
	@Test
	@Override
	public void listCustomerBillOnDemand401() throws Exception {

	}

	@Disabled("Security is handled externally, thus 401 and 403 cannot happen.")
	@Test
	@Override
	public void listCustomerBillOnDemand403() throws Exception {

	}

	@Disabled("Not found is not possible here, will be answered with an empty list instead.")
	@Test
	@Override
	public void listCustomerBillOnDemand404() throws Exception {

	}

	@Disabled("Prohibited by the framework.")
	@Test
	@Override
	public void listCustomerBillOnDemand405() throws Exception {

	}

	@Disabled("Impossible status.")
	@Test
	@Override
	public void listCustomerBillOnDemand409() throws Exception {

	}

	@Override
	public void listCustomerBillOnDemand500() throws Exception {

	}

	@ParameterizedTest
	@MethodSource("provideFieldParameters")
	public void retrieveCustomerBillOnDemand200(String message, String fields,
			CustomerBillOnDemandVO expectedCustomerBillOnDemand) throws Exception {
		this.fieldsParameter = fields;
		this.message = message;
		this.expectedCustomerBillOnDemand = expectedCustomerBillOnDemand;
		retrieveCustomerBillOnDemand200();
	}

	@Override
	public void retrieveCustomerBillOnDemand200() throws Exception {

		CustomerBillOnDemandCreateVO customerBillOnDemandCreateVO = CustomerBillOnDemandCreateVOTestExample.build()
				.relatedParty(null)
				.billingAccount(null)
				.customerBill(null)
				.lastUpdate(Instant.MAX.toString());
		HttpResponse<CustomerBillOnDemandVO> createResponse = callAndCatch(
				() -> customerBillOnDemandApiTestClient.createCustomerBillOnDemand(
						customerBillOnDemandCreateVO));
		assertEquals(HttpStatus.CREATED, createResponse.getStatus(), message);
		String id = createResponse.body().getId();

		expectedCustomerBillOnDemand
				.id(id)
				.href(id);

		//then retrieve
		HttpResponse<CustomerBillOnDemandVO> retrievedRF = callAndCatch(
				() -> customerBillOnDemandApiTestClient.retrieveCustomerBillOnDemand(id, fieldsParameter));
		assertEquals(HttpStatus.OK, retrievedRF.getStatus(), message);
		assertEquals(expectedCustomerBillOnDemand, retrievedRF.body(), message);
	}

	private static Stream<Arguments> provideFieldParameters() {
		return Stream.of(
				Arguments.of("Without a fields parameter everything should be returned.", null,
						CustomerBillOnDemandVOTestExample.build()
								.relatedParty(null)
								.billingAccount(null)
								.customerBill(null)
								.lastUpdate(Instant.MAX.toString())),
				Arguments.of("Only name and the mandatory parameters should have been included.", "name",
						CustomerBillOnDemandVOTestExample.build()
								.description(null)
								.lastUpdate(null)
								.state(null)
								.relatedParty(null)
								.customerBill(null)
								.billingAccount(null)
								.atType(null)
								.atBaseType(null)
								.atSchemaLocation(null)),
				Arguments.of(
						"Only the mandatory parameters should have been included when a non-existent field was requested.",
						"nothingToSeeHere", CustomerBillOnDemandVOTestExample.build()
								.description(null)
								.lastUpdate(null)
								.state(null)
								.name(null)
								.relatedParty(null)
								.customerBill(null)
								.billingAccount(null)
								.atType(null)
								.atBaseType(null)
								.atSchemaLocation(null)),
				Arguments.of("Only name, state, description and the mandatory parameters should have been included.",
						"name,state,description", CustomerBillOnDemandVOTestExample.build()
								.lastUpdate(null)
								.relatedParty(null)
								.customerBill(null)
								.billingAccount(null)
								.atType(null)
								.atBaseType(null)
								.atSchemaLocation(null)));
	}

	@Disabled("400 cannot happen, only 404")
	@Test
	@Override
	public void retrieveCustomerBillOnDemand400() throws Exception {

	}

	@Disabled("Security is handled externally, thus 401 and 403 cannot happen.")
	@Test
	@Override
	public void retrieveCustomerBillOnDemand401() throws Exception {

	}

	@Disabled("Security is handled externally, thus 401 and 403 cannot happen.")
	@Test
	@Override
	public void retrieveCustomerBillOnDemand403() throws Exception {

	}

	@Test
	@Override
	public void retrieveCustomerBillOnDemand404() throws Exception {
		HttpResponse<CustomerBillOnDemandVO> response = callAndCatch(
				() -> customerBillOnDemandApiTestClient.retrieveCustomerBillOnDemand(
						"urn:ngsi-ld:resource-function:non-existent", null));
		assertEquals(HttpStatus.NOT_FOUND, response.getStatus(), "No such service-category should exist.");

		Optional<ErrorDetails> optionalErrorDetails = response.getBody(ErrorDetails.class);
		assertTrue(optionalErrorDetails.isPresent(), "Error details should have been provided.");
	}

	@Disabled("Prohibited by the framework.")
	@Test
	@Override
	public void retrieveCustomerBillOnDemand405() throws Exception {

	}

	@Disabled("Conflict not possible on retrieval")
	@Test
	@Override
	public void retrieveCustomerBillOnDemand409() throws Exception {

	}

	@Override
	public void retrieveCustomerBillOnDemand500() throws Exception {

	}

	@Override protected String getEntityType() {
		return CustomerBillOnDemand.TYPE_CUSTOMER_BILL_ON_DEMAND;
	}
}
