package org.fiware.tmforum.serviceordering;

import lombok.RequiredArgsConstructor;
import org.fiware.serviceordering.model.CancelServiceOrderVO;
import org.fiware.serviceordering.model.ServiceOrderVO;
import org.fiware.tmforum.common.exception.TmForumException;
import org.fiware.tmforum.common.exception.TmForumExceptionReason;
import org.fiware.tmforum.common.mapping.EventMapping;
import org.fiware.tmforum.common.notification.ModuleEventMapper;
import org.fiware.tmforum.serviceordering.domain.CancelServiceOrder;
import org.fiware.tmforum.serviceordering.domain.ServiceOrder;

import javax.inject.Singleton;
import java.util.Map;

import static java.util.Map.entry;

/**
 * Maps service ordering domain events to their VO counterparts for event publishing.
 */
@RequiredArgsConstructor
@Singleton
public class ServiceOrderingEventMapper implements ModuleEventMapper {

	private final TMForumMapper tmForumMapper;

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Map<String, EventMapping> getEntityClassMapping() {
		return Map.ofEntries(
				entry(CancelServiceOrder.TYPE_CANCEL_SERVICE_ORDER, new EventMapping(CancelServiceOrderVO.class, CancelServiceOrder.class)),
				entry(ServiceOrder.TYPE_SERVICE_ORDER, new EventMapping(ServiceOrderVO.class, ServiceOrder.class))
		);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Object mapPayload(Object rawPayload, Class<?> rawClass) {
		if (rawClass == CancelServiceOrder.class) {
			return tmForumMapper.map((CancelServiceOrder) rawPayload);
		}
		if (rawClass == ServiceOrder.class) {
			return tmForumMapper.map((ServiceOrder) rawPayload);
		}
		throw new TmForumException(String.format("Event-Payload %s is not supported.", rawPayload), TmForumExceptionReason.INVALID_DATA);
	}
}
