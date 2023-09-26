package org.fiware.tmforum.productcatalog;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import io.micronaut.test.annotation.MockBean;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import org.fiware.ngsi.api.EntitiesApiClient;
import org.fiware.productcatalog.api.ProductSpecificationApiTestClient;
import org.fiware.productcatalog.api.ProductSpecificationApiTestSpec;
import org.fiware.productcatalog.model.BundledProductSpecificationVOTestExample;
import org.fiware.productcatalog.model.ProductSpecificationCharacteristicRelationshipVOTestExample;
import org.fiware.productcatalog.model.ProductSpecificationCharacteristicVOTestExample;
import org.fiware.productcatalog.model.ProductSpecificationCreateVO;
import org.fiware.productcatalog.model.ProductSpecificationCreateVOTestExample;
import org.fiware.productcatalog.model.ProductSpecificationRelationshipVOTestExample;
import org.fiware.productcatalog.model.ProductSpecificationUpdateVO;
import org.fiware.productcatalog.model.ProductSpecificationUpdateVOTestExample;
import org.fiware.productcatalog.model.ProductSpecificationVO;
import org.fiware.productcatalog.model.ProductSpecificationVOTestExample;
import org.fiware.productcatalog.model.RelatedPartyVOTestExample;
import org.fiware.productcatalog.model.ResourceSpecificationRefVOTestExample;
import org.fiware.productcatalog.model.ServiceSpecificationRefVOTestExample;
import org.fiware.productcatalog.model.TimePeriodVO;
import org.fiware.productcatalog.model.TimePeriodVOTestExample;
import org.fiware.tmforum.common.notification.EventHandler;
import org.fiware.tmforum.common.configuration.GeneralProperties;
import org.fiware.tmforum.common.exception.ErrorDetails;
import org.fiware.tmforum.common.test.AbstractApiIT;
import org.fiware.tmforum.product.ProductSpecification;
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
public class ProductSpecificationApiIT extends AbstractApiIT implements ProductSpecificationApiTestSpec {

	public final ProductSpecificationApiTestClient productSpecificationApiTestClient;

	private String message;
	private ProductSpecificationCreateVO productSpecificationCreateVO;
	private ProductSpecificationUpdateVO productSpecificationUpdateVO;
	private ProductSpecificationVO expectedProductOfferingPrice;

	private Clock clock = mock(Clock.class);

	public ProductSpecificationApiIT(ProductSpecificationApiTestClient productSpecificationApiTestClient,
			EntitiesApiClient entitiesApiClient, ObjectMapper objectMapper, GeneralProperties generalProperties) {
		super(entitiesApiClient, objectMapper, generalProperties);
		this.productSpecificationApiTestClient = productSpecificationApiTestClient;
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
	@MethodSource("provideValidProductSpecifications")
	public void createProductSpecification201(String message, ProductSpecificationCreateVO productSpecificationCreateVO,
			ProductSpecificationVO expectedProductOfferingPrice) throws Exception {
		this.message = message;
		this.productSpecificationCreateVO = productSpecificationCreateVO;
		this.expectedProductOfferingPrice = expectedProductOfferingPrice;
		createProductSpecification201();
	}

	@Override
	public void createProductSpecification201() throws Exception {

		Instant currentTimeInstant = Instant.ofEpochSecond(10000);

		when(clock.instant()).thenReturn(currentTimeInstant);

		HttpResponse<ProductSpecificationVO> productSpecificationVOHttpResponse = callAndCatch(
				() -> productSpecificationApiTestClient.createProductSpecification(productSpecificationCreateVO));
		assertEquals(HttpStatus.CREATED, productSpecificationVOHttpResponse.getStatus(), message);
		String productSpecificationId = productSpecificationVOHttpResponse.body().getId();

		expectedProductOfferingPrice.setId(productSpecificationId);
		expectedProductOfferingPrice.setHref(productSpecificationId);
		expectedProductOfferingPrice.setLastUpdate(currentTimeInstant);

		assertEquals(expectedProductOfferingPrice, productSpecificationVOHttpResponse.body(), message);
	}

	private static Stream<Arguments> provideValidProductSpecifications() {
		List<Arguments> testEntries = new ArrayList<>();

		ProductSpecificationCreateVO productSpecificationCreateVO = ProductSpecificationCreateVOTestExample.build();

		ProductSpecificationVO expectedProductOfferingPrice = ProductSpecificationVOTestExample.build();
		testEntries.add(Arguments.of("An empty product spec should have been created.", productSpecificationCreateVO,
				expectedProductOfferingPrice));

		return testEntries.stream();
	}

	@ParameterizedTest
	@MethodSource("provideInvalidProductSpecifications")
	public void createProductSpecification400(String message, ProductSpecificationCreateVO invalidCreateVO)
			throws Exception {
		this.message = message;
		this.productSpecificationCreateVO = invalidCreateVO;
		createProductSpecification400();
	}

	@Override
	public void createProductSpecification400() throws Exception {
		HttpResponse<ProductSpecificationVO> creationResponse = callAndCatch(
				() -> productSpecificationApiTestClient.createProductSpecification(productSpecificationCreateVO));
		assertEquals(HttpStatus.BAD_REQUEST, creationResponse.getStatus(), message);
		Optional<ErrorDetails> optionalErrorDetails = creationResponse.getBody(ErrorDetails.class);
		assertTrue(optionalErrorDetails.isPresent(), "Error details should be provided.");
	}

	private static Stream<Arguments> provideInvalidProductSpecifications() {
		List<Arguments> testEntries = new ArrayList<>();

		testEntries.add(Arguments.of("A product spec with invalid bundled ps rel should not be created.",
				ProductSpecificationCreateVOTestExample.build()
						.bundledProductSpecification(List.of(BundledProductSpecificationVOTestExample.build()))));
		testEntries.add(Arguments.of("A product spec with non-existent bundled ps rel should not be created.",
				ProductSpecificationCreateVOTestExample.build().bundledProductSpecification(
						List.of(BundledProductSpecificationVOTestExample.build()
								.id("urn:ngsi-ld:product-specification:non-existent")))));

		testEntries.add(Arguments.of("A product spec with invalid product spec relationship should not be created.",
				ProductSpecificationCreateVOTestExample.build().productSpecificationRelationship(
						List.of(ProductSpecificationRelationshipVOTestExample.build()))));
		testEntries.add(
				Arguments.of("A product spec with non-existent product spec relationship should not be created.",
						ProductSpecificationCreateVOTestExample.build().productSpecificationRelationship(
								List.of(ProductSpecificationRelationshipVOTestExample.build()
										.id("urn:ngsi-ld:product-specification:non-existent")))));

		testEntries.add(Arguments.of("A product spec with invalid related party should not be created.",
				ProductSpecificationCreateVOTestExample.build()
						.relatedParty(List.of(RelatedPartyVOTestExample.build()))));
		testEntries.add(Arguments.of("A product spec with non-existent related party should not be created.",
				ProductSpecificationCreateVOTestExample.build().relatedParty(
						List.of(RelatedPartyVOTestExample.build().id("urn:ngsi-ld:organization:non-existent")))));

		testEntries.add(Arguments.of("A product spec with invalid resource spec should not be created.",
				ProductSpecificationCreateVOTestExample.build()
						.resourceSpecification(List.of(ResourceSpecificationRefVOTestExample.build()))));
		testEntries.add(Arguments.of("A product spec with non-existent resource spec should not be created.",
				ProductSpecificationCreateVOTestExample.build().resourceSpecification(
						List.of(ResourceSpecificationRefVOTestExample.build()
								.id("urn:ngsi-ld:resource:non-existent")))));

		testEntries.add(Arguments.of("A product spec with invalid service spec should not be created.",
				ProductSpecificationCreateVOTestExample.build()
						.serviceSpecification(List.of(ServiceSpecificationRefVOTestExample.build()))));
		testEntries.add(Arguments.of("A product spec with non-existent service spec should not be created.",
				ProductSpecificationCreateVOTestExample.build().serviceSpecification(
						List.of(ServiceSpecificationRefVOTestExample.build().id("urn:ngsi-ld:service:non-existent")))));

		testEntries.add(Arguments.of("A product spec with invalid prod-spec-characteristic should not be created.",
				ProductSpecificationCreateVOTestExample.build().productSpecCharacteristic(List.of(
						ProductSpecificationCharacteristicVOTestExample.build()
								.productSpecCharRelationship(
										List.of(ProductSpecificationCharacteristicRelationshipVOTestExample.build()))))));
		return testEntries.stream();
	}

	@Disabled("Security is handled externally, thus 401 and 403 cannot happen.")
	@Test
	@Override
	public void createProductSpecification401() throws Exception {
	}

	@Disabled("Security is handled externally, thus 401 and 403 cannot happen.")
	@Test
	@Override
	public void createProductSpecification403() throws Exception {
	}

	@Disabled("Prohibited by the framework.")
	@Test
	@Override
	public void createProductSpecification405() throws Exception {

	}

	@Disabled("Product spec doesn't have 'implicit' entities and id is generated by the implementation, no conflict possible.")
	@Test
	@Override
	public void createProductSpecification409() throws Exception {

	}

	@Override
	public void createProductSpecification500() throws Exception {

	}

	@Test
	@Override
	public void deleteProductSpecification204() throws Exception {
		//first create
		ProductSpecificationCreateVO productSpecificationCreateVO = ProductSpecificationCreateVOTestExample.build();

		HttpResponse<ProductSpecificationVO> createResponse = callAndCatch(
				() -> productSpecificationApiTestClient.createProductSpecification(productSpecificationCreateVO));
		assertEquals(HttpStatus.CREATED, createResponse.getStatus(),
				"The productSpecification should have been created first.");

		String popId = createResponse.body().getId();

		assertEquals(HttpStatus.NO_CONTENT,
				callAndCatch(() -> productSpecificationApiTestClient.deleteProductSpecification(popId)).getStatus(),
				"The productSpecification should have been deleted.");

		assertEquals(HttpStatus.NOT_FOUND,
				callAndCatch(
						() -> productSpecificationApiTestClient.retrieveProductSpecification(popId, null)).status(),
				"The productSpecification should not exist anymore.");

	}

	@Disabled("400 is impossible to happen on deletion with the current implementation.")
	@Test
	@Override
	public void deleteProductSpecification400() throws Exception {

	}

	@Disabled("Security is handled externally, thus 401 and 403 cannot happen.")
	@Test
	@Override
	public void deleteProductSpecification401() throws Exception {

	}

	@Disabled("Security is handled externally, thus 401 and 403 cannot happen.")
	@Test
	@Override
	public void deleteProductSpecification403() throws Exception {

	}

	@Test
	@Override
	public void deleteProductSpecification404() throws Exception {
		HttpResponse<?> notFoundResponse = callAndCatch(
				() -> productSpecificationApiTestClient.deleteProductSpecification(
						"urn:ngsi-ld:productSpecification:no-pop"));
		assertEquals(HttpStatus.NOT_FOUND,
				notFoundResponse.getStatus(),
				"No such productSpecification should exist.");

		Optional<ErrorDetails> optionalErrorDetails = notFoundResponse.getBody(ErrorDetails.class);
		assertTrue(optionalErrorDetails.isPresent(), "Error details should be provided.");

		notFoundResponse = callAndCatch(
				() -> productSpecificationApiTestClient.deleteProductSpecification("invalid-id"));
		assertEquals(HttpStatus.NOT_FOUND,
				notFoundResponse.getStatus(),
				"No such productSpecification should exist.");

		optionalErrorDetails = notFoundResponse.getBody(ErrorDetails.class);
		assertTrue(optionalErrorDetails.isPresent(), "Error details should be provided.");
	}

	@Disabled("Prohibited by the framework.")
	@Test
	@Override
	public void deleteProductSpecification405() throws Exception {

	}

	@Disabled("Impossible status.")
	@Test
	@Override
	public void deleteProductSpecification409() throws Exception {

	}

	@Override
	public void deleteProductSpecification500() throws Exception {

	}

	@Test
	@Override
	public void listProductSpecification200() throws Exception {

		List<ProductSpecificationVO> expectedProductSpecifications = new ArrayList<>();
		for (int i = 0; i < 10; i++) {
			ProductSpecificationCreateVO productSpecificationCreateVO = ProductSpecificationCreateVOTestExample.build();
			String id = productSpecificationApiTestClient.createProductSpecification(productSpecificationCreateVO)
					.body().getId();
			ProductSpecificationVO productSpecificationVO = ProductSpecificationVOTestExample.build();
			productSpecificationVO
					.id(id)
					.href(id)
					.bundledProductSpecification(null)
					.productSpecificationRelationship(null)
					.relatedParty(null)
					.resourceSpecification(null)
					.serviceSpecification(null);
			expectedProductSpecifications.add(productSpecificationVO);
		}

		HttpResponse<List<ProductSpecificationVO>> productSpecificationResponse = callAndCatch(
				() -> productSpecificationApiTestClient.listProductSpecification(null, null, null));

		assertEquals(HttpStatus.OK, productSpecificationResponse.getStatus(), "The list should be accessible.");
		assertEquals(expectedProductSpecifications.size(), productSpecificationResponse.getBody().get().size(),
				"All productSpecifications should have been returned.");
		List<ProductSpecificationVO> retrievedProductSpecifications = productSpecificationResponse.getBody().get();

		Map<String, ProductSpecificationVO> retrievedMap = retrievedProductSpecifications.stream()
				.collect(Collectors.toMap(productSpecification -> productSpecification.getId(),
						productSpecification -> productSpecification));

		expectedProductSpecifications.stream()
				.forEach(
						expectedProductSpecification -> assertTrue(
								retrievedMap.containsKey(expectedProductSpecification.getId()),
								String.format("All created productSpecifications should be returned - Missing: %s.",
										expectedProductSpecification,
										retrievedProductSpecifications)));
		expectedProductSpecifications.stream().forEach(
				expectedProductSpecification -> assertEquals(expectedProductSpecification,
						retrievedMap.get(expectedProductSpecification.getId()),
						"The correct productSpecifications should be retrieved."));

		// get with pagination
		Integer limit = 5;
		HttpResponse<List<ProductSpecificationVO>> firstPartResponse = callAndCatch(
				() -> productSpecificationApiTestClient.listProductSpecification(null, 0, limit));
		assertEquals(limit, firstPartResponse.body().size(),
				"Only the requested number of entries should be returend.");
		HttpResponse<List<ProductSpecificationVO>> secondPartResponse = callAndCatch(
				() -> productSpecificationApiTestClient.listProductSpecification(null, 0 + limit, limit));
		assertEquals(limit, secondPartResponse.body().size(),
				"Only the requested number of entries should be returend.");

		retrievedProductSpecifications.clear();
		retrievedProductSpecifications.addAll(firstPartResponse.body());
		retrievedProductSpecifications.addAll(secondPartResponse.body());
		expectedProductSpecifications.stream()
				.forEach(
						expectedProductSpecification -> assertTrue(
								retrievedMap.containsKey(expectedProductSpecification.getId()),
								String.format("All created productSpecifications should be returned - Missing: %s.",
										expectedProductSpecification)));
		expectedProductSpecifications.stream().forEach(
				expectedProductSpecification -> assertEquals(expectedProductSpecification,
						retrievedMap.get(expectedProductSpecification.getId()),
						"The correct productSpecifications should be retrieved."));
	}

	@Test
	@Override
	public void listProductSpecification400() throws Exception {
		HttpResponse<List<ProductSpecificationVO>> badRequestResponse = callAndCatch(
				() -> productSpecificationApiTestClient.listProductSpecification(null, -1, null));
		assertEquals(HttpStatus.BAD_REQUEST,
				badRequestResponse.getStatus(),
				"Negative offsets are impossible.");

		Optional<ErrorDetails> optionalErrorDetails = badRequestResponse.getBody(ErrorDetails.class);
		assertTrue(optionalErrorDetails.isPresent(), "Error details should be provided.");

		badRequestResponse = callAndCatch(
				() -> productSpecificationApiTestClient.listProductSpecification(null, null, -1));
		assertEquals(HttpStatus.BAD_REQUEST,
				badRequestResponse.getStatus(),
				"Negative limits are impossible.");
		optionalErrorDetails = badRequestResponse.getBody(ErrorDetails.class);
		assertTrue(optionalErrorDetails.isPresent(), "Error details should be provided.");
	}

	@Disabled("Security is handled externally, thus 401 and 403 cannot happen.")
	@Test
	@Override
	public void listProductSpecification401() throws Exception {

	}

	@Disabled("Security is handled externally, thus 401 and 403 cannot happen.")
	@Test
	@Override
	public void listProductSpecification403() throws Exception {

	}

	@Disabled("Not found is not possible here, will be answered with an empty list instead.")
	@Test
	@Override
	public void listProductSpecification404() throws Exception {

	}

	@Disabled("Prohibited by the framework.")
	@Test
	@Override
	public void listProductSpecification405() throws Exception {

	}

	@Disabled("Impossible status.")
	@Test
	@Override
	public void listProductSpecification409() throws Exception {

	}

	@Override
	public void listProductSpecification500() throws Exception {

	}

	@ParameterizedTest
	@MethodSource("provideProductOfferingUpdates")
	public void patchProductSpecification200(String message, ProductSpecificationUpdateVO productSpecificationUpdateVO,
			ProductSpecificationVO expectedProductOfferingPrice) throws Exception {
		this.message = message;
		this.productSpecificationUpdateVO = productSpecificationUpdateVO;
		this.expectedProductOfferingPrice = expectedProductOfferingPrice;
		patchProductSpecification200();
	}

	@Override
	public void patchProductSpecification200() throws Exception {
		//first create
		ProductSpecificationCreateVO productSpecificationCreateVO = ProductSpecificationCreateVOTestExample.build();

		HttpResponse<ProductSpecificationVO> createResponse = callAndCatch(
				() -> productSpecificationApiTestClient.createProductSpecification(productSpecificationCreateVO));
		assertEquals(HttpStatus.CREATED, createResponse.getStatus(), "The catalog should have been created first.");

		String catalogId = createResponse.body().getId();

		HttpResponse<ProductSpecificationVO> updateResponse = callAndCatch(
				() -> productSpecificationApiTestClient.patchProductSpecification(catalogId,
						productSpecificationUpdateVO));
		assertEquals(HttpStatus.OK, updateResponse.getStatus(), message);

		ProductSpecificationVO updatedCatalog = updateResponse.body();
		expectedProductOfferingPrice.setHref(catalogId);
		expectedProductOfferingPrice.setId(catalogId);
		// empty list mapping
		expectedProductOfferingPrice.setBundledProductSpecification(null);
		expectedProductOfferingPrice.setProductSpecificationRelationship(null);
		expectedProductOfferingPrice.setRelatedParty(null);
		expectedProductOfferingPrice.setResourceSpecification(null);
		expectedProductOfferingPrice.setServiceSpecification(null);

		assertEquals(expectedProductOfferingPrice, updatedCatalog, message);
	}

	private static Stream<Arguments> provideProductOfferingUpdates() {
		List<Arguments> testEntries = new ArrayList<>();

		ProductSpecificationUpdateVO newDesc = ProductSpecificationUpdateVOTestExample.build();
		newDesc.setDescription("New description");
		ProductSpecificationVO expectedNewDesc = ProductSpecificationVOTestExample.build();
		expectedNewDesc.setDescription("New description");
		testEntries.add(Arguments.of("The description should have been updated.", newDesc, expectedNewDesc));

		ProductSpecificationUpdateVO newLifeCycle = ProductSpecificationUpdateVOTestExample.build();
		newLifeCycle.setLifecycleStatus("Dead");
		ProductSpecificationVO expectedNewLifeCycle = ProductSpecificationVOTestExample.build();
		expectedNewLifeCycle.setLifecycleStatus("Dead");
		testEntries.add(
				Arguments.of("The lifecycle state should have been updated.", newLifeCycle, expectedNewLifeCycle));

		ProductSpecificationUpdateVO newName = ProductSpecificationUpdateVOTestExample.build();
		newName.setName("New name");
		ProductSpecificationVO expectedNewName = ProductSpecificationVOTestExample.build();
		expectedNewName.setName("New name");
		testEntries.add(Arguments.of("The name should have been updated.", newName, expectedNewName));

		ProductSpecificationUpdateVO newVersion = ProductSpecificationUpdateVOTestExample.build();
		newVersion.setVersion("1.23.1");
		ProductSpecificationVO expectedNewVersion = ProductSpecificationVOTestExample.build();
		expectedNewVersion.setVersion("1.23.1");
		testEntries.add(Arguments.of("The version should have been updated.", newVersion, expectedNewVersion));

		ProductSpecificationUpdateVO newValidFor = ProductSpecificationUpdateVOTestExample.build();
		TimePeriodVO timePeriodVO = TimePeriodVOTestExample.build();
		timePeriodVO.setEndDateTime(Instant.now());
		timePeriodVO.setStartDateTime(Instant.now());
		newValidFor.setValidFor(timePeriodVO);
		ProductSpecificationVO expectedNewValidFor = ProductSpecificationVOTestExample.build();
		expectedNewValidFor.setValidFor(timePeriodVO);
		testEntries.add(Arguments.of("The validFor should have been updated.", newValidFor, expectedNewValidFor));

		return testEntries.stream();
	}

	@ParameterizedTest
	@MethodSource("provideInvalidUpdates")
	public void patchProductSpecification400(String message, ProductSpecificationUpdateVO invalidUpdateVO)
			throws Exception {
		this.message = message;
		this.productSpecificationUpdateVO = invalidUpdateVO;
		patchProductSpecification400();
	}

	@Override
	public void patchProductSpecification400() throws Exception {
		//first create
		ProductSpecificationCreateVO productSpecificationCreateVO = ProductSpecificationCreateVOTestExample.build();
		HttpResponse<ProductSpecificationVO> createResponse = callAndCatch(
				() -> productSpecificationApiTestClient.createProductSpecification(productSpecificationCreateVO));
		assertEquals(HttpStatus.CREATED, createResponse.getStatus(), "The catalog should have been created first.");

		String productSpecId = createResponse.body().getId();

		HttpResponse<ProductSpecificationVO> updateResponse = callAndCatch(
				() -> productSpecificationApiTestClient.patchProductSpecification(productSpecId,
						productSpecificationUpdateVO));
		assertEquals(HttpStatus.BAD_REQUEST, updateResponse.getStatus(), message);

		Optional<ErrorDetails> optionalErrorDetails = updateResponse.getBody(ErrorDetails.class);
		assertTrue(optionalErrorDetails.isPresent(), "Some error details should be present.");
	}

	private static Stream<Arguments> provideInvalidUpdates() {
		List<Arguments> testEntries = new ArrayList<>();

		testEntries.add(Arguments.of("A product spec with invalid bundled ps rel should not be created.",
				ProductSpecificationUpdateVOTestExample.build()
						.bundledProductSpecification(List.of(BundledProductSpecificationVOTestExample.build()))));
		testEntries.add(Arguments.of("A product spec with non-existent bundled ps rel should not be created.",
				ProductSpecificationUpdateVOTestExample.build().bundledProductSpecification(
						List.of(BundledProductSpecificationVOTestExample.build()
								.id("urn:ngsi-ld:product-specification:non-existent")))));

		testEntries.add(Arguments.of("A product spec with invalid product spec relationship should not be created.",
				ProductSpecificationUpdateVOTestExample.build().productSpecificationRelationship(
						List.of(ProductSpecificationRelationshipVOTestExample.build()))));
		testEntries.add(
				Arguments.of("A product spec with non-existent product spec relationship should not be created.",
						ProductSpecificationUpdateVOTestExample.build().productSpecificationRelationship(
								List.of(ProductSpecificationRelationshipVOTestExample.build()
										.id("urn:ngsi-ld:product-specification:non-existent")))));

		testEntries.add(Arguments.of("A product spec with invalid related party should not be created.",
				ProductSpecificationUpdateVOTestExample.build()
						.relatedParty(List.of(RelatedPartyVOTestExample.build()))));
		testEntries.add(Arguments.of("A product spec with non-existent related party should not be created.",
				ProductSpecificationUpdateVOTestExample.build().relatedParty(
						List.of(RelatedPartyVOTestExample.build().id("urn:ngsi-ld:organization:non-existent")))));

		testEntries.add(Arguments.of("A product spec with invalid resource spec should not be created.",
				ProductSpecificationUpdateVOTestExample.build()
						.resourceSpecification(List.of(ResourceSpecificationRefVOTestExample.build()))));
		testEntries.add(Arguments.of("A product spec with non-existent resource spec should not be created.",
				ProductSpecificationUpdateVOTestExample.build().resourceSpecification(
						List.of(ResourceSpecificationRefVOTestExample.build()
								.id("urn:ngsi-ld:resource:non-existent")))));

		testEntries.add(Arguments.of("A product spec with invalid service spec should not be created.",
				ProductSpecificationUpdateVOTestExample.build()
						.serviceSpecification(List.of(ServiceSpecificationRefVOTestExample.build()))));
		testEntries.add(Arguments.of("A product spec with non-existent service spec should not be created.",
				ProductSpecificationUpdateVOTestExample.build().serviceSpecification(
						List.of(ServiceSpecificationRefVOTestExample.build().id("urn:ngsi-ld:service:non-existent")))));

		testEntries.add(Arguments.of("A product spec with invalid prod-spec-characteristic should not be created.",
				ProductSpecificationUpdateVOTestExample.build().productSpecCharacteristic(List.of(
						ProductSpecificationCharacteristicVOTestExample.build()
								.productSpecCharRelationship(
										List.of(ProductSpecificationCharacteristicRelationshipVOTestExample.build()))))));

		return testEntries.stream();
	}

	@Disabled("Security is handled externally, thus 401 and 403 cannot happen.")
	@Test
	@Override
	public void patchProductSpecification401() throws Exception {

	}

	@Disabled("Security is handled externally, thus 401 and 403 cannot happen.")
	@Test
	@Override
	public void patchProductSpecification403() throws Exception {

	}

	@Test
	@Override
	public void patchProductSpecification404() throws Exception {
		ProductSpecificationUpdateVO productSpecificationUpdateVO = ProductSpecificationUpdateVOTestExample.build();
		assertEquals(
				HttpStatus.NOT_FOUND,
				callAndCatch(() -> productSpecificationApiTestClient.patchProductSpecification(
						"urn:ngsi-ld:productSpecification:not-existent", productSpecificationUpdateVO)).getStatus(),
				"Non existent categories should not be updated.");
	}

	@Disabled("Prohibited by the framework.")
	@Test
	@Override
	public void patchProductSpecification405() throws Exception {

	}

	@Disabled("No implicit creations, cannot happen.")
	@Test
	@Override
	public void patchProductSpecification409() throws Exception {

	}

	@Override
	public void patchProductSpecification500() throws Exception {

	}

	@Test
	@Override
	public void retrieveProductSpecification200() throws Exception {
		Instant currentTimeInstant = Instant.ofEpochSecond(10000);

		when(clock.instant()).thenReturn(currentTimeInstant);

		//first create
		ProductSpecificationCreateVO productSpecificationCreateVO = ProductSpecificationCreateVOTestExample.build();
		// we dont have a parent
		HttpResponse<ProductSpecificationVO> createResponse = callAndCatch(
				() -> productSpecificationApiTestClient.createProductSpecification(productSpecificationCreateVO));
		assertEquals(HttpStatus.CREATED, createResponse.getStatus(),
				"The productSpecification should have been created first.");
		String id = createResponse.body().getId();

		ProductSpecificationVO expectedProductOfferingPrice = ProductSpecificationVOTestExample.build();
		expectedProductOfferingPrice.setId(id);
		expectedProductOfferingPrice.setHref(id);
		expectedProductOfferingPrice.setLastUpdate(currentTimeInstant);
		// empty lists are mapped to null
		expectedProductOfferingPrice.setBundledProductSpecification(null);
		expectedProductOfferingPrice.setProductSpecificationRelationship(null);
		expectedProductOfferingPrice.setRelatedParty(null);
		expectedProductOfferingPrice.setResourceSpecification(null);
		expectedProductOfferingPrice.setServiceSpecification(null);

		//then retrieve
		HttpResponse<ProductSpecificationVO> retrievedPOP = callAndCatch(
				() -> productSpecificationApiTestClient.retrieveProductSpecification(id, null));
		assertEquals(HttpStatus.OK, retrievedPOP.getStatus(), "The retrieval should be ok.");
		assertEquals(expectedProductOfferingPrice, retrievedPOP.body(),
				"The correct productSpecification should be returned.");
	}

	@Disabled("400 cannot happen, only 404")
	@Test
	@Override
	public void retrieveProductSpecification400() throws Exception {

	}

	@Disabled("Security is handled externally, thus 401 and 403 cannot happen.")
	@Test
	@Override
	public void retrieveProductSpecification401() throws Exception {

	}

	@Disabled("Security is handled externally, thus 401 and 403 cannot happen.")
	@Test
	@Override
	public void retrieveProductSpecification403() throws Exception {
	}

	@Test
	@Override
	public void retrieveProductSpecification404() throws Exception {
		HttpResponse<ProductSpecificationVO> response = callAndCatch(
				() -> productSpecificationApiTestClient.retrieveProductSpecification(
						"urn:ngsi-ld:product-specification:non-existent", null));
		assertEquals(HttpStatus.NOT_FOUND, response.getStatus(), "No such productSpecification should exist.");

		Optional<ErrorDetails> optionalErrorDetails = response.getBody(ErrorDetails.class);
		assertTrue(optionalErrorDetails.isPresent(), "Error details should have been provided.");
	}

	@Disabled("Prohibited by the framework.")
	@Test
	@Override
	public void retrieveProductSpecification405() throws Exception {

	}

	@Disabled("Conflict not possible on retrieval")
	@Test
	@Override
	public void retrieveProductSpecification409() throws Exception {

	}

	@Override
	public void retrieveProductSpecification500() throws Exception {

	}

	@Override protected String getEntityType() {
		return ProductSpecification.TYPE_PRODUCT_SPECIFICATION;
	}
}


