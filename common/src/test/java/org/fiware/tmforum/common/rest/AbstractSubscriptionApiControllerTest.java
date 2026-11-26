package org.fiware.tmforum.common.rest;

import io.github.wistefan.mapping.EntityVOMapper;
import org.fiware.tmforum.common.CommonConstants;
import org.fiware.tmforum.common.configuration.GeneralProperties;
import org.fiware.tmforum.common.configuration.NgsiLdBrokerProperties;
import org.fiware.tmforum.common.domain.subscription.TMForumSubscription;
import org.fiware.tmforum.common.exception.TmForumException;
import org.fiware.tmforum.common.exception.TmForumExceptionReason;
import org.fiware.tmforum.common.mapping.SubscriptionMapper;
import org.fiware.tmforum.common.notification.DirectEntityEventPublisher;
import org.fiware.tmforum.common.notification.EntityEventPublisher;
import org.fiware.tmforum.common.notification.EventConstants;
import org.fiware.tmforum.common.notification.NgsiLdEventHandler;
import org.fiware.tmforum.common.notification.TMForumEventHandler;
import org.fiware.tmforum.common.querying.NgsiLdQueryRenderer;
import org.fiware.tmforum.common.querying.QueryParser;
import org.fiware.tmforum.common.repository.SubscriptionRepository;
import org.fiware.tmforum.storage.EntityRepository;
import org.fiware.tmforum.storage.PagedResult;
import org.fiware.tmforum.storage.query.AttributePath;
import org.fiware.tmforum.storage.query.ComparisonOperator;
import org.fiware.tmforum.storage.query.EntityQuery;
import org.fiware.tmforum.storage.query.QueryFilter;
import org.fiware.tmforum.storage.query.ValueType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;

import static org.fiware.tmforum.common.rest.AbstractSubscriptionApiController.SAME_CALLBACK_PAGE_SIZE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AbstractSubscriptionApiControllerTest {

	// holds the separators a TMForum query would split on
	private static final String CALLBACK = "https://example.org/callback?a=1&b=2,3;c=4";
	private static final String RAW_QUERY = "type=product&fields=name";
	private static final String OTHER_RAW_QUERY = "type=product&fields=id";
	private static final String PAST_THE_CHECK = "past the duplicate check";
	private static final String EVENT_GROUP = EventConstants.EVENT_GROUP_CATALOG;
	private static final String ENTITY_TYPE = "product-catalog";
	private static final URI SUBSCRIPTION_ID = URI.create("urn:ngsi-ld:tm-forum-subscription:1");

	private static class TestSubscriptionController extends AbstractSubscriptionApiController {
		TestSubscriptionController(QueryParser queryParser, EntityRepository repository, GeneralProperties generalProperties) {
			this(queryParser, repository, generalProperties, mock(NgsiLdEventHandler.class));
		}

		TestSubscriptionController(QueryParser queryParser, EntityRepository repository,
				GeneralProperties generalProperties, NgsiLdEventHandler ngsiLdEventHandler) {
			super(queryParser, null, repository, Map.of(EVENT_GROUP, ENTITY_TYPE), null, ngsiLdEventHandler,
					generalProperties, mock(EntityVOMapper.class), mock(SubscriptionMapper.class, RETURNS_DEEP_STUBS));
			setEventPublisher(new DirectEntityEventPublisher(repository, mock(TMForumEventHandler.class)));
		}
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("existingHubs")
	public void theDuplicateCheckComparesTheRawQueryOfTheHubsWithTheSameCallback(String description,
			List<List<String>> pagesOfRawQueries, boolean expectConflict) {
		EntityRepository repository = mock(EntityRepository.class);
		SubscriptionRepository subscriptionRepository = mock(SubscriptionRepository.class);
		GeneralProperties properties = new GeneralProperties();
		TestSubscriptionController controller = new TestSubscriptionController(new QueryParser(new GeneralProperties()), repository,
				properties);
		controller.setSubscriptionRepository(subscriptionRepository);
		// stops the create flow right after the duplicate check
		when(subscriptionRepository.createSubscription(any(), any()))
				.thenReturn(Mono.error(new IllegalStateException(PAST_THE_CHECK)));

		for (int page = 0; page < pagesOfRawQueries.size(); page++) {
			int offset = page * SAME_CALLBACK_PAGE_SIZE;
			when(repository.findEntities(eq(offset), eq(SAME_CALLBACK_PAGE_SIZE),
					eq(TMForumSubscription.TYPE_TM_FORUM_SUBSCRIPTION), eq(TMForumSubscription.class), any()))
					.thenReturn(Mono.just(new PagedResult<>(hubs(pagesOfRawQueries.get(page)), offset,
							SAME_CALLBACK_PAGE_SIZE, null)));
		}

		RuntimeException failure = assertThrows(RuntimeException.class,
				() -> controller.create(hub(RAW_QUERY)).block(), description);

		if (expectConflict) {
			assertEquals(TmForumExceptionReason.CONFLICT, ((TmForumException) failure).getExceptionReason(), description);
		} else {
			assertEquals(PAST_THE_CHECK, failure.getMessage(), description);
		}
		verify(repository).findEntities(eq(CommonConstants.DEFAULT_OFFSET), eq(SAME_CALLBACK_PAGE_SIZE),
				eq(TMForumSubscription.TYPE_TM_FORUM_SUBSCRIPTION), eq(TMForumSubscription.class),
				eq(new EntityQuery(TMForumSubscription.class,
						new QueryFilter.Comparison(new AttributePath(List.of("callback"), true),
								ComparisonOperator.EQUALS, ValueType.STRING, List.of(CALLBACK)),
						List.of())));
	}

	@Test
	public void theDuplicateCheckRendersTheCallbackAsOneQuotedNgsiLdLiteral() {
		EntityRepository repository = mock(EntityRepository.class);
		SubscriptionRepository subscriptionRepository = mock(SubscriptionRepository.class);
		GeneralProperties properties = new GeneralProperties();
		TestSubscriptionController controller = new TestSubscriptionController(new QueryParser(new GeneralProperties()), repository,
				properties);
		controller.setSubscriptionRepository(subscriptionRepository);
		when(subscriptionRepository.createSubscription(any(), any()))
				.thenReturn(Mono.error(new IllegalStateException(PAST_THE_CHECK)));
		when(repository.findEntities(any(), any(), any(), any(), any()))
				.thenReturn(Mono.just(new PagedResult<>(List.of(), CommonConstants.DEFAULT_OFFSET, SAME_CALLBACK_PAGE_SIZE,
						null)));

		assertThrows(RuntimeException.class, () -> controller.create(hub(RAW_QUERY)).block());

		ArgumentCaptor<EntityQuery> query = ArgumentCaptor.forClass(EntityQuery.class);
		verify(repository).findEntities(any(), any(), any(), any(), query.capture());
		assertEquals("callback==\"" + CALLBACK + "\"", new NgsiLdQueryRenderer(new NgsiLdBrokerProperties()).renderFilter(query.getValue()));
	}

	private static Stream<Arguments> existingHubs() {
		List<String> fullPageOfOtherQueries = Collections.nCopies(SAME_CALLBACK_PAGE_SIZE, OTHER_RAW_QUERY);
		return Stream.of(
				Arguments.of("No hub with the callback exists.", List.of(List.of()), false),
				Arguments.of("A hub with the callback and the same raw query exists.",
						List.of(List.of(OTHER_RAW_QUERY, RAW_QUERY)), true),
				Arguments.of("Hubs with the callback only have other raw queries.",
						List.of(List.of(OTHER_RAW_QUERY)), false),
				Arguments.of("The same raw query is on the second page.",
						List.of(fullPageOfOtherQueries, List.of(RAW_QUERY)), true),
				Arguments.of("A full first page is followed by an empty one.",
						List.of(fullPageOfOtherQueries, List.of()), false));
	}

	private static List<TMForumSubscription> hubs(List<String> rawQueries) {
		return rawQueries.stream().map(AbstractSubscriptionApiControllerTest::hub).toList();
	}

	private static TMForumSubscription hub(String rawQuery) {
		TMForumSubscription subscription = new TMForumSubscription("urn:ngsi-ld:tm-forum-subscription:" + UUID.randomUUID());
		subscription.setCallback(URI.create(CALLBACK));
		subscription.setRawQuery(rawQuery);
		return subscription;
	}

	@Test
	public void deleteRemovesTheBrokerSubscriptionBeforeTheEntity() {
		EntityRepository repository = mock(EntityRepository.class);
		SubscriptionRepository subscriptionRepository = mock(SubscriptionRepository.class);
		GeneralProperties properties = new GeneralProperties();
		TestSubscriptionController controller = new TestSubscriptionController(new QueryParser(new GeneralProperties()), repository,
				properties);
		controller.setSubscriptionRepository(subscriptionRepository);
		URI id = URI.create("urn:ngsi-ld:tm-forum-subscription:1");

		when(subscriptionRepository.deleteDomainSubscriptionByTmForumSubscription(id)).thenReturn(Mono.empty());
		when(repository.deleteDomainEntity(id)).thenReturn(Mono.empty());

		controller.delete(id.toString()).block();

		InOrder order = inOrder(subscriptionRepository, repository);
		order.verify(subscriptionRepository).deleteDomainSubscriptionByTmForumSubscription(id);
		order.verify(repository).deleteDomainEntity(id);
	}

	/** Broker: both NGSI-LD collaborators wired. PostgreSQL: neither is. */
	private static Stream<Arguments> backends() {
		return Stream.of(Arguments.of("broker", true), Arguments.of("postgresql", false));
	}

	private record Backend(TestSubscriptionController controller, EntityEventPublisher publisher,
			SubscriptionRepository subscriptionRepository) {
	}

	private static Backend backend(boolean broker) {
		EntityRepository repository = mock(EntityRepository.class);
		GeneralProperties properties = new GeneralProperties();
		TestSubscriptionController controller = new TestSubscriptionController(new QueryParser(new GeneralProperties()), repository,
				properties, broker ? mock(NgsiLdEventHandler.class) : null);
		SubscriptionRepository subscriptionRepository = mock(SubscriptionRepository.class);
		controller.setSubscriptionRepository(broker ? subscriptionRepository : null);
		EntityEventPublisher publisher = mock(EntityEventPublisher.class);
		controller.setEventPublisher(publisher);

		when(repository.findEntities(any(), any(), any(), any(), any()))
				.thenReturn(Mono.just(new PagedResult<>(List.of(), CommonConstants.DEFAULT_OFFSET, SAME_CALLBACK_PAGE_SIZE,
						null)));
		when(subscriptionRepository.createSubscription(any(), any())).thenReturn(Mono.empty());
		when(subscriptionRepository.deleteDomainSubscriptionByTmForumSubscription(any())).thenReturn(Mono.empty());
		when(publisher.createWithEvent(any())).thenReturn(Mono.empty());
		when(publisher.deleteWithEvent(any(), any())).thenReturn(Mono.empty());
		return new Backend(controller, publisher, subscriptionRepository);
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("backends")
	public void buildSubscriptionAddsTheNgsiLdSubscriptionOnlyOnTheBroker(String description, boolean broker) {
		TMForumSubscription subscription = backend(broker).controller()
				.buildSubscription(CALLBACK, null, List.of(EVENT_GROUP));

		assertEquals(List.of(ENTITY_TYPE), subscription.getEntities(), description);
		if (broker) {
			assertNotNull(subscription.getSubscription(), description);
			assertEquals(ENTITY_TYPE, subscription.getSubscription().getEntities().getFirst().getType(), description);
		} else {
			assertNull(subscription.getSubscription(), description);
		}
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("backends")
	public void createRegistersAtTheBrokerOnlyOnTheBroker(String description, boolean broker) {
		Backend backend = backend(broker);
		TMForumSubscription subscription = backend.controller().buildSubscription(CALLBACK, null, List.of(EVENT_GROUP));

		backend.controller().create(subscription).block();

		verify(backend.subscriptionRepository(), broker ? times(1) : never()).createSubscription(any(), any());
		ArgumentCaptor<TMForumSubscription> stored = ArgumentCaptor.forClass(TMForumSubscription.class);
		verify(backend.publisher()).createWithEvent(stored.capture());
		assertEquals(subscription, stored.getValue(), description);
		assertEquals(broker, stored.getValue().getSubscription() != null, description);
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("backends")
	public void deleteRemovesTheBrokerSubscriptionOnlyOnTheBroker(String description, boolean broker) {
		Backend backend = backend(broker);

		backend.controller().delete(SUBSCRIPTION_ID.toString()).block();

		verify(backend.subscriptionRepository(), broker ? times(1) : never())
				.deleteDomainSubscriptionByTmForumSubscription(SUBSCRIPTION_ID);
		verify(backend.publisher()).deleteWithEvent(SUBSCRIPTION_ID,
				List.of(TMForumSubscription.TYPE_TM_FORUM_SUBSCRIPTION));
	}
}
