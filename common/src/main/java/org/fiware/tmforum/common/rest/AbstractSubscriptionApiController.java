package org.fiware.tmforum.common.rest;

import io.micronaut.cache.annotation.CacheInvalidate;
import io.micronaut.http.HttpResponse;
import lombok.extern.slf4j.Slf4j;
import org.fiware.tmforum.common.CommonConstants;
import org.fiware.tmforum.common.domain.subscription.TMForumSubscription;
import org.fiware.tmforum.common.exception.TmForumException;
import org.fiware.tmforum.common.exception.TmForumExceptionReason;
import org.fiware.tmforum.common.notification.EventHandler;
import org.fiware.tmforum.common.querying.QueryParser;
import org.fiware.tmforum.common.querying.SubscriptionQuery;
import org.fiware.tmforum.common.querying.SubscriptionQueryParser;
import org.fiware.tmforum.common.repository.TmForumRepository;
import org.fiware.tmforum.common.validation.ReferenceValidationService;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.fiware.tmforum.common.CommonConstants.DEFAULT_OFFSET;

@Slf4j
public abstract class AbstractSubscriptionApiController extends AbstractApiController<TMForumSubscription> {
    private final Map<String, String> eventGroupToEntityNameMapping;

    public AbstractSubscriptionApiController(QueryParser queryParser, ReferenceValidationService validationService, TmForumRepository repository,
                                             Map<String, String> eventGroupToEntityNameMapping, EventHandler eventHandler) {
        super(queryParser, validationService, repository, eventHandler);
        this.eventGroupToEntityNameMapping = eventGroupToEntityNameMapping;
    }

    @CacheInvalidate(value = CommonConstants.SUBSCRIPTIONS_CACHE_NAME, all = true)
    protected Mono<TMForumSubscription> create(TMForumSubscription subscription) {
        return findExistingSubscription(subscription)
                .switchIfEmpty(create(Mono.just(subscription), TMForumSubscription.class));
    }

    private Mono<TMForumSubscription> findExistingSubscription(TMForumSubscription subscription) {
        String query = String.format(queryParser.toNgsiLdQuery(TMForumSubscription.class, "callback=%s&rawQuery=%s"),
                subscription.getCallback(), subscription.getRawQuery());

        return repository.findEntities(DEFAULT_OFFSET, 1, TMForumSubscription.TYPE_TM_FORUM_SUBSCRIPTION,
                    TMForumSubscription.class, query)
                .flatMap(list -> list.isEmpty() ? Mono.empty() :
                        Mono.error(new TmForumException("Such subscription already exists.", TmForumExceptionReason.CONFLICT)));
    }

    protected TMForumSubscription buildSubscription(String callback, String query, List<String> eventGroups) {
        log.debug(query);
        SubscriptionQuery subscriptionQuery = SubscriptionQueryParser.parse(query, eventGroups);

        String subId = UUID.randomUUID().toString();
        TMForumSubscription subscription = new TMForumSubscription(subId);
        subscription.setRawQuery(query != null ? query : "");
        subscription.setEventTypes(subscriptionQuery.getEventTypes());
        subscription.setEntities(subscriptionQuery.getEventGroups().stream()
                .map(eventGroup -> {
                    if (eventGroupToEntityNameMapping.containsKey(eventGroup)) {
                        return eventGroupToEntityNameMapping.get(eventGroup);
                    } else {
                        throw new TmForumException("Such subscription already exists.",
                                TmForumExceptionReason.INVALID_DATA);
                    }
                }).toList());
        subscription.setQuery(subscriptionQuery.getQuery());
        subscription.setCallback(URI.create(callback));
        subscription.setFields(subscriptionQuery.getFields());
        return subscription;
    }

    @Override
    @CacheInvalidate(value = CommonConstants.SUBSCRIPTIONS_CACHE_NAME, all = true)
    protected Mono<HttpResponse<Object>> delete(String id) {
        return super.delete(id);
    }
}
