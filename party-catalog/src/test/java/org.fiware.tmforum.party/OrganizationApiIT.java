package org.fiware.tmforum.party;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import io.micronaut.test.annotation.MockBean;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import org.fiware.ngsi.api.EntitiesApiClient;
import org.fiware.party.api.OrganizationApiTestClient;
import org.fiware.party.api.OrganizationApiTestSpec;
import org.fiware.party.model.*;
import org.fiware.tmforum.common.configuration.GeneralProperties;
import org.fiware.tmforum.common.exception.ErrorDetails;
import org.fiware.tmforum.common.notification.TMForumEventHandler;
import org.fiware.tmforum.common.test.AbstractApiIT;
import org.fiware.tmforum.party.domain.organization.Organization;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@MicronautTest(packages = {"org.fiware.tmforum.party"})
class OrganizationApiIT extends AbstractApiIT implements OrganizationApiTestSpec {

	private final OrganizationApiTestClient organizationApiTestClient;

	private OrganizationCreateVO organizationCreateVO;
	private OrganizationUpdateVO organizationUpdateVO;
	private OrganizationVO expectedOrganization;
	private String message;

	private final EntitiesApiClient entitiesApiClient;
	private final ObjectMapper objectMapper;
	private final GeneralProperties generalProperties;

	OrganizationApiIT(OrganizationApiTestClient organizationApiTestClient, EntitiesApiClient entitiesApiClient,
					  ObjectMapper objectMapper, GeneralProperties generalProperties) {
		super(entitiesApiClient, objectMapper, generalProperties);
		this.organizationApiTestClient = organizationApiTestClient;
		this.entitiesApiClient = entitiesApiClient;
		this.objectMapper = objectMapper;
		this.generalProperties = generalProperties;
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
		return Organization.TYPE_ORGANIZATION;
	}

	@ParameterizedTest
	@MethodSource("provideValidOrganizations")
	public void createOrganization201(String message, OrganizationCreateVO organizationCreateVO,
									  OrganizationVO expectedOrganization) throws Exception {
		this.organizationCreateVO = organizationCreateVO;
		this.expectedOrganization = expectedOrganization;
		this.message = message;
		createOrganization201();
	}

	@Override
	public void createOrganization201() throws Exception {

		HttpResponse<OrganizationVO> organizationCreateResponse = callAndCatch(
				() -> organizationApiTestClient.createOrganization(null, organizationCreateVO));
		assertEquals(HttpStatus.CREATED, organizationCreateResponse.getStatus(), message);

		OrganizationVO createdOrganizationVO = organizationCreateResponse.body();
		expectedOrganization.setId(createdOrganizationVO.getId());
		expectedOrganization.setHref(createdOrganizationVO.getId());
		assertEquals(expectedOrganization, createdOrganizationVO, message);

	}

	private static Stream<Arguments> provideValidOrganizations() {
		List<Arguments> validOrgs = new ArrayList<>();

		OrganizationCreateVO organizationCreateVO = OrganizationCreateVOTestExample.build().atSchemaLocation(null);
		organizationCreateVO.setOrganizationParentRelationship(null);
		OrganizationVO expectedOrganization = OrganizationVOTestExample.build().atSchemaLocation(null);
		expectedOrganization.setOrganizationParentRelationship(null);
		validOrgs.add(Arguments.of("Empty org should have been created.", organizationCreateVO, expectedOrganization));

		OrganizationCreateVO orgWithContactMedium = OrganizationCreateVOTestExample.build().atSchemaLocation(null);
		orgWithContactMedium.setOrganizationParentRelationship(null);
		orgWithContactMedium.setContactMedium(List.of(ContactMediumVOTestExample.build().characteristic(null).atSchemaLocation(null)));
		OrganizationVO expectedOrgWithContactMedium = OrganizationVOTestExample.build()
				.atSchemaLocation(null);
		expectedOrgWithContactMedium.organizationParentRelationship(null)
				.contactMedium(List.of(ContactMediumVOTestExample.build()
						.atSchemaLocation(null)
						.characteristic(null)));
		validOrgs.add(Arguments.of("Org with contact medium should have been created.", orgWithContactMedium,
				expectedOrgWithContactMedium));

		OrganizationCreateVO orgWithCreditRating = OrganizationCreateVOTestExample.build().atSchemaLocation(null);
		orgWithCreditRating.setOrganizationParentRelationship(null);
		orgWithCreditRating.setCreditRating(List.of(PartyCreditProfileVOTestExample.build().atSchemaLocation(null)));
		OrganizationVO expectedOrgWithCreditRating = OrganizationVOTestExample.build().atSchemaLocation(null);
		expectedOrgWithCreditRating.setOrganizationParentRelationship(null);
		expectedOrgWithCreditRating.setCreditRating(List.of(PartyCreditProfileVOTestExample.build().atSchemaLocation(null)));
		validOrgs.add(Arguments.of("Org with credit rating should have been created.", orgWithCreditRating,
				expectedOrgWithCreditRating));

		OrganizationCreateVO orgWithExistsDuring = OrganizationCreateVOTestExample.build().atSchemaLocation(null);
		orgWithExistsDuring.setOrganizationParentRelationship(null);
		orgWithExistsDuring.setExistsDuring(TimePeriodVOTestExample.build());
		OrganizationVO expectedOrgWithExistsDuring = OrganizationVOTestExample.build().atSchemaLocation(null);
		expectedOrgWithExistsDuring.setOrganizationParentRelationship(null);
		expectedOrgWithExistsDuring.setExistsDuring(TimePeriodVOTestExample.build());
		validOrgs.add(Arguments.of("Org with exists during should have been created.", orgWithExistsDuring,
				expectedOrgWithExistsDuring));

		OrganizationCreateVO orgWithOtherName = OrganizationCreateVOTestExample.build().atSchemaLocation(null);
		orgWithOtherName.setOrganizationParentRelationship(null);
		orgWithOtherName.setOtherName(List.of(OtherNameOrganizationVOTestExample.build().atSchemaLocation(null)));
		OrganizationVO expectedOrgWithOtherName = OrganizationVOTestExample.build().atSchemaLocation(null);
		expectedOrgWithOtherName.setOrganizationParentRelationship(null);
		expectedOrgWithOtherName.setOtherName(List.of(OtherNameOrganizationVOTestExample.build().atSchemaLocation(null)));
		validOrgs.add(Arguments.of("Org with other name should have been created.", orgWithOtherName,
				expectedOrgWithOtherName));

		OrganizationCreateVO orgWithPartyChar = OrganizationCreateVOTestExample.build().atSchemaLocation(null);
		orgWithPartyChar.setOrganizationParentRelationship(null);
		orgWithPartyChar.setPartyCharacteristic(List.of(CharacteristicVOTestExample.build().atSchemaLocation(null)));
		OrganizationVO expectedOrgWithPartyChar = OrganizationVOTestExample.build().atSchemaLocation(null);
		expectedOrgWithPartyChar.setOrganizationParentRelationship(null);
		expectedOrgWithPartyChar.setPartyCharacteristic(List.of(CharacteristicVOTestExample.build().atSchemaLocation(null)));
		validOrgs.add(Arguments.of("Org with party characteristics should have been created.", orgWithPartyChar,
				expectedOrgWithPartyChar));

		OrganizationCreateVO orgWithTaxExemption = OrganizationCreateVOTestExample.build().atSchemaLocation(null);
		orgWithTaxExemption.setOrganizationParentRelationship(null);
		TaxDefinitionVO taxDefinitionVO = TaxDefinitionVOTestExample.build().atSchemaLocation(null);
		taxDefinitionVO.setId("urn:" + UUID.randomUUID());
		TaxExemptionCertificateVO taxExemptionCertificateVO = TaxExemptionCertificateVOTestExample.build().atSchemaLocation(null);
		// prevent duplicates
		taxExemptionCertificateVO.setId("urn:" + UUID.randomUUID());
		taxExemptionCertificateVO.setTaxDefinition(List.of(taxDefinitionVO));
		// fix the example
		AttachmentRefOrValueVO attachmentRefOrValueVO = AttachmentRefOrValueVOTestExample.build().atSchemaLocation(null);
		attachmentRefOrValueVO.setHref("http://my-ref.de");
		attachmentRefOrValueVO.setUrl("http://my-url.de");
		attachmentRefOrValueVO.setId("urn:attachment");
		taxExemptionCertificateVO.setAttachment(attachmentRefOrValueVO);
		orgWithTaxExemption.setTaxExemptionCertificate(List.of(taxExemptionCertificateVO));
		OrganizationVO expectedOrgWithTaxExemption = OrganizationVOTestExample.build().atSchemaLocation(null);
		expectedOrgWithTaxExemption.setOrganizationParentRelationship(null);
		expectedOrgWithTaxExemption.setTaxExemptionCertificate(List.of(taxExemptionCertificateVO));
		validOrgs.add(Arguments.of("Org with tax exemption certificate should have been created.", orgWithTaxExemption,
				expectedOrgWithTaxExemption));

		return validOrgs.stream();
	}

	@Test
	@Override
	public void createOrganization400() throws Exception {
		for (OrganizationCreateVO ocVO : provideInvalidOrganizationCreate()) {
			HttpResponse<OrganizationVO> organizationCreateResponse = callAndCatch(
					() -> organizationApiTestClient.createOrganization(null, ocVO));
			assertEquals(HttpStatus.BAD_REQUEST, organizationCreateResponse.getStatus(),
					"Organization should not have been created.");

			Optional<ErrorDetails> optionalErrorDetails = organizationCreateResponse.getBody(ErrorDetails.class);
			assertTrue(optionalErrorDetails.isPresent(), "Error details should be provided.");
		}
	}

	public static List<OrganizationCreateVO> provideInvalidOrganizationCreate() {
		OrganizationCreateVO nonExistentParentCreateVO = OrganizationCreateVOTestExample.build().atSchemaLocation(null);
		nonExistentParentCreateVO.getOrganizationParentRelationship().getOrganization()
				.setId("urn:ngsi-ld:organization:valid-but-not-existent");

		OrganizationCreateVO invalidRelatedPartyOrg = OrganizationCreateVOTestExample.build().atSchemaLocation(null);
		RelatedPartyVO invalidRelatedPartyRef = RelatedPartyVOTestExample.build().atSchemaLocation(null);
		invalidRelatedPartyOrg.setRelatedParty(List.of(invalidRelatedPartyRef));

		OrganizationCreateVO nonExistentRelatedPartyOrg = OrganizationCreateVOTestExample.build().atSchemaLocation(null);
		RelatedPartyVO nonExistentRelatedPartyRef = RelatedPartyVOTestExample.build().atSchemaLocation(null);
		nonExistentRelatedPartyRef.setId("urn:ngsi-ld:organization:non-existent");
		nonExistentRelatedPartyOrg.setRelatedParty(List.of(nonExistentRelatedPartyRef));

		return List.of(
				// invalid parent org
				OrganizationCreateVOTestExample.build().atSchemaLocation(null),
				nonExistentParentCreateVO,
				invalidRelatedPartyOrg,
				nonExistentRelatedPartyOrg
		);
	}

	@Disabled("Security is handled externally, thus 401 and 403 cannot happen.")
	@Test
	@Override
	public void createOrganization401() throws Exception {
	}

	@Disabled("Security is handled externally, thus 401 and 403 cannot happen.")
	@Test
	@Override
	public void createOrganization403() throws Exception {
	}

	@Disabled("Prohibited by the framework.")
	@Test
	@Override
	public void createOrganization405() throws Exception {

	}

	@Disabled("No implicit creation.")
	@Test
	@Override
	public void createOrganization409() throws Exception {
	}

	@Override
	public void createOrganization500() throws Exception {

	}

	@Test
	@Override
	public void deleteOrganization204() throws Exception {
		// first create one
		OrganizationCreateVO organizationCreateVO = OrganizationCreateVOTestExample.build().atSchemaLocation(null);
		organizationCreateVO.setOrganizationParentRelationship(null);
		HttpResponse<OrganizationVO> organizationCreateResponse = callAndCatch(
				() -> organizationApiTestClient.createOrganization(null, organizationCreateVO));
		assertEquals(HttpStatus.CREATED, organizationCreateResponse.getStatus(),
				"The organization should have been created first.");

		String orgId = organizationCreateResponse.body().getId();

		assertEquals(HttpStatus.NO_CONTENT,
				callAndCatch(() -> organizationApiTestClient.deleteOrganization(null, orgId)).getStatus(),
				"The organization should have been deleted.");

		assertEquals(HttpStatus.NOT_FOUND,
				callAndCatch(() -> organizationApiTestClient.retrieveOrganization(null, orgId, null)).status(),
				"The organization should not exist anymore.");

	}

	@Disabled("400 is impossible to happen on deletion with the current implementation.")
	@Test
	@Override
	public void deleteOrganization400() throws Exception {
	}

	@Disabled("Security is handled externally, thus 401 and 403 cannot happen.")
	@Test
	@Override
	public void deleteOrganization401() throws Exception {

	}

	@Disabled("Security is handled externally, thus 401 and 403 cannot happen.")
	@Test
	@Override
	public void deleteOrganization403() throws Exception {

	}

	@Test
	@Override
	public void deleteOrganization404() throws Exception {
		String ngsiLdOrgId = "urn:ngsi-ld:organization:valid";
		String nonNgsiLdOrgId = "non-ngsi";

		HttpResponse<?> notFoundResponse = callAndCatch(
				() -> organizationApiTestClient.deleteOrganization(null, ngsiLdOrgId));
		assertEquals(HttpStatus.NOT_FOUND,
				notFoundResponse.getStatus(),
				"No such organization should exist.");

		Optional<ErrorDetails> optionalErrorDetails = notFoundResponse.getBody(ErrorDetails.class);
		assertTrue(optionalErrorDetails.isPresent(), "Error details should be provided.");

		notFoundResponse = callAndCatch(() -> organizationApiTestClient.deleteOrganization(null, nonNgsiLdOrgId));
		assertEquals(HttpStatus.NOT_FOUND,
				notFoundResponse.getStatus(),
				"No such organization should exist.");
		optionalErrorDetails = notFoundResponse.getBody(ErrorDetails.class);
		assertTrue(optionalErrorDetails.isPresent(), "Error details should be provided.");
	}

	@Disabled("Prohibited by the framework.")
	@Test
	@Override
	public void deleteOrganization405() throws Exception {

	}

	@Disabled("Impossible status.")
	@Test
	@Override
	public void deleteOrganization409() throws Exception {

	}

	@Override
	public void deleteOrganization500() throws Exception {

	}

	@Test
	@Override
	public void listOrganization200() throws Exception {
		List<OrganizationVO> expectedOrganizations = new ArrayList<>();
		for (int i = 0; i < 10; i++) {
			OrganizationCreateVO organizationCreateVO = OrganizationCreateVOTestExample.build().atSchemaLocation(null)
					.organizationParentRelationship(null);
			String id = organizationApiTestClient.createOrganization(null, organizationCreateVO).body().getId();
			OrganizationVO organizationVO = OrganizationVOTestExample.build().atSchemaLocation(null);
			organizationVO
					.id(id)
					.href(id)
					.organizationParentRelationship(null)
					.relatedParty(null);
			expectedOrganizations.add(organizationVO);
		}

		HttpResponse<List<OrganizationVO>> organizationResponse = callAndCatch(
				() -> organizationApiTestClient.listOrganization(null, null, null, null));

		assertEquals(HttpStatus.OK, organizationResponse.getStatus(), "The list should be accessible.");
		assertEquals(expectedOrganizations.size(), organizationResponse.getBody().get().size(),
				"All organizations should have been returned.");
		List<OrganizationVO> retrievedOrganizations = organizationResponse.getBody().get();

		Map<String, OrganizationVO> retrievedMap = retrievedOrganizations.stream()
				.collect(Collectors.toMap(organization -> organization.getId(), organization -> organization));

		expectedOrganizations.stream()
				.forEach(expectedOrganization -> assertTrue(retrievedMap.containsKey(expectedOrganization.getId()),
						String.format("All created organizations should be returned - Missing: %s.",
								expectedOrganization,
								retrievedOrganizations)));
		expectedOrganizations.stream().forEach(
				expectedOrganization -> assertEquals(expectedOrganization,
						retrievedMap.get(expectedOrganization.getId()),
						"The correct organizations should be retrieved."));

		// get with pagination
		Integer limit = 5;
		HttpResponse<List<OrganizationVO>> firstPartResponse = callAndCatch(
				() -> organizationApiTestClient.listOrganization(null, null, 0, limit));
		assertEquals(limit, firstPartResponse.body().size(),
				"Only the requested number of entries should be returend.");
		HttpResponse<List<OrganizationVO>> secondPartResponse = callAndCatch(
				() -> organizationApiTestClient.listOrganization(null, null, 0 + limit, limit));
		assertEquals(limit, secondPartResponse.body().size(),
				"Only the requested number of entries should be returend.");

		retrievedOrganizations.clear();
		retrievedOrganizations.addAll(firstPartResponse.body());
		retrievedOrganizations.addAll(secondPartResponse.body());
		expectedOrganizations.stream()
				.forEach(expectedOrganization -> assertTrue(retrievedMap.containsKey(expectedOrganization.getId()),
						String.format("All created organizations should be returned - Missing: %s.",
								expectedOrganization)));
		expectedOrganizations.stream().forEach(
				expectedOrganization -> assertEquals(expectedOrganization,
						retrievedMap.get(expectedOrganization.getId()),
						"The correct organizations should be retrieved."));
	}

	@Test
	@Override
	public void listOrganization400() throws Exception {
		HttpResponse<List<OrganizationVO>> badRequestResponse = callAndCatch(
				() -> organizationApiTestClient.listOrganization(null, null, -1, null));
		assertEquals(HttpStatus.BAD_REQUEST,
				badRequestResponse.getStatus(),
				"Negative offsets are impossible.");

		Optional<ErrorDetails> optionalErrorDetails = badRequestResponse.getBody(ErrorDetails.class);
		assertTrue(optionalErrorDetails.isPresent(), "Error details should be provided.");

		badRequestResponse = callAndCatch(() -> organizationApiTestClient.listOrganization(null, null, null, -1));
		assertEquals(HttpStatus.BAD_REQUEST,
				badRequestResponse.getStatus(),
				"Negative limits are impossible.");
		optionalErrorDetails = badRequestResponse.getBody(ErrorDetails.class);
		assertTrue(optionalErrorDetails.isPresent(), "Error details should be provided.");

	}

	@Disabled("Security is handled externally, thus 401 and 403 cannot happen.")
	@Test
	@Override
	public void listOrganization401() throws Exception {

	}

	@Disabled("Security is handled externally, thus 401 and 403 cannot happen.")
	@Test
	@Override
	public void listOrganization403() throws Exception {

	}

	@Disabled("Not found is not possible here, will be answerd with an empty list instead.")
	@Test
	@Override
	public void listOrganization404() throws Exception {

	}

	@Disabled("Prohibited by the framework.")
	@Test
	@Override
	public void listOrganization405() throws Exception {

	}

	@Disabled("Impossible status.")
	@Test
	@Override
	public void listOrganization409() throws Exception {

	}

	@Override
	public void listOrganization500() throws Exception {

	}

	@ParameterizedTest
	@MethodSource("provideValidUpdates")
	public void patchOrganization200(String message, OrganizationUpdateVO organizationUpdateVO,
									 OrganizationVO expectedOrganization) throws Exception {
		this.message = message;
		this.organizationUpdateVO = organizationUpdateVO;
		this.expectedOrganization = expectedOrganization;
		patchOrganization200();
	}

	@Test
	public void patchOrgWithTaxEx() throws Exception {
		String teId = "urn:" + UUID.randomUUID();
		String tdId = "urn:" + UUID.randomUUID();

		TimePeriodVO timePeriodVO = TimePeriodVOTestExample.build();
		timePeriodVO.setStartDateTime(Instant.now());
		timePeriodVO.setEndDateTime(Instant.now());
		OrganizationCreateVO orgWithTaxExemption = OrganizationCreateVOTestExample.build().atSchemaLocation(null);
		orgWithTaxExemption.setOrganizationParentRelationship(null);
		TaxDefinitionVO taxDefinitionVO = TaxDefinitionVOTestExample.build().atSchemaLocation(null);
		taxDefinitionVO.setId(tdId);
		TaxExemptionCertificateVO taxExemptionCertificateVO = TaxExemptionCertificateVOTestExample.build().atSchemaLocation(null);
		// prevent duplicates
		taxExemptionCertificateVO.setId(teId);
		taxExemptionCertificateVO.setValidFor(timePeriodVO);
		taxExemptionCertificateVO.setTaxDefinition(List.of(taxDefinitionVO));
		// fix the example
		AttachmentRefOrValueVO attachmentRefOrValueVO = AttachmentRefOrValueVOTestExample.build().atSchemaLocation(null);
		attachmentRefOrValueVO.setHref("http://my-ref.de");
		attachmentRefOrValueVO.setUrl("http://my-url.de");
		attachmentRefOrValueVO.setId("urn:attachment");
		attachmentRefOrValueVO.setValidFor(timePeriodVO);
		taxExemptionCertificateVO.setAttachment(attachmentRefOrValueVO);
		orgWithTaxExemption.setTaxExemptionCertificate(List.of(taxExemptionCertificateVO));

		HttpResponse<OrganizationVO> createResponse = callAndCatch(
				() -> organizationApiTestClient.createOrganization(null, orgWithTaxExemption));
		assertEquals(HttpStatus.CREATED, createResponse.getStatus(), "The initial organization was created.");

		String id = createResponse.body().getId();

		OrganizationUpdateVO updateVO = OrganizationUpdateVOTestExample.build().atSchemaLocation(null);
		attachmentRefOrValueVO.setUrl("http://my-updated-url.de");
		updateVO.setTaxExemptionCertificate(List.of(taxExemptionCertificateVO));
		updateVO.setOrganizationParentRelationship(null);

		OrganizationVO expectedOrg = OrganizationVOTestExample.build().atSchemaLocation(null);
		expectedOrg.setHref(id);
		expectedOrg.setId(id);
		expectedOrg.setTaxExemptionCertificate(List.of(taxExemptionCertificateVO));
		expectedOrg.setRelatedParty(null);
		expectedOrg.setOrganizationParentRelationship(null);

		HttpResponse<OrganizationVO> updateResponse = callAndCatch(
				() -> organizationApiTestClient.patchOrganization(null, id, updateVO));
		assertEquals(HttpStatus.OK, updateResponse.getStatus(), "The organization should have been updated");
		assertEquals(expectedOrg, updateResponse.body(), "The changes should have been set.");

	}

	@Override
	public void patchOrganization200() throws Exception {

		OrganizationCreateVO organizationCreateVO = OrganizationCreateVOTestExample.build().atSchemaLocation(null);
		organizationCreateVO.setOrganizationParentRelationship(null);

		HttpResponse<OrganizationVO> organizationCreateResponse = callAndCatch(
				() -> organizationApiTestClient.createOrganization(null, organizationCreateVO));
		assertEquals(HttpStatus.CREATED, organizationCreateResponse.getStatus(), message);

		String organizationId = organizationCreateResponse.body().getId();

		HttpResponse<OrganizationVO> organizationUpdateResponse = callAndCatch(
				() -> organizationApiTestClient.patchOrganization(null, organizationId, organizationUpdateVO));
		assertEquals(HttpStatus.OK, organizationUpdateResponse.getStatus(), message);

		expectedOrganization.setHref(organizationId);
		expectedOrganization.setId(organizationId);
		expectedOrganization.setOrganizationParentRelationship(null);

		if (expectedOrganization.getRelatedParty() != null && expectedOrganization.getRelatedParty().isEmpty()) {
			expectedOrganization.setRelatedParty(null);
		}


		assertEquals(expectedOrganization, organizationUpdateResponse.body(), message);

		HttpResponse<OrganizationVO> organizationGetResponse = callAndCatch(
				() -> organizationApiTestClient.retrieveOrganization(null, organizationId, null));
		assertEquals(expectedOrganization, organizationGetResponse.body(), message);

	}

	private static Stream<Arguments> provideValidUpdates() {
		List<Arguments> validOrgs = new ArrayList<>();

		OrganizationUpdateVO organizationUpdateVO = OrganizationUpdateVOTestExample.build().atSchemaLocation(null);
		organizationUpdateVO.setOrganizationParentRelationship(null);
		organizationUpdateVO.setName("MyNewName");
		OrganizationVO expectedOrganization = OrganizationVOTestExample.build().atSchemaLocation(null);
		expectedOrganization.setOrganizationParentRelationship(null);
		expectedOrganization.setName("MyNewName");
		validOrgs.add(Arguments.of("Empty org should have been updated.", organizationUpdateVO, expectedOrganization));

		OrganizationUpdateVO orgWithContactMedium = OrganizationUpdateVOTestExample.build().atSchemaLocation(null);
		orgWithContactMedium.setOrganizationParentRelationship(null);
		ContactMediumVO contactMediumVO = ContactMediumVOTestExample.build().characteristic(null).atSchemaLocation(null);
		TimePeriodVO timePeriodVO = TimePeriodVOTestExample.build();
		timePeriodVO.setStartDateTime(Instant.now());
		timePeriodVO.setStartDateTime(Instant.now());
		contactMediumVO.setMediumType("My email");
		contactMediumVO.setValidFor(timePeriodVO);
		orgWithContactMedium.setContactMedium(List.of(contactMediumVO));
		OrganizationVO expectedOrgWithContactMedium = OrganizationVOTestExample.build().atSchemaLocation(null);
		expectedOrgWithContactMedium.setOrganizationParentRelationship(null);
		expectedOrgWithContactMedium.setContactMedium(List.of(contactMediumVO));
		validOrgs.add(Arguments.of("Org with contact medium should have been updated.", orgWithContactMedium,
				expectedOrgWithContactMedium));

		OrganizationUpdateVO orgWithCreditRating = OrganizationUpdateVOTestExample.build().atSchemaLocation(null);
		orgWithCreditRating.setOrganizationParentRelationship(null);
		PartyCreditProfileVO partyCreditProfileVO = PartyCreditProfileVOTestExample.build().atSchemaLocation(null);
		partyCreditProfileVO.setCreditAgencyName("Credit agency");
		partyCreditProfileVO.setValidFor(timePeriodVO);
		orgWithCreditRating.setCreditRating(List.of(partyCreditProfileVO));
		OrganizationVO expectedOrgWithCreditRating = OrganizationVOTestExample.build().atSchemaLocation(null);
		expectedOrgWithCreditRating.setOrganizationParentRelationship(null);
		expectedOrgWithCreditRating.setCreditRating(List.of(partyCreditProfileVO));
		validOrgs.add(Arguments.of("Org with credit rating should have been updated.", orgWithCreditRating,
				expectedOrgWithCreditRating));

		OrganizationUpdateVO orgWithExistsDuring = OrganizationUpdateVOTestExample.build().atSchemaLocation(null);
		orgWithExistsDuring.setOrganizationParentRelationship(null);
		orgWithExistsDuring.setExistsDuring(timePeriodVO);
		OrganizationVO expectedOrgWithExistsDuring = OrganizationVOTestExample.build().atSchemaLocation(null);
		expectedOrgWithExistsDuring.setOrganizationParentRelationship(null);
		expectedOrgWithExistsDuring.setExistsDuring(timePeriodVO);
		validOrgs.add(Arguments.of("Org with exists during should have been updated.", orgWithExistsDuring,
				expectedOrgWithExistsDuring));

		OrganizationUpdateVO orgWithOtherName = OrganizationUpdateVOTestExample.build().atSchemaLocation(null);
		orgWithOtherName.setOrganizationParentRelationship(null);
		OtherNameOrganizationVO otherNameOrganizationVO = OtherNameOrganizationVOTestExample.build().atSchemaLocation(null);
		otherNameOrganizationVO.setName("New name");
		otherNameOrganizationVO.setValidFor(timePeriodVO);
		orgWithOtherName.setOtherName(List.of(otherNameOrganizationVO));
		OrganizationVO expectedOrgWithOtherName = OrganizationVOTestExample.build().atSchemaLocation(null);
		expectedOrgWithOtherName.setOrganizationParentRelationship(null);
		expectedOrgWithOtherName.setOtherName(List.of(otherNameOrganizationVO));
		validOrgs.add(Arguments.of("Org with other name should have been updated.", orgWithOtherName,
				expectedOrgWithOtherName));

		OrganizationUpdateVO orgWithPartyChar = OrganizationUpdateVOTestExample.build().atSchemaLocation(null);
		orgWithPartyChar.setOrganizationParentRelationship(null);
		CharacteristicVO characteristicVO = CharacteristicVOTestExample.build().atSchemaLocation(null);
		characteristicVO.setName("New Char");
		orgWithPartyChar.setPartyCharacteristic(List.of(characteristicVO));
		OrganizationVO expectedOrgWithPartyChar = OrganizationVOTestExample.build().atSchemaLocation(null);
		expectedOrgWithPartyChar.setOrganizationParentRelationship(null);
		expectedOrgWithPartyChar.setPartyCharacteristic(List.of(characteristicVO));
		validOrgs.add(Arguments.of("Org with party characteristics should have been updated.", orgWithPartyChar,
				expectedOrgWithPartyChar));

		OrganizationUpdateVO orgWithTaxExemption = OrganizationUpdateVOTestExample.build().atSchemaLocation(null);
		orgWithTaxExemption.setOrganizationParentRelationship(null);

		TaxDefinitionVO taxDefinitionVO = TaxDefinitionVOTestExample.build().atSchemaLocation(null);
		// prevent duplicates
		taxDefinitionVO.setId("urn:" + UUID.randomUUID());
		TaxExemptionCertificateVO taxExemptionCertificateVO = TaxExemptionCertificateVOTestExample.build().atSchemaLocation(null);
		taxExemptionCertificateVO.setTaxDefinition(List.of(taxDefinitionVO));
		taxExemptionCertificateVO.setValidFor(timePeriodVO);
		// prevent duplicates
		taxExemptionCertificateVO.setId("urn:" + UUID.randomUUID());

		// fix the example
		AttachmentRefOrValueVO attachmentRefOrValueVO = AttachmentRefOrValueVOTestExample.build().atSchemaLocation(null);
		attachmentRefOrValueVO.setHref("http://my-ref.de");
		attachmentRefOrValueVO.setUrl("http://my-url.de");
		attachmentRefOrValueVO.setId("urn:attachment");
		attachmentRefOrValueVO.setValidFor(timePeriodVO);
		taxExemptionCertificateVO.setAttachment(attachmentRefOrValueVO);
		orgWithTaxExemption.setTaxExemptionCertificate(List.of(taxExemptionCertificateVO));
		OrganizationVO expectedOrgWithTaxExemption = OrganizationVOTestExample.build().atSchemaLocation(null);
		expectedOrgWithTaxExemption.setOrganizationParentRelationship(null);
		expectedOrgWithTaxExemption.setTaxExemptionCertificate(List.of(taxExemptionCertificateVO));
		validOrgs.add(Arguments.of("Org with tax exemption certificate should have been updated.", orgWithTaxExemption,
				expectedOrgWithTaxExemption));

		return validOrgs.stream();
	}

	@Disabled("Not implemented yet")
	@Test
	@Override
	public void patchOrganization400() throws Exception {

		OrganizationCreateVO organizationCreateVO = OrganizationCreateVOTestExample.build().atSchemaLocation(null);
		organizationCreateVO.setOrganizationParentRelationship(null);
		HttpResponse<OrganizationVO> organizationCreateResponse = callAndCatch(
				() -> organizationApiTestClient.createOrganization(null, organizationCreateVO));
		assertEquals(HttpStatus.CREATED, organizationCreateResponse.getStatus(),
				"The organization should have been initially created. ");

		String organizationId = organizationCreateResponse.body().getId();

		for (OrganizationUpdateVO ouVO : provideInvalidOrganizationUpdate()) {
			HttpResponse<OrganizationVO> organizationUpdateResponse = callAndCatch(
					() -> organizationApiTestClient.patchOrganization(null, organizationId, ouVO));
			assertEquals(HttpStatus.BAD_REQUEST, organizationUpdateResponse.getStatus(),
					"Organization should not have been created.");

			Optional<ErrorDetails> optionalErrorDetails = organizationUpdateResponse.getBody(ErrorDetails.class);
			assertTrue(optionalErrorDetails.isPresent(), "Error details should be provided.");
		}
	}

	public static List<OrganizationUpdateVO> provideInvalidOrganizationUpdate() {
		OrganizationUpdateVO nonExistentParentUpdateVO = OrganizationUpdateVOTestExample.build().atSchemaLocation(null);
		nonExistentParentUpdateVO.getOrganizationParentRelationship().getOrganization()
				.setId("urn:ngsi-ld:organization:valid-but-not-existent");

		OrganizationUpdateVO invalidRelatedPartyOrg = OrganizationUpdateVOTestExample.build().atSchemaLocation(null);
		RelatedPartyVO invalidRelatedPartyRef = RelatedPartyVOTestExample.build().atSchemaLocation(null);
		invalidRelatedPartyOrg.setRelatedParty(List.of(invalidRelatedPartyRef));

		OrganizationUpdateVO nonExistentRelatedPartyOrg = OrganizationUpdateVOTestExample.build().atSchemaLocation(null);
		RelatedPartyVO nonExistentRelatedPartyRef = RelatedPartyVOTestExample.build().atSchemaLocation(null);
		nonExistentRelatedPartyRef.setId("urn:ngsi-ld:organization:non-existent");
		nonExistentRelatedPartyOrg.setRelatedParty(List.of(nonExistentRelatedPartyRef));

		return List.of(
				// invalid parent org
				OrganizationUpdateVOTestExample.build().atSchemaLocation(null),
				nonExistentParentUpdateVO,
				invalidRelatedPartyOrg,
				nonExistentRelatedPartyOrg
		);
	}

	@Disabled("Security is handled externally, thus 401 and 403 cannot happen.")
	@Test
	@Override
	public void patchOrganization401() throws Exception {

	}

	@Disabled("Security is handled externally, thus 401 and 403 cannot happen.")
	@Test
	@Override
	public void patchOrganization403() throws Exception {

	}

	@Disabled("Not implemented yet")
	@Test
	@Override
	public void patchOrganization404() throws Exception {
		OrganizationUpdateVO organizationUpdateVO = OrganizationUpdateVOTestExample.build().atSchemaLocation(null);
		organizationUpdateVO.setOrganizationParentRelationship(null);
		assertEquals(
				HttpStatus.NOT_FOUND,
				callAndCatch(() -> organizationApiTestClient.patchOrganization(null, "urn:ngsi-ld:organization:not-existent",
						organizationUpdateVO)).getStatus(),
				"Non existent organizations should not be updated.");
	}

	@Disabled("Prohibited by the framework.")
	@Test
	@Override
	public void patchOrganization405() throws Exception {

	}

	@Disabled("TODO: Decide if that can happen.")
	@Test
	@Override
	public void patchOrganization409() throws Exception {

	}

	@Override
	public void patchOrganization500() throws Exception {

	}

	@Test
	@Override
	public void retrieveOrganization200() throws Exception {
		OrganizationCreateVO organizationCreateVO = OrganizationCreateVOTestExample.build().atSchemaLocation(null);
		organizationCreateVO.setOrganizationParentRelationship(null);
		HttpResponse<OrganizationVO> createdOrg = callAndCatch(
				() -> organizationApiTestClient.createOrganization(null, organizationCreateVO));
		assertEquals(HttpStatus.CREATED, createdOrg.getStatus(), "Create the org to retrieve.");

		String organizationId = createdOrg.body().getId();

		OrganizationVO expectedOrg = OrganizationVOTestExample.build().atSchemaLocation(null);
		expectedOrg.setOrganizationParentRelationship(null);
		expectedOrg.setId(organizationId);
		expectedOrg.setHref(organizationId);
		expectedOrg.setRelatedParty(null);

		HttpResponse<OrganizationVO> retrievedOrganization = callAndCatch(
				() -> organizationApiTestClient.retrieveOrganization(null, organizationId, null));
		assertEquals(HttpStatus.OK, retrievedOrganization.getStatus(), "The retrieval should be ok.");
		assertEquals(expectedOrg, retrievedOrganization.body(), "The correct org should be returned.");
	}

	@Disabled("400 cannot happen, only 404")
	@Test
	@Override
	public void retrieveOrganization400() throws Exception {

	}

	@Disabled("Security is handled externally, thus 401 and 403 cannot happen.")
	@Test
	@Override
	public void retrieveOrganization401() throws Exception {

	}

	@Disabled("Security is handled externally, thus 401 and 403 cannot happen.")
	@Test
	@Override
	public void retrieveOrganization403() throws Exception {

	}

	@Test
	@Override
	public void retrieveOrganization404() throws Exception {
		HttpResponse<OrganizationVO> notFoundResponse = callAndCatch(
				() -> organizationApiTestClient.retrieveOrganization(null, "urn:ngsi-ld:organization:not-found", null));

		assertEquals(HttpStatus.NOT_FOUND, notFoundResponse.getStatus(), "No such org exists.");

		Optional<ErrorDetails> optionalErrorDetails = notFoundResponse.getBody(ErrorDetails.class);
		assertTrue(optionalErrorDetails.isPresent(), "Error details should have been provided.");
	}

	@Disabled("Prohibited by the framework.")
	@Test
	@Override
	public void retrieveOrganization405() throws Exception {

	}

	@Disabled("Conflict not possible on retrieval")
	@Test
	@Override
	public void retrieveOrganization409() throws Exception {

	}

	@Override
	public void retrieveOrganization500() throws Exception {

	}

}
