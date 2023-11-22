package org.fiware.resourcecatalog.model;

@jakarta.annotation.Generated("org.openapitools.codegen.languages.MicronautCodegen")
@io.micronaut.core.annotation.Introspected
public class ResourceSpecificationCreateEventPayloadVO {

	public static final java.lang.String JSON_PROPERTY_RESOURCE_SPECIFICATION = "resourceSpecification";

	@com.fasterxml.jackson.annotation.JsonProperty(JSON_PROPERTY_RESOURCE_SPECIFICATION)
	@com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
	private ResourceSpecificationVO resourceSpecification;

	// methods

	@Override
	public boolean equals(Object object) {
		if (object == this) {
			return true;
		}
		if (object == null || getClass() != object.getClass()) {
			return false;
		}
		ResourceSpecificationCreateEventPayloadVO other = (ResourceSpecificationCreateEventPayloadVO) object;
		return java.util.Objects.equals(resourceSpecification, other.resourceSpecification);
	}

	@Override
	public int hashCode() {
		return java.util.Objects.hash(resourceSpecification);
	}

	@Override
	public java.lang.String toString() {
		return new java.lang.StringBuilder()
				.append("ResourceSpecificationCreateEventPayloadVO[")
				.append("resourceSpecification=").append(resourceSpecification)
				.append("]")
				.toString();
	}

	// fluent

	public ResourceSpecificationCreateEventPayloadVO resourceSpecification(ResourceSpecificationVO newResourceSpecification) {
		this.resourceSpecification = newResourceSpecification;
		return this;
	}

	// getter/setter

	public ResourceSpecificationVO getResourceSpecification() {
		return resourceSpecification;
	}

	public void setResourceSpecification(ResourceSpecificationVO newResourceSpecification) {
		this.resourceSpecification = newResourceSpecification;
	}
}
