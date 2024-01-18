package org.fiware.tmforum.resourcefunction;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import io.micronaut.test.annotation.MockBean;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import org.fiware.ngsi.api.EntitiesApiClient;
import org.fiware.resourcefunction.api.MigrateApiTestClient;
import org.fiware.resourcefunction.api.MigrateApiTestSpec;
import org.fiware.resourcefunction.model.CharacteristicVOTestExample;
import org.fiware.resourcefunction.model.ConnectionPointRefVOTestExample;
import org.fiware.resourcefunction.model.MigrateCreateVO;
import org.fiware.resourcefunction.model.MigrateCreateVOTestExample;
import org.fiware.resourcefunction.model.MigrateVO;
import org.fiware.resourcefunction.model.MigrateVOTestExample;
import org.fiware.resourcefunction.model.PlaceRefVOTestExample;
import org.fiware.resourcefunction.model.ResourceFunctionRefVOTestExample;
import org.fiware.resourcefunction.model.TaskStateTypeVO;
import org.fiware.tmforum.common.notification.EventHandler;
import org.fiware.tmforum.common.configuration.GeneralProperties;
import org.fiware.tmforum.common.exception.ErrorDetails;
import org.fiware.tmforum.common.test.AbstractApiIT;
import org.fiware.tmforum.resourcefunction.domain.Migrate;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@MicronautTest(packages = { "org.fiware.tmforum.resourcefunction" })
public class MigrateApiIT extends AbstractApiIT implements MigrateApiTestSpec {

	public final MigrateApiTestClient migrateApiTestClient;

	private String message;
	private MigrateCreateVO migrateCreateVO;
	private MigrateVO expectedMigrateVO;

	public MigrateApiIT(MigrateApiTestClient migrateApiTestClient, EntitiesApiClient entitiesApiClient,
			ObjectMapper objectMapper, GeneralProperties generalProperties) {
		super(entitiesApiClient, objectMapper, generalProperties);
		this.migrateApiTestClient = migrateApiTestClient;
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
	@MethodSource("provideValidMigrates")
	public void createMigrate201(String message, MigrateCreateVO migrateCreateVO, MigrateVO expectedMigrateVO)
			throws Exception {
		this.message = message;
		this.migrateCreateVO = migrateCreateVO;
		this.expectedMigrateVO = expectedMigrateVO;
		createMigrate201();
	}

	@Override
	public void createMigrate201() throws Exception {

		HttpResponse<MigrateVO> migrateVOHttpResponse = callAndCatch(
				() -> migrateApiTestClient.createMigrate(migrateCreateVO));
		assertEquals(HttpStatus.CREATED, migrateVOHttpResponse.getStatus(), message);
		String migrateId = migrateVOHttpResponse.body().getId();

		expectedMigrateVO.id(migrateId).href(URI.create(migrateId));

		assertEquals(expectedMigrateVO, migrateVOHttpResponse.body(), message);
	}

	private static Stream<Arguments> provideValidMigrates() {
		List<Arguments> testEntries = new ArrayList<>();

		MigrateCreateVO migrateCreateVO = MigrateCreateVOTestExample.build().resourceFunction(null).place(null);
		MigrateVO expectedMigrateVO = MigrateVOTestExample.build().resourceFunction(null).place(null);
		testEntries.add(Arguments.of("An empty migrate should have been created.", migrateCreateVO, expectedMigrateVO));

		MigrateCreateVO adminStateModificationCreateVO = MigrateCreateVOTestExample.build()
				.adminStateModification("make-it-migratethy").place(null).resourceFunction(null);
		MigrateVO expectedAdminStateModificationVO = MigrateVOTestExample.build()
				.adminStateModification("make-it-migratethy").resourceFunction(null).place(null);
		testEntries.add(Arguments.of("A migrate with an adminStateModification should have been created.",
				adminStateModificationCreateVO, expectedAdminStateModificationVO));

		MigrateCreateVO causeCreateVO = MigrateCreateVOTestExample.build().cause("needs to move").place(null)
				.resourceFunction(null);
		MigrateVO expectedCauseVO = MigrateVOTestExample.build().cause("needs to move").resourceFunction(null)
				.place(null);
		testEntries.add(
				Arguments.of("A migrate with a cause should have been created.", causeCreateVO, expectedCauseVO));

		MigrateCreateVO completionModeCreateVO = MigrateCreateVOTestExample.build().completionMode("done").place(null)
				.resourceFunction(null);
		MigrateVO expectedCompletionModeVO = MigrateVOTestExample.build().completionMode("done").resourceFunction(null)
				.place(null);
		testEntries.add(
				Arguments.of("A migrate with a completionMode should have been created.", completionModeCreateVO,
						expectedCompletionModeVO));

		MigrateCreateVO nameCreateVO = MigrateCreateVOTestExample.build().name("my-name").place(null)
				.resourceFunction(null);
		MigrateVO expectedNameVO = MigrateVOTestExample.build().name("my-name").resourceFunction(null).place(null);
		testEntries.add(Arguments.of("A migrate with a name should have been created.", nameCreateVO, expectedNameVO));

		MigrateCreateVO priorityCreateVO = MigrateCreateVOTestExample.build().priority(10).place(null)
				.resourceFunction(null);
		MigrateVO expectedPriorityVO = MigrateVOTestExample.build().priority(10).resourceFunction(null).place(null);
		testEntries.add(Arguments.of("A migrate with a priority should have been created.", priorityCreateVO,
				expectedPriorityVO));

		MigrateCreateVO stateCreateVO = MigrateCreateVOTestExample.build().state(TaskStateTypeVO.INPROGRESS).place(null)
				.resourceFunction(null);
		MigrateVO expectedStateVO = MigrateVOTestExample.build().state(TaskStateTypeVO.INPROGRESS)
				.resourceFunction(null).place(null);
		testEntries.add(
				Arguments.of("A migrate with a state should have been created.", stateCreateVO, expectedStateVO));

		MigrateCreateVO startTimeCreateVO = MigrateCreateVOTestExample.build().startTime("10-10-2022").place(null)
				.resourceFunction(null);
		MigrateVO expectedStartTimeVO = MigrateVOTestExample.build().startTime("10-10-2022").resourceFunction(null)
				.place(null);
		testEntries.add(Arguments.of("A migrate with a start time should have been created.", startTimeCreateVO,
				expectedStartTimeVO));

		String charId = "urn:" + UUID.randomUUID();
		MigrateCreateVO additionalParamsCreateVO = MigrateCreateVOTestExample.build()
				.characteristics(List.of(CharacteristicVOTestExample.build().id(charId))).place(null)
				.resourceFunction(null);
		MigrateVO expectedAdditionalParamsVO = MigrateVOTestExample.build()
				.characteristics(List.of(CharacteristicVOTestExample.build().id(charId))).resourceFunction(null)
				.place(null);
		testEntries.add(
				Arguments.of("A migrate with additional parameters should have been created.", additionalParamsCreateVO,
						expectedAdditionalParamsVO));

		return testEntries.stream();
	}

	@ParameterizedTest
	@MethodSource("provideInvalidCreates")
	public void createMigrate400(String message, MigrateCreateVO invalidCreateVO) throws Exception {
		this.message = message;
		this.migrateCreateVO = invalidCreateVO;
		createMigrate400();
	}

	@Override
	public void createMigrate400() throws Exception {
		HttpResponse<MigrateVO> creationResponse = callAndCatch(
				() -> migrateApiTestClient.createMigrate(migrateCreateVO));
		assertEquals(HttpStatus.BAD_REQUEST, creationResponse.getStatus(), message);
		Optional<ErrorDetails> optionalErrorDetails = creationResponse.getBody(ErrorDetails.class);
		assertTrue(optionalErrorDetails.isPresent(), "Error details should be provided.");
	}

	private static Stream<Arguments> provideInvalidCreates() {
		List<Arguments> testEntries = new ArrayList<>();

		testEntries.add(Arguments.of("A migrate with an invalid place ref should not have been created.",
				MigrateCreateVOTestExample.build().place(PlaceRefVOTestExample.build()).resourceFunction(null)));
		testEntries.add(Arguments.of("A migrate with a non existent place ref should not have been created.",
				MigrateCreateVOTestExample.build()
						.place(PlaceRefVOTestExample.build().id("urn:ngsi-ld:place:non-existent"))
						.resourceFunction(null)));

		testEntries.add(Arguments.of("A migrate with an invalid resource function should not have been created.",
				MigrateCreateVOTestExample.build().place(null)
						.resourceFunction(ResourceFunctionRefVOTestExample.build())));
		testEntries.add(Arguments.of("A migrate with a non existent resource function should not have been created.",
				MigrateCreateVOTestExample.build().place(null).resourceFunction(
						ResourceFunctionRefVOTestExample.build().id("urn:ngsi-ld:resource-function:non-existent"))));

		testEntries.add(Arguments.of("A migrate with invalid add connection points should not have been created.",
				MigrateCreateVOTestExample.build().place(null).resourceFunction(null)
						.addConnectionPoint(List.of(ConnectionPointRefVOTestExample.build()))));
		testEntries.add(Arguments.of("A migrate with nonexitent add connection points should not have been created.",
				MigrateCreateVOTestExample.build().place(null).resourceFunction(null).addConnectionPoint(
						List.of(ConnectionPointRefVOTestExample.build()
								.id("urn:ngsi-ld:connection-point:non-existent")))));

		testEntries.add(Arguments.of("A migrate with invalid remove connection points should not have been created.",
				MigrateCreateVOTestExample.build().place(null).resourceFunction(null)
						.removeConnectionPoint(List.of(ConnectionPointRefVOTestExample.build()))));
		testEntries.add(Arguments.of("A migrate with nonexitent remove connection points should not have been created.",
				MigrateCreateVOTestExample.build().place(null).resourceFunction(null).removeConnectionPoint(
						List.of(ConnectionPointRefVOTestExample.build()
								.id("urn:ngsi-ld:connection-point:non-existent")))));

		return testEntries.stream();
	}

	@Disabled("Security is handled externally, thus 401 and 403 cannot happen.")
	@Test
	@Override
	public void createMigrate401() throws Exception {

	}

	@Disabled("Security is handled externally, thus 401 and 403 cannot happen.")
	@Test
	@Override
	public void createMigrate403() throws Exception {

	}

	@Disabled("Prohibited by the framework.")
	@Test
	@Override
	public void createMigrate405() throws Exception {

	}

	@Disabled("Migrate doesn't have 'implicit' entities and id is generated by the implementation, no conflict possible.")
	@Test
	@Override
	public void createMigrate409() throws Exception {
	}

	@Override
	public void createMigrate500() throws Exception {

	}

	@Test
	@Override
	public void listMigrate200() throws Exception {

		List<MigrateVO> expectedMigrates = new ArrayList<>();
		for (int i = 0; i < 10; i++) {
			MigrateCreateVO migrateCreateVO = MigrateCreateVOTestExample.build()
					.place(null)
					.resourceFunction(null);
			String id = migrateApiTestClient.createMigrate(migrateCreateVO)
					.body().getId();
			MigrateVO migrateVO = MigrateVOTestExample.build();
			migrateVO
					.id(id)
					.href(URI.create(id))
					.addConnectionPoint(null)
					.removeConnectionPoint(null)
					.place(null)
					.resourceFunction(null);
			expectedMigrates.add(migrateVO);
		}

		HttpResponse<List<MigrateVO>> migrateResponse = callAndCatch(
				() -> migrateApiTestClient.listMigrate(null, null, null));

		assertEquals(HttpStatus.OK, migrateResponse.getStatus(), "The list should be accessible.");
		assertEquals(expectedMigrates.size(), migrateResponse.getBody().get().size(),
				"All migrates should have been returned.");
		List<MigrateVO> retrievedMigrates = migrateResponse.getBody().get();

		Map<String, MigrateVO> retrievedMap = retrievedMigrates.stream()
				.collect(Collectors.toMap(migrate -> migrate.getId(),
						migrate -> migrate));

		expectedMigrates.stream()
				.forEach(
						expectedMigrate -> assertTrue(
								retrievedMap.containsKey(expectedMigrate.getId()),
								String.format("All created migrates should be returned - Missing: %s.",
										expectedMigrate,
										retrievedMigrates)));
		expectedMigrates.stream().forEach(
				expectedMigrate -> assertEquals(expectedMigrate,
						retrievedMap.get(expectedMigrate.getId()),
						"The correct migrates should be retrieved."));

		// get with pagination
		Integer limit = 5;
		HttpResponse<List<MigrateVO>> firstPartResponse = callAndCatch(
				() -> migrateApiTestClient.listMigrate(null, 0, limit));
		assertEquals(limit, firstPartResponse.body().size(),
				"Only the requested number of entries should be returend.");
		HttpResponse<List<MigrateVO>> secondPartResponse = callAndCatch(
				() -> migrateApiTestClient.listMigrate(null, 0 + limit, limit));
		assertEquals(limit, secondPartResponse.body().size(),
				"Only the requested number of entries should be returend.");

		retrievedMigrates.clear();
		retrievedMigrates.addAll(firstPartResponse.body());
		retrievedMigrates.addAll(secondPartResponse.body());
		expectedMigrates.stream()
				.forEach(
						expectedMigrate -> assertTrue(
								retrievedMap.containsKey(expectedMigrate.getId()),
								String.format("All created migrates should be returned - Missing: %s.",
										expectedMigrate)));
		expectedMigrates.stream().forEach(
				expectedMigrate -> assertEquals(expectedMigrate,
						retrievedMap.get(expectedMigrate.getId()),
						"The correct migrates should be retrieved."));
	}

	@Test
	@Override
	public void listMigrate400() throws Exception {
		HttpResponse<List<MigrateVO>> badRequestResponse = callAndCatch(
				() -> migrateApiTestClient.listMigrate(null, -1, null));
		assertEquals(HttpStatus.BAD_REQUEST,
				badRequestResponse.getStatus(),
				"Negative offsets are impossible.");

		Optional<ErrorDetails> optionalErrorDetails = badRequestResponse.getBody(ErrorDetails.class);
		assertTrue(optionalErrorDetails.isPresent(), "Error details should be provided.");

		badRequestResponse = callAndCatch(() -> migrateApiTestClient.listMigrate(null, null, -1));
		assertEquals(HttpStatus.BAD_REQUEST,
				badRequestResponse.getStatus(),
				"Negative limits are impossible.");
		optionalErrorDetails = badRequestResponse.getBody(ErrorDetails.class);
		assertTrue(optionalErrorDetails.isPresent(), "Error details should be provided.");
	}

	@Disabled("Security is handled externally, thus 401 and 403 cannot happen.")
	@Test
	@Override
	public void listMigrate401() throws Exception {

	}

	@Disabled("Security is handled externally, thus 401 and 403 cannot happen.")
	@Test
	@Override
	public void listMigrate403() throws Exception {

	}

	@Disabled("Not found is not possible here, will be answered with an empty list instead.")
	@Test
	@Override
	public void listMigrate404() throws Exception {

	}

	@Disabled("Prohibited by the framework.")
	@Test
	@Override
	public void listMigrate405() throws Exception {

	}

	@Disabled("Impossible status.")
	@Test
	@Override
	public void listMigrate409() throws Exception {

	}

	@Override
	public void listMigrate500() throws Exception {

	}

	@Test
	@Override
	public void retrieveMigrate200() throws Exception {

		MigrateCreateVO migrateCreateVO = MigrateCreateVOTestExample.build().place(null).resourceFunction(null);

		HttpResponse<MigrateVO> migrateVOHttpResponse = callAndCatch(
				() -> migrateApiTestClient.createMigrate(migrateCreateVO));
		assertEquals(HttpStatus.CREATED, migrateVOHttpResponse.getStatus(),
				"The initial create should be successfully.");
		String migrateId = migrateVOHttpResponse.body().getId();

		MigrateVO expectedMigrate = MigrateVOTestExample.build()
				.id(migrateId)
				.href(URI.create(migrateId))
				.place(null)
				.resourceFunction(null)
				.addConnectionPoint(null)
				.removeConnectionPoint(null);

		HttpResponse<MigrateVO> retrieveResponse = callAndCatch(
				() -> migrateApiTestClient.retrieveMigrate(migrateId, null));
		assertEquals(HttpStatus.OK, retrieveResponse.getStatus(), "The retrieval should be successfully.");
		assertEquals(expectedMigrate, retrieveResponse.body(), "The expected migrate should be returend.");
	}

	@Disabled("400 cannot happen, only 404")
	@Test
	@Override
	public void retrieveMigrate400() throws Exception {

	}

	@Disabled("Security is handled externally, thus 401 and 403 cannot happen.")
	@Test
	@Override
	public void retrieveMigrate401() throws Exception {

	}

	@Disabled("Security is handled externally, thus 401 and 403 cannot happen.")
	@Test
	@Override
	public void retrieveMigrate403() throws Exception {

	}

	@Test
	@Override
	public void retrieveMigrate404() throws Exception {

		HttpResponse<MigrateVO> response = callAndCatch(
				() -> migrateApiTestClient.retrieveMigrate("urn:ngsi-ld:migrate:non-existent", null));
		assertEquals(HttpStatus.NOT_FOUND, response.getStatus(), "No such migrate should exist.");

		Optional<ErrorDetails> optionalErrorDetails = response.getBody(ErrorDetails.class);
		assertTrue(optionalErrorDetails.isPresent(), "Error details should have been provided.");
	}

	@Disabled("Prohibited by the framework.")
	@Test
	@Override
	public void retrieveMigrate405() throws Exception {

	}

	@Disabled("Conflict not possible on retrieval")
	@Test
	@Override
	public void retrieveMigrate409() throws Exception {

	}

	@Override
	public void retrieveMigrate500() throws Exception {

	}

	@Override
	protected String getEntityType() {
		return Migrate.TYPE_MIGRATE;
	}
}
