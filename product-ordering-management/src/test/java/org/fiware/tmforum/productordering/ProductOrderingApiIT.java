package org.fiware.tmforum.productordering;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import io.micronaut.test.annotation.MockBean;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import org.fiware.ngsi.api.EntitiesApiClient;
import org.fiware.productordering.api.ProductOrderApiTestClient;
import org.fiware.productordering.api.ProductOrderApiTestSpec;
import org.fiware.productordering.model.*;
import org.fiware.tmforum.common.configuration.GeneralProperties;
import org.fiware.tmforum.common.exception.ErrorDetails;
import org.fiware.tmforum.common.notification.TMForumEventHandler;
import org.fiware.tmforum.common.test.AbstractApiIT;
import org.fiware.tmforum.common.test.ArgumentPair;
import org.fiware.tmforum.productordering.domain.ProductOrder;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import reactor.core.publisher.Mono;

import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
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
public class ProductOrderingApiIT extends AbstractApiIT implements ProductOrderApiTestSpec {

	public final ProductOrderApiTestClient productOrderApiTestClient;

	private String message;
	private String fieldsParameter;
	private ProductOrderCreateVO productCreateVO;
	private ProductOrderUpdateVO productUpdateVO;
	private ProductOrderVO expectedProduct;

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

	public ProductOrderingApiIT(ProductOrderApiTestClient productOrderApiTestClient,
								EntitiesApiClient entitiesApiClient,
								ObjectMapper objectMapper, GeneralProperties generalProperties) {
		super(entitiesApiClient, objectMapper, generalProperties);
		this.productOrderApiTestClient = productOrderApiTestClient;
	}

	@ParameterizedTest
	@MethodSource("provideValidProducts")
	public void createProductOrder201(String message, ProductOrderCreateVO productCreateVO,
									  ProductOrderVO expectedProduct)
			throws Exception {
		this.message = message;
		this.productCreateVO = productCreateVO;
		this.expectedProduct = expectedProduct;
		createProductOrder201();
	}

	@Override
	public void createProductOrder201() throws Exception {

		Instant now = Instant.now();
		when(clock.instant()).thenReturn(now);

		HttpResponse<ProductOrderVO> productVOHttpResponse = callAndCatch(
				() -> productOrderApiTestClient.createProductOrder(null, productCreateVO));
		assertEquals(HttpStatus.CREATED, productVOHttpResponse.getStatus(), message);
		String rfId = productVOHttpResponse.body().getId();
		expectedProduct.setId(rfId);
		expectedProduct.setHref(rfId);
		expectedProduct.setOrderDate(now);
		assertEquals(expectedProduct, productVOHttpResponse.body(), message);
	}

	private static Stream<Arguments> provideValidProducts() {
		List<Arguments> testEntries = new ArrayList<>();

		testEntries.add(
				Arguments.of("An empty product order should have been created.",
						ProductOrderCreateVOTestExample.build().atSchemaLocation(null)
								.billingAccount(null),
						ProductOrderVOTestExample.build().atSchemaLocation(null)
								.billingAccount(null)));

		Instant now = Instant.now();
		testEntries.add(
				Arguments.of("A product order with cancellationDate should have been created.",
						ProductOrderCreateVOTestExample.build().atSchemaLocation(null)
								.cancellationDate(now)
								.billingAccount(null),
						ProductOrderVOTestExample.build().atSchemaLocation(null)
								.cancellationDate(now)
								.billingAccount(null)));
		testEntries.add(
				Arguments.of("A product order with cancellation reason should have been created.",
						ProductOrderCreateVOTestExample.build().atSchemaLocation(null)
								.cancellationReason("Wrong product.")
								.billingAccount(null),
						ProductOrderVOTestExample.build().atSchemaLocation(null)
								.cancellationReason("Wrong product.")
								.billingAccount(null)));
		testEntries.add(
				Arguments.of("A product order with a category should have been created.",
						ProductOrderCreateVOTestExample.build().atSchemaLocation(null)
								.category("A")
								.billingAccount(null),
						ProductOrderVOTestExample.build().atSchemaLocation(null)
								.category("A")
								.billingAccount(null)));
		testEntries.add(
				Arguments.of("A product order with a description should have been created.",
						ProductOrderCreateVOTestExample.build().atSchemaLocation(null)
								.description("A")
								.billingAccount(null),
						ProductOrderVOTestExample.build().atSchemaLocation(null)
								.description("A")
								.billingAccount(null)));
		testEntries.add(
				Arguments.of("A product order with an externalId should have been created.",
						ProductOrderCreateVOTestExample.build().atSchemaLocation(null)
								.externalId("id")
								.billingAccount(null),
						ProductOrderVOTestExample.build().atSchemaLocation(null)
								.externalId("id")
								.billingAccount(null)));
		testEntries.add(
				Arguments.of("A product order with a notificationContact should have been created.",
						ProductOrderCreateVOTestExample.build().atSchemaLocation(null)
								.notificationContact("admin@admin.org")
								.billingAccount(null),
						ProductOrderVOTestExample.build().atSchemaLocation(null)
								.notificationContact("admin@admin.org")
								.billingAccount(null)));
		testEntries.add(
				Arguments.of("A product order with a priority should have been created.",
						ProductOrderCreateVOTestExample.build().atSchemaLocation(null)
								.priority("1")
								.billingAccount(null),
						ProductOrderVOTestExample.build().atSchemaLocation(null)
								.priority("1")
								.billingAccount(null)));
		testEntries.add(
				Arguments.of("A product order with a requestedCompletionDate should have been created.",
						ProductOrderCreateVOTestExample.build().atSchemaLocation(null)
								.requestedCompletionDate(now)
								.billingAccount(null),
						ProductOrderVOTestExample.build().atSchemaLocation(null)
								.requestedCompletionDate(now)
								.billingAccount(null)));
		testEntries.add(
				Arguments.of("A product order with a requestedStartDate should have been created.",
						ProductOrderCreateVOTestExample.build().atSchemaLocation(null)
								.requestedStartDate(now)
								.billingAccount(null),
						ProductOrderVOTestExample.build().atSchemaLocation(null)
								.requestedStartDate(now)
								.billingAccount(null)));
		testEntries.add(
				Arguments.of("A product order with a note should have been created.",
						ProductOrderCreateVOTestExample.build().atSchemaLocation(null)
								.note(List.of(NoteVOTestExample.build().atSchemaLocation(null).id("urn:note").text("myNote")))
								.billingAccount(null),
						ProductOrderVOTestExample.build().atSchemaLocation(null)
								.note(List.of(NoteVOTestExample.build().atSchemaLocation(null).id("urn:note").text("myNote")))
								.billingAccount(null)));
		testEntries.add(
				Arguments.of("A product order with multiple notes should have been created.",
						ProductOrderCreateVOTestExample.build().atSchemaLocation(null)
								.note(List.of(NoteVOTestExample.build().atSchemaLocation(null).id("urn:note1").text("myNote"),
										NoteVOTestExample.build().atSchemaLocation(null).id("urn:note2").author("fromAnotherAuthor")))
								.billingAccount(null),
						ProductOrderVOTestExample.build().atSchemaLocation(null)
								.note(List.of(NoteVOTestExample.build().atSchemaLocation(null).id("urn:note1").text("myNote"),
										NoteVOTestExample.build().atSchemaLocation(null).id("urn:note2").author("fromAnotherAuthor")))
								.billingAccount(null)));

		OrderPriceVO orderPriceVO = OrderPriceVOTestExample.build().atSchemaLocation(null)
				.price(PriceVOTestExample.build().atSchemaLocation(null).dutyFreeAmount(MoneyVOTestExample.build().value(2.1f)))
				.productOfferingPrice(null)
				.billingAccount(null);

		testEntries.add(
				Arguments.of("A product order with an total price should have been created.",
						ProductOrderCreateVOTestExample.build().atSchemaLocation(null)
								.orderTotalPrice(List.of(orderPriceVO))
								.billingAccount(null),
						ProductOrderVOTestExample.build().atSchemaLocation(null)
								.orderTotalPrice(List.of(orderPriceVO))
								.billingAccount(null)));

		ProductOrderItemVO productOrderItemVO = ProductOrderItemVOTestExample.build().atSchemaLocation(null)
				.action(OrderItemActionTypeVO.ADD)
				.id("urn:order-item")
				.appointment(null)
				.billingAccount(null)
				.product(null)
				.productOffering(null)
				.productOfferingQualificationItem(null)
				.quoteItem(null);

		testEntries.add(
				Arguments.of("A product order with an order item should have been created.",
						ProductOrderCreateVOTestExample.build().atSchemaLocation(null)
								.productOrderItem(List.of(productOrderItemVO))
								.billingAccount(null),
						ProductOrderVOTestExample.build().atSchemaLocation(null)
								.productOrderItem(List.of(productOrderItemVO))
								.billingAccount(null)));

		CharacteristicVO characteristicVO = CharacteristicVOTestExample.build().atSchemaLocation(null)
				.name("Characteristic Name")
				.value("Value");

		ProductRefOrValueVO productVO = ProductRefOrValueVOTestExample.build().atSchemaLocation(null)
				.id(null)
				.description("Product Ref")
				.productCharacteristic(List.of(characteristicVO))
				.billingAccount(null)
				.productOffering(null)
				.productSpecification(null)
				.atReferredType(null);

		ProductOrderItemVO productOrderItemProductVO = ProductOrderItemVOTestExample.build().atSchemaLocation(null)
				.action(OrderItemActionTypeVO.ADD)
				.id("urn:order-item")
				.appointment(null)
				.billingAccount(null)
				.product(productVO)
				.productOffering(null)
				.productOfferingQualificationItem(null)
				.quoteItem(null);

		testEntries.add(
				Arguments.of("A product order with an order item with product template should have been created.",
						ProductOrderCreateVOTestExample.build().atSchemaLocation(null)
								.productOrderItem(List.of(productOrderItemProductVO))
								.billingAccount(null),
						ProductOrderVOTestExample.build().atSchemaLocation(null)
								.productOrderItem(List.of(productOrderItemProductVO))
								.billingAccount(null)));

		return testEntries.stream();
	}

	@ParameterizedTest
	@MethodSource("provideInvalidProducts")
	public void createProductOrder400(String message, ProductOrderCreateVO invalidCreateVO) throws Exception {
		this.message = message;
		this.productCreateVO = invalidCreateVO;
		createProductOrder400();
	}

	@Override
	public void createProductOrder400() throws Exception {
		HttpResponse<ProductOrderVO> creationResponse = callAndCatch(
				() -> productOrderApiTestClient.createProductOrder(null, productCreateVO));
		assertEquals(HttpStatus.BAD_REQUEST, creationResponse.getStatus(), message);
		Optional<ErrorDetails> optionalErrorDetails = creationResponse.getBody(ErrorDetails.class);
		assertTrue(optionalErrorDetails.isPresent(), "Error details should be provided.");
	}

	private static Stream<Arguments> provideInvalidProducts() {
		List<Arguments> testEntries = new ArrayList<>();

		testEntries.add(Arguments.of("A product with invalid related parties should not be created.",
				ProductOrderCreateVOTestExample.build().atSchemaLocation(null)
						.billingAccount(null)
						.relatedParty(List.of(RelatedPartyVOTestExample.build().atSchemaLocation(null)))));
		testEntries.add(Arguments.of("A product with non-existent related parties should not be created.",
				ProductOrderCreateVOTestExample.build().atSchemaLocation(null)
						.billingAccount(null)
						.relatedParty(
								List.of((RelatedPartyVOTestExample.build().atSchemaLocation(null)
										.id("urn:ngsi-ld:organisation:non-existent"))))));

		testEntries.add(Arguments.of("A product with an invalid agreement should not be created.",
				ProductOrderCreateVOTestExample.build().atSchemaLocation(null)
						.billingAccount(null)
						.agreement(List.of(AgreementRefVOTestExample.build().atSchemaLocation(null)))));
		testEntries.add(Arguments.of("A product with a non-existent agreement should not be created.",
				ProductOrderCreateVOTestExample.build().atSchemaLocation(null)
						.billingAccount(null)
						.agreement(
								List.of(AgreementRefVOTestExample.build().atSchemaLocation(null)
										.id("urn:ngsi-ld:agreement:non-existent")))));

		testEntries.add(Arguments.of("A product with an invalid billing account should not be created.",
				ProductOrderCreateVOTestExample.build().atSchemaLocation(null)
						.billingAccount(BillingAccountRefVOTestExample.build().atSchemaLocation(null))));
		testEntries.add(Arguments.of("A product with non-existent billing account should not be created.",
				ProductOrderCreateVOTestExample.build().atSchemaLocation(null)
						.billingAccount(BillingAccountRefVOTestExample.build().atSchemaLocation(null)
								.id("urn:ngsi-ld:billing-account:non-existent"))));

		testEntries.add(Arguments.of("A product with an invalid channel should not be created.",
				ProductOrderCreateVOTestExample.build().atSchemaLocation(null)
						.billingAccount(null)
						.channel(List.of(RelatedChannelVOTestExample.build().atSchemaLocation(null)))));
		testEntries.add(Arguments.of("A product with a non-existent channel should not be created.",
				ProductOrderCreateVOTestExample.build().atSchemaLocation(null)
						.billingAccount(null)
						.channel(
								List.of(RelatedChannelVOTestExample.build().atSchemaLocation(null).id("urn:ngsi-ld:channel:non-existent")))));

		testEntries.add(Arguments.of("A product with an invalid payment should not be created.",
				ProductOrderCreateVOTestExample.build().atSchemaLocation(null)
						.billingAccount(null)
						.payment(List.of(PaymentRefVOTestExample.build().atSchemaLocation(null)))));
		testEntries.add(Arguments.of("A product with a non-existent payment should not be created.",
				ProductOrderCreateVOTestExample.build().atSchemaLocation(null)
						.billingAccount(null)
						.payment(List.of(PaymentRefVOTestExample.build().atSchemaLocation(null).id("urn:ngsi-ld:payment:non-existent")))));

		testEntries.add(Arguments.of("A product with an invalid payment should not be created.",
				ProductOrderCreateVOTestExample.build().atSchemaLocation(null)
						.billingAccount(null)
						.productOfferingQualification(
								List.of(ProductOfferingQualificationRefVOTestExample.build().atSchemaLocation(null)))));
		testEntries.add(Arguments.of("A product with a non-existent payment should not be created.",
				ProductOrderCreateVOTestExample.build().atSchemaLocation(null)
						.billingAccount(null)
						.productOfferingQualification(
								List.of(ProductOfferingQualificationRefVOTestExample.build().atSchemaLocation(null)
										.id("urn:ngsi-ld:product-offering-qualification:non-existent")))));

		testEntries.add(Arguments.of("A product with an invalid quote should not be created.",
				ProductOrderCreateVOTestExample.build().atSchemaLocation(null)
						.billingAccount(null)
						.quote(
								List.of(QuoteRefVOTestExample.build().atSchemaLocation(null)))));
		testEntries.add(Arguments.of("A product with a non-existent quote should not be created.",
				ProductOrderCreateVOTestExample.build().atSchemaLocation(null)
						.billingAccount(null)
						.quote(
								List.of(QuoteRefVOTestExample.build().atSchemaLocation(null)
										.id("urn:ngsi-ld:quote:non-existent")))));

		provideInvalidPrices().forEach(invalidPrice ->
				testEntries.add(
						Arguments.of(invalidPrice.message(),
								ProductOrderCreateVOTestExample.build().atSchemaLocation(null).billingAccount(null)
										.orderTotalPrice(invalidPrice.value()))
				));

		provideInvalidOrderItems().forEach(invalidOrderItem ->
				testEntries.add(Arguments.of(invalidOrderItem.message(),
						ProductOrderCreateVOTestExample.build().atSchemaLocation(null).billingAccount(null)
								.productOrderItem(invalidOrderItem.value()))));
		return testEntries.stream();
	}

	private static Stream<ArgumentPair<List<ProductOrderItemVO>>> provideInvalidOrderItems() {

		List<ArgumentPair<List<ProductOrderItemVO>>> invalidItems = new ArrayList<>();

		invalidItems.add(new ArgumentPair<>("An order item with an invalid appointment should not be accepted.",
				List.of(ProductOrderItemVOTestExample.build().atSchemaLocation(null)
						.billingAccount(null)
						.product(null)
						.productOffering(null)
						.productOfferingQualificationItem(null)
						.quoteItem(null)
						.appointment(AppointmentRefVOTestExample.build().atSchemaLocation(null)))));
		invalidItems.add(new ArgumentPair<>("An order item with a non existent appointment should not be accepted.",
				List.of(ProductOrderItemVOTestExample.build().atSchemaLocation(null)
						.billingAccount(null)
						.product(null)
						.productOffering(null)
						.productOfferingQualificationItem(null)
						.quoteItem(null)
						.appointment(AppointmentRefVOTestExample.build().atSchemaLocation(null).id("urn:ngsi-ld:appointment:non-existent")))));

		invalidItems.add(new ArgumentPair<>("An order item with an invalid billing account should not be accepted.",
				List.of(ProductOrderItemVOTestExample.build().atSchemaLocation(null)
						.billingAccount(BillingAccountRefVOTestExample.build().atSchemaLocation(null))
						.product(null)
						.productOffering(null)
						.productOfferingQualificationItem(null)
						.quoteItem(null)
						.appointment(null))));
		invalidItems.add(new ArgumentPair<>("An order item with a non existent appointment should not be accepted.",
				List.of(ProductOrderItemVOTestExample.build().atSchemaLocation(null)
						.billingAccount(
								BillingAccountRefVOTestExample.build().atSchemaLocation(null).id("urn:ngsi-ld:billing-account:non-existent"))
						.product(null)
						.productOffering(null)
						.productOfferingQualificationItem(null)
						.quoteItem(null)
						.appointment(null))));

		invalidItems.add(new ArgumentPair<>("An order item with a non existent product should not be accepted.",
				List.of(ProductOrderItemVOTestExample.build().atSchemaLocation(null)
						.billingAccount(null)
						.product(ProductRefOrValueVOTestExample.build().atSchemaLocation(null).id("urn:ngsi-ld:product:non-existent"))
						.productOffering(null)
						.productOfferingQualificationItem(null)
						.quoteItem(null)
						.appointment(null))));

		invalidItems.add(new ArgumentPair<>("An order item with an invalid productOffering should not be accepted.",
				List.of(ProductOrderItemVOTestExample.build().atSchemaLocation(null)
						.billingAccount(null)
						.product(null)
						.productOffering(ProductOfferingRefVOTestExample.build().atSchemaLocation(null))
						.productOfferingQualificationItem(null)
						.quoteItem(null)
						.appointment(null))));
		invalidItems.add(new ArgumentPair<>("An order item with a non existent productOffering should not be accepted.",
				List.of(ProductOrderItemVOTestExample.build().atSchemaLocation(null)
						.billingAccount(null)
						.product(null)
						.productOffering(
								ProductOfferingRefVOTestExample.build().atSchemaLocation(null).id("urn:ngsi-ld:product-offering:non-existent"))
						.productOfferingQualificationItem(null)
						.quoteItem(null)
						.appointment(null))));

		invalidItems.add(new ArgumentPair<>(
				"An order item with an invalid productOffering qualification item should not be accepted.",
				List.of(ProductOrderItemVOTestExample.build().atSchemaLocation(null)
						.billingAccount(null)
						.product(null)
						.productOffering(null)
						.productOfferingQualificationItem(ProductOfferingQualificationItemRefVOTestExample.build().atSchemaLocation(null))
						.quoteItem(null)
						.appointment(null))));
		invalidItems.add(new ArgumentPair<>(
				"An order item with a non existent productOffering qualification item should not be accepted.",
				List.of(ProductOrderItemVOTestExample.build().atSchemaLocation(null)
						.billingAccount(null)
						.product(null)
						.productOffering(null)
						.productOfferingQualificationItem(ProductOfferingQualificationItemRefVOTestExample.build().atSchemaLocation(null)
								.productOfferingQualificationId(
										"urn:ngsi-ld:product-offering-qualification:non-existent"))
						.quoteItem(null)
						.appointment(null))));

		invalidItems.add(new ArgumentPair<>(
				"An order item with an invalid quote should not be accepted.",
				List.of(ProductOrderItemVOTestExample.build().atSchemaLocation(null)
						.billingAccount(null)
						.product(null)
						.productOffering(null)
						.productOfferingQualificationItem(null)
						.quoteItem(QuoteItemRefVOTestExample.build().atSchemaLocation(null))
						.appointment(null))));
		invalidItems.add(new ArgumentPair<>(
				"An order item with a non existent quote should not be accepted.",
				List.of(ProductOrderItemVOTestExample.build().atSchemaLocation(null)
						.billingAccount(null)
						.product(null)
						.productOffering(null)
						.productOfferingQualificationItem(null)
						.quoteItem(QuoteItemRefVOTestExample.build().atSchemaLocation(null).quoteId("urn:ngsi-ld:quote:non-existent"))
						.appointment(null))));

		return invalidItems.stream();
	}

	private static Stream<ArgumentPair<List<OrderPriceVO>>> provideInvalidPrices() {
		List<ArgumentPair<List<OrderPriceVO>>> invalidPrices = new ArrayList<>();

		invalidPrices.add(
				new ArgumentPair<>("A price with an invalid billing account should not be accepted",
						List.of(OrderPriceVOTestExample.build().atSchemaLocation(null)
								.billingAccount(BillingAccountRefVOTestExample.build().atSchemaLocation(null).id("invalid"))
								.productOfferingPrice(null))));
		invalidPrices.add(
				new ArgumentPair<>("A price with an non-existent billing account should not be accepted",
						List.of(OrderPriceVOTestExample.build().atSchemaLocation(null)
								.billingAccount(BillingAccountRefVOTestExample.build().atSchemaLocation(null)
										.id("urn:ngsi-ld:billing-account:non-existent"))
								.productOfferingPrice(null))));
		invalidPrices.add(
				new ArgumentPair<>("A price with an invalid product offering price should not be accepted",
						List.of(OrderPriceVOTestExample.build().atSchemaLocation(null)
								.productOfferingPrice(ProductOfferingPriceRefVOTestExample.build().atSchemaLocation(null).id("invalid"))
								.billingAccount(null))));
		invalidPrices.add(
				new ArgumentPair<>("A price with an non-existent product offering price should not be accepted",
						List.of(OrderPriceVOTestExample.build().atSchemaLocation(null)
								.productOfferingPrice(ProductOfferingPriceRefVOTestExample.build().atSchemaLocation(null)
										.id("urn:ngsi-ld:product-offering-price:non-existent"))
								.billingAccount(null))));

		invalidPrices.add(
				new ArgumentPair<>("A price with an invalid price alteration should not be accepted",
						List.of(OrderPriceVOTestExample.build().atSchemaLocation(null)
								.productOfferingPrice(null)
								.billingAccount(null)
								.priceAlteration(List.of(PriceAlterationVOTestExample.build().atSchemaLocation(null))))));
		invalidPrices.add(
				new ArgumentPair<>("A price with an non-existent price alteration internal ref should not be accepted",
						List.of(OrderPriceVOTestExample.build().atSchemaLocation(null)
								.productOfferingPrice(null)
								.billingAccount(null)
								.priceAlteration(List.of(PriceAlterationVOTestExample.build().atSchemaLocation(null)
										.productOfferingPrice(ProductOfferingPriceRefVOTestExample.build().atSchemaLocation(null)
												.id("urn:ngsi-ld:product-offering-price:non-existent")))))));

		return invalidPrices.stream();
	}

	@Disabled("Security is handled externally, thus 401 and 403 cannot happen.")
	@Test
	@Override
	public void createProductOrder401() throws Exception {

	}

	@Disabled("Security is handled externally, thus 401 and 403 cannot happen.")
	@Test
	@Override
	public void createProductOrder403() throws Exception {

	}

	@Disabled("Prohibited by the framework.")
	@Test
	@Override
	public void createProductOrder405() throws Exception {
	}

	@Disabled("No implicit creation, impossible state.")
	@Test
	@Override
	public void createProductOrder409() throws Exception {

	}

	@Override
	public void createProductOrder500() throws Exception {

	}

	@Test
	@Override
	public void deleteProductOrder204() throws Exception {
		ProductOrderCreateVO emptyCreate = ProductOrderCreateVOTestExample.build().atSchemaLocation(null)
				.billingAccount(null);

		HttpResponse<ProductOrderVO> createResponse = productOrderApiTestClient.createProductOrder(null, emptyCreate);
		assertEquals(HttpStatus.CREATED, createResponse.getStatus(),
				"The product order should have been created first.");

		String rfId = createResponse.body().getId();

		assertEquals(HttpStatus.NO_CONTENT,
				callAndCatch(() -> productOrderApiTestClient.deleteProductOrder(null, rfId)).getStatus(),
				"The product order should have been deleted.");

		assertEquals(HttpStatus.NOT_FOUND,
				callAndCatch(() -> productOrderApiTestClient.retrieveProductOrder(null, rfId, null)).status(),
				"The product order should not exist anymore.");
	}

	@Disabled("400 is impossible to happen on deletion with the current implementation.")
	@Test
	@Override
	public void deleteProductOrder400() throws Exception {

	}

	@Disabled("Security is handled externally, thus 401 and 403 cannot happen.")
	@Test
	@Override
	public void deleteProductOrder401() throws Exception {

	}

	@Disabled("Security is handled externally, thus 401 and 403 cannot happen.")
	@Test
	@Override
	public void deleteProductOrder403() throws Exception {

	}

	@Test
	@Override
	public void deleteProductOrder404() throws Exception {
		HttpResponse<?> notFoundResponse = callAndCatch(
				() -> productOrderApiTestClient.deleteProductOrder(null, "urn:ngsi-ld:product-catalog:no-pop"));
		assertEquals(HttpStatus.NOT_FOUND,
				notFoundResponse.getStatus(),
				"No such product-order should exist.");

		Optional<ErrorDetails> optionalErrorDetails = notFoundResponse.getBody(ErrorDetails.class);
		assertTrue(optionalErrorDetails.isPresent(), "Error details should be provided.");

		notFoundResponse = callAndCatch(() -> productOrderApiTestClient.deleteProductOrder(null, "invalid-id"));
		assertEquals(HttpStatus.NOT_FOUND,
				notFoundResponse.getStatus(),
				"No such product-order should exist.");

		optionalErrorDetails = notFoundResponse.getBody(ErrorDetails.class);
		assertTrue(optionalErrorDetails.isPresent(), "Error details should be provided.");
	}

	@Disabled("Prohibited by the framework.")
	@Test
	@Override
	public void deleteProductOrder405() throws Exception {

	}

	@Disabled("Impossible status.")
	@Test
	@Override
	public void deleteProductOrder409() throws Exception {

	}

	@Override
	public void deleteProductOrder500() throws Exception {

	}

	@Test
	@Override
	public void listProductOrder200() throws Exception {
		Instant now = Instant.now();
		when(clock.instant()).thenReturn(now);
		List<ProductOrderVO> expectedProducts = new ArrayList<>();
		for (int i = 0; i < 10; i++) {
			ProductOrderCreateVO productCreateVO = ProductOrderCreateVOTestExample.build().atSchemaLocation(null)
					.billingAccount(null);

			String id = productOrderApiTestClient.createProductOrder(null, productCreateVO)
					.body().getId();
			ProductOrderVO productOrderVO = ProductOrderVOTestExample.build().atSchemaLocation(null);
			productOrderVO
					.id(id)
					.href(id)
					.billingAccount(null)
					.relatedParty(null)
					.productOfferingQualification(null)
					.payment(null)
					.quote(null)
					.channel(null)
					.orderDate(now);
			expectedProducts.add(productOrderVO);
		}

		HttpResponse<List<ProductOrderVO>> productResponse = callAndCatch(
				() -> productOrderApiTestClient.listProductOrder(null, null, null, null));

		assertEquals(HttpStatus.OK, productResponse.getStatus(), "The list should be accessible.");
		assertEquals(expectedProducts.size(), productResponse.getBody().get().size(),
				"All product orders should have been returned.");
		List<ProductOrderVO> retrievedProducts = productResponse.getBody().get();

		Map<String, ProductOrderVO> retrievedMap = retrievedProducts.stream()
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
		HttpResponse<List<ProductOrderVO>> firstPartResponse = callAndCatch(
				() -> productOrderApiTestClient.listProductOrder(null, null, 0, limit));
		assertEquals(limit, firstPartResponse.body().size(),
				"Only the requested number of entries should be returend.");
		HttpResponse<List<ProductOrderVO>> secondPartResponse = callAndCatch(
				() -> productOrderApiTestClient.listProductOrder(null, null, 0 + limit, limit));
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
	public void listProductOrder400() throws Exception {
		HttpResponse<List<ProductOrderVO>> badRequestResponse = callAndCatch(
				() -> productOrderApiTestClient.listProductOrder(null, null, -1, null));
		assertEquals(HttpStatus.BAD_REQUEST,
				badRequestResponse.getStatus(),
				"Negative offsets are impossible.");

		Optional<ErrorDetails> optionalErrorDetails = badRequestResponse.getBody(ErrorDetails.class);
		assertTrue(optionalErrorDetails.isPresent(), "Error details should be provided.");

		badRequestResponse = callAndCatch(() -> productOrderApiTestClient.listProductOrder(null, null, null, -1));
		assertEquals(HttpStatus.BAD_REQUEST,
				badRequestResponse.getStatus(),
				"Negative limits are impossible.");
		optionalErrorDetails = badRequestResponse.getBody(ErrorDetails.class);
		assertTrue(optionalErrorDetails.isPresent(), "Error details should be provided.");
	}

	@Disabled("Security is handled externally, thus 401 and 403 cannot happen.")
	@Test
	@Override
	public void listProductOrder401() throws Exception {

	}

	@Disabled("Security is handled externally, thus 401 and 403 cannot happen.")
	@Test
	@Override
	public void listProductOrder403() throws Exception {

	}

	@Disabled("Not found is not possible here, will be answered with an empty list instead.")
	@Test
	@Override
	public void listProductOrder404() throws Exception {

	}

	@Disabled("Prohibited by the framework.")
	@Test
	@Override
	public void listProductOrder405() throws Exception {

	}

	@Disabled("Impossible status.")
	@Test
	@Override
	public void listProductOrder409() throws Exception {

	}

	@Override
	public void listProductOrder500() throws Exception {

	}

	@ParameterizedTest
	@MethodSource("provideProductOrderUpdates")
	public void patchProductOrder200(String message, ProductOrderCreateVO productCreateVO, ProductOrderUpdateVO productUpdateVO,
									 ProductOrderVO expectedProduct) throws Exception {
		this.message = message;
		this.productCreateVO = productCreateVO;
		this.productUpdateVO = productUpdateVO;
		this.expectedProduct = expectedProduct;
		patchProductOrder200();
	}

	@Override
	public void patchProductOrder200() throws Exception {

		Instant now = Instant.now();
		when(clock.instant()).thenReturn(now);

		HttpResponse<ProductOrderVO> createResponse = callAndCatch(
				() -> productOrderApiTestClient.createProductOrder(null, productCreateVO));
		assertEquals(HttpStatus.CREATED, createResponse.getStatus(),
				"The product function should have been created first.");

		String productId = createResponse.body().getId();

		HttpResponse<ProductOrderVO> updateResponse = callAndCatch(
				() -> productOrderApiTestClient.patchProductOrder(null, productId, productUpdateVO));
		assertEquals(HttpStatus.OK, updateResponse.getStatus(), message);

		ProductOrderVO updatedProduct = updateResponse.body();
		expectedProduct.href(productId).id(productId).orderDate(now);

		assertEquals(expectedProduct, updatedProduct, message);
	}

	private static Stream<Arguments> provideProductOrderUpdates() {
		List<Arguments> testEntries = new ArrayList<>();

		testEntries.add(Arguments.of("The description should have been updated.",
				ProductOrderCreateVOTestExample.build()
						.atSchemaLocation(null)
						.billingAccount(null),
				ProductOrderUpdateVOTestExample.build().atSchemaLocation(null)
						.billingAccount(null)
						.description("new-description"),
				ProductOrderVOTestExample.build().atSchemaLocation(null)
						.billingAccount(null)
						.channel(null)
						.payment(null)
						.productOfferingQualification(null)
						.quote(null)
						.relatedParty(null)
						.description("new-description")));

		testEntries.add(Arguments.of("The description should have been updated and the item should be preserved.",
				ProductOrderCreateVOTestExample.build()
						.atSchemaLocation(null)
						.billingAccount(null)
						.productOrderItem(List.of(ProductOrderItemVOTestExample.build()
								.atSchemaLocation(null)
								.id("test-item")
								.quantity(1)
								.appointment(null)
								.billingAccount(null)
								.product(null)
								.productOffering(null)
								.productOfferingQualificationItem(null)
								.quoteItem(null))),
				ProductOrderUpdateVOTestExample.build().atSchemaLocation(null)
						.billingAccount(null)
						.description("new-description")
						.productOrderItem(null),
				ProductOrderVOTestExample.build().atSchemaLocation(null)
						.billingAccount(null)
						.channel(null)
						.payment(null)
						.productOfferingQualification(null)
						.quote(null)
						.relatedParty(null)
						.description("new-description")
						.productOrderItem(List.of(ProductOrderItemVOTestExample.build()
								.atSchemaLocation(null)
								.id("test-item")
								.quantity(1)
								.appointment(null)
								.billingAccount(null)
								.product(null)
								.productOffering(null)
								.productOfferingQualificationItem(null)
								.quoteItem(null)))));

		testEntries.add(Arguments.of("The description should have been updated and the item should have been removed.",
				ProductOrderCreateVOTestExample.build()
						.atSchemaLocation(null)
						.billingAccount(null)
						.productOrderItem(List.of(ProductOrderItemVOTestExample.build()
								.atSchemaLocation(null)
								.id("test-item")
								.quantity(1)
								.appointment(null)
								.billingAccount(null)
								.product(null)
								.productOffering(null)
								.productOfferingQualificationItem(null)
								.quoteItem(null))),
				ProductOrderUpdateVOTestExample.build().atSchemaLocation(null)
						.billingAccount(null)
						.description("new-description")
						.productOrderItem(List.of()),
				ProductOrderVOTestExample.build().atSchemaLocation(null)
						.billingAccount(null)
						.channel(null)
						.payment(null)
						.productOfferingQualification(null)
						.quote(null)
						.relatedParty(null)
						.description("new-description")
						.productOrderItem(List.of())));

		testEntries.add(Arguments.of("The description should have been updated and the item quantity should have been increased.",
				ProductOrderCreateVOTestExample.build()
						.atSchemaLocation(null)
						.billingAccount(null)
						.productOrderItem(List.of(ProductOrderItemVOTestExample.build()
								.atSchemaLocation(null)
								.id("test-item")
								.quantity(1)
								.appointment(null)
								.billingAccount(null)
								.product(null)
								.productOffering(null)
								.productOfferingQualificationItem(null)
								.quoteItem(null))),
				ProductOrderUpdateVOTestExample.build().atSchemaLocation(null)
						.billingAccount(null)
						.description("new-description")
						.productOrderItem(List.of(ProductOrderItemVOTestExample.build()
								.atSchemaLocation(null)
								.id("test-item")
								.quantity(3)
								.appointment(null)
								.billingAccount(null)
								.product(null)
								.productOffering(null)
								.productOfferingQualificationItem(null)
								.quoteItem(null))),
				ProductOrderVOTestExample.build().atSchemaLocation(null)
						.billingAccount(null)
						.channel(null)
						.payment(null)
						.productOfferingQualification(null)
						.quote(null)
						.relatedParty(null)
						.description("new-description")
						.productOrderItem(List.of(ProductOrderItemVOTestExample.build()
								.atSchemaLocation(null)
								.id("test-item")
								.quantity(3)
								.appointment(null)
								.billingAccount(null)
								.product(null)
								.productOffering(null)
								.productOfferingQualificationItem(null)
								.quoteItem(null)))));

		testEntries.add(Arguments.of("The description should have been updated and the item should have been replaced.",
				ProductOrderCreateVOTestExample.build()
						.atSchemaLocation(null)
						.billingAccount(null)
						.productOrderItem(List.of(ProductOrderItemVOTestExample.build()
								.atSchemaLocation(null)
								.id("test-item")
								.quantity(1)
								.appointment(null)
								.billingAccount(null)
								.product(null)
								.productOffering(null)
								.productOfferingQualificationItem(null)
								.quoteItem(null))),
				ProductOrderUpdateVOTestExample.build().atSchemaLocation(null)
						.billingAccount(null)
						.description("new-description")
						.productOrderItem(List.of(ProductOrderItemVOTestExample.build()
								.atSchemaLocation(null)
								.id("test-item-new")
								.quantity(3)
								.appointment(null)
								.billingAccount(null)
								.product(null)
								.productOffering(null)
								.productOfferingQualificationItem(null)
								.quoteItem(null))),
				ProductOrderVOTestExample.build().atSchemaLocation(null)
						.billingAccount(null)
						.channel(null)
						.payment(null)
						.productOfferingQualification(null)
						.quote(null)
						.relatedParty(null)
						.description("new-description")
						.productOrderItem(List.of(ProductOrderItemVOTestExample.build()
								.atSchemaLocation(null)
								.id("test-item-new")
								.quantity(3)
								.appointment(null)
								.billingAccount(null)
								.product(null)
								.productOffering(null)
								.productOfferingQualificationItem(null)
								.quoteItem(null)))));

		testEntries.add(Arguments.of("The description should have been updated and the new item should have been added.",
				ProductOrderCreateVOTestExample.build()
						.atSchemaLocation(null)
						.billingAccount(null)
						.productOrderItem(List.of(ProductOrderItemVOTestExample.build()
								.atSchemaLocation(null)
								.id("test-item")
								.quantity(1)
								.appointment(null)
								.billingAccount(null)
								.product(null)
								.productOffering(null)
								.productOfferingQualificationItem(null)
								.quoteItem(null))),
				ProductOrderUpdateVOTestExample.build().atSchemaLocation(null)
						.billingAccount(null)
						.description("new-description")
						.productOrderItem(List.of(
								ProductOrderItemVOTestExample.build()
										.atSchemaLocation(null)
										.id("test-item")
										.quantity(1)
										.appointment(null)
										.billingAccount(null)
										.product(null)
										.productOffering(null)
										.productOfferingQualificationItem(null)
										.quoteItem(null),
								ProductOrderItemVOTestExample.build()
										.atSchemaLocation(null)
										.id("test-item-new")
										.quantity(3)
										.appointment(null)
										.billingAccount(null)
										.product(null)
										.productOffering(null)
										.productOfferingQualificationItem(null)
										.quoteItem(null))),
				ProductOrderVOTestExample.build().atSchemaLocation(null)
						.billingAccount(null)
						.channel(null)
						.payment(null)
						.productOfferingQualification(null)
						.quote(null)
						.relatedParty(null)
						.description("new-description")
						.productOrderItem(List.of(
								ProductOrderItemVOTestExample.build()
										.atSchemaLocation(null)
										.id("test-item")
										.quantity(1)
										.appointment(null)
										.billingAccount(null)
										.product(null)
										.productOffering(null)
										.productOfferingQualificationItem(null)
										.quoteItem(null),
								ProductOrderItemVOTestExample.build()
										.atSchemaLocation(null)
										.id("test-item-new")
										.quantity(3)
										.appointment(null)
										.billingAccount(null)
										.product(null)
										.productOffering(null)
										.productOfferingQualificationItem(null)
										.quoteItem(null)))));

		Instant date = Instant.now().plus(10, ChronoUnit.MINUTES);

		testEntries.add(Arguments.of("The cancellationDate should have been updated.",
				ProductOrderCreateVOTestExample.build()
						.atSchemaLocation(null)
						.billingAccount(null),
				ProductOrderUpdateVOTestExample.build().atSchemaLocation(null)
						.billingAccount(null)
						.cancellationDate(date),
				ProductOrderVOTestExample.build().atSchemaLocation(null)
						.billingAccount(null)
						.channel(null)
						.payment(null)
						.productOfferingQualification(null)
						.quote(null)
						.relatedParty(null)
						.cancellationDate(date)));

		testEntries.add(Arguments.of("The cancellationReason should have been updated.",
				ProductOrderCreateVOTestExample.build()
						.atSchemaLocation(null)
						.billingAccount(null),
				ProductOrderUpdateVOTestExample.build().atSchemaLocation(null)
						.billingAccount(null)
						.cancellationReason("To big."),
				ProductOrderVOTestExample.build().atSchemaLocation(null)
						.billingAccount(null)
						.channel(null)
						.payment(null)
						.productOfferingQualification(null)
						.quote(null)
						.relatedParty(null)
						.cancellationReason("To big.")));

		testEntries.add(Arguments.of("The category should have been updated.",
				ProductOrderCreateVOTestExample.build()
						.atSchemaLocation(null)
						.billingAccount(null),
				ProductOrderUpdateVOTestExample.build().atSchemaLocation(null)
						.billingAccount(null)
						.category("D"),
				ProductOrderVOTestExample.build().atSchemaLocation(null)
						.billingAccount(null)
						.channel(null)
						.payment(null)
						.productOfferingQualification(null)
						.quote(null)
						.relatedParty(null)
						.category("D")));

		testEntries.add(Arguments.of("The completionDate should have been updated.",
				ProductOrderCreateVOTestExample.build()
						.atSchemaLocation(null)
						.billingAccount(null),
				ProductOrderUpdateVOTestExample.build().atSchemaLocation(null)
						.billingAccount(null)
						.completionDate(date),
				ProductOrderVOTestExample.build().atSchemaLocation(null)
						.billingAccount(null)
						.channel(null)
						.payment(null)
						.productOfferingQualification(null)
						.quote(null)
						.relatedParty(null)
						.completionDate(date)));

		testEntries.add(Arguments.of("The expectedCompletionDate should have been updated.",
				ProductOrderCreateVOTestExample.build()
						.atSchemaLocation(null)
						.billingAccount(null),
				ProductOrderUpdateVOTestExample.build().atSchemaLocation(null)
						.billingAccount(null)
						.expectedCompletionDate(date),
				ProductOrderVOTestExample.build().atSchemaLocation(null)
						.billingAccount(null)
						.channel(null)
						.payment(null)
						.productOfferingQualification(null)
						.quote(null)
						.relatedParty(null)
						.expectedCompletionDate(date)));

		testEntries.add(Arguments.of("The requestedCompletionDate should have been updated.",
				ProductOrderCreateVOTestExample.build()
						.atSchemaLocation(null)
						.billingAccount(null),
				ProductOrderUpdateVOTestExample.build().atSchemaLocation(null)
						.billingAccount(null)
						.requestedCompletionDate(date),
				ProductOrderVOTestExample.build().atSchemaLocation(null)
						.billingAccount(null)
						.channel(null)
						.payment(null)
						.productOfferingQualification(null)
						.quote(null)
						.relatedParty(null)
						.requestedCompletionDate(date)));

		testEntries.add(Arguments.of("The externalId should have been updated.",
				ProductOrderCreateVOTestExample.build()
						.atSchemaLocation(null)
						.billingAccount(null),
				ProductOrderUpdateVOTestExample.build().atSchemaLocation(null)
						.billingAccount(null)
						.externalId("otherId"),
				ProductOrderVOTestExample.build().atSchemaLocation(null)
						.billingAccount(null)
						.channel(null)
						.payment(null)
						.productOfferingQualification(null)
						.quote(null)
						.relatedParty(null)
						.externalId("otherId")));

		testEntries.add(Arguments.of("The notificationContact should have been updated.",
				ProductOrderCreateVOTestExample.build()
						.atSchemaLocation(null)
						.billingAccount(null),
				ProductOrderUpdateVOTestExample.build().atSchemaLocation(null)
						.billingAccount(null)
						.notificationContact("otherContact"),
				ProductOrderVOTestExample.build().atSchemaLocation(null)
						.billingAccount(null)
						.channel(null)
						.payment(null)
						.productOfferingQualification(null)
						.quote(null)
						.relatedParty(null)
						.notificationContact("otherContact")));

		testEntries.add(Arguments.of("The priority should have been updated.",
				ProductOrderCreateVOTestExample.build()
						.atSchemaLocation(null)
						.billingAccount(null),
				ProductOrderUpdateVOTestExample.build().atSchemaLocation(null)
						.billingAccount(null)
						.priority("2"),
				ProductOrderVOTestExample.build().atSchemaLocation(null)
						.billingAccount(null)
						.channel(null)
						.payment(null)
						.productOfferingQualification(null)
						.quote(null)
						.relatedParty(null)
						.priority("2")));

		testEntries.add(Arguments.of("The requestedStartDate should have been updated.",
				ProductOrderCreateVOTestExample.build()
						.atSchemaLocation(null)
						.billingAccount(null),
				ProductOrderUpdateVOTestExample.build().atSchemaLocation(null)
						.billingAccount(null)
						.requestedStartDate(date),
				ProductOrderVOTestExample.build().atSchemaLocation(null)
						.billingAccount(null)
						.channel(null)
						.payment(null)
						.productOfferingQualification(null)
						.quote(null)
						.relatedParty(null)
						.requestedStartDate(date)));

		testEntries.add(Arguments.of("The note should have been updated.",
				ProductOrderCreateVOTestExample.build()
						.atSchemaLocation(null)
						.billingAccount(null),
				ProductOrderUpdateVOTestExample.build().atSchemaLocation(null)
						.billingAccount(null)
						.note(List.of(NoteVOTestExample.build().atSchemaLocation(null).id("urn:note").text("my note"))),
				ProductOrderVOTestExample.build().atSchemaLocation(null)
						.billingAccount(null)
						.channel(null)
						.payment(null)
						.productOfferingQualification(null)
						.quote(null)
						.relatedParty(null)
						.note(List.of(NoteVOTestExample.build().atSchemaLocation(null).id("urn:note").text("my note")))));

		testEntries.add(Arguments.of("The state should have been updated.",
				ProductOrderCreateVOTestExample.build()
						.atSchemaLocation(null)
						.billingAccount(null),
				ProductOrderUpdateVOTestExample.build().atSchemaLocation(null)
						.billingAccount(null)
						.state(ProductOrderStateTypeVO.IN_PROGRESS),
				ProductOrderVOTestExample.build().atSchemaLocation(null)
						.billingAccount(null)
						.channel(null)
						.payment(null)
						.productOfferingQualification(null)
						.quote(null)
						.relatedParty(null)
						.state(ProductOrderStateTypeVO.IN_PROGRESS)));

		return testEntries.stream();
	}

	@ParameterizedTest
	@MethodSource("provideInvalidUpdates")
	public void patchProductOrder400(String message, ProductOrderUpdateVO invalidUpdateVO) throws Exception {
		this.message = message;
		this.productUpdateVO = invalidUpdateVO;
		patchProductOrder400();
	}

	@Override
	public void patchProductOrder400() throws Exception {
		//first create
		ProductOrderCreateVO productCreateVO = ProductOrderCreateVOTestExample.build().atSchemaLocation(null)
				.billingAccount(null);

		HttpResponse<ProductOrderVO> createResponse = callAndCatch(
				() -> productOrderApiTestClient.createProductOrder(null, productCreateVO));
		assertEquals(HttpStatus.CREATED, createResponse.getStatus(),
				"The product order should have been created first.");

		String productId = createResponse.body().getId();

		HttpResponse<ProductOrderVO> updateResponse = callAndCatch(
				() -> productOrderApiTestClient.patchProductOrder(null, productId, productUpdateVO));
		assertEquals(HttpStatus.BAD_REQUEST, updateResponse.getStatus(), message);

		Optional<ErrorDetails> optionalErrorDetails = updateResponse.getBody(ErrorDetails.class);
		assertTrue(optionalErrorDetails.isPresent(), "Some error details should be present.");
	}

	private static Stream<Arguments> provideInvalidUpdates() {
		List<Arguments> testEntries = new ArrayList<>();
		testEntries.add(Arguments.of("A product order with invalid related parties should not be updated.",
				ProductOrderUpdateVOTestExample.build().atSchemaLocation(null)
						.billingAccount(null)
						.relatedParty(List.of(RelatedPartyVOTestExample.build().atSchemaLocation(null)))));
		testEntries.add(Arguments.of("A product order with non-existent related parties should not be updated.",
				ProductOrderUpdateVOTestExample.build().atSchemaLocation(null)
						.billingAccount(null)
						.relatedParty(
								List.of((RelatedPartyVOTestExample.build().atSchemaLocation(null)
										.id("urn:ngsi-ld:organisation:non-existent"))))));

		testEntries.add(Arguments.of("A product order with an invalid agreement should not be updated.",
				ProductOrderUpdateVOTestExample.build().atSchemaLocation(null)
						.billingAccount(null)
						.agreement(List.of(AgreementRefVOTestExample.build().atSchemaLocation(null)))));
		testEntries.add(Arguments.of("A product order with a non-existent agreement should not be updated.",
				ProductOrderUpdateVOTestExample.build().atSchemaLocation(null)
						.billingAccount(null)
						.agreement(
								List.of(AgreementRefVOTestExample.build().atSchemaLocation(null)
										.id("urn:ngsi-ld:agreement:non-existent")))));

		testEntries.add(Arguments.of("A product order with an invalid billing account should not be updated.",
				ProductOrderUpdateVOTestExample.build().atSchemaLocation(null)
						.billingAccount(BillingAccountRefVOTestExample.build().atSchemaLocation(null))));
		testEntries.add(Arguments.of("A product order with non-existent billing account should not be updated.",
				ProductOrderUpdateVOTestExample.build().atSchemaLocation(null)
						.billingAccount(BillingAccountRefVOTestExample.build().atSchemaLocation(null)
								.id("urn:ngsi-ld:billing-account:non-existent"))));

		testEntries.add(Arguments.of("A product order with an invalid channel should not be updated.",
				ProductOrderUpdateVOTestExample.build().atSchemaLocation(null)
						.billingAccount(null)
						.channel(List.of(RelatedChannelVOTestExample.build().atSchemaLocation(null)))));
		testEntries.add(Arguments.of("A product order with a non-existent channel should not be updated.",
				ProductOrderUpdateVOTestExample.build().atSchemaLocation(null)
						.billingAccount(null)
						.channel(
								List.of(RelatedChannelVOTestExample.build().atSchemaLocation(null).id("urn:ngsi-ld:channel:non-existent")))));

		testEntries.add(Arguments.of("A product order with an invalid payment should not be updated.",
				ProductOrderUpdateVOTestExample.build().atSchemaLocation(null)
						.billingAccount(null)
						.payment(List.of(PaymentRefVOTestExample.build().atSchemaLocation(null)))));
		testEntries.add(Arguments.of("A product order with a non-existent payment should not be updated.",
				ProductOrderUpdateVOTestExample.build().atSchemaLocation(null)
						.billingAccount(null)
						.payment(List.of(PaymentRefVOTestExample.build().atSchemaLocation(null).id("urn:ngsi-ld:payment:non-existent")))));

		testEntries.add(Arguments.of("A product order with an invalid payment should not be updated.",
				ProductOrderUpdateVOTestExample.build().atSchemaLocation(null)
						.billingAccount(null)
						.productOfferingQualification(
								List.of(ProductOfferingQualificationRefVOTestExample.build().atSchemaLocation(null)))));
		testEntries.add(Arguments.of("A product order with a non-existent payment should not be updated.",
				ProductOrderUpdateVOTestExample.build().atSchemaLocation(null)
						.billingAccount(null)
						.productOfferingQualification(
								List.of(ProductOfferingQualificationRefVOTestExample.build().atSchemaLocation(null)
										.id("urn:ngsi-ld:product-offering-qualification:non-existent")))));

		testEntries.add(Arguments.of("A product order with an invalid quote should not be updated.",
				ProductOrderUpdateVOTestExample.build().atSchemaLocation(null)
						.billingAccount(null)
						.quote(
								List.of(QuoteRefVOTestExample.build().atSchemaLocation(null)))));
		testEntries.add(Arguments.of("A product order with a non-existent quote should not be updated.",
				ProductOrderUpdateVOTestExample.build().atSchemaLocation(null)
						.billingAccount(null)
						.quote(
								List.of(QuoteRefVOTestExample.build().atSchemaLocation(null)
										.id("urn:ngsi-ld:quote:non-existent")))));

		provideInvalidPrices().forEach(invalidPrice ->
				testEntries.add(
						Arguments.of(invalidPrice.message(),
								ProductOrderUpdateVOTestExample.build().atSchemaLocation(null).billingAccount(null)
										.orderTotalPrice(invalidPrice.value()))
				));

		provideInvalidOrderItems().filter(invalidOrderItem ->
				testEntries.add(
						Arguments.of(invalidOrderItem.message(),
								ProductOrderUpdateVOTestExample.build().atSchemaLocation(null).billingAccount(null)
										.productOrderItem(invalidOrderItem.value()))
				));

		return testEntries.stream();
	}

	@Disabled("Security is handled externally, thus 401 and 403 cannot happen.")
	@Test
	@Override
	public void patchProductOrder401() throws Exception {

	}

	@Disabled("Security is handled externally, thus 401 and 403 cannot happen.")
	@Test
	@Override
	public void patchProductOrder403() throws Exception {

	}

	@Test
	@Override
	public void patchProductOrder404() throws Exception {
		ProductOrderUpdateVO productUpdateVO = ProductOrderUpdateVOTestExample.build()
				.atSchemaLocation(null)
				.billingAccount(null);
		assertEquals(
				HttpStatus.NOT_FOUND,
				callAndCatch(
						() -> productOrderApiTestClient.patchProductOrder(null, "urn:ngsi-ld:product-order:not-existent",
								productUpdateVO)).getStatus(),
				"Non existent product order should not be updated.");
	}

	@Disabled("Prohibited by the framework.")
	@Test
	@Override
	public void patchProductOrder405() throws Exception {

	}

	@Override
	public void patchProductOrder409() throws Exception {
		// TODO: can this happen?
	}

	@Override
	public void patchProductOrder500() throws Exception {

	}

	@ParameterizedTest
	@MethodSource("provideFieldParameters")
	public void retrieveProductOrder200(String message, String fields, ProductOrderVO expectedProduct)
			throws Exception {
		this.fieldsParameter = fields;
		this.message = message;
		this.expectedProduct = expectedProduct;
		retrieveProductOrder200();
	}

	@Override
	public void retrieveProductOrder200() throws Exception {

		ProductOrderCreateVO productCreateVO = ProductOrderCreateVOTestExample.build().atSchemaLocation(null)
				.billingAccount(null);
		HttpResponse<ProductOrderVO> createResponse = callAndCatch(
				() -> productOrderApiTestClient.createProductOrder(null, productCreateVO));
		assertEquals(HttpStatus.CREATED, createResponse.getStatus(), message);
		String id = createResponse.body().getId();

		expectedProduct
				.id(id)
				.href(id);

		//then retrieve
		HttpResponse<ProductOrderVO> retrievedRF = callAndCatch(
				() -> productOrderApiTestClient.retrieveProductOrder(null, id, fieldsParameter));
		assertEquals(HttpStatus.OK, retrievedRF.getStatus(), message);
		assertEquals(expectedProduct, retrievedRF.body(), message);
	}

	private static Stream<Arguments> provideFieldParameters() {
		return Stream.of(
				Arguments.of("Without a fields parameter everything should be returned.", null,
						ProductOrderVOTestExample.build().atSchemaLocation(null)
								.billingAccount(null)
								.channel(null)
								.payment(null)
								.productOfferingQualification(null)
								.quote(null)
								.relatedParty(null)),
				Arguments.of("Only description and the mandatory parameters should have been included.", "description",
						ProductOrderVOTestExample.build().atSchemaLocation(null)
								.cancellationDate(null)
								.cancellationReason(null)
								.category(null)
								.completionDate(null)
								.expectedCompletionDate(null)
								.requestedCompletionDate(null)
								.externalId(null)
								.notificationContact(null)
								.priority(null)
								.requestedStartDate(null)
								.agreement(null)
								.billingAccount(null)
								.channel(null)
								.note(null)
								.orderTotalPrice(null)
								.payment(null)
								.productOfferingQualification(null)
								.quote(null)
								.relatedParty(null)
								.state(null)
								.atBaseType(null)
								.atType(null)
								.atSchemaLocation(null)),
				Arguments.of(
						"Only the mandatory parameters should have been included when a non-existent field was requested.",
						"nothingToSeeHere", ProductOrderVOTestExample.build().atSchemaLocation(null)
								.description(null)
								.cancellationDate(null)
								.cancellationReason(null)
								.category(null)
								.completionDate(null)
								.expectedCompletionDate(null)
								.requestedCompletionDate(null)
								.externalId(null)
								.notificationContact(null)
								.priority(null)
								.requestedStartDate(null)
								.agreement(null)
								.billingAccount(null)
								.channel(null)
								.note(null)
								.orderTotalPrice(null)
								.payment(null)
								.productOfferingQualification(null)
								.quote(null)
								.relatedParty(null)
								.state(null)
								.atBaseType(null)
								.atType(null)
								.atSchemaLocation(null)),
				Arguments.of("Only description, externalId and the mandatory parameters should have been included.",
						"description,externalId", ProductOrderVOTestExample.build().atSchemaLocation(null)
								.cancellationDate(null)
								.cancellationReason(null)
								.category(null)
								.completionDate(null)
								.expectedCompletionDate(null)
								.requestedCompletionDate(null)
								.notificationContact(null)
								.priority(null)
								.requestedStartDate(null)
								.agreement(null)
								.billingAccount(null)
								.channel(null)
								.note(null)
								.orderTotalPrice(null)
								.payment(null)
								.productOfferingQualification(null)
								.quote(null)
								.relatedParty(null)
								.state(null)
								.atBaseType(null)
								.atType(null)
								.atSchemaLocation(null)));
	}

	@Disabled("400 cannot happen, only 404")
	@Test
	@Override
	public void retrieveProductOrder400() throws Exception {

	}

	@Disabled("Security is handled externally, thus 401 and 403 cannot happen.")
	@Test
	@Override
	public void retrieveProductOrder401() throws Exception {

	}

	@Disabled("Security is handled externally, thus 401 and 403 cannot happen.")
	@Test
	@Override
	public void retrieveProductOrder403() throws Exception {

	}

	@Test
	@Override
	public void retrieveProductOrder404() throws Exception {
		HttpResponse<ProductOrderVO> response = callAndCatch(
				() -> productOrderApiTestClient.retrieveProductOrder(null, "urn:ngsi-ld:product-function:non-existent",
						null));
		assertEquals(HttpStatus.NOT_FOUND, response.getStatus(), "No such product-catalog should exist.");

		Optional<ErrorDetails> optionalErrorDetails = response.getBody(ErrorDetails.class);
		assertTrue(optionalErrorDetails.isPresent(), "Error details should have been provided.");
	}

	@Disabled("Prohibited by the framework.")
	@Test
	@Override
	public void retrieveProductOrder405() throws Exception {

	}

	@Disabled("Conflict not possible on retrieval")
	@Test
	@Override
	public void retrieveProductOrder409() throws Exception {

	}

	@Override
	public void retrieveProductOrder500() throws Exception {

	}

	@Override
	protected String getEntityType() {
		return ProductOrder.TYPE_PRODUCT_ORDER;
	}
}

