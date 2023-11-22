package org.fiware.resourcecatalog.model;

@jakarta.annotation.Generated("org.openapitools.codegen.languages.MicronautCodegen")
@io.micronaut.core.annotation.Introspected
public class AddressableVO {

	public static final java.lang.String JSON_PROPERTY_ID = "id";
	public static final java.lang.String JSON_PROPERTY_HREF = "href";

	/** unique identifier */
	@com.fasterxml.jackson.annotation.JsonProperty(JSON_PROPERTY_ID)
	@com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
	private java.lang.String id;

	/** Hyperlink reference */
	@com.fasterxml.jackson.annotation.JsonProperty(JSON_PROPERTY_HREF)
	@com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
	private java.net.URI href;

	// methods

	@Override
	public boolean equals(Object object) {
		if (object == this) {
			return true;
		}
		if (object == null || getClass() != object.getClass()) {
			return false;
		}
		AddressableVO other = (AddressableVO) object;
		return java.util.Objects.equals(id, other.id)
				&& java.util.Objects.equals(href, other.href);
	}

	@Override
	public int hashCode() {
		return java.util.Objects.hash(id, href);
	}

	@Override
	public java.lang.String toString() {
		return new java.lang.StringBuilder()
				.append("AddressableVO[")
				.append("id=").append(id).append(",")
				.append("href=").append(href)
				.append("]")
				.toString();
	}

	// fluent

	public AddressableVO id(java.lang.String newId) {
		this.id = newId;
		return this;
	}

	public AddressableVO href(java.net.URI newHref) {
		this.href = newHref;
		return this;
	}

	// getter/setter

	public java.lang.String getId() {
		return id;
	}

	public void setId(java.lang.String newId) {
		this.id = newId;
	}

	public java.net.URI getHref() {
		return href;
	}

	public void setHref(java.net.URI newHref) {
		this.href = newHref;
	}
}
