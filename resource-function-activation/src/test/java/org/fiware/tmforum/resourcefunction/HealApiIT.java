package org.fiware.tmforum.resourcefunction;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import lombok.RequiredArgsConstructor;
import org.fiware.ngsi.api.EntitiesApiClient;
import org.fiware.resourcefunction.api.HealApiTestClient;
import org.fiware.resourcefunction.api.HealApiTestSpec;
import org.fiware.resourcefunction.model.CharacteristicVOTestExample;
import org.fiware.resourcefunction.model.HealCreateVO;
import org.fiware.resourcefunction.model.HealCreateVOTestExample;
import org.fiware.resourcefunction.model.HealPolicyRefVOTestExample;
import org.fiware.resourcefunction.model.HealVO;
import org.fiware.resourcefunction.model.HealVOTestExample;
import org.fiware.resourcefunction.model.ResourceFunctionRefVOTestExample;
import org.fiware.resourcefunction.model.TaskStateTypeVO;
import org.fiware.tmforum.common.configuration.GeneralProperties;
import org.fiware.tmforum.common.exception.ErrorDetails;
import org.fiware.tmforum.common.test.AbstractApiIT;
import org.fiware.tmforum.resourcefunction.domain.Heal;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@MicronautTest(packages = { "org.fiware.tmforum.resourcefunction" })
public class HealApiIT extends AbstractApiIT implements HealApiTestSpec {

	public final HealApiTestClient healApiTestClient;

	private String message;
	private HealCreateVO healCreateVO;
	private HealVO expectedHealVO;

	public HealApiIT(HealApiTestClient healApiTestClient, EntitiesApiClient entitiesApiClient,
			ObjectMapper objectMapper, GeneralProperties generalProperties) {
		super(entitiesApiClient, objectMapper, generalProperties);
		this.healApiTestClient = healApiTestClient;
	}

	@ParameterizedTest
	@MethodSource("provideValidHeals")
	public void createHeal201(String message, HealCreateVO healCreateVO, HealVO expectedHealVO) throws Exception {
		this.message = message;
		this.healCreateVO = healCreateVO;
		this.expectedHealVO = expectedHealVO;
		createHeal201();
	}

	@Override
	public void createHeal201() throws Exception {

		HttpResponse<HealVO> healVOHttpResponse = callAndCatch(() -> healApiTestClient.createHeal(healCreateVO));
		assertEquals(HttpStatus.CREATED, healVOHttpResponse.getStatus(), message);
		String healId = healVOHttpResponse.body().getId();

		expectedHealVO.id(healId).href(healId);

		assertEquals(expectedHealVO, healVOHttpResponse.body(), message);
	}

	private static Stream<Arguments> provideValidHeals() {
		List<Arguments> testEntries = new ArrayList<>();

		HealCreateVO healCreateVO = HealCreateVOTestExample.build().healPolicy(null).resourceFunction(null);
		HealVO expectedHealVO = HealVOTestExample.build().resourceFunction(null).healPolicy(null);
		testEntries.add(Arguments.of("An empty heal should have been created.", healCreateVO, expectedHealVO));

		HealCreateVO actionCreateVO = HealCreateVOTestExample.build().healAction("make-it-healthy").healPolicy(null)
				.resourceFunction(null);
		HealVO expectedActionVO = HealVOTestExample.build().healAction("make-it-healthy").resourceFunction(null)
				.healPolicy(null);
		testEntries.add(
				Arguments.of("A heal with an action should have been created.", actionCreateVO, expectedActionVO));

		HealCreateVO causeCreateVO = HealCreateVOTestExample.build().cause("its-unhealthy").healPolicy(null)
				.resourceFunction(null);
		HealVO expectedCauseVO = HealVOTestExample.build().cause("its-unhealthy").resourceFunction(null)
				.healPolicy(null);
		testEntries.add(Arguments.of("A heal with a cause should have been created.", causeCreateVO, expectedCauseVO));

		HealCreateVO degreeCreateVO = HealCreateVOTestExample.build().degreeOfHealing("not-yet-healthy-again")
				.healPolicy(null).resourceFunction(null);
		HealVO expectedDegreeVO = HealVOTestExample.build().degreeOfHealing("not-yet-healthy-again")
				.resourceFunction(null).healPolicy(null);
		testEntries.add(Arguments.of("A heal with a degree of healing should have been created.", degreeCreateVO,
				expectedDegreeVO));

		HealCreateVO nameCreateVO = HealCreateVOTestExample.build().name("my-name").healPolicy(null)
				.resourceFunction(null);
		HealVO expectedNameVO = HealVOTestExample.build().name("my-name").resourceFunction(null).healPolicy(null);
		testEntries.add(Arguments.of("A heal with a name should have been created.", nameCreateVO, expectedNameVO));

		HealCreateVO stateCreateVO = HealCreateVOTestExample.build().state(TaskStateTypeVO.INPROGRESS).healPolicy(null)
				.resourceFunction(null);
		HealVO expectedStateVO = HealVOTestExample.build().state(TaskStateTypeVO.INPROGRESS).resourceFunction(null)
				.healPolicy(null);
		testEntries.add(Arguments.of("A heal with a state should have been created.", stateCreateVO, expectedStateVO));

		HealCreateVO startTimeCreateVO = HealCreateVOTestExample.build().startTime("10-10-2022").healPolicy(null)
				.resourceFunction(null);
		HealVO expectedStartTimeVO = HealVOTestExample.build().startTime("10-10-2022").resourceFunction(null)
				.healPolicy(null);
		testEntries.add(Arguments.of("A heal with a start time should have been created.", startTimeCreateVO,
				expectedStartTimeVO));

		HealCreateVO additionalParamsCreateVO = HealCreateVOTestExample.build()
				.additionalParms(List.of(CharacteristicVOTestExample.build())).healPolicy(null).resourceFunction(null);
		HealVO expectedAdditionalParamsVO = HealVOTestExample.build()
				.additionalParms(List.of(CharacteristicVOTestExample.build())).resourceFunction(null).healPolicy(null);
		testEntries.add(
				Arguments.of("A heal with additional parameters should have been created.", additionalParamsCreateVO,
						expectedAdditionalParamsVO));

		return testEntries.stream();
	}

	@ParameterizedTest
	@MethodSource("provideInvalidCreates")
	public void createHeal400(String message, HealCreateVO invalidCreateVO) throws Exception {
		this.message = message;
		this.healCreateVO = invalidCreateVO;
		createHeal400();
	}

	@Override
	public void createHeal400() throws Exception {
		HttpResponse<HealVO> creationResponse = callAndCatch(() -> healApiTestClient.createHeal(healCreateVO));
		assertEquals(HttpStatus.BAD_REQUEST, creationResponse.getStatus(), message);
		Optional<ErrorDetails> optionalErrorDetails = creationResponse.getBody(ErrorDetails.class);
		assertTrue(optionalErrorDetails.isPresent(), "Error details should be provided.");
	}

	private static Stream<Arguments> provideInvalidCreates() {
		List<Arguments> testEntries = new ArrayList<>();

		testEntries.add(Arguments.of("A heal with an invalid policy should not have been created.",
				HealCreateVOTestExample.build().healPolicy(HealPolicyRefVOTestExample.build()).resourceFunction(null)));
		testEntries.add(Arguments.of("A heal with a non existent policy should not have been created.",
				HealCreateVOTestExample.build()
						.healPolicy(HealPolicyRefVOTestExample.build().id("urn:ngsi-ld:heal-policy:non-existent"))
						.resourceFunction(null)));

		testEntries.add(Arguments.of("A heal with an invalid resource function should not have been created.",
				HealCreateVOTestExample.build().healPolicy(null)
						.resourceFunction(ResourceFunctionRefVOTestExample.build())));
		testEntries.add(Arguments.of("A heal with a non existent resource function should not have been created.",
				HealCreateVOTestExample.build().healPolicy(null).resourceFunction(
						ResourceFunctionRefVOTestExample.build().id("urn:ngsi-ld:resource-function:non-existent"))));

		return testEntries.stream();
	}

	@Disabled("Security is handled externally, thus 401 and 403 cannot happen.")
	@Test
	@Override
	public void createHeal401() throws Exception {

	}

	@Disabled("Security is handled externally, thus 401 and 403 cannot happen.")
	@Test
	@Override
	public void createHeal403() throws Exception {

	}

	@Disabled("Prohibited by the framework.")
	@Test
	@Override
	public void createHeal405() throws Exception {

	}

	@Disabled("Heal doesn't have 'implicit' entities and id is generated by the implementation, no conflict possible.")
	@Test
	@Override
	public void createHeal409() throws Exception {
	}

	@Override
	public void createHeal500() throws Exception {

	}

	@Test
	@Override
	public void listHeal200() throws Exception {

		List<HealVO> expectedHeals = new ArrayList<>();
		for (int i = 0; i < 10; i++) {
			HealCreateVO healCreateVO = HealCreateVOTestExample.build()
					.healPolicy(null)
					.resourceFunction(null);
			String id = healApiTestClient.createHeal(healCreateVO)
					.body()
					.getId();
			HealVO healVO = HealVOTestExample.build();
			healVO
					.id(id)
					.href(id)
					.healPolicy(null)
					.resourceFunction(null);
			expectedHeals.add(healVO);
		}

		HttpResponse<List<HealVO>> healResponse = callAndCatch(
				() -> healApiTestClient.listHeal(null, null, null));

		assertEquals(HttpStatus.OK, healResponse.getStatus(), "The list should be accessible.");
		assertEquals(expectedHeals.size(), healResponse.getBody().get().size(),
				"All heals should have been returned.");
		List<HealVO> retrievedHeals = healResponse.getBody().get();

		Map<String, HealVO> retrievedMap = retrievedHeals.stream()
				.collect(Collectors.toMap(heal -> heal.getId(),
						heal -> heal));

		expectedHeals.stream()
				.forEach(
						expectedHeal -> assertTrue(
								retrievedMap.containsKey(expectedHeal.getId()),
								String.format("All created heals should be returned - Missing: %s.",
										expectedHeal,
										retrievedHeals)));
		expectedHeals.stream().forEach(
				expectedHeal -> assertEquals(expectedHeal,
						retrievedMap.get(expectedHeal.getId()),
						"The correct heals should be retrieved."));

		// get with pagination
		Integer limit = 5;
		HttpResponse<List<HealVO>> firstPartResponse = callAndCatch(
				() -> healApiTestClient.listHeal(null, 0, limit));
		assertEquals(limit, firstPartResponse.body().size(),
				"Only the requested number of entries should be returend.");
		HttpResponse<List<HealVO>> secondPartResponse = callAndCatch(
				() -> healApiTestClient.listHeal(null, 0 + limit, limit));
		assertEquals(limit, secondPartResponse.body().size(),
				"Only the requested number of entries should be returend.");

		retrievedHeals.clear();
		retrievedHeals.addAll(firstPartResponse.body());
		retrievedHeals.addAll(secondPartResponse.body());
		expectedHeals.stream()
				.forEach(
						expectedHeal -> assertTrue(
								retrievedMap.containsKey(expectedHeal.getId()),
								String.format("All created heals should be returned - Missing: %s.",
										expectedHeal)));
		expectedHeals.stream().forEach(
				expectedHeal -> assertEquals(expectedHeal,
						retrievedMap.get(expectedHeal.getId()),
						"The correct heals should be retrieved."));
	}

	@Test
	@Override
	public void listHeal400() throws Exception {
		HttpResponse<List<HealVO>> badRequestResponse = callAndCatch(() -> healApiTestClient.listHeal(null, -1, null));
		assertEquals(HttpStatus.BAD_REQUEST,
				badRequestResponse.getStatus(),
				"Negative offsets are impossible.");

		Optional<ErrorDetails> optionalErrorDetails = badRequestResponse.getBody(ErrorDetails.class);
		assertTrue(optionalErrorDetails.isPresent(), "Error details should be provided.");

		badRequestResponse = callAndCatch(() -> healApiTestClient.listHeal(null, null, -1));
		assertEquals(HttpStatus.BAD_REQUEST,
				badRequestResponse.getStatus(),
				"Negative limits are impossible.");
		optionalErrorDetails = badRequestResponse.getBody(ErrorDetails.class);
		assertTrue(optionalErrorDetails.isPresent(), "Error details should be provided.");
	}

	@Disabled("Security is handled externally, thus 401 and 403 cannot happen.")
	@Test
	@Override
	public void listHeal401() throws Exception {

	}

	@Disabled("Security is handled externally, thus 401 and 403 cannot happen.")
	@Test
	@Override
	public void listHeal403() throws Exception {

	}

	@Disabled("Not found is not possible here, will be answered with an empty list instead.")
	@Test
	@Override
	public void listHeal404() throws Exception {

	}

	@Disabled("Prohibited by the framework.")
	@Test
	@Override
	public void listHeal405() throws Exception {

	}

	@Disabled("Impossible status.")
	@Test
	@Override
	public void listHeal409() throws Exception {

	}

	@Override
	public void listHeal500() throws Exception {

	}

	@Test
	@Override
	public void retrieveHeal200() throws Exception {

		HealCreateVO healCreateVO = HealCreateVOTestExample.build().healPolicy(null).resourceFunction(null);

		HttpResponse<HealVO> healVOHttpResponse = callAndCatch(() -> healApiTestClient.createHeal(healCreateVO));
		assertEquals(HttpStatus.CREATED, healVOHttpResponse.getStatus(), "The initial create should be successfully.");
		String healId = healVOHttpResponse.body().getId();

		HealVO expectedHeal = HealVOTestExample.build().id(healId).href(healId).healPolicy(null).resourceFunction(null);

		HttpResponse<HealVO> retreiveResponse = callAndCatch(() -> healApiTestClient.retrieveHeal(healId, null));
		assertEquals(HttpStatus.OK, retreiveResponse.getStatus(), "The retrieval should be successfully.");
		assertEquals(expectedHeal, retreiveResponse.body(), "The expected heal should be returend.");
	}

	@Disabled("400 cannot happen, only 404")
	@Test
	@Override
	public void retrieveHeal400() throws Exception {

	}

	@Disabled("Security is handled externally, thus 401 and 403 cannot happen.")
	@Test
	@Override
	public void retrieveHeal401() throws Exception {

	}

	@Disabled("Security is handled externally, thus 401 and 403 cannot happen.")
	@Test
	@Override
	public void retrieveHeal403() throws Exception {

	}

	@Test
	@Override
	public void retrieveHeal404() throws Exception {

		HttpResponse<HealVO> response = callAndCatch(
				() -> healApiTestClient.retrieveHeal("urn:ngsi-ld:heal:non-existent", null));
		assertEquals(HttpStatus.NOT_FOUND, response.getStatus(), "No such heal should exist.");

		Optional<ErrorDetails> optionalErrorDetails = response.getBody(ErrorDetails.class);
		assertTrue(optionalErrorDetails.isPresent(), "Error details should have been provided.");
	}

	@Disabled("Prohibited by the framework.")
	@Test
	@Override
	public void retrieveHeal405() throws Exception {

	}

	@Disabled("Conflict not possible on retrieval")
	@Test
	@Override
	public void retrieveHeal409() throws Exception {

	}

	@Override
	public void retrieveHeal500() throws Exception {

	}

	@Override protected String getEntityType() {
		return Heal.TYPE_HEAL;
	}
}
