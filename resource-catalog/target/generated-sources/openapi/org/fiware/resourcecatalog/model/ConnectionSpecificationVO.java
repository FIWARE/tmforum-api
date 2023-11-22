package org.fiware.resourcecatalog.model;

@jakarta.annotation.Generated("org.openapitools.codegen.languages.MicronautCodegen")
@io.micronaut.core.annotation.Introspected
public class ConnectionSpecificationVO {

	public static final java.lang.String JSON_PROPERTY_ID = "id";
	public static final java.lang.String JSON_PROPERTY_HREF = "href";
	public static final java.lang.String JSON_PROPERTY_ASSOCIATION_TYPE = "associationType";
	public static final java.lang.String JSON_PROPERTY_NAME = "name";
	public static final java.lang.String JSON_PROPERTY_ENDPOINT_SPECIFICATION = "endpointSpecification";
	public static final java.lang.String JSON_PROPERTY_AT_BASE_TYPE = "@baseType";
	public static final java.lang.String JSON_PROPERTY_AT_SCHEMA_LOCATION = "@schemaLocation";
	public static final java.lang.String JSON_PROPERTY_AT_TYPE = "@type";

	/** Unique identifier for graph edge specification. */
	@com.fasterxml.jackson.annotation.JsonProperty(JSON_PROPERTY_ID)
	@com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
	private java.lang.String id;

	/** Hyperlink reference */
	@com.fasterxml.jackson.annotation.JsonProperty(JSON_PROPERTY_HREF)
	@com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
	private java.net.URI href;

	/** Association type. */
	@com.fasterxml.jackson.annotation.JsonProperty(JSON_PROPERTY_ASSOCIATION_TYPE)
	@com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.ALWAYS)
	private java.lang.String associationType;

	/** Descriptive name for graph edge specification. */
	@com.fasterxml.jackson.annotation.JsonProperty(JSON_PROPERTY_NAME)
	@com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
	private java.lang.String name;

	/** Specifications for resource graph vertices connected by this edge. */
	@com.fasterxml.jackson.annotation.JsonProperty(JSON_PROPERTY_ENDPOINT_SPECIFICATION)
	@com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.ALWAYS)
	private java.util.List<EndpointSpecificationRefVO> endpointSpecification = new java.util.ArrayList<>();

	/** When sub-classing, this defines the super-class */
	@com.fasterxml.jackson.annotation.JsonProperty(JSON_PROPERTY_AT_BASE_TYPE)
	@com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
	private java.lang.String atBaseType;

	/** A URI to a JSON-Schema file that defines additional attributes and relationships */
	@com.fasterxml.jackson.annotation.JsonProperty(JSON_PROPERTY_AT_SCHEMA_LOCATION)
	@com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
	private java.net.URI atSchemaLocation;

	/** When sub-classing, this defines the sub-class Extensible name */
	@com.fasterxml.jackson.annotation.JsonProperty(JSON_PROPERTY_AT_TYPE)
	@com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
	private java.lang.String atType;

	// methods

	@Override
	public boolean equals(Object object) {
		if (object == this) {
			return true;
		}
		if (object == null || getClass() != object.getClass()) {
			return false;
		}
		ConnectionSpecificationVO other = (ConnectionSpecificationVO) object;
		return java.util.Objects.equals(id, other.id)
				&& java.util.Objects.equals(href, other.href)
				&& java.util.Objects.equals(associationType, other.associationType)
				&& java.util.Objects.equals(name, other.name)
				&& java.util.Objects.equals(endpointSpecification, other.endpointSpecification)
				&& java.util.Objects.equals(atBaseType, other.atBaseType)
				&& java.util.Objects.equals(atSchemaLocation, other.atSchemaLocation)
				&& java.util.Objects.equals(atType, other.atType);
	}

	@Override
	public int hashCode() {
		return java.util.Objects.hash(id, href, associationType, name, endpointSpecification, atBaseType, atSchemaLocation, atType);
	}

	@Override
	public java.lang.String toString() {
		return new java.lang.StringBuilder()
				.append("ConnectionSpecificationVO[")
				.append("id=").append(id).append(",")
				.append("href=").append(href).append(",")
				.append("associationType=").append(associationType).append(",")
				.append("name=").append(name).append(",")
				.append("endpointSpecification=").append(endpointSpecification).append(",")
				.append("atBaseType=").append(atBaseType).append(",")
				.append("atSchemaLocation=").append(atSchemaLocation).append(",")
				.append("atType=").append(atType)
				.append("]")
				.toString();
	}

	// fluent

	public ConnectionSpecificationVO id(java.lang.String newId) {
		this.id = newId;
		return this;
	}

	public ConnectionSpecificationVO href(java.net.URI newHref) {
		this.href = newHref;
		return this;
	}

	public ConnectionSpecificationVO associationType(java.lang.String newAssociationType) {
		this.associationType = newAssociationType;
		return this;
	}

	public ConnectionSpecificationVO name(java.lang.String newName) {
		this.name = newName;
		return this;
	}

	public ConnectionSpecificationVO endpointSpecification(java.util.List<EndpointSpecificationRefVO> newEndpointSpecification) {
		this.endpointSpecification = newEndpointSpecification;
		return this;
	}
	
	public ConnectionSpecificationVO addEndpointSpecificationItem(EndpointSpecificationRefVO endpointSpecificationItem) {
		if (this.endpointSpecification == null) {
			this.endpointSpecification = new java.util.ArrayList<>();
		}
		this.endpointSpecification.add(endpointSpecificationItem);
		return this;
	}

	public ConnectionSpecificationVO removeEndpointSpecificationItem(EndpointSpecificationRefVO endpointSpecificationItem) {
		if (this.endpointSpecification != null) {
			this.endpointSpecification.remove(endpointSpecificationItem);
		}
		return this;
	}

	public ConnectionSpecificationVO atBaseType(java.lang.String newAtBaseType) {
		this.atBaseType = newAtBaseType;
		return this;
	}

	public ConnectionSpecificationVO atSchemaLocation(java.net.URI newAtSchemaLocation) {
		this.atSchemaLocation = newAtSchemaLocation;
		return this;
	}

	public ConnectionSpecificationVO atType(java.lang.String newAtType) {
		this.atType = newAtType;
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

	public java.lang.String getAssociationType() {
		return associationType;
	}

	public void setAssociationType(java.lang.String newAssociationType) {
		this.associationType = newAssociationType;
	}

	public java.lang.String getName() {
		return name;
	}

	public void setName(java.lang.String newName) {
		this.name = newName;
	}

	public java.util.List<EndpointSpecificationRefVO> getEndpointSpecification() {
		return endpointSpecification;
	}

	public void setEndpointSpecification(java.util.List<EndpointSpecificationRefVO> newEndpointSpecification) {
		this.endpointSpecification = newEndpointSpecification;
	}

	public java.lang.String getAtBaseType() {
		return atBaseType;
	}

	public void setAtBaseType(java.lang.String newAtBaseType) {
		this.atBaseType = newAtBaseType;
	}

	public java.net.URI getAtSchemaLocation() {
		return atSchemaLocation;
	}

	public void setAtSchemaLocation(java.net.URI newAtSchemaLocation) {
		this.atSchemaLocation = newAtSchemaLocation;
	}

	public java.lang.String getAtType() {
		return atType;
	}

	public void setAtType(java.lang.String newAtType) {
		this.atType = newAtType;
	}
}
