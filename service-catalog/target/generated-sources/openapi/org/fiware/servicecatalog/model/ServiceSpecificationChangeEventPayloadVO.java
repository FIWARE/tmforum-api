package org.fiware.servicecatalog.model;

@jakarta.annotation.Generated("org.openapitools.codegen.languages.MicronautCodegen")
@io.micronaut.core.annotation.Introspected
public class ServiceSpecificationChangeEventPayloadVO {

	public static final java.lang.String JSON_PROPERTY_SERVICE_SPECIFICATION = "serviceSpecification";

	@com.fasterxml.jackson.annotation.JsonProperty(JSON_PROPERTY_SERVICE_SPECIFICATION)
	@com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
	private ServiceSpecificationVO serviceSpecification;

	// methods

	@Override
	public boolean equals(Object object) {
		if (object == this) {
			return true;
		}
		if (object == null || getClass() != object.getClass()) {
			return false;
		}
		ServiceSpecificationChangeEventPayloadVO other = (ServiceSpecificationChangeEventPayloadVO) object;
		return java.util.Objects.equals(serviceSpecification, other.serviceSpecification);
	}

	@Override
	public int hashCode() {
		return java.util.Objects.hash(serviceSpecification);
	}

	@Override
	public java.lang.String toString() {
		return new java.lang.StringBuilder()
				.append("ServiceSpecificationChangeEventPayloadVO[")
				.append("serviceSpecification=").append(serviceSpecification)
				.append("]")
				.toString();
	}

	// fluent

	public ServiceSpecificationChangeEventPayloadVO serviceSpecification(ServiceSpecificationVO newServiceSpecification) {
		this.serviceSpecification = newServiceSpecification;
		return this;
	}

	// getter/setter

	public ServiceSpecificationVO getServiceSpecification() {
		return serviceSpecification;
	}

	public void setServiceSpecification(ServiceSpecificationVO newServiceSpecification) {
		this.serviceSpecification = newServiceSpecification;
	}
}
