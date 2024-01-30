package org.fiware.tmforum.common.rest;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.github.wistefan.mapping.EntityVOMapper;
import io.micronaut.cache.annotation.CacheInvalidate;
import io.micronaut.core.annotation.NonNull;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.annotation.Body;
import io.micronaut.http.annotation.Consumes;
import io.micronaut.http.annotation.Post;
import io.micronaut.http.client.exceptions.HttpClientResponseException;
import lombok.extern.slf4j.Slf4j;
import org.fiware.ngsi.model.NotificationVO;
import org.fiware.tmforum.common.CommonConstants;
import org.fiware.tmforum.common.configuration.GeneralProperties;
import org.fiware.tmforum.common.domain.subscription.*;
import org.fiware.tmforum.common.exception.TmForumException;
import org.fiware.tmforum.common.exception.TmForumExceptionReason;
import org.fiware.tmforum.common.notification.EventConstants;
import org.fiware.tmforum.common.notification.NgsiLdEventHandler;
import org.fiware.tmforum.common.notification.TMForumEventHandler;
import org.fiware.tmforum.common.querying.QueryParser;
import org.fiware.tmforum.common.querying.SubscriptionQuery;
import org.fiware.tmforum.common.querying.SubscriptionQueryParser;
import org.fiware.tmforum.common.repository.TmForumRepository;
import org.fiware.tmforum.common.validation.ReferenceValidationService;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Slf4j
public abstract class AbstractSubscriptionApiController extends AbstractApiController<TMForumSubscription> {
    private static final String RECEIVER_INFO_LISTENER_ENDPOINT = "Listener-Endpoint";

    private final Map<String, String> eventGroupToEntityNameMapping;
    private final Map<String, Class<?>> entityNameToEntityClassMapping;
    private final GeneralProperties generalProperties;
    protected final EntityVOMapper entityVOMapper;
    private final NgsiLdEventHandler ngsiLdEventHandler;

    public AbstractSubscriptionApiController(
            QueryParser queryParser, ReferenceValidationService validationService, TmForumRepository repository,
            Map<String, String> eventGroupToEntityNameMapping, Map<String, Class<?>> entityNameToEntityClassMapping,
            TMForumEventHandler tmForumEventHandler, NgsiLdEventHandler ngsiLdEventHandler,
            GeneralProperties generalProperties, EntityVOMapper entityVOMapper) {
        super(queryParser, validationService, repository, tmForumEventHandler);
        this.eventGroupToEntityNameMapping = eventGroupToEntityNameMapping;
        this.entityNameToEntityClassMapping = entityNameToEntityClassMapping;
        this.generalProperties = generalProperties;
        this.entityVOMapper = entityVOMapper;
        this.ngsiLdEventHandler = ngsiLdEventHandler;
    }

    @CacheInvalidate(value = CommonConstants.TMFORUM_SUBSCRIPTIONS_CACHE_NAME, all = true)
    protected Mono<TMForumSubscription> create(TMForumSubscription tmForumSubscription) {
        return findExistingTMForumSubscription(tmForumSubscription)
                .switchIfEmpty(
                        create(Mono.just(tmForumSubscription.getSubscription()))
                                .then(create(Mono.just(tmForumSubscription), TMForumSubscription.class)));
    }

    private Mono<Subscription> create(Mono<Subscription> subscriptionMono) {
        return subscriptionMono
                .flatMap(checkedResult -> repository.createDomainSubscription(checkedResult)
                        .then(Mono.just(checkedResult)))
                .onErrorMap(t -> {
                    if (t instanceof HttpClientResponseException e) {
                        return switch (e.getStatus()) {
                            case CONFLICT -> new TmForumException(
                                    String.format("Conflict on creating the subscription: %s", e.getMessage()),
                                    TmForumExceptionReason.CONFLICT);
                            case BAD_REQUEST -> new TmForumException(
                                    String.format("Did not receive a valid subscription: %s.", e.getMessage()),
                                    TmForumExceptionReason.INVALID_DATA);
                            default -> new TmForumException(
                                    String.format("Unspecified downstream error: %s", e.getMessage()),
                                    TmForumExceptionReason.UNKNOWN);
                        };
                    } else {
                        return t;
                    }
                })
                .cast(Subscription.class);
    }

    private Mono<TMForumSubscription> findExistingTMForumSubscription(TMForumSubscription subscription) {
        String query = String.format(queryParser.toNgsiLdQuery(TMForumSubscription.class, "callback=%s&rawQuery=%s"),
                subscription.getCallback(), subscription.getRawQuery());

        return repository.findEntities(CommonConstants.DEFAULT_OFFSET, 1, TMForumSubscription.TYPE_TM_FORUM_SUBSCRIPTION,
                    TMForumSubscription.class, query)
                .flatMap(list -> list.isEmpty() ? Mono.empty() :
                        Mono.error(new TmForumException("Such subscription already exists.", TmForumExceptionReason.CONFLICT)));
    }

    protected TMForumSubscription buildSubscription(String callback, String query, List<String> eventGroups) {
        log.debug(query);
        SubscriptionQuery subscriptionQuery = SubscriptionQueryParser.parse(query, eventGroups);
        List<String> entities = subscriptionQuery.getEventGroups().stream()
                .map(eventGroup -> {
                    if (eventGroupToEntityNameMapping.containsKey(eventGroup)) {
                        return eventGroupToEntityNameMapping.get(eventGroup);
                    } else {
                        throw new TmForumException("Such subscription already exists.",
                                TmForumExceptionReason.INVALID_DATA);
                    }
                }).toList();

        TMForumSubscription tmForumSubscription = new TMForumSubscription(UUID.randomUUID().toString());
        tmForumSubscription.setRawQuery(query != null ? query : "");
        tmForumSubscription.setEventTypes(subscriptionQuery.getEventTypes());
        tmForumSubscription.setEntities(entities);
        tmForumSubscription.setQuery(subscriptionQuery.getQuery());
        tmForumSubscription.setCallback(URI.create(callback));
        tmForumSubscription.setFields(subscriptionQuery.getFields());
        tmForumSubscription.setSubscription(buildNgsiLdSubscription(callback, subscriptionQuery, entities));
        return tmForumSubscription;
    }

    private Subscription buildNgsiLdSubscription(String callback, SubscriptionQuery subscriptionQuery, List<String> entities) {
        Subscription subscription = new Subscription(UUID.randomUUID().toString());
        subscription.setQ(subscriptionQuery.getQuery());
        subscription.setEntities(entities.stream().map(entityType -> {
            EntityInfo entityInfo = new EntityInfo();
            entityInfo.setType(entityType);
            return entityInfo;
        }).toList());
        subscription.setNotification(buildNotificationParams(callback, subscriptionQuery));
        return subscription;
    }

    private NotificationParams buildNotificationParams(String callback, SubscriptionQuery subscriptionQuery) {
        NotificationParams notificationParams = new NotificationParams();
        notificationParams.setAttributes(Set.copyOf(subscriptionQuery.getFields()));
        notificationParams.setFormat(EventConstants.NOTIFICATION_FORMAT);
        notificationParams.setEndpoint(
                new Endpoint(
                    getCallbackURI(),
                    EventConstants.NOTIFICATION_PAYLOAD_MIME_TYPE,
                    List.of(new KeyValuePair(RECEIVER_INFO_LISTENER_ENDPOINT, callback))));
        return notificationParams;
    }

    @Override
    @CacheInvalidate(value = CommonConstants.TMFORUM_SUBSCRIPTIONS_CACHE_NAME, all = true)
    protected Mono<HttpResponse<Object>> delete(String id) {
        return repository.deleteDomainSubscriptionByTmForumSubscription(URI.create(id))
                .then(super.delete(id));
    }

    private URI getCallbackURI() {
        return URI.create(generalProperties.getServerHost() + generalProperties.getBasepath() +
                EventConstants.SUBSCRIPTION_CALLBACK_PATH);
    }

    @Post(EventConstants.SUBSCRIPTION_CALLBACK_PATH)
    @Consumes({"application/json;charset=utf-8"})
    public Mono<HttpResponse<Void>> callback(@NonNull @Body String payload) {
        log.debug("Callback for NGSI-LD subscription");

        try {
            NotificationVO notificationVO = this.entityVOMapper.readNotificationFromJSON(payload);

            log.debug(String.format("Callback for subscription %s with notification %s", notificationVO.getSubscriptionId(), payload));

            assert !notificationVO.getData().isEmpty();

            return this.repository.retrieveSubscriptionById(notificationVO.getSubscriptionId())
                    .flatMap(subscriptionVO -> ngsiLdEventHandler.handle(notificationVO, subscriptionVO,
                            entityNameToEntityClassMapping))
                    .then(Mono.just(HttpResponse.noContent()));
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }
}
