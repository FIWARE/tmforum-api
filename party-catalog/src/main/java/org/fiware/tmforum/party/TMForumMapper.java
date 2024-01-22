package org.fiware.tmforum.party;

import io.github.wistefan.mapping.MappingException;
import org.fiware.party.model.*;
import org.fiware.tmforum.common.domain.TimePeriod;
import org.fiware.tmforum.common.domain.subscription.TMForumSubscription;
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
public interface TMForumMapper {

	// using inline expression, since else it might overwrite the String-String mapping
	@Mapping(target = "id", source = "id")
	@Mapping(target = "href", source = "id")
	OrganizationVO map(OrganizationCreateVO organizationCreateVO, URI id);

	OrganizationVO map(Organization organization);

	@Mapping(target = "href", source = "id")
	Organization map(OrganizationVO organizationVO);

	@Mapping(target = "id", source = "id")
	Organization map(OrganizationUpdateVO organizationUpdateVO, String id);

	@Mapping(target = "id", source = "id")
	@Mapping(target = "href", source = "id")
	IndividualVO map(IndividualCreateVO individualCreateVO, URI id);

	@Mapping(target = "id", source = "id")
	Individual map(IndividualUpdateVO individualUpdateVO, String id);

	IndividualVO map(Individual individual);

	@Mapping(target = "href", source = "id")
	Individual map(IndividualVO individualVO);

	@Mapping(target = "isFavouriteLanguage", source = "favouriteLanguage")
	LanguageAbilityVO map(LanguageAbility languageAbility);

	@Mapping(target = "query", source = "rawQuery")
	EventSubscriptionVO map(TMForumSubscription subscription);

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
		if(organizationParentRelationship.getHref() != null) {
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
		if(organizationChildRelationship.getHref() != null) {
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

	default OrganizationParentRelationship map(OrganizationParentRelationshipVO organizationParentRelationshipVO) {
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

	default OrganizationChildRelationship map(OrganizationChildRelationshipVO organizationChildRelationshipVO) {
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


