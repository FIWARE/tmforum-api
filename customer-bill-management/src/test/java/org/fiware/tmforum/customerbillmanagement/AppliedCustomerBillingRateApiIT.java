package org.fiware.tmforum.customerbillmanagement;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.wistefan.mapping.JavaObjectMapper;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import io.micronaut.test.annotation.MockBean;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import org.fiware.customerbillmanagement.api.AppliedCustomerBillingRateApiTestClient;
import org.fiware.customerbillmanagement.api.AppliedCustomerBillingRateApiTestSpec;
import org.fiware.customerbillmanagement.model.*;
import org.fiware.ngsi.api.EntitiesApiClient;
import org.fiware.ngsi.model.EntityVO;
import org.fiware.tmforum.common.configuration.GeneralProperties;
import org.fiware.tmforum.common.exception.ErrorDetails;
import org.fiware.tmforum.common.notification.TMForumEventHandler;
import org.fiware.tmforum.common.test.AbstractApiIT;
import org.fiware.tmforum.customerbillmanagement.configuration.ApiExtensionProperties;
import org.fiware.tmforum.customerbillmanagement.domain.AppliedCustomerBillingRate;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import reactor.core.publisher.Mono;

import java.time.Clock;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@MicronautTest(packages = { "org.fiware.tmforum.customerbillmanagement" })
public class AppliedCustomerBillingRateApiIT extends AbstractApiIT implements
		AppliedCustomerBillingRateApiTestSpec {

	private final AppliedCustomerBillingRateApiTestClient appliedCustomerBillingRateApiTestClient;
	private final EntitiesApiClient entitiesApiClient;
	private final JavaObjectMapper javaObjectMapper;
	private final TMForumMapper tmForumMapper;

	private String message;
	private String fieldsParameter;
	private AppliedCustomerBillingRateVO expectedAppliedCustomerBillingRateVo;

	private Clock clock = mock(Clock.class);

	protected AppliedCustomerBillingRateApiIT(EntitiesApiClient entitiesApiClient, ObjectMapper objectMapper,
			GeneralProperties generalProperties,
			AppliedCustomerBillingRateApiTestClient appliedCustomerBillingRateApiTestClient,
			EntitiesApiClient entitiesApiClient1, JavaObjectMapper javaObjectMapper,
			TMForumMapper tmForumMapper) {
		super(entitiesApiClient, objectMapper, generalProperties);
		this.appliedCustomerBillingRateApiTestClient = appliedCustomerBillingRateApiTestClient;
		this.entitiesApiClient = entitiesApiClient1;
		this.javaObjectMapper = javaObjectMapper;
		this.tmForumMapper = tmForumMapper;
	}

	@MockBean(Clock.class)
	public Clock clock() {
		return clock;
	}

	@MockBean(ApiExtensionProperties.class)
	public ApiExtensionProperties apiExtensionProperties() {
		ApiExtensionProperties apiExtensionProperties = new ApiExtensionProperties();
		apiExtensionProperties.setEnabled(true);
		return apiExtensionProperties;
	}

	@MockBean(TMForumEventHandler.class)
	public TMForumEventHandler eventHandler() {
		TMForumEventHandler eventHandler = mock(TMForumEventHandler.class);

		when(eventHandler.handleCreateEvent(any())).thenReturn(Mono.empty());
		when(eventHandler.handleUpdateEvent(any(), any())).thenReturn(Mono.empty());

		return eventHandler;
	}

	private void createBill(AppliedCustomerBillingRate appliedCustomerBillingRateVO) {
		EntityVO entityVO = javaObjectMapper.toEntityVO(appliedCustomerBillingRateVO);
		entitiesApiClient.createEntity(entityVO, null).block();
	}

	@Test
	@Override
	public void listAppliedCustomerBillingRate200() throws Exception {
		List<AppliedCustomerBillingRate> billRates = new ArrayList<>();
		List<AppliedCustomerBillingRateVO> expectedBillVOS = new ArrayList<>();
		for (int i = 0; i < 10; i++) {
			AppliedCustomerBillingRateVO appliedCustomerBillingRateVO = AppliedCustomerBillingRateVOTestExample.build().atSchemaLocation(null)
					.id("urn:ngsi-ld:applied-customer-billing-rate:" + UUID.randomUUID().toString())
					.billingAccount(null)
					.product(null)
					.bill(null)
					.periodCoverage(null);
			billRates.add(tmForumMapper.map(appliedCustomerBillingRateVO));
			expectedBillVOS.add(appliedCustomerBillingRateVO);
		}
		billRates.forEach(this::createBill);

		HttpResponse<List<AppliedCustomerBillingRateVO>> appliedCustomerBillingRateResponse = callAndCatch(
				() -> appliedCustomerBillingRateApiTestClient.listAppliedCustomerBillingRate(null, null, null, null));

		assertEquals(HttpStatus.OK, appliedCustomerBillingRateResponse.getStatus(), "The list should be accessible.");
		assertEquals(expectedBillVOS.size(), appliedCustomerBillingRateResponse.getBody().get().size(),
				"All billRates should have been returend.");
		List<AppliedCustomerBillingRateVO> retrievedBills = appliedCustomerBillingRateResponse.getBody().get();

		Map<String, AppliedCustomerBillingRateVO> retrievedBillsMap = retrievedBills.stream()
				.collect(Collectors.toMap(bill -> bill.getId(), bill -> bill));

		expectedBillVOS.stream().forEach(expectedBill -> assertTrue(retrievedBillsMap.containsKey(expectedBill.getId()),
				String.format("All created billRates should be returned - Missing: %s.", expectedBill,
						retrievedBills)));
		expectedBillVOS.stream().forEach(
				expectedBill -> assertEquals(expectedBill, retrievedBillsMap.get(expectedBill.getId()),
						"The correct bill should be retrieved."));

		// get with pagination
		Integer limit = 5;
		HttpResponse<List<AppliedCustomerBillingRateVO>> firstPartResponse = callAndCatch(
				() -> appliedCustomerBillingRateApiTestClient.listAppliedCustomerBillingRate(null, null, 0, limit));
		assertEquals(limit, firstPartResponse.body().size(),
				"Only the requested number of entries should be returend.");
		HttpResponse<List<AppliedCustomerBillingRateVO>> secondPartResponse = callAndCatch(
				() -> appliedCustomerBillingRateApiTestClient.listAppliedCustomerBillingRate(null, null, 0 + limit, limit));
		assertEquals(limit, secondPartResponse.body().size(),
				"Only the requested number of entries should be returend.");

		retrievedBills.clear();
		retrievedBills.addAll(firstPartResponse.body());
		retrievedBills.addAll(secondPartResponse.body());
		expectedBillVOS.stream().forEach(expectedBill -> assertTrue(retrievedBillsMap.containsKey(expectedBill.getId()),
				String.format("All created billRates should be returned - Missing: %s.", expectedBill)));
		expectedBillVOS.stream().forEach(
				expectedBill -> assertEquals(expectedBill, retrievedBillsMap.get(expectedBill.getId()),
						"The correct bill should be retrieved."));

	}

	@Test
	@Override
	public void listAppliedCustomerBillingRate400() throws Exception {
		HttpResponse<List<AppliedCustomerBillingRateVO>> badRequestResponse = callAndCatch(
				() -> appliedCustomerBillingRateApiTestClient.listAppliedCustomerBillingRate(null, null, -1, null));
		assertEquals(HttpStatus.BAD_REQUEST,
				badRequestResponse.getStatus(),
				"Negative offsets are impossible.");

		Optional<ErrorDetails> optionalErrorDetails = badRequestResponse.getBody(ErrorDetails.class);
		assertTrue(optionalErrorDetails.isPresent(), "Error details should be provided.");

		badRequestResponse = callAndCatch(
				() -> appliedCustomerBillingRateApiTestClient.listAppliedCustomerBillingRate(null, null, null, -1));
		assertEquals(HttpStatus.BAD_REQUEST,
				badRequestResponse.getStatus(),
				"Negative limits are impossible.");
		optionalErrorDetails = badRequestResponse.getBody(ErrorDetails.class);
		assertTrue(optionalErrorDetails.isPresent(), "Error details should be provided.");
	}

	@Disabled("Security is handled externally, thus 401 and 403 cannot happen.")
	@Test
	@Override
	public void listAppliedCustomerBillingRate401() throws Exception {

	}

	@Disabled("Security is handled externally, thus 401 and 403 cannot happen.")
	@Test
	@Override
	public void listAppliedCustomerBillingRate403() throws Exception {

	}

	@Test
	@Override
	public void listAppliedCustomerBillingRate404() throws Exception {
		HttpResponse<List<AppliedCustomerBillingRateVO>> badRequestResponse = callAndCatch(
				() -> appliedCustomerBillingRateApiTestClient.listAppliedCustomerBillingRate(null, null, null, null));

		assertEquals(HttpStatus.OK, badRequestResponse.getStatus(), "We should get an empty list.");
		assertTrue(badRequestResponse.body().isEmpty(), "We should get an empty list.");
	}

	@Disabled("Prohibited by the framework.")
	@Test
	@Override
	public void listAppliedCustomerBillingRate405() throws Exception {

	}

	@Disabled("Impossible status.")
	@Test
	@Override
	public void listAppliedCustomerBillingRate409() throws Exception {

	}

	@Override public void listAppliedCustomerBillingRate500() throws Exception {
	}

	@ParameterizedTest
	@MethodSource("provideFieldParameters")
	public void retrieveAppliedCustomerBillingRate200(String message, String fields,
			AppliedCustomerBillingRateVO expectedAppliedCustomerBillingRate)
			throws Exception {
		this.expectedAppliedCustomerBillingRateVo = expectedAppliedCustomerBillingRate;
		this.message = message;
		this.fieldsParameter = fields;
		retrieveAppliedCustomerBillingRate200();
	}

	@Override
	public void retrieveAppliedCustomerBillingRate200() throws Exception {
		String billId = "urn:ngsi-ld:applied-customer-billing-rate:test-rate";
		expectedAppliedCustomerBillingRateVo.id(billId);

		AppliedCustomerBillingRateVO appliedCustomerBillingRateVO = AppliedCustomerBillingRateVOTestExample.build().atSchemaLocation(null)
				.id(billId)
				.bill(BillRefVOTestExample.build().atSchemaLocation(null).id("urn:ngsi-ld:bill:bill"))
				.billingAccount(BillingAccountRefVOTestExample.build().atSchemaLocation(null).id("urn:ngsi-ld:billing-account:account"))
				.product(ProductRefVOTestExample.build().atSchemaLocation(null).id("urn:ngsi-ld:product:product"));
		createBill(tmForumMapper.map(appliedCustomerBillingRateVO));

		HttpResponse<AppliedCustomerBillingRateVO> response = callAndCatch(
				() -> appliedCustomerBillingRateApiTestClient.retrieveAppliedCustomerBillingRate(null, billId,
						fieldsParameter));

		assertEquals(HttpStatus.OK, response.getStatus(), message);
		assertEquals(expectedAppliedCustomerBillingRateVo, response.body(), message);
	}

	private static Stream<Arguments> provideFieldParameters() {
		return Stream.of(
				Arguments.of("Without a fields parameter everything should be returned.", null,
						AppliedCustomerBillingRateVOTestExample.build().atSchemaLocation(null)
								.periodCoverage(null)
								.billingAccount(BillingAccountRefVOTestExample.build().atSchemaLocation(null)
										.id("urn:ngsi-ld:billing-account:account"))
								.product(ProductRefVOTestExample.build().atSchemaLocation(null).id("urn:ngsi-ld:product:product"))
								.bill(BillRefVOTestExample.build().atSchemaLocation(null).id("urn:ngsi-ld:bill:bill"))),
				Arguments.of("With an empty fields parameter only mandatory should be returned.", "",
						AppliedCustomerBillingRateVOTestExample.build().atSchemaLocation(null)
								.date(null)
								.description(null)
								.isBilled(null)
								.name(null)
								.type(null)
								.appliedTax(null)
								.bill(null)
								.billingAccount(null)
								.characteristic(null)
								.periodCoverage(null)
								.product(null)
								.taxExcludedAmount(null)
								.taxIncludedAmount(null)
								.atBaseType(null)
								.atSchemaLocation(null)
								.atType(null)
								.type(null)),
				Arguments.of("Only mandatory and name should be returned.", "name",
						AppliedCustomerBillingRateVOTestExample.build().atSchemaLocation(null)
								.date(null)
								.description(null)
								.isBilled(null)
								.type(null)
								.appliedTax(null)
								.bill(null)
								.billingAccount(null)
								.characteristic(null)
								.periodCoverage(null)
								.product(null)
								.taxExcludedAmount(null)
								.taxIncludedAmount(null)
								.atBaseType(null)
								.atSchemaLocation(null)
								.atType(null)
								.type(null)),
				Arguments.of("Only mandatory, name, bill and type should be returned.", "name,bill,type",
						AppliedCustomerBillingRateVOTestExample.build().atSchemaLocation(null)
								.date(null)
								.description(null)
								.isBilled(null)
								.appliedTax(null)
								.bill(BillRefVOTestExample.build().atSchemaLocation(null).id("urn:ngsi-ld:bill:bill"))
								.billingAccount(null)
								.characteristic(null)
								.periodCoverage(null)
								.product(null)
								.taxExcludedAmount(null)
								.taxIncludedAmount(null)
								.atBaseType(null)
								.atSchemaLocation(null)
								.atType(null)),
				Arguments.of("Only mandatory should be returned for non-existent.", "non-existent",
						AppliedCustomerBillingRateVOTestExample.build().atSchemaLocation(null)
								.date(null)
								.description(null)
								.isBilled(null)
								.name(null)
								.type(null)
								.appliedTax(null)
								.bill(null)
								.billingAccount(null)
								.characteristic(null)
								.periodCoverage(null)
								.product(null)
								.taxExcludedAmount(null)
								.taxIncludedAmount(null)
								.atBaseType(null)
								.atSchemaLocation(null)
								.atType(null)
								.type(null))
		);
	}

	@Disabled("400 cannot happen, only 404")
	@Test
	@Override
	public void retrieveAppliedCustomerBillingRate400() throws Exception {

	}

	@Disabled("Security is handled externally, thus 401 and 403 cannot happen.")
	@Test
	@Override
	public void retrieveAppliedCustomerBillingRate401() throws Exception {

	}

	@Disabled("Security is handled externally, thus 401 and 403 cannot happen.")
	@Test
	@Override
	public void retrieveAppliedCustomerBillingRate403() throws Exception {

	}

	@Test
	@Override public void retrieveAppliedCustomerBillingRate404() throws Exception {
		HttpResponse<AppliedCustomerBillingRateVO> response = callAndCatch(
				() -> appliedCustomerBillingRateApiTestClient.retrieveAppliedCustomerBillingRate(null,
						"urn:ngsi-ld:customer-bill:non-existent",
						fieldsParameter));

		assertEquals(HttpStatus.NOT_FOUND, response.getStatus(),
				"Not found should be returned for non-existent billRates.");
	}

	@Disabled("Prohibited by the framework.")
	@Test
	@Override
	public void retrieveAppliedCustomerBillingRate405() throws Exception {

	}

	@Disabled("Conflict not possible on retrieval")
	@Test
	@Override
	public void retrieveAppliedCustomerBillingRate409() throws Exception {

	}

	@Override public void retrieveAppliedCustomerBillingRate500() throws Exception {

	}
	

	@Override protected String getEntityType() {
		return AppliedCustomerBillingRate.TYPE_APPLIED_CUSTOMER_BILLING_RATE;
	}
}
