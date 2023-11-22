package org.fiware.resourcecatalog.model;

@jakarta.annotation.Generated("org.openapitools.codegen.languages.MicronautCodegen")
@io.micronaut.core.annotation.Introspected
public class ResourceCatalogDeleteEventPayloadVO {

	public static final java.lang.String JSON_PROPERTY_RESOURCE_CATALOG = "resourceCatalog";

	@com.fasterxml.jackson.annotation.JsonProperty(JSON_PROPERTY_RESOURCE_CATALOG)
	@com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
	private ResourceCatalogVO resourceCatalog;

	// methods

	@Override
	public boolean equals(Object object) {
		if (object == this) {
			return true;
		}
		if (object == null || getClass() != object.getClass()) {
			return false;
		}
		ResourceCatalogDeleteEventPayloadVO other = (ResourceCatalogDeleteEventPayloadVO) object;
		return java.util.Objects.equals(resourceCatalog, other.resourceCatalog);
	}

	@Override
	public int hashCode() {
		return java.util.Objects.hash(resourceCatalog);
	}

	@Override
	public java.lang.String toString() {
		return new java.lang.StringBuilder()
				.append("ResourceCatalogDeleteEventPayloadVO[")
				.append("resourceCatalog=").append(resourceCatalog)
				.append("]")
				.toString();
	}

	// fluent

	public ResourceCatalogDeleteEventPayloadVO resourceCatalog(ResourceCatalogVO newResourceCatalog) {
		this.resourceCatalog = newResourceCatalog;
		return this;
	}

	// getter/setter

	public ResourceCatalogVO getResourceCatalog() {
		return resourceCatalog;
	}

	public void setResourceCatalog(ResourceCatalogVO newResourceCatalog) {
		this.resourceCatalog = newResourceCatalog;
	}
}
