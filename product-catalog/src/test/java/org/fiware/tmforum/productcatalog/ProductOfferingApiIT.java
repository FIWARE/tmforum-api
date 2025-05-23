package org.fiware.tmforum.productcatalog;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micronaut.context.annotation.Requires;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import io.micronaut.test.annotation.MockBean;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import org.fiware.ngsi.api.EntitiesApiClient;
import org.fiware.productcatalog.api.ProductOfferingApiTestClient;
import org.fiware.productcatalog.api.ProductOfferingApiTestSpec;
import org.fiware.productcatalog.api.ProductSpecificationApiTestClient;
import org.fiware.productcatalog.model.*;
import org.fiware.tmforum.common.configuration.GeneralProperties;
import org.fiware.tmforum.common.exception.ErrorDetails;
import org.fiware.tmforum.common.notification.TMForumEventHandler;
import org.fiware.tmforum.common.test.AbstractApiIT;
import org.fiware.tmforum.product.ProductOffering;
import org.junit.jupiter.api.Disabled;
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
public class ProductOfferingApiIT extends AbstractApiIT implements ProductOfferingApiTestSpec {

	public final ProductOfferingApiTestClient productOfferingApiTestClient;
	private final ProductSpecificationApiTestClient productSpecificationApiTestClient;

	private String message;
	private ProductOfferingCreateVO productOfferingCreateVO;
	private ProductOfferingUpdateVO productOfferingUpdateVO;
	private ProductOfferingVO expectedProductOffering;

	private Clock clock = mock(Clock.class);

	public ProductOfferingApiIT(ProductOfferingApiTestClient productOfferingApiTestClient,
								EntitiesApiClient entitiesApiClient, ObjectMapper objectMapper, GeneralProperties generalProperties, ProductSpecificationApiTestClient productSpecificationApiTestClient) {
		super(entitiesApiClient, objectMapper, generalProperties);
		this.productOfferingApiTestClient = productOfferingApiTestClient;
		this.productSpecificationApiTestClient = productSpecificationApiTestClient;
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
	@MethodSource("provideValidProductOfferings")
	public void createProductOffering201(String message, ProductOfferingCreateVO productOfferingCreateVO,
										 ProductOfferingVO expectedProductOffering) throws Exception {
		this.message = message;
		this.productOfferingCreateVO = productOfferingCreateVO;
		this.expectedProductOffering = expectedProductOffering;
		createProductOffering201();
	}

	@Override
	public void createProductOffering201() throws Exception {

		Instant currentTimeInstant = Instant.ofEpochSecond(10000);

		when(clock.instant()).thenReturn(currentTimeInstant);

		productOfferingCreateVO.setProductSpecification(null);
		productOfferingCreateVO.setResourceCandidate(null);
		productOfferingCreateVO.setServiceCandidate(null);
		productOfferingCreateVO.setServiceLevelAgreement(null);

		HttpResponse<ProductOfferingVO> productOfferingVOHttpResponse = callAndCatch(
				() -> productOfferingApiTestClient.createProductOffering(null, productOfferingCreateVO));
		assertEquals(HttpStatus.CREATED, productOfferingVOHttpResponse.getStatus(), message);
		String productOfferingId = productOfferingVOHttpResponse.body().getId();

		expectedProductOffering.setId(productOfferingId);
		expectedProductOffering.setHref(productOfferingId);
		expectedProductOffering.setLastUpdate(currentTimeInstant);
		expectedProductOffering.setProductSpecification(null);
		expectedProductOffering.setServiceCandidate(null);
		expectedProductOffering.setResourceCandidate(null);
		expectedProductOffering.setServiceLevelAgreement(null);

		assertEquals(expectedProductOffering, productOfferingVOHttpResponse.body(), message);
	}

	private static Stream<Arguments> provideValidProductOfferings() {
		List<Arguments> testEntries = new ArrayList<>();

		ProductOfferingCreateVO productOfferingCreateVO = ProductOfferingCreateVOTestExample.build().atSchemaLocation(null);

		ProductOfferingVO expectedProductOffering = ProductOfferingVOTestExample.build().atSchemaLocation(null);
		testEntries.add(Arguments.of("An empty productOffering should have been created.", productOfferingCreateVO,
				expectedProductOffering));

		return testEntries.stream();
	}

	@ParameterizedTest
	@MethodSource("provideInvalidProductOfferings")
	public void createProductOffering400(String message, ProductOfferingCreateVO invalidCreateVO) throws Exception {
		this.message = message;
		this.productOfferingCreateVO = invalidCreateVO;
		createProductOffering400();
	}

	@Override
	public void createProductOffering400() throws Exception {
		HttpResponse<ProductOfferingVO> creationResponse = callAndCatch(
				() -> productOfferingApiTestClient.createProductOffering(null, productOfferingCreateVO));
		assertEquals(HttpStatus.BAD_REQUEST, creationResponse.getStatus(), message);
		Optional<ErrorDetails> optionalErrorDetails = creationResponse.getBody(ErrorDetails.class);
		assertTrue(optionalErrorDetails.isPresent(), "Error details should be provided.");
	}

	private static Stream<Arguments> provideInvalidProductOfferings() {
		List<Arguments> testEntries = new ArrayList<>();

		testEntries.add(Arguments.of("A productOffering with invalid agreements should not be created.",
				ProductOfferingCreateVOTestExample.build().atSchemaLocation(null).agreement(List.of(AgreementRefVOTestExample.build().atSchemaLocation(null)))));
		testEntries.add(Arguments.of("A productOffering with non-existent agreements should not be created.",
				ProductOfferingCreateVOTestExample.build().atSchemaLocation(null).agreement(
						List.of(AgreementRefVOTestExample.build().atSchemaLocation(null).id("urn:ngsi-ld:agreement:non-existent")))));

		testEntries.add(Arguments.of("A productOffering with invalid bundled offerings should not be created.",
				ProductOfferingCreateVOTestExample.build().atSchemaLocation(null)
						.bundledProductOffering(List.of(BundledProductOfferingVOTestExample.build().atSchemaLocation(null)))));
		testEntries.add(Arguments.of("A productOffering with non-existent agreements should not be created.",
				ProductOfferingCreateVOTestExample.build().atSchemaLocation(null).bundledProductOffering(
						List.of(BundledProductOfferingVOTestExample.build().atSchemaLocation(null)
								.id("urn:ngsi-ld:bundle-product-offering:non-existent")))));

		testEntries.add(Arguments.of("A productOffering with invalid category should not be created.",
				ProductOfferingCreateVOTestExample.build().atSchemaLocation(null).category(List.of(CategoryRefVOTestExample.build().atSchemaLocation(null)))));
		testEntries.add(Arguments.of("A productOffering with non-existent category should not be created.",
				ProductOfferingCreateVOTestExample.build().atSchemaLocation(null)
						.category(List.of(CategoryRefVOTestExample.build().atSchemaLocation(null).id("urn:ngsi-ld:category:non-existent")))));

		testEntries.add(Arguments.of("A productOffering with invalid channel should not be created.",
				ProductOfferingCreateVOTestExample.build().atSchemaLocation(null).channel(List.of(ChannelRefVOTestExample.build().atSchemaLocation(null)))));
		testEntries.add(Arguments.of("A productOffering with non-existent channel should not be created.",
				ProductOfferingCreateVOTestExample.build().atSchemaLocation(null)
						.channel(List.of(ChannelRefVOTestExample.build().atSchemaLocation(null).id("urn:ngsi-ld:channel:non-existent")))));

		testEntries.add(Arguments.of("A productOffering with invalid marketSegment should not be created.",
				ProductOfferingCreateVOTestExample.build().atSchemaLocation(null)
						.marketSegment(List.of(MarketSegmentRefVOTestExample.build().atSchemaLocation(null)))));
		testEntries.add(Arguments.of("A productOffering with non-existent marketSegment should not be created.",
				ProductOfferingCreateVOTestExample.build().atSchemaLocation(null).marketSegment(
						List.of(MarketSegmentRefVOTestExample.build().atSchemaLocation(null).id("urn:ngsi-ld:market-segment:non-existent")))));

		testEntries.add(Arguments.of("A productOffering with invalid place should not be created.",
				ProductOfferingCreateVOTestExample.build().atSchemaLocation(null).place(List.of(PlaceRefVOTestExample.build().atSchemaLocation(null)))));
		testEntries.add(Arguments.of("A productOffering with non-existent place should not be created.",
				ProductOfferingCreateVOTestExample.build().atSchemaLocation(null)
						.place(List.of(PlaceRefVOTestExample.build().atSchemaLocation(null).id("urn:ngsi-ld:place:non-existent")))));

		testEntries.add(
				Arguments.of("A productOffering with invalid prod-spec-characteristic-value-use should not be created.",
						ProductOfferingCreateVOTestExample.build().atSchemaLocation(null).prodSpecCharValueUse(
								List.of(ProductSpecificationCharacteristicValueUseVOTestExample.build().atSchemaLocation(null)))));
		testEntries.add(Arguments.of(
				"A productOffering with non-existent prod-spec-characteristic-value-use should not be created.",
				ProductOfferingCreateVOTestExample.build().atSchemaLocation(null).prodSpecCharValueUse(
						List.of(ProductSpecificationCharacteristicValueUseVOTestExample.build().atSchemaLocation(null)
								.id("urn:ngsi-ld:prod-spec-characteristic-value-use:non-existent")))));

		testEntries.add(Arguments.of("A productOffering with invalid productOfferingPrice should not be created.",
				ProductOfferingCreateVOTestExample.build().atSchemaLocation(null)
						.productOfferingPrice(List.of(ProductOfferingPriceRefOrValueVOTestExample.build().atSchemaLocation(null)))));
		testEntries.add(Arguments.of("A productOffering with non-existent productOfferingPrice should not be created.",
				ProductOfferingCreateVOTestExample.build().atSchemaLocation(null).productOfferingPrice(
						List.of(ProductOfferingPriceRefOrValueVOTestExample.build().atSchemaLocation(null)
								.id("urn:ngsi-ld:product-offering-price:non-existent")))));

		testEntries.add(
				Arguments.of("A productOffering with invalid productOfferingRelationship should not be created.",
						ProductOfferingCreateVOTestExample.build().atSchemaLocation(null).productOfferingRelationship(
								List.of(ProductOfferingRelationshipVOTestExample.build().atSchemaLocation(null)))));
		testEntries.add(
				Arguments.of("A productOffering with non-existent productOfferingRelationship should not be created.",
						ProductOfferingCreateVOTestExample.build().atSchemaLocation(null).productOfferingRelationship(
								List.of(ProductOfferingRelationshipVOTestExample.build().atSchemaLocation(null)
										.id("urn:ngsi-ld:product-offering:non-existent")))));

		testEntries.add(Arguments.of("A productOffering with invalid productSpecification should not be created.",
				ProductOfferingCreateVOTestExample.build().atSchemaLocation(null)
						.productSpecification((ProductSpecificationRefVOTestExample.build().atSchemaLocation(null)))));
		testEntries.add(Arguments.of("A productOffering with non-existent productSpecification should not be created.",
				ProductOfferingCreateVOTestExample.build().atSchemaLocation(null).productSpecification(
						(ProductSpecificationRefVOTestExample.build().atSchemaLocation(null)
								.id("urn:ngsi-ld:product-specification:non-existent")))));

		testEntries.add(Arguments.of("A productOffering with invalid resourceCandidate should not be created.",
				ProductOfferingCreateVOTestExample.build().atSchemaLocation(null)
						.resourceCandidate((ResourceCandidateRefVOTestExample.build().atSchemaLocation(null)))));
		testEntries.add(Arguments.of("A productOffering with non-existent resourceCandidate should not be created.",
				ProductOfferingCreateVOTestExample.build().atSchemaLocation(null).resourceCandidate((ResourceCandidateRefVOTestExample.build().atSchemaLocation(null)
						.id("urn:ngsi-ld:resource-candidate:non-existent")))));

		testEntries.add(Arguments.of("A productOffering with invalid serviceCandidate should not be created.",
				ProductOfferingCreateVOTestExample.build().atSchemaLocation(null)
						.serviceCandidate((ServiceCandidateRefVOTestExample.build().atSchemaLocation(null)))));
		testEntries.add(Arguments.of("A productOffering with non-existent serviceCandidate should not be created.",
				ProductOfferingCreateVOTestExample.build().atSchemaLocation(null).serviceCandidate(
						(ServiceCandidateRefVOTestExample.build().atSchemaLocation(null).id("urn:ngsi-ld:service-candidate:non-existent")))));

		testEntries.add(Arguments.of("A productOffering with invalid SLAs should not be created.",
				ProductOfferingCreateVOTestExample.build().atSchemaLocation(null).serviceLevelAgreement((SLARefVOTestExample.build().atSchemaLocation(null)))));
		testEntries.add(Arguments.of("A productOffering with non-existent SLAs should not be created.",
				ProductOfferingCreateVOTestExample.build().atSchemaLocation(null)
						.serviceLevelAgreement((SLARefVOTestExample.build().atSchemaLocation(null).id("urn:ngsi-ld:sla:non-existent")))));

		return testEntries.stream();
	}

	@Disabled("Security is handled externally, thus 401 and 403 cannot happen.")
	@Test
	@Override
	public void createProductOffering401() throws Exception {
	}

	@Disabled("Security is handled externally, thus 401 and 403 cannot happen.")
	@Test
	@Override
	public void createProductOffering403() throws Exception {
	}

	@Disabled("Prohibited by the framework.")
	@Test
	@Override
	public void createProductOffering405() throws Exception {

	}

	@Disabled("Catalog doesn't have 'implicit' entities and id is generated by the implementation, no conflict possible.")
	@Test
	@Override
	public void createProductOffering409() throws Exception {

	}

	@Override
	public void createProductOffering500() throws Exception {

	}

	@Test
	@Override
	public void deleteProductOffering204() throws Exception {
		//first create
		ProductOfferingCreateVO productOfferingCreateVO = ProductOfferingCreateVOTestExample.build().atSchemaLocation(null);
		productOfferingCreateVO.setProductSpecification(null);
		productOfferingCreateVO.setResourceCandidate(null);
		productOfferingCreateVO.setServiceCandidate(null);
		productOfferingCreateVO.setServiceLevelAgreement(null);

		HttpResponse<ProductOfferingVO> createResponse = callAndCatch(
				() -> productOfferingApiTestClient.createProductOffering(null, productOfferingCreateVO));
		assertEquals(HttpStatus.CREATED, createResponse.getStatus(), "The catalog should have been created first.");

		String catalogId = createResponse.body().getId();

		assertEquals(HttpStatus.NO_CONTENT,
				callAndCatch(() -> productOfferingApiTestClient.deleteProductOffering(null, catalogId)).getStatus(),
				"The productOffering should have been deleted.");

		assertEquals(HttpStatus.NOT_FOUND,
				callAndCatch(() -> productOfferingApiTestClient.retrieveProductOffering(null, catalogId, null)).status(),
				"The productOffering should not exist anymore.");

	}

	@Disabled("400 is impossible to happen on deletion with the current implementation.")
	@Test
	@Override
	public void deleteProductOffering400() throws Exception {

	}

	@Disabled("Security is handled externally, thus 401 and 403 cannot happen.")
	@Test
	@Override
	public void deleteProductOffering401() throws Exception {

	}

	@Disabled("Security is handled externally, thus 401 and 403 cannot happen.")
	@Test
	@Override
	public void deleteProductOffering403() throws Exception {

	}

	@Test
	@Override
	public void deleteProductOffering404() throws Exception {
		HttpResponse<?> notFoundResponse = callAndCatch(
				() -> productOfferingApiTestClient.deleteProductOffering(null, "urn:ngsi-ld:product-offering:no-catalog"));
		assertEquals(HttpStatus.NOT_FOUND,
				notFoundResponse.getStatus(),
				"No such catalog should exist.");

		Optional<ErrorDetails> optionalErrorDetails = notFoundResponse.getBody(ErrorDetails.class);
		assertTrue(optionalErrorDetails.isPresent(), "Error details should be provided.");

		notFoundResponse = callAndCatch(() -> productOfferingApiTestClient.deleteProductOffering(null, "invalid-id"));
		assertEquals(HttpStatus.NOT_FOUND,
				notFoundResponse.getStatus(),
				"No such catalog should exist.");

		optionalErrorDetails = notFoundResponse.getBody(ErrorDetails.class);
		assertTrue(optionalErrorDetails.isPresent(), "Error details should be provided.");
	}

	@Disabled("Prohibited by the framework.")
	@Test
	@Override
	public void deleteProductOffering405() throws Exception {

	}

	@Disabled("Impossible status.")
	@Test
	@Override
	public void deleteProductOffering409() throws Exception {

	}

	@Override
	public void deleteProductOffering500() throws Exception {

	}

	@Test
	@Override
	public void listProductOffering200() throws Exception {
		//Persist Product Specification to be used
		ProductSpecificationVO productSpecification = productSpecificationApiTestClient.createProductSpecification(null,
				ProductSpecificationCreateVOTestExample.build().atSchemaLocation(null)
						.atSchemaLocation(null)
						.targetProductSchema(null)
						.productSpecCharacteristic(List.of(ProductSpecificationCharacteristicVOTestExample.build().atSchemaLocation(null).id("urn:spec")
								.atSchemaLocation(null)))).body();
		String productSpecId = productSpecification.getId();
		ProductSpecificationRefVO productSpecReference = ProductSpecificationRefVOTestExample.build().atSchemaLocation(null)
				.targetProductSchema(productSpecification.getTargetProductSchema())
				.id(productSpecId)
				.href(URI.create(productSpecId))
				.atSchemaLocation(null)
				.atReferredType(null);

		List<ProductOfferingVO> expectedProductOfferings = new ArrayList<>();
		for (int i = 0; i < 10; i++) {
			ProductOfferingCreateVO productOfferingCreateVO = ProductOfferingCreateVOTestExample.build().atSchemaLocation(null)
					.productSpecification(new ProductSpecificationRefVO().id(productSpecId))
					.atSchemaLocation(null)
					.resourceCandidate(null)
					.serviceCandidate(null)
					.serviceLevelAgreement(null);
			String id = productOfferingApiTestClient.createProductOffering(null, productOfferingCreateVO).body().getId();
			ProductOfferingVO productOfferingVO = ProductOfferingVOTestExample.build().atSchemaLocation(null);
			productOfferingVO
					.id(id)
					.href(id)
					.atSchemaLocation(null)
					.productSpecification(productSpecReference)
					.resourceCandidate(null)
					.serviceCandidate(null)
					.serviceLevelAgreement(null)
					.validFor(null);
			expectedProductOfferings.add(productOfferingVO);
		}

		HttpResponse<List<ProductOfferingVO>> productOfferingResponse = callAndCatch(
				() -> productOfferingApiTestClient.listProductOffering(null, null, null, null));

		assertEquals(HttpStatus.OK, productOfferingResponse.getStatus(), "The list should be accessible.");
		assertEquals(expectedProductOfferings.size(), productOfferingResponse.getBody().get().size(),
				"All productOfferings should have been returned.");
		List<ProductOfferingVO> retrievedProductOfferings = productOfferingResponse.getBody().get();

		Map<String, ProductOfferingVO> retrievedMap = retrievedProductOfferings.stream()
				.collect(Collectors.toMap(productOffering -> productOffering.getId(),
						productOffering -> productOffering));

		expectedProductOfferings.stream()
				.forEach(
						expectedProductOffering -> assertTrue(retrievedMap.containsKey(expectedProductOffering.getId()),
								String.format("All created productOfferings should be returned - Missing: %s.",
										expectedProductOffering,
										retrievedProductOfferings)));
		expectedProductOfferings.stream().forEach(
				expectedProductOffering -> assertEquals(expectedProductOffering,
						retrievedMap.get(expectedProductOffering.getId()),
						"The correct productOfferings should be retrieved."));

		// get with pagination
		Integer limit = 5;
		HttpResponse<List<ProductOfferingVO>> firstPartResponse = callAndCatch(
				() -> productOfferingApiTestClient.listProductOffering(null, null, 0, limit));
		assertEquals(limit, firstPartResponse.body().size(),
				"Only the requested number of entries should be returend.");
		HttpResponse<List<ProductOfferingVO>> secondPartResponse = callAndCatch(
				() -> productOfferingApiTestClient.listProductOffering(null, null, 0 + limit, limit));
		assertEquals(limit, secondPartResponse.body().size(),
				"Only the requested number of entries should be returend.");

		retrievedProductOfferings.clear();
		retrievedProductOfferings.addAll(firstPartResponse.body());
		retrievedProductOfferings.addAll(secondPartResponse.body());
		expectedProductOfferings.stream()
				.forEach(
						expectedProductOffering -> assertTrue(retrievedMap.containsKey(expectedProductOffering.getId()),
								String.format("All created productOfferings should be returned - Missing: %s.",
										expectedProductOffering)));
		expectedProductOfferings.stream().forEach(
				expectedProductOffering -> assertEquals(expectedProductOffering,
						retrievedMap.get(expectedProductOffering.getId()),
						"The correct productOfferings should be retrieved."));
	}

	@Test
	@Override
	public void listProductOffering400() throws Exception {
		HttpResponse<List<ProductOfferingVO>> badRequestResponse = callAndCatch(
				() -> productOfferingApiTestClient.listProductOffering(null, null, -1, null));
		assertEquals(HttpStatus.BAD_REQUEST,
				badRequestResponse.getStatus(),
				"Negative offsets are impossible.");

		Optional<ErrorDetails> optionalErrorDetails = badRequestResponse.getBody(ErrorDetails.class);
		assertTrue(optionalErrorDetails.isPresent(), "Error details should be provided.");

		badRequestResponse = callAndCatch(() -> productOfferingApiTestClient.listProductOffering(null, null, null, -1));
		assertEquals(HttpStatus.BAD_REQUEST,
				badRequestResponse.getStatus(),
				"Negative limits are impossible.");
		optionalErrorDetails = badRequestResponse.getBody(ErrorDetails.class);
		assertTrue(optionalErrorDetails.isPresent(), "Error details should be provided.");

	}

	@Disabled("Security is handled externally, thus 401 and 403 cannot happen.")
	@Test
	@Override
	public void listProductOffering401() throws Exception {

	}

	@Disabled("Security is handled externally, thus 401 and 403 cannot happen.")
	@Test
	@Override
	public void listProductOffering403() throws Exception {

	}

	@Disabled("Not found is not possible here, will be answerd with an empty list instead.")
	@Test
	@Override
	public void listProductOffering404() throws Exception {

	}

	@Disabled("Prohibited by the framework.")
	@Test
	@Override
	public void listProductOffering405() throws Exception {

	}

	@Disabled("Impossible status.")
	@Test
	@Override
	public void listProductOffering409() throws Exception {

	}

	@Override
	public void listProductOffering500() throws Exception {

	}

	@ParameterizedTest
	@MethodSource("provideProductOfferingUpdates")
	public void patchProductOffering200(String message, ProductOfferingUpdateVO productOfferingUpdateVO,
										ProductOfferingVO expectedProductOffering) throws Exception {
		this.message = message;
		this.productOfferingUpdateVO = productOfferingUpdateVO;
		this.expectedProductOffering = expectedProductOffering;
		patchProductOffering200();
	}

	@Override
	public void patchProductOffering200() throws Exception {
		//first create
		ProductOfferingCreateVO productOfferingCreateVO = ProductOfferingCreateVOTestExample.build().atSchemaLocation(null);
		productOfferingCreateVO.setProductSpecification(null);
		productOfferingCreateVO.setResourceCandidate(null);
		productOfferingCreateVO.setServiceCandidate(null);
		productOfferingCreateVO.setServiceLevelAgreement(null);

		HttpResponse<ProductOfferingVO> createResponse = callAndCatch(
				() -> productOfferingApiTestClient.createProductOffering(null, productOfferingCreateVO));
		assertEquals(HttpStatus.CREATED, createResponse.getStatus(),
				"The product offering should have been created first.");

		String catalogId = createResponse.body().getId();

		productOfferingUpdateVO.setProductSpecification(null);
		productOfferingUpdateVO.setResourceCandidate(null);
		productOfferingUpdateVO.setServiceCandidate(null);
		productOfferingUpdateVO.setServiceLevelAgreement(null);
		HttpResponse<ProductOfferingVO> updateResponse = callAndCatch(
				() -> productOfferingApiTestClient.patchProductOffering(null, catalogId, productOfferingUpdateVO));
		assertEquals(HttpStatus.OK, updateResponse.getStatus(), message);

		ProductOfferingVO updatedCatalog = updateResponse.body();
		expectedProductOffering.setHref(catalogId);
		expectedProductOffering.setId(catalogId);
		expectedProductOffering.setProductSpecification(null);
		expectedProductOffering.setResourceCandidate(null);
		expectedProductOffering.setServiceCandidate(null);
		expectedProductOffering.setServiceLevelAgreement(null);

		assertEquals(expectedProductOffering, updatedCatalog, message);
	}

	private static Stream<Arguments> provideProductOfferingUpdates() {
		List<Arguments> testEntries = new ArrayList<>();

		ProductOfferingUpdateVO newDesc = ProductOfferingUpdateVOTestExample.build().atSchemaLocation(null).atSchemaLocation(null);
		newDesc.setDescription("New description");
		ProductOfferingVO expectedNewDesc = ProductOfferingVOTestExample.build().atSchemaLocation(null).atSchemaLocation(null).validFor(null);
		expectedNewDesc.setDescription("New description");
		testEntries.add(Arguments.of("The description should have been updated.", newDesc, expectedNewDesc));

		ProductOfferingUpdateVO newLifeCycle = ProductOfferingUpdateVOTestExample.build().atSchemaLocation(null).atSchemaLocation(null);
		newLifeCycle.setLifecycleStatus("Dead");
		ProductOfferingVO expectedNewLifeCycle = ProductOfferingVOTestExample.build().atSchemaLocation(null).atSchemaLocation(null).validFor(null);
		expectedNewLifeCycle.setLifecycleStatus("Dead");
		testEntries.add(
				Arguments.of("The lifecycle state should have been updated.", newLifeCycle, expectedNewLifeCycle));

		ProductOfferingUpdateVO newName = ProductOfferingUpdateVOTestExample.build().atSchemaLocation(null).atSchemaLocation(null);
		newName.setName("New name");
		ProductOfferingVO expectedNewName = ProductOfferingVOTestExample.build().atSchemaLocation(null).atSchemaLocation(null).validFor(null);
		expectedNewName.setName("New name");
		testEntries.add(Arguments.of("The name should have been updated.", newName, expectedNewName));

		ProductOfferingUpdateVO newVersion = ProductOfferingUpdateVOTestExample.build().atSchemaLocation(null).atSchemaLocation(null);
		newVersion.setVersion("1.23.1");
		ProductOfferingVO expectedNewVersion = ProductOfferingVOTestExample.build().atSchemaLocation(null).atSchemaLocation(null).validFor(null);
		expectedNewVersion.setVersion("1.23.1");
		testEntries.add(Arguments.of("The version should have been updated.", newVersion, expectedNewVersion));

		ProductOfferingUpdateVO newValidFor = ProductOfferingUpdateVOTestExample.build().atSchemaLocation(null).atSchemaLocation(null);
		TimePeriodVO timePeriodVO = TimePeriodVOTestExample.build();
		timePeriodVO.setEndDateTime(Instant.now());
		timePeriodVO.setStartDateTime(Instant.now());
		newValidFor.setValidFor(timePeriodVO);
		ProductOfferingVO expectedNewValidFor = ProductOfferingVOTestExample.build().atSchemaLocation(null).atSchemaLocation(null);
		expectedNewValidFor.setValidFor(timePeriodVO);
		testEntries.add(Arguments.of("The validFor should have been updated.", newValidFor, expectedNewValidFor));

		return testEntries.stream();
	}

	@ParameterizedTest
	@MethodSource("provideInvalidUpdates")
	public void patchProductOffering400(String message, ProductOfferingUpdateVO invalidUpdateVO) throws Exception {
		this.message = message;
		this.productOfferingUpdateVO = invalidUpdateVO;
		patchProductOffering400();
	}

	@Override
	public void patchProductOffering400() throws Exception {
		//first create
		ProductOfferingCreateVO productOfferingCreateVO = ProductOfferingCreateVOTestExample.build().atSchemaLocation(null);
		productOfferingCreateVO.setProductSpecification(null);
		productOfferingCreateVO.setResourceCandidate(null);
		productOfferingCreateVO.setServiceCandidate(null);
		productOfferingCreateVO.setServiceLevelAgreement(null);
		HttpResponse<ProductOfferingVO> createResponse = callAndCatch(
				() -> productOfferingApiTestClient.createProductOffering(null, productOfferingCreateVO));
		assertEquals(HttpStatus.CREATED, createResponse.getStatus(),
				"The product offering should have been created first.");

		String catalogId = createResponse.body().getId();

		HttpResponse<ProductOfferingVO> updateResponse = callAndCatch(
				() -> productOfferingApiTestClient.patchProductOffering(null, catalogId, productOfferingUpdateVO));
		assertEquals(HttpStatus.BAD_REQUEST, updateResponse.getStatus(), message);

		Optional<ErrorDetails> optionalErrorDetails = updateResponse.getBody(ErrorDetails.class);
		assertTrue(optionalErrorDetails.isPresent(), "Some error details should be present.");
	}

	private static Stream<Arguments> provideInvalidUpdates() {
		List<Arguments> testEntries = new ArrayList<>();

		testEntries.add(Arguments.of("A productOffering with invalid agreements should not be created.",
				ProductOfferingUpdateVOTestExample.build().atSchemaLocation(null).atSchemaLocation(null).agreement(List.of(AgreementRefVOTestExample.build().atSchemaLocation(null).atSchemaLocation(null)))));
		testEntries.add(Arguments.of("A productOffering with non-existent agreements should not be created.",
				ProductOfferingUpdateVOTestExample.build().atSchemaLocation(null).atSchemaLocation(null).agreement(
						List.of(AgreementRefVOTestExample.build().atSchemaLocation(null).atSchemaLocation(null).id("urn:ngsi-ld:agreement:non-existent")))));

		testEntries.add(Arguments.of("A productOffering with invalid bundled offerings should not be created.",
				ProductOfferingUpdateVOTestExample.build().atSchemaLocation(null)
						.atSchemaLocation(null)
						.bundledProductOffering(List.of(BundledProductOfferingVOTestExample.build().atSchemaLocation(null).atSchemaLocation(null)))));
		testEntries.add(Arguments.of("A productOffering with non-existent agreements should not be created.",
				ProductOfferingUpdateVOTestExample.build().atSchemaLocation(null).atSchemaLocation(null).bundledProductOffering(
						List.of(BundledProductOfferingVOTestExample.build().atSchemaLocation(null)
								.atSchemaLocation(null)
								.id("urn:ngsi-ld:bundle-product-offering:non-existent")))));

		testEntries.add(Arguments.of("A productOffering with invalid category should not be created.",
				ProductOfferingUpdateVOTestExample.build().atSchemaLocation(null).atSchemaLocation(null).category(List.of(CategoryRefVOTestExample.build().atSchemaLocation(null).atSchemaLocation(null)))));
		testEntries.add(Arguments.of("A productOffering with non-existent category should not be created.",
				ProductOfferingUpdateVOTestExample.build().atSchemaLocation(null).atSchemaLocation(null)
						.category(List.of(CategoryRefVOTestExample.build().atSchemaLocation(null).atSchemaLocation(null).id("urn:ngsi-ld:category:non-existent")))));

		testEntries.add(Arguments.of("A productOffering with invalid channel should not be created.",
				ProductOfferingUpdateVOTestExample.build().atSchemaLocation(null).atSchemaLocation(null).channel(List.of(ChannelRefVOTestExample.build().atSchemaLocation(null).atSchemaLocation(null)))));
		testEntries.add(Arguments.of("A productOffering with non-existent channel should not be created.",
				ProductOfferingUpdateVOTestExample.build().atSchemaLocation(null)
						.channel(List.of(ChannelRefVOTestExample.build().atSchemaLocation(null).id("urn:ngsi-ld:channel:non-existent")))));

		testEntries.add(Arguments.of("A productOffering with invalid marketSegment should not be created.",
				ProductOfferingUpdateVOTestExample.build().atSchemaLocation(null)
						.marketSegment(List.of(MarketSegmentRefVOTestExample.build().atSchemaLocation(null)))));
		testEntries.add(Arguments.of("A productOffering with non-existent marketSegment should not be created.",
				ProductOfferingUpdateVOTestExample.build().atSchemaLocation(null).marketSegment(
						List.of(MarketSegmentRefVOTestExample.build().atSchemaLocation(null).id("urn:ngsi-ld:market-segment:non-existent")))));

		testEntries.add(Arguments.of("A productOffering with invalid place should not be created.",
				ProductOfferingUpdateVOTestExample.build().atSchemaLocation(null).place(List.of(PlaceRefVOTestExample.build().atSchemaLocation(null)))));
		testEntries.add(Arguments.of("A productOffering with non-existent place should not be created.",
				ProductOfferingUpdateVOTestExample.build().atSchemaLocation(null)
						.place(List.of(PlaceRefVOTestExample.build().atSchemaLocation(null).id("urn:ngsi-ld:place:non-existent")))));

		testEntries.add(
				Arguments.of("A productOffering with invalid prod-spec-characteristic-value-use should not be created.",
						ProductOfferingUpdateVOTestExample.build().atSchemaLocation(null).prodSpecCharValueUse(
								List.of(ProductSpecificationCharacteristicValueUseVOTestExample.build().atSchemaLocation(null)))));
		testEntries.add(Arguments.of(
				"A productOffering with non-existent prod-spec-characteristic-value-use should not be created.",
				ProductOfferingUpdateVOTestExample.build().atSchemaLocation(null).prodSpecCharValueUse(
						List.of(ProductSpecificationCharacteristicValueUseVOTestExample.build().atSchemaLocation(null)
								.id("urn:ngsi-ld:prod-spec-characteristic-value-use:non-existent")))));

		testEntries.add(Arguments.of("A productOffering with invalid productOfferingPrice should not be created.",
				ProductOfferingUpdateVOTestExample.build().atSchemaLocation(null)
						.productOfferingPrice(List.of(ProductOfferingPriceRefOrValueVOTestExample.build().atSchemaLocation(null)))));
		testEntries.add(Arguments.of("A productOffering with non-existent productOfferingPrice should not be created.",
				ProductOfferingUpdateVOTestExample.build().atSchemaLocation(null).productOfferingPrice(
						List.of(ProductOfferingPriceRefOrValueVOTestExample.build().atSchemaLocation(null)
								.id("urn:ngsi-ld:product-offering-price:non-existent")))));

		testEntries.add(
				Arguments.of("A productOffering with invalid productOfferingRelationship should not be created.",
						ProductOfferingUpdateVOTestExample.build().atSchemaLocation(null).productOfferingRelationship(
								List.of(ProductOfferingRelationshipVOTestExample.build().atSchemaLocation(null)))));
		testEntries.add(
				Arguments.of("A productOffering with non-existent productOfferingRelationship should not be created.",
						ProductOfferingUpdateVOTestExample.build().atSchemaLocation(null).productOfferingRelationship(
								List.of(ProductOfferingRelationshipVOTestExample.build().atSchemaLocation(null)
										.id("urn:ngsi-ld:product-offering:non-existent")))));

		testEntries.add(Arguments.of("A productOffering with invalid productSpecification should not be created.",
				ProductOfferingUpdateVOTestExample.build().atSchemaLocation(null)
						.productSpecification((ProductSpecificationRefVOTestExample.build().atSchemaLocation(null)))));
		testEntries.add(Arguments.of("A productOffering with non-existent productSpecification should not be created.",
				ProductOfferingUpdateVOTestExample.build().atSchemaLocation(null).productSpecification(
						(ProductSpecificationRefVOTestExample.build().atSchemaLocation(null)
								.id("urn:ngsi-ld:product-specification:non-existent")))));

		testEntries.add(Arguments.of("A productOffering with invalid resourceCandidate should not be created.",
				ProductOfferingUpdateVOTestExample.build().atSchemaLocation(null)
						.resourceCandidate((ResourceCandidateRefVOTestExample.build().atSchemaLocation(null)))));
		testEntries.add(Arguments.of("A productOffering with non-existent resourceCandidate should not be created.",
				ProductOfferingUpdateVOTestExample.build().atSchemaLocation(null).resourceCandidate((ResourceCandidateRefVOTestExample.build().atSchemaLocation(null)
						.id("urn:ngsi-ld:resource-candidate:non-existent")))));

		testEntries.add(Arguments.of("A productOffering with invalid serviceCandidate should not be created.",
				ProductOfferingUpdateVOTestExample.build().atSchemaLocation(null)
						.serviceCandidate((ServiceCandidateRefVOTestExample.build().atSchemaLocation(null)))));
		testEntries.add(Arguments.of("A productOffering with non-existent serviceCandidate should not be created.",
				ProductOfferingUpdateVOTestExample.build().atSchemaLocation(null).serviceCandidate(
						(ServiceCandidateRefVOTestExample.build().atSchemaLocation(null).id("urn:ngsi-ld:service-candidate:non-existent")))));

		testEntries.add(Arguments.of("A productOffering with invalid SLAs should not be created.",
				ProductOfferingUpdateVOTestExample.build().atSchemaLocation(null).serviceLevelAgreement((SLARefVOTestExample.build().atSchemaLocation(null)))));
		testEntries.add(Arguments.of("A productOffering with non-existent SLAs should not be created.",
				ProductOfferingUpdateVOTestExample.build().atSchemaLocation(null)
						.serviceLevelAgreement((SLARefVOTestExample.build().atSchemaLocation(null).id("urn:ngsi-ld:sla:non-existent")))));

		return testEntries.stream();
	}

	@Disabled("Security is handled externally, thus 401 and 403 cannot happen.")
	@Test
	@Override
	public void patchProductOffering401() throws Exception {

	}

	@Disabled("Security is handled externally, thus 401 and 403 cannot happen.")
	@Test
	@Override
	public void patchProductOffering403() throws Exception {

	}

	@Test
	@Override
	public void patchProductOffering404() throws Exception {
		ProductOfferingUpdateVO productOfferingUpdateVO = ProductOfferingUpdateVOTestExample.build()
				.atSchemaLocation(null)
				.productSpecification(null)
				.resourceCandidate(null)
				.serviceLevelAgreement(null)
				.serviceCandidate(null);
		assertEquals(
				HttpStatus.NOT_FOUND,
				callAndCatch(() -> productOfferingApiTestClient.patchProductOffering(
						null, "urn:ngsi-ld:product-offering:not-existent", productOfferingUpdateVO)).getStatus(),
				"Non existent categories should not be updated.");
	}

	@Disabled("Prohibited by the framework.")
	@Test
	@Override
	public void patchProductOffering405() throws Exception {

	}

	@Disabled("No implicit creations, cannot happen.")
	@Test
	@Override
	public void patchProductOffering409() throws Exception {

	}

	@Override
	public void patchProductOffering500() throws Exception {

	}

	@Test
	@Override
	public void retrieveProductOffering200() throws Exception {
		Instant currentTimeInstant = Instant.ofEpochSecond(10000);

		when(clock.instant()).thenReturn(currentTimeInstant);

		//first create
		ProductOfferingCreateVO productOfferingCreateVO = ProductOfferingCreateVOTestExample.build().atSchemaLocation(null);
		// default generates invalid ids
		productOfferingCreateVO.setProductSpecification(null);
		productOfferingCreateVO.setResourceCandidate(null);
		productOfferingCreateVO.setServiceCandidate(null);
		productOfferingCreateVO.setServiceLevelAgreement(null);
		productOfferingCreateVO.setAtSchemaLocation(null);
		// we dont have a parent
		HttpResponse<ProductOfferingVO> createResponse = callAndCatch(
				() -> productOfferingApiTestClient.createProductOffering(null, productOfferingCreateVO));
		assertEquals(HttpStatus.CREATED, createResponse.getStatus(),
				"The product offering should have been created first.");
		String id = createResponse.body().getId();

		ProductOfferingVO expectedProductOffering = ProductOfferingVOTestExample.build().atSchemaLocation(null).validFor(null);
		expectedProductOffering.setId(id);
		expectedProductOffering.setHref(id);
		expectedProductOffering.setLastUpdate(currentTimeInstant);
		expectedProductOffering.productSpecification(null)
				.resourceCandidate(null)
				.serviceCandidate(null)
				.serviceLevelAgreement(null);

		//then retrieve
		HttpResponse<ProductOfferingVO> retrievedCatalog = callAndCatch(
				() -> productOfferingApiTestClient.retrieveProductOffering(null, id, null));
		assertEquals(HttpStatus.OK, retrievedCatalog.getStatus(), "The retrieval should be ok.");
		assertEquals(expectedProductOffering, retrievedCatalog.body(),
				"The correct productOffering should be returned.");
	}

	@MethodSource("extendedOfferingProvider")
	@ParameterizedTest
	public void retrieveProductOfferingWithExtension200(ProductOfferingCreateVO productOfferingCreateVO, ProductOfferingVO expectedProductOffering, String message) throws Exception {

		Instant currentTimeInstant = Instant.ofEpochSecond(10000);

		when(clock.instant()).thenReturn(currentTimeInstant);

		// we dont have a parent
		HttpResponse<ProductOfferingVO> createResponse = callAndCatch(
				() -> productOfferingApiTestClient.createProductOffering(null, productOfferingCreateVO));
		assertEquals(HttpStatus.CREATED, createResponse.getStatus(),
				"The product offering should have been created first.");
		String id = createResponse.body().getId();
		expectedProductOffering.lastUpdate(currentTimeInstant)
				.id(id)
				.href(id);
		//then retrieve
		HttpResponse<ProductOfferingVO> retrievedCatalog = callAndCatch(
				() -> productOfferingApiTestClient.retrieveProductOffering(null, id, null));

		Map expectedAsMap = objectMapper.convertValue(expectedProductOffering, new TypeReference<Map<String, Object>>() {
		});
		Map responseAsMap = retrievedCatalog.getBody(Map.class).get();
		assertEquals(expectedAsMap, responseAsMap, "The correct catalog should be returned.");
	}

	@MethodSource("provideInvalidExtensions")
	@ParameterizedTest
	public void createOfferingsWithInvalidExtensions(ProductOfferingCreateVO productOfferingCreateVO, String message) throws Exception {

		HttpResponse<ProductOfferingVO> createResponse = callAndCatch(
				() -> productOfferingApiTestClient.createProductOffering(null, productOfferingCreateVO));
		assertEquals(HttpStatus.BAD_REQUEST, createResponse.getStatus(),
				message);
	}

	private static Stream<Arguments> provideInvalidExtensions() {
		return Stream.of(
				Arguments.of(ProductOfferingCreateVOTestExample.build().atSchemaLocation(null)
						.atSchemaLocation(null)
						.productSpecification(null)
						.resourceCandidate(null)
						.serviceCandidate(null)
						.serviceLevelAgreement(null)
						.unknownProperties(Map.of("unknownProperty", "a")), "With no schema defined, no additional properties are allowed."),
				Arguments.of(
						ProductOfferingCreateVOTestExample.build().atSchemaLocation(null)
								.atSchemaLocation(URI.create("http://localhost:3000/schemas/no-additional-extension-schema.json"))
								.productSpecification(null)
								.resourceCandidate(null)
								.serviceCandidate(null)
								.serviceLevelAgreement(null)
								.unknownProperties(Map.of("undefinedProperty", "a")), "Undefined properties should not be allowed."),
				Arguments.of(
						ProductOfferingCreateVOTestExample.build().atSchemaLocation(null)
								.atSchemaLocation(URI.create("http://localhost:3000/schemas/properties-extension-schema.json"))
								.productSpecification(null)
								.resourceCandidate(null)
								.serviceCandidate(null)
								.serviceLevelAgreement(null)
								.unknownProperties(Map.of("extensionNumber", "a")), "Properties in the wrong format should not be allowed."),
				Arguments.of(
						ProductOfferingCreateVOTestExample.build().atSchemaLocation(null)
								.atSchemaLocation(URI.create("http://localhost:3000/schemas/properties-extension-schema.json"))
								.productSpecification(null)
								.resourceCandidate(null)
								.serviceCandidate(null)
								.serviceLevelAgreement(null)
								.unknownProperties(Map.of("extensionObject", Map.of("a", 2))), "When a sub-object contains the wrong type, validation should fail."),
				Arguments.of(
						ProductOfferingCreateVOTestExample.build().atSchemaLocation(null)
								.atSchemaLocation(URI.create("http://localhost:3000/schemas/required-properties-schema.json"))
								.productSpecification(null)
								.resourceCandidate(null)
								.serviceCandidate(null)
								.serviceLevelAgreement(null)
								.unknownProperties(Map.of("extensionNumber", 1)), "When not all required properties are provided, validation should fail.")

		);
	}

	private static Stream<Arguments> extendedOfferingProvider() {
		List<Arguments> testEntries = new ArrayList<>();
		ProductOfferingTermVO productOfferingTermVO = ProductOfferingTermVOTestExample.build().atSchemaLocation(null)
				.atSchemaLocation(URI.create("http://localhost:3000/schemas/required-properties-schema.json"));
		productOfferingTermVO.getUnknownProperties().put("extensionNumber", 3);
		productOfferingTermVO.getUnknownProperties().put("extensionString", "extension");

		ProductOfferingCreateVO productOfferingCreateVO = (ProductOfferingCreateVO) ProductOfferingCreateVOTestExample.build().atSchemaLocation(null)
				.description("string")
				.name("string")
				.atSchemaLocation(URI.create("http://localhost:3000/schemas/properties-extension-schema.json"))
				.lifecycleStatus("string")
				.productSpecification(null)
				.resourceCandidate(null)
				.serviceCandidate(null)
				.serviceLevelAgreement(null)
				.productOfferingTerm(List.of(productOfferingTermVO))
				.unknownProperties(Map.of("extensionArray", List.of("a", "b")));
		ProductOfferingVO expectedProductOfferingVO = ProductOfferingVOTestExample.build().atSchemaLocation(null)
				.validFor(null)
				.description("string")
				.name("string")
				.atSchemaLocation(URI.create("http://localhost:3000/schemas/properties-extension-schema.json"))
				.lifecycleStatus("string")
				.productSpecification(null)
				.resourceCandidate(null)
				.serviceCandidate(null)
				.serviceLevelAgreement(null)
				.productOfferingTerm(List.of(productOfferingTermVO));
		expectedProductOfferingVO.getUnknownProperties().put("extensionArray", List.of("a", "b"));
		testEntries.add(Arguments.of(productOfferingCreateVO, expectedProductOfferingVO, "The extended offering should have been created."));
		ProductOfferingCreateVO productOfferingCreateVO2 = (ProductOfferingCreateVO) ProductOfferingCreateVOTestExample.build().atSchemaLocation(null)
				.description("string")
				.name("string")
				.atSchemaLocation(URI.create("http://localhost:3000/schemas/properties-extension-schema.json"))
				.lifecycleStatus("string")
				.productSpecification(null)
				.resourceCandidate(null)
				.serviceCandidate(null)
				.serviceLevelAgreement(null)
				.unknownProperties(Map.of("extensionObject", Map.of("a", "a")));
		ProductOfferingVO expectedProductOfferingVO2 = (ProductOfferingVO) ProductOfferingVOTestExample.build().atSchemaLocation(null)
				.description("string")
				.name("string")
				.validFor(null)
				.atSchemaLocation(URI.create("http://localhost:3000/schemas/properties-extension-schema.json"))
				.lifecycleStatus("string")
				.productSpecification(null)
				.resourceCandidate(null)
				.serviceCandidate(null)
				.serviceLevelAgreement(null)
				.unknownProperties(Map.of("extensionObject", Map.of("a", "a")));
		testEntries.add(Arguments.of(productOfferingCreateVO2, expectedProductOfferingVO2, "The extended offering should have been created."));
		return testEntries.stream();
	}

	@Disabled("400 cannot happen, only 404")
	@Test
	@Override
	public void retrieveProductOffering400() throws Exception {

	}

	@Disabled("Security is handled externally, thus 401 and 403 cannot happen.")
	@Test
	@Override
	public void retrieveProductOffering401() throws Exception {

	}

	@Disabled("Security is handled externally, thus 401 and 403 cannot happen.")
	@Test
	@Override
	public void retrieveProductOffering403() throws Exception {
	}

	@Test
	@Override
	public void retrieveProductOffering404() throws Exception {
		HttpResponse<ProductOfferingVO> response = callAndCatch(
				() -> productOfferingApiTestClient.retrieveProductOffering(null, "urn:ngsi-ld:product-offering:non-existent",
						null));
		assertEquals(HttpStatus.NOT_FOUND, response.getStatus(), "No such productOffering should exist.");

		Optional<ErrorDetails> optionalErrorDetails = response.getBody(ErrorDetails.class);
		assertTrue(optionalErrorDetails.isPresent(), "Error details should have been provided.");
	}

	@Disabled("Prohibited by the framework.")
	@Test
	@Override
	public void retrieveProductOffering405() throws Exception {

	}

	@Disabled("Conflict not possible on retrieval")
	@Test
	@Override
	public void retrieveProductOffering409() throws Exception {

	}

	@Override
	public void retrieveProductOffering500() throws Exception {

	}

	@Override
	protected String getEntityType() {
		return ProductOffering.TYPE_PRODUCT_OFFERING;
	}
}