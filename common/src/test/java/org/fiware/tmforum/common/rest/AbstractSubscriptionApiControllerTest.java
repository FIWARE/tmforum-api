package org.fiware.tmforum.common.rest;

import io.github.wistefan.mapping.EntityVOMapper;
import org.fiware.tmforum.common.CommonConstants;
import org.fiware.tmforum.common.configuration.GeneralProperties;
import org.fiware.tmforum.common.domain.subscription.TMForumSubscription;
import org.fiware.tmforum.common.exception.TmForumException;
import org.fiware.tmforum.common.mapping.SubscriptionMapper;
import org.fiware.tmforum.common.querying.QueryParser;
import org.fiware.tmforum.common.repository.PagedResult;
import org.fiware.tmforum.common.repository.TmForumRepository;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AbstractSubscriptionApiControllerTest {

	private static class TestSubscriptionController extends AbstractSubscriptionApiController {
		TestSubscriptionController(QueryParser queryParser, TmForumRepository repository, GeneralProperties generalProperties) {
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
		TmForumRepository repository = mock(TmForumRepository.class);
		TestSubscriptionController controller = new TestSubscriptionController(queryParser, repository, properties);

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
}
