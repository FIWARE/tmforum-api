package org.fiware.tmforum.party;

import org.fiware.party.model.AttachmentVO;
import org.fiware.party.model.CharacteristicVO;
import org.fiware.party.model.ContactMediumVO;
import org.fiware.party.model.DisabilityVO;
import org.fiware.party.model.ExternalReferenceVO;
import org.fiware.party.model.IndividualCreateVO;
import org.fiware.party.model.IndividualIdentificationVO;
import org.fiware.party.model.IndividualStateTypeVO;
import org.fiware.party.model.IndividualUpdateVO;
import org.fiware.party.model.IndividualVO;
import org.fiware.party.model.LanguageAbilityVO;
import org.fiware.party.model.MediumCharacteristicVO;
import org.fiware.party.model.OrganizationChildRelationshipVO;
import org.fiware.party.model.OrganizationCreateVO;
import org.fiware.party.model.OrganizationParentRelationshipVO;
import org.fiware.party.model.OrganizationRefVO;
import org.fiware.party.model.OrganizationUpdateVO;
import org.fiware.party.model.OrganizationVO;
import org.fiware.party.model.OtherNameIndividualVO;
import org.fiware.party.model.OtherNameOrganizationVO;
import org.fiware.party.model.PartyCreditProfileVO;
import org.fiware.party.model.QuantityVO;
import org.fiware.party.model.RelatedPartyVO;
import org.fiware.party.model.SkillVO;
import org.fiware.party.model.TaxDefinitionVO;
import org.fiware.party.model.TaxExemptionCertificateVO;
import org.fiware.party.model.TimePeriodVO;
import org.fiware.tmforum.common.mapping.IdHelper;
import org.fiware.tmforum.mapping.MappingException;
import org.fiware.tmforum.party.domain.Attachment;
import org.fiware.tmforum.party.domain.Characteristic;
import org.fiware.tmforum.common.domain.ContactMedium;
import org.fiware.tmforum.party.domain.ExternalReference;
import org.fiware.tmforum.common.domain.MediumCharacteristic;
import org.fiware.tmforum.party.domain.PartyCreditProfile;
import org.fiware.tmforum.common.domain.Quantity;
import org.fiware.tmforum.common.domain.RelatedParty;
import org.fiware.tmforum.party.domain.TaxDefinition;
import org.fiware.tmforum.party.domain.TaxExemptionCertificate;
import org.fiware.tmforum.common.domain.TimePeriod;
import org.fiware.tmforum.party.domain.individual.Disability;
import org.fiware.tmforum.party.domain.individual.Individual;
import org.fiware.tmforum.party.domain.individual.IndividualIdentification;
import org.fiware.tmforum.party.domain.individual.IndividualState;
import org.fiware.tmforum.party.domain.individual.LanguageAbility;
import org.fiware.tmforum.party.domain.individual.OtherIndividualName;
import org.fiware.tmforum.party.domain.individual.Skill;
import org.fiware.tmforum.party.domain.organization.Organization;
import org.fiware.tmforum.party.domain.organization.OrganizationChildRelationship;
import org.fiware.tmforum.party.domain.organization.OrganizationParentRelationship;
import org.fiware.tmforum.party.domain.organization.OtherOrganizationName;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;

/**
 * Mapper between the internal model and api-domain objects
 */
@Mapper(componentModel = "jsr330", uses = IdHelper.class)
public interface TMForumMapper {

    // using inline expression, since else it might overwrite the String-String mapping
    @Mapping(target = "id", source = "id")
    @Mapping(target = "href", source = "id")
    OrganizationVO map(OrganizationCreateVO organizationCreateVO, URI id);


    @Mapping(target = "isHeadOffice", source = "headOffice")
    @Mapping(target = "isLegalEntity", source = "legalEntity")
    @Mapping(target = "status", source = "organizationState")
    OrganizationVO map(Organization organization);

    @Mapping(target = "organizationState", source = "status")
    @Mapping(target = "href", source = "id")
    Organization map(OrganizationVO organizationVO);

    @Mapping(target = "id", source = "id")
    @Mapping(target = "href", source = "id")
    OrganizationVO map(OrganizationUpdateVO organizationUpdateVO, String id);

    @Mapping(target = "id", source = "id")
    @Mapping(target = "href", source = "id")
    IndividualVO map(IndividualCreateVO individualCreateVO, URI id);

    @Mapping(target = "id", source = "id")
    @Mapping(target = "href", source = "id")
    IndividualVO map(IndividualUpdateVO individualUpdateVO, String id);


    @Mapping(target = "status", source = "individualState")
    IndividualVO map(Individual individual);

    @Mapping(target = "individualState", source = "status")
    @Mapping(target = "href", source = "id")
    Individual map(IndividualVO individualVO);

    RelatedParty map(RelatedPartyVO relatedPartyVO);

    RelatedPartyVO map(RelatedParty relatedParty);

    @Mapping(source = "characteristic", target = "mediumCharacteristic")
    ContactMedium map(ContactMediumVO contactMediumVO);

    @Mapping(target = "characteristic", source = "mediumCharacteristic")
    @Mapping(target = "validFor", source = "validFor")
    ContactMediumVO map(ContactMedium contactMedium);

    CharacteristicVO map(Characteristic characteristic);

    Characteristic map(CharacteristicVO characteristicVO);

    Attachment map(AttachmentVO attachmentVO);

    AttachmentVO map(Attachment attachment);

    ExternalReference map(ExternalReferenceVO externalReferenceVO);

    ExternalReferenceVO map(ExternalReference externalReference);

    MediumCharacteristic map(MediumCharacteristicVO mediumCharacteristicVO);

    MediumCharacteristicVO map(MediumCharacteristic mediumCharacteristic);

    OtherOrganizationName map(OtherNameOrganizationVO otherNameOrganizationVO);

    OtherNameOrganizationVO map(OtherOrganizationName otherOrganizationName);

    PartyCreditProfile map(PartyCreditProfileVO value);

    PartyCreditProfileVO map(PartyCreditProfile partyCreditProfile);

    Quantity map(QuantityVO quantityVO);

    QuantityVO map(Quantity quantity);


    TaxDefinition map(TaxDefinitionVO taxDefinitionVO);

    @Mapping(target = "id", qualifiedByName = {"IdHelper", "FromNgsiLd"})
    TaxDefinitionVO map(TaxDefinition taxDefinition);

    TaxExemptionCertificate map(TaxExemptionCertificateVO taxExemptionCertificateVO);

    @Mapping(target = "id", qualifiedByName = {"IdHelper", "FromNgsiLd"})
    TaxExemptionCertificateVO map(TaxExemptionCertificate taxExemptionCertificate);

    TimePeriodVO map(TimePeriod timePeriod);

    Disability map(DisabilityVO disabilityVO);

    DisabilityVO map(Disability disability);

    IndividualIdentification map(IndividualIdentificationVO individualIdentificationVO);

    IndividualIdentificationVO map(IndividualIdentification individualIdentification);

    LanguageAbility map(LanguageAbilityVO languageAbilityVO);

    @Mapping(target = "isFavouriteLanguage", source = "favouriteLanguage")
    LanguageAbilityVO map(LanguageAbility languageAbility);

    OtherIndividualName map(OtherNameIndividualVO otherNameIndividualVO);

    OtherNameIndividualVO map(OtherIndividualName otherIndividualName);

    Skill map(SkillVO skillVO);

    SkillVO map(Skill skill);

    IndividualState map(IndividualStateTypeVO individualStateTypeVO);

    IndividualStateTypeVO map(IndividualState individualState);


    default OrganizationParentRelationshipVO map(OrganizationParentRelationship organizationParentRelationship) {
        if (organizationParentRelationship == null) {
            return null;
        }
        OrganizationParentRelationshipVO organizationParentRelationshipVO = new OrganizationParentRelationshipVO();
        organizationParentRelationshipVO.setRelationshipType(organizationParentRelationship.getRelationshipType());
        organizationParentRelationshipVO.setAtBaseType(organizationParentRelationship.getAtBaseType());
        organizationParentRelationshipVO.setAtSchemaLocation(organizationParentRelationship.getAtSchemaLocation());
        organizationParentRelationshipVO.setAtType(organizationParentRelationship.getAtType());
        OrganizationRefVO organizationRefVO = new OrganizationRefVO();
        organizationRefVO.setId(organizationParentRelationship.getId().toString());
        organizationRefVO.setName(organizationParentRelationship.getName());
        if(organizationParentRelationship.getHref() != null) {
            organizationRefVO.setHref(organizationParentRelationship.getHref().toString());
        }
        organizationParentRelationshipVO.setOrganization(organizationRefVO);
        return organizationParentRelationshipVO;
    }

    default OrganizationChildRelationshipVO map(OrganizationChildRelationship organizationChildRelationship) {
        if (organizationChildRelationship == null) {
            return null;
        }
        OrganizationChildRelationshipVO organizationChildRelationshipVO = new OrganizationChildRelationshipVO();
        organizationChildRelationshipVO.setRelationshipType(organizationChildRelationship.getRelationshipType());
        organizationChildRelationshipVO.setAtBaseType(organizationChildRelationship.getAtBaseType());
        organizationChildRelationshipVO.setAtSchemaLocation(organizationChildRelationship.getAtSchemaLocation());
        organizationChildRelationshipVO.setAtType(organizationChildRelationship.getAtType());
        OrganizationRefVO organizationRefVO = new OrganizationRefVO();
        organizationRefVO.setId(organizationChildRelationship.getId().toString());
        organizationRefVO.setName(organizationChildRelationship.getName());
        if (organizationChildRelationship.getHref() != null) {
            organizationRefVO.setHref(organizationChildRelationship.getHref().toString());
        }
        organizationChildRelationshipVO.setOrganization(organizationRefVO);
        return organizationChildRelationshipVO;
    }

    default OrganizationParentRelationship map(OrganizationParentRelationshipVO organizationParentRelationshipVO) {
        if (organizationParentRelationshipVO == null) {
            return null;
        }
        OrganizationParentRelationship organizationParentRelationship = new OrganizationParentRelationship(organizationParentRelationshipVO.getOrganization().getId());
        organizationParentRelationship.setRelationshipType(organizationParentRelationshipVO.getRelationshipType());
        organizationParentRelationship.setAtType(organizationParentRelationshipVO.getAtType());
        organizationParentRelationship.setAtSchemaLocation(organizationParentRelationshipVO.getAtSchemaLocation());
        organizationParentRelationship.setAtBaseType(organizationParentRelationshipVO.getAtBaseType());
        organizationParentRelationship.setName(organizationParentRelationshipVO.getOrganization().getName());
        return organizationParentRelationship;
    }

    default OrganizationChildRelationship map(OrganizationChildRelationshipVO organizationChildRelationshipVO) {
        if (organizationChildRelationshipVO == null) {
            return null;
        }
        OrganizationChildRelationship organizationChildRelationship = new OrganizationChildRelationship(organizationChildRelationshipVO.getOrganization().getId());
        organizationChildRelationship.setRelationshipType(organizationChildRelationshipVO.getRelationshipType());
        organizationChildRelationship.setAtType(organizationChildRelationshipVO.getAtType());
        organizationChildRelationship.setAtSchemaLocation(organizationChildRelationshipVO.getAtSchemaLocation());
        organizationChildRelationship.setAtBaseType(organizationChildRelationshipVO.getAtBaseType());
        organizationChildRelationship.setName(organizationChildRelationshipVO.getOrganization().getName());
        organizationChildRelationship.setHref(URI.create(organizationChildRelationshipVO.getOrganization().getHref()));
        return organizationChildRelationship;
    }

    TimePeriod map(TimePeriodVO value);

    default URL map(String value) {
        if (value == null) {
            return null;
        }
        try {
            return new URL(value);
        } catch (MalformedURLException e) {
            throw new MappingException(String.format("%s is not a URL.", value), e);
        }
    }

    default String map(URL value) {
        if (value == null) {
            return null;
        }
        return value.toString();
    }

    default URI mapToURI(String value) {
        if (value == null) {
            return null;
        }
        return URI.create(value);
    }

    default String mapFromURI(URI value) {
        if (value == null) {
            return null;
        }
        return value.toString();
    }

}


