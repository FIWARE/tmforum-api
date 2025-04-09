package org.fiware.tmforum.common.notification;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.wistefan.mapping.EntityVOMapper;
import lombok.RequiredArgsConstructor;
import org.fiware.tmforum.common.domain.subscription.Event;
import org.fiware.tmforum.common.mapping.EventMapping;
import org.fiware.tmforum.common.rest.AbstractSubscriptionApiController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;

@RequiredArgsConstructor
public abstract class EventHandler {

	protected final NotificationSender notificationSender;
	protected final EntityVOMapper entityVOMapper;
	protected final EventMapper eventMapper;
	protected final ObjectMapper objectMapper;

	protected <T> Event createEvent(EventDetails eventDetails, T entity, Function entityFilter) {
		Event event = new Event();

		event.setEventType(eventDetails.eventType());
		event.setEventId(UUID.randomUUID().toString());

		EventMapping eventMapping = eventMapper.getEntityClassMapping().get(eventDetails.entityType());
		event.setEvent(Map.of(eventDetails.payloadName(), entityFilter.apply(eventMapper.mapPayload(entity, eventMapping.rawClass()))));
		event.setEventTime(eventDetails.eventTime());

		return event;
	}

	protected <T> Map<String, Object> applyFieldsFilter(T entity, List<String> fields) {
		Map<String, Object> entityMap = entityVOMapper.convertEntityToMap(entity);

		if (fields != null && !fields.isEmpty()) {
			Map<String, Object> filteredMap = new HashMap<>();
			fields.forEach(field -> {
				if (entityMap.containsKey(field)) {
					filteredMap.put(field, entityMap.get(field));
				}
			});
			return filteredMap;
		} else {
			return entityMap;
		}
	}
}
