package org.fiware.tmforum.common.notification;

import io.github.resilience4j.core.IntervalFunction;
import io.github.resilience4j.reactor.retry.RetryOperator;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import io.github.resilience4j.retry.RetryRegistry;
import io.github.wistefan.mapping.EntityVOMapper;
import io.micronaut.context.annotation.Bean;
import io.micronaut.http.HttpHeaders;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.client.HttpClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.fiware.ngsi.model.EntityVO;
import org.fiware.tmforum.common.domain.subscription.Event;
import org.fiware.tmforum.common.domain.subscription.Subscription;
import org.fiware.tmforum.common.querying.QueryResolver;
import org.fiware.tmforum.common.util.StringUtils;
import reactor.core.publisher.Mono;

import java.lang.reflect.InvocationTargetException;
import java.net.URI;
import java.time.Instant;
import java.util.*;

import static org.fiware.tmforum.common.notification.EventConstants.*;

@RequiredArgsConstructor
@Bean
@Slf4j
public class NotificationSender {
    private final HttpClient httpClient;
    private final QueryResolver queryResolver;
    private final EntityVOMapper entityVOMapper;

    public String buildCreateEventType(String entityType) {
        return StringUtils.toCamelCase(entityType) + CREATE_EVENT_SUFFIX;
    }

    public String buildAttributeValueChangeEventType(String entityType) {
        return StringUtils.toCamelCase(entityType) + ATTRIBUTE_VALUE_CHANGE_EVENT_SUFFIX;
    }

    public String buildStateChangeEventType(String entityType) {
        return StringUtils.toCamelCase(entityType) + STATE_CHANGE_EVENT_SUFFIX;
    }

    public String buildDeleteEventType(String entityType) {
        return StringUtils.toCamelCase(entityType) + DELETE_EVENT_SUFFIX;
    }

    private static final RetryRegistry RETRY_REGISTRY = RetryRegistry.of(RetryConfig.custom()
            .maxAttempts(10)
            .intervalFunction(IntervalFunction.ofExponentialBackoff())
            .failAfterMaxAttempts(true)
            .build());

    private Mono<HttpResponse<Object>> sendToClient(Notification notification) {
        HttpRequest<?> req = HttpRequest.POST(notification.callback, notification.event)
                .header(HttpHeaders.CONTENT_TYPE, "application/json");
        return Mono.fromDirect(this.httpClient.exchange(req, Object.class));
    }

    private void addNotifications(List<Notification> notifications) {
        notifications
                .forEach(notification -> {
                    Retry retry = RETRY_REGISTRY
                            .retry(String.valueOf(notification.hashCode()));
                    sendToClient(notification)
                            .transformDeferred(RetryOperator.of(retry))
                            .doOnError(e -> log.warn("Was not able to deliver notification {}.", notification, e))
                            .subscribe();
                });
    }

    public <T> void handleCreateEvent(List<Subscription> subscriptions, T entity) {
        String entityType = getEntityType(entity);
        String eventType = buildCreateEventType(entityType);
        String payloadName = StringUtils.decapitalize(StringUtils.getEventGroupName(eventType));

        List<Notification> notifications = new ArrayList<>();
        subscriptions.forEach(subscription -> {
            String query = subscription.getQuery();
            if (queryResolver.doesQueryMatchCreateEvent(query, entity, payloadName)) {
                Event event = createEvent(eventType, applyFieldsFilter(entity, subscription.getFields()),
                        payloadName);
                notifications.add(new Notification(subscription.getCallback(), event));
            }
        });
        addNotifications(notifications);
    }

    public <T> void handleUpdateEvent(List<Subscription> subscriptions, T oldState, T newState) {
        String entityType = getEntityType(newState);
        String eventType = buildAttributeValueChangeEventType(entityType);
        String payloadName = StringUtils.decapitalize(StringUtils.getEventGroupName(eventType));

        List<Notification> notifications = new ArrayList<>();
        subscriptions.forEach(subscription -> {
            String query = subscription.getQuery();
            if (queryResolver.doesQueryMatchUpdateEvent(query, newState, oldState, payloadName)) {
                Event event = createEvent(eventType, applyFieldsFilter(newState, subscription.getFields()),
                        payloadName);
                notifications.add(new Notification(subscription.getCallback(), event));
            }
        });
        addNotifications(notifications);
    }

    public <T> void handleStateChangeEvent(List<Subscription> subscriptions, T oldState, T newState) {
        String entityType = getEntityType(newState);
        String eventType = buildStateChangeEventType(entityType);
        String payloadName = StringUtils.decapitalize(StringUtils.getEventGroupName(eventType));

        List<Notification> notifications = new ArrayList<>();
        subscriptions.forEach(subscription -> {
            if (hasEntityStateChanged(oldState, newState)) {
                Event event = createEvent(eventType, applyFieldsFilter(newState, subscription.getFields()),
                        payloadName);
                notifications.add(new Notification(subscription.getCallback(), event));
            }
        });
        addNotifications(notifications);
    }

    public void handleDeleteEvent(List<Subscription> subscriptions, EntityVO entityVO) {
        String entityType = entityVO.getType();
        String eventType = buildDeleteEventType(entityType);
        String payloadName = StringUtils.decapitalize(StringUtils.getEventGroupName(eventType));

        List<Notification> notifications = new ArrayList<>();
        subscriptions.forEach(subscription -> {
            Event event = createEvent(eventType, applyFieldsFilter(entityVO, subscription.getFields()),
                    payloadName);
            notifications.add(new Notification(subscription.getCallback(), event));
        });
        addNotifications(notifications);
    }

    private Event createEvent(String eventType, Map<String, Object> payload, String payloadName) {
        Event event = new Event();

        event.setEventType(eventType);
        event.setEventId(UUID.randomUUID().toString());
        event.setEvent(Map.of(payloadName, payload));
        event.setEventTime(Instant.now());

        return event;
    }

    private <T> Map<String, Object> applyFieldsFilter(T entity, List<String> fields) {
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

    public  <T> String getEntityType(T entity) {
        try {
            return entity.getClass().getMethod("getType").invoke(entity).toString();
        } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            return "";
        }
    }

    private <T> Object getEntityState(T entity) {
        Object stateFieldValue;
        try {
            stateFieldValue = entity.getClass().getMethod("getEntityState").invoke(entity);
        } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            stateFieldValue = null;
        }
        return stateFieldValue;
    }

    private <T> boolean hasEntityStateChanged(T oldState, T newState) {
        Object oldStateFieldValue = getEntityState(oldState);
        Object newStateFieldValue = getEntityState(newState);

        return oldStateFieldValue == null && newStateFieldValue != null ||
                oldStateFieldValue != null && newStateFieldValue == null ||
                oldStateFieldValue != null && !oldStateFieldValue.equals(newStateFieldValue) ||
                newStateFieldValue != null && !newStateFieldValue.equals(oldStateFieldValue);
    }

    private record Notification(URI callback, Event event) {}
}
