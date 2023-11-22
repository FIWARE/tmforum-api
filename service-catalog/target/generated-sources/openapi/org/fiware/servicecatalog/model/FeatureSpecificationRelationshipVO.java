package org.fiware.servicecatalog.model;

@jakarta.annotation.Generated("org.openapitools.codegen.languages.MicronautCodegen")
@io.micronaut.core.annotation.Introspected
public class FeatureSpecificationRelationshipVO {

	public static final java.lang.String JSON_PROPERTY_FEATURE_ID = "featureId";
	public static final java.lang.String JSON_PROPERTY_NAME = "name";
	public static final java.lang.String JSON_PROPERTY_PARENT_SPECIFICATION_HREF = "parentSpecificationHref";
	public static final java.lang.String JSON_PROPERTY_PARENT_SPECIFICATION_ID = "parentSpecificationId";
	public static final java.lang.String JSON_PROPERTY_RELATIONSHIP_TYPE = "relationshipType";
	public static final java.lang.String JSON_PROPERTY_VALID_FOR = "validFor";

	/** Unique identifier of the target feature specification. */
	@com.fasterxml.jackson.annotation.JsonProperty(JSON_PROPERTY_FEATURE_ID)
	@com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
	private java.lang.String featureId;

	/** This is the name of the target feature specification. */
	@com.fasterxml.jackson.annotation.JsonProperty(JSON_PROPERTY_NAME)
	@com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.ALWAYS)
	private java.lang.String name;

	/** Hyperlink reference to the parent specification containing the target feature */
	@com.fasterxml.jackson.annotation.JsonProperty(JSON_PROPERTY_PARENT_SPECIFICATION_HREF)
	@com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
	private java.net.URI parentSpecificationHref;

	/** Unique identifier of the parent specification containing the target feature */
	@com.fasterxml.jackson.annotation.JsonProperty(JSON_PROPERTY_PARENT_SPECIFICATION_ID)
	@com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
	private java.lang.String parentSpecificationId;

	/** This is the type of the feature specification relationship. */
	@com.fasterxml.jackson.annotation.JsonProperty(JSON_PROPERTY_RELATIONSHIP_TYPE)
	@com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.ALWAYS)
	private java.lang.String relationshipType;

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
		FeatureSpecificationRelationshipVO other = (FeatureSpecificationRelationshipVO) object;
		return java.util.Objects.equals(featureId, other.featureId)
				&& java.util.Objects.equals(name, other.name)
				&& java.util.Objects.equals(parentSpecificationHref, other.parentSpecificationHref)
				&& java.util.Objects.equals(parentSpecificationId, other.parentSpecificationId)
				&& java.util.Objects.equals(relationshipType, other.relationshipType)
				&& java.util.Objects.equals(validFor, other.validFor);
	}

	@Override
	public int hashCode() {
		return java.util.Objects.hash(featureId, name, parentSpecificationHref, parentSpecificationId, relationshipType, validFor);
	}

	@Override
	public java.lang.String toString() {
		return new java.lang.StringBuilder()
				.append("FeatureSpecificationRelationshipVO[")
				.append("featureId=").append(featureId).append(",")
				.append("name=").append(name).append(",")
				.append("parentSpecificationHref=").append(parentSpecificationHref).append(",")
				.append("parentSpecificationId=").append(parentSpecificationId).append(",")
				.append("relationshipType=").append(relationshipType).append(",")
				.append("validFor=").append(validFor)
				.append("]")
				.toString();
	}

	// fluent

	public FeatureSpecificationRelationshipVO featureId(java.lang.String newFeatureId) {
		this.featureId = newFeatureId;
		return this;
	}

	public FeatureSpecificationRelationshipVO name(java.lang.String newName) {
		this.name = newName;
		return this;
	}

	public FeatureSpecificationRelationshipVO parentSpecificationHref(java.net.URI newParentSpecificationHref) {
		this.parentSpecificationHref = newParentSpecificationHref;
		return this;
	}

	public FeatureSpecificationRelationshipVO parentSpecificationId(java.lang.String newParentSpecificationId) {
		this.parentSpecificationId = newParentSpecificationId;
		return this;
	}

	public FeatureSpecificationRelationshipVO relationshipType(java.lang.String newRelationshipType) {
		this.relationshipType = newRelationshipType;
		return this;
	}

	public FeatureSpecificationRelationshipVO validFor(TimePeriodVO newValidFor) {
		this.validFor = newValidFor;
		return this;
	}

	// getter/setter

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

	public java.net.URI getParentSpecificationHref() {
		return parentSpecificationHref;
	}

	public void setParentSpecificationHref(java.net.URI newParentSpecificationHref) {
		this.parentSpecificationHref = newParentSpecificationHref;
	}

	public java.lang.String getParentSpecificationId() {
		return parentSpecificationId;
	}

	public void setParentSpecificationId(java.lang.String newParentSpecificationId) {
		this.parentSpecificationId = newParentSpecificationId;
	}

	public java.lang.String getRelationshipType() {
		return relationshipType;
	}

	public void setRelationshipType(java.lang.String newRelationshipType) {
		this.relationshipType = newRelationshipType;
	}

	public TimePeriodVO getValidFor() {
		return validFor;
	}

	public void setValidFor(TimePeriodVO newValidFor) {
		this.validFor = newValidFor;
	}
}
