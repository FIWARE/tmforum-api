package org.fiware.resourcecatalog.model;

@jakarta.annotation.Generated("org.openapitools.codegen.languages.MicronautCodegen")
@io.micronaut.core.annotation.Introspected
public class ResourceCategoryCreateEventPayloadVO {

	public static final java.lang.String JSON_PROPERTY_RESOURCE_CATEGORY = "resourceCategory";

	@com.fasterxml.jackson.annotation.JsonProperty(JSON_PROPERTY_RESOURCE_CATEGORY)
	@com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
	private ResourceCategoryVO resourceCategory;

	// methods

	@Override
	public boolean equals(Object object) {
		if (object == this) {
			return true;
		}
		if (object == null || getClass() != object.getClass()) {
			return false;
		}
		ResourceCategoryCreateEventPayloadVO other = (ResourceCategoryCreateEventPayloadVO) object;
		return java.util.Objects.equals(resourceCategory, other.resourceCategory);
	}

	@Override
	public int hashCode() {
		return java.util.Objects.hash(resourceCategory);
	}

	@Override
	public java.lang.String toString() {
		return new java.lang.StringBuilder()
				.append("ResourceCategoryCreateEventPayloadVO[")
				.append("resourceCategory=").append(resourceCategory)
				.append("]")
				.toString();
	}

	// fluent

	public ResourceCategoryCreateEventPayloadVO resourceCategory(ResourceCategoryVO newResourceCategory) {
		this.resourceCategory = newResourceCategory;
		return this;
	}

	// getter/setter

	public ResourceCategoryVO getResourceCategory() {
		return resourceCategory;
	}

	public void setResourceCategory(ResourceCategoryVO newResourceCategory) {
		this.resourceCategory = newResourceCategory;
	}
}
