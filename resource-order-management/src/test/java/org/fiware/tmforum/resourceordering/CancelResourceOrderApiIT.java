package org.fiware.tmforum.resourceordering;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import io.micronaut.test.annotation.MockBean;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import org.fiware.ngsi.api.EntitiesApiClient;
import org.fiware.resourceordering.api.CancelResourceOrderApiTestClient;
import org.fiware.resourceordering.api.CancelResourceOrderApiTestSpec;
import org.fiware.resourceordering.api.ResourceOrderApiTestClient;
import org.fiware.resourceordering.model.*;
import org.fiware.tmforum.common.configuration.GeneralProperties;
import org.fiware.tmforum.common.exception.ErrorDetails;
import org.fiware.tmforum.common.notification.TMForumEventHandler;
import org.fiware.tmforum.common.test.AbstractApiIT;
import org.fiware.tmforum.resourceordering.domain.CancelResourceOrder;
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

@MicronautTest(packages = { "org.fiware.tmforum.resourceordering" })
public class CancelResourceOrderApiIT extends AbstractApiIT implements CancelResourceOrderApiTestSpec {

	public final CancelResourceOrderApiTestClient cancelResourceOrderApiTestClient;
	public final ResourceOrderApiTestClient resourceOrderApiTestClient;

	private String message;
	private CancelResourceOrderCreateVO cancelResourceOrderCreateVO;
	private CancelResourceOrderVO expectedCancelResourceOrder;

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

	public CancelResourceOrderApiIT(CancelResourceOrderApiTestClient cancelResourceOrderApiTestClient,
									ResourceOrderApiTestClient resourceOrderApiTestClient,
									EntitiesApiClient entitiesApiClient,
									ObjectMapper objectMapper, GeneralProperties generalProperties) {
		super(entitiesApiClient, objectMapper, generalProperties);
		this.cancelResourceOrderApiTestClient = cancelResourceOrderApiTestClient;
		this.resourceOrderApiTestClient = resourceOrderApiTestClient;
	}

	@ParameterizedTest
	@MethodSource("provideValidCancelResourceOrders")
	public void createCancelResourceOrder201(String message, CancelResourceOrderCreateVO cancelResourceOrderCreateVO,
											 CancelResourceOrderVO expectedCancelResourceOrder) throws Exception {
		this.message = message;
		this.cancelResourceOrderCreateVO = cancelResourceOrderCreateVO;
		this.expectedCancelResourceOrder = expectedCancelResourceOrder;
		createCancelResourceOrder201();
	}

	@Override
	public void createCancelResourceOrder201() throws Exception {
		// first create a resource order to cancel
		ResourceOrderCreateVO resourceOrderCreateVO = ResourceOrderCreateVOTestExample.build()
				.atSchemaLocation(null);
		HttpResponse<ResourceOrderVO> createResponse = callAndCatch(
				() -> resourceOrderApiTestClient.createResourceOrder(null, resourceOrderCreateVO));
		assertEquals(HttpStatus.CREATED, createResponse.getStatus(), "A resource order should have been created first.");
		String resourceOrderId = createResponse.body().getId();

		cancelResourceOrderCreateVO.getResourceOrder().setId(resourceOrderId);

		HttpResponse<CancelResourceOrderVO> cancelResponse = callAndCatch(
				() -> cancelResourceOrderApiTestClient.createCancelResourceOrder(null, cancelResourceOrderCreateVO));
		assertEquals(HttpStatus.CREATED, cancelResponse.getStatus(), message);

		String cancelId = cancelResponse.body().getId();
		expectedCancelResourceOrder.setId(cancelId);
		expectedCancelResourceOrder.setHref(cancelId);
		expectedCancelResourceOrder.getResourceOrder().setId(resourceOrderId);
	}

	private static Stream<Arguments> provideValidCancelResourceOrders() {
		List<Arguments> testEntries = new ArrayList<>();

		ResourceOrderRefVO resourceOrderRef = new ResourceOrderRefVO()
				.id("urn:ngsi-ld:resource-order:placeholder");

		testEntries.add(
				Arguments.of("A cancellation should have been created.",
						CancelResourceOrderCreateVOTestExample.build()
								.atSchemaLocation(null)
								.resourceOrder(resourceOrderRef),
						CancelResourceOrderVOTestExample.build()
								.atSchemaLocation(null)
								.resourceOrder(resourceOrderRef)));

		return testEntries.stream();
	}

	@ParameterizedTest
	@MethodSource("provideInvalidCancelResourceOrders")
	public void createCancelResourceOrder400(String message, CancelResourceOrderCreateVO invalidCreateVO)
			throws Exception {
		this.message = message;
		this.cancelResourceOrderCreateVO = invalidCreateVO;
		createCancelResourceOrder400();
	}

	@Override
	public void createCancelResourceOrder400() throws Exception {
		HttpResponse<CancelResourceOrderVO> creationResponse = callAndCatch(
				() -> cancelResourceOrderApiTestClient.createCancelResourceOrder(null, cancelResourceOrderCreateVO));
		assertEquals(HttpStatus.BAD_REQUEST, creationResponse.getStatus(), message);
	}

	private static Stream<Arguments> provideInvalidCancelResourceOrders() {
		List<Arguments> testEntries = new ArrayList<>();

		testEntries.add(
				Arguments.of("A cancel resource order without a resource order reference should not be created.",
						CancelResourceOrderCreateVOTestExample.build()
								.atSchemaLocation(null)
								.resourceOrder(null)));

		ResourceOrderRefVO nonExistentRef = new ResourceOrderRefVO()
				.id("urn:ngsi-ld:resource-order:non-existent");
		testEntries.add(
				Arguments.of("A cancel resource order with a non-existent resource order should not be created.",
						CancelResourceOrderCreateVOTestExample.build()
								.atSchemaLocation(null)
								.resourceOrder(nonExistentRef)));

		return testEntries.stream();
	}

	@Disabled("Security is handled externally, thus 401 and 403 cannot happen.")
	@Override
	public void createCancelResourceOrder401() throws Exception {
	}

	@Disabled("Security is handled externally, thus 401 and 403 cannot happen.")
	@Override
	public void createCancelResourceOrder403() throws Exception {
	}

	@Disabled("Prohibited by the framework.")
	@Override
	public void createCancelResourceOrder405() throws Exception {
	}

	@Disabled("Impossible status.")
	@Override
	public void createCancelResourceOrder409() throws Exception {
	}

	@Override
	public void createCancelResourceOrder500() throws Exception {
	}

	@Override
	protected String getEntityType() {
		return CancelResourceOrder.TYPE_CANCEL_RESOURCE_ORDER;
	}

	@Test
	@Override
	public void listCancelResourceOrder200() throws Exception {
		HttpResponse<List<CancelResourceOrderVO>> listResponse = callAndCatch(
				() -> cancelResourceOrderApiTestClient.listCancelResourceOrder(null, null, null, null));
		assertEquals(HttpStatus.OK, listResponse.getStatus(), "The list should be accessible.");
	}

	@Disabled("400 is impossible.")
	@Override
	public void listCancelResourceOrder400() throws Exception {
	}

	@Disabled("Security is handled externally, thus 401 and 403 cannot happen.")
	@Override
	public void listCancelResourceOrder401() throws Exception {
	}

	@Disabled("Security is handled externally, thus 401 and 403 cannot happen.")
	@Override
	public void listCancelResourceOrder403() throws Exception {
	}

	@Disabled("Not applicable.")
	@Override
	public void listCancelResourceOrder404() throws Exception {
	}

	@Disabled("Prohibited by the framework.")
	@Override
	public void listCancelResourceOrder405() throws Exception {
	}

	@Override
	public void listCancelResourceOrder409() throws Exception {
	}

	@Override
	public void listCancelResourceOrder500() throws Exception {
	}

	@Test
	@Override
	public void retrieveCancelResourceOrder200() throws Exception {
		// first create a resource order, then cancel it, then retrieve the cancellation
		ResourceOrderCreateVO resourceOrderCreateVO = ResourceOrderCreateVOTestExample.build()
				.atSchemaLocation(null);
		HttpResponse<ResourceOrderVO> createResponse = callAndCatch(
				() -> resourceOrderApiTestClient.createResourceOrder(null, resourceOrderCreateVO));
		String resourceOrderId = createResponse.body().getId();

		ResourceOrderRefVO resourceOrderRef = new ResourceOrderRefVO().id(resourceOrderId);
		CancelResourceOrderCreateVO cancelCreateVO = CancelResourceOrderCreateVOTestExample.build()
				.atSchemaLocation(null)
				.resourceOrder(resourceOrderRef);
		HttpResponse<CancelResourceOrderVO> cancelResponse = callAndCatch(
				() -> cancelResourceOrderApiTestClient.createCancelResourceOrder(null, cancelCreateVO));
		assertEquals(HttpStatus.CREATED, cancelResponse.getStatus());
		String cancelId = cancelResponse.body().getId();

		HttpResponse<CancelResourceOrderVO> retrievedCancel = callAndCatch(
				() -> cancelResourceOrderApiTestClient.retrieveCancelResourceOrder(null, cancelId, null));
		assertEquals(HttpStatus.OK, retrievedCancel.getStatus(), "The cancellation should be retrievable.");
		assertEquals(cancelId, retrievedCancel.body().getId(), "The correct cancellation should be returned.");
	}

	@Disabled("400 is impossible.")
	@Override
	public void retrieveCancelResourceOrder400() throws Exception {
	}

	@Disabled("Security is handled externally, thus 401 and 403 cannot happen.")
	@Override
	public void retrieveCancelResourceOrder401() throws Exception {
	}

	@Disabled("Security is handled externally, thus 401 and 403 cannot happen.")
	@Override
	public void retrieveCancelResourceOrder403() throws Exception {
	}

	@Test
	@Override
	public void retrieveCancelResourceOrder404() throws Exception {
		HttpResponse<CancelResourceOrderVO> response = callAndCatch(
				() -> cancelResourceOrderApiTestClient.retrieveCancelResourceOrder(null,
						"urn:ngsi-ld:cancel-resource-order:non-existing", null));
		assertEquals(HttpStatus.NOT_FOUND, response.getStatus(), "No such cancel resource order should exist.");
	}

	@Disabled("Prohibited by the framework.")
	@Override
	public void retrieveCancelResourceOrder405() throws Exception {
	}

	@Override
	public void retrieveCancelResourceOrder409() throws Exception {
	}

	@Override
	public void retrieveCancelResourceOrder500() throws Exception {
	}
}
