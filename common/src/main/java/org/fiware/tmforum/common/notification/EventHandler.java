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
import org.fiware.tmforum.common.notification.checkers.ngsild.*;
import org.fiware.tmforum.common.notification.checkers.tmforum.*;
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

    private <T> Mono<Void> handle(T entity, EventDetails<T> eventDetails, TmForumEventChecker eventChecker) {
        return getSubscriptions(eventDetails.entityType, eventDetails.eventType)
                .doOnNext(subscriptions -> {
                    List<TMForumNotification> notifications = new ArrayList<>();
                    subscriptions.forEach(subscription -> {
                        if (eventChecker.wasFired(subscription.getQuery())) {
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
            TmForumEventChecker tmForumEventChecker = new CreateTmForumEventChecker<>(subscriptionQueryResolver, entity, eventDetails.entityType);
            return handle(entity, eventDetails, tmForumEventChecker);
        } catch (EventHandlingException e) {
            return Mono.empty();
        }
    }

    public <T> Mono<Void> handleUpdateEvent(T newState, T oldState) {
        try {
            EventDetails<T> eventDetails1 = new EventDetails<>(newState, ATTRIBUTE_VALUE_CHANGE_EVENT_SUFFIX);
            TmForumEventChecker eventChecker1 = new AttributeValueChangeTmForumEventChecker<>(
                    subscriptionQueryResolver, newState, oldState, eventDetails1.entityType);
            Mono<Void> attrUpdateMono = handle(newState, eventDetails1, eventChecker1);


            EventDetails<T> eventDetails2 = new EventDetails<>(newState, STATE_CHANGE_EVENT_SUFFIX);
            TmForumEventChecker eventChecker2 = new StateChangeTmForumEventChecker<>(newState, oldState);
            Mono<Void> stateChangeMono = handle(newState, eventDetails2, eventChecker2);

            return attrUpdateMono.then(stateChangeMono);
        } catch (EventHandlingException e) {
            return Mono.empty();
        }
    }

    private  <T> Event createEvent(EventDetails<T> eventDetails, Map<String, Object> payload) {
        Event event = new Event();

        event.setEventType(eventDetails.eventType);
        event.setEventId(UUID.randomUUID().toString());
        event.setEvent(Map.of(eventDetails.entityType, payload));
        event.setEventTime(eventDetails.eventTime);

        return event;
    }

    private TMForumNotification createNotification(EntityVO payload, String eventTypeSuffix, Instant eventTime,
                                                   List<String> selectedFields, URI listenerCallback) {
        EventDetails<EntityVO> eventDetails = new EventDetails<>(payload.getType(),
                eventTypeSuffix, eventTime);
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

        List<NgsiLdEventChecker> ngsiLdEventCheckers = buildNgsiLdEventCheckers(relevantEventTypes);
        notificationVO.getData().forEach(entityVO -> ngsiLdEventCheckers.forEach(eventChecker -> {
            if (eventChecker.wasFired(entityVO)) {
                notifications.add(createNotification(entityVO, eventChecker.getEventTypeSuffix(),
                        notificationVO.getNotifiedAt(), selectedFields == null ? List.of()
                                : Arrays.stream(selectedFields.split(",")).toList(),
                        URI.create(listenerCallback)));
            }
        }));

        notificationSender.sendNotifications(notifications);
    }

    private List<NgsiLdEventChecker> buildNgsiLdEventCheckers(String relevantEventTypes) throws EventHandlingException {
        List<NgsiLdEventChecker> ngsiLdEventCheckers = new ArrayList<>();
        Arrays.stream(relevantEventTypes.split(",")).forEach(relevantEventType -> {
            if (relevantEventType.contains(CREATE_EVENT_SUFFIX)) {
                ngsiLdEventCheckers.add(new CreateNgsiLdEventChecker());
            } else if (relevantEventType.contains(ATTRIBUTE_VALUE_CHANGE_EVENT_SUFFIX)) {
                ngsiLdEventCheckers.add(new AttributeValueChangeNgsiLdEventChecker());
            } else if (relevantEventType.contains(STATE_CHANGE_EVENT_SUFFIX)) {
                ngsiLdEventCheckers.add(new StateChangeNgsiLdEventChecker());
            } else if (relevantEventType.contains(DELETE_EVENT_SUFFIX)) {
                ngsiLdEventCheckers.add(new DeleteNgsiLdEventChecker());
            } else {
                throw new EventHandlingException("Event type was not recognized.");
            }
        });
        return ngsiLdEventCheckers;
    }

    private static class EventDetails<T> {
        String entityType;
        String eventType;
        Instant eventTime;

        public EventDetails(T entity, String eventSuffix) {
            entityType = getEntityType(entity);
            this.eventTime = Instant.now();
            init(eventSuffix);
        }

        public EventDetails(String entityType, String eventSuffix, Instant eventTime) {
            this.entityType = entityType;
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
        }
    }
}
