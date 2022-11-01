package org.fiware.tmforum.resourceinventory;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import lombok.RequiredArgsConstructor;
import org.fiware.ngsi.api.EntitiesApiClient;
import org.fiware.resourceinventory.api.ResourceApiTestClient;
import org.fiware.resourceinventory.api.ResourceApiTestSpec;
import org.fiware.resourceinventory.model.CharacteristicRelationshipVOTestExample;
import org.fiware.resourceinventory.model.CharacteristicVO;
import org.fiware.resourceinventory.model.CharacteristicVOTestExample;
import org.fiware.resourceinventory.model.FeatureRelationshipVOTestExample;
import org.fiware.resourceinventory.model.FeatureVO;
import org.fiware.resourceinventory.model.FeatureVOTestExample;
import org.fiware.resourceinventory.model.NoteVO;
import org.fiware.resourceinventory.model.NoteVOTestExample;
import org.fiware.resourceinventory.model.RelatedPartyVOTestExample;
import org.fiware.resourceinventory.model.RelatedPlaceRefOrValueVOTestExample;
import org.fiware.resourceinventory.model.ResourceCreateVO;
import org.fiware.resourceinventory.model.ResourceCreateVOTestExample;
import org.fiware.resourceinventory.model.ResourceOperationalStateTypeVO;
import org.fiware.resourceinventory.model.ResourceSpecificationRefVOTestExample;
import org.fiware.resourceinventory.model.ResourceStatusTypeVO;
import org.fiware.resourceinventory.model.ResourceUpdateVO;
import org.fiware.resourceinventory.model.ResourceUpdateVOTestExample;
import org.fiware.resourceinventory.model.ResourceUsageStateTypeVO;
import org.fiware.resourceinventory.model.ResourceVO;
import org.fiware.resourceinventory.model.ResourceVOTestExample;
import org.fiware.tmforum.common.configuration.GeneralProperties;
import org.fiware.tmforum.common.exception.ErrorDetails;
import org.fiware.tmforum.common.test.AbstractApiIT;
import org.fiware.tmforum.common.test.ArgumentPair;
import org.fiware.tmforum.resource.Resource;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.net.URI;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@MicronautTest(packages = { "org.fiware.tmforum.resourceinventory" })
public class ResourceApiIT extends AbstractApiIT implements ResourceApiTestSpec {

	public final ResourceApiTestClient resourceApiTestClient;

	private String message;
	private String fieldsParameter;
	private ResourceCreateVO resourceCreateVO;
	private ResourceUpdateVO resourceUpdateVO;
	private ResourceVO expectedResource;

	public ResourceApiIT(ResourceApiTestClient resourceApiTestClient, EntitiesApiClient entitiesApiClient,
			ObjectMapper objectMapper, GeneralProperties generalProperties) {
		super(entitiesApiClient, objectMapper, generalProperties);
		this.resourceApiTestClient = resourceApiTestClient;
	}

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
				() -> resourceApiTestClient.createResource(resourceCreateVO));
		assertEquals(HttpStatus.CREATED, resourceVOHttpResponse.getStatus(), message);
		String rfId = resourceVOHttpResponse.body().getId();
		expectedResource.setId(rfId);
		expectedResource.setHref(rfId);

		assertEquals(expectedResource, resourceVOHttpResponse.body(), message);
	}

	private static Stream<Arguments> provideValidResources() {
		List<Arguments> testEntries = new ArrayList<>();

		testEntries.add(
				Arguments.of("An empty resource should have been created.",
						ResourceCreateVOTestExample.build().place(null).resourceSpecification(null),
						ResourceVOTestExample.build().place(null).resourceSpecification(null)));

		Instant start = Instant.now();
		Instant end = Instant.now();
		testEntries.add(
				Arguments.of("A resource with operating times should have been created.",
						ResourceCreateVOTestExample.build().place(null).resourceSpecification(null)
								.startOperatingDate(start).endOperatingDate(end),
						ResourceVOTestExample.build().place(null).resourceSpecification(null).startOperatingDate(start)
								.endOperatingDate(end)));

		List<NoteVO> notes = List.of(NoteVOTestExample.build().id("note-1"), NoteVOTestExample.build().id("note-2"));
		testEntries.add(
				Arguments.of("A resource with notes should have been created.",
						ResourceCreateVOTestExample.build().place(null).resourceSpecification(null).note(notes),
						ResourceVOTestExample.build().place(null).resourceSpecification(null).note(notes)));

		provideValidFeatureLists()
				.map(ap ->
						Arguments.of(
								ap.message(),
								ResourceCreateVOTestExample.build().place(null).resourceSpecification(null)
										.activationFeature(ap.value()),
								ResourceVOTestExample.build().place(null).resourceSpecification(null)
										.activationFeature(ap.value()))
				).forEach(testEntries::add);

		provideValidCharacteristicLists()
				.map(ap -> Arguments.of(ap.message(),
						ResourceCreateVOTestExample.build().place(null).resourceSpecification(null)
								.resourceCharacteristic(ap.value()),
						ResourceVOTestExample.build().place(null).resourceSpecification(null)
								.resourceCharacteristic(ap.value())))
				.forEach(testEntries::add);

		return testEntries.stream();
	}

	private static Stream<ArgumentPair<List<FeatureVO>>> provideValidFeatureLists() {
		List<ArgumentPair<List<FeatureVO>>> featureArguments = new ArrayList<>();

		featureArguments.add(new ArgumentPair<>("A single feature without references should be valid.",
				List.of(FeatureVOTestExample.build().constraint(null).featureRelationship(null)
						.featureCharacteristic(null))));
		featureArguments.add(new ArgumentPair<>("Multiple features without references should be valid.",
				List.of(
						FeatureVOTestExample.build().id("f-1").constraint(null).featureRelationship(null)
								.featureCharacteristic(null),
						FeatureVOTestExample.build().id("f-2").constraint(null).featureRelationship(null)
								.featureCharacteristic(null))));
		featureArguments.add(new ArgumentPair<>("Features referencing should be valid.",
				List.of(
						FeatureVOTestExample.build().id("f-1").constraint(null).featureRelationship(null)
								.featureCharacteristic(null),
						FeatureVOTestExample.build().id("f-2").constraint(null).featureCharacteristic(null)
								.featureRelationship(
										List.of(FeatureRelationshipVOTestExample.build().validFor(null).id("f-1"))))));

		provideValidCharacteristicLists()
				.map(ap -> new ArgumentPair<>(String.format("Features should be valid - %s", ap.message()),
						List.of(FeatureVOTestExample.build().constraint(null).featureRelationship(null)
								.featureCharacteristic(ap.value()))))
				.forEach(featureArguments::add);

		return featureArguments.stream();
	}

	private static Stream<ArgumentPair<List<CharacteristicVO>>> provideValidCharacteristicLists() {
		List<ArgumentPair<List<CharacteristicVO>>> characteristicArguments = new ArrayList<>();

		characteristicArguments.add(new ArgumentPair<>("Single characteristics should be valid.",
				List.of(CharacteristicVOTestExample.build().characteristicRelationship(null))));
		characteristicArguments.add(new ArgumentPair<>("Mulitple characteristics should be valid.",
				List.of(CharacteristicVOTestExample.build().id("c-1").characteristicRelationship(null),
						CharacteristicVOTestExample.build().id("c-2").characteristicRelationship(null))));
		characteristicArguments.add(new ArgumentPair<>("Referencing characteristics should be valid.",
				List.of(CharacteristicVOTestExample.build().id("c-1").characteristicRelationship(null),
						CharacteristicVOTestExample.build().id("c-2")
								.characteristicRelationship(
										List.of(CharacteristicRelationshipVOTestExample.build().id("c-1"))))));
		return characteristicArguments.stream();
	}

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
				() -> resourceApiTestClient.createResource(resourceCreateVO));
		assertEquals(HttpStatus.BAD_REQUEST, creationResponse.getStatus(), message);
		Optional<ErrorDetails> optionalErrorDetails = creationResponse.getBody(ErrorDetails.class);
		assertTrue(optionalErrorDetails.isPresent(), "Error details should be provided.");
	}

	private static Stream<Arguments> provideInvalidResources() {
		List<Arguments> testEntries = new ArrayList<>();

		testEntries.add(Arguments.of("A resource with invalid related parties should not be created.",
				ResourceCreateVOTestExample.build().place(null).resourceSpecification(null)
						.relatedParty(List.of(RelatedPartyVOTestExample.build()))));
		testEntries.add(Arguments.of("A resource with non-existent related parties should not be created.",
				ResourceCreateVOTestExample.build().place(null).resourceSpecification(null).relatedParty(
						List.of((RelatedPartyVOTestExample.build().id("urn:ngsi-ld:organisation:non-existent"))))));

		testEntries.add(Arguments.of("A resource with an invalid place ref should not be created.",
				ResourceCreateVOTestExample.build().place(RelatedPlaceRefOrValueVOTestExample.build())
						.resourceSpecification(null)));
		testEntries.add(Arguments.of("A resource with non-existent place ref should not be created.",
				ResourceCreateVOTestExample.build()
						.place(RelatedPlaceRefOrValueVOTestExample.build().id("urn:ngsi-ld:place:non-existent"))
						.resourceSpecification(null)));

		testEntries.add(Arguments.of("A resource with an invalid resource ref should not be created.",
				ResourceCreateVOTestExample.build().place(null)
						.resourceSpecification(ResourceSpecificationRefVOTestExample.build())));
		testEntries.add(Arguments.of("A resource with non-existent resource ref should not be created.",
				ResourceCreateVOTestExample.build().place(null).resourceSpecification(
						ResourceSpecificationRefVOTestExample.build()
								.id("urn:ngsi-ld:resource-specification:non-existent"))));

		List<NoteVO> duplicateNoteVOS = List.of(NoteVOTestExample.build().id("note"),
				NoteVOTestExample.build().id("note"));
		testEntries.add(Arguments.of("A resource with duplicate note ids should not be created.",
				ResourceCreateVOTestExample.build().place(null).resourceSpecification(null).note(duplicateNoteVOS)));

		testEntries.add(Arguments.of("A resource with duplicate feature ids should not be created.",
				ResourceCreateVOTestExample.build().place(null).resourceSpecification(null)
						.activationFeature(List.of(FeatureVOTestExample.build().id("my-feature"),
								FeatureVOTestExample.build().id("my-feature")))));
		testEntries.add(Arguments.of("A resource with invalid feature references should not be created.",
				ResourceCreateVOTestExample.build().place(null).resourceSpecification(null)
						.activationFeature(List.of(
								FeatureVOTestExample.build().id("my-feature"),
								FeatureVOTestExample.build().featureRelationship(
										List.of(FeatureRelationshipVOTestExample.build().id("non-existent")))))));

		testEntries.add(Arguments.of("A resource with duplicate resource characteristic ids should not be created.",
				ResourceCreateVOTestExample.build().place(null).resourceSpecification(null)
						.resourceCharacteristic(List.of(CharacteristicVOTestExample.build().id("my-characteristic"),
								CharacteristicVOTestExample.build().id("my-characteristic")))));
		testEntries.add(Arguments.of("A resource with invalid feature references should not be created.",
				ResourceCreateVOTestExample.build().place(null).resourceSpecification(null)
						.resourceCharacteristic(List.of(
								CharacteristicVOTestExample.build().id("my-feature"),
								CharacteristicVOTestExample.build().characteristicRelationship(
										List.of(CharacteristicRelationshipVOTestExample.build()
												.id("non-existent")))))));

		testEntries.add(Arguments.of("A resource with duplicate feature characteristic ids should not be created.",
				ResourceCreateVOTestExample.build().place(null).resourceSpecification(null)
						.activationFeature(List.of(FeatureVOTestExample.build()
								.featureCharacteristic(
										List.of(CharacteristicVOTestExample.build().id("my-characteristic"),
												CharacteristicVOTestExample.build().id("my-characteristic")))))));
		testEntries.add(Arguments.of("A resource with invalid feature references should not be created.",
				ResourceCreateVOTestExample.build().place(null).resourceSpecification(null)
						.activationFeature(List.of(FeatureVOTestExample.build()
								.featureCharacteristic(
										List.of(CharacteristicVOTestExample.build().id("my-characteristic"),
												CharacteristicVOTestExample.build()
														.characteristicRelationship(
																List.of(CharacteristicRelationshipVOTestExample.build()
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
		ResourceCreateVO emptyCreate = ResourceCreateVOTestExample.build()
				.place(null)
				.resourceSpecification(null);

		HttpResponse<ResourceVO> createResponse = resourceApiTestClient.createResource(emptyCreate);
		assertEquals(HttpStatus.CREATED, createResponse.getStatus(), "The resource should have been created first.");

		String rfId = createResponse.body().getId();

		assertEquals(HttpStatus.NO_CONTENT,
				callAndCatch(() -> resourceApiTestClient.deleteResource(rfId)).getStatus(),
				"The resource should have been deleted.");

		assertEquals(HttpStatus.NOT_FOUND,
				callAndCatch(() -> resourceApiTestClient.retrieveResource(rfId, null)).status(),
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
				() -> resourceApiTestClient.deleteResource("urn:ngsi-ld:resource-catalog:no-pop"));
		assertEquals(HttpStatus.NOT_FOUND,
				notFoundResponse.getStatus(),
				"No such resource-catalog should exist.");

		Optional<ErrorDetails> optionalErrorDetails = notFoundResponse.getBody(ErrorDetails.class);
		assertTrue(optionalErrorDetails.isPresent(), "Error details should be provided.");

		notFoundResponse = callAndCatch(() -> resourceApiTestClient.deleteResource("invalid-id"));
		assertEquals(HttpStatus.NOT_FOUND,
				notFoundResponse.getStatus(),
				"No such resource-catalog should exist.");

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
			ResourceCreateVO resourceCreateVO = ResourceCreateVOTestExample.build()
					.place(null)
					.resourceSpecification(null);
			String id = resourceApiTestClient.createResource(resourceCreateVO)
					.body().getId();
			ResourceVO resourceVO = ResourceVOTestExample.build();
			resourceVO
					.id(id)
					.href(id)
					.place(null)
					.relatedParty(null)
					.resourceRelationship(null)
					.resourceSpecification(null);
			expectedResources.add(resourceVO);
		}

		HttpResponse<List<ResourceVO>> resourceResponse = callAndCatch(
				() -> resourceApiTestClient.listResource(null, null, null));

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
				() -> resourceApiTestClient.listResource(null, 0, limit));
		assertEquals(limit, firstPartResponse.body().size(),
				"Only the requested number of entries should be returend.");
		HttpResponse<List<ResourceVO>> secondPartResponse = callAndCatch(
				() -> resourceApiTestClient.listResource(null, 0 + limit, limit));
		assertEquals(limit, secondPartResponse.body().size(),
				"Only the requested number of entries should be returend.");

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
				() -> resourceApiTestClient.listResource(null, -1, null));
		assertEquals(HttpStatus.BAD_REQUEST,
				badRequestResponse.getStatus(),
				"Negative offsets are impossible.");

		Optional<ErrorDetails> optionalErrorDetails = badRequestResponse.getBody(ErrorDetails.class);
		assertTrue(optionalErrorDetails.isPresent(), "Error details should be provided.");

		badRequestResponse = callAndCatch(() -> resourceApiTestClient.listResource(null, null, -1));
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
		ResourceCreateVO resourceCreateVO = ResourceCreateVOTestExample.build().place(null).resourceSpecification(null);

		HttpResponse<ResourceVO> createResponse = callAndCatch(
				() -> resourceApiTestClient.createResource(resourceCreateVO));
		assertEquals(HttpStatus.CREATED, createResponse.getStatus(),
				"The resource function should have been created first.");

		String resourceId = createResponse.body().getId();

		HttpResponse<ResourceVO> updateResponse = callAndCatch(
				() -> resourceApiTestClient.patchResource(resourceId, resourceUpdateVO));
		assertEquals(HttpStatus.OK, updateResponse.getStatus(), message);

		ResourceVO updatedResource = updateResponse.body();
		expectedResource.href(resourceId).id(resourceId).relatedParty(null).resourceRelationship(null);

		assertEquals(expectedResource, updatedResource, message);
	}

	private static Stream<Arguments> provideResourceUpdates() {
		List<Arguments> testEntries = new ArrayList<>();

		testEntries.add(Arguments.of("The description should have been updated.",
				ResourceUpdateVOTestExample.build()
						.place(null)
						.resourceSpecification(null)
						.description("new-description"),
				ResourceVOTestExample.build()
						.place(null)
						.resourceSpecification(null)
						.description("new-description")));

		testEntries.add(Arguments.of("The name should have been updated.",
				ResourceUpdateVOTestExample.build()
						.place(null)
						.resourceSpecification(null)
						.name("new-name"),
				ResourceVOTestExample.build()
						.place(null)
						.resourceSpecification(null)
						.name("new-name")));

		testEntries.add(Arguments.of("The category should have been updated.",
				ResourceUpdateVOTestExample.build()
						.place(null)
						.resourceSpecification(null)
						.category("new-category"),
				ResourceVOTestExample.build()
						.place(null)
						.resourceSpecification(null)
						.category("new-category")));

		Instant date = Instant.now();
		testEntries.add(Arguments.of("The endOperatingDate should have been updated.",
				ResourceUpdateVOTestExample.build()
						.place(null)
						.resourceSpecification(null)
						.endOperatingDate(date),
				ResourceVOTestExample.build()
						.place(null)
						.resourceSpecification(null)
						.endOperatingDate(date)));

		testEntries.add(Arguments.of("The startOperatingDate should have been updated.",
				ResourceUpdateVOTestExample.build()
						.place(null)
						.resourceSpecification(null)
						.startOperatingDate(date),
				ResourceVOTestExample.build()
						.place(null)
						.resourceSpecification(null)
						.startOperatingDate(date)));

		testEntries.add(Arguments.of("The resourceVersion should have been updated.",
				ResourceUpdateVOTestExample.build()
						.place(null)
						.resourceSpecification(null)
						.resourceVersion("new-version"),
				ResourceVOTestExample.build()
						.place(null)
						.resourceSpecification(null)
						.resourceVersion("new-version")));

		List<NoteVO> notes = List.of(NoteVOTestExample.build().id("note-1"), NoteVOTestExample.build().id("note-2"));
		testEntries.add(Arguments.of("The notes should have been updated.",
				ResourceUpdateVOTestExample.build()
						.place(null)
						.resourceSpecification(null)
						.note(notes),
				ResourceVOTestExample.build()
						.place(null)
						.resourceSpecification(null)
						.note(notes)));

		provideValidCharacteristicLists()
				.map(ap -> Arguments.of(
						String.format("Resource characteristics should be updated - %s", ap.message()),
						ResourceUpdateVOTestExample.build()
								.place(null)
								.resourceSpecification(null)
								.resourceCharacteristic(ap.value()),
						ResourceVOTestExample.build()
								.place(null)
								.resourceSpecification(null)
								.resourceCharacteristic(ap.value())
				))
				.forEach(testEntries::add);

		provideValidFeatureLists()
				.map(ap -> Arguments.of(
						String.format("Activation feature should be updated - %s", ap.message()),
						ResourceUpdateVOTestExample.build()
								.place(null)
								.resourceSpecification(null)
								.activationFeature(ap.value()),
						ResourceVOTestExample.build()
								.place(null)
								.resourceSpecification(null)
								.activationFeature(ap.value())
				))
				.forEach(testEntries::add);

		testEntries.add(Arguments.of("The operational state should have been updated.",
				ResourceUpdateVOTestExample.build()
						.place(null)
						.resourceSpecification(null)
						.operationalState(ResourceOperationalStateTypeVO.DISABLE),
				ResourceVOTestExample.build()
						.place(null)
						.resourceSpecification(null)
						.operationalState(ResourceOperationalStateTypeVO.DISABLE)));

		testEntries.add(Arguments.of("The status should have been updated.",
				ResourceUpdateVOTestExample.build()
						.place(null)
						.resourceSpecification(null)
						.resourceStatus(ResourceStatusTypeVO.ALARM),
				ResourceVOTestExample.build()
						.place(null)
						.resourceSpecification(null)
						.resourceStatus(ResourceStatusTypeVO.ALARM)));

		testEntries.add(Arguments.of("The usageState should have been updated.",
				ResourceUpdateVOTestExample.build()
						.place(null)
						.resourceSpecification(null)
						.usageState(ResourceUsageStateTypeVO.IDLE),
				ResourceVOTestExample.build()
						.place(null)
						.resourceSpecification(null)
						.usageState(ResourceUsageStateTypeVO.IDLE)));

		testEntries.add(Arguments.of("The baseType should have been updated.",
				ResourceUpdateVOTestExample.build()
						.place(null)
						.resourceSpecification(null)
						.atBaseType("Resource"),
				ResourceVOTestExample.build()
						.place(null)
						.resourceSpecification(null)
						.atBaseType("Resource")));

		testEntries.add(Arguments.of("The baseType should have been updated.",
				ResourceUpdateVOTestExample.build()
						.place(null)
						.resourceSpecification(null)
						.atBaseType("Resource"),
				ResourceVOTestExample.build()
						.place(null)
						.resourceSpecification(null)
						.atBaseType("Resource")));

		testEntries.add(Arguments.of("The schemaLocation should have been updated.",
				ResourceUpdateVOTestExample.build()
						.place(null)
						.resourceSpecification(null)
						.atSchemaLocation(URI.create("/my-shema/location/Resource")),
				ResourceVOTestExample.build()
						.place(null)
						.resourceSpecification(null)
						.atSchemaLocation(URI.create("/my-shema/location/Resource"))));

		testEntries.add(Arguments.of("The type should have been updated.",
				ResourceUpdateVOTestExample.build()
						.place(null)
						.resourceSpecification(null)
						.atType("CloudResource"),
				ResourceVOTestExample.build()
						.place(null)
						.resourceSpecification(null)
						.atType("CloudResource")));

		return testEntries.stream();
	}

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
		ResourceCreateVO resourceCreateVO = ResourceCreateVOTestExample.build().place(null).resourceSpecification(null);

		HttpResponse<ResourceVO> createResponse = callAndCatch(
				() -> resourceApiTestClient.createResource(resourceCreateVO));
		assertEquals(HttpStatus.CREATED, createResponse.getStatus(),
				"The resource function should have been created first.");

		String resourceId = createResponse.body().getId();

		HttpResponse<ResourceVO> updateResponse = callAndCatch(
				() -> resourceApiTestClient.patchResource(resourceId, resourceUpdateVO));
		assertEquals(HttpStatus.BAD_REQUEST, updateResponse.getStatus(), message);

		Optional<ErrorDetails> optionalErrorDetails = updateResponse.getBody(ErrorDetails.class);
		assertTrue(optionalErrorDetails.isPresent(), "Some error details should be present.");
	}

	private static Stream<Arguments> provideInvalidUpdates() {
		List<Arguments> testEntries = new ArrayList<>();

		testEntries.add(Arguments.of("A resource with invalid related parties should not be created.",
				ResourceUpdateVOTestExample.build().place(null).resourceSpecification(null)
						.relatedParty(List.of(RelatedPartyVOTestExample.build()))));
		testEntries.add(Arguments.of("A resource with non-existent related parties should not be created.",
				ResourceUpdateVOTestExample.build().place(null).resourceSpecification(null).relatedParty(
						List.of((RelatedPartyVOTestExample.build().id("urn:ngsi-ld:organisation:non-existent"))))));

		testEntries.add(Arguments.of("A resource with an invalid place ref should not be created.",
				ResourceUpdateVOTestExample.build().place(RelatedPlaceRefOrValueVOTestExample.build())
						.resourceSpecification(null)));
		testEntries.add(Arguments.of("A resource with non-existent place ref should not be created.",
				ResourceUpdateVOTestExample.build()
						.place(RelatedPlaceRefOrValueVOTestExample.build().id("urn:ngsi-ld:place:non-existent"))
						.resourceSpecification(null)));

		testEntries.add(Arguments.of("A resource with an invalid resource ref should not be created.",
				ResourceUpdateVOTestExample.build().place(null)
						.resourceSpecification(ResourceSpecificationRefVOTestExample.build())));
		testEntries.add(Arguments.of("A resource with non-existent resource ref should not be created.",
				ResourceUpdateVOTestExample.build().place(null).resourceSpecification(
						ResourceSpecificationRefVOTestExample.build()
								.id("urn:ngsi-ld:resource-specification:non-existent"))));

		testEntries.add(Arguments.of("A resource with duplicate feature ids should not be created.",
				ResourceUpdateVOTestExample.build().place(null).resourceSpecification(null)
						.activationFeature(List.of(FeatureVOTestExample.build().id("my-feature"),
								FeatureVOTestExample.build().id("my-feature")))));
		testEntries.add(Arguments.of("A resource with invalid feature references should not be created.",
				ResourceUpdateVOTestExample.build().place(null).resourceSpecification(null)
						.activationFeature(List.of(
								FeatureVOTestExample.build().id("my-feature"),
								FeatureVOTestExample.build().featureRelationship(
										List.of(FeatureRelationshipVOTestExample.build().id("non-existent")))))));

		testEntries.add(Arguments.of("A resource with duplicate resource characteristic ids should not be created.",
				ResourceUpdateVOTestExample.build().place(null).resourceSpecification(null)
						.resourceCharacteristic(List.of(CharacteristicVOTestExample.build().id("my-characteristic"),
								CharacteristicVOTestExample.build().id("my-characteristic")))));
		testEntries.add(Arguments.of("A resource with invalid feature references should not be created.",
				ResourceUpdateVOTestExample.build().place(null).resourceSpecification(null)
						.resourceCharacteristic(List.of(
								CharacteristicVOTestExample.build().id("my-feature"),
								CharacteristicVOTestExample.build().characteristicRelationship(
										List.of(CharacteristicRelationshipVOTestExample.build()
												.id("non-existent")))))));

		testEntries.add(Arguments.of("A resource with duplicate feature characteristic ids should not be created.",
				ResourceUpdateVOTestExample.build().place(null).resourceSpecification(null)
						.activationFeature(List.of(FeatureVOTestExample.build()
								.featureCharacteristic(
										List.of(CharacteristicVOTestExample.build().id("my-characteristic"),
												CharacteristicVOTestExample.build().id("my-characteristic")))))));
		testEntries.add(Arguments.of("A resource with invalid feature references should not be created.",
				ResourceUpdateVOTestExample.build().place(null).resourceSpecification(null)
						.activationFeature(List.of(FeatureVOTestExample.build()
								.featureCharacteristic(
										List.of(CharacteristicVOTestExample.build().id("my-characteristic"),
												CharacteristicVOTestExample.build()
														.characteristicRelationship(
																List.of(CharacteristicRelationshipVOTestExample.build()
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
		ResourceUpdateVO resourceUpdateVO = ResourceUpdateVOTestExample.build();
		assertEquals(
				HttpStatus.NOT_FOUND,
				callAndCatch(() -> resourceApiTestClient.patchResource("urn:ngsi-ld:resource-catalog:not-existent",
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

		ResourceCreateVO resourceCreateVO = ResourceCreateVOTestExample.build()
				.place(null)
				.resourceSpecification(null);
		HttpResponse<ResourceVO> createResponse = callAndCatch(
				() -> resourceApiTestClient.createResource(resourceCreateVO));
		assertEquals(HttpStatus.CREATED, createResponse.getStatus(), message);
		String id = createResponse.body().getId();

		expectedResource
				.id(id)
				.href(id);

		//then retrieve
		HttpResponse<ResourceVO> retrievedRF = callAndCatch(
				() -> resourceApiTestClient.retrieveResource(id, fieldsParameter));
		assertEquals(HttpStatus.OK, retrievedRF.getStatus(), message);
		assertEquals(expectedResource, retrievedRF.body(), message);
	}

	private static Stream<Arguments> provideFieldParameters() {
		return Stream.of(
				Arguments.of("Without a fields parameter everything should be returned.", null,
						ResourceVOTestExample.build()
								// get nulled without values
								.relatedParty(null)
								.place(null)
								.resourceSpecification(null)
								.resourceRelationship(null)),
				Arguments.of("Only category and the mandatory parameters should have been included.", "category",
						ResourceVOTestExample.build()
								.relatedParty(null)
								.place(null)
								.resourceVersion(null)
								.resourceSpecification(null)
								.resourceCharacteristic(null)
								.resourceRelationship(null)
								.activationFeature(null)
								.description(null)
								.attachment(null)
								.note(null)
								.name(null)
								.atBaseType(null)
								.atSchemaLocation(null)
								.atType(null)),
				Arguments.of(
						"Only the mandatory parameters should have been included when a non-existent field was requested.",
						"nothingToSeeHere", ResourceVOTestExample.build()
								.relatedParty(null)
								.place(null)
								.category(null)
								.resourceVersion(null)
								.resourceSpecification(null)
								.resourceCharacteristic(null)
								.resourceRelationship(null)
								.activationFeature(null)
								.description(null)
								.attachment(null)
								.note(null)
								.name(null)
								.atBaseType(null)
								.atSchemaLocation(null)
								.atType(null)),
				Arguments.of("Only description, name and the mandatory parameters should have been included.",
						"name,description", ResourceVOTestExample.build()
								.relatedParty(null)
								.place(null)
								.resourceVersion(null)
								.resourceSpecification(null)
								.resourceCharacteristic(null)
								.resourceRelationship(null)
								.activationFeature(null)
								.category(null)
								.attachment(null)
								.note(null)
								.atBaseType(null)
								.atSchemaLocation(null)
								.atType(null)));
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
				() -> resourceApiTestClient.retrieveResource("urn:ngsi-ld:resource-function:non-existent", null));
		assertEquals(HttpStatus.NOT_FOUND, response.getStatus(), "No such resource-catalog should exist.");

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

	@Override protected String getEntityType() {
		return Resource.TYPE_RESOURCE;
	}
}
