package org.fiware.tmforum.servicecatalog;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import io.micronaut.test.annotation.MockBean;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import org.fiware.ngsi.api.EntitiesApiClient;
import org.fiware.servicecatalog.api.ServiceSpecificationApiTestClient;
import org.fiware.servicecatalog.api.ServiceSpecificationApiTestSpec;
import org.fiware.servicecatalog.model.CharacteristicSpecificationRelationshipVOTestExample;
import org.fiware.servicecatalog.model.CharacteristicSpecificationVO;
import org.fiware.servicecatalog.model.CharacteristicSpecificationVOTestExample;
import org.fiware.servicecatalog.model.ConstraintRefVOTestExample;
import org.fiware.servicecatalog.model.EntitySpecificationRelationshipVOTestExample;
import org.fiware.servicecatalog.model.FeatureSpecificationCharacteristicRelationshipVOTestExample;
import org.fiware.servicecatalog.model.FeatureSpecificationCharacteristicVOTestExample;
import org.fiware.servicecatalog.model.FeatureSpecificationRelationshipVOTestExample;
import org.fiware.servicecatalog.model.FeatureSpecificationVO;
import org.fiware.servicecatalog.model.FeatureSpecificationVOTestExample;
import org.fiware.servicecatalog.model.RelatedPartyVOTestExample;
import org.fiware.servicecatalog.model.ResourceSpecificationRefVOTestExample;
import org.fiware.servicecatalog.model.ServiceLevelSpecificationRefVOTestExample;
import org.fiware.servicecatalog.model.ServiceSpecRelationshipVOTestExample;
import org.fiware.servicecatalog.model.ServiceSpecificationCreateVO;
import org.fiware.servicecatalog.model.ServiceSpecificationCreateVOTestExample;
import org.fiware.servicecatalog.model.ServiceSpecificationUpdateVO;
import org.fiware.servicecatalog.model.ServiceSpecificationUpdateVOTestExample;
import org.fiware.servicecatalog.model.ServiceSpecificationVO;
import org.fiware.servicecatalog.model.ServiceSpecificationVOTestExample;
import org.fiware.servicecatalog.model.TimePeriodVO;
import org.fiware.servicecatalog.model.TimePeriodVOTestExample;
import org.fiware.tmforum.common.notification.EventHandler;
import org.fiware.tmforum.common.configuration.GeneralProperties;
import org.fiware.tmforum.common.exception.ErrorDetails;
import org.fiware.tmforum.common.test.AbstractApiIT;
import org.fiware.tmforum.common.test.ArgumentPair;
import org.fiware.tmforum.servicecatalog.domain.ServiceSpecification;
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
public class ServiceSpecificationApiIT extends AbstractApiIT implements ServiceSpecificationApiTestSpec {

	public final ServiceSpecificationApiTestClient serviceSpecificationApiTestClient;

	private String message;
	private ServiceSpecificationCreateVO serviceSpecificationCreateVO;
	private ServiceSpecificationUpdateVO serviceSpecificationUpdateVO;
	private ServiceSpecificationVO expectedServiceSpecification;
	private String fieldsParameter;

	private Clock clock = mock(Clock.class);

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

	public ServiceSpecificationApiIT(ServiceSpecificationApiTestClient serviceSpecificationApiTestClient,
			EntitiesApiClient entitiesApiClient, ObjectMapper objectMapper, GeneralProperties generalProperties) {
		super(entitiesApiClient, objectMapper, generalProperties);
		this.serviceSpecificationApiTestClient = serviceSpecificationApiTestClient;
	}

	@ParameterizedTest
	@MethodSource("provideValidServiceSpecifications")
	public void createServiceSpecification201(String message, ServiceSpecificationCreateVO serviceSpecificationCreateVO,
			ServiceSpecificationVO expectedServiceSpecification) throws Exception {
		this.message = message;
		this.serviceSpecificationCreateVO = serviceSpecificationCreateVO;
		this.expectedServiceSpecification = expectedServiceSpecification;
		createServiceSpecification201();
	}

	@Override
	public void createServiceSpecification201() throws Exception {

		Instant currentTimeInstant = Instant.ofEpochSecond(10000);
		when(clock.instant()).thenReturn(currentTimeInstant);

		HttpResponse<ServiceSpecificationVO> serviceSpecificationVOHttpResponse = callAndCatch(
				() -> serviceSpecificationApiTestClient.createServiceSpecification(serviceSpecificationCreateVO));
		assertEquals(HttpStatus.CREATED, serviceSpecificationVOHttpResponse.getStatus(), message);
		String rsId = serviceSpecificationVOHttpResponse.body().getId();
		expectedServiceSpecification.id(rsId).href(URI.create(rsId)).lastUpdate(currentTimeInstant).constraint(null);

		assertEquals(expectedServiceSpecification, serviceSpecificationVOHttpResponse.body(), message);
	}

	private static Stream<Arguments> provideValidServiceSpecifications() {
		List<Arguments> testEntries = new ArrayList<>();

		ServiceSpecificationCreateVO emptyCreate = ServiceSpecificationCreateVOTestExample.build()
				.lifecycleStatus("created");
		ServiceSpecificationVO expectedEmpty = ServiceSpecificationVOTestExample.build().lifecycleStatus("created");
		testEntries.add(
				Arguments.of("An empty service specification should have been created.", emptyCreate, expectedEmpty));

		testEntries.add(Arguments.of("A service without feature specs should have been created.",
				ServiceSpecificationCreateVOTestExample.build().lifecycleStatus("created").featureSpecification(null),
				ServiceSpecificationVOTestExample.build().lifecycleStatus("created").featureSpecification(null)));

		testEntries.add(Arguments.of("A service without characteristic specs should have been created.",
				ServiceSpecificationCreateVOTestExample.build().lifecycleStatus("created").specCharacteristic(null),
				ServiceSpecificationVOTestExample.build().lifecycleStatus("created").specCharacteristic(null)));

		TimePeriodVO timePeriodVO = TimePeriodVOTestExample.build().endDateTime(Instant.now())
				.startDateTime(Instant.now());
		ServiceSpecificationCreateVO createValidFor = ServiceSpecificationCreateVOTestExample.build()
				.validFor(timePeriodVO).lifecycleStatus("created");
		ServiceSpecificationVO expectedValidFor = ServiceSpecificationVOTestExample.build().validFor(timePeriodVO)
				.lifecycleStatus("created");
		testEntries.add(
				Arguments.of("An service specification with a validFor should have been created.", createValidFor,
						expectedValidFor));

		provideValidFeatureSpecs().map(ap ->
						Arguments.of(
								ap.message(),
								ServiceSpecificationCreateVOTestExample.build().lifecycleStatus("created")
										.featureSpecification(List.of(ap.value())),
								ServiceSpecificationVOTestExample.build().lifecycleStatus("created")
										.featureSpecification(List.of(ap.value()))))
				.forEach(testEntries::add);

		provideValidCharacteristicSpecLists().map(ap ->
						Arguments.of(
								String.format("Service should have been created - %s", ap.message()),
								ServiceSpecificationCreateVOTestExample.build().lifecycleStatus("created")
										.specCharacteristic(ap.value()),
								ServiceSpecificationVOTestExample.build().lifecycleStatus("created")
										.specCharacteristic(ap.value())
						))
				.forEach(testEntries::add);

		return testEntries.stream();
	}

	private static Stream<ArgumentPair<List<CharacteristicSpecificationVO>>> provideValidCharacteristicSpecLists() {
		List<ArgumentPair<List<CharacteristicSpecificationVO>>> validCharacteristicSpecs = new ArrayList<>();

		validCharacteristicSpecs.add(
				new ArgumentPair<>("A single char without rels should be valid", List.of(
						CharacteristicSpecificationVOTestExample.build().id("cs-1").charSpecRelationship(null)
								.characteristicValueSpecification(null).validFor(null))));

		validCharacteristicSpecs.add(
				new ArgumentPair<>("Multiple chars without rels should be valid", List.of(
						CharacteristicSpecificationVOTestExample.build().id("cs-1").charSpecRelationship(null)
								.characteristicValueSpecification(null).validFor(null),
						CharacteristicSpecificationVOTestExample.build().id("cs-2").charSpecRelationship(null)
								.characteristicValueSpecification(null).validFor(null))));

		validCharacteristicSpecs.add(
				new ArgumentPair<>("Multiple chars with rels should be valid", List.of(
						CharacteristicSpecificationVOTestExample.build().id("cs-1").charSpecRelationship(null)
								.characteristicValueSpecification(null).validFor(null),
						CharacteristicSpecificationVOTestExample.build().id("cs-2")
								.characteristicValueSpecification(null).validFor(null)
								.charSpecRelationship(
										List.of(CharacteristicSpecificationRelationshipVOTestExample.build()
												.characteristicSpecificationId("cs-1").validFor(null))))));
		return validCharacteristicSpecs.stream();
	}

	private static Stream<ArgumentPair<FeatureSpecificationVO>> provideValidFeatureSpecs() {
		List<ArgumentPair<FeatureSpecificationVO>> validFeatureSpecs = new ArrayList<>();

		validFeatureSpecs.add(new ArgumentPair<>("Feature specification with feature spec char rel should be created.",
				FeatureSpecificationVOTestExample.build()
						.constraint(null)
						.validFor(null)
						.featureSpecRelationship(null)
						.featureSpecCharacteristic(List.of(
								FeatureSpecificationCharacteristicVOTestExample.build()
										.validFor(null)
										.featureSpecCharacteristicValue(null)
										.featureSpecCharRelationship(
												List.of(FeatureSpecificationCharacteristicRelationshipVOTestExample.build()
														.validFor(null)
														.resourceSpecificationId(null)))
						))));
		validFeatureSpecs.add(new ArgumentPair<>("Feature specification with feature spec rel should be created.",
				FeatureSpecificationVOTestExample.build()
						.constraint(null)
						.validFor(null)
						.featureSpecCharacteristic(null)
						.featureSpecRelationship(List.of(
								FeatureSpecificationRelationshipVOTestExample.build()
										.validFor(null)))));

		return validFeatureSpecs.stream();
	}

	@ParameterizedTest
	@MethodSource("provideInvalidServiceSpecifications")
	public void createServiceSpecification400(String message, ServiceSpecificationCreateVO invalidCreateVO)
			throws Exception {
		this.message = message;
		this.serviceSpecificationCreateVO = invalidCreateVO;
		createServiceSpecification400();
	}

	@Override
	public void createServiceSpecification400() throws Exception {
		HttpResponse<ServiceSpecificationVO> creationResponse = callAndCatch(
				() -> serviceSpecificationApiTestClient.createServiceSpecification(serviceSpecificationCreateVO));
		assertEquals(HttpStatus.BAD_REQUEST, creationResponse.getStatus(), message);
		Optional<ErrorDetails> optionalErrorDetails = creationResponse.getBody(ErrorDetails.class);
		assertTrue(optionalErrorDetails.isPresent(), "Error details should be provided.");
	}

	private static Stream<Arguments> provideInvalidServiceSpecifications() {
		List<Arguments> testEntries = new ArrayList<>();

		testEntries.add(Arguments.of("A service specification with an invalid relatedParty should not be created.",
				ServiceSpecificationCreateVOTestExample.build()
						.relatedParty(List.of(RelatedPartyVOTestExample.build()))));
		testEntries.add(Arguments.of("A service specification with a non-existent relatedParty should not be created.",
				ServiceSpecificationCreateVOTestExample.build().relatedParty(
						List.of(RelatedPartyVOTestExample.build().id("urn:ngsi-ld:organization:non-existent")))));

		testEntries
				.addAll(provideInvalidFeatureSpecs()
						.map(ap -> Arguments.of(ap.message(), ServiceSpecificationCreateVOTestExample.build()
								.featureSpecification(List.of(ap.value()))))
						.toList());

		testEntries.add(
				Arguments.of("A service specification with an invalid entitySpecRelationship should not be created.",
						ServiceSpecificationCreateVOTestExample.build().entitySpecRelationship(
								List.of(EntitySpecificationRelationshipVOTestExample.build()))));
		testEntries.add(Arguments.of(
				"A service specification with a non-existent entitySpecRelationship should not be created.",
				ServiceSpecificationCreateVOTestExample.build().entitySpecRelationship(
						List.of(EntitySpecificationRelationshipVOTestExample.build()
								.id("urn:ngsi-ld:entity:non-existent")))));

		testEntries.add(Arguments.of("A service specification with an invalid resource spec ref should not be created.",
				ServiceSpecificationCreateVOTestExample.build()
						.resourceSpecification(List.of(ResourceSpecificationRefVOTestExample.build()))));
		testEntries.add(
				Arguments.of("A service specification with a non-existent resource spec ref should not be created.",
						ServiceSpecificationCreateVOTestExample.build()
								.resourceSpecification(List.of(ResourceSpecificationRefVOTestExample.build()
										.id("urn:ngsi-ld:resource-specification:non-existent")))));

		testEntries.add(
				Arguments.of("A service specification with an invalid service level spec ref should not be created.",
						ServiceSpecificationCreateVOTestExample.build()
								.serviceLevelSpecification(
										List.of(ServiceLevelSpecificationRefVOTestExample.build()))));
		testEntries.add(Arguments.of(
				"A service specification with a non-existent service level spec ref should not be created.",
				ServiceSpecificationCreateVOTestExample.build()
						.serviceLevelSpecification(List.of(ServiceLevelSpecificationRefVOTestExample.build()
								.id("urn:ngsi-ld:service-level-specification:non-existent")))));

		testEntries.add(
				Arguments.of("A service specification with an invalid service spec relationship should not be created.",
						ServiceSpecificationCreateVOTestExample.build()
								.serviceSpecRelationship(List.of(ServiceSpecRelationshipVOTestExample.build()))));
		testEntries.add(Arguments.of(
				"A service specification with a non-existent service spec relationship should not be created.",
				ServiceSpecificationCreateVOTestExample.build()
						.serviceSpecRelationship(List.of(ServiceSpecRelationshipVOTestExample.build()
								.id("urn:ngsi-ld:service-specification:non-existent")))));

		provideInvalidCharacteristicSpecsLists()
				.map(ap -> Arguments.of(
						String.format("A service specifcation with invalid char specs should not be created - %s",
								ap.message()),
						ServiceSpecificationCreateVOTestExample.build()
								.specCharacteristic(ap.value())
				))
				.forEach(testEntries::add);

		return testEntries.stream();
	}

	private static Stream<ArgumentPair<List<CharacteristicSpecificationVO>>> provideInvalidCharacteristicSpecsLists() {
		List<ArgumentPair<List<CharacteristicSpecificationVO>>> invalidCharacteristicSpecLists = new ArrayList<>();

		invalidCharacteristicSpecLists.add(new ArgumentPair<>(
				"A char spec with an invalid char spec rel is invalid.",
				List.of(CharacteristicSpecificationVOTestExample.build()
						.charSpecRelationship(List.of(CharacteristicSpecificationRelationshipVOTestExample.build()
								.characteristicSpecificationId("non-existent"))))
		));
		invalidCharacteristicSpecLists.add(new ArgumentPair<>(
				"Char specs with duplicate IDs are invalid.",
				List.of(CharacteristicSpecificationVOTestExample.build().id("cs-1"),
						CharacteristicSpecificationVOTestExample.build().id("cs-1"))
		));

		return invalidCharacteristicSpecLists.stream();
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
				new ArgumentPair<>("Feature specification with non-existent feature id on spec rel should fail.",
						FeatureSpecificationVOTestExample.build()
								.featureSpecRelationship(List.of(FeatureSpecificationRelationshipVOTestExample.build()
										.featureId("non-existent")))));

		return invalidFeatureSpecs.stream();
	}

	@Disabled("Security is handled externally, thus 401 and 403 cannot happen.")
	@Test
	@Override
	public void createServiceSpecification401() throws Exception {

	}

	@Disabled("Security is handled externally, thus 401 and 403 cannot happen.")
	@Test
	@Override
	public void createServiceSpecification403() throws Exception {

	}

	@Disabled("Prohibited by the framework.")
	@Test
	@Override
	public void createServiceSpecification405() throws Exception {
	}

	@Disabled("No implicit creation, impossible state.")
	@Test
	@Override
	public void createServiceSpecification409() throws Exception {

	}

	@Override
	public void createServiceSpecification500() throws Exception {

	}

	@Test
	@Override
	public void deleteServiceSpecification204() throws Exception {
		ServiceSpecificationCreateVO emptyCreate = ServiceSpecificationCreateVOTestExample.build();

		HttpResponse<ServiceSpecificationVO> createResponse = serviceSpecificationApiTestClient.createServiceSpecification(
				emptyCreate);
		assertEquals(HttpStatus.CREATED, createResponse.getStatus(),
				"The service specification should have been created first.");

		String rfId = createResponse.body().getId();

		assertEquals(HttpStatus.NO_CONTENT,
				callAndCatch(() -> serviceSpecificationApiTestClient.deleteServiceSpecification(rfId)).getStatus(),
				"The service specification should have been deleted.");

		assertEquals(HttpStatus.NOT_FOUND,
				callAndCatch(() -> serviceSpecificationApiTestClient.retrieveServiceSpecification(rfId, null)).status(),
				"The service specification should not exist anymore.");
	}

	@Disabled("400 is impossible to happen on deletion with the current implementation.")
	@Test
	@Override
	public void deleteServiceSpecification400() throws Exception {

	}

	@Disabled("Security is handled externally, thus 401 and 403 cannot happen.")
	@Test
	@Override
	public void deleteServiceSpecification401() throws Exception {

	}

	@Disabled("Security is handled externally, thus 401 and 403 cannot happen.")
	@Test
	@Override
	public void deleteServiceSpecification403() throws Exception {

	}

	@Test
	@Override
	public void deleteServiceSpecification404() throws Exception {
		HttpResponse<?> notFoundResponse = callAndCatch(
				() -> serviceSpecificationApiTestClient.deleteServiceSpecification(
						"urn:ngsi-ld:resource-specification:no-pop"));
		assertEquals(HttpStatus.NOT_FOUND,
				notFoundResponse.getStatus(),
				"No such resource-specification should exist.");

		Optional<ErrorDetails> optionalErrorDetails = notFoundResponse.getBody(ErrorDetails.class);
		assertTrue(optionalErrorDetails.isPresent(), "Error details should be provided.");

		notFoundResponse = callAndCatch(
				() -> serviceSpecificationApiTestClient.deleteServiceSpecification("invalid-id"));
		assertEquals(HttpStatus.NOT_FOUND,
				notFoundResponse.getStatus(),
				"No such resource-specification should exist.");

		optionalErrorDetails = notFoundResponse.getBody(ErrorDetails.class);
		assertTrue(optionalErrorDetails.isPresent(), "Error details should be provided.");
	}

	@Disabled("Prohibited by the framework.")
	@Test
	@Override
	public void deleteServiceSpecification405() throws Exception {

	}

	@Disabled("Impossible status.")
	@Test
	@Override
	public void deleteServiceSpecification409() throws Exception {

	}

	@Override
	public void deleteServiceSpecification500() throws Exception {

	}

	@Test
	@Override
	public void listServiceSpecification200() throws Exception {

		List<ServiceSpecificationVO> expectedServiceSpecifications = new ArrayList<>();
		for (int i = 0; i < 10; i++) {
			ServiceSpecificationCreateVO serviceSpecificationCreateVO = ServiceSpecificationCreateVOTestExample.build();
			String id = serviceSpecificationApiTestClient.createServiceSpecification(serviceSpecificationCreateVO)
					.body().getId();
			ServiceSpecificationVO serviceSpecificationVO = ServiceSpecificationVOTestExample.build();
			serviceSpecificationVO
					.id(id)
					.href(URI.create(id))
					.entitySpecRelationship(null)
					.resourceSpecification(null)
					.serviceLevelSpecification(null)
					.serviceSpecRelationship(null)
					.constraint(null)
					.relatedParty(null);
			expectedServiceSpecifications.add(serviceSpecificationVO);
		}

		HttpResponse<List<ServiceSpecificationVO>> serviceSpecificationResponse = callAndCatch(
				() -> serviceSpecificationApiTestClient.listServiceSpecification(null, null, null));

		assertEquals(HttpStatus.OK, serviceSpecificationResponse.getStatus(), "The list should be accessible.");
		assertEquals(expectedServiceSpecifications.size(), serviceSpecificationResponse.getBody().get().size(),
				"All serviceSpecifications should have been returned.");
		List<ServiceSpecificationVO> retrievedServiceSpecifications = serviceSpecificationResponse.getBody().get();

		Map<String, ServiceSpecificationVO> retrievedMap = retrievedServiceSpecifications.stream()
				.collect(Collectors.toMap(serviceSpecification -> serviceSpecification.getId(),
						serviceSpecification -> serviceSpecification));

		expectedServiceSpecifications.stream()
				.forEach(
						expectedServiceSpecification -> assertTrue(
								retrievedMap.containsKey(expectedServiceSpecification.getId()),
								String.format("All created serviceSpecifications should be returned - Missing: %s.",
										expectedServiceSpecification,
										retrievedServiceSpecifications)));
		expectedServiceSpecifications.stream().forEach(
				expectedServiceSpecification -> assertEquals(expectedServiceSpecification,
						retrievedMap.get(expectedServiceSpecification.getId()),
						"The correct serviceSpecifications should be retrieved."));

		// get with pagination
		Integer limit = 5;
		HttpResponse<List<ServiceSpecificationVO>> firstPartResponse = callAndCatch(
				() -> serviceSpecificationApiTestClient.listServiceSpecification(null, 0, limit));
		assertEquals(limit, firstPartResponse.body().size(),
				"Only the requested number of entries should be returend.");
		HttpResponse<List<ServiceSpecificationVO>> secondPartResponse = callAndCatch(
				() -> serviceSpecificationApiTestClient.listServiceSpecification(null, 0 + limit, limit));
		assertEquals(limit, secondPartResponse.body().size(),
				"Only the requested number of entries should be returend.");

		retrievedServiceSpecifications.clear();
		retrievedServiceSpecifications.addAll(firstPartResponse.body());
		retrievedServiceSpecifications.addAll(secondPartResponse.body());
		expectedServiceSpecifications.stream()
				.forEach(
						expectedServiceSpecification -> assertTrue(
								retrievedMap.containsKey(expectedServiceSpecification.getId()),
								String.format("All created serviceSpecifications should be returned - Missing: %s.",
										expectedServiceSpecification)));
		expectedServiceSpecifications.stream().forEach(
				expectedServiceSpecification -> assertEquals(expectedServiceSpecification,
						retrievedMap.get(expectedServiceSpecification.getId()),
						"The correct serviceSpecifications should be retrieved."));
	}

	@Test
	@Override
	public void listServiceSpecification400() throws Exception {
		HttpResponse<List<ServiceSpecificationVO>> badRequestResponse = callAndCatch(
				() -> serviceSpecificationApiTestClient.listServiceSpecification(null, -1, null));
		assertEquals(HttpStatus.BAD_REQUEST,
				badRequestResponse.getStatus(),
				"Negative offsets are impossible.");

		Optional<ErrorDetails> optionalErrorDetails = badRequestResponse.getBody(ErrorDetails.class);
		assertTrue(optionalErrorDetails.isPresent(), "Error details should be provided.");

		badRequestResponse = callAndCatch(
				() -> serviceSpecificationApiTestClient.listServiceSpecification(null, null, -1));
		assertEquals(HttpStatus.BAD_REQUEST,
				badRequestResponse.getStatus(),
				"Negative limits are impossible.");
		optionalErrorDetails = badRequestResponse.getBody(ErrorDetails.class);
		assertTrue(optionalErrorDetails.isPresent(), "Error details should be provided.");
	}

	@Disabled("Security is handled externally, thus 401 and 403 cannot happen.")
	@Test
	@Override
	public void listServiceSpecification401() throws Exception {

	}

	@Disabled("Security is handled externally, thus 401 and 403 cannot happen.")
	@Test
	@Override
	public void listServiceSpecification403() throws Exception {

	}

	@Disabled("Not found is not possible here, will be answered with an empty list instead.")
	@Test
	@Override
	public void listServiceSpecification404() throws Exception {

	}

	@Disabled("Prohibited by the framework.")
	@Test
	@Override
	public void listServiceSpecification405() throws Exception {

	}

	@Disabled("Impossible status.")
	@Test
	@Override
	public void listServiceSpecification409() throws Exception {

	}

	@Override
	public void listServiceSpecification500() throws Exception {

	}

	@ParameterizedTest
	@MethodSource("provideServiceSpecificationUpdates")
	public void patchServiceSpecification200(String message, ServiceSpecificationUpdateVO serviceSpecificationUpdateVO,
			ServiceSpecificationVO expectedServiceSpecification) throws Exception {
		this.message = message;
		this.serviceSpecificationUpdateVO = serviceSpecificationUpdateVO;
		this.expectedServiceSpecification = expectedServiceSpecification;
		patchServiceSpecification200();
	}

	@Override
	public void patchServiceSpecification200() throws Exception {

		Instant currentTimeInstant = Instant.ofEpochSecond(10000);
		when(clock.instant()).thenReturn(currentTimeInstant);

		//first create
		ServiceSpecificationCreateVO serviceSpecificationCreateVO = ServiceSpecificationCreateVOTestExample.build();

		HttpResponse<ServiceSpecificationVO> createResponse = callAndCatch(
				() -> serviceSpecificationApiTestClient.createServiceSpecification(serviceSpecificationCreateVO));
		assertEquals(HttpStatus.CREATED, createResponse.getStatus(),
				"The resource function should have been created first.");

		String resourceId = createResponse.body().getId();

		HttpResponse<ServiceSpecificationVO> updateResponse = callAndCatch(
				() -> serviceSpecificationApiTestClient.patchServiceSpecification(resourceId,
						serviceSpecificationUpdateVO));
		assertEquals(HttpStatus.OK, updateResponse.getStatus(), message);

		ServiceSpecificationVO updatedServiceSpecification = updateResponse.body();
		expectedServiceSpecification.href(URI.create(resourceId)).id(resourceId).relatedParty(null)
				.lastUpdate(currentTimeInstant);

		assertEquals(expectedServiceSpecification, updatedServiceSpecification, message);
	}

	private static Stream<Arguments> provideServiceSpecificationUpdates() {
		List<Arguments> testEntries = new ArrayList<>();

		ServiceSpecificationUpdateVO lifecycleStatusUpdate = ServiceSpecificationUpdateVOTestExample.build()
				.lifecycleStatus("dead")
				.relatedParty(null)
				.constraint(null)
				.entitySpecRelationship(null)
				.resourceSpecification(null)
				.serviceLevelSpecification(null)
				.serviceSpecRelationship(null);
		ServiceSpecificationVO expectedLifecycleStatus = ServiceSpecificationVOTestExample.build()
				.lifecycleStatus("dead")
				.relatedParty(null)
				.constraint(null)
				.entitySpecRelationship(null)
				.resourceSpecification(null)
				.serviceLevelSpecification(null)
				.serviceSpecRelationship(null);
		testEntries.add(Arguments.of("The lifecycle state should have been updated.", lifecycleStatusUpdate,
				expectedLifecycleStatus));

		ServiceSpecificationUpdateVO descriptionUpdate = ServiceSpecificationUpdateVOTestExample.build()
				.description("new-description")
				.relatedParty(null)
				.constraint(null)
				.entitySpecRelationship(null)
				.resourceSpecification(null)
				.serviceLevelSpecification(null)
				.serviceSpecRelationship(null);
		ServiceSpecificationVO expectedDescriptionUpdate = ServiceSpecificationVOTestExample.build()
				.description("new-description")
				.relatedParty(null)
				.constraint(null)
				.entitySpecRelationship(null)
				.resourceSpecification(null)
				.serviceLevelSpecification(null)
				.serviceSpecRelationship(null);
		testEntries.add(Arguments.of("The description should have been updated.", descriptionUpdate,
				expectedDescriptionUpdate));

		ServiceSpecificationUpdateVO nameUpdate = ServiceSpecificationUpdateVOTestExample.build()
				.name("new-name")
				.relatedParty(null)
				.constraint(null)
				.entitySpecRelationship(null)
				.resourceSpecification(null)
				.serviceLevelSpecification(null)
				.serviceSpecRelationship(null);
		ServiceSpecificationVO expectedNameUpdate = ServiceSpecificationVOTestExample.build()
				.name("new-name")
				.relatedParty(null)
				.constraint(null)
				.entitySpecRelationship(null)
				.resourceSpecification(null)
				.serviceLevelSpecification(null)
				.serviceSpecRelationship(null);
		testEntries.add(Arguments.of("The name should have been updated.", nameUpdate, expectedNameUpdate));

		ServiceSpecificationUpdateVO versionUpdate = ServiceSpecificationUpdateVOTestExample.build()
				.version("v0.0.2")
				.relatedParty(null)
				.constraint(null)
				.entitySpecRelationship(null)
				.resourceSpecification(null)
				.serviceLevelSpecification(null)
				.serviceSpecRelationship(null);
		ServiceSpecificationVO expectedVersionUpdate = ServiceSpecificationVOTestExample.build()
				.version("v0.0.2")
				.relatedParty(null)
				.constraint(null)
				.entitySpecRelationship(null)
				.resourceSpecification(null)
				.serviceLevelSpecification(null)
				.serviceSpecRelationship(null);
		testEntries.add(Arguments.of("The version should have been updated.", versionUpdate, expectedVersionUpdate));

		TimePeriodVO timePeriodVO = TimePeriodVOTestExample.build().endDateTime(Instant.now())
				.startDateTime(Instant.now());
		ServiceSpecificationUpdateVO validForUpdate = ServiceSpecificationUpdateVOTestExample.build()
				.validFor(timePeriodVO)
				.relatedParty(null)
				.constraint(null)
				.entitySpecRelationship(null)
				.resourceSpecification(null)
				.serviceLevelSpecification(null)
				.serviceSpecRelationship(null);
		ServiceSpecificationVO expectedValidForUpdate = ServiceSpecificationVOTestExample.build()
				.validFor(timePeriodVO)
				.relatedParty(null)
				.constraint(null)
				.entitySpecRelationship(null)
				.resourceSpecification(null)
				.serviceLevelSpecification(null)
				.serviceSpecRelationship(null);
		testEntries.add(Arguments.of("The validFor should have been updated.", validForUpdate, expectedValidForUpdate));

		provideValidFeatureSpecs().map(ap ->
						Arguments.of(
								ap.message(),
								ServiceSpecificationUpdateVOTestExample.build()
										.lifecycleStatus("created")
										.featureSpecification(List.of(ap.value()))
										.relatedParty(null)
										.constraint(null)
										.entitySpecRelationship(null)
										.resourceSpecification(null)
										.serviceLevelSpecification(null)
										.serviceSpecRelationship(null),
								ServiceSpecificationVOTestExample.build()
										.lifecycleStatus("created")
										.featureSpecification(List.of(ap.value()))
										.relatedParty(null)
										.constraint(null)
										.entitySpecRelationship(null)
										.resourceSpecification(null)
										.serviceLevelSpecification(null)
										.serviceSpecRelationship(null)))
				.forEach(testEntries::add);

		provideValidCharacteristicSpecLists().map(ap ->
						Arguments.of(String.format("Characteristic spec should be updated - %s", ap.message()),
								ServiceSpecificationUpdateVOTestExample.build()
										.lifecycleStatus("updated")
										.specCharacteristic(ap.value())
										.relatedParty(null)
										.constraint(null)
										.entitySpecRelationship(null)
										.resourceSpecification(null)
										.serviceLevelSpecification(null)
										.serviceSpecRelationship(null),
								ServiceSpecificationVOTestExample.build()
										.lifecycleStatus("updated")
										.specCharacteristic(ap.value())
										.relatedParty(null)
										.constraint(null)
										.entitySpecRelationship(null)
										.resourceSpecification(null)
										.serviceLevelSpecification(null)
										.serviceSpecRelationship(null)))
				.forEach(testEntries::add);

		return testEntries.stream();
	}

	@ParameterizedTest
	@MethodSource("provideInvalidUpdates")
	public void patchServiceSpecification400(String message, ServiceSpecificationUpdateVO invalidUpdateVO)
			throws Exception {
		this.message = message;
		this.serviceSpecificationUpdateVO = invalidUpdateVO;
		patchServiceSpecification400();
	}

	@Override
	public void patchServiceSpecification400() throws Exception {
		//first create
		ServiceSpecificationCreateVO serviceSpecificationCreateVO = ServiceSpecificationCreateVOTestExample.build();

		HttpResponse<ServiceSpecificationVO> createResponse = callAndCatch(
				() -> serviceSpecificationApiTestClient.createServiceSpecification(serviceSpecificationCreateVO));
		assertEquals(HttpStatus.CREATED, createResponse.getStatus(),
				"The resource function should have been created first.");

		String resourceId = createResponse.body().getId();

		HttpResponse<ServiceSpecificationVO> updateResponse = callAndCatch(
				() -> serviceSpecificationApiTestClient.patchServiceSpecification(resourceId,
						serviceSpecificationUpdateVO));
		assertEquals(HttpStatus.BAD_REQUEST, updateResponse.getStatus(), message);

		Optional<ErrorDetails> optionalErrorDetails = updateResponse.getBody(ErrorDetails.class);
		assertTrue(optionalErrorDetails.isPresent(), "Some error details should be present.");
	}

	private static Stream<Arguments> provideInvalidUpdates() {
		List<Arguments> testEntries = new ArrayList<>();

		testEntries.add(Arguments.of("An update with an invalid relatedParty is not allowed.",
				ServiceSpecificationUpdateVOTestExample.build()
						.relatedParty(List.of(RelatedPartyVOTestExample.build()))));
		testEntries.add(Arguments.of("An update with an non existent relatedParty is not allowed.",
				ServiceSpecificationUpdateVOTestExample.build()
						.relatedParty(List.of(RelatedPartyVOTestExample.build()
								.id("urn:ngsi-ld:organization:non-existent")))));
		testEntries.addAll(
				provideInvalidFeatureSpecs()
						.map(ap -> Arguments.of(ap.message(), ServiceSpecificationUpdateVOTestExample.build()
								.featureSpecification(List.of(ap.value()))))
						.toList());
		testEntries.addAll(
				provideInvalidCharacteristicSpecsLists()
						.map(ap -> Arguments.of(
								String.format("Updates with invalid char specs should not be allowed - %s",
										ap.message()),
								ServiceSpecificationUpdateVOTestExample.build().specCharacteristic(ap.value())))
						.toList());

		return testEntries.stream();
	}

	@Disabled("Security is handled externally, thus 401 and 403 cannot happen.")
	@Test
	@Override
	public void patchServiceSpecification401() throws Exception {

	}

	@Disabled("Security is handled externally, thus 401 and 403 cannot happen.")
	@Test
	@Override
	public void patchServiceSpecification403() throws Exception {

	}

	@Test
	@Override
	public void patchServiceSpecification404() throws Exception {
		ServiceSpecificationUpdateVO serviceSpecificationUpdateVO = ServiceSpecificationUpdateVOTestExample.build();
		assertEquals(
				HttpStatus.NOT_FOUND,
				callAndCatch(() -> serviceSpecificationApiTestClient.patchServiceSpecification(
						"urn:ngsi-ld:resource-specification:not-existent", serviceSpecificationUpdateVO)).getStatus(),
				"Non existent service specification should not be updated.");
	}

	@Disabled("Prohibited by the framework.")
	@Test
	@Override
	public void patchServiceSpecification405() throws Exception {

	}

	@Override
	public void patchServiceSpecification409() throws Exception {
		// TODO: can this happen?
	}

	@Override
	public void patchServiceSpecification500() throws Exception {

	}

	@ParameterizedTest
	@MethodSource("provideFieldParameters")
	public void retrieveServiceSpecification200(String message, String fields,
			ServiceSpecificationVO expectedServiceSpecification) throws Exception {
		this.fieldsParameter = fields;
		this.message = message;
		this.expectedServiceSpecification = expectedServiceSpecification;
		retrieveServiceSpecification200();
	}

	@Override
	public void retrieveServiceSpecification200() throws Exception {

		when(clock.instant()).thenReturn(Instant.MAX);

		ServiceSpecificationCreateVO serviceSpecificationCreateVO = ServiceSpecificationCreateVOTestExample.build();
		HttpResponse<ServiceSpecificationVO> createResponse = callAndCatch(
				() -> serviceSpecificationApiTestClient.createServiceSpecification(serviceSpecificationCreateVO));
		assertEquals(HttpStatus.CREATED, createResponse.getStatus(), message);
		String id = createResponse.body().getId();

		expectedServiceSpecification
				.id(id)
				.href(URI.create(id));

		//then retrieve
		HttpResponse<ServiceSpecificationVO> retrievedResourceSpec = callAndCatch(
				() -> serviceSpecificationApiTestClient.retrieveServiceSpecification(id, fieldsParameter));
		assertEquals(HttpStatus.OK, retrievedResourceSpec.getStatus(), message);
		assertEquals(expectedServiceSpecification, retrievedResourceSpec.body(), message);
	}

	private static Stream<Arguments> provideFieldParameters() {
		return Stream.of(
				Arguments.of("Without a fields parameter everything should be returned.", null,
						ServiceSpecificationVOTestExample.build()
								.relatedParty(null)
								.constraint(null)
								.entitySpecRelationship(null)
								.resourceSpecification(null)
								.serviceLevelSpecification(null)
								.serviceSpecRelationship(null)
								.lastUpdate(Instant.MAX)),
				Arguments.of("Only version and the mandatory parameters should have been included.", "version",
						ServiceSpecificationVOTestExample.build()
								.description(null)
								.isBundle(null)
								.lastUpdate(null)
								.lifecycleStatus(null)
								.name(null)
								.attachment(null)
								.constraint(null)
								.entitySpecRelationship(null)
								.featureSpecification(null)
								.relatedParty(null)
								.resourceSpecification(null)
								.serviceLevelSpecification(null)
								.serviceSpecRelationship(null)
								.specCharacteristic(null)
								.targetEntitySchema(null)
								.validFor(null)
								.atBaseType(null)
								.atSchemaLocation(null)
								.atType(null)),
				Arguments.of(
						"Only the mandatory parameters should have been included when a non-existent field was requested.",
						"nothingToSeeHere", ServiceSpecificationVOTestExample.build()
								.description(null)
								.isBundle(null)
								.lastUpdate(null)
								.lifecycleStatus(null)
								.name(null)
								.version(null)
								.attachment(null)
								.constraint(null)
								.entitySpecRelationship(null)
								.featureSpecification(null)
								.relatedParty(null)
								.resourceSpecification(null)
								.serviceLevelSpecification(null)
								.serviceSpecRelationship(null)
								.specCharacteristic(null)
								.targetEntitySchema(null)
								.validFor(null)
								.atBaseType(null)
								.atSchemaLocation(null)
								.atType(null)),
				Arguments.of(
						"Only version, lastUpdate, lifecycleStatus, description and the mandatory parameters should have been included.",
						"version,lastUpdate,lifecycleStatus,description", ServiceSpecificationVOTestExample.build()
								.isBundle(null)
								.name(null)
								.lastUpdate(Instant.MAX)
								.attachment(null)
								.constraint(null)
								.entitySpecRelationship(null)
								.featureSpecification(null)
								.relatedParty(null)
								.resourceSpecification(null)
								.serviceLevelSpecification(null)
								.serviceSpecRelationship(null)
								.specCharacteristic(null)
								.targetEntitySchema(null)
								.validFor(null)
								.atBaseType(null)
								.atSchemaLocation(null)
								.atType(null)));
	}

	@Disabled("400 cannot happen, only 404")
	@Test
	@Override
	public void retrieveServiceSpecification400() throws Exception {

	}

	@Disabled("Security is handled externally, thus 401 and 403 cannot happen.")
	@Test
	@Override
	public void retrieveServiceSpecification401() throws Exception {

	}

	@Disabled("Security is handled externally, thus 401 and 403 cannot happen.")
	@Test
	@Override
	public void retrieveServiceSpecification403() throws Exception {

	}

	@Test
	@Override
	public void retrieveServiceSpecification404() throws Exception {
		HttpResponse<ServiceSpecificationVO> response = callAndCatch(
				() -> serviceSpecificationApiTestClient.retrieveServiceSpecification(
						"urn:ngsi-ld:resource-function:non-existent", null));
		assertEquals(HttpStatus.NOT_FOUND, response.getStatus(), "No such resource-specification should exist.");

		Optional<ErrorDetails> optionalErrorDetails = response.getBody(ErrorDetails.class);
		assertTrue(optionalErrorDetails.isPresent(), "Error details should have been provided.");
	}

	@Disabled("Prohibited by the framework.")
	@Test
	@Override
	public void retrieveServiceSpecification405() throws Exception {

	}

	@Disabled("Conflict not possible on retrieval")
	@Test
	@Override
	public void retrieveServiceSpecification409() throws Exception {

	}

	@Override
	public void retrieveServiceSpecification500() throws Exception {

	}

	@Override
	protected String getEntityType() {
		return ServiceSpecification.TYPE_SERVICE_SPECIFICATION;
	}
}
