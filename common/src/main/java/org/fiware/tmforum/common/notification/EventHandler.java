package org.fiware.tmforum.common.notification;

import io.micronaut.context.annotation.Bean;
import io.micronaut.http.HttpResponse;
import lombok.RequiredArgsConstructor;
import org.fiware.ngsi.model.EntityVO;
import org.fiware.tmforum.common.domain.subscription.Subscription;
import org.fiware.tmforum.common.repository.TmForumRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

import static org.fiware.tmforum.common.CommonConstants.DEFAULT_LIMIT;
import static org.fiware.tmforum.common.CommonConstants.DEFAULT_OFFSET;

@Bean
@RequiredArgsConstructor
public class EventHandler {
    private final TmForumRepository repository;
    private final NotificationSender notificationSender;

    private Mono<List<Subscription>> getSubscriptions(String entityType, String eventType) {
        return repository.findEntities(
                DEFAULT_OFFSET,
                DEFAULT_LIMIT,
                Subscription.TYPE_SUBSCRIPTION,
                Subscription.class,
                String.format("entities==\"%s\";eventTypes==\"%s\"", entityType, eventType)
        );
    }

    public <T> Mono<List<HttpResponse<String>>> handleCreateEvent(T entity) {
        String entityType = notificationSender.getEntityType(entity);
        if (entityType.equals(Subscription.TYPE_SUBSCRIPTION)) {
            return Mono.empty();
        }

        String eventType = notificationSender.buildCreateEventType(entityType);
        return getSubscriptions(entityType, eventType)
                .flatMap(subscriptions -> notificationSender.handleCreateEvent(subscriptions, entity));
    }

    public <T> Mono<List<HttpResponse<String>>> handleUpdateEvent(T newState, T oldState) {
        String entityType = notificationSender.getEntityType(newState);
        if (entityType.equals(Subscription.TYPE_SUBSCRIPTION)) {
            return Mono.empty();
        }

        String buildAttributeValueChangeEventType = notificationSender.buildAttributeValueChangeEventType(entityType);
        Flux<HttpResponse<String>> attrUpdateMono = getSubscriptions(entityType, buildAttributeValueChangeEventType)
                .flatMapMany(subscriptions ->
                        notificationSender.handleUpdateEvent(subscriptions, oldState, newState));

        String stateChangeEventType = notificationSender.buildStateChangeEventType(entityType);
        Flux<HttpResponse<String>> stateChangeMono = getSubscriptions(entityType, stateChangeEventType)
                .flatMapMany(subscriptions ->
                        notificationSender.handleStateChangeEvent(subscriptions, oldState, newState));
        return Flux.merge(attrUpdateMono, stateChangeMono).collectList();
    }

    public Mono<List<HttpResponse<String>>> handleDeleteEvent(EntityVO entityVO) {
        String entityType = entityVO.getType();
        if (entityType.equals(Subscription.TYPE_SUBSCRIPTION)) {
            return Mono.empty();
        }

        String eventType = notificationSender.buildDeleteEventType(entityType);
        return getSubscriptions(entityType, eventType)
                .flatMap(subscriptions -> notificationSender.handleDeleteEvent(subscriptions, entityVO));
    }
}
