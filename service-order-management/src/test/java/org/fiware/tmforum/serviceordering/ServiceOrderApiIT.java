package org.fiware.tmforum.serviceordering;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import io.micronaut.test.annotation.MockBean;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import org.fiware.ngsi.api.EntitiesApiClient;
import org.fiware.serviceordering.api.ServiceOrderApiTestClient;
import org.fiware.serviceordering.api.ServiceOrderApiTestSpec;
import org.fiware.serviceordering.model.*;
import org.fiware.tmforum.common.configuration.GeneralProperties;
import org.fiware.tmforum.common.exception.ErrorDetails;
import org.fiware.tmforum.common.notification.TMForumEventHandler;
import org.fiware.tmforum.common.test.AbstractApiIT;
import org.fiware.tmforum.serviceordering.domain.ServiceOrder;
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

/**
 * Integration tests for the ServiceOrder API endpoints.
 */
@MicronautTest(packages = { "org.fiware.tmforum.serviceordering" })
public class ServiceOrderApiIT extends AbstractApiIT implements ServiceOrderApiTestSpec {

	public final ServiceOrderApiTestClient serviceOrderApiTestClient;

	private String message;
	private String fieldsParameter;
	private ServiceOrderCreateVO serviceOrderCreateVO;
	private ServiceOrderUpdateVO serviceOrderUpdateVO;
	private ServiceOrderVO expectedServiceOrder;

	private Clock clock = mock(Clock.class);

	/**
	 * Provides a mock clock for testing order date assignment.
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
	 * @param serviceOrderApiTestClient the test client for service order API calls
	 * @param entitiesApiClient         the NGSI-LD entities API client
	 * @param objectMapper              the JSON object mapper
	 * @param generalProperties         the general configuration properties
	 */
	public ServiceOrderApiIT(ServiceOrderApiTestClient serviceOrderApiTestClient,
							 EntitiesApiClient entitiesApiClient,
							 ObjectMapper objectMapper, GeneralProperties generalProperties) {
		super(entitiesApiClient, objectMapper, generalProperties);
		this.serviceOrderApiTestClient = serviceOrderApiTestClient;
	}

	@ParameterizedTest
	@MethodSource("provideValidServiceOrders")
	public void createServiceOrder201(String message, ServiceOrderCreateVO serviceOrderCreateVO,
									  ServiceOrderVO expectedServiceOrder)
			throws Exception {
		this.message = message;
		this.serviceOrderCreateVO = serviceOrderCreateVO;
		this.expectedServiceOrder = expectedServiceOrder;
		createServiceOrder201();
	}

	@Override
	public void createServiceOrder201() throws Exception {

		Instant now = Instant.now();
		when(clock.instant()).thenReturn(now);

		HttpResponse<ServiceOrderVO> serviceOrderVOHttpResponse = callAndCatch(
				() -> serviceOrderApiTestClient.createServiceOrder(null, serviceOrderCreateVO));
		assertEquals(HttpStatus.CREATED, serviceOrderVOHttpResponse.getStatus(), message);
		String rfId = serviceOrderVOHttpResponse.body().getId();
		expectedServiceOrder.setId(rfId);
		expectedServiceOrder.setHref(rfId);
		expectedServiceOrder.setOrderDate(now);
		assertEquals(expectedServiceOrder, serviceOrderVOHttpResponse.body(), message);
	}

	private static Stream<Arguments> provideValidServiceOrders() {
		List<Arguments> testEntries = new ArrayList<>();

		testEntries.add(
				Arguments.of("An empty service order should have been created.",
						ServiceOrderCreateVOTestExample.build().atSchemaLocation(null),
						ServiceOrderVOTestExample.build().atSchemaLocation(null)));

		testEntries.add(
				Arguments.of("A service order with a category should have been created.",
						ServiceOrderCreateVOTestExample.build().atSchemaLocation(null)
								.category("Premium"),
						ServiceOrderVOTestExample.build().atSchemaLocation(null)
								.category("Premium")));

		testEntries.add(
				Arguments.of("A service order with a description should have been created.",
						ServiceOrderCreateVOTestExample.build().atSchemaLocation(null)
								.description("Test order"),
						ServiceOrderVOTestExample.build().atSchemaLocation(null)
								.description("Test order")));

		testEntries.add(
				Arguments.of("A service order with a name should have been created.",
						ServiceOrderCreateVOTestExample.build().atSchemaLocation(null)
								.description("My Order"),
						ServiceOrderVOTestExample.build().atSchemaLocation(null)
								.description("My Order")));

		return testEntries.stream();
	}

	@ParameterizedTest
	@MethodSource("provideInvalidServiceOrders")
	public void createServiceOrder400(String message, ServiceOrderCreateVO invalidCreateVO) throws Exception {
		this.message = message;
		this.serviceOrderCreateVO = invalidCreateVO;
		createServiceOrder400();
	}

	@Override
	public void createServiceOrder400() throws Exception {
		HttpResponse<ServiceOrderVO> creationResponse = callAndCatch(
				() -> serviceOrderApiTestClient.createServiceOrder(null, serviceOrderCreateVO));
		assertEquals(HttpStatus.BAD_REQUEST, creationResponse.getStatus(), message);
		Optional<ErrorDetails> optionalErrorDetails = creationResponse.getBody(ErrorDetails.class);
		assertTrue(optionalErrorDetails.isPresent(), "Error details should be provided.");
	}

	private static Stream<Arguments> provideInvalidServiceOrders() {
		List<Arguments> testEntries = new ArrayList<>();

		testEntries.add(Arguments.of("A service order with an invalid related party should not be created.",
				ServiceOrderCreateVOTestExample.build().atSchemaLocation(null)
						.relatedParty(List.of(RelatedPartyVOTestExample.build()))));

		return testEntries.stream();
	}

	@Disabled("Security is handled externally, thus 401 and 403 cannot happen.")
	@Override
	public void createServiceOrder401() throws Exception {
	}

	@Disabled("Security is handled externally, thus 401 and 403 cannot happen.")
	@Override
	public void createServiceOrder403() throws Exception {
	}

	@Disabled("Prohibited by the framework.")
	@Override
	public void createServiceOrder405() throws Exception {
	}

	@Disabled("Impossible status.")
	@Override
	public void createServiceOrder409() throws Exception {
	}

	@Override
	public void createServiceOrder500() throws Exception {
	}

	@Override
	protected String getEntityType() {
		return ServiceOrder.TYPE_SERVICE_ORDER;
	}

	@Test
	@Override
	public void deleteServiceOrder204() throws Exception {
		ServiceOrderCreateVO serviceOrderCreateVO = ServiceOrderCreateVOTestExample.build()
				.atSchemaLocation(null);

		HttpResponse<ServiceOrderVO> createResponse = callAndCatch(
				() -> serviceOrderApiTestClient.createServiceOrder(null, serviceOrderCreateVO));
		assertEquals(HttpStatus.CREATED, createResponse.getStatus(), "The service order should have been created first.");

		String serviceOrderId = createResponse.body().getId();

		assertEquals(HttpStatus.NO_CONTENT,
				callAndCatch(() -> serviceOrderApiTestClient.deleteServiceOrder(null, serviceOrderId)).getStatus(),
				"The service order should have been deleted.");

		assertEquals(HttpStatus.NOT_FOUND,
				callAndCatch(() -> serviceOrderApiTestClient.retrieveServiceOrder(null, serviceOrderId, null)).getStatus(),
				"The service order should not exist anymore.");
	}

	@Disabled("400 is impossible.")
	@Override
	public void deleteServiceOrder400() throws Exception {
	}

	@Disabled("Security is handled externally, thus 401 and 403 cannot happen.")
	@Override
	public void deleteServiceOrder401() throws Exception {
	}

	@Disabled("Security is handled externally, thus 401 and 403 cannot happen.")
	@Override
	public void deleteServiceOrder403() throws Exception {
	}

	@Test
	@Override
	public void deleteServiceOrder404() throws Exception {
		HttpResponse<?> notFoundResponse = callAndCatch(
				() -> serviceOrderApiTestClient.deleteServiceOrder(null,
						"urn:ngsi-ld:service-order:no-such-order"));
		assertEquals(HttpStatus.NOT_FOUND, notFoundResponse.getStatus(),
				"No such service-order should exist.");
	}

	@Disabled("Prohibited by the framework.")
	@Override
	public void deleteServiceOrder405() throws Exception {
	}

	@Override
	public void deleteServiceOrder409() throws Exception {
	}

	@Override
	public void deleteServiceOrder500() throws Exception {
	}

	@Test
	@Override
	public void listServiceOrder200() throws Exception {
		List<ServiceOrderVO> expectedServiceOrders = new ArrayList<>();
		for (int i = 0; i < 10; i++) {
			ServiceOrderCreateVO serviceOrderCreateVO = ServiceOrderCreateVOTestExample.build()
					.atSchemaLocation(null);
			String id = callAndCatch(
					() -> serviceOrderApiTestClient.createServiceOrder(null, serviceOrderCreateVO))
					.body().getId();
			expectedServiceOrders.add(ServiceOrderVOTestExample.build()
					.atSchemaLocation(null)
					.id(id).href(id));
		}

		HttpResponse<List<ServiceOrderVO>> serviceOrderResponse = callAndCatch(
				() -> serviceOrderApiTestClient.listServiceOrder(null, null, null, null));
		assertEquals(HttpStatus.OK, serviceOrderResponse.getStatus(), "The list should be accessible.");
		assertEquals(expectedServiceOrders.size(), serviceOrderResponse.getBody().get().size(),
				"All service orders should have been returned.");
	}

	@Disabled("400 is impossible.")
	@Override
	public void listServiceOrder400() throws Exception {
	}

	@Disabled("Security is handled externally, thus 401 and 403 cannot happen.")
	@Override
	public void listServiceOrder401() throws Exception {
	}

	@Disabled("Security is handled externally, thus 401 and 403 cannot happen.")
	@Override
	public void listServiceOrder403() throws Exception {
	}

	@Disabled("Not applicable.")
	@Override
	public void listServiceOrder404() throws Exception {
	}

	@Disabled("Prohibited by the framework.")
	@Override
	public void listServiceOrder405() throws Exception {
	}

	@Override
	public void listServiceOrder409() throws Exception {
	}

	@Override
	public void listServiceOrder500() throws Exception {
	}

	@Test
	@Override
	public void retrieveServiceOrder200() throws Exception {
		ServiceOrderCreateVO serviceOrderCreateVO = ServiceOrderCreateVOTestExample.build()
				.atSchemaLocation(null);

		HttpResponse<ServiceOrderVO> createResponse = callAndCatch(
				() -> serviceOrderApiTestClient.createServiceOrder(null, serviceOrderCreateVO));
		assertEquals(HttpStatus.CREATED, createResponse.getStatus(), "The service order should have been created first.");
		String id = createResponse.body().getId();

		ServiceOrderVO expectedServiceOrder = ServiceOrderVOTestExample.build()
				.atSchemaLocation(null)
				.id(id).href(id);

		HttpResponse<ServiceOrderVO> retrievedSO = callAndCatch(
				() -> serviceOrderApiTestClient.retrieveServiceOrder(null, id, null));
		assertEquals(HttpStatus.OK, retrievedSO.getStatus(), "The retrieval should be successful.");
		assertEquals(expectedServiceOrder.getId(), retrievedSO.body().getId(), "The correct service order should be returned.");
	}

	@Disabled("400 is impossible.")
	@Override
	public void retrieveServiceOrder400() throws Exception {
	}

	@Disabled("Security is handled externally, thus 401 and 403 cannot happen.")
	@Override
	public void retrieveServiceOrder401() throws Exception {
	}

	@Disabled("Security is handled externally, thus 401 and 403 cannot happen.")
	@Override
	public void retrieveServiceOrder403() throws Exception {
	}

	@Test
	@Override
	public void retrieveServiceOrder404() throws Exception {
		HttpResponse<ServiceOrderVO> response = callAndCatch(
				() -> serviceOrderApiTestClient.retrieveServiceOrder(null,
						"urn:ngsi-ld:service-order:non-existing", null));
		assertEquals(HttpStatus.NOT_FOUND, response.getStatus(), "No such service order should exist.");
	}

	@Disabled("Prohibited by the framework.")
	@Override
	public void retrieveServiceOrder405() throws Exception {
	}

	@Override
	public void retrieveServiceOrder409() throws Exception {
	}

	@Override
	public void retrieveServiceOrder500() throws Exception {
	}

	@ParameterizedTest
	@MethodSource("provideServiceOrderUpdates")
	public void patchServiceOrder200(String message, ServiceOrderUpdateVO serviceOrderUpdateVO,
									 ServiceOrderVO expectedServiceOrder) throws Exception {
		this.message = message;
		this.serviceOrderUpdateVO = serviceOrderUpdateVO;
		this.expectedServiceOrder = expectedServiceOrder;
		patchServiceOrder200();
	}

	@Override
	public void patchServiceOrder200() throws Exception {
		Instant now = Instant.now();
		when(clock.instant()).thenReturn(now);

		ServiceOrderCreateVO serviceOrderCreateVO = ServiceOrderCreateVOTestExample.build()
				.atSchemaLocation(null);

		HttpResponse<ServiceOrderVO> createResponse = callAndCatch(
				() -> serviceOrderApiTestClient.createServiceOrder(null, serviceOrderCreateVO));
		assertEquals(HttpStatus.CREATED, createResponse.getStatus(), message);
		String serviceOrderId = createResponse.body().getId();

		HttpResponse<ServiceOrderVO> updateResponse = callAndCatch(
				() -> serviceOrderApiTestClient.patchServiceOrder(null, serviceOrderId, serviceOrderUpdateVO));
		assertEquals(HttpStatus.OK, updateResponse.getStatus(), message);

		ServiceOrderVO updatedServiceOrder = updateResponse.body();
		expectedServiceOrder.setId(serviceOrderId);
		expectedServiceOrder.setHref(serviceOrderId);
		expectedServiceOrder.setOrderDate(now);

		assertEquals(expectedServiceOrder.getCategory(), updatedServiceOrder.getCategory(), message);
	}

	private static Stream<Arguments> provideServiceOrderUpdates() {
		List<Arguments> testEntries = new ArrayList<>();

		testEntries.add(Arguments.of("The description should be updated.",
				ServiceOrderUpdateVOTestExample.build()
						.description("Updated"),
				ServiceOrderVOTestExample.build().atSchemaLocation(null)
						.description("Updated")));

		testEntries.add(Arguments.of("The description should be updated.",
				ServiceOrderUpdateVOTestExample.build()
						.description("Updated description"),
				ServiceOrderVOTestExample.build().atSchemaLocation(null)
						.description("Updated description")));

		return testEntries.stream();
	}

	@ParameterizedTest
	@MethodSource("provideInvalidServiceOrderUpdates")
	public void patchServiceOrder400(String message, ServiceOrderUpdateVO invalidUpdateVO) throws Exception {
		this.message = message;
		this.serviceOrderUpdateVO = invalidUpdateVO;
		patchServiceOrder400();
	}

	@Override
	public void patchServiceOrder400() throws Exception {
		ServiceOrderCreateVO serviceOrderCreateVO = ServiceOrderCreateVOTestExample.build()
				.atSchemaLocation(null);

		HttpResponse<ServiceOrderVO> createResponse = callAndCatch(
				() -> serviceOrderApiTestClient.createServiceOrder(null, serviceOrderCreateVO));
		assertEquals(HttpStatus.CREATED, createResponse.getStatus(), "The service order should have been created first.");
		String serviceOrderId = createResponse.body().getId();

		HttpResponse<ServiceOrderVO> updateResponse = callAndCatch(
				() -> serviceOrderApiTestClient.patchServiceOrder(null, serviceOrderId, serviceOrderUpdateVO));
		assertEquals(HttpStatus.BAD_REQUEST, updateResponse.getStatus(), message);
	}

	private static Stream<Arguments> provideInvalidServiceOrderUpdates() {
		List<Arguments> testEntries = new ArrayList<>();

		testEntries.add(Arguments.of("An update with an invalid related party should not be applied.",
				ServiceOrderUpdateVOTestExample.build()
						.relatedParty(List.of(RelatedPartyVOTestExample.build()))));

		return testEntries.stream();
	}

	@Disabled("Security is handled externally, thus 401 and 403 cannot happen.")
	@Override
	public void patchServiceOrder401() throws Exception {
	}

	@Disabled("Security is handled externally, thus 401 and 403 cannot happen.")
	@Override
	public void patchServiceOrder403() throws Exception {
	}

	@Test
	@Override
	public void patchServiceOrder404() throws Exception {
		ServiceOrderUpdateVO serviceOrderUpdateVO = ServiceOrderUpdateVOTestExample.build();
		assertEquals(HttpStatus.NOT_FOUND,
				callAndCatch(() -> serviceOrderApiTestClient.patchServiceOrder(null,
						"urn:ngsi-ld:service-order:not-existent", serviceOrderUpdateVO)).getStatus(),
				"Non-existent service orders should not be updated.");
	}

	@Disabled("Prohibited by the framework.")
	@Override
	public void patchServiceOrder405() throws Exception {
	}

	@Override
	public void patchServiceOrder409() throws Exception {
	}

	@Override
	public void patchServiceOrder500() throws Exception {
	}
}
