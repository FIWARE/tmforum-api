package org.fiware.tmforum.customerbillmanagement;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import io.micronaut.test.annotation.MockBean;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import lombok.RequiredArgsConstructor;
import org.fiware.customerbillmanagement.api.CustomerBillApiTestClient;
import org.fiware.customerbillmanagement.api.CustomerBillApiTestSpec;
import org.fiware.customerbillmanagement.model.CustomerBillUpdateVOTestExample;
import org.fiware.customerbillmanagement.model.CustomerBillVO;
import org.fiware.customerbillmanagement.model.CustomerBillVOTestExample;
import org.fiware.customerbillmanagement.model.StateValueVO;
import org.fiware.ngsi.api.EntitiesApiClient;
import org.fiware.ngsi.model.EntityVO;
import org.fiware.tmforum.common.EventHandler;
import org.fiware.tmforum.common.configuration.GeneralProperties;
import org.fiware.tmforum.common.exception.ErrorDetails;
import org.fiware.tmforum.common.test.AbstractApiIT;
import org.fiware.tmforum.customerbillmanagement.domain.CustomerBill;
import io.github.wistefan.mapping.JavaObjectMapper;
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
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@MicronautTest(packages = { "org.fiware.tmforum.customerbillmanagement" })
public class CustomerBillApiIT extends AbstractApiIT implements CustomerBillApiTestSpec {

	private final CustomerBillApiTestClient customerBillApiTestClient;
	private final EntitiesApiClient entitiesApiClient;
	private final JavaObjectMapper javaObjectMapper;
	private final TMForumMapper tmForumMapper;

	private String message;
	private String fieldsParameter;
	private CustomerBillVO expectedCustomerBillVo;

	private Clock clock = mock(Clock.class);

	public CustomerBillApiIT(CustomerBillApiTestClient customerBillApiTestClient, EntitiesApiClient entitiesApiClient,
			JavaObjectMapper javaObjectMapper, TMForumMapper tmForumMapper, ObjectMapper objectMapper,
			GeneralProperties generalProperties) {
		super(entitiesApiClient, objectMapper, generalProperties);
		this.customerBillApiTestClient = customerBillApiTestClient;
		this.entitiesApiClient = entitiesApiClient;
		this.javaObjectMapper = javaObjectMapper;
		this.tmForumMapper = tmForumMapper;
	}

	@MockBean(Clock.class)
	public Clock clock() {
		return clock;
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

	private void createBill(CustomerBill customerBillVO) {
		EntityVO entityVO = javaObjectMapper.toEntityVO(customerBillVO);
		entitiesApiClient.createEntity(entityVO, null).block();
	}

	@Test
	@Override
	public void listCustomerBill200() throws Exception {
		List<CustomerBill> bills = new ArrayList<>();
		List<CustomerBillVO> expectedBillVOS = new ArrayList<>();
		for (int i = 0; i < 10; i++) {
			CustomerBillVO customerBillVO = CustomerBillVOTestExample.build()
					.id("urn:ngsi-ld:customer-bill:" + UUID.randomUUID().toString())
					.billingAccount(null)
					.financialAccount(null)
					.paymentMethod(null);
			bills.add(tmForumMapper.map(customerBillVO));
			customerBillVO.relatedParty(null);
			expectedBillVOS.add(customerBillVO);
		}
		bills.forEach(this::createBill);

		HttpResponse<List<CustomerBillVO>> customerBillResponse = callAndCatch(
				() -> customerBillApiTestClient.listCustomerBill(null, null, null));

		assertEquals(HttpStatus.OK, customerBillResponse.getStatus(), "The list should be accessible.");
		assertEquals(expectedBillVOS.size(), customerBillResponse.getBody().get().size(),
				"All bills should have been returend.");
		List<CustomerBillVO> retrievedBills = customerBillResponse.getBody().get();

		Map<String, CustomerBillVO> retrievedBillsMap = retrievedBills.stream()
				.collect(Collectors.toMap(bill -> bill.getId(), bill -> bill));

		expectedBillVOS.stream().forEach(expectedBill -> assertTrue(retrievedBillsMap.containsKey(expectedBill.getId()),
				String.format("All created bills should be returned - Missing: %s.", expectedBill,
						retrievedBills)));
		expectedBillVOS.stream().forEach(
				expectedBill -> assertEquals(expectedBill, retrievedBillsMap.get(expectedBill.getId()),
						"The correct bill should be retrieved."));

		// get with pagination
		Integer limit = 5;
		HttpResponse<List<CustomerBillVO>> firstPartResponse = callAndCatch(
				() -> customerBillApiTestClient.listCustomerBill(null, 0, limit));
		assertEquals(limit, firstPartResponse.body().size(),
				"Only the requested number of entries should be returend.");
		HttpResponse<List<CustomerBillVO>> secondPartResponse = callAndCatch(
				() -> customerBillApiTestClient.listCustomerBill(null, 0 + limit, limit));
		assertEquals(limit, secondPartResponse.body().size(),
				"Only the requested number of entries should be returend.");

		retrievedBills.clear();
		retrievedBills.addAll(firstPartResponse.body());
		retrievedBills.addAll(secondPartResponse.body());
		expectedBillVOS.stream().forEach(expectedBill -> assertTrue(retrievedBillsMap.containsKey(expectedBill.getId()),
				String.format("All created bills should be returned - Missing: %s.", expectedBill)));
		expectedBillVOS.stream().forEach(
				expectedBill -> assertEquals(expectedBill, retrievedBillsMap.get(expectedBill.getId()),
						"The correct bill should be retrieved."));

	}

	@Test
	@Override
	public void listCustomerBill400() throws Exception {
		HttpResponse<List<CustomerBillVO>> badRequestResponse = callAndCatch(
				() -> customerBillApiTestClient.listCustomerBill(null, -1, null));
		assertEquals(HttpStatus.BAD_REQUEST,
				badRequestResponse.getStatus(),
				"Negative offsets are impossible.");

		Optional<ErrorDetails> optionalErrorDetails = badRequestResponse.getBody(ErrorDetails.class);
		assertTrue(optionalErrorDetails.isPresent(), "Error details should be provided.");

		badRequestResponse = callAndCatch(() -> customerBillApiTestClient.listCustomerBill(null, null, -1));
		assertEquals(HttpStatus.BAD_REQUEST,
				badRequestResponse.getStatus(),
				"Negative limits are impossible.");
		optionalErrorDetails = badRequestResponse.getBody(ErrorDetails.class);
		assertTrue(optionalErrorDetails.isPresent(), "Error details should be provided.");
	}

	@Disabled("Security is handled externally, thus 401 and 403 cannot happen.")
	@Test
	@Override
	public void listCustomerBill401() throws Exception {

	}

	@Disabled("Security is handled externally, thus 401 and 403 cannot happen.")
	@Test
	@Override
	public void listCustomerBill403() throws Exception {

	}

	@Test
	@Override
	public void listCustomerBill404() throws Exception {
		HttpResponse<List<CustomerBillVO>> badRequestResponse = callAndCatch(
				() -> customerBillApiTestClient.listCustomerBill(null, null, null));

		assertEquals(HttpStatus.OK, badRequestResponse.getStatus(), "We should get an empty list.");
		assertTrue(badRequestResponse.body().isEmpty(), "We should get an empty list.");
	}

	@Disabled("Prohibited by the framework.")
	@Test
	@Override
	public void listCustomerBill405() throws Exception {

	}

	@Disabled("Impossible status.")
	@Test
	@Override
	public void listCustomerBill409() throws Exception {

	}

	@Override public void listCustomerBill500() throws Exception {
	}

	@Test
	@Override
	public void patchCustomerBill200() throws Exception {
		when(clock.instant()).thenReturn(Instant.ofEpochSecond(1234));

		String billId = "urn:ngsi-ld:customer-bill:test-bill";

		CustomerBillVO customerBillVO = CustomerBillVOTestExample.build().id(billId)
				.relatedParty(null)
				.financialAccount(null)
				.billingAccount(null)
				.paymentMethod(null);
		createBill(tmForumMapper.map(customerBillVO));

		HttpResponse<CustomerBillVO> updateResponse = callAndCatch(
				() -> customerBillApiTestClient.patchCustomerBill(billId,
						CustomerBillUpdateVOTestExample.build().state(StateValueVO.ONHOLD)));
		assertEquals(HttpStatus.OK, updateResponse.getStatus(), "The update should be successfull");
		CustomerBillVO updatedCustomerBill = updateResponse.body();

		customerBillVO.setState(StateValueVO.ONHOLD);
		customerBillVO.setLastUpdate(Instant.ofEpochSecond(1234));
		assertEquals(customerBillVO, updatedCustomerBill, "The updated bill should be returned");

	}

	@Test
	@Override
	public void patchCustomerBill400() throws Exception {
		when(clock.instant()).thenReturn(Instant.ofEpochSecond(1234));

		String billId = "urn:ngsi-ld:customer-bill:test-bill";

		CustomerBillVO customerBillVO = CustomerBillVOTestExample.build().id(billId)
				.relatedParty(null)
				.financialAccount(null)
				.billingAccount(null)
				.paymentMethod(null);
		createBill(tmForumMapper.map(customerBillVO));
		HttpResponse<CustomerBillVO> updateResponse = callAndCatch(
				() -> customerBillApiTestClient.patchCustomerBill(billId,
						null));
		assertEquals(HttpStatus.BAD_REQUEST, updateResponse.getStatus(), "The update should not be successfull");
	}

	@Disabled("Security is handled externally, thus 401 and 403 cannot happen.")
	@Test
	@Override
	public void patchCustomerBill401() throws Exception {

	}

	@Disabled("Security is handled externally, thus 401 and 403 cannot happen.")
	@Test
	@Override
	public void patchCustomerBill403() throws Exception {

	}

	@Test
	@Override
	public void patchCustomerBill404() throws Exception {
		HttpResponse<CustomerBillVO> updateResponse = callAndCatch(
				() -> customerBillApiTestClient.patchCustomerBill("urn:ngsi-ld:customer-bill:non-existent",
						CustomerBillUpdateVOTestExample.build()));
		assertEquals(HttpStatus.NOT_FOUND, updateResponse.getStatus(), "The update should not be successfull");
	}

	@Disabled("Prohibited by the framework.")
	@Test
	@Override public void patchCustomerBill405() throws Exception {

	}

	@Disabled("Impossible status.")
	@Test
	@Override public void patchCustomerBill409() throws Exception {

	}

	@Override public void patchCustomerBill500() throws Exception {

	}

	@ParameterizedTest
	@MethodSource("provideFieldParameters")
	public void retrieveCustomerBill200(String message, String fields, CustomerBillVO expectedCustomerBill)
			throws Exception {
		this.expectedCustomerBillVo = expectedCustomerBill;
		this.message = message;
		this.fieldsParameter = fields;
		retrieveCustomerBill200();
	}

	@Override
	public void retrieveCustomerBill200() throws Exception {
		String billId = "urn:ngsi-ld:customer-bill:test-bill";
		expectedCustomerBillVo.id(billId);

		CustomerBillVO customerBillVO = CustomerBillVOTestExample.build().id(billId)
				.relatedParty(null)
				.financialAccount(null)
				.billingAccount(null)
				.paymentMethod(null);
		createBill(tmForumMapper.map(customerBillVO));

		HttpResponse<CustomerBillVO> response = callAndCatch(
				() -> customerBillApiTestClient.retrieveCustomerBill(billId, fieldsParameter));

		assertEquals(HttpStatus.OK, response.getStatus(), message);
		assertEquals(expectedCustomerBillVo, response.body(), message);
	}

	private static Stream<Arguments> provideFieldParameters() {
		return Stream.of(
				Arguments.of("Without a fields parameter everything should be returned.", null,
						CustomerBillVOTestExample.build()
								.relatedParty(null)
								.billingAccount(null)
								.financialAccount(null)
								.paymentMethod(null)
				),
				Arguments.of("With an empty fields parameter only mandatory should be returned.", "",
						CustomerBillVOTestExample.build()
								.billNo(null)
								.category(null)
								.state(null)
								.billDate(null)
								.lastUpdate(null)
								.nextBillDate(null)
								.paymentDueDate(null)
								.runType(null)
								.amountDue(null)
								.appliedPayment(null)
								.billDocument(null)
								.billingAccount(null)
								.billingPeriod(null)
								.financialAccount(null)
								.paymentMethod(null)
								.relatedParty(null)
								.remainingAmount(null)
								.taxExcludedAmount(null)
								.taxIncludedAmount(null)
								.taxItem(null)
								.atBaseType(null)
								.atSchemaLocation(null)
								.atType(null)),
				Arguments.of("Only mandatory and billNo should be returned.", "billNo",
						CustomerBillVOTestExample.build()
								.billDate(null)
								.category(null)
								.lastUpdate(null)
								.nextBillDate(null)
								.paymentDueDate(null)
								.runType(null)
								.amountDue(null)
								.appliedPayment(null)
								.billDocument(null)
								.billingAccount(null)
								.billingPeriod(null)
								.financialAccount(null)
								.paymentMethod(null)
								.relatedParty(null)
								.remainingAmount(null)
								.state(null)
								.taxExcludedAmount(null)
								.taxIncludedAmount(null)
								.taxItem(null)
								.atBaseType(null)
								.atSchemaLocation(null)
								.atType(null)),
				Arguments.of("Only mandatory, billNo, category and state should be returned.", "billNo,category,state",
						CustomerBillVOTestExample.build()
								.billDate(null)
								.lastUpdate(null)
								.nextBillDate(null)
								.paymentDueDate(null)
								.runType(null)
								.amountDue(null)
								.appliedPayment(null)
								.billDocument(null)
								.billingAccount(null)
								.billingPeriod(null)
								.financialAccount(null)
								.paymentMethod(null)
								.relatedParty(null)
								.remainingAmount(null)
								.taxExcludedAmount(null)
								.taxIncludedAmount(null)
								.taxItem(null)
								.atBaseType(null)
								.atSchemaLocation(null)
								.atType(null)),
				Arguments.of("Only mandatory should be returned for non-existent.", "non-existent",
						CustomerBillVOTestExample.build()
								.billNo(null)
								.category(null)
								.state(null)
								.billDate(null)
								.lastUpdate(null)
								.nextBillDate(null)
								.paymentDueDate(null)
								.runType(null)
								.amountDue(null)
								.appliedPayment(null)
								.billDocument(null)
								.billingAccount(null)
								.billingPeriod(null)
								.financialAccount(null)
								.paymentMethod(null)
								.relatedParty(null)
								.remainingAmount(null)
								.taxExcludedAmount(null)
								.taxIncludedAmount(null)
								.taxItem(null)
								.atBaseType(null)
								.atSchemaLocation(null)
								.atType(null))
		);
	}

	@Disabled("400 cannot happen, only 404")
	@Test
	@Override
	public void retrieveCustomerBill400() throws Exception {

	}

	@Disabled("Security is handled externally, thus 401 and 403 cannot happen.")
	@Test
	@Override
	public void retrieveCustomerBill401() throws Exception {

	}

	@Disabled("Security is handled externally, thus 401 and 403 cannot happen.")
	@Test
	@Override
	public void retrieveCustomerBill403() throws Exception {

	}

	@Test
	@Override public void retrieveCustomerBill404() throws Exception {
		HttpResponse<CustomerBillVO> response = callAndCatch(
				() -> customerBillApiTestClient.retrieveCustomerBill("urn:ngsi-ld:customer-bill:non-existent",
						fieldsParameter));

		assertEquals(HttpStatus.NOT_FOUND, response.getStatus(),
				"Not found should be returned for non-existent bills.");
	}

	@Disabled("Prohibited by the framework.")
	@Test
	@Override
	public void retrieveCustomerBill405() throws Exception {

	}

	@Disabled("Conflict not possible on retrieval")
	@Test
	@Override
	public void retrieveCustomerBill409() throws Exception {

	}

	@Override public void retrieveCustomerBill500() throws Exception {

	}

	@Override protected String getEntityType() {
		return CustomerBill.TYPE_CUSTOMER_BILL;
	}
}
