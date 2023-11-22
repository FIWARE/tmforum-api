package org.fiware.resourcecatalog.model;

@jakarta.annotation.Generated("org.openapitools.codegen.languages.MicronautCodegen")
@io.micronaut.core.annotation.Introspected
public class ResourceSpecificationRelationshipVO {

	public static final java.lang.String JSON_PROPERTY_ID = "id";
	public static final java.lang.String JSON_PROPERTY_HREF = "href";
	public static final java.lang.String JSON_PROPERTY_DEFAULT_QUANTITY = "defaultQuantity";
	public static final java.lang.String JSON_PROPERTY_MAXIMUM_QUANTITY = "maximumQuantity";
	public static final java.lang.String JSON_PROPERTY_MINIMUM_QUANTITY = "minimumQuantity";
	public static final java.lang.String JSON_PROPERTY_NAME = "name";
	public static final java.lang.String JSON_PROPERTY_RELATIONSHIP_TYPE = "relationshipType";
	public static final java.lang.String JSON_PROPERTY_ROLE = "role";
	public static final java.lang.String JSON_PROPERTY_CHARACTERISTIC = "characteristic";
	public static final java.lang.String JSON_PROPERTY_VALID_FOR = "validFor";
	public static final java.lang.String JSON_PROPERTY_AT_BASE_TYPE = "@baseType";
	public static final java.lang.String JSON_PROPERTY_AT_SCHEMA_LOCATION = "@schemaLocation";
	public static final java.lang.String JSON_PROPERTY_AT_TYPE = "@type";

	/** Unique identifier of target ResourceSpecification */
	@com.fasterxml.jackson.annotation.JsonProperty(JSON_PROPERTY_ID)
	@com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
	private java.lang.String id;

	/** Reference of the target ResourceSpecification */
	@com.fasterxml.jackson.annotation.JsonProperty(JSON_PROPERTY_HREF)
	@com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
	private java.net.URI href;

	/** The default number of the related resource that should be instantiated, for example a rack would typically have 4 cards, although it could support more. */
	@com.fasterxml.jackson.annotation.JsonProperty(JSON_PROPERTY_DEFAULT_QUANTITY)
	@com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
	private java.lang.Integer defaultQuantity;

	/** The maximum number of the related resource that should be instantiated, for example a rack supports a maximum of 16 cards */
	@com.fasterxml.jackson.annotation.JsonProperty(JSON_PROPERTY_MAXIMUM_QUANTITY)
	@com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
	private java.lang.Integer maximumQuantity;

	/** The minimum number of the related resource that should be instantiated, for example a rack must have at least 1 card */
	@com.fasterxml.jackson.annotation.JsonProperty(JSON_PROPERTY_MINIMUM_QUANTITY)
	@com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
	private java.lang.Integer minimumQuantity;

	/** The name given to the target resource specification instance */
	@com.fasterxml.jackson.annotation.JsonProperty(JSON_PROPERTY_NAME)
	@com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
	private java.lang.String name;

	/** Type of relationship such as migration, substitution, dependency, exclusivity */
	@com.fasterxml.jackson.annotation.JsonProperty(JSON_PROPERTY_RELATIONSHIP_TYPE)
	@com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
	private java.lang.String relationshipType;

	/** The association role for this resource specification */
	@com.fasterxml.jackson.annotation.JsonProperty(JSON_PROPERTY_ROLE)
	@com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
	private java.lang.String role;

	/** A characteristic that refines the relationship. For example, consider the relationship between a slot and a card. For a half-height card it is important to know the position at which the card is inserted, so a characteristic Position might be defined on the relationship to allow capturing of this in the inventory */
	@com.fasterxml.jackson.annotation.JsonProperty(JSON_PROPERTY_CHARACTERISTIC)
	@com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
	private java.util.List<ResourceSpecificationCharacteristicVO> characteristic;

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
		ResourceSpecificationRelationshipVO other = (ResourceSpecificationRelationshipVO) object;
		return java.util.Objects.equals(id, other.id)
				&& java.util.Objects.equals(href, other.href)
				&& java.util.Objects.equals(defaultQuantity, other.defaultQuantity)
				&& java.util.Objects.equals(maximumQuantity, other.maximumQuantity)
				&& java.util.Objects.equals(minimumQuantity, other.minimumQuantity)
				&& java.util.Objects.equals(name, other.name)
				&& java.util.Objects.equals(relationshipType, other.relationshipType)
				&& java.util.Objects.equals(role, other.role)
				&& java.util.Objects.equals(characteristic, other.characteristic)
				&& java.util.Objects.equals(validFor, other.validFor)
				&& java.util.Objects.equals(atBaseType, other.atBaseType)
				&& java.util.Objects.equals(atSchemaLocation, other.atSchemaLocation)
				&& java.util.Objects.equals(atType, other.atType);
	}

	@Override
	public int hashCode() {
		return java.util.Objects.hash(id, href, defaultQuantity, maximumQuantity, minimumQuantity, name, relationshipType, role, characteristic, validFor, atBaseType, atSchemaLocation, atType);
	}

	@Override
	public java.lang.String toString() {
		return new java.lang.StringBuilder()
				.append("ResourceSpecificationRelationshipVO[")
				.append("id=").append(id).append(",")
				.append("href=").append(href).append(",")
				.append("defaultQuantity=").append(defaultQuantity).append(",")
				.append("maximumQuantity=").append(maximumQuantity).append(",")
				.append("minimumQuantity=").append(minimumQuantity).append(",")
				.append("name=").append(name).append(",")
				.append("relationshipType=").append(relationshipType).append(",")
				.append("role=").append(role).append(",")
				.append("characteristic=").append(characteristic).append(",")
				.append("validFor=").append(validFor).append(",")
				.append("atBaseType=").append(atBaseType).append(",")
				.append("atSchemaLocation=").append(atSchemaLocation).append(",")
				.append("atType=").append(atType)
				.append("]")
				.toString();
	}

	// fluent

	public ResourceSpecificationRelationshipVO id(java.lang.String newId) {
		this.id = newId;
		return this;
	}

	public ResourceSpecificationRelationshipVO href(java.net.URI newHref) {
		this.href = newHref;
		return this;
	}

	public ResourceSpecificationRelationshipVO defaultQuantity(java.lang.Integer newDefaultQuantity) {
		this.defaultQuantity = newDefaultQuantity;
		return this;
	}

	public ResourceSpecificationRelationshipVO maximumQuantity(java.lang.Integer newMaximumQuantity) {
		this.maximumQuantity = newMaximumQuantity;
		return this;
	}

	public ResourceSpecificationRelationshipVO minimumQuantity(java.lang.Integer newMinimumQuantity) {
		this.minimumQuantity = newMinimumQuantity;
		return this;
	}

	public ResourceSpecificationRelationshipVO name(java.lang.String newName) {
		this.name = newName;
		return this;
	}

	public ResourceSpecificationRelationshipVO relationshipType(java.lang.String newRelationshipType) {
		this.relationshipType = newRelationshipType;
		return this;
	}

	public ResourceSpecificationRelationshipVO role(java.lang.String newRole) {
		this.role = newRole;
		return this;
	}

	public ResourceSpecificationRelationshipVO characteristic(java.util.List<ResourceSpecificationCharacteristicVO> newCharacteristic) {
		this.characteristic = newCharacteristic;
		return this;
	}
	
	public ResourceSpecificationRelationshipVO addCharacteristicItem(ResourceSpecificationCharacteristicVO characteristicItem) {
		if (this.characteristic == null) {
			this.characteristic = new java.util.ArrayList<>();
		}
		this.characteristic.add(characteristicItem);
		return this;
	}

	public ResourceSpecificationRelationshipVO removeCharacteristicItem(ResourceSpecificationCharacteristicVO characteristicItem) {
		if (this.characteristic != null) {
			this.characteristic.remove(characteristicItem);
		}
		return this;
	}

	public ResourceSpecificationRelationshipVO validFor(TimePeriodVO newValidFor) {
		this.validFor = newValidFor;
		return this;
	}

	public ResourceSpecificationRelationshipVO atBaseType(java.lang.String newAtBaseType) {
		this.atBaseType = newAtBaseType;
		return this;
	}

	public ResourceSpecificationRelationshipVO atSchemaLocation(java.net.URI newAtSchemaLocation) {
		this.atSchemaLocation = newAtSchemaLocation;
		return this;
	}

	public ResourceSpecificationRelationshipVO atType(java.lang.String newAtType) {
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

	public java.lang.Integer getDefaultQuantity() {
		return defaultQuantity;
	}

	public void setDefaultQuantity(java.lang.Integer newDefaultQuantity) {
		this.defaultQuantity = newDefaultQuantity;
	}

	public java.lang.Integer getMaximumQuantity() {
		return maximumQuantity;
	}

	public void setMaximumQuantity(java.lang.Integer newMaximumQuantity) {
		this.maximumQuantity = newMaximumQuantity;
	}

	public java.lang.Integer getMinimumQuantity() {
		return minimumQuantity;
	}

	public void setMinimumQuantity(java.lang.Integer newMinimumQuantity) {
		this.minimumQuantity = newMinimumQuantity;
	}

	public java.lang.String getName() {
		return name;
	}

	public void setName(java.lang.String newName) {
		this.name = newName;
	}

	public java.lang.String getRelationshipType() {
		return relationshipType;
	}

	public void setRelationshipType(java.lang.String newRelationshipType) {
		this.relationshipType = newRelationshipType;
	}

	public java.lang.String getRole() {
		return role;
	}

	public void setRole(java.lang.String newRole) {
		this.role = newRole;
	}

	public java.util.List<ResourceSpecificationCharacteristicVO> getCharacteristic() {
		return characteristic;
	}

	public void setCharacteristic(java.util.List<ResourceSpecificationCharacteristicVO> newCharacteristic) {
		this.characteristic = newCharacteristic;
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
