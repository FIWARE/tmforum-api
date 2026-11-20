package org.fiware.tmforum.common.notification;

import io.github.wistefan.mapping.EntityVOMapper;
import org.fiware.ngsi.model.EndpointVO;
import org.fiware.ngsi.model.EntityVO;
import org.fiware.ngsi.model.KeyValuePairVO;
import org.fiware.ngsi.model.NotificationParamsVO;
import org.fiware.ngsi.model.NotificationVO;
import org.fiware.ngsi.model.SubscriptionVO;
import org.fiware.tmforum.common.domain.subscription.Subscription;
import org.fiware.tmforum.common.domain.subscription.TMForumSubscription;
import org.fiware.tmforum.common.mapping.EventMapping;
import org.fiware.tmforum.common.rest.CharacterizationEntity;
import org.junit.jupiter.api.BeforeEach;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class NgsiLdEventHandlerTest {

	private static final URI ENTITY_ID = URI.create("urn:ngsi-ld:characterization-entity:deleted");
	private static final String DELETE_EVENT_TYPE = "CharacterizationEntityDeleteEvent";
	private static final String LISTENER_ENDPOINT_KEY = "Listener-Endpoint";
	private static final URI CALLBACK = URI.create("http://hub");
	private static final URI NOTIFIED_SUBSCRIPTION = URI.create("urn:ngsi-ld:Subscription:notified");
	private static final URI OTHER_SUBSCRIPTION = URI.create("urn:ngsi-ld:Subscription:other");
	private static final String OTHER_TYPE = "other-entity";
	private static final Instant DELETED_AT = Instant.parse("2026-01-01T00:00:00Z");

	private NotificationSender notificationSender;
	private TMForumEventHandler tmForumEventHandler;
	private EntityVOMapper entityVOMapper;
	private NgsiLdEventHandler eventHandler;

	@BeforeEach
	void setUp() {
		notificationSender = mock(NotificationSender.class);
		when(notificationSender.sendNotifications(anyList())).thenReturn(Mono.empty());
		EventMapper eventMapper = mock(EventMapper.class);
		when(eventMapper.getEntityClassMapping()).thenReturn(Map.of(CharacterizationEntity.TYPE,
				new EventMapping(CharacterizationEntity.class, CharacterizationEntity.class)));
		when(eventMapper.mapPayload(any(), any())).thenAnswer(invocation -> invocation.getArgument(0));
		entityVOMapper = mock(EntityVOMapper.class);
		when(entityVOMapper.fromEntityVO(any(), eq(CharacterizationEntity.class)))
				.thenAnswer(invocation -> Mono.just(new CharacterizationEntity(ENTITY_ID.toString())));
		when(entityVOMapper.convertEntityToMap(any())).thenReturn(Map.of("id", ENTITY_ID.toString()));
		tmForumEventHandler = mock(TMForumEventHandler.class);
		eventHandler = new NgsiLdEventHandler(notificationSender, entityVOMapper, eventMapper, null,
				tmForumEventHandler);
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("deleteHubs")
	void deleteEventReachesOnlyHubsTakingDeleteEvents(String description, List<TMForumSubscription> deleteHubs,
													   int expectedNotifications) {
		when(tmForumEventHandler.findSubscriptions(any())).thenReturn(Mono.just(deleteHubs));

		eventHandler.handle(deletion(), subscription()).block();

		ArgumentCaptor<List<TMForumNotification>> sent = ArgumentCaptor.captor();
		verify(notificationSender).sendNotifications(sent.capture());
		assertEquals(expectedNotifications, sent.getValue().size());
		ArgumentCaptor<EventDetails> details = ArgumentCaptor.forClass(EventDetails.class);
		verify(tmForumEventHandler).findSubscriptions(details.capture());
		assertEquals(DELETE_EVENT_TYPE, details.getValue().eventType());
	}

	@ParameterizedTest(name = "notified type {0}")
	@MethodSource("notifiedTypes")
	void deletionWithoutTypeTakesTheTypeOfItsId(String notifiedType) {
		EventMapper eventMapper = mock(EventMapper.class);
		Map<String, EventMapping> mapping = new LinkedHashMap<>();
		// listed first, so a fallback to the first mapped type would pick it
		mapping.put(OTHER_TYPE, new EventMapping(CharacterizationEntity.class, CharacterizationEntity.class));
		mapping.put(CharacterizationEntity.TYPE,
				new EventMapping(CharacterizationEntity.class, CharacterizationEntity.class));
		when(eventMapper.getEntityClassMapping()).thenReturn(mapping);
		when(eventMapper.mapPayload(any(), any())).thenAnswer(invocation -> invocation.getArgument(0));
		when(tmForumEventHandler.findSubscriptions(any())).thenReturn(Mono.just(List.of(hub(NOTIFIED_SUBSCRIPTION))));
		NotificationVO deletion = deletion();
		deletion.getData().getFirst().setType(notifiedType);

		new NgsiLdEventHandler(notificationSender, entityVOMapper, eventMapper, null, tmForumEventHandler)
				.handle(deletion, subscription()).block();

		ArgumentCaptor<EventDetails> details = ArgumentCaptor.forClass(EventDetails.class);
		verify(tmForumEventHandler).findSubscriptions(details.capture());
		assertEquals(DELETE_EVENT_TYPE, details.getValue().eventType());
	}

	private static Stream<Arguments> notifiedTypes() {
		return Stream.of(Arguments.of(CharacterizationEntity.TYPE), Arguments.of((String) null));
	}

	private static Stream<Arguments> deleteHubs() {
		return Stream.of(
				Arguments.of("the notified hub takes DeleteEvents", List.of(hub(NOTIFIED_SUBSCRIPTION)), 1),
				Arguments.of("only another hub on the same callback takes DeleteEvents",
						List.of(hub(OTHER_SUBSCRIPTION)), 0),
				Arguments.of("no hub takes DeleteEvents", List.of(), 0));
	}

	// every hub shares the notified callback, only its NGSI-LD subscription tells them apart
	private static TMForumSubscription hub(URI ngsiLdSubscription) {
		TMForumSubscription subscription = new TMForumSubscription("urn:ngsi-ld:tm-forum-subscription:hub");
		subscription.setCallback(CALLBACK);
		subscription.setSubscription(new Subscription(ngsiLdSubscription.toString()));
		return subscription;
	}

	private static NotificationVO deletion() {
		return new NotificationVO()
				.notifiedAt(DELETED_AT)
				.data(List.of(new EntityVO().id(ENTITY_ID).type(CharacterizationEntity.TYPE).deletedAt(DELETED_AT)));
	}

	private static SubscriptionVO subscription() {
		return new SubscriptionVO().id(NOTIFIED_SUBSCRIPTION).notification(new NotificationParamsVO().endpoint(new EndpointVO()
				.receiverInfo(List.of(new KeyValuePairVO().key(LISTENER_ENDPOINT_KEY).value(CALLBACK.toString())))));
	}
}
