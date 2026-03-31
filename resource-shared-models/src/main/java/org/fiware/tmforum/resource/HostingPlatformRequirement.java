package org.fiware.tmforum.resource;

import lombok.EqualsAndHashCode;
import io.github.wistefan.mapping.annotations.MappingEnabled;

import java.util.ArrayList;
import java.util.List;

/**
 * A HostingPlatformRequirement implements a HostingPlatformRequirementSpecification
 * for a specific InstalledSoftware. It is a sub-type of LogicalResource with no additional fields.
 */
@EqualsAndHashCode(callSuper = true)
@MappingEnabled(entityType = HostingPlatformRequirement.TYPE_HOSTING_PLATFORM_REQUIREMENT)
public class HostingPlatformRequirement extends LogicalResource {

	public static final String TYPE_HOSTING_PLATFORM_REQUIREMENT = "hosting-platform-requirement";

	/**
	 * Create a new HostingPlatformRequirement.
	 *
	 * @param id the entity id
	 */
	public HostingPlatformRequirement(String id) {
		super(TYPE_HOSTING_PLATFORM_REQUIREMENT, id);
	}

	@Override
	public List<String> getReferencedTypes() {
		return new ArrayList<>(List.of(TYPE_HOSTING_PLATFORM_REQUIREMENT));
	}
}
