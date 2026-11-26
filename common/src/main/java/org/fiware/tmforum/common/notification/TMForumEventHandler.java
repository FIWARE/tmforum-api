package org.fiware.tmforum.common.notification;

import tools.jackson.databind.ObjectMapper;
import com.google.common.base.CaseFormat;
import io.github.wistefan.mapping.EntityVOMapper;
import io.micronaut.cache.annotation.Cacheable;
import io.micronaut.context.annotation.Bean;
import org.fiware.ngsi.model.EntityVO;
import org.fiware.tmforum.common.CommonConstants;
import org.fiware.tmforum.common.domain.EntityWithId;
import org.fiware.tmforum.common.domain.subscription.TMForumSubscription;
import org.fiware.tmforum.common.mapping.EventMapping;
import org.fiware.tmforum.common.exception.EventHandlingException;
import org.fiware.tmforum.common.notification.checkers.tmforum.AttributeValueChangeTmForumEventChecker;
import org.fiware.tmforum.common.notification.checkers.tmforum.CreateTmForumEventChecker;
import org.fiware.tmforum.common.notification.checkers.tmforum.DeleteTmForumEventChecker;
import org.fiware.tmforum.common.notification.checkers.tmforum.StateChangeTmForumEventChecker;
import org.fiware.tmforum.common.notification.checkers.tmforum.TmForumEventChecker;
import org.fiware.tmforum.common.querying.SubscriptionQueryResolver;
import org.fiware.tmforum.storage.EntityReadRepository;
import org.fiware.tmforum.storage.PagedResult;
import org.fiware.tmforum.storage.query.AttributePath;
import org.fiware.tmforum.storage.query.ComparisonOperator;
import org.fiware.tmforum.storage.query.EntityQuery;
import org.fiware.tmforum.storage.query.QueryFilter;
import org.fiware.tmforum.storage.query.ValueType;
import org.fiware.tmforum.common.rest.AbstractSubscriptionApiController;
import reactor.core.publisher.Mono;

import java.lang.reflect.InvocationTargetException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.function.Function;

import static org.fiware.tmforum.common.CommonConstants.DEFAULT_OFFSET;
import static org.fiware.tmforum.common.notification.EventConstants.*;

@Bean
public class TMForumEventHandler extends EventHandler {

	public static final int SUBSCRIPTION_PAGE_SIZE = 100;

	/**
	 * Pages a lookup reads at most, so a backend that keeps answering with a full page cannot spin forever. At the
	 * page size that is {@value #SUBSCRIPTION_PAGE_SIZE} x {@value #MAX_SUBSCRIPTION_PAGES} hubs of one event.
	 */
	static final int MAX_SUBSCRIPTION_PAGES = 100;

	private static final String ENTITIES_ATTRIBUTE = "entities";
	private static final String EVENT_TYPES_ATTRIBUTE = "eventTypes";

	private final SubscriptionQueryResolver subscriptionQueryResolver;
	private final EntityReadRepository repository;

	public TMForumEventHandler(SubscriptionQueryResolver subscriptionQueryResolver,
							   NotificationSender notificationSender, EntityVOMapper entityVOMapper,
							   EntityReadRepository repository, EventMapper eventMapper, ObjectMapper objectMapper) {
		super(notificationSender, entityVOMapper, eventMapper, objectMapper);
		this.subscriptionQueryResolver = subscriptionQueryResolver;
		this.repository = repository;
	}

	/**
	 * The hubs taking the event, from the cache when the same kind of event was looked up before.
	 * <p>
	 * The lookup only depends on the event's entity type and event type, so only those are the cache key. Keying on
	 * the whole {@link EventDetails} made every call a miss, since it carries the event time.
	 * </p>
	 */
	public Mono<List<TMForumSubscription>> getSubscriptions(EventDetails eventDetails) {
		return getSubscriptions(eventDetails.entityType(), eventDetails.eventType());
	}

	/**
	 * Cached lookup of the hubs taking an event of {@code entityType} and {@code eventType}; invalidated whole
	 * whenever a hub is written or deleted.
	 */
	@Cacheable(CommonConstants.TMFORUM_SUBSCRIPTIONS_CACHE_NAME)
	public Mono<List<TMForumSubscription>> getSubscriptions(String entityType, String eventType) {
		return findSubscriptions(entityType, eventType);
	}

	/**
	 * Uncached lookup of the first {@value #SUBSCRIPTION_PAGE_SIZE} hubs taking an event. Outbox retries reuse one
	 * event time, so a cached list would outlive hubs registered or deleted since the row's first attempt.
	 */
	public Mono<List<TMForumSubscription>> findSubscriptions(EventDetails eventDetails) {
		return findSubscriptions(eventDetails.entityType(), eventDetails.eventType());
	}

	/**
	 * As {@link #findSubscriptions(EventDetails)}, for the two attributes the lookup depends on.
	 * <p>
	 * Reads page after page until one comes back short: a deployment with more than {@value #SUBSCRIPTION_PAGE_SIZE}
	 * hubs on one event would otherwise never notify the ones past the first page, and the outbox would delete the
	 * row as delivered.
	 * </p>
	 */
	public Mono<List<TMForumSubscription>> findSubscriptions(String entityType, String eventType) {
		return subscriptionPage(entityType, eventType, DEFAULT_OFFSET)
				.expand(page -> page.items().size() < SUBSCRIPTION_PAGE_SIZE
						? Mono.empty()
						: subscriptionPage(entityType, eventType, page.offset() + page.items().size()))
				.take(MAX_SUBSCRIPTION_PAGES)
				.map(PagedResult::items)
				.collectList()
				.map(pages -> pages.stream().flatMap(List::stream).toList());
	}

	private Mono<PagedResult<TMForumSubscription>> subscriptionPage(String entityType, String eventType, int offset) {
		return repository.findEntities(
				offset,
				SUBSCRIPTION_PAGE_SIZE,
				TMForumSubscription.TYPE_TM_FORUM_SUBSCRIPTION,
				TMForumSubscription.class,
				subscriptionsTaking(entityType, eventType));
	}

	private static EntityQuery subscriptionsTaking(String entityType, String eventType) {
		QueryFilter filter = new QueryFilter.AllOf(List.of(
				listContains(ENTITIES_ATTRIBUTE, entityType),
				listContains(EVENT_TYPES_ATTRIBUTE, eventType)));
		return new EntityQuery(TMForumSubscription.class, filter, List.of());
	}

	private static QueryFilter listContains(String attribute, String value) {
		return new QueryFilter.Comparison(new AttributePath(List.of(attribute), true), ComparisonOperator.EQUALS,
				ValueType.STRING, List.of(value));
	}

	private <T> Mono<Void> handle(T entity, EventDetails eventDetails, TmForumEventChecker eventChecker) {
		return notify(this::getSubscriptions, eventDetails, eventChecker,
				subscription -> createNotification(subscription.getCallback(), eventDetails, entity,
						subscription.getFields(), UUID.randomUUID().toString()),
				notificationSender::sendNotifications);
	}

	// every retry of an outbox row carries the same eventId, so consumers can deduplicate
	private <T> Mono<Void> handleOutbox(T entity, EventDetails eventDetails, TmForumEventChecker eventChecker,
										long outboxId) {
		String eventId = outboxEventId(outboxId, eventDetails.eventType());
		return notify(this::findSubscriptions, eventDetails, eventChecker,
				subscription -> createNotification(subscription.getCallback(), eventDetails, entity,
						subscription.getFields(), eventId),
				notificationSender::deliverNotifications);
	}

	private Mono<Void> notify(Function<EventDetails, Mono<List<TMForumSubscription>>> lookup,
							  EventDetails eventDetails, TmForumEventChecker eventChecker,
							  Function<TMForumSubscription, TMForumNotification> toNotification,
							  Function<List<TMForumNotification>, Mono<Void>> sender) {
		return lookup.apply(eventDetails)
				.map(subscriptions -> subscriptions.stream()
						.filter(subscription -> eventChecker.wasFired(subscription.getQuery()))
						.map(toNotification)
						.toList())
				.flatMap(sender);
	}

	static String outboxEventId(long outboxId, String eventType) {
		return UUID.nameUUIDFromBytes((outboxId + eventType).getBytes(StandardCharsets.UTF_8)).toString();
	}

	public <T> Mono<Void> handleCreateEvent(T entity) {
		try {
			EventDetails eventDetails = makeEventDetails(entity, CREATE_EVENT_SUFFIX, Instant.now());
			return handle(entity, eventDetails, createChecker(entity, eventDetails));
		} catch (EventHandlingException e) {
			return Mono.empty();
		}
	}

	/**
	 * Outbox delivery of a CreateEvent at the row's creation time. Errors unless every matching hub acknowledged.
	 */
	public <T> Mono<Void> handleCreateEvent(T entity, long outboxId, Instant eventTime) {
		try {
			EventDetails eventDetails = makeEventDetails(entity, CREATE_EVENT_SUFFIX, eventTime);
			return handleOutbox(entity, eventDetails, createChecker(entity, eventDetails), outboxId);
		} catch (EventHandlingException e) {
			return Mono.empty();
		}
	}

	public <T> Mono<Void> handleUpdateEvent(T newState, T oldState) {
		return handleUpdateEvent(newState, oldState, Instant.now(), this::handle);
	}

	/**
	 * Outbox delivery of the update's change events at the row's creation time. Errors unless every matching hub
	 * acknowledged.
	 */
	public <T> Mono<Void> handleUpdateEvent(T newState, T oldState, long outboxId, Instant eventTime) {
		return handleUpdateEvent(newState, oldState, eventTime,
				(entity, details, checker) -> handleOutbox(entity, details, checker, outboxId));
	}

	/**
	 * Outbox delivery of a DeleteEvent at the row's creation time, to the hubs of {@code <Type>DeleteEvent} whose
	 * query {@code oldState} matched. The payload carries the id only, as the broker's deletion notification does.
	 */
	public <T> Mono<Void> handleDeleteEvent(T oldState, long outboxId, Instant eventTime) {
		try {
			EventDetails eventDetails = makeEventDetails(oldState, DELETE_EVENT_SUFFIX, eventTime);
			TmForumEventChecker eventChecker = new DeleteTmForumEventChecker<>(subscriptionQueryResolver, oldState,
					eventDetails.payloadName());
			return idOnly(oldState, eventDetails.entityType())
					.flatMap(payload -> handleOutbox(payload, eventDetails, eventChecker, outboxId));
		} catch (EventHandlingException e) {
			return Mono.empty();
		}
	}

	private <T> Mono<Void> handleUpdateEvent(T newState, T oldState, Instant eventTime, EventDelivery delivery) {
		try {
			EventDetails eventDetails1 = makeEventDetails(newState, ATTRIBUTE_VALUE_CHANGE_EVENT_SUFFIX, eventTime);
			TmForumEventChecker eventChecker1 = new AttributeValueChangeTmForumEventChecker<>(
					subscriptionQueryResolver, newState, oldState, eventDetails1.payloadName());
			Mono<Void> attrUpdateMono = delivery.deliver(newState, eventDetails1, eventChecker1);

			EventDetails eventDetails2 = makeEventDetails(newState, STATE_CHANGE_EVENT_SUFFIX, eventTime);
			TmForumEventChecker eventChecker2 = new StateChangeTmForumEventChecker<>(newState, oldState);
			Mono<Void> stateChangeMono = delivery.deliver(newState, eventDetails2, eventChecker2);

			// concurrent, so an outbox row takes one round of hub calls, not two (see OutboxProperties' lease)
			return Mono.whenDelayError(attrUpdateMono, stateChangeMono);
		} catch (EventHandlingException e) {
			return Mono.empty();
		}
	}

	private <T> TmForumEventChecker createChecker(T entity, EventDetails eventDetails) {
		return new CreateTmForumEventChecker<>(subscriptionQueryResolver, entity, eventDetails.payloadName());
	}

	// built like the broker path builds it from its deletion notification, which carries only id and type
	private Mono<?> idOnly(Object oldState, String entityType) {
		EventMapping eventMapping = eventMapper.getEntityClassMapping().get(entityType);
		if (eventMapping == null || !(oldState instanceof EntityWithId entity)) {
			// an unmapped type is no delivery failure, as in the create/update overloads
			throw new EventHandlingException();
		}
		return entityVOMapper.fromEntityVO(new EntityVO().id(entity.getId()).type(entityType), eventMapping.rawClass());
	}

	private <T> EventDetails makeEventDetails(T entity, String eventSuffix, Instant eventTime) {
		String entityType = getEntityType(entity);
		if (entityType.equals(TMForumSubscription.TYPE_TM_FORUM_SUBSCRIPTION)) {
			throw new EventHandlingException();
		}
		String payloadName = CaseFormat.LOWER_HYPHEN.to(CaseFormat.LOWER_CAMEL, entityType);
		String eventType = CaseFormat.LOWER_CAMEL.to(CaseFormat.UPPER_CAMEL, payloadName) + eventSuffix;
		return new EventDetails(entityType, eventType, eventTime, payloadName);
	}

	@FunctionalInterface
	private interface EventDelivery {
		Mono<Void> deliver(Object entity, EventDetails eventDetails, TmForumEventChecker eventChecker);
	}

	private <T> String getEntityType(T entity) {
		try {
			return entity.getClass().getMethod("getType").invoke(entity).toString();
		} catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
			return "";
		}
	}

}
