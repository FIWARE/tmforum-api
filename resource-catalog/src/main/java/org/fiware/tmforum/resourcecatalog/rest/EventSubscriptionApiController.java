package org.fiware.tmforum.resourcecatalog.rest;

import io.micronaut.core.annotation.NonNull;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.annotation.Controller;
import lombok.extern.slf4j.Slf4j;
import org.fiware.resourcecatalog.api.EventsSubscriptionApi;
import org.fiware.resourcecatalog.model.EventSubscriptionInputVO;
import org.fiware.resourcecatalog.model.EventSubscriptionVO;
import org.fiware.tmforum.common.domain.subscription.Subscription;
import org.fiware.tmforum.common.repository.TmForumRepository;
import org.fiware.tmforum.common.rest.AbstractSubscriptionApiController;
import org.fiware.tmforum.common.validation.ReferenceValidationService;
import org.fiware.tmforum.resource.ResourceCandidate;
import org.fiware.tmforum.resource.ResourceCategory;
import org.fiware.tmforum.resource.ResourceSpecification;
import org.fiware.tmforum.resourcecatalog.TMForumMapper;
import org.fiware.tmforum.resourcecatalog.domain.ResourceCatalog;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

import static java.util.Map.entry;
import static org.fiware.tmforum.common.EventConstants.*;

@Slf4j
@Controller("${general.basepath:/}")
public class EventSubscriptionApiController extends AbstractSubscriptionApiController implements EventsSubscriptionApi {
    private final TMForumMapper tmForumMapper;
    private static final Map<String, String> EVENT_GROUP_TO_ENTITY_NAME_MAPPING = Map.ofEntries(
		entry(EVENT_GROUP_RESOURCE_CANDIDATE, ResourceCandidate.TYPE_RESOURCE_CANDIDATE),
		entry(EVENT_GROUP_RESOURCE_CATALOG, ResourceCatalog.TYPE_RESOURCE_CATALOG),
		entry(EVENT_GROUP_RESOURCE_CATEGORY, ResourceCategory.TYPE_RESOURCE_CATEGORY),
		entry(EVENT_GROUP_RESOURCE_SPECIFICATION, ResourceSpecification.TYPE_RESOURCE_SPECIFICATION)
	);
    private static final List<String> EVENT_GROUPS = List.of(
            EVENT_GROUP_RESOURCE_CANDIDATE, EVENT_GROUP_RESOURCE_CATALOG,
            EVENT_GROUP_RESOURCE_CATEGORY, EVENT_GROUP_RESOURCE_SPECIFICATION);

    public EventSubscriptionApiController(ReferenceValidationService validationService,
                                          TmForumRepository repository, TMForumMapper tmForumMapper) {
        super(validationService, repository, EVENT_GROUP_TO_ENTITY_NAME_MAPPING);
        this.tmForumMapper = tmForumMapper;
    }

    @Override
    public Mono<HttpResponse<EventSubscriptionVO>> registerListener(
            @NonNull EventSubscriptionInputVO eventSubscriptionInputVO) {
        Subscription subscription = buildSubscription(eventSubscriptionInputVO.getCallback(),
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
