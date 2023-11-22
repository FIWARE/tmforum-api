package org.fiware.resourcecatalog.model;

@jakarta.annotation.Generated("org.openapitools.codegen.languages.MicronautCodegen")
@io.micronaut.core.annotation.Introspected
public class CharacteristicSpecificationBaseVO {

	public static final java.lang.String JSON_PROPERTY_ID = "id";
	public static final java.lang.String JSON_PROPERTY_CONFIGURABLE = "configurable";
	public static final java.lang.String JSON_PROPERTY_DESCRIPTION = "description";
	public static final java.lang.String JSON_PROPERTY_EXTENSIBLE = "extensible";
	public static final java.lang.String JSON_PROPERTY_IS_UNIQUE = "isUnique";
	public static final java.lang.String JSON_PROPERTY_MAX_CARDINALITY = "maxCardinality";
	public static final java.lang.String JSON_PROPERTY_MIN_CARDINALITY = "minCardinality";
	public static final java.lang.String JSON_PROPERTY_NAME = "name";
	public static final java.lang.String JSON_PROPERTY_REGEX = "regex";
	public static final java.lang.String JSON_PROPERTY_VALUE_TYPE = "valueType";
	public static final java.lang.String JSON_PROPERTY_VALID_FOR = "validFor";
	public static final java.lang.String JSON_PROPERTY_AT_BASE_TYPE = "@baseType";
	public static final java.lang.String JSON_PROPERTY_AT_SCHEMA_LOCATION = "@schemaLocation";
	public static final java.lang.String JSON_PROPERTY_AT_TYPE = "@type";
	public static final java.lang.String JSON_PROPERTY_AT_VALUE_SCHEMA_LOCATION = "@valueSchemaLocation";

	/** Unique ID for the characteristic */
	@com.fasterxml.jackson.annotation.JsonProperty(JSON_PROPERTY_ID)
	@com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
	private java.lang.String id;

	/** If true, the Boolean indicates that the target Characteristic is configurable */
	@com.fasterxml.jackson.annotation.JsonProperty(JSON_PROPERTY_CONFIGURABLE)
	@com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
	private java.lang.Boolean configurable;

	/** A narrative that explains the CharacteristicSpecification. */
	@com.fasterxml.jackson.annotation.JsonProperty(JSON_PROPERTY_DESCRIPTION)
	@com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
	private java.lang.String description;

	/** An indicator that specifies that the values for the characteristic can be extended by adding new values when instantiating a characteristic for a resource. */
	@com.fasterxml.jackson.annotation.JsonProperty(JSON_PROPERTY_EXTENSIBLE)
	@com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
	private java.lang.Boolean extensible;

	/** An indicator that specifies if a value is unique for the specification. Possible values are; \&quot;unique while value is in effect\&quot; and \&quot;unique whether value is in effect or not\&quot; */
	@com.fasterxml.jackson.annotation.JsonProperty(JSON_PROPERTY_IS_UNIQUE)
	@com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
	private java.lang.Boolean isUnique;

	/** The maximum number of instances a CharacteristicValue can take on. For example, zero to five phone numbers in a group calling plan, where five is the value for the maxCardinality. */
	@com.fasterxml.jackson.annotation.JsonProperty(JSON_PROPERTY_MAX_CARDINALITY)
	@com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
	private java.lang.Integer maxCardinality;

	/** The minimum number of instances a CharacteristicValue can take on. For example, zero to five phone numbers in a group calling plan, where zero is the value for the minCardinality. */
	@com.fasterxml.jackson.annotation.JsonProperty(JSON_PROPERTY_MIN_CARDINALITY)
	@com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
	private java.lang.Integer minCardinality;

	/** A word, term, or phrase by which this characteristic specification is known and distinguished from other characteristic specifications. */
	@com.fasterxml.jackson.annotation.JsonProperty(JSON_PROPERTY_NAME)
	@com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
	private java.lang.String name;

	/** A rule or principle represented in regular expression used to derive the value of a characteristic value. */
	@com.fasterxml.jackson.annotation.JsonProperty(JSON_PROPERTY_REGEX)
	@com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
	private java.lang.String regex;

	/** A kind of value that the characteristic can take on, such as numeric, text and so forth */
	@com.fasterxml.jackson.annotation.JsonProperty(JSON_PROPERTY_VALUE_TYPE)
	@com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
	private java.lang.String valueType;

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

	/** This (optional) field provides a link to the schema describing the value type. */
	@com.fasterxml.jackson.annotation.JsonProperty(JSON_PROPERTY_AT_VALUE_SCHEMA_LOCATION)
	@com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
	private java.lang.String atValueSchemaLocation;

	// methods

	@Override
	public boolean equals(Object object) {
		if (object == this) {
			return true;
		}
		if (object == null || getClass() != object.getClass()) {
			return false;
		}
		CharacteristicSpecificationBaseVO other = (CharacteristicSpecificationBaseVO) object;
		return java.util.Objects.equals(id, other.id)
				&& java.util.Objects.equals(configurable, other.configurable)
				&& java.util.Objects.equals(description, other.description)
				&& java.util.Objects.equals(extensible, other.extensible)
				&& java.util.Objects.equals(isUnique, other.isUnique)
				&& java.util.Objects.equals(maxCardinality, other.maxCardinality)
				&& java.util.Objects.equals(minCardinality, other.minCardinality)
				&& java.util.Objects.equals(name, other.name)
				&& java.util.Objects.equals(regex, other.regex)
				&& java.util.Objects.equals(valueType, other.valueType)
				&& java.util.Objects.equals(validFor, other.validFor)
				&& java.util.Objects.equals(atBaseType, other.atBaseType)
				&& java.util.Objects.equals(atSchemaLocation, other.atSchemaLocation)
				&& java.util.Objects.equals(atType, other.atType)
				&& java.util.Objects.equals(atValueSchemaLocation, other.atValueSchemaLocation);
	}

	@Override
	public int hashCode() {
		return java.util.Objects.hash(id, configurable, description, extensible, isUnique, maxCardinality, minCardinality, name, regex, valueType, validFor, atBaseType, atSchemaLocation, atType, atValueSchemaLocation);
	}

	@Override
	public java.lang.String toString() {
		return new java.lang.StringBuilder()
				.append("CharacteristicSpecificationBaseVO[")
				.append("id=").append(id).append(",")
				.append("configurable=").append(configurable).append(",")
				.append("description=").append(description).append(",")
				.append("extensible=").append(extensible).append(",")
				.append("isUnique=").append(isUnique).append(",")
				.append("maxCardinality=").append(maxCardinality).append(",")
				.append("minCardinality=").append(minCardinality).append(",")
				.append("name=").append(name).append(",")
				.append("regex=").append(regex).append(",")
				.append("valueType=").append(valueType).append(",")
				.append("validFor=").append(validFor).append(",")
				.append("atBaseType=").append(atBaseType).append(",")
				.append("atSchemaLocation=").append(atSchemaLocation).append(",")
				.append("atType=").append(atType).append(",")
				.append("atValueSchemaLocation=").append(atValueSchemaLocation)
				.append("]")
				.toString();
	}

	// fluent

	public CharacteristicSpecificationBaseVO id(java.lang.String newId) {
		this.id = newId;
		return this;
	}

	public CharacteristicSpecificationBaseVO configurable(java.lang.Boolean newConfigurable) {
		this.configurable = newConfigurable;
		return this;
	}

	public CharacteristicSpecificationBaseVO description(java.lang.String newDescription) {
		this.description = newDescription;
		return this;
	}

	public CharacteristicSpecificationBaseVO extensible(java.lang.Boolean newExtensible) {
		this.extensible = newExtensible;
		return this;
	}

	public CharacteristicSpecificationBaseVO isUnique(java.lang.Boolean newIsUnique) {
		this.isUnique = newIsUnique;
		return this;
	}

	public CharacteristicSpecificationBaseVO maxCardinality(java.lang.Integer newMaxCardinality) {
		this.maxCardinality = newMaxCardinality;
		return this;
	}

	public CharacteristicSpecificationBaseVO minCardinality(java.lang.Integer newMinCardinality) {
		this.minCardinality = newMinCardinality;
		return this;
	}

	public CharacteristicSpecificationBaseVO name(java.lang.String newName) {
		this.name = newName;
		return this;
	}

	public CharacteristicSpecificationBaseVO regex(java.lang.String newRegex) {
		this.regex = newRegex;
		return this;
	}

	public CharacteristicSpecificationBaseVO valueType(java.lang.String newValueType) {
		this.valueType = newValueType;
		return this;
	}

	public CharacteristicSpecificationBaseVO validFor(TimePeriodVO newValidFor) {
		this.validFor = newValidFor;
		return this;
	}

	public CharacteristicSpecificationBaseVO atBaseType(java.lang.String newAtBaseType) {
		this.atBaseType = newAtBaseType;
		return this;
	}

	public CharacteristicSpecificationBaseVO atSchemaLocation(java.net.URI newAtSchemaLocation) {
		this.atSchemaLocation = newAtSchemaLocation;
		return this;
	}

	public CharacteristicSpecificationBaseVO atType(java.lang.String newAtType) {
		this.atType = newAtType;
		return this;
	}

	public CharacteristicSpecificationBaseVO atValueSchemaLocation(java.lang.String newAtValueSchemaLocation) {
		this.atValueSchemaLocation = newAtValueSchemaLocation;
		return this;
	}

	// getter/setter

	public java.lang.String getId() {
		return id;
	}

	public void setId(java.lang.String newId) {
		this.id = newId;
	}

	public java.lang.Boolean getConfigurable() {
		return configurable;
	}

	public void setConfigurable(java.lang.Boolean newConfigurable) {
		this.configurable = newConfigurable;
	}

	public java.lang.String getDescription() {
		return description;
	}

	public void setDescription(java.lang.String newDescription) {
		this.description = newDescription;
	}

	public java.lang.Boolean getExtensible() {
		return extensible;
	}

	public void setExtensible(java.lang.Boolean newExtensible) {
		this.extensible = newExtensible;
	}

	public java.lang.Boolean getIsUnique() {
		return isUnique;
	}

	public void setIsUnique(java.lang.Boolean newIsUnique) {
		this.isUnique = newIsUnique;
	}

	public java.lang.Integer getMaxCardinality() {
		return maxCardinality;
	}

	public void setMaxCardinality(java.lang.Integer newMaxCardinality) {
		this.maxCardinality = newMaxCardinality;
	}

	public java.lang.Integer getMinCardinality() {
		return minCardinality;
	}

	public void setMinCardinality(java.lang.Integer newMinCardinality) {
		this.minCardinality = newMinCardinality;
	}

	public java.lang.String getName() {
		return name;
	}

	public void setName(java.lang.String newName) {
		this.name = newName;
	}

	public java.lang.String getRegex() {
		return regex;
	}

	public void setRegex(java.lang.String newRegex) {
		this.regex = newRegex;
	}

	public java.lang.String getValueType() {
		return valueType;
	}

	public void setValueType(java.lang.String newValueType) {
		this.valueType = newValueType;
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

	public java.lang.String getAtValueSchemaLocation() {
		return atValueSchemaLocation;
	}

	public void setAtValueSchemaLocation(java.lang.String newAtValueSchemaLocation) {
		this.atValueSchemaLocation = newAtValueSchemaLocation;
	}
}
