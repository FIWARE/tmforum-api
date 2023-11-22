package org.fiware.servicecatalog.model;

@jakarta.annotation.Generated("org.openapitools.codegen.languages.MicronautCodegen")
@io.micronaut.core.annotation.Introspected
public class ServiceSpecificationVO {

	public static final java.lang.String JSON_PROPERTY_ID = "id";
	public static final java.lang.String JSON_PROPERTY_HREF = "href";
	public static final java.lang.String JSON_PROPERTY_DESCRIPTION = "description";
	public static final java.lang.String JSON_PROPERTY_IS_BUNDLE = "isBundle";
	public static final java.lang.String JSON_PROPERTY_LAST_UPDATE = "lastUpdate";
	public static final java.lang.String JSON_PROPERTY_LIFECYCLE_STATUS = "lifecycleStatus";
	public static final java.lang.String JSON_PROPERTY_NAME = "name";
	public static final java.lang.String JSON_PROPERTY_VERSION = "version";
	public static final java.lang.String JSON_PROPERTY_ATTACHMENT = "attachment";
	public static final java.lang.String JSON_PROPERTY_CONSTRAINT = "constraint";
	public static final java.lang.String JSON_PROPERTY_ENTITY_SPEC_RELATIONSHIP = "entitySpecRelationship";
	public static final java.lang.String JSON_PROPERTY_FEATURE_SPECIFICATION = "featureSpecification";
	public static final java.lang.String JSON_PROPERTY_RELATED_PARTY = "relatedParty";
	public static final java.lang.String JSON_PROPERTY_RESOURCE_SPECIFICATION = "resourceSpecification";
	public static final java.lang.String JSON_PROPERTY_SERVICE_LEVEL_SPECIFICATION = "serviceLevelSpecification";
	public static final java.lang.String JSON_PROPERTY_SERVICE_SPEC_RELATIONSHIP = "serviceSpecRelationship";
	public static final java.lang.String JSON_PROPERTY_SPEC_CHARACTERISTIC = "specCharacteristic";
	public static final java.lang.String JSON_PROPERTY_TARGET_ENTITY_SCHEMA = "targetEntitySchema";
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

	/** Description of the specification */
	@com.fasterxml.jackson.annotation.JsonProperty(JSON_PROPERTY_DESCRIPTION)
	@com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
	private java.lang.String description;

	/** isBundle determines whether specification represents a single specification (false), or a bundle of specifications (true). */
	@com.fasterxml.jackson.annotation.JsonProperty(JSON_PROPERTY_IS_BUNDLE)
	@com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
	private java.lang.Boolean isBundle;

	/** Date and time of the last update of the specification */
	@com.fasterxml.jackson.annotation.JsonProperty(JSON_PROPERTY_LAST_UPDATE)
	@com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
	private java.time.Instant lastUpdate;

	/** Used to indicate the current lifecycle status of this catalog item */
	@com.fasterxml.jackson.annotation.JsonProperty(JSON_PROPERTY_LIFECYCLE_STATUS)
	@com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
	private java.lang.String lifecycleStatus;

	/** Name given to the specification */
	@com.fasterxml.jackson.annotation.JsonProperty(JSON_PROPERTY_NAME)
	@com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
	private java.lang.String name;

	/** specification version */
	@com.fasterxml.jackson.annotation.JsonProperty(JSON_PROPERTY_VERSION)
	@com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
	private java.lang.String version;

	/** Attachments that may be of relevance to this specification, such as picture, document, media */
	@com.fasterxml.jackson.annotation.JsonProperty(JSON_PROPERTY_ATTACHMENT)
	@com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
	private java.util.List<AttachmentRefOrValueVO> attachment;

	/** This is a list of constraint references applied to this specification */
	@com.fasterxml.jackson.annotation.JsonProperty(JSON_PROPERTY_CONSTRAINT)
	@com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
	private java.util.List<ConstraintRefVO> constraint;

	/** Relationship to another specification */
	@com.fasterxml.jackson.annotation.JsonProperty(JSON_PROPERTY_ENTITY_SPEC_RELATIONSHIP)
	@com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
	private java.util.List<EntitySpecificationRelationshipVO> entitySpecRelationship;

	/** A list of Features for this specification. */
	@com.fasterxml.jackson.annotation.JsonProperty(JSON_PROPERTY_FEATURE_SPECIFICATION)
	@com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
	private java.util.List<FeatureSpecificationVO> featureSpecification;

	/** Parties who manage or otherwise have an interest in this specification */
	@com.fasterxml.jackson.annotation.JsonProperty(JSON_PROPERTY_RELATED_PARTY)
	@com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
	private java.util.List<RelatedPartyVO> relatedParty;

	/** A list of resource specification references (ResourceSpecificationRef [*]). The ResourceSpecification is required for a service specification with type ResourceFacingServiceSpecification (RFSS). */
	@com.fasterxml.jackson.annotation.JsonProperty(JSON_PROPERTY_RESOURCE_SPECIFICATION)
	@com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
	private java.util.List<ResourceSpecificationRefVO> resourceSpecification;

	/** A list of service level specifications related to this service specification, and which will need to be satisifiable for corresponding service instances; e.g. Gold, Platinum */
	@com.fasterxml.jackson.annotation.JsonProperty(JSON_PROPERTY_SERVICE_LEVEL_SPECIFICATION)
	@com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
	private java.util.List<ServiceLevelSpecificationRefVO> serviceLevelSpecification;

	/** A list of service specifications related to this specification, e.g. migration, substitution, dependency or exclusivity relationship */
	@com.fasterxml.jackson.annotation.JsonProperty(JSON_PROPERTY_SERVICE_SPEC_RELATIONSHIP)
	@com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
	private java.util.List<ServiceSpecRelationshipVO> serviceSpecRelationship;

	/** List of characteristics that the entity can take */
	@com.fasterxml.jackson.annotation.JsonProperty(JSON_PROPERTY_SPEC_CHARACTERISTIC)
	@com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
	private java.util.List<CharacteristicSpecificationVO> specCharacteristic;

	@com.fasterxml.jackson.annotation.JsonProperty(JSON_PROPERTY_TARGET_ENTITY_SCHEMA)
	@com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
	private TargetEntitySchemaVO targetEntitySchema;

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
		ServiceSpecificationVO other = (ServiceSpecificationVO) object;
		return java.util.Objects.equals(id, other.id)
				&& java.util.Objects.equals(href, other.href)
				&& java.util.Objects.equals(description, other.description)
				&& java.util.Objects.equals(isBundle, other.isBundle)
				&& java.util.Objects.equals(lastUpdate, other.lastUpdate)
				&& java.util.Objects.equals(lifecycleStatus, other.lifecycleStatus)
				&& java.util.Objects.equals(name, other.name)
				&& java.util.Objects.equals(version, other.version)
				&& java.util.Objects.equals(attachment, other.attachment)
				&& java.util.Objects.equals(constraint, other.constraint)
				&& java.util.Objects.equals(entitySpecRelationship, other.entitySpecRelationship)
				&& java.util.Objects.equals(featureSpecification, other.featureSpecification)
				&& java.util.Objects.equals(relatedParty, other.relatedParty)
				&& java.util.Objects.equals(resourceSpecification, other.resourceSpecification)
				&& java.util.Objects.equals(serviceLevelSpecification, other.serviceLevelSpecification)
				&& java.util.Objects.equals(serviceSpecRelationship, other.serviceSpecRelationship)
				&& java.util.Objects.equals(specCharacteristic, other.specCharacteristic)
				&& java.util.Objects.equals(targetEntitySchema, other.targetEntitySchema)
				&& java.util.Objects.equals(validFor, other.validFor)
				&& java.util.Objects.equals(atBaseType, other.atBaseType)
				&& java.util.Objects.equals(atSchemaLocation, other.atSchemaLocation)
				&& java.util.Objects.equals(atType, other.atType);
	}

	@Override
	public int hashCode() {
		return java.util.Objects.hash(id, href, description, isBundle, lastUpdate, lifecycleStatus, name, version, attachment, constraint, entitySpecRelationship, featureSpecification, relatedParty, resourceSpecification, serviceLevelSpecification, serviceSpecRelationship, specCharacteristic, targetEntitySchema, validFor, atBaseType, atSchemaLocation, atType);
	}

	@Override
	public java.lang.String toString() {
		return new java.lang.StringBuilder()
				.append("ServiceSpecificationVO[")
				.append("id=").append(id).append(",")
				.append("href=").append(href).append(",")
				.append("description=").append(description).append(",")
				.append("isBundle=").append(isBundle).append(",")
				.append("lastUpdate=").append(lastUpdate).append(",")
				.append("lifecycleStatus=").append(lifecycleStatus).append(",")
				.append("name=").append(name).append(",")
				.append("version=").append(version).append(",")
				.append("attachment=").append(attachment).append(",")
				.append("constraint=").append(constraint).append(",")
				.append("entitySpecRelationship=").append(entitySpecRelationship).append(",")
				.append("featureSpecification=").append(featureSpecification).append(",")
				.append("relatedParty=").append(relatedParty).append(",")
				.append("resourceSpecification=").append(resourceSpecification).append(",")
				.append("serviceLevelSpecification=").append(serviceLevelSpecification).append(",")
				.append("serviceSpecRelationship=").append(serviceSpecRelationship).append(",")
				.append("specCharacteristic=").append(specCharacteristic).append(",")
				.append("targetEntitySchema=").append(targetEntitySchema).append(",")
				.append("validFor=").append(validFor).append(",")
				.append("atBaseType=").append(atBaseType).append(",")
				.append("atSchemaLocation=").append(atSchemaLocation).append(",")
				.append("atType=").append(atType)
				.append("]")
				.toString();
	}

	// fluent

	public ServiceSpecificationVO id(java.lang.String newId) {
		this.id = newId;
		return this;
	}

	public ServiceSpecificationVO href(java.net.URI newHref) {
		this.href = newHref;
		return this;
	}

	public ServiceSpecificationVO description(java.lang.String newDescription) {
		this.description = newDescription;
		return this;
	}

	public ServiceSpecificationVO isBundle(java.lang.Boolean newIsBundle) {
		this.isBundle = newIsBundle;
		return this;
	}

	public ServiceSpecificationVO lastUpdate(java.time.Instant newLastUpdate) {
		this.lastUpdate = newLastUpdate;
		return this;
	}

	public ServiceSpecificationVO lifecycleStatus(java.lang.String newLifecycleStatus) {
		this.lifecycleStatus = newLifecycleStatus;
		return this;
	}

	public ServiceSpecificationVO name(java.lang.String newName) {
		this.name = newName;
		return this;
	}

	public ServiceSpecificationVO version(java.lang.String newVersion) {
		this.version = newVersion;
		return this;
	}

	public ServiceSpecificationVO attachment(java.util.List<AttachmentRefOrValueVO> newAttachment) {
		this.attachment = newAttachment;
		return this;
	}
	
	public ServiceSpecificationVO addAttachmentItem(AttachmentRefOrValueVO attachmentItem) {
		if (this.attachment == null) {
			this.attachment = new java.util.ArrayList<>();
		}
		this.attachment.add(attachmentItem);
		return this;
	}

	public ServiceSpecificationVO removeAttachmentItem(AttachmentRefOrValueVO attachmentItem) {
		if (this.attachment != null) {
			this.attachment.remove(attachmentItem);
		}
		return this;
	}

	public ServiceSpecificationVO constraint(java.util.List<ConstraintRefVO> newConstraint) {
		this.constraint = newConstraint;
		return this;
	}
	
	public ServiceSpecificationVO addConstraintItem(ConstraintRefVO constraintItem) {
		if (this.constraint == null) {
			this.constraint = new java.util.ArrayList<>();
		}
		this.constraint.add(constraintItem);
		return this;
	}

	public ServiceSpecificationVO removeConstraintItem(ConstraintRefVO constraintItem) {
		if (this.constraint != null) {
			this.constraint.remove(constraintItem);
		}
		return this;
	}

	public ServiceSpecificationVO entitySpecRelationship(java.util.List<EntitySpecificationRelationshipVO> newEntitySpecRelationship) {
		this.entitySpecRelationship = newEntitySpecRelationship;
		return this;
	}
	
	public ServiceSpecificationVO addEntitySpecRelationshipItem(EntitySpecificationRelationshipVO entitySpecRelationshipItem) {
		if (this.entitySpecRelationship == null) {
			this.entitySpecRelationship = new java.util.ArrayList<>();
		}
		this.entitySpecRelationship.add(entitySpecRelationshipItem);
		return this;
	}

	public ServiceSpecificationVO removeEntitySpecRelationshipItem(EntitySpecificationRelationshipVO entitySpecRelationshipItem) {
		if (this.entitySpecRelationship != null) {
			this.entitySpecRelationship.remove(entitySpecRelationshipItem);
		}
		return this;
	}

	public ServiceSpecificationVO featureSpecification(java.util.List<FeatureSpecificationVO> newFeatureSpecification) {
		this.featureSpecification = newFeatureSpecification;
		return this;
	}
	
	public ServiceSpecificationVO addFeatureSpecificationItem(FeatureSpecificationVO featureSpecificationItem) {
		if (this.featureSpecification == null) {
			this.featureSpecification = new java.util.ArrayList<>();
		}
		this.featureSpecification.add(featureSpecificationItem);
		return this;
	}

	public ServiceSpecificationVO removeFeatureSpecificationItem(FeatureSpecificationVO featureSpecificationItem) {
		if (this.featureSpecification != null) {
			this.featureSpecification.remove(featureSpecificationItem);
		}
		return this;
	}

	public ServiceSpecificationVO relatedParty(java.util.List<RelatedPartyVO> newRelatedParty) {
		this.relatedParty = newRelatedParty;
		return this;
	}
	
	public ServiceSpecificationVO addRelatedPartyItem(RelatedPartyVO relatedPartyItem) {
		if (this.relatedParty == null) {
			this.relatedParty = new java.util.ArrayList<>();
		}
		this.relatedParty.add(relatedPartyItem);
		return this;
	}

	public ServiceSpecificationVO removeRelatedPartyItem(RelatedPartyVO relatedPartyItem) {
		if (this.relatedParty != null) {
			this.relatedParty.remove(relatedPartyItem);
		}
		return this;
	}

	public ServiceSpecificationVO resourceSpecification(java.util.List<ResourceSpecificationRefVO> newResourceSpecification) {
		this.resourceSpecification = newResourceSpecification;
		return this;
	}
	
	public ServiceSpecificationVO addResourceSpecificationItem(ResourceSpecificationRefVO resourceSpecificationItem) {
		if (this.resourceSpecification == null) {
			this.resourceSpecification = new java.util.ArrayList<>();
		}
		this.resourceSpecification.add(resourceSpecificationItem);
		return this;
	}

	public ServiceSpecificationVO removeResourceSpecificationItem(ResourceSpecificationRefVO resourceSpecificationItem) {
		if (this.resourceSpecification != null) {
			this.resourceSpecification.remove(resourceSpecificationItem);
		}
		return this;
	}

	public ServiceSpecificationVO serviceLevelSpecification(java.util.List<ServiceLevelSpecificationRefVO> newServiceLevelSpecification) {
		this.serviceLevelSpecification = newServiceLevelSpecification;
		return this;
	}
	
	public ServiceSpecificationVO addServiceLevelSpecificationItem(ServiceLevelSpecificationRefVO serviceLevelSpecificationItem) {
		if (this.serviceLevelSpecification == null) {
			this.serviceLevelSpecification = new java.util.ArrayList<>();
		}
		this.serviceLevelSpecification.add(serviceLevelSpecificationItem);
		return this;
	}

	public ServiceSpecificationVO removeServiceLevelSpecificationItem(ServiceLevelSpecificationRefVO serviceLevelSpecificationItem) {
		if (this.serviceLevelSpecification != null) {
			this.serviceLevelSpecification.remove(serviceLevelSpecificationItem);
		}
		return this;
	}

	public ServiceSpecificationVO serviceSpecRelationship(java.util.List<ServiceSpecRelationshipVO> newServiceSpecRelationship) {
		this.serviceSpecRelationship = newServiceSpecRelationship;
		return this;
	}
	
	public ServiceSpecificationVO addServiceSpecRelationshipItem(ServiceSpecRelationshipVO serviceSpecRelationshipItem) {
		if (this.serviceSpecRelationship == null) {
			this.serviceSpecRelationship = new java.util.ArrayList<>();
		}
		this.serviceSpecRelationship.add(serviceSpecRelationshipItem);
		return this;
	}

	public ServiceSpecificationVO removeServiceSpecRelationshipItem(ServiceSpecRelationshipVO serviceSpecRelationshipItem) {
		if (this.serviceSpecRelationship != null) {
			this.serviceSpecRelationship.remove(serviceSpecRelationshipItem);
		}
		return this;
	}

	public ServiceSpecificationVO specCharacteristic(java.util.List<CharacteristicSpecificationVO> newSpecCharacteristic) {
		this.specCharacteristic = newSpecCharacteristic;
		return this;
	}
	
	public ServiceSpecificationVO addSpecCharacteristicItem(CharacteristicSpecificationVO specCharacteristicItem) {
		if (this.specCharacteristic == null) {
			this.specCharacteristic = new java.util.ArrayList<>();
		}
		this.specCharacteristic.add(specCharacteristicItem);
		return this;
	}

	public ServiceSpecificationVO removeSpecCharacteristicItem(CharacteristicSpecificationVO specCharacteristicItem) {
		if (this.specCharacteristic != null) {
			this.specCharacteristic.remove(specCharacteristicItem);
		}
		return this;
	}

	public ServiceSpecificationVO targetEntitySchema(TargetEntitySchemaVO newTargetEntitySchema) {
		this.targetEntitySchema = newTargetEntitySchema;
		return this;
	}

	public ServiceSpecificationVO validFor(TimePeriodVO newValidFor) {
		this.validFor = newValidFor;
		return this;
	}

	public ServiceSpecificationVO atBaseType(java.lang.String newAtBaseType) {
		this.atBaseType = newAtBaseType;
		return this;
	}

	public ServiceSpecificationVO atSchemaLocation(java.net.URI newAtSchemaLocation) {
		this.atSchemaLocation = newAtSchemaLocation;
		return this;
	}

	public ServiceSpecificationVO atType(java.lang.String newAtType) {
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

	public java.lang.String getName() {
		return name;
	}

	public void setName(java.lang.String newName) {
		this.name = newName;
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

	public java.util.List<ConstraintRefVO> getConstraint() {
		return constraint;
	}

	public void setConstraint(java.util.List<ConstraintRefVO> newConstraint) {
		this.constraint = newConstraint;
	}

	public java.util.List<EntitySpecificationRelationshipVO> getEntitySpecRelationship() {
		return entitySpecRelationship;
	}

	public void setEntitySpecRelationship(java.util.List<EntitySpecificationRelationshipVO> newEntitySpecRelationship) {
		this.entitySpecRelationship = newEntitySpecRelationship;
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

	public java.util.List<ResourceSpecificationRefVO> getResourceSpecification() {
		return resourceSpecification;
	}

	public void setResourceSpecification(java.util.List<ResourceSpecificationRefVO> newResourceSpecification) {
		this.resourceSpecification = newResourceSpecification;
	}

	public java.util.List<ServiceLevelSpecificationRefVO> getServiceLevelSpecification() {
		return serviceLevelSpecification;
	}

	public void setServiceLevelSpecification(java.util.List<ServiceLevelSpecificationRefVO> newServiceLevelSpecification) {
		this.serviceLevelSpecification = newServiceLevelSpecification;
	}

	public java.util.List<ServiceSpecRelationshipVO> getServiceSpecRelationship() {
		return serviceSpecRelationship;
	}

	public void setServiceSpecRelationship(java.util.List<ServiceSpecRelationshipVO> newServiceSpecRelationship) {
		this.serviceSpecRelationship = newServiceSpecRelationship;
	}

	public java.util.List<CharacteristicSpecificationVO> getSpecCharacteristic() {
		return specCharacteristic;
	}

	public void setSpecCharacteristic(java.util.List<CharacteristicSpecificationVO> newSpecCharacteristic) {
		this.specCharacteristic = newSpecCharacteristic;
	}

	public TargetEntitySchemaVO getTargetEntitySchema() {
		return targetEntitySchema;
	}

	public void setTargetEntitySchema(TargetEntitySchemaVO newTargetEntitySchema) {
		this.targetEntitySchema = newTargetEntitySchema;
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
