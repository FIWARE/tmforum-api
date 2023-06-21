package org.fiware.tmforum.party;

import org.fiware.party.model.IndividualCreateVO;
import org.fiware.party.model.IndividualUpdateVO;
import org.fiware.party.model.IndividualVO;
import org.fiware.party.model.LanguageAbilityVO;
import org.fiware.party.model.OrganizationChildRelationshipVO;
import org.fiware.party.model.OrganizationCreateVO;
import org.fiware.party.model.OrganizationParentRelationshipVO;
import org.fiware.party.model.OrganizationRefVO;
import org.fiware.party.model.OrganizationUpdateVO;
import org.fiware.party.model.OrganizationVO;
import org.fiware.party.model.TimePeriodVO;
import org.fiware.tmforum.common.mapping.IdHelper;
import io.github.wistefan.mapping.MappingException;
import org.fiware.tmforum.common.domain.TimePeriod;
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
	Organization map(OrganizationUpdateVO organizationUpdateVO, String id);

	@Mapping(target = "id", source = "id")
	@Mapping(target = "href", source = "id")
	IndividualVO map(IndividualCreateVO individualCreateVO, URI id);

	@Mapping(target = "id", source = "id")
	Individual map(IndividualUpdateVO individualUpdateVO, String id);

	@Mapping(target = "status", source = "individualState")
	IndividualVO map(Individual individual);

	@Mapping(target = "individualState", source = "status")
	@Mapping(target = "href", source = "id")
	Individual map(IndividualVO individualVO);

	@Mapping(target = "isFavouriteLanguage", source = "favouriteLanguage")
	LanguageAbilityVO map(LanguageAbility languageAbility);

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
		organizationRefVO.setId(organizationParentRelationship.getEntityId().toString());
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

	default OrganizationChildRelationshipVO map(OrganizationChildRelationship organizationChildRelationship) {
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

	default OrganizationParentRelationship map(OrganizationParentRelationshipVO organizationParentRelationshipVO) {
		if (organizationParentRelationshipVO == null) {
			return null;
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
		return organizationParentRelationship;
	}

	default OrganizationChildRelationship map(OrganizationChildRelationshipVO organizationChildRelationshipVO) {
		if (organizationChildRelationshipVO == null) {
			return null;
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


