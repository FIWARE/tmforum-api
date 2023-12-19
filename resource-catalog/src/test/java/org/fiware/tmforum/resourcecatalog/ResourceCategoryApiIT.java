package org.fiware.tmforum.resourcecatalog;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import io.micronaut.test.annotation.MockBean;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import org.fiware.ngsi.api.EntitiesApiClient;
import org.fiware.resourcecatalog.api.ResourceCategoryApiTestClient;
import org.fiware.resourcecatalog.api.ResourceCategoryApiTestSpec;
import org.fiware.resourcecatalog.model.RelatedPartyVOTestExample;
import org.fiware.resourcecatalog.model.ResourceCandidateRefVOTestExample;
import org.fiware.resourcecatalog.model.ResourceCategoryCreateVO;
import org.fiware.resourcecatalog.model.ResourceCategoryCreateVOTestExample;
import org.fiware.resourcecatalog.model.ResourceCategoryUpdateVO;
import org.fiware.resourcecatalog.model.ResourceCategoryUpdateVOTestExample;
import org.fiware.resourcecatalog.model.ResourceCategoryVO;
import org.fiware.resourcecatalog.model.ResourceCategoryVOTestExample;
import org.fiware.resourcecatalog.model.ResourceCategoryRefVOTestExample;
import org.fiware.resourcecatalog.model.TimePeriodVO;
import org.fiware.resourcecatalog.model.TimePeriodVOTestExample;
import org.fiware.tmforum.common.notification.EventHandler;
import org.fiware.tmforum.common.configuration.GeneralProperties;
import org.fiware.tmforum.common.exception.ErrorDetails;
import org.fiware.tmforum.common.test.AbstractApiIT;
import org.fiware.tmforum.resource.ResourceCategory;
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
public class ResourceCategoryApiIT extends AbstractApiIT implements ResourceCategoryApiTestSpec {

	public final ResourceCategoryApiTestClient resourceCategoryApiTestClient;

	private String message;
	private String fieldsParameter;
	private ResourceCategoryCreateVO resourceCategoryCreateVO;
	private ResourceCategoryUpdateVO resourceCategoryUpdateVO;
	private ResourceCategoryVO expectedResourceCategory;

	private Clock clock = mock(Clock.class);

	public ResourceCategoryApiIT(ResourceCategoryApiTestClient resourceCategoryApiTestClient,
			EntitiesApiClient entitiesApiClient, ObjectMapper objectMapper, GeneralProperties generalProperties) {
		super(entitiesApiClient, objectMapper, generalProperties);
		this.resourceCategoryApiTestClient = resourceCategoryApiTestClient;
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
	@MethodSource("provideValidResourceCategorys")
	public void createResourceCategory201(String message, ResourceCategoryCreateVO resourceCategoryCreateVO,
			ResourceCategoryVO expectedResourceCategory) throws Exception {
		this.message = message;
		this.resourceCategoryCreateVO = resourceCategoryCreateVO;
		this.expectedResourceCategory = expectedResourceCategory;
		createResourceCategory201();
	}

	@Override
	public void createResourceCategory201() throws Exception {

		Instant currentTimeInstant = Instant.ofEpochSecond(10000);
		when(clock.instant()).thenReturn(currentTimeInstant);

		HttpResponse<ResourceCategoryVO> resourceCategoryVOHttpResponse = callAndCatch(
				() -> resourceCategoryApiTestClient.createResourceCategory(resourceCategoryCreateVO));
		assertEquals(HttpStatus.CREATED, resourceCategoryVOHttpResponse.getStatus(), message);
		String rfId = resourceCategoryVOHttpResponse.body().getId();
		expectedResourceCategory.setId(rfId);
		expectedResourceCategory.setHref(URI.create(rfId));
		expectedResourceCategory.setLastUpdate(currentTimeInstant);

		assertEquals(expectedResourceCategory, resourceCategoryVOHttpResponse.body(), message);
	}

	private static Stream<Arguments> provideValidResourceCategorys() {
		List<Arguments> testEntries = new ArrayList<>();

		ResourceCategoryCreateVO emptyCreate = ResourceCategoryCreateVOTestExample.build().lifecycleStatus("created")
				.parentId(null);
		ResourceCategoryVO expectedEmpty = ResourceCategoryVOTestExample.build().lifecycleStatus("created")
				.parentId(null);
		testEntries.add(
				Arguments.of("An empty resource category should have been created.", emptyCreate, expectedEmpty));

		TimePeriodVO timePeriodVO = TimePeriodVOTestExample.build().endDateTime(Instant.now())
				.startDateTime(Instant.now());
		ResourceCategoryCreateVO createValidFor = ResourceCategoryCreateVOTestExample.build().validFor(timePeriodVO)
				.lifecycleStatus("created").parentId(null);
		ResourceCategoryVO expectedValidFor = ResourceCategoryVOTestExample.build().validFor(timePeriodVO)
				.lifecycleStatus("created").parentId(null);
		testEntries.add(Arguments.of("An resource category with a validFor should have been created.", createValidFor,
				expectedValidFor));

		return testEntries.stream();
	}

	@ParameterizedTest
	@MethodSource("provideInvalidResourceCategorys")
	public void createResourceCategory400(String message, ResourceCategoryCreateVO invalidCreateVO) throws Exception {
		this.message = message;
		this.resourceCategoryCreateVO = invalidCreateVO;
		createResourceCategory400();
	}

	@Override
	public void createResourceCategory400() throws Exception {
		HttpResponse<ResourceCategoryVO> creationResponse = callAndCatch(
				() -> resourceCategoryApiTestClient.createResourceCategory(resourceCategoryCreateVO));
		assertEquals(HttpStatus.BAD_REQUEST, creationResponse.getStatus(), message);
		Optional<ErrorDetails> optionalErrorDetails = creationResponse.getBody(ErrorDetails.class);
		assertTrue(optionalErrorDetails.isPresent(), "Error details should be provided.");
	}

	private static Stream<Arguments> provideInvalidResourceCategorys() {
		List<Arguments> testEntries = new ArrayList<>();

		testEntries.add(Arguments.of("A resource category with an invalid parent category should not be created.",
				ResourceCategoryCreateVOTestExample.build().parentId("my-invalid-id")));
		testEntries.add(Arguments.of("A resource category with a non-existent parent category should not be created.",
				ResourceCategoryCreateVOTestExample.build().parentId("urn:ngsi-ld:resource-category:non-existent")));

		testEntries.add(Arguments.of("A resource category with an invalid resource category should not be created.",
				ResourceCategoryCreateVOTestExample.build().parentId(null)
						.category(List.of(ResourceCategoryRefVOTestExample.build()))));
		testEntries.add(Arguments.of("A resource category with a non-existent resource category should not be created.",
				ResourceCategoryCreateVOTestExample.build().parentId(null).category(
						List.of(ResourceCategoryRefVOTestExample.build()
								.id("urn:ngsi-ld:resource-category:non-existent")))));

		testEntries.add(Arguments.of("A resource category with an invalid related party should not be created.",
				ResourceCategoryCreateVOTestExample.build().parentId(null)
						.relatedParty(List.of(RelatedPartyVOTestExample.build()))));
		testEntries.add(Arguments.of("A resource category with a non-existent resource category should not be created.",
				ResourceCategoryCreateVOTestExample.build().parentId(null).relatedParty(
						List.of(RelatedPartyVOTestExample.build().id("urn:ngsi-ld:organization:non-existent")))));

		testEntries.add(Arguments.of("A resource category with an invalid resource category should not be created.",
				ResourceCategoryCreateVOTestExample.build().parentId(null)
						.resourceCandidate(List.of(ResourceCandidateRefVOTestExample.build()))));
		testEntries.add(Arguments.of("A resource category with a non-existent resource category should not be created.",
				ResourceCategoryCreateVOTestExample.build().parentId(null).resourceCandidate(
						List.of(ResourceCandidateRefVOTestExample.build()
								.id("urn:ngsi-ld:resource-candidate:non-existent")))));

		return testEntries.stream();
	}

	@Disabled("Security is handled externally, thus 401 and 403 cannot happen.")
	@Test
	@Override
	public void createResourceCategory401() throws Exception {

	}

	@Disabled("Security is handled externally, thus 401 and 403 cannot happen.")
	@Test
	@Override
	public void createResourceCategory403() throws Exception {

	}

	@Disabled("Prohibited by the framework.")
	@Test
	@Override
	public void createResourceCategory405() throws Exception {
	}

	@Disabled("No implicit creation, impossible state.")
	@Test
	@Override
	public void createResourceCategory409() throws Exception {

	}

	@Override
	public void createResourceCategory500() throws Exception {

	}

	@Test
	@Override
	public void deleteResourceCategory204() throws Exception {
		ResourceCategoryCreateVO emptyCreate = ResourceCategoryCreateVOTestExample.build().parentId(null);

		HttpResponse<ResourceCategoryVO> createResponse = resourceCategoryApiTestClient.createResourceCategory(
				emptyCreate);
		assertEquals(HttpStatus.CREATED, createResponse.getStatus(),
				"The resource category should have been created first.");

		String rfId = createResponse.body().getId();

		assertEquals(HttpStatus.NO_CONTENT,
				callAndCatch(() -> resourceCategoryApiTestClient.deleteResourceCategory(rfId)).getStatus(),
				"The resource category should have been deleted.");

		assertEquals(HttpStatus.NOT_FOUND,
				callAndCatch(() -> resourceCategoryApiTestClient.retrieveResourceCategory(rfId, null)).status(),
				"The resource category should not exist anymore.");
	}

	@Disabled("400 is impossible to happen on deletion with the current implementation.")
	@Test
	@Override
	public void deleteResourceCategory400() throws Exception {

	}

	@Disabled("Security is handled externally, thus 401 and 403 cannot happen.")
	@Test
	@Override
	public void deleteResourceCategory401() throws Exception {

	}

	@Disabled("Security is handled externally, thus 401 and 403 cannot happen.")
	@Test
	@Override
	public void deleteResourceCategory403() throws Exception {

	}

	@Test
	@Override
	public void deleteResourceCategory404() throws Exception {
		HttpResponse<?> notFoundResponse = callAndCatch(
				() -> resourceCategoryApiTestClient.deleteResourceCategory("urn:ngsi-ld:resource-category:no-pop"));
		assertEquals(HttpStatus.NOT_FOUND,
				notFoundResponse.getStatus(),
				"No such resource-category should exist.");

		Optional<ErrorDetails> optionalErrorDetails = notFoundResponse.getBody(ErrorDetails.class);
		assertTrue(optionalErrorDetails.isPresent(), "Error details should be provided.");

		notFoundResponse = callAndCatch(() -> resourceCategoryApiTestClient.deleteResourceCategory("invalid-id"));
		assertEquals(HttpStatus.NOT_FOUND,
				notFoundResponse.getStatus(),
				"No such resource-category should exist.");

		optionalErrorDetails = notFoundResponse.getBody(ErrorDetails.class);
		assertTrue(optionalErrorDetails.isPresent(), "Error details should be provided.");
	}

	@Disabled("Prohibited by the framework.")
	@Test
	@Override
	public void deleteResourceCategory405() throws Exception {

	}

	@Disabled("Impossible status.")
	@Test
	@Override
	public void deleteResourceCategory409() throws Exception {

	}

	@Override
	public void deleteResourceCategory500() throws Exception {

	}

	@Test
	@Override
	public void listResourceCategory200() throws Exception {

		List<ResourceCategoryVO> expectedResourceCategorys = new ArrayList<>();
		for (int i = 0; i < 10; i++) {
			ResourceCategoryCreateVO resourceCategoryCreateVO = ResourceCategoryCreateVOTestExample.build()
					.parentId(null);
			String id = resourceCategoryApiTestClient.createResourceCategory(resourceCategoryCreateVO)
					.body().getId();
			ResourceCategoryVO resourceCategoryVO = ResourceCategoryVOTestExample.build();
			resourceCategoryVO
					.id(id)
					.href(URI.create(id))
					.parentId(null)
					.category(null)
					.relatedParty(null)
					.resourceCandidate(null);
			expectedResourceCategorys.add(resourceCategoryVO);
		}

		HttpResponse<List<ResourceCategoryVO>> resourceCategoryResponse = callAndCatch(
				() -> resourceCategoryApiTestClient.listResourceCategory(null, null, null));

		assertEquals(HttpStatus.OK, resourceCategoryResponse.getStatus(), "The list should be accessible.");
		assertEquals(expectedResourceCategorys.size(), resourceCategoryResponse.getBody().get().size(),
				"All resourceCategorys should have been returned.");
		List<ResourceCategoryVO> retrievedResourceCategorys = resourceCategoryResponse.getBody().get();

		Map<String, ResourceCategoryVO> retrievedMap = retrievedResourceCategorys.stream()
				.collect(Collectors.toMap(resourceCategory -> resourceCategory.getId(),
						resourceCategory -> resourceCategory));

		expectedResourceCategorys.stream()
				.forEach(
						expectedResourceCategory -> assertTrue(
								retrievedMap.containsKey(expectedResourceCategory.getId()),
								String.format("All created resourceCategorys should be returned - Missing: %s.",
										expectedResourceCategory,
										retrievedResourceCategorys)));
		expectedResourceCategorys.stream().forEach(
				expectedResourceCategory -> assertEquals(expectedResourceCategory,
						retrievedMap.get(expectedResourceCategory.getId()),
						"The correct resourceCategorys should be retrieved."));

		// get with pagination
		Integer limit = 5;
		HttpResponse<List<ResourceCategoryVO>> firstPartResponse = callAndCatch(
				() -> resourceCategoryApiTestClient.listResourceCategory(null, 0, limit));
		assertEquals(limit, firstPartResponse.body().size(),
				"Only the requested number of entries should be returend.");
		HttpResponse<List<ResourceCategoryVO>> secondPartResponse = callAndCatch(
				() -> resourceCategoryApiTestClient.listResourceCategory(null, 0 + limit, limit));
		assertEquals(limit, secondPartResponse.body().size(),
				"Only the requested number of entries should be returend.");

		retrievedResourceCategorys.clear();
		retrievedResourceCategorys.addAll(firstPartResponse.body());
		retrievedResourceCategorys.addAll(secondPartResponse.body());
		expectedResourceCategorys.stream()
				.forEach(
						expectedResourceCategory -> assertTrue(
								retrievedMap.containsKey(expectedResourceCategory.getId()),
								String.format("All created resourceCategorys should be returned - Missing: %s.",
										expectedResourceCategory)));
		expectedResourceCategorys.stream().forEach(
				expectedResourceCategory -> assertEquals(expectedResourceCategory,
						retrievedMap.get(expectedResourceCategory.getId()),
						"The correct resourceCategorys should be retrieved."));
	}

	@Test
	@Override
	public void listResourceCategory400() throws Exception {
		HttpResponse<List<ResourceCategoryVO>> badRequestResponse = callAndCatch(
				() -> resourceCategoryApiTestClient.listResourceCategory(null, -1, null));
		assertEquals(HttpStatus.BAD_REQUEST,
				badRequestResponse.getStatus(),
				"Negative offsets are impossible.");

		Optional<ErrorDetails> optionalErrorDetails = badRequestResponse.getBody(ErrorDetails.class);
		assertTrue(optionalErrorDetails.isPresent(), "Error details should be provided.");

		badRequestResponse = callAndCatch(() -> resourceCategoryApiTestClient.listResourceCategory(null, null, -1));
		assertEquals(HttpStatus.BAD_REQUEST,
				badRequestResponse.getStatus(),
				"Negative limits are impossible.");
		optionalErrorDetails = badRequestResponse.getBody(ErrorDetails.class);
		assertTrue(optionalErrorDetails.isPresent(), "Error details should be provided.");
	}

	@Disabled("Security is handled externally, thus 401 and 403 cannot happen.")
	@Test
	@Override
	public void listResourceCategory401() throws Exception {

	}

	@Disabled("Security is handled externally, thus 401 and 403 cannot happen.")
	@Test
	@Override
	public void listResourceCategory403() throws Exception {

	}

	@Disabled("Not found is not possible here, will be answered with an empty list instead.")
	@Test
	@Override
	public void listResourceCategory404() throws Exception {

	}

	@Disabled("Prohibited by the framework.")
	@Test
	@Override
	public void listResourceCategory405() throws Exception {

	}

	@Disabled("Impossible status.")
	@Test
	@Override
	public void listResourceCategory409() throws Exception {

	}

	@Override
	public void listResourceCategory500() throws Exception {

	}

	@ParameterizedTest
	@MethodSource("provideResourceCategoryUpdates")
	public void patchResourceCategory200(String message, ResourceCategoryUpdateVO resourceCategoryUpdateVO,
			ResourceCategoryVO expectedResourceCategory) throws Exception {
		this.message = message;
		this.resourceCategoryUpdateVO = resourceCategoryUpdateVO;
		this.expectedResourceCategory = expectedResourceCategory;
		patchResourceCategory200();
	}

	@Override
	public void patchResourceCategory200() throws Exception {
		//first create
		ResourceCategoryCreateVO resourceCategoryCreateVO = ResourceCategoryCreateVOTestExample.build().parentId(null);

		HttpResponse<ResourceCategoryVO> createResponse = callAndCatch(
				() -> resourceCategoryApiTestClient.createResourceCategory(resourceCategoryCreateVO));
		assertEquals(HttpStatus.CREATED, createResponse.getStatus(),
				"The resource category should have been created first.");

		String resourceId = createResponse.body().getId();

		HttpResponse<ResourceCategoryVO> updateResponse = callAndCatch(
				() -> resourceCategoryApiTestClient.patchResourceCategory(resourceId, resourceCategoryUpdateVO));
		assertEquals(HttpStatus.OK, updateResponse.getStatus(), message);

		ResourceCategoryVO updatedResourceCategory = updateResponse.body();
		expectedResourceCategory.href(URI.create(resourceId)).id(resourceId).relatedParty(null).resourceCandidate(null);

		expectedResourceCategory.category(null);

		assertEquals(expectedResourceCategory, updatedResourceCategory, message);
	}

	private static Stream<Arguments> provideResourceCategoryUpdates() {
		List<Arguments> testEntries = new ArrayList<>();

		ResourceCategoryUpdateVO lifecycleStatusUpdate = ResourceCategoryUpdateVOTestExample.build()
				.parentId(null)
				.lifecycleStatus("dead");
		ResourceCategoryVO expectedLifecycleStatus = ResourceCategoryVOTestExample.build()
				.parentId(null)
				.lifecycleStatus("dead");
		testEntries.add(Arguments.of("The lifecycle state should have been updated.", lifecycleStatusUpdate,
				expectedLifecycleStatus));

		ResourceCategoryUpdateVO descriptionUpdate = ResourceCategoryUpdateVOTestExample.build()
				.parentId(null)
				.description("new-description");
		ResourceCategoryVO expectedDescriptionUpdate = ResourceCategoryVOTestExample.build()
				.parentId(null)
				.description("new-description");
		testEntries.add(Arguments.of("The description should have been updated.", descriptionUpdate,
				expectedDescriptionUpdate));

		ResourceCategoryUpdateVO nameUpdate = ResourceCategoryUpdateVOTestExample.build()
				.parentId(null)
				.name("new-name");
		ResourceCategoryVO expectedNameUpdate = ResourceCategoryVOTestExample.build()
				.parentId(null)
				.name("new-name");
		testEntries.add(Arguments.of("The name should have been updated.", nameUpdate, expectedNameUpdate));

		ResourceCategoryUpdateVO isRootUpdate = ResourceCategoryUpdateVOTestExample.build()
				.parentId(null)
				.isRoot(true);
		ResourceCategoryVO expectedIsRoot = ResourceCategoryVOTestExample.build()
				.parentId(null)
				.isRoot(true);
		testEntries.add(Arguments.of("isRoot should have been updated.", isRootUpdate, expectedIsRoot));

		ResourceCategoryUpdateVO versionUpdate = ResourceCategoryUpdateVOTestExample.build()
				.parentId(null)
				.version("v0.0.2");
		ResourceCategoryVO expectedVersionUpdate = ResourceCategoryVOTestExample.build()
				.parentId(null)
				.version("v0.0.2");
		testEntries.add(Arguments.of("The version should have been updated.", versionUpdate, expectedVersionUpdate));

		TimePeriodVO timePeriodVO = TimePeriodVOTestExample.build().endDateTime(Instant.now())
				.startDateTime(Instant.now());
		ResourceCategoryUpdateVO validForUpdate = ResourceCategoryUpdateVOTestExample.build().validFor(timePeriodVO)
				.parentId(null);
		ResourceCategoryVO expectedValidForUpdate = ResourceCategoryVOTestExample.build().validFor(timePeriodVO)
				.parentId(null);
		testEntries.add(Arguments.of("The validFor should have been updated.", validForUpdate, expectedValidForUpdate));

		return testEntries.stream();
	}

	@ParameterizedTest
	@MethodSource("provideInvalidUpdates")
	public void patchResourceCategory400(String message, ResourceCategoryUpdateVO invalidUpdateVO) throws Exception {
		this.message = message;
		this.resourceCategoryUpdateVO = invalidUpdateVO;
		patchResourceCategory400();
	}

	@Override
	public void patchResourceCategory400() throws Exception {
		//first create
		ResourceCategoryCreateVO resourceCategoryCreateVO = ResourceCategoryCreateVOTestExample.build().parentId(null);

		HttpResponse<ResourceCategoryVO> createResponse = callAndCatch(
				() -> resourceCategoryApiTestClient.createResourceCategory(resourceCategoryCreateVO));
		assertEquals(HttpStatus.CREATED, createResponse.getStatus(),
				"The resource category should have been created first.");

		String resourceId = createResponse.body().getId();

		HttpResponse<ResourceCategoryVO> updateResponse = callAndCatch(
				() -> resourceCategoryApiTestClient.patchResourceCategory(resourceId, resourceCategoryUpdateVO));
		assertEquals(HttpStatus.BAD_REQUEST, updateResponse.getStatus(), message);

		Optional<ErrorDetails> optionalErrorDetails = updateResponse.getBody(ErrorDetails.class);
		assertTrue(optionalErrorDetails.isPresent(), "Some error details should be present.");
	}

	private static Stream<Arguments> provideInvalidUpdates() {
		List<Arguments> testEntries = new ArrayList<>();

		testEntries.add(Arguments.of("An update with an invalid parent spe ref is not allowed.",
				ResourceCategoryUpdateVOTestExample.build()
						.parentId("invalid")));
		testEntries.add(Arguments.of("An update with an non existent parent is not allowed.",
				ResourceCategoryUpdateVOTestExample.build()
						.parentId("urn:ngsi-ld:resource-category:non-existent")));

		testEntries.add(Arguments.of("An update with an invalid category ref is not allowed.",
				ResourceCategoryUpdateVOTestExample.build()
						.category(List.of(ResourceCategoryRefVOTestExample.build()))));
		testEntries.add(Arguments.of("An update with an non existent category is not allowed.",
				ResourceCategoryUpdateVOTestExample.build()
						.category(List.of(ResourceCategoryRefVOTestExample.build()
								.id("urn:ngsi-ld:resource-category:non-existent")))));

		testEntries.add(Arguments.of("An update with an invalid relatedParty ref is not allowed.",
				ResourceCategoryUpdateVOTestExample.build()
						.relatedParty(List.of(RelatedPartyVOTestExample.build()))));
		testEntries.add(Arguments.of("An update with an non existent relatedParty is not allowed.",
				ResourceCategoryUpdateVOTestExample.build()
						.relatedParty(List.of(RelatedPartyVOTestExample.build()
								.id("urn:ngsi-ld:organization:non-existent")))));

		testEntries.add(Arguments.of("An update with an invalid resourceCandidate ref is not allowed.",
				ResourceCategoryUpdateVOTestExample.build()
						.resourceCandidate(List.of(ResourceCandidateRefVOTestExample.build()))));
		testEntries.add(Arguments.of("An update with an non existent resourceCandidate is not allowed.",
				ResourceCategoryUpdateVOTestExample.build()
						.resourceCandidate(List.of(ResourceCandidateRefVOTestExample.build()
								.id("urn:ngsi-ld:resource-candidate:non-existent")))));

		return testEntries.stream();
	}

	@Disabled("Security is handled externally, thus 401 and 403 cannot happen.")
	@Test
	@Override
	public void patchResourceCategory401() throws Exception {

	}

	@Disabled("Security is handled externally, thus 401 and 403 cannot happen.")
	@Test
	@Override
	public void patchResourceCategory403() throws Exception {

	}

	@Test
	@Override
	public void patchResourceCategory404() throws Exception {
		ResourceCategoryUpdateVO resourceCategoryUpdateVO = ResourceCategoryUpdateVOTestExample.build();
		assertEquals(
				HttpStatus.NOT_FOUND,
				callAndCatch(() -> resourceCategoryApiTestClient.patchResourceCategory(
						"urn:ngsi-ld:resource-category:not-existent", resourceCategoryUpdateVO)).getStatus(),
				"Non existent resource category should not be updated.");
	}

	@Disabled("Prohibited by the framework.")
	@Test
	@Override
	public void patchResourceCategory405() throws Exception {

	}

	@Override
	public void patchResourceCategory409() throws Exception {
		// TODO: can this happen?
	}

	@Override
	public void patchResourceCategory500() throws Exception {

	}

	@ParameterizedTest
	@MethodSource("provideFieldParameters")
	public void retrieveResourceCategory200(String message, String fields, ResourceCategoryVO expectedResourceCategory)
			throws Exception {
		this.fieldsParameter = fields;
		this.message = message;
		this.expectedResourceCategory = expectedResourceCategory;
		retrieveResourceCategory200();
	}

	@Override
	public void retrieveResourceCategory200() throws Exception {

		ResourceCategoryCreateVO resourceCategoryCreateVO = ResourceCategoryCreateVOTestExample.build().parentId(null);
		HttpResponse<ResourceCategoryVO> createResponse = callAndCatch(
				() -> resourceCategoryApiTestClient.createResourceCategory(resourceCategoryCreateVO));
		assertEquals(HttpStatus.CREATED, createResponse.getStatus(), message);
		String id = createResponse.body().getId();

		expectedResourceCategory
				.id(id)
				.href(URI.create(id));

		//then retrieve
		HttpResponse<ResourceCategoryVO> retrievedRF = callAndCatch(
				() -> resourceCategoryApiTestClient.retrieveResourceCategory(id, fieldsParameter));
		assertEquals(HttpStatus.OK, retrievedRF.getStatus(), message);
		assertEquals(expectedResourceCategory, retrievedRF.body(), message);
	}

	private static Stream<Arguments> provideFieldParameters() {
		return Stream.of(
				Arguments.of("Without a fields parameter everything should be returned.", null,
						ResourceCategoryVOTestExample.build()
								// get nulled without values
								.relatedParty(null)
								.category(null)
								.resourceCandidate(null)
								.parentId(null)),
				Arguments.of("Only version and the mandatory parameters should have been included.", "version",
						ResourceCategoryVOTestExample.build()
								.relatedParty(null)
								.lastUpdate(null)
								.isRoot(null)
								.category(null)
								.relatedParty(null)
								.resourceCandidate(null)
								.description(null)
								.lifecycleStatus(null)
								.name(null)
								.parentId(null)
								.validFor(null)
								.atBaseType(null)
								.atSchemaLocation(null)
								.atType(null)),
				Arguments.of(
						"Only the mandatory parameters should have been included when a non-existent field was requested.",
						"nothingToSeeHere", ResourceCategoryVOTestExample.build()
								.relatedParty(null)
								.lastUpdate(null)
								.isRoot(null)
								.category(null)
								.relatedParty(null)
								.resourceCandidate(null)
								.description(null)
								.lifecycleStatus(null)
								.name(null)
								.version(null)
								.parentId(null)
								.validFor(null)
								.atBaseType(null)
								.atSchemaLocation(null)
								.atType(null)),
				Arguments.of(
						"Only version, lastUpdate, lifecycleStatus, description and the mandatory parameters should have been included.",
						"version,lastUpdate,lifecycleStatus,description", ResourceCategoryVOTestExample.build()
								.relatedParty(null)
								.isRoot(null)
								.category(null)
								.relatedParty(null)
								.resourceCandidate(null)
								.name(null)
								.parentId(null)
								.validFor(null)
								.atBaseType(null)
								.atSchemaLocation(null)
								.atType(null)));
	}

	@Disabled("400 cannot happen, only 404")
	@Test
	@Override
	public void retrieveResourceCategory400() throws Exception {

	}

	@Disabled("Security is handled externally, thus 401 and 403 cannot happen.")
	@Test
	@Override
	public void retrieveResourceCategory401() throws Exception {

	}

	@Disabled("Security is handled externally, thus 401 and 403 cannot happen.")
	@Test
	@Override
	public void retrieveResourceCategory403() throws Exception {

	}

	@Test
	@Override
	public void retrieveResourceCategory404() throws Exception {
		HttpResponse<ResourceCategoryVO> response = callAndCatch(
				() -> resourceCategoryApiTestClient.retrieveResourceCategory(
						"urn:ngsi-ld:resource-function:non-existent", null));
		assertEquals(HttpStatus.NOT_FOUND, response.getStatus(), "No such resource-category should exist.");

		Optional<ErrorDetails> optionalErrorDetails = response.getBody(ErrorDetails.class);
		assertTrue(optionalErrorDetails.isPresent(), "Error details should have been provided.");
	}

	@Disabled("Prohibited by the framework.")
	@Test
	@Override
	public void retrieveResourceCategory405() throws Exception {

	}

	@Disabled("Conflict not possible on retrieval")
	@Test
	@Override
	public void retrieveResourceCategory409() throws Exception {

	}

	@Override
	public void retrieveResourceCategory500() throws Exception {

	}

	@Override protected String getEntityType() {
		return ResourceCategory.TYPE_RESOURCE_CATEGORY;
	}
}
