package org.fiware.tmforum.productordering;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import io.micronaut.test.annotation.MockBean;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import org.fiware.ngsi.api.EntitiesApiClient;
import org.fiware.productordering.api.CancelProductOrderApiTestClient;
import org.fiware.productordering.api.CancelProductOrderApiTestSpec;
import org.fiware.productordering.api.ProductOrderApiTestClient;
import org.fiware.productordering.model.*;
import org.fiware.tmforum.common.configuration.GeneralProperties;
import org.fiware.tmforum.common.exception.ErrorDetails;
import org.fiware.tmforum.common.notification.TMForumEventHandler;
import org.fiware.tmforum.common.test.AbstractApiIT;
import org.fiware.tmforum.productordering.domain.CancelProductOrder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
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

@MicronautTest(packages = {"org.fiware.tmforum.productordering"})
public class CancelProductOrderApiIT extends AbstractApiIT implements CancelProductOrderApiTestSpec {

	public final CancelProductOrderApiTestClient cancelProductOrderApiTestClient;
	public final ProductOrderApiTestClient productOrderApiTestClient;

	private String message;
	private String fieldsParameter;
	private CancelProductOrderCreateVO productCreateVO;
	private CancelProductOrderVO expectedProduct;
	private String productId;

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

	public CancelProductOrderApiIT(CancelProductOrderApiTestClient productOrderApiTestClient,
								   EntitiesApiClient entitiesApiClient,
								   ObjectMapper objectMapper, GeneralProperties generalProperties,
								   ProductOrderApiTestClient productOrderApiTestClient1) {
		super(entitiesApiClient, objectMapper, generalProperties);
		this.cancelProductOrderApiTestClient = productOrderApiTestClient;
		this.productOrderApiTestClient = productOrderApiTestClient1;
	}

	@BeforeEach
	public void setupProduct() {
		ProductOrderCreateVO productCreateVO = ProductOrderCreateVOTestExample.build()
				.billingAccount(null);
		productId = productOrderApiTestClient.createProductOrder(null, productCreateVO)
				.body().getId();
	}

	@AfterEach
	public void cleanProduct() {
		productOrderApiTestClient.deleteProductOrder(null, productId);
	}

	@ParameterizedTest
	@MethodSource("provideValidProducts")
	public void createCancelProductOrder201(String message, CancelProductOrderCreateVO productCreateVO,
											CancelProductOrderVO expectedProduct)
			throws Exception {
		this.message = message;
		this.productCreateVO = productCreateVO.productOrder(ProductOrderRefVOTestExample.build().id(productId));
		this.expectedProduct = expectedProduct.productOrder(ProductOrderRefVOTestExample.build().id(productId));
		createCancelProductOrder201();
	}

	@Override
	public void createCancelProductOrder201() throws Exception {

		Instant fixed = Instant.ofEpochSecond(10000);
		when(clock.instant()).thenReturn(fixed);

		HttpResponse<CancelProductOrderVO> productVOHttpResponse = callAndCatch(
				() -> cancelProductOrderApiTestClient.createCancelProductOrder(null, productCreateVO));
		assertEquals(HttpStatus.CREATED, productVOHttpResponse.getStatus(), message);
		String rfId = productVOHttpResponse.body().getId();
		expectedProduct.setId(rfId);
		expectedProduct.setHref(rfId);
		assertEquals(expectedProduct, productVOHttpResponse.body(), message);
	}

	private static Stream<Arguments> provideValidProducts() {
		List<Arguments> testEntries = new ArrayList<>();

		testEntries.add(
				Arguments.of("An empty product order should have been created.",
						CancelProductOrderCreateVOTestExample.build()
								.productOrder(null),
						CancelProductOrderVOTestExample.build()
								.productOrder(null)));

		testEntries.add(
				Arguments.of("A product order with cancellation reason should have been created.",
						CancelProductOrderCreateVOTestExample.build()
								.cancellationReason("Wrong product.")
								.productOrder(null),
						CancelProductOrderVOTestExample.build()
								.cancellationReason("Wrong product.")
								.productOrder(null)));
		testEntries.add(
				Arguments.of("A product order with a requestedCancellationDate should have been created.",
						CancelProductOrderCreateVOTestExample.build()
								.requestedCancellationDate(Instant.ofEpochSecond(10000))
								.productOrder(null),
						CancelProductOrderVOTestExample.build()
								.requestedCancellationDate(Instant.ofEpochSecond(10000))
								.productOrder(null)));

		return testEntries.stream();
	}

	@ParameterizedTest
	@MethodSource("provideInvalidProducts")
	public void createCancelProductOrder400(String message, CancelProductOrderCreateVO invalidCreateVO)
			throws Exception {
		this.message = message;
		this.productCreateVO = invalidCreateVO;
		createCancelProductOrder400();
	}

	@Override
	public void createCancelProductOrder400() throws Exception {
		HttpResponse<CancelProductOrderVO> creationResponse = callAndCatch(
				() -> cancelProductOrderApiTestClient.createCancelProductOrder(null, productCreateVO));
		assertEquals(HttpStatus.BAD_REQUEST, creationResponse.getStatus(), message);
		Optional<ErrorDetails> optionalErrorDetails = creationResponse.getBody(ErrorDetails.class);
		assertTrue(optionalErrorDetails.isPresent(), "Error details should be provided.");
	}

	private static Stream<Arguments> provideInvalidProducts() {
		List<Arguments> testEntries = new ArrayList<>();

		testEntries.add(Arguments.of("A cancellation with an invalid product order should not be created.",
				CancelProductOrderCreateVOTestExample.build()
						.productOrder(ProductOrderRefVOTestExample.build())));
		testEntries.add(Arguments.of("A product with non-existent related parties should not be created.",
				CancelProductOrderCreateVOTestExample.build()
						.productOrder(ProductOrderRefVOTestExample.build()
								.id("urn:ngsi-ld:product-order:non-existent"))));

		return testEntries.stream();
	}

	@Disabled("Security is handled externally, thus 401 and 403 cannot happen.")
	@Test
	@Override
	public void createCancelProductOrder401() throws Exception {

	}

	@Disabled("Security is handled externally, thus 401 and 403 cannot happen.")
	@Test
	@Override
	public void createCancelProductOrder403() throws Exception {

	}

	@Disabled("Prohibited by the framework.")
	@Test
	@Override
	public void createCancelProductOrder405() throws Exception {
	}

	@Disabled("No implicit creation, impossible state.")
	@Test
	@Override
	public void createCancelProductOrder409() throws Exception {

	}

	@Override
	public void createCancelProductOrder500() throws Exception {

	}

	@Test
	@Override
	public void listCancelProductOrder200() throws Exception {
		Instant now = Instant.now();
		when(clock.instant()).thenReturn(now);
		List<CancelProductOrderVO> expectedProducts = new ArrayList<>();
		for (int i = 0; i < 10; i++) {
			CancelProductOrderCreateVO productCreateVO = CancelProductOrderCreateVOTestExample.build()
					.productOrder(null);

			productCreateVO.productOrder(ProductOrderRefVOTestExample.build().id(productId));

			String id = cancelProductOrderApiTestClient.createCancelProductOrder(null, productCreateVO)
					.body().getId();
			CancelProductOrderVO productOrderVO = CancelProductOrderVOTestExample.build();
			productOrderVO
					.id(id)
					.href(id)
					.productOrder(ProductOrderRefVOTestExample.build().id(productId));
			expectedProducts.add(productOrderVO);
		}

		HttpResponse<List<CancelProductOrderVO>> productResponse = callAndCatch(
				() -> cancelProductOrderApiTestClient.listCancelProductOrder(null, null, null, null));

		assertEquals(HttpStatus.OK, productResponse.getStatus(), "The list should be accessible.");
		assertEquals(expectedProducts.size(), productResponse.getBody().get().size(),
				"All product orders should have been returned.");
		List<CancelProductOrderVO> retrievedProducts = productResponse.getBody().get();

		Map<String, CancelProductOrderVO> retrievedMap = retrievedProducts.stream()
				.collect(Collectors.toMap(product -> product.getId(),
						product -> product));

		expectedProducts.stream()
				.forEach(
						expectedProduct -> assertTrue(
								retrievedMap.containsKey(expectedProduct.getId()),
								String.format("All created product orders should be returned - Missing: %s.",
										expectedProduct,
										retrievedProducts)));
		expectedProducts.stream().forEach(
				expectedProduct -> assertEquals(expectedProduct,
						retrievedMap.get(expectedProduct.getId()),
						"The correct product orders should be retrieved."));

		// get with pagination
		Integer limit = 5;
		HttpResponse<List<CancelProductOrderVO>> firstPartResponse = callAndCatch(
				() -> cancelProductOrderApiTestClient.listCancelProductOrder(null, null, 0, limit));
		assertEquals(limit, firstPartResponse.body().size(),
				"Only the requested number of entries should be returend.");
		HttpResponse<List<CancelProductOrderVO>> secondPartResponse = callAndCatch(
				() -> cancelProductOrderApiTestClient.listCancelProductOrder(null, null, 0 + limit, limit));
		assertEquals(limit, secondPartResponse.body().size(),
				"Only the requested number of entries should be returend.");

		retrievedProducts.clear();
		retrievedProducts.addAll(firstPartResponse.body());
		retrievedProducts.addAll(secondPartResponse.body());
		expectedProducts.stream()
				.forEach(
						expectedProduct -> assertTrue(
								retrievedMap.containsKey(expectedProduct.getId()),
								String.format("All created product orders should be returned - Missing: %s.",
										expectedProduct)));
		expectedProducts.stream().forEach(
				expectedProduct -> assertEquals(expectedProduct,
						retrievedMap.get(expectedProduct.getId()),
						"The correct product orders should be retrieved."));
	}

	@Test
	@Override
	public void listCancelProductOrder400() throws Exception {
		HttpResponse<List<CancelProductOrderVO>> badRequestResponse = callAndCatch(
				() -> cancelProductOrderApiTestClient.listCancelProductOrder(null, null, -1, null));
		assertEquals(HttpStatus.BAD_REQUEST,
				badRequestResponse.getStatus(),
				"Negative offsets are impossible.");

		Optional<ErrorDetails> optionalErrorDetails = badRequestResponse.getBody(ErrorDetails.class);
		assertTrue(optionalErrorDetails.isPresent(), "Error details should be provided.");

		badRequestResponse = callAndCatch(() -> cancelProductOrderApiTestClient.listCancelProductOrder(null, null, null, -1));
		assertEquals(HttpStatus.BAD_REQUEST,
				badRequestResponse.getStatus(),
				"Negative limits are impossible.");
		optionalErrorDetails = badRequestResponse.getBody(ErrorDetails.class);
		assertTrue(optionalErrorDetails.isPresent(), "Error details should be provided.");
	}

	@Disabled("Security is handled externally, thus 401 and 403 cannot happen.")
	@Test
	@Override
	public void listCancelProductOrder401() throws Exception {

	}

	@Disabled("Security is handled externally, thus 401 and 403 cannot happen.")
	@Test
	@Override
	public void listCancelProductOrder403() throws Exception {

	}

	@Disabled("Not found is not possible here, will be answered with an empty list instead.")
	@Test
	@Override
	public void listCancelProductOrder404() throws Exception {

	}

	@Disabled("Prohibited by the framework.")
	@Test
	@Override
	public void listCancelProductOrder405() throws Exception {

	}

	@Disabled("Impossible status.")
	@Test
	@Override
	public void listCancelProductOrder409() throws Exception {

	}

	@Override
	public void listCancelProductOrder500() throws Exception {

	}

	@ParameterizedTest
	@MethodSource("provideFieldParameters")
	public void retrieveCancelProductOrder200(String message, String fields, CancelProductOrderVO expectedProduct)
			throws Exception {
		this.fieldsParameter = fields;
		this.message = message;
		this.expectedProduct = expectedProduct;
		retrieveCancelProductOrder200();
	}

	@Override
	public void retrieveCancelProductOrder200() throws Exception {

		CancelProductOrderCreateVO productCreateVO = CancelProductOrderCreateVOTestExample.build()
				.productOrder(ProductOrderRefVOTestExample.build().id(productId));
		HttpResponse<CancelProductOrderVO> createResponse = callAndCatch(
				() -> cancelProductOrderApiTestClient.createCancelProductOrder(null, productCreateVO));
		assertEquals(HttpStatus.CREATED, createResponse.getStatus(), message);
		String id = createResponse.body().getId();

		expectedProduct
				.id(id)
				.href(id);

		Optional.ofNullable(expectedProduct.getProductOrder())
				.ifPresent(po -> expectedProduct.productOrder(ProductOrderRefVOTestExample.build().id(productId)));

		//then retrieve
		HttpResponse<CancelProductOrderVO> retrievedRF = callAndCatch(
				() -> cancelProductOrderApiTestClient.retrieveCancelProductOrder(null, id, fieldsParameter));
		assertEquals(HttpStatus.OK, retrievedRF.getStatus(), message);
		assertEquals(expectedProduct, retrievedRF.body(), message);
	}

	private static Stream<Arguments> provideFieldParameters() {
		return Stream.of(
				Arguments.of("Without a fields parameter everything should be returned.", null,
						CancelProductOrderVOTestExample.build()),
				Arguments.of("Only cancellationReason and the mandatory parameters should have been included.",
						"cancellationReason",
						CancelProductOrderVOTestExample.build()
								.productOrder(null)
								.effectiveCancellationDate(null)
								.requestedCancellationDate(null)
								.state(null)
								.atBaseType(null)
								.atType(null)
								.atSchemaLocation(null)),
				Arguments.of(
						"Only the mandatory parameters should have been included when a non-existent field was requested.",
						"nothingToSeeHere", CancelProductOrderVOTestExample.build()
								.productOrder(null)
								.cancellationReason(null)
								.effectiveCancellationDate(null)
								.requestedCancellationDate(null)
								.state(null)
								.atBaseType(null)
								.atType(null)
								.atSchemaLocation(null)),
				Arguments.of("Only state, cancellationReason and the mandatory parameters should have been included.",
						"state,cancellationReason", CancelProductOrderVOTestExample.build()
								.productOrder(null)
								.effectiveCancellationDate(null)
								.requestedCancellationDate(null)
								.atBaseType(null)
								.atType(null)
								.atSchemaLocation(null)));
	}

	@Disabled("400 cannot happen, only 404")
	@Test
	@Override
	public void retrieveCancelProductOrder400() throws Exception {

	}

	@Disabled("Security is handled externally, thus 401 and 403 cannot happen.")
	@Test
	@Override
	public void retrieveCancelProductOrder401() throws Exception {

	}

	@Disabled("Security is handled externally, thus 401 and 403 cannot happen.")
	@Test
	@Override
	public void retrieveCancelProductOrder403() throws Exception {

	}

	@Test
	@Override
	public void retrieveCancelProductOrder404() throws Exception {
		HttpResponse<CancelProductOrderVO> response = callAndCatch(
				() -> cancelProductOrderApiTestClient.retrieveCancelProductOrder(null,
						"urn:ngsi-ld:product-function:non-existent",
						null));
		assertEquals(HttpStatus.NOT_FOUND, response.getStatus(), "No such product-catalog should exist.");

		Optional<ErrorDetails> optionalErrorDetails = response.getBody(ErrorDetails.class);
		assertTrue(optionalErrorDetails.isPresent(), "Error details should have been provided.");
	}

	@Disabled("Prohibited by the framework.")
	@Test
	@Override
	public void retrieveCancelProductOrder405() throws Exception {

	}

	@Disabled("Conflict not possible on retrieval")
	@Test
	@Override
	public void retrieveCancelProductOrder409() throws Exception {

	}

	@Override
	public void retrieveCancelProductOrder500() throws Exception {

	}

	@Override
	protected String getEntityType() {
		return CancelProductOrder.TYPE_CANCEL_PRODUCT_ORDER;
	}
}

