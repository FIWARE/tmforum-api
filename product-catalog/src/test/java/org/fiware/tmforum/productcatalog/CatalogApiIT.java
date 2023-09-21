package org.fiware.tmforum.productcatalog;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import io.micronaut.test.annotation.MockBean;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import lombok.RequiredArgsConstructor;
import org.fiware.ngsi.api.EntitiesApiClient;
import org.fiware.productcatalog.api.CatalogApiTestClient;
import org.fiware.productcatalog.api.CatalogApiTestSpec;
import org.fiware.productcatalog.model.CatalogCreateVO;
import org.fiware.productcatalog.model.CatalogCreateVOTestExample;
import org.fiware.productcatalog.model.CatalogUpdateVO;
import org.fiware.productcatalog.model.CatalogUpdateVOTestExample;
import org.fiware.productcatalog.model.CatalogVO;
import org.fiware.productcatalog.model.CatalogVOTestExample;
import org.fiware.productcatalog.model.CategoryRefVO;
import org.fiware.productcatalog.model.CategoryRefVOTestExample;
import org.fiware.productcatalog.model.RelatedPartyVO;
import org.fiware.productcatalog.model.RelatedPartyVOTestExample;
import org.fiware.productcatalog.model.TimePeriodVO;
import org.fiware.productcatalog.model.TimePeriodVOTestExample;
import org.fiware.tmforum.common.EventHandler;
import org.fiware.tmforum.common.configuration.GeneralProperties;
import org.fiware.tmforum.common.exception.ErrorDetails;
import org.fiware.tmforum.common.test.AbstractApiIT;
import org.fiware.tmforum.productcatalog.domain.Catalog;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import reactor.core.publisher.Mono;

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
public class CatalogApiIT extends AbstractApiIT implements CatalogApiTestSpec {

	public final CatalogApiTestClient catalogApiTestClient;

	private String message;
	private CatalogCreateVO catalogCreateVO;
	private CatalogUpdateVO catalogUpdateVO;
	private CatalogVO expectedCatalog;

	public CatalogApiIT(CatalogApiTestClient catalogApiTestClient, EntitiesApiClient entitiesApiClient,
			ObjectMapper objectMapper, GeneralProperties generalProperties) {
		super(entitiesApiClient, objectMapper, generalProperties);
		this.catalogApiTestClient = catalogApiTestClient;
	}

	@MockBean(EventHandler.class)
	public EventHandler eventHandler() {
		EventHandler eventHandler = mock(EventHandler.class);

		Mono<List<HttpResponse<String>>> response = Mono.just(Stream.of("ok")
				.map(HttpResponse::ok).collect(Collectors.toList()));
		when(eventHandler.handleCreateEvent(any())).thenReturn(response);
		when(eventHandler.handleUpdateEvent(any(), any())).thenReturn(response);
		when(eventHandler.handleDeleteEvent(any())).thenReturn(response);

		return eventHandler;
	}

	@ParameterizedTest
	@MethodSource("provideValidCatalogs")
	public void createCatalog201(String message, CatalogCreateVO catalogCreateVO, CatalogVO expectedCatalog)
			throws Exception {
		this.message = message;
		this.catalogCreateVO = catalogCreateVO;
		this.expectedCatalog = expectedCatalog;
		createCatalog201();
	}

	@Override
	public void createCatalog201() throws Exception {

		HttpResponse<CatalogVO> catalogVOHttpResponse = callAndCatch(
				() -> catalogApiTestClient.createCatalog(catalogCreateVO));
		assertEquals(HttpStatus.CREATED, catalogVOHttpResponse.getStatus(), message);
		String catalogId = catalogVOHttpResponse.body().getId();
		expectedCatalog.setId(catalogId);
		expectedCatalog.setHref(catalogId);

		assertEquals(expectedCatalog, catalogVOHttpResponse.body(), message);

	}

	private static Stream<Arguments> provideValidCatalogs() {
		List<Arguments> testEntries = new ArrayList<>();

		CatalogCreateVO catalogCreateVO = CatalogCreateVOTestExample.build();
		CatalogVO expectedCatalog = CatalogVOTestExample.build();
		testEntries.add(Arguments.of("An empty catalog should have been created.", catalogCreateVO, expectedCatalog));

		return testEntries.stream();
	}

	@ParameterizedTest
	@MethodSource("provideInvalidCatalogs")
	public void createCatalog400(String message, CatalogCreateVO invalidCreateVO) throws Exception {
		this.message = message;
		this.catalogCreateVO = invalidCreateVO;
		createCatalog400();
	}

	@Override
	public void createCatalog400() throws Exception {
		HttpResponse<CatalogVO> creationResponse = callAndCatch(
				() -> catalogApiTestClient.createCatalog(catalogCreateVO));
		assertEquals(HttpStatus.BAD_REQUEST, creationResponse.getStatus(), message);
		Optional<ErrorDetails> optionalErrorDetails = creationResponse.getBody(ErrorDetails.class);
		assertTrue(optionalErrorDetails.isPresent(), "Error details should be provided.");
	}

	private static Stream<Arguments> provideInvalidCatalogs() {
		List<Arguments> testEntries = new ArrayList<>();

		CatalogCreateVO invalidRelatedPartyCreate = CatalogCreateVOTestExample.build();
		// no valid id
		RelatedPartyVO invalidRelatedParty = RelatedPartyVOTestExample.build();
		invalidRelatedPartyCreate.setRelatedParty(List.of(invalidRelatedParty));
		testEntries.add(Arguments.of("A catalog with invalid related parties should not be created.",
				invalidRelatedPartyCreate));

		CatalogCreateVO nonExistentRelatedPartyCreate = CatalogCreateVOTestExample.build();
		// no existent id
		RelatedPartyVO nonExistentRelatedParty = RelatedPartyVOTestExample.build();
		nonExistentRelatedParty.setId("urn:ngsi-ld:individual:non-existent");
		nonExistentRelatedPartyCreate.setRelatedParty(List.of(nonExistentRelatedParty));
		testEntries.add(Arguments.of("A catalog with non-existent related parties should not be created.",
				nonExistentRelatedPartyCreate));

		CatalogCreateVO invalidCategoryCreate = CatalogCreateVOTestExample.build();
		// no valid id
		CategoryRefVO categoryRef = CategoryRefVOTestExample.build();
		invalidCategoryCreate.setCategory(List.of(categoryRef));
		testEntries.add(
				Arguments.of("A catalog with invalid categories should not be created.", invalidCategoryCreate));

		CatalogCreateVO nonExistentCategoryCreate = CatalogCreateVOTestExample.build();
		// no existent id
		CategoryRefVO nonExistentCategoryRef = CategoryRefVOTestExample.build();
		nonExistentCategoryRef.setId("urn:ngsi-ld:category:non-existent");
		nonExistentCategoryCreate.setCategory(List.of(nonExistentCategoryRef));
		testEntries.add(Arguments.of("A catalog with non-existent categories should not be created.",
				nonExistentCategoryCreate));

		return testEntries.stream();
	}

	@Disabled("Security is handled externally, thus 401 and 403 cannot happen.")
	@Test
	@Override
	public void createCatalog401() throws Exception {
	}

	@Disabled("Security is handled externally, thus 401 and 403 cannot happen.")
	@Test
	@Override
	public void createCatalog403() throws Exception {
	}

	@Disabled("Prohibited by the framework.")
	@Test
	@Override
	public void createCatalog405() throws Exception {

	}

	@Disabled("Catalog doesn't have 'implicit' entities and id is generated by the implementation, no conflict possible.")
	@Test
	@Override
	public void createCatalog409() throws Exception {

	}

	@Override
	public void createCatalog500() throws Exception {

	}

	@Test
	@Override
	public void deleteCatalog204() throws Exception {
		//first create
		CatalogCreateVO catalogCreateVO = CatalogCreateVOTestExample.build();
		HttpResponse<CatalogVO> createResponse = callAndCatch(
				() -> catalogApiTestClient.createCatalog(catalogCreateVO));
		assertEquals(HttpStatus.CREATED, createResponse.getStatus(), "The catalog should have been created first.");

		String catalogId = createResponse.body().getId();

		assertEquals(HttpStatus.NO_CONTENT,
				callAndCatch(() -> catalogApiTestClient.deleteCatalog(catalogId)).getStatus(),
				"The catalog should have been deleted.");

		assertEquals(HttpStatus.NOT_FOUND,
				callAndCatch(() -> catalogApiTestClient.retrieveCatalog(catalogId, null)).status(),
				"The catalog should not exist anymore.");

	}

	@Disabled("400 is impossible to happen on deletion with the current implementation.")
	@Test
	@Override
	public void deleteCatalog400() throws Exception {

	}

	@Disabled("Security is handled externally, thus 401 and 403 cannot happen.")
	@Test
	@Override
	public void deleteCatalog401() throws Exception {

	}

	@Disabled("Security is handled externally, thus 401 and 403 cannot happen.")
	@Test
	@Override
	public void deleteCatalog403() throws Exception {

	}

	@Test
	@Override
	public void deleteCatalog404() throws Exception {
		HttpResponse<?> notFoundResponse = callAndCatch(
				() -> catalogApiTestClient.deleteCatalog("urn:ngsi-ld:catalog:no-catalog"));
		assertEquals(HttpStatus.NOT_FOUND,
				notFoundResponse.getStatus(),
				"No such catalog should exist.");

		Optional<ErrorDetails> optionalErrorDetails = notFoundResponse.getBody(ErrorDetails.class);
		assertTrue(optionalErrorDetails.isPresent(), "Error details should be provided.");

		notFoundResponse = callAndCatch(() -> catalogApiTestClient.deleteCatalog("invalid-id"));
		assertEquals(HttpStatus.NOT_FOUND,
				notFoundResponse.getStatus(),
				"No such catalog should exist.");

		optionalErrorDetails = notFoundResponse.getBody(ErrorDetails.class);
		assertTrue(optionalErrorDetails.isPresent(), "Error details should be provided.");
	}

	@Disabled("Prohibited by the framework.")
	@Test
	@Override
	public void deleteCatalog405() throws Exception {

	}

	@Disabled("Impossible status.")
	@Test
	@Override
	public void deleteCatalog409() throws Exception {

	}

	@Override
	public void deleteCatalog500() throws Exception {

	}

	@Test
	@Override
	public void listCatalog200() throws Exception {
		List<CatalogVO> expectedCatalogs = new ArrayList<>();
		for (int i = 0; i < 10; i++) {
			CatalogCreateVO catalogCreateVO = CatalogCreateVOTestExample.build();
			String id = catalogApiTestClient.createCatalog(catalogCreateVO).body().getId();
			CatalogVO catalogVO = CatalogVOTestExample.build();
			catalogVO
					.id(id)
					.href(id)
					.category(null)
					.relatedParty(null);
			expectedCatalogs.add(catalogVO);
		}

		HttpResponse<List<CatalogVO>> catalogResponse = callAndCatch(
				() -> catalogApiTestClient.listCatalog(null, null, null));

		assertEquals(HttpStatus.OK, catalogResponse.getStatus(), "The list should be accessible.");
		assertEquals(expectedCatalogs.size(), catalogResponse.getBody().get().size(),
				"All catalogs should have been returned.");
		List<CatalogVO> retrievedCatalogs = catalogResponse.getBody().get();

		Map<String, CatalogVO> retrievedMap = retrievedCatalogs.stream()
				.collect(Collectors.toMap(catalog -> catalog.getId(), catalog -> catalog));

		expectedCatalogs.stream()
				.forEach(expectedCatalog -> assertTrue(retrievedMap.containsKey(expectedCatalog.getId()),
						String.format("All created catalogs should be returned - Missing: %s.", expectedCatalog,
								retrievedCatalogs)));
		expectedCatalogs.stream().forEach(
				expectedCatalog -> assertEquals(expectedCatalog, retrievedMap.get(expectedCatalog.getId()),
						"The correct catalogs should be retrieved."));

		// get with pagination
		Integer limit = 5;
		HttpResponse<List<CatalogVO>> firstPartResponse = callAndCatch(
				() -> catalogApiTestClient.listCatalog(null, 0, limit));
		assertEquals(limit, firstPartResponse.body().size(),
				"Only the requested number of entries should be returend.");
		HttpResponse<List<CatalogVO>> secondPartResponse = callAndCatch(
				() -> catalogApiTestClient.listCatalog(null, 0 + limit, limit));
		assertEquals(limit, secondPartResponse.body().size(),
				"Only the requested number of entries should be returend.");

		retrievedCatalogs.clear();
		retrievedCatalogs.addAll(firstPartResponse.body());
		retrievedCatalogs.addAll(secondPartResponse.body());
		expectedCatalogs.stream()
				.forEach(expectedCatalog -> assertTrue(retrievedMap.containsKey(expectedCatalog.getId()),
						String.format("All created catalogs should be returned - Missing: %s.", expectedCatalog)));
		expectedCatalogs.stream().forEach(
				expectedCatalog -> assertEquals(expectedCatalog, retrievedMap.get(expectedCatalog.getId()),
						"The correct catalogs should be retrieved."));
	}

	@Test
	@Override
	public void listCatalog400() throws Exception {
		HttpResponse<List<CatalogVO>> badRequestResponse = callAndCatch(
				() -> catalogApiTestClient.listCatalog(null, -1, null));
		assertEquals(HttpStatus.BAD_REQUEST,
				badRequestResponse.getStatus(),
				"Negative offsets are impossible.");

		Optional<ErrorDetails> optionalErrorDetails = badRequestResponse.getBody(ErrorDetails.class);
		assertTrue(optionalErrorDetails.isPresent(), "Error details should be provided.");

		badRequestResponse = callAndCatch(() -> catalogApiTestClient.listCatalog(null, null, -1));
		assertEquals(HttpStatus.BAD_REQUEST,
				badRequestResponse.getStatus(),
				"Negative limits are impossible.");
		optionalErrorDetails = badRequestResponse.getBody(ErrorDetails.class);
		assertTrue(optionalErrorDetails.isPresent(), "Error details should be provided.");

	}

	@Disabled("Security is handled externally, thus 401 and 403 cannot happen.")
	@Test
	@Override
	public void listCatalog401() throws Exception {

	}

	@Disabled("Security is handled externally, thus 401 and 403 cannot happen.")
	@Test
	@Override
	public void listCatalog403() throws Exception {

	}

	@Disabled("Not found is not possible here, will be answerd with an empty list instead.")
	@Test
	@Override
	public void listCatalog404() throws Exception {

	}

	@Disabled("Prohibited by the framework.")
	@Test
	@Override
	public void listCatalog405() throws Exception {

	}

	@Disabled("Impossible status.")
	@Test
	@Override
	public void listCatalog409() throws Exception {

	}

	@Override
	public void listCatalog500() throws Exception {

	}

	@ParameterizedTest
	@MethodSource("provideCatalogUpdates")
	public void patchCatalog200(String message, CatalogUpdateVO catalogUpdateVO, CatalogVO expectedCatalog)
			throws Exception {
		this.message = message;
		this.catalogUpdateVO = catalogUpdateVO;
		this.expectedCatalog = expectedCatalog;
		patchCatalog200();
	}

	@Override
	public void patchCatalog200() throws Exception {
		//first create
		CatalogCreateVO catalogCreateVO = CatalogCreateVOTestExample.build();
		HttpResponse<CatalogVO> createResponse = callAndCatch(
				() -> catalogApiTestClient.createCatalog(catalogCreateVO));
		assertEquals(HttpStatus.CREATED, createResponse.getStatus(), "The catalog should have been created first.");

		String catalogId = createResponse.body().getId();

		HttpResponse<CatalogVO> updateResponse = callAndCatch(
				() -> catalogApiTestClient.patchCatalog(catalogId, catalogUpdateVO));
		assertEquals(HttpStatus.OK, updateResponse.getStatus(), message);

		CatalogVO updatedCatalog = updateResponse.body();
		expectedCatalog.setHref(catalogId);
		expectedCatalog.setId(catalogId);
		expectedCatalog.setRelatedParty(null);
		expectedCatalog.setCategory(null);

		assertEquals(expectedCatalog, updatedCatalog, message);
	}

	private static Stream<Arguments> provideCatalogUpdates() {
		List<Arguments> testEntries = new ArrayList<>();

		CatalogUpdateVO newTypeCatalog = CatalogUpdateVOTestExample.build();
		newTypeCatalog.setCatalogType("New-Type");
		CatalogVO expectedNewType = CatalogVOTestExample.build();
		expectedNewType.setCatalogType("New-Type");
		testEntries.add(Arguments.of("The type should have been updated.", newTypeCatalog, expectedNewType));

		CatalogUpdateVO newDesc = CatalogUpdateVOTestExample.build();
		newDesc.setDescription("New description");
		CatalogVO expectedNewDesc = CatalogVOTestExample.build();
		expectedNewDesc.setDescription("New description");
		testEntries.add(Arguments.of("The description should have been updated.", newDesc, expectedNewDesc));

		CatalogUpdateVO newLifeCycle = CatalogUpdateVOTestExample.build();
		newLifeCycle.setLifecycleStatus("Dead");
		CatalogVO expectedNewLifeCycle = CatalogVOTestExample.build();
		expectedNewLifeCycle.setLifecycleStatus("Dead");
		testEntries.add(
				Arguments.of("The lifecycle state should have been updated.", newLifeCycle, expectedNewLifeCycle));

		CatalogUpdateVO newName = CatalogUpdateVOTestExample.build();
		newName.setName("New name");
		CatalogVO expectedNewName = CatalogVOTestExample.build();
		expectedNewName.setName("New name");
		testEntries.add(Arguments.of("The name should have been updated.", newName, expectedNewName));

		CatalogUpdateVO newVersion = CatalogUpdateVOTestExample.build();
		newVersion.setVersion("1.23.1");
		CatalogVO expectedNewVersion = CatalogVOTestExample.build();
		expectedNewVersion.setVersion("1.23.1");
		testEntries.add(Arguments.of("The version should have been updated.", newVersion, expectedNewVersion));

		CatalogUpdateVO newValidFor = CatalogUpdateVOTestExample.build();
		TimePeriodVO timePeriodVO = TimePeriodVOTestExample.build();
		timePeriodVO.setEndDateTime(Instant.now());
		timePeriodVO.setStartDateTime(Instant.now());
		newValidFor.setValidFor(timePeriodVO);
		CatalogVO expectedNewValidFor = CatalogVOTestExample.build();
		expectedNewValidFor.setValidFor(timePeriodVO);
		testEntries.add(Arguments.of("The validFor should have been updated.", newValidFor, expectedNewValidFor));

		return testEntries.stream();
	}

	@ParameterizedTest
	@MethodSource("provideInvalidUpdates")
	public void patchCatalog400(String message, CatalogUpdateVO invalidUpdateVO) throws Exception {
		this.message = message;
		this.catalogUpdateVO = invalidUpdateVO;
		patchCatalog400();
	}

	@Override
	public void patchCatalog400() throws Exception {
		//first create
		CatalogCreateVO catalogCreateVO = CatalogCreateVOTestExample.build();
		HttpResponse<CatalogVO> createResponse = callAndCatch(
				() -> catalogApiTestClient.createCatalog(catalogCreateVO));
		assertEquals(HttpStatus.CREATED, createResponse.getStatus(), "The catalog should have been created first.");

		String catalogId = createResponse.body().getId();

		HttpResponse<CatalogVO> updateResponse = callAndCatch(
				() -> catalogApiTestClient.patchCatalog(catalogId, catalogUpdateVO));
		assertEquals(HttpStatus.BAD_REQUEST, updateResponse.getStatus(), message);

		Optional<ErrorDetails> optionalErrorDetails = updateResponse.getBody(ErrorDetails.class);
		assertTrue(optionalErrorDetails.isPresent(), "Some error details should be present.");
	}

	private static Stream<Arguments> provideInvalidUpdates() {
		List<Arguments> testEntries = new ArrayList<>();

		CatalogUpdateVO invalidRelatedPartyUpdate = CatalogUpdateVOTestExample.build();
		// no valid id
		RelatedPartyVO invalidRelatedParty = RelatedPartyVOTestExample.build();
		invalidRelatedPartyUpdate.setRelatedParty(List.of(invalidRelatedParty));
		testEntries.add(Arguments.of("A catalog with invalid related parties should not be updated.",
				invalidRelatedPartyUpdate));

		CatalogUpdateVO nonExistentRelatedPartyUpdate = CatalogUpdateVOTestExample.build();
		// no existent id
		RelatedPartyVO nonExistentRelatedParty = RelatedPartyVOTestExample.build();
		nonExistentRelatedParty.setId("urn:ngsi-ld:individual:non-existent");
		nonExistentRelatedPartyUpdate.setRelatedParty(List.of(nonExistentRelatedParty));
		testEntries.add(Arguments.of("A catalog with non-existent related parties should not be updated.",
				nonExistentRelatedPartyUpdate));

		CatalogUpdateVO invalidCategoryUpdate = CatalogUpdateVOTestExample.build();
		// no valid id
		CategoryRefVO categoryRef = CategoryRefVOTestExample.build();
		invalidCategoryUpdate.setCategory(List.of(categoryRef));
		testEntries.add(
				Arguments.of("A catalog with invalid categories should not be updated.", invalidCategoryUpdate));

		CatalogUpdateVO nonExistentCategoryUpdate = CatalogUpdateVOTestExample.build();
		// no existent id
		CategoryRefVO nonExistentCategoryRef = CategoryRefVOTestExample.build();
		nonExistentCategoryRef.setId("urn:ngsi-ld:category:non-existent");
		nonExistentCategoryUpdate.setCategory(List.of(nonExistentCategoryRef));
		testEntries.add(Arguments.of("A catalog with non-existent categories should not be updated.",
				nonExistentCategoryUpdate));

		return testEntries.stream();
	}

	@Disabled("Security is handled externally, thus 401 and 403 cannot happen.")
	@Test
	@Override
	public void patchCatalog401() throws Exception {

	}

	@Disabled("Security is handled externally, thus 401 and 403 cannot happen.")
	@Test
	@Override
	public void patchCatalog403() throws Exception {

	}

	@Test
	@Override
	public void patchCatalog404() throws Exception {
		CatalogUpdateVO catalogUpdateVO = CatalogUpdateVOTestExample.build();
		assertEquals(
				HttpStatus.NOT_FOUND,
				callAndCatch(() -> catalogApiTestClient.patchCatalog("urn:ngsi-ld:catalog:not-existent",
						catalogUpdateVO)).getStatus(),
				"Non existent catalogs should not be updated.");
	}

	@Disabled("Prohibited by the framework.")
	@Test
	@Override
	public void patchCatalog405() throws Exception {

	}

	@Disabled("No implicit creations, cannot happen.")
	@Test
	@Override
	public void patchCatalog409() throws Exception {

	}

	@Override
	public void patchCatalog500() throws Exception {

	}

	@Test
	@Override
	public void retrieveCatalog200() throws Exception {

		//first create
		CatalogCreateVO catalogCreateVO = CatalogCreateVOTestExample.build();
		HttpResponse<CatalogVO> createResponse = callAndCatch(
				() -> catalogApiTestClient.createCatalog(catalogCreateVO));
		assertEquals(HttpStatus.CREATED, createResponse.getStatus(), "The catalog should have been created first.");
		String id = createResponse.body().getId();

		CatalogVO expectedCatalog = CatalogVOTestExample.build();
		expectedCatalog.setId(id);
		expectedCatalog.setHref(id);
		// empty list is mapped to null
		expectedCatalog.setCategory(null);
		expectedCatalog.setRelatedParty(null);

		//then retrieve
		HttpResponse<CatalogVO> retrievedCatalog = callAndCatch(() -> catalogApiTestClient.retrieveCatalog(id, null));
		assertEquals(HttpStatus.OK, retrievedCatalog.getStatus(), "The retrieval should be ok.");
		assertEquals(expectedCatalog, retrievedCatalog.body(), "The correct catalog should be returned.");
	}

	@Disabled("400 cannot happen, only 404")
	@Test
	@Override
	public void retrieveCatalog400() throws Exception {

	}

	@Disabled("Security is handled externally, thus 401 and 403 cannot happen.")
	@Test
	@Override
	public void retrieveCatalog401() throws Exception {

	}

	@Disabled("Security is handled externally, thus 401 and 403 cannot happen.")
	@Test
	@Override
	public void retrieveCatalog403() throws Exception {
	}

	@Test
	@Override
	public void retrieveCatalog404() throws Exception {
		HttpResponse<CatalogVO> response = callAndCatch(
				() -> catalogApiTestClient.retrieveCatalog("urn:ngsi-ld:catalog:non-existent", null));
		assertEquals(HttpStatus.NOT_FOUND, response.getStatus(), "No such catalog should exist.");

		Optional<ErrorDetails> optionalErrorDetails = response.getBody(ErrorDetails.class);
		assertTrue(optionalErrorDetails.isPresent(), "Error details should have been provided.");
	}

	@Disabled("Prohibited by the framework.")
	@Test
	@Override
	public void retrieveCatalog405() throws Exception {

	}

	@Disabled("Conflict not possible on retrieval")
	@Test
	@Override
	public void retrieveCatalog409() throws Exception {

	}

	@Override
	public void retrieveCatalog500() throws Exception {

	}

	@Override
	protected String getEntityType() {
		return Catalog.TYPE_CATALOG;
	}
}
