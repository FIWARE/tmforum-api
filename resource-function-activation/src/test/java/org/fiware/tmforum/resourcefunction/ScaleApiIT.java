package org.fiware.tmforum.resourcefunction;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import io.micronaut.test.annotation.MockBean;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import org.fiware.ngsi.api.EntitiesApiClient;
import org.fiware.resourcefunction.api.ScaleApiTestClient;
import org.fiware.resourcefunction.api.ScaleApiTestSpec;
import org.fiware.resourcefunction.model.*;
import org.fiware.tmforum.common.configuration.GeneralProperties;
import org.fiware.tmforum.common.exception.ErrorDetails;
import org.fiware.tmforum.common.notification.EventHandler;
import org.fiware.tmforum.common.test.AbstractApiIT;
import org.fiware.tmforum.resourcefunction.domain.Scale;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import reactor.core.publisher.Mono;

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

@MicronautTest(packages = { "org.fiware.tmforum.resourcefunction" })
public class ScaleApiIT extends AbstractApiIT implements ScaleApiTestSpec {

	public final ScaleApiTestClient scaleApiTestClient;

	private String message;
	private ScaleCreateVO scaleCreateVO;
	private ScaleVO expectedScaleVO;

	public ScaleApiIT(ScaleApiTestClient scaleApiTestClient, EntitiesApiClient entitiesApiClient,
			ObjectMapper objectMapper, GeneralProperties generalProperties) {
		super(entitiesApiClient, objectMapper, generalProperties);
		this.scaleApiTestClient = scaleApiTestClient;
	}

	@MockBean(EventHandler.class)
	public EventHandler eventHandler() {
		EventHandler eventHandler = mock(EventHandler.class);

		when(eventHandler.handleCreateEvent(any())).thenReturn(Mono.empty());
		when(eventHandler.handleUpdateEvent(any(), any())).thenReturn(Mono.empty());

		return eventHandler;
	}

	@ParameterizedTest
	@MethodSource("provideValidScales")
	public void createScale201(String message, ScaleCreateVO scaleCreateVO, ScaleVO expectedScaleVO) throws Exception {
		this.message = message;
		this.scaleCreateVO = scaleCreateVO;
		this.expectedScaleVO = expectedScaleVO;
		createScale201();
	}

	@Override
	public void createScale201() throws Exception {

		HttpResponse<ScaleVO> scaleVOHttpResponse = callAndCatch(() -> scaleApiTestClient.createScale(scaleCreateVO));
		assertEquals(HttpStatus.CREATED, scaleVOHttpResponse.getStatus(), message);
		String scaleId = scaleVOHttpResponse.body().getId();

		expectedScaleVO.id(scaleId).href(scaleId);

		assertEquals(expectedScaleVO, scaleVOHttpResponse.body(), message);
	}

	private static Stream<Arguments> provideValidScales() {
		List<Arguments> testEntries = new ArrayList<>();

		ScaleCreateVO scaleCreateVO = ScaleCreateVOTestExample.build().resourceFunction(null);
		ScaleVO expectedScaleVO = ScaleVOTestExample.build().resourceFunction(null);
		testEntries.add(Arguments.of("An empty scale should have been created.", scaleCreateVO, expectedScaleVO));

		ScaleCreateVO aspectCreateVO = ScaleCreateVOTestExample.build().aspectId("my-aspect").resourceFunction(null);
		ScaleVO expectedAspectVO = ScaleVOTestExample.build().aspectId("my-aspect").resourceFunction(null);
		testEntries.add(
				Arguments.of("A scale with an aspect  should have been created.", aspectCreateVO, expectedAspectVO));

		ScaleCreateVO stepsCreateVO = ScaleCreateVOTestExample.build().numberOfSteps(3).resourceFunction(null);
		ScaleVO expectedStepsVO = ScaleVOTestExample.build().numberOfSteps(3).resourceFunction(null);
		testEntries.add(Arguments.of("A scale with a number of steps should have been created.", stepsCreateVO,
				expectedStepsVO));

		ScaleCreateVO typeCreateVO = ScaleCreateVOTestExample.build().scaleType("up").resourceFunction(null);
		ScaleVO expectedTypeVO = ScaleVOTestExample.build().scaleType("up").resourceFunction(null);
		testEntries.add(
				Arguments.of("A scale with a scale type should have been created.", typeCreateVO, expectedTypeVO));

		ScaleCreateVO nameCreateVO = ScaleCreateVOTestExample.build().name("my-name").resourceFunction(null);
		ScaleVO expectedNameVO = ScaleVOTestExample.build().name("my-name").resourceFunction(null);
		testEntries.add(Arguments.of("A scale with a name should have been created.", nameCreateVO, expectedNameVO));

		ScaleCreateVO stateCreateVO = ScaleCreateVOTestExample.build().state(TaskStateTypeVO.INPROGRESS)
				.resourceFunction(null);
		ScaleVO expectedStateVO = ScaleVOTestExample.build().state(TaskStateTypeVO.INPROGRESS).resourceFunction(null);
		testEntries.add(Arguments.of("A scale with a state should have been created.", stateCreateVO, expectedStateVO));

		return testEntries.stream();
	}

	@ParameterizedTest
	@MethodSource("provideInvalidCreates")
	public void createScale400(String message, ScaleCreateVO invalidCreateVO) throws Exception {
		this.message = message;
		this.scaleCreateVO = invalidCreateVO;
		createScale400();
	}

	@Override
	public void createScale400() throws Exception {
		HttpResponse<ScaleVO> creationResponse = callAndCatch(() -> scaleApiTestClient.createScale(scaleCreateVO));
		assertEquals(HttpStatus.BAD_REQUEST, creationResponse.getStatus(), message);
		Optional<ErrorDetails> optionalErrorDetails = creationResponse.getBody(ErrorDetails.class);
		assertTrue(optionalErrorDetails.isPresent(), "Error details should be provided.");
	}

	private static Stream<Arguments> provideInvalidCreates() {
		List<Arguments> testEntries = new ArrayList<>();

		testEntries.add(Arguments.of("A scale with an invalid resource function should not have been created.",
				ScaleCreateVOTestExample.build().resourceFunction(ResourceFunctionRefVOTestExample.build())));
		testEntries.add(Arguments.of("A scale with a non existent resource function should not have been created.",
				ScaleCreateVOTestExample.build().resourceFunction(
						ResourceFunctionRefVOTestExample.build().id("urn:ngsi-ld:resource-function:non-existent"))));

		testEntries.add(Arguments.of("A scale wit an invalid schedule should not be created.",
				ScaleCreateVOTestExample.build()
						.resourceFunction(null)
						.schedule(List.of(ScheduleRefVOTestExample.build()))));
		testEntries.add(Arguments.of("A scale wit a non-existent schedule should not be created.",
				ScaleCreateVOTestExample.build()
						.resourceFunction(null)
						.schedule(List.of(ScheduleRefVOTestExample.build().id("urn:ngsi-ld:schedule:non-existent")))));

		return testEntries.stream();
	}

	@Disabled("Security is handled externally, thus 401 and 403 cannot happen.")
	@Test
	@Override
	public void createScale401() throws Exception {

	}

	@Disabled("Security is handled externally, thus 401 and 403 cannot happen.")
	@Test
	@Override
	public void createScale403() throws Exception {

	}

	@Disabled("Prohibited by the framework.")
	@Test
	@Override
	public void createScale405() throws Exception {

	}

	@Disabled("Scale doesn't have 'implicit' entities and id is generated by the implementation, no conflict possible.")
	@Test
	@Override
	public void createScale409() throws Exception {
	}

	@Override
	public void createScale500() throws Exception {

	}

	@Test
	@Override
	public void listScale200() throws Exception {

		List<ScaleVO> expectedScales = new ArrayList<>();
		for (int i = 0; i < 10; i++) {
			ScaleCreateVO scaleCreateVO = ScaleCreateVOTestExample.build().resourceFunction(null);
			String id = scaleApiTestClient.createScale(scaleCreateVO)
					.body().getId();
			ScaleVO scaleVO = ScaleVOTestExample.build();
			scaleVO
					.id(id)
					.href(id)
					.resourceFunction(null)
					.schedule(null);
			expectedScales.add(scaleVO);
		}

		HttpResponse<List<ScaleVO>> scaleResponse = callAndCatch(
				() -> scaleApiTestClient.listScale(null, null, null));

		assertEquals(HttpStatus.OK, scaleResponse.getStatus(), "The list should be accessible.");
		assertEquals(expectedScales.size(), scaleResponse.getBody().get().size(),
				"All scales should have been returned.");
		List<ScaleVO> retrievedScales = scaleResponse.getBody().get();

		Map<String, ScaleVO> retrievedMap = retrievedScales.stream()
				.collect(Collectors.toMap(scale -> scale.getId(),
						scale -> scale));

		expectedScales.stream()
				.forEach(
						expectedScale -> assertTrue(
								retrievedMap.containsKey(expectedScale.getId()),
								String.format("All created scales should be returned - Missing: %s.",
										expectedScale,
										retrievedScales)));
		expectedScales.stream().forEach(
				expectedScale -> assertEquals(expectedScale,
						retrievedMap.get(expectedScale.getId()),
						"The correct scales should be retrieved."));

		// get with pagination
		Integer limit = 5;
		HttpResponse<List<ScaleVO>> firstPartResponse = callAndCatch(
				() -> scaleApiTestClient.listScale(null, 0, limit));
		assertEquals(limit, firstPartResponse.body().size(),
				"Only the requested number of entries should be returend.");
		HttpResponse<List<ScaleVO>> secondPartResponse = callAndCatch(
				() -> scaleApiTestClient.listScale(null, 0 + limit, limit));
		assertEquals(limit, secondPartResponse.body().size(),
				"Only the requested number of entries should be returend.");

		retrievedScales.clear();
		retrievedScales.addAll(firstPartResponse.body());
		retrievedScales.addAll(secondPartResponse.body());
		expectedScales.stream()
				.forEach(
						expectedScale -> assertTrue(
								retrievedMap.containsKey(expectedScale.getId()),
								String.format("All created scales should be returned - Missing: %s.",
										expectedScale)));
		expectedScales.stream().forEach(
				expectedScale -> assertEquals(expectedScale,
						retrievedMap.get(expectedScale.getId()),
						"The correct scales should be retrieved."));
	}

	@Test
	@Override
	public void listScale400() throws Exception {
		HttpResponse<List<ScaleVO>> badRequestResponse = callAndCatch(
				() -> scaleApiTestClient.listScale(null, -1, null));
		assertEquals(HttpStatus.BAD_REQUEST,
				badRequestResponse.getStatus(),
				"Negative offsets are impossible.");

		Optional<ErrorDetails> optionalErrorDetails = badRequestResponse.getBody(ErrorDetails.class);
		assertTrue(optionalErrorDetails.isPresent(), "Error details should be provided.");

		badRequestResponse = callAndCatch(() -> scaleApiTestClient.listScale(null, null, -1));
		assertEquals(HttpStatus.BAD_REQUEST,
				badRequestResponse.getStatus(),
				"Negative limits are impossible.");
		optionalErrorDetails = badRequestResponse.getBody(ErrorDetails.class);
		assertTrue(optionalErrorDetails.isPresent(), "Error details should be provided.");
	}

	@Disabled("Security is handled externally, thus 401 and 403 cannot happen.")
	@Test
	@Override
	public void listScale401() throws Exception {

	}

	@Disabled("Security is handled externally, thus 401 and 403 cannot happen.")
	@Test
	@Override
	public void listScale403() throws Exception {

	}

	@Disabled("Not found is not possible here, will be answered with an empty list instead.")
	@Test
	@Override
	public void listScale404() throws Exception {

	}

	@Disabled("Prohibited by the framework.")
	@Test
	@Override
	public void listScale405() throws Exception {

	}

	@Disabled("Impossible status.")
	@Test
	@Override
	public void listScale409() throws Exception {

	}

	@Override
	public void listScale500() throws Exception {

	}

	@Test
	@Override
	public void retrieveScale200() throws Exception {

		ScaleCreateVO scaleCreateVO = ScaleCreateVOTestExample.build().resourceFunction(null);

		HttpResponse<ScaleVO> scaleVOHttpResponse = callAndCatch(() -> scaleApiTestClient.createScale(scaleCreateVO));
		assertEquals(HttpStatus.CREATED, scaleVOHttpResponse.getStatus(), "The initial create should be successfully.");
		String scaleId = scaleVOHttpResponse.body().getId();

		ScaleVO expectedScale = ScaleVOTestExample.build().id(scaleId).href(scaleId).resourceFunction(null)
				.schedule(null);

		HttpResponse<ScaleVO> retreiveResponse = callAndCatch(() -> scaleApiTestClient.retrieveScale(scaleId, null));
		assertEquals(HttpStatus.OK, retreiveResponse.getStatus(), "The retrieval should be successfully.");
		assertEquals(expectedScale, retreiveResponse.body(), "The expected scale should be returend.");
	}

	@Disabled("400 cannot happen, only 404")
	@Test
	@Override
	public void retrieveScale400() throws Exception {

	}

	@Disabled("Security is handled externally, thus 401 and 403 cannot happen.")
	@Test
	@Override
	public void retrieveScale401() throws Exception {

	}

	@Disabled("Security is handled externally, thus 401 and 403 cannot happen.")
	@Test
	@Override
	public void retrieveScale403() throws Exception {

	}

	@Test
	@Override
	public void retrieveScale404() throws Exception {

		HttpResponse<ScaleVO> response = callAndCatch(
				() -> scaleApiTestClient.retrieveScale("urn:ngsi-ld:scale:non-existent", null));
		assertEquals(HttpStatus.NOT_FOUND, response.getStatus(), "No such scale should exist.");

		Optional<ErrorDetails> optionalErrorDetails = response.getBody(ErrorDetails.class);
		assertTrue(optionalErrorDetails.isPresent(), "Error details should have been provided.");
	}

	@Disabled("Prohibited by the framework.")
	@Test
	@Override
	public void retrieveScale405() throws Exception {

	}

	@Disabled("Conflict not possible on retrieval")
	@Test
	@Override
	public void retrieveScale409() throws Exception {

	}

	@Override
	public void retrieveScale500() throws Exception {

	}

	@Override
	protected String getEntityType() {
		return Scale.TYPE_SCALE;
	}
}
