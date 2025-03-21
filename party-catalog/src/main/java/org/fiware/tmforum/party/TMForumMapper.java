package org.fiware.tmforum.party;

import io.github.wistefan.mapping.MappingException;
import org.fiware.party.model.*;
import org.fiware.tmforum.common.domain.AttachmentRefOrValue;
import org.fiware.tmforum.common.domain.Characteristic;
import org.fiware.tmforum.common.domain.TaxExemptionCertificate;
import org.fiware.tmforum.common.domain.TimePeriod;
import org.fiware.tmforum.common.domain.subscription.TMForumSubscription;
import org.fiware.tmforum.common.mapping.BaseMapper;
import org.fiware.tmforum.common.mapping.IdHelper;
import org.fiware.tmforum.party.domain.individual.Individual;
import org.fiware.tmforum.party.domain.individual.LanguageAbility;
import org.fiware.tmforum.party.domain.organization.Organization;
import org.fiware.tmforum.party.domain.organization.OrganizationChildRelationship;
import org.fiware.tmforum.party.domain.organization.OrganizationParentRelationship;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;

/**
 * Mapper between the internal model and api-domain objects
 */
@Mapper(componentModel = "jsr330", uses = IdHelper.class)
public abstract class TMForumMapper extends BaseMapper {

	// using inline expression, since else it might overwrite the String-String mapping
	@Mapping(target = "id", source = "id")
	@Mapping(target = "href", source = "id")
	public abstract OrganizationVO map(OrganizationCreateVO organizationCreateVO, URI id);

	public abstract OrganizationVO map(Organization organization);

	@Mapping(target = "href", source = "id")
	public abstract Organization map(OrganizationVO organizationVO);

	@Mapping(target = "id", source = "id")
	public abstract Organization map(OrganizationUpdateVO organizationUpdateVO, String id);

	@Mapping(target = "id", source = "id")
	@Mapping(target = "href", source = "id")
	public abstract IndividualVO map(IndividualCreateVO individualCreateVO, URI id);

	@Mapping(target = "id", source = "id")
	public abstract Individual map(IndividualUpdateVO individualUpdateVO, String id);

	public abstract IndividualVO map(Individual individual);

	@Mapping(target = "href", source = "id")
	public abstract Individual map(IndividualVO individualVO);

	@Mapping(target = "isFavouriteLanguage", source = "favouriteLanguage")
	public abstract LanguageAbilityVO map(LanguageAbility languageAbility);

	@Mapping(target = "query", source = "rawQuery")
	public abstract EventSubscriptionVO map(TMForumSubscription subscription);

	@Mapping(target = "characteristicValue", source = "value")
	public abstract Characteristic map(CharacteristicVO characteristicVO);

	@Mapping(target = "value", source = "characteristicValue")
	public abstract CharacteristicVO map(Characteristic characteristic);

	@Mapping(target = "certificateId", source = "id")
	public abstract TaxExemptionCertificate map(TaxExemptionCertificateVO taxExemptionCertificateVO);

	@Mapping(target = "id", source = "certificateId")
	public abstract TaxExemptionCertificateVO map(TaxExemptionCertificate taxExemptionCertificate);

	@Mapping(target = "attachementId", source = "id")
	public abstract AttachmentRefOrValue map(AttachmentRefOrValueVO attachmentRefOrValueVO);

	@Mapping(target = "id", source = "attachementId")
	public abstract AttachmentRefOrValueVO map(AttachmentRefOrValue attachmentRefOrValue);

	public OrganizationParentRelationshipVO map(OrganizationParentRelationship organizationParentRelationship) {
		if (organizationParentRelationship == null) {
			return null;
		}
		OrganizationParentRelationshipVO organizationParentRelationshipVO = new OrganizationParentRelationshipVO();
		organizationParentRelationshipVO.setRelationshipType(organizationParentRelationship.getRelationshipType());
		organizationParentRelationshipVO.setAtBaseType(organizationParentRelationship.getAtBaseType());
		organizationParentRelationshipVO.setAtSchemaLocation(organizationParentRelationship.getAtSchemaLocation());
		organizationParentRelationshipVO.setAtType(organizationParentRelationship.getAtType());
		OrganizationRefVO organizationRefVO = new OrganizationRefVO();
		organizationRefVO.setId(organizationParentRelationship.getEntityId().toString());
		if (organizationParentRelationship.getHref() != null) {
			organizationRefVO.setHref(organizationParentRelationship.getHref().toString());
		}
		organizationRefVO.setName(organizationParentRelationship.getName());
		organizationRefVO.setAtReferredType(organizationParentRelationship.getAtReferredType());
		organizationRefVO.setAtType(organizationParentRelationship.getAtType());
		organizationRefVO.setAtBaseType(organizationParentRelationship.getAtBaseType());
		organizationRefVO.setAtSchemaLocation(organizationParentRelationship.getAtSchemaLocation());
		if (organizationParentRelationship.getHref() != null) {
			organizationRefVO.setHref(organizationParentRelationship.getHref().toString());
		}
		organizationParentRelationshipVO.setOrganization(organizationRefVO);
		return organizationParentRelationshipVO;
	}

	public OrganizationChildRelationshipVO map(OrganizationChildRelationship organizationChildRelationship) {
		if (organizationChildRelationship == null) {
			return null;
		}
		OrganizationChildRelationshipVO organizationChildRelationshipVO = new OrganizationChildRelationshipVO();
		organizationChildRelationshipVO.setRelationshipType(organizationChildRelationship.getRelationshipType());
		organizationChildRelationshipVO.setAtBaseType(organizationChildRelationship.getAtBaseType());
		organizationChildRelationshipVO.setAtSchemaLocation(organizationChildRelationship.getAtSchemaLocation());
		organizationChildRelationshipVO.setAtType(organizationChildRelationship.getAtType());
		organizationChildRelationship.setAtReferredType(organizationChildRelationship.getAtReferredType());
		OrganizationRefVO organizationRefVO = new OrganizationRefVO();
		organizationRefVO.setId(organizationChildRelationship.getEntityId().toString());
		if (organizationChildRelationship.getHref() != null) {
			organizationRefVO.setHref(organizationChildRelationship.getHref().toString());
		}
		organizationRefVO.setName(organizationChildRelationship.getName());
		organizationRefVO.setAtSchemaLocation(organizationChildRelationship.getAtSchemaLocation());
		organizationRefVO.setAtBaseType(organizationChildRelationship.getAtBaseType());
		organizationRefVO.setAtType(organizationChildRelationship.getAtType());
		organizationRefVO.setAtReferredType(organizationChildRelationship.getAtReferredType());
		if (organizationChildRelationship.getHref() != null) {
			organizationRefVO.setHref(organizationChildRelationship.getHref().toString());
		}
		organizationChildRelationshipVO.setOrganization(organizationRefVO);
		return organizationChildRelationshipVO;
	}

	public OrganizationParentRelationship map(OrganizationParentRelationshipVO organizationParentRelationshipVO) {
		if (organizationParentRelationshipVO == null) {
			return null;
		}
		if (organizationParentRelationshipVO.getOrganization() == null) {
			throw new IllegalArgumentException("No organization is set for the parent relationship.");
		}
		OrganizationParentRelationship organizationParentRelationship = new OrganizationParentRelationship(
				organizationParentRelationshipVO.getOrganization().getId());
		organizationParentRelationship.setRelationshipType(organizationParentRelationshipVO.getRelationshipType());
		organizationParentRelationship.setAtType(organizationParentRelationshipVO.getAtType());
		organizationParentRelationship.setAtSchemaLocation(organizationParentRelationshipVO.getAtSchemaLocation());
		organizationParentRelationship.setAtBaseType(organizationParentRelationshipVO.getAtBaseType());
		organizationParentRelationship.setAtReferredType(
				organizationParentRelationshipVO.getOrganization().getAtReferredType());
		organizationParentRelationship.setName(organizationParentRelationshipVO.getOrganization().getName());
		if (organizationParentRelationshipVO.getOrganization().getHref() != null) {
			organizationParentRelationship.setHref(
					URI.create(organizationParentRelationshipVO.getOrganization().getHref()));
		}
		return organizationParentRelationship;
	}

	public OrganizationChildRelationship map(OrganizationChildRelationshipVO organizationChildRelationshipVO) {
		if (organizationChildRelationshipVO == null) {
			return null;
		}
		if (organizationChildRelationshipVO.getOrganization() == null) {
			throw new IllegalArgumentException("No organization is set for the child relationship.");
		}
		OrganizationChildRelationship organizationChildRelationship = new OrganizationChildRelationship(
				organizationChildRelationshipVO.getOrganization().getId());
		organizationChildRelationship.setRelationshipType(organizationChildRelationshipVO.getRelationshipType());
		organizationChildRelationship.setAtType(organizationChildRelationshipVO.getAtType());
		organizationChildRelationship.setAtSchemaLocation(organizationChildRelationshipVO.getAtSchemaLocation());
		organizationChildRelationship.setAtBaseType(organizationChildRelationshipVO.getAtBaseType());
		organizationChildRelationship.setName(organizationChildRelationshipVO.getOrganization().getName());
		organizationChildRelationship.setAtReferredType(
				organizationChildRelationshipVO.getOrganization().getAtReferredType());
		if (organizationChildRelationshipVO.getOrganization().getHref() != null) {
			organizationChildRelationship.setHref(
					URI.create(organizationChildRelationshipVO.getOrganization().getHref()));
		}
		return organizationChildRelationship;
	}

	public abstract TimePeriod map(TimePeriodVO value);

	public URL map(String value) {
		if (value == null) {
			return null;
		}
		try {
			return new URL(value);
		} catch (MalformedURLException e) {
			throw new MappingException(String.format("%s is not a URL.", value), e);
		}
	}

	public String map(URL value) {
		if (value == null) {
			return null;
		}
		return value.toString();
	}

	public URI mapToURI(String value) {
		if (value == null) {
			return null;
		}
		return URI.create(value);
	}

	public String mapFromURI(URI value) {
		if (value == null) {
			return null;
		}
		return value.toString();
	}

}


