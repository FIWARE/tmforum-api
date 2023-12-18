package org.fiware.tmforum.common.notification;

import io.github.wistefan.mapping.EntityVOMapper;
import io.micronaut.cache.annotation.Cacheable;
import io.micronaut.context.annotation.Bean;
import lombok.RequiredArgsConstructor;
import org.fiware.ngsi.model.EntityVO;
import org.fiware.ngsi.model.NotificationVO;
import org.fiware.tmforum.common.CommonConstants;
import org.fiware.tmforum.common.domain.subscription.Event;
import org.fiware.tmforum.common.domain.subscription.TMForumSubscription;
import org.fiware.tmforum.common.exception.EventHandlingException;
import org.fiware.tmforum.common.notification.checkers.*;
import org.fiware.tmforum.common.notification.command.*;
import org.fiware.tmforum.common.querying.QueryParser;
import org.fiware.tmforum.common.querying.SubscriptionQueryResolver;
import org.fiware.tmforum.common.repository.TmForumRepository;
import org.fiware.tmforum.common.util.StringUtils;
import reactor.core.publisher.Mono;

import java.lang.reflect.InvocationTargetException;
import java.net.URI;
import java.time.Instant;
import java.util.*;

import static org.fiware.tmforum.common.CommonConstants.DEFAULT_OFFSET;
import static org.fiware.tmforum.common.notification.EventConstants.*;

@Bean
@RequiredArgsConstructor
public class EventHandler {

    private final QueryParser queryParser;
    private final NotificationSender notificationSender;
    private final SubscriptionQueryResolver subscriptionQueryResolver;
    private final EntityVOMapper entityVOMapper;
    private final TmForumRepository repository;

    @Cacheable(CommonConstants.SUBSCRIPTIONS_CACHE_NAME)
    public Mono<List<TMForumSubscription>> getSubscriptions(String entityType, String eventType) {
        return repository.findEntities(
                DEFAULT_OFFSET,
                100,
                TMForumSubscription.TYPE_TM_FORUM_SUBSCRIPTION,
                TMForumSubscription.class,
                queryParser.toNgsiLdQuery(TMForumSubscription.class,
                        String.format("entities=%s&eventTypes=%s", entityType, eventType))
        );
    }

    private <T> Mono<Void> handle(T entity, EventDetails<T> eventDetails, Command command) {
        return getSubscriptions(eventDetails.entityType, eventDetails.eventType)
                .doOnNext(subscriptions -> {
                    List<TMForumNotification> notifications = new ArrayList<>();
                    subscriptions.forEach(subscription -> {
                        if (command.execute(subscription.getQuery())) {
                            Event event = createEvent(eventDetails, applyFieldsFilter(entity, subscription.getFields()));
                            notifications.add(new TMForumNotification(subscription.getCallback(), event));
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
            EventDetails<EntityVO> eventDetails = new EventDetails<>(entityVO, DELETE_EVENT_SUFFIX);
            Command command = new DeleteCommand();
            return handle(entityVO, eventDetails, command);
        } catch (EventHandlingException e) {
            return Mono.empty();
        }
    }

    private  <T> Event createEvent(EventDetails<T> eventDetails, Map<String, Object> payload) {
        Event event = new Event();

        event.setEventType(eventDetails.eventType);
        event.setEventId(UUID.randomUUID().toString());
        event.setEvent(Map.of(eventDetails.payloadName, payload));
        event.setEventTime(eventDetails.eventTime);

        return event;
    }

    private TMForumNotification createNotification(EntityVO payload, String eventType, Instant eventTime,
                                                   List<String> selectedFields, URI listenerCallback) {
        EventDetails<EntityVO> eventDetails = new EventDetails<>(payload,
                eventType, eventTime);
        Event event = createEvent(eventDetails, applyFieldsFilter(
                payload, selectedFields));
        return new TMForumNotification(listenerCallback, event);
    }

    private <T> Map<String, Object> applyFieldsFilter(T entity, List<String> fields) {
        Map<String, Object> entityMap = entityVOMapper.convertEntityToMap(entity);

        if (entity instanceof EntityVO && entityMap.containsKey("additionalProperties") &&
                entityMap.get("additionalProperties") != null) {
            entityMap.putAll(entityVOMapper.convertEntityToMap(entityMap.get("additionalProperties")));
            entityMap.remove("additionalProperties");
        }

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

    public void handleNgsiLdNotification(NotificationVO notificationVO, String relevantEventTypes,
                                         String listenerCallback, String selectedFields) {
        List<TMForumNotification> notifications = new ArrayList<>();

        List<EventChecker> eventCheckers = buildEventCheckers(relevantEventTypes);
        notificationVO.getData().forEach(entityVO -> eventCheckers.forEach(eventChecker -> {
            if (eventChecker.wasFired(entityVO)) {
                notifications.add(createNotification(entityVO, eventChecker.getEventTypeSuffix(),
                        notificationVO.getNotifiedAt(), selectedFields == null ? List.of()
                                : Arrays.stream(selectedFields.split(",")).toList(),
                        URI.create(listenerCallback)));
            }
        }));

        notificationSender.sendNotifications(notifications);
    }

    private List<EventChecker> buildEventCheckers(String relevantEventTypes) throws EventHandlingException {
        List<EventChecker> eventCheckers = new ArrayList<>();
        Arrays.stream(relevantEventTypes.split(",")).forEach(relevantEventType -> {
            if (relevantEventType.contains(CREATE_EVENT_SUFFIX)) {
                eventCheckers.add(new CreateEventChecker());
            } else if (relevantEventType.contains(ATTRIBUTE_VALUE_CHANGE_EVENT_SUFFIX)) {
                eventCheckers.add(new AttributeValueChangeEventChecker());
            } else if (relevantEventType.contains(STATE_CHANGE_EVENT_SUFFIX)) {
                eventCheckers.add(new StateChangeEventChecker());
            } else if (relevantEventType.contains(DELETE_EVENT_SUFFIX)) {
                eventCheckers.add(new DeleteEventChecker());
            } else {
                throw new EventHandlingException("Event type was not recognized.");
            }
        });
        return eventCheckers;
    }

    private static class EventDetails<T> {
        String entityType;
        String eventType;
        String payloadName;
        Instant eventTime;

        public EventDetails(T entity, String eventSuffix) {
            entityType = getEntityType(entity);
            this.eventTime = Instant.now();
            init(eventSuffix);
        }

        public EventDetails(T entity, String eventSuffix, Instant eventTime) {
            entityType = getEntityType(entity);
            this.eventTime = eventTime;
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
            if (entityType.equals(TMForumSubscription.TYPE_TM_FORUM_SUBSCRIPTION)) {
                throw new EventHandlingException();
            }
            eventType = StringUtils.toCamelCase(entityType) + eventSuffix;
            payloadName = StringUtils.decapitalize(StringUtils.getEventGroupName(eventType));
        }
    }
}
