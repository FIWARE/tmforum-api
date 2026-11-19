package org.fiware.tmforum.productcatalog;

import io.micronaut.context.annotation.Property;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import io.micronaut.runtime.server.EmbeddedServer;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import org.fiware.ngsi.api.EntitiesApiClient;
import org.fiware.productcatalog.api.CatalogApiTestClient;
import org.fiware.productcatalog.api.EventsSubscriptionApiTestClient;
import org.fiware.productcatalog.model.CatalogCreateVO;
import org.fiware.productcatalog.model.CatalogVO;
import org.fiware.productcatalog.model.EventSubscriptionInputVO;
import org.fiware.productcatalog.model.EventSubscriptionVO;
import org.fiware.tmforum.common.configuration.GeneralProperties;
import org.fiware.tmforum.common.domain.subscription.TMForumSubscription;
import org.fiware.tmforum.common.test.AbstractApiIT;
import org.fiware.tmforum.common.test.FirstMatchConfigContextBuilder;
import org.fiware.tmforum.common.test.PostgresqlOnly;
import org.fiware.tmforum.productcatalog.domain.Catalog;
import org.fiware.tmforum.storage.EntityRepository;
import org.fiware.tmforum.storage.postgresql.PostgresStorage;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import tools.jackson.databind.ObjectMapper;

import java.net.URI;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * On PostgreSQL, a hub registered through the API is stored without an NGSI-LD subscription, and the outbox delivers
 * its TMForum events with no broker involved. Broker delivery is pinned by {@link DeleteEventBrokerIT}.
 */
@PostgresqlOnly
@MicronautTest(packages = {"org.fiware.tmforum.productcatalog"}, environments = PostgresStorage.ENVIRONMENT,
		contextBuilder = FirstMatchConfigContextBuilder.class)
@Property(name = RecordingHubController.ENABLED_PROPERTY, value = "true")
public class HubEventDeliveryIT extends AbstractApiIT {

	private static final String EVENT_TYPE_KEY = "eventType";
	private static final String EVENT_TYPE_QUERY = "eventType=";
	private static final Duration DELIVERY_TIMEOUT = Duration.ofSeconds(20);
	private static final Duration QUIET_PERIOD = Duration.ofSeconds(5);

	/**
	 * The registered hubs, each taking one event type; {@link #OTHER_ENTITY} must never get a catalog event.
	 */
	enum Hub {
		CREATE("CatalogCreateEvent"),
		DELETE("CatalogDeleteEvent"),
		OTHER_ENTITY("CategoryCreateEvent");

		private final String eventType;

		Hub(String eventType) {
			this.eventType = eventType;
		}

		private String path() {
			return name().toLowerCase();
		}
	}

	private final CatalogApiTestClient catalogApiTestClient;
	private final EventsSubscriptionApiTestClient hubApiTestClient;
	private final RecordingHubController hubs;
	private final EmbeddedServer server;
	private final EntityRepository entityRepository;
	private final List<String> hubIds = new ArrayList<>();

	public HubEventDeliveryIT(CatalogApiTestClient catalogApiTestClient,
							  EventsSubscriptionApiTestClient hubApiTestClient, RecordingHubController hubs,
							  EmbeddedServer server, EntityRepository entityRepository,
							  EntitiesApiClient entitiesApiClient, ObjectMapper objectMapper,
							  GeneralProperties generalProperties) {
		super(entitiesApiClient, objectMapper, generalProperties);
		this.catalogApiTestClient = catalogApiTestClient;
		this.hubApiTestClient = hubApiTestClient;
		this.hubs = hubs;
		this.server = server;
		this.entityRepository = entityRepository;
	}

	@Override
	protected String getEntityType() {
		return Catalog.TYPE_CATALOG;
	}

	@AfterEach
	public void unregisterHubs() throws Exception {
		for (String hubId : hubIds) {
			callAndCatch(() -> hubApiTestClient.unregisterListener(null, hubId));
		}
		hubIds.clear();
		hubs.clear();
	}

	@Test
	public void hubIsStoredWithoutNgsiLdSubscription() throws Exception {
		String hubId = registerHub(Hub.CREATE);

		TMForumSubscription stored = entityRepository.get(URI.create(hubId), TMForumSubscription.class).block();

		assertNotNull(stored, "The hub should be stored.");
		assertNull(stored.getSubscription(), "No NGSI-LD subscription should be created on PostgreSQL.");
	}

	@ParameterizedTest(name = "{0}")
	@EnumSource(value = Hub.class, names = {"CREATE", "DELETE"})
	public void eventReachesOnlyTheMatchingHub(Hub matching) throws Exception {
		for (Hub hub : Hub.values()) {
			registerHub(hub);
		}
		HttpResponse<CatalogVO> created = callAndCatch(
				() -> catalogApiTestClient.createCatalog(null, new CatalogCreateVO().name("hub-event")));
		assertEquals(HttpStatus.CREATED, created.getStatus());
		String id = created.body().getId();
		assertEquals(HttpStatus.NO_CONTENT, callAndCatch(() -> catalogApiTestClient.deleteCatalog(null, id)).getStatus());

		await().atMost(DELIVERY_TIMEOUT).until(() -> !events(matching, matching.eventType).isEmpty());
		// later deliveries would be duplicates or misrouted; give them the time to show up
		Thread.sleep(QUIET_PERIOD.toMillis());

		for (Hub hub : Hub.values()) {
			int expected = hub == matching ? 1 : 0;
			assertEquals(expected, events(hub, matching.eventType).size(),
					String.format("Hub %s should get %d %s.", hub, expected, matching.eventType));
		}
		assertTrue(hubs.received(Hub.OTHER_ENTITY.path()).isEmpty(), "No catalog event should reach a category hub.");
	}

	private List<Map<String, Object>> events(Hub hub, String eventType) {
		return hubs.received(hub.path()).stream()
				.filter(event -> eventType.equals(event.get(EVENT_TYPE_KEY)))
				.toList();
	}

	private String registerHub(Hub hub) throws Exception {
		EventSubscriptionInputVO input = new EventSubscriptionInputVO()
				.callback(server.getURI() + RecordingHubController.PATH + "/" + hub.path())
				.query(EVENT_TYPE_QUERY + hub.eventType);
		HttpResponse<EventSubscriptionVO> response = callAndCatch(() -> hubApiTestClient.registerListener(null, input));
		assertEquals(HttpStatus.CREATED, response.getStatus(), String.format("Hub %s should be registered.", hub));
		assertTrue(response.getBody().isPresent());
		String hubId = response.getBody().get().getId();
		hubIds.add(hubId);
		return hubId;
	}
}
