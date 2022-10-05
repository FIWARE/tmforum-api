package org.fiware.tmforum.party;


import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import lombok.RequiredArgsConstructor;
import org.fiware.party.api.IndividualApiTestClient;
import org.fiware.party.api.IndividualApiTestSpec;
import org.fiware.party.model.AttachmentRefOrValueVO;
import org.fiware.party.model.AttachmentRefOrValueVOTestExample;
import org.fiware.party.model.ContactMediumVOTestExample;
import org.fiware.party.model.DisabilityVO;
import org.fiware.party.model.DisabilityVOTestExample;
import org.fiware.party.model.ExternalReferenceVOTestExample;
import org.fiware.party.model.IndividualCreateVO;
import org.fiware.party.model.IndividualCreateVOTestExample;
import org.fiware.party.model.IndividualIdentificationVO;
import org.fiware.party.model.IndividualIdentificationVOTestExample;
import org.fiware.party.model.IndividualUpdateVO;
import org.fiware.party.model.IndividualUpdateVOTestExample;
import org.fiware.party.model.IndividualVO;
import org.fiware.party.model.IndividualVOTestExample;
import org.fiware.party.model.LanguageAbilityVO;
import org.fiware.party.model.LanguageAbilityVOTestExample;
import org.fiware.party.model.OtherNameIndividualVO;
import org.fiware.party.model.OtherNameIndividualVOTestExample;
import org.fiware.party.model.PartyCreditProfileVO;
import org.fiware.party.model.PartyCreditProfileVOTestExample;
import org.fiware.party.model.RelatedPartyVO;
import org.fiware.party.model.RelatedPartyVOTestExample;
import org.fiware.party.model.SkillVO;
import org.fiware.party.model.SkillVOTestExample;
import org.fiware.party.model.TaxDefinitionVO;
import org.fiware.party.model.TaxDefinitionVOTestExample;
import org.fiware.party.model.TaxExemptionCertificateVO;
import org.fiware.party.model.TaxExemptionCertificateVOTestExample;
import org.fiware.party.model.TimePeriodVO;
import org.fiware.party.model.TimePeriodVOTestExample;
import org.fiware.tmforum.common.exception.ErrorDetails;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@RequiredArgsConstructor
@MicronautTest(packages = {"org.fiware.tmforum.party"})
public class IndividualApiIT extends AbstractApiIT implements IndividualApiTestSpec {

    private final IndividualApiTestClient individualApiTestClient;

    private String message;
    private IndividualCreateVO individualCreateVO;
    private IndividualUpdateVO individualUpdateVO;
    private IndividualVO expectedIndividual;

    @ParameterizedTest
    @MethodSource("provideValidIndividuals")
    public void createIndividual201(String message, IndividualCreateVO individualCreateVO, IndividualVO expectedIndividual) throws Exception {
        this.message = message;
        this.individualCreateVO = individualCreateVO;
        this.expectedIndividual = expectedIndividual;
        createIndividual201();
    }

    @Override
    public void createIndividual201() throws Exception {

        HttpResponse<IndividualVO> individualCreateResponse = callAndCatch(() -> individualApiTestClient.createIndividual(individualCreateVO));
        assertEquals(HttpStatus.CREATED, individualCreateResponse.getStatus(), message);
        IndividualVO createdIndividualVO = individualCreateResponse.body();

        expectedIndividual.setId(createdIndividualVO.getId());
        expectedIndividual.setHref(createdIndividualVO.getId());
        assertEquals(expectedIndividual, createdIndividualVO, message);

    }

    private static Stream<Arguments> provideValidIndividuals() {
        List<Arguments> testEntries = new ArrayList<>();

        IndividualCreateVO individualCreateVO = IndividualCreateVOTestExample.build();
        IndividualVO expectedIndividual = IndividualVOTestExample.build();
        testEntries.add(Arguments.of("Empty individual should have been created.", individualCreateVO, expectedIndividual));

        IndividualCreateVO withContactMediumCreateVO = IndividualCreateVOTestExample.build();
        withContactMediumCreateVO.setContactMedium(List.of(ContactMediumVOTestExample.build()));
        IndividualVO expectedWithContactMedium = IndividualVOTestExample.build();
        expectedWithContactMedium.setContactMedium(List.of(ContactMediumVOTestExample.build()));
        testEntries.add(Arguments.of("Individual with contact medium should have been created.", individualCreateVO, expectedIndividual));

        IndividualCreateVO withPartyCreditProfileCreateVO = IndividualCreateVOTestExample.build();
        withPartyCreditProfileCreateVO.setCreditRating(List.of(PartyCreditProfileVOTestExample.build()));
        IndividualVO expectedWithPartyCreditProfile = IndividualVOTestExample.build();
        expectedWithPartyCreditProfile.setCreditRating(List.of(PartyCreditProfileVOTestExample.build()));
        testEntries.add(Arguments.of("Individual with credit profile should have been created.", withPartyCreditProfileCreateVO, expectedWithPartyCreditProfile));

        IndividualCreateVO withDisabilityCreateVO = IndividualCreateVOTestExample.build();
        withDisabilityCreateVO.setDisability(List.of(DisabilityVOTestExample.build()));
        IndividualVO expectedWithDisability = IndividualVOTestExample.build();
        expectedWithDisability.setDisability(List.of(DisabilityVOTestExample.build()));
        testEntries.add(Arguments.of("Individual with disability should have been created.", withDisabilityCreateVO, expectedWithDisability));

        IndividualCreateVO withExternalRefCreateVO = IndividualCreateVOTestExample.build();
        withExternalRefCreateVO.setExternalReference(List.of(ExternalReferenceVOTestExample.build()));
        IndividualVO expectedWithExternalRef = IndividualVOTestExample.build();
        expectedWithExternalRef.setExternalReference(List.of(ExternalReferenceVOTestExample.build()));
        testEntries.add(Arguments.of("Individual with external reference should have been created.", withExternalRefCreateVO, expectedWithExternalRef));

        IndividualCreateVO withIdCreateVO = IndividualCreateVOTestExample.build();
        IndividualIdentificationVO id = IndividualIdentificationVOTestExample.build();
        id.setAttachment(null);
        withIdCreateVO.setIndividualIdentification(List.of(id));
        IndividualVO expectedWithId = IndividualVOTestExample.build();
        expectedWithId.setIndividualIdentification(List.of(id));
        testEntries.add(Arguments.of("Individual with id should have been created.", withIdCreateVO, expectedWithId));

        IndividualCreateVO withLanguageCreateVO = IndividualCreateVOTestExample.build();
        withLanguageCreateVO.setLanguageAbility(List.of(LanguageAbilityVOTestExample.build()));
        IndividualVO expectedWithLanguage = IndividualVOTestExample.build();
        expectedWithLanguage.setLanguageAbility(List.of(LanguageAbilityVOTestExample.build()));
        testEntries.add(Arguments.of("Individual with language ability should have been created.", withLanguageCreateVO, expectedWithLanguage));

        IndividualCreateVO withOtherNameCreateVO = IndividualCreateVOTestExample.build();
        withOtherNameCreateVO.setOtherName(List.of(OtherNameIndividualVOTestExample.build()));
        IndividualVO expectedWithOtherName = IndividualVOTestExample.build();
        expectedWithOtherName.setOtherName(List.of(OtherNameIndividualVOTestExample.build()));
        testEntries.add(Arguments.of("Individual with other name should have been created.", withOtherNameCreateVO, expectedWithOtherName));

        IndividualCreateVO withSkillsCreateVO = IndividualCreateVOTestExample.build();
        withSkillsCreateVO.setSkill(List.of(SkillVOTestExample.build()));
        IndividualVO expectedWithSkills = IndividualVOTestExample.build();
        expectedWithSkills.setSkill(List.of(SkillVOTestExample.build()));
        testEntries.add(Arguments.of("Individual with skill should have been created.", withSkillsCreateVO, expectedWithSkills));

        IndividualCreateVO withTaxExCreateVO = IndividualCreateVOTestExample.build();
        TaxDefinitionVO taxDefinitionVO = TaxDefinitionVOTestExample.build();
        taxDefinitionVO.setId(UUID.randomUUID().toString());
        TaxExemptionCertificateVO taxExemptionCertificateVO = TaxExemptionCertificateVOTestExample.build();
        // prevent duplicates
        taxExemptionCertificateVO.setId(UUID.randomUUID().toString());
        taxExemptionCertificateVO.setTaxDefinition(List.of(taxDefinitionVO));
        // fix the example
        AttachmentRefOrValueVO attachmentRefOrValueVO = AttachmentRefOrValueVOTestExample.build();
        attachmentRefOrValueVO.setHref("http://my-ref.de");
        attachmentRefOrValueVO.setUrl("http://my-url.de");
        taxExemptionCertificateVO.setAttachment(attachmentRefOrValueVO);

        withTaxExCreateVO.setTaxExemptionCertificate(List.of(taxExemptionCertificateVO));
        IndividualVO expectedWithTaxEx = IndividualVOTestExample.build();
        expectedWithTaxEx.setTaxExemptionCertificate(List.of(taxExemptionCertificateVO));
        testEntries.add(Arguments.of("Individual with tax exemption should have been created.", withTaxExCreateVO, expectedWithTaxEx));

        return testEntries.stream();
    }


    @Test
    @Override
    public void createIndividual400() throws Exception {
        for (IndividualCreateVO ocVO : provideInvalidIndividualCreate()) {
            HttpResponse<IndividualVO> individualCreateResponse = callAndCatch(() -> individualApiTestClient.createIndividual(ocVO));
            assertEquals(HttpStatus.BAD_REQUEST, individualCreateResponse.getStatus(), "Individual should not have been created.");

            Optional<ErrorDetails> optionalErrorDetails = individualCreateResponse.getBody(ErrorDetails.class);
            assertTrue(optionalErrorDetails.isPresent(), "Error details should be provided.");
        }
    }

    public List<IndividualCreateVO> provideInvalidIndividualCreate() {

        IndividualCreateVO invalidRelatedPartyOrg = IndividualCreateVOTestExample.build();
        RelatedPartyVO invalidRelatedPartyRef = RelatedPartyVOTestExample.build();
        invalidRelatedPartyOrg.setRelatedParty(List.of(invalidRelatedPartyRef));

        IndividualCreateVO nonExistentRelatedPartyOrg = IndividualCreateVOTestExample.build();
        RelatedPartyVO nonExistentRelatedPartyRef = RelatedPartyVOTestExample.build();
        nonExistentRelatedPartyRef.setId("urn:ngsi-ld:individual:non-existent");
        nonExistentRelatedPartyOrg.setRelatedParty(List.of(nonExistentRelatedPartyRef));

        return List.of(
                invalidRelatedPartyOrg,
                nonExistentRelatedPartyOrg
        );
    }

    @Disabled("Security is handled externally, thus 401 and 403 cannot happen.")
    @Test
    @Override
    public void createIndividual401() throws Exception {
    }

    @Disabled("Security is handled externally, thus 401 and 403 cannot happen.")
    @Test
    @Override
    public void createIndividual403() throws Exception {
    }

    @Disabled("Prohibited by the framework.")
    @Test
    @Override
    public void createIndividual405() throws Exception {

    }

    @Test
    @Override
    public void createIndividual409() throws Exception {
        TaxDefinitionVO taxDefinitionVO = TaxDefinitionVOTestExample.build();
        // make the test repeatable
        taxDefinitionVO.setId(UUID.randomUUID().toString());
        TaxExemptionCertificateVO taxExemptionCertificateVO = TaxExemptionCertificateVOTestExample.build();
        taxExemptionCertificateVO.setTaxDefinition(List.of(taxDefinitionVO));
        // make the test repeatable
        taxExemptionCertificateVO.setId(UUID.randomUUID().toString());
        // workaround for the bad example
        taxExemptionCertificateVO.setAttachment(null);
        IndividualCreateVO individualCreateVO = IndividualCreateVOTestExample.build();
        individualCreateVO.setTaxExemptionCertificate(List.of(taxExemptionCertificateVO));

        // first create should succeed
        assertEquals(
                HttpStatus.CREATED,
                callAndCatch(() -> individualApiTestClient.createIndividual(individualCreateVO)).getStatus(),
                "Individual should have been created.");

        // second should be a conflict, since the tax-exemption already exists.
        HttpResponse<IndividualVO> conflictResponse = callAndCatch(() -> individualApiTestClient.createIndividual(individualCreateVO));
        assertEquals(
                HttpStatus.CONFLICT,
                conflictResponse.getStatus(),
                "Individual should not have been created, due to the conflicting tax-exemption.");

        Optional<ErrorDetails> optionalErrorDetails = conflictResponse.getBody(ErrorDetails.class);
        assertTrue(optionalErrorDetails.isPresent(), "Error details should be provided.");


    }

    @Override
    public void createIndividual500() throws Exception {

    }

    @Test
    @Override
    public void deleteIndividual204() throws Exception {
        // first create one
        IndividualCreateVO individualCreateVO = IndividualCreateVOTestExample.build();
        HttpResponse<IndividualVO> individualCreateResponse = callAndCatch(() -> individualApiTestClient.createIndividual(individualCreateVO));
        assertEquals(HttpStatus.CREATED, individualCreateResponse.getStatus(), "The Individual should have been created first.");

        String individualId = individualCreateResponse.body().getId();

        assertEquals(HttpStatus.NO_CONTENT,
                callAndCatch(() -> individualApiTestClient.deleteIndividual(individualId)).getStatus(),
                "The Individual should have been deleted.");

        assertEquals(HttpStatus.NOT_FOUND,
                callAndCatch(() -> individualApiTestClient.retrieveIndividual(individualId, null)).status(),
                "The Individual should not exist anymore.");

    }

    @Disabled("400 is impossible to happen on deletion with the current implementation.")
    @Test
    @Override
    public void deleteIndividual400() throws Exception {
    }

    @Disabled("Security is handled externally, thus 401 and 403 cannot happen.")
    @Test
    @Override
    public void deleteIndividual401() throws Exception {

    }

    @Disabled("Security is handled externally, thus 401 and 403 cannot happen.")
    @Test
    @Override
    public void deleteIndividual403() throws Exception {

    }

    @Test
    @Override
    public void deleteIndividual404() throws Exception {
        String ngsiLdOrgId = "urn:ngsi-ld:Individual:valid";
        String nonNgsiLdOrgId = "non-ngsi";

        HttpResponse<?> notFoundResponse = callAndCatch(() -> individualApiTestClient.deleteIndividual(ngsiLdOrgId));
        assertEquals(HttpStatus.NOT_FOUND,
                notFoundResponse.getStatus(),
                "No such individual should exist.");

        Optional<ErrorDetails> optionalErrorDetails = notFoundResponse.getBody(ErrorDetails.class);
        assertTrue(optionalErrorDetails.isPresent(), "Error details should be provided.");

        notFoundResponse = callAndCatch(() -> individualApiTestClient.deleteIndividual(nonNgsiLdOrgId));
        assertEquals(HttpStatus.NOT_FOUND,
                notFoundResponse.getStatus(),
                "No such individual should exist.");
        optionalErrorDetails = notFoundResponse.getBody(ErrorDetails.class);
        assertTrue(optionalErrorDetails.isPresent(), "Error details should be provided.");
    }

    @Disabled("Prohibited by the framework.")
    @Test
    @Override
    public void deleteIndividual405() throws Exception {

    }

    @Disabled("Impossible status.")
    @Test
    @Override
    public void deleteIndividual409() throws Exception {

    }

    @Override
    public void deleteIndividual500() throws Exception {

    }

    @Disabled("Needs db cleaning.")
    @Test
    @Override
    public void listIndividual200() throws Exception {
        // find a way to clean before
        List<IndividualVO> expectedIndividuals = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            IndividualCreateVO individualCreateVO = IndividualCreateVOTestExample.build();
            String id = individualApiTestClient.createIndividual(individualCreateVO).body().getId();
            IndividualVO individualVO = IndividualVOTestExample.build();
            individualVO.setId(id);
            individualVO.setHref(id);
            expectedIndividuals.add(individualVO);
        }

        HttpResponse<List<IndividualVO>> individualListResponse = callAndCatch(() -> individualApiTestClient.listIndividual(null, null, null));
        assertEquals(HttpStatus.OK, individualListResponse.getStatus(), "The list should be accessible.");

        // ignore order
        List<IndividualVO> individualVOS = individualListResponse.body();
        assertEquals(expectedIndividuals.size(), individualVOS.size(), "All individuals should be returned.");
        expectedIndividuals
                .forEach(individualVO ->
                        assertTrue(individualVOS.contains(individualVO),
                                String.format("All individuals should be contained. Missing: %s", individualVO)));

        // get with pagination
        Integer limit = 5;
        HttpResponse<List<IndividualVO>> firstPartResponse = callAndCatch(() -> individualApiTestClient.listIndividual(null, 0, limit));
        assertEquals(limit, firstPartResponse.body(), "Only the requested number of entries should be returend.");
        HttpResponse<List<IndividualVO>> secondPartResponse = callAndCatch(() -> individualApiTestClient.listIndividual(null, 0 + limit, limit));
        assertEquals(limit, secondPartResponse.body(), "Only the requested number of entries should be returend.");

        List<IndividualVO> retrievedIndividuals = firstPartResponse.body();
        retrievedIndividuals.addAll(secondPartResponse.body());
        expectedIndividuals
                .forEach(individualVO ->
                        assertTrue(retrievedIndividuals.contains(individualVO),
                                String.format("All individuals should be contained. Missing: %s", individualVO)));
    }

    @Test
    @Override
    public void listIndividual400() throws Exception {
        HttpResponse<List<IndividualVO>> badRequestResponse = callAndCatch(() -> individualApiTestClient.listIndividual(null, -1, null));
        assertEquals(HttpStatus.BAD_REQUEST,
                badRequestResponse.getStatus(),
                "Negative offsets are impossible.");

        Optional<ErrorDetails> optionalErrorDetails = badRequestResponse.getBody(ErrorDetails.class);
        assertTrue(optionalErrorDetails.isPresent(), "Error details should be provided.");

        badRequestResponse = callAndCatch(() -> individualApiTestClient.listIndividual(null, null, -1));
        assertEquals(HttpStatus.BAD_REQUEST,
                badRequestResponse.getStatus(),
                "Negative limits are impossible.");
        optionalErrorDetails = badRequestResponse.getBody(ErrorDetails.class);
        assertTrue(optionalErrorDetails.isPresent(), "Error details should be provided.");

    }

    @Disabled("Security is handled externally, thus 401 and 403 cannot happen.")
    @Test
    @Override
    public void listIndividual401() throws Exception {

    }

    @Disabled("Security is handled externally, thus 401 and 403 cannot happen.")
    @Test
    @Override
    public void listIndividual403() throws Exception {

    }

    @Disabled("Not found is not possible here, will be answerd with an empty list instead.")
    @Test
    @Override
    public void listIndividual404() throws Exception {

    }

    @Disabled("Prohibited by the framework.")
    @Test
    @Override
    public void listIndividual405() throws Exception {

    }

    @Disabled("Impossible status.")
    @Test
    @Override
    public void listIndividual409() throws Exception {

    }

    @Override
    public void listIndividual500() throws Exception {

    }

    @ParameterizedTest
    @MethodSource("provideValidPatches")
    public void patchIndividual200(String message, IndividualUpdateVO individualUpdateVO, IndividualVO expectedIndividual) throws Exception {
        this.message = message;
        this.individualUpdateVO = individualUpdateVO;
        this.expectedIndividual = expectedIndividual;
        patchIndividual200();
    }

    @Override
    public void patchIndividual200() throws Exception {

        IndividualCreateVO individualCreateVO = IndividualCreateVOTestExample.build();

        HttpResponse<IndividualVO> individualCreateResponse = callAndCatch(() -> individualApiTestClient.createIndividual(individualCreateVO));
        assertEquals(HttpStatus.CREATED, individualCreateResponse.getStatus(), message);

        String individualId = individualCreateResponse.body().getId();

        HttpResponse<IndividualVO> individualUpdateResponse = callAndCatch(() -> individualApiTestClient.patchIndividual(individualId, individualUpdateVO));
        assertEquals(HttpStatus.OK, individualUpdateResponse.getStatus(), message);

        expectedIndividual.setHref(individualId);
        expectedIndividual.setId(individualId);

        // no list is initiated in empty case
        if (expectedIndividual.getTaxExemptionCertificate() != null && expectedIndividual.getTaxExemptionCertificate().isEmpty()) {
            expectedIndividual.setTaxExemptionCertificate(null);
        }

        if (expectedIndividual.getRelatedParty() != null && expectedIndividual.getRelatedParty().isEmpty()) {
            expectedIndividual.setRelatedParty(null);
        }

        assertEquals(expectedIndividual, individualUpdateResponse.body(), message);

        HttpResponse<IndividualVO> individualGetResponse = callAndCatch(() -> individualApiTestClient.retrieveIndividual(individualId, null));
        assertEquals(expectedIndividual, individualGetResponse.body(), message);
    }

    private static Stream<Arguments> provideValidPatches() {
        List<Arguments> testEntries = new ArrayList<>();

        IndividualUpdateVO individualUpdateVO = IndividualUpdateVOTestExample.build();
        IndividualVO expectedIndividual = IndividualVOTestExample.build();
        testEntries.add(Arguments.of("Empty individual should have been updated.", individualUpdateVO, expectedIndividual));

        IndividualUpdateVO withContactMediumUpdateVO = IndividualUpdateVOTestExample.build();
        withContactMediumUpdateVO.setContactMedium(List.of(ContactMediumVOTestExample.build()));
        IndividualVO expectedWithContactMedium = IndividualVOTestExample.build();
        expectedWithContactMedium.setContactMedium(List.of(ContactMediumVOTestExample.build()));
        testEntries.add(Arguments.of("Individual with contact medium should have been updated.", individualUpdateVO, expectedIndividual));

        IndividualUpdateVO withPartyCreditProfileUpdateVO = IndividualUpdateVOTestExample.build();
        PartyCreditProfileVO partyCreditProfileVO = PartyCreditProfileVOTestExample.build();
        TimePeriodVO timePeriodVO = TimePeriodVOTestExample.build();
        timePeriodVO.setStartDateTime(Instant.now());
        timePeriodVO.setStartDateTime(Instant.now());
        partyCreditProfileVO.setValidFor(timePeriodVO);
        withPartyCreditProfileUpdateVO.setCreditRating(List.of(partyCreditProfileVO));
        IndividualVO expectedWithPartyCreditProfile = IndividualVOTestExample.build();
        expectedWithPartyCreditProfile.setCreditRating(List.of(partyCreditProfileVO));
        testEntries.add(Arguments.of("Individual with credit profile should have been updated.", withPartyCreditProfileUpdateVO, expectedWithPartyCreditProfile));

        IndividualUpdateVO withDisabilityUpdateVO = IndividualUpdateVOTestExample.build();
        DisabilityVO disabilityVO = DisabilityVOTestExample.build();
        disabilityVO.setValidFor(timePeriodVO);
        withDisabilityUpdateVO.setDisability(List.of(disabilityVO));
        IndividualVO expectedWithDisability = IndividualVOTestExample.build();
        expectedWithDisability.setDisability(List.of(disabilityVO));
        testEntries.add(Arguments.of("Individual with disability should have been updated.", withDisabilityUpdateVO, expectedWithDisability));

        IndividualUpdateVO withExternalRefUpdateVO = IndividualUpdateVOTestExample.build();
        withExternalRefUpdateVO.setExternalReference(List.of(ExternalReferenceVOTestExample.build()));
        IndividualVO expectedWithExternalRef = IndividualVOTestExample.build();
        expectedWithExternalRef.setExternalReference(List.of(ExternalReferenceVOTestExample.build()));
        testEntries.add(Arguments.of("Individual with external reference should have been updated.", withExternalRefUpdateVO, expectedWithExternalRef));

        IndividualUpdateVO withIdUpdateVO = IndividualUpdateVOTestExample.build();
        IndividualIdentificationVO id = IndividualIdentificationVOTestExample.build();
        id.setAttachment(null);
        id.setValidFor(timePeriodVO);
        withIdUpdateVO.setIndividualIdentification(List.of(id));
        IndividualVO expectedWithId = IndividualVOTestExample.build();
        expectedWithId.setIndividualIdentification(List.of(id));
        testEntries.add(Arguments.of("Individual with id should have been updated.", withIdUpdateVO, expectedWithId));

        IndividualUpdateVO withLanguageUpdateVO = IndividualUpdateVOTestExample.build();
        LanguageAbilityVO languageAbilityVO = LanguageAbilityVOTestExample.build();
        languageAbilityVO.setValidFor(timePeriodVO);
        withLanguageUpdateVO.setLanguageAbility(List.of(languageAbilityVO));
        IndividualVO expectedWithLanguage = IndividualVOTestExample.build();
        expectedWithLanguage.setLanguageAbility(List.of(languageAbilityVO));
        testEntries.add(Arguments.of("Individual with language ability should have been updated.", withLanguageUpdateVO, expectedWithLanguage));

        IndividualUpdateVO withOtherNameUpdateVO = IndividualUpdateVOTestExample.build();
        OtherNameIndividualVO otherNameIndividualVO = OtherNameIndividualVOTestExample.build();
        otherNameIndividualVO.setValidFor(timePeriodVO);
        withOtherNameUpdateVO.setOtherName(List.of(otherNameIndividualVO));
        IndividualVO expectedWithOtherName = IndividualVOTestExample.build();
        expectedWithOtherName.setOtherName(List.of(otherNameIndividualVO));
        testEntries.add(Arguments.of("Individual with other name should have been updated.", withOtherNameUpdateVO, expectedWithOtherName));

        IndividualUpdateVO withSkillsUpdateVO = IndividualUpdateVOTestExample.build();
        SkillVO skillVO = SkillVOTestExample.build();
        skillVO.setValidFor(timePeriodVO);
        withSkillsUpdateVO.setSkill(List.of(skillVO));
        IndividualVO expectedWithSkills = IndividualVOTestExample.build();
        expectedWithSkills.setSkill(List.of(skillVO));
        testEntries.add(Arguments.of("Individual with skill should have been updated.", withSkillsUpdateVO, expectedWithSkills));

        IndividualUpdateVO withTaxExUpdateVO = IndividualUpdateVOTestExample.build();
        TaxDefinitionVO taxDefinitionVO = TaxDefinitionVOTestExample.build();
        taxDefinitionVO.setId(UUID.randomUUID().toString());
        TaxExemptionCertificateVO taxExemptionCertificateVO = TaxExemptionCertificateVOTestExample.build();
        // prevent duplicates
        taxExemptionCertificateVO.setId(UUID.randomUUID().toString());
        taxExemptionCertificateVO.setTaxDefinition(List.of(taxDefinitionVO));
        taxExemptionCertificateVO.setValidFor(timePeriodVO);
        // fix the example
        AttachmentRefOrValueVO attachmentRefOrValueVO = AttachmentRefOrValueVOTestExample.build();
        attachmentRefOrValueVO.setHref("http://my-ref.de");
        attachmentRefOrValueVO.setUrl("http://my-url.de");
        attachmentRefOrValueVO.validFor(timePeriodVO);
        taxExemptionCertificateVO.setAttachment(attachmentRefOrValueVO);

        withTaxExUpdateVO.setTaxExemptionCertificate(List.of(taxExemptionCertificateVO));
        IndividualVO expectedWithTaxEx = IndividualVOTestExample.build();
        expectedWithTaxEx.setTaxExemptionCertificate(List.of(taxExemptionCertificateVO));
        testEntries.add(Arguments.of("Individual with tax exemption should have been updated.", withTaxExUpdateVO, expectedWithTaxEx));

        return testEntries.stream();
    }


    @Test
    @Override
    public void patchIndividual400() throws Exception {

        IndividualCreateVO individualCreateVO = IndividualCreateVOTestExample.build();
        HttpResponse<IndividualVO> individualCreateResponse = callAndCatch(() -> individualApiTestClient.createIndividual(individualCreateVO));
        assertEquals(HttpStatus.CREATED, individualCreateResponse.getStatus(), "The individual should have been initially created. ");

        String individualId = individualCreateResponse.body().getId();

        for (IndividualUpdateVO ouVO : provideInvalidIndividualUpdate()) {
            HttpResponse<IndividualVO> individualUpdateResponse = callAndCatch(() -> individualApiTestClient.patchIndividual(individualId, ouVO));
            assertEquals(HttpStatus.BAD_REQUEST, individualUpdateResponse.getStatus(), "Individual should not have been created.");

            Optional<ErrorDetails> optionalErrorDetails = individualUpdateResponse.getBody(ErrorDetails.class);
            assertTrue(optionalErrorDetails.isPresent(), "Error details should be provided.");
        }
    }

    public static List<IndividualUpdateVO> provideInvalidIndividualUpdate() {

        IndividualUpdateVO invalidRelatedPartyIndividual = IndividualUpdateVOTestExample.build();
        RelatedPartyVO invalidRelatedPartyRef = RelatedPartyVOTestExample.build();
        invalidRelatedPartyIndividual.setRelatedParty(List.of(invalidRelatedPartyRef));

        IndividualUpdateVO nonExistentRelatedPartyOrg = IndividualUpdateVOTestExample.build();
        RelatedPartyVO nonExistentRelatedPartyRef = RelatedPartyVOTestExample.build();
        nonExistentRelatedPartyRef.setId("urn:ngsi-ld:individual:non-existent");
        nonExistentRelatedPartyOrg.setRelatedParty(List.of(nonExistentRelatedPartyRef));

        return List.of(
                invalidRelatedPartyIndividual,
                nonExistentRelatedPartyOrg
        );
    }

    @Disabled("Security is handled externally, thus 401 and 403 cannot happen.")
    @Test
    @Override
    public void patchIndividual401() throws Exception {

    }

    @Disabled("Security is handled externally, thus 401 and 403 cannot happen.")
    @Test
    @Override
    public void patchIndividual403() throws Exception {

    }

    @Disabled("Not implemented yet")
    @Test
    @Override
    public void patchIndividual404() throws Exception {
        IndividualUpdateVO individualUpdateVO = IndividualUpdateVOTestExample.build();
        assertEquals(
                HttpStatus.NOT_FOUND,
                callAndCatch(() -> individualApiTestClient.patchIndividual("urn:ngsi-ld:Individual:not-existent", individualUpdateVO)).getStatus(),
                "Non existent individuals should not be updated.");
    }

    @Disabled("Prohibited by the framework.")
    @Test
    @Override
    public void patchIndividual405() throws Exception {

    }

    @Disabled("TODO: Decide if that can happen.")
    @Test
    @Override
    public void patchIndividual409() throws Exception {

    }

    @Override
    public void patchIndividual500() throws Exception {

    }

    @Test
    @Override
    public void retrieveIndividual200() throws Exception {
        IndividualCreateVO individualCreateVO = IndividualCreateVOTestExample.build();
        HttpResponse<IndividualVO> createdOrg = callAndCatch(() -> individualApiTestClient.createIndividual(individualCreateVO));
        assertEquals(HttpStatus.CREATED, createdOrg.getStatus(), "Create the org to retrieve.");

        String individualId = createdOrg.body().getId();

        IndividualVO expectedIndividual = IndividualVOTestExample.build();
        expectedIndividual.setId(individualId);
        expectedIndividual.setHref(individualId);
        // no list is initiated in that case
        expectedIndividual.setTaxExemptionCertificate(null);
        expectedIndividual.setRelatedParty(null);

        HttpResponse<IndividualVO> retrievedIndividual = callAndCatch(() -> individualApiTestClient.retrieveIndividual(individualId, null));
        assertEquals(HttpStatus.OK, retrievedIndividual.getStatus(), "The retrieval should be ok.");
        assertEquals(expectedIndividual, retrievedIndividual.body(), "The correct org should be returned.");
    }

    @Disabled("400 cannot happen, only 404")
    @Test
    @Override
    public void retrieveIndividual400() throws Exception {

    }

    @Disabled("Security is handled externally, thus 401 and 403 cannot happen.")
    @Test
    @Override
    public void retrieveIndividual401() throws Exception {

    }

    @Disabled("Security is handled externally, thus 401 and 403 cannot happen.")
    @Test
    @Override
    public void retrieveIndividual403() throws Exception {

    }

    @Test
    @Override
    public void retrieveIndividual404() throws Exception {
        HttpResponse<IndividualVO> notFoundResponse = callAndCatch(() -> individualApiTestClient.retrieveIndividual("urn:ngsi-ld:individual:not-found", null));

        assertEquals(HttpStatus.NOT_FOUND, notFoundResponse.getStatus(), "No such org exists.");

        Optional<ErrorDetails> optionalErrorDetails = notFoundResponse.getBody(ErrorDetails.class);
        assertTrue(optionalErrorDetails.isPresent(), "Error details should have been provided.");
    }

    @Disabled("Prohibited by the framework.")
    @Test
    @Override
    public void retrieveIndividual405() throws Exception {

    }

    @Disabled("Conflict not possible on retrieval")
    @Test
    @Override
    public void retrieveIndividual409() throws Exception {

    }

    @Override
    public void retrieveIndividual500() throws Exception {

    }
}
