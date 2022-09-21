package org.fiware.tmforum.party;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import lombok.RequiredArgsConstructor;
import org.fiware.party.model.CharacteristicVO;
import org.fiware.party.model.ContactMediumVO;
import org.fiware.party.model.DisabilityVO;
import org.fiware.party.model.ExternalReferenceVO;
import org.fiware.party.model.IndividualCreateVO;
import org.fiware.party.model.IndividualIdentificationVO;
import org.fiware.party.model.IndividualUpdateVO;
import org.fiware.party.model.IndividualVO;
import org.fiware.party.model.LanguageAbilityVO;
import org.fiware.party.model.MediumCharacteristicVO;
import org.fiware.party.model.OrganizationCreateVO;
import org.fiware.party.model.OrganizationIdentificationVO;
import org.fiware.party.model.OrganizationStateTypeVO;
import org.fiware.party.model.OrganizationVO;
import org.fiware.party.model.OtherNameIndividualVO;
import org.fiware.party.model.OtherNameOrganizationVO;
import org.fiware.party.model.PartyCreditProfileVO;
import org.fiware.party.model.RelatedPartyVO;
import org.fiware.party.model.SkillVO;
import org.fiware.party.model.TaxDefinitionVO;
import org.fiware.party.model.TaxExemptionCertificateVO;
import org.fiware.party.model.TimePeriodVO;
import org.fiware.tmforum.common.mapping.NGSIMapperImpl;
import org.fiware.tmforum.party.rest.IndividualApiController;
import org.fiware.tmforum.party.rest.OrganizationApiController;
import org.junit.jupiter.api.Test;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@RequiredArgsConstructor
@MicronautTest(packages = {"org.fiware.tmforum.party"})
class PartyApiIT {

	private final ObjectMapper objectMapper;
	private final OrganizationApiController organizationApiController;
	private final IndividualApiController individualApiController;


	@Test
	void simpleFullUserUpdate() throws JsonProcessingException, ParseException {
		OrganizationCreateVO myFancyCompanyCreate = getMyFancyCompany();

		HttpResponse<OrganizationVO> myFancyCompanyCreateResponse = organizationApiController.createOrganization(myFancyCompanyCreate).blockingGet();
		assertEquals(HttpStatus.CREATED, myFancyCompanyCreateResponse.getStatus(), "Company should have been created.");
		OrganizationVO myFancyCompany = myFancyCompanyCreateResponse.body();

		IndividualCreateVO earlMustermannCreate = getIndividualEmployee(myFancyCompany.getId());
		HttpResponse<IndividualVO> earlMustermannCreateResponse = individualApiController.createIndividual(earlMustermannCreate).blockingGet();
		assertEquals(HttpStatus.CREATED, earlMustermannCreateResponse.getStatus(), "Individual should have been created.");
		IndividualVO earlMustermann = earlMustermannCreateResponse.body();

		HttpResponse<OrganizationVO> organizationVOHttpResponse = organizationApiController.retrieveOrganization(myFancyCompany.getId(), null).blockingGet();

		assertEquals(HttpStatus.OK, organizationVOHttpResponse.getStatus(), "An organization response is expected.");
		assertTrue(organizationVOHttpResponse.getBody().isPresent(), "An organization response is expected.");
		// this is a valid assertion, since "myFancyCompany" is constructed form the request-body(e.g. myFancyCompanyCreate) and persisted to the broker
		// while the response body is retrieved from the broker and constructed from the entity retrieved.
		// Both have to be equal, thus it actually tests something.
		assertEquals(myFancyCompany, organizationVOHttpResponse.getBody().get(), "The full organization should be retrieved");

		HttpResponse<IndividualVO> individualVOHttpResponse = individualApiController.retrieveIndividual(earlMustermann.getId(), null).blockingGet();

		assertEquals(HttpStatus.OK, individualVOHttpResponse.getStatus(), "An individual response is expected.");
		assertTrue(individualVOHttpResponse.getBody().isPresent(), "An individual response is expected.");
		assertEquals(earlMustermann, individualVOHttpResponse.getBody().get(), "The full individual should be retrieved");

		IndividualUpdateVO individualUpdateVO = getIndividualEmployeeUpdate("Musterfrau");
		HttpResponse<IndividualVO> individualUpdateVOHttpResponse = individualApiController.patchIndividual(earlMustermann.getId(), individualUpdateVO).blockingGet();

		assertEquals(HttpStatus.OK, individualUpdateVOHttpResponse.getStatus(), "An individual response is expected.");
		assertTrue(individualVOHttpResponse.getBody().isPresent(), "An individual response is expected.");
		assertEquals(earlMustermann.setFamilyName("Musterfrau"), individualUpdateVOHttpResponse.getBody().get(), "The updated individual should be retrieved");

		HttpResponse<List<IndividualVO>> indvidualListResponse = individualApiController.listIndividual(null, null, null).blockingGet();
		assertEquals(HttpStatus.OK, indvidualListResponse.getStatus(), "An individual list response is expected.");
		assertFalse(indvidualListResponse.body().isEmpty(), "Some indivuals should exist.");

		HttpResponse<List<OrganizationVO>> organizationListResponse = organizationApiController.listOrganization(null, null, null).blockingGet();
		assertEquals(HttpStatus.OK, organizationListResponse.getStatus(), "An organization list response is expected.");
		assertFalse(organizationListResponse.body().isEmpty(), "Some organizations should exist.");
	}

	private OrganizationCreateVO getMyFancyCompany() throws JsonProcessingException {
		// create the organization
		OrganizationCreateVO organizationVO = new OrganizationCreateVO();
		organizationVO.setIsHeadOffice(true);
		organizationVO.setIsLegalEntity(true);
		organizationVO.setName("My Fancy Company");
		organizationVO.setNameType("Inc");
		organizationVO.setOrganizationType("Company");
		organizationVO.setTradingName("My Fancy Company");

		MediumCharacteristicVO mediumCharacteristicVO = new MediumCharacteristicVO();
		mediumCharacteristicVO.setCity("Dresden");
		mediumCharacteristicVO.setContactType("postal address");
		mediumCharacteristicVO.setCountry("Germany");
		mediumCharacteristicVO.setEmailAddress("my-fancy@company.org");
		mediumCharacteristicVO.setPhoneNumber("0123/4567890-0");
		mediumCharacteristicVO.setFaxNumber("0123/4567890-1");
		mediumCharacteristicVO.setPostCode("01189");
		mediumCharacteristicVO.setSocialNetworkId("@fancy");
		mediumCharacteristicVO.setStateOrProvince("Saxony");
		mediumCharacteristicVO.street1("Prager Straße 1");

		PartyCreditProfileVO partyCreditProfileVO = new PartyCreditProfileVO();
		partyCreditProfileVO.setCreditAgencyName("Experian");
		partyCreditProfileVO.setCreditAgencyType("Rating-Agency");
		partyCreditProfileVO.setRatingReference("Rating ref");
		partyCreditProfileVO.setRatingScore(100);
		partyCreditProfileVO.setValidFor(new TimePeriodVO().startDateTime(Instant.now()).endDateTime(Instant.now().plus(Duration.of(10, ChronoUnit.DAYS))));

		PartyCreditProfileVO partyCreditProfileVO2 = new PartyCreditProfileVO();
		partyCreditProfileVO2.setCreditAgencyName("TransUnion");
		partyCreditProfileVO2.setCreditAgencyType("Rating-Agency");
		partyCreditProfileVO2.setRatingReference("Rating ref");
		partyCreditProfileVO2.setRatingScore(100);
		partyCreditProfileVO2.setValidFor(new TimePeriodVO().startDateTime(Instant.now()).endDateTime(Instant.now().plus(Duration.of(10, ChronoUnit.DAYS))));

		ContactMediumVO contactMediumVO = new ContactMediumVO();
		contactMediumVO.setMediumType("postal address");
		contactMediumVO.setPreferred(true);
		contactMediumVO.setCharacteristic(mediumCharacteristicVO);
		contactMediumVO.setValidFor(new TimePeriodVO().startDateTime(Instant.now()).endDateTime(Instant.now().plus(Duration.of(10, ChronoUnit.DAYS))));

		OrganizationIdentificationVO organizationIdentificationVO = new OrganizationIdentificationVO();
		organizationIdentificationVO.setIdentificationId("My-Fancy-Company-ID");
		organizationIdentificationVO.setValidFor(new TimePeriodVO().startDateTime(Instant.now()).endDateTime(Instant.now().plus(Duration.of(20, ChronoUnit.DAYS))));
		organizationIdentificationVO.setIdentificationType("Country-ID");
		organizationIdentificationVO.setIssuingAuthority("Gewerbeamt Dresden");
		organizationIdentificationVO.setIssuingDate(Instant.now().minus(Duration.of(1, ChronoUnit.DAYS)));

		OtherNameOrganizationVO otherOrganizationName = new OtherNameOrganizationVO();
		otherOrganizationName.setName("My-Other-Company");
		otherOrganizationName.setNameType("Ldt.");
		otherOrganizationName.setTradingName("My-Other-Company");
		otherOrganizationName.setValidFor(new TimePeriodVO().startDateTime(Instant.now()).endDateTime(Instant.now().plus(Duration.of(20, ChronoUnit.DAYS))));

		CharacteristicVO characteristicVO = new CharacteristicVO();
		characteristicVO.setName("My-Company-Valuation");
		characteristicVO.setValueType("valuation");
		characteristicVO.setValue(objectMapper.writeValueAsString(new TestCharacteristic(1000000l, "Euro")));

		TaxDefinitionVO taxDefinitionVO = new TaxDefinitionVO();
		taxDefinitionVO.setName("Gewerbe-Steuer");
		taxDefinitionVO.setTaxType("Gewerbe-Steuer");
		TaxDefinitionVO taxDefinitionVO2 = new TaxDefinitionVO();
		taxDefinitionVO2.setName("Gewerbe-Steuer2");
		taxDefinitionVO2.setTaxType("Gewerbe-Steuer2");

		TaxExemptionCertificateVO taxExemptionCertificateVO = new TaxExemptionCertificateVO();
		taxExemptionCertificateVO.setTaxDefinition(List.of(taxDefinitionVO, taxDefinitionVO2));
		taxExemptionCertificateVO.setValidFor(new TimePeriodVO().startDateTime(Instant.now()).endDateTime(Instant.now().plus(Duration.of(20, ChronoUnit.DAYS))));

		organizationVO.setContactMedium(List.of(contactMediumVO));
		organizationVO.setCreditRating(List.of(partyCreditProfileVO, partyCreditProfileVO2));
		organizationVO.setExistsDuring(new TimePeriodVO().startDateTime(Instant.now().minus(Duration.of(100, ChronoUnit.DAYS))).endDateTime(Instant.now().plus(Duration.of(100, ChronoUnit.DAYS))));
		organizationVO.setExternalReference(List.of(new ExternalReferenceVO().name("Ext-Ref").externalReferenceType("Ref")));
		organizationVO.setOrganizationIdentification(List.of(organizationIdentificationVO));
		organizationVO.setOtherName(List.of(otherOrganizationName));
		organizationVO.setPartyCharacteristic(List.of(characteristicVO));
		organizationVO.setStatus(OrganizationStateTypeVO.VALIDATED);
		organizationVO.setTaxExemptionCertificate(List.of(taxExemptionCertificateVO));

		return organizationVO;
	}

	private IndividualUpdateVO getIndividualEmployeeUpdate(String newFamilyName) {
		IndividualUpdateVO individualUpdateVO = new IndividualUpdateVO();
		individualUpdateVO.setFamilyName(newFamilyName);
		return individualUpdateVO;
	}

	private IndividualCreateVO getIndividualEmployee(String orgId) throws ParseException {
		IndividualCreateVO individualCreateVO = new IndividualCreateVO();
		individualCreateVO.setAristocraticTitle("Earl");
		individualCreateVO.setBirthDate(new SimpleDateFormat("yyyy-MM-dd").parse("1970-01-01").toInstant());
		individualCreateVO.setCountryOfBirth("Germany");
		individualCreateVO.setFamilyName("Mustermann");
		individualCreateVO.setFormattedName("Max Markus Mustermann, Earl of Saxony");
		individualCreateVO.setFullName("Max Markus Mustermann");
		individualCreateVO.setGender("male");
		individualCreateVO.setGeneration("III.");
		individualCreateVO.setGivenName("Max");
		individualCreateVO.setLegalName("Mustermann");
		individualCreateVO.setLocation("Schloßstraße 1, Dresden");
		individualCreateVO.setMaritalStatus("married");
		individualCreateVO.setMiddleName("Markus");
		individualCreateVO.setNationality("German");
		individualCreateVO.setPlaceOfBirth("Dresden");
		individualCreateVO.setPreferredGivenName("The earl");
		individualCreateVO.setTitle("Dr.");

		MediumCharacteristicVO mediumCharacteristicVO = new MediumCharacteristicVO();
		mediumCharacteristicVO.setCity("Dresden");
		mediumCharacteristicVO.setContactType("postal address");
		mediumCharacteristicVO.setCountry("Germany");
		mediumCharacteristicVO.setEmailAddress("the-earl@company.org");
		mediumCharacteristicVO.setPhoneNumber("0123/4567890-2");
		mediumCharacteristicVO.setFaxNumber("0123/4567890-3");
		mediumCharacteristicVO.setPostCode("01189");
		mediumCharacteristicVO.setSocialNetworkId("@earl");
		mediumCharacteristicVO.setStateOrProvince("Saxony");
		mediumCharacteristicVO.street1("Schlossstraße 1");


		ContactMediumVO contactMediumVO = new ContactMediumVO();
		contactMediumVO.setMediumType("postal address");
		contactMediumVO.setPreferred(true);
		contactMediumVO.setCharacteristic(mediumCharacteristicVO);
		contactMediumVO.setValidFor(new TimePeriodVO().startDateTime(Instant.now()).endDateTime(Instant.now().plus(Duration.of(10, ChronoUnit.DAYS))));

		ContactMediumVO contactMediumVO2 = new ContactMediumVO();
		contactMediumVO2.setMediumType("email");
		contactMediumVO2.setPreferred(false);
		contactMediumVO2.setCharacteristic(mediumCharacteristicVO);
		contactMediumVO2.setValidFor(new TimePeriodVO().startDateTime(Instant.now()).endDateTime(Instant.now().plus(Duration.of(10, ChronoUnit.DAYS))));

		individualCreateVO.setContactMedium(List.of(contactMediumVO, contactMediumVO2));

		PartyCreditProfileVO partyCreditProfileVO = new PartyCreditProfileVO();
		partyCreditProfileVO.setCreditAgencyName("Experian");
		partyCreditProfileVO.setCreditAgencyType("Rating-Agency");
		partyCreditProfileVO.setRatingReference("Rating ref");
		partyCreditProfileVO.setRatingScore(100);
		partyCreditProfileVO.setValidFor(new TimePeriodVO().startDateTime(Instant.now()).endDateTime(Instant.now().plus(Duration.of(10, ChronoUnit.DAYS))));

		individualCreateVO.setCreditRating(List.of(partyCreditProfileVO));

		DisabilityVO disabilityVO = new DisabilityVO();
		disabilityVO.setValidFor(new TimePeriodVO().startDateTime(Instant.now()).endDateTime(Instant.now().plus(Duration.of(10, ChronoUnit.DAYS))));
		disabilityVO.setDisabilityCode("02");
		disabilityVO.setDisabilityName("Hearing");

		DisabilityVO disabilityVO2 = new DisabilityVO();
		disabilityVO2.setValidFor(new TimePeriodVO().startDateTime(Instant.now()).endDateTime(Instant.now().plus(Duration.of(10, ChronoUnit.DAYS))));
		disabilityVO2.setDisabilityCode("03");
		disabilityVO2.setDisabilityName("Manual Dexterity");

		DisabilityVO disabilityVO3 = new DisabilityVO();
		disabilityVO3.setValidFor(new TimePeriodVO().startDateTime(Instant.now()).endDateTime(Instant.now().plus(Duration.of(10, ChronoUnit.DAYS))));
		disabilityVO3.setDisabilityCode("09");
		disabilityVO3.setDisabilityName("Sight");

		individualCreateVO.setDisability(List.of(disabilityVO, disabilityVO2, disabilityVO3));

		ExternalReferenceVO externalReferenceVO = new ExternalReferenceVO();
		externalReferenceVO.setExternalReferenceType("ext");
		externalReferenceVO.setName("My-Ref");

		individualCreateVO.setExternalReference(List.of(externalReferenceVO));

		IndividualIdentificationVO individualIdentificationVO = new IndividualIdentificationVO();
		individualIdentificationVO.setIdentificationId("T22000129");
		individualIdentificationVO.setIdentificationType("Passport");
		individualIdentificationVO.setIssuingAuthority("Einwohnermeldeamt Dresden");
		individualIdentificationVO.setIssuingDate(Instant.now());
		individualIdentificationVO.setValidFor(new TimePeriodVO().startDateTime(Instant.now()).endDateTime(Instant.now().plus(Duration.of(10, ChronoUnit.DAYS))));

		individualCreateVO.setIndividualIdentification(List.of(individualIdentificationVO));

		LanguageAbilityVO languageAbilityVO = new LanguageAbilityVO();
		languageAbilityVO.isFavouriteLanguage(true);
		languageAbilityVO.setLanguageCode("DE");
		languageAbilityVO.setLanguageName("German");
		languageAbilityVO.setListeningProficiency("C2");
		languageAbilityVO.setReadingProficiency("C2");
		languageAbilityVO.setSpeakingProficiency("C2");
		languageAbilityVO.setWritingProficiency("C2");
		languageAbilityVO.setValidFor(new TimePeriodVO().startDateTime(Instant.now()).endDateTime(Instant.now().plus(Duration.of(100, ChronoUnit.DAYS))));

		individualCreateVO.setLanguageAbility(List.of(languageAbilityVO));

		OtherNameIndividualVO otherNameIndividualVO = new OtherNameIndividualVO();
		otherNameIndividualVO.setFamilyName("Mannmuster");
		otherNameIndividualVO.setValidFor(new TimePeriodVO().startDateTime(Instant.now()).endDateTime(Instant.now().plus(Duration.of(10, ChronoUnit.DAYS))));
		otherNameIndividualVO.setFormattedName("Max Markus Mustermann, Earl of Saxony");
		otherNameIndividualVO.setFullName("Max Markus Mustermann");
		otherNameIndividualVO.setGeneration("III.");
		otherNameIndividualVO.setGivenName("Max");
		otherNameIndividualVO.setLegalName("Mustermann");
		otherNameIndividualVO.setMiddleName("Markus");
		otherNameIndividualVO.setPreferredGivenName("The earl");
		otherNameIndividualVO.setTitle("Dr.");

		individualCreateVO.setOtherName(List.of(otherNameIndividualVO));

		CharacteristicVO characteristicVO = new CharacteristicVO();
		characteristicVO.setName("Position");
		characteristicVO.setValueType("String");
		characteristicVO.setValue("CEO");

		individualCreateVO.setPartyCharacteristic(List.of(characteristicVO));

		RelatedPartyVO partyVO = new RelatedPartyVO();
		partyVO.setRole("Employer");
		partyVO.setId(orgId);

		individualCreateVO.setRelatedParty(List.of(partyVO));

		SkillVO skillVO = new SkillVO();
		skillVO.setComment("Programming proficiency.");
		skillVO.setEvaluatedLevel("High");
		skillVO.setSkillCode("0123");
		skillVO.setSkillName("Java");
		skillVO.setValidFor(new TimePeriodVO().startDateTime(Instant.now()).endDateTime(Instant.now().plus(Duration.of(100, ChronoUnit.DAYS))));

		individualCreateVO.setSkill(List.of(skillVO));

		return individualCreateVO;
	}

	class TestCharacteristic {
		public final long valuation;
		public final String unit;

		TestCharacteristic(long valuation, String unit) {
			this.valuation = valuation;
			this.unit = unit;
		}
	}

}
