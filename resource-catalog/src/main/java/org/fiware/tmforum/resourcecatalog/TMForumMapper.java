package org.fiware.tmforum.resourcecatalog;

import io.github.wistefan.mapping.MappingException;
import org.fiware.resourcecatalog.model.APISpecificationVO;
import org.fiware.resourcecatalog.model.CharacteristicValueSpecificationVO;
import org.fiware.resourcecatalog.model.EventSubscriptionVO;
import org.fiware.resourcecatalog.model.FeatureSpecificationCharacteristicRelationshipVO;
import org.fiware.resourcecatalog.model.FeatureSpecificationCharacteristicVO;
import org.fiware.resourcecatalog.model.FeatureSpecificationRelationshipVO;
import org.fiware.resourcecatalog.model.FeatureSpecificationVO;
import org.fiware.resourcecatalog.model.HostingPlatformRequirementSpecificationVO;
import org.fiware.resourcecatalog.model.LogicalResourceSpecificationVO;
import org.fiware.resourcecatalog.model.PhysicalResourceSpecificationVO;
import org.fiware.resourcecatalog.model.ResourceCandidateCreateVO;
import org.fiware.resourcecatalog.model.ResourceCandidateUpdateVO;
import org.fiware.resourcecatalog.model.ResourceCandidateVO;
import org.fiware.resourcecatalog.model.ResourceCatalogCreateVO;
import org.fiware.resourcecatalog.model.ResourceCatalogUpdateVO;
import org.fiware.resourcecatalog.model.ResourceCatalogVO;
import org.fiware.resourcecatalog.model.ResourceCategoryCreateVO;
import org.fiware.resourcecatalog.model.ResourceCategoryUpdateVO;
import org.fiware.resourcecatalog.model.ResourceCategoryVO;
import org.fiware.resourcecatalog.model.ResourceSpecificationCreateVO;
import org.fiware.resourcecatalog.model.ResourceSpecificationUpdateVO;
import org.fiware.resourcecatalog.model.ResourceSpecificationVO;
import org.fiware.resourcecatalog.model.SoftwareResourceSpecificationVO;
import org.fiware.resourcecatalog.model.SoftwareSpecificationVO;
import org.fiware.resourcecatalog.model.SoftwareSupportPackageSpecificationVO;
import org.fiware.tmforum.common.domain.subscription.TMForumSubscription;
import org.fiware.tmforum.common.mapping.BaseMapper;
import org.fiware.tmforum.common.mapping.IdHelper;
import org.fiware.tmforum.resource.ApiSpecification;
import org.fiware.tmforum.resource.CharacteristicValue;
import org.fiware.tmforum.resource.FeatureSpecification;
import org.fiware.tmforum.resource.FeatureSpecificationCharacteristic;
import org.fiware.tmforum.resource.FeatureSpecificationCharacteristicRelationship;
import org.fiware.tmforum.resource.FeatureSpecificationRelationship;
import org.fiware.tmforum.resource.HostingPlatformRequirementSpecification;
import org.fiware.tmforum.resource.LogicalResourceSpecification;
import org.fiware.tmforum.resource.PhysicalResourceSpecification;
import org.fiware.tmforum.resource.ResourceCandidate;
import org.fiware.tmforum.resource.ResourceCategory;
import org.fiware.tmforum.resource.ResourceCategoryRef;
import org.fiware.tmforum.resource.ResourceSpecification;
import org.fiware.tmforum.resource.ResourceSpecificationRef;
import org.fiware.tmforum.resource.SoftwareResourceSpecification;
import org.fiware.tmforum.resource.SoftwareSpecification;
import org.fiware.tmforum.resource.SoftwareSupportPackageSpecification;
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

	@Mapping(target = "tmfId", source = "id")
	public abstract FeatureSpecification map(FeatureSpecificationVO featureSpecificationVO);

	@Mapping(target = "id", source = "tmfId")
	public abstract FeatureSpecificationVO map(FeatureSpecification featureSpecification);

	@Mapping(target = "tmfId", source = "id")
	public abstract FeatureSpecificationCharacteristic map(FeatureSpecificationCharacteristicVO featureSpecificationVO);

	@Mapping(target = "id", source = "tmfId")
	public abstract FeatureSpecificationCharacteristicVO map(FeatureSpecificationCharacteristic featureSpecification);

	@Mapping(target = "tmfId", source = "id")
	public abstract FeatureSpecificationCharacteristicRelationship map(FeatureSpecificationCharacteristicRelationshipVO featureSpecificationVO);

	@Mapping(target = "id", source = "tmfId")
	public abstract FeatureSpecificationCharacteristicRelationshipVO map(FeatureSpecificationCharacteristicRelationship featureSpecification);

	@Mapping(target = "tmfId", source = "id")
	public abstract FeatureSpecificationRelationship map(FeatureSpecificationRelationshipVO featureSpecificationVO);

	@Mapping(target = "id", source = "tmfId")
	public abstract FeatureSpecificationRelationshipVO map(FeatureSpecificationRelationship featureSpecification);

	@Mapping(target = "tmfValue", source = "value")
	public abstract CharacteristicValue map(CharacteristicValueSpecificationVO characteristicVO);

	@Mapping(target = "value", source = "tmfValue")
	public abstract CharacteristicValueSpecificationVO map(CharacteristicValue characteristic);

	// --- ResourceSpecification sub-type VO <-> domain mappings (for polymorphic dispatch) ---

	public abstract LogicalResourceSpecification map(LogicalResourceSpecificationVO vo);

	public abstract LogicalResourceSpecificationVO mapToLogicalResourceSpecificationVO(
			LogicalResourceSpecification entity);

	public abstract SoftwareResourceSpecification map(SoftwareResourceSpecificationVO vo);

	public abstract SoftwareResourceSpecificationVO mapToSoftwareResourceSpecificationVO(
			SoftwareResourceSpecification entity);

	public abstract ApiSpecification map(APISpecificationVO vo);

	public abstract APISpecificationVO mapToApiSpecificationVO(ApiSpecification entity);

	public abstract SoftwareSpecification map(SoftwareSpecificationVO vo);

	public abstract SoftwareSpecificationVO mapToSoftwareSpecificationVO(SoftwareSpecification entity);

	public abstract HostingPlatformRequirementSpecification map(HostingPlatformRequirementSpecificationVO vo);

	public abstract HostingPlatformRequirementSpecificationVO mapToHostingPlatformRequirementSpecificationVO(
			HostingPlatformRequirementSpecification entity);

	public abstract PhysicalResourceSpecification map(PhysicalResourceSpecificationVO vo);

	public abstract PhysicalResourceSpecificationVO mapToPhysicalResourceSpecificationVO(
			PhysicalResourceSpecification entity);

	public abstract SoftwareSupportPackageSpecification map(SoftwareSupportPackageSpecificationVO vo);

	public abstract SoftwareSupportPackageSpecificationVO mapToSoftwareSupportPackageSpecificationVO(
			SoftwareSupportPackageSpecification entity);

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


