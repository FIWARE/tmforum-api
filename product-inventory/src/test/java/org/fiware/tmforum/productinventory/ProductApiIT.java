package org.fiware.tmforum.productinventory;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import io.micronaut.test.annotation.MockBean;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import org.fiware.ngsi.api.EntitiesApiClient;
import org.fiware.productinventory.api.ProductApiTestClient;
import org.fiware.productinventory.api.ProductApiTestSpec;
import org.fiware.productinventory.model.*;
import org.fiware.tmforum.common.configuration.GeneralProperties;
import org.fiware.tmforum.common.exception.ErrorDetails;
import org.fiware.tmforum.common.notification.TMForumEventHandler;
import org.fiware.tmforum.common.test.AbstractApiIT;
import org.fiware.tmforum.common.test.ArgumentPair;
import org.fiware.tmforum.product.Product;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import reactor.core.publisher.Mono;

import java.net.URI;
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

@MicronautTest(packages = {"org.fiware.tmforum.productinventory"})
public class ProductApiIT extends AbstractApiIT implements ProductApiTestSpec {

	public final ProductApiTestClient productApiTestClient;

	private String message;
	private String fieldsParameter;
	private ProductCreateVO productCreateVO;
	private ProductUpdateVO productUpdateVO;
	private ProductVO expectedProduct;

	public ProductApiIT(ProductApiTestClient productApiTestClient, EntitiesApiClient entitiesApiClient,
						ObjectMapper objectMapper, GeneralProperties generalProperties) {
		super(entitiesApiClient, objectMapper, generalProperties);
		this.productApiTestClient = productApiTestClient;
	}

	@MockBean(TMForumEventHandler.class)
	public TMForumEventHandler eventHandler() {
		TMForumEventHandler eventHandler = mock(TMForumEventHandler.class);

		when(eventHandler.handleCreateEvent(any())).thenReturn(Mono.empty());
		when(eventHandler.handleUpdateEvent(any(), any())).thenReturn(Mono.empty());

		return eventHandler;
	}

	@ParameterizedTest
	@MethodSource("provideValidProducts")
	public void createProduct201(String message, ProductCreateVO productCreateVO, ProductVO expectedProduct)
			throws Exception {
		this.message = message;
		this.productCreateVO = productCreateVO;
		this.expectedProduct = expectedProduct;
		createProduct201();
	}

	@Override
	public void createProduct201() throws Exception {

		HttpResponse<ProductVO> productVOHttpResponse = callAndCatch(
				() -> productApiTestClient.createProduct(null, productCreateVO));
		assertEquals(HttpStatus.CREATED, productVOHttpResponse.getStatus(), message);
		String rfId = productVOHttpResponse.body().getId();
		Instant lastUpdate = productVOHttpResponse.body().getLastUpdate();
		expectedProduct.setId(rfId);
		expectedProduct.setHref(rfId);
		expectedProduct.setLastUpdate(lastUpdate);

		assertEquals(expectedProduct, productVOHttpResponse.body(), message);
	}

	private static Stream<Arguments> provideValidProducts() {
		List<Arguments> testEntries = new ArrayList<>();

		testEntries.add(
				Arguments.of("An empty product should have been created.",
						ProductCreateVOTestExample.build().atSchemaLocation(null)
								.productSpecification(null)
								.billingAccount(null)
								.productOffering(null),
						ProductVOTestExample.build().atSchemaLocation(null)
								.productSpecification(null)
								.billingAccount(null)
								.productOffering(null)));

		Instant start = Instant.now();
		Instant end = Instant.now();
		testEntries.add(
				Arguments.of("A product with operating times should have been created.",
						ProductCreateVOTestExample.build().atSchemaLocation(null)
								.productSpecification(null)
								.billingAccount(null)
								.productOffering(null)
								.startDate(start).terminationDate(end),
						ProductVOTestExample.build().atSchemaLocation(null).productSpecification(null)
								.billingAccount(null)
								.productOffering(null)
								.startDate(start).terminationDate(end)));

		testEntries.add(
				Arguments.of("A product with characteristic should have been created.",
						ProductCreateVOTestExample.build().atSchemaLocation(null)
								.productSpecification(null)
								.billingAccount(null)
								.productOffering(null)
								.productCharacteristic(List.of(CharacteristicVOTestExample.build().atSchemaLocation(null))),
						ProductVOTestExample.build().atSchemaLocation(null).productSpecification(null)
								.billingAccount(null)
								.productOffering(null)
								.productCharacteristic(List.of(CharacteristicVOTestExample.build().atSchemaLocation(null)))));
		testEntries.add(
				Arguments.of("A product with a price should have been created.",
						ProductCreateVOTestExample.build().atSchemaLocation(null)
								.productSpecification(null)
								.billingAccount(null)
								.productOffering(null)
								.productPrice(List.of(ProductPriceVOTestExample.build().atSchemaLocation(null)
										.price(null)
										.productOfferingPrice(null)
										.billingAccount(null))),
						ProductVOTestExample.build().atSchemaLocation(null).productSpecification(null)
								.billingAccount(null)
								.productOffering(null)
								.productPrice(List.of(ProductPriceVOTestExample.build().atSchemaLocation(null)
										.price(null)
										.productOfferingPrice(null)
										.billingAccount(null)))));
		testEntries.add(
				Arguments.of("A product with a term should have been created.",
						ProductCreateVOTestExample.build().atSchemaLocation(null)
								.productSpecification(null)
								.billingAccount(null)
								.productOffering(null)
								.productTerm(List.of(ProductTermVOTestExample.build().atSchemaLocation(null))),
						ProductVOTestExample.build().atSchemaLocation(null).productSpecification(null)
								.billingAccount(null)
								.productOffering(null)
								.productTerm(List.of(ProductTermVOTestExample.build().atSchemaLocation(null)))));
		testEntries.add(
				Arguments.of("A product with a status should have been created.",
						ProductCreateVOTestExample.build().atSchemaLocation(null)
								.productSpecification(null)
								.billingAccount(null)
								.productOffering(null)
								.status(ProductStatusTypeVO.ACTIVE),
						ProductVOTestExample.build().atSchemaLocation(null).productSpecification(null)
								.billingAccount(null)
								.productOffering(null)
								.status(ProductStatusTypeVO.ACTIVE)));
		return testEntries.stream();
	}

	@ParameterizedTest
	@MethodSource("provideInvalidProducts")
	public void createProduct400(String message, ProductCreateVO invalidCreateVO) throws Exception {
		this.message = message;
		this.productCreateVO = invalidCreateVO;
		createProduct400();
	}

	@Override
	public void createProduct400() throws Exception {
		HttpResponse<ProductVO> creationResponse = callAndCatch(
				() -> productApiTestClient.createProduct(null, productCreateVO));
		assertEquals(HttpStatus.BAD_REQUEST, creationResponse.getStatus(), message);
		Optional<ErrorDetails> optionalErrorDetails = creationResponse.getBody(ErrorDetails.class);
		assertTrue(optionalErrorDetails.isPresent(), "Error details should be provided.");
	}

	private static Stream<Arguments> provideInvalidProducts() {
		List<Arguments> testEntries = new ArrayList<>();

		testEntries.add(Arguments.of("A product with invalid related parties should not be created.",
				ProductCreateVOTestExample.build().atSchemaLocation(null)
						.productSpecification(null)
						.billingAccount(null)
						.productOffering(null)
						.relatedParty(List.of(RelatedPartyVOTestExample.build().atSchemaLocation(null)))));
		testEntries.add(Arguments.of("A product with non-existent related parties should not be created.",
				ProductCreateVOTestExample.build().atSchemaLocation(null)
						.productSpecification(null)
						.billingAccount(null)
						.productOffering(null)
						.relatedParty(
								List.of((RelatedPartyVOTestExample.build().atSchemaLocation(null)
										.id("urn:ngsi-ld:organisation:non-existent"))))));

		testEntries.add(Arguments.of("A product with an invalid place ref should not be created.",
				ProductCreateVOTestExample.build().atSchemaLocation(null)
						.productSpecification(null)
						.billingAccount(null)
						.productOffering(null)
						.place(List.of(RelatedPlaceRefOrValueVOTestExample.build().atSchemaLocation(null)))));
		testEntries.add(Arguments.of("A product with non-existent place ref should not be created.",
				ProductCreateVOTestExample.build().atSchemaLocation(null)
						.productSpecification(null)
						.billingAccount(null)
						.productOffering(null)
						.place(List.of(
								RelatedPlaceRefOrValueVOTestExample.build().atSchemaLocation(null).id("urn:ngsi-ld:place:non-existent")))));

		testEntries.add(Arguments.of("A product with an invalid product ref should not be created.",
				ProductCreateVOTestExample.build().atSchemaLocation(null)
						.billingAccount(null)
						.productOffering(null)
						.productSpecification(ProductSpecificationRefVOTestExample.build().atSchemaLocation(null))));
		testEntries.add(Arguments.of("A product with non-existent product ref should not be created.",
				ProductCreateVOTestExample.build().atSchemaLocation(null)
						.billingAccount(null)
						.productOffering(null)
						.productSpecification(
								ProductSpecificationRefVOTestExample.build().atSchemaLocation(null)
										.id("urn:ngsi-ld:product-specification:non-existent"))));

		testEntries.add(Arguments.of("A product with an invalid billing account ref should not be created.",
				ProductCreateVOTestExample.build().atSchemaLocation(null)
						.billingAccount(BillingAccountRefVOTestExample.build().atSchemaLocation(null))
						.productOffering(null)
						.productSpecification(null)));
		testEntries.add(Arguments.of("A product with non-existent billing account ref should not be created.",
				ProductCreateVOTestExample.build().atSchemaLocation(null)
						.billingAccount(BillingAccountRefVOTestExample.build().atSchemaLocation(null)
								.id("urn:ngsi-ld:billing-account:non-existent"))
						.productOffering(null)
						.productSpecification(null)));

		testEntries.add(Arguments.of("A product with an invalid product offering ref should not be created.",
				ProductCreateVOTestExample.build().atSchemaLocation(null)
						.billingAccount(null)
						.productOffering(ProductOfferingRefVOTestExample.build().atSchemaLocation(null))
						.productSpecification(null)));
		testEntries.add(Arguments.of("A product with non-existent product offering ref should not be created.",
				ProductCreateVOTestExample.build().atSchemaLocation(null)
						.billingAccount(null)
						.productOffering(
								ProductOfferingRefVOTestExample.build().atSchemaLocation(null).id("urn:ngsi-ld:product-offering:non-existent"))
						.productSpecification(null)));

		testEntries.add(Arguments.of("A product with an invalid agreement ref should not be created.",
				ProductCreateVOTestExample.build().atSchemaLocation(null)
						.agreement(List.of(AgreementItemRefVOTestExample.build().atSchemaLocation(null)))
						.billingAccount(null)
						.productOffering(null)
						.productSpecification(null)));
		testEntries.add(Arguments.of("A product with non-existent agreement ref should not be created.",
				ProductCreateVOTestExample.build().atSchemaLocation(null)
						.agreement(
								List.of(AgreementItemRefVOTestExample.build().atSchemaLocation(null).id("urn:ngsi-ld:agreement:non-existent")))
						.billingAccount(null)
						.productOffering(null)
						.productSpecification(null)));

		testEntries.add(Arguments.of("A product with an invalid product order ref should not be created.",
				ProductCreateVOTestExample.build().atSchemaLocation(null)
						.productOrderItem(List.of(RelatedProductOrderItemVOTestExample.build().atSchemaLocation(null)))
						.billingAccount(null)
						.productOffering(null)
						.productSpecification(null)));
		testEntries.add(Arguments.of("A product with non-existent product order ref should not be created.",
				ProductCreateVOTestExample.build().atSchemaLocation(null)
						.productOrderItem(
								List.of(RelatedProductOrderItemVOTestExample.build().atSchemaLocation(null)
										.productOrderId("urn:ngsi-ld:product-order:non-existent")))
						.billingAccount(null)
						.productOffering(null)
						.productSpecification(null)));

		testEntries.add(Arguments.of("A product with an invalid product order ref should not be created.",
				ProductCreateVOTestExample.build().atSchemaLocation(null)
						.productOrderItem(List.of(RelatedProductOrderItemVOTestExample.build().atSchemaLocation(null)))
						.billingAccount(null)
						.productOffering(null)
						.productSpecification(null)));
		testEntries.add(Arguments.of("A product with non-existent product order ref should not be created.",
				ProductCreateVOTestExample.build().atSchemaLocation(null)
						.productOrderItem(
								List.of(RelatedProductOrderItemVOTestExample.build().atSchemaLocation(null)
										.productOrderId("urn:ngsi-ld:product-order:non-existent")))
						.billingAccount(null)
						.productOffering(null)
						.productSpecification(null)));

		provideInvalidPrices().forEach(invalidPrice ->
				testEntries.add(
						Arguments.of(invalidPrice.message(), ProductCreateVOTestExample.build().atSchemaLocation(null).billingAccount(null)
								.productOffering(null)
								.productSpecification(null)
								.productPrice(invalidPrice.value()))
				));

		testEntries.add(Arguments.of("A product with an invalid product relationship should not be created.",
				ProductCreateVOTestExample.build().atSchemaLocation(null)
						.productRelationship(
								List.of(ProductRelationshipVOTestExample.build().atSchemaLocation(null)
										.product(ProductRefOrValueVOTestExample.build().atSchemaLocation(null))))
						.billingAccount(null)
						.productOffering(null)
						.productSpecification(null)));
		testEntries.add(Arguments.of("A product with non-existent product relationship should not be created.",
				ProductCreateVOTestExample.build().atSchemaLocation(null)
						.productRelationship(
								List.of(ProductRelationshipVOTestExample.build().atSchemaLocation(null)
										.product(ProductRefOrValueVOTestExample.build().atSchemaLocation(null)
												.id("urn:ngsi-ld:product:non-existent"))))
						.billingAccount(null)
						.productOffering(null)
						.productSpecification(null)));

		testEntries.add(Arguments.of("A product with an invalid resource ref should not be created.",
				ProductCreateVOTestExample.build().atSchemaLocation(null)
						.realizingResource(List.of(ResourceRefVOTestExample.build().atSchemaLocation(null)))
						.billingAccount(null)
						.productOffering(null)
						.productSpecification(null)));
		testEntries.add(Arguments.of("A product with non-existent resource ref should not be created.",
				ProductCreateVOTestExample.build().atSchemaLocation(null)
						.realizingResource(
								List.of(ResourceRefVOTestExample.build().atSchemaLocation(null).id("urn:ngsi-ld:resource:non-existent")))
						.billingAccount(null)
						.productOffering(null)
						.productSpecification(null)));

		testEntries.add(Arguments.of("A product with an invalid service ref should not be created.",
				ProductCreateVOTestExample.build().atSchemaLocation(null)
						.realizingService(List.of(ServiceRefVOTestExample.build().atSchemaLocation(null)))
						.billingAccount(null)
						.productOffering(null)
						.productSpecification(null)));
		testEntries.add(Arguments.of("A product with non-existent service ref should not be created.",
				ProductCreateVOTestExample.build().atSchemaLocation(null)
						.realizingService(
								List.of(ServiceRefVOTestExample.build().atSchemaLocation(null).id("urn:ngsi-ld:service:non-existent")))
						.billingAccount(null)
						.productOffering(null)
						.productSpecification(null)));
		return testEntries.stream();
	}

	private static Stream<ArgumentPair<List<ProductPriceVO>>> provideInvalidPrices() {
		List<ArgumentPair<List<ProductPriceVO>>> invalidPrices = new ArrayList<>();

		invalidPrices.add(
				new ArgumentPair<>("A price with an invalid billing account should not be accepted",
						List.of(ProductPriceVOTestExample.build().atSchemaLocation(null)
								.billingAccount(BillingAccountRefVOTestExample.build().atSchemaLocation(null).id("invalid"))
								.productOfferingPrice(null))));
		invalidPrices.add(
				new ArgumentPair<>("A price with an non-existent billing account should not be accepted",
						List.of(ProductPriceVOTestExample.build().atSchemaLocation(null)
								.billingAccount(BillingAccountRefVOTestExample.build().atSchemaLocation(null)
										.id("urn:ngsi-ld:billing-account:non-existent"))
								.productOfferingPrice(null))));
		invalidPrices.add(
				new ArgumentPair<>("A price with an invalid product offering price should not be accepted",
						List.of(ProductPriceVOTestExample.build().atSchemaLocation(null)
								.productOfferingPrice(ProductOfferingPriceRefVOTestExample.build().atSchemaLocation(null).id("invalid"))
								.billingAccount(null))));
		invalidPrices.add(
				new ArgumentPair<>("A price with an non-existent product offering price should not be accepted",
						List.of(ProductPriceVOTestExample.build().atSchemaLocation(null)
								.productOfferingPrice(ProductOfferingPriceRefVOTestExample.build().atSchemaLocation(null)
										.id("urn:ngsi-ld:product-offering-price:non-existent"))
								.billingAccount(null))));

		invalidPrices.add(
				new ArgumentPair<>("A price with an invalid price alteration should not be accepted",
						List.of(ProductPriceVOTestExample.build().atSchemaLocation(null)
								.productOfferingPrice(null)
								.billingAccount(null)
								.productPriceAlteration(List.of(PriceAlterationVOTestExample.build().atSchemaLocation(null))))));
		invalidPrices.add(
				new ArgumentPair<>("A price with an non-existent price alteration internal ref should not be accepted",
						List.of(ProductPriceVOTestExample.build().atSchemaLocation(null)
								.productOfferingPrice(null)
								.billingAccount(null)
								.productPriceAlteration(List.of(PriceAlterationVOTestExample.build().atSchemaLocation(null)
										.productOfferingPrice(ProductOfferingPriceRefVOTestExample.build().atSchemaLocation(null)
												.id("urn:ngsi-ld:product-offering-price:non-existent")))))));

		return invalidPrices.stream();
	}

	@Disabled("Security is handled externally, thus 401 and 403 cannot happen.")
	@Test
	@Override
	public void createProduct401() throws Exception {

	}

	@Disabled("Security is handled externally, thus 401 and 403 cannot happen.")
	@Test
	@Override
	public void createProduct403() throws Exception {

	}

	@Disabled("Prohibited by the framework.")
	@Test
	@Override
	public void createProduct405() throws Exception {
	}

	@Disabled("No implicit creation, impossible state.")
	@Test
	@Override
	public void createProduct409() throws Exception {

	}

	@Override
	public void createProduct500() throws Exception {

	}

	@Test
	@Override
	public void deleteProduct204() throws Exception {
		ProductCreateVO emptyCreate = ProductCreateVOTestExample.build().atSchemaLocation(null)
				.productSpecification(null)
				.billingAccount(null)
				.productOffering(null);

		HttpResponse<ProductVO> createResponse = productApiTestClient.createProduct(null, emptyCreate);
		assertEquals(HttpStatus.CREATED, createResponse.getStatus(), "The product should have been created first.");

		String rfId = createResponse.body().getId();

		assertEquals(HttpStatus.NO_CONTENT,
				callAndCatch(() -> productApiTestClient.deleteProduct(null, rfId)).getStatus(),
				"The product should have been deleted.");

		assertEquals(HttpStatus.NOT_FOUND,
				callAndCatch(() -> productApiTestClient.retrieveProduct(null, rfId, null)).status(),
				"The product should not exist anymore.");
	}

	@Disabled("400 is impossible to happen on deletion with the current implementation.")
	@Test
	@Override
	public void deleteProduct400() throws Exception {

	}

	@Disabled("Security is handled externally, thus 401 and 403 cannot happen.")
	@Test
	@Override
	public void deleteProduct401() throws Exception {

	}

	@Disabled("Security is handled externally, thus 401 and 403 cannot happen.")
	@Test
	@Override
	public void deleteProduct403() throws Exception {

	}

	@Test
	@Override
	public void deleteProduct404() throws Exception {
		HttpResponse<?> notFoundResponse = callAndCatch(
				() -> productApiTestClient.deleteProduct(null, "urn:ngsi-ld:product-catalog:no-pop"));
		assertEquals(HttpStatus.NOT_FOUND,
				notFoundResponse.getStatus(),
				"No such product-catalog should exist.");

		Optional<ErrorDetails> optionalErrorDetails = notFoundResponse.getBody(ErrorDetails.class);
		assertTrue(optionalErrorDetails.isPresent(), "Error details should be provided.");

		notFoundResponse = callAndCatch(() -> productApiTestClient.deleteProduct(null, "invalid-id"));
		assertEquals(HttpStatus.NOT_FOUND,
				notFoundResponse.getStatus(),
				"No such product-catalog should exist.");

		optionalErrorDetails = notFoundResponse.getBody(ErrorDetails.class);
		assertTrue(optionalErrorDetails.isPresent(), "Error details should be provided.");
	}

	@Disabled("Prohibited by the framework.")
	@Test
	@Override
	public void deleteProduct405() throws Exception {

	}

	@Disabled("Impossible status.")
	@Test
	@Override
	public void deleteProduct409() throws Exception {

	}

	@Override
	public void deleteProduct500() throws Exception {

	}

	@Test
	@Override
	public void listProduct200() throws Exception {

		List<ProductVO> expectedProducts = new ArrayList<>();
		for (int i = 0; i < 10; i++) {
			ProductCreateVO productCreateVO = ProductCreateVOTestExample.build().atSchemaLocation(null)
					.productSpecification(null)
					.billingAccount(null)
					.productOffering(null);
			ProductVO body = productApiTestClient.createProduct(null, productCreateVO).body();
			String id = body.getId();
			Instant lastUpdate = body.getLastUpdate();
			ProductVO productVO = ProductVOTestExample.build().atSchemaLocation(null);
			productVO
					.id(id)
					.href(id)
					.lastUpdate(lastUpdate)
					.productSpecification(null)
					.billingAccount(null)
					.productOffering(null)
					.agreement(null)
					.product(null)
					.place(null)
					.relatedParty(null)
					.productOrderItem(null)
					.realizingResource(null)
					.realizingService(null);
			expectedProducts.add(productVO);
		}

		HttpResponse<List<ProductVO>> productResponse = callAndCatch(
				() -> productApiTestClient.listProduct(null, null, null, null));

		assertEquals(HttpStatus.OK, productResponse.getStatus(), "The list should be accessible.");
		assertEquals(expectedProducts.size(), productResponse.getBody().get().size(),
				"All products should have been returned.");
		List<ProductVO> retrievedProducts = productResponse.getBody().get();

		Map<String, ProductVO> retrievedMap = retrievedProducts.stream()
				.collect(Collectors.toMap(product -> product.getId(),
						product -> product));

		expectedProducts.stream()
				.forEach(
						expectedProduct -> assertTrue(
								retrievedMap.containsKey(expectedProduct.getId()),
								String.format("All created products should be returned - Missing: %s.",
										expectedProduct,
										retrievedProducts)));
		expectedProducts.stream().forEach(
				expectedProduct -> assertEquals(expectedProduct,
						retrievedMap.get(expectedProduct.getId()),
						"The correct products should be retrieved."));

		// get with pagination
		Integer limit = 5;
		HttpResponse<List<ProductVO>> firstPartResponse = callAndCatch(
				() -> productApiTestClient.listProduct(null, null, 0, limit));
		assertEquals(limit, firstPartResponse.body().size(),
				"Only the requested number of entries should be returend.");
		HttpResponse<List<ProductVO>> secondPartResponse = callAndCatch(
				() -> productApiTestClient.listProduct(null, null, 0 + limit, limit));
		assertEquals(limit, secondPartResponse.body().size(),
				"Only the requested number of entries should be returend.");

		retrievedProducts.clear();
		retrievedProducts.addAll(firstPartResponse.body());
		retrievedProducts.addAll(secondPartResponse.body());
		expectedProducts.stream()
				.forEach(
						expectedProduct -> assertTrue(
								retrievedMap.containsKey(expectedProduct.getId()),
								String.format("All created products should be returned - Missing: %s.",
										expectedProduct)));
		expectedProducts.stream().forEach(
				expectedProduct -> assertEquals(expectedProduct,
						retrievedMap.get(expectedProduct.getId()),
						"The correct products should be retrieved."));
	}

	@Test
	@Override
	public void listProduct400() throws Exception {
		HttpResponse<List<ProductVO>> badRequestResponse = callAndCatch(
				() -> productApiTestClient.listProduct(null, null, -1, null));
		assertEquals(HttpStatus.BAD_REQUEST,
				badRequestResponse.getStatus(),
				"Negative offsets are impossible.");

		Optional<ErrorDetails> optionalErrorDetails = badRequestResponse.getBody(ErrorDetails.class);
		assertTrue(optionalErrorDetails.isPresent(), "Error details should be provided.");

		badRequestResponse = callAndCatch(() -> productApiTestClient.listProduct(null, null, null, -1));
		assertEquals(HttpStatus.BAD_REQUEST,
				badRequestResponse.getStatus(),
				"Negative limits are impossible.");
		optionalErrorDetails = badRequestResponse.getBody(ErrorDetails.class);
		assertTrue(optionalErrorDetails.isPresent(), "Error details should be provided.");
	}

	@Disabled("Security is handled externally, thus 401 and 403 cannot happen.")
	@Test
	@Override
	public void listProduct401() throws Exception {

	}

	@Disabled("Security is handled externally, thus 401 and 403 cannot happen.")
	@Test
	@Override
	public void listProduct403() throws Exception {

	}

	@Disabled("Not found is not possible here, will be answered with an empty list instead.")
	@Test
	@Override
	public void listProduct404() throws Exception {

	}

	@Disabled("Prohibited by the framework.")
	@Test
	@Override
	public void listProduct405() throws Exception {

	}

	@Disabled("Impossible status.")
	@Test
	@Override
	public void listProduct409() throws Exception {

	}

	@Override
	public void listProduct500() throws Exception {

	}

	@ParameterizedTest
	@MethodSource("provideProductUpdates")
	public void patchProduct200(String message, ProductUpdateVO productUpdateVO, ProductVO expectedProduct)
			throws Exception {
		this.message = message;
		this.productUpdateVO = productUpdateVO;
		this.expectedProduct = expectedProduct;
		patchProduct200();
	}

	@Override
	public void patchProduct200() throws Exception {
		//first create
		ProductCreateVO productCreateVO = ProductCreateVOTestExample.build().atSchemaLocation(null)
				.productSpecification(null)
				.billingAccount(null)
				.productOffering(null);

		HttpResponse<ProductVO> createResponse = callAndCatch(
				() -> productApiTestClient.createProduct(null, productCreateVO));
		assertEquals(HttpStatus.CREATED, createResponse.getStatus(),
				"The product function should have been created first.");

		String productId = createResponse.body().getId();

		HttpResponse<ProductVO> updateResponse = callAndCatch(
				() -> productApiTestClient.patchProduct(null, productId, productUpdateVO));
		assertEquals(HttpStatus.OK, updateResponse.getStatus(), message);

		ProductVO updatedProduct = updateResponse.body();
		Instant lastUpdate = updatedProduct.getLastUpdate();
		expectedProduct.href(productId).id(productId).lastUpdate(lastUpdate);

		assertEquals(expectedProduct, updatedProduct, message);
	}

	private static Stream<Arguments> provideProductUpdates() {
		List<Arguments> testEntries = new ArrayList<>();

		testEntries.add(Arguments.of("The description should have been updated.",
				ProductUpdateVOTestExample.build().atSchemaLocation(null)
						.billingAccount(null)
						.productOffering(null)
						.productSpecification(null)
						.description("new-description"),
				ProductVOTestExample.build().atSchemaLocation(null)
						.billingAccount(null)
						.productOffering(null)
						.productSpecification(null)
						.agreement(null)
						.product(null)
						.place(null)
						.relatedParty(null)
						.productOrderItem(null)
						.realizingResource(null)
						.realizingService(null)
						.description("new-description")));

		testEntries.add(Arguments.of("The name should have been updated.",
				ProductUpdateVOTestExample.build().atSchemaLocation(null)
						.billingAccount(null)
						.productOffering(null)
						.productSpecification(null)
						.name("new-name"),
				ProductVOTestExample.build().atSchemaLocation(null)
						.billingAccount(null)
						.productOffering(null)
						.productSpecification(null)
						.agreement(null)
						.product(null)
						.place(null)
						.relatedParty(null)
						.productOrderItem(null)
						.realizingResource(null)
						.realizingService(null)
						.name("new-name")));

		testEntries.add(Arguments.of("The isBundle should have been updated.",
				ProductUpdateVOTestExample.build().atSchemaLocation(null)
						.billingAccount(null)
						.productOffering(null)
						.productSpecification(null)
						.isBundle(false),
				ProductVOTestExample.build().atSchemaLocation(null)
						.billingAccount(null)
						.productOffering(null)
						.productSpecification(null)
						.agreement(null)
						.product(null)
						.place(null)
						.relatedParty(null)
						.productOrderItem(null)
						.realizingResource(null)
						.realizingService(null)
						.isBundle(false)));

		testEntries.add(Arguments.of("The isCustomerVisible should have been updated.",
				ProductUpdateVOTestExample.build().atSchemaLocation(null)
						.billingAccount(null)
						.productOffering(null)
						.productSpecification(null)
						.isCustomerVisible(false),
				ProductVOTestExample.build().atSchemaLocation(null)
						.billingAccount(null)
						.productOffering(null)
						.productSpecification(null)
						.agreement(null)
						.product(null)
						.place(null)
						.relatedParty(null)
						.productOrderItem(null)
						.realizingResource(null)
						.realizingService(null)
						.isCustomerVisible(false)));

		Instant date = Instant.now();
		testEntries.add(Arguments.of("The orderDate should have been updated.",
				ProductUpdateVOTestExample.build().atSchemaLocation(null)
						.billingAccount(null)
						.productOffering(null)
						.productSpecification(null)
						.orderDate(date),
				ProductVOTestExample.build().atSchemaLocation(null)
						.billingAccount(null)
						.productOffering(null)
						.productSpecification(null)
						.agreement(null)
						.product(null)
						.place(null)
						.relatedParty(null)
						.productOrderItem(null)
						.realizingResource(null)
						.realizingService(null)
						.orderDate(date)));

		testEntries.add(Arguments.of("The productSerialNumber should have been updated.",
				ProductUpdateVOTestExample.build().atSchemaLocation(null)
						.billingAccount(null)
						.productOffering(null)
						.productSpecification(null)
						.productSerialNumber("two"),
				ProductVOTestExample.build().atSchemaLocation(null)
						.billingAccount(null)
						.productOffering(null)
						.productSpecification(null)
						.agreement(null)
						.product(null)
						.place(null)
						.relatedParty(null)
						.productOrderItem(null)
						.realizingResource(null)
						.realizingService(null)
						.productSerialNumber("two")));

		testEntries.add(Arguments.of("The startDate should have been updated.",
				ProductUpdateVOTestExample.build().atSchemaLocation(null)
						.billingAccount(null)
						.productOffering(null)
						.productSpecification(null)
						.startDate(date),
				ProductVOTestExample.build().atSchemaLocation(null)
						.billingAccount(null)
						.productOffering(null)
						.productSpecification(null)
						.agreement(null)
						.product(null)
						.place(null)
						.relatedParty(null)
						.productOrderItem(null)
						.realizingResource(null)
						.realizingService(null)
						.startDate(date)));

		testEntries.add(Arguments.of("The terminationDate should have been updated.",
				ProductUpdateVOTestExample.build().atSchemaLocation(null)
						.billingAccount(null)
						.productOffering(null)
						.productSpecification(null)
						.terminationDate(date),
				ProductVOTestExample.build().atSchemaLocation(null)
						.billingAccount(null)
						.productOffering(null)
						.productSpecification(null)
						.agreement(null)
						.product(null)
						.place(null)
						.relatedParty(null)
						.productOrderItem(null)
						.realizingResource(null)
						.realizingService(null)
						.terminationDate(date)));

		testEntries.add(Arguments.of("The characteristic should have been updated.",
				ProductUpdateVOTestExample.build().atSchemaLocation(null)
						.billingAccount(null)
						.productOffering(null)
						.productSpecification(null)
						.productCharacteristic(List.of(CharacteristicVOTestExample.build().atSchemaLocation(null).name("new"))),
				ProductVOTestExample.build().atSchemaLocation(null)
						.billingAccount(null)
						.productOffering(null)
						.productSpecification(null)
						.agreement(null)
						.product(null)
						.place(null)
						.relatedParty(null)
						.productOrderItem(null)
						.realizingResource(null)
						.realizingService(null)
						.productCharacteristic(List.of(CharacteristicVOTestExample.build().atSchemaLocation(null).name("new")))));

		testEntries.add(Arguments.of("The product term should have been updated.",
				ProductUpdateVOTestExample.build().atSchemaLocation(null)
						.billingAccount(null)
						.productOffering(null)
						.productSpecification(null)
						.productTerm(List.of(ProductTermVOTestExample.build().atSchemaLocation(null).name("new"))),
				ProductVOTestExample.build().atSchemaLocation(null)
						.billingAccount(null)
						.productOffering(null)
						.productSpecification(null)
						.agreement(null)
						.product(null)
						.place(null)
						.relatedParty(null)
						.productOrderItem(null)
						.realizingResource(null)
						.realizingService(null)
						.productTerm(List.of(ProductTermVOTestExample.build().atSchemaLocation(null).name("new")))));

		testEntries.add(Arguments.of("The status should have been updated.",
				ProductUpdateVOTestExample.build().atSchemaLocation(null)
						.billingAccount(null)
						.productOffering(null)
						.productSpecification(null)
						.status(ProductStatusTypeVO.CREATED),
				ProductVOTestExample.build().atSchemaLocation(null)
						.billingAccount(null)
						.productOffering(null)
						.productSpecification(null)
						.agreement(null)
						.product(null)
						.place(null)
						.relatedParty(null)
						.productOrderItem(null)
						.realizingResource(null)
						.realizingService(null)
						.status(ProductStatusTypeVO.CREATED)));

		testEntries.add(Arguments.of("The baseType should have been updated.",
				ProductUpdateVOTestExample.build().atSchemaLocation(null)
						.billingAccount(null)
						.productOffering(null)
						.productSpecification(null)
						.atBaseType("Product"),
				ProductVOTestExample.build().atSchemaLocation(null)
						.billingAccount(null)
						.productOffering(null)
						.productSpecification(null)
						.agreement(null)
						.product(null)
						.place(null)
						.relatedParty(null)
						.productOrderItem(null)
						.realizingResource(null)
						.realizingService(null)
						.atBaseType("Product")));

		testEntries.add(Arguments.of("The baseType should have been updated.",
				ProductUpdateVOTestExample.build().atSchemaLocation(null)
						.billingAccount(null)
						.productOffering(null)
						.productSpecification(null)
						.atBaseType("Product"),
				ProductVOTestExample.build().atSchemaLocation(null)
						.billingAccount(null)
						.productOffering(null)
						.productSpecification(null)
						.agreement(null)
						.product(null)
						.place(null)
						.relatedParty(null)
						.productOrderItem(null)
						.realizingResource(null)
						.realizingService(null)
						.atBaseType("Product")));

		testEntries.add(Arguments.of("The type should have been updated.",
				ProductUpdateVOTestExample.build().atSchemaLocation(null)
						.billingAccount(null)
						.productOffering(null)
						.productSpecification(null)
						.atType("CloudProduct"),
				ProductVOTestExample.build().atSchemaLocation(null)
						.billingAccount(null)
						.productOffering(null)
						.productSpecification(null)
						.agreement(null)
						.product(null)
						.place(null)
						.relatedParty(null)
						.productOrderItem(null)
						.realizingResource(null)
						.realizingService(null)
						.atType("CloudProduct")));

		return testEntries.stream();
	}

	@ParameterizedTest
	@MethodSource("provideInvalidUpdates")
	public void patchProduct400(String message, ProductUpdateVO invalidUpdateVO) throws Exception {
		this.message = message;
		this.productUpdateVO = invalidUpdateVO;
		patchProduct400();
	}

	@Override
	public void patchProduct400() throws Exception {
		//first create
		ProductCreateVO productCreateVO = ProductCreateVOTestExample.build().atSchemaLocation(null)
				.productSpecification(null)
				.billingAccount(null)
				.productOffering(null);

		HttpResponse<ProductVO> createResponse = callAndCatch(
				() -> productApiTestClient.createProduct(null, productCreateVO));
		assertEquals(HttpStatus.CREATED, createResponse.getStatus(),
				"The product function should have been created first.");

		String productId = createResponse.body().getId();

		HttpResponse<ProductVO> updateResponse = callAndCatch(
				() -> productApiTestClient.patchProduct(null, productId, productUpdateVO));
		assertEquals(HttpStatus.BAD_REQUEST, updateResponse.getStatus(), message);

		Optional<ErrorDetails> optionalErrorDetails = updateResponse.getBody(ErrorDetails.class);
		assertTrue(optionalErrorDetails.isPresent(), "Some error details should be present.");
	}

	private static Stream<Arguments> provideInvalidUpdates() {
		List<Arguments> testEntries = new ArrayList<>();
		testEntries.add(Arguments.of("A product with invalid related parties should not be created.",
				ProductUpdateVOTestExample.build().atSchemaLocation(null)
						.productSpecification(null)
						.billingAccount(null)
						.productOffering(null)
						.relatedParty(List.of(RelatedPartyVOTestExample.build().atSchemaLocation(null)))));
		testEntries.add(Arguments.of("A product with non-existent related parties should not be created.",
				ProductUpdateVOTestExample.build().atSchemaLocation(null)
						.productSpecification(null)
						.billingAccount(null)
						.productOffering(null)
						.relatedParty(
								List.of((RelatedPartyVOTestExample.build().atSchemaLocation(null)
										.id("urn:ngsi-ld:organisation:non-existent"))))));

		testEntries.add(Arguments.of("A product with an invalid place ref should not be created.",
				ProductUpdateVOTestExample.build().atSchemaLocation(null)
						.productSpecification(null)
						.billingAccount(null)
						.productOffering(null)
						.place(List.of(RelatedPlaceRefOrValueVOTestExample.build().atSchemaLocation(null)))));
		testEntries.add(Arguments.of("A product with non-existent place ref should not be created.",
				ProductUpdateVOTestExample.build().atSchemaLocation(null)
						.productSpecification(null)
						.billingAccount(null)
						.productOffering(null)
						.place(List.of(
								RelatedPlaceRefOrValueVOTestExample.build().atSchemaLocation(null).id("urn:ngsi-ld:place:non-existent")))));

		testEntries.add(Arguments.of("A product with an invalid product ref should not be created.",
				ProductUpdateVOTestExample.build().atSchemaLocation(null)
						.billingAccount(null)
						.productOffering(null)
						.productSpecification(ProductSpecificationRefVOTestExample.build().atSchemaLocation(null))));
		testEntries.add(Arguments.of("A product with non-existent product ref should not be created.",
				ProductUpdateVOTestExample.build().atSchemaLocation(null)
						.billingAccount(null)
						.productOffering(null)
						.productSpecification(
								ProductSpecificationRefVOTestExample.build().atSchemaLocation(null)
										.id("urn:ngsi-ld:product-specification:non-existent"))));

		testEntries.add(Arguments.of("A product with an invalid billing account ref should not be created.",
				ProductUpdateVOTestExample.build().atSchemaLocation(null)
						.billingAccount(BillingAccountRefVOTestExample.build().atSchemaLocation(null))
						.productOffering(null)
						.productSpecification(null)));
		testEntries.add(Arguments.of("A product with non-existent billing account ref should not be created.",
				ProductUpdateVOTestExample.build().atSchemaLocation(null)
						.billingAccount(BillingAccountRefVOTestExample.build().atSchemaLocation(null)
								.id("urn:ngsi-ld:billing-account:non-existent"))
						.productOffering(null)
						.productSpecification(null)));

		testEntries.add(Arguments.of("A product with an invalid product offering ref should not be created.",
				ProductUpdateVOTestExample.build().atSchemaLocation(null)
						.billingAccount(null)
						.productOffering(ProductOfferingRefVOTestExample.build().atSchemaLocation(null))
						.productSpecification(null)));
		testEntries.add(Arguments.of("A product with non-existent product offering ref should not be created.",
				ProductUpdateVOTestExample.build().atSchemaLocation(null)
						.billingAccount(null)
						.productOffering(
								ProductOfferingRefVOTestExample.build().atSchemaLocation(null).id("urn:ngsi-ld:product-offering:non-existent"))
						.productSpecification(null)));

		testEntries.add(Arguments.of("A product with an invalid agreement ref should not be created.",
				ProductUpdateVOTestExample.build().atSchemaLocation(null)
						.agreement(List.of(AgreementItemRefVOTestExample.build().atSchemaLocation(null)))
						.billingAccount(null)
						.productOffering(null)
						.productSpecification(null)));
		testEntries.add(Arguments.of("A product with non-existent agreement ref should not be created.",
				ProductUpdateVOTestExample.build().atSchemaLocation(null)
						.agreement(
								List.of(AgreementItemRefVOTestExample.build().atSchemaLocation(null).id("urn:ngsi-ld:agreement:non-existent")))
						.billingAccount(null)
						.productOffering(null)
						.productSpecification(null)));

		testEntries.add(Arguments.of("A product with an invalid product order ref should not be created.",
				ProductUpdateVOTestExample.build().atSchemaLocation(null)
						.productOrderItem(List.of(RelatedProductOrderItemVOTestExample.build().atSchemaLocation(null)))
						.billingAccount(null)
						.productOffering(null)
						.productSpecification(null)));
		testEntries.add(Arguments.of("A product with non-existent product order ref should not be created.",
				ProductUpdateVOTestExample.build().atSchemaLocation(null)
						.productOrderItem(
								List.of(RelatedProductOrderItemVOTestExample.build().atSchemaLocation(null)
										.productOrderId("urn:ngsi-ld:product-order:non-existent")))
						.billingAccount(null)
						.productOffering(null)
						.productSpecification(null)));

		testEntries.add(Arguments.of("A product with an invalid product order ref should not be created.",
				ProductUpdateVOTestExample.build().atSchemaLocation(null)
						.productOrderItem(List.of(RelatedProductOrderItemVOTestExample.build().atSchemaLocation(null)))
						.billingAccount(null)
						.productOffering(null)
						.productSpecification(null)));
		testEntries.add(Arguments.of("A product with non-existent product order ref should not be created.",
				ProductUpdateVOTestExample.build().atSchemaLocation(null)
						.productOrderItem(
								List.of(RelatedProductOrderItemVOTestExample.build().atSchemaLocation(null)
										.productOrderId("urn:ngsi-ld:product-order:non-existent")))
						.billingAccount(null)
						.productOffering(null)
						.productSpecification(null)));

		provideInvalidPrices().forEach(invalidPrice ->
				testEntries.add(
						Arguments.of(invalidPrice.message(), ProductUpdateVOTestExample.build().atSchemaLocation(null).billingAccount(null)
								.productOffering(null)
								.productSpecification(null)
								.productPrice(invalidPrice.value()))
				));

		testEntries.add(Arguments.of("A product with an invalid product relationship should not be created.",
				ProductUpdateVOTestExample.build().atSchemaLocation(null)
						.productRelationship(
								List.of(ProductRelationshipVOTestExample.build().atSchemaLocation(null)
										.product(ProductRefOrValueVOTestExample.build().atSchemaLocation(null))))
						.billingAccount(null)
						.productOffering(null)
						.productSpecification(null)));
		testEntries.add(Arguments.of("A product with non-existent product relationship should not be created.",
				ProductUpdateVOTestExample.build().atSchemaLocation(null)
						.productRelationship(
								List.of(ProductRelationshipVOTestExample.build().atSchemaLocation(null)
										.product(ProductRefOrValueVOTestExample.build().atSchemaLocation(null)
												.id("urn:ngsi-ld:product:non-existent"))))
						.billingAccount(null)
						.productOffering(null)
						.productSpecification(null)));

		testEntries.add(Arguments.of("A product with an invalid resource ref should not be created.",
				ProductUpdateVOTestExample.build().atSchemaLocation(null)
						.realizingResource(List.of(ResourceRefVOTestExample.build().atSchemaLocation(null)))
						.billingAccount(null)
						.productOffering(null)
						.productSpecification(null)));
		testEntries.add(Arguments.of("A product with non-existent resource ref should not be created.",
				ProductUpdateVOTestExample.build().atSchemaLocation(null)
						.realizingResource(
								List.of(ResourceRefVOTestExample.build().atSchemaLocation(null).id("urn:ngsi-ld:resource:non-existent")))
						.billingAccount(null)
						.productOffering(null)
						.productSpecification(null)));

		testEntries.add(Arguments.of("A product with an invalid service ref should not be created.",
				ProductUpdateVOTestExample.build().atSchemaLocation(null)
						.realizingService(List.of(ServiceRefVOTestExample.build().atSchemaLocation(null)))
						.billingAccount(null)
						.productOffering(null)
						.productSpecification(null)));
		testEntries.add(Arguments.of("A product with non-existent service ref should not be created.",
				ProductUpdateVOTestExample.build().atSchemaLocation(null)
						.realizingService(
								List.of(ServiceRefVOTestExample.build().atSchemaLocation(null).id("urn:ngsi-ld:service:non-existent")))
						.billingAccount(null)
						.productOffering(null)
						.productSpecification(null)));
		return testEntries.stream();
	}

	@Disabled("Security is handled externally, thus 401 and 403 cannot happen.")
	@Test
	@Override
	public void patchProduct401() throws Exception {

	}

	@Disabled("Security is handled externally, thus 401 and 403 cannot happen.")
	@Test
	@Override
	public void patchProduct403() throws Exception {

	}

	@Test
	@Override
	public void patchProduct404() throws Exception {
		ProductUpdateVO productUpdateVO = ProductUpdateVOTestExample.build()
				.atSchemaLocation(null)
				.billingAccount(null)
				.productSpecification(null)
				.productOffering(null);
		assertEquals(
				HttpStatus.NOT_FOUND,
				callAndCatch(() -> productApiTestClient.patchProduct(null, "urn:ngsi-ld:product-catalog:not-existent",
						productUpdateVO)).getStatus(),
				"Non existent product should not be updated.");
	}

	@Disabled("Prohibited by the framework.")
	@Test
	@Override
	public void patchProduct405() throws Exception {

	}

	@Override
	public void patchProduct409() throws Exception {
		// TODO: can this happen?
	}

	@Override
	public void patchProduct500() throws Exception {

	}

	@ParameterizedTest
	@MethodSource("provideFieldParameters")
	public void retrieveProduct200(String message, String fields, ProductVO expectedProduct) throws Exception {
		this.fieldsParameter = fields;
		this.message = message;
		this.expectedProduct = expectedProduct;

		retrieveProduct200();
	}

	@Override
	public void retrieveProduct200() throws Exception {

		ProductCreateVO productCreateVO = ProductCreateVOTestExample.build().atSchemaLocation(null)
				.billingAccount(null)
				.productOffering(null)
				.productSpecification(null);
		HttpResponse<ProductVO> createResponse = callAndCatch(
				() -> productApiTestClient.createProduct(null, productCreateVO));
		assertEquals(HttpStatus.CREATED, createResponse.getStatus(), message);
		String id = createResponse.body().getId();
		Instant lastUpdate = createResponse.body().getLastUpdate();

		expectedProduct
				.id(id)
				.href(id)
				.lastUpdate(lastUpdate);


		//then retrieve
		HttpResponse<ProductVO> retrievedRF = callAndCatch(
				() -> productApiTestClient.retrieveProduct(null, id, fieldsParameter));
		assertEquals(HttpStatus.OK, retrievedRF.getStatus(), message);

		//retrievedRF.body().setLastUpdate(lastUpdate);
		assertEquals(expectedProduct, retrievedRF.body(), message);
	}

	private static Stream<Arguments> provideFieldParameters() {
		return Stream.of(
				Arguments.of("Without a fields parameter everything should be returned.", null,
						ProductVOTestExample.build().atSchemaLocation(null)
								.billingAccount(null)
								.productOffering(null)
								.productSpecification(null)
								.agreement(null)
								.place(null)
								.product(null)
								.productOrderItem(null)
								.realizingResource(null)
								.realizingService(null)
								//.lastUpdate(Instant.MAX)
								.relatedParty(null)),
				Arguments.of("Only description and the mandatory parameters should have been included.", "description",
						ProductVOTestExample.build().atSchemaLocation(null)
								.isBundle(null)
								.isCustomerVisible(null)
								.name(null)
								.orderDate(null)
								.productSerialNumber(null)
								.startDate(null)
								.terminationDate(null)
								.agreement(null)
								.billingAccount(null)
								.place(null)
								.productCharacteristic(null)
								.productOffering(null)
								.productOrderItem(null)
								.productPrice(null)
								.productRelationship(null)
								.productSpecification(null)
								.productTerm(null)
								.product(null)
								.realizingResource(null)
								.realizingService(null)
								.relatedParty(null)
								.status(null)
								.atBaseType(null)
								.atType(null)
								.atSchemaLocation(null)),
				Arguments.of(
						"Only the mandatory parameters should have been included when a non-existent field was requested.",
						"nothingToSeeHere", ProductVOTestExample.build().atSchemaLocation(null)
								.description(null)
								.isBundle(null)
								.isCustomerVisible(null)
								.name(null)
								.orderDate(null)
								.productSerialNumber(null)
								.startDate(null)
								.terminationDate(null)
								.agreement(null)
								.billingAccount(null)
								.place(null)
								.product(null)
								.productCharacteristic(null)
								.productOffering(null)
								.productOrderItem(null)
								.productPrice(null)
								.productRelationship(null)
								.productSpecification(null)
								.productTerm(null)
								.realizingResource(null)
								.realizingService(null)
								.relatedParty(null)
								.status(null)
								.atBaseType(null)
								.atType(null)
								.atSchemaLocation(null)),
				Arguments.of("Only description, lastUpdate, isBundle and the mandatory parameters should have been included.",
						"description,isBundle", ProductVOTestExample.build().atSchemaLocation(null)
								.isCustomerVisible(null)
								.name(null)
								.orderDate(null)
								.productSerialNumber(null)
								.startDate(null)
								.terminationDate(null)
								.agreement(null)
								.billingAccount(null)
								.place(null)
								.product(null)
								.productCharacteristic(null)
								.productOffering(null)
								.productOrderItem(null)
								.productPrice(null)
								.productRelationship(null)
								.productSpecification(null)
								.productTerm(null)
								.realizingResource(null)
								.realizingService(null)
								.relatedParty(null)
								.status(null)
								.atBaseType(null)
								.atType(null)
								.atSchemaLocation(null)));

	}

	@Disabled("400 cannot happen, only 404")
	@Test
	@Override
	public void retrieveProduct400() throws Exception {

	}

	@Disabled("Security is handled externally, thus 401 and 403 cannot happen.")
	@Test
	@Override
	public void retrieveProduct401() throws Exception {

	}

	@Disabled("Security is handled externally, thus 401 and 403 cannot happen.")
	@Test
	@Override
	public void retrieveProduct403() throws Exception {

	}

	@Test
	@Override
	public void retrieveProduct404() throws Exception {
		HttpResponse<ProductVO> response = callAndCatch(
				() -> productApiTestClient.retrieveProduct(null, "urn:ngsi-ld:product-function:non-existent", null));
		assertEquals(HttpStatus.NOT_FOUND, response.getStatus(), "No such product-catalog should exist.");

		Optional<ErrorDetails> optionalErrorDetails = response.getBody(ErrorDetails.class);
		assertTrue(optionalErrorDetails.isPresent(), "Error details should have been provided.");
	}

	@Disabled("Prohibited by the framework.")
	@Test
	@Override
	public void retrieveProduct405() throws Exception {

	}

	@Disabled("Conflict not possible on retrieval")
	@Test
	@Override
	public void retrieveProduct409() throws Exception {

	}

	@Override
	public void retrieveProduct500() throws Exception {

	}

	@Override
	protected String getEntityType() {
		return Product.TYPE_PRODUCT;
	}
}