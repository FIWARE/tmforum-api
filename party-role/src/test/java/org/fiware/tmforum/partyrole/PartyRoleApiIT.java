package org.fiware.tmforum.partyrole;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.fiware.ngsi.api.EntitiesApiClient;
import org.fiware.partyRole.api.PartyRoleApiTestClient;
import org.fiware.partyRole.api.PartyRoleApiTestSpec;
import org.fiware.partyRole.model.AgreementRefVOTestExample;
import org.fiware.partyRole.model.PartyRoleCreateVO;
import org.fiware.partyRole.model.PartyRoleCreateVOTestExample;
import org.fiware.partyRole.model.PartyRoleUpdateVO;
import org.fiware.partyRole.model.PartyRoleUpdateVOTestExample;
import org.fiware.partyRole.model.PartyRoleVO;
import org.fiware.partyRole.model.PartyRoleVOTestExample;
import org.fiware.partyRole.model.RelatedPartyVO;
import org.fiware.partyRole.model.RelatedPartyVOTestExample;
import org.fiware.partyRole.model.TimePeriodVO;
import org.fiware.partyRole.model.TimePeriodVOTestExample;
import org.fiware.tmforum.common.configuration.GeneralProperties;
import org.fiware.tmforum.common.exception.ErrorDetails;
import org.fiware.tmforum.common.test.AbstractApiIT;
import org.fiware.tmforum.partyrole.domain.PartyRole;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import lombok.val;

@MicronautTest(packages = {"org.fiware.tmforum.partyRole"})
public class PartyRoleApiIT extends AbstractApiIT implements PartyRoleApiTestSpec {

	private final PartyRoleApiTestClient prApiTestClient;
	private String message;
	private String fields;
	private PartyRoleCreateVO prCreateVO;
	private PartyRoleUpdateVO prUpdateVO;
	private PartyRoleVO expectedpr;

	private final EntitiesApiClient entitiesApiClient;
	private final ObjectMapper objectMapper;
	private final GeneralProperties generalProperties;

	protected PartyRoleApiIT(PartyRoleApiTestClient agSpecApiTestClient,
							 EntitiesApiClient entitiesApiClient, ObjectMapper objectMapper, GeneralProperties generalProperties) {
		super(entitiesApiClient, objectMapper, generalProperties);
		this.prApiTestClient = agSpecApiTestClient;
		this.entitiesApiClient = entitiesApiClient;
		this.objectMapper = objectMapper;
		this.generalProperties = generalProperties;
	}

	@Override
	protected String getEntityType() {
		return PartyRole.TYPE_PR;
	}

	private static Stream<Arguments> provideValidAgSpec() {
		List<Arguments> testEntries = new ArrayList<>();
		PartyRoleCreateVO prCreateVO = PartyRoleCreateVOTestExample.build().atSchemaLocation(null).engagedParty(null);
		PartyRoleVO expectedAgSpec = PartyRoleVOTestExample.build().atSchemaLocation(null).engagedParty(null);
		testEntries.add(
				Arguments.of("Empty PartyRole should have been created", prCreateVO,
						expectedAgSpec));
		TimePeriodVO timePeriodVO = TimePeriodVOTestExample.build().endDateTime(Instant.now())
				.startDateTime(Instant.now());
		prCreateVO = PartyRoleCreateVOTestExample.build().atSchemaLocation(null).engagedParty(null)
				.validFor(timePeriodVO);
		expectedAgSpec = PartyRoleVOTestExample.build().atSchemaLocation(null).engagedParty(null)
				.validFor(timePeriodVO);
		testEntries.add(
				Arguments.of("PartyRole with a engagedParty should have been created",
						prCreateVO,
						expectedAgSpec));

		return testEntries.stream();
	}

	@ParameterizedTest
	@MethodSource({"provideValidAgSpec"})
	public void createPartyRole201(String message, PartyRoleCreateVO agSpecCreateVO,
								   PartyRoleVO expectedAgSpec) throws Exception {
		this.message = message;
		this.prCreateVO = agSpecCreateVO;
		this.expectedpr = expectedAgSpec;
		createPartyRole201();
	}

	@Override
	public void createPartyRole201() throws Exception {
		HttpResponse<PartyRoleVO> agCreateResponse = callAndCatch(
				() -> prApiTestClient.createPartyRole(null, prCreateVO));
		assertEquals(HttpStatus.CREATED, agCreateResponse.getStatus(), message);
		String id = agCreateResponse.body().getId();
		expectedpr.id(id).href(id);
		assertEquals(expectedpr, agCreateResponse.body(), message);
	}

	private static Stream<Arguments> provideInvalidAg() {
		List<Arguments> testEntries = new ArrayList<>();
		RelatedPartyVO engagedParty = RelatedPartyVOTestExample.build().atSchemaLocation(null)
				.id("non-existent");
		PartyRoleCreateVO prCreateVO = PartyRoleCreateVOTestExample.build().atSchemaLocation(null).engagedParty(engagedParty);
		testEntries.add(
				Arguments.of("An PartyRole with an invalid engagedParty should not be created",
						prCreateVO));

		prCreateVO = PartyRoleCreateVOTestExample.build().atSchemaLocation(null)
				.relatedParty(List
						.of(RelatedPartyVOTestExample.build().atSchemaLocation(null).id(
								"urn:ngsi-ld:PartyRole:non-existent")));
		testEntries.add(
				Arguments.of("PartyRole with an invalid relatedParty should not be created",
						prCreateVO));

		prCreateVO = PartyRoleCreateVOTestExample.build().atSchemaLocation(null).agreement(List.of(AgreementRefVOTestExample.build().atSchemaLocation(null).id("non-existent").name("Lambda")));
		testEntries.add(Arguments.of("PartyRole with a invalid agreementRef should not be created", prCreateVO));

		return testEntries.stream();
	}

	@ParameterizedTest
	@MethodSource({"provideInvalidAg"})
	public void createCustomer400(String message, PartyRoleCreateVO agCreateVO) throws Exception {
		this.message = message;
		this.prCreateVO = agCreateVO;
		createPartyRole400();
	}

	@Override
	public void createPartyRole400() throws Exception {
		HttpResponse<PartyRoleVO> agSpecCreateResponse = callAndCatch(() -> prApiTestClient.createPartyRole(null, prCreateVO));
		assertEquals(HttpStatus.BAD_REQUEST, agSpecCreateResponse.getStatus(),
				message);
	}

	@Disabled("Security is handled externally, thus 401 and 403 cannot happen.")
	@Test
	@Override
	public void createPartyRole401() throws Exception {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException("Unimplemented method 'createPartyRole401'");
	}

	@Disabled("Security is handled externally, thus 401 and 403 cannot happen.")
	@Test
	@Override
	public void createPartyRole403() throws Exception {
	}

	@Disabled("Prohibited by the framework.")
	@Test
	@Override
	public void createPartyRole405() throws Exception {
	}

	@Disabled("No implicit creation, impossible state.")
	@Test
	@Override
	public void createPartyRole409() throws Exception {
	}

	@Override
	public void createPartyRole500() throws Exception {
	}

	@Override
	public void deletePartyRole204() throws Exception {
		PartyRoleCreateVO agSpecCreate = PartyRoleCreateVOTestExample.build().atSchemaLocation(null);
		HttpResponse<PartyRoleVO> createAgSpecResponse = prApiTestClient
				.createPartyRole(null, agSpecCreate);
		assertEquals(HttpStatus.CREATED, createAgSpecResponse.getStatus(),
				"PartyRole should be created");

		String prId = createAgSpecResponse.body().getId();

		assertEquals(HttpStatus.NO_CONTENT,
				callAndCatch(() -> prApiTestClient.deletePartyRole(null, prId)).getStatus(),
				"The partyRole should have been deleted.");

		assertEquals(HttpStatus.NOT_FOUND,
				callAndCatch(() -> prApiTestClient.retrievePartyRole(null, prId, null))
						.status(),
				"The partyRole should not exist anymore.");
	}

	@Disabled("400 is impossible to happen on deletion with the current implementation.")
	@Test
	@Override
	public void deletePartyRole400() throws Exception {
	}

	@Disabled("Security is handled externally, thus 401 and 403 cannot happen.")
	@Test
	@Override
	public void deletePartyRole401() throws Exception {
	}

	@Disabled("Security is handled externally, thus 401 and 403 cannot happen.")
	@Test
	@Override
	public void deletePartyRole403() throws Exception {
	}

	@Test
	@Override
	public void deletePartyRole404() throws Exception {
		String prId = "urn:ngsi-ld:PartyRole:non-existent";

		assertEquals(HttpStatus.NOT_FOUND,
				callAndCatch(() -> prApiTestClient.deletePartyRole(null, prId)).getStatus(),
				"The partyRole should have been deleted.");
	}

	@Disabled("Prohibited by the framework.")
	@Test
	@Override
	public void deletePartyRole405() throws Exception {
	}

	@Disabled("Impossible status.")
	@Test
	@Override
	public void deletePartyRole409() throws Exception {
	}

	@Override
	public void deletePartyRole500() throws Exception {
	}

	@Test
	@Override
	public void listPartyRole200() throws Exception {
		List<PartyRoleVO> expectedPr = new ArrayList<>();
		HttpResponse<PartyRoleVO> createPrResponse;
		PartyRoleCreateVO createAg;
		for (int i = 0; i < 10; i++) {
			createAg = PartyRoleCreateVOTestExample.build().atSchemaLocation(null).engagedParty(null);
			createPrResponse = prApiTestClient.createPartyRole(null, createAg);
			expectedPr.add(createPrResponse.body());
		}
		HttpResponse<List<PartyRoleVO>> listResponse = callAndCatch(
				() -> prApiTestClient.listPartyRole(null, null, null, null));
		assertEquals(HttpStatus.OK, listResponse.getStatus(),
				"PartyRole list should be accessible");
		assertEquals(expectedPr.size(), listResponse.body().size(),
				"The number of party roles should be the same");
		Map<String, PartyRoleVO> expectedmap = expectedPr.stream()
				.collect(Collectors.toMap((r) -> r.getId(), (t) -> t));
		listResponse.body().forEach((obj) -> assertNotNull(expectedmap.get(obj.getId()),
				"Retrieved partyRole list should contain all objects created previously"));

		// get with limit
		Integer limit = 5;
		listResponse = callAndCatch(
				() -> prApiTestClient.listPartyRole(null, null, null, limit));
		assertEquals(HttpStatus.OK, listResponse.getStatus(),
				"PartyRole list should be accessible");
		assertEquals(limit, listResponse.body().size(),
				"The number of party roles should be the same");
	}

	@Test
	@Override
	public void listPartyRole400() throws Exception {
		HttpResponse<List<PartyRoleVO>> listResponse = callAndCatch(() -> prApiTestClient.listPartyRole(null, null, -1, null));
		assertEquals(HttpStatus.BAD_REQUEST, listResponse.getStatus(),
				"PartyRole list should not be accessible");
		listResponse = callAndCatch(
				() -> prApiTestClient.listPartyRole(null, null, null, -1));
		assertEquals(HttpStatus.BAD_REQUEST, listResponse.getStatus(),
				"PartyRole list should not be accessible");
	}

	@Disabled("Security is handled externally, thus 401 and 403 cannot happen.")
	@Test
	@Override
	public void listPartyRole401() throws Exception {
	}

	@Disabled("Security is handled externally, thus 401 and 403 cannot happen.")
	@Test
	@Override
	public void listPartyRole403() throws Exception {
	}

	@Disabled("Not found is not possible here, will be answered with an empty list instead.")
	@Test
	@Override
	public void listPartyRole404() throws Exception {
	}

	@Disabled("Prohibited by the framework.")
	@Test
	@Override
	public void listPartyRole405() throws Exception {
	}

	@Disabled("Impossible status.")
	@Test
	@Override
	public void listPartyRole409() throws Exception {
	}

	@Override
	public void listPartyRole500() throws Exception {
	}

	private static Stream<Arguments> providePrUpdates() {
		List<Arguments> result = new ArrayList<>();
		result.add(Arguments.of("The name should have been updated",
				PartyRoleUpdateVOTestExample.build().atSchemaLocation(null).engagedParty(null).name("Updated"),
				PartyRoleVOTestExample.build().atSchemaLocation(null)
						.name("Updated")
						.engagedParty(null)
						.validFor(null)));
		result.add(Arguments.of("The status should have been updated",
				PartyRoleUpdateVOTestExample.build().atSchemaLocation(null).engagedParty(null).status("canceled"),
				PartyRoleVOTestExample.build().atSchemaLocation(null)
						.engagedParty(null)
						.validFor(null)
						.status("canceled")));

		Instant now1 = Instant.now();
		Instant now2 = Instant.now();
		TimePeriodVO validFor = TimePeriodVOTestExample.build().startDateTime(now1).endDateTime(now2);
		result.add(Arguments.of("The validFor info should have been updated",
				PartyRoleUpdateVOTestExample.build().atSchemaLocation(null).engagedParty(null).validFor(validFor),
				PartyRoleVOTestExample.build().atSchemaLocation(null).validFor(validFor)
						.engagedParty(null)));

		return result.stream();
	}

	@ParameterizedTest
	@MethodSource("providePrUpdates")
	public void patchPartyRole200(String message, PartyRoleUpdateVO updateVO,
								  PartyRoleVO expectedPrVO) throws Exception {
		this.message = message;
		this.prUpdateVO = updateVO;
		this.expectedpr = expectedPrVO;
		patchPartyRole200();

	}

	@Override
	public void patchPartyRole200() throws Exception {
		PartyRoleCreateVO prCreate = PartyRoleCreateVOTestExample.build().atSchemaLocation(null).engagedParty(null);
		HttpResponse<PartyRoleVO> prCreateResponse = callAndCatch(
				() -> prApiTestClient.createPartyRole(null, prCreate));
		String id = prCreateResponse.body().getId();
		assertEquals(HttpStatus.CREATED, prCreateResponse.getStatus(),
				"PartyRole should have been created firstly");
		HttpResponse<PartyRoleVO> updatePrResponse = callAndCatch(
				() -> prApiTestClient.patchPartyRole(null, id, prUpdateVO));
		assertEquals(HttpStatus.OK, updatePrResponse.getStatus(), message);
		PartyRoleVO updatedAgSpec = updatePrResponse.body();
		expectedpr.id(id).href(id);
		assertEquals(expectedpr, updatedAgSpec, message);
	}

	private static Stream<Arguments> provideInvalidUpdates() {
		List<Arguments> result = new ArrayList<>();
		result.add(Arguments.of("An update with an invalid agreement reference is not allowed.",
				PartyRoleUpdateVOTestExample.build().atSchemaLocation(null)
						.agreement(List.of(AgreementRefVOTestExample.build().atSchemaLocation(null).id("invalid")))));
		result.add(Arguments.of("An update with an invalid engaged party is not allowed.",
				PartyRoleUpdateVOTestExample.build().atSchemaLocation(null)
						.agreement(List.of(AgreementRefVOTestExample.build().atSchemaLocation(null).id("invalid")))));
		return result.stream();
	}

	@ParameterizedTest
	@MethodSource("provideInvalidUpdates")
	public void patchAgreement400(String messages, PartyRoleUpdateVO invalidVO) throws Exception {
		this.message = messages;
		this.prUpdateVO = invalidVO;
		patchPartyRole400();
	}

	@Override
	public void patchPartyRole400() throws Exception {
		PartyRoleCreateVO prSpecCreate = PartyRoleCreateVOTestExample.build().atSchemaLocation(null).engagedParty(null);
		HttpResponse<PartyRoleVO> agSpecCreateResponse = callAndCatch(
				() -> prApiTestClient.createPartyRole(null, prSpecCreate));
		String id = agSpecCreateResponse.body().getId();
		assertEquals(HttpStatus.CREATED, agSpecCreateResponse.getStatus(),
				"PartyRole should have been created firstly");
		HttpResponse<PartyRoleVO> updateAgSpecResponse = callAndCatch(
				() -> prApiTestClient.patchPartyRole(null, id, prUpdateVO));
		assertEquals(HttpStatus.BAD_REQUEST, updateAgSpecResponse.getStatus(), message);
	}

	@Disabled("Security is handled externally, thus 401 and 403 cannot happen.")
	@Override
	public void patchPartyRole401() throws Exception {
	}

	@Disabled("Security is handled externally, thus 401 and 403 cannot happen.")
	@Override
	public void patchPartyRole403() throws Exception {
	}

	@Test
	@Override
	public void patchPartyRole404() throws Exception {
		PartyRoleUpdateVO agspec = PartyRoleUpdateVOTestExample.build().atSchemaLocation(null).engagedParty(null);
		assertEquals(HttpStatus.NOT_FOUND,
				callAndCatch(() -> prApiTestClient.patchPartyRole(null, "non-existent",
						agspec)).getStatus(),
				"It should not be able to patch a non-existent party role");
	}

	@Disabled("Prohibited by the framework.")
	@Override
	public void patchPartyRole405() throws Exception {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException("Unimplemented method 'patchPartyRole405'");
	}

	@Override
	public void patchPartyRole409() throws Exception {
	}

	@Override
	public void patchPartyRole500() throws Exception {
	}

	private static Stream<Arguments> provideFieldsRetrieve() {
		List<Arguments> result = new ArrayList<>();
		result.add(Arguments.of("If no fields are established, all attributes should be returned", null,
				PartyRoleVOTestExample.build().atSchemaLocation(null).engagedParty(null).validFor(null)));
		result.add(Arguments.of(
				"It should only show up name,version,description with the default mandatory attributes",
				"name,version,description",
				PartyRoleVOTestExample.build().atSchemaLocation(null).engagedParty(null).account(null).validFor(null)
						.agreement(null).paymentMethod(null).relatedParty(null)
						.status(null).statusReason(null).characteristic(null).contactMedium(null)
						.creditProfile(null).atBaseType(null).atType(null).atSchemaLocation(null)));
		return result.stream();
	}

	@ParameterizedTest
	@MethodSource("provideFieldsRetrieve")
	public void retrievePartyRole200(String message, String fields, PartyRoleVO pr)
			throws Exception {
		this.fields = fields;
		this.message = message;
		this.expectedpr = pr;
		retrievePartyRole200();
	}

	@Override
	public void retrievePartyRole200() throws Exception {
		PartyRoleCreateVO agCreate = PartyRoleCreateVOTestExample.build().atSchemaLocation(null).engagedParty(null);
		HttpResponse<PartyRoleVO> agSpecCreateResponse = callAndCatch(
				() -> prApiTestClient.createPartyRole(null, agCreate));
		String id = agSpecCreateResponse.body().getId();
		assertEquals(HttpStatus.CREATED, agSpecCreateResponse.getStatus(),
				"A party role should have been created firstly");
		expectedpr.id(id).href(id);

		HttpResponse<PartyRoleVO> retrieveResponse = callAndCatch(
				() -> prApiTestClient.retrievePartyRole(null, id, fields));
		assertEquals(HttpStatus.OK, retrieveResponse.getStatus(), message);
		assertEquals(expectedpr, retrieveResponse.body(), message);
	}

	@Disabled("400 cannot happen, only 404")
	@Test
	@Override
	public void retrievePartyRole400() throws Exception {
	}

	@Disabled("Security is handled externally, thus 401 and 403 cannot happen.")
	@Test
	@Override
	public void retrievePartyRole401() throws Exception {
	}

	@Disabled("Security is handled externally, thus 401 and 403 cannot happen.")
	@Test
	@Override
	public void retrievePartyRole403() throws Exception {
	}

	@Test
	@Override
	public void retrievePartyRole404() throws Exception {
		HttpResponse<PartyRoleVO> response = callAndCatch(
				() -> prApiTestClient.retrievePartyRole(null,
						"urn:ngsi-ld:PartyRole:non-existent", null));
		assertEquals(HttpStatus.NOT_FOUND, response.getStatus(),
				"No such partyRole should exist");

		Optional<ErrorDetails> optionalErrorDetails = response.getBody(ErrorDetails.class);

		assertTrue(optionalErrorDetails.isPresent(), "Error details should have been provided.");
	}

	@Disabled("Prohibited by the framework.")
	@Test
	@Override
	public void retrievePartyRole405() throws Exception {
	}

	@Disabled("Conflict not possible on retrieval")
	@Test
	@Override
	public void retrievePartyRole409() throws Exception {
	}

	@Override
	public void retrievePartyRole500() throws Exception {
	}


}