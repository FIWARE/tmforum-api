package org.fiware.tmforum.migration.postgresql;

import org.fiware.ngsi.api.EntitiesApiClient;
import org.fiware.ngsi.api.SubscriptionsApiClient;
import org.fiware.tmforum.common.configuration.GeneralProperties;
import org.fiware.tmforum.common.configuration.NgsiLdBrokerProperties;
import org.fiware.tmforum.common.domain.subscription.Endpoint;
import org.fiware.tmforum.common.domain.subscription.EntityInfo;
import org.fiware.tmforum.common.domain.subscription.KeyValuePair;
import org.fiware.tmforum.common.domain.subscription.NotificationParams;
import org.fiware.tmforum.common.domain.subscription.Subscription;
import org.fiware.tmforum.common.domain.subscription.TMForumSubscription;
import org.fiware.tmforum.common.mapping.ForbiddenCharacterEscaper;
import org.fiware.tmforum.common.mapping.SubscriptionMapper;
import org.fiware.tmforum.common.notification.EventConstants;
import org.fiware.tmforum.common.test.BrokerOnly;
import org.fiware.tmforum.common.test.NgsiLdTestEntityCleaner;
import org.fiware.tmforum.product.Product;
import org.fiware.tmforum.storage.EntityRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import picocli.CommandLine;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ObjectNode;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URI;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Registers hubs the way the broker-backed subscription controller does, imports them, and runs
 * {@code cleanup-subscriptions} against the test cluster's broker and PostgreSQL.
 */
@BrokerOnly
class CleanupSubscriptionsIT {

	private static final String JDBC_URL = "jdbc:postgresql://localhost:5432/tmforum";
	private static final String DB_CREDENTIAL = "tmforum";
	/** Set by the failsafe configuration from the build's {@code -Dbroker}. */
	private static final String BROKER_PROPERTY = "broker";
	private static final String TYPE = TMForumSubscription.TYPE_TM_FORUM_SUBSCRIPTION;
	private static final String DELETE_HUBS = "DELETE FROM tmforum.entity WHERE type = ?";
	private static final String SELECT_HUBS = "SELECT id, data::text FROM tmforum.entity WHERE type = ?";
	private static final String SUBSCRIPTION_KEY = RawAttributeCheck.SUBSCRIPTION_ATTRIBUTE;
	private static final String LISTENER_HEADER = "Listener-Endpoint";
	private static final String HUB_EVENT_TYPE = "ProductCreateEvent";
	private static final int HUB_COUNT = 2;
	private static final int PAGE_SIZE = 100;

	private final JsonMapper jsonMapper = JsonMapper.builder().build();
	// unique per run, so the orphan search sees no other test's subscriptions
	private final String callbackEndpoint = String.format("http://cleanup-%s.example.org%s", UUID.randomUUID(),
			EventConstants.SUBSCRIPTION_CALLBACK_PATH);
	private MigrationContexts seedContexts;
	private BrokerSource broker;
	private final List<URI> hubSubscriptions = new ArrayList<>();
	private URI orphan;
	private URI foreign;

	@BeforeEach
	void seedAndImport() throws SQLException {
		seedContexts = new MigrationContexts();
		new CommandLine(seedContexts).parseArgs(brokerTypeOption());
		seedContexts.start();
		broker = new BrokerSource(seedContexts.source(), EntityTypeRegistry.scan());
		deleteBrokerHubs();
		clearTarget();

		EntityRepository repository = seedContexts.source().getBean(EntityRepository.class);
		for (int i = 0; i < HUB_COUNT; i++) {
			TMForumSubscription hub = new TMForumSubscription(UUID.randomUUID().toString());
			hub.setCallback(URI.create("http://hub-" + i + ".example.org/listener"));
			hub.setRawQuery("");
			hub.setEventTypes(List.of(HUB_EVENT_TYPE));
			hub.setEntities(List.of(Product.TYPE_PRODUCT));
			hub.setSubscription(registerSubscription(callbackEndpoint, hub.getCallback()));
			repository.createDomainEntity(hub).block();
			hubSubscriptions.add(hub.getSubscription().getId());
		}
		// this service's endpoint, but no hub references it
		orphan = registerSubscription(callbackEndpoint, URI.create("http://lost.example.org/listener")).getId();
		foreign = registerSubscription("http://other-service.example.org/notify",
				URI.create("http://foreign.example.org/listener")).getId();

		assertEquals(CommandLine.ExitCode.OK, run("import", "--types", TYPE).exitCode(), "The import must succeed.");
	}

	@AfterEach
	void cleanUp() throws SQLException {
		if (seedContexts != null) {
			List<URI> leftovers = new ArrayList<>(hubSubscriptions);
			leftovers.add(orphan);
			leftovers.add(foreign);
			leftovers.stream().filter(Objects::nonNull).forEach(broker::deleteSubscription);
			deleteBrokerHubs();
			seedContexts.close();
		}
		clearTarget();
		ForbiddenCharacterEscaper.setEnabled(true);
	}

	@Test
	void deletesTheServicesSubscriptionsAndDropsTheAttribute() throws SQLException {
		Map<String, ObjectNode> before = storedHubs();
		assertEquals(HUB_COUNT, before.size());
		before.values().forEach(row -> assertTrue(row.has(SUBSCRIPTION_KEY),
				"The import must store the hub's subscription, else the PostgreSQL half has nothing to do: " + row));

		Run cleanup = run("cleanup-subscriptions", Application.CONFIRM_CUTOVER_FINAL, "--include-orphans",
				"--callback-endpoint", callbackEndpoint);

		assertEquals(CommandLine.ExitCode.OK, cleanup.exitCode(), cleanup.output());
		Set<URI> remaining = brokerSubscriptionIds();
		hubSubscriptions.forEach(id -> assertFalse(remaining.contains(id), id + " must be deleted"));
		assertFalse(remaining.contains(orphan), "The orphan must be deleted.");
		assertTrue(remaining.contains(foreign), "Another service's subscription must stay.");
		Map<String, ObjectNode> after = storedHubs();
		before.values().forEach(row -> row.remove(SUBSCRIPTION_KEY));
		assertEquals(before, after, "Only the subscription key may change.");
		assertTrue(cleanup.output().contains(
				String.format("PostgreSQL: %d hub rows hold a subscription attribute, %d updated", HUB_COUNT, HUB_COUNT)),
				cleanup.output());

		// idempotent: a re-run finds the subscriptions gone and nothing left to drop
		Run rerun = run("cleanup-subscriptions", Application.CONFIRM_CUTOVER_FINAL);

		assertEquals(CommandLine.ExitCode.OK, rerun.exitCode(), rerun.output());
		assertTrue(rerun.output().contains(String.format("referenced subscriptions: %d, deleted: 0, already missing: %d",
				HUB_COUNT, HUB_COUNT)), rerun.output());
		assertTrue(rerun.output().contains(CleanupSubscriptionsReport.NO_STORED_SUBSCRIPTION), rerun.output());
	}

	@Test
	void aDryRunListsButDeletesNothing() throws SQLException {
		Map<String, ObjectNode> before = storedHubs();

		Run dryRun = run("cleanup-subscriptions", "--dry-run", "--include-orphans",
				"--callback-endpoint", callbackEndpoint);

		assertEquals(CommandLine.ExitCode.OK, dryRun.exitCode(), dryRun.output());
		assertTrue(brokerSubscriptionIds().containsAll(hubSubscriptions), "A dry run must delete nothing.");
		assertTrue(brokerSubscriptionIds().contains(orphan), "A dry run must delete nothing.");
		assertEquals(before, storedHubs(), "A dry run must write nothing.");
		hubSubscriptions.forEach(id -> assertTrue(dryRun.output().contains("would delete " + id), dryRun.output()));
		assertTrue(dryRun.output().contains("would delete " + orphan), dryRun.output());
	}

	// as AbstractSubscriptionApiController#create registers a hub's subscription on the broker path
	private Subscription registerSubscription(String endpoint, URI listener) {
		Subscription subscription = new Subscription(UUID.randomUUID().toString());
		EntityInfo entityInfo = new EntityInfo();
		entityInfo.setType(Product.TYPE_PRODUCT);
		subscription.setEntities(List.of(entityInfo));
		NotificationParams notification = new NotificationParams();
		notification.setFormat(EventConstants.NOTIFICATION_FORMAT);
		// every attribute, as NgsiLdSubscriptionFactory sets it; SubscriptionMapper rejects a null set
		notification.setAttributes(Set.of());
		notification.setEndpoint(new Endpoint(URI.create(endpoint), EventConstants.NOTIFICATION_PAYLOAD_MIME_TYPE,
				List.of(new KeyValuePair(LISTENER_HEADER, listener.toString()))));
		subscription.setNotification(notification);
		GeneralProperties properties = seedContexts.source().getBean(GeneralProperties.class);
		seedContexts.source().getBean(SubscriptionsApiClient.class).createSubscription(
				seedContexts.source().getBean(SubscriptionMapper.class).map(subscription)
						.atContext(properties.getContextUrl()),
				seedContexts.source().getBean(NgsiLdBrokerProperties.class).getTenant()).block();
		return subscription;
	}

	private Set<URI> brokerSubscriptionIds() {
		return broker.subscriptions(PAGE_SIZE).stream()
				.map(SubscriptionStore.BrokerSubscription::id)
				.collect(Collectors.toSet());
	}

	private Map<String, ObjectNode> storedHubs() throws SQLException {
		Map<String, ObjectNode> rows = new HashMap<>();
		try (Connection connection = connection(); PreparedStatement select = connection.prepareStatement(SELECT_HUBS)) {
			select.setString(1, TYPE);
			try (ResultSet result = select.executeQuery()) {
				while (result.next()) {
					rows.put(result.getString(1), (ObjectNode) jsonMapper.readTree(result.getString(2)));
				}
			}
		}
		return rows;
	}

	private Run run(String command, String... options) {
		List<String> args = new ArrayList<>(List.of(command, brokerTypeOption()));
		args.addAll(List.of(options));
		StringWriter out = new StringWriter();
		StringWriter err = new StringWriter();
		int exitCode = new CommandLine(new Application())
				.setOut(new PrintWriter(out))
				.setErr(new PrintWriter(err))
				.execute(args.toArray(String[]::new));
		return new Run(exitCode, out + "\n" + err);
	}

	private void deleteBrokerHubs() {
		new NgsiLdTestEntityCleaner(seedContexts.source().getBean(EntitiesApiClient.class),
				seedContexts.source().getBean(GeneralProperties.class)).deleteAll(TYPE);
	}

	private static void clearTarget() throws SQLException {
		try (Connection connection = connection(); PreparedStatement delete = connection.prepareStatement(DELETE_HUBS)) {
			delete.setString(1, TYPE);
			delete.executeUpdate();
		}
	}

	private static Connection connection() throws SQLException {
		return DriverManager.getConnection(JDBC_URL, DB_CREDENTIAL, DB_CREDENTIAL);
	}

	private static String brokerTypeOption() {
		return "--broker-type=" + System.getProperty(BROKER_PROPERTY, BrokerType.ORION_LD.getEnvironment());
	}

	private record Run(int exitCode, String output) {
	}
}
