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
 * Abstract class describing the common set of attributes shared by all concrete
 * software resources (e.g. API, InstalledSoftware).
 */
@EqualsAndHashCode(callSuper = true)
@MappingEnabled(entityType = SoftwareResource.TYPE_SOFTWARE_RESOURCE)
public class SoftwareResource extends LogicalResource {

	public static final String TYPE_SOFTWARE_RESOURCE = "software-resource";

	@Getter(onMethod = @__({ @AttributeGetter(value = AttributeType.PROPERTY, targetName = "isDistributedCurrent") }))
	@Setter(onMethod = @__({ @AttributeSetter(value = AttributeType.PROPERTY, targetName = "isDistributedCurrent") }))
	private Boolean isDistributedCurrent;

	@Getter(onMethod = @__({ @AttributeGetter(value = AttributeType.PROPERTY, targetName = "lastUpdate") }))
	@Setter(onMethod = @__({ @AttributeSetter(value = AttributeType.PROPERTY, targetName = "lastUpdate") }))
	private Instant lastUpdate;

	@Getter(onMethod = @__({ @AttributeGetter(value = AttributeType.PROPERTY, targetName = "targetPlatform") }))
	@Setter(onMethod = @__({ @AttributeSetter(value = AttributeType.PROPERTY, targetName = "targetPlatform") }))
	private String targetPlatform;

	/**
	 * Create a new SoftwareResource with the default entity type.
	 *
	 * @param id the entity id
	 */
	public SoftwareResource(String id) {
		super(TYPE_SOFTWARE_RESOURCE, id);
	}

	/**
	 * Protected constructor for sub-types to specify their own NGSI-LD entity type.
	 *
	 * @param type the NGSI-LD entity type
	 * @param id   the entity id
	 */
	protected SoftwareResource(String type, String id) {
		super(type, id);
	}

	@Override
	public List<String> getReferencedTypes() {
		return new ArrayList<>(List.of(TYPE_SOFTWARE_RESOURCE));
	}
}
