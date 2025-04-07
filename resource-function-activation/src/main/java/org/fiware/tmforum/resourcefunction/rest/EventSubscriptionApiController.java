package org.fiware.tmforum.resourcefunction.rest;

import io.github.wistefan.mapping.EntityVOMapper;
import io.micronaut.core.annotation.NonNull;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.annotation.Controller;
import lombok.extern.slf4j.Slf4j;
import org.fiware.resourcefunction.api.EventsSubscriptionApi;
import org.fiware.resourcefunction.model.*;
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
import org.fiware.tmforum.resourcefunction.TMForumMapper;
import org.fiware.tmforum.resourcefunction.domain.*;
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
			entry(EVENT_GROUP_HEAL, Heal.TYPE_HEAL),
			entry(EVENT_GROUP_MIGRATE, Migrate.TYPE_MIGRATE),
			entry(EVENT_GROUP_MONITOR, Monitor.TYPE_MONITOR),
			entry(EVENT_GROUP_RESOURCE_FUNCTION, ResourceFunction.TYPE_RESOURCE_FUNCTION),
			entry(EVENT_GROUP_SCALE, Scale.TYPE_SCALE)
	);
	private static final List<String> EVENT_GROUPS = List.of(EVENT_GROUP_HEAL, EVENT_GROUP_MIGRATE,
			EVENT_GROUP_MONITOR, EVENT_GROUP_RESOURCE_FUNCTION, EVENT_GROUP_SCALE);
	private static final Map<String, EventMapping> ENTITY_NAME_TO_ENTITY_CLASS_MAPPING = Map.ofEntries(
			entry(Heal.TYPE_HEAL, new EventMapping(HealVO.class, Heal.class)),
			entry(Migrate.TYPE_MIGRATE, new EventMapping(MigrateVO.class, Migrate.class)),
			entry(Monitor.TYPE_MONITOR, new EventMapping(MonitorVO.class, Monitor.class)),
			entry(ResourceFunction.TYPE_RESOURCE_FUNCTION, new EventMapping(ResourceFunctionVO.class, ResourceFunction.class)),
			entry(Scale.TYPE_SCALE, new EventMapping(ScaleVO.class, Scale.class))
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
		if (targetClass == Heal.class) {
			return tmForumMapper.map((Heal) rawPayload);
		}
		if (targetClass == Migrate.class) {
			return tmForumMapper.map((Migrate) rawPayload);
		}
		if (targetClass == Monitor.class) {
			return tmForumMapper.map((Monitor) rawPayload);
		}
		if (targetClass == ResourceFunction.class) {
			return tmForumMapper.map((ResourceFunction) rawPayload);
		}
		if (targetClass == Scale.class) {
			return tmForumMapper.map((Scale) rawPayload);
		}
		throw new TmForumException(String.format("Event-Payload %s is not supported.", rawPayload), TmForumExceptionReason.INVALID_DATA);
	}
}
