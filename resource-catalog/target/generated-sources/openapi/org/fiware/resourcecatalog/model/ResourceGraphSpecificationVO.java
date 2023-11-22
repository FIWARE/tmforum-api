package org.fiware.resourcecatalog.model;

@jakarta.annotation.Generated("org.openapitools.codegen.languages.MicronautCodegen")
@io.micronaut.core.annotation.Introspected
public class ResourceGraphSpecificationVO {

	public static final java.lang.String JSON_PROPERTY_ID = "id";
	public static final java.lang.String JSON_PROPERTY_HREF = "href";
	public static final java.lang.String JSON_PROPERTY_DESCRIPTION = "description";
	public static final java.lang.String JSON_PROPERTY_NAME = "name";
	public static final java.lang.String JSON_PROPERTY_CONNECTION_SPECIFICATION = "connectionSpecification";
	public static final java.lang.String JSON_PROPERTY_GRAPH_SPECIFICATION_RELATIONSHIP = "graphSpecificationRelationship";
	public static final java.lang.String JSON_PROPERTY_AT_BASE_TYPE = "@baseType";
	public static final java.lang.String JSON_PROPERTY_AT_SCHEMA_LOCATION = "@schemaLocation";
	public static final java.lang.String JSON_PROPERTY_AT_TYPE = "@type";

	/** Unique identifier of the resource graph specification. */
	@com.fasterxml.jackson.annotation.JsonProperty(JSON_PROPERTY_ID)
	@com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
	private java.lang.String id;

	/** Hyperlink reference */
	@com.fasterxml.jackson.annotation.JsonProperty(JSON_PROPERTY_HREF)
	@com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
	private java.net.URI href;

	/** Description of the resource graph specification. */
	@com.fasterxml.jackson.annotation.JsonProperty(JSON_PROPERTY_DESCRIPTION)
	@com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
	private java.lang.String description;

	/** Descriptive name for the resource graph specification. */
	@com.fasterxml.jackson.annotation.JsonProperty(JSON_PROPERTY_NAME)
	@com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
	private java.lang.String name;

	/** Resource graph edge specifications. */
	@com.fasterxml.jackson.annotation.JsonProperty(JSON_PROPERTY_CONNECTION_SPECIFICATION)
	@com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.ALWAYS)
	private java.util.List<ConnectionSpecificationVO> connectionSpecification = new java.util.ArrayList<>();

	/** Relationships to other resource graph specifications. */
	@com.fasterxml.jackson.annotation.JsonProperty(JSON_PROPERTY_GRAPH_SPECIFICATION_RELATIONSHIP)
	@com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
	private java.util.List<ResourceGraphSpecificationRelationshipVO> graphSpecificationRelationship;

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
		ResourceGraphSpecificationVO other = (ResourceGraphSpecificationVO) object;
		return java.util.Objects.equals(id, other.id)
				&& java.util.Objects.equals(href, other.href)
				&& java.util.Objects.equals(description, other.description)
				&& java.util.Objects.equals(name, other.name)
				&& java.util.Objects.equals(connectionSpecification, other.connectionSpecification)
				&& java.util.Objects.equals(graphSpecificationRelationship, other.graphSpecificationRelationship)
				&& java.util.Objects.equals(atBaseType, other.atBaseType)
				&& java.util.Objects.equals(atSchemaLocation, other.atSchemaLocation)
				&& java.util.Objects.equals(atType, other.atType);
	}

	@Override
	public int hashCode() {
		return java.util.Objects.hash(id, href, description, name, connectionSpecification, graphSpecificationRelationship, atBaseType, atSchemaLocation, atType);
	}

	@Override
	public java.lang.String toString() {
		return new java.lang.StringBuilder()
				.append("ResourceGraphSpecificationVO[")
				.append("id=").append(id).append(",")
				.append("href=").append(href).append(",")
				.append("description=").append(description).append(",")
				.append("name=").append(name).append(",")
				.append("connectionSpecification=").append(connectionSpecification).append(",")
				.append("graphSpecificationRelationship=").append(graphSpecificationRelationship).append(",")
				.append("atBaseType=").append(atBaseType).append(",")
				.append("atSchemaLocation=").append(atSchemaLocation).append(",")
				.append("atType=").append(atType)
				.append("]")
				.toString();
	}

	// fluent

	public ResourceGraphSpecificationVO id(java.lang.String newId) {
		this.id = newId;
		return this;
	}

	public ResourceGraphSpecificationVO href(java.net.URI newHref) {
		this.href = newHref;
		return this;
	}

	public ResourceGraphSpecificationVO description(java.lang.String newDescription) {
		this.description = newDescription;
		return this;
	}

	public ResourceGraphSpecificationVO name(java.lang.String newName) {
		this.name = newName;
		return this;
	}

	public ResourceGraphSpecificationVO connectionSpecification(java.util.List<ConnectionSpecificationVO> newConnectionSpecification) {
		this.connectionSpecification = newConnectionSpecification;
		return this;
	}
	
	public ResourceGraphSpecificationVO addConnectionSpecificationItem(ConnectionSpecificationVO connectionSpecificationItem) {
		if (this.connectionSpecification == null) {
			this.connectionSpecification = new java.util.ArrayList<>();
		}
		this.connectionSpecification.add(connectionSpecificationItem);
		return this;
	}

	public ResourceGraphSpecificationVO removeConnectionSpecificationItem(ConnectionSpecificationVO connectionSpecificationItem) {
		if (this.connectionSpecification != null) {
			this.connectionSpecification.remove(connectionSpecificationItem);
		}
		return this;
	}

	public ResourceGraphSpecificationVO graphSpecificationRelationship(java.util.List<ResourceGraphSpecificationRelationshipVO> newGraphSpecificationRelationship) {
		this.graphSpecificationRelationship = newGraphSpecificationRelationship;
		return this;
	}
	
	public ResourceGraphSpecificationVO addGraphSpecificationRelationshipItem(ResourceGraphSpecificationRelationshipVO graphSpecificationRelationshipItem) {
		if (this.graphSpecificationRelationship == null) {
			this.graphSpecificationRelationship = new java.util.ArrayList<>();
		}
		this.graphSpecificationRelationship.add(graphSpecificationRelationshipItem);
		return this;
	}

	public ResourceGraphSpecificationVO removeGraphSpecificationRelationshipItem(ResourceGraphSpecificationRelationshipVO graphSpecificationRelationshipItem) {
		if (this.graphSpecificationRelationship != null) {
			this.graphSpecificationRelationship.remove(graphSpecificationRelationshipItem);
		}
		return this;
	}

	public ResourceGraphSpecificationVO atBaseType(java.lang.String newAtBaseType) {
		this.atBaseType = newAtBaseType;
		return this;
	}

	public ResourceGraphSpecificationVO atSchemaLocation(java.net.URI newAtSchemaLocation) {
		this.atSchemaLocation = newAtSchemaLocation;
		return this;
	}

	public ResourceGraphSpecificationVO atType(java.lang.String newAtType) {
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

	public java.lang.String getDescription() {
		return description;
	}

	public void setDescription(java.lang.String newDescription) {
		this.description = newDescription;
	}

	public java.lang.String getName() {
		return name;
	}

	public void setName(java.lang.String newName) {
		this.name = newName;
	}

	public java.util.List<ConnectionSpecificationVO> getConnectionSpecification() {
		return connectionSpecification;
	}

	public void setConnectionSpecification(java.util.List<ConnectionSpecificationVO> newConnectionSpecification) {
		this.connectionSpecification = newConnectionSpecification;
	}

	public java.util.List<ResourceGraphSpecificationRelationshipVO> getGraphSpecificationRelationship() {
		return graphSpecificationRelationship;
	}

	public void setGraphSpecificationRelationship(java.util.List<ResourceGraphSpecificationRelationshipVO> newGraphSpecificationRelationship) {
		this.graphSpecificationRelationship = newGraphSpecificationRelationship;
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
