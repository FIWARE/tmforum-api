package org.fiware.tmforum.common.rest;

import io.github.wistefan.mapping.EntityVOMapper;
import io.micronaut.cache.annotation.CacheInvalidate;
import io.micronaut.core.annotation.NonNull;
import io.micronaut.core.annotation.Nullable;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.annotation.Body;
import io.micronaut.http.annotation.Consumes;
import io.micronaut.http.annotation.Post;
import io.micronaut.http.annotation.RouteCondition;
import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import org.fiware.ngsi.model.NotificationVO;
import org.fiware.tmforum.common.CommonConstants;
import org.fiware.tmforum.common.configuration.GeneralProperties;
import org.fiware.tmforum.common.domain.subscription.TMForumSubscription;
import org.fiware.tmforum.common.exception.TmForumException;
import org.fiware.tmforum.common.exception.TmForumExceptionReason;
import org.fiware.tmforum.common.mapping.SubscriptionMapper;
import org.fiware.tmforum.common.notification.EventConstants;
import org.fiware.tmforum.common.notification.NgsiLdEventHandler;
import org.fiware.tmforum.common.notification.NgsiLdSubscriptionFactory;
import org.fiware.tmforum.common.notification.TMForumEventHandler;
import org.fiware.tmforum.common.querying.QueryParser;
import org.fiware.tmforum.common.querying.SubscriptionQuery;
import org.fiware.tmforum.common.querying.SubscriptionQueryParser;
import org.fiware.tmforum.common.repository.SubscriptionRepository;
import org.fiware.tmforum.common.validation.ReferenceValidationService;
import org.fiware.tmforum.storage.EntityRepository;
import org.fiware.tmforum.storage.PagedResult;
import org.fiware.tmforum.storage.query.AttributePath;
import org.fiware.tmforum.storage.query.ComparisonOperator;
import org.fiware.tmforum.storage.query.EntityQuery;
import org.fiware.tmforum.storage.query.QueryFilter;
import org.fiware.tmforum.storage.query.ValueType;
import reactor.core.publisher.Mono;
import tools.jackson.core.JacksonException;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

@Slf4j
public abstract class AbstractSubscriptionApiController extends AbstractApiController<TMForumSubscription> {
	private static final String CALLBACK_ATTRIBUTE = "callback";
	static final int SAME_CALLBACK_PAGE_SIZE = 100;
	// false on PostgreSQL, where the NGSI-LD path is not wired, so the router answers its normal 404
	private static final String BROKER_NOTIFICATIONS_CONDITION =
			"#{ !ctx[T(io.micronaut.context.env.Environment)].getActiveNames().contains('"
					+ CommonConstants.POSTGRESQL_ENVIRONMENT + "') }";

	private final Map<String, String> eventGroupToEntityNameMapping;
	private final GeneralProperties generalProperties;
	protected final EntityVOMapper entityVOMapper;
	@Nullable
	private final NgsiLdEventHandler ngsiLdEventHandler;
	private final SubscriptionMapper subscriptionMapper;
	@Nullable
	private SubscriptionRepository subscriptionRepository;

	public AbstractSubscriptionApiController(
			QueryParser queryParser, ReferenceValidationService validationService, EntityRepository repository,
			Map<String, String> eventGroupToEntityNameMapping,
			TMForumEventHandler tmForumEventHandler, @Nullable NgsiLdEventHandler ngsiLdEventHandler,
			GeneralProperties generalProperties, EntityVOMapper entityVOMapper, SubscriptionMapper subscriptionMapper) {
		super(queryParser, validationService, repository, tmForumEventHandler);
		this.eventGroupToEntityNameMapping = eventGroupToEntityNameMapping;
		this.generalProperties = generalProperties;
		this.entityVOMapper = entityVOMapper;
		this.ngsiLdEventHandler = ngsiLdEventHandler;
		this.subscriptionMapper = subscriptionMapper;
	}

	/**
	 * Setter-injected so the module constructors stay port-only; the broker notification path stays out of the port.
	 * Null on PostgreSQL, where the NGSI-LD path is not wired.
	 */
	@Inject
	public void setSubscriptionRepository(@Nullable SubscriptionRepository subscriptionRepository) {
		this.subscriptionRepository = subscriptionRepository;
	}

	private boolean brokerNotifications() {
		return subscriptionRepository != null && ngsiLdEventHandler != null;
	}

	@CacheInvalidate(value = CommonConstants.TMFORUM_SUBSCRIPTIONS_CACHE_NAME, all = true)
	protected Mono<TMForumSubscription> create(TMForumSubscription tmForumSubscription) {
		if (!brokerNotifications()) {
			return assureNotExistingTMForumSubscription(tmForumSubscription)
					.then(create(Mono.just(tmForumSubscription), TMForumSubscription.class));
		}
		return assureNotExistingTMForumSubscription(tmForumSubscription)
				.then(Mono.defer(() -> subscriptionRepository.createSubscription(
						subscriptionMapper
								.map(tmForumSubscription.getSubscription())
								.atContext(generalProperties.getContextUrl()),
						generalProperties.getTenant())))
				.then(create(Mono.just(tmForumSubscription), TMForumSubscription.class));
	}

	// rawQuery is compared in Java: its stored form differs per backend (escaped on NGSI-LD), and hubs sharing one
	// callback are few. The pages are unsorted, since Orion-LD 1.9.0 rejects orderBy (501) and Scorpio 5.0.3 ignores
	// it, so the check is exact only while the matches fit one page of SAME_CALLBACK_PAGE_SIZE.
	private Mono<Void> assureNotExistingTMForumSubscription(TMForumSubscription subscription) {
		// built directly, since parsing would split a callback's query string on its separators
		EntityQuery sameCallback = new EntityQuery(TMForumSubscription.class,
				new QueryFilter.Comparison(new AttributePath(List.of(CALLBACK_ATTRIBUTE), true),
						ComparisonOperator.EQUALS, ValueType.STRING, List.of(subscription.getCallback().toString())),
				List.of());

		return findSameCallback(sameCallback, CommonConstants.DEFAULT_OFFSET)
				.expand(page -> page.items().size() < SAME_CALLBACK_PAGE_SIZE ? Mono.empty() :
						findSameCallback(sameCallback, page.offset() + SAME_CALLBACK_PAGE_SIZE))
				.flatMapIterable(PagedResult::items)
				.any(existing -> Objects.equals(existing.getRawQuery(), subscription.getRawQuery()))
				.flatMap(exists -> exists ?
						Mono.<Void>error(new TmForumException("Such subscription already exists.",
								TmForumExceptionReason.CONFLICT)) :
						Mono.<Void>empty());
	}

	private Mono<PagedResult<TMForumSubscription>> findSameCallback(EntityQuery sameCallback, int offset) {
		return repository.findEntities(offset, SAME_CALLBACK_PAGE_SIZE, TMForumSubscription.TYPE_TM_FORUM_SUBSCRIPTION,
				TMForumSubscription.class, sameCallback);
	}

	protected TMForumSubscription buildSubscription(String callback, String query, List<String> eventGroups) {
		log.debug(query);
		SubscriptionQuery subscriptionQuery = SubscriptionQueryParser.parse(query, eventGroups);
		List<String> entities = subscriptionQuery.getEventGroups().stream()
				.map(eventGroup -> {
					if (eventGroupToEntityNameMapping.containsKey(eventGroup)) {
						return eventGroupToEntityNameMapping.get(eventGroup);
					} else {
						throw new TmForumException("Such subscription already exists.",
								TmForumExceptionReason.INVALID_DATA);
					}
				}).toList();

		TMForumSubscription tmForumSubscription = new TMForumSubscription(UUID.randomUUID().toString());
		tmForumSubscription.setRawQuery(query != null ? query : "");
		tmForumSubscription.setEventTypes(subscriptionQuery.getEventTypes());
		tmForumSubscription.setEntities(entities);
		tmForumSubscription.setQuery(subscriptionQuery.getQuery());
		tmForumSubscription.setCallback(URI.create(callback));
		tmForumSubscription.setFields(subscriptionQuery.getFields());
		if (brokerNotifications()) {
			tmForumSubscription.setSubscription(NgsiLdSubscriptionFactory.build(
					NgsiLdSubscriptionFactory.notificationEndpoint(generalProperties.getServerHost(),
							generalProperties.getBasepath()),
					callback, subscriptionQuery.getQuery(), entities, subscriptionQuery.getFields()));
		}
		return tmForumSubscription;
	}

	@Override
	@CacheInvalidate(value = CommonConstants.TMFORUM_SUBSCRIPTIONS_CACHE_NAME, all = true)
	protected Mono<HttpResponse<Object>> delete(String id) {
		if (!brokerNotifications()) {
			return super.delete(id);
		}
		return subscriptionRepository.deleteDomainSubscriptionByTmForumSubscription(URI.create(id))
				.then(super.delete(id));
	}

	@Post(EventConstants.SUBSCRIPTION_CALLBACK_PATH)
	@RouteCondition(BROKER_NOTIFICATIONS_CONDITION)
	@Consumes({"application/json;charset=utf-8"})
	public Mono<HttpResponse<Void>> callback(@NonNull @Body String payload) {
		log.warn("Callback for NGSI-LD subscription: {}", payload);

		try {
			NotificationVO notificationVO = this.entityVOMapper.readNotificationFromJSON(payload);

			log.debug(String.format("Callback for subscription %s with notification %s", notificationVO.getSubscriptionId(), payload));

			assert !notificationVO.getData().isEmpty();

			return subscriptionRepository.retrieveSubscriptionById(notificationVO.getSubscriptionId())
					.flatMap(subscriptionVO -> ngsiLdEventHandler.handle(notificationVO, subscriptionVO))
					.then(Mono.just(HttpResponse.noContent()));
		} catch (JacksonException e) {
			throw new RuntimeException(e);
		}
	}

}
