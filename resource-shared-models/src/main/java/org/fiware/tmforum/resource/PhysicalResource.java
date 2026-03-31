package org.fiware.tmforum.resource;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import io.github.wistefan.mapping.annotations.AttributeGetter;
import io.github.wistefan.mapping.annotations.AttributeSetter;
import io.github.wistefan.mapping.annotations.AttributeType;
import io.github.wistefan.mapping.annotations.MappingEnabled;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Physical resource is a type of resource that describes the common set of attributes
 * shared by all concrete physical resources (e.g. EQUIPMENT) in the inventory.
 */
@EqualsAndHashCode(callSuper = true)
@MappingEnabled(entityType = PhysicalResource.TYPE_PHYSICAL_RESOURCE)
public class PhysicalResource extends Resource {

	public static final String TYPE_PHYSICAL_RESOURCE = "physical-resource";

	@Getter(onMethod = @__({ @AttributeGetter(value = AttributeType.PROPERTY, targetName = "manufactureDate") }))
	@Setter(onMethod = @__({ @AttributeSetter(value = AttributeType.PROPERTY, targetName = "manufactureDate") }))
	private Instant manufactureDate;

	@Getter(onMethod = @__({ @AttributeGetter(value = AttributeType.PROPERTY, targetName = "powerState") }))
	@Setter(onMethod = @__({ @AttributeSetter(value = AttributeType.PROPERTY, targetName = "powerState") }))
	private String powerState;

	@Getter(onMethod = @__({ @AttributeGetter(value = AttributeType.PROPERTY, targetName = "serialNumber") }))
	@Setter(onMethod = @__({ @AttributeSetter(value = AttributeType.PROPERTY, targetName = "serialNumber") }))
	private String serialNumber;

	@Getter(onMethod = @__({ @AttributeGetter(value = AttributeType.PROPERTY, targetName = "versionNumber") }))
	@Setter(onMethod = @__({ @AttributeSetter(value = AttributeType.PROPERTY, targetName = "versionNumber") }))
	private String versionNumber;

	/**
	 * Create a new PhysicalResource with the default entity type.
	 *
	 * @param id the entity id
	 */
	public PhysicalResource(String id) {
		super(TYPE_PHYSICAL_RESOURCE, id);
	}

	/**
	 * Protected constructor for sub-types to specify their own NGSI-LD entity type.
	 *
	 * @param type the NGSI-LD entity type
	 * @param id   the entity id
	 */
	protected PhysicalResource(String type, String id) {
		super(type, id);
	}

	@Override
	public List<String> getReferencedTypes() {
		return new ArrayList<>(List.of(TYPE_PHYSICAL_RESOURCE));
	}
}
