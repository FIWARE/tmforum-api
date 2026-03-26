package org.fiware.tmforum.softwaremanagement.rest;

import org.fiware.tmforum.resource.*;

import java.util.Map;
import java.util.Set;

/**
 * Registry mapping @type values (from the TMForum spec) to NGSI-LD entity types
 * and domain classes for Resource and ResourceSpecification sub-types.
 */
public final class ResourceTypeRegistry {

	private ResourceTypeRegistry() {
	}

	/**
	 * Maps the TMForum @type string to the corresponding domain class for Resource sub-types.
	 */
	public static final Map<String, Class<? extends Resource>> RESOURCE_TYPES = Map.ofEntries(
			Map.entry("LogicalResource", LogicalResource.class),
			Map.entry("SoftwareResource", SoftwareResource.class),
			Map.entry("API", ApiResource.class),
			Map.entry("InstalledSoftware", InstalledSoftware.class),
			Map.entry("HostingPlatformRequirement", HostingPlatformRequirement.class),
			Map.entry("PhysicalResource", PhysicalResource.class),
			Map.entry("SoftwareSupportPackage", SoftwareSupportPackage.class)
	);

	/**
	 * Maps the NGSI-LD entity type string to the corresponding domain class for Resource sub-types.
	 */
	public static final Map<String, Class<? extends Resource>> RESOURCE_ENTITY_TYPES = Map.ofEntries(
			Map.entry(Resource.TYPE_RESOURCE, Resource.class),
			Map.entry(LogicalResource.TYPE_LOGICAL_RESOURCE, LogicalResource.class),
			Map.entry(SoftwareResource.TYPE_SOFTWARE_RESOURCE, SoftwareResource.class),
			Map.entry(ApiResource.TYPE_API_RESOURCE, ApiResource.class),
			Map.entry(InstalledSoftware.TYPE_INSTALLED_SOFTWARE, InstalledSoftware.class),
			Map.entry(HostingPlatformRequirement.TYPE_HOSTING_PLATFORM_REQUIREMENT, HostingPlatformRequirement.class),
			Map.entry(PhysicalResource.TYPE_PHYSICAL_RESOURCE, PhysicalResource.class),
			Map.entry(SoftwareSupportPackage.TYPE_SOFTWARE_SUPPORT_PACKAGE, SoftwareSupportPackage.class)
	);

	/**
	 * All NGSI-LD entity types for Resources, comma-separated for NGSI-LD type queries.
	 */
	public static final String ALL_RESOURCE_TYPES = String.join(",", RESOURCE_ENTITY_TYPES.keySet());

	/**
	 * Maps the TMForum @type string to the corresponding domain class for ResourceSpecification sub-types.
	 */
	public static final Map<String, Class<? extends ResourceSpecification>> SPEC_TYPES = Map.ofEntries(
			Map.entry("LogicalResourceSpecification", LogicalResourceSpecification.class),
			Map.entry("SoftwareResourceSpecification", SoftwareResourceSpecification.class),
			Map.entry("APISpecification", ApiSpecification.class),
			Map.entry("SoftwareSpecification", SoftwareSpecification.class),
			Map.entry("HostingPlatformRequirementSpecification", HostingPlatformRequirementSpecification.class),
			Map.entry("PhysicalResourceSpecification", PhysicalResourceSpecification.class),
			Map.entry("SoftwareSupportPackageSpecification", SoftwareSupportPackageSpecification.class)
	);

	/**
	 * Maps the NGSI-LD entity type string to the corresponding domain class for ResourceSpecification sub-types.
	 */
	public static final Map<String, Class<? extends ResourceSpecification>> SPEC_ENTITY_TYPES = Map.ofEntries(
			Map.entry(ResourceSpecification.TYPE_RESOURCE_SPECIFICATION, ResourceSpecification.class),
			Map.entry(LogicalResourceSpecification.TYPE_LOGICAL_RESOURCE_SPECIFICATION,
					LogicalResourceSpecification.class),
			Map.entry(SoftwareResourceSpecification.TYPE_SOFTWARE_RESOURCE_SPECIFICATION,
					SoftwareResourceSpecification.class),
			Map.entry(ApiSpecification.TYPE_API_SPECIFICATION, ApiSpecification.class),
			Map.entry(SoftwareSpecification.TYPE_SOFTWARE_SPECIFICATION, SoftwareSpecification.class),
			Map.entry(HostingPlatformRequirementSpecification.TYPE_HOSTING_PLATFORM_REQUIREMENT_SPECIFICATION,
					HostingPlatformRequirementSpecification.class),
			Map.entry(PhysicalResourceSpecification.TYPE_PHYSICAL_RESOURCE_SPECIFICATION,
					PhysicalResourceSpecification.class),
			Map.entry(SoftwareSupportPackageSpecification.TYPE_SOFTWARE_SUPPORT_PACKAGE_SPECIFICATION,
					SoftwareSupportPackageSpecification.class)
	);

	/**
	 * All NGSI-LD entity types for ResourceSpecifications, comma-separated for NGSI-LD type queries.
	 */
	public static final String ALL_SPEC_TYPES = String.join(",", SPEC_ENTITY_TYPES.keySet());

	/**
	 * Extract the NGSI-LD entity type from an NGSI-LD ID.
	 * ID format: {@code urn:ngsi-ld:TYPE:UUID}
	 *
	 * @param ngsiLdId the NGSI-LD ID string
	 * @return the entity type, or null if the ID format is invalid
	 */
	public static String extractTypeFromId(String ngsiLdId) {
		if (ngsiLdId == null) {
			return null;
		}
		String[] parts = ngsiLdId.split(":");
		if (parts.length >= 4) {
			return parts[2];
		}
		return null;
	}

	/**
	 * Get the Resource domain class for a given NGSI-LD entity type.
	 *
	 * @param entityType the NGSI-LD entity type
	 * @return the domain class, defaults to Resource.class if not found
	 */
	public static Class<? extends Resource> getResourceClass(String entityType) {
		return RESOURCE_ENTITY_TYPES.getOrDefault(entityType, Resource.class);
	}

	/**
	 * Get the ResourceSpecification domain class for a given NGSI-LD entity type.
	 *
	 * @param entityType the NGSI-LD entity type
	 * @return the domain class, defaults to ResourceSpecification.class if not found
	 */
	public static Class<? extends ResourceSpecification> getSpecClass(String entityType) {
		return SPEC_ENTITY_TYPES.getOrDefault(entityType, ResourceSpecification.class);
	}

	/**
	 * Get the NGSI-LD entity type for a given TMForum @type and domain class.
	 *
	 * @param atType the TMForum @type value
	 * @return the NGSI-LD entity type, or Resource.TYPE_RESOURCE if not recognized
	 */
	public static String getResourceEntityType(String atType) {
		Class<? extends Resource> clazz = RESOURCE_TYPES.get(atType);
		if (clazz == null) {
			return Resource.TYPE_RESOURCE;
		}
		return RESOURCE_ENTITY_TYPES.entrySet().stream()
				.filter(e -> e.getValue().equals(clazz))
				.map(Map.Entry::getKey)
				.findFirst()
				.orElse(Resource.TYPE_RESOURCE);
	}

	/**
	 * Get the NGSI-LD entity type for a given TMForum @type specification.
	 *
	 * @param atType the TMForum @type value
	 * @return the NGSI-LD entity type, or ResourceSpecification.TYPE_RESOURCE_SPECIFICATION if not recognized
	 */
	public static String getSpecEntityType(String atType) {
		Class<? extends ResourceSpecification> clazz = SPEC_TYPES.get(atType);
		if (clazz == null) {
			return ResourceSpecification.TYPE_RESOURCE_SPECIFICATION;
		}
		return SPEC_ENTITY_TYPES.entrySet().stream()
				.filter(e -> e.getValue().equals(clazz))
				.map(Map.Entry::getKey)
				.findFirst()
				.orElse(ResourceSpecification.TYPE_RESOURCE_SPECIFICATION);
	}
}
