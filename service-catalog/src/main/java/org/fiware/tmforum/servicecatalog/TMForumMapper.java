package org.fiware.tmforum.servicecatalog;

import io.github.wistefan.mapping.MappingException;
import org.fiware.servicecatalog.model.*;
import org.fiware.tmforum.common.domain.subscription.TMForumSubscription;
import org.fiware.tmforum.common.mapping.BaseMapper;
import org.fiware.tmforum.common.mapping.IdHelper;
import org.fiware.tmforum.resource.Feature;
import org.fiware.tmforum.resource.FeatureSpecificationCharacteristicRelationship;
import org.fiware.tmforum.resource.ResourceSpecificationRef;
import org.fiware.tmforum.service.*;
import org.fiware.tmforum.servicecatalog.domain.ServiceCatalog;
import org.fiware.tmforum.servicecatalog.domain.ServiceSpecification;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

/**
 * Mapper between the internal model and api-domain objects
 */
@Mapper(componentModel = "jsr330", uses = IdHelper.class)
public abstract class TMForumMapper extends BaseMapper {

	// service catalog

	@Mapping(target = "id", source = "id")
	@Mapping(target = "href", source = "id")
	public abstract ServiceCatalogVO map(ServiceCatalogCreateVO serviceCatalogCreateVO, URI id);

	public abstract ServiceCatalogVO map(ServiceCatalog serviceCatalog);

	public abstract ServiceCatalog map(ServiceCatalogVO serviceCatalogVO);

	@Mapping(target = "id", source = "id")
	public abstract ServiceCatalog map(ServiceCatalogUpdateVO serviceCatalogUpdateVO, String id);

	// service candidate

	@Mapping(target = "id", source = "id")
	@Mapping(target = "href", source = "id")
	public abstract ServiceCandidateVO map(ServiceCandidateCreateVO serviceCandidateCreateVO, URI id);

	public abstract ServiceCandidateVO map(ServiceCandidate serviceCandidate);

	public abstract ServiceCandidate map(ServiceCandidateVO serviceCandidateVO);

	@Mapping(target = "id", source = "id")
	public abstract ServiceCandidate map(ServiceCandidateUpdateVO serviceCandidateUpdateVO, String id);

	// service category

	@Mapping(target = "id", source = "id")
	@Mapping(target = "href", source = "id")
	public abstract ServiceCategoryVO map(ServiceCategoryCreateVO serviceCategoryCreateVO, URI id);

	public abstract ServiceCategoryVO map(ServiceCategory serviceCategory);

	public abstract ServiceCategory map(ServiceCategoryVO serviceCandidateVO);

	@Mapping(target = "id", source = "id")
	public abstract ServiceCategory map(ServiceCategoryUpdateVO serviceCategoryUpdateVO, String id);

	// service specification

	@Mapping(target = "id", source = "id")
	@Mapping(target = "href", source = "id")
	public abstract ServiceSpecificationVO map(ServiceSpecificationCreateVO serviceSpecificationCreateVO, URI id);

	public abstract ServiceSpecificationVO map(ServiceSpecification serviceSpecification);

	public abstract ServiceSpecification map(ServiceSpecificationVO serviceSpecificationVO);

	@Mapping(target = "id", source = "id")
	public abstract ServiceSpecification map(ServiceSpecificationUpdateVO serviceSpecificationUpdateVO, String id);

	@Mapping(target = "query", source = "rawQuery")
	public abstract EventSubscriptionVO map(TMForumSubscription subscription);

	@Mapping(target = "id", source = "tmfId")
	public abstract FeatureSpecificationVO map(FeatureSpecification feature);

	@Mapping(target = "tmfId", source = "id")
	public abstract FeatureSpecification map(FeatureSpecificationVO featureVO);

	@Mapping(target = "id", source = "tmfId")
	public abstract FeatureSpecificationCharacteristicVO map(FeatureSpecificationCharacteristic feature);

	@Mapping(target = "tmfId", source = "id")
	public abstract FeatureSpecificationCharacteristic map(FeatureSpecificationCharacteristicVO featureVO);
	
	@Mapping(target = "id", source = "tmfId")
	public abstract CharacteristicSpecificationVO map(CharacteristicSpecification characteristicSpecification);

	@Mapping(target = "tmfId", source = "id")
	public abstract CharacteristicSpecification map(CharacteristicSpecificationVO characteristicSpecificationVO);

	@Mapping(target = "tmfValue", source = "value")
	public abstract CharacteristicValueSpecification map(CharacteristicValueSpecificationVO characteristicVO);

	@Mapping(target = "value", source = "tmfValue")
	public abstract CharacteristicValueSpecificationVO map(CharacteristicValueSpecification characteristic);

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

	public ServiceCategoryRef mapFromServiceCategoryRefId(String id) {
		if (id == null) {
			return null;
		}
		return new ServiceCategoryRef(URI.create(id));
	}

	public ServiceSpecificationRelationship mapFromServiceSpecificationRelationshipId(String id) {
		if (id == null) {
			return null;
		}
		return new ServiceSpecificationRelationship(URI.create(id));
	}

	public String mapFromServiceSpecificationRelationship(ServiceSpecificationRelationship serviceSpecificationRelationship) {
		if (serviceSpecificationRelationship == null) {
			return null;
		}
		return serviceSpecificationRelationship.getEntityId().toString();
	}

	public String mapFromServiceCategoryRef(ServiceCategoryRef serviceCategoryRef) {
		if (serviceCategoryRef == null) {
			return null;
		}
		return serviceCategoryRef.getEntityId().toString();
	}

	public String mapFromResourceSpecificationRef(ResourceSpecificationRef resourceSpecificationRef) {
		if (resourceSpecificationRef == null) {
			return null;
		}
		return resourceSpecificationRef.getEntityId().toString();
	}

	public <C> URI mapGeneric(C value) {
		if (value == null) {
			return null;
		}
		if (value instanceof URI uri) {
			return uri;
		} else if (value instanceof String string) {
			try {
				return new URI(string);
			} catch (URISyntaxException e) {
				throw new MappingException(String.format("String %s is not an URI.", string), e);
			}
		}
		throw new MappingException("Value is not a URI.");
	}
}


