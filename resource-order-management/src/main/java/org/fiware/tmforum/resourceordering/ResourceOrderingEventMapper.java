package org.fiware.tmforum.resourceordering;

import lombok.RequiredArgsConstructor;
import org.fiware.resourceordering.model.CancelResourceOrderVO;
import org.fiware.resourceordering.model.ResourceOrderVO;
import org.fiware.tmforum.common.exception.TmForumException;
import org.fiware.tmforum.common.exception.TmForumExceptionReason;
import org.fiware.tmforum.common.mapping.EventMapping;
import org.fiware.tmforum.common.notification.ModuleEventMapper;
import org.fiware.tmforum.resourceordering.domain.CancelResourceOrder;
import org.fiware.tmforum.resourceordering.domain.ResourceOrder;

import javax.inject.Singleton;
import java.util.Map;

import static java.util.Map.entry;

/**
 * Maps resource ordering domain events to their VO counterparts for event publishing.
 */
@RequiredArgsConstructor
@Singleton
public class ResourceOrderingEventMapper implements ModuleEventMapper {

	private final TMForumMapper tmForumMapper;

	@Override
	public Map<String, EventMapping> getEntityClassMapping() {
		return Map.ofEntries(
				entry(CancelResourceOrder.TYPE_CANCEL_RESOURCE_ORDER, new EventMapping(CancelResourceOrderVO.class, CancelResourceOrder.class)),
				entry(ResourceOrder.TYPE_RESOURCE_ORDER, new EventMapping(ResourceOrderVO.class, ResourceOrder.class))
		);
	}

	@Override
	public Object mapPayload(Object rawPayload, Class<?> rawClass) {
		if (rawClass == CancelResourceOrder.class) {
			return tmForumMapper.map((CancelResourceOrder) rawPayload);
		}
		if (rawClass == ResourceOrder.class) {
			return tmForumMapper.map((ResourceOrder) rawPayload);
		}
		throw new TmForumException(String.format("Event-Payload %s is not supported.", rawPayload), TmForumExceptionReason.INVALID_DATA);
	}
}
