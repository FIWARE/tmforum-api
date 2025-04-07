package org.fiware.tmforum.servicecatalog;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import io.micronaut.test.annotation.MockBean;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import org.fiware.ngsi.api.EntitiesApiClient;
import org.fiware.servicecatalog.api.ServiceCategoryApiTestClient;
import org.fiware.servicecatalog.api.ServiceCategoryApiTestSpec;
import org.fiware.servicecatalog.model.*;
import org.fiware.tmforum.common.configuration.GeneralProperties;
import org.fiware.tmforum.common.exception.ErrorDetails;
import org.fiware.tmforum.common.notification.TMForumEventHandler;
import org.fiware.tmforum.common.test.AbstractApiIT;
import org.fiware.tmforum.service.ServiceCategory;
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

@MicronautTest(packages = {"org.fiware.tmforum.servicecatalog"})
public class ServiceCategoryApiIT extends AbstractApiIT implements ServiceCategoryApiTestSpec {

	public final ServiceCategoryApiTestClient serviceCategoryApiTestClient;

	private String message;
	private String fieldsParameter;
	private ServiceCategoryCreateVO serviceCategoryCreateVO;
	private ServiceCategoryUpdateVO serviceCategoryUpdateVO;
	private ServiceCategoryVO expectedServiceCategory;

	private Clock clock = mock(Clock.class);

	public ServiceCategoryApiIT(ServiceCategoryApiTestClient serviceCategoryApiTestClient,
								EntitiesApiClient entitiesApiClient, ObjectMapper objectMapper, GeneralProperties generalProperties) {
		super(entitiesApiClient, objectMapper, generalProperties);
		this.serviceCategoryApiTestClient = serviceCategoryApiTestClient;
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
	@MethodSource("provideValidServiceCategorys")
	public void createServiceCategory201(String message, ServiceCategoryCreateVO serviceCategoryCreateVO,
										 ServiceCategoryVO expectedServiceCategory) throws Exception {
		this.message = message;
		this.serviceCategoryCreateVO = serviceCategoryCreateVO;
		this.expectedServiceCategory = expectedServiceCategory;
		createServiceCategory201();
	}

	@Override
	public void createServiceCategory201() throws Exception {

		Instant currentTimeInstant = Instant.ofEpochSecond(10000);
		when(clock.instant()).thenReturn(currentTimeInstant);

		HttpResponse<ServiceCategoryVO> serviceCategoryVOHttpResponse = callAndCatch(
				() -> serviceCategoryApiTestClient.createServiceCategory(null, serviceCategoryCreateVO));
		assertEquals(HttpStatus.CREATED, serviceCategoryVOHttpResponse.getStatus(), message);
		String rfId = serviceCategoryVOHttpResponse.body().getId();
		expectedServiceCategory.id(rfId)
				.href(URI.create(rfId))
				.lastUpdate(currentTimeInstant);

		assertEquals(expectedServiceCategory, serviceCategoryVOHttpResponse.body(), message);
	}

	private static Stream<Arguments> provideValidServiceCategorys() {
		List<Arguments> testEntries = new ArrayList<>();

		ServiceCategoryCreateVO emptyCreate = ServiceCategoryCreateVOTestExample.build().atSchemaLocation(null).lifecycleStatus("created")
				.parentId(null);
		ServiceCategoryVO expectedEmpty = ServiceCategoryVOTestExample.build().atSchemaLocation(null).lifecycleStatus("created")
				.parentId(null);
		testEntries.add(
				Arguments.of("An empty service category should have been created.", emptyCreate, expectedEmpty));

		TimePeriodVO timePeriodVO = TimePeriodVOTestExample.build().endDateTime(Instant.now())
				.startDateTime(Instant.now());
		ServiceCategoryCreateVO createValidFor = ServiceCategoryCreateVOTestExample.build().atSchemaLocation(null).validFor(timePeriodVO)
				.lifecycleStatus("created").parentId(null);
		ServiceCategoryVO expectedValidFor = ServiceCategoryVOTestExample.build().atSchemaLocation(null).validFor(timePeriodVO)
				.lifecycleStatus("created").parentId(null);
		testEntries.add(Arguments.of("An service category with a validFor should have been created.", createValidFor,
				expectedValidFor));

		return testEntries.stream();
	}

	@ParameterizedTest
	@MethodSource("provideInvalidServiceCategorys")
	public void createServiceCategory400(String message, ServiceCategoryCreateVO invalidCreateVO) throws Exception {
		this.message = message;
		this.serviceCategoryCreateVO = invalidCreateVO;
		createServiceCategory400();
	}

	@Override
	public void createServiceCategory400() throws Exception {
		HttpResponse<ServiceCategoryVO> creationResponse = callAndCatch(
				() -> serviceCategoryApiTestClient.createServiceCategory(null, serviceCategoryCreateVO));
		assertEquals(HttpStatus.BAD_REQUEST, creationResponse.getStatus(), message);
		Optional<ErrorDetails> optionalErrorDetails = creationResponse.getBody(ErrorDetails.class);
		assertTrue(optionalErrorDetails.isPresent(), "Error details should be provided.");
	}

	private static Stream<Arguments> provideInvalidServiceCategorys() {
		List<Arguments> testEntries = new ArrayList<>();

		testEntries.add(Arguments.of("A service category with an invalid parent category should not be created.",
				ServiceCategoryCreateVOTestExample.build().atSchemaLocation(null).parentId("my-invalid-id")));
		testEntries.add(Arguments.of("A service category with a non-existent parent category should not be created.",
				ServiceCategoryCreateVOTestExample.build().atSchemaLocation(null).parentId("urn:ngsi-ld:service-category:non-existent")));

		testEntries.add(Arguments.of("A service category with an invalid service category should not be created.",
				ServiceCategoryCreateVOTestExample.build().atSchemaLocation(null).parentId(null)
						.category(List.of(ServiceCategoryRefVOTestExample.build().atSchemaLocation(null)))));
		testEntries.add(Arguments.of("A service category with a non-existent service category should not be created.",
				ServiceCategoryCreateVOTestExample.build().atSchemaLocation(null).parentId(null).category(
						List.of(ServiceCategoryRefVOTestExample.build().atSchemaLocation(null)
								.id("urn:ngsi-ld:service-category:non-existent")))));

		testEntries.add(Arguments.of("A service category with an invalid service candidate should not be created.",
				ServiceCategoryCreateVOTestExample.build().atSchemaLocation(null).parentId(null)
						.serviceCandidate(List.of(ServiceCandidateRefVOTestExample.build().atSchemaLocation(null)))));
		testEntries.add(Arguments.of("A service category with a non-existent service candidate should not be created.",
				ServiceCategoryCreateVOTestExample.build().atSchemaLocation(null).parentId(null).serviceCandidate(
						List.of(ServiceCandidateRefVOTestExample.build().atSchemaLocation(null)
								.id("urn:ngsi-ld:service-candidate:non-existent")))));

		return testEntries.stream();
	}

	@Disabled("Security is handled externally, thus 401 and 403 cannot happen.")
	@Test
	@Override
	public void createServiceCategory401() throws Exception {

	}

	@Disabled("Security is handled externally, thus 401 and 403 cannot happen.")
	@Test
	@Override
	public void createServiceCategory403() throws Exception {

	}

	@Disabled("Prohibited by the framework.")
	@Test
	@Override
	public void createServiceCategory405() throws Exception {
	}

	@Disabled("No implicit creation, impossible state.")
	@Test
	@Override
	public void createServiceCategory409() throws Exception {

	}

	@Override
	public void createServiceCategory500() throws Exception {

	}

	@Test
	@Override
	public void deleteServiceCategory204() throws Exception {
		ServiceCategoryCreateVO emptyCreate = ServiceCategoryCreateVOTestExample.build().atSchemaLocation(null).parentId(null);

		HttpResponse<ServiceCategoryVO> createResponse = serviceCategoryApiTestClient.createServiceCategory(null,
				emptyCreate);
		assertEquals(HttpStatus.CREATED, createResponse.getStatus(),
				"The service category should have been created first.");

		String rfId = createResponse.body().getId();

		assertEquals(HttpStatus.NO_CONTENT,
				callAndCatch(() -> serviceCategoryApiTestClient.deleteServiceCategory(null, rfId)).getStatus(),
				"The service category should have been deleted.");

		assertEquals(HttpStatus.NOT_FOUND,
				callAndCatch(() -> serviceCategoryApiTestClient.retrieveServiceCategory(null, rfId, null)).status(),
				"The service category should not exist anymore.");
	}

	@Disabled("400 is impossible to happen on deletion with the current implementation.")
	@Test
	@Override
	public void deleteServiceCategory400() throws Exception {

	}

	@Disabled("Security is handled externally, thus 401 and 403 cannot happen.")
	@Test
	@Override
	public void deleteServiceCategory401() throws Exception {

	}

	@Disabled("Security is handled externally, thus 401 and 403 cannot happen.")
	@Test
	@Override
	public void deleteServiceCategory403() throws Exception {

	}

	@Test
	@Override
	public void deleteServiceCategory404() throws Exception {
		HttpResponse<?> notFoundResponse = callAndCatch(
				() -> serviceCategoryApiTestClient.deleteServiceCategory(null, "urn:ngsi-ld:service-category:no-pop"));
		assertEquals(HttpStatus.NOT_FOUND,
				notFoundResponse.getStatus(),
				"No such service-category should exist.");

		Optional<ErrorDetails> optionalErrorDetails = notFoundResponse.getBody(ErrorDetails.class);
		assertTrue(optionalErrorDetails.isPresent(), "Error details should be provided.");

		notFoundResponse = callAndCatch(() -> serviceCategoryApiTestClient.deleteServiceCategory(null, "invalid-id"));
		assertEquals(HttpStatus.NOT_FOUND,
				notFoundResponse.getStatus(),
				"No such service-category should exist.");

		optionalErrorDetails = notFoundResponse.getBody(ErrorDetails.class);
		assertTrue(optionalErrorDetails.isPresent(), "Error details should be provided.");
	}

	@Disabled("Prohibited by the framework.")
	@Test
	@Override
	public void deleteServiceCategory405() throws Exception {

	}

	@Disabled("Impossible status.")
	@Test
	@Override
	public void deleteServiceCategory409() throws Exception {

	}

	@Override
	public void deleteServiceCategory500() throws Exception {

	}

	@Test
	@Override
	public void listServiceCategory200() throws Exception {

		List<ServiceCategoryVO> expectedServiceCategorys = new ArrayList<>();
		for (int i = 0; i < 10; i++) {
			ServiceCategoryCreateVO serviceCategoryCreateVO = ServiceCategoryCreateVOTestExample.build().atSchemaLocation(null).parentId(null);
			String id = serviceCategoryApiTestClient.createServiceCategory(null, serviceCategoryCreateVO)
					.body().getId();
			ServiceCategoryVO serviceCategoryVO = ServiceCategoryVOTestExample.build().atSchemaLocation(null)

					.id(id)
					.href(URI.create(id))
					.parentId(null);
			expectedServiceCategorys.add(serviceCategoryVO);
		}

		HttpResponse<List<ServiceCategoryVO>> serviceCategoryResponse = callAndCatch(
				() -> serviceCategoryApiTestClient.listServiceCategory(null, null, null, null));

		assertEquals(HttpStatus.OK, serviceCategoryResponse.getStatus(), "The list should be accessible.");
		assertEquals(expectedServiceCategorys.size(), serviceCategoryResponse.getBody().get().size(),
				"All serviceCategorys should have been returned.");
		List<ServiceCategoryVO> retrievedServiceCategorys = serviceCategoryResponse.getBody().get();

		Map<String, ServiceCategoryVO> retrievedMap = retrievedServiceCategorys.stream()
				.collect(Collectors.toMap(serviceCategory -> serviceCategory.getId(),
						serviceCategory -> serviceCategory));

		expectedServiceCategorys.stream()
				.forEach(
						expectedServiceCategory -> assertTrue(
								retrievedMap.containsKey(expectedServiceCategory.getId()),
								String.format("All created serviceCategorys should be returned - Missing: %s.",
										expectedServiceCategory,
										retrievedServiceCategorys)));
		expectedServiceCategorys.stream().forEach(
				expectedServiceCategory -> assertEquals(expectedServiceCategory,
						retrievedMap.get(expectedServiceCategory.getId()),
						"The correct serviceCategorys should be retrieved."));

		// get with pagination
		Integer limit = 5;
		HttpResponse<List<ServiceCategoryVO>> firstPartResponse = callAndCatch(
				() -> serviceCategoryApiTestClient.listServiceCategory(null, null, 0, limit));
		assertEquals(limit, firstPartResponse.body().size(),
				"Only the requested number of entries should be returend.");
		HttpResponse<List<ServiceCategoryVO>> secondPartResponse = callAndCatch(
				() -> serviceCategoryApiTestClient.listServiceCategory(null, null, 0 + limit, limit));
		assertEquals(limit, secondPartResponse.body().size(),
				"Only the requested number of entries should be returend.");

		retrievedServiceCategorys.clear();
		retrievedServiceCategorys.addAll(firstPartResponse.body());
		retrievedServiceCategorys.addAll(secondPartResponse.body());
		expectedServiceCategorys.stream()
				.forEach(
						expectedServiceCategory -> assertTrue(
								retrievedMap.containsKey(expectedServiceCategory.getId()),
								String.format("All created serviceCategorys should be returned - Missing: %s.",
										expectedServiceCategory)));
		expectedServiceCategorys.stream().forEach(
				expectedServiceCategory -> assertEquals(expectedServiceCategory,
						retrievedMap.get(expectedServiceCategory.getId()),
						"The correct serviceCategorys should be retrieved."));
	}

	@Test
	@Override
	public void listServiceCategory400() throws Exception {
		HttpResponse<List<ServiceCategoryVO>> badRequestResponse = callAndCatch(
				() -> serviceCategoryApiTestClient.listServiceCategory(null, null, -1, null));
		assertEquals(HttpStatus.BAD_REQUEST,
				badRequestResponse.getStatus(),
				"Negative offsets are impossible.");

		Optional<ErrorDetails> optionalErrorDetails = badRequestResponse.getBody(ErrorDetails.class);
		assertTrue(optionalErrorDetails.isPresent(), "Error details should be provided.");

		badRequestResponse = callAndCatch(() -> serviceCategoryApiTestClient.listServiceCategory(null, null, null, -1));
		assertEquals(HttpStatus.BAD_REQUEST,
				badRequestResponse.getStatus(),
				"Negative limits are impossible.");
		optionalErrorDetails = badRequestResponse.getBody(ErrorDetails.class);
		assertTrue(optionalErrorDetails.isPresent(), "Error details should be provided.");
	}

	@Disabled("Security is handled externally, thus 401 and 403 cannot happen.")
	@Test
	@Override
	public void listServiceCategory401() throws Exception {

	}

	@Disabled("Security is handled externally, thus 401 and 403 cannot happen.")
	@Test
	@Override
	public void listServiceCategory403() throws Exception {

	}

	@Disabled("Not found is not possible here, will be answered with an empty list instead.")
	@Test
	@Override
	public void listServiceCategory404() throws Exception {

	}

	@Disabled("Prohibited by the framework.")
	@Test
	@Override
	public void listServiceCategory405() throws Exception {

	}

	@Disabled("Impossible status.")
	@Test
	@Override
	public void listServiceCategory409() throws Exception {

	}

	@Override
	public void listServiceCategory500() throws Exception {

	}

	@ParameterizedTest
	@MethodSource("provideServiceCategoryUpdates")
	public void patchServiceCategory200(String message, ServiceCategoryUpdateVO serviceCategoryUpdateVO,
										ServiceCategoryVO expectedServiceCategory) throws Exception {
		this.message = message;
		this.serviceCategoryUpdateVO = serviceCategoryUpdateVO;
		this.expectedServiceCategory = expectedServiceCategory;
		patchServiceCategory200();
	}

	@Override
	public void patchServiceCategory200() throws Exception {
		//first create
		ServiceCategoryCreateVO serviceCategoryCreateVO = ServiceCategoryCreateVOTestExample.build().atSchemaLocation(null).parentId(null);

		HttpResponse<ServiceCategoryVO> createResponse = callAndCatch(
				() -> serviceCategoryApiTestClient.createServiceCategory(null, serviceCategoryCreateVO));
		assertEquals(HttpStatus.CREATED, createResponse.getStatus(),
				"The service category should have been created first.");

		String resourceId = createResponse.body().getId();

		HttpResponse<ServiceCategoryVO> updateResponse = callAndCatch(
				() -> serviceCategoryApiTestClient.patchServiceCategory(null, resourceId, serviceCategoryUpdateVO));
		assertEquals(HttpStatus.OK, updateResponse.getStatus(), message);

		ServiceCategoryVO updatedServiceCategory = updateResponse.body();
		expectedServiceCategory.href(URI.create(resourceId)).id(resourceId);

		assertEquals(expectedServiceCategory, updatedServiceCategory, message);
	}

	private static Stream<Arguments> provideServiceCategoryUpdates() {
		List<Arguments> testEntries = new ArrayList<>();

		ServiceCategoryUpdateVO lifecycleStatusUpdate = ServiceCategoryUpdateVOTestExample.build().atSchemaLocation(null)
				.parentId(null)
				.lifecycleStatus("dead");
		ServiceCategoryVO expectedLifecycleStatus = ServiceCategoryVOTestExample.build().atSchemaLocation(null)
				.parentId(null)
				.lifecycleStatus("dead");
		testEntries.add(Arguments.of("The lifecycle state should have been updated.", lifecycleStatusUpdate,
				expectedLifecycleStatus));

		ServiceCategoryUpdateVO descriptionUpdate = ServiceCategoryUpdateVOTestExample.build().atSchemaLocation(null)
				.parentId(null)
				.description("new-description");
		ServiceCategoryVO expectedDescriptionUpdate = ServiceCategoryVOTestExample.build().atSchemaLocation(null)
				.parentId(null)
				.description("new-description");
		testEntries.add(Arguments.of("The description should have been updated.", descriptionUpdate,
				expectedDescriptionUpdate));

		ServiceCategoryUpdateVO nameUpdate = ServiceCategoryUpdateVOTestExample.build().atSchemaLocation(null)
				.parentId(null)
				.name("new-name");
		ServiceCategoryVO expectedNameUpdate = ServiceCategoryVOTestExample.build().atSchemaLocation(null)
				.parentId(null)
				.name("new-name");
		testEntries.add(Arguments.of("The name should have been updated.", nameUpdate, expectedNameUpdate));

		ServiceCategoryUpdateVO isRootUpdate = ServiceCategoryUpdateVOTestExample.build().atSchemaLocation(null)
				.parentId(null)
				.isRoot(true);
		ServiceCategoryVO expectedIsRoot = ServiceCategoryVOTestExample.build().atSchemaLocation(null)
				.parentId(null)
				.isRoot(true);
		testEntries.add(Arguments.of("isRoot should have been updated.", isRootUpdate, expectedIsRoot));

		ServiceCategoryUpdateVO versionUpdate = ServiceCategoryUpdateVOTestExample.build().atSchemaLocation(null)
				.parentId(null)
				.version("v0.0.2");
		ServiceCategoryVO expectedVersionUpdate = ServiceCategoryVOTestExample.build().atSchemaLocation(null)
				.parentId(null)
				.version("v0.0.2");
		testEntries.add(Arguments.of("The version should have been updated.", versionUpdate, expectedVersionUpdate));

		TimePeriodVO timePeriodVO = TimePeriodVOTestExample.build().endDateTime(Instant.now())
				.startDateTime(Instant.now());
		testEntries.add(Arguments.of("The validFor should have been updated.",
				ServiceCategoryUpdateVOTestExample.build().atSchemaLocation(null).validFor(timePeriodVO).parentId(null),
				ServiceCategoryVOTestExample.build().atSchemaLocation(null).validFor(timePeriodVO).parentId(null)));

		return testEntries.stream();
	}

	@ParameterizedTest
	@MethodSource("provideInvalidUpdates")
	public void patchServiceCategory400(String message, ServiceCategoryUpdateVO invalidUpdateVO) throws Exception {
		this.message = message;
		this.serviceCategoryUpdateVO = invalidUpdateVO;
		patchServiceCategory400();
	}

	@Override
	public void patchServiceCategory400() throws Exception {
		//first create
		ServiceCategoryCreateVO serviceCategoryCreateVO = ServiceCategoryCreateVOTestExample.build().atSchemaLocation(null).parentId(null);

		HttpResponse<ServiceCategoryVO> createResponse = callAndCatch(
				() -> serviceCategoryApiTestClient.createServiceCategory(null, serviceCategoryCreateVO));
		assertEquals(HttpStatus.CREATED, createResponse.getStatus(),
				"The service category should have been created first.");

		String resourceId = createResponse.body().getId();

		HttpResponse<ServiceCategoryVO> updateResponse = callAndCatch(
				() -> serviceCategoryApiTestClient.patchServiceCategory(null, resourceId, serviceCategoryUpdateVO));
		assertEquals(HttpStatus.BAD_REQUEST, updateResponse.getStatus(), message);

		Optional<ErrorDetails> optionalErrorDetails = updateResponse.getBody(ErrorDetails.class);
		assertTrue(optionalErrorDetails.isPresent(), "Some error details should be present.");
	}

	private static Stream<Arguments> provideInvalidUpdates() {
		List<Arguments> testEntries = new ArrayList<>();

		testEntries.add(Arguments.of("An update with an invalid parent spe ref is not allowed.",
				ServiceCategoryUpdateVOTestExample.build().atSchemaLocation(null)
						.parentId("invalid")));
		testEntries.add(Arguments.of("An update with an non existent parent is not allowed.",
				ServiceCategoryUpdateVOTestExample.build().atSchemaLocation(null)
						.parentId("urn:ngsi-ld:service-category:non-existent")));

		testEntries.add(Arguments.of("An update with an invalid category ref is not allowed.",
				ServiceCategoryUpdateVOTestExample.build().atSchemaLocation(null)
						.category(List.of(ServiceCategoryRefVOTestExample.build().atSchemaLocation(null)))));
		testEntries.add(Arguments.of("An update with an non existent category is not allowed.",
				ServiceCategoryUpdateVOTestExample.build().atSchemaLocation(null)
						.category(List.of(ServiceCategoryRefVOTestExample.build().atSchemaLocation(null)
								.id("urn:ngsi-ld:service-category:non-existent")))));

		testEntries.add(Arguments.of("An update with an invalid serviceCandidate ref is not allowed.",
				ServiceCategoryUpdateVOTestExample.build().atSchemaLocation(null)
						.serviceCandidate(List.of(ServiceCandidateRefVOTestExample.build().atSchemaLocation(null)))));
		testEntries.add(Arguments.of("An update with an non existent serviceCandidate is not allowed.",
				ServiceCategoryUpdateVOTestExample.build().atSchemaLocation(null)
						.serviceCandidate(List.of(ServiceCandidateRefVOTestExample.build().atSchemaLocation(null)
								.id("urn:ngsi-ld:service-candidate:non-existent")))));

		return testEntries.stream();
	}

	@Disabled("Security is handled externally, thus 401 and 403 cannot happen.")
	@Test
	@Override
	public void patchServiceCategory401() throws Exception {

	}

	@Disabled("Security is handled externally, thus 401 and 403 cannot happen.")
	@Test
	@Override
	public void patchServiceCategory403() throws Exception {

	}

	@Test
	@Override
	public void patchServiceCategory404() throws Exception {
		ServiceCategoryUpdateVO serviceCategoryUpdateVO = ServiceCategoryUpdateVOTestExample.build().atSchemaLocation(null);
		assertEquals(
				HttpStatus.NOT_FOUND,
				callAndCatch(() -> serviceCategoryApiTestClient.patchServiceCategory(null,
						"urn:ngsi-ld:service-category:not-existent", serviceCategoryUpdateVO)).getStatus(),
				"Non existent service category should not be updated.");
	}

	@Disabled("Prohibited by the framework.")
	@Test
	@Override
	public void patchServiceCategory405() throws Exception {

	}

	@Override
	public void patchServiceCategory409() throws Exception {
		// TODO: can this happen?
	}

	@Override
	public void patchServiceCategory500() throws Exception {

	}

	@ParameterizedTest
	@MethodSource("provideFieldParameters")
	public void retrieveServiceCategory200(String message, String fields, ServiceCategoryVO expectedServiceCategory)
			throws Exception {
		this.fieldsParameter = fields;
		this.message = message;
		this.expectedServiceCategory = expectedServiceCategory;
		retrieveServiceCategory200();
	}

	@Override
	public void retrieveServiceCategory200() throws Exception {

		ServiceCategoryCreateVO serviceCategoryCreateVO = ServiceCategoryCreateVOTestExample.build().atSchemaLocation(null).parentId(null);
		HttpResponse<ServiceCategoryVO> createResponse = callAndCatch(
				() -> serviceCategoryApiTestClient.createServiceCategory(null, serviceCategoryCreateVO));
		assertEquals(HttpStatus.CREATED, createResponse.getStatus(), message);
		String id = createResponse.body().getId();

		expectedServiceCategory
				.id(id)
				.href(URI.create(id));

		//then retrieve
		HttpResponse<ServiceCategoryVO> retrievedRF = callAndCatch(
				() -> serviceCategoryApiTestClient.retrieveServiceCategory(null, id, fieldsParameter));
		assertEquals(HttpStatus.OK, retrievedRF.getStatus(), message);
		assertEquals(expectedServiceCategory, retrievedRF.body(), message);
	}

	private static Stream<Arguments> provideFieldParameters() {
		return Stream.of(
				Arguments.of("Without a fields parameter everything should be returned.", null,
						ServiceCategoryVOTestExample.build().atSchemaLocation(null)
								.parentId(null)),
				Arguments.of("Only version and the mandatory parameters should have been included.", "version",
						ServiceCategoryVOTestExample.build().atSchemaLocation(null)
								.lastUpdate(null)
								.isRoot(null)
								.category(null)
								.serviceCandidate(null)
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
						"nothingToSeeHere", ServiceCategoryVOTestExample.build().atSchemaLocation(null)
								.lastUpdate(null)
								.isRoot(null)
								.category(null)
								.serviceCandidate(null)
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
						"version,lastUpdate,lifecycleStatus,description", ServiceCategoryVOTestExample.build().atSchemaLocation(null)
								.isRoot(null)
								.category(null)
								.serviceCandidate(null)
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
	public void retrieveServiceCategory400() throws Exception {

	}

	@Disabled("Security is handled externally, thus 401 and 403 cannot happen.")
	@Test
	@Override
	public void retrieveServiceCategory401() throws Exception {

	}

	@Disabled("Security is handled externally, thus 401 and 403 cannot happen.")
	@Test
	@Override
	public void retrieveServiceCategory403() throws Exception {

	}

	@Test
	@Override
	public void retrieveServiceCategory404() throws Exception {
		HttpResponse<ServiceCategoryVO> response = callAndCatch(
				() -> serviceCategoryApiTestClient.retrieveServiceCategory(null, "urn:ngsi-ld:resource-function:non-existent",
						null));
		assertEquals(HttpStatus.NOT_FOUND, response.getStatus(), "No such service-category should exist.");

		Optional<ErrorDetails> optionalErrorDetails = response.getBody(ErrorDetails.class);
		assertTrue(optionalErrorDetails.isPresent(), "Error details should have been provided.");
	}

	@Disabled("Prohibited by the framework.")
	@Test
	@Override
	public void retrieveServiceCategory405() throws Exception {

	}

	@Disabled("Conflict not possible on retrieval")
	@Test
	@Override
	public void retrieveServiceCategory409() throws Exception {

	}

	@Override
	public void retrieveServiceCategory500() throws Exception {

	}

	@Override
	protected String getEntityType() {
		return ServiceCategory.TYPE_SERVICE_CATEGORY;
	}
}
