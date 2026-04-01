package org.fiware.tmforum.resource;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import io.github.wistefan.mapping.annotations.AttributeGetter;
import io.github.wistefan.mapping.annotations.AttributeSetter;
import io.github.wistefan.mapping.annotations.AttributeType;
import io.github.wistefan.mapping.annotations.MappingEnabled;

import java.util.ArrayList;
import java.util.List;

/**
 * Logic resource is a type of resource that describes the common set of attributes
 * shared by all concrete logical resources (e.g. TPE, MSISDN, IP Addresses) in the inventory.
 */
@EqualsAndHashCode(callSuper = true)
@MappingEnabled(entityType = LogicalResource.TYPE_LOGICAL_RESOURCE)
public class LogicalResource extends Resource {

	public static final String TYPE_LOGICAL_RESOURCE = "logical-resource";

	@Getter(onMethod = @__({ @AttributeGetter(value = AttributeType.PROPERTY, targetName = "value") }))
	@Setter(onMethod = @__({ @AttributeSetter(value = AttributeType.PROPERTY, targetName = "value") }))
	private String value;

	/**
	 * Create a new LogicalResource with the default entity type.
	 *
	 * @param id the entity id
	 */
	public LogicalResource(String id) {
		super(TYPE_LOGICAL_RESOURCE, id);
	}

	/**
	 * Protected constructor for sub-types to specify their own NGSI-LD entity type.
	 *
	 * @param type the NGSI-LD entity type
	 * @param id   the entity id
	 */
	protected LogicalResource(String type, String id) {
		super(type, id);
	}

	@Override
	public List<String> getReferencedTypes() {
		return new ArrayList<>(List.of(TYPE_LOGICAL_RESOURCE));
	}
}
