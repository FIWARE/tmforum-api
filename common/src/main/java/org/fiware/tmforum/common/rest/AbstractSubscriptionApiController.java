package org.fiware.tmforum.common.rest;

import lombok.extern.slf4j.Slf4j;
import org.fiware.tmforum.common.EventConstants;
import org.fiware.tmforum.common.domain.subscription.Subscription;
import org.fiware.tmforum.common.querying.QueryParserTmp;
import org.fiware.tmforum.common.querying.SubscriptionQuery;
import org.fiware.tmforum.common.querying.QueryParser;
import org.fiware.tmforum.common.repository.TmForumRepository;
import org.fiware.tmforum.common.util.StringUtils;
import org.fiware.tmforum.common.validation.ReferenceValidationService;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.fiware.tmforum.common.CommonConstants.DEFAULT_OFFSET;

@Slf4j
public abstract class AbstractSubscriptionApiController extends AbstractApiController<Subscription> {
    private final Map<String, String> eventGroupToEntityNameMapping;

    public AbstractSubscriptionApiController(ReferenceValidationService validationService, TmForumRepository repository,
                                             Map<String, String> eventGroupToEntityNameMapping) {
        super(validationService, repository);
        this.eventGroupToEntityNameMapping = eventGroupToEntityNameMapping;
    }

    protected Mono<Subscription> create(Subscription subscription) {
        return findExistingSubscription(subscription)
                .switchIfEmpty(create(Mono.just(subscription), Subscription.class));
    }

    private Mono<Subscription> findExistingSubscription(Subscription subscription) {
        String query = String.format("callback==\"%s\";query==\"%s\";%s",
                subscription.getCallback(), subscription.getQuery(),
                String.join(";", subscription.getEventTypes().stream().map(eventType ->
                    String.format("eventTypes==\"%s\"", eventType)).toList()));
        return repository.findEntities(DEFAULT_OFFSET, 1, Subscription.TYPE_SUBSCRIPTION,
                    Subscription.class, query)
                .flatMap(list -> list.isEmpty() ? Mono.empty() : Mono.just(list.get(0)));
    }

    protected Subscription buildSubscription(String callback, String query, List<String> eventGroups) {
        SubscriptionQuery subscriptionQuery = QueryParserTmp.parseNotificationQuery(query, eventGroups);
        log.debug(subscriptionQuery.toString());

        List<String> eventTypes;
        if (subscriptionQuery.getEventTypes().isEmpty()) {
            eventTypes = EventConstants.ALLOWED_EVENT_TYPES.get(subscriptionQuery.getEventGroupName()).stream().map(
                    eventType -> subscriptionQuery.getEventGroupName() + eventType).toList();
        } else {
            eventTypes = subscriptionQuery.getEventTypes();
        }

        String subId = UUID.randomUUID().toString();
        Subscription subscription = new Subscription(subId);
        subscription.setEventTypes(eventTypes);
        subscription.setEntities(List.of(eventGroupToEntityNameMapping.get(subscriptionQuery.getEventGroupName())));
        subscription.setQuery(subscriptionQuery.getQuery());
        subscription.setPayloadName(StringUtils.decapitalize(subscriptionQuery.getEventGroupName()));
        subscription.setCallback(URI.create(callback));
        subscription.setFields(subscriptionQuery.getFields());
        return subscription;
    }
}
