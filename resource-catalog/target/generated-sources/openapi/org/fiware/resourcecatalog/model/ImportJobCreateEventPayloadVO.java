package org.fiware.resourcecatalog.model;

@jakarta.annotation.Generated("org.openapitools.codegen.languages.MicronautCodegen")
@io.micronaut.core.annotation.Introspected
public class ImportJobCreateEventPayloadVO {

	public static final java.lang.String JSON_PROPERTY_IMPORT_JOB = "importJob";

	@com.fasterxml.jackson.annotation.JsonProperty(JSON_PROPERTY_IMPORT_JOB)
	@com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
	private ImportJobVO importJob;

	// methods

	@Override
	public boolean equals(Object object) {
		if (object == this) {
			return true;
		}
		if (object == null || getClass() != object.getClass()) {
			return false;
		}
		ImportJobCreateEventPayloadVO other = (ImportJobCreateEventPayloadVO) object;
		return java.util.Objects.equals(importJob, other.importJob);
	}

	@Override
	public int hashCode() {
		return java.util.Objects.hash(importJob);
	}

	@Override
	public java.lang.String toString() {
		return new java.lang.StringBuilder()
				.append("ImportJobCreateEventPayloadVO[")
				.append("importJob=").append(importJob)
				.append("]")
				.toString();
	}

	// fluent

	public ImportJobCreateEventPayloadVO importJob(ImportJobVO newImportJob) {
		this.importJob = newImportJob;
		return this;
	}

	// getter/setter

	public ImportJobVO getImportJob() {
		return importJob;
	}

	public void setImportJob(ImportJobVO newImportJob) {
		this.importJob = newImportJob;
	}
}
