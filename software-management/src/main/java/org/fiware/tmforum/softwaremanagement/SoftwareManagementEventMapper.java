package org.fiware.tmforum.softwaremanagement;

import lombok.RequiredArgsConstructor;
import org.fiware.softwaremanagement.model.ResourceSpecificationVO;
import org.fiware.softwaremanagement.model.ResourceVO;
import org.fiware.tmforum.common.exception.TmForumException;
import org.fiware.tmforum.common.exception.TmForumExceptionReason;
import org.fiware.tmforum.common.mapping.EventMapping;
import org.fiware.tmforum.common.notification.ModuleEventMapper;
import org.fiware.tmforum.resource.Resource;
import org.fiware.tmforum.resource.ResourceSpecification;

import javax.inject.Singleton;
import java.util.Map;

import static java.util.Map.entry;

/**
 * Event mapper for the Software Management module (TMF730).
 * Maps Resource and ResourceSpecification entity types to their corresponding VO classes
 * and handles event payload mapping.
 */
@RequiredArgsConstructor
@Singleton
public class SoftwareManagementEventMapper implements ModuleEventMapper {

	private final TMForumMapper tmForumMapper;

	/**
	 * {@inheritDoc}
	 * Returns mappings for Resource and ResourceSpecification entity types.
	 */
	@Override
	public Map<String, EventMapping> getEntityClassMapping() {
		return Map.ofEntries(
				entry(Resource.TYPE_RESOURCE, new EventMapping(ResourceVO.class, Resource.class)),
				entry(ResourceSpecification.TYPE_RESOURCE_SPECIFICATION,
						new EventMapping(ResourceSpecificationVO.class, ResourceSpecification.class))
		);
	}

	/**
	 * {@inheritDoc}
	 * Maps raw event payloads of type Resource or ResourceSpecification to their VO representations.
	 */
	@Override
	public Object mapPayload(Object rawPayload, Class<?> rawClass) {
		if (rawClass == Resource.class) {
			return tmForumMapper.map((Resource) rawPayload);
		}
		if (rawClass == ResourceSpecification.class) {
			return tmForumMapper.map((ResourceSpecification) rawPayload);
		}
		throw new TmForumException(
				String.format("Event-Payload %s is not supported.", rawPayload),
				TmForumExceptionReason.INVALID_DATA);
	}
}
