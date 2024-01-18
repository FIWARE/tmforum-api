package org.fiware.tmforum.common.notification;

import io.github.wistefan.mapping.EntityVOMapper;
import lombok.RequiredArgsConstructor;
import org.fiware.tmforum.common.domain.subscription.Event;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RequiredArgsConstructor
public abstract class EventHandler {

    protected final NotificationSender notificationSender;
    protected final EntityVOMapper entityVOMapper;

    protected Event createEvent(EventDetails eventDetails, Map<String, Object> payload) {
        Event event = new Event();

        event.setEventType(eventDetails.eventType());
        event.setEventId(UUID.randomUUID().toString());
        event.setEvent(Map.of(eventDetails.payloadName(), payload));
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
