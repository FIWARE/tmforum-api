package org.fiware.resourcecatalog.model;

@jakarta.annotation.Generated("org.openapitools.codegen.languages.MicronautCodegen")
@io.micronaut.core.annotation.Introspected
public class ExportJobCreateEventPayloadVO {

	public static final java.lang.String JSON_PROPERTY_EXPORT_JOB = "exportJob";

	@com.fasterxml.jackson.annotation.JsonProperty(JSON_PROPERTY_EXPORT_JOB)
	@com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
	private ExportJobVO exportJob;

	// methods

	@Override
	public boolean equals(Object object) {
		if (object == this) {
			return true;
		}
		if (object == null || getClass() != object.getClass()) {
			return false;
		}
		ExportJobCreateEventPayloadVO other = (ExportJobCreateEventPayloadVO) object;
		return java.util.Objects.equals(exportJob, other.exportJob);
	}

	@Override
	public int hashCode() {
		return java.util.Objects.hash(exportJob);
	}

	@Override
	public java.lang.String toString() {
		return new java.lang.StringBuilder()
				.append("ExportJobCreateEventPayloadVO[")
				.append("exportJob=").append(exportJob)
				.append("]")
				.toString();
	}

	// fluent

	public ExportJobCreateEventPayloadVO exportJob(ExportJobVO newExportJob) {
		this.exportJob = newExportJob;
		return this;
	}

	// getter/setter

	public ExportJobVO getExportJob() {
		return exportJob;
	}

	public void setExportJob(ExportJobVO newExportJob) {
		this.exportJob = newExportJob;
	}
}
