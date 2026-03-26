package org.fiware.tmforum.softwaremanagement;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import io.micronaut.test.annotation.MockBean;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import org.fiware.ngsi.api.EntitiesApiClient;
import org.fiware.softwaremanagement.api.ResourceApiTestClient;
import org.fiware.softwaremanagement.api.ResourceApiTestSpec;
import org.fiware.softwaremanagement.model.*;
import org.fiware.tmforum.common.configuration.GeneralProperties;
import org.fiware.tmforum.common.exception.ErrorDetails;
import org.fiware.tmforum.common.notification.TMForumEventHandler;
import org.fiware.tmforum.common.test.AbstractApiIT;
import org.fiware.tmforum.common.test.ArgumentPair;
import org.fiware.tmforum.resource.*;
import org.fiware.tmforum.softwaremanagement.rest.ResourceTypeRegistry;
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

/**
 * Integration tests for the Resource API in the Software Management module (TMF730).
 */
@MicronautTest(packages = {"org.fiware.tmforum.softwaremanagement"})
public class ResourceApiIT extends AbstractApiIT implements ResourceApiTestSpec {

	public final ResourceApiTestClient resourceApiTestClient;

	private String message;
	private String fieldsParameter;
	private ResourceCreateVO resourceCreateVO;
	private ResourceUpdateVO resourceUpdateVO;
	private ResourceVO expectedResource;

	/**
	 * Create the integration test instance.
	 *
	 * @param resourceApiTestClient the generated test client
	 * @param entitiesApiClient     the NGSI-LD entities API client
	 * @param objectMapper          the JSON object mapper
	 * @param generalProperties     the general properties
	 */
	public ResourceApiIT(ResourceApiTestClient resourceApiTestClient, EntitiesApiClient entitiesApiClient,
			ObjectMapper objectMapper, GeneralProperties generalProperties) {
		super(entitiesApiClient, objectMapper, generalProperties);
		this.resourceApiTestClient = resourceApiTestClient;
	}

	/**
	 * Mock the TMForumEventHandler to avoid actual event processing during tests.
	 *
	 * @return a mocked TMForumEventHandler
	 */
	@MockBean(TMForumEventHandler.class)
	public TMForumEventHandler eventHandler() {
		TMForumEventHandler eventHandler = mock(TMForumEventHandler.class);

		when(eventHandler.handleCreateEvent(any())).thenReturn(Mono.empty());
		when(eventHandler.handleUpdateEvent(any(), any())).thenReturn(Mono.empty());

		return eventHandler;
	}

	/**
	 * Parameterized test for successful resource creation.
	 *
	 * @param message          the test case description
	 * @param resourceCreateVO the resource creation VO
	 * @param expectedResource the expected result
	 * @throws Exception on test failure
	 */
	@ParameterizedTest
	@MethodSource("provideValidResources")
	public void createResource201(String message, ResourceCreateVO resourceCreateVO, ResourceVO expectedResource)
			throws Exception {
		this.message = message;
		this.resourceCreateVO = resourceCreateVO;
		this.expectedResource = expectedResource;
		createResource201();
	}

	@Override
	public void createResource201() throws Exception {

		HttpResponse<ResourceVO> resourceVOHttpResponse = callAndCatch(
				() -> resourceApiTestClient.createResource(null, resourceCreateVO));
		assertEquals(HttpStatus.CREATED, resourceVOHttpResponse.getStatus(),
				message + " - Error: " + resourceVOHttpResponse.getBody(ErrorDetails.class).orElse(null));
		String rfId = resourceVOHttpResponse.body().getId();
		expectedResource.setId(rfId);
		expectedResource.setHref(rfId);

		assertEquals(expectedResource, resourceVOHttpResponse.body(), message);
	}

	private static Stream<Arguments> provideValidResources() {
		List<Arguments> testEntries = new ArrayList<>();

		testEntries.add(
				Arguments.of("An empty resource should have been created.",
						ResourceCreateVOTestExample.build().atSchemaLocation(null).place(null)
								.resourceSpecification(null),
						ResourceVOTestExample.build().atSchemaLocation(null).place(null)
								.resourceSpecification(null)));

		Instant start = Instant.now();
		Instant end = Instant.now();
		testEntries.add(
				Arguments.of("A resource with operating times should have been created.",
						ResourceCreateVOTestExample.build().atSchemaLocation(null).place(null)
								.resourceSpecification(null)
								.startOperatingDate(start).endOperatingDate(end),
						ResourceVOTestExample.build().atSchemaLocation(null).place(null)
								.resourceSpecification(null).startOperatingDate(start)
								.endOperatingDate(end)));

		List<NoteVO> notes = List.of(NoteVOTestExample.build().atSchemaLocation(null).id("urn:note-1"),
				NoteVOTestExample.build().atSchemaLocation(null).id("urn:note-2"));
		testEntries.add(
				Arguments.of("A resource with notes should have been created.",
						ResourceCreateVOTestExample.build().atSchemaLocation(null).place(null)
								.resourceSpecification(null).note(notes),
						ResourceVOTestExample.build().atSchemaLocation(null).place(null)
								.resourceSpecification(null).note(notes)));

		provideValidFeatureLists()
				.map(ap ->
						Arguments.of(
								ap.message(),
								ResourceCreateVOTestExample.build().atSchemaLocation(null).place(null)
										.resourceSpecification(null)
										.activationFeature(ap.value()),
								ResourceVOTestExample.build().atSchemaLocation(null).place(null)
										.resourceSpecification(null)
										.activationFeature(ap.value()))
				).forEach(testEntries::add);

		provideValidCharacteristicLists()
				.map(ap -> Arguments.of(ap.message(),
						ResourceCreateVOTestExample.build().atSchemaLocation(null).place(null)
								.resourceSpecification(null)
								.resourceCharacteristic(ap.value()),
						ResourceVOTestExample.build().atSchemaLocation(null).place(null)
								.resourceSpecification(null)
								.resourceCharacteristic(ap.value())))
				.forEach(testEntries::add);

		return testEntries.stream();
	}

	/**
	 * Build a clean FeatureVO with test-example defaults nulled for fields that are
	 * either ignored by the mapper (atBaseType, atType, atSchemaLocation) or rejected
	 * by the broker (href).
	 */
	private static FeatureVO cleanFeature(String id) {
		return FeatureVOTestExample.build().id(id)
				.href(null).atSchemaLocation(null).atBaseType(null).atType(null)
				.constraint(null).featureRelationship(null).featureCharacteristic(null);
	}

	private static Stream<ArgumentPair<List<FeatureVO>>> provideValidFeatureLists() {
		List<ArgumentPair<List<FeatureVO>>> featureArguments = new ArrayList<>();

		featureArguments.add(new ArgumentPair<>("A single feature without references should be valid.",
				List.of(cleanFeature("urn:f-1"))));
		featureArguments.add(new ArgumentPair<>("Multiple features without references should be valid.",
				List.of(cleanFeature("urn:f-1"), cleanFeature("urn:f-2"))));
		featureArguments.add(new ArgumentPair<>("Features referencing should be valid.",
				List.of(
						cleanFeature("urn:f-1"),
						cleanFeature("urn:f-2").featureRelationship(
								List.of(FeatureRelationshipVOTestExample.build()
										.href(null).atSchemaLocation(null).atBaseType(null).atType(null)
										.validFor(null).id("urn:f-1"))))));

		provideValidCharacteristicLists()
				.map(ap -> new ArgumentPair<>(String.format("Features should be valid - %s", ap.message()),
						List.of(cleanFeature("urn:f-1").featureCharacteristic(ap.value()))))
				.forEach(featureArguments::add);

		return featureArguments.stream();
	}

	private static Stream<ArgumentPair<List<CharacteristicVO>>> provideValidCharacteristicLists() {
		List<ArgumentPair<List<CharacteristicVO>>> characteristicArguments = new ArrayList<>();

		characteristicArguments.add(new ArgumentPair<>("Single characteristics should be valid.",
				List.of(CharacteristicVOTestExample.build().atSchemaLocation(null).id("urn:c-1")
						.characteristicRelationship(null))));
		characteristicArguments.add(new ArgumentPair<>("Mulitple characteristics should be valid.",
				List.of(CharacteristicVOTestExample.build().atSchemaLocation(null).id("urn:c-1")
								.characteristicRelationship(null),
						CharacteristicVOTestExample.build().atSchemaLocation(null).id("urn:c-2")
								.characteristicRelationship(null))));
		characteristicArguments.add(new ArgumentPair<>("Referencing characteristics should be valid.",
				List.of(CharacteristicVOTestExample.build().atSchemaLocation(null).id("urn:c-1")
								.characteristicRelationship(null),
						CharacteristicVOTestExample.build().atSchemaLocation(null).id("urn:c-2")
								.characteristicRelationship(
										List.of(CharacteristicRelationshipVOTestExample.build()
												.atSchemaLocation(null).id("urn:c-1"))))));
		return characteristicArguments.stream();
	}

	/**
	 * Parameterized test for resource creation with invalid data.
	 *
	 * @param message        the test case description
	 * @param invalidCreateVO the invalid resource creation VO
	 * @throws Exception on test failure
	 */
	@ParameterizedTest
	@MethodSource("provideInvalidResources")
	public void createResource400(String message, ResourceCreateVO invalidCreateVO) throws Exception {
		this.message = message;
		this.resourceCreateVO = invalidCreateVO;
		createResource400();
	}

	@Override
	public void createResource400() throws Exception {
		HttpResponse<ResourceVO> creationResponse = callAndCatch(
				() -> resourceApiTestClient.createResource(null, resourceCreateVO));
		assertEquals(HttpStatus.BAD_REQUEST, creationResponse.getStatus(), message);
		Optional<ErrorDetails> optionalErrorDetails = creationResponse.getBody(ErrorDetails.class);
		assertTrue(optionalErrorDetails.isPresent(), "Error details should be provided.");
	}

	private static Stream<Arguments> provideInvalidResources() {
		List<Arguments> testEntries = new ArrayList<>();

		testEntries.add(Arguments.of("A resource with invalid related parties should not be created.",
				ResourceCreateVOTestExample.build().atSchemaLocation(null).place(null).resourceSpecification(null)
						.relatedParty(List.of(RelatedPartyVOTestExample.build().atSchemaLocation(null)))));
		testEntries.add(Arguments.of("A resource with non-existent related parties should not be created.",
				ResourceCreateVOTestExample.build().atSchemaLocation(null).place(null).resourceSpecification(null)
						.relatedParty(
								List.of((RelatedPartyVOTestExample.build().atSchemaLocation(null)
										.id("urn:ngsi-ld:organisation:non-existent"))))));

		testEntries.add(Arguments.of("A resource with an invalid place ref should not be created.",
				ResourceCreateVOTestExample.build().atSchemaLocation(null)
						.place(RelatedPlaceRefOrValueVOTestExample.build().atSchemaLocation(null))
						.resourceSpecification(null)));
		testEntries.add(Arguments.of("A resource with non-existent place ref should not be created.",
				ResourceCreateVOTestExample.build().atSchemaLocation(null)
						.place(RelatedPlaceRefOrValueVOTestExample.build().atSchemaLocation(null)
								.id("urn:ngsi-ld:place:non-existent"))
						.resourceSpecification(null)));

		testEntries.add(Arguments.of("A resource with an invalid resource ref should not be created.",
				ResourceCreateVOTestExample.build().atSchemaLocation(null).place(null)
						.resourceSpecification(
								ResourceSpecificationRefVOTestExample.build().atSchemaLocation(null))));
		testEntries.add(Arguments.of("A resource with non-existent resource ref should not be created.",
				ResourceCreateVOTestExample.build().atSchemaLocation(null).place(null).resourceSpecification(
						ResourceSpecificationRefVOTestExample.build().atSchemaLocation(null)
								.id("urn:ngsi-ld:resource-specification:non-existent"))));

		List<NoteVO> duplicateNoteVOS = List.of(NoteVOTestExample.build().atSchemaLocation(null).id("note"),
				NoteVOTestExample.build().atSchemaLocation(null).id("note"));
		testEntries.add(Arguments.of("A resource with duplicate note ids should not be created.",
				ResourceCreateVOTestExample.build().atSchemaLocation(null).place(null).resourceSpecification(null)
						.note(duplicateNoteVOS)));

		testEntries.add(Arguments.of("A resource with duplicate feature ids should not be created.",
				ResourceCreateVOTestExample.build().atSchemaLocation(null).place(null).resourceSpecification(null)
						.activationFeature(List.of(FeatureVOTestExample.build().id("my-feature"),
								FeatureVOTestExample.build().id("my-feature")))));
		testEntries.add(Arguments.of("A resource with invalid feature references should not be created.",
				ResourceCreateVOTestExample.build().atSchemaLocation(null).place(null).resourceSpecification(null)
						.activationFeature(List.of(
								FeatureVOTestExample.build().id("my-feature"),
								FeatureVOTestExample.build().featureRelationship(
										List.of(FeatureRelationshipVOTestExample.build()
												.id("non-existent")))))));

		testEntries.add(Arguments.of(
				"A resource with duplicate resource characteristic ids should not be created.",
				ResourceCreateVOTestExample.build().atSchemaLocation(null).place(null).resourceSpecification(null)
						.resourceCharacteristic(
								List.of(CharacteristicVOTestExample.build().atSchemaLocation(null)
												.id("my-characteristic"),
										CharacteristicVOTestExample.build().atSchemaLocation(null)
												.id("my-characteristic")))));
		testEntries.add(Arguments.of("A resource with invalid characteristic references should not be created.",
				ResourceCreateVOTestExample.build().atSchemaLocation(null).place(null).resourceSpecification(null)
						.resourceCharacteristic(List.of(
								CharacteristicVOTestExample.build().atSchemaLocation(null).id("my-feature"),
								CharacteristicVOTestExample.build().atSchemaLocation(null)
										.characteristicRelationship(
												List.of(CharacteristicRelationshipVOTestExample.build()
														.atSchemaLocation(null)
														.id("non-existent")))))));

		testEntries.add(Arguments.of(
				"A resource with duplicate feature characteristic ids should not be created.",
				ResourceCreateVOTestExample.build().atSchemaLocation(null).place(null).resourceSpecification(null)
						.activationFeature(List.of(FeatureVOTestExample.build()
								.featureCharacteristic(
										List.of(CharacteristicVOTestExample.build().atSchemaLocation(null)
														.id("my-characteristic"),
												CharacteristicVOTestExample.build().atSchemaLocation(null)
														.id("my-characteristic")))))));
		testEntries.add(Arguments.of(
				"A resource with invalid feature characteristic references should not be created.",
				ResourceCreateVOTestExample.build().atSchemaLocation(null).place(null).resourceSpecification(null)
						.activationFeature(List.of(FeatureVOTestExample.build()
								.featureCharacteristic(
										List.of(CharacteristicVOTestExample.build().atSchemaLocation(null)
														.id("my-characteristic"),
												CharacteristicVOTestExample.build().atSchemaLocation(null)
														.characteristicRelationship(
																List.of(CharacteristicRelationshipVOTestExample
																		.build().atSchemaLocation(null)
																		.id("non-existent")))))))));

		return testEntries.stream();
	}

	@Disabled("Security is handled externally, thus 401 and 403 cannot happen.")
	@Test
	@Override
	public void createResource401() throws Exception {
	}

	@Disabled("Security is handled externally, thus 401 and 403 cannot happen.")
	@Test
	@Override
	public void createResource403() throws Exception {
	}

	@Disabled("Prohibited by the framework.")
	@Test
	@Override
	public void createResource405() throws Exception {
	}

	@Disabled("No implicit creation, impossible state.")
	@Test
	@Override
	public void createResource409() throws Exception {
	}

	@Override
	public void createResource500() throws Exception {
	}

	@Test
	@Override
	public void deleteResource204() throws Exception {
		ResourceCreateVO emptyCreate = ResourceCreateVOTestExample.build().atSchemaLocation(null)
				.place(null)
				.resourceSpecification(null);

		HttpResponse<ResourceVO> createResponse = resourceApiTestClient.createResource(null, emptyCreate);
		assertEquals(HttpStatus.CREATED, createResponse.getStatus(),
				"The resource should have been created first.");

		String rfId = createResponse.body().getId();

		assertEquals(HttpStatus.NO_CONTENT,
				callAndCatch(() -> resourceApiTestClient.deleteResource(null, rfId)).getStatus(),
				"The resource should have been deleted.");

		assertEquals(HttpStatus.NOT_FOUND,
				callAndCatch(() -> resourceApiTestClient.retrieveResource(null, rfId, null)).status(),
				"The resource should not exist anymore.");
	}

	@Disabled("400 is impossible to happen on deletion with the current implementation.")
	@Test
	@Override
	public void deleteResource400() throws Exception {
	}

	@Disabled("Security is handled externally, thus 401 and 403 cannot happen.")
	@Test
	@Override
	public void deleteResource401() throws Exception {
	}

	@Disabled("Security is handled externally, thus 401 and 403 cannot happen.")
	@Test
	@Override
	public void deleteResource403() throws Exception {
	}

	@Test
	@Override
	public void deleteResource404() throws Exception {
		HttpResponse<?> notFoundResponse = callAndCatch(
				() -> resourceApiTestClient.deleteResource(null, "urn:ngsi-ld:resource:no-pop"));
		assertEquals(HttpStatus.NOT_FOUND,
				notFoundResponse.getStatus(),
				"No such resource should exist.");

		Optional<ErrorDetails> optionalErrorDetails = notFoundResponse.getBody(ErrorDetails.class);
		assertTrue(optionalErrorDetails.isPresent(), "Error details should be provided.");

		notFoundResponse = callAndCatch(() -> resourceApiTestClient.deleteResource(null, "invalid-id"));
		assertEquals(HttpStatus.NOT_FOUND,
				notFoundResponse.getStatus(),
				"No such resource should exist.");

		optionalErrorDetails = notFoundResponse.getBody(ErrorDetails.class);
		assertTrue(optionalErrorDetails.isPresent(), "Error details should be provided.");
	}

	@Disabled("Prohibited by the framework.")
	@Test
	@Override
	public void deleteResource405() throws Exception {
	}

	@Disabled("Impossible status.")
	@Test
	@Override
	public void deleteResource409() throws Exception {
	}

	@Override
	public void deleteResource500() throws Exception {
	}

	@Test
	@Override
	public void listResource200() throws Exception {

		List<ResourceVO> expectedResources = new ArrayList<>();
		for (int i = 0; i < 10; i++) {
			ResourceCreateVO resourceCreateVO = ResourceCreateVOTestExample.build().atSchemaLocation(null)
					.place(null)
					.resourceSpecification(null);
			String id = resourceApiTestClient.createResource(null, resourceCreateVO)
					.body().getId();
			ResourceVO resourceVO = ResourceVOTestExample.build().atSchemaLocation(null);
			resourceVO
					.id(id)
					.href(id)
					.place(null)
					.relatedParty(null)
					.resourceSpecification(null);
			expectedResources.add(resourceVO);
		}

		HttpResponse<List<ResourceVO>> resourceResponse = callAndCatch(
				() -> resourceApiTestClient.listResource(null, null, null, null));

		assertEquals(HttpStatus.OK, resourceResponse.getStatus(), "The list should be accessible.");
		assertEquals(expectedResources.size(), resourceResponse.getBody().get().size(),
				"All resources should have been returned.");
		List<ResourceVO> retrievedResources = resourceResponse.getBody().get();

		Map<String, ResourceVO> retrievedMap = retrievedResources.stream()
				.collect(Collectors.toMap(resource -> resource.getId(),
						resource -> resource));

		expectedResources.stream()
				.forEach(
						expectedResource -> assertTrue(
								retrievedMap.containsKey(expectedResource.getId()),
								String.format("All created resources should be returned - Missing: %s.",
										expectedResource,
										retrievedResources)));
		expectedResources.stream().forEach(
				expectedResource -> assertEquals(expectedResource,
						retrievedMap.get(expectedResource.getId()),
						"The correct resources should be retrieved."));

		// get with pagination
		Integer limit = 5;
		HttpResponse<List<ResourceVO>> firstPartResponse = callAndCatch(
				() -> resourceApiTestClient.listResource(null, null, 0, limit));
		assertEquals(limit, firstPartResponse.body().size(),
				"Only the requested number of entries should be returned.");
		HttpResponse<List<ResourceVO>> secondPartResponse = callAndCatch(
				() -> resourceApiTestClient.listResource(null, null, 0 + limit, limit));
		assertEquals(limit, secondPartResponse.body().size(),
				"Only the requested number of entries should be returned.");

		retrievedResources.clear();
		retrievedResources.addAll(firstPartResponse.body());
		retrievedResources.addAll(secondPartResponse.body());
		expectedResources.stream()
				.forEach(
						expectedResource -> assertTrue(
								retrievedMap.containsKey(expectedResource.getId()),
								String.format("All created resources should be returned - Missing: %s.",
										expectedResource)));
		expectedResources.stream().forEach(
				expectedResource -> assertEquals(expectedResource,
						retrievedMap.get(expectedResource.getId()),
						"The correct resources should be retrieved."));
	}

	@Test
	@Override
	public void listResource400() throws Exception {
		HttpResponse<List<ResourceVO>> badRequestResponse = callAndCatch(
				() -> resourceApiTestClient.listResource(null, null, -1, null));
		assertEquals(HttpStatus.BAD_REQUEST,
				badRequestResponse.getStatus(),
				"Negative offsets are impossible.");

		Optional<ErrorDetails> optionalErrorDetails = badRequestResponse.getBody(ErrorDetails.class);
		assertTrue(optionalErrorDetails.isPresent(), "Error details should be provided.");

		badRequestResponse = callAndCatch(() -> resourceApiTestClient.listResource(null, null, null, -1));
		assertEquals(HttpStatus.BAD_REQUEST,
				badRequestResponse.getStatus(),
				"Negative limits are impossible.");
		optionalErrorDetails = badRequestResponse.getBody(ErrorDetails.class);
		assertTrue(optionalErrorDetails.isPresent(), "Error details should be provided.");
	}

	@Disabled("Security is handled externally, thus 401 and 403 cannot happen.")
	@Test
	@Override
	public void listResource401() throws Exception {
	}

	@Disabled("Security is handled externally, thus 401 and 403 cannot happen.")
	@Test
	@Override
	public void listResource403() throws Exception {
	}

	@Disabled("Not found is not possible here, will be answered with an empty list instead.")
	@Test
	@Override
	public void listResource404() throws Exception {
	}

	@Disabled("Prohibited by the framework.")
	@Test
	@Override
	public void listResource405() throws Exception {
	}

	@Disabled("Impossible status.")
	@Test
	@Override
	public void listResource409() throws Exception {
	}

	@Override
	public void listResource500() throws Exception {
	}

	/**
	 * Parameterized test for successful resource patching.
	 *
	 * @param message          the test case description
	 * @param resourceUpdateVO the resource update VO
	 * @param expectedResource the expected result
	 * @throws Exception on test failure
	 */
	@ParameterizedTest
	@MethodSource("provideResourceUpdates")
	public void patchResource200(String message, ResourceUpdateVO resourceUpdateVO, ResourceVO expectedResource)
			throws Exception {
		this.message = message;
		this.resourceUpdateVO = resourceUpdateVO;
		this.expectedResource = expectedResource;
		patchResource200();
	}

	@Override
	public void patchResource200() throws Exception {
		//first create
		ResourceCreateVO resourceCreateVO = ResourceCreateVOTestExample.build().atSchemaLocation(null)
				.place(null).resourceSpecification(null);

		HttpResponse<ResourceVO> createResponse = callAndCatch(
				() -> resourceApiTestClient.createResource(null, resourceCreateVO));
		assertEquals(HttpStatus.CREATED, createResponse.getStatus(),
				"The resource function should have been created first.");

		String resourceId = createResponse.body().getId();

		HttpResponse<ResourceVO> updateResponse = callAndCatch(
				() -> resourceApiTestClient.patchResource(null, resourceId, resourceUpdateVO));
		assertEquals(HttpStatus.OK, updateResponse.getStatus(), message);

		ResourceVO updatedResource = updateResponse.body();
		expectedResource.href(resourceId).id(resourceId).relatedParty(null);

		assertEquals(expectedResource, updatedResource, message);
	}

	private static Stream<Arguments> provideResourceUpdates() {
		List<Arguments> testEntries = new ArrayList<>();

		testEntries.add(Arguments.of("The description should have been updated.",
				ResourceUpdateVOTestExample.build().atSchemaLocation(null)
						.place(null)
						.resourceSpecification(null)
						.description("new-description"),
				ResourceVOTestExample.build().atSchemaLocation(null)
						.place(null)
						.resourceSpecification(null)
						.description("new-description")));

		testEntries.add(Arguments.of("The name should have been updated.",
				ResourceUpdateVOTestExample.build().atSchemaLocation(null)
						.place(null)
						.resourceSpecification(null)
						.name("new-name"),
				ResourceVOTestExample.build().atSchemaLocation(null)
						.place(null)
						.resourceSpecification(null)
						.name("new-name")));

		testEntries.add(Arguments.of("The category should have been updated.",
				ResourceUpdateVOTestExample.build().atSchemaLocation(null)
						.place(null)
						.resourceSpecification(null)
						.category("new-category"),
				ResourceVOTestExample.build().atSchemaLocation(null)
						.place(null)
						.resourceSpecification(null)
						.category("new-category")));

		Instant date = Instant.now();
		testEntries.add(Arguments.of("The endOperatingDate should have been updated.",
				ResourceUpdateVOTestExample.build().atSchemaLocation(null)
						.place(null)
						.resourceSpecification(null)
						.endOperatingDate(date),
				ResourceVOTestExample.build().atSchemaLocation(null)
						.place(null)
						.resourceSpecification(null)
						.endOperatingDate(date)));

		testEntries.add(Arguments.of("The startOperatingDate should have been updated.",
				ResourceUpdateVOTestExample.build().atSchemaLocation(null)
						.place(null)
						.resourceSpecification(null)
						.startOperatingDate(date),
				ResourceVOTestExample.build().atSchemaLocation(null)
						.place(null)
						.resourceSpecification(null)
						.startOperatingDate(date)));

		testEntries.add(Arguments.of("The resourceVersion should have been updated.",
				ResourceUpdateVOTestExample.build().atSchemaLocation(null)
						.place(null)
						.resourceSpecification(null)
						.resourceVersion("new-version"),
				ResourceVOTestExample.build().atSchemaLocation(null)
						.place(null)
						.resourceSpecification(null)
						.resourceVersion("new-version")));

		List<NoteVO> notes = List.of(NoteVOTestExample.build().atSchemaLocation(null).id("urn:note-1"),
				NoteVOTestExample.build().atSchemaLocation(null).id("urn:note-2"));
		testEntries.add(Arguments.of("The notes should have been updated.",
				ResourceUpdateVOTestExample.build().atSchemaLocation(null)
						.place(null)
						.resourceSpecification(null)
						.note(notes),
				ResourceVOTestExample.build().atSchemaLocation(null)
						.place(null)
						.resourceSpecification(null)
						.note(notes)));

		provideValidCharacteristicLists()
				.map(ap -> Arguments.of(
						String.format("Resource characteristics should be updated - %s", ap.message()),
						ResourceUpdateVOTestExample.build().atSchemaLocation(null)
								.place(null)
								.resourceSpecification(null)
								.resourceCharacteristic(ap.value()),
						ResourceVOTestExample.build().atSchemaLocation(null)
								.place(null)
								.resourceSpecification(null)
								.resourceCharacteristic(ap.value())
				))
				.forEach(testEntries::add);

		provideValidFeatureLists()
				.map(ap -> Arguments.of(
						String.format("Activation feature should be updated - %s", ap.message()),
						ResourceUpdateVOTestExample.build().atSchemaLocation(null)
								.place(null)
								.resourceSpecification(null)
								.activationFeature(ap.value()),
						ResourceVOTestExample.build().atSchemaLocation(null)
								.place(null)
								.resourceSpecification(null)
								.activationFeature(ap.value())
				))
				.forEach(testEntries::add);

		testEntries.add(Arguments.of("The operationalState should have been updated.",
				ResourceUpdateVOTestExample.build().atSchemaLocation(null)
						.place(null)
						.resourceSpecification(null)
						.operationalState(ResourceOperationalStateTypeVO.DISABLE),
				ResourceVOTestExample.build().atSchemaLocation(null)
						.place(null)
						.resourceSpecification(null)
						.operationalState(ResourceOperationalStateTypeVO.DISABLE)));

		testEntries.add(Arguments.of("The status should have been updated.",
				ResourceUpdateVOTestExample.build().atSchemaLocation(null)
						.place(null)
						.resourceSpecification(null)
						.resourceStatus(ResourceStatusTypeVO.ALARM),
				ResourceVOTestExample.build().atSchemaLocation(null)
						.place(null)
						.resourceSpecification(null)
						.resourceStatus(ResourceStatusTypeVO.ALARM)));

		testEntries.add(Arguments.of("The usageState should have been updated.",
				ResourceUpdateVOTestExample.build().atSchemaLocation(null)
						.place(null)
						.resourceSpecification(null)
						.usageState(ResourceUsageStateTypeVO.IDLE),
				ResourceVOTestExample.build().atSchemaLocation(null)
						.place(null)
						.resourceSpecification(null)
						.usageState(ResourceUsageStateTypeVO.IDLE)));

		testEntries.add(Arguments.of("The baseType should have been updated.",
				ResourceUpdateVOTestExample.build().atSchemaLocation(null)
						.place(null)
						.resourceSpecification(null)
						.atBaseType("Resource"),
				ResourceVOTestExample.build().atSchemaLocation(null)
						.place(null)
						.resourceSpecification(null)
						.atBaseType("Resource")));

		testEntries.add(Arguments.of("The name should have been updated.",
				ResourceUpdateVOTestExample.build().atSchemaLocation(null)
						.place(null)
						.resourceSpecification(null)
						.name("Updated Resource"),
				ResourceVOTestExample.build().atSchemaLocation(null)
						.place(null)
						.resourceSpecification(null)
						.name("Updated Resource")));

		return testEntries.stream();
	}

	/**
	 * Parameterized test for resource patching with invalid data.
	 *
	 * @param message         the test case description
	 * @param invalidUpdateVO the invalid resource update VO
	 * @throws Exception on test failure
	 */
	@ParameterizedTest
	@MethodSource("provideInvalidUpdates")
	public void patchResource400(String message, ResourceUpdateVO invalidUpdateVO) throws Exception {
		this.message = message;
		this.resourceUpdateVO = invalidUpdateVO;
		patchResource400();
	}

	@Override
	public void patchResource400() throws Exception {
		//first create
		ResourceCreateVO resourceCreateVO = ResourceCreateVOTestExample.build().atSchemaLocation(null)
				.place(null).resourceSpecification(null);

		HttpResponse<ResourceVO> createResponse = callAndCatch(
				() -> resourceApiTestClient.createResource(null, resourceCreateVO));
		assertEquals(HttpStatus.CREATED, createResponse.getStatus(),
				"The resource function should have been created first.");

		String resourceId = createResponse.body().getId();

		HttpResponse<ResourceVO> updateResponse = callAndCatch(
				() -> resourceApiTestClient.patchResource(null, resourceId, resourceUpdateVO));
		assertEquals(HttpStatus.BAD_REQUEST, updateResponse.getStatus(), message);

		Optional<ErrorDetails> optionalErrorDetails = updateResponse.getBody(ErrorDetails.class);
		assertTrue(optionalErrorDetails.isPresent(), "Some error details should be present.");
	}

	private static Stream<Arguments> provideInvalidUpdates() {
		List<Arguments> testEntries = new ArrayList<>();

		testEntries.add(Arguments.of("A resource with invalid related parties should not be created.",
				ResourceUpdateVOTestExample.build().atSchemaLocation(null).place(null).resourceSpecification(null)
						.relatedParty(List.of(RelatedPartyVOTestExample.build().atSchemaLocation(null)))));
		testEntries.add(Arguments.of("A resource with non-existent related parties should not be created.",
				ResourceUpdateVOTestExample.build().atSchemaLocation(null).place(null).resourceSpecification(null)
						.relatedParty(
								List.of((RelatedPartyVOTestExample.build().atSchemaLocation(null)
										.id("urn:ngsi-ld:organisation:non-existent"))))));

		testEntries.add(Arguments.of("A resource with an invalid place ref should not be created.",
				ResourceUpdateVOTestExample.build().atSchemaLocation(null)
						.place(RelatedPlaceRefOrValueVOTestExample.build().atSchemaLocation(null))
						.resourceSpecification(null)));
		testEntries.add(Arguments.of("A resource with non-existent place ref should not be created.",
				ResourceUpdateVOTestExample.build().atSchemaLocation(null)
						.place(RelatedPlaceRefOrValueVOTestExample.build().atSchemaLocation(null)
								.id("urn:ngsi-ld:place:non-existent"))
						.resourceSpecification(null)));

		testEntries.add(Arguments.of("A resource with an invalid resource ref should not be created.",
				ResourceUpdateVOTestExample.build().atSchemaLocation(null).place(null)
						.resourceSpecification(
								ResourceSpecificationRefVOTestExample.build().atSchemaLocation(null))));
		testEntries.add(Arguments.of("A resource with non-existent resource ref should not be created.",
				ResourceUpdateVOTestExample.build().atSchemaLocation(null).place(null).resourceSpecification(
						ResourceSpecificationRefVOTestExample.build().atSchemaLocation(null)
								.id("urn:ngsi-ld:resource-specification:non-existent"))));

		testEntries.add(Arguments.of("A resource with duplicate feature ids should not be created.",
				ResourceUpdateVOTestExample.build().atSchemaLocation(null).place(null).resourceSpecification(null)
						.activationFeature(List.of(FeatureVOTestExample.build().id("my-feature"),
								FeatureVOTestExample.build().id("my-feature")))));
		testEntries.add(Arguments.of("A resource with invalid feature references should not be created.",
				ResourceUpdateVOTestExample.build().atSchemaLocation(null).place(null).resourceSpecification(null)
						.activationFeature(List.of(
								FeatureVOTestExample.build().id("my-feature"),
								FeatureVOTestExample.build().featureRelationship(
										List.of(FeatureRelationshipVOTestExample.build()
												.id("non-existent")))))));

		testEntries.add(Arguments.of(
				"A resource with duplicate resource characteristic ids should not be created.",
				ResourceUpdateVOTestExample.build().atSchemaLocation(null).place(null).resourceSpecification(null)
						.resourceCharacteristic(
								List.of(CharacteristicVOTestExample.build().atSchemaLocation(null)
												.id("my-characteristic"),
										CharacteristicVOTestExample.build().atSchemaLocation(null)
												.id("my-characteristic")))));
		testEntries.add(Arguments.of("A resource with invalid characteristic references should not be created.",
				ResourceUpdateVOTestExample.build().atSchemaLocation(null).place(null).resourceSpecification(null)
						.resourceCharacteristic(List.of(
								CharacteristicVOTestExample.build().atSchemaLocation(null).id("my-feature"),
								CharacteristicVOTestExample.build().atSchemaLocation(null)
										.characteristicRelationship(
												List.of(CharacteristicRelationshipVOTestExample.build()
														.atSchemaLocation(null)
														.id("non-existent")))))));

		testEntries.add(Arguments.of(
				"A resource with duplicate feature characteristic ids should not be created.",
				ResourceUpdateVOTestExample.build().atSchemaLocation(null).place(null).resourceSpecification(null)
						.activationFeature(List.of(FeatureVOTestExample.build()
								.featureCharacteristic(
										List.of(CharacteristicVOTestExample.build().atSchemaLocation(null)
														.id("my-characteristic"),
												CharacteristicVOTestExample.build().atSchemaLocation(null)
														.id("my-characteristic")))))));
		testEntries.add(Arguments.of(
				"A resource with invalid feature characteristic references should not be created.",
				ResourceUpdateVOTestExample.build().atSchemaLocation(null).place(null).resourceSpecification(null)
						.activationFeature(List.of(FeatureVOTestExample.build()
								.featureCharacteristic(
										List.of(CharacteristicVOTestExample.build().atSchemaLocation(null)
														.id("my-characteristic"),
												CharacteristicVOTestExample.build().atSchemaLocation(null)
														.characteristicRelationship(
																List.of(CharacteristicRelationshipVOTestExample
																		.build().atSchemaLocation(null)
																		.id("non-existent")))))))));

		return testEntries.stream();
	}

	@Disabled("Security is handled externally, thus 401 and 403 cannot happen.")
	@Test
	@Override
	public void patchResource401() throws Exception {
	}

	@Disabled("Security is handled externally, thus 401 and 403 cannot happen.")
	@Test
	@Override
	public void patchResource403() throws Exception {
	}

	@Test
	@Override
	public void patchResource404() throws Exception {
		ResourceUpdateVO resourceUpdateVO = ResourceUpdateVOTestExample.build()
				.atSchemaLocation(null)
				.place(null)
				.resourceSpecification(null);
		assertEquals(
				HttpStatus.NOT_FOUND,
				callAndCatch(() -> resourceApiTestClient.patchResource(null,
						"urn:ngsi-ld:resource:not-existent",
						resourceUpdateVO)).getStatus(),
				"Non existent resource should not be updated.");
	}

	@Disabled("Prohibited by the framework.")
	@Test
	@Override
	public void patchResource405() throws Exception {
	}

	@Override
	public void patchResource409() throws Exception {
		// TODO: can this happen?
	}

	@Override
	public void patchResource500() throws Exception {
	}

	/**
	 * Parameterized test for successful resource retrieval.
	 *
	 * @param message          the test case description
	 * @param fields           the fields parameter
	 * @param expectedResource the expected result
	 * @throws Exception on test failure
	 */
	@ParameterizedTest
	@MethodSource("provideFieldParameters")
	public void retrieveResource200(String message, String fields, ResourceVO expectedResource) throws Exception {
		this.fieldsParameter = fields;
		this.message = message;
		this.expectedResource = expectedResource;
		retrieveResource200();
	}

	@Override
	public void retrieveResource200() throws Exception {

		ResourceCreateVO resourceCreateVO = ResourceCreateVOTestExample.build().atSchemaLocation(null)
				.place(null)
				.resourceSpecification(null);
		HttpResponse<ResourceVO> createResponse = callAndCatch(
				() -> resourceApiTestClient.createResource(null, resourceCreateVO));
		assertEquals(HttpStatus.CREATED, createResponse.getStatus(), message);
		String id = createResponse.body().getId();

		expectedResource
				.id(id)
				.href(id);

		//then retrieve
		HttpResponse<ResourceVO> retrievedRF = callAndCatch(
				() -> resourceApiTestClient.retrieveResource(null, id, fieldsParameter));
		assertEquals(HttpStatus.OK, retrievedRF.getStatus(), message);
		assertEquals(expectedResource, retrievedRF.body(), message);
	}

	private static Stream<Arguments> provideFieldParameters() {
		// NOTE: Field filtering via the 'fields' query parameter is not yet implemented.
		// All test cases expect the full entity to be returned regardless of the fields parameter.
		ResourceVO fullExpected = ResourceVOTestExample.build().atSchemaLocation(null)
				.relatedParty(null)
				.place(null)
				.resourceSpecification(null);
		return Stream.of(
				Arguments.of("Without a fields parameter everything should be returned.",
						null, fullExpected),
				Arguments.of("With a fields parameter, everything is still returned (filtering not implemented).",
						"category", fullExpected),
				Arguments.of("With a non-existent field, everything is still returned (filtering not implemented).",
						"nothingToSeeHere", fullExpected),
				Arguments.of("With multiple fields, everything is still returned (filtering not implemented).",
						"name,description", fullExpected));
	}

	@Disabled("400 cannot happen, only 404")
	@Test
	@Override
	public void retrieveResource400() throws Exception {
	}

	@Disabled("Security is handled externally, thus 401 and 403 cannot happen.")
	@Test
	@Override
	public void retrieveResource401() throws Exception {
	}

	@Disabled("Security is handled externally, thus 401 and 403 cannot happen.")
	@Test
	@Override
	public void retrieveResource403() throws Exception {
	}

	@Test
	@Override
	public void retrieveResource404() throws Exception {
		HttpResponse<ResourceVO> response = callAndCatch(
				() -> resourceApiTestClient.retrieveResource(null,
						"urn:ngsi-ld:resource:non-existent", null));
		assertEquals(HttpStatus.NOT_FOUND, response.getStatus(), "No such resource should exist.");

		Optional<ErrorDetails> optionalErrorDetails = response.getBody(ErrorDetails.class);
		assertTrue(optionalErrorDetails.isPresent(), "Error details should have been provided.");
	}

	@Disabled("Prohibited by the framework.")
	@Test
	@Override
	public void retrieveResource405() throws Exception {
	}

	@Disabled("Conflict not possible on retrieval")
	@Test
	@Override
	public void retrieveResource409() throws Exception {
	}

	@Override
	public void retrieveResource500() throws Exception {
	}

	@Override
	protected String getEntityType() {
		return ResourceTypeRegistry.ALL_RESOURCE_TYPES;
	}

	// --- Sub-type specific integration tests ---

	/**
	 * A permissive JSON Schema URI that allows any additional properties.
	 * Required because the ValidatingDeserializer rejects unknown properties when no schema is provided,
	 * and sub-type-specific fields are treated as unknown on the base ResourceCreateVO.
	 */
	private static final java.net.URI PERMISSIVE_SCHEMA = java.net.URI.create("classpath:permissive-schema.json");

	/**
	 * Helper to build a ResourceCreateVO that represents a sub-type by setting @type and
	 * sub-type-specific fields via the unknownProperties map. A permissive @schemaLocation
	 * is set to pass the schema validation for the additional sub-type fields.
	 *
	 * @param atType       the TMForum @type value (e.g. "SoftwareResource")
	 * @param extraFields  additional sub-type fields to set via unknownProperties
	 * @return the configured ResourceCreateVO
	 */
	private static ResourceCreateVO buildSubTypeCreate(String atType, Map<String, Object> extraFields) {
		ResourceCreateVO createVO = ResourceCreateVOTestExample.build()
				.atSchemaLocation(PERMISSIVE_SCHEMA)
				.place(null)
				.resourceSpecification(null)
				.atType(atType);
		extraFields.forEach(createVO::setUnknownProperties);
		return createVO;
	}

	/**
	 * Helper to build the expected ResourceVO for a sub-type. Base fields come from the test example;
	 * sub-type fields are added to unknownProperties.
	 *
	 * @param atType       the TMForum @type value
	 * @param extraFields  additional sub-type fields expected in unknownProperties
	 * @return the expected ResourceVO
	 */
	private static ResourceVO buildSubTypeExpected(String atType, Map<String, Object> extraFields) {
		ResourceVO expected = ResourceVOTestExample.build()
				.atSchemaLocation(PERMISSIVE_SCHEMA)
				.place(null)
				.resourceSpecification(null)
				.atType(atType);
		extraFields.forEach(expected::setUnknownProperties);
		return expected;
	}

	/**
	 * Parameterized test for creating sub-type Resource entities.
	 *
	 * @param message          the test case description
	 * @param resourceCreateVO the sub-type resource creation VO
	 * @param expectedResource the expected result
	 * @throws Exception on test failure
	 */
	@ParameterizedTest
	@MethodSource("provideSubTypeResources")
	public void createSubTypeResource201(String message, ResourceCreateVO resourceCreateVO,
			ResourceVO expectedResource) throws Exception {
		this.message = message;
		this.resourceCreateVO = resourceCreateVO;
		this.expectedResource = expectedResource;
		createResource201();
	}

	private static Stream<Arguments> provideSubTypeResources() {
		List<Arguments> testEntries = new ArrayList<>();

		// LogicalResource
		testEntries.add(Arguments.of(
				"A LogicalResource should have been created.",
				buildSubTypeCreate("LogicalResource", Map.of("value", "my-logical-value")),
				buildSubTypeExpected("LogicalResource", Map.of("value", "my-logical-value"))));

		// SoftwareResource
		testEntries.add(Arguments.of(
				"A SoftwareResource should have been created.",
				buildSubTypeCreate("SoftwareResource", Map.of(
						"value", "sw-value",
						"isDistributedCurrent", false,
						"targetPlatform", "server")),
				buildSubTypeExpected("SoftwareResource", Map.of(
						"value", "sw-value",
						"isDistributedCurrent", false,
						"targetPlatform", "server"))));

		// API
		testEntries.add(Arguments.of(
				"An API resource should have been created.",
				buildSubTypeCreate("API", Map.of(
						"value", "api-value",
						"targetPlatform", "cloud")),
				buildSubTypeExpected("API", Map.of(
						"value", "api-value",
						"targetPlatform", "cloud"))));

		// InstalledSoftware
		testEntries.add(Arguments.of(
				"An InstalledSoftware resource should have been created.",
				buildSubTypeCreate("InstalledSoftware", Map.of(
						"value", "installed-sw-value",
						"isDistributedCurrent", true,
						"targetPlatform", "server",
						"isUTCTime", true,
						"numProcessesActiveCurrent", 8,
						"numUsersCurrent", 3,
						"serialNumber", "SN-12345")),
				buildSubTypeExpected("InstalledSoftware", Map.of(
						"value", "installed-sw-value",
						"isDistributedCurrent", true,
						"targetPlatform", "server",
						"isUTCTime", true,
						"numProcessesActiveCurrent", 8,
						"numUsersCurrent", 3,
						"serialNumber", "SN-12345"))));

		// HostingPlatformRequirement
		testEntries.add(Arguments.of(
				"A HostingPlatformRequirement resource should have been created.",
				buildSubTypeCreate("HostingPlatformRequirement",
						Map.of("value", "hpr-value")),
				buildSubTypeExpected("HostingPlatformRequirement",
						Map.of("value", "hpr-value"))));

		// PhysicalResource
		testEntries.add(Arguments.of(
				"A PhysicalResource should have been created.",
				buildSubTypeCreate("PhysicalResource", Map.of(
						"powerState", "Full Power Applied",
						"serialNumber", "HW-SN-001",
						"versionNumber", "v2.0")),
				buildSubTypeExpected("PhysicalResource", Map.of(
						"powerState", "Full Power Applied",
						"serialNumber", "HW-SN-001",
						"versionNumber", "v2.0"))));

		// SoftwareSupportPackage
		testEntries.add(Arguments.of(
				"A SoftwareSupportPackage resource should have been created.",
				buildSubTypeCreate("SoftwareSupportPackage", Map.of(
						"powerState", "Unknown",
						"serialNumber", "PKG-001")),
				buildSubTypeExpected("SoftwareSupportPackage", Map.of(
						"powerState", "Unknown",
						"serialNumber", "PKG-001"))));

		return testEntries.stream();
	}

	/**
	 * Test retrieving a sub-type resource preserves all sub-type-specific fields.
	 */
	@ParameterizedTest
	@MethodSource("provideSubTypeRetrievalCases")
	public void retrieveSubTypeResource200(String message, String atType, Map<String, Object> extraFields)
			throws Exception {
		ResourceCreateVO createVO = buildSubTypeCreate(atType, extraFields);
		HttpResponse<ResourceVO> createResponse = callAndCatch(
				() -> resourceApiTestClient.createResource(null, createVO));
		assertEquals(HttpStatus.CREATED, createResponse.getStatus(), message + " - creation failed");
		String id = createResponse.body().getId();

		HttpResponse<ResourceVO> retrieveResponse = callAndCatch(
				() -> resourceApiTestClient.retrieveResource(null, id, null));
		assertEquals(HttpStatus.OK, retrieveResponse.getStatus(), message + " - retrieve failed");

		ResourceVO retrieved = retrieveResponse.body();
		assertEquals(atType, retrieved.getAtType(), message + " - @type mismatch");
		for (Map.Entry<String, Object> entry : extraFields.entrySet()) {
			assertEquals(entry.getValue(), retrieved.getUnknownProperties().get(entry.getKey()),
					String.format("%s - field '%s' mismatch", message, entry.getKey()));
		}
	}

	private static Stream<Arguments> provideSubTypeRetrievalCases() {
		return Stream.of(
				Arguments.of("LogicalResource retrieve", "LogicalResource",
						Map.of("value", "lr-val")),
				Arguments.of("SoftwareResource retrieve", "SoftwareResource",
						Map.of("value", "sr-val", "isDistributedCurrent", false, "targetPlatform", "edge")),
				Arguments.of("API retrieve", "API",
						Map.of("value", "api-val", "targetPlatform", "cloud")),
				Arguments.of("InstalledSoftware retrieve", "InstalledSoftware",
						Map.of("value", "is-val", "serialNumber", "SN-999", "numUsersCurrent", 5)),
				Arguments.of("PhysicalResource retrieve", "PhysicalResource",
						Map.of("serialNumber", "HW-999", "powerState", "Unknown")),
				Arguments.of("SoftwareSupportPackage retrieve", "SoftwareSupportPackage",
						Map.of("serialNumber", "PKG-999")),
				Arguments.of("HostingPlatformRequirement retrieve", "HostingPlatformRequirement",
						Map.of("value", "hpr-val"))
		);
	}

	/**
	 * Test that listing resources returns sub-type resources alongside base resources.
	 */
	@Test
	public void listResourceIncludesSubTypes() throws Exception {
		// Create a base resource
		ResourceCreateVO baseCreate = ResourceCreateVOTestExample.build()
				.atSchemaLocation(null).place(null).resourceSpecification(null);
		HttpResponse<ResourceVO> baseResponse = callAndCatch(
				() -> resourceApiTestClient.createResource(null, baseCreate));
		assertEquals(HttpStatus.CREATED, baseResponse.getStatus());
		String baseId = baseResponse.body().getId();

		// Create a SoftwareResource sub-type
		ResourceCreateVO swCreate = buildSubTypeCreate("SoftwareResource",
				Map.of("value", "list-sw", "targetPlatform", "server"));
		HttpResponse<ResourceVO> swResponse = callAndCatch(
				() -> resourceApiTestClient.createResource(null, swCreate));
		assertEquals(HttpStatus.CREATED, swResponse.getStatus());
		String swId = swResponse.body().getId();

		// Create a PhysicalResource sub-type
		ResourceCreateVO prCreate = buildSubTypeCreate("PhysicalResource",
				Map.of("serialNumber", "LIST-HW-001"));
		HttpResponse<ResourceVO> prResponse = callAndCatch(
				() -> resourceApiTestClient.createResource(null, prCreate));
		assertEquals(HttpStatus.CREATED, prResponse.getStatus());
		String prId = prResponse.body().getId();

		// List all resources - should contain all three
		HttpResponse<List<ResourceVO>> listResponse = callAndCatch(
				() -> resourceApiTestClient.listResource(null, null, null, null));
		assertEquals(HttpStatus.OK, listResponse.getStatus());

		List<String> returnedIds = listResponse.body().stream()
				.map(ResourceVO::getId)
				.toList();
		assertTrue(returnedIds.contains(baseId),
				"Base resource should be in the list.");
		assertTrue(returnedIds.contains(swId),
				"SoftwareResource should be in the list.");
		assertTrue(returnedIds.contains(prId),
				"PhysicalResource should be in the list.");
	}

	/**
	 * Test patching a sub-type resource preserves and updates sub-type-specific fields.
	 */
	@Test
	public void patchSubTypeResource200() throws Exception {
		// Create a SoftwareResource
		ResourceCreateVO createVO = buildSubTypeCreate("SoftwareResource",
				Map.of("value", "original", "targetPlatform", "server", "isDistributedCurrent", false));
		HttpResponse<ResourceVO> createResponse = callAndCatch(
				() -> resourceApiTestClient.createResource(null, createVO));
		assertEquals(HttpStatus.CREATED, createResponse.getStatus(),
				"SoftwareResource creation failed: "
						+ createResponse.getBody(ErrorDetails.class).orElse(null));
		String id = createResponse.body().getId();

		// Patch with updated sub-type field
		ResourceUpdateVO updateVO = ResourceUpdateVOTestExample.build()
				.atSchemaLocation(PERMISSIVE_SCHEMA).place(null).resourceSpecification(null)
				.name("updated-sw");
		updateVO.setUnknownProperties("targetPlatform", "cloud");
		updateVO.setUnknownProperties("isDistributedCurrent", true);

		HttpResponse<ResourceVO> patchResponse = callAndCatch(
				() -> resourceApiTestClient.patchResource(null, id, updateVO));
		assertEquals(HttpStatus.OK, patchResponse.getStatus(),
				"Patching a SoftwareResource should succeed.");

		ResourceVO patched = patchResponse.body();
		assertEquals("updated-sw", patched.getName(),
				"Name should have been updated.");
		assertEquals("cloud", patched.getUnknownProperties().get("targetPlatform"),
				"targetPlatform should have been updated.");
		assertEquals(true, patched.getUnknownProperties().get("isDistributedCurrent"),
				"isDistributedCurrent should have been updated.");
	}

	/**
	 * Test deleting a sub-type resource.
	 */
	@Test
	public void deleteSubTypeResource204() throws Exception {
		ResourceCreateVO createVO = buildSubTypeCreate("InstalledSoftware",
				Map.of("value", "to-delete", "serialNumber", "DEL-001", "isDistributedCurrent", false));
		HttpResponse<ResourceVO> createResponse = callAndCatch(
				() -> resourceApiTestClient.createResource(null, createVO));
		assertEquals(HttpStatus.CREATED, createResponse.getStatus());
		String id = createResponse.body().getId();

		assertEquals(HttpStatus.NO_CONTENT,
				callAndCatch(() -> resourceApiTestClient.deleteResource(null, id)).getStatus(),
				"The sub-type resource should have been deleted.");

		assertEquals(HttpStatus.NOT_FOUND,
				callAndCatch(() -> resourceApiTestClient.retrieveResource(null, id, null)).getStatus(),
				"The sub-type resource should not exist anymore.");
	}
}
