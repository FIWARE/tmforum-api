package org.fiware.tmforum.servicecatalog.rest;

import io.github.wistefan.mapping.EntityVOMapper;
import io.micronaut.core.annotation.NonNull;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.annotation.Controller;
import lombok.extern.slf4j.Slf4j;
import org.fiware.servicecatalog.api.EventsSubscriptionApi;
import org.fiware.servicecatalog.model.*;
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
import org.fiware.tmforum.service.ServiceCandidate;
import org.fiware.tmforum.service.ServiceCategory;
import org.fiware.tmforum.servicecatalog.TMForumMapper;
import org.fiware.tmforum.servicecatalog.domain.ServiceCatalog;
import org.fiware.tmforum.servicecatalog.domain.ServiceSpecification;
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
			entry(EVENT_GROUP_SERVICE_CANDIDATE, ServiceCandidate.TYPE_SERVICE_CANDIDATE),
			entry(EVENT_GROUP_SERVICE_CATALOG, ServiceCatalog.TYPE_SERVICE_CATALOG),
			entry(EVENT_GROUP_SERVICE_CATEGORY, ServiceCategory.TYPE_SERVICE_CATEGORY),
			entry(EVENT_GROUP_SERVICE_SPECIFICATION, ServiceSpecification.TYPE_SERVICE_SPECIFICATION)
	);
	private static final List<String> EVENT_GROUPS = List.of(EVENT_GROUP_SERVICE_CANDIDATE,
			EVENT_GROUP_SERVICE_CATALOG, EVENT_GROUP_SERVICE_CATEGORY, EVENT_GROUP_SERVICE_SPECIFICATION);
	private static final Map<String, EventMapping> ENTITY_NAME_TO_ENTITY_CLASS_MAPPING = Map.ofEntries(
			entry(ServiceCandidate.TYPE_SERVICE_CANDIDATE, new EventMapping(ServiceCandidateVO.class, ServiceCandidate.class)),
			entry(ServiceCatalog.TYPE_SERVICE_CATALOG, new EventMapping(ServiceCatalogVO.class, ServiceCatalog.class)),
			entry(ServiceCategory.TYPE_SERVICE_CATEGORY, new EventMapping(ServiceCategoryVO.class, ServiceCategory.class)),
			entry(ServiceSpecification.TYPE_SERVICE_SPECIFICATION, new EventMapping(ServiceSpecificationVO.class, ServiceSpecification.class))
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
		if (targetClass == ServiceCandidate.class) {
			return tmForumMapper.map((ServiceCandidate) rawPayload);
		}
		if (targetClass == ServiceCatalog.class) {
			return tmForumMapper.map((ServiceCatalog) rawPayload);
		}
		if (targetClass == ServiceCategory.class) {
			return tmForumMapper.map((ServiceCategory) rawPayload);
		}
		if (targetClass == ServiceSpecification.class) {
			return tmForumMapper.map((ServiceSpecification) rawPayload);
		}
		throw new TmForumException(String.format("Event-Payload %s is not supported.", rawPayload), TmForumExceptionReason.INVALID_DATA);
	}
}
