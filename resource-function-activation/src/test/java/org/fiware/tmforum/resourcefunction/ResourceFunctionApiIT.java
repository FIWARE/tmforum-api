package org.fiware.tmforum.resourcefunction;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import io.micronaut.test.annotation.MockBean;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import org.fiware.ngsi.api.EntitiesApiClient;
import org.fiware.resourcefunction.api.ResourceFunctionApiTestClient;
import org.fiware.resourcefunction.api.ResourceFunctionApiTestSpec;
import org.fiware.resourcefunction.model.*;
import org.fiware.tmforum.common.configuration.GeneralProperties;
import org.fiware.tmforum.common.exception.ErrorDetails;
import org.fiware.tmforum.common.notification.TMForumEventHandler;
import org.fiware.tmforum.common.test.AbstractApiIT;
import org.fiware.tmforum.resourcefunction.domain.ResourceFunction;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@MicronautTest(packages = {"org.fiware.tmforum.resourcefunction"})
public class ResourceFunctionApiIT extends AbstractApiIT implements ResourceFunctionApiTestSpec {

	public final ResourceFunctionApiTestClient resourceFunctionApiTestClient;

	private String message;
	private ResourceFunctionCreateVO resourceFunctionCreateVO;
	private ResourceFunctionUpdateVO resourceFunctionUpdateVO;
	private ResourceFunctionVO expectedResourceFunction;

	public ResourceFunctionApiIT(ResourceFunctionApiTestClient resourceFunctionApiTestClient,
								 EntitiesApiClient entitiesApiClient, ObjectMapper objectMapper, GeneralProperties generalProperties) {
		super(entitiesApiClient, objectMapper, generalProperties);
		this.resourceFunctionApiTestClient = resourceFunctionApiTestClient;
	}

	@MockBean(TMForumEventHandler.class)
	public TMForumEventHandler eventHandler() {
		TMForumEventHandler eventHandler = mock(TMForumEventHandler.class);

		when(eventHandler.handleCreateEvent(any())).thenReturn(Mono.empty());
		when(eventHandler.handleUpdateEvent(any(), any())).thenReturn(Mono.empty());

		return eventHandler;
	}

	@ParameterizedTest
	@MethodSource("provideValidResourceFunctions")
	public void createResourceFunction201(String message, ResourceFunctionCreateVO resourceFunctionCreateVO,
										  ResourceFunctionVO expectedResourceFunction) throws Exception {
		this.message = message;
		this.resourceFunctionCreateVO = resourceFunctionCreateVO;
		this.expectedResourceFunction = expectedResourceFunction;
		createResourceFunction201();
	}

	@Override
	public void createResourceFunction201() throws Exception {

		HttpResponse<ResourceFunctionVO> resourceFunctionVOHttpResponse = callAndCatch(
				() -> resourceFunctionApiTestClient.createResourceFunction(null, resourceFunctionCreateVO));
		assertEquals(HttpStatus.CREATED, resourceFunctionVOHttpResponse.getStatus(), message);
		String rfId = resourceFunctionVOHttpResponse.body().getId();
		expectedResourceFunction.setId(rfId);
		expectedResourceFunction.setHref(URI.create(rfId));

		assertEquals(expectedResourceFunction, resourceFunctionVOHttpResponse.body(), message);
	}

	private static Stream<Arguments> provideValidResourceFunctions() {
		List<Arguments> testEntries = new ArrayList<>();

		ResourceFunctionCreateVO emptyCreate = ResourceFunctionCreateVOTestExample.build().atSchemaLocation(null);
		emptyCreate.place(null).resourceSpecification(null);
		ResourceFunctionVO expectedEmptyRF = ResourceFunctionVOTestExample.build().atSchemaLocation(null);
		expectedEmptyRF.place(null).resourceSpecification(null);
		testEntries.add(
				Arguments.of("An empty resource function should have been created.", emptyCreate, expectedEmptyRF));

		return testEntries.stream();
	}

	@ParameterizedTest
	@MethodSource("provideInvalidResourceFunctions")
	public void createResourceFunction400(String message, ResourceFunctionCreateVO invalidCreateVO) throws Exception {
		this.message = message;
		this.resourceFunctionCreateVO = invalidCreateVO;
		createResourceFunction400();
	}

	@Override
	public void createResourceFunction400() throws Exception {
		HttpResponse<ResourceFunctionVO> creationResponse = callAndCatch(
				() -> resourceFunctionApiTestClient.createResourceFunction(null, resourceFunctionCreateVO));
		assertEquals(HttpStatus.BAD_REQUEST, creationResponse.getStatus(), message);
		Optional<ErrorDetails> optionalErrorDetails = creationResponse.getBody(ErrorDetails.class);
		assertTrue(optionalErrorDetails.isPresent(), "Error details should be provided.");
	}

	private static Stream<Arguments> provideInvalidResourceFunctions() {
		List<Arguments> testEntries = new ArrayList<>();

		testEntries.add(Arguments.of("A resource functions with invalid connection points should not be created.",
				ResourceFunctionCreateVOTestExample.build().atSchemaLocation(null)
						.connectionPoint(List.of(ConnectionPointRefVOTestExample.build().atSchemaLocation(null)))));
		testEntries.add(Arguments.of("A resource functions with non-existent connection point should not be created.",
				ResourceFunctionCreateVOTestExample.build().atSchemaLocation(null).connectionPoint(
						List.of(ConnectionPointRefVOTestExample.build().atSchemaLocation(null).id("urn:ngsi-ld:connection:non-existent")))));

		testEntries.add(Arguments.of("A resource functions with invalid related parties should not be created.",
				ResourceFunctionCreateVOTestExample.build().atSchemaLocation(null).relatedParty(List.of(RelatedPartyVOTestExample.build().atSchemaLocation(null)))));
		testEntries.add(Arguments.of("A resource functions with non-existent related parties should not be created.",
				ResourceFunctionCreateVOTestExample.build().atSchemaLocation(null).relatedParty(
						List.of(RelatedPartyVOTestExample.build().atSchemaLocation(null).id("urn:ngsi-ld:organisation:non-existent")))));

		testEntries.add(Arguments.of("A resource functions with invalid schedules should not be created.",
				ResourceFunctionCreateVOTestExample.build().atSchemaLocation(null).schedule(List.of(ScheduleRefVOTestExample.build().atSchemaLocation(null)))));
		testEntries.add(Arguments.of("A resource functions with non-existent schedules should not be created.",
				ResourceFunctionCreateVOTestExample.build().atSchemaLocation(null)
						.schedule(List.of(ScheduleRefVOTestExample.build().atSchemaLocation(null).id("urn:ngsi-ld:schedule:non-existent")))));

		testEntries.add(Arguments.of("A resource functions with an invalid place should not be created.",
				ResourceFunctionCreateVOTestExample.build().atSchemaLocation(null).place(RelatedPlaceRefOrValueVOTestExample.build().atSchemaLocation(null))));
		testEntries.add(Arguments.of("A resource functions with a non-existent place should not be created.",
				ResourceFunctionCreateVOTestExample.build().atSchemaLocation(null)
						.place(RelatedPlaceRefOrValueVOTestExample.build().atSchemaLocation(null).id("urn:ngsi-ld:place:non-existent"))));

		testEntries.add(Arguments.of("A resource functions with an invalid resource spec should not be created.",
				ResourceFunctionCreateVOTestExample.build().atSchemaLocation(null)
						.resourceSpecification(ResourceSpecificationRefVOTestExample.build().atSchemaLocation(null))));
		testEntries.add(Arguments.of("A resource functions with a non-existent resource spec  should not be created.",
				ResourceFunctionCreateVOTestExample.build().atSchemaLocation(null).resourceSpecification(
						ResourceSpecificationRefVOTestExample.build().atSchemaLocation(null).id("urn:ngsi-ld:resource-spec:non-existent"))));

		testEntries.add(Arguments.of("A resource function with an invalid activation feature should not be created.",
				ResourceFunctionCreateVOTestExample.build().atSchemaLocation(null).activationFeature(
						List.of(FeatureVOTestExample.build().atSchemaLocation(null)
								.constraint(List.of(ConstraintRefVOTestExample.build().atSchemaLocation(null)))))));

		testEntries.add(Arguments.of("A resource functions with an invalid activation feature should not be created.",
				ResourceFunctionCreateVOTestExample.build().atSchemaLocation(null).activationFeature(
						List.of(FeatureVOTestExample.build().atSchemaLocation(null)
								.featureRelationship(List.of(FeatureRelationshipVOTestExample.build().atSchemaLocation(null)))))));

		testEntries.add(Arguments.of("A resource functions with an invalid auto modification should not be created.",
				ResourceFunctionCreateVOTestExample.build().atSchemaLocation(null).autoModification(
						List.of(CharacteristicVOTestExample.build().atSchemaLocation(null)
								.characteristicRelationship(
										List.of(CharacteristicRelationshipVOTestExample.build().atSchemaLocation(null)))))));

		testEntries.add(
				Arguments.of("A resource functions with an invalid resource characteristic should not be created.",
						ResourceFunctionCreateVOTestExample.build().atSchemaLocation(null).resourceCharacteristic(
								List.of(CharacteristicVOTestExample.build().atSchemaLocation(null)
										.characteristicRelationship(
												List.of(CharacteristicRelationshipVOTestExample.build().atSchemaLocation(null)))))));

		testEntries.add(Arguments.of("A resource functions with an invalid connectivity should not be created.",
				ResourceFunctionCreateVOTestExample.build().atSchemaLocation(null).connectivity(
						List.of(ResourceGraphVOTestExample.build().atSchemaLocation(null)
								.graphRelationship(List.of(ResourceGraphRelationshipVOTestExample.build().atSchemaLocation(null)))))));
		testEntries.add(
				Arguments.of("A resource functions with an invalid resource should not be created.",
						ResourceFunctionCreateVOTestExample.build().atSchemaLocation(null).resourceRelationship(
								List.of(ResourceRelationshipVOTestExample.build().atSchemaLocation(null)
										.resource(ResourceRefOrValueVOTestExample.build().atSchemaLocation(null))))));

		testEntries.add(Arguments.of("A resource functions with a non existing resource should not be created.",
				ResourceFunctionCreateVOTestExample.build().atSchemaLocation(null).resourceRelationship(
						List.of(ResourceRelationshipVOTestExample.build().atSchemaLocation(null)
								.resource(ResourceRefOrValueVOTestExample.build().atSchemaLocation(null)
										.id("urn:ngsi-ld:resource:non-existent"))))));

		return testEntries.stream();
	}

	@Disabled("Security is handled externally, thus 401 and 403 cannot happen.")
	@Test
	@Override
	public void createResourceFunction401() throws Exception {

	}

	@Disabled("Security is handled externally, thus 401 and 403 cannot happen.")
	@Test
	@Override
	public void createResourceFunction403() throws Exception {

	}

	@Disabled("Prohibited by the framework.")
	@Test
	@Override
	public void createResourceFunction405() throws Exception {
	}

	@Override
	public void createResourceFunction409() throws Exception {
		// TODO: check if that can happen
	}

	@Override
	public void createResourceFunction500() throws Exception {

	}

	@Test
	@Override
	public void deleteResourceFunction204() throws Exception {
		ResourceFunctionCreateVO emptyCreate = ResourceFunctionCreateVOTestExample.build().atSchemaLocation(null);
		emptyCreate.place(null).resourceSpecification(null);

		HttpResponse<ResourceFunctionVO> createResponse = resourceFunctionApiTestClient.createResourceFunction(null,
				emptyCreate);
		assertEquals(HttpStatus.CREATED, createResponse.getStatus(),
				"The resource function should have been created first.");

		String rfId = createResponse.body().getId();

		assertEquals(HttpStatus.NO_CONTENT,
				callAndCatch(() -> resourceFunctionApiTestClient.deleteResourceFunction(null, rfId)).getStatus(),
				"The resource function should have been deleted.");

		assertEquals(HttpStatus.NOT_FOUND,
				callAndCatch(() -> resourceFunctionApiTestClient.retrieveResourceFunction(null, rfId, null)).status(),
				"The resource function should not exist anymore.");
	}

	@Disabled("400 is impossible to happen on deletion with the current implementation.")
	@Test
	@Override
	public void deleteResourceFunction400() throws Exception {

	}

	@Disabled("Security is handled externally, thus 401 and 403 cannot happen.")
	@Test
	@Override
	public void deleteResourceFunction401() throws Exception {

	}

	@Disabled("Security is handled externally, thus 401 and 403 cannot happen.")
	@Test
	@Override
	public void deleteResourceFunction403() throws Exception {

	}

	@Test
	@Override
	public void deleteResourceFunction404() throws Exception {
		HttpResponse<?> notFoundResponse = callAndCatch(
				() -> resourceFunctionApiTestClient.deleteResourceFunction(null, "urn:ngsi-ld:resource-function:no-pop"));
		assertEquals(HttpStatus.NOT_FOUND,
				notFoundResponse.getStatus(),
				"No such resource-function should exist.");

		Optional<ErrorDetails> optionalErrorDetails = notFoundResponse.getBody(ErrorDetails.class);
		assertTrue(optionalErrorDetails.isPresent(), "Error details should be provided.");

		notFoundResponse = callAndCatch(() -> resourceFunctionApiTestClient.deleteResourceFunction(null, "invalid-id"));
		assertEquals(HttpStatus.NOT_FOUND,
				notFoundResponse.getStatus(),
				"No such resource-function should exist.");

		optionalErrorDetails = notFoundResponse.getBody(ErrorDetails.class);
		assertTrue(optionalErrorDetails.isPresent(), "Error details should be provided.");
	}

	@Disabled("Prohibited by the framework.")
	@Test
	@Override
	public void deleteResourceFunction405() throws Exception {

	}

	@Disabled("Impossible status.")
	@Test
	@Override
	public void deleteResourceFunction409() throws Exception {

	}

	@Override
	public void deleteResourceFunction500() throws Exception {

	}

	@Test
	@Override
	public void listResourceFunction200() throws Exception {

		List<ResourceFunctionVO> expectedResourceFunctions = new ArrayList<>();
		for (int i = 0; i < 10; i++) {
			ResourceFunctionCreateVO resourceFunctionCreateVO = ResourceFunctionCreateVOTestExample.build().atSchemaLocation(null)
					.place(null)
					.resourceSpecification(null);
			String id = resourceFunctionApiTestClient.createResourceFunction(null, resourceFunctionCreateVO)
					.body().getId();
			ResourceFunctionVO resourceFunctionVO = ResourceFunctionVOTestExample.build().atSchemaLocation(null);
			resourceFunctionVO
					.id(id)
					.href(URI.create(id))
					.place(null)
					.relatedParty(null)
					.resourceSpecification(null);
			expectedResourceFunctions.add(resourceFunctionVO);
		}

		HttpResponse<List<ResourceFunctionVO>> resourceFunctionResponse = callAndCatch(
				() -> resourceFunctionApiTestClient.listResourceFunction(null, null, null, null));

		assertEquals(HttpStatus.OK, resourceFunctionResponse.getStatus(), "The list should be accessible.");
		assertEquals(expectedResourceFunctions.size(), resourceFunctionResponse.getBody().get().size(),
				"All resourceFunctions should have been returned.");
		List<ResourceFunctionVO> retrievedResourceFunctions = resourceFunctionResponse.getBody().get();

		Map<String, ResourceFunctionVO> retrievedMap = retrievedResourceFunctions.stream()
				.collect(Collectors.toMap(resourceFunction -> resourceFunction.getId(),
						resourceFunction -> resourceFunction));

		expectedResourceFunctions.stream()
				.forEach(
						expectedResourceFunction -> assertTrue(
								retrievedMap.containsKey(expectedResourceFunction.getId()),
								String.format("All created resourceFunctions should be returned - Missing: %s.",
										expectedResourceFunction,
										retrievedResourceFunctions)));
		expectedResourceFunctions.stream().forEach(
				expectedResourceFunction -> assertEquals(expectedResourceFunction,
						retrievedMap.get(expectedResourceFunction.getId()),
						"The correct resourceFunctions should be retrieved."));

		// get with pagination
		Integer limit = 5;
		HttpResponse<List<ResourceFunctionVO>> firstPartResponse = callAndCatch(
				() -> resourceFunctionApiTestClient.listResourceFunction(null, null, 0, limit));
		assertEquals(limit, firstPartResponse.body().size(),
				"Only the requested number of entries should be returend.");
		HttpResponse<List<ResourceFunctionVO>> secondPartResponse = callAndCatch(
				() -> resourceFunctionApiTestClient.listResourceFunction(null, null, 0 + limit, limit));
		assertEquals(limit, secondPartResponse.body().size(),
				"Only the requested number of entries should be returend.");

		retrievedResourceFunctions.clear();
		retrievedResourceFunctions.addAll(firstPartResponse.body());
		retrievedResourceFunctions.addAll(secondPartResponse.body());
		expectedResourceFunctions.stream()
				.forEach(
						expectedResourceFunction -> assertTrue(
								retrievedMap.containsKey(expectedResourceFunction.getId()),
								String.format("All created resourceFunctions should be returned - Missing: %s.",
										expectedResourceFunction)));
		expectedResourceFunctions.stream().forEach(
				expectedResourceFunction -> assertEquals(expectedResourceFunction,
						retrievedMap.get(expectedResourceFunction.getId()),
						"The correct resourceFunctions should be retrieved."));
	}

	@Test
	@Override
	public void listResourceFunction400() throws Exception {
		HttpResponse<List<ResourceFunctionVO>> badRequestResponse = callAndCatch(
				() -> resourceFunctionApiTestClient.listResourceFunction(null, null, -1, null));
		assertEquals(HttpStatus.BAD_REQUEST,
				badRequestResponse.getStatus(),
				"Negative offsets are impossible.");

		Optional<ErrorDetails> optionalErrorDetails = badRequestResponse.getBody(ErrorDetails.class);
		assertTrue(optionalErrorDetails.isPresent(), "Error details should be provided.");

		badRequestResponse = callAndCatch(() -> resourceFunctionApiTestClient.listResourceFunction(null, null, null, -1));
		assertEquals(HttpStatus.BAD_REQUEST,
				badRequestResponse.getStatus(),
				"Negative limits are impossible.");
		optionalErrorDetails = badRequestResponse.getBody(ErrorDetails.class);
		assertTrue(optionalErrorDetails.isPresent(), "Error details should be provided.");
	}

	@Disabled("Security is handled externally, thus 401 and 403 cannot happen.")
	@Test
	@Override
	public void listResourceFunction401() throws Exception {

	}

	@Disabled("Security is handled externally, thus 401 and 403 cannot happen.")
	@Test
	@Override
	public void listResourceFunction403() throws Exception {

	}

	@Disabled("Not found is not possible here, will be answered with an empty list instead.")
	@Test
	@Override
	public void listResourceFunction404() throws Exception {

	}

	@Disabled("Prohibited by the framework.")
	@Test
	@Override
	public void listResourceFunction405() throws Exception {

	}

	@Disabled("Impossible status.")
	@Test
	@Override
	public void listResourceFunction409() throws Exception {

	}

	@Override
	public void listResourceFunction500() throws Exception {

	}

	@ParameterizedTest
	@MethodSource("provideResourceFunctionUpdates")
	public void patchResourceFunction200(String message, ResourceFunctionUpdateVO resourceFunctionUpdateVO,
										 ResourceFunctionVO expectedResourceFunction) throws Exception {
		this.message = message;
		this.resourceFunctionUpdateVO = resourceFunctionUpdateVO;
		this.expectedResourceFunction = expectedResourceFunction;
		patchResourceFunction200();
	}

	@Override
	public void patchResourceFunction200() throws Exception {
		//first create
		ResourceFunctionCreateVO resourceFunctionCreateVO = ResourceFunctionCreateVOTestExample.build().atSchemaLocation(null).place(null)
				.resourceSpecification(null);

		HttpResponse<ResourceFunctionVO> createResponse = callAndCatch(
				() -> resourceFunctionApiTestClient.createResourceFunction(null, resourceFunctionCreateVO));
		assertEquals(HttpStatus.CREATED, createResponse.getStatus(),
				"The resource function should have been created first.");

		String resourceId = createResponse.body().getId();

		HttpResponse<ResourceFunctionVO> updateResponse = callAndCatch(
				() -> resourceFunctionApiTestClient.patchResourceFunction(null, resourceId, resourceFunctionUpdateVO));
		assertEquals(HttpStatus.OK, updateResponse.getStatus(), message);

		ResourceFunctionVO updatedResourceFunction = updateResponse.body();
		expectedResourceFunction.href(URI.create(resourceId)).id(resourceId);

		if (expectedResourceFunction.getRelatedParty() != null && expectedResourceFunction.getRelatedParty()
				.isEmpty()) {
			expectedResourceFunction.relatedParty(null);
		}

		assertEquals(expectedResourceFunction, updatedResourceFunction, message);
	}

	private static Stream<Arguments> provideResourceFunctionUpdates() {
		List<Arguments> testEntries = new ArrayList<>();

		ResourceFunctionUpdateVO functionUpdate = ResourceFunctionUpdateVOTestExample.build().atSchemaLocation(null)
				.functionType("new-function")
				.place(null)
				.resourceSpecification(null);
		ResourceFunctionVO expectedFunctionUpdate = ResourceFunctionVOTestExample.build().atSchemaLocation(null)
				.functionType("new-function")
				.place(null)
				.resourceSpecification(null);
		testEntries.add(
				Arguments.of("The function type should have been updated.", functionUpdate, expectedFunctionUpdate));

		ResourceFunctionUpdateVO categoryUpdate = ResourceFunctionUpdateVOTestExample.build().atSchemaLocation(null).category("new-category")
				.place(null)
				.resourceSpecification(null);
		ResourceFunctionVO expectedCategoryUpdate = ResourceFunctionVOTestExample.build().atSchemaLocation(null)
				.category("new-category")
				.place(null)
				.resourceSpecification(null);
		testEntries.add(Arguments.of("The category should have been updated.", categoryUpdate, expectedCategoryUpdate));

		ResourceFunctionUpdateVO descriptionUpdate = ResourceFunctionUpdateVOTestExample.build().atSchemaLocation(null)
				.description("new-description")
				.place(null)
				.resourceSpecification(null);
		ResourceFunctionVO expectedDescriptionUpdate = ResourceFunctionVOTestExample.build().atSchemaLocation(null)
				.description("new-description")
				.place(null)
				.resourceSpecification(null);
		testEntries.add(Arguments.of("The description should have been updated.", descriptionUpdate,
				expectedDescriptionUpdate));

		Instant endTime = Instant.now();
		ResourceFunctionUpdateVO endTimeUpdate = ResourceFunctionUpdateVOTestExample.build().atSchemaLocation(null)
				.endOperatingDate(endTime)
				.place(null)
				.resourceSpecification(null);
		ResourceFunctionVO expectedEndTimeUpdate = ResourceFunctionVOTestExample.build().atSchemaLocation(null)
				.place(null)
				.resourceSpecification(null)
				.endOperatingDate(endTime);
		testEntries.add(Arguments.of("The endTime should have been updated.", endTimeUpdate, expectedEndTimeUpdate));

		ResourceFunctionUpdateVO nameUpdate = ResourceFunctionUpdateVOTestExample.build().atSchemaLocation(null)
				.name("new-name")
				.place(null)
				.resourceSpecification(null);
		ResourceFunctionVO expectedNameUpdate = ResourceFunctionVOTestExample.build().atSchemaLocation(null)
				.name("new-name")
				.place(null)
				.resourceSpecification(null);
		testEntries.add(Arguments.of("The name should have been updated.", nameUpdate, expectedNameUpdate));

		ResourceFunctionUpdateVO priorityUpdate = ResourceFunctionUpdateVOTestExample.build().atSchemaLocation(null)
				.priority(10)
				.place(null)
				.resourceSpecification(null);
		ResourceFunctionVO expectedPriorityUpdate = ResourceFunctionVOTestExample.build().atSchemaLocation(null)
				.priority(10)
				.place(null)
				.resourceSpecification(null);
		testEntries.add(Arguments.of("The priority should have been updated.", priorityUpdate, expectedPriorityUpdate));

		ResourceFunctionUpdateVO resourceVersionUpdate = ResourceFunctionUpdateVOTestExample.build().atSchemaLocation(null)
				.resourceVersion("10")
				.place(null)
				.resourceSpecification(null);
		ResourceFunctionVO expectedResourceVersionUpdate = ResourceFunctionVOTestExample.build().atSchemaLocation(null)
				.resourceVersion("10")
				.place(null)
				.resourceSpecification(null);
		testEntries.add(Arguments.of("The resourceVersion should have been updated.", resourceVersionUpdate,
				expectedResourceVersionUpdate));

		ResourceFunctionUpdateVO roleUpdate = ResourceFunctionUpdateVOTestExample.build().atSchemaLocation(null)
				.role("new-role")
				.place(null)
				.resourceSpecification(null);
		ResourceFunctionVO expectedRoleUpdate = ResourceFunctionVOTestExample.build().atSchemaLocation(null)
				.role("new-role")
				.place(null)
				.resourceSpecification(null);
		testEntries.add(Arguments.of("The role should have been updated.", roleUpdate, expectedRoleUpdate));

		Instant starTime = Instant.now();
		ResourceFunctionUpdateVO startimeUpdate = ResourceFunctionUpdateVOTestExample.build().atSchemaLocation(null)
				.startOperatingDate(starTime)
				.place(null)
				.resourceSpecification(null);
		ResourceFunctionVO expectedStartimeUpdate = ResourceFunctionVOTestExample.build().atSchemaLocation(null)
				.startOperatingDate(starTime)
				.place(null)
				.resourceSpecification(null);
		testEntries.add(Arguments.of("The startime should have been updated.", startimeUpdate, expectedStartimeUpdate));

		ResourceFunctionUpdateVO valueUpdate = ResourceFunctionUpdateVOTestExample.build().atSchemaLocation(null)
				.value("new-value")
				.place(null)
				.resourceSpecification(null);
		ResourceFunctionVO expectedValueUpdate = ResourceFunctionVOTestExample.build().atSchemaLocation(null)
				.value("new-value")
				.place(null)
				.resourceSpecification(null);
		testEntries.add(Arguments.of("The value should have been updated.", valueUpdate, expectedValueUpdate));

		ResourceFunctionUpdateVO adminStateUpdate = ResourceFunctionUpdateVOTestExample.build().atSchemaLocation(null)
				.administrativeState(ResourceAdministrativeStateTypeVO.LOCKED)
				.place(null)
				.resourceSpecification(null);
		ResourceFunctionVO expectedAdminStateUpdate = ResourceFunctionVOTestExample.build().atSchemaLocation(null)
				.administrativeState(ResourceAdministrativeStateTypeVO.LOCKED)
				.place(null)
				.resourceSpecification(null);
		testEntries.add(Arguments.of("The administrative state should have been updated.", adminStateUpdate,
				expectedAdminStateUpdate));

		ResourceFunctionUpdateVO attachmentUpdate = ResourceFunctionUpdateVOTestExample.build().atSchemaLocation(null)
				.attachment(List.of(AttachmentRefOrValueVOTestExample.build().atSchemaLocation(null)
						.id("urn:ngsi-ld:attachment:a")
						.url(URI.create("http://my-attachment.com"))
						.href(URI.create("urn:ngsi-ld:attachment:a"))))
				.place(null)
				.resourceSpecification(null);
		ResourceFunctionVO expectedAttachmentUpdate = ResourceFunctionVOTestExample.build().atSchemaLocation(null)
				.attachment(List.of(AttachmentRefOrValueVOTestExample.build().atSchemaLocation(null)
						.id("urn:ngsi-ld:attachment:a")
						.url(URI.create("http://my-attachment.com"))
						.href(URI.create("urn:ngsi-ld:attachment:a"))))
				.place(null)
				.resourceSpecification(null);
		testEntries.add(
				Arguments.of("The attachments should have been updated.", attachmentUpdate, expectedAttachmentUpdate));

		String featureId = "urn:" + UUID.randomUUID();
		ResourceFunctionUpdateVO featureUpdate = ResourceFunctionUpdateVOTestExample.build().atSchemaLocation(null)
				.activationFeature(List.of(FeatureVOTestExample.build().atSchemaLocation(null).id(featureId)))
				.place(null)
				.resourceSpecification(null);
		ResourceFunctionVO expectedFeatureUpdate = ResourceFunctionVOTestExample.build().atSchemaLocation(null)
				.activationFeature(
						List.of(FeatureVOTestExample.build().atSchemaLocation(null).id(featureId)))
				.place(null)
				.resourceSpecification(null);
		testEntries.add(
				Arguments.of("The activation feature should have been updated.", featureUpdate, expectedFeatureUpdate));

		String charId = "urn:" + UUID.randomUUID();
		ResourceFunctionUpdateVO autoModificationUpdate = ResourceFunctionUpdateVOTestExample.build().atSchemaLocation(null)
				.autoModification(List.of(CharacteristicVOTestExample.build().atSchemaLocation(null).id(charId)))
				.place(null)
				.resourceSpecification(null);
		ResourceFunctionVO expectedAutoModificationUpdate = ResourceFunctionVOTestExample.build().atSchemaLocation(null)
				.autoModification(
						List.of(CharacteristicVOTestExample.build().atSchemaLocation(null).id(charId)))
				.place(null)
				.resourceSpecification(null);
		testEntries.add(Arguments.of("The autoModification should have been updated.", autoModificationUpdate,
				expectedAutoModificationUpdate));

		String resourceCharId = "urn:" + UUID.randomUUID();
		ResourceFunctionUpdateVO resourceCharacteristicUpdate = ResourceFunctionUpdateVOTestExample.build().atSchemaLocation(null)
				.resourceCharacteristic(List.of(CharacteristicVOTestExample.build().atSchemaLocation(null).id(resourceCharId)))
				.place(null)
				.resourceSpecification(null);
		ResourceFunctionVO expectedResourceCharacteristicUpdate = ResourceFunctionVOTestExample.build().atSchemaLocation(null)
				.resourceCharacteristic(List.of(CharacteristicVOTestExample.build().atSchemaLocation(null)
						.id(resourceCharId)))
				.place(null)
				.resourceSpecification(null);
		testEntries.add(
				Arguments.of("The resourceCharacteristic should have been updated.", resourceCharacteristicUpdate,
						expectedResourceCharacteristicUpdate));

		String graphId = "urn:" + UUID.randomUUID();
		ResourceFunctionUpdateVO connectivityUpdate = ResourceFunctionUpdateVOTestExample.build().atSchemaLocation(null)
				.connectivity(List.of(ResourceGraphVOTestExample.build().atSchemaLocation(null).id(graphId)))
				.place(null)
				.resourceSpecification(null);
		ResourceFunctionVO expectedConnectivityUpdate = ResourceFunctionVOTestExample.build().atSchemaLocation(null)
				.connectivity(List.of(ResourceGraphVOTestExample.build().atSchemaLocation(null).id(graphId)))
				.place(null)
				.resourceSpecification(null);
		testEntries.add(Arguments.of("The connectivity should have been updated.", connectivityUpdate,
				expectedConnectivityUpdate));

		ResourceFunctionUpdateVO notesUpdate = ResourceFunctionUpdateVOTestExample.build().atSchemaLocation(null)
				.note(List.of(NoteVOTestExample.build().atSchemaLocation(null).author("My author").id("urn:author-note").text("This is my note.")))
				.place(null)
				.resourceSpecification(null);
		ResourceFunctionVO expectedNotesUpdate = ResourceFunctionVOTestExample.build().atSchemaLocation(null)
				.note(List.of(NoteVOTestExample.build().atSchemaLocation(null).author("My author").id("urn:author-note").text("This is my note.")))
				.place(null)
				.resourceSpecification(null);
		testEntries.add(Arguments.of("The notes should have been updated.", notesUpdate, expectedNotesUpdate));

		ResourceFunctionUpdateVO opStateUpdate = ResourceFunctionUpdateVOTestExample.build().atSchemaLocation(null)
				.operationalState(ResourceOperationalStateTypeVO.ENABLE)
				.place(null)
				.resourceSpecification(null);
		ResourceFunctionVO expectedOpStateUpdate = ResourceFunctionVOTestExample.build().atSchemaLocation(null)
				.operationalState(ResourceOperationalStateTypeVO.ENABLE)
				.place(null)
				.resourceSpecification(null);
		testEntries.add(
				Arguments.of("The operational state should have been updated.", opStateUpdate, expectedOpStateUpdate));

		ResourceFunctionUpdateVO statusUpdate = ResourceFunctionUpdateVOTestExample.build().atSchemaLocation(null)
				.resourceStatus(ResourceStatusTypeVO.AVAILABLE)
				.place(null)
				.resourceSpecification(null);
		ResourceFunctionVO expectedStatusUpdate = ResourceFunctionVOTestExample.build().atSchemaLocation(null)
				.resourceStatus(ResourceStatusTypeVO.AVAILABLE)
				.place(null)
				.resourceSpecification(null);
		testEntries.add(
				Arguments.of("The resource status should have been updated.", statusUpdate, expectedStatusUpdate));

		ResourceFunctionUpdateVO usageStateUpdate = ResourceFunctionUpdateVOTestExample.build().atSchemaLocation(null)
				.usageState(ResourceUsageStateTypeVO.ACTIVE)
				.place(null)
				.resourceSpecification(null);
		ResourceFunctionVO expectedUsageStateUpdate = ResourceFunctionVOTestExample.build().atSchemaLocation(null)
				.usageState(ResourceUsageStateTypeVO.ACTIVE)
				.place(null)
				.resourceSpecification(null);
		testEntries.add(
				Arguments.of("The usageState should have been updated.", usageStateUpdate, expectedUsageStateUpdate));

		return testEntries.stream();
	}

	@ParameterizedTest
	@MethodSource("provideInvalidUpdates")
	public void patchResourceFunction400(String message, ResourceFunctionUpdateVO invalidUpdateVO) throws Exception {
		this.message = message;
		this.resourceFunctionUpdateVO = invalidUpdateVO;
		patchResourceFunction400();
	}

	@Override
	public void patchResourceFunction400() throws Exception {
		//first create
		ResourceFunctionCreateVO resourceFunctionCreateVO = ResourceFunctionCreateVOTestExample.build().atSchemaLocation(null).place(null)
				.resourceSpecification(null);

		HttpResponse<ResourceFunctionVO> createResponse = callAndCatch(
				() -> resourceFunctionApiTestClient.createResourceFunction(null, resourceFunctionCreateVO));
		assertEquals(HttpStatus.CREATED, createResponse.getStatus(),
				"The resource function should have been created first.");

		String resourceId = createResponse.body().getId();

		HttpResponse<ResourceFunctionVO> updateResponse = callAndCatch(
				() -> resourceFunctionApiTestClient.patchResourceFunction(null, resourceId, resourceFunctionUpdateVO));
		assertEquals(HttpStatus.BAD_REQUEST, updateResponse.getStatus(), message);

		Optional<ErrorDetails> optionalErrorDetails = updateResponse.getBody(ErrorDetails.class);
		assertTrue(optionalErrorDetails.isPresent(), "Some error details should be present.");
	}

	private static Stream<Arguments> provideInvalidUpdates() {
		List<Arguments> testEntries = new ArrayList<>();

		testEntries.add(Arguments.of("An update with an invalid connection point ref is not allowed.",
				ResourceFunctionUpdateVOTestExample.build().atSchemaLocation(null)
						.connectionPoint(List.of(ConnectionPointRefVOTestExample.build().atSchemaLocation(null)))));
		testEntries.add(Arguments.of("An update with an non existent connection point ref is not allowed.",
				ResourceFunctionUpdateVOTestExample.build().atSchemaLocation(null)
						.connectionPoint(List.of(ConnectionPointRefVOTestExample.build().atSchemaLocation(null)
								.id("urn:ngsi-ld:connection-point:non-existent")))));

		testEntries.add(Arguments.of("An update with an invalid related party ref is not allowed.",
				ResourceFunctionUpdateVOTestExample.build().atSchemaLocation(null)
						.relatedParty(List.of(RelatedPartyVOTestExample.build().atSchemaLocation(null)))));
		testEntries.add(Arguments.of("An update with an non existent related party is not allowed.",
				ResourceFunctionUpdateVOTestExample.build().atSchemaLocation(null)
						.relatedParty(List.of(RelatedPartyVOTestExample.build().atSchemaLocation(null)
								.id("urn:ngsi-ld:organisation:non-existent")))));

		testEntries.add(Arguments.of("An update with an invalid schedule ref is not allowed.",
				ResourceFunctionUpdateVOTestExample.build().atSchemaLocation(null)
						.schedule(List.of(ScheduleRefVOTestExample.build().atSchemaLocation(null)))));
		testEntries.add(Arguments.of("An update with an non existent schedule is not allowed.",
				ResourceFunctionUpdateVOTestExample.build().atSchemaLocation(null)
						.schedule(List.of(ScheduleRefVOTestExample.build().atSchemaLocation(null).id("urn:ngsi-ld:schedule:non-existent")))));

		// TODO: build place handling like the one for resource relationship ref-or-value
		testEntries.add(Arguments.of("An update with an invalid place ref is not allowed.",
				ResourceFunctionUpdateVOTestExample.build().atSchemaLocation(null)
						.place(RelatedPlaceRefOrValueVOTestExample.build().atSchemaLocation(null))));
		testEntries.add(Arguments.of("An update with an non existent schedule is not allowed.",
				ResourceFunctionUpdateVOTestExample.build().atSchemaLocation(null)
						.place(RelatedPlaceRefOrValueVOTestExample.build().atSchemaLocation(null).id("urn:ngsi-ld:place:non-existent"))));

		testEntries.add(Arguments.of("An update with an invalid resource spec ref is not allowed.",
				ResourceFunctionUpdateVOTestExample.build().atSchemaLocation(null)
						.resourceSpecification(ResourceSpecificationRefVOTestExample.build().atSchemaLocation(null))));
		testEntries.add(Arguments.of("An update with an non existent schedule is not allowed.",
				ResourceFunctionUpdateVOTestExample.build().atSchemaLocation(null)
						.resourceSpecification(ResourceSpecificationRefVOTestExample.build().atSchemaLocation(null)
								.id("urn:ngsi-ld:resource:non-existent"))));

		testEntries.add(Arguments.of("An update with an invalid activation constraint spec ref is not allowed.",
				ResourceFunctionUpdateVOTestExample.build().atSchemaLocation(null)
						.activationFeature(List.of(FeatureVOTestExample.build().atSchemaLocation(null)
								.constraint(List.of(ConstraintRefVOTestExample.build().atSchemaLocation(null)))))));
		testEntries.add(Arguments.of("An update with an non existent activation constraint is not allowed.",
				ResourceFunctionUpdateVOTestExample.build().atSchemaLocation(null)
						.activationFeature(List.of(FeatureVOTestExample.build().atSchemaLocation(null).constraint(
								List.of(ConstraintRefVOTestExample.build().atSchemaLocation(null)
										.id("urn:ngsi-ld:constraint:non-existent")))))));

		testEntries.add(Arguments.of("An update with an invalid activation feature ref is not allowed.",
				ResourceFunctionUpdateVOTestExample.build().atSchemaLocation(null)
						.activationFeature(List.of(FeatureVOTestExample.build().atSchemaLocation(null)
								.featureRelationship(List.of(FeatureRelationshipVOTestExample.build().atSchemaLocation(null)))))));
		testEntries.add(Arguments.of("An update with an non existent activation feature ref is not allowed.",
				ResourceFunctionUpdateVOTestExample.build().atSchemaLocation(null)
						.activationFeature(List.of(FeatureVOTestExample.build().atSchemaLocation(null).featureRelationship(
								List.of(FeatureRelationshipVOTestExample.build().atSchemaLocation(null)
										.id("urn:ngsi-ld:feature:non-existent")))))));

		testEntries.add(Arguments.of("An update with an invalid auto modification char ref is not allowed.",
				ResourceFunctionUpdateVOTestExample.build().atSchemaLocation(null)
						.autoModification(List.of(CharacteristicVOTestExample.build().atSchemaLocation(null).characteristicRelationship(
								List.of(CharacteristicRelationshipVOTestExample.build().atSchemaLocation(null)))))));
		testEntries.add(Arguments.of("An update with an non existent auto modification char ref is not allowed.",
				ResourceFunctionUpdateVOTestExample.build().atSchemaLocation(null)
						.autoModification(List.of(CharacteristicVOTestExample.build().atSchemaLocation(null).characteristicRelationship(
								List.of(CharacteristicRelationshipVOTestExample.build().atSchemaLocation(null)
										.id("urn:ngsi-ld:char:non-existent")))))));

		testEntries.add(Arguments.of("An update with an invalid resource char char ref is not allowed.",
				ResourceFunctionUpdateVOTestExample.build().atSchemaLocation(null)
						.resourceCharacteristic(List.of(CharacteristicVOTestExample.build().atSchemaLocation(null).characteristicRelationship(
								List.of(CharacteristicRelationshipVOTestExample.build().atSchemaLocation(null)))))));
		testEntries.add(Arguments.of("An update with an non existent resource char char ref is not allowed.",
				ResourceFunctionUpdateVOTestExample.build().atSchemaLocation(null)
						.resourceCharacteristic(List.of(CharacteristicVOTestExample.build().atSchemaLocation(null).characteristicRelationship(
								List.of(CharacteristicRelationshipVOTestExample.build().atSchemaLocation(null)
										.id("urn:ngsi-ld:char:non-existent")))))));

		testEntries.add(Arguments.of("An update with an invalid connectivity connection is not allowed.",
				ResourceFunctionUpdateVOTestExample.build().atSchemaLocation(null)
						.connectivity(List.of(ResourceGraphVOTestExample.build().atSchemaLocation(null)
								.connection(List.of(ConnectionVOTestExample.build().atSchemaLocation(null)))))));
		testEntries.add(Arguments.of("An update with an non existent connectivity connection is not allowed.",
				ResourceFunctionUpdateVOTestExample.build().atSchemaLocation(null)
						.connectivity(List.of(ResourceGraphVOTestExample.build().atSchemaLocation(null).connection(
								List.of(ConnectionVOTestExample.build().atSchemaLocation(null).id("urn:ngsi-ld:connection:non-existent")))))));

		testEntries.add(Arguments.of("An update with an invalid connectivity graph rel is not allowed.",
				ResourceFunctionUpdateVOTestExample.build().atSchemaLocation(null)
						.connectivity(List.of(ResourceGraphVOTestExample.build().atSchemaLocation(null).graphRelationship(
								List.of(ResourceGraphRelationshipVOTestExample.build().atSchemaLocation(null)
										.resourceGraph(ResourceGraphRefVOTestExample.build().atSchemaLocation(null))))))));
		testEntries.add(Arguments.of("An update with an non existent connectivity graph rel connection is not allowed.",
				ResourceFunctionUpdateVOTestExample.build().atSchemaLocation(null)
						.connectivity(List.of(ResourceGraphVOTestExample.build().atSchemaLocation(null).graphRelationship(
								List.of(ResourceGraphRelationshipVOTestExample.build().atSchemaLocation(null).resourceGraph(
										ResourceGraphRefVOTestExample.build().atSchemaLocation(null)
												.id("urn:ngsi-ld:graph:non-existent"))))))));

		return testEntries.stream();
	}

	@Disabled("Security is handled externally, thus 401 and 403 cannot happen.")
	@Test
	@Override
	public void patchResourceFunction401() throws Exception {

	}

	@Disabled("Security is handled externally, thus 401 and 403 cannot happen.")
	@Test
	@Override
	public void patchResourceFunction403() throws Exception {

	}

	@Test
	@Override
	public void patchResourceFunction404() throws Exception {
		ResourceFunctionUpdateVO resourceFunctionUpdateVO = ResourceFunctionUpdateVOTestExample.build()
				.atSchemaLocation(null)
				.resourceSpecification(null)
				.place(null);
		assertEquals(
				HttpStatus.NOT_FOUND,
				callAndCatch(() -> resourceFunctionApiTestClient.patchResourceFunction(null,
						"urn:ngsi-ld:resource-function:not-existent", resourceFunctionUpdateVO)).getStatus(),
				"Non existent resource functions should not be updated.");
	}

	@Disabled("Prohibited by the framework.")
	@Test
	@Override
	public void patchResourceFunction405() throws Exception {

	}

	@Override
	public void patchResourceFunction409() throws Exception {
		// TODO: can this happen?
	}

	@Override
	public void patchResourceFunction500() throws Exception {

	}

	@Test
	@Override
	public void retrieveResourceFunction200() throws Exception {

		ResourceFunctionCreateVO resourceFunctionCreateVO = ResourceFunctionCreateVOTestExample.build().atSchemaLocation(null).place(null)
				.resourceSpecification(null);
		HttpResponse<ResourceFunctionVO> createResponse = callAndCatch(
				() -> resourceFunctionApiTestClient.createResourceFunction(null, resourceFunctionCreateVO));
		assertEquals(HttpStatus.CREATED, createResponse.getStatus(),
				"The productSpecification should have been created first.");
		String id = createResponse.body().getId();

		ResourceFunctionVO expectedResourceFunctionVO = ResourceFunctionVOTestExample.build().atSchemaLocation(null);
		expectedResourceFunctionVO
				.id(id)
				.href(URI.create(id))
				.place(null)
				.resourceSpecification(null)
				.relatedParty(null);

		//then retrieve
		HttpResponse<ResourceFunctionVO> retrievedRF = callAndCatch(
				() -> resourceFunctionApiTestClient.retrieveResourceFunction(null, id, null));
		assertEquals(HttpStatus.OK, retrievedRF.getStatus(), "The retrieval should be ok.");
		assertEquals(expectedResourceFunctionVO, retrievedRF.body(),
				"The correct resource function should be returned.");
	}

	@Disabled("400 cannot happen, only 404")
	@Test
	@Override
	public void retrieveResourceFunction400() throws Exception {

	}

	@Disabled("Security is handled externally, thus 401 and 403 cannot happen.")
	@Test
	@Override
	public void retrieveResourceFunction401() throws Exception {

	}

	@Disabled("Security is handled externally, thus 401 and 403 cannot happen.")
	@Test
	@Override
	public void retrieveResourceFunction403() throws Exception {

	}

	@Test
	@Override
	public void retrieveResourceFunction404() throws Exception {
		HttpResponse<ResourceFunctionVO> response = callAndCatch(
				() -> resourceFunctionApiTestClient.retrieveResourceFunction(null,
						"urn:ngsi-ld:resource-function:non-existent", null));
		assertEquals(HttpStatus.NOT_FOUND, response.getStatus(), "No such resource-function should exist.");

		Optional<ErrorDetails> optionalErrorDetails = response.getBody(ErrorDetails.class);
		assertTrue(optionalErrorDetails.isPresent(), "Error details should have been provided.");
	}

	@Disabled("Prohibited by the framework.")
	@Test
	@Override
	public void retrieveResourceFunction405() throws Exception {

	}

	@Disabled("Conflict not possible on retrieval")
	@Test
	@Override
	public void retrieveResourceFunction409() throws Exception {

	}

	@Override
	public void retrieveResourceFunction500() throws Exception {

	}

	@Override
	protected String getEntityType() {
		return ResourceFunction.TYPE_RESOURCE_FUNCTION;
	}
}
