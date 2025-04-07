package org.fiware.tmforum.serviceinventory;

import lombok.RequiredArgsConstructor;
import org.fiware.serviceinventory.model.ServiceVO;
import org.fiware.tmforum.common.exception.TmForumException;
import org.fiware.tmforum.common.exception.TmForumExceptionReason;
import org.fiware.tmforum.common.mapping.EventMapping;
import org.fiware.tmforum.common.notification.EventMapper;
import org.fiware.tmforum.serviceinventory.domain.Service;

import javax.inject.Singleton;
import java.util.Map;

import static java.util.Map.entry;

@RequiredArgsConstructor
@Singleton
public class ServiceInventoryEventMapper implements EventMapper {

	private final TMForumMapper tmForumMapper;

	@Override
	public Map<String, EventMapping> getEntityClassMapping() {
		return Map.ofEntries(
				entry(Service.TYPE_SERVICE, new EventMapping(ServiceVO.class, Service.class))
		);
	}

	@Override
	public Object mapPayload(Object rawPayload, Class<?> targetClass) {
		if (targetClass == Service.class) {
			return tmForumMapper.map((Service) rawPayload);
		}
		throw new TmForumException(String.format("Event-Payload %s is not supported.", rawPayload), TmForumExceptionReason.INVALID_DATA);
	}
}
