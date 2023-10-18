package org.fiware.tmforum.common.notification;

import io.github.wistefan.mapping.EntityVOMapper;
import io.micronaut.cache.annotation.Cacheable;
import io.micronaut.context.annotation.Bean;
import lombok.RequiredArgsConstructor;
import org.fiware.ngsi.model.EntityVO;
import org.fiware.tmforum.common.domain.subscription.Event;
import org.fiware.tmforum.common.domain.subscription.Subscription;
import org.fiware.tmforum.common.exception.EventHandlingException;
import org.fiware.tmforum.common.notification.command.*;
import org.fiware.tmforum.common.querying.QueryParser;
import org.fiware.tmforum.common.querying.SubscriptionQueryResolver;
import org.fiware.tmforum.common.repository.TmForumRepository;
import org.fiware.tmforum.common.util.StringUtils;
import reactor.core.publisher.Mono;

import java.lang.reflect.InvocationTargetException;
import java.time.Instant;
import java.util.*;

import static org.fiware.tmforum.common.CommonConstants.DEFAULT_OFFSET;
import static org.fiware.tmforum.common.notification.EventConstants.*;

@Bean
@RequiredArgsConstructor
public class EventHandler {
    public static final String SUBSCRIPTIONS_CACHE_NAME = "subscriptions";

    private final QueryParser queryParser;
    private final TmForumRepository repository;
    private final NotificationSender notificationSender;
    private final SubscriptionQueryResolver subscriptionQueryResolver;
    private final EntityVOMapper entityVOMapper;

    @Cacheable(SUBSCRIPTIONS_CACHE_NAME)
    public Mono<List<Subscription>> getSubscriptions(String entityType, String eventType) {
        return repository.findEntities(
                DEFAULT_OFFSET,
                100,
                Subscription.TYPE_SUBSCRIPTION,
                Subscription.class,
                queryParser.toNgsiLdQuery(Subscription.class,
                        String.format("entities=%s&eventTypes=%s", entityType, eventType))
        );
    }

    private <T> Mono<Void> handle(T entity, EventDetails<T> eventDetails, Command command) {
        return getSubscriptions(eventDetails.entityType, eventDetails.eventType)
                .doOnNext(subscriptions -> {
                    List<Notification> notifications = new ArrayList<>();
                    subscriptions.forEach(subscription -> {
                        if (command.execute(subscription.getQuery())) {
                            Event event = createEvent(eventDetails.eventType, applyFieldsFilter(entity, subscription.getFields()),
                                    eventDetails.payloadName);
                            notifications.add(new Notification(subscription.getCallback(), event));
                        }
                    });
                    notificationSender.sendNotifications(notifications);
                })
                .then();
    }

    public <T> Mono<Void> handleCreateEvent(T entity) {
        try {
            EventDetails<T> eventDetails = new EventDetails<>(entity, CREATE_EVENT_SUFFIX);
            Command command = new CreateEventCommand<>(subscriptionQueryResolver, entity, eventDetails.payloadName);
            return handle(entity, eventDetails, command);
        } catch (EventHandlingException e) {
            return Mono.empty();
        }
    }

    public <T> Mono<Void> handleUpdateEvent(T newState, T oldState) {
        try {
            EventDetails<T> eventDetails1 = new EventDetails<>(newState, ATTRIBUTE_VALUE_CHANGE_EVENT_SUFFIX);
            Command command1 = new AttributeValueChangeEventCommand<>(
                    subscriptionQueryResolver, newState, oldState, eventDetails1.payloadName);
            Mono<Void> attrUpdateMono = handle(newState, eventDetails1, command1);


            EventDetails<T> eventDetails2 = new EventDetails<>(newState, STATE_CHANGE_EVENT_SUFFIX);
            Command command2 = new StateChangeEventCommand<>(newState, oldState);
            Mono<Void> stateChangeMono = handle(newState, eventDetails2, command2);

            return attrUpdateMono.then(stateChangeMono);
        } catch (EventHandlingException e) {
            return Mono.empty();
        }
    }

    public Mono<Void> handleDeleteEvent(EntityVO entityVO) {
        try {
            EventDetails<Object> eventDetails = new EventDetails<>(entityVO, DELETE_EVENT_SUFFIX);
            Command command = new DeleteCommand();
            return handle(entityVO, eventDetails, command);
        } catch (EventHandlingException e) {
            return Mono.empty();
        }
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

    private static class EventDetails<T> {
        String entityType;
        String eventType;
        String payloadName;

        public EventDetails(T entity, String eventSuffix) {
            entityType = getEntityType(entity);
            init(eventSuffix);
        }

        public EventDetails(EntityVO entityVO, String eventSuffix) {
            entityType = entityVO.getType();
            init(eventSuffix);
        }

        private String getEntityType(T entity) {
            try {
                return entity.getClass().getMethod("getType").invoke(entity).toString();
            } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
                return "";
            }
        }

        private void init(String eventSuffix) {
            if (entityType.equals(Subscription.TYPE_SUBSCRIPTION)) {
                throw new EventHandlingException();
            }
            eventType = StringUtils.toCamelCase(entityType) + eventSuffix;
            payloadName = StringUtils.decapitalize(StringUtils.getEventGroupName(eventType));
        }
    }
}
