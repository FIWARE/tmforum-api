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
import reactor.core.publisher.Flux;
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
                    .map(subscriptions -> subscriptions.stream()
                        .filter(subscription -> eventChecker.wasFired(subscription.getQuery()))
                        .map(subscription -> new TMForumNotification(subscription.getCallback(),
                                createEvent(eventDetails, applyFieldsFilter(entity, subscription.getFields()))))
                        .toList()).flatMap(notificationSender::sendNotifications);
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

    private Mono<TMForumNotification> createNotification(EntityVO payload, String eventTypeSuffix, Instant eventTime,
                                                   List<String> selectedFields, URI listenerCallback,
                                                   Map<String, Class<?>> entityNameToEntityClassMapping) {
        EventDetails<EntityVO> eventDetails = new EventDetails<>(payload.getType(),
                eventTypeSuffix, eventTime);
        return entityVOMapper.fromEntityVO(payload, entityNameToEntityClassMapping.get(payload.getType()))
                .map(entity -> new TMForumNotification(listenerCallback, createEvent(eventDetails, applyFieldsFilter(
                        entity, selectedFields))));
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

    public Mono<Void> handleNgsiLdNotification(NotificationVO notificationVO, String relevantEventTypes,
                                         String listenerCallback, String selectedFields,
                                         Map<String, Class<?>> entityNameToEntityClassMapping) {
        List<Mono<TMForumNotification>> notifications = notificationVO.getData().stream().flatMap(entityVO ->
                buildNgsiLdEventCheckers(relevantEventTypes).stream().filter(eventChecker -> eventChecker.wasFired(entityVO))
                        .map(eventChecker -> createNotification(
                                entityVO, eventChecker.getEventTypeSuffix(), notificationVO.getNotifiedAt(),
                                selectedFields == null ? List.of() : Arrays.stream(selectedFields.split(",")).toList(),
                                URI.create(listenerCallback), entityNameToEntityClassMapping))).toList();

        return Flux.fromIterable(notifications).flatMap(x -> x).collectList()
                .flatMap(notificationSender::sendNotifications);
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
