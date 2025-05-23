package org.fiware.tmforum.productcatalog;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import io.micronaut.test.annotation.MockBean;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import org.fiware.ngsi.api.EntitiesApiClient;
import org.fiware.ngsi.model.EntityVO;
import org.fiware.ngsi.model.PropertyVO;
import org.fiware.productcatalog.api.ProductSpecificationApiTestClient;
import org.fiware.productcatalog.api.ProductSpecificationApiTestSpec;
import org.fiware.productcatalog.model.*;
import org.fiware.tmforum.common.configuration.GeneralProperties;
import org.fiware.tmforum.common.exception.ErrorDetails;
import org.fiware.tmforum.common.notification.TMForumEventHandler;
import org.fiware.tmforum.common.test.AbstractApiIT;
import org.fiware.tmforum.product.ProductSpecification;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import reactor.core.publisher.Mono;

import java.net.URI;
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

@MicronautTest(packages = {"org.fiware.tmforum.productcatalog"})
public class ProductSpecificationApiIT extends AbstractApiIT implements ProductSpecificationApiTestSpec {

	public final ProductSpecificationApiTestClient productSpecificationApiTestClient;
	private final EntitiesApiClient entitiesApi;
	private String message;
	private ProductSpecificationCreateVO productSpecificationCreateVO;
	private ProductSpecificationUpdateVO productSpecificationUpdateVO;
	private ProductSpecificationVO expectedProductOfferingPrice;

	private Clock clock = mock(Clock.class);

	public ProductSpecificationApiIT(ProductSpecificationApiTestClient productSpecificationApiTestClient,
									 EntitiesApiClient entitiesApiClient, ObjectMapper objectMapper,
									 GeneralProperties generalProperties, EntitiesApiClient entitiesApi) {
		super(entitiesApiClient, objectMapper, generalProperties);
		this.productSpecificationApiTestClient = productSpecificationApiTestClient;
		this.entitiesApi = entitiesApi;
	}

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
				() -> productSpecificationApiTestClient.createProductSpecification(null, productSpecificationCreateVO));
		assertEquals(HttpStatus.CREATED, productSpecificationVOHttpResponse.getStatus(), message);
		String productSpecificationId = productSpecificationVOHttpResponse.body().getId();

		expectedProductOfferingPrice.setId(productSpecificationId);
		expectedProductOfferingPrice.setHref(productSpecificationId);
		expectedProductOfferingPrice.setLastUpdate(currentTimeInstant);

		assertEquals(expectedProductOfferingPrice, productSpecificationVOHttpResponse.body(), message);
	}

	private static Stream<Arguments> provideValidProductSpecifications() {
		List<Arguments> testEntries = new ArrayList<>();

		ProductSpecificationCreateVO productSpecificationCreateVO = ProductSpecificationCreateVOTestExample.build()
				.atSchemaLocation(null)
				.targetProductSchema(null);

		ProductSpecificationVO expectedProductOfferingPrice = ProductSpecificationVOTestExample.build()
				.atSchemaLocation(null)
				.targetProductSchema(null);
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
				() -> productSpecificationApiTestClient.createProductSpecification(null, productSpecificationCreateVO));
		assertEquals(HttpStatus.BAD_REQUEST, creationResponse.getStatus(), message);
		Optional<ErrorDetails> optionalErrorDetails = creationResponse.getBody(ErrorDetails.class);
		assertTrue(optionalErrorDetails.isPresent(), "Error details should be provided.");
	}

	private static Stream<Arguments> provideInvalidProductSpecifications() {
		List<Arguments> testEntries = new ArrayList<>();

		testEntries.add(Arguments.of("A product spec with invalid bundled ps rel should not be created.",
				ProductSpecificationCreateVOTestExample.build().atSchemaLocation(null)
						.bundledProductSpecification(List.of(BundledProductSpecificationVOTestExample.build().atSchemaLocation(null)))));
		testEntries.add(Arguments.of("A product spec with non-existent bundled ps rel should not be created.",
				ProductSpecificationCreateVOTestExample.build().atSchemaLocation(null).bundledProductSpecification(
						List.of(BundledProductSpecificationVOTestExample.build().atSchemaLocation(null)
								.id("urn:ngsi-ld:product-specification:non-existent")))));

		testEntries.add(Arguments.of("A product spec with invalid product spec relationship should not be created.",
				ProductSpecificationCreateVOTestExample.build().atSchemaLocation(null).productSpecificationRelationship(
						List.of(ProductSpecificationRelationshipVOTestExample.build().atSchemaLocation(null)))));
		testEntries.add(
				Arguments.of("A product spec with non-existent product spec relationship should not be created.",
						ProductSpecificationCreateVOTestExample.build().atSchemaLocation(null).productSpecificationRelationship(
								List.of(ProductSpecificationRelationshipVOTestExample.build().atSchemaLocation(null)
										.id("urn:ngsi-ld:product-specification:non-existent")))));

		testEntries.add(Arguments.of("A product spec with invalid related party should not be created.",
				ProductSpecificationCreateVOTestExample.build().atSchemaLocation(null)
						.relatedParty(List.of(RelatedPartyVOTestExample.build().atSchemaLocation(null)))));
		testEntries.add(Arguments.of("A product spec with non-existent related party should not be created.",
				ProductSpecificationCreateVOTestExample.build().atSchemaLocation(null).relatedParty(
						List.of(RelatedPartyVOTestExample.build().atSchemaLocation(null).id("urn:ngsi-ld:organization:non-existent")))));

		testEntries.add(Arguments.of("A product spec with invalid resource spec should not be created.",
				ProductSpecificationCreateVOTestExample.build().atSchemaLocation(null)
						.resourceSpecification(List.of(ResourceSpecificationRefVOTestExample.build().atSchemaLocation(null)))));
		testEntries.add(Arguments.of("A product spec with non-existent resource spec should not be created.",
				ProductSpecificationCreateVOTestExample.build().atSchemaLocation(null).resourceSpecification(
						List.of(ResourceSpecificationRefVOTestExample.build().atSchemaLocation(null)
								.id("urn:ngsi-ld:resource:non-existent")))));

		testEntries.add(Arguments.of("A product spec with invalid service spec should not be created.",
				ProductSpecificationCreateVOTestExample.build().atSchemaLocation(null)
						.serviceSpecification(List.of(ServiceSpecificationRefVOTestExample.build().atSchemaLocation(null)))));
		testEntries.add(Arguments.of("A product spec with non-existent service spec should not be created.",
				ProductSpecificationCreateVOTestExample.build().atSchemaLocation(null).serviceSpecification(
						List.of(ServiceSpecificationRefVOTestExample.build().atSchemaLocation(null).id("urn:ngsi-ld:service:non-existent")))));

		testEntries.add(Arguments.of("A product spec with invalid prod-spec-characteristic should not be created.",
				ProductSpecificationCreateVOTestExample.build().atSchemaLocation(null).productSpecCharacteristic(List.of(
						ProductSpecificationCharacteristicVOTestExample.build().atSchemaLocation(null)
								.productSpecCharRelationship(
										List.of(ProductSpecificationCharacteristicRelationshipVOTestExample.build().atSchemaLocation(null)))))));
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
		ProductSpecificationCreateVO productSpecificationCreateVO = ProductSpecificationCreateVOTestExample.build()
				.atSchemaLocation(null)
				.targetProductSchema(null);

		HttpResponse<ProductSpecificationVO> createResponse = callAndCatch(
				() -> productSpecificationApiTestClient.createProductSpecification(null, productSpecificationCreateVO));
		assertEquals(HttpStatus.CREATED, createResponse.getStatus(),
				"The productSpecification should have been created first.");

		String popId = createResponse.body().getId();

		assertEquals(HttpStatus.NO_CONTENT,
				callAndCatch(() -> productSpecificationApiTestClient.deleteProductSpecification(null, popId)).getStatus(),
				"The productSpecification should have been deleted.");

		assertEquals(HttpStatus.NOT_FOUND,
				callAndCatch(
						() -> productSpecificationApiTestClient.retrieveProductSpecification(null, popId, null)).status(),
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
						null, "urn:ngsi-ld:productSpecification:no-pop"));
		assertEquals(HttpStatus.NOT_FOUND,
				notFoundResponse.getStatus(),
				"No such productSpecification should exist.");

		Optional<ErrorDetails> optionalErrorDetails = notFoundResponse.getBody(ErrorDetails.class);
		assertTrue(optionalErrorDetails.isPresent(), "Error details should be provided.");

		notFoundResponse = callAndCatch(
				() -> productSpecificationApiTestClient.deleteProductSpecification(null, "invalid-id"));
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
			ProductSpecificationCreateVO productSpecificationCreateVO = ProductSpecificationCreateVOTestExample.build()
					.atSchemaLocation(null)
					.targetProductSchema(null);
			String id = productSpecificationApiTestClient.createProductSpecification(null, productSpecificationCreateVO)
					.body().getId();
			ProductSpecificationVO productSpecificationVO = ProductSpecificationVOTestExample.build()
					.atSchemaLocation(null)
					.targetProductSchema(null)
					.validFor(null)
					.id(id)
					.href(id);
			expectedProductSpecifications.add(productSpecificationVO);
		}

		HttpResponse<List<ProductSpecificationVO>> productSpecificationResponse = callAndCatch(
				() -> productSpecificationApiTestClient.listProductSpecification(null, null, null, null));

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
				() -> productSpecificationApiTestClient.listProductSpecification(null, null, 0, limit));
		assertEquals(limit, firstPartResponse.body().size(),
				"Only the requested number of entries should be returend.");
		HttpResponse<List<ProductSpecificationVO>> secondPartResponse = callAndCatch(
				() -> productSpecificationApiTestClient.listProductSpecification(null, null, 0 + limit, limit));
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
				() -> productSpecificationApiTestClient.listProductSpecification(null, null, -1, null));
		assertEquals(HttpStatus.BAD_REQUEST,
				badRequestResponse.getStatus(),
				"Negative offsets are impossible.");

		Optional<ErrorDetails> optionalErrorDetails = badRequestResponse.getBody(ErrorDetails.class);
		assertTrue(optionalErrorDetails.isPresent(), "Error details should be provided.");

		badRequestResponse = callAndCatch(
				() -> productSpecificationApiTestClient.listProductSpecification(null, null, null, -1));
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
		ProductSpecificationCreateVO productSpecificationCreateVO = ProductSpecificationCreateVOTestExample.build()
				.atSchemaLocation(null)
				.targetProductSchema(null);

		HttpResponse<ProductSpecificationVO> createResponse = callAndCatch(
				() -> productSpecificationApiTestClient.createProductSpecification(null, productSpecificationCreateVO));
		assertEquals(HttpStatus.CREATED, createResponse.getStatus(), "The catalog should have been created first.");

		String catalogId = createResponse.body().getId();

		HttpResponse<ProductSpecificationVO> updateResponse = callAndCatch(
				() -> productSpecificationApiTestClient.patchProductSpecification(null, catalogId,
						productSpecificationUpdateVO));
		assertEquals(HttpStatus.OK, updateResponse.getStatus(), message);

		ProductSpecificationVO updatedCatalog = updateResponse.body();
		expectedProductOfferingPrice.setHref(catalogId);
		expectedProductOfferingPrice.setId(catalogId);

		assertEquals(expectedProductOfferingPrice, updatedCatalog, message);
	}

	@Test
	public void patchSpecEmptyList() throws Exception {

		String resourceSpecId = "urn:ngsi-ld:resource-specification:test-spec";
		try {
			entitiesApi.removeEntityById(URI.create(resourceSpecId), null, null).block();
		} catch (Exception e) {
			// ignore, just for cleanup
			e.getMessage();
		}

		EntityVO resourceSpecEntity = new EntityVO()
				.atContext("https://uri.etsi.org/ngsi-ld/v1/ngsi-ld-core-context.jsonld")
				.id(URI.create(resourceSpecId))
				.type("resource-specification");

		PropertyVO isBundle = new PropertyVO().value(false);
		resourceSpecEntity.setAdditionalProperties("isBundle", isBundle);
		PropertyVO lifecycleStatus = new PropertyVO().value("string");
		resourceSpecEntity.setAdditionalProperties("lifecycleStatus", lifecycleStatus);
		PropertyVO description = new PropertyVO().value("string");
		resourceSpecEntity.setAdditionalProperties("description", description);
		PropertyVO version = new PropertyVO().value("0.1.2");
		resourceSpecEntity.setAdditionalProperties("version", version);
		PropertyVO href = new PropertyVO().value(resourceSpecId);
		resourceSpecEntity.setAdditionalProperties("href", href);
		PropertyVO category = new PropertyVO().value("category");
		resourceSpecEntity.setAdditionalProperties("category", category);
		entitiesApi.createEntity(resourceSpecEntity, null).block();

		ResourceSpecificationRefVO rsrV = new ResourceSpecificationRefVO()
				.atSchemaLocation(null)
				.id(resourceSpecId)
				.href(URI.create(resourceSpecId))
				.name("myTestRsrv");


		//first create
		ProductSpecificationCreateVO productSpecificationCreateVO = ProductSpecificationCreateVOTestExample.build()
				.atSchemaLocation(null)
				.targetProductSchema(null)
				.resourceSpecification(List.of(rsrV));

		HttpResponse<ProductSpecificationVO> createResponse = callAndCatch(
				() -> productSpecificationApiTestClient.createProductSpecification(null, productSpecificationCreateVO));
		assertEquals(HttpStatus.CREATED, createResponse.getStatus(), "The catalog should have been created first.");

		String specId = createResponse.body().getId();

		// null updates should not change anything
		ProductSpecificationUpdateVO productSpecificationUpdateNullList = ProductSpecificationUpdateVOTestExample.build()
				.atSchemaLocation(null)
				.targetProductSchema(null)
				.resourceSpecification(null);
		HttpResponse<ProductSpecificationVO> nullUpdateResponse = callAndCatch(
				() -> productSpecificationApiTestClient.patchProductSpecification(null, specId,
						productSpecificationUpdateNullList));
		assertEquals(HttpStatus.OK, nullUpdateResponse.getStatus(), message);
		assertEquals(1, nullUpdateResponse.body().getResourceSpecification().size(), "If set to null, the spec-list should stay untouched.");

		// empty list updates should empty the list
		ProductSpecificationUpdateVO productSpecificationUpdateEmptyList = ProductSpecificationUpdateVOTestExample.build()
				.atSchemaLocation(null)
				.targetProductSchema(null)
				.resourceSpecification(List.of());
		HttpResponse<ProductSpecificationVO> emptyUpdateResponse = callAndCatch(
				() -> productSpecificationApiTestClient.patchProductSpecification(null, specId,
						productSpecificationUpdateEmptyList));
		assertEquals(HttpStatus.OK, emptyUpdateResponse.getStatus(), message);
		assertEquals(0, emptyUpdateResponse.body().getResourceSpecification().size(), "If set to empty, the spec-list should be emptied.");
	}

	private static Stream<Arguments> provideProductOfferingUpdates() {
		List<Arguments> testEntries = new ArrayList<>();

		ProductSpecificationUpdateVO newDesc = ProductSpecificationUpdateVOTestExample.build()
				.atSchemaLocation(null)
				.targetProductSchema(null);
		newDesc.setDescription("New description");
		ProductSpecificationVO expectedNewDesc = ProductSpecificationVOTestExample.build()
				.atSchemaLocation(null)
				.targetProductSchema(null)
				.validFor(null);
		expectedNewDesc.setDescription("New description");
		testEntries.add(Arguments.of("The description should have been updated.", newDesc, expectedNewDesc));

		ProductSpecificationUpdateVO newLifeCycle = ProductSpecificationUpdateVOTestExample.build()
				.atSchemaLocation(null)
				.targetProductSchema(null);
		newLifeCycle.setLifecycleStatus("Dead");
		ProductSpecificationVO expectedNewLifeCycle = ProductSpecificationVOTestExample.build()
				.atSchemaLocation(null)
				.targetProductSchema(null)
				.validFor(null);
		expectedNewLifeCycle.setLifecycleStatus("Dead");
		testEntries.add(
				Arguments.of("The lifecycle state should have been updated.", newLifeCycle, expectedNewLifeCycle));

		ProductSpecificationUpdateVO newName = ProductSpecificationUpdateVOTestExample.build()
				.atSchemaLocation(null)
				.targetProductSchema(null);
		newName.setName("New name");
		ProductSpecificationVO expectedNewName = ProductSpecificationVOTestExample.build()
				.atSchemaLocation(null)
				.targetProductSchema(null)
				.validFor(null);
		expectedNewName.setName("New name");
		testEntries.add(Arguments.of("The name should have been updated.", newName, expectedNewName));

		ProductSpecificationUpdateVO newVersion = ProductSpecificationUpdateVOTestExample.build()
				.atSchemaLocation(null)
				.targetProductSchema(null);
		newVersion.setVersion("1.23.1");
		ProductSpecificationVO expectedNewVersion = ProductSpecificationVOTestExample.build()
				.atSchemaLocation(null)
				.targetProductSchema(null)
				.validFor(null);
		expectedNewVersion.setVersion("1.23.1");
		testEntries.add(Arguments.of("The version should have been updated.", newVersion, expectedNewVersion));

		ProductSpecificationUpdateVO newValidFor = ProductSpecificationUpdateVOTestExample.build()
				.atSchemaLocation(null)
				.targetProductSchema(null);
		TimePeriodVO timePeriodVO = TimePeriodVOTestExample.build();
		timePeriodVO.setEndDateTime(Instant.now());
		timePeriodVO.setStartDateTime(Instant.now());
		newValidFor.setValidFor(timePeriodVO);
		ProductSpecificationVO expectedNewValidFor = ProductSpecificationVOTestExample.build()
				.atSchemaLocation(null)
				.targetProductSchema(null);
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
		ProductSpecificationCreateVO productSpecificationCreateVO = ProductSpecificationCreateVOTestExample.build()
				.atSchemaLocation(null)
				.targetProductSchema(null);
		HttpResponse<ProductSpecificationVO> createResponse = callAndCatch(
				() -> productSpecificationApiTestClient.createProductSpecification(null, productSpecificationCreateVO));
		assertEquals(HttpStatus.CREATED, createResponse.getStatus(), "The catalog should have been created first.");

		String productSpecId = createResponse.body().getId();

		HttpResponse<ProductSpecificationVO> updateResponse = callAndCatch(
				() -> productSpecificationApiTestClient.patchProductSpecification(null, productSpecId,
						productSpecificationUpdateVO));
		assertEquals(HttpStatus.BAD_REQUEST, updateResponse.getStatus(), message);

		Optional<ErrorDetails> optionalErrorDetails = updateResponse.getBody(ErrorDetails.class);
		assertTrue(optionalErrorDetails.isPresent(), "Some error details should be present.");
	}

	private static Stream<Arguments> provideInvalidUpdates() {
		List<Arguments> testEntries = new ArrayList<>();

		testEntries.add(Arguments.of("A product spec with invalid bundled ps rel should not be created.",
				ProductSpecificationUpdateVOTestExample.build()
						.atSchemaLocation(null)
						.targetProductSchema(null)
						.bundledProductSpecification(List.of(BundledProductSpecificationVOTestExample.build().atSchemaLocation(null)))));
		testEntries.add(Arguments.of("A product spec with non-existent bundled ps rel should not be created.",
				ProductSpecificationUpdateVOTestExample.build()
						.atSchemaLocation(null)
						.targetProductSchema(null)
						.bundledProductSpecification(
								List.of(BundledProductSpecificationVOTestExample.build().atSchemaLocation(null)
										.id("urn:ngsi-ld:product-specification:non-existent")))));

		testEntries.add(Arguments.of("A product spec with invalid product spec relationship should not be created.",
				ProductSpecificationUpdateVOTestExample.build()
						.atSchemaLocation(null)
						.targetProductSchema(null)
						.productSpecificationRelationship(
								List.of(ProductSpecificationRelationshipVOTestExample.build().atSchemaLocation(null)))));
		testEntries.add(
				Arguments.of("A product spec with non-existent product spec relationship should not be created.",
						ProductSpecificationUpdateVOTestExample.build()
								.atSchemaLocation(null)
								.targetProductSchema(null)
								.productSpecificationRelationship(
										List.of(ProductSpecificationRelationshipVOTestExample.build().atSchemaLocation(null)
												.id("urn:ngsi-ld:product-specification:non-existent")))));

		testEntries.add(Arguments.of("A product spec with invalid related party should not be created.",
				ProductSpecificationUpdateVOTestExample.build()
						.atSchemaLocation(null)
						.targetProductSchema(null)
						.relatedParty(List.of(RelatedPartyVOTestExample.build().atSchemaLocation(null)))));
		testEntries.add(Arguments.of("A product spec with non-existent related party should not be created.",
				ProductSpecificationUpdateVOTestExample.build()
						.atSchemaLocation(null)
						.targetProductSchema(null)
						.relatedParty(
								List.of(RelatedPartyVOTestExample.build().atSchemaLocation(null).id("urn:ngsi-ld:organization:non-existent")))));

		testEntries.add(Arguments.of("A product spec with invalid resource spec should not be created.",
				ProductSpecificationUpdateVOTestExample.build()
						.atSchemaLocation(null)
						.targetProductSchema(null)
						.resourceSpecification(List.of(ResourceSpecificationRefVOTestExample.build().atSchemaLocation(null)))));
		testEntries.add(Arguments.of("A product spec with non-existent resource spec should not be created.",
				ProductSpecificationUpdateVOTestExample.build()
						.atSchemaLocation(null)
						.targetProductSchema(null)
						.resourceSpecification(
								List.of(ResourceSpecificationRefVOTestExample.build().atSchemaLocation(null)
										.id("urn:ngsi-ld:resource:non-existent")))));

		testEntries.add(Arguments.of("A product spec with invalid service spec should not be created.",
				ProductSpecificationUpdateVOTestExample.build()
						.atSchemaLocation(null)
						.targetProductSchema(null)
						.serviceSpecification(List.of(ServiceSpecificationRefVOTestExample.build().atSchemaLocation(null)))));
		testEntries.add(Arguments.of("A product spec with non-existent service spec should not be created.",
				ProductSpecificationUpdateVOTestExample.build()
						.atSchemaLocation(null)
						.targetProductSchema(null)
						.serviceSpecification(
								List.of(ServiceSpecificationRefVOTestExample.build().atSchemaLocation(null).id("urn:ngsi-ld:service:non-existent")))));

		testEntries.add(Arguments.of("A product spec with invalid prod-spec-characteristic should not be created.",
				ProductSpecificationUpdateVOTestExample.build()
						.atSchemaLocation(null)
						.targetProductSchema(null)
						.productSpecCharacteristic(List.of(
								ProductSpecificationCharacteristicVOTestExample.build().atSchemaLocation(null)
										.productSpecCharRelationship(
												List.of(ProductSpecificationCharacteristicRelationshipVOTestExample.build().atSchemaLocation(null)))))));

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
		ProductSpecificationUpdateVO productSpecificationUpdateVO = ProductSpecificationUpdateVOTestExample.build()
				.atSchemaLocation(null)
				.targetProductSchema(null);
		assertEquals(
				HttpStatus.NOT_FOUND,
				callAndCatch(() -> productSpecificationApiTestClient.patchProductSpecification(
						null, "urn:ngsi-ld:productSpecification:not-existent", productSpecificationUpdateVO)).getStatus(),
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
		ProductSpecificationCreateVO productSpecificationCreateVO = ProductSpecificationCreateVOTestExample.build()
				.atSchemaLocation(null)
				.targetProductSchema(null);
		// we dont have a parent
		HttpResponse<ProductSpecificationVO> createResponse = callAndCatch(
				() -> productSpecificationApiTestClient.createProductSpecification(null, productSpecificationCreateVO));
		assertEquals(HttpStatus.CREATED, createResponse.getStatus(),
				"The productSpecification should have been created first.");
		String id = createResponse.body().getId();

		ProductSpecificationVO expectedProductOfferingPrice = ProductSpecificationVOTestExample.build()
				.atSchemaLocation(null)
				.targetProductSchema(null)
				.id(id)
				.href(id)
				.lastUpdate(currentTimeInstant)
				.validFor(null);

		//then retrieve
		HttpResponse<ProductSpecificationVO> retrievedPOP = callAndCatch(
				() -> productSpecificationApiTestClient.retrieveProductSpecification(null, id, null));
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
						null, "urn:ngsi-ld:product-specification:non-existent", null));
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

	@Override
	protected String getEntityType() {
		return ProductSpecification.TYPE_PRODUCT_SPECIFICATION;
	}

	@DisplayName("Duplicate relationship issue - DOME#83519")
	@Test
	public void duplicateRelationshipIssue() throws Exception {
		ProductSpecificationCreateVO productSpecCreate1 = ProductSpecificationCreateVOTestExample.build()
				.atSchemaLocation(null)
				.targetProductSchema(null);
		HttpResponse<ProductSpecificationVO> createResponse = callAndCatch(
				() -> productSpecificationApiTestClient.createProductSpecification(null, productSpecCreate1));
		assertEquals(HttpStatus.CREATED, createResponse.getStatus(),
				"The productSpecification should have been created first.");
		String specOne = createResponse.body().getId();

		ProductSpecificationRelationshipVO specRel1 = ProductSpecificationRelationshipVOTestExample.build().atSchemaLocation(null)
				.id(specOne)
				.href(URI.create(specOne))
				.name("spec-one-1");
		ProductSpecificationRelationshipVO specRel2 = ProductSpecificationRelationshipVOTestExample.build().atSchemaLocation(null)
				.id(specOne)
				.href(URI.create(specOne))
				.name("spec-one-2");

		ProductSpecificationCreateVO productSpecCreate2 = ProductSpecificationCreateVOTestExample.build()
				.atSchemaLocation(null)
				.targetProductSchema(null)
				.productSpecificationRelationship(List.of(specRel1, specRel2));
		createResponse = callAndCatch(
				() -> productSpecificationApiTestClient.createProductSpecification(null, productSpecCreate2));
		assertEquals(HttpStatus.CREATED, createResponse.getStatus(),
				"The productSpecification should have been created first.");
		String specTwo = createResponse.body().getId();

		HttpResponse<ProductSpecificationVO> retrievalResponse = callAndCatch(() -> productSpecificationApiTestClient.retrieveProductSpecification(null, specTwo, null));
		assertEquals(HttpStatus.OK, retrievalResponse.getStatus(), "The spec should have been retrieved.");

	}

}