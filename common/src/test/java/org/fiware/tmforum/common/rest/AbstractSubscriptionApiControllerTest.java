package org.fiware.tmforum.common.rest;

import io.github.wistefan.mapping.EntityVOMapper;
import org.fiware.tmforum.common.CommonConstants;
import org.fiware.tmforum.common.configuration.GeneralProperties;
import org.fiware.tmforum.common.domain.subscription.TMForumSubscription;
import org.fiware.tmforum.common.exception.TmForumException;
import org.fiware.tmforum.common.mapping.SubscriptionMapper;
import org.fiware.tmforum.common.querying.QueryParser;
import org.fiware.tmforum.common.repository.SubscriptionRepository;
import org.fiware.tmforum.storage.EntityRepository;
import org.fiware.tmforum.storage.PagedResult;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AbstractSubscriptionApiControllerTest {

	private static class TestSubscriptionController extends AbstractSubscriptionApiController {
		TestSubscriptionController(QueryParser queryParser, EntityRepository repository, GeneralProperties generalProperties) {
			super(queryParser, null, repository, Map.of(), null, null, generalProperties,
					mock(EntityVOMapper.class), mock(SubscriptionMapper.class));
		}
	}

	@Test
	public void theDuplicateCheckQueriesForTheEscapedRawQuery() {
		// rawQuery is stored escaped on the broker (see TMForumSubscription.getEscapedRawQuery), so
		// the duplicate-subscription check has to compare against the escaped value or it would
		// never find an existing match.
		GeneralProperties properties = new GeneralProperties();
		QueryParser queryParser = new QueryParser(properties);
		EntityRepository repository = mock(EntityRepository.class);
		TestSubscriptionController controller = new TestSubscriptionController(queryParser, repository, properties);
		controller.setSubscriptionRepository(mock(SubscriptionRepository.class));

		TMForumSubscription subscription = new TMForumSubscription("urn:ngsi-ld:tm-forum-subscription:1");
		subscription.setCallback(URI.create("https://example.org/callback"));
		subscription.setRawQuery("type=product&fields=name");

		// non-empty result makes assureNotExistingTMForumSubscription short-circuit with a CONFLICT
		// before reaching any of the (unmocked) rest of the create() flow.
		when(repository.findEntities(any(), any(), any(), any(), any()))
				.thenReturn(Mono.just(new PagedResult<>(List.of(subscription), 1, 1, null)));

		assertThrows(TmForumException.class, () -> controller.create(subscription).block());

		verify(repository).findEntities(eq(CommonConstants.DEFAULT_OFFSET), any(), eq(TMForumSubscription.TYPE_TM_FORUM_SUBSCRIPTION),
				eq(TMForumSubscription.class), contains("type%3Dproduct%26fields%3Dname"));
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
