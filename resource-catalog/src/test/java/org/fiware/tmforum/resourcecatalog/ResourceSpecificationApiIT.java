package org.fiware.tmforum.resourcecatalog;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import io.micronaut.test.annotation.MockBean;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import org.fiware.ngsi.api.EntitiesApiClient;
import org.fiware.resourcecatalog.api.ResourceSpecificationApiTestClient;
import org.fiware.resourcecatalog.api.ResourceSpecificationApiTestSpec;
import org.fiware.resourcecatalog.model.ConstraintRefVOTestExample;
import org.fiware.resourcecatalog.model.FeatureSpecificationCharacteristicRelationshipVOTestExample;
import org.fiware.resourcecatalog.model.FeatureSpecificationCharacteristicVOTestExample;
import org.fiware.resourcecatalog.model.FeatureSpecificationRelationshipVOTestExample;
import org.fiware.resourcecatalog.model.FeatureSpecificationVO;
import org.fiware.resourcecatalog.model.FeatureSpecificationVOTestExample;
import org.fiware.resourcecatalog.model.RelatedPartyVOTestExample;
import org.fiware.resourcecatalog.model.ResourceSpecificationCharacteristicRelationshipVOTestExample;
import org.fiware.resourcecatalog.model.ResourceSpecificationCharacteristicVOTestExample;
import org.fiware.resourcecatalog.model.ResourceSpecificationCreateVO;
import org.fiware.resourcecatalog.model.ResourceSpecificationCreateVOTestExample;
import org.fiware.resourcecatalog.model.ResourceSpecificationUpdateVO;
import org.fiware.resourcecatalog.model.ResourceSpecificationUpdateVOTestExample;
import org.fiware.resourcecatalog.model.ResourceSpecificationVO;
import org.fiware.resourcecatalog.model.ResourceSpecificationVOTestExample;
import org.fiware.resourcecatalog.model.TimePeriodVO;
import org.fiware.resourcecatalog.model.TimePeriodVOTestExample;
import org.fiware.tmforum.common.notification.EventHandler;
import org.fiware.tmforum.common.configuration.GeneralProperties;
import org.fiware.tmforum.common.exception.ErrorDetails;
import org.fiware.tmforum.common.test.AbstractApiIT;
import org.fiware.tmforum.common.test.ArgumentPair;
import org.fiware.tmforum.resource.ResourceSpecification;
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
import java.net.URI;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@MicronautTest(packages = { "org.fiware.tmforum.resourcecatalog" })
public class ResourceSpecificationApiIT extends AbstractApiIT implements ResourceSpecificationApiTestSpec {

	public final ResourceSpecificationApiTestClient resourceSpecificationApiTestClient;

	private String message;
	private ResourceSpecificationCreateVO resourceSpecificationCreateVO;
	private ResourceSpecificationUpdateVO resourceSpecificationUpdateVO;
	private ResourceSpecificationVO expectedResourceSpecification;
	private String fieldsParameter;

	private Clock clock = mock(Clock.class);

	public ResourceSpecificationApiIT(ResourceSpecificationApiTestClient resourceSpecificationApiTestClient,
			EntitiesApiClient entitiesApiClient, ObjectMapper objectMapper, GeneralProperties generalProperties) {
		super(entitiesApiClient, objectMapper, generalProperties);
		this.resourceSpecificationApiTestClient = resourceSpecificationApiTestClient;
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
				() -> resourceSpecificationApiTestClient.createResourceSpecification(resourceSpecificationCreateVO));
		assertEquals(HttpStatus.CREATED, resourceSpecificationVOHttpResponse.getStatus(), message);
		String rsId = resourceSpecificationVOHttpResponse.body().getId();
		expectedResourceSpecification.id(rsId).href(URI.create(rsId)).lastUpdate(currentTimeInstant).resourceSpecRelationship(null);

		assertEquals(expectedResourceSpecification, resourceSpecificationVOHttpResponse.body(), message);
	}

	private static Stream<Arguments> provideValidResourceSpecifications() {
		List<Arguments> testEntries = new ArrayList<>();

		ResourceSpecificationCreateVO emptyCreate = ResourceSpecificationCreateVOTestExample.build()
				.lifecycleStatus("created");
		ResourceSpecificationVO expectedEmpty = ResourceSpecificationVOTestExample.build().lifecycleStatus("created");
		testEntries.add(
				Arguments.of("An empty resource specification should have been created.", emptyCreate, expectedEmpty));

		TimePeriodVO timePeriodVO = TimePeriodVOTestExample.build().endDateTime(Instant.now())
				.startDateTime(Instant.now());
		ResourceSpecificationCreateVO createValidFor = ResourceSpecificationCreateVOTestExample.build()
				.validFor(timePeriodVO).lifecycleStatus("created");
		ResourceSpecificationVO expectedValidFor = ResourceSpecificationVOTestExample.build().validFor(timePeriodVO)
				.lifecycleStatus("created");
		testEntries.add(
				Arguments.of("An resource specification with a validFor should have been created.", createValidFor,
						expectedValidFor));

		provideValidFeatureSpecs().map(ap ->
						Arguments.of(
								ap.message(),
								ResourceSpecificationCreateVOTestExample.build().lifecycleStatus("created")
										.featureSpecification(List.of(ap.value())),
								ResourceSpecificationVOTestExample.build().lifecycleStatus("created")
										.featureSpecification(List.of(ap.value()))))
				.forEach(testEntries::add);

		return testEntries.stream();
	}

	private static Stream<ArgumentPair<FeatureSpecificationVO>> provideValidFeatureSpecs() {
		List<ArgumentPair<FeatureSpecificationVO>> validFeatureSpecs = new ArrayList<>();

		validFeatureSpecs.add(new ArgumentPair<>("Feature specification with feature spec char rel should be created.",
				FeatureSpecificationVOTestExample.build()
						.constraint(null)
						.id(null)
						.validFor(null)
						.featureSpecRelationship(null)
						.featureSpecCharacteristic(List.of(
								FeatureSpecificationCharacteristicVOTestExample.build()
										.id(null)
										.validFor(null)
										.featureSpecCharacteristicValue(null)
										.featureSpecCharRelationship(
												List.of(FeatureSpecificationCharacteristicRelationshipVOTestExample.build()
														.id(null)
														.validFor(null)
														.resourceSpecificationId(null)))
						))));
		validFeatureSpecs.add(new ArgumentPair<>("Feature specification with feature spec rel should be created.",
				FeatureSpecificationVOTestExample.build()
						.constraint(null)
						.id(null)
						.validFor(null)
						.featureSpecCharacteristic(null)
						.featureSpecRelationship(List.of(
								FeatureSpecificationRelationshipVOTestExample.build()
										.id(null)
										.validFor(null)
										//.resourceSpecificationId(null)
						))));

		return validFeatureSpecs.stream();
	}

	@ParameterizedTest
	@MethodSource("provideInvalidResourceSpecifications")
	public void createResourceSpecification400(String message, ResourceSpecificationCreateVO invalidCreateVO)
			throws Exception {
		this.message = message;
		this.resourceSpecificationCreateVO = invalidCreateVO;
		createResourceSpecification400();
	}

	@Override
	public void createResourceSpecification400() throws Exception {
		HttpResponse<ResourceSpecificationVO> creationResponse = callAndCatch(
				() -> resourceSpecificationApiTestClient.createResourceSpecification(resourceSpecificationCreateVO));
		assertEquals(HttpStatus.BAD_REQUEST, creationResponse.getStatus(), message);
		Optional<ErrorDetails> optionalErrorDetails = creationResponse.getBody(ErrorDetails.class);
		assertTrue(optionalErrorDetails.isPresent(), "Error details should be provided.");
	}

	private static Stream<Arguments> provideInvalidResourceSpecifications() {
		List<Arguments> testEntries = new ArrayList<>();

		testEntries.add(Arguments.of("A resource specification with an invalid relatedParty should not be created.",
				ResourceSpecificationCreateVOTestExample.build()
						.relatedParty(List.of(RelatedPartyVOTestExample.build()))));
		testEntries.add(Arguments.of("A resource specification with a non-existent relatedParty should not be created.",
				ResourceSpecificationCreateVOTestExample.build().relatedParty(
						List.of(RelatedPartyVOTestExample.build().id("urn:ngsi-ld:organization:non-existent")))));

		testEntries.add(Arguments.of(
				"A resource specification with an invalid resource spec id on the resource spec char rel should not be created.",
				ResourceSpecificationCreateVOTestExample.build()
						.resourceSpecCharacteristic(List.of(ResourceSpecificationCharacteristicVOTestExample.build()
								.resourceSpecCharRelationship(
										List.of(ResourceSpecificationCharacteristicRelationshipVOTestExample.build()
												.resourceSpecificationId("invalid")))))));

		testEntries.add(Arguments.of(
				"A resource specification with an non-existent resource spec id on the resource spec char rel should not be created.",
				ResourceSpecificationCreateVOTestExample.build()
						.resourceSpecCharacteristic(List.of(ResourceSpecificationCharacteristicVOTestExample.build()
								.resourceSpecCharRelationship(
										List.of(ResourceSpecificationCharacteristicRelationshipVOTestExample.build()
												.resourceSpecificationId(
														"urn:ngsi-ld:resource-specification:non-existent")))))));
		testEntries
				.addAll(provideInvalidFeatureSpecs()
						.map(ap -> Arguments.of(ap.message(), ResourceSpecificationCreateVOTestExample.build()
								.featureSpecification(List.of(ap.value()))))
						.toList());

		return testEntries.stream();
	}

	private static Stream<ArgumentPair<FeatureSpecificationVO>> provideInvalidFeatureSpecs() {
		List<ArgumentPair<FeatureSpecificationVO>> invalidFeatureSpecs = new ArrayList<>();

		invalidFeatureSpecs.add(
				new ArgumentPair<>("Feature specification with invalid resource spec id on spec char rel should fail.",
						FeatureSpecificationVOTestExample.build()
								.featureSpecCharacteristic(List.of(
										FeatureSpecificationCharacteristicVOTestExample.build()
												.featureSpecCharRelationship(
														List.of(FeatureSpecificationCharacteristicRelationshipVOTestExample.build()
																.featureId(null)
																.resourceSpecificationId("invalid")))
								))));

		invalidFeatureSpecs.add(new ArgumentPair<>(
				"Feature specification with non-existent resource spec id on spec char rel should fail.",
				FeatureSpecificationVOTestExample.build()
						.featureSpecCharacteristic(List.of(
								FeatureSpecificationCharacteristicVOTestExample.build()
										.featureSpecCharRelationship(
												List.of(FeatureSpecificationCharacteristicRelationshipVOTestExample.build()
														.resourceSpecificationId(
																"urn:ngsi-ld:resource-specification:non-existent")
														.featureId(null)))
						))));

		// constraint
		invalidFeatureSpecs.add(new ArgumentPair<>("Feature specification with invalid constraint ref should fail.",
				FeatureSpecificationVOTestExample.build()
						.constraint(List.of(ConstraintRefVOTestExample.build().id("invalid")))));

		invalidFeatureSpecs.add(
				new ArgumentPair<>("Feature specification with non-existent constraint ref should fail.",
						FeatureSpecificationVOTestExample.build()
								.constraint(List.of(ConstraintRefVOTestExample.build()
										.id("urn:ngsi-ld:constraint:non-existent")))));

		invalidFeatureSpecs.add(
				new ArgumentPair<>("Feature specification with invalid resource id on spec rel should fail.",
						FeatureSpecificationVOTestExample.build()
								.id(null)
								.featureSpecRelationship(List.of(FeatureSpecificationRelationshipVOTestExample.build()
										.featureId(null)
										//.resourceSpecificationId("invalid")
								))));
		invalidFeatureSpecs.add(
				new ArgumentPair<>("Feature specification with non-existent resource id on spec rel should fail.",
						FeatureSpecificationVOTestExample.build()
								.id(null)
								.featureSpecRelationship(List.of(FeatureSpecificationRelationshipVOTestExample.build()
										//.resourceSpecificationId("urn:ngsi-ld:resource-specification:non-existent")
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
		ResourceSpecificationCreateVO emptyCreate = ResourceSpecificationCreateVOTestExample.build();

		HttpResponse<ResourceSpecificationVO> createResponse = resourceSpecificationApiTestClient.createResourceSpecification(
				emptyCreate);
		assertEquals(HttpStatus.CREATED, createResponse.getStatus(),
				"The resource specification should have been created first.");

		String rfId = createResponse.body().getId();

		assertEquals(HttpStatus.NO_CONTENT,
				callAndCatch(() -> resourceSpecificationApiTestClient.deleteResourceSpecification(rfId)).getStatus(),
				"The resource specification should have been deleted.");

		assertEquals(HttpStatus.NOT_FOUND,
				callAndCatch(
						() -> resourceSpecificationApiTestClient.retrieveResourceSpecification(rfId, null)).status(),
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
				() -> resourceSpecificationApiTestClient.deleteResourceSpecification(
						"urn:ngsi-ld:resource-specification:no-pop"));
		assertEquals(HttpStatus.NOT_FOUND,
				notFoundResponse.getStatus(),
				"No such resource-specification should exist.");

		Optional<ErrorDetails> optionalErrorDetails = notFoundResponse.getBody(ErrorDetails.class);
		assertTrue(optionalErrorDetails.isPresent(), "Error details should be provided.");

		notFoundResponse = callAndCatch(
				() -> resourceSpecificationApiTestClient.deleteResourceSpecification("invalid-id"));
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
			ResourceSpecificationCreateVO resourceSpecificationCreateVO = ResourceSpecificationCreateVOTestExample.build();
			String id = resourceSpecificationApiTestClient.createResourceSpecification(resourceSpecificationCreateVO)
					.body().getId();
			ResourceSpecificationVO resourceSpecificationVO = ResourceSpecificationVOTestExample.build();
			resourceSpecificationVO
					.id(id)
					.href(URI.create(id))
					.relatedParty(null);
			expectedResourceSpecifications.add(resourceSpecificationVO);
		}

		HttpResponse<List<ResourceSpecificationVO>> resourceSpecificationResponse = callAndCatch(
				() -> resourceSpecificationApiTestClient.listResourceSpecification(null, null, null));

		assertEquals(HttpStatus.OK, resourceSpecificationResponse.getStatus(), "The list should be accessible.");
		assertEquals(expectedResourceSpecifications.size(), resourceSpecificationResponse.getBody().get().size(),
				"All resourceSpecifications should have been returned.");
		List<ResourceSpecificationVO> retrievedResourceSpecifications = resourceSpecificationResponse.getBody().get();

		Map<String, ResourceSpecificationVO> retrievedMap = retrievedResourceSpecifications.stream()
				.collect(Collectors.toMap(resourceSpecification -> resourceSpecification.getId(),
						resourceSpecification -> resourceSpecification));

		expectedResourceSpecifications.stream()
				.forEach(
						expectedResourceSpecification -> assertTrue(
								retrievedMap.containsKey(expectedResourceSpecification.getId()),
								String.format("All created resourceSpecifications should be returned - Missing: %s.",
										expectedResourceSpecification,
										retrievedResourceSpecifications)));
		expectedResourceSpecifications.stream().forEach(
				expectedResourceSpecification -> assertEquals(expectedResourceSpecification,
						retrievedMap.get(expectedResourceSpecification.getId()),
						"The correct resourceSpecifications should be retrieved."));

		// get with pagination
		Integer limit = 5;
		HttpResponse<List<ResourceSpecificationVO>> firstPartResponse = callAndCatch(
				() -> resourceSpecificationApiTestClient.listResourceSpecification(null, 0, limit));
		assertEquals(limit, firstPartResponse.body().size(),
				"Only the requested number of entries should be returend.");
		HttpResponse<List<ResourceSpecificationVO>> secondPartResponse = callAndCatch(
				() -> resourceSpecificationApiTestClient.listResourceSpecification(null, 0 + limit, limit));
		assertEquals(limit, secondPartResponse.body().size(),
				"Only the requested number of entries should be returend.");

		retrievedResourceSpecifications.clear();
		retrievedResourceSpecifications.addAll(firstPartResponse.body());
		retrievedResourceSpecifications.addAll(secondPartResponse.body());
		expectedResourceSpecifications.stream()
				.forEach(
						expectedResourceSpecification -> assertTrue(
								retrievedMap.containsKey(expectedResourceSpecification.getId()),
								String.format("All created resourceSpecifications should be returned - Missing: %s.",
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
				() -> resourceSpecificationApiTestClient.listResourceSpecification(null, -1, null));
		assertEquals(HttpStatus.BAD_REQUEST,
				badRequestResponse.getStatus(),
				"Negative offsets are impossible.");

		Optional<ErrorDetails> optionalErrorDetails = badRequestResponse.getBody(ErrorDetails.class);
		assertTrue(optionalErrorDetails.isPresent(), "Error details should be provided.");

		badRequestResponse = callAndCatch(
				() -> resourceSpecificationApiTestClient.listResourceSpecification(null, null, -1));
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
		ResourceSpecificationCreateVO resourceSpecificationCreateVO = ResourceSpecificationCreateVOTestExample.build();

		HttpResponse<ResourceSpecificationVO> createResponse = callAndCatch(
				() -> resourceSpecificationApiTestClient.createResourceSpecification(resourceSpecificationCreateVO));
		assertEquals(HttpStatus.CREATED, createResponse.getStatus(),
				"The resource function should have been created first.");

		String resourceId = createResponse.body().getId();

		HttpResponse<ResourceSpecificationVO> updateResponse = callAndCatch(
				() -> resourceSpecificationApiTestClient.patchResourceSpecification(resourceId,
						resourceSpecificationUpdateVO));
		assertEquals(HttpStatus.OK, updateResponse.getStatus(), message);

		ResourceSpecificationVO updatedResourceSpecification = updateResponse.body();
		expectedResourceSpecification.href(URI.create(resourceId)).id(resourceId).relatedParty(null).resourceSpecRelationship(null)
				.lastUpdate(currentTimeInstant);

		assertEquals(expectedResourceSpecification, updatedResourceSpecification, message);
	}

	private static Stream<Arguments> provideResourceSpecificationUpdates() {
		List<Arguments> testEntries = new ArrayList<>();

		ResourceSpecificationUpdateVO lifecycleStatusUpdate = ResourceSpecificationUpdateVOTestExample.build()
				.lifecycleStatus("dead");
		ResourceSpecificationVO expectedLifecycleStatus = ResourceSpecificationVOTestExample.build()
				.lifecycleStatus("dead");
		testEntries.add(Arguments.of("The lifecycle state should have been updated.", lifecycleStatusUpdate,
				expectedLifecycleStatus));

		ResourceSpecificationUpdateVO descriptionUpdate = ResourceSpecificationUpdateVOTestExample.build()
				.description("new-description");
		ResourceSpecificationVO expectedDescriptionUpdate = ResourceSpecificationVOTestExample.build()
				.description("new-description");
		testEntries.add(Arguments.of("The description should have been updated.", descriptionUpdate,
				expectedDescriptionUpdate));

		ResourceSpecificationUpdateVO nameUpdate = ResourceSpecificationUpdateVOTestExample.build()
				.name("new-name");
		ResourceSpecificationVO expectedNameUpdate = ResourceSpecificationVOTestExample.build()
				.name("new-name");
		testEntries.add(Arguments.of("The name should have been updated.", nameUpdate, expectedNameUpdate));

		ResourceSpecificationUpdateVO versionUpdate = ResourceSpecificationUpdateVOTestExample.build()
				.version("v0.0.2");
		ResourceSpecificationVO expectedVersionUpdate = ResourceSpecificationVOTestExample.build()
				.version("v0.0.2");
		testEntries.add(Arguments.of("The version should have been updated.", versionUpdate, expectedVersionUpdate));

		TimePeriodVO timePeriodVO = TimePeriodVOTestExample.build().endDateTime(Instant.now())
				.startDateTime(Instant.now());
		ResourceSpecificationUpdateVO validForUpdate = ResourceSpecificationUpdateVOTestExample.build()
				.validFor(timePeriodVO);
		ResourceSpecificationVO expectedValidForUpdate = ResourceSpecificationVOTestExample.build()
				.validFor(timePeriodVO);
		testEntries.add(Arguments.of("The validFor should have been updated.", validForUpdate, expectedValidForUpdate));

		provideValidFeatureSpecs().map(ap ->
						Arguments.of(
								ap.message(),
								ResourceSpecificationUpdateVOTestExample.build().lifecycleStatus("created")
										.featureSpecification(List.of(ap.value())),
								ResourceSpecificationVOTestExample.build().lifecycleStatus("created")
										.featureSpecification(List.of(ap.value()))))
				.forEach(testEntries::add);

		return testEntries.stream();
	}

	@ParameterizedTest
	@MethodSource("provideInvalidUpdates")
	public void patchResourceSpecification400(String message, ResourceSpecificationUpdateVO invalidUpdateVO)
			throws Exception {
		this.message = message;
		this.resourceSpecificationUpdateVO = invalidUpdateVO;
		patchResourceSpecification400();
	}

	@Override
	public void patchResourceSpecification400() throws Exception {
		//first create
		ResourceSpecificationCreateVO resourceSpecificationCreateVO = ResourceSpecificationCreateVOTestExample.build();

		HttpResponse<ResourceSpecificationVO> createResponse = callAndCatch(
				() -> resourceSpecificationApiTestClient.createResourceSpecification(resourceSpecificationCreateVO));
		assertEquals(HttpStatus.CREATED, createResponse.getStatus(),
				"The resource function should have been created first.");

		String resourceId = createResponse.body().getId();

		HttpResponse<ResourceSpecificationVO> updateResponse = callAndCatch(
				() -> resourceSpecificationApiTestClient.patchResourceSpecification(resourceId,
						resourceSpecificationUpdateVO));
		assertEquals(HttpStatus.BAD_REQUEST, updateResponse.getStatus(), message);

		Optional<ErrorDetails> optionalErrorDetails = updateResponse.getBody(ErrorDetails.class);
		assertTrue(optionalErrorDetails.isPresent(), "Some error details should be present.");
	}

	private static Stream<Arguments> provideInvalidUpdates() {
		List<Arguments> testEntries = new ArrayList<>();

		testEntries.add(Arguments.of("An update with an invalid relatedParty is not allowed.",
				ResourceSpecificationUpdateVOTestExample.build()
						.relatedParty(List.of(RelatedPartyVOTestExample.build()))));
		testEntries.add(Arguments.of("An update with an non existent relatedParty is not allowed.",
				ResourceSpecificationUpdateVOTestExample.build()
						.relatedParty(List.of(RelatedPartyVOTestExample.build()
								.id("urn:ngsi-ld:organization:non-existent")))));
		testEntries
				.addAll(provideInvalidFeatureSpecs()
						.map(ap -> Arguments.of(ap.message(), ResourceSpecificationUpdateVOTestExample.build()
								.featureSpecification(List.of(ap.value()))))
						.toList());

		testEntries.add(Arguments.of(
				"A resource specification with an invalid resource spec id on the resource spec char rel should not be accepted.",
				ResourceSpecificationUpdateVOTestExample.build()
						.resourceSpecCharacteristic(List.of(ResourceSpecificationCharacteristicVOTestExample.build()
								.resourceSpecCharRelationship(
										List.of(ResourceSpecificationCharacteristicRelationshipVOTestExample.build()
												.resourceSpecificationId("invalid")))))));

		testEntries.add(Arguments.of(
				"A resource specification with an non-existent resource spec id on the resource spec char rel should not be accepted.",
				ResourceSpecificationUpdateVOTestExample.build()
						.resourceSpecCharacteristic(List.of(ResourceSpecificationCharacteristicVOTestExample.build()
								.resourceSpecCharRelationship(
										List.of(ResourceSpecificationCharacteristicRelationshipVOTestExample.build()
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
		ResourceSpecificationUpdateVO resourceSpecificationUpdateVO = ResourceSpecificationUpdateVOTestExample.build();
		assertEquals(
				HttpStatus.NOT_FOUND,
				callAndCatch(() -> resourceSpecificationApiTestClient.patchResourceSpecification(
						"urn:ngsi-ld:resource-specification:not-existent", resourceSpecificationUpdateVO)).getStatus(),
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

		ResourceSpecificationCreateVO resourceSpecificationCreateVO = ResourceSpecificationCreateVOTestExample.build();
		HttpResponse<ResourceSpecificationVO> createResponse = callAndCatch(
				() -> resourceSpecificationApiTestClient.createResourceSpecification(resourceSpecificationCreateVO));
		assertEquals(HttpStatus.CREATED, createResponse.getStatus(), message);
		String id = createResponse.body().getId();

		expectedResourceSpecification
				.id(id)
				.href(URI.create(id));

		//then retrieve
		HttpResponse<ResourceSpecificationVO> retrievedResourceSpec = callAndCatch(
				() -> resourceSpecificationApiTestClient.retrieveResourceSpecification(id, fieldsParameter));
		assertEquals(HttpStatus.OK, retrievedResourceSpec.getStatus(), message);
		assertEquals(expectedResourceSpecification, retrievedResourceSpec.body(), message);
	}

	private static Stream<Arguments> provideFieldParameters() {
		return Stream.of(
				Arguments.of("Without a fields parameter everything should be returned.", null,
						ResourceSpecificationVOTestExample.build().relatedParty(null).resourceSpecRelationship(null)),
				Arguments.of("Only version and the mandatory parameters should have been included.", "version",
						ResourceSpecificationVOTestExample.build()
								.relatedParty(null)
								.lastUpdate(null)
								.isBundle(null)
								.resourceSpecRelationship(null)
								.featureSpecification(null)
								.category(null)
								.description(null)
								.lifecycleStatus(null)
								.name(null)
								.attachment(null)
								.resourceSpecCharacteristic(null)
								.targetResourceSchema(null)
								.validFor(null)
								.atBaseType(null)
								.atSchemaLocation(null)
								.atType(null)),
				Arguments.of(
						"Only the mandatory parameters should have been included when a non-existent field was requested.",
						"nothingToSeeHere", ResourceSpecificationVOTestExample.build()
								.relatedParty(null)
								.lastUpdate(null)
								.isBundle(null)
								.resourceSpecRelationship(null)
								.featureSpecification(null)
								.category(null)
								.description(null)
								.lifecycleStatus(null)
								.name(null)
								.version(null)
								.attachment(null)
								.resourceSpecCharacteristic(null)
								.targetResourceSchema(null)
								.validFor(null)
								.atBaseType(null)
								.atSchemaLocation(null)
								.atType(null)),
				Arguments.of(
						"Only version, lastUpdate, lifecycleStatus, description and the mandatory parameters should have been included.",
						"version,lastUpdate,lifecycleStatus,description", ResourceSpecificationVOTestExample.build()
								.relatedParty(null)
								.isBundle(null)
								.resourceSpecRelationship(null)
								.featureSpecification(null)
								.category(null)
								.name(null)
								.attachment(null)
								.resourceSpecCharacteristic(null)
								.targetResourceSchema(null)
								.validFor(null)
								.atBaseType(null)
								.atSchemaLocation(null)
								.atType(null)));
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
				() -> resourceSpecificationApiTestClient.retrieveResourceSpecification(
						"urn:ngsi-ld:resource-function:non-existent", null));
		assertEquals(HttpStatus.NOT_FOUND, response.getStatus(), "No such resource-specification should exist.");

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

	@Override protected String getEntityType() {
		return ResourceSpecification.TYPE_RESOURCE_SPECIFICATION;
	}
}
