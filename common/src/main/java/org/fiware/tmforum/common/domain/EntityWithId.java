package org.fiware.tmforum.common.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.github.wistefan.mapping.annotations.AttributeGetter;
import io.github.wistefan.mapping.annotations.AttributeSetter;
import io.github.wistefan.mapping.annotations.AttributeType;
import io.github.wistefan.mapping.annotations.DatasetId;
import io.github.wistefan.mapping.annotations.EntityId;
import io.github.wistefan.mapping.annotations.EntityType;
import io.github.wistefan.mapping.annotations.Ignore;
import io.github.wistefan.mapping.annotations.RelationshipObject;
import lombok.Getter;
import lombok.Setter;
import org.fiware.tmforum.common.mapping.IdHelper;
import javax.annotation.Nullable;
import java.net.URI;

/**
 * Abstract superclass for all entities with an id
 */
public abstract class EntityWithId {

	/**
	 * Type of the entity
	 */
	@Getter(onMethod = @__({ @EntityType })) final String type;

	/**
	 * Id of the entity. This is the id part of "urn:ngsi-ld:TYPE:ID"
	 */
	@Ignore
	@Getter(onMethod = @__({ @EntityId, @RelationshipObject, @DatasetId }))
	@Setter
	URI id;

	protected EntityWithId(String type, String id) {
		this.type = type;
		if (IdHelper.isNgsiLdId(id)) {
			this.id = URI.create(id);
		} else {
			this.id = IdHelper.toNgsiLd(id, type);
		}
	}

	/**
	 * When sub-classing, this defines the super-class
	 */
	@Getter(onMethod = @__({ @AttributeGetter(value = AttributeType.PROPERTY, targetName = "atBaseType") }))
	@Setter(onMethod = @__({ @AttributeSetter(value = AttributeType.PROPERTY, targetName = "atBaseType") }))
	@Nullable
	String atBaseType;

	/**
	 * A URI to a JSON-Schema file that defines additional attributes and relationships
	 */
	@Getter(onMethod = @__({ @AttributeGetter(value = AttributeType.PROPERTY, targetName = "atSchemaLocation") }))
	@Setter(onMethod = @__({ @AttributeSetter(value = AttributeType.PROPERTY, targetName = "atSchemaLocation") }))
	@Nullable
	URI atSchemaLocation;

	/**
	 * When sub-classing, this defines the sub-class entity name.
	 * We cannot use @type, since it clashes with the ngsi-ld type field(e.g. reserved name)
	 */
	@Getter(onMethod = @__({ @AttributeGetter(value = AttributeType.PROPERTY, targetName = "tmForumType") }))
	@Setter(onMethod = @__({ @AttributeSetter(value = AttributeType.PROPERTY, targetName = "tmForumType") }))
	@Nullable
	String atType;

	@JsonIgnore
	public String getEntityState() {
		return "default";
	}

}
