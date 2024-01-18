package org.fiware.tmforum.common.notification;

import com.google.common.base.CaseFormat;
import io.github.wistefan.mapping.EntityVOMapper;
import io.micronaut.cache.annotation.Cacheable;
import io.micronaut.context.annotation.Bean;
import org.fiware.tmforum.common.CommonConstants;
import org.fiware.tmforum.common.domain.subscription.TMForumSubscription;
import org.fiware.tmforum.common.exception.EventHandlingException;
import org.fiware.tmforum.common.notification.checkers.tmforum.AttributeValueChangeTmForumEventChecker;
import org.fiware.tmforum.common.notification.checkers.tmforum.CreateTmForumEventChecker;
import org.fiware.tmforum.common.notification.checkers.tmforum.StateChangeTmForumEventChecker;
import org.fiware.tmforum.common.notification.checkers.tmforum.TmForumEventChecker;
import org.fiware.tmforum.common.querying.QueryParser;
import org.fiware.tmforum.common.querying.SubscriptionQueryResolver;
import org.fiware.tmforum.common.repository.TmForumRepository;
import reactor.core.publisher.Mono;

import java.lang.reflect.InvocationTargetException;
import java.time.Instant;
import java.util.List;

import static org.fiware.tmforum.common.CommonConstants.DEFAULT_OFFSET;
import static org.fiware.tmforum.common.notification.EventConstants.*;

@Bean
public class TMForumEventHandler extends EventHandler {

    private final QueryParser queryParser;
    private final SubscriptionQueryResolver subscriptionQueryResolver;
    private final TmForumRepository repository;

    public TMForumEventHandler(QueryParser queryParser, SubscriptionQueryResolver subscriptionQueryResolver,
                               NotificationSender notificationSender, EntityVOMapper entityVOMapper,
                               TmForumRepository repository) {
        super(notificationSender, entityVOMapper);
        this.queryParser = queryParser;
        this.subscriptionQueryResolver = subscriptionQueryResolver;
        this.repository = repository;
    }

    @Cacheable(CommonConstants.SUBSCRIPTIONS_CACHE_NAME)
    public Mono<List<TMForumSubscription>> getSubscriptions(EventDetails eventDetails) {
        return repository.findEntities(
                DEFAULT_OFFSET,
                100,
                TMForumSubscription.TYPE_TM_FORUM_SUBSCRIPTION,
                TMForumSubscription.class,
                queryParser.toNgsiLdQuery(TMForumSubscription.class,
                        String.format("entities=%s&eventTypes=%s", eventDetails.entityType(), eventDetails.eventType()))
        );
    }

    private <T> Mono<Void> handle(T entity, EventDetails eventDetails, TmForumEventChecker eventChecker) {
        return getSubscriptions(eventDetails)
                .map(subscriptions -> subscriptions.stream()
                        .filter(subscription -> eventChecker.wasFired(subscription.getQuery()))
                        .map(subscription -> new TMForumNotification(subscription.getCallback(),
                                createEvent(eventDetails, applyFieldsFilter(entity, subscription.getFields()))))
                        .toList()).flatMap(notificationSender::sendNotifications);
    }

    public <T> Mono<Void> handleCreateEvent(T entity) {
        try {
            EventDetails eventDetails = makeEventDetails(entity, CREATE_EVENT_SUFFIX);
            TmForumEventChecker tmForumEventChecker = new CreateTmForumEventChecker<>(subscriptionQueryResolver, entity,
                    eventDetails.payloadName());
            return handle(entity, eventDetails, tmForumEventChecker);
        } catch (EventHandlingException e) {
            return Mono.empty();
        }
    }

    public <T> Mono<Void> handleUpdateEvent(T newState, T oldState) {
        try {
            EventDetails eventDetails1 = makeEventDetails(newState, ATTRIBUTE_VALUE_CHANGE_EVENT_SUFFIX);
            TmForumEventChecker eventChecker1 = new AttributeValueChangeTmForumEventChecker<>(
                    subscriptionQueryResolver, newState, oldState, eventDetails1.payloadName());
            Mono<Void> attrUpdateMono = handle(newState, eventDetails1, eventChecker1);


            EventDetails eventDetails2 = makeEventDetails(newState, STATE_CHANGE_EVENT_SUFFIX);
            TmForumEventChecker eventChecker2 = new StateChangeTmForumEventChecker<>(newState, oldState);
            Mono<Void> stateChangeMono = handle(newState, eventDetails2, eventChecker2);

            return attrUpdateMono.then(stateChangeMono);
        } catch (EventHandlingException e) {
            return Mono.empty();
        }
    }

    private <T> EventDetails makeEventDetails(T entity, String eventSuffix) {
        String entityType = getEntityType(entity);
        if (entityType.equals(TMForumSubscription.TYPE_TM_FORUM_SUBSCRIPTION)) {
            throw new EventHandlingException();
        }
        Instant eventTime = Instant.now();
        String payloadName = CaseFormat.LOWER_HYPHEN.to(CaseFormat.LOWER_CAMEL, entityType);
        String eventType = CaseFormat.LOWER_CAMEL.to(CaseFormat.UPPER_CAMEL, payloadName) + eventSuffix;
        return new EventDetails(entityType, eventType, eventTime, payloadName);
    }

    private <T> String getEntityType(T entity) {
        try {
            return entity.getClass().getMethod("getType").invoke(entity).toString();
        } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            return "";
        }
    }

}
