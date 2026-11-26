package org.fiware.tmforum.common.notification;

import io.github.wistefan.mapping.EntityVOMapper;
import org.fiware.ngsi.model.EntityVO;
import org.fiware.tmforum.common.configuration.GeneralProperties;
import org.fiware.tmforum.common.configuration.NgsiLdBrokerProperties;
import org.fiware.tmforum.common.domain.subscription.TMForumSubscription;
import org.fiware.tmforum.common.mapping.EventMapping;
import org.fiware.tmforum.common.querying.NgsiLdQueryRenderer;
import org.fiware.tmforum.common.querying.QueryParser;
import org.fiware.tmforum.common.querying.SubscriptionQueryResolver;
import org.fiware.tmforum.common.rest.CharacterizationEntity;
import org.fiware.tmforum.storage.EntityReadRepository;
import org.fiware.tmforum.storage.PagedResult;
import org.fiware.tmforum.storage.query.AttributePath;
import org.fiware.tmforum.storage.query.ComparisonOperator;
import org.fiware.tmforum.storage.query.EntityQuery;
import org.fiware.tmforum.storage.query.QueryFilter;
import org.fiware.tmforum.storage.query.ValueType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import reactor.core.Disposable;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TMForumEventHandlerTest {

	private static final String ENTITY_ID = "urn:ngsi-ld:characterization-entity:deleted";
	private static final String ENTITY_NAME = "deleted-name";
	private static final String PAYLOAD_NAME = "characterizationEntity";
	private static final String DELETE_EVENT_TYPE = "CharacterizationEntityDeleteEvent";
	private static final String ID_FIELD = "id";
	private static final String NAME_FIELD = "name";
	private static final String UPDATED_NAME = "updated-name";
	private static final long OUTBOX_ID = 42L;
	private static final Instant EVENT_TIME = Instant.parse("2026-01-01T00:00:00Z");
	private static final URI CALLBACK = URI.create("http://hub");

	private NotificationSender notificationSender;
	private EventMapper eventMapper;
	private EntityReadRepository repository;
	private TMForumEventHandler eventHandler;

	@BeforeEach
	void setUp() {
		notificationSender = mock(NotificationSender.class);
		when(notificationSender.deliverNotifications(anyList())).thenReturn(Mono.empty());
		eventMapper = mock(EventMapper.class);
		when(eventMapper.getEntityClassMapping()).thenReturn(Map.of(CharacterizationEntity.TYPE,
				new EventMapping(CharacterizationEntity.class, CharacterizationEntity.class)));
		when(eventMapper.mapPayload(any(), any())).thenAnswer(invocation -> invocation.getArgument(0));
		EntityVOMapper entityVOMapper = mock(EntityVOMapper.class);
		when(entityVOMapper.fromEntityVO(any(), eq(CharacterizationEntity.class))).thenAnswer(invocation ->
				Mono.just(new CharacterizationEntity(invocation.<EntityVO>getArgument(0).getId().toString())));
		// stands in for the VO's JSON: the fields the payload object carries
		when(entityVOMapper.convertEntityToMap(any())).thenAnswer(invocation -> {
			CharacterizationEntity entity = invocation.getArgument(0);
			Map<String, Object> map = new LinkedHashMap<>();
			map.put(ID_FIELD, entity.getId().toString());
			if (entity.getName() != null) {
				map.put(NAME_FIELD, entity.getName());
			}
			return map;
		});
		repository = mock(EntityReadRepository.class);
		eventHandler = spy(new TMForumEventHandler(new SubscriptionQueryResolver(), notificationSender, entityVOMapper,
				repository, eventMapper, null));
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("deleteHubs")
	void handleDeleteEventNotifiesTheDeleteHubsWhoseQueryMatchedTheOldState(String description, String query,
																			  boolean expectNotified) {
		doReturn(Mono.just(List.of(hub(query)))).when(eventHandler).findSubscriptions(any());

		eventHandler.handleDeleteEvent(oldState(), OUTBOX_ID, EVENT_TIME).block();

		List<TMForumNotification> sent = sentNotifications();
		assertEquals(expectNotified ? 1 : 0, sent.size(), description);
		if (expectNotified) {
			TMForumNotification notification = sent.getFirst();
			assertEquals(CALLBACK, notification.callback());
			assertEquals(DELETE_EVENT_TYPE, notification.event().getEventType());
			assertEquals(EVENT_TIME, notification.event().getEventTime());
			// the broker's deletion notification carries the id only, so does the outbox's
			assertEquals(Map.of(PAYLOAD_NAME, Map.of(ID_FIELD, ENTITY_ID)), notification.event().getEvent());
		}
	}

	private static Stream<Arguments> deleteHubs() {
		return Stream.of(
				Arguments.of("hub without query", null, true),
				Arguments.of("hub query matching the old state", PAYLOAD_NAME + ".name=" + ENTITY_NAME, true),
				Arguments.of("hub query not matching the old state", PAYLOAD_NAME + ".name=other", false));
	}

	@Test
	void findSubscriptionsFiltersTheHubsByEntityTypeAndEventType() {
		EventDetails details = new EventDetails(CharacterizationEntity.TYPE, DELETE_EVENT_TYPE, EVENT_TIME, PAYLOAD_NAME);
		when(repository.findEntities(any(), any(), any(), any(), any()))
				.thenReturn(Mono.just(new PagedResult<>(List.of(), 0, TMForumEventHandler.SUBSCRIPTION_PAGE_SIZE, null)));

		eventHandler.findSubscriptions(details).block();

		EntityQuery expected = new EntityQuery(TMForumSubscription.class, new QueryFilter.AllOf(List.of(
				contains("entities", CharacterizationEntity.TYPE),
				contains("eventTypes", DELETE_EVENT_TYPE))), List.of());
		verify(repository).findEntities(any(), eq(TMForumEventHandler.SUBSCRIPTION_PAGE_SIZE),
				eq(TMForumSubscription.TYPE_TM_FORUM_SUBSCRIPTION), eq(TMForumSubscription.class), eq(expected));
	}

	@Test
	void findSubscriptionsRendersTheSameNgsiLdQueryAsTheParsedHubLookup() {
		EventDetails details = new EventDetails(CharacterizationEntity.TYPE, DELETE_EVENT_TYPE, EVENT_TIME, PAYLOAD_NAME);
		when(repository.findEntities(any(), any(), any(), any(), any()))
				.thenReturn(Mono.just(new PagedResult<>(List.of(), 0, TMForumEventHandler.SUBSCRIPTION_PAGE_SIZE, null)));

		eventHandler.findSubscriptions(details).block();

		ArgumentCaptor<EntityQuery> query = ArgumentCaptor.forClass(EntityQuery.class);
		verify(repository).findEntities(any(), any(), any(), any(), query.capture());
		GeneralProperties properties = new GeneralProperties();
		NgsiLdQueryRenderer renderer = new NgsiLdQueryRenderer(new NgsiLdBrokerProperties());
		// the hub lookup built this filter through the parser before the port took the model
		QueryFilter parsed = new QueryParser(new GeneralProperties()).parse(TMForumSubscription.class,
				String.format("entities=%s&eventTypes=%s", CharacterizationEntity.TYPE, DELETE_EVENT_TYPE)).filter();
		String rendered = renderer.renderFilter(query.getValue());
		assertEquals(String.format("entities==\"%s\";eventTypes==\"%s\"", CharacterizationEntity.TYPE, DELETE_EVENT_TYPE),
				rendered);
		assertEquals(renderer.renderFilter(new EntityQuery(TMForumSubscription.class, parsed, List.of())), rendered);
	}

	private static QueryFilter contains(String attribute, String value) {
		return new QueryFilter.Comparison(new AttributePath(List.of(attribute), true), ComparisonOperator.EQUALS,
				ValueType.STRING, List.of(value));
	}

	@Test
	void handleDeleteEventLooksUpTheHubsOfTheDeleteEventType() {
		doReturn(Mono.just(List.of())).when(eventHandler).findSubscriptions(any());

		eventHandler.handleDeleteEvent(oldState(), OUTBOX_ID, EVENT_TIME).block();

		ArgumentCaptor<EventDetails> details = ArgumentCaptor.forClass(EventDetails.class);
		verify(eventHandler).findSubscriptions(details.capture());
		assertEquals(new EventDetails(CharacterizationEntity.TYPE, DELETE_EVENT_TYPE, EVENT_TIME, PAYLOAD_NAME),
				details.getValue());
	}

	@Test
	void handleDeleteEventOfAnUnmappedTypeCompletesWithoutDelivery() {
		when(eventMapper.getEntityClassMapping()).thenReturn(Map.of());

		eventHandler.handleDeleteEvent(oldState(), OUTBOX_ID, EVENT_TIME).block();

		verify(notificationSender, never()).deliverNotifications(anyList());
	}

	@Test
	void outboxEventIdIsStablePerRowAndEventType() {
		doReturn(Mono.just(List.of(hub(null)))).when(eventHandler).findSubscriptions(any());

		eventHandler.handleDeleteEvent(oldState(), OUTBOX_ID, EVENT_TIME).block();
		eventHandler.handleDeleteEvent(oldState(), OUTBOX_ID, EVENT_TIME).block();

		ArgumentCaptor<List<TMForumNotification>> sent = ArgumentCaptor.captor();
		verify(notificationSender, times(2)).deliverNotifications(sent.capture());
		assertEquals(sent.getAllValues().get(0).getFirst().event().getEventId(),
				sent.getAllValues().get(1).getFirst().event().getEventId());
		assertNotEquals(TMForumEventHandler.outboxEventId(OUTBOX_ID, DELETE_EVENT_TYPE),
				TMForumEventHandler.outboxEventId(OUTBOX_ID + 1, DELETE_EVENT_TYPE));
	}

	static Stream<Arguments> outboxDeliveries() {
		return Stream.of(
				Arguments.of("create", (Function<TMForumEventHandler, Mono<Void>>) handler ->
						handler.handleCreateEvent(oldState(), OUTBOX_ID, EVENT_TIME)),
				Arguments.of("update", (Function<TMForumEventHandler, Mono<Void>>) handler ->
						handler.handleUpdateEvent(newState(), oldState(), OUTBOX_ID, EVENT_TIME)),
				Arguments.of("delete", (Function<TMForumEventHandler, Mono<Void>>) handler ->
						handler.handleDeleteEvent(oldState(), OUTBOX_ID, EVENT_TIME)));
	}

	// outbox retries share one event time, so a cached hub list would outlive hub changes
	@ParameterizedTest(name = "{0}")
	@MethodSource("outboxDeliveries")
	void outboxDeliveryLooksUpTheHubsUncached(String description, Function<TMForumEventHandler, Mono<Void>> delivery) {
		doReturn(Mono.just(List.of())).when(eventHandler).findSubscriptions(any());

		delivery.apply(eventHandler).block();

		verify(eventHandler, atLeastOnce()).findSubscriptions(any());
		verify(eventHandler, never()).getSubscriptions(any());
	}

	@Test
	void brokerDeliveryLooksUpTheHubsThroughTheCache() {
		doReturn(Mono.just(List.of())).when(eventHandler).findSubscriptions(any(), any());
		when(notificationSender.sendNotifications(anyList())).thenReturn(Mono.empty());

		eventHandler.handleCreateEvent(oldState()).block();

		verify(eventHandler).getSubscriptions(any());
	}

	/**
	 * A deployment with more hubs on one event than a page holds would otherwise never notify the ones past the
	 * first page, while the outbox deletes the row as delivered.
	 */
	@ParameterizedTest(name = "{0} hubs")
	@ValueSource(ints = {0, 1, TMForumEventHandler.SUBSCRIPTION_PAGE_SIZE - 1,
			TMForumEventHandler.SUBSCRIPTION_PAGE_SIZE, TMForumEventHandler.SUBSCRIPTION_PAGE_SIZE + 1,
			2 * TMForumEventHandler.SUBSCRIPTION_PAGE_SIZE + 3})
	void findSubscriptionsReadsEveryPage(int hubCount) {
		List<TMForumSubscription> stored = IntStream.range(0, hubCount)
				.mapToObj(index -> hub(null, "urn:ngsi-ld:tm-forum-subscription:" + index))
				.toList();
		when(repository.findEntities(any(), any(), any(), any(), any())).thenAnswer(invocation -> {
			int offset = invocation.getArgument(0);
			int limit = invocation.getArgument(1);
			List<TMForumSubscription> page = stored.subList(Math.min(offset, stored.size()),
					Math.min(offset + limit, stored.size()));
			return Mono.just(new PagedResult<>(page, offset, limit, stored.size()));
		});

		List<TMForumSubscription> found = eventHandler.findSubscriptions(CharacterizationEntity.TYPE,
				DELETE_EVENT_TYPE).block();

		assertEquals(stored, found, "Every hub should be found, whatever the page size.");
	}

	/**
	 * The cached lookup is keyed on its arguments, so it has to take only what the lookup depends on. Keyed on the
	 * whole EventDetails it was keyed on the event time as well, which is unique per call, so the cache never hit.
	 */
	@Test
	void theCachedLookupTakesOnlyTheEntityTypeAndTheEventType() {
		doReturn(Mono.just(List.of())).when(eventHandler).findSubscriptions(any(), any());
		EventDetails eventDetails = new EventDetails(CharacterizationEntity.TYPE, DELETE_EVENT_TYPE, Instant.now(),
				PAYLOAD_NAME);

		eventHandler.getSubscriptions(eventDetails).block();

		verify(eventHandler).getSubscriptions(CharacterizationEntity.TYPE, DELETE_EVENT_TYPE);
		verify(eventHandler).findSubscriptions(CharacterizationEntity.TYPE, DELETE_EVENT_TYPE);
	}

	@Test
	void updateDeliversBothChangeEventsConcurrently() {
		AtomicBoolean stateChangeLookedUp = new AtomicBoolean();
		// the AttributeValueChange lookup never completes, a sequential delivery would never reach the StateChange
		doReturn(Mono.never(), Mono.just(List.of()).doOnSubscribe(subscription -> stateChangeLookedUp.set(true)))
				.when(eventHandler).findSubscriptions(any());

		Disposable delivery = eventHandler.handleUpdateEvent(newState(), oldState(), OUTBOX_ID, EVENT_TIME)
				.subscribe();

		assertTrue(stateChangeLookedUp.get(), "The StateChange event should not wait for the AttributeValueChange.");
		delivery.dispose();
	}

	private static CharacterizationEntity newState() {
		CharacterizationEntity newState = new CharacterizationEntity(ENTITY_ID);
		newState.setName(UPDATED_NAME);
		return newState;
	}

	private static CharacterizationEntity oldState() {
		CharacterizationEntity oldState = new CharacterizationEntity(ENTITY_ID);
		oldState.setName(ENTITY_NAME);
		return oldState;
	}

	private static TMForumSubscription hub(String query) {
		return hub(query, "urn:ngsi-ld:tm-forum-subscription:hub");
	}

	private static TMForumSubscription hub(String query, String id) {
		TMForumSubscription subscription = new TMForumSubscription(id);
		subscription.setCallback(CALLBACK);
		subscription.setQuery(query);
		return subscription;
	}

	private List<TMForumNotification> sentNotifications() {
		ArgumentCaptor<List<TMForumNotification>> sent = ArgumentCaptor.captor();
		verify(notificationSender).deliverNotifications(sent.capture());
		return sent.getValue();
	}
}
