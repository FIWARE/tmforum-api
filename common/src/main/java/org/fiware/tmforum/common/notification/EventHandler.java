package org.fiware.tmforum.common.notification;

import io.micronaut.cache.annotation.Cacheable;
import io.micronaut.context.annotation.Bean;
import lombok.RequiredArgsConstructor;
import org.fiware.ngsi.model.EntityVO;
import org.fiware.tmforum.common.domain.subscription.Subscription;
import org.fiware.tmforum.common.querying.QueryParser;
import org.fiware.tmforum.common.repository.TmForumRepository;
import reactor.core.publisher.Mono;

import java.util.List;

import static org.fiware.tmforum.common.CommonConstants.DEFAULT_LIMIT;
import static org.fiware.tmforum.common.CommonConstants.DEFAULT_OFFSET;

@Bean
@RequiredArgsConstructor
public class EventHandler {
    public static final String SUBSCRIPTIONS_CACHE_NAME = "subscriptions";

    private final TmForumRepository repository;
    private final NotificationSender notificationSender;

    @Cacheable(SUBSCRIPTIONS_CACHE_NAME)
    public Mono<List<Subscription>> getSubscriptions(String entityType, String eventType) {
        return repository.findEntities(
                DEFAULT_OFFSET,
                DEFAULT_LIMIT,
                Subscription.TYPE_SUBSCRIPTION,
                Subscription.class,
                QueryParser.toNgsiLdQuery(Subscription.class,
                        String.format("entities=%s&eventTypes=%s", entityType, eventType))
        );
    }

    public <T> Mono<Void> handleCreateEvent(T entity) {
        String entityType = notificationSender.getEntityType(entity);
        if (entityType.equals(Subscription.TYPE_SUBSCRIPTION)) {
            return Mono.empty();
        }

        String eventType = notificationSender.buildCreateEventType(entityType);
        return getSubscriptions(entityType, eventType)
                .doOnNext(subscriptions -> notificationSender.handleCreateEvent(subscriptions, entity))
                .then();
    }

    public <T> Mono<Void> handleUpdateEvent(T newState, T oldState) {
        String entityType = notificationSender.getEntityType(newState);
        if (entityType.equals(Subscription.TYPE_SUBSCRIPTION)) {
            return Mono.empty();
        }

        String buildAttributeValueChangeEventType = notificationSender.buildAttributeValueChangeEventType(entityType);
        Mono<Void> attrUpdateMono = getSubscriptions(entityType, buildAttributeValueChangeEventType)
                .doOnNext(subscriptions ->
                        notificationSender.handleUpdateEvent(subscriptions, oldState, newState))
                .then();

        String stateChangeEventType = notificationSender.buildStateChangeEventType(entityType);
        Mono<Void> stateChangeMono = getSubscriptions(entityType, stateChangeEventType)
                .doOnNext(subscriptions ->
                        notificationSender.handleStateChangeEvent(subscriptions, oldState, newState))
                .then();
        return attrUpdateMono.then(stateChangeMono);
    }

    public Mono<Void> handleDeleteEvent(EntityVO entityVO) {
        String entityType = entityVO.getType();
        if (entityType.equals(Subscription.TYPE_SUBSCRIPTION)) {
            return Mono.empty();
        }

        String eventType = notificationSender.buildDeleteEventType(entityType);
        return getSubscriptions(entityType, eventType)
                .doOnNext(subscriptions -> notificationSender.handleDeleteEvent(subscriptions, entityVO))
                .then();
    }
}
