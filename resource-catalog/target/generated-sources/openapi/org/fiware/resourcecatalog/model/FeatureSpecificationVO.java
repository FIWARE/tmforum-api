package org.fiware.resourcecatalog.model;

@jakarta.annotation.Generated("org.openapitools.codegen.languages.MicronautCodegen")
@io.micronaut.core.annotation.Introspected
public class FeatureSpecificationVO {

	public static final java.lang.String JSON_PROPERTY_ID = "id";
	public static final java.lang.String JSON_PROPERTY_HREF = "href";
	public static final java.lang.String JSON_PROPERTY_IS_BUNDLE = "isBundle";
	public static final java.lang.String JSON_PROPERTY_IS_ENABLED = "isEnabled";
	public static final java.lang.String JSON_PROPERTY_NAME = "name";
	public static final java.lang.String JSON_PROPERTY_VERSION = "version";
	public static final java.lang.String JSON_PROPERTY_CONSTRAINT = "constraint";
	public static final java.lang.String JSON_PROPERTY_FEATURE_SPEC_CHARACTERISTIC = "featureSpecCharacteristic";
	public static final java.lang.String JSON_PROPERTY_FEATURE_SPEC_RELATIONSHIP = "featureSpecRelationship";
	public static final java.lang.String JSON_PROPERTY_VALID_FOR = "validFor";
	public static final java.lang.String JSON_PROPERTY_AT_BASE_TYPE = "@baseType";
	public static final java.lang.String JSON_PROPERTY_AT_SCHEMA_LOCATION = "@schemaLocation";
	public static final java.lang.String JSON_PROPERTY_AT_TYPE = "@type";

	/** Identifier of the feature specification. Must be locally unique within the containing specification, thus allowing direct access to the feature spec. */
	@com.fasterxml.jackson.annotation.JsonProperty(JSON_PROPERTY_ID)
	@com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
	private java.lang.String id;

	/** Hyperlink reference */
	@com.fasterxml.jackson.annotation.JsonProperty(JSON_PROPERTY_HREF)
	@com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
	private java.net.URI href;

	/** A flag indicating if this is a feature group (true) or not (false) */
	@com.fasterxml.jackson.annotation.JsonProperty(JSON_PROPERTY_IS_BUNDLE)
	@com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
	private java.lang.Boolean isBundle;

	/** A flag indicating if the feature is enabled (true) or not (false) */
	@com.fasterxml.jackson.annotation.JsonProperty(JSON_PROPERTY_IS_ENABLED)
	@com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
	private java.lang.Boolean isEnabled;

	/** Unique name given to the feature specification */
	@com.fasterxml.jackson.annotation.JsonProperty(JSON_PROPERTY_NAME)
	@com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
	private java.lang.String name;

	/** Version of the feature specification */
	@com.fasterxml.jackson.annotation.JsonProperty(JSON_PROPERTY_VERSION)
	@com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
	private java.lang.String version;

	/** This is a list of feature constraints */
	@com.fasterxml.jackson.annotation.JsonProperty(JSON_PROPERTY_CONSTRAINT)
	@com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
	private java.util.List<ConstraintRefVO> constraint;

	/** This is a list of characteristics for a particular feature */
	@com.fasterxml.jackson.annotation.JsonProperty(JSON_PROPERTY_FEATURE_SPEC_CHARACTERISTIC)
	@com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
	private java.util.List<FeatureSpecificationCharacteristicVO> featureSpecCharacteristic;

	/** A dependency, exclusivity or aggratation relationship between/among feature specifications. */
	@com.fasterxml.jackson.annotation.JsonProperty(JSON_PROPERTY_FEATURE_SPEC_RELATIONSHIP)
	@com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
	private java.util.List<FeatureSpecificationRelationshipVO> featureSpecRelationship;

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
		FeatureSpecificationVO other = (FeatureSpecificationVO) object;
		return java.util.Objects.equals(id, other.id)
				&& java.util.Objects.equals(href, other.href)
				&& java.util.Objects.equals(isBundle, other.isBundle)
				&& java.util.Objects.equals(isEnabled, other.isEnabled)
				&& java.util.Objects.equals(name, other.name)
				&& java.util.Objects.equals(version, other.version)
				&& java.util.Objects.equals(constraint, other.constraint)
				&& java.util.Objects.equals(featureSpecCharacteristic, other.featureSpecCharacteristic)
				&& java.util.Objects.equals(featureSpecRelationship, other.featureSpecRelationship)
				&& java.util.Objects.equals(validFor, other.validFor)
				&& java.util.Objects.equals(atBaseType, other.atBaseType)
				&& java.util.Objects.equals(atSchemaLocation, other.atSchemaLocation)
				&& java.util.Objects.equals(atType, other.atType);
	}

	@Override
	public int hashCode() {
		return java.util.Objects.hash(id, href, isBundle, isEnabled, name, version, constraint, featureSpecCharacteristic, featureSpecRelationship, validFor, atBaseType, atSchemaLocation, atType);
	}

	@Override
	public java.lang.String toString() {
		return new java.lang.StringBuilder()
				.append("FeatureSpecificationVO[")
				.append("id=").append(id).append(",")
				.append("href=").append(href).append(",")
				.append("isBundle=").append(isBundle).append(",")
				.append("isEnabled=").append(isEnabled).append(",")
				.append("name=").append(name).append(",")
				.append("version=").append(version).append(",")
				.append("constraint=").append(constraint).append(",")
				.append("featureSpecCharacteristic=").append(featureSpecCharacteristic).append(",")
				.append("featureSpecRelationship=").append(featureSpecRelationship).append(",")
				.append("validFor=").append(validFor).append(",")
				.append("atBaseType=").append(atBaseType).append(",")
				.append("atSchemaLocation=").append(atSchemaLocation).append(",")
				.append("atType=").append(atType)
				.append("]")
				.toString();
	}

	// fluent

	public FeatureSpecificationVO id(java.lang.String newId) {
		this.id = newId;
		return this;
	}

	public FeatureSpecificationVO href(java.net.URI newHref) {
		this.href = newHref;
		return this;
	}

	public FeatureSpecificationVO isBundle(java.lang.Boolean newIsBundle) {
		this.isBundle = newIsBundle;
		return this;
	}

	public FeatureSpecificationVO isEnabled(java.lang.Boolean newIsEnabled) {
		this.isEnabled = newIsEnabled;
		return this;
	}

	public FeatureSpecificationVO name(java.lang.String newName) {
		this.name = newName;
		return this;
	}

	public FeatureSpecificationVO version(java.lang.String newVersion) {
		this.version = newVersion;
		return this;
	}

	public FeatureSpecificationVO constraint(java.util.List<ConstraintRefVO> newConstraint) {
		this.constraint = newConstraint;
		return this;
	}
	
	public FeatureSpecificationVO addConstraintItem(ConstraintRefVO constraintItem) {
		if (this.constraint == null) {
			this.constraint = new java.util.ArrayList<>();
		}
		this.constraint.add(constraintItem);
		return this;
	}

	public FeatureSpecificationVO removeConstraintItem(ConstraintRefVO constraintItem) {
		if (this.constraint != null) {
			this.constraint.remove(constraintItem);
		}
		return this;
	}

	public FeatureSpecificationVO featureSpecCharacteristic(java.util.List<FeatureSpecificationCharacteristicVO> newFeatureSpecCharacteristic) {
		this.featureSpecCharacteristic = newFeatureSpecCharacteristic;
		return this;
	}
	
	public FeatureSpecificationVO addFeatureSpecCharacteristicItem(FeatureSpecificationCharacteristicVO featureSpecCharacteristicItem) {
		if (this.featureSpecCharacteristic == null) {
			this.featureSpecCharacteristic = new java.util.ArrayList<>();
		}
		this.featureSpecCharacteristic.add(featureSpecCharacteristicItem);
		return this;
	}

	public FeatureSpecificationVO removeFeatureSpecCharacteristicItem(FeatureSpecificationCharacteristicVO featureSpecCharacteristicItem) {
		if (this.featureSpecCharacteristic != null) {
			this.featureSpecCharacteristic.remove(featureSpecCharacteristicItem);
		}
		return this;
	}

	public FeatureSpecificationVO featureSpecRelationship(java.util.List<FeatureSpecificationRelationshipVO> newFeatureSpecRelationship) {
		this.featureSpecRelationship = newFeatureSpecRelationship;
		return this;
	}
	
	public FeatureSpecificationVO addFeatureSpecRelationshipItem(FeatureSpecificationRelationshipVO featureSpecRelationshipItem) {
		if (this.featureSpecRelationship == null) {
			this.featureSpecRelationship = new java.util.ArrayList<>();
		}
		this.featureSpecRelationship.add(featureSpecRelationshipItem);
		return this;
	}

	public FeatureSpecificationVO removeFeatureSpecRelationshipItem(FeatureSpecificationRelationshipVO featureSpecRelationshipItem) {
		if (this.featureSpecRelationship != null) {
			this.featureSpecRelationship.remove(featureSpecRelationshipItem);
		}
		return this;
	}

	public FeatureSpecificationVO validFor(TimePeriodVO newValidFor) {
		this.validFor = newValidFor;
		return this;
	}

	public FeatureSpecificationVO atBaseType(java.lang.String newAtBaseType) {
		this.atBaseType = newAtBaseType;
		return this;
	}

	public FeatureSpecificationVO atSchemaLocation(java.net.URI newAtSchemaLocation) {
		this.atSchemaLocation = newAtSchemaLocation;
		return this;
	}

	public FeatureSpecificationVO atType(java.lang.String newAtType) {
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

	public java.lang.Boolean getIsBundle() {
		return isBundle;
	}

	public void setIsBundle(java.lang.Boolean newIsBundle) {
		this.isBundle = newIsBundle;
	}

	public java.lang.Boolean getIsEnabled() {
		return isEnabled;
	}

	public void setIsEnabled(java.lang.Boolean newIsEnabled) {
		this.isEnabled = newIsEnabled;
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

	public java.util.List<ConstraintRefVO> getConstraint() {
		return constraint;
	}

	public void setConstraint(java.util.List<ConstraintRefVO> newConstraint) {
		this.constraint = newConstraint;
	}

	public java.util.List<FeatureSpecificationCharacteristicVO> getFeatureSpecCharacteristic() {
		return featureSpecCharacteristic;
	}

	public void setFeatureSpecCharacteristic(java.util.List<FeatureSpecificationCharacteristicVO> newFeatureSpecCharacteristic) {
		this.featureSpecCharacteristic = newFeatureSpecCharacteristic;
	}

	public java.util.List<FeatureSpecificationRelationshipVO> getFeatureSpecRelationship() {
		return featureSpecRelationship;
	}

	public void setFeatureSpecRelationship(java.util.List<FeatureSpecificationRelationshipVO> newFeatureSpecRelationship) {
		this.featureSpecRelationship = newFeatureSpecRelationship;
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
