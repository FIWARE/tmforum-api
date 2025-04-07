package org.fiware.tmforum.resourceinventory;

import lombok.RequiredArgsConstructor;
import org.fiware.resourceinventory.model.ResourceVO;
import org.fiware.tmforum.common.exception.TmForumException;
import org.fiware.tmforum.common.exception.TmForumExceptionReason;
import org.fiware.tmforum.common.mapping.EventMapping;
import org.fiware.tmforum.common.notification.EventMapper;
import org.fiware.tmforum.resource.Resource;

import javax.inject.Singleton;
import java.util.Map;

import static java.util.Map.entry;

@RequiredArgsConstructor
@Singleton
public class ResourceInventoryEventMapper implements EventMapper {

	private final TMForumMapper tmForumMapper;

	@Override
	public Map<String, EventMapping> getEntityClassMapping() {
		return Map.ofEntries(
				entry(Resource.TYPE_RESOURCE, new EventMapping(ResourceVO.class, Resource.class))
		);
	}

	@Override
	public Object mapPayload(Object rawPayload, Class<?> targetClass) {
		if (targetClass == Resource.class) {
			return tmForumMapper.map((Resource) rawPayload);
		}
		throw new TmForumException(String.format("Event-Payload %s is not supported.", rawPayload), TmForumExceptionReason.INVALID_DATA);
	}
}
