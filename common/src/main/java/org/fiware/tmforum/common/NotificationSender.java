package org.fiware.tmforum.common;

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
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.lang.reflect.InvocationTargetException;
import java.net.URI;
import java.time.Instant;
import java.util.*;

import static org.fiware.tmforum.common.EventConstants.*;

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

    private Mono<HttpResponse<String>> sendEventToClient(URI callbackURI, Event event) {
        HttpRequest<?> req = HttpRequest.POST(callbackURI, event)
                .header(HttpHeaders.CONTENT_TYPE, "application/ld+json");
        return Mono.fromDirect(this.httpClient.exchange(req, String.class));
    }

    public <T> Mono<List<HttpResponse<String>>> handleCreateEvent(List<Subscription> subscriptions, T entity) {
        String entityType = getEntityType(entity);
        String eventType = buildCreateEventType(entityType);
        String payloadName = StringUtils.decapitalize(StringUtils.getEventGroupName(eventType));

        List<Mono<HttpResponse<String>>> monos = new ArrayList<>();
        subscriptions.forEach(subscription -> {
            String query = subscription.getQuery();
            if (queryResolver.doesQueryMatchCreateEvent(query, entity, payloadName)) {
                Event event = createEvent(eventType, applyFieldsFilter(entity, subscription.getFields()),
                        payloadName);
                monos.add(sendEventToClient(subscription.getCallback(), event));
            }
        });
        return Flux.concat(monos).collectList();
    }

    public <T> Flux<HttpResponse<String>> handleUpdateEvent(List<Subscription> subscriptions, T oldState, T newState) {
        String entityType = getEntityType(newState);
        String eventType = buildAttributeValueChangeEventType(entityType);
        String payloadName = StringUtils.decapitalize(StringUtils.getEventGroupName(eventType));

        List<Mono<HttpResponse<String>>> monos = new ArrayList<>();
        subscriptions.forEach(subscription -> {
            String query = subscription.getQuery();
            if (queryResolver.doesQueryMatchUpdateEvent(query, newState, oldState, payloadName)) {
                Event event = createEvent(eventType, applyFieldsFilter(newState, subscription.getFields()),
                        StringUtils.decapitalize(StringUtils.getEventGroupName(eventType)));
                monos.add(sendEventToClient(subscription.getCallback(), event));
            }
        });
        return Flux.concat(monos);
    }

    public <T> Flux<HttpResponse<String>> handleStateChangeEvent(List<Subscription> subscriptions, T oldState, T newState) {
        String entityType = getEntityType(newState);

        List<Mono<HttpResponse<String>>> monos = new ArrayList<>();
        subscriptions.forEach(subscription -> {
            if (hasEntityStateChanged(oldState, newState)) {
                String eventType = buildStateChangeEventType(entityType);
                Event event = createEvent(eventType, applyFieldsFilter(newState, subscription.getFields()),
                        StringUtils.decapitalize(StringUtils.getEventGroupName(eventType)));
                monos.add(sendEventToClient(subscription.getCallback(), event));
            }
        });
        return Flux.concat(monos);
    }

    public Mono<List<HttpResponse<String>>> handleDeleteEvent(List<Subscription> subscriptions, EntityVO entityVO) {
        String entityType = entityVO.getType();
        String eventType = buildDeleteEventType(entityType);

        List<Mono<HttpResponse<String>>> monos = new ArrayList<>();
        subscriptions.forEach(subscription -> {
            Event event = createEvent(eventType, applyFieldsFilter(entityVO, subscription.getFields()),
                    StringUtils.decapitalize(StringUtils.getEventGroupName(eventType)));
            monos.add(sendEventToClient(subscription.getCallback(), event));
        });
        return Flux.concat(monos).collectList();
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
}
