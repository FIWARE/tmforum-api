package org.fiware.tmforum.productcatalog;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import io.micronaut.test.annotation.MockBean;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import org.fiware.ngsi.api.EntitiesApiClient;
import org.fiware.productcatalog.api.ProductOfferingPriceApiTestClient;
import org.fiware.productcatalog.api.ProductOfferingPriceApiTestSpec;
import org.fiware.productcatalog.model.BundledProductOfferingPriceRelationshipVOTestExample;
import org.fiware.productcatalog.model.ConstraintRefVOTestExample;
import org.fiware.productcatalog.model.PlaceRefVOTestExample;
import org.fiware.productcatalog.model.PricingLogicAlgorithmVO;
import org.fiware.productcatalog.model.PricingLogicAlgorithmVOTestExample;
import org.fiware.productcatalog.model.ProductOfferingPriceCreateVO;
import org.fiware.productcatalog.model.ProductOfferingPriceCreateVOTestExample;
import org.fiware.productcatalog.model.ProductOfferingPriceRelationshipVOTestExample;
import org.fiware.productcatalog.model.ProductOfferingPriceUpdateVO;
import org.fiware.productcatalog.model.ProductOfferingPriceUpdateVOTestExample;
import org.fiware.productcatalog.model.ProductOfferingPriceVO;
import org.fiware.productcatalog.model.ProductOfferingPriceVOTestExample;
import org.fiware.productcatalog.model.ProductSpecificationCharacteristicValueUseVOTestExample;
import org.fiware.productcatalog.model.TaxItemVO;
import org.fiware.productcatalog.model.TaxItemVOTestExample;
import org.fiware.productcatalog.model.TimePeriodVO;
import org.fiware.productcatalog.model.TimePeriodVOTestExample;
import org.fiware.tmforum.common.notification.EventHandler;
import org.fiware.tmforum.common.configuration.GeneralProperties;
import org.fiware.tmforum.common.exception.ErrorDetails;
import org.fiware.tmforum.common.test.AbstractApiIT;
import org.fiware.tmforum.product.ProductOfferingPrice;
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

@MicronautTest(packages = { "org.fiware.tmforum.productcatalog" })
public class ProductOfferingPriceApiIT extends AbstractApiIT implements ProductOfferingPriceApiTestSpec {

	public final ProductOfferingPriceApiTestClient productOfferingPriceApiTestClient;

	private String message;
	private ProductOfferingPriceCreateVO productOfferingPriceCreateVO;
	private ProductOfferingPriceUpdateVO productOfferingPriceUpdateVO;
	private ProductOfferingPriceVO expectedProductOfferingPrice;

	private Clock clock = mock(Clock.class);

	public ProductOfferingPriceApiIT(ProductOfferingPriceApiTestClient productOfferingPriceApiTestClient,
			EntitiesApiClient entitiesApiClient, ObjectMapper objectMapper, GeneralProperties generalProperties) {
		super(entitiesApiClient, objectMapper, generalProperties);
		this.productOfferingPriceApiTestClient = productOfferingPriceApiTestClient;
	}

	@MockBean(Clock.class)
	public Clock clock() {
		return clock;
	}

	@MockBean(EventHandler.class)
	public EventHandler eventHandler() {
		EventHandler eventHandler = mock(EventHandler.class);

		when(eventHandler.handleCreateEvent(any())).thenReturn(Mono.empty());
		when(eventHandler.handleUpdateEvent(any(), any())).thenReturn(Mono.empty());
		when(eventHandler.handleDeleteEvent(any())).thenReturn(Mono.empty());

		return eventHandler;
	}

	@ParameterizedTest
	@MethodSource("provideValidProductOfferingPrices")
	public void createProductOfferingPrice201(String message, ProductOfferingPriceCreateVO productOfferingPriceCreateVO,
			ProductOfferingPriceVO expectedProductOfferingPrice) throws Exception {
		this.message = message;
		this.productOfferingPriceCreateVO = productOfferingPriceCreateVO;
		this.expectedProductOfferingPrice = expectedProductOfferingPrice;
		createProductOfferingPrice201();
	}

	@Override
	public void createProductOfferingPrice201() throws Exception {

		Instant currentTimeInstant = Instant.ofEpochSecond(10000);

		when(clock.instant()).thenReturn(currentTimeInstant);

		HttpResponse<ProductOfferingPriceVO> productOfferingVOHttpResponse = callAndCatch(
				() -> productOfferingPriceApiTestClient.createProductOfferingPrice(productOfferingPriceCreateVO));
		assertEquals(HttpStatus.CREATED, productOfferingVOHttpResponse.getStatus(), message);
		String productOfferingId = productOfferingVOHttpResponse.body().getId();

		expectedProductOfferingPrice.setId(productOfferingId);
		expectedProductOfferingPrice.setHref(productOfferingId);
		expectedProductOfferingPrice.setLastUpdate(currentTimeInstant);

		assertEquals(expectedProductOfferingPrice, productOfferingVOHttpResponse.body(), message);
	}

	private static Stream<Arguments> provideValidProductOfferingPrices() {
		List<Arguments> testEntries = new ArrayList<>();

		ProductOfferingPriceCreateVO productOfferingPriceCreateVO = ProductOfferingPriceCreateVOTestExample.build();

		ProductOfferingPriceVO expectedProductOfferingPrice = ProductOfferingPriceVOTestExample.build();
		testEntries.add(Arguments.of("An empty productOffering should have been created.", productOfferingPriceCreateVO,
				expectedProductOfferingPrice));

		ProductOfferingPriceCreateVO withPriceLogic = ProductOfferingPriceCreateVOTestExample.build();
		PricingLogicAlgorithmVO pricingLogicAlgorithmVO = PricingLogicAlgorithmVOTestExample.build();
		pricingLogicAlgorithmVO.setId("urn:price-logic");
		pricingLogicAlgorithmVO.setPlaSpecId(null);
		withPriceLogic.setPricingLogicAlgorithm(List.of(pricingLogicAlgorithmVO));
		ProductOfferingPriceVO expectedWithPriceLogic = ProductOfferingPriceVOTestExample.build();
		expectedWithPriceLogic.setPricingLogicAlgorithm(List.of(pricingLogicAlgorithmVO));
		testEntries.add(Arguments.of("A product offering with a price logic should have been created.", withPriceLogic,
				expectedWithPriceLogic));

		ProductOfferingPriceCreateVO withTaxItem = ProductOfferingPriceCreateVOTestExample.build();
		TaxItemVO taxItemVO = TaxItemVOTestExample.build();
		taxItemVO.setId("urn:tax-item");
		withTaxItem.setTax(List.of(taxItemVO));
		ProductOfferingPriceVO expectedWithTax = ProductOfferingPriceVOTestExample.build();
		expectedWithTax.setTax(List.of(taxItemVO));
		testEntries.add(
				Arguments.of("A product offering with a tax should have been created.", withTaxItem, expectedWithTax));

		return testEntries.stream();
	}

	@ParameterizedTest
	@MethodSource("provideInvalidProductOfferingPrices")
	public void createProductOfferingPrice400(String message, ProductOfferingPriceCreateVO invalidCreateVO)
			throws Exception {
		this.message = message;
		this.productOfferingPriceCreateVO = invalidCreateVO;
		createProductOfferingPrice400();
	}

	@Override
	public void createProductOfferingPrice400() throws Exception {
		HttpResponse<ProductOfferingPriceVO> creationResponse = callAndCatch(
				() -> productOfferingPriceApiTestClient.createProductOfferingPrice(productOfferingPriceCreateVO));
		assertEquals(HttpStatus.BAD_REQUEST, creationResponse.getStatus(), message);
		Optional<ErrorDetails> optionalErrorDetails = creationResponse.getBody(ErrorDetails.class);
		assertTrue(optionalErrorDetails.isPresent(), "Error details should be provided.");
	}

	private static Stream<Arguments> provideInvalidProductOfferingPrices() {
		List<Arguments> testEntries = new ArrayList<>();

		testEntries.add(Arguments.of("A productOfferingPrice with invalid popRel should not be created.",
				ProductOfferingPriceCreateVOTestExample.build().bundledPopRelationship(
						List.of(BundledProductOfferingPriceRelationshipVOTestExample.build()))));
		testEntries.add(Arguments.of("A productOffering with non-existent popRel should not be created.",
				ProductOfferingPriceCreateVOTestExample.build().bundledPopRelationship(
						List.of(BundledProductOfferingPriceRelationshipVOTestExample.build()
								.id("urn:ngsi-ld:product-offering-price:non-existent")))));

		testEntries.add(Arguments.of("A productOffering with invalid constraint should not be created.",
				ProductOfferingPriceCreateVOTestExample.build()
						.constraint(List.of(ConstraintRefVOTestExample.build()))));
		testEntries.add(Arguments.of("A productOffering with non-existent constraint should not be created.",
				ProductOfferingPriceCreateVOTestExample.build().constraint(
						List.of(ConstraintRefVOTestExample.build().id("urn:ngsi-ld:constraint:non-existent")))));

		testEntries.add(Arguments.of("A productOffering with invalid place should not be created.",
				ProductOfferingPriceCreateVOTestExample.build().place(List.of(PlaceRefVOTestExample.build()))));
		testEntries.add(Arguments.of("A productOffering with non-existent place should not be created.",
				ProductOfferingPriceCreateVOTestExample.build()
						.place(List.of(PlaceRefVOTestExample.build().id("urn:ngsi-ld:place:non-existent")))));

		testEntries.add(Arguments.of("A productOffering with invalid popRelationship should not be created.",
				ProductOfferingPriceCreateVOTestExample.build()
						.popRelationship(List.of(ProductOfferingPriceRelationshipVOTestExample.build()))));
		testEntries.add(Arguments.of("A productOffering with non-existent popRelationship should not be created.",
				ProductOfferingPriceCreateVOTestExample.build().popRelationship(
						List.of(ProductOfferingPriceRelationshipVOTestExample.build()
								.id("urn:ngsi-ld:product-offering-price:non-existent")))));

		// TODO: Add test for sub-relationships
		testEntries.add(
				Arguments.of("A productOffering with invalid prod-spec-characteristic-value-use should not be created.",
						ProductOfferingPriceCreateVOTestExample.build().prodSpecCharValueUse(
								List.of(ProductSpecificationCharacteristicValueUseVOTestExample.build()))));
		testEntries.add(Arguments.of(
				"A productOffering with non-existent prod-spec-characteristic-value-use should not be created.",
				ProductOfferingPriceCreateVOTestExample.build().prodSpecCharValueUse(
						List.of(ProductSpecificationCharacteristicValueUseVOTestExample.build()
								.id("urn:ngsi-ld:prod-spec-characteristic-value-use:non-existent")))));

		return testEntries.stream();
	}

	@Disabled("Security is handled externally, thus 401 and 403 cannot happen.")
	@Test
	@Override
	public void createProductOfferingPrice401() throws Exception {
	}

	@Disabled("Security is handled externally, thus 401 and 403 cannot happen.")
	@Test
	@Override
	public void createProductOfferingPrice403() throws Exception {
	}

	@Disabled("Prohibited by the framework.")
	@Test
	@Override
	public void createProductOfferingPrice405() throws Exception {

	}

	@Disabled("Catalog doesn't have 'implicit' entities and id is generated by the implementation, no conflict possible.")
	@Test
	@Override
	public void createProductOfferingPrice409() throws Exception {

	}

	@Override
	public void createProductOfferingPrice500() throws Exception {

	}

	@Test
	@Override
	public void deleteProductOfferingPrice204() throws Exception {
		//first create
		ProductOfferingPriceCreateVO productOfferingPriceCreateVO = ProductOfferingPriceCreateVOTestExample.build();

		HttpResponse<ProductOfferingPriceVO> createResponse = callAndCatch(
				() -> productOfferingPriceApiTestClient.createProductOfferingPrice(productOfferingPriceCreateVO));
		assertEquals(HttpStatus.CREATED, createResponse.getStatus(),
				"The productOfferingPrice should have been created first.");

		String popId = createResponse.body().getId();

		assertEquals(HttpStatus.NO_CONTENT,
				callAndCatch(() -> productOfferingPriceApiTestClient.deleteProductOfferingPrice(popId)).getStatus(),
				"The productOfferingPrice should have been deleted.");

		assertEquals(HttpStatus.NOT_FOUND,
				callAndCatch(
						() -> productOfferingPriceApiTestClient.retrieveProductOfferingPrice(popId, null)).status(),
				"The productOfferingPrice should not exist anymore.");

	}

	@Disabled("400 is impossible to happen on deletion with the current implementation.")
	@Test
	@Override
	public void deleteProductOfferingPrice400() throws Exception {

	}

	@Disabled("Security is handled externally, thus 401 and 403 cannot happen.")
	@Test
	@Override
	public void deleteProductOfferingPrice401() throws Exception {

	}

	@Disabled("Security is handled externally, thus 401 and 403 cannot happen.")
	@Test
	@Override
	public void deleteProductOfferingPrice403() throws Exception {

	}

	@Test
	@Override
	public void deleteProductOfferingPrice404() throws Exception {
		HttpResponse<?> notFoundResponse = callAndCatch(
				() -> productOfferingPriceApiTestClient.deleteProductOfferingPrice(
						"urn:ngsi-ld:product-offering-price:no-pop"));
		assertEquals(HttpStatus.NOT_FOUND,
				notFoundResponse.getStatus(),
				"No such product-offering-price should exist.");

		Optional<ErrorDetails> optionalErrorDetails = notFoundResponse.getBody(ErrorDetails.class);
		assertTrue(optionalErrorDetails.isPresent(), "Error details should be provided.");

		notFoundResponse = callAndCatch(
				() -> productOfferingPriceApiTestClient.deleteProductOfferingPrice("invalid-id"));
		assertEquals(HttpStatus.NOT_FOUND,
				notFoundResponse.getStatus(),
				"No such product-offering-price should exist.");

		optionalErrorDetails = notFoundResponse.getBody(ErrorDetails.class);
		assertTrue(optionalErrorDetails.isPresent(), "Error details should be provided.");
	}

	@Disabled("Prohibited by the framework.")
	@Test
	@Override
	public void deleteProductOfferingPrice405() throws Exception {

	}

	@Disabled("Impossible status.")
	@Test
	@Override
	public void deleteProductOfferingPrice409() throws Exception {

	}

	@Override
	public void deleteProductOfferingPrice500() throws Exception {

	}

	@Test
	@Override
	public void listProductOfferingPrice200() throws Exception {

		List<ProductOfferingPriceVO> expectedProductOfferingPrices = new ArrayList<>();
		for (int i = 0; i < 10; i++) {
			ProductOfferingPriceCreateVO productOfferingPriceCreateVO = ProductOfferingPriceCreateVOTestExample.build();
			String id = productOfferingPriceApiTestClient.createProductOfferingPrice(productOfferingPriceCreateVO)
					.body().getId();
			ProductOfferingPriceVO productOfferingPriceVO = ProductOfferingPriceVOTestExample.build();
			productOfferingPriceVO
					.id(id)
					.href(id)
					.bundledPopRelationship(null)
					.place(null)
					.popRelationship(null)
					.constraint(null);
			expectedProductOfferingPrices.add(productOfferingPriceVO);
		}

		HttpResponse<List<ProductOfferingPriceVO>> productOfferingPriceResponse = callAndCatch(
				() -> productOfferingPriceApiTestClient.listProductOfferingPrice(null, null, null));

		assertEquals(HttpStatus.OK, productOfferingPriceResponse.getStatus(), "The list should be accessible.");
		assertEquals(expectedProductOfferingPrices.size(), productOfferingPriceResponse.getBody().get().size(),
				"All productOfferingPrices should have been returned.");
		List<ProductOfferingPriceVO> retrievedProductOfferingPrices = productOfferingPriceResponse.getBody().get();

		Map<String, ProductOfferingPriceVO> retrievedMap = retrievedProductOfferingPrices.stream()
				.collect(Collectors.toMap(productOfferingPrice -> productOfferingPrice.getId(),
						productOfferingPrice -> productOfferingPrice));

		expectedProductOfferingPrices.stream()
				.forEach(
						expectedProductOfferingPrice -> assertTrue(
								retrievedMap.containsKey(expectedProductOfferingPrice.getId()),
								String.format("All created productOfferingPrices should be returned - Missing: %s.",
										expectedProductOfferingPrice,
										retrievedProductOfferingPrices)));
		expectedProductOfferingPrices.stream().forEach(
				expectedProductOfferingPrice -> assertEquals(expectedProductOfferingPrice,
						retrievedMap.get(expectedProductOfferingPrice.getId()),
						"The correct productOfferingPrices should be retrieved."));

		// get with pagination
		Integer limit = 5;
		HttpResponse<List<ProductOfferingPriceVO>> firstPartResponse = callAndCatch(
				() -> productOfferingPriceApiTestClient.listProductOfferingPrice(null, 0, limit));
		assertEquals(limit, firstPartResponse.body().size(),
				"Only the requested number of entries should be returend.");
		HttpResponse<List<ProductOfferingPriceVO>> secondPartResponse = callAndCatch(
				() -> productOfferingPriceApiTestClient.listProductOfferingPrice(null, 0 + limit, limit));
		assertEquals(limit, secondPartResponse.body().size(),
				"Only the requested number of entries should be returend.");

		retrievedProductOfferingPrices.clear();
		retrievedProductOfferingPrices.addAll(firstPartResponse.body());
		retrievedProductOfferingPrices.addAll(secondPartResponse.body());
		expectedProductOfferingPrices.stream()
				.forEach(
						expectedProductOfferingPrice -> assertTrue(
								retrievedMap.containsKey(expectedProductOfferingPrice.getId()),
								String.format("All created productOfferingPrices should be returned - Missing: %s.",
										expectedProductOfferingPrice)));
		expectedProductOfferingPrices.stream().forEach(
				expectedProductOfferingPrice -> assertEquals(expectedProductOfferingPrice,
						retrievedMap.get(expectedProductOfferingPrice.getId()),
						"The correct productOfferingPrices should be retrieved."));
	}

	@Test
	@Override
	public void listProductOfferingPrice400() throws Exception {
		HttpResponse<List<ProductOfferingPriceVO>> badRequestResponse = callAndCatch(
				() -> productOfferingPriceApiTestClient.listProductOfferingPrice(null, -1, null));
		assertEquals(HttpStatus.BAD_REQUEST,
				badRequestResponse.getStatus(),
				"Negative offsets are impossible.");

		Optional<ErrorDetails> optionalErrorDetails = badRequestResponse.getBody(ErrorDetails.class);
		assertTrue(optionalErrorDetails.isPresent(), "Error details should be provided.");

		badRequestResponse = callAndCatch(
				() -> productOfferingPriceApiTestClient.listProductOfferingPrice(null, null, -1));
		assertEquals(HttpStatus.BAD_REQUEST,
				badRequestResponse.getStatus(),
				"Negative limits are impossible.");
		optionalErrorDetails = badRequestResponse.getBody(ErrorDetails.class);
		assertTrue(optionalErrorDetails.isPresent(), "Error details should be provided.");

	}

	@Disabled("Security is handled externally, thus 401 and 403 cannot happen.")
	@Test
	@Override
	public void listProductOfferingPrice401() throws Exception {

	}

	@Disabled("Security is handled externally, thus 401 and 403 cannot happen.")
	@Test
	@Override
	public void listProductOfferingPrice403() throws Exception {

	}

	@Disabled("Not found is not possible here, will be answered with an empty list instead.")
	@Test
	@Override
	public void listProductOfferingPrice404() throws Exception {

	}

	@Disabled("Prohibited by the framework.")
	@Test
	@Override
	public void listProductOfferingPrice405() throws Exception {

	}

	@Disabled("Impossible status.")
	@Test
	@Override
	public void listProductOfferingPrice409() throws Exception {

	}

	@Override
	public void listProductOfferingPrice500() throws Exception {

	}

	@ParameterizedTest
	@MethodSource("provideProductOfferingPriceUpdates")
	public void patchProductOfferingPrice200(String message, ProductOfferingPriceUpdateVO productOfferingPriceUpdateVO,
			ProductOfferingPriceVO expectedProductOfferingPrice) throws Exception {
		this.message = message;
		this.productOfferingPriceUpdateVO = productOfferingPriceUpdateVO;
		this.expectedProductOfferingPrice = expectedProductOfferingPrice;
		patchProductOfferingPrice200();
	}

	@Override
	public void patchProductOfferingPrice200() throws Exception {
		//first create
		ProductOfferingPriceCreateVO productOfferingPriceCreateVO = ProductOfferingPriceCreateVOTestExample.build();

		HttpResponse<ProductOfferingPriceVO> createResponse = callAndCatch(
				() -> productOfferingPriceApiTestClient.createProductOfferingPrice(productOfferingPriceCreateVO));
		assertEquals(HttpStatus.CREATED, createResponse.getStatus(), "The catalog should have been created first.");

		String catalogId = createResponse.body().getId();

		HttpResponse<ProductOfferingPriceVO> updateResponse = callAndCatch(
				() -> productOfferingPriceApiTestClient.patchProductOfferingPrice(catalogId,
						productOfferingPriceUpdateVO));
		assertEquals(HttpStatus.OK, updateResponse.getStatus(), message);

		ProductOfferingPriceVO updatedCatalog = updateResponse.body();
		expectedProductOfferingPrice.setHref(catalogId);
		expectedProductOfferingPrice.setId(catalogId);

		expectedProductOfferingPrice.setConstraint(null);
		expectedProductOfferingPrice.setPopRelationship(null);
		expectedProductOfferingPrice.setBundledPopRelationship(null);
		expectedProductOfferingPrice.setPlace(null);
		assertEquals(expectedProductOfferingPrice, updatedCatalog, message);
	}

	private static Stream<Arguments> provideProductOfferingPriceUpdates() {
		List<Arguments> testEntries = new ArrayList<>();

		ProductOfferingPriceUpdateVO newDesc = ProductOfferingPriceUpdateVOTestExample.build();
		newDesc.setDescription("New description");
		ProductOfferingPriceVO expectedNewDesc = ProductOfferingPriceVOTestExample.build();
		expectedNewDesc.setDescription("New description");
		testEntries.add(Arguments.of("The description should have been updated.", newDesc, expectedNewDesc));

		ProductOfferingPriceUpdateVO newLifeCycle = ProductOfferingPriceUpdateVOTestExample.build();
		newLifeCycle.setLifecycleStatus("Dead");
		ProductOfferingPriceVO expectedNewLifeCycle = ProductOfferingPriceVOTestExample.build();
		expectedNewLifeCycle.setLifecycleStatus("Dead");
		testEntries.add(
				Arguments.of("The lifecycle state should have been updated.", newLifeCycle, expectedNewLifeCycle));

		ProductOfferingPriceUpdateVO newName = ProductOfferingPriceUpdateVOTestExample.build();
		newName.setName("New name");
		ProductOfferingPriceVO expectedNewName = ProductOfferingPriceVOTestExample.build();
		expectedNewName.setName("New name");
		testEntries.add(Arguments.of("The name should have been updated.", newName, expectedNewName));

		ProductOfferingPriceUpdateVO newVersion = ProductOfferingPriceUpdateVOTestExample.build();
		newVersion.setVersion("1.23.1");
		ProductOfferingPriceVO expectedNewVersion = ProductOfferingPriceVOTestExample.build();
		expectedNewVersion.setVersion("1.23.1");
		testEntries.add(Arguments.of("The version should have been updated.", newVersion, expectedNewVersion));

		ProductOfferingPriceUpdateVO newValidFor = ProductOfferingPriceUpdateVOTestExample.build();
		TimePeriodVO timePeriodVO = TimePeriodVOTestExample.build();
		timePeriodVO.setEndDateTime(Instant.now());
		timePeriodVO.setStartDateTime(Instant.now());
		newValidFor.setValidFor(timePeriodVO);
		ProductOfferingPriceVO expectedNewValidFor = ProductOfferingPriceVOTestExample.build();
		expectedNewValidFor.setValidFor(timePeriodVO);
		testEntries.add(Arguments.of("The validFor should have been updated.", newValidFor, expectedNewValidFor));

		ProductOfferingPriceUpdateVO newTax = ProductOfferingPriceUpdateVOTestExample.build();
		newTax.setTax(List.of(TaxItemVOTestExample.build().id("urn:tax-item")));
		ProductOfferingPriceVO expectedNewTax = ProductOfferingPriceVOTestExample.build();
		expectedNewTax.setTax(List.of(TaxItemVOTestExample.build().id("urn:tax-item")));
		testEntries.add(Arguments.of("The tax should have been updated.", newTax, expectedNewTax));

		return testEntries.stream();
	}

	@ParameterizedTest
	@MethodSource("provideInvalidUpdates")
	public void patchProductOfferingPrice400(String message, ProductOfferingPriceUpdateVO invalidUpdateVO)
			throws Exception {
		this.message = message;
		this.productOfferingPriceUpdateVO = invalidUpdateVO;
		patchProductOfferingPrice400();
	}

	@Override
	public void patchProductOfferingPrice400() throws Exception {
		//first create
		ProductOfferingPriceCreateVO productOfferingPriceCreateVO = ProductOfferingPriceCreateVOTestExample.build();
		HttpResponse<ProductOfferingPriceVO> createResponse = callAndCatch(
				() -> productOfferingPriceApiTestClient.createProductOfferingPrice(productOfferingPriceCreateVO));
		assertEquals(HttpStatus.CREATED, createResponse.getStatus(), "The catalog should have been created first.");

		String catalogId = createResponse.body().getId();

		HttpResponse<ProductOfferingPriceVO> updateResponse = callAndCatch(
				() -> productOfferingPriceApiTestClient.patchProductOfferingPrice(catalogId,
						productOfferingPriceUpdateVO));
		assertEquals(HttpStatus.BAD_REQUEST, updateResponse.getStatus(), message);

		Optional<ErrorDetails> optionalErrorDetails = updateResponse.getBody(ErrorDetails.class);
		assertTrue(optionalErrorDetails.isPresent(), "Some error details should be present.");
	}

	private static Stream<Arguments> provideInvalidUpdates() {
		List<Arguments> testEntries = new ArrayList<>();

		testEntries.add(Arguments.of("A productOfferingPrice with invalid popRel should not be created.",
				ProductOfferingPriceUpdateVOTestExample.build().bundledPopRelationship(
						List.of(BundledProductOfferingPriceRelationshipVOTestExample.build()))));
		testEntries.add(Arguments.of("A productOffering with non-existent popRel should not be created.",
				ProductOfferingPriceUpdateVOTestExample.build().bundledPopRelationship(
						List.of(BundledProductOfferingPriceRelationshipVOTestExample.build()
								.id("urn:ngsi-ld:product-offering-price:non-existent")))));

		testEntries.add(Arguments.of("A productOffering with invalid constraint should not be created.",
				ProductOfferingPriceUpdateVOTestExample.build()
						.constraint(List.of(ConstraintRefVOTestExample.build()))));
		testEntries.add(Arguments.of("A productOffering with non-existent constraint should not be created.",
				ProductOfferingPriceUpdateVOTestExample.build().constraint(
						List.of(ConstraintRefVOTestExample.build().id("urn:ngsi-ld:constraint:non-existent")))));

		testEntries.add(Arguments.of("A productOffering with invalid place should not be created.",
				ProductOfferingPriceUpdateVOTestExample.build().place(List.of(PlaceRefVOTestExample.build()))));
		testEntries.add(Arguments.of("A productOffering with non-existent place should not be created.",
				ProductOfferingPriceUpdateVOTestExample.build()
						.place(List.of(PlaceRefVOTestExample.build().id("urn:ngsi-ld:place:non-existent")))));

		testEntries.add(Arguments.of("A productOffering with invalid popRelationship should not be created.",
				ProductOfferingPriceUpdateVOTestExample.build()
						.popRelationship(List.of(ProductOfferingPriceRelationshipVOTestExample.build()))));
		testEntries.add(Arguments.of("A productOffering with non-existent popRelationship should not be created.",
				ProductOfferingPriceUpdateVOTestExample.build().popRelationship(
						List.of(ProductOfferingPriceRelationshipVOTestExample.build()
								.id("urn:ngsi-ld:product-offering-price:non-existent")))));

		// TODO: Add test for sub-relationships
		testEntries.add(
				Arguments.of("A productOffering with invalid prod-spec-characteristic-value-use should not be created.",
						ProductOfferingPriceUpdateVOTestExample.build().prodSpecCharValueUse(
								List.of(ProductSpecificationCharacteristicValueUseVOTestExample.build()))));
		testEntries.add(Arguments.of(
				"A productOffering with non-existent prod-spec-characteristic-value-use should not be created.",
				ProductOfferingPriceUpdateVOTestExample.build().prodSpecCharValueUse(
						List.of(ProductSpecificationCharacteristicValueUseVOTestExample.build()
								.id("urn:ngsi-ld:prod-spec-characteristic-value-use:non-existent")))));

		return testEntries.stream();
	}

	@Disabled("Security is handled externally, thus 401 and 403 cannot happen.")
	@Test
	@Override
	public void patchProductOfferingPrice401() throws Exception {

	}

	@Disabled("Security is handled externally, thus 401 and 403 cannot happen.")
	@Test
	@Override
	public void patchProductOfferingPrice403() throws Exception {

	}

	@Test
	@Override
	public void patchProductOfferingPrice404() throws Exception {
		ProductOfferingPriceUpdateVO productOfferingPriceUpdateVO = ProductOfferingPriceUpdateVOTestExample.build();
		assertEquals(
				HttpStatus.NOT_FOUND,
				callAndCatch(() -> productOfferingPriceApiTestClient.patchProductOfferingPrice(
						"urn:ngsi-ld:product-offering-price:not-existent", productOfferingPriceUpdateVO)).getStatus(),
				"Non existent categories should not be updated.");
	}

	@Disabled("Prohibited by the framework.")
	@Test
	@Override
	public void patchProductOfferingPrice405() throws Exception {

	}

	@Disabled("No implicit creations, cannot happen.")
	@Test
	@Override
	public void patchProductOfferingPrice409() throws Exception {

	}

	@Override
	public void patchProductOfferingPrice500() throws Exception {

	}

	@Test
	@Override
	public void retrieveProductOfferingPrice200() throws Exception {
		Instant currentTimeInstant = Instant.ofEpochSecond(10000);

		when(clock.instant()).thenReturn(currentTimeInstant);

		//first create
		ProductOfferingPriceCreateVO productOfferingPriceCreateVO = ProductOfferingPriceCreateVOTestExample.build();
		// we dont have a parent
		HttpResponse<ProductOfferingPriceVO> createResponse = callAndCatch(
				() -> productOfferingPriceApiTestClient.createProductOfferingPrice(productOfferingPriceCreateVO));
		assertEquals(HttpStatus.CREATED, createResponse.getStatus(),
				"The productOffering should have been created first.");
		String id = createResponse.body().getId();

		ProductOfferingPriceVO expectedProductOfferingPrice = ProductOfferingPriceVOTestExample.build();
		expectedProductOfferingPrice.setId(id);
		expectedProductOfferingPrice.setHref(id);
		expectedProductOfferingPrice.setLastUpdate(currentTimeInstant);
		// empty lists are mapped to null
		expectedProductOfferingPrice.setConstraint(null);
		expectedProductOfferingPrice.setPopRelationship(null);
		expectedProductOfferingPrice.setBundledPopRelationship(null);
		expectedProductOfferingPrice.setPlace(null);

		//then retrieve
		HttpResponse<ProductOfferingPriceVO> retrievedPOP = callAndCatch(
				() -> productOfferingPriceApiTestClient.retrieveProductOfferingPrice(id, null));
		assertEquals(HttpStatus.OK, retrievedPOP.getStatus(), "The retrieval should be ok.");
		assertEquals(expectedProductOfferingPrice, retrievedPOP.body(),
				"The correct productOffering should be returned.");
	}

	@Disabled("400 cannot happen, only 404")
	@Test
	@Override
	public void retrieveProductOfferingPrice400() throws Exception {

	}

	@Disabled("Security is handled externally, thus 401 and 403 cannot happen.")
	@Test
	@Override
	public void retrieveProductOfferingPrice401() throws Exception {

	}

	@Disabled("Security is handled externally, thus 401 and 403 cannot happen.")
	@Test
	@Override
	public void retrieveProductOfferingPrice403() throws Exception {
	}

	@Test
	@Override
	public void retrieveProductOfferingPrice404() throws Exception {
		HttpResponse<ProductOfferingPriceVO> response = callAndCatch(
				() -> productOfferingPriceApiTestClient.retrieveProductOfferingPrice(
						"urn:ngsi-ld:productOffering:non-existent", null));
		assertEquals(HttpStatus.NOT_FOUND, response.getStatus(), "No such productOffering should exist.");

		Optional<ErrorDetails> optionalErrorDetails = response.getBody(ErrorDetails.class);
		assertTrue(optionalErrorDetails.isPresent(), "Error details should have been provided.");
	}

	@Disabled("Prohibited by the framework.")
	@Test
	@Override
	public void retrieveProductOfferingPrice405() throws Exception {

	}

	@Disabled("Conflict not possible on retrieval")
	@Test
	@Override
	public void retrieveProductOfferingPrice409() throws Exception {

	}

	@Override
	public void retrieveProductOfferingPrice500() throws Exception {

	}

	@Override
	protected String getEntityType() {
		return ProductOfferingPrice.TYPE_PRODUCT_OFFERING_PRICE;
	}
}


