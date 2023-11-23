package org.fiware.tmforum.usagemanagement.rest;

import io.micronaut.core.annotation.NonNull;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.annotation.Controller;
import lombok.extern.slf4j.Slf4j;
import org.fiware.tmforum.common.querying.QueryParser;
import org.fiware.usagemanagement.api.EventsSubscriptionApi;
import org.fiware.usagemanagement.model.EventSubscriptionInputVO;
import org.fiware.usagemanagement.model.EventSubscriptionVO;
import org.fiware.tmforum.usagemanagement.TMForumMapper;
import org.fiware.tmforum.usagemanagement.domain.Usage;
import org.fiware.tmforum.usagemanagement.domain.UsageSpecification;
import org.fiware.tmforum.common.notification.EventHandler;
import org.fiware.tmforum.common.domain.subscription.TMForumSubscription;
import org.fiware.tmforum.common.repository.TmForumRepository;
import org.fiware.tmforum.common.rest.AbstractSubscriptionApiController;
import org.fiware.tmforum.common.validation.ReferenceValidationService;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

import static java.util.Map.entry;
import static org.fiware.tmforum.common.notification.EventConstants.EVENT_GROUP_USAGE;
import static org.fiware.tmforum.common.notification.EventConstants.EVENT_GROUP_USAGE_SPECIFICATION;

@Slf4j
@Controller("${general.basepath:/}")
public class EventSubscriptionApiController extends AbstractSubscriptionApiController implements EventsSubscriptionApi {
    private final TMForumMapper tmForumMapper;
    private static final Map<String, String> EVENT_GROUP_TO_ENTITY_NAME_MAPPING = Map.ofEntries(
            entry(EVENT_GROUP_USAGE, Usage.TYPE_U),
            entry(EVENT_GROUP_USAGE_SPECIFICATION, UsageSpecification.TYPE_USP)
    );
    private static final List<String> EVENT_GROUPS = List.of(
            EVENT_GROUP_USAGE, EVENT_GROUP_USAGE_SPECIFICATION);

    public EventSubscriptionApiController(QueryParser queryParser, ReferenceValidationService validationService,
                                          TmForumRepository repository, TMForumMapper tmForumMapper, EventHandler eventHandler) {
        super(queryParser, validationService, repository, EVENT_GROUP_TO_ENTITY_NAME_MAPPING, eventHandler);
        this.tmForumMapper = tmForumMapper;
    }

    @Override
    public Mono<HttpResponse<EventSubscriptionVO>> registerListener(
            @NonNull EventSubscriptionInputVO eventSubscriptionInputVO) {
        TMForumSubscription subscription = buildSubscription(eventSubscriptionInputVO.getCallback(),
                eventSubscriptionInputVO.getQuery(), EVENT_GROUPS);

        return create(subscription)
                .map(tmForumMapper::map)
                .map(HttpResponse::created);
    }

    @Override
    public Mono<HttpResponse<Object>> unregisterListener(@NonNull String id) {
        return delete(id);
    }
}