package org.fiware.tmforum.resource;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import io.github.wistefan.mapping.annotations.AttributeGetter;
import io.github.wistefan.mapping.annotations.AttributeSetter;
import io.github.wistefan.mapping.annotations.AttributeType;
import io.github.wistefan.mapping.annotations.MappingEnabled;

/**
 * A base class that is used to define the invariant characteristics and behavior
 * of a HostingPlatformRequirement Resource.
 */
@EqualsAndHashCode(callSuper = true)
@MappingEnabled(entityType = HostingPlatformRequirementSpecification.TYPE_HOSTING_PLATFORM_REQUIREMENT_SPECIFICATION)
public class HostingPlatformRequirementSpecification extends LogicalResourceSpecification {

	public static final String TYPE_HOSTING_PLATFORM_REQUIREMENT_SPECIFICATION =
			"hosting-platform-requirement-specification";

	@Getter(onMethod = @__({ @AttributeGetter(value = AttributeType.PROPERTY, targetName = "isVirtualizable") }))
	@Setter(onMethod = @__({ @AttributeSetter(value = AttributeType.PROPERTY, targetName = "isVirtualizable") }))
	private Boolean isVirtualizable;

	/**
	 * Create a new HostingPlatformRequirementSpecification.
	 *
	 * @param id the entity id
	 */
	public HostingPlatformRequirementSpecification(String id) {
		super(TYPE_HOSTING_PLATFORM_REQUIREMENT_SPECIFICATION, id);
	}
}
