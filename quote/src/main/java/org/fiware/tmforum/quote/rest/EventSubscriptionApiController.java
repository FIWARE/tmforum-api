package org.fiware.tmforum.quote.rest;

import io.github.wistefan.mapping.EntityVOMapper;
import io.micronaut.core.annotation.NonNull;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.annotation.Controller;
import lombok.extern.slf4j.Slf4j;
import org.fiware.quote.api.EventsSubscriptionApi;
import org.fiware.quote.model.EventSubscriptionInputVO;
import org.fiware.quote.model.EventSubscriptionVO;
import org.fiware.tmforum.common.configuration.GeneralProperties;
import org.fiware.tmforum.common.domain.subscription.TMForumSubscription;
import org.fiware.tmforum.common.mapping.SubscriptionMapper;
import org.fiware.tmforum.common.notification.NgsiLdEventHandler;
import org.fiware.tmforum.common.notification.TMForumEventHandler;
import org.fiware.tmforum.common.querying.QueryParser;
import org.fiware.tmforum.common.repository.TmForumRepository;
import org.fiware.tmforum.common.rest.AbstractSubscriptionApiController;
import org.fiware.tmforum.common.validation.ReferenceValidationService;
import org.fiware.tmforum.quote.TMForumMapper;
import org.fiware.tmforum.quote.domain.Quote;
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
			entry(EVENT_GROUP_QUOTE, Quote.TYPE_QUOTE));
	private static final List<String> EVENT_GROUPS = List.of(EVENT_GROUP_QUOTE);
	private static final Map<String, Class<?>> ENTITY_NAME_TO_ENTITY_CLASS_MAPPING = Map.ofEntries(
			entry(Quote.TYPE_QUOTE, Quote.class));

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
}
