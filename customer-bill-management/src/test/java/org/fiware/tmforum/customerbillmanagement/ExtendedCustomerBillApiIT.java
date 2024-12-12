package org.fiware.tmforum.customerbillmanagement;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.micronaut.context.annotation.Property;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import io.micronaut.test.annotation.MockBean;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import org.fiware.customerbillmanagement.api.ext.AppliedCustomerBillingRateApiTestSpec;
import org.fiware.customerbillmanagement.api.ext.CustomerBillExtensionApiTestClient;
import org.fiware.customerbillmanagement.api.ext.CustomerBillExtensionApiTestSpec;
import org.fiware.customerbillmanagement.model.*;
import org.fiware.ngsi.api.EntitiesApiClient;
import org.fiware.tmforum.common.configuration.GeneralProperties;
import org.fiware.tmforum.common.test.AbstractApiIT;
import org.fiware.tmforum.customerbillmanagement.domain.CustomerBill;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.net.URI;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@MicronautTest(packages = {"org.fiware.tmforum.customerbillmanagement"})
@Property(name = "apiExtension.enabled", value = "true")
public class ExtendedCustomerBillApiIT extends AbstractApiIT implements CustomerBillExtensionApiTestSpec {

	private final CustomerBillExtensionApiTestClient testClient;

	private String message;
	private CustomerBillCreateVO customerBillCreateVO;

	private Clock clock = mock(Clock.class);

	@MockBean(Clock.class)
	public Clock clock() {
		return clock;
	}


	protected ExtendedCustomerBillApiIT(EntitiesApiClient entitiesApiClient, ObjectMapper objectMapper, GeneralProperties generalProperties, CustomerBillExtensionApiTestClient testClient) {
		super(entitiesApiClient, objectMapper, generalProperties);
		this.testClient = testClient;
	}

	@Test
	@Override
	public void createCustomerBill201() throws Exception {

		Instant currentTimeInstant = Instant.ofEpochSecond(10000);
		when(clock.instant()).thenReturn(currentTimeInstant);

		CustomerBillCreateVO createVO = CustomerBillCreateVOTestExample.build()
				.atSchemaLocation(null)
				.billingAccount(null)
				.financialAccount(null)
				.paymentMethod(null);

		HttpResponse<CustomerBillVO> customerBillVOHttpResponse = callAndCatch(
				() -> testClient.createCustomerBill(null,
						createVO));
		assertEquals(HttpStatus.CREATED, customerBillVOHttpResponse.getStatus(), "The bill should have successfully been created.");
		String rfId = customerBillVOHttpResponse.body().getId();

		CustomerBillVO expectedCustomerBillVO = CustomerBillVOTestExample.build()
				.id(rfId)
				.href(rfId)
				.lastUpdate(currentTimeInstant)
				.atSchemaLocation(null)
				.billingAccount(null)
				.financialAccount(null)
				.paymentMethod(null);

		assertEquals(expectedCustomerBillVO, customerBillVOHttpResponse.body(), "The bill should have successfully been created.");
	}

	@ParameterizedTest
	@MethodSource("provideValidCustomerBillOnDemands")
	public void createCustomerBill400(String message, CustomerBillCreateVO invalidCreateVO) throws Exception {

		HttpResponse<CustomerBillVO> updateResponse = callAndCatch(
				() -> testClient.createCustomerBill(null, invalidCreateVO));
		assertEquals(HttpStatus.BAD_REQUEST, updateResponse.getStatus(), message);
	}

	private static Stream<Arguments> provideValidCustomerBillOnDemands() {
		return Stream.of(
				Arguments.of("Unreachable schemas are not allowed.", CustomerBillCreateVOTestExample.build()
						.atSchemaLocation(URI.create("my:uri"))
						.billingAccount(null)
						.financialAccount(null)
						.paymentMethod(null)),
				Arguments.of("Creation with invalid billing accounts are not allowed.", CustomerBillVOTestExample.build()
						.billingAccount(BillingAccountRefVOTestExample.build())
						.financialAccount(null)
						.paymentMethod(null)
						.atSchemaLocation(null)),
				Arguments.of("Creation with invalid financial accounts are not allowed.", CustomerBillVOTestExample.build()
						.billingAccount(null)
						.financialAccount(FinancialAccountRefVOTestExample.build())
						.paymentMethod(null)
						.atSchemaLocation(null)),
				Arguments.of("Creation with invalid payment methods are not allowed.", CustomerBillVOTestExample.build()
						.billingAccount(null)
						.financialAccount(null)
						.paymentMethod(PaymentMethodRefVOTestExample.build())
						.atSchemaLocation(null)),
				Arguments.of("Creation with additional properties without a schema is not allowed.", CustomerBillVOTestExample.build()
						.billingAccount(null)
						.financialAccount(null)
						.paymentMethod(PaymentMethodRefVOTestExample.build())
						.atSchemaLocation(null)
						.unknownProperties(Map.of("something", "unknown")))
		);
	}

	@Override
	public void createCustomerBill400() throws Exception {

	}

	@Disabled("Security is handled externally, thus 401 and 403 cannot happen.")
	@Test
	@Override
	public void createCustomerBill401() throws Exception {

	}

	@Disabled("Security is handled externally, thus 401 and 403 cannot happen.")
	@Test
	@Override
	public void createCustomerBill403() throws Exception {

	}

	@Disabled("Prohibited by the framework.")
	@Test
	@Override
	public void createCustomerBill405() throws Exception {

	}

	@Override
	public void createCustomerBill409() throws Exception {

	}

	@Override
	public void createCustomerBill500() throws Exception {

	}


	@Override
	protected String getEntityType() {
		return CustomerBill.TYPE_CUSTOMER_BILL;
	}
}
