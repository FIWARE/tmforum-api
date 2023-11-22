package org.fiware.servicecatalog.model;

@jakarta.annotation.Generated("org.openapitools.codegen.languages.MicronautCodegen")
@io.micronaut.core.annotation.Introspected
public class FeatureSpecificationCharacteristicRelationshipVO {

	public static final java.lang.String JSON_PROPERTY_CHARACTERISTIC_ID = "characteristicId";
	public static final java.lang.String JSON_PROPERTY_FEATURE_ID = "featureId";
	public static final java.lang.String JSON_PROPERTY_NAME = "name";
	public static final java.lang.String JSON_PROPERTY_RELATIONSHIP_TYPE = "relationshipType";
	public static final java.lang.String JSON_PROPERTY_RESOURCE_SPECIFICATION_HREF = "resourceSpecificationHref";
	public static final java.lang.String JSON_PROPERTY_RESOURCE_SPECIFICATION_ID = "resourceSpecificationId";
	public static final java.lang.String JSON_PROPERTY_VALID_FOR = "validFor";

	/** Unique identifier of the characteristic within the the target feature specification */
	@com.fasterxml.jackson.annotation.JsonProperty(JSON_PROPERTY_CHARACTERISTIC_ID)
	@com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
	private java.lang.String characteristicId;

	/** Unique identifier of the target feature specification within the resource specification. */
	@com.fasterxml.jackson.annotation.JsonProperty(JSON_PROPERTY_FEATURE_ID)
	@com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
	private java.lang.String featureId;

	/** Name of the target characteristic */
	@com.fasterxml.jackson.annotation.JsonProperty(JSON_PROPERTY_NAME)
	@com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
	private java.lang.String name;

	/** Type of relationship such as aggregation, migration, substitution, dependency, exclusivity */
	@com.fasterxml.jackson.annotation.JsonProperty(JSON_PROPERTY_RELATIONSHIP_TYPE)
	@com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
	private java.lang.String relationshipType;

	/** Hyperlink reference to the resource specification containing the target feature and feature characteristic */
	@com.fasterxml.jackson.annotation.JsonProperty(JSON_PROPERTY_RESOURCE_SPECIFICATION_HREF)
	@com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
	private java.net.URI resourceSpecificationHref;

	/** Unique identifier of the resource specification containing the target feature and feature characteristic */
	@com.fasterxml.jackson.annotation.JsonProperty(JSON_PROPERTY_RESOURCE_SPECIFICATION_ID)
	@com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
	private java.lang.String resourceSpecificationId;

	@com.fasterxml.jackson.annotation.JsonProperty(JSON_PROPERTY_VALID_FOR)
	@com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
	private TimePeriodVO validFor;

	// methods

	@Override
	public boolean equals(Object object) {
		if (object == this) {
			return true;
		}
		if (object == null || getClass() != object.getClass()) {
			return false;
		}
		FeatureSpecificationCharacteristicRelationshipVO other = (FeatureSpecificationCharacteristicRelationshipVO) object;
		return java.util.Objects.equals(characteristicId, other.characteristicId)
				&& java.util.Objects.equals(featureId, other.featureId)
				&& java.util.Objects.equals(name, other.name)
				&& java.util.Objects.equals(relationshipType, other.relationshipType)
				&& java.util.Objects.equals(resourceSpecificationHref, other.resourceSpecificationHref)
				&& java.util.Objects.equals(resourceSpecificationId, other.resourceSpecificationId)
				&& java.util.Objects.equals(validFor, other.validFor);
	}

	@Override
	public int hashCode() {
		return java.util.Objects.hash(characteristicId, featureId, name, relationshipType, resourceSpecificationHref, resourceSpecificationId, validFor);
	}

	@Override
	public java.lang.String toString() {
		return new java.lang.StringBuilder()
				.append("FeatureSpecificationCharacteristicRelationshipVO[")
				.append("characteristicId=").append(characteristicId).append(",")
				.append("featureId=").append(featureId).append(",")
				.append("name=").append(name).append(",")
				.append("relationshipType=").append(relationshipType).append(",")
				.append("resourceSpecificationHref=").append(resourceSpecificationHref).append(",")
				.append("resourceSpecificationId=").append(resourceSpecificationId).append(",")
				.append("validFor=").append(validFor)
				.append("]")
				.toString();
	}

	// fluent

	public FeatureSpecificationCharacteristicRelationshipVO characteristicId(java.lang.String newCharacteristicId) {
		this.characteristicId = newCharacteristicId;
		return this;
	}

	public FeatureSpecificationCharacteristicRelationshipVO featureId(java.lang.String newFeatureId) {
		this.featureId = newFeatureId;
		return this;
	}

	public FeatureSpecificationCharacteristicRelationshipVO name(java.lang.String newName) {
		this.name = newName;
		return this;
	}

	public FeatureSpecificationCharacteristicRelationshipVO relationshipType(java.lang.String newRelationshipType) {
		this.relationshipType = newRelationshipType;
		return this;
	}

	public FeatureSpecificationCharacteristicRelationshipVO resourceSpecificationHref(java.net.URI newResourceSpecificationHref) {
		this.resourceSpecificationHref = newResourceSpecificationHref;
		return this;
	}

	public FeatureSpecificationCharacteristicRelationshipVO resourceSpecificationId(java.lang.String newResourceSpecificationId) {
		this.resourceSpecificationId = newResourceSpecificationId;
		return this;
	}

	public FeatureSpecificationCharacteristicRelationshipVO validFor(TimePeriodVO newValidFor) {
		this.validFor = newValidFor;
		return this;
	}

	// getter/setter

	public java.lang.String getCharacteristicId() {
		return characteristicId;
	}

	public void setCharacteristicId(java.lang.String newCharacteristicId) {
		this.characteristicId = newCharacteristicId;
	}

	public java.lang.String getFeatureId() {
		return featureId;
	}

	public void setFeatureId(java.lang.String newFeatureId) {
		this.featureId = newFeatureId;
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
}
