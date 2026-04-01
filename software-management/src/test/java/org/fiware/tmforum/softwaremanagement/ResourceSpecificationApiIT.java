package org.fiware.tmforum.softwaremanagement;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import io.micronaut.test.annotation.MockBean;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import org.fiware.ngsi.api.EntitiesApiClient;
import org.fiware.softwaremanagement.api.ResourceSpecificationApiTestClient;
import org.fiware.softwaremanagement.api.ResourceSpecificationApiTestSpec;
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

/**
 * Integration tests for the ResourceSpecification API in the Software Management module (TMF730).
 */
@MicronautTest(packages = {"org.fiware.tmforum.softwaremanagement"})
public class ResourceSpecificationApiIT extends AbstractApiIT implements ResourceSpecificationApiTestSpec {

	public final ResourceSpecificationApiTestClient resourceSpecificationApiTestClient;

	private String message;
	private ResourceSpecificationCreateVO resourceSpecificationCreateVO;
	private ResourceSpecificationUpdateVO resourceSpecificationUpdateVO;
	private ResourceSpecificationVO expectedResourceSpecification;
	private String fieldsParameter;

	private Clock clock = mock(Clock.class);

	/**
	 * Create the integration test instance.
	 *
	 * @param resourceSpecificationApiTestClient the generated test client
	 * @param entitiesApiClient                  the NGSI-LD entities API client
	 * @param objectMapper                       the JSON object mapper
	 * @param generalProperties                  the general properties
	 */
	public ResourceSpecificationApiIT(ResourceSpecificationApiTestClient resourceSpecificationApiTestClient,
			EntitiesApiClient entitiesApiClient, ObjectMapper objectMapper, GeneralProperties generalProperties) {
		super(entitiesApiClient, objectMapper, generalProperties);
		this.resourceSpecificationApiTestClient = resourceSpecificationApiTestClient;
	}

	/**
	 * Mock the Clock to control time in tests.
	 *
	 * @return a mocked Clock
	 */
	@MockBean(Clock.class)
	public Clock clock() {
		return clock;
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
	 * Parameterized test for successful resource specification creation.
	 *
	 * @param message                        the test case description
	 * @param resourceSpecificationCreateVO  the creation VO
	 * @param expectedResourceSpecification  the expected result
	 * @throws Exception on test failure
	 */
	@ParameterizedTest
	@MethodSource("provideValidResourceSpecifications")
	public void createResourceSpecification201(String message,
			ResourceSpecificationCreateVO resourceSpecificationCreateVO,
			ResourceSpecificationVO expectedResourceSpecification) throws Exception {
		this.message = message;
		this.resourceSpecificationCreateVO = resourceSpecificationCreateVO;
		this.expectedResourceSpecification = expectedResourceSpecification;
		createResourceSpecification201();
	}

	@Override
	public void createResourceSpecification201() throws Exception {

		Instant currentTimeInstant = Instant.ofEpochSecond(10000);
		when(clock.instant()).thenReturn(currentTimeInstant);

		HttpResponse<ResourceSpecificationVO> resourceSpecificationVOHttpResponse = callAndCatch(
				() -> resourceSpecificationApiTestClient.createResourceSpecification(null,
						resourceSpecificationCreateVO));
		assertEquals(HttpStatus.CREATED, resourceSpecificationVOHttpResponse.getStatus(), message);
		String rsId = resourceSpecificationVOHttpResponse.body().getId();
		expectedResourceSpecification.id(rsId).href(URI.create(rsId)).lastUpdate(currentTimeInstant)
				.resourceSpecRelationship(null);

		assertEquals(expectedResourceSpecification, resourceSpecificationVOHttpResponse.body(), message);
	}

	private static Stream<Arguments> provideValidResourceSpecifications() {
		List<Arguments> testEntries = new ArrayList<>();

		ResourceSpecificationCreateVO emptyCreate = ResourceSpecificationCreateVOTestExample.build()
				.atSchemaLocation(null)
				.targetResourceSchema(null)
				.lifecycleStatus("created");
		ResourceSpecificationVO expectedEmpty = ResourceSpecificationVOTestExample.build()
				.atSchemaLocation(null)
				.targetResourceSchema(null)
				.lifecycleStatus("created");
		testEntries.add(
				Arguments.of("An empty resource specification should have been created.", emptyCreate,
						expectedEmpty));

		TimePeriodVO timePeriodVO = TimePeriodVOTestExample.build().endDateTime(Instant.now())
				.startDateTime(Instant.now());
		ResourceSpecificationCreateVO createValidFor = ResourceSpecificationCreateVOTestExample.build()
				.atSchemaLocation(null)
				.targetResourceSchema(null)
				.validFor(timePeriodVO)
				.lifecycleStatus("created");
		ResourceSpecificationVO expectedValidFor = ResourceSpecificationVOTestExample.build()
				.atSchemaLocation(null)
				.targetResourceSchema(null)
				.validFor(timePeriodVO)
				.lifecycleStatus("created");
		testEntries.add(
				Arguments.of("A resource specification with a validFor should have been created.",
						createValidFor, expectedValidFor));

		provideValidFeatureSpecs().map(ap ->
						Arguments.of(
								ap.message(),
								ResourceSpecificationCreateVOTestExample.build()
										.atSchemaLocation(null)
										.targetResourceSchema(null)
										.lifecycleStatus("created")
										.featureSpecification(List.of(ap.value())),
								ResourceSpecificationVOTestExample.build()
										.atSchemaLocation(null)
										.targetResourceSchema(null)
										.lifecycleStatus("created")
										.featureSpecification(List.of(ap.value()))))
				.forEach(testEntries::add);

		return testEntries.stream();
	}

	private static Stream<ArgumentPair<FeatureSpecificationVO>> provideValidFeatureSpecs() {
		List<ArgumentPair<FeatureSpecificationVO>> validFeatureSpecs = new ArrayList<>();

		validFeatureSpecs.add(new ArgumentPair<>(
				"Feature specification with feature spec char rel should be created.",
				FeatureSpecificationVOTestExample.build().atSchemaLocation(null)
						.constraint(null)
						.id("urn:feature-spec")
						.validFor(null)
						.featureSpecRelationship(null)
						.featureSpecCharacteristic(List.of(
								FeatureSpecificationCharacteristicVOTestExample.build().atSchemaLocation(null)
										.id("urn:feature-spec-char")
										.validFor(null)
										.featureSpecCharacteristicValue(null)
										.featureSpecCharRelationship(
												List.of(FeatureSpecificationCharacteristicRelationshipVOTestExample
														.build().atSchemaLocation(null)
														.id("urn:feature-spec-char-rel")
														.validFor(null)
														.resourceSpecificationId(null)))
						))));
		validFeatureSpecs.add(new ArgumentPair<>(
				"Feature specification with feature spec rel should be created.",
				FeatureSpecificationVOTestExample.build().atSchemaLocation(null)
						.constraint(null)
						.id("urn:feature-spec")
						.validFor(null)
						.featureSpecCharacteristic(null)
						.featureSpecRelationship(List.of(
								FeatureSpecificationRelationshipVOTestExample.build().atSchemaLocation(null)
										.validFor(null)
										.parentSpecificationId(null)))));

		return validFeatureSpecs.stream();
	}

	/**
	 * Parameterized test for resource specification creation with invalid data.
	 *
	 * @param message         the test case description
	 * @param invalidCreateVO the invalid creation VO
	 * @throws Exception on test failure
	 */
	@ParameterizedTest
	@MethodSource("provideInvalidResourceSpecifications")
	public void createResourceSpecification400(String message,
			ResourceSpecificationCreateVO invalidCreateVO)
			throws Exception {
		this.message = message;
		this.resourceSpecificationCreateVO = invalidCreateVO;
		createResourceSpecification400();
	}

	@Override
	public void createResourceSpecification400() throws Exception {
		HttpResponse<ResourceSpecificationVO> creationResponse = callAndCatch(
				() -> resourceSpecificationApiTestClient.createResourceSpecification(null,
						resourceSpecificationCreateVO));
		assertEquals(HttpStatus.BAD_REQUEST, creationResponse.getStatus(), message);
		Optional<ErrorDetails> optionalErrorDetails = creationResponse.getBody(ErrorDetails.class);
		assertTrue(optionalErrorDetails.isPresent(), "Error details should be provided.");
	}

	private static Stream<Arguments> provideInvalidResourceSpecifications() {
		List<Arguments> testEntries = new ArrayList<>();

		testEntries.add(Arguments.of(
				"A resource specification with an invalid relatedParty should not be created.",
				ResourceSpecificationCreateVOTestExample.build().atSchemaLocation(null)
						.relatedParty(List.of(RelatedPartyVOTestExample.build().atSchemaLocation(null)))));
		testEntries.add(Arguments.of(
				"A resource specification with a non-existent relatedParty should not be created.",
				ResourceSpecificationCreateVOTestExample.build().atSchemaLocation(null).relatedParty(
						List.of(RelatedPartyVOTestExample.build().atSchemaLocation(null)
								.id("urn:ngsi-ld:organization:non-existent")))));

		testEntries.add(Arguments.of(
				"A resource specification with an invalid resource spec id on the resource spec char rel should not be created.",
				ResourceSpecificationCreateVOTestExample.build().atSchemaLocation(null)
						.resourceSpecCharacteristic(List.of(
								ResourceSpecificationCharacteristicVOTestExample.build().atSchemaLocation(null)
										.resourceSpecCharRelationship(
												List.of(ResourceSpecificationCharacteristicRelationshipVOTestExample
														.build().atSchemaLocation(null)
														.resourceSpecificationId("invalid")))))));

		testEntries.add(Arguments.of(
				"A resource specification with a non-existent resource spec id on the resource spec char rel should not be created.",
				ResourceSpecificationCreateVOTestExample.build().atSchemaLocation(null)
						.resourceSpecCharacteristic(List.of(
								ResourceSpecificationCharacteristicVOTestExample.build().atSchemaLocation(null)
										.resourceSpecCharRelationship(
												List.of(ResourceSpecificationCharacteristicRelationshipVOTestExample
														.build().atSchemaLocation(null)
														.resourceSpecificationId(
																"urn:ngsi-ld:resource-specification:non-existent")))))));
		testEntries.add(Arguments.of(
				"A resource specification with an invalid resourceSpecRelationship id should not be created.",
				ResourceSpecificationCreateVOTestExample.build().atSchemaLocation(null)
						.targetResourceSchema(null)
						.lifecycleStatus("created")
						.resourceSpecRelationship(List.of(
								ResourceSpecificationRelationshipVOTestExample.build().atSchemaLocation(null)
										.id("de29d1fd-cbf8-4bd8-9e75-3f3905db4389")))));

		testEntries.add(Arguments.of(
				"A SoftwareSpecification with an invalid softwareSupportPackage id should not be created.",
				buildSubTypeSpecCreate("SoftwareSpecification", Map.of(
						"softwareSupportPackage", Map.of("id", "not-a-valid-uri")))));

		testEntries
				.addAll(provideInvalidFeatureSpecs()
						.map(ap -> Arguments.of(ap.message(),
								ResourceSpecificationCreateVOTestExample.build().atSchemaLocation(null)
										.featureSpecification(List.of(ap.value()))))
						.toList());

		return testEntries.stream();
	}

	private static Stream<ArgumentPair<FeatureSpecificationVO>> provideInvalidFeatureSpecs() {
		List<ArgumentPair<FeatureSpecificationVO>> invalidFeatureSpecs = new ArrayList<>();

		invalidFeatureSpecs.add(
				new ArgumentPair<>(
						"Feature specification with invalid resource spec id on spec char rel should fail.",
						FeatureSpecificationVOTestExample.build().atSchemaLocation(null)
								.featureSpecCharacteristic(List.of(
										FeatureSpecificationCharacteristicVOTestExample.build()
												.atSchemaLocation(null)
												.featureSpecCharRelationship(
														List.of(FeatureSpecificationCharacteristicRelationshipVOTestExample
																.build().atSchemaLocation(null)
																.featureId(null)
																.resourceSpecificationId("invalid")))
								))));

		invalidFeatureSpecs.add(new ArgumentPair<>(
				"Feature specification with non-existent resource spec id on spec char rel should fail.",
				FeatureSpecificationVOTestExample.build().atSchemaLocation(null)
						.featureSpecCharacteristic(List.of(
								FeatureSpecificationCharacteristicVOTestExample.build().atSchemaLocation(null)
										.featureSpecCharRelationship(
												List.of(FeatureSpecificationCharacteristicRelationshipVOTestExample
														.build().atSchemaLocation(null)
														.resourceSpecificationId(
																"urn:ngsi-ld:resource-specification:non-existent")
														.featureId(null)))
						))));

		// constraint
		invalidFeatureSpecs.add(new ArgumentPair<>(
				"Feature specification with invalid constraint ref should fail.",
				FeatureSpecificationVOTestExample.build().atSchemaLocation(null)
						.constraint(List.of(ConstraintRefVOTestExample.build().atSchemaLocation(null)
								.id("invalid")))));

		invalidFeatureSpecs.add(
				new ArgumentPair<>(
						"Feature specification with non-existent constraint ref should fail.",
						FeatureSpecificationVOTestExample.build().atSchemaLocation(null)
								.constraint(List.of(ConstraintRefVOTestExample.build().atSchemaLocation(null)
										.id("urn:ngsi-ld:constraint:non-existent")))));

		invalidFeatureSpecs.add(
				new ArgumentPair<>(
						"Feature specification with invalid resource id on spec rel should fail.",
						FeatureSpecificationVOTestExample.build().atSchemaLocation(null)
								.featureSpecRelationship(
										List.of(FeatureSpecificationRelationshipVOTestExample.build()
												.atSchemaLocation(null)
												.featureId(null)
												.parentSpecificationId("invalid")))));

		invalidFeatureSpecs.add(
				new ArgumentPair<>(
						"Feature specification with non-existent resource id on spec rel should fail.",
						FeatureSpecificationVOTestExample.build().atSchemaLocation(null)
								.featureSpecRelationship(
										List.of(FeatureSpecificationRelationshipVOTestExample.build()
												.atSchemaLocation(null)
												.parentSpecificationId(
														"urn:ngsi-ld:resource-specification:non-existent")
												.featureId(null)))));

		return invalidFeatureSpecs.stream();
	}

	@Disabled("Security is handled externally, thus 401 and 403 cannot happen.")
	@Test
	@Override
	public void createResourceSpecification401() throws Exception {
	}

	@Disabled("Security is handled externally, thus 401 and 403 cannot happen.")
	@Test
	@Override
	public void createResourceSpecification403() throws Exception {
	}

	@Disabled("Prohibited by the framework.")
	@Test
	@Override
	public void createResourceSpecification405() throws Exception {
	}

	@Disabled("No implicit creation, impossible state.")
	@Test
	@Override
	public void createResourceSpecification409() throws Exception {
	}

	@Override
	public void createResourceSpecification500() throws Exception {
	}

	@Test
	@Override
	public void deleteResourceSpecification204() throws Exception {
		ResourceSpecificationCreateVO emptyCreate = ResourceSpecificationCreateVOTestExample.build()
				.atSchemaLocation(null)
				.targetResourceSchema(null);

		HttpResponse<ResourceSpecificationVO> createResponse =
				resourceSpecificationApiTestClient.createResourceSpecification(null, emptyCreate);
		assertEquals(HttpStatus.CREATED, createResponse.getStatus(),
				"The resource specification should have been created first.");

		String rfId = createResponse.body().getId();

		assertEquals(HttpStatus.NO_CONTENT,
				callAndCatch(
						() -> resourceSpecificationApiTestClient.deleteResourceSpecification(null, rfId))
						.getStatus(),
				"The resource specification should have been deleted.");

		assertEquals(HttpStatus.NOT_FOUND,
				callAndCatch(
						() -> resourceSpecificationApiTestClient.retrieveResourceSpecification(null, rfId,
								null)).status(),
				"The resource specification should not exist anymore.");
	}

	@Disabled("400 is impossible to happen on deletion with the current implementation.")
	@Test
	@Override
	public void deleteResourceSpecification400() throws Exception {
	}

	@Disabled("Security is handled externally, thus 401 and 403 cannot happen.")
	@Test
	@Override
	public void deleteResourceSpecification401() throws Exception {
	}

	@Disabled("Security is handled externally, thus 401 and 403 cannot happen.")
	@Test
	@Override
	public void deleteResourceSpecification403() throws Exception {
	}

	@Test
	@Override
	public void deleteResourceSpecification404() throws Exception {
		HttpResponse<?> notFoundResponse = callAndCatch(
				() -> resourceSpecificationApiTestClient.deleteResourceSpecification(null,
						"urn:ngsi-ld:resource-specification:no-pop"));
		assertEquals(HttpStatus.NOT_FOUND,
				notFoundResponse.getStatus(),
				"No such resource-specification should exist.");

		Optional<ErrorDetails> optionalErrorDetails = notFoundResponse.getBody(ErrorDetails.class);
		assertTrue(optionalErrorDetails.isPresent(), "Error details should be provided.");

		notFoundResponse = callAndCatch(
				() -> resourceSpecificationApiTestClient.deleteResourceSpecification(null, "invalid-id"));
		assertEquals(HttpStatus.NOT_FOUND,
				notFoundResponse.getStatus(),
				"No such resource-specification should exist.");

		optionalErrorDetails = notFoundResponse.getBody(ErrorDetails.class);
		assertTrue(optionalErrorDetails.isPresent(), "Error details should be provided.");
	}

	@Disabled("Prohibited by the framework.")
	@Test
	@Override
	public void deleteResourceSpecification405() throws Exception {
	}

	@Disabled("Impossible status.")
	@Test
	@Override
	public void deleteResourceSpecification409() throws Exception {
	}

	@Override
	public void deleteResourceSpecification500() throws Exception {
	}

	@Disabled("Cleanup has to be solved")
	@Test
	@Override
	public void listResourceSpecification200() throws Exception {

		List<ResourceSpecificationVO> expectedResourceSpecifications = new ArrayList<>();
		for (int i = 0; i < 10; i++) {
			ResourceSpecificationCreateVO resourceSpecificationCreateVO =
					ResourceSpecificationCreateVOTestExample.build().atSchemaLocation(null);
			String id = resourceSpecificationApiTestClient.createResourceSpecification(null,
							resourceSpecificationCreateVO)
					.body().getId();
			ResourceSpecificationVO resourceSpecificationVO = ResourceSpecificationVOTestExample.build()
					.atSchemaLocation(null);
			resourceSpecificationVO
					.id(id)
					.href(URI.create(id))
					.relatedParty(null);
			expectedResourceSpecifications.add(resourceSpecificationVO);
		}

		HttpResponse<List<ResourceSpecificationVO>> resourceSpecificationResponse = callAndCatch(
				() -> resourceSpecificationApiTestClient.listResourceSpecification(null, null, null, null));

		assertEquals(HttpStatus.OK, resourceSpecificationResponse.getStatus(),
				"The list should be accessible.");
		assertEquals(expectedResourceSpecifications.size(),
				resourceSpecificationResponse.getBody().get().size(),
				"All resourceSpecifications should have been returned.");
		List<ResourceSpecificationVO> retrievedResourceSpecifications =
				resourceSpecificationResponse.getBody().get();

		Map<String, ResourceSpecificationVO> retrievedMap = retrievedResourceSpecifications.stream()
				.collect(Collectors.toMap(
						resourceSpecification -> resourceSpecification.getId(),
						resourceSpecification -> resourceSpecification));

		expectedResourceSpecifications.stream()
				.forEach(
						expectedResourceSpecification -> assertTrue(
								retrievedMap.containsKey(expectedResourceSpecification.getId()),
								String.format(
										"All created resourceSpecifications should be returned - Missing: %s.",
										expectedResourceSpecification,
										retrievedResourceSpecifications)));
		expectedResourceSpecifications.stream().forEach(
				expectedResourceSpecification -> assertEquals(expectedResourceSpecification,
						retrievedMap.get(expectedResourceSpecification.getId()),
						"The correct resourceSpecifications should be retrieved."));

		// get with pagination
		Integer limit = 5;
		HttpResponse<List<ResourceSpecificationVO>> firstPartResponse = callAndCatch(
				() -> resourceSpecificationApiTestClient.listResourceSpecification(null, null, 0, limit));
		assertEquals(limit, firstPartResponse.body().size(),
				"Only the requested number of entries should be returned.");
		HttpResponse<List<ResourceSpecificationVO>> secondPartResponse = callAndCatch(
				() -> resourceSpecificationApiTestClient.listResourceSpecification(null, null, 0 + limit,
						limit));
		assertEquals(limit, secondPartResponse.body().size(),
				"Only the requested number of entries should be returned.");

		retrievedResourceSpecifications.clear();
		retrievedResourceSpecifications.addAll(firstPartResponse.body());
		retrievedResourceSpecifications.addAll(secondPartResponse.body());
		expectedResourceSpecifications.stream()
				.forEach(
						expectedResourceSpecification -> assertTrue(
								retrievedMap.containsKey(expectedResourceSpecification.getId()),
								String.format(
										"All created resourceSpecifications should be returned - Missing: %s.",
										expectedResourceSpecification)));
		expectedResourceSpecifications.stream().forEach(
				expectedResourceSpecification -> assertEquals(expectedResourceSpecification,
						retrievedMap.get(expectedResourceSpecification.getId()),
						"The correct resourceSpecifications should be retrieved."));
	}

	@Test
	@Override
	public void listResourceSpecification400() throws Exception {
		HttpResponse<List<ResourceSpecificationVO>> badRequestResponse = callAndCatch(
				() -> resourceSpecificationApiTestClient.listResourceSpecification(null, null, -1, null));
		assertEquals(HttpStatus.BAD_REQUEST,
				badRequestResponse.getStatus(),
				"Negative offsets are impossible.");

		Optional<ErrorDetails> optionalErrorDetails = badRequestResponse.getBody(ErrorDetails.class);
		assertTrue(optionalErrorDetails.isPresent(), "Error details should be provided.");

		badRequestResponse = callAndCatch(
				() -> resourceSpecificationApiTestClient.listResourceSpecification(null, null, null, -1));
		assertEquals(HttpStatus.BAD_REQUEST,
				badRequestResponse.getStatus(),
				"Negative limits are impossible.");
		optionalErrorDetails = badRequestResponse.getBody(ErrorDetails.class);
		assertTrue(optionalErrorDetails.isPresent(), "Error details should be provided.");
	}

	@Disabled("Security is handled externally, thus 401 and 403 cannot happen.")
	@Test
	@Override
	public void listResourceSpecification401() throws Exception {
	}

	@Disabled("Security is handled externally, thus 401 and 403 cannot happen.")
	@Test
	@Override
	public void listResourceSpecification403() throws Exception {
	}

	@Disabled("Not found is not possible here, will be answered with an empty list instead.")
	@Test
	@Override
	public void listResourceSpecification404() throws Exception {
	}

	@Disabled("Prohibited by the framework.")
	@Test
	@Override
	public void listResourceSpecification405() throws Exception {
	}

	@Disabled("Impossible status.")
	@Test
	@Override
	public void listResourceSpecification409() throws Exception {
	}

	@Override
	public void listResourceSpecification500() throws Exception {
	}

	/**
	 * Parameterized test for successful resource specification patching.
	 *
	 * @param message                        the test case description
	 * @param resourceSpecificationUpdateVO  the update VO
	 * @param expectedResourceSpecification  the expected result
	 * @throws Exception on test failure
	 */
	@ParameterizedTest
	@MethodSource("provideResourceSpecificationUpdates")
	public void patchResourceSpecification200(String message,
			ResourceSpecificationUpdateVO resourceSpecificationUpdateVO,
			ResourceSpecificationVO expectedResourceSpecification) throws Exception {
		this.message = message;
		this.resourceSpecificationUpdateVO = resourceSpecificationUpdateVO;
		this.expectedResourceSpecification = expectedResourceSpecification;
		patchResourceSpecification200();
	}

	@Override
	public void patchResourceSpecification200() throws Exception {

		Instant currentTimeInstant = Instant.ofEpochSecond(10000);
		when(clock.instant()).thenReturn(currentTimeInstant);

		//first create
		ResourceSpecificationCreateVO resourceSpecificationCreateVO =
				ResourceSpecificationCreateVOTestExample.build()
						.atSchemaLocation(null)
						.targetResourceSchema(null);

		HttpResponse<ResourceSpecificationVO> createResponse = callAndCatch(
				() -> resourceSpecificationApiTestClient.createResourceSpecification(null,
						resourceSpecificationCreateVO));
		assertEquals(HttpStatus.CREATED, createResponse.getStatus(),
				"The resource function should have been created first.");

		String resourceId = createResponse.body().getId();

		HttpResponse<ResourceSpecificationVO> updateResponse = callAndCatch(
				() -> resourceSpecificationApiTestClient.patchResourceSpecification(null, resourceId,
						resourceSpecificationUpdateVO));
		assertEquals(HttpStatus.OK, updateResponse.getStatus(), message);

		ResourceSpecificationVO updatedResourceSpecification = updateResponse.body();
		expectedResourceSpecification.href(URI.create(resourceId)).id(resourceId)
				.resourceSpecRelationship(null)
				.lastUpdate(currentTimeInstant);

		assertEquals(expectedResourceSpecification, updatedResourceSpecification, message);
	}

	private static Stream<Arguments> provideResourceSpecificationUpdates() {
		List<Arguments> testEntries = new ArrayList<>();

		ResourceSpecificationUpdateVO lifecycleStatusUpdate = ResourceSpecificationUpdateVOTestExample.build()
				.atSchemaLocation(null)
				.targetResourceSchema(null)
				.lifecycleStatus("dead");
		ResourceSpecificationVO expectedLifecycleStatus = ResourceSpecificationVOTestExample.build()
				.validFor(null)
				.atSchemaLocation(null)
				.relatedParty(null)
				.targetResourceSchema(null)
				.lifecycleStatus("dead");
		testEntries.add(Arguments.of("The lifecycle state should have been updated.",
				lifecycleStatusUpdate, expectedLifecycleStatus));

		ResourceSpecificationUpdateVO descriptionUpdate = ResourceSpecificationUpdateVOTestExample.build()
				.atSchemaLocation(null)
				.targetResourceSchema(null)
				.description("new-description");
		ResourceSpecificationVO expectedDescriptionUpdate = ResourceSpecificationVOTestExample.build()
				.validFor(null)
				.atSchemaLocation(null)
				.relatedParty(null)
				.targetResourceSchema(null)
				.description("new-description");
		testEntries.add(Arguments.of("The description should have been updated.", descriptionUpdate,
				expectedDescriptionUpdate));

		ResourceSpecificationUpdateVO nameUpdate = ResourceSpecificationUpdateVOTestExample.build()
				.atSchemaLocation(null)
				.targetResourceSchema(null)
				.name("new-name");
		ResourceSpecificationVO expectedNameUpdate = ResourceSpecificationVOTestExample.build().validFor(null)
				.atSchemaLocation(null)
				.relatedParty(null)
				.targetResourceSchema(null)
				.name("new-name");
		testEntries.add(Arguments.of("The name should have been updated.", nameUpdate, expectedNameUpdate));

		ResourceSpecificationUpdateVO versionUpdate = ResourceSpecificationUpdateVOTestExample.build()
				.atSchemaLocation(null)
				.targetResourceSchema(null)
				.version("v0.0.2");
		ResourceSpecificationVO expectedVersionUpdate = ResourceSpecificationVOTestExample.build()
				.validFor(null)
				.atSchemaLocation(null)
				.relatedParty(null)
				.targetResourceSchema(null)
				.version("v0.0.2");
		testEntries.add(Arguments.of("The version should have been updated.", versionUpdate,
				expectedVersionUpdate));

		TimePeriodVO timePeriodVO = TimePeriodVOTestExample.build().endDateTime(Instant.now())
				.startDateTime(Instant.now());
		ResourceSpecificationUpdateVO validForUpdate = ResourceSpecificationUpdateVOTestExample.build()
				.atSchemaLocation(null)
				.targetResourceSchema(null)
				.validFor(timePeriodVO);
		ResourceSpecificationVO expectedValidForUpdate = ResourceSpecificationVOTestExample.build()
				.atSchemaLocation(null)
				.relatedParty(null)
				.targetResourceSchema(null)
				.validFor(timePeriodVO);
		testEntries.add(Arguments.of("The validFor should have been updated.", validForUpdate,
				expectedValidForUpdate));

		provideValidFeatureSpecs().map(ap ->
						Arguments.of(
								ap.message(),
								ResourceSpecificationUpdateVOTestExample.build()
										.atSchemaLocation(null)
										.targetResourceSchema(null)
										.lifecycleStatus("created")
										.featureSpecification(List.of(ap.value())),
								ResourceSpecificationVOTestExample.build()
										.atSchemaLocation(null)
										.relatedParty(null)
										.targetResourceSchema(null)
										.lifecycleStatus("created")
										.validFor(null)
										.featureSpecification(List.of(ap.value()))))
				.forEach(testEntries::add);

		return testEntries.stream();
	}

	/**
	 * Parameterized test for resource specification patching with invalid data.
	 *
	 * @param message         the test case description
	 * @param invalidUpdateVO the invalid update VO
	 * @throws Exception on test failure
	 */
	@ParameterizedTest
	@MethodSource("provideInvalidUpdates")
	public void patchResourceSpecification400(String message,
			ResourceSpecificationUpdateVO invalidUpdateVO)
			throws Exception {
		this.message = message;
		this.resourceSpecificationUpdateVO = invalidUpdateVO;
		patchResourceSpecification400();
	}

	@Override
	public void patchResourceSpecification400() throws Exception {
		//first create
		ResourceSpecificationCreateVO resourceSpecificationCreateVO =
				ResourceSpecificationCreateVOTestExample.build()
						.atSchemaLocation(null)
						.targetResourceSchema(null);

		HttpResponse<ResourceSpecificationVO> createResponse = callAndCatch(
				() -> resourceSpecificationApiTestClient.createResourceSpecification(null,
						resourceSpecificationCreateVO));
		assertEquals(HttpStatus.CREATED, createResponse.getStatus(),
				"The resource function should have been created first.");

		String resourceId = createResponse.body().getId();

		HttpResponse<ResourceSpecificationVO> updateResponse = callAndCatch(
				() -> resourceSpecificationApiTestClient.patchResourceSpecification(null, resourceId,
						resourceSpecificationUpdateVO));
		assertEquals(HttpStatus.BAD_REQUEST, updateResponse.getStatus(), message);

		Optional<ErrorDetails> optionalErrorDetails = updateResponse.getBody(ErrorDetails.class);
		assertTrue(optionalErrorDetails.isPresent(), "Some error details should be present.");
	}

	private static Stream<Arguments> provideInvalidUpdates() {
		List<Arguments> testEntries = new ArrayList<>();

		testEntries.add(Arguments.of("An update with an invalid relatedParty is not allowed.",
				ResourceSpecificationUpdateVOTestExample.build().atSchemaLocation(null)
						.relatedParty(
								List.of(RelatedPartyVOTestExample.build().atSchemaLocation(null)))));
		testEntries.add(Arguments.of("An update with a non existent relatedParty is not allowed.",
				ResourceSpecificationUpdateVOTestExample.build().atSchemaLocation(null)
						.relatedParty(List.of(RelatedPartyVOTestExample.build().atSchemaLocation(null)
								.id("urn:ngsi-ld:organization:non-existent")))));
		testEntries
				.addAll(provideInvalidFeatureSpecs()
						.map(ap -> Arguments.of(ap.message(),
								ResourceSpecificationUpdateVOTestExample.build().atSchemaLocation(null)
										.featureSpecification(List.of(ap.value()))))
						.toList());

		testEntries.add(Arguments.of(
				"A resource specification with an invalid resource spec id on the resource spec char rel should not be accepted.",
				ResourceSpecificationUpdateVOTestExample.build().atSchemaLocation(null)
						.resourceSpecCharacteristic(List.of(
								ResourceSpecificationCharacteristicVOTestExample.build()
										.atSchemaLocation(null)
										.resourceSpecCharRelationship(
												List.of(ResourceSpecificationCharacteristicRelationshipVOTestExample
														.build().atSchemaLocation(null)
														.resourceSpecificationId("invalid")))))));

		testEntries.add(Arguments.of(
				"A resource specification with a non-existent resource spec id on the resource spec char rel should not be accepted.",
				ResourceSpecificationUpdateVOTestExample.build().atSchemaLocation(null)
						.resourceSpecCharacteristic(List.of(
								ResourceSpecificationCharacteristicVOTestExample.build()
										.atSchemaLocation(null)
										.resourceSpecCharRelationship(
												List.of(ResourceSpecificationCharacteristicRelationshipVOTestExample
														.build().atSchemaLocation(null)
														.resourceSpecificationId(
																"urn:ngsi-ld:resource-specification:non-existent")))))));

		return testEntries.stream();
	}

	@Disabled("Security is handled externally, thus 401 and 403 cannot happen.")
	@Test
	@Override
	public void patchResourceSpecification401() throws Exception {
	}

	@Disabled("Security is handled externally, thus 401 and 403 cannot happen.")
	@Test
	@Override
	public void patchResourceSpecification403() throws Exception {
	}

	@Test
	@Override
	public void patchResourceSpecification404() throws Exception {
		ResourceSpecificationUpdateVO resourceSpecificationUpdateVO =
				ResourceSpecificationUpdateVOTestExample.build()
						.atSchemaLocation(null)
						.targetResourceSchema(null);
		assertEquals(
				HttpStatus.NOT_FOUND,
				callAndCatch(() -> resourceSpecificationApiTestClient.patchResourceSpecification(null,
						"urn:ngsi-ld:resource-specification:not-existent",
						resourceSpecificationUpdateVO)).getStatus(),
				"Non existent resource specification should not be updated.");
	}

	@Disabled("Prohibited by the framework.")
	@Test
	@Override
	public void patchResourceSpecification405() throws Exception {
	}

	@Override
	public void patchResourceSpecification409() throws Exception {
		// TODO: can this happen?
	}

	@Override
	public void patchResourceSpecification500() throws Exception {
	}

	/**
	 * Parameterized test for successful resource specification retrieval.
	 *
	 * @param message                        the test case description
	 * @param fields                         the fields parameter
	 * @param expectedResourceSpecification  the expected result
	 * @throws Exception on test failure
	 */
	@ParameterizedTest
	@MethodSource("provideFieldParameters")
	public void retrieveResourceSpecification200(String message, String fields,
			ResourceSpecificationVO expectedResourceSpecification) throws Exception {
		this.fieldsParameter = fields;
		this.message = message;
		this.expectedResourceSpecification = expectedResourceSpecification;
		retrieveResourceSpecification200();
	}

	@Override
	public void retrieveResourceSpecification200() throws Exception {

		ResourceSpecificationCreateVO resourceSpecificationCreateVO =
				ResourceSpecificationCreateVOTestExample.build()
						.atSchemaLocation(null)
						.targetResourceSchema(null);
		HttpResponse<ResourceSpecificationVO> createResponse = callAndCatch(
				() -> resourceSpecificationApiTestClient.createResourceSpecification(null,
						resourceSpecificationCreateVO));
		assertEquals(HttpStatus.CREATED, createResponse.getStatus(), message);
		String id = createResponse.body().getId();

		expectedResourceSpecification
				.id(id)
				.href(URI.create(id));

		//then retrieve
		HttpResponse<ResourceSpecificationVO> retrievedResourceSpec = callAndCatch(
				() -> resourceSpecificationApiTestClient.retrieveResourceSpecification(null, id,
						fieldsParameter));
		assertEquals(HttpStatus.OK, retrievedResourceSpec.getStatus(), message);
		assertEquals(expectedResourceSpecification, retrievedResourceSpec.body(), message);
	}

	private static Stream<Arguments> provideFieldParameters() {
		ResourceSpecificationVO fullExpected = ResourceSpecificationVOTestExample.build()
				.atSchemaLocation(null)
				.validFor(null)
				.relatedParty(null)
				.targetResourceSchema(null)
				.resourceSpecRelationship(null);

		// When a fields parameter is provided, only the requested fields plus the
		// mandatory fields (id, href) are returned by the FieldCleaningSerializer.
		ResourceSpecificationVO versionOnly = new ResourceSpecificationVO()
				.version("string");

		ResourceSpecificationVO mandatoryOnly = new ResourceSpecificationVO();

		ResourceSpecificationVO multipleFields = new ResourceSpecificationVO()
				.version("string")
				.description("string")
				.lifecycleStatus("string");

		return Stream.of(
				Arguments.of("Without a fields parameter everything should be returned.",
						null, fullExpected),
				Arguments.of("With a single field, only that field plus id/href should be returned.",
						"version", versionOnly),
				Arguments.of("With a non-existent field, only mandatory id/href should be returned.",
						"nothingToSeeHere", mandatoryOnly),
				Arguments.of("With multiple fields, only those fields plus id/href should be returned.",
						"version,lastUpdate,lifecycleStatus,description", multipleFields));
	}

	@Disabled("400 cannot happen, only 404")
	@Test
	@Override
	public void retrieveResourceSpecification400() throws Exception {
	}

	@Disabled("Security is handled externally, thus 401 and 403 cannot happen.")
	@Test
	@Override
	public void retrieveResourceSpecification401() throws Exception {
	}

	@Disabled("Security is handled externally, thus 401 and 403 cannot happen.")
	@Test
	@Override
	public void retrieveResourceSpecification403() throws Exception {
	}

	@Test
	@Override
	public void retrieveResourceSpecification404() throws Exception {
		HttpResponse<ResourceSpecificationVO> response = callAndCatch(
				() -> resourceSpecificationApiTestClient.retrieveResourceSpecification(null,
						"urn:ngsi-ld:resource-function:non-existent", null));
		assertEquals(HttpStatus.NOT_FOUND, response.getStatus(),
				"No such resource-specification should exist.");

		Optional<ErrorDetails> optionalErrorDetails = response.getBody(ErrorDetails.class);
		assertTrue(optionalErrorDetails.isPresent(), "Error details should have been provided.");
	}

	@Disabled("Prohibited by the framework.")
	@Test
	@Override
	public void retrieveResourceSpecification405() throws Exception {
	}

	@Disabled("Conflict not possible on retrieval")
	@Test
	@Override
	public void retrieveResourceSpecification409() throws Exception {
	}

	@Override
	public void retrieveResourceSpecification500() throws Exception {
	}

	@Override
	protected String getEntityType() {
		return ResourceTypeRegistry.ALL_SPEC_TYPES;
	}

	// --- Sub-type specific integration tests ---

	/**
	 * Helper to build a ResourceSpecificationCreateVO that represents a sub-type.
	 * Known sub-type properties are recognized by the
	 * {@link org.fiware.tmforum.common.mapping.ValidatingDeserializer} via the registered
	 * {@link org.fiware.tmforum.common.mapping.SubTypePropertyProvider}, so no explicit
	 * {@code @schemaLocation} is needed.
	 */
	private static ResourceSpecificationCreateVO buildSubTypeSpecCreate(String atType,
			Map<String, Object> extraFields) {
		ResourceSpecificationCreateVO createVO = ResourceSpecificationCreateVOTestExample.build()
				.targetResourceSchema(null)
				.lifecycleStatus("created")
				.atType(atType);
		extraFields.forEach(createVO::setUnknownProperties);
		return createVO;
	}

	/**
	 * Parameterized test for creating sub-type ResourceSpecification entities.
	 */
	@ParameterizedTest
	@MethodSource("provideSubTypeSpecs")
	public void createSubTypeResourceSpecification201(String message,
			ResourceSpecificationCreateVO createVO) throws Exception {
		Instant currentTimeInstant = Instant.ofEpochSecond(10000);
		when(clock.instant()).thenReturn(currentTimeInstant);

		HttpResponse<ResourceSpecificationVO> response = callAndCatch(
				() -> resourceSpecificationApiTestClient.createResourceSpecification(null, createVO));
		assertEquals(HttpStatus.CREATED, response.getStatus(),
				message + " - Error: " + response.getBody(ErrorDetails.class).orElse(null));
		assertEquals(message.contains("Logical") ? "LogicalResourceSpecification" :
						message.contains("SoftwareResource") ? "SoftwareResourceSpecification" :
						message.contains("APISpec") ? "APISpecification" :
						message.contains("SoftwareSpec") ? "SoftwareSpecification" :
						message.contains("Hosting") ? "HostingPlatformRequirementSpecification" :
						message.contains("PhysicalResource") ? "PhysicalResourceSpecification" :
						"SoftwareSupportPackageSpecification",
				response.body().getAtType(), message + " - @type mismatch");
	}

	private static Stream<Arguments> provideSubTypeSpecs() {
		return Stream.of(
				Arguments.of("LogicalResourceSpecification should be created.",
						buildSubTypeSpecCreate("LogicalResourceSpecification", Map.of())),
				Arguments.of("SoftwareResourceSpecification should be created.",
						buildSubTypeSpecCreate("SoftwareResourceSpecification", Map.of(
								"buildNumber", "build-42",
								"majorVersion", "1",
								"minorVersion", "0",
								"releaseStatus", "generalDeployment"))),
				Arguments.of("APISpecification should be created.",
						buildSubTypeSpecCreate("APISpecification", Map.of(
								"apiProtocolType", "REST",
								"authenticationType", "Basic",
								"buildNumber", "api-1.0"))),
				Arguments.of("SoftwareSpecification should be created.",
						buildSubTypeSpecCreate("SoftwareSpecification", Map.of(
								"buildNumber", "sw-1.0",
								"numUsersMax", 10,
								"numberProcessActiveTotal", 4))),
				Arguments.of("HostingPlatformRequirementSpecification should be created.",
						buildSubTypeSpecCreate("HostingPlatformRequirementSpecification", Map.of(
								"isVirtualizable", true))),
				Arguments.of("PhysicalResourceSpecification should be created.",
						buildSubTypeSpecCreate("PhysicalResourceSpecification", Map.of())),
				Arguments.of("SoftwareSupportPackageSpecification should be created.",
						buildSubTypeSpecCreate("SoftwareSupportPackageSpecification", Map.of()))
		);
	}

	/**
	 * Test retrieving a sub-type specification preserves sub-type-specific fields.
	 */
	@ParameterizedTest
	@MethodSource("provideSubTypeSpecRetrievalCases")
	public void retrieveSubTypeResourceSpecification200(String message, String atType,
			Map<String, Object> extraFields) throws Exception {
		Instant currentTimeInstant = Instant.ofEpochSecond(10000);
		when(clock.instant()).thenReturn(currentTimeInstant);

		ResourceSpecificationCreateVO createVO = buildSubTypeSpecCreate(atType, extraFields);
		HttpResponse<ResourceSpecificationVO> createResponse = callAndCatch(
				() -> resourceSpecificationApiTestClient.createResourceSpecification(null, createVO));
		assertEquals(HttpStatus.CREATED, createResponse.getStatus(), message + " - creation failed");
		String id = createResponse.body().getId();

		HttpResponse<ResourceSpecificationVO> retrieveResponse = callAndCatch(
				() -> resourceSpecificationApiTestClient.retrieveResourceSpecification(null, id, null));
		assertEquals(HttpStatus.OK, retrieveResponse.getStatus(), message + " - retrieve failed");

		ResourceSpecificationVO retrieved = retrieveResponse.body();
		assertEquals(atType, retrieved.getAtType(), message + " - @type mismatch");
		for (Map.Entry<String, Object> entry : extraFields.entrySet()) {
			assertEquals(entry.getValue(), retrieved.getUnknownProperties().get(entry.getKey()),
					String.format("%s - field '%s' mismatch", message, entry.getKey()));
		}
	}

	private static Stream<Arguments> provideSubTypeSpecRetrievalCases() {
		return Stream.of(
				Arguments.of("SoftwareResourceSpecification retrieve",
						"SoftwareResourceSpecification",
						Map.of("buildNumber", "build-99", "releaseStatus", "beta")),
				Arguments.of("APISpecification retrieve",
						"APISpecification",
						Map.of("apiProtocolType", "gRPC", "authenticationType", "OAuth2")),
				Arguments.of("SoftwareSpecification retrieve",
						"SoftwareSpecification",
						Map.of("numUsersMax", 50)),
				Arguments.of("LogicalResourceSpecification retrieve",
						"LogicalResourceSpecification",
						Map.of()),
				Arguments.of("PhysicalResourceSpecification retrieve",
						"PhysicalResourceSpecification",
						Map.of())
		);
	}

	/**
	 * Test listing specifications returns sub-type specs alongside base specs.
	 */
	@Test
	public void listResourceSpecificationIncludesSubTypes() throws Exception {
		Instant currentTimeInstant = Instant.ofEpochSecond(10000);
		when(clock.instant()).thenReturn(currentTimeInstant);

		// Create a base spec
		ResourceSpecificationCreateVO baseCreate = ResourceSpecificationCreateVOTestExample.build()
				.atSchemaLocation(null).targetResourceSchema(null).lifecycleStatus("created");
		HttpResponse<ResourceSpecificationVO> baseResponse = callAndCatch(
				() -> resourceSpecificationApiTestClient.createResourceSpecification(null, baseCreate));
		assertEquals(HttpStatus.CREATED, baseResponse.getStatus());
		String baseId = baseResponse.body().getId();

		// Create a SoftwareResourceSpecification sub-type
		ResourceSpecificationCreateVO swCreate = buildSubTypeSpecCreate(
				"SoftwareResourceSpecification", Map.of("buildNumber", "list-build"));
		HttpResponse<ResourceSpecificationVO> swResponse = callAndCatch(
				() -> resourceSpecificationApiTestClient.createResourceSpecification(null, swCreate));
		assertEquals(HttpStatus.CREATED, swResponse.getStatus());
		String swId = swResponse.body().getId();

		// List all specs
		HttpResponse<List<ResourceSpecificationVO>> listResponse = callAndCatch(
				() -> resourceSpecificationApiTestClient.listResourceSpecification(null, null, null, null));
		assertEquals(HttpStatus.OK, listResponse.getStatus());

		List<String> returnedIds = listResponse.body().stream()
				.map(ResourceSpecificationVO::getId)
				.toList();
		assertTrue(returnedIds.contains(baseId), "Base spec should be in the list.");
		assertTrue(returnedIds.contains(swId), "SoftwareResourceSpecification should be in the list.");
	}

	/**
	 * Test deleting a sub-type specification.
	 */
	@Test
	public void deleteSubTypeResourceSpecification204() throws Exception {
		Instant currentTimeInstant = Instant.ofEpochSecond(10000);
		when(clock.instant()).thenReturn(currentTimeInstant);

		ResourceSpecificationCreateVO createVO = buildSubTypeSpecCreate(
				"APISpecification", Map.of("apiProtocolType", "REST"));
		HttpResponse<ResourceSpecificationVO> createResponse = callAndCatch(
				() -> resourceSpecificationApiTestClient.createResourceSpecification(null, createVO));
		assertEquals(HttpStatus.CREATED, createResponse.getStatus());
		String id = createResponse.body().getId();

		assertEquals(HttpStatus.NO_CONTENT,
				callAndCatch(() -> resourceSpecificationApiTestClient
						.deleteResourceSpecification(null, id)).getStatus(),
				"The sub-type spec should have been deleted.");

		assertEquals(HttpStatus.NOT_FOUND,
				callAndCatch(() -> resourceSpecificationApiTestClient
						.retrieveResourceSpecification(null, id, null)).getStatus(),
				"The sub-type spec should not exist anymore.");
	}
}
