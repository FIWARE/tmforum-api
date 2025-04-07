package org.fiware.tmforum.partyrole;

import lombok.RequiredArgsConstructor;
import org.fiware.tmforum.common.exception.TmForumException;
import org.fiware.tmforum.common.exception.TmForumExceptionReason;
import org.fiware.tmforum.common.mapping.EventMapping;
import org.fiware.tmforum.common.notification.EventMapper;

import javax.inject.Singleton;
import java.util.Map;

@RequiredArgsConstructor
@Singleton
public class PartyRoleEventMapper implements EventMapper {

	private final TMForumMapper tmForumMapper;

	@Override
	public Map<String, EventMapping> getEntityClassMapping() {
		return Map.ofEntries();
	}

	@Override
	public Object mapPayload(Object rawPayload, Class<?> targetClass) {

		throw new TmForumException(String.format("Event-Payload %s is not supported.", rawPayload), TmForumExceptionReason.INVALID_DATA);
	}
}
