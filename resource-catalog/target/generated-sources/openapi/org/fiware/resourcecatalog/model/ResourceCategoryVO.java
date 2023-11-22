package org.fiware.resourcecatalog.model;

@jakarta.annotation.Generated("org.openapitools.codegen.languages.MicronautCodegen")
@io.micronaut.core.annotation.Introspected
public class ResourceCategoryVO {

	public static final java.lang.String JSON_PROPERTY_ID = "id";
	public static final java.lang.String JSON_PROPERTY_HREF = "href";
	public static final java.lang.String JSON_PROPERTY_DESCRIPTION = "description";
	public static final java.lang.String JSON_PROPERTY_IS_ROOT = "isRoot";
	public static final java.lang.String JSON_PROPERTY_LAST_UPDATE = "lastUpdate";
	public static final java.lang.String JSON_PROPERTY_LIFECYCLE_STATUS = "lifecycleStatus";
	public static final java.lang.String JSON_PROPERTY_NAME = "name";
	public static final java.lang.String JSON_PROPERTY_PARENT_ID = "parentId";
	public static final java.lang.String JSON_PROPERTY_VERSION = "version";
	public static final java.lang.String JSON_PROPERTY_CATEGORY = "category";
	public static final java.lang.String JSON_PROPERTY_RELATED_PARTY = "relatedParty";
	public static final java.lang.String JSON_PROPERTY_RESOURCE_CANDIDATE = "resourceCandidate";
	public static final java.lang.String JSON_PROPERTY_VALID_FOR = "validFor";
	public static final java.lang.String JSON_PROPERTY_AT_BASE_TYPE = "@baseType";
	public static final java.lang.String JSON_PROPERTY_AT_SCHEMA_LOCATION = "@schemaLocation";
	public static final java.lang.String JSON_PROPERTY_AT_TYPE = "@type";

	/** Unique identifier of the category */
	@com.fasterxml.jackson.annotation.JsonProperty(JSON_PROPERTY_ID)
	@com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
	private java.lang.String id;

	/** Hyperlink reference to the category */
	@com.fasterxml.jackson.annotation.JsonProperty(JSON_PROPERTY_HREF)
	@com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
	private java.net.URI href;

	/** Description of the category */
	@com.fasterxml.jackson.annotation.JsonProperty(JSON_PROPERTY_DESCRIPTION)
	@com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
	private java.lang.String description;

	/** If true, this Boolean indicates that the category is a root of categories */
	@com.fasterxml.jackson.annotation.JsonProperty(JSON_PROPERTY_IS_ROOT)
	@com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
	private java.lang.Boolean isRoot;

	/** Date and time of the last update */
	@com.fasterxml.jackson.annotation.JsonProperty(JSON_PROPERTY_LAST_UPDATE)
	@com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
	private java.time.Instant lastUpdate;

	/** Used to indicate the current lifecycle status */
	@com.fasterxml.jackson.annotation.JsonProperty(JSON_PROPERTY_LIFECYCLE_STATUS)
	@com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
	private java.lang.String lifecycleStatus;

	/** Name of the category */
	@com.fasterxml.jackson.annotation.JsonProperty(JSON_PROPERTY_NAME)
	@com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
	private java.lang.String name;

	/** Unique identifier of the parent category */
	@com.fasterxml.jackson.annotation.JsonProperty(JSON_PROPERTY_PARENT_ID)
	@com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
	private java.lang.String parentId;

	/** Category version */
	@com.fasterxml.jackson.annotation.JsonProperty(JSON_PROPERTY_VERSION)
	@com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
	private java.lang.String version;

	/** The category resource is used to group product offerings, service and resource candidates in logical containers. Categories can contain other (sub-)categories and/or product offerings. */
	@com.fasterxml.jackson.annotation.JsonProperty(JSON_PROPERTY_CATEGORY)
	@com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
	private java.util.List<ResourceCategoryRefVO> category;

	/** List of parties involved in this category */
	@com.fasterxml.jackson.annotation.JsonProperty(JSON_PROPERTY_RELATED_PARTY)
	@com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
	private java.util.List<RelatedPartyVO> relatedParty;

	/** List of resource candidates accessible via this category */
	@com.fasterxml.jackson.annotation.JsonProperty(JSON_PROPERTY_RESOURCE_CANDIDATE)
	@com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
	private java.util.List<ResourceCandidateRefVO> resourceCandidate;

	@com.fasterxml.jackson.annotation.JsonProperty(JSON_PROPERTY_VALID_FOR)
	@com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
	private TimePeriodVO validFor;

	/** When sub-classing, this defines the super-class */
	@com.fasterxml.jackson.annotation.JsonProperty(JSON_PROPERTY_AT_BASE_TYPE)
	@com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
	private java.lang.String atBaseType;

	/** A URI to a JSON-Schema file that defines additional attributes and relationships */
	@com.fasterxml.jackson.annotation.JsonProperty(JSON_PROPERTY_AT_SCHEMA_LOCATION)
	@com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
	private java.net.URI atSchemaLocation;

	/** When sub-classing, this defines the sub-class Extensible name */
	@com.fasterxml.jackson.annotation.JsonProperty(JSON_PROPERTY_AT_TYPE)
	@com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
	private java.lang.String atType;

	// methods

	@Override
	public boolean equals(Object object) {
		if (object == this) {
			return true;
		}
		if (object == null || getClass() != object.getClass()) {
			return false;
		}
		ResourceCategoryVO other = (ResourceCategoryVO) object;
		return java.util.Objects.equals(id, other.id)
				&& java.util.Objects.equals(href, other.href)
				&& java.util.Objects.equals(description, other.description)
				&& java.util.Objects.equals(isRoot, other.isRoot)
				&& java.util.Objects.equals(lastUpdate, other.lastUpdate)
				&& java.util.Objects.equals(lifecycleStatus, other.lifecycleStatus)
				&& java.util.Objects.equals(name, other.name)
				&& java.util.Objects.equals(parentId, other.parentId)
				&& java.util.Objects.equals(version, other.version)
				&& java.util.Objects.equals(category, other.category)
				&& java.util.Objects.equals(relatedParty, other.relatedParty)
				&& java.util.Objects.equals(resourceCandidate, other.resourceCandidate)
				&& java.util.Objects.equals(validFor, other.validFor)
				&& java.util.Objects.equals(atBaseType, other.atBaseType)
				&& java.util.Objects.equals(atSchemaLocation, other.atSchemaLocation)
				&& java.util.Objects.equals(atType, other.atType);
	}

	@Override
	public int hashCode() {
		return java.util.Objects.hash(id, href, description, isRoot, lastUpdate, lifecycleStatus, name, parentId, version, category, relatedParty, resourceCandidate, validFor, atBaseType, atSchemaLocation, atType);
	}

	@Override
	public java.lang.String toString() {
		return new java.lang.StringBuilder()
				.append("ResourceCategoryVO[")
				.append("id=").append(id).append(",")
				.append("href=").append(href).append(",")
				.append("description=").append(description).append(",")
				.append("isRoot=").append(isRoot).append(",")
				.append("lastUpdate=").append(lastUpdate).append(",")
				.append("lifecycleStatus=").append(lifecycleStatus).append(",")
				.append("name=").append(name).append(",")
				.append("parentId=").append(parentId).append(",")
				.append("version=").append(version).append(",")
				.append("category=").append(category).append(",")
				.append("relatedParty=").append(relatedParty).append(",")
				.append("resourceCandidate=").append(resourceCandidate).append(",")
				.append("validFor=").append(validFor).append(",")
				.append("atBaseType=").append(atBaseType).append(",")
				.append("atSchemaLocation=").append(atSchemaLocation).append(",")
				.append("atType=").append(atType)
				.append("]")
				.toString();
	}

	// fluent

	public ResourceCategoryVO id(java.lang.String newId) {
		this.id = newId;
		return this;
	}

	public ResourceCategoryVO href(java.net.URI newHref) {
		this.href = newHref;
		return this;
	}

	public ResourceCategoryVO description(java.lang.String newDescription) {
		this.description = newDescription;
		return this;
	}

	public ResourceCategoryVO isRoot(java.lang.Boolean newIsRoot) {
		this.isRoot = newIsRoot;
		return this;
	}

	public ResourceCategoryVO lastUpdate(java.time.Instant newLastUpdate) {
		this.lastUpdate = newLastUpdate;
		return this;
	}

	public ResourceCategoryVO lifecycleStatus(java.lang.String newLifecycleStatus) {
		this.lifecycleStatus = newLifecycleStatus;
		return this;
	}

	public ResourceCategoryVO name(java.lang.String newName) {
		this.name = newName;
		return this;
	}

	public ResourceCategoryVO parentId(java.lang.String newParentId) {
		this.parentId = newParentId;
		return this;
	}

	public ResourceCategoryVO version(java.lang.String newVersion) {
		this.version = newVersion;
		return this;
	}

	public ResourceCategoryVO category(java.util.List<ResourceCategoryRefVO> newCategory) {
		this.category = newCategory;
		return this;
	}
	
	public ResourceCategoryVO addCategoryItem(ResourceCategoryRefVO categoryItem) {
		if (this.category == null) {
			this.category = new java.util.ArrayList<>();
		}
		this.category.add(categoryItem);
		return this;
	}

	public ResourceCategoryVO removeCategoryItem(ResourceCategoryRefVO categoryItem) {
		if (this.category != null) {
			this.category.remove(categoryItem);
		}
		return this;
	}

	public ResourceCategoryVO relatedParty(java.util.List<RelatedPartyVO> newRelatedParty) {
		this.relatedParty = newRelatedParty;
		return this;
	}
	
	public ResourceCategoryVO addRelatedPartyItem(RelatedPartyVO relatedPartyItem) {
		if (this.relatedParty == null) {
			this.relatedParty = new java.util.ArrayList<>();
		}
		this.relatedParty.add(relatedPartyItem);
		return this;
	}

	public ResourceCategoryVO removeRelatedPartyItem(RelatedPartyVO relatedPartyItem) {
		if (this.relatedParty != null) {
			this.relatedParty.remove(relatedPartyItem);
		}
		return this;
	}

	public ResourceCategoryVO resourceCandidate(java.util.List<ResourceCandidateRefVO> newResourceCandidate) {
		this.resourceCandidate = newResourceCandidate;
		return this;
	}
	
	public ResourceCategoryVO addResourceCandidateItem(ResourceCandidateRefVO resourceCandidateItem) {
		if (this.resourceCandidate == null) {
			this.resourceCandidate = new java.util.ArrayList<>();
		}
		this.resourceCandidate.add(resourceCandidateItem);
		return this;
	}

	public ResourceCategoryVO removeResourceCandidateItem(ResourceCandidateRefVO resourceCandidateItem) {
		if (this.resourceCandidate != null) {
			this.resourceCandidate.remove(resourceCandidateItem);
		}
		return this;
	}

	public ResourceCategoryVO validFor(TimePeriodVO newValidFor) {
		this.validFor = newValidFor;
		return this;
	}

	public ResourceCategoryVO atBaseType(java.lang.String newAtBaseType) {
		this.atBaseType = newAtBaseType;
		return this;
	}

	public ResourceCategoryVO atSchemaLocation(java.net.URI newAtSchemaLocation) {
		this.atSchemaLocation = newAtSchemaLocation;
		return this;
	}

	public ResourceCategoryVO atType(java.lang.String newAtType) {
		this.atType = newAtType;
		return this;
	}

	// getter/setter

	public java.lang.String getId() {
		return id;
	}

	public void setId(java.lang.String newId) {
		this.id = newId;
	}

	public java.net.URI getHref() {
		return href;
	}

	public void setHref(java.net.URI newHref) {
		this.href = newHref;
	}

	public java.lang.String getDescription() {
		return description;
	}

	public void setDescription(java.lang.String newDescription) {
		this.description = newDescription;
	}

	public java.lang.Boolean getIsRoot() {
		return isRoot;
	}

	public void setIsRoot(java.lang.Boolean newIsRoot) {
		this.isRoot = newIsRoot;
	}

	public java.time.Instant getLastUpdate() {
		return lastUpdate;
	}

	public void setLastUpdate(java.time.Instant newLastUpdate) {
		this.lastUpdate = newLastUpdate;
	}

	public java.lang.String getLifecycleStatus() {
		return lifecycleStatus;
	}

	public void setLifecycleStatus(java.lang.String newLifecycleStatus) {
		this.lifecycleStatus = newLifecycleStatus;
	}

	public java.lang.String getName() {
		return name;
	}

	public void setName(java.lang.String newName) {
		this.name = newName;
	}

	public java.lang.String getParentId() {
		return parentId;
	}

	public void setParentId(java.lang.String newParentId) {
		this.parentId = newParentId;
	}

	public java.lang.String getVersion() {
		return version;
	}

	public void setVersion(java.lang.String newVersion) {
		this.version = newVersion;
	}

	public java.util.List<ResourceCategoryRefVO> getCategory() {
		return category;
	}

	public void setCategory(java.util.List<ResourceCategoryRefVO> newCategory) {
		this.category = newCategory;
	}

	public java.util.List<RelatedPartyVO> getRelatedParty() {
		return relatedParty;
	}

	public void setRelatedParty(java.util.List<RelatedPartyVO> newRelatedParty) {
		this.relatedParty = newRelatedParty;
	}

	public java.util.List<ResourceCandidateRefVO> getResourceCandidate() {
		return resourceCandidate;
	}

	public void setResourceCandidate(java.util.List<ResourceCandidateRefVO> newResourceCandidate) {
		this.resourceCandidate = newResourceCandidate;
	}

	public TimePeriodVO getValidFor() {
		return validFor;
	}

	public void setValidFor(TimePeriodVO newValidFor) {
		this.validFor = newValidFor;
	}

	public java.lang.String getAtBaseType() {
		return atBaseType;
	}

	public void setAtBaseType(java.lang.String newAtBaseType) {
		this.atBaseType = newAtBaseType;
	}

	public java.net.URI getAtSchemaLocation() {
		return atSchemaLocation;
	}

	public void setAtSchemaLocation(java.net.URI newAtSchemaLocation) {
		this.atSchemaLocation = newAtSchemaLocation;
	}

	public java.lang.String getAtType() {
		return atType;
	}

	public void setAtType(java.lang.String newAtType) {
		this.atType = newAtType;
	}
}
