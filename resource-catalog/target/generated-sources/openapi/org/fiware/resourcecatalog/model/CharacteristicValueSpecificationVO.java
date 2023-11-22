package org.fiware.resourcecatalog.model;

@jakarta.annotation.Generated("org.openapitools.codegen.languages.MicronautCodegen")
@io.micronaut.core.annotation.Introspected
public class CharacteristicValueSpecificationVO {

	public static final java.lang.String JSON_PROPERTY_IS_DEFAULT = "isDefault";
	public static final java.lang.String JSON_PROPERTY_RANGE_INTERVAL = "rangeInterval";
	public static final java.lang.String JSON_PROPERTY_REGEX = "regex";
	public static final java.lang.String JSON_PROPERTY_UNIT_OF_MEASURE = "unitOfMeasure";
	public static final java.lang.String JSON_PROPERTY_VALUE_FROM = "valueFrom";
	public static final java.lang.String JSON_PROPERTY_VALUE_TO = "valueTo";
	public static final java.lang.String JSON_PROPERTY_VALUE_TYPE = "valueType";
	public static final java.lang.String JSON_PROPERTY_VALID_FOR = "validFor";
	public static final java.lang.String JSON_PROPERTY_VALUE = "value";
	public static final java.lang.String JSON_PROPERTY_AT_BASE_TYPE = "@baseType";
	public static final java.lang.String JSON_PROPERTY_AT_SCHEMA_LOCATION = "@schemaLocation";
	public static final java.lang.String JSON_PROPERTY_AT_TYPE = "@type";

	/** If true, the Boolean Indicates if the value is the default value for a characteristic */
	@com.fasterxml.jackson.annotation.JsonProperty(JSON_PROPERTY_IS_DEFAULT)
	@com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
	private java.lang.Boolean isDefault;

	/** An indicator that specifies the inclusion or exclusion of the valueFrom and valueTo attributes. If applicable, possible values are \&quot;open\&quot;, \&quot;closed\&quot;, \&quot;closedBottom\&quot; and \&quot;closedTop\&quot;. */
	@com.fasterxml.jackson.annotation.JsonProperty(JSON_PROPERTY_RANGE_INTERVAL)
	@com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
	private java.lang.String rangeInterval;

	/** A regular expression constraint for given value */
	@com.fasterxml.jackson.annotation.JsonProperty(JSON_PROPERTY_REGEX)
	@com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
	private java.lang.String regex;

	/** A length, surface, volume, dry measure, liquid measure, money, weight, time, and the like. In general, a determinate quantity or magnitude of the kind designated, taken as a standard of comparison for others of the same kind, in assigning to them numerical values, as 1 foot, 1 yard, 1 mile, 1 square foot. */
	@com.fasterxml.jackson.annotation.JsonProperty(JSON_PROPERTY_UNIT_OF_MEASURE)
	@com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
	private java.lang.String unitOfMeasure;

	/** The low range value that a characteristic can take on */
	@com.fasterxml.jackson.annotation.JsonProperty(JSON_PROPERTY_VALUE_FROM)
	@com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
	private java.lang.Integer valueFrom;

	/** The upper range value that a characteristic can take on */
	@com.fasterxml.jackson.annotation.JsonProperty(JSON_PROPERTY_VALUE_TO)
	@com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
	private java.lang.Integer valueTo;

	/** A kind of value that the characteristic value can take on, such as numeric, text and so forth */
	@com.fasterxml.jackson.annotation.JsonProperty(JSON_PROPERTY_VALUE_TYPE)
	@com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
	private java.lang.String valueType;

	@com.fasterxml.jackson.annotation.JsonProperty(JSON_PROPERTY_VALID_FOR)
	@com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
	private TimePeriodVO validFor;

	@com.fasterxml.jackson.annotation.JsonProperty(JSON_PROPERTY_VALUE)
	@com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
	private java.lang.Object value;

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
		CharacteristicValueSpecificationVO other = (CharacteristicValueSpecificationVO) object;
		return java.util.Objects.equals(isDefault, other.isDefault)
				&& java.util.Objects.equals(rangeInterval, other.rangeInterval)
				&& java.util.Objects.equals(regex, other.regex)
				&& java.util.Objects.equals(unitOfMeasure, other.unitOfMeasure)
				&& java.util.Objects.equals(valueFrom, other.valueFrom)
				&& java.util.Objects.equals(valueTo, other.valueTo)
				&& java.util.Objects.equals(valueType, other.valueType)
				&& java.util.Objects.equals(validFor, other.validFor)
				&& java.util.Objects.equals(value, other.value)
				&& java.util.Objects.equals(atBaseType, other.atBaseType)
				&& java.util.Objects.equals(atSchemaLocation, other.atSchemaLocation)
				&& java.util.Objects.equals(atType, other.atType);
	}

	@Override
	public int hashCode() {
		return java.util.Objects.hash(isDefault, rangeInterval, regex, unitOfMeasure, valueFrom, valueTo, valueType, validFor, value, atBaseType, atSchemaLocation, atType);
	}

	@Override
	public java.lang.String toString() {
		return new java.lang.StringBuilder()
				.append("CharacteristicValueSpecificationVO[")
				.append("isDefault=").append(isDefault).append(",")
				.append("rangeInterval=").append(rangeInterval).append(",")
				.append("regex=").append(regex).append(",")
				.append("unitOfMeasure=").append(unitOfMeasure).append(",")
				.append("valueFrom=").append(valueFrom).append(",")
				.append("valueTo=").append(valueTo).append(",")
				.append("valueType=").append(valueType).append(",")
				.append("validFor=").append(validFor).append(",")
				.append("value=").append(value).append(",")
				.append("atBaseType=").append(atBaseType).append(",")
				.append("atSchemaLocation=").append(atSchemaLocation).append(",")
				.append("atType=").append(atType)
				.append("]")
				.toString();
	}

	// fluent

	public CharacteristicValueSpecificationVO isDefault(java.lang.Boolean newIsDefault) {
		this.isDefault = newIsDefault;
		return this;
	}

	public CharacteristicValueSpecificationVO rangeInterval(java.lang.String newRangeInterval) {
		this.rangeInterval = newRangeInterval;
		return this;
	}

	public CharacteristicValueSpecificationVO regex(java.lang.String newRegex) {
		this.regex = newRegex;
		return this;
	}

	public CharacteristicValueSpecificationVO unitOfMeasure(java.lang.String newUnitOfMeasure) {
		this.unitOfMeasure = newUnitOfMeasure;
		return this;
	}

	public CharacteristicValueSpecificationVO valueFrom(java.lang.Integer newValueFrom) {
		this.valueFrom = newValueFrom;
		return this;
	}

	public CharacteristicValueSpecificationVO valueTo(java.lang.Integer newValueTo) {
		this.valueTo = newValueTo;
		return this;
	}

	public CharacteristicValueSpecificationVO valueType(java.lang.String newValueType) {
		this.valueType = newValueType;
		return this;
	}

	public CharacteristicValueSpecificationVO validFor(TimePeriodVO newValidFor) {
		this.validFor = newValidFor;
		return this;
	}

	public CharacteristicValueSpecificationVO value(java.lang.Object newValue) {
		this.value = newValue;
		return this;
	}

	public CharacteristicValueSpecificationVO atBaseType(java.lang.String newAtBaseType) {
		this.atBaseType = newAtBaseType;
		return this;
	}

	public CharacteristicValueSpecificationVO atSchemaLocation(java.net.URI newAtSchemaLocation) {
		this.atSchemaLocation = newAtSchemaLocation;
		return this;
	}

	public CharacteristicValueSpecificationVO atType(java.lang.String newAtType) {
		this.atType = newAtType;
		return this;
	}

	// getter/setter

	public java.lang.Boolean getIsDefault() {
		return isDefault;
	}

	public void setIsDefault(java.lang.Boolean newIsDefault) {
		this.isDefault = newIsDefault;
	}

	public java.lang.String getRangeInterval() {
		return rangeInterval;
	}

	public void setRangeInterval(java.lang.String newRangeInterval) {
		this.rangeInterval = newRangeInterval;
	}

	public java.lang.String getRegex() {
		return regex;
	}

	public void setRegex(java.lang.String newRegex) {
		this.regex = newRegex;
	}

	public java.lang.String getUnitOfMeasure() {
		return unitOfMeasure;
	}

	public void setUnitOfMeasure(java.lang.String newUnitOfMeasure) {
		this.unitOfMeasure = newUnitOfMeasure;
	}

	public java.lang.Integer getValueFrom() {
		return valueFrom;
	}

	public void setValueFrom(java.lang.Integer newValueFrom) {
		this.valueFrom = newValueFrom;
	}

	public java.lang.Integer getValueTo() {
		return valueTo;
	}

	public void setValueTo(java.lang.Integer newValueTo) {
		this.valueTo = newValueTo;
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

	public java.lang.Object getValue() {
		return value;
	}

	public void setValue(java.lang.Object newValue) {
		this.value = newValue;
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
