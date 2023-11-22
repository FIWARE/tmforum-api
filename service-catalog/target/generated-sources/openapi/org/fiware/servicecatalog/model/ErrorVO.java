package org.fiware.servicecatalog.model;

@jakarta.annotation.Generated("org.openapitools.codegen.languages.MicronautCodegen")
@io.micronaut.core.annotation.Introspected
public class ErrorVO {

	public static final java.lang.String JSON_PROPERTY_CODE = "code";
	public static final java.lang.String JSON_PROPERTY_REASON = "reason";
	public static final java.lang.String JSON_PROPERTY_MESSAGE = "message";
	public static final java.lang.String JSON_PROPERTY_STATUS = "status";
	public static final java.lang.String JSON_PROPERTY_REFERENCE_ERROR = "referenceError";
	public static final java.lang.String JSON_PROPERTY_AT_BASE_TYPE = "@baseType";
	public static final java.lang.String JSON_PROPERTY_AT_SCHEMA_LOCATION = "@schemaLocation";
	public static final java.lang.String JSON_PROPERTY_AT_TYPE = "@type";

	/** Application relevant detail, defined in the API or a common list. */
	@com.fasterxml.jackson.annotation.JsonProperty(JSON_PROPERTY_CODE)
	@com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.ALWAYS)
	private java.lang.String code;

	/** Explanation of the reason for the error which can be shown to a client user. */
	@com.fasterxml.jackson.annotation.JsonProperty(JSON_PROPERTY_REASON)
	@com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.ALWAYS)
	private java.lang.String reason;

	/** More details and corrective actions related to the error which can be shown to a client user. */
	@com.fasterxml.jackson.annotation.JsonProperty(JSON_PROPERTY_MESSAGE)
	@com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
	private java.lang.String message;

	/** HTTP Error code extension */
	@com.fasterxml.jackson.annotation.JsonProperty(JSON_PROPERTY_STATUS)
	@com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
	private java.lang.String status;

	/** URI of documentation describing the error. */
	@com.fasterxml.jackson.annotation.JsonProperty(JSON_PROPERTY_REFERENCE_ERROR)
	@com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
	private java.net.URI referenceError;

	/** When sub-classing, this defines the super-class. */
	@com.fasterxml.jackson.annotation.JsonProperty(JSON_PROPERTY_AT_BASE_TYPE)
	@com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
	private java.lang.String atBaseType;

	/** A URI to a JSON-Schema file that defines additional attributes and relationships */
	@com.fasterxml.jackson.annotation.JsonProperty(JSON_PROPERTY_AT_SCHEMA_LOCATION)
	@com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
	private java.net.URI atSchemaLocation;

	/** When sub-classing, this defines the sub-class entity name. */
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
		ErrorVO other = (ErrorVO) object;
		return java.util.Objects.equals(code, other.code)
				&& java.util.Objects.equals(reason, other.reason)
				&& java.util.Objects.equals(message, other.message)
				&& java.util.Objects.equals(status, other.status)
				&& java.util.Objects.equals(referenceError, other.referenceError)
				&& java.util.Objects.equals(atBaseType, other.atBaseType)
				&& java.util.Objects.equals(atSchemaLocation, other.atSchemaLocation)
				&& java.util.Objects.equals(atType, other.atType);
	}

	@Override
	public int hashCode() {
		return java.util.Objects.hash(code, reason, message, status, referenceError, atBaseType, atSchemaLocation, atType);
	}

	@Override
	public java.lang.String toString() {
		return new java.lang.StringBuilder()
				.append("ErrorVO[")
				.append("code=").append(code).append(",")
				.append("reason=").append(reason).append(",")
				.append("message=").append(message).append(",")
				.append("status=").append(status).append(",")
				.append("referenceError=").append(referenceError).append(",")
				.append("atBaseType=").append(atBaseType).append(",")
				.append("atSchemaLocation=").append(atSchemaLocation).append(",")
				.append("atType=").append(atType)
				.append("]")
				.toString();
	}

	// fluent

	public ErrorVO code(java.lang.String newCode) {
		this.code = newCode;
		return this;
	}

	public ErrorVO reason(java.lang.String newReason) {
		this.reason = newReason;
		return this;
	}

	public ErrorVO message(java.lang.String newMessage) {
		this.message = newMessage;
		return this;
	}

	public ErrorVO status(java.lang.String newStatus) {
		this.status = newStatus;
		return this;
	}

	public ErrorVO referenceError(java.net.URI newReferenceError) {
		this.referenceError = newReferenceError;
		return this;
	}

	public ErrorVO atBaseType(java.lang.String newAtBaseType) {
		this.atBaseType = newAtBaseType;
		return this;
	}

	public ErrorVO atSchemaLocation(java.net.URI newAtSchemaLocation) {
		this.atSchemaLocation = newAtSchemaLocation;
		return this;
	}

	public ErrorVO atType(java.lang.String newAtType) {
		this.atType = newAtType;
		return this;
	}

	// getter/setter

	public java.lang.String getCode() {
		return code;
	}

	public void setCode(java.lang.String newCode) {
		this.code = newCode;
	}

	public java.lang.String getReason() {
		return reason;
	}

	public void setReason(java.lang.String newReason) {
		this.reason = newReason;
	}

	public java.lang.String getMessage() {
		return message;
	}

	public void setMessage(java.lang.String newMessage) {
		this.message = newMessage;
	}

	public java.lang.String getStatus() {
		return status;
	}

	public void setStatus(java.lang.String newStatus) {
		this.status = newStatus;
	}

	public java.net.URI getReferenceError() {
		return referenceError;
	}

	public void setReferenceError(java.net.URI newReferenceError) {
		this.referenceError = newReferenceError;
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
