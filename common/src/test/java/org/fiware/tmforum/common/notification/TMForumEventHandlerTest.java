package org.fiware.tmforum.common.notification;

import io.github.wistefan.mapping.EntityVOMapper;
import org.fiware.ngsi.model.EntityVO;
import org.fiware.tmforum.common.domain.subscription.TMForumSubscription;
import org.fiware.tmforum.common.mapping.EventMapping;
import org.fiware.tmforum.common.querying.QueryParser;
import org.fiware.tmforum.common.querying.SubscriptionQueryResolver;
import org.fiware.tmforum.common.rest.CharacterizationEntity;
import org.fiware.tmforum.storage.EntityReadRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
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
	private static final long OUTBOX_ID = 42L;
	private static final Instant EVENT_TIME = Instant.parse("2026-01-01T00:00:00Z");
	private static final URI CALLBACK = URI.create("http://hub");

	private NotificationSender notificationSender;
	private EventMapper eventMapper;
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
		eventHandler = spy(new TMForumEventHandler(mock(QueryParser.class), new SubscriptionQueryResolver(),
				notificationSender, entityVOMapper, mock(EntityReadRepository.class), eventMapper, null));
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("deleteHubs")
	void handleDeleteEventNotifiesTheDeleteHubsWhoseQueryMatchedTheOldState(String description, String query,
																			  boolean expectNotified) {
		doReturn(Mono.just(List.of(hub(query)))).when(eventHandler).getSubscriptions(any());

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
	void handleDeleteEventLooksUpTheHubsOfTheDeleteEventType() {
		doReturn(Mono.just(List.of())).when(eventHandler).getSubscriptions(any());

		eventHandler.handleDeleteEvent(oldState(), OUTBOX_ID, EVENT_TIME).block();

		ArgumentCaptor<EventDetails> details = ArgumentCaptor.forClass(EventDetails.class);
		verify(eventHandler).getSubscriptions(details.capture());
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
		doReturn(Mono.just(List.of(hub(null)))).when(eventHandler).getSubscriptions(any());

		eventHandler.handleDeleteEvent(oldState(), OUTBOX_ID, EVENT_TIME).block();
		eventHandler.handleDeleteEvent(oldState(), OUTBOX_ID, EVENT_TIME).block();

		ArgumentCaptor<List<TMForumNotification>> sent = ArgumentCaptor.captor();
		verify(notificationSender, times(2)).deliverNotifications(sent.capture());
		assertEquals(sent.getAllValues().get(0).getFirst().event().getEventId(),
				sent.getAllValues().get(1).getFirst().event().getEventId());
		assertNotEquals(TMForumEventHandler.outboxEventId(OUTBOX_ID, DELETE_EVENT_TYPE),
				TMForumEventHandler.outboxEventId(OUTBOX_ID + 1, DELETE_EVENT_TYPE));
	}

	private static CharacterizationEntity oldState() {
		CharacterizationEntity oldState = new CharacterizationEntity(ENTITY_ID);
		oldState.setName(ENTITY_NAME);
		return oldState;
	}

	private static TMForumSubscription hub(String query) {
		TMForumSubscription subscription = new TMForumSubscription("urn:ngsi-ld:tm-forum-subscription:hub");
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
