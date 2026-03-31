package org.fiware.tmforum.softwaremanagement.bean;

import org.fiware.softwaremanagement.model.*;
import org.fiware.tmforum.common.mapping.SubTypePropertyProvider;

import javax.inject.Singleton;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Registers all known Resource and ResourceSpecification sub-type VO classes
 * for the Software Management module (TMF730). This allows the
 * {@link org.fiware.tmforum.common.mapping.ValidatingDeserializer} to recognize
 * sub-type-specific properties without requiring an explicit {@code @schemaLocation}.
 */
@Singleton
public class SoftwareManagementSubTypePropertyProvider implements SubTypePropertyProvider {

	/**
	 * Maps the TMForum {@code @type} string to the corresponding generated VO class
	 * for all supported Resource and ResourceSpecification sub-types.
	 */
	private static final Map<String, Class<?>> SUB_TYPE_VO_CLASSES = Map.ofEntries(
			// Resource sub-types
			Map.entry("LogicalResource", LogicalResourceVO.class),
			Map.entry("SoftwareResource", SoftwareResourceVO.class),
			Map.entry("API", APIVO.class),
			Map.entry("InstalledSoftware", InstalledSoftwareVO.class),
			Map.entry("HostingPlatformRequirement", HostingPlatformRequirementVO.class),
			Map.entry("PhysicalResource", PhysicalResourceVO.class),
			Map.entry("SoftwareSupportPackage", SoftwareSupportPackageVO.class),
			// ResourceSpecification sub-types
			Map.entry("LogicalResourceSpecification", LogicalResourceSpecificationVO.class),
			Map.entry("SoftwareResourceSpecification", SoftwareResourceSpecificationVO.class),
			Map.entry("APISpecification", APISpecificationVO.class),
			Map.entry("SoftwareSpecification", SoftwareSpecificationVO.class),
			Map.entry("HostingPlatformRequirementSpecification",
					HostingPlatformRequirementSpecificationVO.class),
			Map.entry("PhysicalResourceSpecification", PhysicalResourceSpecificationVO.class),
			Map.entry("SoftwareSupportPackageSpecification",
					SoftwareSupportPackageSpecificationVO.class)
	);

	private final Map<String, Set<String>> knownPropertiesCache = new ConcurrentHashMap<>();

	@Override
	public Optional<Set<String>> getKnownProperties(String atType) {
		if (!SUB_TYPE_VO_CLASSES.containsKey(atType)) {
			return Optional.empty();
		}
		return Optional.of(
				knownPropertiesCache.computeIfAbsent(atType,
						t -> SubTypePropertyProvider.resolveJsonProperties(SUB_TYPE_VO_CLASSES.get(t)))
		);
	}
}
