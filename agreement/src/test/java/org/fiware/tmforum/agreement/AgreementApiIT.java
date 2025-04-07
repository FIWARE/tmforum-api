package org.fiware.tmforum.agreement;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import io.micronaut.test.annotation.MockBean;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import org.fiware.agreement.api.AgreementApiTestClient;
import org.fiware.agreement.api.AgreementApiTestSpec;
import org.fiware.agreement.model.*;
import org.fiware.ngsi.api.EntitiesApiClient;
import org.fiware.tmforum.agreement.domain.Agreement;
import org.fiware.tmforum.common.configuration.GeneralProperties;
import org.fiware.tmforum.common.exception.ErrorDetails;
import org.fiware.tmforum.common.notification.TMForumEventHandler;
import org.fiware.tmforum.common.test.AbstractApiIT;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@MicronautTest(packages = {"org.fiware.tmforum.agreement"})
public class AgreementApiIT extends AbstractApiIT implements AgreementApiTestSpec {
	private final AgreementApiTestClient agApiTestClient;
	private String message;
	private String fields;
	private AgreementCreateVO agCreateVO;
	private AgreementUpdateVO agUpdateVO;
	private AgreementVO expectedAg;

	private final EntitiesApiClient entitiesApiClient;
	private final ObjectMapper objectMapper;
	private final GeneralProperties generalProperties;

	public AgreementApiIT(AgreementApiTestClient agSpecApiTestClient,
						  EntitiesApiClient entitiesApiClient, ObjectMapper objectMapper,
						  GeneralProperties generalProperties) {
		super(entitiesApiClient, objectMapper, generalProperties);
		this.agApiTestClient = agSpecApiTestClient;
		this.entitiesApiClient = entitiesApiClient;
		this.objectMapper = objectMapper;
		this.generalProperties = generalProperties;
	}

	@Override
	protected String getEntityType() {
		return Agreement.TYPE_AGREEMENT;
	}

	@MockBean(TMForumEventHandler.class)
	public TMForumEventHandler eventHandler() {
		TMForumEventHandler eventHandler = mock(TMForumEventHandler.class);

		when(eventHandler.handleCreateEvent(any())).thenReturn(Mono.empty());
		when(eventHandler.handleUpdateEvent(any(), any())).thenReturn(Mono.empty());

		return eventHandler;
	}

	@ParameterizedTest
	@MethodSource({"provideValidAgSpec"})
	public void createAgreement201(String message, AgreementCreateVO agSpecCreateVO,
								   AgreementVO expectedAgSpec) throws Exception {
		this.message = message;
		this.agCreateVO = agSpecCreateVO;
		this.expectedAg = expectedAgSpec;
		createAgreement201();
	}

	private static Stream<Arguments> provideValidAgSpec() {
		List<Arguments> testEntries = new ArrayList<>();
		AgreementCreateVO agCreateVO = AgreementCreateVOTestExample.build().atSchemaLocation(null).agreementSpecification(null);
		AgreementVO expectedAgSpec = AgreementVOTestExample.build().atSchemaLocation(null).agreementSpecification(null);
		testEntries.add(
				Arguments.of("Empty Agreement should have been created", agCreateVO,
						expectedAgSpec));
		TimePeriodVO timePeriodVO = TimePeriodVOTestExample.build().endDateTime(Instant.now())
				.startDateTime(Instant.now());
		agCreateVO = AgreementCreateVOTestExample.build().atSchemaLocation(null).agreementSpecification(null)
				.agreementPeriod(timePeriodVO);
		expectedAgSpec = AgreementVOTestExample.build().atSchemaLocation(null).agreementSpecification(null)
				.agreementPeriod(timePeriodVO);
		testEntries.add(
				Arguments.of("Agreement with a agreementPeriod should have been created",
						agCreateVO,
						expectedAgSpec));

		return testEntries.stream();
	}

	@Override
	public void createAgreement201() throws Exception {
		HttpResponse<AgreementVO> agCreateResponse = callAndCatch(
				() -> agApiTestClient.createAgreement(null, agCreateVO));
		assertEquals(HttpStatus.CREATED, agCreateResponse.getStatus(), message);
		String id = agCreateResponse.body().getId();
		expectedAg.id(id).href(id);
		assertEquals(expectedAg, agCreateResponse.body(), message);
	}

	@ParameterizedTest
	@MethodSource({"provideInvalidAg"})
	public void createCustomer400(String message, AgreementCreateVO agCreateVO) throws Exception {
		this.message = message;
		this.agCreateVO = agCreateVO;
		createAgreement400();
	}

	private static Stream<Arguments> provideInvalidAg() {
		List<Arguments> testEntries = new ArrayList<>();
		AgreementSpecificationRefVO agSpecRef = AgreementSpecificationRefVOTestExample.build().atSchemaLocation(null)
				.id("non-existent");
		AgreementCreateVO agCreateVO = AgreementCreateVOTestExample.build().atSchemaLocation(null).agreementSpecification(agSpecRef);
		testEntries.add(
				Arguments.of("An Agreement with an invalid AgreementSpecificationRef should not be created",
						agCreateVO));

		agCreateVO = AgreementCreateVOTestExample.build().atSchemaLocation(null)
				.engagedParty(List
						.of(RelatedPartyVOTestExample.build().atSchemaLocation(null).id(
								"urn:ngsi-ld:Agreement:non-existent")));
		testEntries.add(
				Arguments.of("Agreement with an invalid engagedParty should not be created",
						agCreateVO));

		return testEntries.stream();
	}

	@Override
	public void createAgreement400() throws Exception {
		HttpResponse<AgreementVO> agSpecCreateResponse = callAndCatch(
				() -> agApiTestClient.createAgreement(null, agCreateVO));
		assertEquals(HttpStatus.BAD_REQUEST, agSpecCreateResponse.getStatus(),
				message);
	}

	@Disabled("Security is handled externally, thus 401 and 403 cannot happen.")
	@Test
	@Override
	public void createAgreement401() throws Exception {

	}

	@Disabled("Security is handled externally, thus 401 and 403 cannot happen.")
	@Test
	@Override
	public void createAgreement403() throws Exception {

	}

	@Disabled("Prohibited by the framework.")
	@Test
	@Override
	public void createAgreement405() throws Exception {

	}

	@Disabled("No implicit creation, impossible state.")
	@Test
	@Override
	public void createAgreement409() throws Exception {

	}

	@Override
	public void createAgreement500() throws Exception {

	}

	@Override
	public void deleteAgreement204() throws Exception {
		AgreementCreateVO agSpecCreate = AgreementCreateVOTestExample.build().atSchemaLocation(null);
		HttpResponse<AgreementVO> createAgSpecResponse = agApiTestClient
				.createAgreement(null, agSpecCreate);
		assertEquals(HttpStatus.CREATED, createAgSpecResponse.getStatus(),
				"Agreement should be created");

		String agId = createAgSpecResponse.body().getId();

		assertEquals(HttpStatus.NO_CONTENT,
				callAndCatch(() -> agApiTestClient.deleteAgreement(null, agId)).getStatus(),
				"The agreement should have been deleted.");

		assertEquals(HttpStatus.NOT_FOUND,
				callAndCatch(() -> agApiTestClient.retrieveAgreement(null, agId, null))
						.status(),
				"The agreement should not exist anymore.");
	}

	@Disabled("400 is impossible to happen on deletion with the current implementation.")
	@Test
	@Override
	public void deleteAgreement400() throws Exception {

	}

	@Disabled("Security is handled externally, thus 401 and 403 cannot happen.")
	@Test
	@Override
	public void deleteAgreement401() throws Exception {

	}

	@Disabled("Security is handled externally, thus 401 and 403 cannot happen.")
	@Test
	@Override
	public void deleteAgreement403() throws Exception {

	}

	@Test
	@Override
	public void deleteAgreement404() throws Exception {
		String agId = "urn:ngsi-ld:Agreement:non-existent";

		assertEquals(HttpStatus.NOT_FOUND,
				callAndCatch(() -> agApiTestClient.deleteAgreement(null, agId)).getStatus(),
				"The customer should have been deleted.");
	}

	@Disabled("Prohibited by the framework.")
	@Test
	@Override
	public void deleteAgreement405() throws Exception {

	}

	@Disabled("Impossible status.")
	@Test
	@Override
	public void deleteAgreement409() throws Exception {

	}

	@Override
	public void deleteAgreement500() throws Exception {

	}

	@Test
	@Override
	public void listAgreement200() throws Exception {
		List<AgreementVO> expectedAg = new ArrayList<>();
		HttpResponse<AgreementVO> createAgResponse;
		AgreementCreateVO createAg;
		for (int i = 0; i < 10; i++) {
			createAg = AgreementCreateVOTestExample.build().atSchemaLocation(null).agreementSpecification(null);
			createAgResponse = agApiTestClient.createAgreement(null, createAg);
			expectedAg.add(createAgResponse.body());
		}
		HttpResponse<List<AgreementVO>> listResponse = callAndCatch(
				() -> agApiTestClient.listAgreement(null, null, null, null));
		assertEquals(HttpStatus.OK, listResponse.getStatus(),
				"Agreement specification list should be accessible");
		assertEquals(expectedAg.size(), listResponse.body().size(),
				"The number of agreement specifications should be the same");
		Map<String, AgreementVO> expectedmap = expectedAg.stream()
				.collect(Collectors.toMap((r) -> r.getId(), (t) -> t));
		listResponse.body().forEach((obj) -> assertNotNull(expectedmap.get(obj.getId()),
				"Retrieved agreement specification list should contain all objects created previously"));

		// get with limit
		Integer limit = 5;
		listResponse = callAndCatch(
				() -> agApiTestClient.listAgreement(null, null, null, limit));
		assertEquals(HttpStatus.OK, listResponse.getStatus(),
				"Agreement list should be accessible");
		assertEquals(limit, listResponse.body().size(),
				"The number of agreements should be the same");
	}

	@Test
	@Override
	public void listAgreement400() throws Exception {
		HttpResponse<List<AgreementVO>> listResponse = callAndCatch(
				() -> agApiTestClient.listAgreement(null, null, -1, null));
		assertEquals(HttpStatus.BAD_REQUEST, listResponse.getStatus(),
				"Agreement list should be accessible");
		listResponse = callAndCatch(
				() -> agApiTestClient.listAgreement(null, null, null, -1));
		assertEquals(HttpStatus.BAD_REQUEST, listResponse.getStatus(),
				"Agreement list should be accessible");
	}

	@Disabled("Security is handled externally, thus 401 and 403 cannot happen.")
	@Test
	@Override
	public void listAgreement401() throws Exception {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException("Unimplemented method 'listAgreement401'");
	}

	@Disabled("Security is handled externally, thus 401 and 403 cannot happen.")
	@Test
	@Override
	public void listAgreement403() throws Exception {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException("Unimplemented method 'listAgreement403'");
	}

	@Disabled("Not found is not possible here, will be answered with an empty list instead.")
	@Test
	@Override
	public void listAgreement404() throws Exception {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException("Unimplemented method 'listAgreement404'");
	}

	@Disabled("Prohibited by the framework.")
	@Test
	@Override
	public void listAgreement405() throws Exception {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException("Unimplemented method 'listAgreement405'");
	}

	@Disabled("Impossible status.")
	@Test
	@Override
	public void listAgreement409() throws Exception {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException("Unimplemented method 'listAgreement409'");
	}

	@Override
	public void listAgreement500() throws Exception {
	}

	@ParameterizedTest
	@MethodSource("provideAgUpdates")
	public void patchAgreement200(String message, AgreementUpdateVO updateVO,
								  AgreementVO agSpecVO) throws Exception {
		this.message = message;
		this.agUpdateVO = updateVO;
		this.expectedAg = agSpecVO;
		patchAgreement200();

	}

	private static Stream<Arguments> provideAgUpdates() {
		List<Arguments> result = new ArrayList<>();
		result.add(Arguments.of("The name should have been updated",
				AgreementUpdateVOTestExample.build().atSchemaLocation(null).agreementSpecification(null).name("Updated"),
				AgreementVOTestExample.build().atSchemaLocation(null).agreementSpecification(null)
						.name("Updated")));
		result.add(Arguments.of("The version should have been updated",
				AgreementUpdateVOTestExample.build().atSchemaLocation(null).agreementSpecification(null).version("2.2"),
				AgreementVOTestExample.build().atSchemaLocation(null).agreementSpecification(null)
						.version("2.2")));
		Instant now = Instant.now();
		result.add(Arguments.of("The initial date info should have been updated",
				AgreementUpdateVOTestExample.build().atSchemaLocation(null).agreementSpecification(null).initialDate(now),
				AgreementVOTestExample.build().atSchemaLocation(null).agreementSpecification(null)
						.initialDate(now)));

		return result.stream();
	}

	@Override
	public void patchAgreement200() throws Exception {
		// Agreement specification creation
		AgreementCreateVO agCreate = AgreementCreateVOTestExample.build().atSchemaLocation(null)
				.agreementSpecification(null);
		HttpResponse<AgreementVO> agCreateResponse = callAndCatch(
				() -> agApiTestClient.createAgreement(null, agCreate));
		String id = agCreateResponse.body().getId();
		assertEquals(HttpStatus.CREATED, agCreateResponse.getStatus(),
				"An agreeement should have been created firstly");
		HttpResponse<AgreementVO> updateAgResponse = callAndCatch(
				() -> agApiTestClient.patchAgreement(null, id, agUpdateVO));
		assertEquals(HttpStatus.OK, updateAgResponse.getStatus(), message);
		AgreementVO updatedAgSpec = updateAgResponse.body();
		expectedAg.id(id).href(id);
		assertEquals(expectedAg, updatedAgSpec, message);
	}

	@ParameterizedTest
	@MethodSource("provideInvalidUpdates")
	public void patchAgreement400(String messages, AgreementUpdateVO invalidVO) throws Exception {
		this.message = message;
		this.agUpdateVO = invalidVO;
		patchAgreement400();
	}

	private static Stream<Arguments> provideInvalidUpdates() {
		List<Arguments> result = new ArrayList<>();
		result.add(Arguments.of("An update with an invalid agreement specification is not allowed.",
				AgreementUpdateVOTestExample.build().atSchemaLocation(null)
						.agreementSpecification(AgreementSpecificationRefVOTestExample.build().atSchemaLocation(null)
								.id("invalid"))));
		result.add(Arguments.of("An update with an invalid engaged party is not allowed.",
				AgreementUpdateVOTestExample.build().atSchemaLocation(null)
						.agreementSpecification(null).engagedParty(List
								.of(RelatedPartyVOTestExample.build().atSchemaLocation(null).id("invalid")))));
		return result.stream();
	}

	@Override
	public void patchAgreement400() throws Exception {
		// Agreement specification creation
		AgreementCreateVO agSpecCreate = AgreementCreateVOTestExample.build().atSchemaLocation(null)
				.agreementSpecification(null);
		HttpResponse<AgreementVO> agSpecCreateResponse = callAndCatch(
				() -> agApiTestClient.createAgreement(null, agSpecCreate));
		String id = agSpecCreateResponse.body().getId();
		assertEquals(HttpStatus.CREATED, agSpecCreateResponse.getStatus(),
				"An agreeement should have been created firstly");
		HttpResponse<AgreementVO> updateAgSpecResponse = callAndCatch(
				() -> agApiTestClient.patchAgreement(null, id, agUpdateVO));
		assertEquals(HttpStatus.BAD_REQUEST, updateAgSpecResponse.getStatus(), message);
	}

	@Disabled("Security is handled externally, thus 401 and 403 cannot happen.")
	@Test
	@Override
	public void patchAgreement401() throws Exception {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException("Unimplemented method 'patchAgreement401'");
	}

	@Disabled("Security is handled externally, thus 401 and 403 cannot happen.")
	@Test
	@Override
	public void patchAgreement403() throws Exception {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException("Unimplemented method 'patchAgreement403'");
	}

	@Test
	@Override
	public void patchAgreement404() throws Exception {
		AgreementUpdateVO agspec = AgreementUpdateVOTestExample.build().atSchemaLocation(null).agreementSpecification(null);
		assertEquals(HttpStatus.NOT_FOUND,
				callAndCatch(() -> agApiTestClient.patchAgreement(null, "non-existent",
						agspec)).getStatus(),
				"It should not be able to find a non-existent agreement specification");
	}

	@Disabled("Prohibited by the framework.")
	@Test
	@Override
	public void patchAgreement405() throws Exception {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException("Unimplemented method 'patchAgreement405'");
	}

	@Override
	public void patchAgreement409() throws Exception {

	}

	@Override
	public void patchAgreement500() throws Exception {

	}

	@ParameterizedTest
	@MethodSource("provideFieldsRetrieve")
	public void retrieveAgreement200(String message, String fields, AgreementVO ag)
			throws Exception {
		this.fields = fields;
		this.message = message;
		this.expectedAg = ag;
		retrieveAgreement200();
	}

	private static Stream<Arguments> provideFieldsRetrieve() {
		List<Arguments> result = new ArrayList<>();
		result.add(Arguments.of("If no fields are established, all attributes should be returned", null,
				AgreementVOTestExample.build()
						.atSchemaLocation(null)
						.agreementSpecification(null)));
		result.add(Arguments.of(
				"It should only name,version,description with the mandatory attributes",
				"name,version,description",
				AgreementVOTestExample.build().atSchemaLocation(null).agreementSpecification(null)
						.atType(null)
						.atSchemaLocation(null)
						.atBaseType(null)
						.completionDate(null)
						.characteristic(null)
						.associatedAgreement(null)
						.agreementType(null)
						.documentNumber(null).status(null)
						.agreementAuthorization(null).statementOfIntent(null)
						.agreementPeriod(null)));
		return result.stream();
	}

	@Override
	public void retrieveAgreement200() throws Exception {
		// Agreement specification creation
		AgreementCreateVO agCreate = AgreementCreateVOTestExample.build().atSchemaLocation(null)
				.agreementSpecification(null);
		HttpResponse<AgreementVO> agSpecCreateResponse = callAndCatch(
				() -> agApiTestClient.createAgreement(null, agCreate));
		String id = agSpecCreateResponse.body().getId();
		assertEquals(HttpStatus.CREATED, agSpecCreateResponse.getStatus(),
				"An agreeement specification should have been created firstly");
		expectedAg.id(id).href(id);

		HttpResponse<AgreementVO> retrieveResponse = callAndCatch(
				() -> agApiTestClient.retrieveAgreement(null, id, fields));
		assertEquals(HttpStatus.OK, retrieveResponse.getStatus(), message);
		assertEquals(expectedAg, retrieveResponse.body(), message);
	}

	@Disabled("400 cannot happen, only 404")
	@Test
	@Override
	public void retrieveAgreement400() throws Exception {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException("Unimplemented method 'retrieveAgreement400'");
	}

	@Disabled("Security is handled externally, thus 401 and 403 cannot happen.")
	@Test
	@Override
	public void retrieveAgreement401() throws Exception {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException("Unimplemented method 'retrieveAgreement401'");
	}

	@Disabled("Security is handled externally, thus 401 and 403 cannot happen.")
	@Test
	@Override
	public void retrieveAgreement403() throws Exception {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException("Unimplemented method 'retrieveAgreement403'");
	}

	@Override
	public void retrieveAgreement404() throws Exception {
		HttpResponse<AgreementVO> response = callAndCatch(
				() -> agApiTestClient.retrieveAgreement(null,
						"urn:ngsi-ld:Agreement:non-existent", null));
		assertEquals(HttpStatus.NOT_FOUND, response.getStatus(),
				"No such agreement should exist");

		Optional<ErrorDetails> optionalErrorDetails = response.getBody(ErrorDetails.class);

		assertTrue(optionalErrorDetails.isPresent(), "Error details should have been provided.");
	}

	@Disabled("Prohibited by the framework.")
	@Test
	@Override
	public void retrieveAgreement405() throws Exception {
	}

	@Disabled("Conflict not possible on retrieval")
	@Test
	@Override
	public void retrieveAgreement409() throws Exception {
	}

	@Override
	public void retrieveAgreement500() throws Exception {

	}

}
