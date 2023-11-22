package org.fiware.servicecatalog.model;

@jakarta.annotation.Generated("org.openapitools.codegen.languages.MicronautCodegen")
@io.micronaut.core.annotation.Introspected
public class EntitySpecificationRelationshipVO {

	public static final java.lang.String JSON_PROPERTY_ID = "id";
	public static final java.lang.String JSON_PROPERTY_HREF = "href";
	public static final java.lang.String JSON_PROPERTY_NAME = "name";
	public static final java.lang.String JSON_PROPERTY_RELATIONSHIP_TYPE = "relationshipType";
	public static final java.lang.String JSON_PROPERTY_ROLE = "role";
	public static final java.lang.String JSON_PROPERTY_ASSOCIATION_SPEC = "associationSpec";
	public static final java.lang.String JSON_PROPERTY_VALID_FOR = "validFor";
	public static final java.lang.String JSON_PROPERTY_AT_BASE_TYPE = "@baseType";
	public static final java.lang.String JSON_PROPERTY_AT_SCHEMA_LOCATION = "@schemaLocation";
	public static final java.lang.String JSON_PROPERTY_AT_TYPE = "@type";
	public static final java.lang.String JSON_PROPERTY_AT_REFERRED_TYPE = "@referredType";

	/** unique identifier */
	@com.fasterxml.jackson.annotation.JsonProperty(JSON_PROPERTY_ID)
	@com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
	private java.lang.String id;

	/** Hyperlink reference */
	@com.fasterxml.jackson.annotation.JsonProperty(JSON_PROPERTY_HREF)
	@com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
	private java.net.URI href;

	/** Name of the related entity. */
	@com.fasterxml.jackson.annotation.JsonProperty(JSON_PROPERTY_NAME)
	@com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
	private java.lang.String name;

	/** Type of relationship such as migration, substitution, dependency, exclusivity */
	@com.fasterxml.jackson.annotation.JsonProperty(JSON_PROPERTY_RELATIONSHIP_TYPE)
	@com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.ALWAYS)
	private java.lang.String relationshipType;

	/** The association role for this entity specification */
	@com.fasterxml.jackson.annotation.JsonProperty(JSON_PROPERTY_ROLE)
	@com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
	private java.lang.String role;

	@com.fasterxml.jackson.annotation.JsonProperty(JSON_PROPERTY_ASSOCIATION_SPEC)
	@com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
	private AssociationSpecificationRefVO associationSpec;

	@com.fasterxml.jackson.annotation.JsonProperty(JSON_PROPERTY_VALID_FOR)
	@com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
	private TimePeriodVO validFor;

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

	/** The actual type of the target instance when needed for disambiguation. */
	@com.fasterxml.jackson.annotation.JsonProperty(JSON_PROPERTY_AT_REFERRED_TYPE)
	@com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
	private java.lang.String atReferredType;

	// methods

	@Override
	public boolean equals(Object object) {
		if (object == this) {
			return true;
		}
		if (object == null || getClass() != object.getClass()) {
			return false;
		}
		EntitySpecificationRelationshipVO other = (EntitySpecificationRelationshipVO) object;
		return java.util.Objects.equals(id, other.id)
				&& java.util.Objects.equals(href, other.href)
				&& java.util.Objects.equals(name, other.name)
				&& java.util.Objects.equals(relationshipType, other.relationshipType)
				&& java.util.Objects.equals(role, other.role)
				&& java.util.Objects.equals(associationSpec, other.associationSpec)
				&& java.util.Objects.equals(validFor, other.validFor)
				&& java.util.Objects.equals(atBaseType, other.atBaseType)
				&& java.util.Objects.equals(atSchemaLocation, other.atSchemaLocation)
				&& java.util.Objects.equals(atType, other.atType)
				&& java.util.Objects.equals(atReferredType, other.atReferredType);
	}

	@Override
	public int hashCode() {
		return java.util.Objects.hash(id, href, name, relationshipType, role, associationSpec, validFor, atBaseType, atSchemaLocation, atType, atReferredType);
	}

	@Override
	public java.lang.String toString() {
		return new java.lang.StringBuilder()
				.append("EntitySpecificationRelationshipVO[")
				.append("id=").append(id).append(",")
				.append("href=").append(href).append(",")
				.append("name=").append(name).append(",")
				.append("relationshipType=").append(relationshipType).append(",")
				.append("role=").append(role).append(",")
				.append("associationSpec=").append(associationSpec).append(",")
				.append("validFor=").append(validFor).append(",")
				.append("atBaseType=").append(atBaseType).append(",")
				.append("atSchemaLocation=").append(atSchemaLocation).append(",")
				.append("atType=").append(atType).append(",")
				.append("atReferredType=").append(atReferredType)
				.append("]")
				.toString();
	}

	// fluent

	public EntitySpecificationRelationshipVO id(java.lang.String newId) {
		this.id = newId;
		return this;
	}

	public EntitySpecificationRelationshipVO href(java.net.URI newHref) {
		this.href = newHref;
		return this;
	}

	public EntitySpecificationRelationshipVO name(java.lang.String newName) {
		this.name = newName;
		return this;
	}

	public EntitySpecificationRelationshipVO relationshipType(java.lang.String newRelationshipType) {
		this.relationshipType = newRelationshipType;
		return this;
	}

	public EntitySpecificationRelationshipVO role(java.lang.String newRole) {
		this.role = newRole;
		return this;
	}

	public EntitySpecificationRelationshipVO associationSpec(AssociationSpecificationRefVO newAssociationSpec) {
		this.associationSpec = newAssociationSpec;
		return this;
	}

	public EntitySpecificationRelationshipVO validFor(TimePeriodVO newValidFor) {
		this.validFor = newValidFor;
		return this;
	}

	public EntitySpecificationRelationshipVO atBaseType(java.lang.String newAtBaseType) {
		this.atBaseType = newAtBaseType;
		return this;
	}

	public EntitySpecificationRelationshipVO atSchemaLocation(java.net.URI newAtSchemaLocation) {
		this.atSchemaLocation = newAtSchemaLocation;
		return this;
	}

	public EntitySpecificationRelationshipVO atType(java.lang.String newAtType) {
		this.atType = newAtType;
		return this;
	}

	public EntitySpecificationRelationshipVO atReferredType(java.lang.String newAtReferredType) {
		this.atReferredType = newAtReferredType;
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

	public java.lang.String getName() {
		return name;
	}

	public void setName(java.lang.String newName) {
		this.name = newName;
	}

	public java.lang.String getRelationshipType() {
		return relationshipType;
	}

	public void setRelationshipType(java.lang.String newRelationshipType) {
		this.relationshipType = newRelationshipType;
	}

	public java.lang.String getRole() {
		return role;
	}

	public void setRole(java.lang.String newRole) {
		this.role = newRole;
	}

	public AssociationSpecificationRefVO getAssociationSpec() {
		return associationSpec;
	}

	public void setAssociationSpec(AssociationSpecificationRefVO newAssociationSpec) {
		this.associationSpec = newAssociationSpec;
	}

	public TimePeriodVO getValidFor() {
		return validFor;
	}

	public void setValidFor(TimePeriodVO newValidFor) {
		this.validFor = newValidFor;
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

	public java.lang.String getAtReferredType() {
		return atReferredType;
	}

	public void setAtReferredType(java.lang.String newAtReferredType) {
		this.atReferredType = newAtReferredType;
	}
}
