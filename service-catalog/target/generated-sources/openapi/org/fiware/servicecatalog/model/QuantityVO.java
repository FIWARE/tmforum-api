package org.fiware.servicecatalog.model;

@jakarta.annotation.Generated("org.openapitools.codegen.languages.MicronautCodegen")
@io.micronaut.core.annotation.Introspected
public class QuantityVO {

	public static final java.lang.String JSON_PROPERTY_AMOUNT = "amount";
	public static final java.lang.String JSON_PROPERTY_UNITS = "units";

	/** Numeric value in a given unit */
	@com.fasterxml.jackson.annotation.JsonProperty(JSON_PROPERTY_AMOUNT)
	@com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
	private java.lang.Float amount = 1.0f;

	/** Unit */
	@com.fasterxml.jackson.annotation.JsonProperty(JSON_PROPERTY_UNITS)
	@com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
	private java.lang.String units;

	// methods

	@Override
	public boolean equals(Object object) {
		if (object == this) {
			return true;
		}
		if (object == null || getClass() != object.getClass()) {
			return false;
		}
		QuantityVO other = (QuantityVO) object;
		return java.util.Objects.equals(amount, other.amount)
				&& java.util.Objects.equals(units, other.units);
	}

	@Override
	public int hashCode() {
		return java.util.Objects.hash(amount, units);
	}

	@Override
	public java.lang.String toString() {
		return new java.lang.StringBuilder()
				.append("QuantityVO[")
				.append("amount=").append(amount).append(",")
				.append("units=").append(units)
				.append("]")
				.toString();
	}

	// fluent

	public QuantityVO amount(java.lang.Float newAmount) {
		this.amount = newAmount;
		return this;
	}

	public QuantityVO units(java.lang.String newUnits) {
		this.units = newUnits;
		return this;
	}

	// getter/setter

	public java.lang.Float getAmount() {
		return amount;
	}

	public void setAmount(java.lang.Float newAmount) {
		this.amount = newAmount;
	}

	public java.lang.String getUnits() {
		return units;
	}

	public void setUnits(java.lang.String newUnits) {
		this.units = newUnits;
	}
}
