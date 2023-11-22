package org.fiware.servicecatalog.model;

@jakarta.annotation.Generated("org.openapitools.codegen.languages.MicronautCodegen")
@io.micronaut.core.annotation.Introspected
public class ServiceCategoryDeleteEventPayloadVO {

	public static final java.lang.String JSON_PROPERTY_SERVICE_CATEGORY = "serviceCategory";

	@com.fasterxml.jackson.annotation.JsonProperty(JSON_PROPERTY_SERVICE_CATEGORY)
	@com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
	private ServiceCategoryVO serviceCategory;

	// methods

	@Override
	public boolean equals(Object object) {
		if (object == this) {
			return true;
		}
		if (object == null || getClass() != object.getClass()) {
			return false;
		}
		ServiceCategoryDeleteEventPayloadVO other = (ServiceCategoryDeleteEventPayloadVO) object;
		return java.util.Objects.equals(serviceCategory, other.serviceCategory);
	}

	@Override
	public int hashCode() {
		return java.util.Objects.hash(serviceCategory);
	}

	@Override
	public java.lang.String toString() {
		return new java.lang.StringBuilder()
				.append("ServiceCategoryDeleteEventPayloadVO[")
				.append("serviceCategory=").append(serviceCategory)
				.append("]")
				.toString();
	}

	// fluent

	public ServiceCategoryDeleteEventPayloadVO serviceCategory(ServiceCategoryVO newServiceCategory) {
		this.serviceCategory = newServiceCategory;
		return this;
	}

	// getter/setter

	public ServiceCategoryVO getServiceCategory() {
		return serviceCategory;
	}

	public void setServiceCategory(ServiceCategoryVO newServiceCategory) {
		this.serviceCategory = newServiceCategory;
	}
}
