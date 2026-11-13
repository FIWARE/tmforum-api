package org.fiware.tmforum.productcatalog;

import io.micronaut.context.annotation.Property;
import io.micronaut.context.annotation.Requires;
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
import org.fiware.tmforum.common.test.AbstractApiIT;
import org.fiware.tmforum.common.test.BrokerCallbackContextBuilder;
import org.fiware.tmforum.common.test.BrokerOnly;
import org.fiware.tmforum.productcatalog.domain.Catalog;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pins which hubs the broker path sends a DeleteEvent to, and its payload. Only hubs that take
 * {@code <Type>DeleteEvent} get one, but whatever their query: the broker notifies every subscription of a deletion
 * and its notification carries no state to evaluate the query on. The PostgreSQL outbox also honours the query and
 * sends the same id-only payload ({@link #BROKER_DELETE_PAYLOAD_FIELDS}).
 * The broker notifies the test server through {@link BrokerCallbackContextBuilder}'s callback host, as its own
 * {@code localhost} is the k3s container.
 * Relies on Orion-LD notifying a deletion ({@code deletedAt}) to a subscription without {@code notificationTrigger},
 * as {@code AbstractSubscriptionApiController} creates them (FIWARE/context.Orion-LD#1307), and on Orion-LD not
 * evaluating the subscription's {@code q} (the hub query) against the deleted entity; both hold on Orion-LD 1.9.0.
 */
@BrokerOnly
@MicronautTest(packages = {"org.fiware.tmforum.productcatalog"}, contextBuilder = BrokerCallbackContextBuilder.class)
// hub registration fails on Scorpio, see EventSubscriptionApiIT; DeleteEvents come from the broker only
@Requires(notEnv = {"scorpio", "postgresql"})
// without, the test client will try to create an empty q subscription
@Property(name = "jackson.serialization-inclusion", value = "non_empty")
@Property(name = RecordingHubController.ENABLED_PROPERTY, value = "true")
public class DeleteEventBrokerIT extends AbstractApiIT {

	/** Keys of the deleted catalog in a broker DeleteEvent. */
	public static final Set<String> BROKER_DELETE_PAYLOAD_FIELDS = Set.of("id");

	private static final String DELETE_EVENT = "CatalogDeleteEvent";
	private static final String MATCHING_STATUS = "created";
	private static final Duration DELIVERY_TIMEOUT = Duration.ofSeconds(20);
	private static final Duration QUIET_PERIOD = Duration.ofSeconds(5);

	private static final String HUB_CREATE_ONLY = "create-only";
	private static final String HUB_DELETE_ONLY = "delete-only";
	private static final String HUB_DELETE_MATCHING = "delete-matching";
	private static final String HUB_DELETE_NOT_MATCHING = "delete-not-matching";
	// no hub without eventType: suspected (unverified) that Orion-LD drops its multi-valued eventTypes
	private static final Map<String, String> HUB_QUERIES = Map.of(
			HUB_CREATE_ONLY, "eventType=CatalogCreateEvent",
			HUB_DELETE_ONLY, "eventType=" + DELETE_EVENT,
			HUB_DELETE_MATCHING, "eventType=" + DELETE_EVENT + "&event.catalog.lifecycleStatus=" + MATCHING_STATUS,
			HUB_DELETE_NOT_MATCHING, "eventType=" + DELETE_EVENT + "&event.catalog.lifecycleStatus=retired");

	private static final Set<String> HUBS_WITHOUT_DELETE_EVENTS = Set.of(HUB_CREATE_ONLY);
	// its NGSI-LD subscription's q does not match the deleted catalog, so it only gets a DeleteEvent if the broker
	// ignores q on deletion; not awaited, so the other hubs' results stay readable if it does not
	private static final Set<String> HUBS_DEPENDING_ON_BROKER_QUERY = Set.of(HUB_DELETE_NOT_MATCHING);

	private final CatalogApiTestClient catalogApiTestClient;
	private final EventsSubscriptionApiTestClient hubApiTestClient;
	private final RecordingHubController hubs;
	private final EmbeddedServer server;
	private final List<String> hubIds = new ArrayList<>();

	public DeleteEventBrokerIT(CatalogApiTestClient catalogApiTestClient,
							   EventsSubscriptionApiTestClient hubApiTestClient, RecordingHubController hubs,
							   EmbeddedServer server, EntitiesApiClient entitiesApiClient, ObjectMapper objectMapper,
							   GeneralProperties generalProperties) {
		super(entitiesApiClient, objectMapper, generalProperties);
		this.catalogApiTestClient = catalogApiTestClient;
		this.hubApiTestClient = hubApiTestClient;
		this.hubs = hubs;
		this.server = server;
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
	public void deletionNotifiesTheDeleteEventHubsWithAnIdOnlyPayload() throws Exception {
		for (Map.Entry<String, String> hub : HUB_QUERIES.entrySet()) {
			registerHub(hub.getKey(), hub.getValue());
		}
		HttpResponse<CatalogVO> created = callAndCatch(() -> catalogApiTestClient.createCatalog(null,
				new CatalogCreateVO().name("to-delete").lifecycleStatus(MATCHING_STATUS)));
		assertEquals(HttpStatus.CREATED, created.getStatus());
		String id = created.body().getId();
		assertEquals(HttpStatus.NO_CONTENT, callAndCatch(() -> catalogApiTestClient.deleteCatalog(null, id)).getStatus());

		await().atMost(DELIVERY_TIMEOUT).until(() -> HUB_QUERIES.keySet().stream()
				.filter(hub -> !HUBS_WITHOUT_DELETE_EVENTS.contains(hub))
				.filter(hub -> !HUBS_DEPENDING_ON_BROKER_QUERY.contains(hub))
				.allMatch(hub -> !deleteEvents(hub).isEmpty()));
		// later notifications would be duplicates or unwanted; give them the time to show up
		Thread.sleep(QUIET_PERIOD.toMillis());

		for (String hub : HUBS_WITHOUT_DELETE_EVENTS) {
			assertTrue(deleteEvents(hub).isEmpty(), String.format("Hub %s should get no DeleteEvent.", hub));
		}
		for (String hub : HUB_QUERIES.keySet()) {
			if (HUBS_WITHOUT_DELETE_EVENTS.contains(hub)) {
				continue;
			}
			List<Map<String, Object>> events = deleteEvents(hub);
			String hint = HUBS_DEPENDING_ON_BROKER_QUERY.contains(hub)
					? " None means Orion-LD applies the subscription's q on deletion: move the hub to "
					+ "HUBS_WITHOUT_DELETE_EVENTS and correct the recipient difference in the plan (Steps 6 and 8)."
					: "";
			assertEquals(1, events.size(),
					String.format("Hub %s should get exactly one DeleteEvent.%s", hub, hint));
			Map<?, ?> catalog = (Map<?, ?>) ((Map<?, ?>) events.getFirst().get("event")).get("catalog");
			assertEquals(BROKER_DELETE_PAYLOAD_FIELDS, catalog.keySet(),
					String.format("Hub %s should get the id-only payload.", hub));
			assertEquals(id, catalog.get("id"));
		}
	}

	private List<Map<String, Object>> deleteEvents(String hub) {
		return hubs.received(hub).stream()
				.filter(event -> DELETE_EVENT.equals(event.get("eventType")))
				.toList();
	}

	private void registerHub(String hub, String query) throws Exception {
		EventSubscriptionInputVO input = new EventSubscriptionInputVO()
				.callback(server.getURI() + RecordingHubController.PATH + "/" + hub)
				.query(query);
		HttpResponse<EventSubscriptionVO> response = callAndCatch(() -> hubApiTestClient.registerListener(null, input));
		assertEquals(HttpStatus.CREATED, response.getStatus(), String.format("Hub %s should be registered.", hub));
		assertTrue(response.getBody().isPresent());
		hubIds.add(response.getBody().get().getId());
	}
}
