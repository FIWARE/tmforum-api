package org.fiware.tmforum.customerbillmanagement;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.micronaut.context.annotation.Property;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import io.micronaut.test.annotation.MockBean;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import org.fiware.customerbillmanagement.api.ext.AppliedCustomerBillingRateApiTestClient;
import org.fiware.customerbillmanagement.api.ext.AppliedCustomerBillingRateApiTestSpec;
import org.fiware.customerbillmanagement.model.*;
import org.fiware.ngsi.api.EntitiesApiClient;
import org.fiware.ngsi.model.EntityVO;
import org.fiware.ngsi.model.PropertyVO;
import org.fiware.tmforum.common.configuration.GeneralProperties;
import org.fiware.tmforum.common.exception.ErrorDetails;
import org.fiware.tmforum.common.test.AbstractApiIT;
import org.fiware.tmforum.customerbillmanagement.configuration.ApiExtensionProperties;
import org.fiware.tmforum.customerbillmanagement.domain.AppliedCustomerBillingRate;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.net.URI;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@MicronautTest(packages = {"org.fiware.tmforum.customerbillmanagement"})
@Property(name = "apiExtension.enabled", value = "true")
public class ExtendedAppliedCustomerBillingRateApiIT extends AbstractApiIT implements
		AppliedCustomerBillingRateApiTestSpec {

	private static final String BILL_ID = "urn:ngsi-ld:customer-bill:test-bill";
	private static final String BILLING_ACCOUNT_ID = "urn:ngsi-ld:billing-account:test-billing-account";

	private final AppliedCustomerBillingRateApiTestClient appliedCustomerBillingRateApiTestClient;
	private final EntitiesApiClient entitiesApiClient;

	private String message;
	private AppliedCustomerBillingRateCreateVO appliedCustomerBillingRateCreateVO;
	private AppliedCustomerBillingRateUpdateVO appliedCustomerBillingRateUpdateVO;
	private AppliedCustomerBillingRateVO expectedAppliedCustomerBillingRateVo;

	private Clock clock = mock(Clock.class);

	protected ExtendedAppliedCustomerBillingRateApiIT(EntitiesApiClient entitiesApiClient, ObjectMapper objectMapper,
													  GeneralProperties generalProperties,
													  AppliedCustomerBillingRateApiTestClient appliedCustomerBillingRateApiTestClient) {
		super(entitiesApiClient, objectMapper, generalProperties);
		this.appliedCustomerBillingRateApiTestClient = appliedCustomerBillingRateApiTestClient;
		this.entitiesApiClient = entitiesApiClient;
	}

	@MockBean(Clock.class)
	public Clock clock() {
		return clock;
	}

	@Override
	protected String getEntityType() {
		return AppliedCustomerBillingRate.TYPE_APPLIED_CUSTOMER_BILLING_RATE;
	}

	@BeforeEach
	public void createRequiredReferences() {

		CustomerBillVO customerBillVO = CustomerBillVOTestExample.build().atSchemaLocation(null);
		EntityVO billEntityVO = new EntityVO()
				.atContext("https://uri.etsi.org/ngsi-ld/v1/ngsi-ld-core-context.jsonld")
				.id(URI.create(BILL_ID))
				.type("customer-bill");
		billEntityVO.setAdditionalProperties("atSchemaLocation", new PropertyVO().value("my:uri"));
		entitiesApiClient.createEntity(billEntityVO, null).block();

		EntityVO billingAccountEntityVO = new EntityVO()
				.atContext("https://uri.etsi.org/ngsi-ld/v1/ngsi-ld-core-context.jsonld")
				.id(URI.create(BILLING_ACCOUNT_ID))
				.type("billing-account");
		billingAccountEntityVO.setAdditionalProperties("atSchemaLocation", new PropertyVO().value("my:uri"));
		entitiesApiClient.createEntity(billingAccountEntityVO, null).block();
	}

	@AfterEach
	public void deleteRequiredReferences() {
		try {
			entitiesApiClient.removeEntityById(URI.create(BILL_ID), null, null).block();
		} catch (Exception e) {
			// noop - might fail if already removed
		}
		try {
			entitiesApiClient.removeEntityById(URI.create(BILLING_ACCOUNT_ID), null, null).block();
		} catch (Exception e) {
			// noop - might fail if already removed
		}
	}

	@ParameterizedTest
	@MethodSource("provideValidAppliedCustomerBillingRates")
	public void createAppliedCustomerBillingRate201(String message, AppliedCustomerBillingRateCreateVO appliedCustomerBillingRateCreateVO,
													AppliedCustomerBillingRateVO expectedAppliedCustomerBillingRateVo) throws Exception {
		this.message = message;
		this.appliedCustomerBillingRateCreateVO = appliedCustomerBillingRateCreateVO;
		this.expectedAppliedCustomerBillingRateVo = expectedAppliedCustomerBillingRateVo;
		createAppliedCustomerBillingRate201();

	}

	@Override
	public void createAppliedCustomerBillingRate201() throws Exception {
		Instant currentTimeInstant = Instant.ofEpochSecond(10000);
		when(clock.instant()).thenReturn(currentTimeInstant);

		HttpResponse<AppliedCustomerBillingRateVO> appliedCustomerBillingRateVOHttpResponse = callAndCatch(
				() -> appliedCustomerBillingRateApiTestClient.createAppliedCustomerBillingRate(null, appliedCustomerBillingRateCreateVO));
		assertEquals(HttpStatus.CREATED, appliedCustomerBillingRateVOHttpResponse.getStatus(), message);
		String acabId = appliedCustomerBillingRateVOHttpResponse.body().getId();
		expectedAppliedCustomerBillingRateVo.setId(acabId);
		expectedAppliedCustomerBillingRateVo.setHref(acabId);
		expectedAppliedCustomerBillingRateVo.setDate(currentTimeInstant);

		assertEquals(expectedAppliedCustomerBillingRateVo, appliedCustomerBillingRateVOHttpResponse.body(), message);
	}

	private static Stream<Arguments> provideValidAppliedCustomerBillingRates() {
		return Stream.of(
				Arguments.of(
						"When the appliedCustomerBillingRate is not yet billed and the account is provided, it should be created.",
						AppliedCustomerBillingRateCreateVOTestExample.build().atSchemaLocation(null)
								.isBilled(false)
								.bill(null)
								.product(null)
								.billingAccount(BillingAccountRefVOTestExample.build().atSchemaLocation(null).id(BILLING_ACCOUNT_ID).href(BILLING_ACCOUNT_ID)),
						AppliedCustomerBillingRateVOTestExample.build().atSchemaLocation(null)
								.isBilled(false)
								.product(null)
								.bill(null)
								.billingAccount(BillingAccountRefVOTestExample.build().atSchemaLocation(null).id(BILLING_ACCOUNT_ID).href(BILLING_ACCOUNT_ID))
				),
				Arguments.of(
						"When the appliedCustomerBillingRate is billed and the bill is provided, it should be created.",
						AppliedCustomerBillingRateCreateVOTestExample.build().atSchemaLocation(null)
								.isBilled(true)
								.billingAccount(null)
								.product(null)
								.bill(BillRefVOTestExample.build().atSchemaLocation(null).id(BILL_ID).href(BILL_ID)),
						AppliedCustomerBillingRateVOTestExample.build().atSchemaLocation(null)
								.isBilled(true)
								.product(null)
								.billingAccount(null)
								.bill(BillRefVOTestExample.build().atSchemaLocation(null).id(BILL_ID).href(BILL_ID))),
				Arguments.of(
						"When the name is included, it should be created.",
						AppliedCustomerBillingRateCreateVOTestExample.build().atSchemaLocation(null)
								.isBilled(true)
								.name("my-name")
								.billingAccount(null)
								.product(null)
								.bill(BillRefVOTestExample.build().atSchemaLocation(null).id(BILL_ID).href(BILL_ID)),
						AppliedCustomerBillingRateVOTestExample.build().atSchemaLocation(null)
								.isBilled(true)
								.name("my-name")
								.product(null)
								.billingAccount(null)
								.bill(BillRefVOTestExample.build().atSchemaLocation(null).id(BILL_ID).href(BILL_ID))),
				Arguments.of(
						"When the description is included, it should be created.",
						AppliedCustomerBillingRateCreateVOTestExample.build().atSchemaLocation(null)
								.isBilled(true)
								.description("my-desc")
								.billingAccount(null)
								.product(null)
								.bill(BillRefVOTestExample.build().atSchemaLocation(null).id(BILL_ID).href(BILL_ID)),
						AppliedCustomerBillingRateVOTestExample.build().atSchemaLocation(null)
								.isBilled(true)
								.description("my-desc")
								.product(null)
								.billingAccount(null)
								.bill(BillRefVOTestExample.build().atSchemaLocation(null).id(BILL_ID).href(BILL_ID))),
				Arguments.of(
						"When the type is included, it should be created.",
						AppliedCustomerBillingRateCreateVOTestExample.build().atSchemaLocation(null)
								.isBilled(true)
								.type("type")
								.billingAccount(null)
								.product(null)
								.bill(BillRefVOTestExample.build().atSchemaLocation(null).id(BILL_ID).href(BILL_ID)),
						AppliedCustomerBillingRateVOTestExample.build().atSchemaLocation(null)
								.isBilled(true)
								.type("type")
								.product(null)
								.billingAccount(null)
								.bill(BillRefVOTestExample.build().atSchemaLocation(null).id(BILL_ID).href(BILL_ID))),
				Arguments.of(
						"When the appliedTaxes are included, it should be created.",
						AppliedCustomerBillingRateCreateVOTestExample.build().atSchemaLocation(null)
								.isBilled(true)
								.appliedTax(List.of(AppliedBillingTaxRateVOTestExample.build().atSchemaLocation(null)))
								.billingAccount(null)
								.product(null)
								.bill(BillRefVOTestExample.build().atSchemaLocation(null).id(BILL_ID).href(BILL_ID)),
						AppliedCustomerBillingRateVOTestExample.build().atSchemaLocation(null)
								.isBilled(true)
								.appliedTax(List.of(AppliedBillingTaxRateVOTestExample.build().atSchemaLocation(null)))
								.product(null)
								.billingAccount(null)
								.bill(BillRefVOTestExample.build().atSchemaLocation(null).id(BILL_ID).href(BILL_ID))),
				Arguments.of(
						"When the billing rate characteristics are included, it should be created.",
						AppliedCustomerBillingRateCreateVOTestExample.build().atSchemaLocation(null)
								.isBilled(true)
								.characteristic(List.of(AppliedBillingRateCharacteristicVOTestExample.build().atSchemaLocation(null)))
								.billingAccount(null)
								.product(null)
								.bill(BillRefVOTestExample.build().atSchemaLocation(null).id(BILL_ID).href(BILL_ID)),
						AppliedCustomerBillingRateVOTestExample.build().atSchemaLocation(null)
								.isBilled(true)
								.characteristic(List.of(AppliedBillingRateCharacteristicVOTestExample.build().atSchemaLocation(null)))
								.product(null)
								.billingAccount(null)
								.bill(BillRefVOTestExample.build().atSchemaLocation(null).id(BILL_ID).href(BILL_ID)))
		);
	}

	@ParameterizedTest
	@MethodSource("provideInvalidAppliedCustomerBillingRates")
	public void createAppliedCustomerBillingRate400(String message, AppliedCustomerBillingRateCreateVO invalidCreateVO) throws Exception {
		this.message = message;
		this.appliedCustomerBillingRateCreateVO = invalidCreateVO;
		createAppliedCustomerBillingRate400();
	}

	@Override
	public void createAppliedCustomerBillingRate400() throws Exception {

		HttpResponse<AppliedCustomerBillingRateVO> creationResponse = callAndCatch(
				() -> appliedCustomerBillingRateApiTestClient.createAppliedCustomerBillingRate(null, appliedCustomerBillingRateCreateVO));
		assertEquals(HttpStatus.BAD_REQUEST, creationResponse.getStatus(), message);
		Optional<ErrorDetails> optionalErrorDetails = creationResponse.getBody(ErrorDetails.class);
		assertTrue(optionalErrorDetails.isPresent(), "Error details should be provided.");
	}

	private static Stream<Arguments> provideInvalidAppliedCustomerBillingRates() {
		return Stream.of(
				Arguments.of(
						"When the appliedCustomerBillingRate is not yet billed and the account is not provided, it should fail.",
						AppliedCustomerBillingRateCreateVOTestExample.build().atSchemaLocation(null)
								.isBilled(false)
								.bill(null)
								.product(null)
								.billingAccount(null)
				),
				Arguments.of(
						"When the appliedCustomerBillingRate is not yet billed and the account is not provided, it should fail.",
						AppliedCustomerBillingRateCreateVOTestExample.build().atSchemaLocation(null)
								.isBilled(false)
								.bill(BillRefVOTestExample.build().atSchemaLocation(null).id(BILL_ID).href(BILL_ID))
								.product(null)
								.billingAccount(null)
				),
				Arguments.of(
						"When the appliedCustomerBillingRate is billed and the bill is not provided, it should fail.",
						AppliedCustomerBillingRateCreateVOTestExample.build().atSchemaLocation(null)
								.isBilled(true)
								.billingAccount(null)
								.product(null)
								.bill(null)),
				Arguments.of(
						"When the appliedCustomerBillingRate is billed and the bill is not provided, it should fail.",
						AppliedCustomerBillingRateCreateVOTestExample.build().atSchemaLocation(null)
								.isBilled(true)
								.billingAccount(BillingAccountRefVOTestExample.build().atSchemaLocation(null).id(BILLING_ACCOUNT_ID).href(BILLING_ACCOUNT_ID))
								.product(null)
								.bill(null)),
				Arguments.of(
						"When an invalid productRef is provided, it should fail.",
						AppliedCustomerBillingRateCreateVOTestExample.build().atSchemaLocation(null)
								.isBilled(true)
								.bill(BillRefVOTestExample.build().atSchemaLocation(null).id(BILL_ID).href(BILL_ID))
								.product(ProductRefVOTestExample.build().atSchemaLocation(null))
								.billingAccount(null))
		);
	}

	@Disabled("Security is handled externally, thus 401 and 403 cannot happen.")
	@Test
	@Override
	public void createAppliedCustomerBillingRate401() throws Exception {

	}

	@Disabled("Security is handled externally, thus 401 and 403 cannot happen.")
	@Test
	@Override
	public void createAppliedCustomerBillingRate403() throws Exception {

	}

	@Disabled("Prohibited by the framework.")
	@Test
	@Override
	public void createAppliedCustomerBillingRate405() throws Exception {

	}

	@Disabled("No implicit creation, impossible state.")
	@Test
	@Override
	public void createAppliedCustomerBillingRate409() throws Exception {

	}

	@Override
	public void createAppliedCustomerBillingRate500() throws Exception {

	}


	@ParameterizedTest
	@MethodSource("provideValidAppliedCustomerBillingRateUpdates")
	public void updateAppliedCustomerBillingRate200(String message, AppliedCustomerBillingRateCreateVO initialCreate, AppliedCustomerBillingRateUpdateVO appliedCustomerBillingRateUpdateVO,
													AppliedCustomerBillingRateVO expectedAppliedCustomerBillingRateVo) throws Exception {
		this.message = message;
		this.appliedCustomerBillingRateCreateVO = initialCreate;
		this.appliedCustomerBillingRateUpdateVO = appliedCustomerBillingRateUpdateVO;
		this.expectedAppliedCustomerBillingRateVo = expectedAppliedCustomerBillingRateVo;
		updateAppliedCustomerBillingRate200();
	}

	@Override
	public void updateAppliedCustomerBillingRate200() throws Exception {

		Instant currentTimeInstant = Instant.ofEpochSecond(10000);
		when(clock.instant()).thenReturn(currentTimeInstant);

		//first create
		HttpResponse<AppliedCustomerBillingRateVO> createResponse = callAndCatch(
				() -> appliedCustomerBillingRateApiTestClient.createAppliedCustomerBillingRate(null, appliedCustomerBillingRateCreateVO));
		assertEquals(HttpStatus.CREATED, createResponse.getStatus(),
				"The applied customer billing rate should have been created first.");

		String id = createResponse.body().getId();

		HttpResponse<AppliedCustomerBillingRateVO> updateResponse = callAndCatch(
				() -> appliedCustomerBillingRateApiTestClient.updateAppliedCustomerBillingRate(null, id, appliedCustomerBillingRateUpdateVO));
		assertEquals(HttpStatus.OK, updateResponse.getStatus(), message);

		AppliedCustomerBillingRateVO updated = updateResponse.body();
		expectedAppliedCustomerBillingRateVo
				.href(id)
				.id(id)
				.date(currentTimeInstant);

		assertEquals(expectedAppliedCustomerBillingRateVo, updated, message);
	}

	public static Stream<Arguments> provideValidAppliedCustomerBillingRateUpdates() {
		return Stream.of(
				Arguments.of(
						"When the appliedCustomerBillingRate is not yet billed and the account is provided, it should be update.",
						AppliedCustomerBillingRateCreateVOTestExample.build().atSchemaLocation(null)
								.isBilled(false)
								.bill(null)
								.product(null)
								.billingAccount(BillingAccountRefVOTestExample.build().atSchemaLocation(null).id(BILLING_ACCOUNT_ID).href(BILLING_ACCOUNT_ID)),
						AppliedCustomerBillingRateUpdateVOTestExample.build().atSchemaLocation(null)
								.isBilled(true)
								.billingAccount(null)
								.product(null)
								.bill(BillRefVOTestExample.build().atSchemaLocation(null).id(BILL_ID).href(BILL_ID)),
						AppliedCustomerBillingRateVOTestExample.build().atSchemaLocation(null)
								.isBilled(true)
								.product(null)
								.billingAccount(BillingAccountRefVOTestExample.build().atSchemaLocation(null).id(BILLING_ACCOUNT_ID).href(BILLING_ACCOUNT_ID))
								.bill(BillRefVOTestExample.build().atSchemaLocation(null).id(BILL_ID).href(BILL_ID))
				),
				Arguments.of(
						"When the appliedCustomerBillingRate is billed and the bill is provided, it should be created.",
						AppliedCustomerBillingRateCreateVOTestExample.build().atSchemaLocation(null)
								.isBilled(true)
								.billingAccount(null)
								.product(null)
								.bill(BillRefVOTestExample.build().atSchemaLocation(null).id(BILL_ID).href(BILL_ID)),
						AppliedCustomerBillingRateUpdateVOTestExample.build().atSchemaLocation(null)
								.isBilled(false)
								.product(null)
								.billingAccount(BillingAccountRefVOTestExample.build().atSchemaLocation(null).id(BILLING_ACCOUNT_ID).href(BILLING_ACCOUNT_ID))
								.bill(null),
						AppliedCustomerBillingRateVOTestExample.build().atSchemaLocation(null)
								.isBilled(false)
								.product(null)
								.billingAccount(BillingAccountRefVOTestExample.build().atSchemaLocation(null).id(BILLING_ACCOUNT_ID).href(BILLING_ACCOUNT_ID))
								.bill(BillRefVOTestExample.build().atSchemaLocation(null).id(BILL_ID).href(BILL_ID)))
		);
	}

	@ParameterizedTest
	@MethodSource("provideInvalidAppliedCustomerBillingRateUpdates")
	public void updateAppliedCustomerBillingRate400(String message, AppliedCustomerBillingRateUpdateVO invalidUpdate) throws Exception {
		this.message = message;
		this.appliedCustomerBillingRateUpdateVO = invalidUpdate;
		updateAppliedCustomerBillingRate400();
	}

	@Override
	public void updateAppliedCustomerBillingRate400() throws Exception {
		Instant currentTimeInstant = Instant.ofEpochSecond(10000);
		when(clock.instant()).thenReturn(currentTimeInstant);

		//first create
		AppliedCustomerBillingRateCreateVO initialCreate = AppliedCustomerBillingRateCreateVOTestExample.build().atSchemaLocation(null)
				.product(null)
				.isBilled(true)
				.billingAccount(null)
				.bill(BillRefVOTestExample.build().atSchemaLocation(null).id(BILL_ID).href(BILL_ID));

		HttpResponse<AppliedCustomerBillingRateVO> createResponse = callAndCatch(
				() -> appliedCustomerBillingRateApiTestClient.createAppliedCustomerBillingRate(null, initialCreate));
		assertEquals(HttpStatus.CREATED, createResponse.getStatus(),
				"The applied customer billing rate should have been created first.");

		String id = createResponse.body().getId();

		HttpResponse<AppliedCustomerBillingRateVO> updateResponse = callAndCatch(
				() -> appliedCustomerBillingRateApiTestClient.updateAppliedCustomerBillingRate(null, id, appliedCustomerBillingRateUpdateVO));
		assertEquals(HttpStatus.BAD_REQUEST, updateResponse.getStatus(), message);

		Optional<ErrorDetails> optionalErrorDetails = updateResponse.getBody(ErrorDetails.class);
		assertTrue(optionalErrorDetails.isPresent(), "Some error details should be present.");
	}

	public static Stream<Arguments> provideInvalidAppliedCustomerBillingRateUpdates() {
		return Stream.of(
				Arguments.of(
						"When the appliedCustomerBillingRate is not yet billed and the account is not provided, it should fail.",
						AppliedCustomerBillingRateUpdateVOTestExample.build().atSchemaLocation(null)
								.isBilled(false)
								.bill(null)
								.billingAccount(null)
				),
				Arguments.of(
						"When the appliedCustomerBillingRate is not yet billed and the account is not provided, it should fail.",
						AppliedCustomerBillingRateUpdateVOTestExample.build().atSchemaLocation(null)
								.isBilled(false)
								.bill(BillRefVOTestExample.build().atSchemaLocation(null).id(BILL_ID).href(BILL_ID))
								.billingAccount(null)
				),
				Arguments.of(
						"When the appliedCustomerBillingRate is billed and the bill is not provided, it should fail.",
						AppliedCustomerBillingRateUpdateVOTestExample.build().atSchemaLocation(null)
								.isBilled(true)
								.billingAccount(null)
								.bill(null)),
				Arguments.of(
						"When the appliedCustomerBillingRate is billed and the bill is not provided, it should fail.",
						AppliedCustomerBillingRateUpdateVOTestExample.build().atSchemaLocation(null)
								.isBilled(true)
								.billingAccount(BillingAccountRefVOTestExample.build().atSchemaLocation(null).id(BILLING_ACCOUNT_ID).href(BILLING_ACCOUNT_ID))
								.bill(null))
		);
	}

	@Disabled("Security is handled externally, thus 401 and 403 cannot happen.")
	@Test
	@Override
	public void updateAppliedCustomerBillingRate401() throws Exception {

	}

	@Disabled("Security is handled externally, thus 401 and 403 cannot happen.")
	@Test
	@Override
	public void updateAppliedCustomerBillingRate403() throws Exception {

	}

	@Test
	@Override
	public void updateAppliedCustomerBillingRate404() throws Exception {
		AppliedCustomerBillingRateUpdateVO updateVO = AppliedCustomerBillingRateUpdateVOTestExample.build().atSchemaLocation(null)
				.bill(BillRefVOTestExample.build().atSchemaLocation(null).id(BILL_ID).href(BILL_ID))
				.isBilled(true)
				.product(null)
				.billingAccount(null);

		assertEquals(
				HttpStatus.NOT_FOUND,
				callAndCatch(() -> appliedCustomerBillingRateApiTestClient.updateAppliedCustomerBillingRate(null,
						"urn:ngsi-ld:applied-customer-billing-rate:not-existent", updateVO)).getStatus(),
				"Non existent applied-customer-billing-rate should not be updated.");
	}

	@Disabled("Prohibited by the framework.")
	@Test
	@Override
	public void updateAppliedCustomerBillingRate405() throws Exception {

	}

	@Override
	public void updateAppliedCustomerBillingRate409() throws Exception {

	}

	@Override
	public void updateAppliedCustomerBillingRate500() throws Exception {

	}
}
