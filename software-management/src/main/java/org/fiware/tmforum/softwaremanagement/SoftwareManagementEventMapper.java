package org.fiware.tmforum.softwaremanagement;

import lombok.RequiredArgsConstructor;
import org.fiware.softwaremanagement.model.ResourceSpecificationVO;
import org.fiware.softwaremanagement.model.ResourceVO;
import org.fiware.tmforum.common.exception.TmForumException;
import org.fiware.tmforum.common.exception.TmForumExceptionReason;
import org.fiware.tmforum.common.mapping.EventMapping;
import org.fiware.tmforum.common.notification.ModuleEventMapper;
import org.fiware.tmforum.resource.*;

import javax.inject.Singleton;
import java.util.Map;

import static java.util.Map.entry;

/**
 * Event mapper for the Software Management module (TMF730).
 * Maps all Resource and ResourceSpecification entity types (including sub-types)
 * to their corresponding VO classes and handles event payload mapping.
 */
@RequiredArgsConstructor
@Singleton
public class SoftwareManagementEventMapper implements ModuleEventMapper {

	private final TMForumMapper tmForumMapper;

	/**
	 * {@inheritDoc}
	 * Returns mappings for all Resource and ResourceSpecification entity types including sub-types.
	 */
	@Override
	public Map<String, EventMapping> getEntityClassMapping() {
		return Map.ofEntries(
				// Resource types
				entry(Resource.TYPE_RESOURCE,
						new EventMapping(ResourceVO.class, Resource.class)),
				entry(LogicalResource.TYPE_LOGICAL_RESOURCE,
						new EventMapping(ResourceVO.class, LogicalResource.class)),
				entry(SoftwareResource.TYPE_SOFTWARE_RESOURCE,
						new EventMapping(ResourceVO.class, SoftwareResource.class)),
				entry(ApiResource.TYPE_API_RESOURCE,
						new EventMapping(ResourceVO.class, ApiResource.class)),
				entry(InstalledSoftware.TYPE_INSTALLED_SOFTWARE,
						new EventMapping(ResourceVO.class, InstalledSoftware.class)),
				entry(HostingPlatformRequirement.TYPE_HOSTING_PLATFORM_REQUIREMENT,
						new EventMapping(ResourceVO.class, HostingPlatformRequirement.class)),
				entry(PhysicalResource.TYPE_PHYSICAL_RESOURCE,
						new EventMapping(ResourceVO.class, PhysicalResource.class)),
				entry(SoftwareSupportPackage.TYPE_SOFTWARE_SUPPORT_PACKAGE,
						new EventMapping(ResourceVO.class, SoftwareSupportPackage.class)),
				// ResourceSpecification types
				entry(ResourceSpecification.TYPE_RESOURCE_SPECIFICATION,
						new EventMapping(ResourceSpecificationVO.class, ResourceSpecification.class)),
				entry(LogicalResourceSpecification.TYPE_LOGICAL_RESOURCE_SPECIFICATION,
						new EventMapping(ResourceSpecificationVO.class, LogicalResourceSpecification.class)),
				entry(SoftwareResourceSpecification.TYPE_SOFTWARE_RESOURCE_SPECIFICATION,
						new EventMapping(ResourceSpecificationVO.class, SoftwareResourceSpecification.class)),
				entry(ApiSpecification.TYPE_API_SPECIFICATION,
						new EventMapping(ResourceSpecificationVO.class, ApiSpecification.class)),
				entry(SoftwareSpecification.TYPE_SOFTWARE_SPECIFICATION,
						new EventMapping(ResourceSpecificationVO.class, SoftwareSpecification.class)),
				entry(HostingPlatformRequirementSpecification.TYPE_HOSTING_PLATFORM_REQUIREMENT_SPECIFICATION,
						new EventMapping(ResourceSpecificationVO.class,
								HostingPlatformRequirementSpecification.class)),
				entry(PhysicalResourceSpecification.TYPE_PHYSICAL_RESOURCE_SPECIFICATION,
						new EventMapping(ResourceSpecificationVO.class, PhysicalResourceSpecification.class)),
				entry(SoftwareSupportPackageSpecification.TYPE_SOFTWARE_SUPPORT_PACKAGE_SPECIFICATION,
						new EventMapping(ResourceSpecificationVO.class,
								SoftwareSupportPackageSpecification.class))
		);
	}

	/**
	 * {@inheritDoc}
	 * Maps raw event payloads for all Resource and ResourceSpecification types to their VO representations.
	 */
	@Override
	public Object mapPayload(Object rawPayload, Class<?> rawClass) {
		if (Resource.class.isAssignableFrom(rawClass)) {
			return tmForumMapper.map((Resource) rawPayload);
		}
		if (ResourceSpecification.class.isAssignableFrom(rawClass)) {
			return tmForumMapper.map((ResourceSpecification) rawPayload);
		}
		throw new TmForumException(
				String.format("Event-Payload %s is not supported.", rawPayload),
				TmForumExceptionReason.INVALID_DATA);
	}
}
