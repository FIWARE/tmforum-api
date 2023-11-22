package org.fiware.resourcecatalog.model;

@jakarta.annotation.Generated("org.openapitools.codegen.languages.MicronautCodegen")
@io.micronaut.core.annotation.Introspected
public class PhysicalResourceSpecificationVO {

	public static final java.lang.String JSON_PROPERTY_ID = "id";
	public static final java.lang.String JSON_PROPERTY_HREF = "href";
	public static final java.lang.String JSON_PROPERTY_CATEGORY = "category";
	public static final java.lang.String JSON_PROPERTY_DESCRIPTION = "description";
	public static final java.lang.String JSON_PROPERTY_IS_BUNDLE = "isBundle";
	public static final java.lang.String JSON_PROPERTY_LAST_UPDATE = "lastUpdate";
	public static final java.lang.String JSON_PROPERTY_LIFECYCLE_STATUS = "lifecycleStatus";
	public static final java.lang.String JSON_PROPERTY_MODEL = "model";
	public static final java.lang.String JSON_PROPERTY_NAME = "name";
	public static final java.lang.String JSON_PROPERTY_PART = "part";
	public static final java.lang.String JSON_PROPERTY_SKU = "sku";
	public static final java.lang.String JSON_PROPERTY_VENDOR = "vendor";
	public static final java.lang.String JSON_PROPERTY_VERSION = "version";
	public static final java.lang.String JSON_PROPERTY_ATTACHMENT = "attachment";
	public static final java.lang.String JSON_PROPERTY_FEATURE_SPECIFICATION = "featureSpecification";
	public static final java.lang.String JSON_PROPERTY_RELATED_PARTY = "relatedParty";
	public static final java.lang.String JSON_PROPERTY_RESOURCE_SPEC_CHARACTERISTIC = "resourceSpecCharacteristic";
	public static final java.lang.String JSON_PROPERTY_RESOURCE_SPEC_RELATIONSHIP = "resourceSpecRelationship";
	public static final java.lang.String JSON_PROPERTY_TARGET_RESOURCE_SCHEMA = "targetResourceSchema";
	public static final java.lang.String JSON_PROPERTY_VALID_FOR = "validFor";
	public static final java.lang.String JSON_PROPERTY_AT_BASE_TYPE = "@baseType";
	public static final java.lang.String JSON_PROPERTY_AT_SCHEMA_LOCATION = "@schemaLocation";
	public static final java.lang.String JSON_PROPERTY_AT_TYPE = "@type";

	/** unique identifier */
	@com.fasterxml.jackson.annotation.JsonProperty(JSON_PROPERTY_ID)
	@com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
	private java.lang.String id;

	/** Hyperlink reference */
	@com.fasterxml.jackson.annotation.JsonProperty(JSON_PROPERTY_HREF)
	@com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
	private java.net.URI href;

	/** Category of the target resource like NetworkConnectivity, PhysicalLinks, Generic, L2Network and so on. */
	@com.fasterxml.jackson.annotation.JsonProperty(JSON_PROPERTY_CATEGORY)
	@com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
	private java.lang.String category;

	/** Description of this REST resource */
	@com.fasterxml.jackson.annotation.JsonProperty(JSON_PROPERTY_DESCRIPTION)
	@com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
	private java.lang.String description;

	/** A flag indicates that if this resource specification is a bundled specification (true) or single (false). */
	@com.fasterxml.jackson.annotation.JsonProperty(JSON_PROPERTY_IS_BUNDLE)
	@com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
	private java.lang.Boolean isBundle;

	/** Date and time of the last update of this REST resource */
	@com.fasterxml.jackson.annotation.JsonProperty(JSON_PROPERTY_LAST_UPDATE)
	@com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
	private java.time.Instant lastUpdate;

	/** Used to indicate the current lifecycle status of the resource specification */
	@com.fasterxml.jackson.annotation.JsonProperty(JSON_PROPERTY_LIFECYCLE_STATUS)
	@com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
	private java.lang.String lifecycleStatus;

	/** This is a string that represents a manufacturer-allocated number used to identify the general type and/or category of the hardware item. This, in combination with the Part and the Vendor, identify different types of hardware items. The SerialNumber can then be used to differentiate between different instances of the same type of hardware item. This is an optional attribute. */
	@com.fasterxml.jackson.annotation.JsonProperty(JSON_PROPERTY_MODEL)
	@com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
	private java.lang.String model;

	/** Name given to this REST resource */
	@com.fasterxml.jackson.annotation.JsonProperty(JSON_PROPERTY_NAME)
	@com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
	private java.lang.String name;

	/** This is a string that defines a manufacturer-allocated part number assigned by the organization that manufactures the hardware item. This, in combination with the Model and the Vendor, identify different types of hardware items. The SerialNumber can then be used to differentiate between different instances of the same type of hardware item. This is a REQUIRED attribute. */
	@com.fasterxml.jackson.annotation.JsonProperty(JSON_PROPERTY_PART)
	@com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
	private java.lang.String part;

	/** This is a string that defines the manufacturer-allocated Stock Keeping Unit (SKU) number of the hardware item. This is an optional attribute. */
	@com.fasterxml.jackson.annotation.JsonProperty(JSON_PROPERTY_SKU)
	@com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
	private java.lang.String sku;

	/** This is a string that defines the name of the manufacturer. This, in combination with the Model and the Part, identify different types of hardware items. The SerialNumber can then be used to differentiate between different instances of the same type of hardware item. This is a REQUIRED attribute for a physical resource. */
	@com.fasterxml.jackson.annotation.JsonProperty(JSON_PROPERTY_VENDOR)
	@com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
	private java.lang.String vendor;

	/** Resource Specification version */
	@com.fasterxml.jackson.annotation.JsonProperty(JSON_PROPERTY_VERSION)
	@com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
	private java.lang.String version;

	/** Complements the description of an element (for instance a resource) through video, pictures ... */
	@com.fasterxml.jackson.annotation.JsonProperty(JSON_PROPERTY_ATTACHMENT)
	@com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
	private java.util.List<AttachmentRefOrValueVO> attachment;

	/** A list of Features for this specification. */
	@com.fasterxml.jackson.annotation.JsonProperty(JSON_PROPERTY_FEATURE_SPECIFICATION)
	@com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
	private java.util.List<FeatureSpecificationVO> featureSpecification;

	/** A related party defines party or party role linked to a specific entity. */
	@com.fasterxml.jackson.annotation.JsonProperty(JSON_PROPERTY_RELATED_PARTY)
	@com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
	private java.util.List<RelatedPartyVO> relatedParty;

	/** A characteristic quality or distinctive feature of a ResourceSpecification.  The characteristic can be take on a discrete value, such as color, can take on a range of values, (for example, sensitivity of 100-240 mV), or can be derived from a formula (for example, usage time (hrs) &#x3D; 30 - talk time *3). Certain characteristics, such as color, may be configured during the ordering or some other process. */
	@com.fasterxml.jackson.annotation.JsonProperty(JSON_PROPERTY_RESOURCE_SPEC_CHARACTERISTIC)
	@com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
	private java.util.List<ResourceSpecificationCharacteristicVO> resourceSpecCharacteristic;

	/** A migration, substitution, dependency or exclusivity relationship between/among resource specifications. */
	@com.fasterxml.jackson.annotation.JsonProperty(JSON_PROPERTY_RESOURCE_SPEC_RELATIONSHIP)
	@com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
	private java.util.List<ResourceSpecificationRelationshipVO> resourceSpecRelationship;

	@com.fasterxml.jackson.annotation.JsonProperty(JSON_PROPERTY_TARGET_RESOURCE_SCHEMA)
	@com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
	private TargetResourceSchemaVO targetResourceSchema;

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
		PhysicalResourceSpecificationVO other = (PhysicalResourceSpecificationVO) object;
		return java.util.Objects.equals(id, other.id)
				&& java.util.Objects.equals(href, other.href)
				&& java.util.Objects.equals(category, other.category)
				&& java.util.Objects.equals(description, other.description)
				&& java.util.Objects.equals(isBundle, other.isBundle)
				&& java.util.Objects.equals(lastUpdate, other.lastUpdate)
				&& java.util.Objects.equals(lifecycleStatus, other.lifecycleStatus)
				&& java.util.Objects.equals(model, other.model)
				&& java.util.Objects.equals(name, other.name)
				&& java.util.Objects.equals(part, other.part)
				&& java.util.Objects.equals(sku, other.sku)
				&& java.util.Objects.equals(vendor, other.vendor)
				&& java.util.Objects.equals(version, other.version)
				&& java.util.Objects.equals(attachment, other.attachment)
				&& java.util.Objects.equals(featureSpecification, other.featureSpecification)
				&& java.util.Objects.equals(relatedParty, other.relatedParty)
				&& java.util.Objects.equals(resourceSpecCharacteristic, other.resourceSpecCharacteristic)
				&& java.util.Objects.equals(resourceSpecRelationship, other.resourceSpecRelationship)
				&& java.util.Objects.equals(targetResourceSchema, other.targetResourceSchema)
				&& java.util.Objects.equals(validFor, other.validFor)
				&& java.util.Objects.equals(atBaseType, other.atBaseType)
				&& java.util.Objects.equals(atSchemaLocation, other.atSchemaLocation)
				&& java.util.Objects.equals(atType, other.atType);
	}

	@Override
	public int hashCode() {
		return java.util.Objects.hash(id, href, category, description, isBundle, lastUpdate, lifecycleStatus, model, name, part, sku, vendor, version, attachment, featureSpecification, relatedParty, resourceSpecCharacteristic, resourceSpecRelationship, targetResourceSchema, validFor, atBaseType, atSchemaLocation, atType);
	}

	@Override
	public java.lang.String toString() {
		return new java.lang.StringBuilder()
				.append("PhysicalResourceSpecificationVO[")
				.append("id=").append(id).append(",")
				.append("href=").append(href).append(",")
				.append("category=").append(category).append(",")
				.append("description=").append(description).append(",")
				.append("isBundle=").append(isBundle).append(",")
				.append("lastUpdate=").append(lastUpdate).append(",")
				.append("lifecycleStatus=").append(lifecycleStatus).append(",")
				.append("model=").append(model).append(",")
				.append("name=").append(name).append(",")
				.append("part=").append(part).append(",")
				.append("sku=").append(sku).append(",")
				.append("vendor=").append(vendor).append(",")
				.append("version=").append(version).append(",")
				.append("attachment=").append(attachment).append(",")
				.append("featureSpecification=").append(featureSpecification).append(",")
				.append("relatedParty=").append(relatedParty).append(",")
				.append("resourceSpecCharacteristic=").append(resourceSpecCharacteristic).append(",")
				.append("resourceSpecRelationship=").append(resourceSpecRelationship).append(",")
				.append("targetResourceSchema=").append(targetResourceSchema).append(",")
				.append("validFor=").append(validFor).append(",")
				.append("atBaseType=").append(atBaseType).append(",")
				.append("atSchemaLocation=").append(atSchemaLocation).append(",")
				.append("atType=").append(atType)
				.append("]")
				.toString();
	}

	// fluent

	public PhysicalResourceSpecificationVO id(java.lang.String newId) {
		this.id = newId;
		return this;
	}

	public PhysicalResourceSpecificationVO href(java.net.URI newHref) {
		this.href = newHref;
		return this;
	}

	public PhysicalResourceSpecificationVO category(java.lang.String newCategory) {
		this.category = newCategory;
		return this;
	}

	public PhysicalResourceSpecificationVO description(java.lang.String newDescription) {
		this.description = newDescription;
		return this;
	}

	public PhysicalResourceSpecificationVO isBundle(java.lang.Boolean newIsBundle) {
		this.isBundle = newIsBundle;
		return this;
	}

	public PhysicalResourceSpecificationVO lastUpdate(java.time.Instant newLastUpdate) {
		this.lastUpdate = newLastUpdate;
		return this;
	}

	public PhysicalResourceSpecificationVO lifecycleStatus(java.lang.String newLifecycleStatus) {
		this.lifecycleStatus = newLifecycleStatus;
		return this;
	}

	public PhysicalResourceSpecificationVO model(java.lang.String newModel) {
		this.model = newModel;
		return this;
	}

	public PhysicalResourceSpecificationVO name(java.lang.String newName) {
		this.name = newName;
		return this;
	}

	public PhysicalResourceSpecificationVO part(java.lang.String newPart) {
		this.part = newPart;
		return this;
	}

	public PhysicalResourceSpecificationVO sku(java.lang.String newSku) {
		this.sku = newSku;
		return this;
	}

	public PhysicalResourceSpecificationVO vendor(java.lang.String newVendor) {
		this.vendor = newVendor;
		return this;
	}

	public PhysicalResourceSpecificationVO version(java.lang.String newVersion) {
		this.version = newVersion;
		return this;
	}

	public PhysicalResourceSpecificationVO attachment(java.util.List<AttachmentRefOrValueVO> newAttachment) {
		this.attachment = newAttachment;
		return this;
	}
	
	public PhysicalResourceSpecificationVO addAttachmentItem(AttachmentRefOrValueVO attachmentItem) {
		if (this.attachment == null) {
			this.attachment = new java.util.ArrayList<>();
		}
		this.attachment.add(attachmentItem);
		return this;
	}

	public PhysicalResourceSpecificationVO removeAttachmentItem(AttachmentRefOrValueVO attachmentItem) {
		if (this.attachment != null) {
			this.attachment.remove(attachmentItem);
		}
		return this;
	}

	public PhysicalResourceSpecificationVO featureSpecification(java.util.List<FeatureSpecificationVO> newFeatureSpecification) {
		this.featureSpecification = newFeatureSpecification;
		return this;
	}
	
	public PhysicalResourceSpecificationVO addFeatureSpecificationItem(FeatureSpecificationVO featureSpecificationItem) {
		if (this.featureSpecification == null) {
			this.featureSpecification = new java.util.ArrayList<>();
		}
		this.featureSpecification.add(featureSpecificationItem);
		return this;
	}

	public PhysicalResourceSpecificationVO removeFeatureSpecificationItem(FeatureSpecificationVO featureSpecificationItem) {
		if (this.featureSpecification != null) {
			this.featureSpecification.remove(featureSpecificationItem);
		}
		return this;
	}

	public PhysicalResourceSpecificationVO relatedParty(java.util.List<RelatedPartyVO> newRelatedParty) {
		this.relatedParty = newRelatedParty;
		return this;
	}
	
	public PhysicalResourceSpecificationVO addRelatedPartyItem(RelatedPartyVO relatedPartyItem) {
		if (this.relatedParty == null) {
			this.relatedParty = new java.util.ArrayList<>();
		}
		this.relatedParty.add(relatedPartyItem);
		return this;
	}

	public PhysicalResourceSpecificationVO removeRelatedPartyItem(RelatedPartyVO relatedPartyItem) {
		if (this.relatedParty != null) {
			this.relatedParty.remove(relatedPartyItem);
		}
		return this;
	}

	public PhysicalResourceSpecificationVO resourceSpecCharacteristic(java.util.List<ResourceSpecificationCharacteristicVO> newResourceSpecCharacteristic) {
		this.resourceSpecCharacteristic = newResourceSpecCharacteristic;
		return this;
	}
	
	public PhysicalResourceSpecificationVO addResourceSpecCharacteristicItem(ResourceSpecificationCharacteristicVO resourceSpecCharacteristicItem) {
		if (this.resourceSpecCharacteristic == null) {
			this.resourceSpecCharacteristic = new java.util.ArrayList<>();
		}
		this.resourceSpecCharacteristic.add(resourceSpecCharacteristicItem);
		return this;
	}

	public PhysicalResourceSpecificationVO removeResourceSpecCharacteristicItem(ResourceSpecificationCharacteristicVO resourceSpecCharacteristicItem) {
		if (this.resourceSpecCharacteristic != null) {
			this.resourceSpecCharacteristic.remove(resourceSpecCharacteristicItem);
		}
		return this;
	}

	public PhysicalResourceSpecificationVO resourceSpecRelationship(java.util.List<ResourceSpecificationRelationshipVO> newResourceSpecRelationship) {
		this.resourceSpecRelationship = newResourceSpecRelationship;
		return this;
	}
	
	public PhysicalResourceSpecificationVO addResourceSpecRelationshipItem(ResourceSpecificationRelationshipVO resourceSpecRelationshipItem) {
		if (this.resourceSpecRelationship == null) {
			this.resourceSpecRelationship = new java.util.ArrayList<>();
		}
		this.resourceSpecRelationship.add(resourceSpecRelationshipItem);
		return this;
	}

	public PhysicalResourceSpecificationVO removeResourceSpecRelationshipItem(ResourceSpecificationRelationshipVO resourceSpecRelationshipItem) {
		if (this.resourceSpecRelationship != null) {
			this.resourceSpecRelationship.remove(resourceSpecRelationshipItem);
		}
		return this;
	}

	public PhysicalResourceSpecificationVO targetResourceSchema(TargetResourceSchemaVO newTargetResourceSchema) {
		this.targetResourceSchema = newTargetResourceSchema;
		return this;
	}

	public PhysicalResourceSpecificationVO validFor(TimePeriodVO newValidFor) {
		this.validFor = newValidFor;
		return this;
	}

	public PhysicalResourceSpecificationVO atBaseType(java.lang.String newAtBaseType) {
		this.atBaseType = newAtBaseType;
		return this;
	}

	public PhysicalResourceSpecificationVO atSchemaLocation(java.net.URI newAtSchemaLocation) {
		this.atSchemaLocation = newAtSchemaLocation;
		return this;
	}

	public PhysicalResourceSpecificationVO atType(java.lang.String newAtType) {
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

	public java.lang.String getCategory() {
		return category;
	}

	public void setCategory(java.lang.String newCategory) {
		this.category = newCategory;
	}

	public java.lang.String getDescription() {
		return description;
	}

	public void setDescription(java.lang.String newDescription) {
		this.description = newDescription;
	}

	public java.lang.Boolean getIsBundle() {
		return isBundle;
	}

	public void setIsBundle(java.lang.Boolean newIsBundle) {
		this.isBundle = newIsBundle;
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

	public java.lang.String getModel() {
		return model;
	}

	public void setModel(java.lang.String newModel) {
		this.model = newModel;
	}

	public java.lang.String getName() {
		return name;
	}

	public void setName(java.lang.String newName) {
		this.name = newName;
	}

	public java.lang.String getPart() {
		return part;
	}

	public void setPart(java.lang.String newPart) {
		this.part = newPart;
	}

	public java.lang.String getSku() {
		return sku;
	}

	public void setSku(java.lang.String newSku) {
		this.sku = newSku;
	}

	public java.lang.String getVendor() {
		return vendor;
	}

	public void setVendor(java.lang.String newVendor) {
		this.vendor = newVendor;
	}

	public java.lang.String getVersion() {
		return version;
	}

	public void setVersion(java.lang.String newVersion) {
		this.version = newVersion;
	}

	public java.util.List<AttachmentRefOrValueVO> getAttachment() {
		return attachment;
	}

	public void setAttachment(java.util.List<AttachmentRefOrValueVO> newAttachment) {
		this.attachment = newAttachment;
	}

	public java.util.List<FeatureSpecificationVO> getFeatureSpecification() {
		return featureSpecification;
	}

	public void setFeatureSpecification(java.util.List<FeatureSpecificationVO> newFeatureSpecification) {
		this.featureSpecification = newFeatureSpecification;
	}

	public java.util.List<RelatedPartyVO> getRelatedParty() {
		return relatedParty;
	}

	public void setRelatedParty(java.util.List<RelatedPartyVO> newRelatedParty) {
		this.relatedParty = newRelatedParty;
	}

	public java.util.List<ResourceSpecificationCharacteristicVO> getResourceSpecCharacteristic() {
		return resourceSpecCharacteristic;
	}

	public void setResourceSpecCharacteristic(java.util.List<ResourceSpecificationCharacteristicVO> newResourceSpecCharacteristic) {
		this.resourceSpecCharacteristic = newResourceSpecCharacteristic;
	}

	public java.util.List<ResourceSpecificationRelationshipVO> getResourceSpecRelationship() {
		return resourceSpecRelationship;
	}

	public void setResourceSpecRelationship(java.util.List<ResourceSpecificationRelationshipVO> newResourceSpecRelationship) {
		this.resourceSpecRelationship = newResourceSpecRelationship;
	}

	public TargetResourceSchemaVO getTargetResourceSchema() {
		return targetResourceSchema;
	}

	public void setTargetResourceSchema(TargetResourceSchemaVO newTargetResourceSchema) {
		this.targetResourceSchema = newTargetResourceSchema;
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
