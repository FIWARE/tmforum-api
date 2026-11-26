package org.fiware.tmforum.migration.postgresql;

import io.github.wistefan.mapping.EntityVOMapper;
import org.fiware.tmforum.common.configuration.GeneralProperties;
import org.fiware.tmforum.common.domain.subscription.Subscription;
import org.fiware.tmforum.common.domain.subscription.TMForumSubscription;
import org.fiware.tmforum.common.mapping.SubscriptionMapper;
import org.fiware.tmforum.common.notification.EventConstants;
import org.fiware.tmforum.common.notification.NgsiLdEventHandler;
import org.fiware.tmforum.common.querying.QueryParser;
import org.fiware.tmforum.common.repository.SubscriptionRepository;
import org.fiware.tmforum.common.rest.AbstractSubscriptionApiController;
import org.fiware.tmforum.storage.EntityRepository;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;

/**
 * The subscription {@code export} registers for a stored hub equals the one the hub controller registered for it.
 */
class HubSubscriptionParityTest {

	private static final String SERVER_HOST = "http://tmforum.example.org";
	private static final String BASEPATH = "/tmf-api/";
	private static final URI NOTIFICATION_ENDPOINT = URI.create(SERVER_HOST + "/tmf-api/callback");
	private static final String CALLBACK = "http://listener.example.org/hub?a=1";
	private static final Map<String, String> EVENT_GROUPS = Map.of(
			EventConstants.EVENT_GROUP_CATALOG, "product-catalog",
			EventConstants.EVENT_GROUP_CATEGORY, "category");
	private static final JsonMapper MAPPER = JsonMapper.builder().build();
	private static final ResponseDiff.Rules UNORDERED = new ResponseDiff.Rules(true, List.of("/id"), Set.of());

	static Stream<Arguments> hubs() {
		return Stream.of(
				Arguments.of(null, List.of(EventConstants.EVENT_GROUP_CATALOG)),
				Arguments.of("eventType=CatalogCreateEvent", List.of(EventConstants.EVENT_GROUP_CATALOG)),
				Arguments.of("eventType=CatalogCreateEvent&name=shop", List.of(EventConstants.EVENT_GROUP_CATALOG)),
				Arguments.of("eventType=CatalogCreateEvent&fields=name,description",
						List.of(EventConstants.EVENT_GROUP_CATALOG)),
				Arguments.of(null, List.of(EventConstants.EVENT_GROUP_CATALOG, EventConstants.EVENT_GROUP_CATEGORY)));
	}

	@ParameterizedTest(name = "{0} {1}")
	@MethodSource("hubs")
	void exportRegistersTheSubscriptionTheControllerRegistered(String query, List<String> eventGroups) {
		TMForumSubscription hub = controller().build(CALLBACK, query, eventGroups);
		Subscription registered = hub.getSubscription();
		// stored without it, as a hub created on PostgreSQL
		hub.setSubscription(null);

		Subscription exported = new ExportCommand(null, null, null,
				new ExportOptions(1, false, false, false, false, NOTIFICATION_ENDPOINT, Map.of())).subscription(hub);

		assertEquals(List.of(), ResponseDiff.diff(json(registered), json(exported), UNORDERED));
		assertEquals(NOTIFICATION_ENDPOINT, exported.getNotification().getEndpoint().getUri());
	}

	private static JsonNode json(Subscription subscription) {
		return MAPPER.valueToTree(subscription);
	}

	private static TestController controller() {
		GeneralProperties properties = new GeneralProperties();
		properties.setServerHost(SERVER_HOST);
		properties.setBasepath(BASEPATH);
		TestController controller = new TestController(properties);
		controller.setSubscriptionRepository(mock(SubscriptionRepository.class));
		return controller;
	}

	private static final class TestController extends AbstractSubscriptionApiController {

		TestController(GeneralProperties properties) {
			super(new QueryParser(), null, mock(EntityRepository.class), EVENT_GROUPS, null,
					mock(NgsiLdEventHandler.class), properties, mock(EntityVOMapper.class), mock(SubscriptionMapper.class));
		}

		TMForumSubscription build(String callback, String query, List<String> eventGroups) {
			return buildSubscription(callback, query, eventGroups);
		}
	}
}
