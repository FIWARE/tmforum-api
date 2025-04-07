package org.fiware.tmforum.resourcecatalog.rest;

import io.github.wistefan.mapping.EntityVOMapper;
import io.micronaut.core.annotation.NonNull;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.annotation.Controller;
import lombok.extern.slf4j.Slf4j;
import org.fiware.resourcecatalog.api.EventsSubscriptionApi;
import org.fiware.resourcecatalog.model.*;
import org.fiware.tmforum.common.configuration.GeneralProperties;
import org.fiware.tmforum.common.domain.subscription.TMForumSubscription;
import org.fiware.tmforum.common.exception.TmForumException;
import org.fiware.tmforum.common.exception.TmForumExceptionReason;
import org.fiware.tmforum.common.mapping.EventMapping;
import org.fiware.tmforum.common.mapping.SubscriptionMapper;
import org.fiware.tmforum.common.notification.NgsiLdEventHandler;
import org.fiware.tmforum.common.notification.TMForumEventHandler;
import org.fiware.tmforum.common.querying.QueryParser;
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
import static org.fiware.tmforum.common.notification.EventConstants.*;

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
	private static final Map<String, EventMapping> ENTITY_NAME_TO_ENTITY_CLASS_MAPPING = Map.ofEntries(
			entry(ResourceCandidate.TYPE_RESOURCE_CANDIDATE, new EventMapping(ResourceCandidateVO.class, ResourceCandidate.class)),
			entry(ResourceCatalog.TYPE_RESOURCE_CATALOG, new EventMapping(ResourceCatalogVO.class, ResourceCatalog.class)),
			entry(ResourceCategory.TYPE_RESOURCE_CATEGORY, new EventMapping(ResourceCategoryVO.class, ResourceCategory.class)),
			entry(ResourceSpecification.TYPE_RESOURCE_SPECIFICATION, new EventMapping(ResourceSpecificationVO.class, ResourceSpecification.class))
	);

	public EventSubscriptionApiController(QueryParser queryParser, ReferenceValidationService validationService,
										  TmForumRepository repository, TMForumMapper tmForumMapper,
										  TMForumEventHandler tmForumEventHandler, NgsiLdEventHandler ngsiLdEventHandler,
										  GeneralProperties generalProperties, EntityVOMapper entityVOMapper, SubscriptionMapper subscriptionMapper) {
		super(queryParser, validationService, repository, EVENT_GROUP_TO_ENTITY_NAME_MAPPING,
				ENTITY_NAME_TO_ENTITY_CLASS_MAPPING, tmForumEventHandler, ngsiLdEventHandler,
				generalProperties, entityVOMapper, subscriptionMapper);
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

	@Override
	public Object mapPayload(Object rawPayload, Class<?> targetClass) {
		if (targetClass == ResourceCandidate.class) {
			return tmForumMapper.map((ResourceCandidate) rawPayload);
		}
		if (targetClass == ResourceCatalog.class) {
			return tmForumMapper.map((ResourceCatalog) rawPayload);
		}
		if (targetClass == ResourceCategory.class) {
			return tmForumMapper.map((ResourceCategory) rawPayload);
		}
		if (targetClass == ResourceSpecification.class) {
			return tmForumMapper.map((ResourceSpecification) rawPayload);
		}
		throw new TmForumException(String.format("Event-Payload %s is not supported.", rawPayload), TmForumExceptionReason.INVALID_DATA);
	}
}
