package org.fiware.tmforum.customerbillmanagement;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import io.micronaut.test.annotation.MockBean;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import lombok.RequiredArgsConstructor;
import org.fiware.customerbillmanagement.api.CustomerBillApi;
import org.fiware.customerbillmanagement.api.CustomerBillApiTestClient;
import org.fiware.customerbillmanagement.api.CustomerBillApiTestSpec;
import org.fiware.customerbillmanagement.model.CustomerBillUpdateVO;
import org.fiware.customerbillmanagement.model.CustomerBillUpdateVOTestExample;
import org.fiware.customerbillmanagement.model.CustomerBillVO;
import org.fiware.customerbillmanagement.model.CustomerBillVOTestExample;
import org.fiware.customerbillmanagement.model.StateValueVO;
import org.fiware.ngsi.api.EntitiesApiClient;
import org.fiware.ngsi.model.AdditionalPropertyVO;
import org.fiware.ngsi.model.EntityListVO;
import org.fiware.ngsi.model.EntityVO;
import org.fiware.tmforum.common.exception.ErrorDetails;
import org.fiware.tmforum.common.test.AbstractApiIT;
import org.fiware.tmforum.customerbillmanagement.domain.CustomerBill;
import org.fiware.tmforum.mapping.AdditionalPropertyMixin;
import org.fiware.tmforum.mapping.EntitiesRepository;
import org.fiware.tmforum.mapping.JavaObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RequiredArgsConstructor
@MicronautTest(packages = { "org.fiware.tmforum.customerbillmanagement" })
public class CustomerBillApiIT extends AbstractApiIT implements CustomerBillApiTestSpec {

	private final CustomerBillApiTestClient customerBillApiTestClient;
	private final EntitiesApiClient entitiesApiClient;
	private final JavaObjectMapper javaObjectMapper;
	private final TMForumMapper tmForumMapper;
	private final ObjectMapper objectMapper;

	private String message;
	private String fieldsParameter;
	private CustomerBillUpdateVO customerBillUpdateVO;
	private CustomerBillVO expectedCustomerBillVo;

	private Clock clock = mock(Clock.class);

	@MockBean(Clock.class)
	public Clock clock() {
		return clock;
	}

	private void createBill(CustomerBill customerBillVO) {
		EntityVO entityVO = javaObjectMapper.toEntityVO(customerBillVO);
		entitiesApiClient.createEntity(entityVO, null).block();
	}

	private void deleteBill(URI id) {
		entitiesApiClient.removeEntityById(id, null, null).block();
	}

	@BeforeEach
	@AfterEach
	public void cleanUp() {
		this.objectMapper
				.addMixIn(AdditionalPropertyVO.class, AdditionalPropertyMixin.class);
		this.objectMapper.findAndRegisterModules();
		EntityListVO entityVOS = entitiesApiClient.queryEntities(null,
				null,
				null,
				CustomerBill.TYPE_CUSTOMER_BILL,
				null,
				null,
				null,
				null,
				null,
				null,
				null,
				1000,
				0,
				null,
				null).block();
		entityVOS.stream()
				.filter(Objects::nonNull)
				.map(EntityVO::getId)
				.filter(Objects::nonNull)
				.forEach(this::deleteBill);
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

	@Override public void patchCustomerBill400() throws Exception {

	}

	@Override public void patchCustomerBill401() throws Exception {

	}

	@Override public void patchCustomerBill403() throws Exception {

	}

	@Override public void patchCustomerBill404() throws Exception {

	}

	@Override public void patchCustomerBill405() throws Exception {

	}

	@Override public void patchCustomerBill409() throws Exception {

	}

	@Override public void patchCustomerBill500() throws Exception {

	}

	@Override public void retrieveCustomerBill200() throws Exception {

	}

	@Override public void retrieveCustomerBill400() throws Exception {

	}

	@Override public void retrieveCustomerBill401() throws Exception {

	}

	@Override public void retrieveCustomerBill403() throws Exception {

	}

	@Override public void retrieveCustomerBill404() throws Exception {

	}

	@Override public void retrieveCustomerBill405() throws Exception {

	}

	@Override public void retrieveCustomerBill409() throws Exception {

	}

	@Override public void retrieveCustomerBill500() throws Exception {

	}
}
