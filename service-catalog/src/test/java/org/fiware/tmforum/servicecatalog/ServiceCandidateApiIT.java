package org.fiware.tmforum.servicecatalog;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import io.micronaut.test.annotation.MockBean;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import org.fiware.ngsi.api.EntitiesApiClient;
import org.fiware.servicecatalog.api.ServiceCandidateApiTestClient;
import org.fiware.servicecatalog.api.ServiceCandidateApiTestSpec;
import org.fiware.servicecatalog.model.ServiceCandidateCreateVO;
import org.fiware.servicecatalog.model.ServiceCandidateCreateVOTestExample;
import org.fiware.servicecatalog.model.ServiceCandidateUpdateVO;
import org.fiware.servicecatalog.model.ServiceCandidateUpdateVOTestExample;
import org.fiware.servicecatalog.model.ServiceCandidateVO;
import org.fiware.servicecatalog.model.ServiceCandidateVOTestExample;
import org.fiware.servicecatalog.model.ServiceCategoryRefVOTestExample;
import org.fiware.servicecatalog.model.ServiceSpecificationRefVOTestExample;
import org.fiware.servicecatalog.model.TimePeriodVO;
import org.fiware.servicecatalog.model.TimePeriodVOTestExample;
import org.fiware.tmforum.common.notification.EventHandler;
import org.fiware.tmforum.common.configuration.GeneralProperties;
import org.fiware.tmforum.common.exception.ErrorDetails;
import org.fiware.tmforum.common.test.AbstractApiIT;
import org.fiware.tmforum.service.ServiceCandidate;
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

@MicronautTest(packages = { "org.fiware.tmforum.servicecatalog" })
public class ServiceCandidateApiIT extends AbstractApiIT implements ServiceCandidateApiTestSpec {

	public final ServiceCandidateApiTestClient serviceCandidateApiTestClient;

	private String message;
	private String fieldsParameter;
	private ServiceCandidateCreateVO serviceCandidateCreateVO;
	private ServiceCandidateUpdateVO serviceCandidateUpdateVO;
	private ServiceCandidateVO expectedServiceCandidate;

	private Clock clock = mock(Clock.class);

	public ServiceCandidateApiIT(ServiceCandidateApiTestClient serviceCandidateApiTestClient,
			EntitiesApiClient entitiesApiClient, ObjectMapper objectMapper, GeneralProperties generalProperties) {
		super(entitiesApiClient, objectMapper, generalProperties);
		this.serviceCandidateApiTestClient = serviceCandidateApiTestClient;
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
	@MethodSource("provideValidServiceCandidates")
	public void createServiceCandidate201(String message, ServiceCandidateCreateVO serviceCandidateCreateVO,
			ServiceCandidateVO expectedServiceCandidate) throws Exception {
		this.message = message;
		this.serviceCandidateCreateVO = serviceCandidateCreateVO;
		this.expectedServiceCandidate = expectedServiceCandidate;
		createServiceCandidate201();
	}

	@Override
	public void createServiceCandidate201() throws Exception {

		Instant currentTimeInstant = Instant.ofEpochSecond(10000);
		when(clock.instant()).thenReturn(currentTimeInstant);

		HttpResponse<ServiceCandidateVO> serviceCandidateVOHttpResponse = callAndCatch(
				() -> serviceCandidateApiTestClient.createServiceCandidate(serviceCandidateCreateVO));
		assertEquals(HttpStatus.CREATED, serviceCandidateVOHttpResponse.getStatus(), message);
		String scId = serviceCandidateVOHttpResponse.body().getId();
		expectedServiceCandidate
				.id(scId)
				.href(URI.create(scId))
				.lastUpdate(currentTimeInstant);

		assertEquals(expectedServiceCandidate, serviceCandidateVOHttpResponse.body(), message);
	}

	private static Stream<Arguments> provideValidServiceCandidates() {
		List<Arguments> testEntries = new ArrayList<>();

		ServiceCandidateCreateVO emptyCreate = ServiceCandidateCreateVOTestExample.build().lifecycleStatus("created")
				.serviceSpecification(null);
		ServiceCandidateVO expectedEmpty = ServiceCandidateVOTestExample.build().lifecycleStatus("created")
				.serviceSpecification(null);
		testEntries.add(
				Arguments.of("An empty service candidate should have been created.", emptyCreate, expectedEmpty));

		TimePeriodVO timePeriodVO = TimePeriodVOTestExample.build().endDateTime(Instant.now())
				.startDateTime(Instant.now());
		ServiceCandidateCreateVO createValidFor = ServiceCandidateCreateVOTestExample.build().validFor(timePeriodVO)
				.lifecycleStatus("created").serviceSpecification(null);
		ServiceCandidateVO expectedValidFor = ServiceCandidateVOTestExample.build().validFor(timePeriodVO)
				.lifecycleStatus("created").serviceSpecification(null);
		testEntries.add(Arguments.of("An service candidate with a validFor should have been created.", createValidFor,
				expectedValidFor));

		return testEntries.stream();
	}

	@ParameterizedTest
	@MethodSource("provideInvalidServiceCandidates")
	public void createServiceCandidate400(String message, ServiceCandidateCreateVO invalidCreateVO) throws Exception {
		this.message = message;
		this.serviceCandidateCreateVO = invalidCreateVO;
		createServiceCandidate400();
	}

	@Override
	public void createServiceCandidate400() throws Exception {
		HttpResponse<ServiceCandidateVO> creationResponse = callAndCatch(
				() -> serviceCandidateApiTestClient.createServiceCandidate(serviceCandidateCreateVO));
		assertEquals(HttpStatus.BAD_REQUEST, creationResponse.getStatus(), message);
		Optional<ErrorDetails> optionalErrorDetails = creationResponse.getBody(ErrorDetails.class);
		assertTrue(optionalErrorDetails.isPresent(), "Error details should be provided.");
	}

	private static Stream<Arguments> provideInvalidServiceCandidates() {
		List<Arguments> testEntries = new ArrayList<>();

		testEntries.add(Arguments.of("A service candidate with an spec ref should not be created.",
				ServiceCandidateCreateVOTestExample.build()
						.serviceSpecification(ServiceSpecificationRefVOTestExample.build())));
		testEntries.add(Arguments.of("A service candidate with a non-existent spec ref should not be created.",
				ServiceCandidateCreateVOTestExample.build()
						.serviceSpecification(ServiceSpecificationRefVOTestExample.build()
								.id("urn:ngsi-ld:service-specification:non-existent"))));

		testEntries.add(Arguments.of("A service candidate with invalid category refs should not be created.",
				ServiceCandidateCreateVOTestExample.build()
						.category(List.of(ServiceCategoryRefVOTestExample.build()))));
		testEntries.add(Arguments.of("A service candidate with non-existent category refs should not be created.",
				ServiceCandidateCreateVOTestExample.build().category(List.of(ServiceCategoryRefVOTestExample.build()
						.id("urn:ngsi-ld:service-category:non-existent")))));

		return testEntries.stream();
	}

	@Disabled("Security is handled externally, thus 401 and 403 cannot happen.")
	@Test
	@Override
	public void createServiceCandidate401() throws Exception {

	}

	@Disabled("Security is handled externally, thus 401 and 403 cannot happen.")
	@Test
	@Override
	public void createServiceCandidate403() throws Exception {

	}

	@Disabled("Prohibited by the framework.")
	@Test
	@Override
	public void createServiceCandidate405() throws Exception {
	}

	@Disabled("No implicit creation, impossible state.")
	@Test
	@Override
	public void createServiceCandidate409() throws Exception {

	}

	@Override
	public void createServiceCandidate500() throws Exception {

	}

	@Test
	@Override
	public void deleteServiceCandidate204() throws Exception {
		ServiceCandidateCreateVO emptyCreate = ServiceCandidateCreateVOTestExample.build().serviceSpecification(null);

		HttpResponse<ServiceCandidateVO> createResponse = serviceCandidateApiTestClient.createServiceCandidate(
				emptyCreate);
		assertEquals(HttpStatus.CREATED, createResponse.getStatus(),
				"The service candidate should have been created first.");

		String rfId = createResponse.body().getId();

		assertEquals(HttpStatus.NO_CONTENT,
				callAndCatch(() -> serviceCandidateApiTestClient.deleteServiceCandidate(rfId)).getStatus(),
				"The service candidate should have been deleted.");

		assertEquals(HttpStatus.NOT_FOUND,
				callAndCatch(() -> serviceCandidateApiTestClient.retrieveServiceCandidate(rfId, null)).status(),
				"The service candidate should not exist anymore.");
	}

	@Disabled("400 is impossible to happen on deletion with the current implementation.")
	@Test
	@Override
	public void deleteServiceCandidate400() throws Exception {

	}

	@Disabled("Security is handled externally, thus 401 and 403 cannot happen.")
	@Test
	@Override
	public void deleteServiceCandidate401() throws Exception {

	}

	@Disabled("Security is handled externally, thus 401 and 403 cannot happen.")
	@Test
	@Override
	public void deleteServiceCandidate403() throws Exception {

	}

	@Test
	@Override
	public void deleteServiceCandidate404() throws Exception {
		HttpResponse<?> notFoundResponse = callAndCatch(
				() -> serviceCandidateApiTestClient.deleteServiceCandidate("urn:ngsi-ld:service-candidate:no-pop"));
		assertEquals(HttpStatus.NOT_FOUND,
				notFoundResponse.getStatus(),
				"No such service-candidate should exist.");

		Optional<ErrorDetails> optionalErrorDetails = notFoundResponse.getBody(ErrorDetails.class);
		assertTrue(optionalErrorDetails.isPresent(), "Error details should be provided.");

		notFoundResponse = callAndCatch(() -> serviceCandidateApiTestClient.deleteServiceCandidate("invalid-id"));
		assertEquals(HttpStatus.NOT_FOUND,
				notFoundResponse.getStatus(),
				"No such service-candidate should exist.");

		optionalErrorDetails = notFoundResponse.getBody(ErrorDetails.class);
		assertTrue(optionalErrorDetails.isPresent(), "Error details should be provided.");
	}

	@Disabled("Prohibited by the framework.")
	@Test
	@Override
	public void deleteServiceCandidate405() throws Exception {

	}

	@Disabled("Impossible status.")
	@Test
	@Override
	public void deleteServiceCandidate409() throws Exception {

	}

	@Override
	public void deleteServiceCandidate500() throws Exception {

	}

	@Test
	@Override
	public void listServiceCandidate200() throws Exception {

		List<ServiceCandidateVO> expectedServiceCandidates = new ArrayList<>();
		for (int i = 0; i < 10; i++) {
			ServiceCandidateCreateVO serviceCandidateCreateVO = ServiceCandidateCreateVOTestExample.build()
					.serviceSpecification(null);
			String id = serviceCandidateApiTestClient.createServiceCandidate(serviceCandidateCreateVO)
					.body().getId();
			ServiceCandidateVO serviceCandidateVO = ServiceCandidateVOTestExample.build();
			serviceCandidateVO
					.id(id)
					.href(URI.create(id))
					.category(null)
					.serviceSpecification(null);
			expectedServiceCandidates.add(serviceCandidateVO);
		}

		HttpResponse<List<ServiceCandidateVO>> serviceCandidateResponse = callAndCatch(
				() -> serviceCandidateApiTestClient.listServiceCandidate(null, null, null));

		assertEquals(HttpStatus.OK, serviceCandidateResponse.getStatus(), "The list should be accessible.");
		assertEquals(expectedServiceCandidates.size(), serviceCandidateResponse.getBody().get().size(),
				"All serviceCandidates should have been returned.");
		List<ServiceCandidateVO> retrievedServiceCandidates = serviceCandidateResponse.getBody().get();

		Map<String, ServiceCandidateVO> retrievedMap = retrievedServiceCandidates.stream()
				.collect(Collectors.toMap(serviceCandidate -> serviceCandidate.getId(),
						serviceCandidate -> serviceCandidate));

		expectedServiceCandidates.stream()
				.forEach(
						expectedServiceCandidate -> assertTrue(
								retrievedMap.containsKey(expectedServiceCandidate.getId()),
								String.format("All created serviceCandidates should be returned - Missing: %s.",
										expectedServiceCandidate,
										retrievedServiceCandidates)));
		expectedServiceCandidates.stream().forEach(
				expectedServiceCandidate -> assertEquals(expectedServiceCandidate,
						retrievedMap.get(expectedServiceCandidate.getId()),
						"The correct serviceCandidates should be retrieved."));

		// get with pagination
		Integer limit = 5;
		HttpResponse<List<ServiceCandidateVO>> firstPartResponse = callAndCatch(
				() -> serviceCandidateApiTestClient.listServiceCandidate(null, 0, limit));
		assertEquals(limit, firstPartResponse.body().size(),
				"Only the requested number of entries should be returend.");
		HttpResponse<List<ServiceCandidateVO>> secondPartResponse = callAndCatch(
				() -> serviceCandidateApiTestClient.listServiceCandidate(null, 0 + limit, limit));
		assertEquals(limit, secondPartResponse.body().size(),
				"Only the requested number of entries should be returend.");

		retrievedServiceCandidates.clear();
		retrievedServiceCandidates.addAll(firstPartResponse.body());
		retrievedServiceCandidates.addAll(secondPartResponse.body());
		expectedServiceCandidates.stream()
				.forEach(
						expectedServiceCandidate -> assertTrue(
								retrievedMap.containsKey(expectedServiceCandidate.getId()),
								String.format("All created serviceCandidates should be returned - Missing: %s.",
										expectedServiceCandidate)));
		expectedServiceCandidates.stream().forEach(
				expectedServiceCandidate -> assertEquals(expectedServiceCandidate,
						retrievedMap.get(expectedServiceCandidate.getId()),
						"The correct serviceCandidates should be retrieved."));
	}

	@Test
	@Override
	public void listServiceCandidate400() throws Exception {
		HttpResponse<List<ServiceCandidateVO>> badRequestResponse = callAndCatch(
				() -> serviceCandidateApiTestClient.listServiceCandidate(null, -1, null));
		assertEquals(HttpStatus.BAD_REQUEST,
				badRequestResponse.getStatus(),
				"Negative offsets are impossible.");

		Optional<ErrorDetails> optionalErrorDetails = badRequestResponse.getBody(ErrorDetails.class);
		assertTrue(optionalErrorDetails.isPresent(), "Error details should be provided.");

		badRequestResponse = callAndCatch(() -> serviceCandidateApiTestClient.listServiceCandidate(null, null, -1));
		assertEquals(HttpStatus.BAD_REQUEST,
				badRequestResponse.getStatus(),
				"Negative limits are impossible.");
		optionalErrorDetails = badRequestResponse.getBody(ErrorDetails.class);
		assertTrue(optionalErrorDetails.isPresent(), "Error details should be provided.");
	}

	@Disabled("Security is handled externally, thus 401 and 403 cannot happen.")
	@Test
	@Override
	public void listServiceCandidate401() throws Exception {

	}

	@Disabled("Security is handled externally, thus 401 and 403 cannot happen.")
	@Test
	@Override
	public void listServiceCandidate403() throws Exception {

	}

	@Disabled("Not found is not possible here, will be answered with an empty list instead.")
	@Test
	@Override
	public void listServiceCandidate404() throws Exception {

	}

	@Disabled("Prohibited by the framework.")
	@Test
	@Override
	public void listServiceCandidate405() throws Exception {

	}

	@Disabled("Impossible status.")
	@Test
	@Override
	public void listServiceCandidate409() throws Exception {

	}

	@Override
	public void listServiceCandidate500() throws Exception {

	}

	@ParameterizedTest
	@MethodSource("provideServiceCandidateUpdates")
	public void patchServiceCandidate200(String message, ServiceCandidateUpdateVO serviceCandidateUpdateVO,
			ServiceCandidateVO expectedServiceCandidate) throws Exception {
		this.message = message;
		this.serviceCandidateUpdateVO = serviceCandidateUpdateVO;
		this.expectedServiceCandidate = expectedServiceCandidate;
		patchServiceCandidate200();
	}

	@Override
	public void patchServiceCandidate200() throws Exception {
		//first create
		ServiceCandidateCreateVO serviceCandidateCreateVO = ServiceCandidateCreateVOTestExample.build()
				.serviceSpecification(null);

		HttpResponse<ServiceCandidateVO> createResponse = callAndCatch(
				() -> serviceCandidateApiTestClient.createServiceCandidate(serviceCandidateCreateVO));
		assertEquals(HttpStatus.CREATED, createResponse.getStatus(),
				"The resource function should have been created first.");

		String resourceId = createResponse.body().getId();

		HttpResponse<ServiceCandidateVO> updateResponse = callAndCatch(
				() -> serviceCandidateApiTestClient.patchServiceCandidate(resourceId, serviceCandidateUpdateVO));
		assertEquals(HttpStatus.OK, updateResponse.getStatus(), message);

		ServiceCandidateVO updatedServiceCandidate = updateResponse.body();
		expectedServiceCandidate.href(URI.create(resourceId)).id(resourceId).category(null);

		assertEquals(expectedServiceCandidate, updatedServiceCandidate, message);
	}

	private static Stream<Arguments> provideServiceCandidateUpdates() {
		List<Arguments> testEntries = new ArrayList<>();

		ServiceCandidateUpdateVO lifecycleStatusUpdate = ServiceCandidateUpdateVOTestExample.build()
				.serviceSpecification(null)
				.lifecycleStatus("dead");
		ServiceCandidateVO expectedLifecycleStatus = ServiceCandidateVOTestExample.build()
				.serviceSpecification(null)
				.lifecycleStatus("dead");
		testEntries.add(Arguments.of("The lifecycle state should have been updated.", lifecycleStatusUpdate,
				expectedLifecycleStatus));

		ServiceCandidateUpdateVO descriptionUpdate = ServiceCandidateUpdateVOTestExample.build()
				.serviceSpecification(null)
				.description("new-description");
		ServiceCandidateVO expectedDescriptionUpdate = ServiceCandidateVOTestExample.build()
				.serviceSpecification(null)
				.description("new-description");
		testEntries.add(Arguments.of("The description should have been updated.", descriptionUpdate,
				expectedDescriptionUpdate));

		ServiceCandidateUpdateVO nameUpdate = ServiceCandidateUpdateVOTestExample.build()
				.serviceSpecification(null)
				.name("new-name");
		ServiceCandidateVO expectedNameUpdate = ServiceCandidateVOTestExample.build()
				.serviceSpecification(null)
				.name("new-name");
		testEntries.add(Arguments.of("The name should have been updated.", nameUpdate, expectedNameUpdate));

		ServiceCandidateUpdateVO versionUpdate = ServiceCandidateUpdateVOTestExample.build()
				.serviceSpecification(null)
				.version("v0.0.2");
		ServiceCandidateVO expectedVersionUpdate = ServiceCandidateVOTestExample.build()
				.serviceSpecification(null)
				.version("v0.0.2");
		testEntries.add(Arguments.of("The version should have been updated.", versionUpdate, expectedVersionUpdate));

		TimePeriodVO timePeriodVO = TimePeriodVOTestExample.build().endDateTime(Instant.now())
				.startDateTime(Instant.now());
		ServiceCandidateUpdateVO validForUpdate = ServiceCandidateUpdateVOTestExample.build().validFor(timePeriodVO)
				.serviceSpecification(null);
		ServiceCandidateVO expectedValidForUpdate = ServiceCandidateVOTestExample.build().validFor(timePeriodVO)
				.serviceSpecification(null);
		testEntries.add(Arguments.of("The validFor should have been updated.", validForUpdate, expectedValidForUpdate));

		return testEntries.stream();
	}

	@ParameterizedTest
	@MethodSource("provideInvalidUpdates")
	public void patchServiceCandidate400(String message, ServiceCandidateUpdateVO invalidUpdateVO) throws Exception {
		this.message = message;
		this.serviceCandidateUpdateVO = invalidUpdateVO;
		patchServiceCandidate400();
	}

	@Override
	public void patchServiceCandidate400() throws Exception {
		//first create
		ServiceCandidateCreateVO serviceCandidateCreateVO = ServiceCandidateCreateVOTestExample.build()
				.serviceSpecification(null);

		HttpResponse<ServiceCandidateVO> createResponse = callAndCatch(
				() -> serviceCandidateApiTestClient.createServiceCandidate(serviceCandidateCreateVO));
		assertEquals(HttpStatus.CREATED, createResponse.getStatus(),
				"The rservice candidate should have been created first.");

		String resourceId = createResponse.body().getId();

		HttpResponse<ServiceCandidateVO> updateResponse = callAndCatch(
				() -> serviceCandidateApiTestClient.patchServiceCandidate(resourceId, serviceCandidateUpdateVO));
		assertEquals(HttpStatus.BAD_REQUEST, updateResponse.getStatus(), message);

		Optional<ErrorDetails> optionalErrorDetails = updateResponse.getBody(ErrorDetails.class);
		assertTrue(optionalErrorDetails.isPresent(), "Some error details should be present.");
	}

	private static Stream<Arguments> provideInvalidUpdates() {
		List<Arguments> testEntries = new ArrayList<>();

		testEntries.add(Arguments.of("An update with an invalid resource spe ref is not allowed.",
				ServiceCandidateUpdateVOTestExample.build()
						.serviceSpecification(ServiceSpecificationRefVOTestExample.build())));
		testEntries.add(Arguments.of("An update with an non existent related party is not allowed.",
				ServiceCandidateUpdateVOTestExample.build()
						.serviceSpecification(ServiceSpecificationRefVOTestExample.build()
								.id("urn:ngsi-ld:service-specification:non-existent"))));

		testEntries.add(Arguments.of("An update with an invalid category ref is not allowed.",
				ServiceCandidateUpdateVOTestExample.build()
						.category(List.of(ServiceCategoryRefVOTestExample.build()))));
		testEntries.add(Arguments.of("An update with an non existent category is not allowed.",
				ServiceCandidateUpdateVOTestExample.build()
						.category(List.of(ServiceCategoryRefVOTestExample.build()
								.id("urn:ngsi-ld:service-category:non-existent")))));

		return testEntries.stream();
	}

	@Disabled("Security is handled externally, thus 401 and 403 cannot happen.")
	@Test
	@Override
	public void patchServiceCandidate401() throws Exception {

	}

	@Disabled("Security is handled externally, thus 401 and 403 cannot happen.")
	@Test
	@Override
	public void patchServiceCandidate403() throws Exception {

	}

	@Test
	@Override
	public void patchServiceCandidate404() throws Exception {
		ServiceCandidateUpdateVO serviceCandidateUpdateVO = ServiceCandidateUpdateVOTestExample.build();
		assertEquals(
				HttpStatus.NOT_FOUND,
				callAndCatch(() -> serviceCandidateApiTestClient.patchServiceCandidate(
						"urn:ngsi-ld:service-candidate:not-existent", serviceCandidateUpdateVO)).getStatus(),
				"Non existent service candidate should not be updated.");
	}

	@Disabled("Prohibited by the framework.")
	@Test
	@Override
	public void patchServiceCandidate405() throws Exception {

	}

	@Disabled("No implicit creations.")
	@Test
	@Override
	public void patchServiceCandidate409() throws Exception {
	}

	@Override
	public void patchServiceCandidate500() throws Exception {

	}

	@ParameterizedTest
	@MethodSource("provideFieldParameters")
	public void retrieveResourceCategory200(String message, String fields, ServiceCandidateVO expectedServiceCandidate)
			throws Exception {
		this.fieldsParameter = fields;
		this.message = message;
		this.expectedServiceCandidate = expectedServiceCandidate;
		retrieveServiceCandidate200();
	}

	@Override
	public void retrieveServiceCandidate200() throws Exception {

		ServiceCandidateCreateVO serviceCandidateCreateVO = ServiceCandidateCreateVOTestExample.build()
				.serviceSpecification(null);
		HttpResponse<ServiceCandidateVO> createResponse = callAndCatch(
				() -> serviceCandidateApiTestClient.createServiceCandidate(serviceCandidateCreateVO));
		assertEquals(HttpStatus.CREATED, createResponse.getStatus(), message);
		String id = createResponse.body().getId();

		expectedServiceCandidate
				.id(id)
				.href(URI.create(id));

		//then retrieve
		HttpResponse<ServiceCandidateVO> retrievedRF = callAndCatch(
				() -> serviceCandidateApiTestClient.retrieveServiceCandidate(id, fieldsParameter));
		assertEquals(HttpStatus.OK, retrievedRF.getStatus(), message);
		assertEquals(expectedServiceCandidate, retrievedRF.body(), message);
	}

	private static Stream<Arguments> provideFieldParameters() {
		return Stream.of(
				Arguments.of("Without a fields parameter everything should be returned.",
						null,
						ServiceCandidateVOTestExample.build()
								// get nulled without values
								.category(null)
								.serviceSpecification(null)),
				Arguments.of("Only version and the mandatory parameters should have been included.",
						"version",
						ServiceCandidateVOTestExample.build()
								.lastUpdate(null)
								.category(null)
								.description(null)
								.lifecycleStatus(null)
								.name(null)
								.validFor(null)
								.atBaseType(null)
								.atSchemaLocation(null)
								.atType(null)
								.serviceSpecification(null)),
				Arguments.of(
						"Only the mandatory parameters should have been included when a non-existent field was requested.",
						"nothingToSeeHere",
						ServiceCandidateVOTestExample.build()
								.lastUpdate(null)
								.category(null)
								.description(null)
								.lifecycleStatus(null)
								.name(null)
								.version(null)
								.validFor(null)
								.atBaseType(null)
								.atSchemaLocation(null)
								.atType(null)
								.serviceSpecification(null)),
				Arguments.of(
						"Only version, lastUpdate, lifecycleStatus, description and the mandatory parameters should have been included.",
						"version,lastUpdate,lifecycleStatus,description",
						ServiceCandidateVOTestExample.build()
								.category(null)
								.name(null)
								.validFor(null)
								.atBaseType(null)
								.atSchemaLocation(null)
								.atType(null)
								.serviceSpecification(null)));
	}

	@Disabled("400 cannot happen, only 404")
	@Test
	@Override
	public void retrieveServiceCandidate400() throws Exception {

	}

	@Disabled("Security is handled externally, thus 401 and 403 cannot happen.")
	@Test
	@Override
	public void retrieveServiceCandidate401() throws Exception {

	}

	@Disabled("Security is handled externally, thus 401 and 403 cannot happen.")
	@Test
	@Override
	public void retrieveServiceCandidate403() throws Exception {

	}

	@Test
	@Override
	public void retrieveServiceCandidate404() throws Exception {
		HttpResponse<ServiceCandidateVO> response = callAndCatch(
				() -> serviceCandidateApiTestClient.retrieveServiceCandidate(
						"urn:ngsi-ld:resource-function:non-existent", null));
		assertEquals(HttpStatus.NOT_FOUND, response.getStatus(), "No such service-candidate should exist.");

		Optional<ErrorDetails> optionalErrorDetails = response.getBody(ErrorDetails.class);
		assertTrue(optionalErrorDetails.isPresent(), "Error details should have been provided.");
	}

	@Disabled("Prohibited by the framework.")
	@Test
	@Override
	public void retrieveServiceCandidate405() throws Exception {

	}

	@Disabled("Conflict not possible on retrieval")
	@Test
	@Override
	public void retrieveServiceCandidate409() throws Exception {

	}

	@Override
	public void retrieveServiceCandidate500() throws Exception {

	}

	@Override
	protected String getEntityType() {
		return ServiceCandidate.TYPE_SERVICE_CANDIDATE;
	}
}
