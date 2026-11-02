package org.fiware.tmforum.common.rest;

import io.github.wistefan.mapping.EntityVOMapper;
import io.micronaut.cache.annotation.CacheInvalidate;
import io.micronaut.core.annotation.NonNull;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.annotation.Body;
import io.micronaut.http.annotation.Consumes;
import io.micronaut.http.annotation.Post;
import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import org.fiware.ngsi.model.NotificationVO;
import org.fiware.tmforum.common.CommonConstants;
import org.fiware.tmforum.common.configuration.GeneralProperties;
import org.fiware.tmforum.common.domain.subscription.Endpoint;
import org.fiware.tmforum.common.domain.subscription.EntityInfo;
import org.fiware.tmforum.common.domain.subscription.KeyValuePair;
import org.fiware.tmforum.common.domain.subscription.NotificationParams;
import org.fiware.tmforum.common.domain.subscription.Subscription;
import org.fiware.tmforum.common.domain.subscription.TMForumSubscription;
import org.fiware.tmforum.common.exception.TmForumException;
import org.fiware.tmforum.common.exception.TmForumExceptionReason;
import org.fiware.tmforum.common.mapping.ForbiddenCharacterEscaper;
import org.fiware.tmforum.common.mapping.SubscriptionMapper;
import org.fiware.tmforum.common.notification.EventConstants;
import org.fiware.tmforum.common.notification.NgsiLdEventHandler;
import org.fiware.tmforum.common.notification.TMForumEventHandler;
import org.fiware.tmforum.common.querying.QueryParser;
import org.fiware.tmforum.common.querying.SubscriptionQuery;
import org.fiware.tmforum.common.querying.SubscriptionQueryParser;
import org.fiware.tmforum.common.repository.SubscriptionRepository;
import org.fiware.tmforum.common.validation.ReferenceValidationService;
import org.fiware.tmforum.storage.EntityRepository;
import org.fiware.tmforum.storage.PagedResult;
import reactor.core.publisher.Mono;
import tools.jackson.core.JacksonException;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Slf4j
public abstract class AbstractSubscriptionApiController extends AbstractApiController<TMForumSubscription> {
	private static final String RECEIVER_INFO_LISTENER_ENDPOINT = "Listener-Endpoint";

	private final Map<String, String> eventGroupToEntityNameMapping;
	private final GeneralProperties generalProperties;
	protected final EntityVOMapper entityVOMapper;
	private final NgsiLdEventHandler ngsiLdEventHandler;
	private final SubscriptionMapper subscriptionMapper;
	private SubscriptionRepository subscriptionRepository;

	public AbstractSubscriptionApiController(
			QueryParser queryParser, ReferenceValidationService validationService, EntityRepository repository,
			Map<String, String> eventGroupToEntityNameMapping,
			TMForumEventHandler tmForumEventHandler, NgsiLdEventHandler ngsiLdEventHandler,
			GeneralProperties generalProperties, EntityVOMapper entityVOMapper, SubscriptionMapper subscriptionMapper) {
		super(queryParser, validationService, repository, tmForumEventHandler);
		this.eventGroupToEntityNameMapping = eventGroupToEntityNameMapping;
		this.generalProperties = generalProperties;
		this.entityVOMapper = entityVOMapper;
		this.ngsiLdEventHandler = ngsiLdEventHandler;
		this.subscriptionMapper = subscriptionMapper;
	}

	/** Setter-injected so the module constructors stay port-only; the broker notification path stays out of the port. */
	@Inject
	public void setSubscriptionRepository(SubscriptionRepository subscriptionRepository) {
		this.subscriptionRepository = subscriptionRepository;
	}

	@CacheInvalidate(value = CommonConstants.TMFORUM_SUBSCRIPTIONS_CACHE_NAME, all = true)
	protected Mono<TMForumSubscription> create(TMForumSubscription tmForumSubscription) {
		return assureNotExistingTMForumSubscription(tmForumSubscription)
				.then(Mono.defer(() -> subscriptionRepository.createSubscription(
						subscriptionMapper
								.map(tmForumSubscription.getSubscription())
								.atContext(generalProperties.getContextUrl()),
						generalProperties.getTenant())))
				.then(create(Mono.just(tmForumSubscription), TMForumSubscription.class));
	}

	private Mono<Void> assureNotExistingTMForumSubscription(TMForumSubscription subscription) {
		// rawQuery is stored escaped (see TMForumSubscription.getEscapedRawQuery), so the value
		// compared against here must be escaped the same way to actually match.
		String query = String.format(queryParser.toNgsiLdQuery(TMForumSubscription.class, "callback=%s&rawQuery=%s").query(),
				subscription.getCallback(), ForbiddenCharacterEscaper.escape(subscription.getRawQuery()));

		return repository.findEntities(CommonConstants.DEFAULT_OFFSET, 1, TMForumSubscription.TYPE_TM_FORUM_SUBSCRIPTION,
						TMForumSubscription.class, query)
				.map(PagedResult::items)
				.flatMap(list -> list.isEmpty() ? Mono.empty() :
						Mono.error(new TmForumException("Such subscription already exists.", TmForumExceptionReason.CONFLICT)));
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
		tmForumSubscription.setSubscription(buildNgsiLdSubscription(callback, subscriptionQuery, entities));
		return tmForumSubscription;
	}

	private Subscription buildNgsiLdSubscription(String callback, SubscriptionQuery subscriptionQuery, List<String> entities) {
		Subscription subscription = new Subscription(UUID.randomUUID().toString());
		subscription.setQ(subscriptionQuery.getQuery());
		subscription.setEntities(entities.stream().map(entityType -> {
			EntityInfo entityInfo = new EntityInfo();
			entityInfo.setType(entityType);
			return entityInfo;
		}).toList());
		subscription.setNotification(buildNotificationParams(callback, subscriptionQuery));
		return subscription;
	}

	private NotificationParams buildNotificationParams(String callback, SubscriptionQuery subscriptionQuery) {
		NotificationParams notificationParams = new NotificationParams();
		notificationParams.setAttributes(Set.copyOf(subscriptionQuery.getFields()));
		notificationParams.setFormat(EventConstants.NOTIFICATION_FORMAT);
		notificationParams.setEndpoint(
				new Endpoint(
						getCallbackURI(),
						EventConstants.NOTIFICATION_PAYLOAD_MIME_TYPE,
						List.of(new KeyValuePair(RECEIVER_INFO_LISTENER_ENDPOINT, callback))));
		return notificationParams;
	}

	@Override
	@CacheInvalidate(value = CommonConstants.TMFORUM_SUBSCRIPTIONS_CACHE_NAME, all = true)
	protected Mono<HttpResponse<Object>> delete(String id) {
		return subscriptionRepository.deleteDomainSubscriptionByTmForumSubscription(URI.create(id))
				.then(super.delete(id));
	}

	private URI getCallbackURI() {
		String basepath = generalProperties.getBasepath() != null ? generalProperties.getBasepath() : "";
		if (basepath.endsWith("/")) {
			basepath = basepath.substring(0, basepath.length() - 1);
		}
		return URI.create(generalProperties.getServerHost() + basepath + EventConstants.SUBSCRIPTION_CALLBACK_PATH);
	}

	@Post(EventConstants.SUBSCRIPTION_CALLBACK_PATH)
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
