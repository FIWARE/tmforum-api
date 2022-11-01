package org.fiware.tmforum.servicecatalog;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import io.micronaut.test.annotation.MockBean;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import org.fiware.ngsi.api.EntitiesApiClient;
import org.fiware.servicecatalog.api.ServiceCatalogApiTestClient;
import org.fiware.servicecatalog.api.ServiceCatalogApiTestSpec;
import org.fiware.servicecatalog.model.RelatedPartyVOTestExample;
import org.fiware.servicecatalog.model.ServiceCatalogCreateVO;
import org.fiware.servicecatalog.model.ServiceCatalogCreateVOTestExample;
import org.fiware.servicecatalog.model.ServiceCatalogUpdateVO;
import org.fiware.servicecatalog.model.ServiceCatalogUpdateVOTestExample;
import org.fiware.servicecatalog.model.ServiceCatalogVO;
import org.fiware.servicecatalog.model.ServiceCatalogVOTestExample;
import org.fiware.servicecatalog.model.ServiceCategoryRefVOTestExample;
import org.fiware.servicecatalog.model.TimePeriodVO;
import org.fiware.servicecatalog.model.TimePeriodVOTestExample;
import org.fiware.tmforum.common.configuration.GeneralProperties;
import org.fiware.tmforum.common.exception.ErrorDetails;
import org.fiware.tmforum.common.test.AbstractApiIT;
import org.fiware.tmforum.servicecatalog.domain.ServiceCatalog;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@MicronautTest(packages = { "org.fiware.tmforum.servicecatalog" })
public class ServiceCatalogApiIT extends AbstractApiIT implements ServiceCatalogApiTestSpec {

	public final ServiceCatalogApiTestClient serviceCatalogApiTestClient;

	private String message;
	private String fieldsParameter;
	private ServiceCatalogCreateVO serviceCatalogCreateVO;
	private ServiceCatalogUpdateVO serviceCatalogUpdateVO;
	private ServiceCatalogVO expectedServiceCatalog;

	private Clock clock = mock(Clock.class);

	public ServiceCatalogApiIT(ServiceCatalogApiTestClient serviceCatalogApiTestClient,
			EntitiesApiClient entitiesApiClient, ObjectMapper objectMapper, GeneralProperties generalProperties) {
		super(entitiesApiClient, objectMapper, generalProperties);
		this.serviceCatalogApiTestClient = serviceCatalogApiTestClient;
	}

	@MockBean(Clock.class)
	public Clock clock() {
		return clock;
	}

	@ParameterizedTest
	@MethodSource("provideValidServiceCatalogs")
	public void createServiceCatalog201(String message, ServiceCatalogCreateVO serviceCatalogCreateVO,
			ServiceCatalogVO expectedServiceCatalog) throws Exception {
		this.message = message;
		this.serviceCatalogCreateVO = serviceCatalogCreateVO;
		this.expectedServiceCatalog = expectedServiceCatalog;
		createServiceCatalog201();
	}

	@Override
	public void createServiceCatalog201() throws Exception {

		Instant currentTimeInstant = Instant.ofEpochSecond(10000);
		when(clock.instant()).thenReturn(currentTimeInstant);

		HttpResponse<ServiceCatalogVO> serviceCatalogVOHttpResponse = callAndCatch(
				() -> serviceCatalogApiTestClient.createServiceCatalog(serviceCatalogCreateVO));
		assertEquals(HttpStatus.CREATED, serviceCatalogVOHttpResponse.getStatus(), message);
		String scId = serviceCatalogVOHttpResponse.body().getId();
		expectedServiceCatalog
				.id(scId)
				.href(URI.create(scId))
				.lastUpdate(currentTimeInstant);

		assertEquals(expectedServiceCatalog, serviceCatalogVOHttpResponse.body(), message);
	}

	private static Stream<Arguments> provideValidServiceCatalogs() {
		List<Arguments> testEntries = new ArrayList<>();

		ServiceCatalogCreateVO emptyCreate = ServiceCatalogCreateVOTestExample.build().lifecycleStatus("created");
		ServiceCatalogVO expectedEmpty = ServiceCatalogVOTestExample.build().lifecycleStatus("created");
		testEntries.add(Arguments.of("An empty service catalog should have been created.", emptyCreate, expectedEmpty));

		TimePeriodVO timePeriodVO = TimePeriodVOTestExample.build().endDateTime(Instant.now())
				.startDateTime(Instant.now());
		ServiceCatalogCreateVO createValidFor = ServiceCatalogCreateVOTestExample.build().validFor(timePeriodVO)
				.lifecycleStatus("created");
		ServiceCatalogVO expectedValidFor = ServiceCatalogVOTestExample.build().validFor(timePeriodVO)
				.lifecycleStatus("created");
		testEntries.add(Arguments.of("An service catalog with a validFor should have been created.", createValidFor,
				expectedValidFor));

		return testEntries.stream();
	}

	@ParameterizedTest
	@MethodSource("provideInvalidServiceCatalogs")
	public void createServiceCatalog400(String message, ServiceCatalogCreateVO invalidCreateVO) throws Exception {
		this.message = message;
		this.serviceCatalogCreateVO = invalidCreateVO;
		createServiceCatalog400();
	}

	@Override
	public void createServiceCatalog400() throws Exception {
		HttpResponse<ServiceCatalogVO> creationResponse = callAndCatch(
				() -> serviceCatalogApiTestClient.createServiceCatalog(serviceCatalogCreateVO));
		assertEquals(HttpStatus.BAD_REQUEST, creationResponse.getStatus(), message);
		Optional<ErrorDetails> optionalErrorDetails = creationResponse.getBody(ErrorDetails.class);
		assertTrue(optionalErrorDetails.isPresent(), "Error details should be provided.");
	}

	private static Stream<Arguments> provideInvalidServiceCatalogs() {
		List<Arguments> testEntries = new ArrayList<>();

		testEntries.add(Arguments.of("A service catalog with invalid related parties should not be created.",
				ServiceCatalogCreateVOTestExample.build().relatedParty(List.of(RelatedPartyVOTestExample.build()))));
		testEntries.add(Arguments.of("A service catalog with non-existent related parties should not be created.",
				ServiceCatalogCreateVOTestExample.build().relatedParty(
						List.of((RelatedPartyVOTestExample.build().id("urn:ngsi-ld:organisation:non-existent"))))));

		testEntries.add(Arguments.of("A service catalog with an invalid service category should not be created.",
				ServiceCatalogCreateVOTestExample.build().category(List.of(ServiceCategoryRefVOTestExample.build()))));
		testEntries.add(Arguments.of("A service catalog with a non-existent service category should not be created.",
				ServiceCatalogCreateVOTestExample.build().category(
						List.of(ServiceCategoryRefVOTestExample.build().id("urn:ngsi-ld:category:non-existent")))));

		return testEntries.stream();
	}

	@Disabled("Security is handled externally, thus 401 and 403 cannot happen.")
	@Test
	@Override
	public void createServiceCatalog401() throws Exception {

	}

	@Disabled("Security is handled externally, thus 401 and 403 cannot happen.")
	@Test
	@Override
	public void createServiceCatalog403() throws Exception {

	}

	@Disabled("Prohibited by the framework.")
	@Test
	@Override
	public void createServiceCatalog405() throws Exception {
	}

	@Disabled("No implicit creation, impossible state.")
	@Test
	@Override
	public void createServiceCatalog409() throws Exception {

	}

	@Override
	public void createServiceCatalog500() throws Exception {

	}

	@Test
	@Override
	public void deleteServiceCatalog204() throws Exception {
		ServiceCatalogCreateVO emptyCreate = ServiceCatalogCreateVOTestExample.build();

		HttpResponse<ServiceCatalogVO> createResponse = serviceCatalogApiTestClient.createServiceCatalog(emptyCreate);
		assertEquals(HttpStatus.CREATED, createResponse.getStatus(),
				"The service catalog should have been created first.");

		String rfId = createResponse.body().getId();

		assertEquals(HttpStatus.NO_CONTENT,
				callAndCatch(() -> serviceCatalogApiTestClient.deleteServiceCatalog(rfId)).getStatus(),
				"The service catalog should have been deleted.");

		assertEquals(HttpStatus.NOT_FOUND,
				callAndCatch(() -> serviceCatalogApiTestClient.retrieveServiceCatalog(rfId, null)).status(),
				"The service catalog should not exist anymore.");
	}

	@Disabled("400 is impossible to happen on deletion with the current implementation.")
	@Test
	@Override
	public void deleteServiceCatalog400() throws Exception {

	}

	@Disabled("Security is handled externally, thus 401 and 403 cannot happen.")
	@Test
	@Override
	public void deleteServiceCatalog401() throws Exception {

	}

	@Disabled("Security is handled externally, thus 401 and 403 cannot happen.")
	@Test
	@Override
	public void deleteServiceCatalog403() throws Exception {

	}

	@Test
	@Override
	public void deleteServiceCatalog404() throws Exception {
		HttpResponse<?> notFoundResponse = callAndCatch(
				() -> serviceCatalogApiTestClient.deleteServiceCatalog("urn:ngsi-ld:service-catalog:no-pop"));
		assertEquals(HttpStatus.NOT_FOUND,
				notFoundResponse.getStatus(),
				"No such service-catalog should exist.");

		Optional<ErrorDetails> optionalErrorDetails = notFoundResponse.getBody(ErrorDetails.class);
		assertTrue(optionalErrorDetails.isPresent(), "Error details should be provided.");

		notFoundResponse = callAndCatch(() -> serviceCatalogApiTestClient.deleteServiceCatalog("invalid-id"));
		assertEquals(HttpStatus.NOT_FOUND,
				notFoundResponse.getStatus(),
				"No such service-catalog should exist.");

		optionalErrorDetails = notFoundResponse.getBody(ErrorDetails.class);
		assertTrue(optionalErrorDetails.isPresent(), "Error details should be provided.");
	}

	@Disabled("Prohibited by the framework.")
	@Test
	@Override
	public void deleteServiceCatalog405() throws Exception {

	}

	@Disabled("Impossible status.")
	@Test
	@Override
	public void deleteServiceCatalog409() throws Exception {

	}

	@Override
	public void deleteServiceCatalog500() throws Exception {

	}

	@Test
	@Override
	public void listServiceCatalog200() throws Exception {

		List<ServiceCatalogVO> expectedServiceCatalogs = new ArrayList<>();
		for (int i = 0; i < 10; i++) {
			ServiceCatalogCreateVO serviceCatalogCreateVO = ServiceCatalogCreateVOTestExample.build();
			String id = serviceCatalogApiTestClient.createServiceCatalog(serviceCatalogCreateVO)
					.body().getId();
			ServiceCatalogVO serviceCatalogVO = ServiceCatalogVOTestExample.build();
			serviceCatalogVO
					.id(id)
					.href(URI.create(id))
					.category(null)
					.relatedParty(null);
			expectedServiceCatalogs.add(serviceCatalogVO);
		}

		HttpResponse<List<ServiceCatalogVO>> serviceCatalogResponse = callAndCatch(
				() -> serviceCatalogApiTestClient.listServiceCatalog(null, null, null));

		assertEquals(HttpStatus.OK, serviceCatalogResponse.getStatus(), "The list should be accessible.");
		assertEquals(expectedServiceCatalogs.size(), serviceCatalogResponse.getBody().get().size(),
				"All serviceCatalogs should have been returned.");
		List<ServiceCatalogVO> retrievedServiceCatalogs = serviceCatalogResponse.getBody().get();

		Map<String, ServiceCatalogVO> retrievedMap = retrievedServiceCatalogs.stream()
				.collect(Collectors.toMap(serviceCatalog -> serviceCatalog.getId(),
						serviceCatalog -> serviceCatalog));

		expectedServiceCatalogs.stream()
				.forEach(
						expectedServiceCatalog -> assertTrue(
								retrievedMap.containsKey(expectedServiceCatalog.getId()),
								String.format("All created serviceCatalogs should be returned - Missing: %s.",
										expectedServiceCatalog,
										retrievedServiceCatalogs)));
		expectedServiceCatalogs.stream().forEach(
				expectedServiceCatalog -> assertEquals(expectedServiceCatalog,
						retrievedMap.get(expectedServiceCatalog.getId()),
						"The correct serviceCatalogs should be retrieved."));

		// get with pagination
		Integer limit = 5;
		HttpResponse<List<ServiceCatalogVO>> firstPartResponse = callAndCatch(
				() -> serviceCatalogApiTestClient.listServiceCatalog(null, 0, limit));
		assertEquals(limit, firstPartResponse.body().size(),
				"Only the requested number of entries should be returend.");
		HttpResponse<List<ServiceCatalogVO>> secondPartResponse = callAndCatch(
				() -> serviceCatalogApiTestClient.listServiceCatalog(null, 0 + limit, limit));
		assertEquals(limit, secondPartResponse.body().size(),
				"Only the requested number of entries should be returend.");

		retrievedServiceCatalogs.clear();
		retrievedServiceCatalogs.addAll(firstPartResponse.body());
		retrievedServiceCatalogs.addAll(secondPartResponse.body());
		expectedServiceCatalogs.stream()
				.forEach(
						expectedServiceCatalog -> assertTrue(
								retrievedMap.containsKey(expectedServiceCatalog.getId()),
								String.format("All created serviceCatalogs should be returned - Missing: %s.",
										expectedServiceCatalog)));
		expectedServiceCatalogs.stream().forEach(
				expectedServiceCatalog -> assertEquals(expectedServiceCatalog,
						retrievedMap.get(expectedServiceCatalog.getId()),
						"The correct serviceCatalogs should be retrieved."));
	}

	@Test
	@Override
	public void listServiceCatalog400() throws Exception {
		HttpResponse<List<ServiceCatalogVO>> badRequestResponse = callAndCatch(
				() -> serviceCatalogApiTestClient.listServiceCatalog(null, -1, null));
		assertEquals(HttpStatus.BAD_REQUEST,
				badRequestResponse.getStatus(),
				"Negative offsets are impossible.");

		Optional<ErrorDetails> optionalErrorDetails = badRequestResponse.getBody(ErrorDetails.class);
		assertTrue(optionalErrorDetails.isPresent(), "Error details should be provided.");

		badRequestResponse = callAndCatch(() -> serviceCatalogApiTestClient.listServiceCatalog(null, null, -1));
		assertEquals(HttpStatus.BAD_REQUEST,
				badRequestResponse.getStatus(),
				"Negative limits are impossible.");
		optionalErrorDetails = badRequestResponse.getBody(ErrorDetails.class);
		assertTrue(optionalErrorDetails.isPresent(), "Error details should be provided.");
	}

	@Disabled("Security is handled externally, thus 401 and 403 cannot happen.")
	@Test
	@Override
	public void listServiceCatalog401() throws Exception {

	}

	@Disabled("Security is handled externally, thus 401 and 403 cannot happen.")
	@Test
	@Override
	public void listServiceCatalog403() throws Exception {

	}

	@Disabled("Not found is not possible here, will be answered with an empty list instead.")
	@Test
	@Override
	public void listServiceCatalog404() throws Exception {

	}

	@Disabled("Prohibited by the framework.")
	@Test
	@Override
	public void listServiceCatalog405() throws Exception {

	}

	@Disabled("Impossible status.")
	@Test
	@Override
	public void listServiceCatalog409() throws Exception {

	}

	@Override
	public void listServiceCatalog500() throws Exception {

	}

	@ParameterizedTest
	@MethodSource("provideServiceCatalogUpdates")
	public void patchServiceCatalog200(String message, ServiceCatalogUpdateVO serviceCatalogUpdateVO,
			ServiceCatalogVO expectedServiceCatalog) throws Exception {
		this.message = message;
		this.serviceCatalogUpdateVO = serviceCatalogUpdateVO;
		this.expectedServiceCatalog = expectedServiceCatalog;
		patchServiceCatalog200();
	}

	@Override
	public void patchServiceCatalog200() throws Exception {
		//first create
		ServiceCatalogCreateVO serviceCatalogCreateVO = ServiceCatalogCreateVOTestExample.build();

		HttpResponse<ServiceCatalogVO> createResponse = callAndCatch(
				() -> serviceCatalogApiTestClient.createServiceCatalog(serviceCatalogCreateVO));
		assertEquals(HttpStatus.CREATED, createResponse.getStatus(),
				"The service function should have been created first.");

		String serviceId = createResponse.body().getId();

		HttpResponse<ServiceCatalogVO> updateResponse = callAndCatch(
				() -> serviceCatalogApiTestClient.patchServiceCatalog(serviceId, serviceCatalogUpdateVO));
		assertEquals(HttpStatus.OK, updateResponse.getStatus(), message);

		ServiceCatalogVO updatedServiceCatalog = updateResponse.body();
		expectedServiceCatalog
				.href(URI.create(serviceId))
				.id(serviceId)
				.relatedParty(null)
				.category(null);

		assertEquals(expectedServiceCatalog, updatedServiceCatalog, message);
	}

	private static Stream<Arguments> provideServiceCatalogUpdates() {
		List<Arguments> testEntries = new ArrayList<>();

		ServiceCatalogUpdateVO lifecycleStatusUpdate = ServiceCatalogUpdateVOTestExample.build()
				.lifecycleStatus("dead");
		ServiceCatalogVO expectedLifecycleStatus = ServiceCatalogVOTestExample.build()
				.lifecycleStatus("dead");
		testEntries.add(Arguments.of("The lifecycle state should have been updated.", lifecycleStatusUpdate,
				expectedLifecycleStatus));

		ServiceCatalogUpdateVO descriptionUpdate = ServiceCatalogUpdateVOTestExample.build()
				.description("new-description");
		ServiceCatalogVO expectedDescriptionUpdate = ServiceCatalogVOTestExample.build()
				.description("new-description");
		testEntries.add(Arguments.of("The description should have been updated.", descriptionUpdate,
				expectedDescriptionUpdate));

		ServiceCatalogUpdateVO nameUpdate = ServiceCatalogUpdateVOTestExample.build()
				.name("new-name");
		ServiceCatalogVO expectedNameUpdate = ServiceCatalogVOTestExample.build()
				.name("new-name");
		testEntries.add(Arguments.of("The name should have been updated.", nameUpdate, expectedNameUpdate));

		ServiceCatalogUpdateVO versionUpdate = ServiceCatalogUpdateVOTestExample.build()
				.version("v0.0.2");
		ServiceCatalogVO expectedVersionUpdate = ServiceCatalogVOTestExample.build()
				.version("v0.0.2");
		testEntries.add(Arguments.of("The version should have been updated.", versionUpdate, expectedVersionUpdate));

		TimePeriodVO timePeriodVO = TimePeriodVOTestExample.build().endDateTime(Instant.now())
				.startDateTime(Instant.now());
		ServiceCatalogUpdateVO validForUpdate = ServiceCatalogUpdateVOTestExample.build().validFor(timePeriodVO);
		ServiceCatalogVO expectedValidForUpdate = ServiceCatalogVOTestExample.build().validFor(timePeriodVO);
		testEntries.add(Arguments.of("The validFor should have been updated.", validForUpdate, expectedValidForUpdate));

		return testEntries.stream();
	}

	@ParameterizedTest
	@MethodSource("provideInvalidUpdates")
	public void patchServiceCatalog400(String message, ServiceCatalogUpdateVO invalidUpdateVO) throws Exception {
		this.message = message;
		this.serviceCatalogUpdateVO = invalidUpdateVO;
		patchServiceCatalog400();
	}

	@Override
	public void patchServiceCatalog400() throws Exception {
		//first create
		ServiceCatalogCreateVO serviceCatalogCreateVO = ServiceCatalogCreateVOTestExample.build();

		HttpResponse<ServiceCatalogVO> createResponse = callAndCatch(
				() -> serviceCatalogApiTestClient.createServiceCatalog(serviceCatalogCreateVO));
		assertEquals(HttpStatus.CREATED, createResponse.getStatus(),
				"The service function should have been created first.");

		String serviceId = createResponse.body().getId();

		HttpResponse<ServiceCatalogVO> updateResponse = callAndCatch(
				() -> serviceCatalogApiTestClient.patchServiceCatalog(serviceId, serviceCatalogUpdateVO));
		assertEquals(HttpStatus.BAD_REQUEST, updateResponse.getStatus(), message);

		Optional<ErrorDetails> optionalErrorDetails = updateResponse.getBody(ErrorDetails.class);
		assertTrue(optionalErrorDetails.isPresent(), "Some error details should be present.");
	}

	private static Stream<Arguments> provideInvalidUpdates() {
		List<Arguments> testEntries = new ArrayList<>();

		testEntries.add(Arguments.of("An update with an invalid related party ref is not allowed.",
				ServiceCatalogUpdateVOTestExample.build()
						.relatedParty(List.of(RelatedPartyVOTestExample.build()))));
		testEntries.add(Arguments.of("An update with an non existent related party is not allowed.",
				ServiceCatalogUpdateVOTestExample.build()
						.relatedParty(List.of(RelatedPartyVOTestExample.build()
								.id("urn:ngsi-ld:organisation:non-existent")))));

		testEntries.add(Arguments.of("An update with an invalid category ref is not allowed.",
				ServiceCatalogUpdateVOTestExample.build()
						.category(List.of(ServiceCategoryRefVOTestExample.build()))));
		testEntries.add(Arguments.of("An update with an non existent category is not allowed.",
				ServiceCatalogUpdateVOTestExample.build()
						.category(List.of(ServiceCategoryRefVOTestExample.build()
								.id("urn:ngsi-ld:service-category:non-existent")))));

		return testEntries.stream();
	}

	@Disabled("Security is handled externally, thus 401 and 403 cannot happen.")
	@Test
	@Override
	public void patchServiceCatalog401() throws Exception {

	}

	@Disabled("Security is handled externally, thus 401 and 403 cannot happen.")
	@Test
	@Override
	public void patchServiceCatalog403() throws Exception {

	}

	@Test
	@Override
	public void patchServiceCatalog404() throws Exception {
		ServiceCatalogUpdateVO serviceCatalogUpdateVO = ServiceCatalogUpdateVOTestExample.build();
		assertEquals(
				HttpStatus.NOT_FOUND,
				callAndCatch(() -> serviceCatalogApiTestClient.patchServiceCatalog(
						"urn:ngsi-ld:service-catalog:not-existent", serviceCatalogUpdateVO)).getStatus(),
				"Non existent service catalog should not be updated.");
	}

	@Disabled("Prohibited by the framework.")
	@Test
	@Override
	public void patchServiceCatalog405() throws Exception {

	}

	@Override
	public void patchServiceCatalog409() throws Exception {
		// TODO: can this happen?
	}

	@Override
	public void patchServiceCatalog500() throws Exception {

	}

	@ParameterizedTest
	@MethodSource("provideFieldParameters")
	public void retrieveServiceCatalog200(String message, String fields, ServiceCatalogVO expectedServiceCatalog)
			throws Exception {
		this.fieldsParameter = fields;
		this.message = message;
		this.expectedServiceCatalog = expectedServiceCatalog;
		retrieveServiceCatalog200();
	}

	@Override
	public void retrieveServiceCatalog200() throws Exception {

		ServiceCatalogCreateVO serviceCatalogCreateVO = ServiceCatalogCreateVOTestExample.build();
		HttpResponse<ServiceCatalogVO> createResponse = callAndCatch(
				() -> serviceCatalogApiTestClient.createServiceCatalog(serviceCatalogCreateVO));
		assertEquals(HttpStatus.CREATED, createResponse.getStatus(), message);
		String id = createResponse.body().getId();

		expectedServiceCatalog
				.id(id)
				.href(URI.create(id));

		//then retrieve
		HttpResponse<ServiceCatalogVO> retrievedRF = callAndCatch(
				() -> serviceCatalogApiTestClient.retrieveServiceCatalog(id, fieldsParameter));
		assertEquals(HttpStatus.OK, retrievedRF.getStatus(), message);
		assertEquals(expectedServiceCatalog, retrievedRF.body(), message);
	}

	private static Stream<Arguments> provideFieldParameters() {
		return Stream.of(
				Arguments.of("Without a fields parameter everything should be returned.", null,
						ServiceCatalogVOTestExample.build()
								// get nulled without values
								.relatedParty(null)
								.category(null)),
				Arguments.of("Only version and the mandatory parameters should have been included.", "version",
						ServiceCatalogVOTestExample.build()
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
						"nothingToSeeHere", ServiceCatalogVOTestExample.build()
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
						"version,lastUpdate,lifecycleStatus,description", ServiceCatalogVOTestExample.build()
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
	public void retrieveServiceCatalog400() throws Exception {

	}

	@Disabled("Security is handled externally, thus 401 and 403 cannot happen.")
	@Test
	@Override
	public void retrieveServiceCatalog401() throws Exception {

	}

	@Disabled("Security is handled externally, thus 401 and 403 cannot happen.")
	@Test
	@Override
	public void retrieveServiceCatalog403() throws Exception {

	}

	@Test
	@Override
	public void retrieveServiceCatalog404() throws Exception {
		HttpResponse<ServiceCatalogVO> response = callAndCatch(
				() -> serviceCatalogApiTestClient.retrieveServiceCatalog("urn:ngsi-ld:service-function:non-existent",
						null));
		assertEquals(HttpStatus.NOT_FOUND, response.getStatus(), "No such service-catalog should exist.");

		Optional<ErrorDetails> optionalErrorDetails = response.getBody(ErrorDetails.class);
		assertTrue(optionalErrorDetails.isPresent(), "Error details should have been provided.");
	}

	@Disabled("Prohibited by the framework.")
	@Test
	@Override
	public void retrieveServiceCatalog405() throws Exception {

	}

	@Disabled("Conflict not possible on retrieval")
	@Test
	@Override
	public void retrieveServiceCatalog409() throws Exception {

	}

	@Override
	public void retrieveServiceCatalog500() throws Exception {

	}

	@Override protected String getEntityType() {
		return ServiceCatalog.TYPE_SERVICE_CATALOG;
	}
}
