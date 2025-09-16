package org.fiware.tmforum.usagemanagement;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import io.micronaut.test.annotation.MockBean;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import org.fiware.ngsi.api.EntitiesApiClient;
import org.fiware.tmforum.common.configuration.GeneralProperties;
import org.fiware.tmforum.common.exception.ErrorDetails;
import org.fiware.tmforum.common.notification.TMForumEventHandler;
import org.fiware.tmforum.common.test.AbstractApiIT;
import org.fiware.tmforum.usagemanagement.domain.UsageSpecification;
import org.fiware.usagemanagement.api.UsageSpecificationApiTestClient;
import org.fiware.usagemanagement.api.UsageSpecificationApiTestSpec;
import org.fiware.usagemanagement.model.*;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import reactor.core.publisher.Mono;

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
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@MicronautTest(packages = {"org.fiware.tmforum.usagemanagement"})
public class UsageSpecificationApiIT extends AbstractApiIT implements UsageSpecificationApiTestSpec {

	private final UsageSpecificationApiTestClient usageSpecificationApiTestClient;
	private String message;
	private String fields;
	private UsageSpecificationCreateVO usageSpecificationCreateVO;
	private UsageSpecificationUpdateVO usageSpecificationUpdateVO;
	private UsageSpecificationVO expectedUsageSpecification;

	public UsageSpecificationApiIT(UsageSpecificationApiTestClient usageSpecificationApiTestClient, EntitiesApiClient entitiesApiClient,
								   ObjectMapper objectMapper, GeneralProperties generalProperties) {
		super(entitiesApiClient, objectMapper, generalProperties);
		this.usageSpecificationApiTestClient = usageSpecificationApiTestClient;
	}

	@MockBean(TMForumEventHandler.class)
	public TMForumEventHandler eventHandler() {
		TMForumEventHandler eventHandler = mock(TMForumEventHandler.class);

		when(eventHandler.handleCreateEvent(any())).thenReturn(Mono.empty());
		when(eventHandler.handleUpdateEvent(any(), any())).thenReturn(Mono.empty());

		return eventHandler;
	}

	@Override
	protected String getEntityType() {
		return UsageSpecification.TYPE_USP;
	}

	@ParameterizedTest
	@MethodSource("provideValidUsageSpecifications")
	public void createUsageSpecification201(String message, UsageSpecificationCreateVO usageSpecificationCreateVO,
											UsageSpecificationVO expectedUsageSpecification) throws Exception {

		this.message = message;
		this.usageSpecificationCreateVO = usageSpecificationCreateVO;
		this.expectedUsageSpecification = expectedUsageSpecification;
		createUsageSpecification201();

	}

	@Override
	public void createUsageSpecification201() throws Exception {
		HttpResponse<UsageSpecificationVO> usageSpecificationVOHttpResponse = callAndCatch(
				() -> usageSpecificationApiTestClient.createUsageSpecification(null, usageSpecificationCreateVO));

		assertEquals(HttpStatus.CREATED, usageSpecificationVOHttpResponse.getStatus(), message);
		UsageSpecificationVO createdUsageSpecificationVO = usageSpecificationVOHttpResponse.body();
		String usageId = usageSpecificationVOHttpResponse.body().getId();

		expectedUsageSpecification.setId(usageId);
		expectedUsageSpecification.setHref(new URI(usageId));

		editExpectedTimePeriod(expectedUsageSpecification);
		assertEquals(expectedUsageSpecification, createdUsageSpecificationVO, message);
	}

	private static Stream<Arguments> provideValidUsageSpecifications() {
		List<Arguments> testEntries = new ArrayList<>();

		UsageSpecificationCreateVO usageSpecificationCreateVO = UsageSpecificationCreateVOTestExample.build()
				.atSchemaLocation(null)
				.validFor(null)
				.targetEntitySchema(null);
		UsageSpecificationVO expectedUsageSpecification = UsageSpecificationVOTestExample.build()
				.atSchemaLocation(null)
				.validFor(null)
				.targetEntitySchema(null);
		testEntries.add(
				Arguments.of("Empty usageSpecification should have been created.", usageSpecificationCreateVO, expectedUsageSpecification));

		TimePeriodVO timePeriodVO = TimePeriodVOTestExample.build().endDateTime(Instant.now())
				.startDateTime(Instant.now())
				.atSchemaLocation(null);
		usageSpecificationCreateVO = UsageSpecificationCreateVOTestExample.build()
				.atSchemaLocation(null)
				.targetEntitySchema(null)
				.validFor(timePeriodVO);
		expectedUsageSpecification = UsageSpecificationVOTestExample.build()
				.atSchemaLocation(null)
				.targetEntitySchema(null)
				.validFor(timePeriodVO);
		testEntries.add(
				Arguments.of("UsageSpecification with a validFor should have been created",
						usageSpecificationCreateVO,
						expectedUsageSpecification));

		return testEntries.stream();
	}

	private void editExpectedTimePeriod(UsageSpecificationVO expected) {
		if (expected.getValidFor() == null) {
			return;
		}
		TimePeriodVO expectedTimePeriod = expected.getValidFor();
		expectedTimePeriod.setId(null);
		expectedTimePeriod.setHref(null);
		expectedTimePeriod.atBaseType(null);
		expectedTimePeriod.atSchemaLocation(null);
		expectedTimePeriod.atType(null);
		expected.setValidFor(expectedTimePeriod);
	}

	@ParameterizedTest
	@MethodSource("provideInvalidUsageSpecifications") //para as funcións 400 teño que mirar de estar creando os is ben
	public void createUsageSpecification400(String message, UsageSpecificationCreateVO usageSpecificationCreateVO) throws Exception {
		this.message = message;
		this.usageSpecificationCreateVO = usageSpecificationCreateVO;
		createUsageSpecification400();
	}

	@Override
	public void createUsageSpecification400() throws Exception {
		HttpResponse<UsageSpecificationVO> usageSpecificationVOHttpResponse = callAndCatch(
				() -> usageSpecificationApiTestClient.createUsageSpecification(null, usageSpecificationCreateVO));
		assertEquals(HttpStatus.BAD_REQUEST, usageSpecificationVOHttpResponse.getStatus(), message);
		Optional<ErrorDetails> optionalErrorDetails = usageSpecificationVOHttpResponse.getBody(ErrorDetails.class);
		assertTrue(optionalErrorDetails.isPresent(), "Error details should be provided.");
	}

	private static Stream<Arguments> provideInvalidUsageSpecifications() {
		List<Arguments> testEntries = new ArrayList<>();

		testEntries.add(Arguments.of("A service catalog with invalid related parties should not be created.",
				UsageSpecificationCreateVOTestExample.build().atSchemaLocation(null).relatedParty(List.of(RelatedPartyVOTestExample.build().atSchemaLocation(null)))));
		testEntries.add(Arguments.of("A service catalog with non-existent related parties should not be created.",
				UsageSpecificationCreateVOTestExample.build().atSchemaLocation(null).relatedParty(
						List.of((RelatedPartyVOTestExample.build().atSchemaLocation(null).id("urn:ngsi-ld:organisation:non-existent"))))));

		testEntries.add(Arguments.of("A service catalog with an invalid constraint should not be created.",
				UsageSpecificationCreateVOTestExample.build().atSchemaLocation(null).constraint(List.of(ConstraintRefVOTestExample.build().atSchemaLocation(null)))));
		testEntries.add(Arguments.of("A service catalog with a non-existent constraint should not be created.",
				UsageSpecificationCreateVOTestExample.build().atSchemaLocation(null).constraint(
						List.of(ConstraintRefVOTestExample.build().atSchemaLocation(null).id("urn:ngsi-ld:usageSpecification:non-existent")))));

		testEntries.add(Arguments.of("A service catalog with an invalid entity specification should not be created.",
				UsageSpecificationCreateVOTestExample.build().atSchemaLocation(null).entitySpecRelationship(List.of(EntitySpecificationRelationshipVOTestExample.build().atSchemaLocation(null)))));
		testEntries.add(Arguments.of("A service catalog with a non-existent entity specification should not be created.",
				UsageSpecificationCreateVOTestExample.build().atSchemaLocation(null).entitySpecRelationship(
						List.of(EntitySpecificationRelationshipVOTestExample.build().atSchemaLocation(null).id("urn:ngsi-ld:usageSpecification:non-existent")))));

		return testEntries.stream();
	}

	@Disabled
	@Test
	@Override
	public void createUsageSpecification401() throws Exception {
		throw new UnsupportedOperationException("Unimplemented method 'createUsageSpecification401'");
	}

	@Disabled
	@Test
	@Override
	public void createUsageSpecification403() throws Exception {
		throw new UnsupportedOperationException("Unimplemented method 'createUsageSpecification403'");
	}

	@Disabled
	@Test
	@Override
	public void createUsageSpecification405() throws Exception {
		throw new UnsupportedOperationException("Unimplemented method 'createUsageSpecification405'");
	}

	@Disabled
	@Test
	@Override
	public void createUsageSpecification409() throws Exception {
		throw new UnsupportedOperationException("Unimplemented method 'createUsageSpecification409'");
	}

	@Override
	public void createUsageSpecification500() throws Exception {

	}

	@Test
	@Override
	public void deleteUsageSpecification204() throws Exception {
		UsageSpecificationCreateVO usageSpecificationCreateVO = UsageSpecificationCreateVOTestExample.build()
				.atSchemaLocation(null)
				.validFor(null)
				.targetEntitySchema(null);
		HttpResponse<UsageSpecificationVO> usageSpecificationCreateResponse = callAndCatch(
				() -> usageSpecificationApiTestClient.createUsageSpecification(null, usageSpecificationCreateVO));
		assertEquals(HttpStatus.CREATED, usageSpecificationCreateResponse.getStatus(),
				"The Usage should have been created first.");

		String usageSpecificationId = usageSpecificationCreateResponse.body().getId();

		assertEquals(HttpStatus.NO_CONTENT,
				callAndCatch(() -> usageSpecificationApiTestClient.deleteUsageSpecification(null, usageSpecificationId)).getStatus(),
				"The Usage should have been deleted.");

		assertEquals(HttpStatus.NOT_FOUND,
				callAndCatch(() -> usageSpecificationApiTestClient.retrieveUsageSpecification(null, usageSpecificationId, null)).status(),
				"The Usage should not exist anymore.");

	}

	@Disabled
	@Override
	public void deleteUsageSpecification400() throws Exception {

	}

	@Disabled
	@Override
	public void deleteUsageSpecification401() throws Exception {

	}

	@Disabled
	@Override
	public void deleteUsageSpecification403() throws Exception {

	}

	@Test
	@Override
	public void deleteUsageSpecification404() throws Exception {
		HttpResponse<?> notFoundResponse = callAndCatch(
				() -> usageSpecificationApiTestClient.deleteUsageSpecification(null, "urn:ngsi-ld:usageSpecification:no-usage"));
		assertEquals(HttpStatus.NOT_FOUND,
				notFoundResponse.getStatus(),
				"No such usage should exist.");

		Optional<ErrorDetails> optionalErrorDetails = notFoundResponse.getBody(ErrorDetails.class);
		assertTrue(optionalErrorDetails.isPresent(), "Error details should be provided.");

		notFoundResponse = callAndCatch(() -> usageSpecificationApiTestClient.deleteUsageSpecification(null, "invalid-id"));
		assertEquals(HttpStatus.NOT_FOUND,
				notFoundResponse.getStatus(),
				"No such usage should exist.");

		optionalErrorDetails = notFoundResponse.getBody(ErrorDetails.class);
		assertTrue(optionalErrorDetails.isPresent(), "Error details should be provided.");
	}

	@Disabled
	@Override
	public void deleteUsageSpecification405() throws Exception {

	}

	@Disabled
	@Override
	public void deleteUsageSpecification409() throws Exception {

	}

	@Disabled
	@Override
	public void deleteUsageSpecification500() throws Exception {

	}

	@Test
	@Override
	public void listUsageSpecification200() throws Exception {

		List<UsageSpecificationVO> expectedUsageSpecifications = new ArrayList<>();
		for (int i = 0; i < 10; i++) {
			UsageSpecificationCreateVO usageSpecificationCreateVO = UsageSpecificationCreateVOTestExample.build()
					.atSchemaLocation(null)
					.validFor(null)
					.targetEntitySchema(null);
			String id = usageSpecificationApiTestClient.createUsageSpecification(null, usageSpecificationCreateVO).body().getId();
			UsageSpecificationVO usageSpecificationVO = UsageSpecificationVOTestExample.build()
					.atSchemaLocation(null)
					.validFor(null)
					.relatedParty(null)
					.targetEntitySchema(null)
					.id(id)
					.href(new URI(id));
			editExpectedTimePeriod(usageSpecificationVO);
			expectedUsageSpecifications.add(usageSpecificationVO);
		}

		HttpResponse<List<UsageSpecificationVO>> usageSpecificationResponse = callAndCatch(
				() -> usageSpecificationApiTestClient.listUsageSpecification(null, null, null, null));
		assertEquals(HttpStatus.OK, usageSpecificationResponse.getStatus(), "The list should be accessible.");
		assertEquals(expectedUsageSpecifications.size(), usageSpecificationResponse.getBody().get().size(),
				"All usageSpecifications should have been returned.");

		List<UsageSpecificationVO> retrievedUsageSpecifications = usageSpecificationResponse.getBody().get();

		Map<String, UsageSpecificationVO> retrievedMap = retrievedUsageSpecifications.stream()
				.collect(Collectors.toMap((usageSpecification) -> usageSpecification.getId(), usageSpecification -> usageSpecification));

		expectedUsageSpecifications.stream()
				.forEach(expectedUsageSpecification -> assertTrue(retrievedMap.containsKey(expectedUsageSpecification.getId()),
						String.format("All created usageSpecifications should be returned - Missing: %s.", expectedUsageSpecification,
								retrievedUsageSpecifications)));

		expectedUsageSpecifications.stream().forEach(
				expectedUsageSpecification -> assertEquals(expectedUsageSpecification, retrievedMap.get(expectedUsageSpecification.getId()),
						"The correct usageSpecifications should be retrieved."));

		// get with pagination
		Integer limit = 5;
		HttpResponse<List<UsageSpecificationVO>> firstPartResponse = callAndCatch(
				() -> usageSpecificationApiTestClient.listUsageSpecification(null, null, 0, limit));
		assertEquals(limit, firstPartResponse.body().size(),
				"Only the requested number of entries should be returned.");
		HttpResponse<List<UsageSpecificationVO>> secondPartResponse = callAndCatch(
				() -> usageSpecificationApiTestClient.listUsageSpecification(null, null, 0 + limit, limit));
		assertEquals(limit, secondPartResponse.body().size(),
				"Only the requested number of entries should be returned.");

		retrievedUsageSpecifications.clear();
		retrievedUsageSpecifications.addAll(firstPartResponse.body());
		retrievedUsageSpecifications.addAll(secondPartResponse.body());
		expectedUsageSpecifications.stream()
				.forEach(expectedUsage -> assertTrue(retrievedMap.containsKey(expectedUsage.getId()),
						String.format("All created usageSpecification should be returned - Missing: %s.", expectedUsage)));
		expectedUsageSpecifications.stream().forEach(
				expectedUsageSpecification -> assertEquals(expectedUsageSpecification, retrievedMap.get(expectedUsageSpecification.getId()),
						"The correct usageSpecification should be retrieved."));

	}

	@Test
	@Override
	public void listUsageSpecification400() throws Exception {
		HttpResponse<List<UsageSpecificationVO>> badRequestResponse = callAndCatch(
				() -> usageSpecificationApiTestClient.listUsageSpecification(null, null, -1, null));
		assertEquals(HttpStatus.BAD_REQUEST,
				badRequestResponse.getStatus(),
				"Negative offsets are impossible.");

		Optional<ErrorDetails> optionalErrorDetails = badRequestResponse.getBody(ErrorDetails.class);
		assertTrue(optionalErrorDetails.isPresent(), "Error details should be provided.");

		badRequestResponse = callAndCatch(() -> usageSpecificationApiTestClient.listUsageSpecification(null, null, null, -1));
		assertEquals(HttpStatus.BAD_REQUEST,
				badRequestResponse.getStatus(),
				"Negative limits are impossible.");
		optionalErrorDetails = badRequestResponse.getBody(ErrorDetails.class);
		assertTrue(optionalErrorDetails.isPresent(), "Error details should be provided.");
	}

	@Disabled
	@Override
	public void listUsageSpecification401() throws Exception {

	}

	@Disabled
	@Override
	public void listUsageSpecification403() throws Exception {

	}

	@Disabled
	@Override
	public void listUsageSpecification404() throws Exception {

	}

	@Disabled
	@Override
	public void listUsageSpecification405() throws Exception {

	}

	@Disabled
	@Override
	public void listUsageSpecification409() throws Exception {

	}

	@Override
	public void listUsageSpecification500() throws Exception {

	}

	@ParameterizedTest
	@MethodSource("provideUsageSpecificationUpdates")
	public void patchUsageSpecification200(String message, UsageSpecificationUpdateVO usageSpecificationUpdateVO, UsageSpecificationVO expectedUsageSpecification)
			throws Exception {
		this.message = message;
		this.usageSpecificationUpdateVO = usageSpecificationUpdateVO;
		this.expectedUsageSpecification = expectedUsageSpecification;
		patchUsageSpecification200();
	}

	@Override
	public void patchUsageSpecification200() throws Exception {
		//first create
		UsageSpecificationCreateVO usageSpecificationCreateVO = UsageSpecificationCreateVOTestExample.build()
				.atSchemaLocation(null)
				.validFor(null)
				.targetEntitySchema(null);
		HttpResponse<UsageSpecificationVO> createResponse = callAndCatch(
				() -> usageSpecificationApiTestClient.createUsageSpecification(null, usageSpecificationCreateVO));
		assertEquals(HttpStatus.CREATED, createResponse.getStatus(), "The usage specification should have been created first.");

		String usageId = createResponse.body().getId();

		HttpResponse<UsageSpecificationVO> updateResponse = callAndCatch(
				() -> usageSpecificationApiTestClient.patchUsageSpecification(null, usageId, usageSpecificationUpdateVO));
		assertEquals(HttpStatus.OK, updateResponse.getStatus(), message);

		UsageSpecificationVO updatedUsageSpecification = updateResponse.body();
		expectedUsageSpecification.setId(usageId);
		expectedUsageSpecification.setHref(new URI(usageId));
		editExpectedTimePeriod(expectedUsageSpecification);


		assertEquals(expectedUsageSpecification, updatedUsageSpecification, message);
	}

	private static Stream<Arguments> provideUsageSpecificationUpdates() {

		List<Arguments> testEntries = new ArrayList<>();

		UsageSpecificationUpdateVO newNameUsage = UsageSpecificationUpdateVOTestExample.build()
				.atSchemaLocation(null)
				.validFor(null)
				.targetEntitySchema(null);
		newNameUsage.setName("New-Name");
		UsageSpecificationVO expectedNewName = UsageSpecificationVOTestExample.build()
				.atSchemaLocation(null)
				.relatedParty(null)
				.validFor(null)
				.targetEntitySchema(null);
		expectedNewName.setName("New-Name");
		testEntries.add(Arguments.of("The type should have been updated.", newNameUsage, expectedNewName));

		UsageSpecificationUpdateVO newDesc = UsageSpecificationUpdateVOTestExample.build()
				.atSchemaLocation(null)
				.validFor(null)
				.targetEntitySchema(null);
		newDesc.setDescription("New description");
		UsageSpecificationVO expectedNewDesc = UsageSpecificationVOTestExample.build()
				.atSchemaLocation(null)
				.relatedParty(null)
				.validFor(null)
				.targetEntitySchema(null);
		expectedNewDesc.setDescription("New description");
		testEntries.add(Arguments.of("The description should have been updated.", newDesc, expectedNewDesc));

		UsageSpecificationUpdateVO newLife = UsageSpecificationUpdateVOTestExample.build()
				.atSchemaLocation(null)
				.validFor(null)
				.targetEntitySchema(null);
		newLife.lifecycleStatus("New life");
		UsageSpecificationVO expectedNewLife = UsageSpecificationVOTestExample.build()
				.atSchemaLocation(null)
				.relatedParty(null)
				.validFor(null)
				.targetEntitySchema(null);
		expectedNewLife.lifecycleStatus("New life");
		testEntries.add(Arguments.of("The life cycle status should have been updated.", newLife, expectedNewLife));

		UsageSpecificationUpdateVO versionUpdate = UsageSpecificationUpdateVOTestExample.build()
				.atSchemaLocation(null)
				.validFor(null)
				.targetEntitySchema(null)
				.version("v0.0.2");
		UsageSpecificationVO expectedVersionUpdate = UsageSpecificationVOTestExample.build()
				.atSchemaLocation(null)
				.relatedParty(null)
				.validFor(null)
				.targetEntitySchema(null)
				.version("v0.0.2");
		testEntries.add(Arguments.of("The version should have been updated.", versionUpdate, expectedVersionUpdate));


		return testEntries.stream();
	}

	@ParameterizedTest
	@MethodSource("provideInvalidUpdates")
	public void patchUsageSpecification400(String message, UsageSpecificationUpdateVO usageSpecificationUpdateVO) throws Exception {
		this.message = message;
		this.usageSpecificationUpdateVO = usageSpecificationUpdateVO;
		patchUsageSpecification400();
	}

	@Override
	public void patchUsageSpecification400() throws Exception {
		//first create
		UsageSpecificationCreateVO usageSpecificationCreateVO = UsageSpecificationCreateVOTestExample.build()
				.atSchemaLocation(null)
				.validFor(null)
				.targetEntitySchema(null);
		HttpResponse<UsageSpecificationVO> createResponse = callAndCatch(
				() -> usageSpecificationApiTestClient.createUsageSpecification(null, usageSpecificationCreateVO));
		assertEquals(HttpStatus.CREATED, createResponse.getStatus(), "The usage should have been created first.");

		String usageId = createResponse.body().getId();

		HttpResponse<UsageSpecificationVO> updateResponse = callAndCatch(
				() -> usageSpecificationApiTestClient.patchUsageSpecification(null, usageId, usageSpecificationUpdateVO));
		assertEquals(HttpStatus.BAD_REQUEST, updateResponse.getStatus(), message);

		Optional<ErrorDetails> optionalErrorDetails = updateResponse.getBody(ErrorDetails.class);
		assertTrue(optionalErrorDetails.isPresent(), "Some error details should be present.");
	}

	private static Stream<Arguments> provideInvalidUpdates() {

		List<Arguments> testEntries = new ArrayList<>();

		UsageSpecificationUpdateVO invalidRelatedPartyUpdate = UsageSpecificationUpdateVOTestExample.build().atSchemaLocation(null);
		// no valid id
		RelatedPartyVO invalidRelatedParty = RelatedPartyVOTestExample.build().atSchemaLocation(null);
		invalidRelatedPartyUpdate.setRelatedParty(List.of(invalidRelatedParty));
		testEntries.add(Arguments.of("A usage with invalid related parties should not be Updated.",
				invalidRelatedPartyUpdate));

		UsageSpecificationUpdateVO nonExistentRelatedPartyUpdate = UsageSpecificationUpdateVOTestExample.build().atSchemaLocation(null);
		// no existent id
		RelatedPartyVO nonExistentRelatedParty = RelatedPartyVOTestExample.build().atSchemaLocation(null);
		nonExistentRelatedParty.setId("urn:ngsi-ld:usage:non-existent");
		nonExistentRelatedPartyUpdate.setRelatedParty(List.of(nonExistentRelatedParty));
		testEntries.add(Arguments.of("A usage with non-existent related parties should not be Updated.",
				nonExistentRelatedPartyUpdate));

		UsageSpecificationUpdateVO invalidConstraintRefUpdate = UsageSpecificationUpdateVOTestExample.build().atSchemaLocation(null);
		// no valid id
		ConstraintRefVO invalidConstraintRef = ConstraintRefVOTestExample.build().atSchemaLocation(null);
		invalidConstraintRefUpdate.setConstraint(List.of(invalidConstraintRef));
		testEntries.add(Arguments.of("A usage with invalid constraint should not be Updated.",
				invalidConstraintRefUpdate));

		UsageSpecificationUpdateVO nonExistentConstraintUpdate = UsageSpecificationUpdateVOTestExample.build().atSchemaLocation(null);
		// no existent id
		ConstraintRefVO nonExistentConstraintRef = ConstraintRefVOTestExample.build().atSchemaLocation(null);
		nonExistentConstraintRef.setId("urn:ngsi-ld:usage:non-existent");
		nonExistentConstraintUpdate.setConstraint(List.of(nonExistentConstraintRef));
		testEntries.add(Arguments.of("A usage with non-existent constraint should not be Updated.",
				nonExistentConstraintUpdate));

		UsageSpecificationUpdateVO invalidEntitySpecificationRelationshipUpdate = UsageSpecificationUpdateVOTestExample.build().atSchemaLocation(null);
		// no valid id
		EntitySpecificationRelationshipVO invalidEntitySpecificationRelationship = EntitySpecificationRelationshipVOTestExample.build().atSchemaLocation(null);
		invalidEntitySpecificationRelationshipUpdate.setEntitySpecRelationship(List.of(invalidEntitySpecificationRelationship));
		testEntries.add(Arguments.of("A usage with invalid entity spec should not be Updated.",
				invalidEntitySpecificationRelationshipUpdate));

		UsageSpecificationUpdateVO nonEntitySpecificationRelationshipUpdate = UsageSpecificationUpdateVOTestExample.build().atSchemaLocation(null);
		// no existent id
		EntitySpecificationRelationshipVO nonExistentEntitySpecificationRelationship = EntitySpecificationRelationshipVOTestExample.build().atSchemaLocation(null);
		nonExistentEntitySpecificationRelationship.setId("urn:ngsi-ld:usage:non-existent");
		nonEntitySpecificationRelationshipUpdate.setEntitySpecRelationship(List.of(nonExistentEntitySpecificationRelationship));
		testEntries.add(Arguments.of("A usage with non-existent entity spec should not be Updated.",
				nonEntitySpecificationRelationshipUpdate));

		return testEntries.stream();
	}

	@Disabled
	@Override
	public void patchUsageSpecification401() throws Exception {

	}

	@Disabled
	@Override
	public void patchUsageSpecification403() throws Exception {

	}

	@Test
	@Override
	public void patchUsageSpecification404() throws Exception {
		UsageSpecificationUpdateVO usageSpecificationUpdateVO = UsageSpecificationUpdateVOTestExample.build()
				.atSchemaLocation(null)
				.validFor(null)
				.targetEntitySchema(null);
		assertEquals(
				HttpStatus.NOT_FOUND,
				callAndCatch(() -> usageSpecificationApiTestClient.patchUsageSpecification(null, "urn:ngsi-ld:usage:not-existent",
						usageSpecificationUpdateVO)).getStatus(),
				"Non existent usages should not be updated.");
	}

	@Disabled
	@Override
	public void patchUsageSpecification405() throws Exception {

	}

	@Disabled
	@Override
	public void patchUsageSpecification409() throws Exception {

	}

	@Override
	public void patchUsageSpecification500() throws Exception {

	}

	@Test
	@Override
	public void retrieveUsageSpecification200() throws Exception {
		//first create
		UsageSpecificationCreateVO usageSpecificationCreateVO = UsageSpecificationCreateVOTestExample.build()
				.atSchemaLocation(null)
				.validFor(null)
				.targetEntitySchema(null);
		HttpResponse<UsageSpecificationVO> createResponse = callAndCatch(
				() -> usageSpecificationApiTestClient.createUsageSpecification(null, usageSpecificationCreateVO));
		assertEquals(HttpStatus.CREATED, createResponse.getStatus(), "The usage should have been created first.");
		String id = createResponse.body().getId();

		UsageSpecificationVO expectedUsageSpecification = UsageSpecificationVOTestExample.build()
				.atSchemaLocation(null)
				.id(id)
				.relatedParty(null)
				.href(new URI(id))
				.validFor(null)
				.targetEntitySchema(null);
		editExpectedTimePeriod(expectedUsageSpecification);

		//then retrieve
		HttpResponse<UsageSpecificationVO> retrievedUsageSpecification = callAndCatch(() -> usageSpecificationApiTestClient.retrieveUsageSpecification(null, id, null));
		assertEquals(HttpStatus.OK, retrievedUsageSpecification.getStatus(), "The retrieval should be ok.");
		assertEquals(expectedUsageSpecification, retrievedUsageSpecification.body(), "The correct usage should be returned.");
	}

	@Disabled
	@Override
	public void retrieveUsageSpecification400() throws Exception {

	}

	@Disabled
	@Override
	public void retrieveUsageSpecification401() throws Exception {

	}

	@Disabled
	@Override
	public void retrieveUsageSpecification403() throws Exception {

	}

	@Test
	@Override
	public void retrieveUsageSpecification404() throws Exception {
		HttpResponse<UsageSpecificationVO> response = callAndCatch(
				() -> usageSpecificationApiTestClient.retrieveUsageSpecification(null, "urn:ngsi-ld:usage:non-existent", null));
		assertEquals(HttpStatus.NOT_FOUND, response.getStatus(), "No such usage should exist.");

		Optional<ErrorDetails> optionalErrorDetails = response.getBody(ErrorDetails.class);
		assertTrue(optionalErrorDetails.isPresent(), "Error details should have been provided.");
	}

	@Disabled
	@Override
	public void retrieveUsageSpecification405() throws Exception {

	}

	@Disabled
	@Override
	public void retrieveUsageSpecification409() throws Exception {

	}

	@Override
	public void retrieveUsageSpecification500() throws Exception {

	}


}

