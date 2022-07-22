package org.fiware.tmforum.common.domain;

import lombok.Getter;
import lombok.Setter;
import org.fiware.tmforum.mapping.annotations.AttributeGetter;
import org.fiware.tmforum.mapping.annotations.AttributeSetter;
import org.fiware.tmforum.mapping.annotations.AttributeType;
import org.fiware.tmforum.mapping.annotations.DatasetId;
import org.fiware.tmforum.mapping.annotations.EntityId;
import org.fiware.tmforum.mapping.annotations.EntityType;
import org.fiware.tmforum.mapping.annotations.Ignore;
import org.fiware.tmforum.mapping.annotations.RelationshipObject;

import javax.annotation.Nullable;
import java.net.URI;

/**
 * Abstract superclass for all entities with an id
 */
public abstract class EntityWithId {

	/**
	 * Type of the entity
	 */
	@Getter(onMethod = @__({@EntityType}))
	final String type;

	/**
	 * Id of the entity. This is the id part of "urn:ngsi-ld:TYPE:ID"
	 */
	@Ignore
	@Getter(onMethod = @__({@EntityId, @RelationshipObject, @DatasetId}))
	@Setter
	URI id;

	protected EntityWithId(String type, String id) {
		this.type = type;
		if (id != null) {
			this.id = URI.create(id);
		}
	}

	/**
	 * When sub-classing, this defines the super-class
	 */
	@Getter(onMethod = @__({@AttributeGetter(value = AttributeType.PROPERTY, targetName = "@baseType")}))
	@Setter(onMethod = @__({@AttributeSetter(value = AttributeType.PROPERTY, targetName = "@baseType")}))
	@Nullable
	String atBaseType;

	/**
	 * A URI to a JSON-Schema file that defines additional attributes and relationships
	 */
	@Getter(onMethod = @__({@AttributeGetter(value = AttributeType.PROPERTY, targetName = "@schemaLocation")}))
	@Setter(onMethod = @__({@AttributeSetter(value = AttributeType.PROPERTY, targetName = "@schemaLocation")}))
	@Nullable
	URI atSchemaLocation;

	/**
	 * When sub-classing, this defines the sub-class entity name.
	 * We cannot use @type, since it clashes with the ngsi-ld type field(e.g. reserved name)
	 */
	@Getter(onMethod = @__({@AttributeGetter(value = AttributeType.PROPERTY, targetName = "tmForumType")}))
	@Setter(onMethod = @__({@AttributeSetter(value = AttributeType.PROPERTY, targetName = "tmForumType")}))
	@Nullable
	String atType;


}
