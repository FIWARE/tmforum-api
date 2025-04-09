package org.fiware.tmforum.resourcefunction;

import lombok.RequiredArgsConstructor;
import org.fiware.resourcefunction.model.*;
import org.fiware.tmforum.common.exception.TmForumException;
import org.fiware.tmforum.common.exception.TmForumExceptionReason;
import org.fiware.tmforum.common.mapping.EventMapping;
import org.fiware.tmforum.common.notification.EventMapper;
import org.fiware.tmforum.resourcefunction.domain.*;

import javax.inject.Singleton;
import java.util.Map;

import static java.util.Map.entry;

@RequiredArgsConstructor
@Singleton
public class ResourceFunctionActivationEventMapper implements EventMapper {

	private final TMForumMapper tmForumMapper;

	@Override
	public Map<String, EventMapping> getEntityClassMapping() {
		return Map.ofEntries(
				entry(Heal.TYPE_HEAL, new EventMapping(HealVO.class, Heal.class)),
				entry(Migrate.TYPE_MIGRATE, new EventMapping(MigrateVO.class, Migrate.class)),
				entry(Monitor.TYPE_MONITOR, new EventMapping(MonitorVO.class, Monitor.class)),
				entry(ResourceFunction.TYPE_RESOURCE_FUNCTION, new EventMapping(ResourceFunctionVO.class, ResourceFunction.class)),
				entry(Scale.TYPE_SCALE, new EventMapping(ScaleVO.class, Scale.class))
		);
	}

	@Override
	public Object mapPayload(Object rawPayload, Class<?> targetClass) {
		if (targetClass == Heal.class) {
			return tmForumMapper.map((Heal) rawPayload);
		}
		if (targetClass == Migrate.class) {
			return tmForumMapper.map((Migrate) rawPayload);
		}
		if (targetClass == Monitor.class) {
			return tmForumMapper.map((Monitor) rawPayload);
		}
		if (targetClass == ResourceFunction.class) {
			return tmForumMapper.map((ResourceFunction) rawPayload);
		}
		if (targetClass == Scale.class) {
			return tmForumMapper.map((Scale) rawPayload);
		}
		throw new TmForumException(String.format("Event-Payload %s is not supported.", rawPayload), TmForumExceptionReason.INVALID_DATA);
	}
}
