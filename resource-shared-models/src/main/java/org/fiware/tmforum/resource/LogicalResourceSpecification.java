package org.fiware.tmforum.resource;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import io.github.wistefan.mapping.annotations.AttributeGetter;
import io.github.wistefan.mapping.annotations.AttributeSetter;
import io.github.wistefan.mapping.annotations.AttributeType;
import io.github.wistefan.mapping.annotations.MappingEnabled;

import java.util.List;

/**
 * This is a derived class of ResourceSpecification, and is used to define the invariant
 * characteristics and behavior (attributes, methods, constraints, and relationships) of a LogicalResource.
 */
@EqualsAndHashCode(callSuper = true)
@MappingEnabled(entityType = LogicalResourceSpecification.TYPE_LOGICAL_RESOURCE_SPECIFICATION)
public class LogicalResourceSpecification extends ResourceSpecification {

	public static final String TYPE_LOGICAL_RESOURCE_SPECIFICATION = "logical-resource-specification";

	@Getter(onMethod = @__({
			@AttributeGetter(value = AttributeType.PROPERTY_LIST, targetName = "resourceSpecRelationship") }))
	@Setter(onMethod = @__({
			@AttributeSetter(value = AttributeType.PROPERTY_LIST, targetName = "resourceSpecRelationship", targetClass = ResourceSpecificationRelationship.class) }))
	private List<ResourceSpecificationRelationship> resourceSpecRelationship;

	/**
	 * Create a new LogicalResourceSpecification with the default entity type.
	 *
	 * @param id the entity id
	 */
	public LogicalResourceSpecification(String id) {
		super(TYPE_LOGICAL_RESOURCE_SPECIFICATION, id);
	}

	/**
	 * Protected constructor for sub-types to specify their own NGSI-LD entity type.
	 *
	 * @param type the NGSI-LD entity type
	 * @param id   the entity id
	 */
	protected LogicalResourceSpecification(String type, String id) {
		super(type, id);
	}

	@Override
	public String getEntityState() {
		return getLifecycleStatus();
	}
}
