package org.fiware.tmforum.migration.postgresql;

import io.micronaut.context.ApplicationContext;
import org.fiware.ngsi.api.EntitiesApiClient;
import org.fiware.ngsi.api.SubscriptionsApiClient;
import org.fiware.tmforum.common.CommonConstants;
import org.fiware.tmforum.common.configuration.GeneralProperties;
import org.fiware.tmforum.common.domain.subscription.Subscription;
import org.fiware.tmforum.common.domain.subscription.TMForumSubscription;
import org.fiware.tmforum.common.mapping.ForbiddenCharacterEscaper;
import org.fiware.tmforum.common.mapping.SubscriptionMapper;
import org.fiware.tmforum.common.notification.EventConstants;
import org.fiware.tmforum.common.notification.NgsiLdSubscriptionFactory;
import org.fiware.tmforum.common.repository.SubscriptionRepository;
import org.fiware.tmforum.common.test.BrokerOnly;
import org.fiware.tmforum.common.test.NgsiLdTestEntityCleaner;
import org.fiware.tmforum.product.Product;
import org.fiware.tmforum.storage.EntityRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import picocli.CommandLine;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Imports a product and a hub from the test cluster's broker, adds a hub on PostgreSQL, empties the broker and runs
 * {@code export} and {@code verify --direction export}, the rollback after cutover.
 */
@BrokerOnly
class ExportIT {

	private static final String JDBC_URL = "jdbc:postgresql://localhost:5432/tmforum";
	private static final String DB_CREDENTIAL = "tmforum";
	/** Set by the failsafe configuration from the build's {@code -Dbroker}. */
	private static final String BROKER_PROPERTY = "broker";
	private static final String DELETE_TYPE = "DELETE FROM tmforum.entity WHERE type = ?";
	private static final String DELETE_OUTBOX = "DELETE FROM tmforum.event_outbox";
	private static final String HUB_TYPE = TMForumSubscription.TYPE_TM_FORUM_SUBSCRIPTION;
	private static final List<String> TYPES = List.of(Product.TYPE_PRODUCT, HUB_TYPE);
	private static final String HUB_EVENT_TYPE = "ProductCreateEvent";
	private static final String IMPORTED_QUERY_VALUE = "imported";
	private static final String CREATED_QUERY_VALUE = "created-on-postgresql";
	private static final String QUERY_TEMPLATE = "name==\"%s\"";
	private static final String RAW_QUERY_TEMPLATE = "name=%s";
	private static final int PAGE_SIZE = 100;
	private static final int HTTP_OK = 200;

	private final HttpClient httpClient = HttpClient.newHttpClient();
	private final JsonMapper jsonMapper = JsonMapper.builder().build();
	// unique per run, so the export's duplicate check sees no other test's subscriptions
	private final URI notificationEndpoint = URI.create(String.format("http://export-%s.example.org%s",
			UUID.randomUUID(), EventConstants.SUBSCRIPTION_CALLBACK_PATH));
	private MigrationContexts seedContexts;
	private BrokerSource broker;
	private URI importedHubId;
	private URI createdHubId;
	private URI importedSubscriptionId;

	@BeforeEach
	void importAndEmptyTheBroker() throws SQLException {
		seedContexts = new MigrationContexts();
		new CommandLine(seedContexts).parseArgs(brokerTypeOption());
		seedContexts.start();
		broker = new BrokerSource(seedContexts.source(), EntityTypeRegistry.scan());
		deleteBrokerEntities();
		clearTarget();

		EntityRepository brokerRepository = seedContexts.source().getBean(EntityRepository.class);
		Product product = new Product(urn(Product.TYPE_PRODUCT));
		product.setName("product");
		brokerRepository.createDomainEntity(product).block();
		TMForumSubscription importedHub = hub(IMPORTED_QUERY_VALUE);
		// registered as the broker-backed hub controller does
		importedHub.setSubscription(NgsiLdSubscriptionFactory.build(notificationEndpoint,
				importedHub.getCallback().toString(), importedHub.getQuery(), importedHub.getEntities(), List.of()));
		registerSubscription(importedHub.getSubscription());
		brokerRepository.createDomainEntity(importedHub).block();
		importedHubId = importedHub.getId();
		importedSubscriptionId = importedHub.getSubscription().getId();

		assertEquals(CommandLine.ExitCode.OK, run("import").exitCode(), "The import must succeed.");

		// created after the cutover: PostgreSQL holds no subscription for it
		TMForumSubscription createdHub = hub(CREATED_QUERY_VALUE);
		seedContexts.target().getBean(EntityRepository.class).createDomainEntity(createdHub).block();
		createdHubId = createdHub.getId();

		// the rollback starts from an empty broker
		assertTrue(broker.deleteSubscription(importedSubscriptionId));
		deleteBrokerEntities();
	}

	@AfterEach
	void cleanUp() throws SQLException {
		if (seedContexts != null) {
			if (importedSubscriptionId != null) {
				broker.deleteSubscription(importedSubscriptionId);
			}
			notifyingSubscriptions().forEach(broker::deleteSubscription);
			deleteBrokerEntities();
			seedContexts.close();
		}
		clearTarget();
		ForbiddenCharacterEscaper.setEnabled(true);
	}

	@Test
	void restoresTheEntitiesAndOneSubscriptionPerHub() throws Exception {
		Run export = run("export", "--notification-endpoint", notificationEndpoint.toString());

		assertEquals(CommandLine.ExitCode.OK, export.exitCode(), export.output());
		Run verify = run("verify", "--direction", "export");
		assertEquals(CommandLine.ExitCode.OK, verify.exitCode(), verify.output());

		Set<URI> notifying = notifyingSubscriptions();
		assertEquals(2, notifying.size(), "Exactly one subscription per hub: " + notifying);
		Map<URI, String> queryValues = Map.of(importedHubId, IMPORTED_QUERY_VALUE, createdHubId, CREATED_QUERY_VALUE);
		Map<URI, URI> subscriptionByHub = new HashMap<>();
		for (Map.Entry<URI, String> hub : queryValues.entrySet()) {
			URI subscriptionId = brokerHub(hub.getKey()).getSubscription().getId();
			assertTrue(notifying.contains(subscriptionId), hub.getKey() + " must reference a new subscription.");
			JsonNode subscription = brokerSubscription(subscriptionId);
			assertTrue(subscription.path("q").asString().contains(hub.getValue()), subscription.toString());
			assertEquals(Product.TYPE_PRODUCT, subscription.path("entities").path(0).path("type").asString(),
					subscription.toString());
			subscriptionByHub.put(hub.getKey(), subscriptionId);
		}

		// as the broker-backed controller deletes a hub
		seedContexts.source().getBean(SubscriptionRepository.class)
				.deleteDomainSubscriptionByTmForumSubscription(importedHubId).block();

		Set<URI> remaining = notifyingSubscriptions();
		assertFalse(remaining.contains(subscriptionByHub.get(importedHubId)), "Deleting the hub must remove it.");
		assertTrue(remaining.contains(subscriptionByHub.get(createdHubId)), "The other hub's must stay.");
	}

	@Test
	void refusesABrokerThatAlreadyNotifiesTheEndpoint() {
		assertEquals(CommandLine.ExitCode.OK,
				run("export", "--notification-endpoint", notificationEndpoint.toString()).exitCode());

		Run again = run("export", "--notification-endpoint", notificationEndpoint.toString());
		Run resumed = run("export", "--resume", "--notification-endpoint", notificationEndpoint.toString());

		assertEquals(CommandLine.ExitCode.SOFTWARE, again.exitCode(), again.output());
		assertTrue(again.output().contains("already holds"), again.output());
		assertEquals(CommandLine.ExitCode.OK, resumed.exitCode(), resumed.output());
		assertEquals(2, notifyingSubscriptions().size(), "A resumed export must not duplicate subscriptions.");
	}

	private TMForumSubscription hub(String queryValue) {
		TMForumSubscription hub = new TMForumSubscription(urn(HUB_TYPE));
		hub.setCallback(URI.create("http://" + queryValue + ".example.org/listener"));
		hub.setEventTypes(List.of(HUB_EVENT_TYPE));
		hub.setEntities(List.of(Product.TYPE_PRODUCT));
		hub.setQuery(String.format(QUERY_TEMPLATE, queryValue));
		hub.setRawQuery(String.format(RAW_QUERY_TEMPLATE, queryValue));
		return hub;
	}

	private void registerSubscription(Subscription subscription) {
		ApplicationContext source = seedContexts.source();
		GeneralProperties properties = source.getBean(GeneralProperties.class);
		source.getBean(SubscriptionsApiClient.class).createSubscription(
				source.getBean(SubscriptionMapper.class).map(subscription).atContext(properties.getContextUrl()),
				properties.getTenant()).block();
	}

	private TMForumSubscription brokerHub(URI id) {
		return broker.toDomain(broker.entity(id).orElseThrow(() -> new AssertionError(id + " was not exported.")),
				TMForumSubscription.class);
	}

	private Set<URI> notifyingSubscriptions() {
		return broker.subscriptions(PAGE_SIZE).stream()
				.filter(subscription -> notificationEndpoint.toString().equals(subscription.endpoint()))
				.map(SubscriptionStore.BrokerSubscription::id)
				.collect(Collectors.toSet());
	}

	private JsonNode brokerSubscription(URI id) throws IOException, InterruptedException {
		GeneralProperties properties = seedContexts.source().getBean(GeneralProperties.class);
		String brokerUrl = seedContexts.source().getRequiredProperty(MigrationContexts.NGSI_URL_PROPERTY, String.class);
		HttpRequest.Builder request = HttpRequest.newBuilder(URI.create(brokerUrl + String.format(
						BrokerSource.SUBSCRIPTION_PATH, URLEncoder.encode(id.toString(), StandardCharsets.UTF_8))))
				.header("Link", String.format(CommonConstants.JSON_LD_CONTEXT_LINK_TEMPLATE, properties.getContextUrl()))
				.GET();
		if (properties.getTenant() != null) {
			request.header("NGSILD-Tenant", properties.getTenant());
		}
		HttpResponse<String> response = httpClient.send(request.build(), HttpResponse.BodyHandlers.ofString());
		assertEquals(HTTP_OK, response.statusCode(), response.body());
		return jsonMapper.readTree(response.body());
	}

	private Run run(String command, String... options) {
		List<String> args = new ArrayList<>(List.of(command, brokerTypeOption(), "--types", String.join(",", TYPES)));
		args.addAll(List.of(options));
		StringWriter out = new StringWriter();
		StringWriter err = new StringWriter();
		int exitCode = new CommandLine(new Application())
				.setOut(new PrintWriter(out))
				.setErr(new PrintWriter(err))
				.execute(args.toArray(String[]::new));
		return new Run(exitCode, out + "\n" + err);
	}

	private void deleteBrokerEntities() {
		NgsiLdTestEntityCleaner cleaner = new NgsiLdTestEntityCleaner(
				seedContexts.source().getBean(EntitiesApiClient.class),
				seedContexts.source().getBean(GeneralProperties.class));
		TYPES.forEach(cleaner::deleteAll);
	}

	private static void clearTarget() throws SQLException {
		try (Connection connection = DriverManager.getConnection(JDBC_URL, DB_CREDENTIAL, DB_CREDENTIAL);
			 PreparedStatement deleteType = connection.prepareStatement(DELETE_TYPE);
			 PreparedStatement deleteOutbox = connection.prepareStatement(DELETE_OUTBOX)) {
			for (String type : TYPES) {
				deleteType.setString(1, type);
				deleteType.executeUpdate();
			}
			deleteOutbox.executeUpdate();
		}
	}

	private static String brokerTypeOption() {
		return "--broker-type=" + System.getProperty(BROKER_PROPERTY, BrokerType.ORION_LD.getEnvironment());
	}

	private static String urn(String type) {
		return String.format("urn:ngsi-ld:%s:%s", type, UUID.randomUUID());
	}

	private record Run(int exitCode, String output) {
	}
}
