package org.fiware.resourcecatalog.model;

@jakarta.annotation.Generated("org.openapitools.codegen.languages.MicronautCodegen")
@io.micronaut.core.annotation.Introspected
public class TimePeriodVO {

	public static final java.lang.String JSON_PROPERTY_END_DATE_TIME = "endDateTime";
	public static final java.lang.String JSON_PROPERTY_START_DATE_TIME = "startDateTime";

	/** End of the time period, using IETC-RFC-3339 format */
	@com.fasterxml.jackson.annotation.JsonProperty(JSON_PROPERTY_END_DATE_TIME)
	@com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
	private java.time.Instant endDateTime;

	/** Start of the time period, using IETC-RFC-3339 format */
	@com.fasterxml.jackson.annotation.JsonProperty(JSON_PROPERTY_START_DATE_TIME)
	@com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
	private java.time.Instant startDateTime;

	// methods

	@Override
	public boolean equals(Object object) {
		if (object == this) {
			return true;
		}
		if (object == null || getClass() != object.getClass()) {
			return false;
		}
		TimePeriodVO other = (TimePeriodVO) object;
		return java.util.Objects.equals(endDateTime, other.endDateTime)
				&& java.util.Objects.equals(startDateTime, other.startDateTime);
	}

	@Override
	public int hashCode() {
		return java.util.Objects.hash(endDateTime, startDateTime);
	}

	@Override
	public java.lang.String toString() {
		return new java.lang.StringBuilder()
				.append("TimePeriodVO[")
				.append("endDateTime=").append(endDateTime).append(",")
				.append("startDateTime=").append(startDateTime)
				.append("]")
				.toString();
	}

	// fluent

	public TimePeriodVO endDateTime(java.time.Instant newEndDateTime) {
		this.endDateTime = newEndDateTime;
		return this;
	}

	public TimePeriodVO startDateTime(java.time.Instant newStartDateTime) {
		this.startDateTime = newStartDateTime;
		return this;
	}

	// getter/setter

	public java.time.Instant getEndDateTime() {
		return endDateTime;
	}

	public void setEndDateTime(java.time.Instant newEndDateTime) {
		this.endDateTime = newEndDateTime;
	}

	public java.time.Instant getStartDateTime() {
		return startDateTime;
	}

	public void setStartDateTime(java.time.Instant newStartDateTime) {
		this.startDateTime = newStartDateTime;
	}
}
