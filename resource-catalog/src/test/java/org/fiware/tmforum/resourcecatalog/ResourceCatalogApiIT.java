package org.fiware.tmforum.resourcecatalog;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import io.micronaut.test.annotation.MockBean;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import org.fiware.ngsi.api.EntitiesApiClient;
import org.fiware.resourcecatalog.api.ResourceCatalogApiTestClient;
import org.fiware.resourcecatalog.api.ResourceCatalogApiTestSpec;
import org.fiware.resourcecatalog.model.*;
import org.fiware.tmforum.common.configuration.GeneralProperties;
import org.fiware.tmforum.common.exception.ErrorDetails;
import org.fiware.tmforum.common.notification.TMForumEventHandler;
import org.fiware.tmforum.common.test.AbstractApiIT;
import org.fiware.tmforum.resourcecatalog.domain.ResourceCatalog;
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

@MicronautTest(packages = { "org.fiware.tmforum.resourcecatalog" })
public class ResourceCatalogApiIT extends AbstractApiIT implements ResourceCatalogApiTestSpec {

	public final ResourceCatalogApiTestClient resourceCatalogApiTestClient;

	private String message;
	private String fieldsParameter;
	private ResourceCatalogCreateVO resourceCatalogCreateVO;
	private ResourceCatalogUpdateVO resourceCatalogUpdateVO;
	private ResourceCatalogVO expectedResourceCatalog;

	private Clock clock = mock(Clock.class);

	public ResourceCatalogApiIT(ResourceCatalogApiTestClient resourceCatalogApiTestClient,
			EntitiesApiClient entitiesApiClient, ObjectMapper objectMapper, GeneralProperties generalProperties) {
		super(entitiesApiClient, objectMapper, generalProperties);
		this.resourceCatalogApiTestClient = resourceCatalogApiTestClient;
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
	@MethodSource("provideValidResourceCatalogs")
	public void createResourceCatalog201(String message, ResourceCatalogCreateVO resourceCatalogCreateVO,
			ResourceCatalogVO expectedResourceCatalog) throws Exception {
		this.message = message;
		this.resourceCatalogCreateVO = resourceCatalogCreateVO;
		this.expectedResourceCatalog = expectedResourceCatalog;
		createResourceCatalog201();
	}

	@Override
	public void createResourceCatalog201() throws Exception {

		Instant currentTimeInstant = Instant.ofEpochSecond(10000);
		when(clock.instant()).thenReturn(currentTimeInstant);

		HttpResponse<ResourceCatalogVO> resourceCatalogVOHttpResponse = callAndCatch(
				() -> resourceCatalogApiTestClient.createResourceCatalog(resourceCatalogCreateVO));
		assertEquals(HttpStatus.CREATED, resourceCatalogVOHttpResponse.getStatus(), message);
		String rfId = resourceCatalogVOHttpResponse.body().getId();
		expectedResourceCatalog.setId(rfId);
		expectedResourceCatalog.setHref(URI.create(rfId));
		expectedResourceCatalog.setLastUpdate(currentTimeInstant);

		assertEquals(expectedResourceCatalog, resourceCatalogVOHttpResponse.body(), message);
	}

	private static Stream<Arguments> provideValidResourceCatalogs() {
		List<Arguments> testEntries = new ArrayList<>();

		ResourceCatalogCreateVO emptyCreate = ResourceCatalogCreateVOTestExample.build().lifecycleStatus("created");
		ResourceCatalogVO expectedEmpty = ResourceCatalogVOTestExample.build().lifecycleStatus("created");
		testEntries.add(
				Arguments.of("An empty resource catalog should have been created.", emptyCreate, expectedEmpty));

		TimePeriodVO timePeriodVO = TimePeriodVOTestExample.build().endDateTime(Instant.now())
				.startDateTime(Instant.now());
		ResourceCatalogCreateVO createValidFor = ResourceCatalogCreateVOTestExample.build().validFor(timePeriodVO)
				.lifecycleStatus("created");
		ResourceCatalogVO expectedValidFor = ResourceCatalogVOTestExample.build().validFor(timePeriodVO)
				.lifecycleStatus("created");
		testEntries.add(Arguments.of("An resource catalog with a validFor should have been created.", createValidFor,
				expectedValidFor));

		return testEntries.stream();
	}

	@ParameterizedTest
	@MethodSource("provideInvalidResourceCatalogs")
	public void createResourceCatalog400(String message, ResourceCatalogCreateVO invalidCreateVO) throws Exception {
		this.message = message;
		this.resourceCatalogCreateVO = invalidCreateVO;
		createResourceCatalog400();
	}

	@Override
	public void createResourceCatalog400() throws Exception {
		HttpResponse<ResourceCatalogVO> creationResponse = callAndCatch(
				() -> resourceCatalogApiTestClient.createResourceCatalog(resourceCatalogCreateVO));
		assertEquals(HttpStatus.BAD_REQUEST, creationResponse.getStatus(), message);
		Optional<ErrorDetails> optionalErrorDetails = creationResponse.getBody(ErrorDetails.class);
		assertTrue(optionalErrorDetails.isPresent(), "Error details should be provided.");
	}

	private static Stream<Arguments> provideInvalidResourceCatalogs() {
		List<Arguments> testEntries = new ArrayList<>();

		testEntries.add(Arguments.of("A resource catalog with invalid related parties should not be created.",
				ResourceCatalogCreateVOTestExample.build().relatedParty(List.of(RelatedPartyVOTestExample.build()))));
		testEntries.add(Arguments.of("A resource catalog with non-existent related parties should not be created.",
				ResourceCatalogCreateVOTestExample.build().relatedParty(
						List.of((RelatedPartyVOTestExample.build().id("urn:ngsi-ld:organisation:non-existent"))))));

		testEntries.add(Arguments.of("A resource catalog with an invalid resource category should not be created.",
				ResourceCatalogCreateVOTestExample.build()
						.category(List.of(ResourceCategoryRefVOTestExample.build()))));
		testEntries.add(Arguments.of("A resource catalog with a non-existent resource category should not be created.",
				ResourceCatalogCreateVOTestExample.build().category(
						List.of(ResourceCategoryRefVOTestExample.build().id("urn:ngsi-ld:category:non-existent")))));

		return testEntries.stream();
	}

	@Disabled("Security is handled externally, thus 401 and 403 cannot happen.")
	@Test
	@Override
	public void createResourceCatalog401() throws Exception {

	}

	@Disabled("Security is handled externally, thus 401 and 403 cannot happen.")
	@Test
	@Override
	public void createResourceCatalog403() throws Exception {

	}

	@Disabled("Prohibited by the framework.")
	@Test
	@Override
	public void createResourceCatalog405() throws Exception {
	}

	@Disabled("No implicit creation, impossible state.")
	@Test
	@Override
	public void createResourceCatalog409() throws Exception {

	}

	@Override
	public void createResourceCatalog500() throws Exception {

	}

	@Test
	@Override
	public void deleteResourceCatalog204() throws Exception {
		ResourceCatalogCreateVO emptyCreate = ResourceCatalogCreateVOTestExample.build();

		HttpResponse<ResourceCatalogVO> createResponse = resourceCatalogApiTestClient.createResourceCatalog(
				emptyCreate);
		assertEquals(HttpStatus.CREATED, createResponse.getStatus(),
				"The resource catalog should have been created first.");

		String rfId = createResponse.body().getId();

		assertEquals(HttpStatus.NO_CONTENT,
				callAndCatch(() -> resourceCatalogApiTestClient.deleteResourceCatalog(rfId)).getStatus(),
				"The resource catalog should have been deleted.");

		assertEquals(HttpStatus.NOT_FOUND,
				callAndCatch(() -> resourceCatalogApiTestClient.retrieveResourceCatalog(rfId, null)).status(),
				"The resource catalog should not exist anymore.");
	}

	@Disabled("400 is impossible to happen on deletion with the current implementation.")
	@Test
	@Override
	public void deleteResourceCatalog400() throws Exception {

	}

	@Disabled("Security is handled externally, thus 401 and 403 cannot happen.")
	@Test
	@Override
	public void deleteResourceCatalog401() throws Exception {

	}

	@Disabled("Security is handled externally, thus 401 and 403 cannot happen.")
	@Test
	@Override
	public void deleteResourceCatalog403() throws Exception {

	}

	@Test
	@Override
	public void deleteResourceCatalog404() throws Exception {
		HttpResponse<?> notFoundResponse = callAndCatch(
				() -> resourceCatalogApiTestClient.deleteResourceCatalog("urn:ngsi-ld:resource-catalog:no-pop"));
		assertEquals(HttpStatus.NOT_FOUND,
				notFoundResponse.getStatus(),
				"No such resource-catalog should exist.");

		Optional<ErrorDetails> optionalErrorDetails = notFoundResponse.getBody(ErrorDetails.class);
		assertTrue(optionalErrorDetails.isPresent(), "Error details should be provided.");

		notFoundResponse = callAndCatch(() -> resourceCatalogApiTestClient.deleteResourceCatalog("invalid-id"));
		assertEquals(HttpStatus.NOT_FOUND,
				notFoundResponse.getStatus(),
				"No such resource-catalog should exist.");

		optionalErrorDetails = notFoundResponse.getBody(ErrorDetails.class);
		assertTrue(optionalErrorDetails.isPresent(), "Error details should be provided.");
	}

	@Disabled("Prohibited by the framework.")
	@Test
	@Override
	public void deleteResourceCatalog405() throws Exception {

	}

	@Disabled("Impossible status.")
	@Test
	@Override
	public void deleteResourceCatalog409() throws Exception {

	}

	@Override
	public void deleteResourceCatalog500() throws Exception {

	}

	@Test
	@Override
	public void listResourceCatalog200() throws Exception {

		List<ResourceCatalogVO> expectedResourceCatalogs = new ArrayList<>();
		for (int i = 0; i < 10; i++) {
			ResourceCatalogCreateVO resourceCatalogCreateVO = ResourceCatalogCreateVOTestExample.build();
			String id = resourceCatalogApiTestClient.createResourceCatalog(resourceCatalogCreateVO)
					.body().getId();
			ResourceCatalogVO resourceCatalogVO = ResourceCatalogVOTestExample.build();
			resourceCatalogVO
					.id(id)
					.href(URI.create(id))
					.category(null)
					.relatedParty(null);
			expectedResourceCatalogs.add(resourceCatalogVO);
		}

		HttpResponse<List<ResourceCatalogVO>> resourceCatalogResponse = callAndCatch(
				() -> resourceCatalogApiTestClient.listResourceCatalog(null, null, null));

		assertEquals(HttpStatus.OK, resourceCatalogResponse.getStatus(), "The list should be accessible.");
		assertEquals(expectedResourceCatalogs.size(), resourceCatalogResponse.getBody().get().size(),
				"All resourceCatalogs should have been returned.");
		List<ResourceCatalogVO> retrievedResourceCatalogs = resourceCatalogResponse.getBody().get();

		Map<String, ResourceCatalogVO> retrievedMap = retrievedResourceCatalogs.stream()
				.collect(Collectors.toMap(resourceCatalog -> resourceCatalog.getId(),
						resourceCatalog -> resourceCatalog));

		expectedResourceCatalogs.stream()
				.forEach(
						expectedResourceCatalog -> assertTrue(
								retrievedMap.containsKey(expectedResourceCatalog.getId()),
								String.format("All created resourceCatalogs should be returned - Missing: %s.",
										expectedResourceCatalog,
										retrievedResourceCatalogs)));
		expectedResourceCatalogs.stream().forEach(
				expectedResourceCatalog -> assertEquals(expectedResourceCatalog,
						retrievedMap.get(expectedResourceCatalog.getId()),
						"The correct resourceCatalogs should be retrieved."));

		// get with pagination
		Integer limit = 5;
		HttpResponse<List<ResourceCatalogVO>> firstPartResponse = callAndCatch(
				() -> resourceCatalogApiTestClient.listResourceCatalog(null, 0, limit));
		assertEquals(limit, firstPartResponse.body().size(),
				"Only the requested number of entries should be returend.");
		HttpResponse<List<ResourceCatalogVO>> secondPartResponse = callAndCatch(
				() -> resourceCatalogApiTestClient.listResourceCatalog(null, 0 + limit, limit));
		assertEquals(limit, secondPartResponse.body().size(),
				"Only the requested number of entries should be returend.");

		retrievedResourceCatalogs.clear();
		retrievedResourceCatalogs.addAll(firstPartResponse.body());
		retrievedResourceCatalogs.addAll(secondPartResponse.body());
		expectedResourceCatalogs.stream()
				.forEach(
						expectedResourceCatalog -> assertTrue(
								retrievedMap.containsKey(expectedResourceCatalog.getId()),
								String.format("All created resourceCatalogs should be returned - Missing: %s.",
										expectedResourceCatalog)));
		expectedResourceCatalogs.stream().forEach(
				expectedResourceCatalog -> assertEquals(expectedResourceCatalog,
						retrievedMap.get(expectedResourceCatalog.getId()),
						"The correct resourceCatalogs should be retrieved."));
	}

	@Test
	@Override
	public void listResourceCatalog400() throws Exception {
		HttpResponse<List<ResourceCatalogVO>> badRequestResponse = callAndCatch(
				() -> resourceCatalogApiTestClient.listResourceCatalog(null, -1, null));
		assertEquals(HttpStatus.BAD_REQUEST,
				badRequestResponse.getStatus(),
				"Negative offsets are impossible.");

		Optional<ErrorDetails> optionalErrorDetails = badRequestResponse.getBody(ErrorDetails.class);
		assertTrue(optionalErrorDetails.isPresent(), "Error details should be provided.");

		badRequestResponse = callAndCatch(() -> resourceCatalogApiTestClient.listResourceCatalog(null, null, -1));
		assertEquals(HttpStatus.BAD_REQUEST,
				badRequestResponse.getStatus(),
				"Negative limits are impossible.");
		optionalErrorDetails = badRequestResponse.getBody(ErrorDetails.class);
		assertTrue(optionalErrorDetails.isPresent(), "Error details should be provided.");
	}

	@Disabled("Security is handled externally, thus 401 and 403 cannot happen.")
	@Test
	@Override
	public void listResourceCatalog401() throws Exception {

	}

	@Disabled("Security is handled externally, thus 401 and 403 cannot happen.")
	@Test
	@Override
	public void listResourceCatalog403() throws Exception {

	}

	@Disabled("Not found is not possible here, will be answered with an empty list instead.")
	@Test
	@Override
	public void listResourceCatalog404() throws Exception {

	}

	@Disabled("Prohibited by the framework.")
	@Test
	@Override
	public void listResourceCatalog405() throws Exception {

	}

	@Disabled("Impossible status.")
	@Test
	@Override
	public void listResourceCatalog409() throws Exception {

	}

	@Override
	public void listResourceCatalog500() throws Exception {

	}

	@ParameterizedTest
	@MethodSource("provideResourceCatalogUpdates")
	public void patchResourceCatalog200(String message, ResourceCatalogUpdateVO resourceCatalogUpdateVO,
			ResourceCatalogVO expectedResourceCatalog) throws Exception {
		this.message = message;
		this.resourceCatalogUpdateVO = resourceCatalogUpdateVO;
		this.expectedResourceCatalog = expectedResourceCatalog;
		patchResourceCatalog200();
	}

	@Override
	public void patchResourceCatalog200() throws Exception {
		//first create
		ResourceCatalogCreateVO resourceCatalogCreateVO = ResourceCatalogCreateVOTestExample.build();

		HttpResponse<ResourceCatalogVO> createResponse = callAndCatch(
				() -> resourceCatalogApiTestClient.createResourceCatalog(resourceCatalogCreateVO));
		assertEquals(HttpStatus.CREATED, createResponse.getStatus(),
				"The resource function should have been created first.");

		String resourceId = createResponse.body().getId();

		HttpResponse<ResourceCatalogVO> updateResponse = callAndCatch(
				() -> resourceCatalogApiTestClient.patchResourceCatalog(resourceId, resourceCatalogUpdateVO));
		assertEquals(HttpStatus.OK, updateResponse.getStatus(), message);

		ResourceCatalogVO updatedResourceCatalog = updateResponse.body();
		expectedResourceCatalog.href(URI.create(resourceId)).id(resourceId);

		expectedResourceCatalog.relatedParty(null);
		expectedResourceCatalog.category(null);

		assertEquals(expectedResourceCatalog, updatedResourceCatalog, message);
	}

	private static Stream<Arguments> provideResourceCatalogUpdates() {
		List<Arguments> testEntries = new ArrayList<>();

		ResourceCatalogUpdateVO lifecycleStatusUpdate = ResourceCatalogUpdateVOTestExample.build()
				.lifecycleStatus("dead");
		ResourceCatalogVO expectedLifecycleStatus = ResourceCatalogVOTestExample.build()
				.lifecycleStatus("dead");
		testEntries.add(Arguments.of("The lifecycle state should have been updated.", lifecycleStatusUpdate,
				expectedLifecycleStatus));

		ResourceCatalogUpdateVO descriptionUpdate = ResourceCatalogUpdateVOTestExample.build()
				.description("new-description");
		ResourceCatalogVO expectedDescriptionUpdate = ResourceCatalogVOTestExample.build()
				.description("new-description");
		testEntries.add(Arguments.of("The description should have been updated.", descriptionUpdate,
				expectedDescriptionUpdate));

		ResourceCatalogUpdateVO nameUpdate = ResourceCatalogUpdateVOTestExample.build()
				.name("new-name");
		ResourceCatalogVO expectedNameUpdate = ResourceCatalogVOTestExample.build()
				.name("new-name");
		testEntries.add(Arguments.of("The name should have been updated.", nameUpdate, expectedNameUpdate));

		ResourceCatalogUpdateVO versionUpdate = ResourceCatalogUpdateVOTestExample.build()
				.version("v0.0.2");
		ResourceCatalogVO expectedVersionUpdate = ResourceCatalogVOTestExample.build()
				.version("v0.0.2");
		testEntries.add(Arguments.of("The version should have been updated.", versionUpdate, expectedVersionUpdate));

		TimePeriodVO timePeriodVO = TimePeriodVOTestExample.build().endDateTime(Instant.now())
				.startDateTime(Instant.now());
		ResourceCatalogUpdateVO validForUpdate = ResourceCatalogUpdateVOTestExample.build().validFor(timePeriodVO);
		ResourceCatalogVO expectedValidForUpdate = ResourceCatalogVOTestExample.build().validFor(timePeriodVO);
		testEntries.add(Arguments.of("The validFor should have been updated.", validForUpdate, expectedValidForUpdate));

		return testEntries.stream();
	}

	@ParameterizedTest
	@MethodSource("provideInvalidUpdates")
	public void patchResourceCatalog400(String message, ResourceCatalogUpdateVO invalidUpdateVO) throws Exception {
		this.message = message;
		this.resourceCatalogUpdateVO = invalidUpdateVO;
		patchResourceCatalog400();
	}

	@Override
	public void patchResourceCatalog400() throws Exception {
		//first create
		ResourceCatalogCreateVO resourceCatalogCreateVO = ResourceCatalogCreateVOTestExample.build();

		HttpResponse<ResourceCatalogVO> createResponse = callAndCatch(
				() -> resourceCatalogApiTestClient.createResourceCatalog(resourceCatalogCreateVO));
		assertEquals(HttpStatus.CREATED, createResponse.getStatus(),
				"The resource function should have been created first.");

		String resourceId = createResponse.body().getId();

		HttpResponse<ResourceCatalogVO> updateResponse = callAndCatch(
				() -> resourceCatalogApiTestClient.patchResourceCatalog(resourceId, resourceCatalogUpdateVO));
		assertEquals(HttpStatus.BAD_REQUEST, updateResponse.getStatus(), message);

		Optional<ErrorDetails> optionalErrorDetails = updateResponse.getBody(ErrorDetails.class);
		assertTrue(optionalErrorDetails.isPresent(), "Some error details should be present.");
	}

	private static Stream<Arguments> provideInvalidUpdates() {
		List<Arguments> testEntries = new ArrayList<>();

		testEntries.add(Arguments.of("An update with an invalid related party ref is not allowed.",
				ResourceCatalogUpdateVOTestExample.build()
						.relatedParty(List.of(RelatedPartyVOTestExample.build()))));
		testEntries.add(Arguments.of("An update with an non existent related party is not allowed.",
				ResourceCatalogUpdateVOTestExample.build()
						.relatedParty(List.of(RelatedPartyVOTestExample.build()
								.id("urn:ngsi-ld:organisation:non-existent")))));

		testEntries.add(Arguments.of("An update with an invalid category ref is not allowed.",
				ResourceCatalogUpdateVOTestExample.build()
						.category(List.of(ResourceCategoryRefVOTestExample.build()))));
		testEntries.add(Arguments.of("An update with an non existent category is not allowed.",
				ResourceCatalogUpdateVOTestExample.build()
						.category(List.of(ResourceCategoryRefVOTestExample.build()
								.id("urn:ngsi-ld:resource-category:non-existent")))));

		return testEntries.stream();
	}

	@Disabled("Security is handled externally, thus 401 and 403 cannot happen.")
	@Test
	@Override
	public void patchResourceCatalog401() throws Exception {

	}

	@Disabled("Security is handled externally, thus 401 and 403 cannot happen.")
	@Test
	@Override
	public void patchResourceCatalog403() throws Exception {

	}

	@Test
	@Override
	public void patchResourceCatalog404() throws Exception {
		ResourceCatalogUpdateVO resourceCatalogUpdateVO = ResourceCatalogUpdateVOTestExample.build();
		assertEquals(
				HttpStatus.NOT_FOUND,
				callAndCatch(() -> resourceCatalogApiTestClient.patchResourceCatalog(
						"urn:ngsi-ld:resource-catalog:not-existent", resourceCatalogUpdateVO)).getStatus(),
				"Non existent resource catalog should not be updated.");
	}

	@Disabled("Prohibited by the framework.")
	@Test
	@Override
	public void patchResourceCatalog405() throws Exception {

	}

	@Override
	public void patchResourceCatalog409() throws Exception {
		// TODO: can this happen?
	}

	@Override
	public void patchResourceCatalog500() throws Exception {

	}

	@ParameterizedTest
	@MethodSource("provideFieldParameters")
	public void retrieveResourceCatalog200(String message, String fields, ResourceCatalogVO expectedResourceCatalog)
			throws Exception {
		this.fieldsParameter = fields;
		this.message = message;
		this.expectedResourceCatalog = expectedResourceCatalog;
		retrieveResourceCatalog200();
	}

	@Override
	public void retrieveResourceCatalog200() throws Exception {

		ResourceCatalogCreateVO resourceCatalogCreateVO = ResourceCatalogCreateVOTestExample.build();
		HttpResponse<ResourceCatalogVO> createResponse = callAndCatch(
				() -> resourceCatalogApiTestClient.createResourceCatalog(resourceCatalogCreateVO));
		assertEquals(HttpStatus.CREATED, createResponse.getStatus(), message);
		String id = createResponse.body().getId();

		expectedResourceCatalog
				.id(id)
				.href(URI.create(id));

		//then retrieve
		HttpResponse<ResourceCatalogVO> retrievedRF = callAndCatch(
				() -> resourceCatalogApiTestClient.retrieveResourceCatalog(id, fieldsParameter));
		assertEquals(HttpStatus.OK, retrievedRF.getStatus(), message);
		assertEquals(expectedResourceCatalog, retrievedRF.body(), message);
	}

	private static Stream<Arguments> provideFieldParameters() {
		return Stream.of(
				Arguments.of("Without a fields parameter everything should be returned.", null,
						ResourceCatalogVOTestExample.build()
								// get nulled without values
								.relatedParty(null)
								.category(null)),
				Arguments.of("Only version and the mandatory parameters should have been included.", "version",
						ResourceCatalogVOTestExample.build()
								.relatedParty(null)
								.lastUpdate(null)
								.category(null)
								.relatedParty(null)
								.description(null)
								.lifecycleStatus(null)
								.name(null)
								.validFor(null)
								.atBaseType(null)
								.atSchemaLocation(null)
								.atType(null)),
				Arguments.of(
						"Only the mandatory parameters should have been included when a non-existent field was requested.",
						"nothingToSeeHere", ResourceCatalogVOTestExample.build()
								.relatedParty(null)
								.lastUpdate(null)
								.category(null)
								.relatedParty(null)
								.description(null)
								.lifecycleStatus(null)
								.name(null)
								.version(null)
								.validFor(null)
								.atBaseType(null)
								.atSchemaLocation(null)
								.atType(null)),
				Arguments.of(
						"Only version, lastUpdate, lifecycleStatus, description and the mandatory parameters should have been included.",
						"version,lastUpdate,lifecycleStatus,description", ResourceCatalogVOTestExample.build()
								.relatedParty(null)
								.category(null)
								.relatedParty(null)
								.name(null)
								.validFor(null)
								.atBaseType(null)
								.atSchemaLocation(null)
								.atType(null)));
	}

	@Disabled("400 cannot happen, only 404")
	@Test
	@Override
	public void retrieveResourceCatalog400() throws Exception {

	}

	@Disabled("Security is handled externally, thus 401 and 403 cannot happen.")
	@Test
	@Override
	public void retrieveResourceCatalog401() throws Exception {

	}

	@Disabled("Security is handled externally, thus 401 and 403 cannot happen.")
	@Test
	@Override
	public void retrieveResourceCatalog403() throws Exception {

	}

	@Test
	@Override
	public void retrieveResourceCatalog404() throws Exception {
		HttpResponse<ResourceCatalogVO> response = callAndCatch(
				() -> resourceCatalogApiTestClient.retrieveResourceCatalog("urn:ngsi-ld:resource-function:non-existent",
						null));
		assertEquals(HttpStatus.NOT_FOUND, response.getStatus(), "No such resource-catalog should exist.");

		Optional<ErrorDetails> optionalErrorDetails = response.getBody(ErrorDetails.class);
		assertTrue(optionalErrorDetails.isPresent(), "Error details should have been provided.");
	}

	@Disabled("Prohibited by the framework.")
	@Test
	@Override
	public void retrieveResourceCatalog405() throws Exception {

	}

	@Disabled("Conflict not possible on retrieval")
	@Test
	@Override
	public void retrieveResourceCatalog409() throws Exception {

	}

	@Override
	public void retrieveResourceCatalog500() throws Exception {

	}

	@Override
	protected String getEntityType() {
		return ResourceCatalog.TYPE_RESOURCE_CATALOG;
	}
}
