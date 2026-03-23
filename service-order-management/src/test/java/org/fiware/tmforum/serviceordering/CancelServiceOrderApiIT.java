package org.fiware.tmforum.serviceordering;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import io.micronaut.test.annotation.MockBean;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import org.fiware.ngsi.api.EntitiesApiClient;
import org.fiware.serviceordering.api.CancelServiceOrderApiTestClient;
import org.fiware.serviceordering.api.CancelServiceOrderApiTestSpec;
import org.fiware.serviceordering.api.ServiceOrderApiTestClient;
import org.fiware.serviceordering.model.*;
import org.fiware.tmforum.common.configuration.GeneralProperties;
import org.fiware.tmforum.common.exception.ErrorDetails;
import org.fiware.tmforum.common.notification.TMForumEventHandler;
import org.fiware.tmforum.common.test.AbstractApiIT;
import org.fiware.tmforum.serviceordering.domain.CancelServiceOrder;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import reactor.core.publisher.Mono;

import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Integration tests for the CancelServiceOrder API endpoints.
 */
@MicronautTest(packages = { "org.fiware.tmforum.serviceordering" })
public class CancelServiceOrderApiIT extends AbstractApiIT implements CancelServiceOrderApiTestSpec {

	public final CancelServiceOrderApiTestClient cancelServiceOrderApiTestClient;
	public final ServiceOrderApiTestClient serviceOrderApiTestClient;

	private String message;
	private CancelServiceOrderCreateVO cancelServiceOrderCreateVO;
	private CancelServiceOrderVO expectedCancelServiceOrder;

	private Clock clock = mock(Clock.class);

	/**
	 * Provides a mock clock for testing.
	 *
	 * @return the mocked clock
	 */
	@MockBean(Clock.class)
	public Clock clock() {
		return clock;
	}

	/**
	 * Provides a mock event handler that returns empty monos.
	 *
	 * @return the mocked event handler
	 */
	@MockBean(TMForumEventHandler.class)
	public TMForumEventHandler eventHandler() {
		TMForumEventHandler eventHandler = mock(TMForumEventHandler.class);

		when(eventHandler.handleCreateEvent(any())).thenReturn(Mono.empty());
		when(eventHandler.handleUpdateEvent(any(), any())).thenReturn(Mono.empty());

		return eventHandler;
	}

	/**
	 * Creates the integration test with the required dependencies.
	 *
	 * @param cancelServiceOrderApiTestClient the test client for cancel service order API calls
	 * @param serviceOrderApiTestClient       the test client for service order API calls
	 * @param entitiesApiClient               the NGSI-LD entities API client
	 * @param objectMapper                    the JSON object mapper
	 * @param generalProperties               the general configuration properties
	 */
	public CancelServiceOrderApiIT(CancelServiceOrderApiTestClient cancelServiceOrderApiTestClient,
								   ServiceOrderApiTestClient serviceOrderApiTestClient,
								   EntitiesApiClient entitiesApiClient,
								   ObjectMapper objectMapper, GeneralProperties generalProperties) {
		super(entitiesApiClient, objectMapper, generalProperties);
		this.cancelServiceOrderApiTestClient = cancelServiceOrderApiTestClient;
		this.serviceOrderApiTestClient = serviceOrderApiTestClient;
	}

	@ParameterizedTest
	@MethodSource("provideValidCancelServiceOrders")
	public void createCancelServiceOrder201(String message, CancelServiceOrderCreateVO cancelServiceOrderCreateVO,
											CancelServiceOrderVO expectedCancelServiceOrder) throws Exception {
		this.message = message;
		this.cancelServiceOrderCreateVO = cancelServiceOrderCreateVO;
		this.expectedCancelServiceOrder = expectedCancelServiceOrder;
		createCancelServiceOrder201();
	}

	@Override
	public void createCancelServiceOrder201() throws Exception {
		// first create a service order to cancel
		ServiceOrderCreateVO serviceOrderCreateVO = ServiceOrderCreateVOTestExample.build()
				.atSchemaLocation(null);
		HttpResponse<ServiceOrderVO> createResponse = callAndCatch(
				() -> serviceOrderApiTestClient.createServiceOrder(null, serviceOrderCreateVO));
		assertEquals(HttpStatus.CREATED, createResponse.getStatus(), "A service order should have been created first.");
		String serviceOrderId = createResponse.body().getId();

		cancelServiceOrderCreateVO.getServiceOrder().setId(serviceOrderId);

		HttpResponse<CancelServiceOrderVO> cancelResponse = callAndCatch(
				() -> cancelServiceOrderApiTestClient.createCancelServiceOrder(null, cancelServiceOrderCreateVO));
		assertEquals(HttpStatus.CREATED, cancelResponse.getStatus(), message);

		String cancelId = cancelResponse.body().getId();
		expectedCancelServiceOrder.setId(cancelId);
		expectedCancelServiceOrder.setHref(java.net.URI.create(cancelId));
		expectedCancelServiceOrder.getServiceOrder().setId(serviceOrderId);
	}

	private static Stream<Arguments> provideValidCancelServiceOrders() {
		List<Arguments> testEntries = new ArrayList<>();

		ServiceOrderRefVO serviceOrderRef = new ServiceOrderRefVO()
				.id("urn:ngsi-ld:service-order:placeholder");

		testEntries.add(
				Arguments.of("A cancellation should have been created.",
						CancelServiceOrderCreateVOTestExample.build()
								.atSchemaLocation(null)
								.serviceOrder(serviceOrderRef),
						CancelServiceOrderVOTestExample.build()
								.atSchemaLocation(null)
								.serviceOrder(serviceOrderRef)));

		return testEntries.stream();
	}

	@ParameterizedTest
	@MethodSource("provideInvalidCancelServiceOrders")
	public void createCancelServiceOrder400(String message, CancelServiceOrderCreateVO invalidCreateVO)
			throws Exception {
		this.message = message;
		this.cancelServiceOrderCreateVO = invalidCreateVO;
		createCancelServiceOrder400();
	}

	@Override
	public void createCancelServiceOrder400() throws Exception {
		HttpResponse<CancelServiceOrderVO> creationResponse = callAndCatch(
				() -> cancelServiceOrderApiTestClient.createCancelServiceOrder(null, cancelServiceOrderCreateVO));
		assertEquals(HttpStatus.BAD_REQUEST, creationResponse.getStatus(), message);
	}

	private static Stream<Arguments> provideInvalidCancelServiceOrders() {
		List<Arguments> testEntries = new ArrayList<>();

		testEntries.add(
				Arguments.of("A cancel service order without a service order reference should not be created.",
						CancelServiceOrderCreateVOTestExample.build()
								.atSchemaLocation(null)
								.serviceOrder(null)));

		ServiceOrderRefVO nonExistentRef = new ServiceOrderRefVO()
				.id("urn:ngsi-ld:service-order:non-existent");
		testEntries.add(
				Arguments.of("A cancel service order with a non-existent service order should not be created.",
						CancelServiceOrderCreateVOTestExample.build()
								.atSchemaLocation(null)
								.serviceOrder(nonExistentRef)));

		return testEntries.stream();
	}

	@Disabled("Security is handled externally, thus 401 and 403 cannot happen.")
	@Override
	public void createCancelServiceOrder401() throws Exception {
	}

	@Disabled("Security is handled externally, thus 401 and 403 cannot happen.")
	@Override
	public void createCancelServiceOrder403() throws Exception {
	}

	@Disabled("Prohibited by the framework.")
	@Override
	public void createCancelServiceOrder405() throws Exception {
	}

	@Disabled("Impossible status.")
	@Override
	public void createCancelServiceOrder409() throws Exception {
	}

	@Override
	public void createCancelServiceOrder500() throws Exception {
	}

	@Override
	protected String getEntityType() {
		return CancelServiceOrder.TYPE_CANCEL_SERVICE_ORDER;
	}

	@Test
	@Override
	public void listCancelServiceOrder200() throws Exception {
		HttpResponse<List<CancelServiceOrderVO>> listResponse = callAndCatch(
				() -> cancelServiceOrderApiTestClient.listCancelServiceOrder(null, null, null, null));
		assertEquals(HttpStatus.OK, listResponse.getStatus(), "The list should be accessible.");
	}

	@Disabled("400 is impossible.")
	@Override
	public void listCancelServiceOrder400() throws Exception {
	}

	@Disabled("Security is handled externally, thus 401 and 403 cannot happen.")
	@Override
	public void listCancelServiceOrder401() throws Exception {
	}

	@Disabled("Security is handled externally, thus 401 and 403 cannot happen.")
	@Override
	public void listCancelServiceOrder403() throws Exception {
	}

	@Disabled("Not applicable.")
	@Override
	public void listCancelServiceOrder404() throws Exception {
	}

	@Disabled("Prohibited by the framework.")
	@Override
	public void listCancelServiceOrder405() throws Exception {
	}

	@Override
	public void listCancelServiceOrder409() throws Exception {
	}

	@Override
	public void listCancelServiceOrder500() throws Exception {
	}

	@Test
	@Override
	public void retrieveCancelServiceOrder200() throws Exception {
		// first create a service order, then cancel it, then retrieve the cancellation
		ServiceOrderCreateVO serviceOrderCreateVO = ServiceOrderCreateVOTestExample.build()
				.atSchemaLocation(null);
		HttpResponse<ServiceOrderVO> createResponse = callAndCatch(
				() -> serviceOrderApiTestClient.createServiceOrder(null, serviceOrderCreateVO));
		String serviceOrderId = createResponse.body().getId();

		ServiceOrderRefVO serviceOrderRef = new ServiceOrderRefVO().id(serviceOrderId);
		CancelServiceOrderCreateVO cancelCreateVO = CancelServiceOrderCreateVOTestExample.build()
				.atSchemaLocation(null)
				.serviceOrder(serviceOrderRef);
		HttpResponse<CancelServiceOrderVO> cancelResponse = callAndCatch(
				() -> cancelServiceOrderApiTestClient.createCancelServiceOrder(null, cancelCreateVO));
		assertEquals(HttpStatus.CREATED, cancelResponse.getStatus());
		String cancelId = cancelResponse.body().getId();

		HttpResponse<CancelServiceOrderVO> retrievedCancel = callAndCatch(
				() -> cancelServiceOrderApiTestClient.retrieveCancelServiceOrder(null, cancelId, null));
		assertEquals(HttpStatus.OK, retrievedCancel.getStatus(), "The cancellation should be retrievable.");
		assertEquals(cancelId, retrievedCancel.body().getId(), "The correct cancellation should be returned.");
	}

	@Disabled("400 is impossible.")
	@Override
	public void retrieveCancelServiceOrder400() throws Exception {
	}

	@Disabled("Security is handled externally, thus 401 and 403 cannot happen.")
	@Override
	public void retrieveCancelServiceOrder401() throws Exception {
	}

	@Disabled("Security is handled externally, thus 401 and 403 cannot happen.")
	@Override
	public void retrieveCancelServiceOrder403() throws Exception {
	}

	@Test
	@Override
	public void retrieveCancelServiceOrder404() throws Exception {
		HttpResponse<CancelServiceOrderVO> response = callAndCatch(
				() -> cancelServiceOrderApiTestClient.retrieveCancelServiceOrder(null,
						"urn:ngsi-ld:cancel-service-order:non-existing", null));
		assertEquals(HttpStatus.NOT_FOUND, response.getStatus(), "No such cancel service order should exist.");
	}

	@Disabled("Prohibited by the framework.")
	@Override
	public void retrieveCancelServiceOrder405() throws Exception {
	}

	@Override
	public void retrieveCancelServiceOrder409() throws Exception {
	}

	@Override
	public void retrieveCancelServiceOrder500() throws Exception {
	}
}
