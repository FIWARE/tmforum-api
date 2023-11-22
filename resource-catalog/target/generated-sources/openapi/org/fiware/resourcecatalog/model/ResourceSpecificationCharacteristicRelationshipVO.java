package org.fiware.resourcecatalog.model;

@jakarta.annotation.Generated("org.openapitools.codegen.languages.MicronautCodegen")
@io.micronaut.core.annotation.Introspected
public class ResourceSpecificationCharacteristicRelationshipVO {

	public static final java.lang.String JSON_PROPERTY_CHARACTERISTIC_SPECIFICATION_ID = "characteristicSpecificationId";
	public static final java.lang.String JSON_PROPERTY_NAME = "name";
	public static final java.lang.String JSON_PROPERTY_RELATIONSHIP_TYPE = "relationshipType";
	public static final java.lang.String JSON_PROPERTY_RESOURCE_SPECIFICATION_HREF = "resourceSpecificationHref";
	public static final java.lang.String JSON_PROPERTY_RESOURCE_SPECIFICATION_ID = "resourceSpecificationId";
	public static final java.lang.String JSON_PROPERTY_VALID_FOR = "validFor";
	public static final java.lang.String JSON_PROPERTY_AT_BASE_TYPE = "@baseType";
	public static final java.lang.String JSON_PROPERTY_AT_SCHEMA_LOCATION = "@schemaLocation";
	public static final java.lang.String JSON_PROPERTY_AT_TYPE = "@type";

	/** Unique identifier of the characteristic within the specification */
	@com.fasterxml.jackson.annotation.JsonProperty(JSON_PROPERTY_CHARACTERISTIC_SPECIFICATION_ID)
	@com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
	private java.lang.String characteristicSpecificationId;

	/** Name of the target characteristic within the specification */
	@com.fasterxml.jackson.annotation.JsonProperty(JSON_PROPERTY_NAME)
	@com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
	private java.lang.String name;

	/** Type of relationship such as aggregation, migration, substitution, dependency, exclusivity */
	@com.fasterxml.jackson.annotation.JsonProperty(JSON_PROPERTY_RELATIONSHIP_TYPE)
	@com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
	private java.lang.String relationshipType;

	/** Hyperlink reference to the resource specification containing the target characteristic */
	@com.fasterxml.jackson.annotation.JsonProperty(JSON_PROPERTY_RESOURCE_SPECIFICATION_HREF)
	@com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
	private java.net.URI resourceSpecificationHref;

	/** Unique identifier of the resource specification containing the target characteristic */
	@com.fasterxml.jackson.annotation.JsonProperty(JSON_PROPERTY_RESOURCE_SPECIFICATION_ID)
	@com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
	private java.lang.String resourceSpecificationId;

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
		ResourceSpecificationCharacteristicRelationshipVO other = (ResourceSpecificationCharacteristicRelationshipVO) object;
		return java.util.Objects.equals(characteristicSpecificationId, other.characteristicSpecificationId)
				&& java.util.Objects.equals(name, other.name)
				&& java.util.Objects.equals(relationshipType, other.relationshipType)
				&& java.util.Objects.equals(resourceSpecificationHref, other.resourceSpecificationHref)
				&& java.util.Objects.equals(resourceSpecificationId, other.resourceSpecificationId)
				&& java.util.Objects.equals(validFor, other.validFor)
				&& java.util.Objects.equals(atBaseType, other.atBaseType)
				&& java.util.Objects.equals(atSchemaLocation, other.atSchemaLocation)
				&& java.util.Objects.equals(atType, other.atType);
	}

	@Override
	public int hashCode() {
		return java.util.Objects.hash(characteristicSpecificationId, name, relationshipType, resourceSpecificationHref, resourceSpecificationId, validFor, atBaseType, atSchemaLocation, atType);
	}

	@Override
	public java.lang.String toString() {
		return new java.lang.StringBuilder()
				.append("ResourceSpecificationCharacteristicRelationshipVO[")
				.append("characteristicSpecificationId=").append(characteristicSpecificationId).append(",")
				.append("name=").append(name).append(",")
				.append("relationshipType=").append(relationshipType).append(",")
				.append("resourceSpecificationHref=").append(resourceSpecificationHref).append(",")
				.append("resourceSpecificationId=").append(resourceSpecificationId).append(",")
				.append("validFor=").append(validFor).append(",")
				.append("atBaseType=").append(atBaseType).append(",")
				.append("atSchemaLocation=").append(atSchemaLocation).append(",")
				.append("atType=").append(atType)
				.append("]")
				.toString();
	}

	// fluent

	public ResourceSpecificationCharacteristicRelationshipVO characteristicSpecificationId(java.lang.String newCharacteristicSpecificationId) {
		this.characteristicSpecificationId = newCharacteristicSpecificationId;
		return this;
	}

	public ResourceSpecificationCharacteristicRelationshipVO name(java.lang.String newName) {
		this.name = newName;
		return this;
	}

	public ResourceSpecificationCharacteristicRelationshipVO relationshipType(java.lang.String newRelationshipType) {
		this.relationshipType = newRelationshipType;
		return this;
	}

	public ResourceSpecificationCharacteristicRelationshipVO resourceSpecificationHref(java.net.URI newResourceSpecificationHref) {
		this.resourceSpecificationHref = newResourceSpecificationHref;
		return this;
	}

	public ResourceSpecificationCharacteristicRelationshipVO resourceSpecificationId(java.lang.String newResourceSpecificationId) {
		this.resourceSpecificationId = newResourceSpecificationId;
		return this;
	}

	public ResourceSpecificationCharacteristicRelationshipVO validFor(TimePeriodVO newValidFor) {
		this.validFor = newValidFor;
		return this;
	}

	public ResourceSpecificationCharacteristicRelationshipVO atBaseType(java.lang.String newAtBaseType) {
		this.atBaseType = newAtBaseType;
		return this;
	}

	public ResourceSpecificationCharacteristicRelationshipVO atSchemaLocation(java.net.URI newAtSchemaLocation) {
		this.atSchemaLocation = newAtSchemaLocation;
		return this;
	}

	public ResourceSpecificationCharacteristicRelationshipVO atType(java.lang.String newAtType) {
		this.atType = newAtType;
		return this;
	}

	// getter/setter

	public java.lang.String getCharacteristicSpecificationId() {
		return characteristicSpecificationId;
	}

	public void setCharacteristicSpecificationId(java.lang.String newCharacteristicSpecificationId) {
		this.characteristicSpecificationId = newCharacteristicSpecificationId;
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

	public java.net.URI getResourceSpecificationHref() {
		return resourceSpecificationHref;
	}

	public void setResourceSpecificationHref(java.net.URI newResourceSpecificationHref) {
		this.resourceSpecificationHref = newResourceSpecificationHref;
	}

	public java.lang.String getResourceSpecificationId() {
		return resourceSpecificationId;
	}

	public void setResourceSpecificationId(java.lang.String newResourceSpecificationId) {
		this.resourceSpecificationId = newResourceSpecificationId;
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
