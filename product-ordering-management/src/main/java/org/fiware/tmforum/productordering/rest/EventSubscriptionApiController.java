package org.fiware.tmforum.productordering.rest;

import io.micronaut.core.annotation.NonNull;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.annotation.Controller;
import lombok.extern.slf4j.Slf4j;
import org.fiware.productordering.api.EventsSubscriptionApi;
import org.fiware.productordering.model.EventSubscriptionInputVO;
import org.fiware.productordering.model.EventSubscriptionVO;
import org.fiware.tmforum.common.notification.EventHandler;
import org.fiware.tmforum.common.querying.QueryParser;
import org.fiware.tmforum.common.domain.subscription.Subscription;
import org.fiware.tmforum.common.repository.TmForumRepository;
import org.fiware.tmforum.common.rest.AbstractSubscriptionApiController;
import org.fiware.tmforum.common.validation.ReferenceValidationService;
import org.fiware.tmforum.productordering.TMForumMapper;
import org.fiware.tmforum.productordering.domain.CancelProductOrder;
import org.fiware.tmforum.productordering.domain.ProductOrder;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

import static java.util.Map.entry;
import static org.fiware.tmforum.common.notification.EventConstants.EVENT_GROUP_CANCEL_PRODUCT_ORDER;
import static org.fiware.tmforum.common.notification.EventConstants.EVENT_GROUP_PRODUCT_ORDER;

@Slf4j
@Controller("${general.basepath:/}")
public class EventSubscriptionApiController extends AbstractSubscriptionApiController implements EventsSubscriptionApi {
	private final TMForumMapper tmForumMapper;
	private static final Map<String, String> EVENT_GROUP_TO_ENTITY_NAME_MAPPING = Map.ofEntries(
		entry(EVENT_GROUP_CANCEL_PRODUCT_ORDER, CancelProductOrder.TYPE_CANCEL_PRODUCT_ORDER),
		entry(EVENT_GROUP_PRODUCT_ORDER, ProductOrder.TYPE_PRODUCT_ORDER)
	);
	private static final List<String> EVENT_GROUPS = List.of(
			EVENT_GROUP_CANCEL_PRODUCT_ORDER, EVENT_GROUP_PRODUCT_ORDER);

	public EventSubscriptionApiController(QueryParser queryParser, ReferenceValidationService validationService,
										  TmForumRepository repository, TMForumMapper tmForumMapper, EventHandler eventHandler) {
		super(queryParser, validationService, repository, EVENT_GROUP_TO_ENTITY_NAME_MAPPING, eventHandler);
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
