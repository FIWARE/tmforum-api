package org.fiware.servicecatalog.model;

@jakarta.annotation.Generated("org.openapitools.codegen.languages.MicronautCodegen")
@io.micronaut.core.annotation.Introspected
public class ServiceCandidateCreateEventPayloadVO {

	public static final java.lang.String JSON_PROPERTY_SERVICE_CANDIDATE = "serviceCandidate";

	@com.fasterxml.jackson.annotation.JsonProperty(JSON_PROPERTY_SERVICE_CANDIDATE)
	@com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
	private ServiceCandidateVO serviceCandidate;

	// methods

	@Override
	public boolean equals(Object object) {
		if (object == this) {
			return true;
		}
		if (object == null || getClass() != object.getClass()) {
			return false;
		}
		ServiceCandidateCreateEventPayloadVO other = (ServiceCandidateCreateEventPayloadVO) object;
		return java.util.Objects.equals(serviceCandidate, other.serviceCandidate);
	}

	@Override
	public int hashCode() {
		return java.util.Objects.hash(serviceCandidate);
	}

	@Override
	public java.lang.String toString() {
		return new java.lang.StringBuilder()
				.append("ServiceCandidateCreateEventPayloadVO[")
				.append("serviceCandidate=").append(serviceCandidate)
				.append("]")
				.toString();
	}

	// fluent

	public ServiceCandidateCreateEventPayloadVO serviceCandidate(ServiceCandidateVO newServiceCandidate) {
		this.serviceCandidate = newServiceCandidate;
		return this;
	}

	// getter/setter

	public ServiceCandidateVO getServiceCandidate() {
		return serviceCandidate;
	}

	public void setServiceCandidate(ServiceCandidateVO newServiceCandidate) {
		this.serviceCandidate = newServiceCandidate;
	}
}
