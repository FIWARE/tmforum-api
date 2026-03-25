package org.fiware.tmforum.resourceordering;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import io.micronaut.test.annotation.MockBean;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import org.fiware.ngsi.api.EntitiesApiClient;
import org.fiware.resourceordering.api.ResourceOrderApiTestClient;
import org.fiware.resourceordering.api.ResourceOrderApiTestSpec;
import org.fiware.resourceordering.model.*;
import org.fiware.tmforum.common.configuration.GeneralProperties;
import org.fiware.tmforum.common.exception.ErrorDetails;
import org.fiware.tmforum.common.notification.TMForumEventHandler;
import org.fiware.tmforum.common.test.AbstractApiIT;
import org.fiware.tmforum.resourceordering.domain.ResourceOrder;
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

@MicronautTest(packages = { "org.fiware.tmforum.resourceordering" })
public class ResourceOrderApiIT extends AbstractApiIT implements ResourceOrderApiTestSpec {

	public final ResourceOrderApiTestClient resourceOrderApiTestClient;

	private String message;
	private String fieldsParameter;
	private ResourceOrderCreateVO resourceOrderCreateVO;
	private ResourceOrderUpdateVO resourceOrderUpdateVO;
	private ResourceOrderVO expectedResourceOrder;

	private Clock clock = mock(Clock.class);

	@MockBean(Clock.class)
	public Clock clock() {
		return clock;
	}

	@MockBean(TMForumEventHandler.class)
	public TMForumEventHandler eventHandler() {
		TMForumEventHandler eventHandler = mock(TMForumEventHandler.class);

		when(eventHandler.handleCreateEvent(any())).thenReturn(Mono.empty());
		when(eventHandler.handleUpdateEvent(any(), any())).thenReturn(Mono.empty());

		return eventHandler;
	}

	public ResourceOrderApiIT(ResourceOrderApiTestClient resourceOrderApiTestClient,
							  EntitiesApiClient entitiesApiClient,
							  ObjectMapper objectMapper, GeneralProperties generalProperties) {
		super(entitiesApiClient, objectMapper, generalProperties);
		this.resourceOrderApiTestClient = resourceOrderApiTestClient;
	}

	@ParameterizedTest
	@MethodSource("provideValidResourceOrders")
	public void createResourceOrder201(String message, ResourceOrderCreateVO resourceOrderCreateVO,
									   ResourceOrderVO expectedResourceOrder)
			throws Exception {
		this.message = message;
		this.resourceOrderCreateVO = resourceOrderCreateVO;
		this.expectedResourceOrder = expectedResourceOrder;
		createResourceOrder201();
	}

	@Override
	public void createResourceOrder201() throws Exception {

		Instant now = Instant.now();
		when(clock.instant()).thenReturn(now);

		HttpResponse<ResourceOrderVO> resourceOrderVOHttpResponse = callAndCatch(
				() -> resourceOrderApiTestClient.createResourceOrder(null, resourceOrderCreateVO));
		assertEquals(HttpStatus.CREATED, resourceOrderVOHttpResponse.getStatus(), message);
		String rfId = resourceOrderVOHttpResponse.body().getId();
		expectedResourceOrder.setId(rfId);
		expectedResourceOrder.setHref(rfId);
		expectedResourceOrder.setOrderDate(now);
		assertEquals(expectedResourceOrder, resourceOrderVOHttpResponse.body(), message);
	}

	private static Stream<Arguments> provideValidResourceOrders() {
		List<Arguments> testEntries = new ArrayList<>();

		testEntries.add(
				Arguments.of("An empty resource order should have been created.",
						ResourceOrderCreateVOTestExample.build().atSchemaLocation(null),
						ResourceOrderVOTestExample.build().atSchemaLocation(null)));

		testEntries.add(
				Arguments.of("A resource order with a category should have been created.",
						ResourceOrderCreateVOTestExample.build().atSchemaLocation(null)
								.category("Premium"),
						ResourceOrderVOTestExample.build().atSchemaLocation(null)
								.category("Premium")));

		testEntries.add(
				Arguments.of("A resource order with a description should have been created.",
						ResourceOrderCreateVOTestExample.build().atSchemaLocation(null)
								.description("Test order"),
						ResourceOrderVOTestExample.build().atSchemaLocation(null)
								.description("Test order")));

		testEntries.add(
				Arguments.of("A resource order with a name should have been created.",
						ResourceOrderCreateVOTestExample.build().atSchemaLocation(null)
								.name("My Order"),
						ResourceOrderVOTestExample.build().atSchemaLocation(null)
								.name("My Order")));

		testEntries.add(
				Arguments.of("A resource order with priority should have been created.",
						ResourceOrderCreateVOTestExample.build().atSchemaLocation(null)
								.priority(1),
						ResourceOrderVOTestExample.build().atSchemaLocation(null)
								.priority(1)));

		return testEntries.stream();
	}

	@ParameterizedTest
	@MethodSource("provideInvalidResourceOrders")
	public void createResourceOrder400(String message, ResourceOrderCreateVO invalidCreateVO) throws Exception {
		this.message = message;
		this.resourceOrderCreateVO = invalidCreateVO;
		createResourceOrder400();
	}

	@Override
	public void createResourceOrder400() throws Exception {
		HttpResponse<ResourceOrderVO> creationResponse = callAndCatch(
				() -> resourceOrderApiTestClient.createResourceOrder(null, resourceOrderCreateVO));
		assertEquals(HttpStatus.BAD_REQUEST, creationResponse.getStatus(), message);
		Optional<ErrorDetails> optionalErrorDetails = creationResponse.getBody(ErrorDetails.class);
		assertTrue(optionalErrorDetails.isPresent(), "Error details should be provided.");
	}

	private static Stream<Arguments> provideInvalidResourceOrders() {
		List<Arguments> testEntries = new ArrayList<>();

		testEntries.add(Arguments.of("A resource order with an invalid related party should not be created.",
				ResourceOrderCreateVOTestExample.build().atSchemaLocation(null)
						.relatedParty(List.of(RelatedPartyVOTestExample.build()))));

		return testEntries.stream();
	}

	@Disabled("Security is handled externally, thus 401 and 403 cannot happen.")
	@Override
	public void createResourceOrder401() throws Exception {
	}

	@Disabled("Security is handled externally, thus 401 and 403 cannot happen.")
	@Override
	public void createResourceOrder403() throws Exception {
	}

	@Disabled("Prohibited by the framework.")
	@Override
	public void createResourceOrder405() throws Exception {
	}

	@Disabled("Impossible status.")
	@Override
	public void createResourceOrder409() throws Exception {
	}

	@Override
	public void createResourceOrder500() throws Exception {
	}

	@Override
	protected String getEntityType() {
		return ResourceOrder.TYPE_RESOURCE_ORDER;
	}

	@Test
	@Override
	public void deleteResourceOrder204() throws Exception {
		ResourceOrderCreateVO resourceOrderCreateVO = ResourceOrderCreateVOTestExample.build()
				.atSchemaLocation(null);

		HttpResponse<ResourceOrderVO> createResponse = callAndCatch(
				() -> resourceOrderApiTestClient.createResourceOrder(null, resourceOrderCreateVO));
		assertEquals(HttpStatus.CREATED, createResponse.getStatus(), "The resource order should have been created first.");

		String resourceOrderId = createResponse.body().getId();

		assertEquals(HttpStatus.NO_CONTENT,
				callAndCatch(() -> resourceOrderApiTestClient.deleteResourceOrder(null, resourceOrderId)).getStatus(),
				"The resource order should have been deleted.");

		assertEquals(HttpStatus.NOT_FOUND,
				callAndCatch(() -> resourceOrderApiTestClient.retrieveResourceOrder(null, resourceOrderId, null)).getStatus(),
				"The resource order should not exist anymore.");
	}

	@Disabled("400 is impossible.")
	@Override
	public void deleteResourceOrder400() throws Exception {
	}

	@Disabled("Security is handled externally, thus 401 and 403 cannot happen.")
	@Override
	public void deleteResourceOrder401() throws Exception {
	}

	@Disabled("Security is handled externally, thus 401 and 403 cannot happen.")
	@Override
	public void deleteResourceOrder403() throws Exception {
	}

	@Test
	@Override
	public void deleteResourceOrder404() throws Exception {
		HttpResponse<?> notFoundResponse = callAndCatch(
				() -> resourceOrderApiTestClient.deleteResourceOrder(null,
						"urn:ngsi-ld:resource-order:no-such-order"));
		assertEquals(HttpStatus.NOT_FOUND, notFoundResponse.getStatus(),
				"No such resource-order should exist.");
	}

	@Disabled("Prohibited by the framework.")
	@Override
	public void deleteResourceOrder405() throws Exception {
	}

	@Override
	public void deleteResourceOrder409() throws Exception {
	}

	@Override
	public void deleteResourceOrder500() throws Exception {
	}

	@Test
	@Override
	public void listResourceOrder200() throws Exception {
		List<ResourceOrderVO> expectedResourceOrders = new ArrayList<>();
		for (int i = 0; i < 10; i++) {
			ResourceOrderCreateVO resourceOrderCreateVO = ResourceOrderCreateVOTestExample.build()
					.atSchemaLocation(null);
			String id = callAndCatch(
					() -> resourceOrderApiTestClient.createResourceOrder(null, resourceOrderCreateVO))
					.body().getId();
			expectedResourceOrders.add(ResourceOrderVOTestExample.build()
					.atSchemaLocation(null)
					.id(id).href(id));
		}

		HttpResponse<List<ResourceOrderVO>> resourceOrderResponse = callAndCatch(
				() -> resourceOrderApiTestClient.listResourceOrder(null, null, null, null));
		assertEquals(HttpStatus.OK, resourceOrderResponse.getStatus(), "The list should be accessible.");
		assertEquals(expectedResourceOrders.size(), resourceOrderResponse.getBody().get().size(),
				"All resource orders should have been returned.");
	}

	@Disabled("400 is impossible.")
	@Override
	public void listResourceOrder400() throws Exception {
	}

	@Disabled("Security is handled externally, thus 401 and 403 cannot happen.")
	@Override
	public void listResourceOrder401() throws Exception {
	}

	@Disabled("Security is handled externally, thus 401 and 403 cannot happen.")
	@Override
	public void listResourceOrder403() throws Exception {
	}

	@Disabled("Not applicable.")
	@Override
	public void listResourceOrder404() throws Exception {
	}

	@Disabled("Prohibited by the framework.")
	@Override
	public void listResourceOrder405() throws Exception {
	}

	@Override
	public void listResourceOrder409() throws Exception {
	}

	@Override
	public void listResourceOrder500() throws Exception {
	}

	@Test
	@Override
	public void retrieveResourceOrder200() throws Exception {
		ResourceOrderCreateVO resourceOrderCreateVO = ResourceOrderCreateVOTestExample.build()
				.atSchemaLocation(null);

		HttpResponse<ResourceOrderVO> createResponse = callAndCatch(
				() -> resourceOrderApiTestClient.createResourceOrder(null, resourceOrderCreateVO));
		assertEquals(HttpStatus.CREATED, createResponse.getStatus(), "The resource order should have been created first.");
		String id = createResponse.body().getId();

		ResourceOrderVO expectedResourceOrder = ResourceOrderVOTestExample.build()
				.atSchemaLocation(null)
				.id(id).href(id);

		HttpResponse<ResourceOrderVO> retrievedRO = callAndCatch(
				() -> resourceOrderApiTestClient.retrieveResourceOrder(null, id, null));
		assertEquals(HttpStatus.OK, retrievedRO.getStatus(), "The retrieval should be successful.");
		assertEquals(expectedResourceOrder.getId(), retrievedRO.body().getId(), "The correct resource order should be returned.");
	}

	@Disabled("400 is impossible.")
	@Override
	public void retrieveResourceOrder400() throws Exception {
	}

	@Disabled("Security is handled externally, thus 401 and 403 cannot happen.")
	@Override
	public void retrieveResourceOrder401() throws Exception {
	}

	@Disabled("Security is handled externally, thus 401 and 403 cannot happen.")
	@Override
	public void retrieveResourceOrder403() throws Exception {
	}

	@Test
	@Override
	public void retrieveResourceOrder404() throws Exception {
		HttpResponse<ResourceOrderVO> response = callAndCatch(
				() -> resourceOrderApiTestClient.retrieveResourceOrder(null,
						"urn:ngsi-ld:resource-order:non-existing", null));
		assertEquals(HttpStatus.NOT_FOUND, response.getStatus(), "No such resource order should exist.");
	}

	@Disabled("Prohibited by the framework.")
	@Override
	public void retrieveResourceOrder405() throws Exception {
	}

	@Override
	public void retrieveResourceOrder409() throws Exception {
	}

	@Override
	public void retrieveResourceOrder500() throws Exception {
	}

	@ParameterizedTest
	@MethodSource("provideResourceOrderUpdates")
	public void patchResourceOrder200(String message, ResourceOrderUpdateVO resourceOrderUpdateVO,
									  ResourceOrderVO expectedResourceOrder) throws Exception {
		this.message = message;
		this.resourceOrderUpdateVO = resourceOrderUpdateVO;
		this.expectedResourceOrder = expectedResourceOrder;
		patchResourceOrder200();
	}

	@Override
	public void patchResourceOrder200() throws Exception {
		Instant now = Instant.now();
		when(clock.instant()).thenReturn(now);

		ResourceOrderCreateVO resourceOrderCreateVO = ResourceOrderCreateVOTestExample.build()
				.atSchemaLocation(null);

		HttpResponse<ResourceOrderVO> createResponse = callAndCatch(
				() -> resourceOrderApiTestClient.createResourceOrder(null, resourceOrderCreateVO));
		assertEquals(HttpStatus.CREATED, createResponse.getStatus(), message);
		String resourceOrderId = createResponse.body().getId();

		HttpResponse<ResourceOrderVO> updateResponse = callAndCatch(
				() -> resourceOrderApiTestClient.patchResourceOrder(null, resourceOrderId, resourceOrderUpdateVO));
		assertEquals(HttpStatus.OK, updateResponse.getStatus(), message);

		ResourceOrderVO updatedResourceOrder = updateResponse.body();
		expectedResourceOrder.setId(resourceOrderId);
		expectedResourceOrder.setHref(resourceOrderId);
		expectedResourceOrder.setOrderDate(now);

		assertEquals(expectedResourceOrder.getCategory(), updatedResourceOrder.getCategory(), message);
	}

	private static Stream<Arguments> provideResourceOrderUpdates() {
		List<Arguments> testEntries = new ArrayList<>();

		testEntries.add(Arguments.of("The category should be updated.",
				ResourceOrderUpdateVOTestExample.build().atSchemaLocation(null)
						.category("Updated"),
				ResourceOrderVOTestExample.build().atSchemaLocation(null)
						.category("Updated")));

		testEntries.add(Arguments.of("The description should be updated.",
				ResourceOrderUpdateVOTestExample.build().atSchemaLocation(null)
						.description("Updated description"),
				ResourceOrderVOTestExample.build().atSchemaLocation(null)
						.description("Updated description")));

		return testEntries.stream();
	}

	@ParameterizedTest
	@MethodSource("provideInvalidResourceOrderUpdates")
	public void patchResourceOrder400(String message, ResourceOrderUpdateVO invalidUpdateVO) throws Exception {
		this.message = message;
		this.resourceOrderUpdateVO = invalidUpdateVO;
		patchResourceOrder400();
	}

	@Override
	public void patchResourceOrder400() throws Exception {
		ResourceOrderCreateVO resourceOrderCreateVO = ResourceOrderCreateVOTestExample.build()
				.atSchemaLocation(null);

		HttpResponse<ResourceOrderVO> createResponse = callAndCatch(
				() -> resourceOrderApiTestClient.createResourceOrder(null, resourceOrderCreateVO));
		assertEquals(HttpStatus.CREATED, createResponse.getStatus(), "The resource order should have been created first.");
		String resourceOrderId = createResponse.body().getId();

		HttpResponse<ResourceOrderVO> updateResponse = callAndCatch(
				() -> resourceOrderApiTestClient.patchResourceOrder(null, resourceOrderId, resourceOrderUpdateVO));
		assertEquals(HttpStatus.BAD_REQUEST, updateResponse.getStatus(), message);
	}

	private static Stream<Arguments> provideInvalidResourceOrderUpdates() {
		List<Arguments> testEntries = new ArrayList<>();

		testEntries.add(Arguments.of("An update with an invalid related party should not be applied.",
				ResourceOrderUpdateVOTestExample.build().atSchemaLocation(null)
						.relatedParty(List.of(RelatedPartyVOTestExample.build()))));

		return testEntries.stream();
	}

	@Disabled("Security is handled externally, thus 401 and 403 cannot happen.")
	@Override
	public void patchResourceOrder401() throws Exception {
	}

	@Disabled("Security is handled externally, thus 401 and 403 cannot happen.")
	@Override
	public void patchResourceOrder403() throws Exception {
	}

	@Test
	@Override
	public void patchResourceOrder404() throws Exception {
		ResourceOrderUpdateVO resourceOrderUpdateVO = ResourceOrderUpdateVOTestExample.build()
				.atSchemaLocation(null);
		assertEquals(HttpStatus.NOT_FOUND,
				callAndCatch(() -> resourceOrderApiTestClient.patchResourceOrder(null,
						"urn:ngsi-ld:resource-order:not-existent", resourceOrderUpdateVO)).getStatus(),
				"Non-existent resource orders should not be updated.");
	}

	@Disabled("Prohibited by the framework.")
	@Override
	public void patchResourceOrder405() throws Exception {
	}

	@Override
	public void patchResourceOrder409() throws Exception {
	}

	@Override
	public void patchResourceOrder500() throws Exception {
	}
}
