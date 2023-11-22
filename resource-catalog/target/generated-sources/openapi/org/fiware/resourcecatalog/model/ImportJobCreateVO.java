package org.fiware.resourcecatalog.model;

@jakarta.annotation.Generated("org.openapitools.codegen.languages.MicronautCodegen")
@io.micronaut.core.annotation.Introspected
public class ImportJobCreateVO {

	public static final java.lang.String JSON_PROPERTY_COMPLETION_DATE = "completionDate";
	public static final java.lang.String JSON_PROPERTY_CONTENT_TYPE = "contentType";
	public static final java.lang.String JSON_PROPERTY_CREATION_DATE = "creationDate";
	public static final java.lang.String JSON_PROPERTY_ERROR_LOG = "errorLog";
	public static final java.lang.String JSON_PROPERTY_PATH = "path";
	public static final java.lang.String JSON_PROPERTY_URL = "url";
	public static final java.lang.String JSON_PROPERTY_STATUS = "status";
	public static final java.lang.String JSON_PROPERTY_AT_BASE_TYPE = "@baseType";
	public static final java.lang.String JSON_PROPERTY_AT_SCHEMA_LOCATION = "@schemaLocation";
	public static final java.lang.String JSON_PROPERTY_AT_TYPE = "@type";

	/** Date at which the job was completed */
	@com.fasterxml.jackson.annotation.JsonProperty(JSON_PROPERTY_COMPLETION_DATE)
	@com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
	private java.time.Instant completionDate;

	/** Indicates the format of the imported data */
	@com.fasterxml.jackson.annotation.JsonProperty(JSON_PROPERTY_CONTENT_TYPE)
	@com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
	private java.lang.String contentType;

	/** Date at which the job was created */
	@com.fasterxml.jackson.annotation.JsonProperty(JSON_PROPERTY_CREATION_DATE)
	@com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
	private java.time.Instant creationDate;

	/** Reason for failure if status is failed */
	@com.fasterxml.jackson.annotation.JsonProperty(JSON_PROPERTY_ERROR_LOG)
	@com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
	private java.lang.String errorLog;

	/** URL of the root resource where the content of the file specified by the import job must be applied */
	@com.fasterxml.jackson.annotation.JsonProperty(JSON_PROPERTY_PATH)
	@com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
	private java.lang.String path;

	/** URL of the file containing the data to be imported */
	@com.fasterxml.jackson.annotation.JsonProperty(JSON_PROPERTY_URL)
	@com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.ALWAYS)
	private java.net.URI url;

	@com.fasterxml.jackson.annotation.JsonProperty(JSON_PROPERTY_STATUS)
	@com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
	private JobStateTypeVO status;

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
		ImportJobCreateVO other = (ImportJobCreateVO) object;
		return java.util.Objects.equals(completionDate, other.completionDate)
				&& java.util.Objects.equals(contentType, other.contentType)
				&& java.util.Objects.equals(creationDate, other.creationDate)
				&& java.util.Objects.equals(errorLog, other.errorLog)
				&& java.util.Objects.equals(path, other.path)
				&& java.util.Objects.equals(url, other.url)
				&& java.util.Objects.equals(status, other.status)
				&& java.util.Objects.equals(atBaseType, other.atBaseType)
				&& java.util.Objects.equals(atSchemaLocation, other.atSchemaLocation)
				&& java.util.Objects.equals(atType, other.atType);
	}

	@Override
	public int hashCode() {
		return java.util.Objects.hash(completionDate, contentType, creationDate, errorLog, path, url, status, atBaseType, atSchemaLocation, atType);
	}

	@Override
	public java.lang.String toString() {
		return new java.lang.StringBuilder()
				.append("ImportJobCreateVO[")
				.append("completionDate=").append(completionDate).append(",")
				.append("contentType=").append(contentType).append(",")
				.append("creationDate=").append(creationDate).append(",")
				.append("errorLog=").append(errorLog).append(",")
				.append("path=").append(path).append(",")
				.append("url=").append(url).append(",")
				.append("status=").append(status).append(",")
				.append("atBaseType=").append(atBaseType).append(",")
				.append("atSchemaLocation=").append(atSchemaLocation).append(",")
				.append("atType=").append(atType)
				.append("]")
				.toString();
	}

	// fluent

	public ImportJobCreateVO completionDate(java.time.Instant newCompletionDate) {
		this.completionDate = newCompletionDate;
		return this;
	}

	public ImportJobCreateVO contentType(java.lang.String newContentType) {
		this.contentType = newContentType;
		return this;
	}

	public ImportJobCreateVO creationDate(java.time.Instant newCreationDate) {
		this.creationDate = newCreationDate;
		return this;
	}

	public ImportJobCreateVO errorLog(java.lang.String newErrorLog) {
		this.errorLog = newErrorLog;
		return this;
	}

	public ImportJobCreateVO path(java.lang.String newPath) {
		this.path = newPath;
		return this;
	}

	public ImportJobCreateVO url(java.net.URI newUrl) {
		this.url = newUrl;
		return this;
	}

	public ImportJobCreateVO status(JobStateTypeVO newStatus) {
		this.status = newStatus;
		return this;
	}

	public ImportJobCreateVO atBaseType(java.lang.String newAtBaseType) {
		this.atBaseType = newAtBaseType;
		return this;
	}

	public ImportJobCreateVO atSchemaLocation(java.net.URI newAtSchemaLocation) {
		this.atSchemaLocation = newAtSchemaLocation;
		return this;
	}

	public ImportJobCreateVO atType(java.lang.String newAtType) {
		this.atType = newAtType;
		return this;
	}

	// getter/setter

	public java.time.Instant getCompletionDate() {
		return completionDate;
	}

	public void setCompletionDate(java.time.Instant newCompletionDate) {
		this.completionDate = newCompletionDate;
	}

	public java.lang.String getContentType() {
		return contentType;
	}

	public void setContentType(java.lang.String newContentType) {
		this.contentType = newContentType;
	}

	public java.time.Instant getCreationDate() {
		return creationDate;
	}

	public void setCreationDate(java.time.Instant newCreationDate) {
		this.creationDate = newCreationDate;
	}

	public java.lang.String getErrorLog() {
		return errorLog;
	}

	public void setErrorLog(java.lang.String newErrorLog) {
		this.errorLog = newErrorLog;
	}

	public java.lang.String getPath() {
		return path;
	}

	public void setPath(java.lang.String newPath) {
		this.path = newPath;
	}

	public java.net.URI getUrl() {
		return url;
	}

	public void setUrl(java.net.URI newUrl) {
		this.url = newUrl;
	}

	public JobStateTypeVO getStatus() {
		return status;
	}

	public void setStatus(JobStateTypeVO newStatus) {
		this.status = newStatus;
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
