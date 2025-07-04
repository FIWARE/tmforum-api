package org.fiware.tmforum.resourcecatalog;

import io.github.wistefan.mapping.MappingException;
import org.fiware.resourcecatalog.model.*;
import org.fiware.tmforum.common.domain.Money;
import org.fiware.tmforum.common.domain.subscription.TMForumSubscription;
import org.fiware.tmforum.common.mapping.BaseMapper;
import org.fiware.tmforum.common.mapping.IdHelper;
import org.fiware.tmforum.resource.*;
import org.fiware.tmforum.resourcecatalog.domain.ResourceCatalog;
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

	// resource catalog

	@Mapping(target = "id", source = "id")
	@Mapping(target = "href", source = "id")
	public abstract ResourceCatalogVO map(ResourceCatalogCreateVO resourceCatalogCreateVO, URI id);

	public abstract ResourceCatalogVO map(ResourceCatalog resourceCatalog);

	public abstract ResourceCatalog map(ResourceCatalogVO resourceCatalogVO);

	@Mapping(target = "id", source = "id")
	public abstract ResourceCatalog map(ResourceCatalogUpdateVO resourceCatalogUpdateVO, String id);

	// resource specification

	@Mapping(target = "id", source = "id")
	@Mapping(target = "href", source = "id")
	public abstract ResourceSpecificationVO map(ResourceSpecificationCreateVO resourceSpecificationCreateVO, URI id);

	public abstract ResourceSpecificationVO map(ResourceSpecification resourceSpecification);

	public abstract ResourceSpecification map(ResourceSpecificationVO resourceCandidateVO);

	@Mapping(target = "id", source = "id")
	public abstract ResourceSpecification map(ResourceSpecificationUpdateVO resourceSpecificationUpdateVO, String id);

	// resource catalog

	@Mapping(target = "id", source = "id")
	@Mapping(target = "href", source = "id")
	public abstract ResourceCandidateVO map(ResourceCandidateCreateVO resourceCandidateCreateVO, URI id);

	public abstract ResourceCandidateVO map(ResourceCandidate resourceCandidate);

	public abstract ResourceCandidate map(ResourceCandidateVO resourceCandidateVO);

	@Mapping(target = "id", source = "id")
	public abstract ResourceCandidate map(ResourceCandidateUpdateVO resourceCandidateUpdateVO, String id);

	// resource catalog

	@Mapping(target = "id", source = "id")
	@Mapping(target = "href", source = "id")
	public abstract ResourceCategoryVO map(ResourceCategoryCreateVO resourceCategoryCreateVO, URI id);

	public abstract ResourceCategoryVO map(ResourceCategory resourceCategory);

	public abstract ResourceCategory map(ResourceCategoryVO resourceCategoryVO);

	@Mapping(target = "id", source = "id")
	public abstract ResourceCategory map(ResourceCategoryUpdateVO resourceCategoryUpdateVO, String id);

	@Mapping(target = "query", source = "rawQuery")
	public abstract EventSubscriptionVO map(TMForumSubscription subscription);

	@Mapping(target = "specId", source = "id")
	public abstract FeatureSpecification map(FeatureSpecificationVO featureSpecificationVO);

	@Mapping(target = "id", source = "specId")
	public abstract FeatureSpecificationVO map(FeatureSpecification featureSpecification);

	@Mapping(target = "charValue", source = "value")
	public abstract CharacteristicValue map(CharacteristicValueSpecificationVO characteristicVO);

	@Mapping(target = "value", source = "charValue")
	public abstract CharacteristicValueSpecificationVO map(CharacteristicValue characteristic);

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

	public ResourceSpecificationRef mapFromResourceSpecId(String id) {
		if (id == null) {
			return null;
		}
		return new ResourceSpecificationRef(id);
	}

	public ResourceCategoryRef mapFromCategoryId(String id) {
		if (id == null) {
			return null;
		}
		return new ResourceCategoryRef(URI.create(id));
	}

	public String mapFromCategoryRef(ResourceCategoryRef categoryRef) {
		if (categoryRef == null) {
			return null;
		}
		return categoryRef.getEntityId().toString();
	}

	public String mapFromResourceSpecificationRef(ResourceSpecificationRef resourceSpecificationRef) {
		if (resourceSpecificationRef == null) {
			return null;
		}
		return resourceSpecificationRef.getEntityId().toString();
	}
}


