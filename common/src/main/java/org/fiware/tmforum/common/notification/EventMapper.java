package org.fiware.tmforum.common.notification;

import org.fiware.tmforum.common.mapping.EventMapping;

import java.util.Map;

public interface EventMapper {

	Map<String, EventMapping> getEntityClassMapping();

	Object mapPayload(Object rawPayload, Class<?> targetClass);

}
