package org.fiware.tmforum.common.rest;

import io.github.wistefan.mapping.EntityVOMapper;
import org.fiware.tmforum.common.CommonConstants;
import org.fiware.tmforum.common.configuration.GeneralProperties;
import org.fiware.tmforum.common.domain.subscription.TMForumSubscription;
import org.fiware.tmforum.common.exception.TmForumException;
import org.fiware.tmforum.common.exception.TmForumExceptionReason;
import org.fiware.tmforum.common.mapping.SubscriptionMapper;
import org.fiware.tmforum.common.notification.DirectEntityEventPublisher;
import org.fiware.tmforum.common.notification.TMForumEventHandler;
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
import org.mockito.InOrder;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AbstractSubscriptionApiControllerTest {

	// holds the separators a TMForum query would split on
	private static final String CALLBACK = "https://example.org/callback?a=1&b=2,3;c=4";
	private static final String RAW_QUERY = "type=product&fields=name";
	private static final String OTHER_RAW_QUERY = "type=product&fields=id";
	private static final String PAST_THE_CHECK = "past the duplicate check";
	private static final int SAME_CALLBACK_PAGE_SIZE = 100;

	private static class TestSubscriptionController extends AbstractSubscriptionApiController {
		TestSubscriptionController(QueryParser queryParser, EntityRepository repository, GeneralProperties generalProperties) {
			super(queryParser, null, repository, Map.of(), null, null, generalProperties,
					mock(EntityVOMapper.class), mock(SubscriptionMapper.class, RETURNS_DEEP_STUBS));
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
		TestSubscriptionController controller = new TestSubscriptionController(new QueryParser(properties), repository,
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
		TestSubscriptionController controller = new TestSubscriptionController(new QueryParser(properties), repository,
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
}
