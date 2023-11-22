package org.fiware.resourcecatalog.model;

@jakarta.annotation.Generated("org.openapitools.codegen.languages.MicronautCodegen")
@io.micronaut.core.annotation.Introspected
public class ResourceCandidateDeleteEventPayloadVO {

	public static final java.lang.String JSON_PROPERTY_RESOURCE_CANDIDATE = "resourceCandidate";

	@com.fasterxml.jackson.annotation.JsonProperty(JSON_PROPERTY_RESOURCE_CANDIDATE)
	@com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
	private ResourceCandidateVO resourceCandidate;

	// methods

	@Override
	public boolean equals(Object object) {
		if (object == this) {
			return true;
		}
		if (object == null || getClass() != object.getClass()) {
			return false;
		}
		ResourceCandidateDeleteEventPayloadVO other = (ResourceCandidateDeleteEventPayloadVO) object;
		return java.util.Objects.equals(resourceCandidate, other.resourceCandidate);
	}

	@Override
	public int hashCode() {
		return java.util.Objects.hash(resourceCandidate);
	}

	@Override
	public java.lang.String toString() {
		return new java.lang.StringBuilder()
				.append("ResourceCandidateDeleteEventPayloadVO[")
				.append("resourceCandidate=").append(resourceCandidate)
				.append("]")
				.toString();
	}

	// fluent

	public ResourceCandidateDeleteEventPayloadVO resourceCandidate(ResourceCandidateVO newResourceCandidate) {
		this.resourceCandidate = newResourceCandidate;
		return this;
	}

	// getter/setter

	public ResourceCandidateVO getResourceCandidate() {
		return resourceCandidate;
	}

	public void setResourceCandidate(ResourceCandidateVO newResourceCandidate) {
		this.resourceCandidate = newResourceCandidate;
	}
}
