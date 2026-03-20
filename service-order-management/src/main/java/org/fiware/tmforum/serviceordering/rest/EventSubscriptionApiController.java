package org.fiware.tmforum.serviceordering.rest;

import io.github.wistefan.mapping.EntityVOMapper;
import io.micronaut.core.annotation.NonNull;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.annotation.Controller;
import lombok.extern.slf4j.Slf4j;
import org.fiware.serviceordering.api.EventsSubscriptionApi;
import org.fiware.serviceordering.model.EventSubscriptionInputVO;
import org.fiware.serviceordering.model.EventSubscriptionVO;
import org.fiware.tmforum.common.configuration.GeneralProperties;
import org.fiware.tmforum.common.domain.subscription.TMForumSubscription;
import org.fiware.tmforum.common.mapping.SubscriptionMapper;
import org.fiware.tmforum.common.notification.NgsiLdEventHandler;
import org.fiware.tmforum.common.notification.TMForumEventHandler;
import org.fiware.tmforum.common.querying.QueryParser;
import org.fiware.tmforum.common.repository.TmForumRepository;
import org.fiware.tmforum.common.rest.AbstractSubscriptionApiController;
import org.fiware.tmforum.common.validation.ReferenceValidationService;
import org.fiware.tmforum.serviceordering.TMForumMapper;
import org.fiware.tmforum.serviceordering.domain.CancelServiceOrder;
import org.fiware.tmforum.serviceordering.domain.ServiceOrder;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

import static java.util.Map.entry;
import static org.fiware.tmforum.common.notification.EventConstants.EVENT_GROUP_CANCEL_SERVICE_ORDER;
import static org.fiware.tmforum.common.notification.EventConstants.EVENT_GROUP_SERVICE_ORDER;

/**
 * Controller handling event subscription endpoints for the Service Order Management API.
 */
@Slf4j
@Controller("${api.service-order-management.basepath:/}")
public class EventSubscriptionApiController extends AbstractSubscriptionApiController implements EventsSubscriptionApi {
	private final TMForumMapper tmForumMapper;
	private static final Map<String, String> EVENT_GROUP_TO_ENTITY_NAME_MAPPING = Map.ofEntries(
			entry(EVENT_GROUP_CANCEL_SERVICE_ORDER, CancelServiceOrder.TYPE_CANCEL_SERVICE_ORDER),
			entry(EVENT_GROUP_SERVICE_ORDER, ServiceOrder.TYPE_SERVICE_ORDER)
	);
	private static final List<String> EVENT_GROUPS = List.of(
			EVENT_GROUP_CANCEL_SERVICE_ORDER, EVENT_GROUP_SERVICE_ORDER);

	/**
	 * Creates the event subscription API controller.
	 *
	 * @param queryParser         the query parser for NGSI-LD queries
	 * @param validationService   the reference validation service
	 * @param repository          the TM Forum repository
	 * @param tmForumMapper       the mapper between VOs and domain objects
	 * @param tmForumEventHandler the TM Forum event handler
	 * @param ngsiLdEventHandler  the NGSI-LD event handler
	 * @param generalProperties   the general configuration properties
	 * @param entityVOMapper      the entity VO mapper
	 * @param subscriptionMapper  the subscription mapper
	 */
	public EventSubscriptionApiController(QueryParser queryParser, ReferenceValidationService validationService,
										  TmForumRepository repository, TMForumMapper tmForumMapper,
										  TMForumEventHandler tmForumEventHandler, NgsiLdEventHandler ngsiLdEventHandler,
										  GeneralProperties generalProperties, EntityVOMapper entityVOMapper, SubscriptionMapper subscriptionMapper) {
		super(queryParser, validationService, repository, EVENT_GROUP_TO_ENTITY_NAME_MAPPING, tmForumEventHandler, ngsiLdEventHandler,
				generalProperties, entityVOMapper, subscriptionMapper);
		this.tmForumMapper = tmForumMapper;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Mono<HttpResponse<EventSubscriptionVO>> registerListener(
			@NonNull EventSubscriptionInputVO eventSubscriptionInputVO) {
		TMForumSubscription subscription = buildSubscription(eventSubscriptionInputVO.getCallback(),
				eventSubscriptionInputVO.getQuery(), EVENT_GROUPS);

		return create(subscription)
				.map(tmForumMapper::map)
				.map(HttpResponse::created);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Mono<HttpResponse<Object>> unregisterListener(@NonNull String id) {
		return delete(id);
	}
}
