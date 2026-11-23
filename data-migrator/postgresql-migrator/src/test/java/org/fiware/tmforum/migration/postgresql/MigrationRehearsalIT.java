package org.fiware.tmforum.migration.postgresql;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import io.micronaut.context.ApplicationContext;
import io.micronaut.runtime.server.EmbeddedServer;
import org.fiware.ngsi.api.EntitiesApiClient;
import org.fiware.ngsi.api.SubscriptionsApiClient;
import org.fiware.tmforum.common.CommonConstants;
import org.fiware.tmforum.common.configuration.GeneralProperties;
import org.fiware.tmforum.common.domain.subscription.Subscription;
import org.fiware.tmforum.common.domain.subscription.TMForumSubscription;
import org.fiware.tmforum.common.mapping.ForbiddenCharacterEscaper;
import org.fiware.tmforum.common.mapping.SubscriptionMapper;
import org.fiware.tmforum.common.notification.NgsiLdSubscriptionFactory;
import org.fiware.tmforum.common.querying.SubscriptionQuery;
import org.fiware.tmforum.common.querying.SubscriptionQueryParser;
import org.fiware.tmforum.common.test.BrokerOnly;
import org.fiware.tmforum.common.test.NgsiLdTestEntityCleaner;
import org.fiware.tmforum.product.Characteristic;
import org.fiware.tmforum.product.Product;
import org.fiware.tmforum.storage.EntityRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import picocli.CommandLine;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ObjectNode;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The runbook end to end: two in-process all-in-one servers, one on the test cluster's broker and one on its
 * PostgreSQL. Seeds the broker, runs {@code import --backfill-references}, {@code verify} and {@code replay}, writes
 * through the PostgreSQL server, then rolls back with {@code export}. It then checks the replay again, checks that every
 * restored hub has its NGSI-LD subscription on the broker (the DeleteEvent path), and checks that both hubs get a create
 * notification, which the broker server sends in-process from the restored hub entities.
 */
@BrokerOnly
class MigrationRehearsalIT {

	private static final String JDBC_URL = "jdbc:postgresql://localhost:5432/tmforum";
	private static final String DB_CREDENTIAL = "tmforum";
	/** Both set by the failsafe configuration from the build's {@code -Dbroker} and {@code -Dcache}. */
	private static final String BROKER_PROPERTY = "broker";
	private static final String CACHE_PROPERTY = "cache";
	private static final String DEFAULT_CACHE = "in-memory";
	private static final String DELETE_TYPE = "DELETE FROM tmforum.entity WHERE type = ?";
	private static final String DELETE_OUTBOX = "DELETE FROM tmforum.event_outbox";
	private static final String ENTITY_TABLE_EXISTS = "SELECT to_regclass('tmforum.entity') IS NOT NULL";
	private static final String REQUESTS_RESOURCE = "/replay/requests.jsonl";
	private static final String LOCALHOST = "http://localhost:";
	private static final String SERVER_PORT_PROPERTY = "micronaut.server.port";
	private static final String SERVER_HOST_PROPERTY = "general.serverHost";
	private static final String API_EXTENSION_PROPERTY = "apiExtension.enabled";
	private static final String JSON = "application/json;charset=utf-8";
	private static final int HTTP_OK = 200;
	private static final int HTTP_CREATED = 201;
	private static final int HTTP_NO_CONTENT = 204;
	private static final int HTTP_PARTIAL_CONTENT = 206;
	private static final int PAGE_SIZE = 100;
	private static final Duration NOTIFICATION_TIMEOUT = Duration.ofSeconds(30);

	private static final String PRODUCT_CATALOG = "/tmf-api/productCatalogManagement/v4";
	private static final String CATEGORY_PATH = PRODUCT_CATALOG + "/category";
	private static final String OFFERING_PATH = PRODUCT_CATALOG + "/productOffering";
	private static final String HUB_PATH = PRODUCT_CATALOG + "/hub";
	private static final String ORGANIZATION_PATH = "/tmf-api/party/v4/organization";
	private static final String NGSI_ENTITIES_PATH = "/ngsi-ld/v1/entities";
	// collection endpoints only: a GET by id is compared positionally, so a backfilled field shows up by name
	private static final String UNORDERED_PATHS = "/tmf-api/*/v4/*";

	private static final String RAW_ID_TEMPLATE = "urn:ngsi-ld:%s:rehearsal";
	private static final String RAW_NAME_TEMPLATE = "rehearsal-%s";
	private static final String RAW_VERSION = "1.0";
	private static final String PRE_88_OFFERING_ID = "urn:ngsi-ld:product-offering:rehearsal-pre-88";
	private static final String ESCAPED_PRODUCT_ID = "urn:ngsi-ld:product:rehearsal-escaped";
	private static final String NAME = "name";
	private static final String DESCRIPTION = "description";
	private static final String VERSION = "version";
	private static final String API_VERSION = RAW_VERSION;
	/** One raw record per API module, read back through {@code requests.jsonl} by the fixed id of RAW_ID_TEMPLATE. */
	private static final List<RawRecord> RAW_RECORDS = List.of(
			new RawRecord("billing-account", NAME, false),
			new RawRecord("agreement", NAME, true),
			new RawRecord("customer-bill", "billNo", false),
			new RawRecord("customer", NAME, false),
			new RawRecord("organization", NAME, false),
			new RawRecord("party-role", NAME, false),
			new RawRecord("product-order", DESCRIPTION, false),
			new RawRecord("quote", DESCRIPTION, true),
			new RawRecord("resource-specification", NAME, true),
			new RawRecord("resource-function", NAME, false),
			new RawRecord("resource", NAME, false),
			new RawRecord("resource-order", NAME, false),
			new RawRecord("service-specification", NAME, true),
			new RawRecord("service", NAME, false),
			new RawRecord("service-order", DESCRIPTION, false),
			new RawRecord("usage", DESCRIPTION, false),
			new RawRecord("document-specification", NAME, true));
	private static final List<String> TYPES = Stream.concat(RAW_RECORDS.stream().map(RawRecord::type),
			Stream.of("category", "product-offering", Product.TYPE_PRODUCT,
					TMForumSubscription.TYPE_TM_FORUM_SUBSCRIPTION)).toList();

	private static final String NOTIFY_MARKER = "rehearsal-notify";
	private static final String IMPORTED_CALLBACK = "/imported";
	private static final String CREATED_CALLBACK = "/created";
	// the payload name is lowerCamel, see TMForumEventHandler.makeEventDetails
	private static final String IMPORTED_HUB_QUERY = "eventType=CategoryCreateEvent&event.category.name=" + NOTIFY_MARKER;
	private static final String HUB_ENTITY_TYPE = "category";
	private static final String CREATED_HUB_QUERY = "eventType=CategoryCreateEvent";
	// the migration's own changes on the pre-88 offering: the seeded version and the backfilled category reference
	private static final Pattern MIGRATED_OFFERING_DIFFERENCE = Pattern.compile(
			"/version: broker <absent>, postgresql \"0\\.0\\.1\""
					+ "|/category/0/(href|name|version): broker <absent>, postgresql .+");
	private static final String BACKFILLED_NAME_DIFFERENCE = "/category/0/name";
	// the empty page of requests.jsonl: PostgreSQL still counts the matches, Orion-LD reports no total for it
	private static final String PAGE_PAST_END_PATH = ORGANIZATION_PATH + "?offset=100&limit=10";
	private static final Pattern PAGE_PAST_END_DIFFERENCE = Pattern.compile("status: broker 200, postgresql 206");
	private static final Map<String, Pattern> CUTOVER_EXPLAINED = Map.of(
			OFFERING_PATH + "/" + PRE_88_OFFERING_ID, MIGRATED_OFFERING_DIFFERENCE,
			PAGE_PAST_END_PATH, PAGE_PAST_END_DIFFERENCE);
	private static final Map<String, Pattern> ROLLBACK_EXPLAINED = Map.of(PAGE_PAST_END_PATH, PAGE_PAST_END_DIFFERENCE);
	private static final Set<Integer> ANSWERED = Set.of(HTTP_OK, HTTP_PARTIAL_CONTENT);

	private final HttpClient httpClient = HttpClient.newHttpClient();
	private final JsonMapper jsonMapper = JsonMapper.builder().build();
	private final BlockingQueue<Notification> notifications = new LinkedBlockingQueue<>();
	private final List<EmbeddedServer> servers = new ArrayList<>();
	private MigrationContexts brokerContexts;
	private BrokerSource broker;
	private HttpServer callbackServer;
	private URI notificationEndpoint;

	@TempDir
	Path reports;

	@AfterEach
	void cleanUp() throws SQLException {
		servers.forEach(MigrationRehearsalIT::stop);
		if (callbackServer != null) {
			callbackServer.stop(0);
		}
		// any of the fields may be unset when the test failed early, and that failure must not be masked
		try {
			if (broker != null && notificationEndpoint != null) {
				deleteBrokerSubscriptions();
			}
			if (broker != null) {
				deleteBrokerEntities();
			}
		} finally {
			if (brokerContexts != null) {
				brokerContexts.close();
			}
			try {
				clearTarget();
			} finally {
				ForbiddenCharacterEscaper.setEnabled(true);
			}
		}
	}

	@Test
	void migratesAndRollsBack() throws Exception {
		brokerContexts = new MigrationContexts();
		new CommandLine(brokerContexts).parseArgs(brokerTypeOption());
		brokerContexts.startSource();
		broker = new BrokerSource(brokerContexts.source(), EntityTypeRegistry.scan());
		URI callbackBase = startCallbackServer();
		int brokerPort = freePort();
		notificationEndpoint = NgsiLdSubscriptionFactory.notificationEndpoint(LOCALHOST + brokerPort, "/");
		deleteBrokerSubscriptions();
		deleteBrokerEntities();
		clearTarget();

		// 1. seed the broker, through the broker server's API and raw
		EmbeddedServer brokerServer = startServer(brokerPort, brokerEnvironment(), cacheEnvironment());
		URI brokerUrl = localUrl(brokerPort);
		// versioned like the raw records, so only the pre-88 offering gets the seeded version
		ObjectNode category = jsonMapper.createObjectNode().put(NAME, "rehearsal-category").put(VERSION, API_VERSION);
		JsonNode storedCategory = post(brokerUrl, CATEGORY_PATH, category);
		post(brokerUrl, OFFERING_PATH, jsonMapper.createObjectNode()
				.put(NAME, "rehearsal-offering")
				.put(VERSION, API_VERSION)
				.set("category", jsonMapper.createArrayNode().add(completeReference(storedCategory))));
		JsonNode organization = post(brokerUrl, ORGANIZATION_PATH,
				jsonMapper.createObjectNode().put(NAME, "api-organization"));
		URI importedHubId = createImportedHub(callbackBase.resolve(IMPORTED_CALLBACK));
		for (RawRecord record : RAW_RECORDS) {
			createRawEntity(record.json());
		}
		// written before ticket 88 and 93: an id-only reference and no version
		createRawEntity(String.format("""
						{"id": "%1$s", "type": "product-offering",
						 "name": {"type": "Property", "value": "pre-88 offering"},
						 "category": [{"type": "Relationship", "object": "%2$s", "datasetId": "%2$s"}]}""",
				PRE_88_OFFERING_ID, storedCategory.path("id").asString()));
		createEscapedProduct(brokerServer.getApplicationContext());

		// 2. import, backfill, verify and replay against the PostgreSQL server
		Run imported = run("import", "--backfill-references");
		assertEquals(CommandLine.ExitCode.OK, imported.exitCode(), imported.output());
		Run verified = run("verify");
		assertEquals(CommandLine.ExitCode.OK, verified.exitCode(), verified.output());
		int postgresPort = freePort();
		startServer(postgresPort, cacheEnvironment(), CommonConstants.POSTGRESQL_ENVIRONMENT);
		URI postgresUrl = localUrl(postgresPort);

		// 3. no diff but the migration's own
		JsonNode cutoverReplay = replay(brokerUrl, postgresUrl, "cutover");
		assertEquals(List.of(), unexplainedDifferences(cutoverReplay, CUTOVER_EXPLAINED), cutoverReplay.toString());
		assertEquals(List.of(), unansweredRequests(cutoverReplay), cutoverReplay.toString());
		assertTrue(differencesOf(cutoverReplay, OFFERING_PATH + "/" + PRE_88_OFFERING_ID).stream()
						.anyMatch(difference -> difference.startsWith(BACKFILLED_NAME_DIFFERENCE)),
				"The replay must show the backfilled reference: " + cutoverReplay);
		stop(brokerServer);

		// 4. write on PostgreSQL, then roll back into an emptied broker
		post(postgresUrl, CATEGORY_PATH, jsonMapper.createObjectNode().put(NAME, "postgresql-category"));
		patch(postgresUrl, ORGANIZATION_PATH + "/" + organization.path("id").asString(),
				jsonMapper.createObjectNode().put("tradingName", "patched-on-postgresql"));
		URI createdHubId = URI.create(post(postgresUrl, HUB_PATH,
				hub(callbackBase.resolve(CREATED_CALLBACK), CREATED_HUB_QUERY)).path("id").asString());
		deleteBrokerSubscriptions();
		deleteBrokerEntities();
		Run exported = run("export", "--notification-endpoint", notificationEndpoint.toString());
		assertEquals(CommandLine.ExitCode.OK, exported.exitCode(), exported.output());
		Run reverseVerified = run("verify", "--direction", "export");
		assertEquals(CommandLine.ExitCode.OK, reverseVerified.exitCode(), reverseVerified.output());
		assertEquals(Set.of(restoredSubscription(importedHubId), restoredSubscription(createdHubId)),
				notifyingSubscriptions(), "Each restored hub must own exactly one broker subscription to the endpoint.");
		startServer(brokerPort, brokerEnvironment(), cacheEnvironment());
		URI restoredUrl = localUrl(brokerPort);

		// 5. the restored broker answers as PostgreSQL does, and both hubs notify
		JsonNode rollbackReplay = replay(restoredUrl, postgresUrl, "rollback");
		assertEquals(List.of(), unexplainedDifferences(rollbackReplay, ROLLBACK_EXPLAINED), rollbackReplay.toString());
		assertEquals(List.of(), unansweredRequests(rollbackReplay), rollbackReplay.toString());
		notifications.clear();
		post(restoredUrl, CATEGORY_PATH, jsonMapper.createObjectNode().put(NAME, NOTIFY_MARKER));
		Set<String> expectedCallbacks = Set.of(IMPORTED_CALLBACK, CREATED_CALLBACK);
		assertEquals(expectedCallbacks, awaitNotifiedCallbacks(expectedCallbacks),
				"Both restored hubs must get the create notification from the restored broker server.");
	}

	private JsonNode replay(URI brokerUrl, URI postgresUrl, String name) throws IOException, URISyntaxException {
		Path report = reports.resolve(name + ".json");
		Path requests = Path.of(MigrationRehearsalIT.class.getResource(REQUESTS_RESOURCE).toURI());
		run(List.of("replay", "--requests", requests.toString(), "--broker-base-url", brokerUrl.toString(),
				"--postgres-base-url", postgresUrl.toString(), "--unordered-paths", UNORDERED_PATHS,
				"--report", report.toString()));
		return jsonMapper.readTree(report.toFile());
	}

	// every difference that its request's entry in explained does not match, plus failed requests
	private static List<String> unexplainedDifferences(JsonNode report, Map<String, Pattern> explained) {
		List<String> unexplained = new ArrayList<>();
		report.path("errors").values().forEach(error -> unexplained.add(error.asString()));
		for (JsonNode request : report.path("requests").values()) {
			String path = request.path("path").asString();
			String outcome = request.path("outcome").asString();
			if (outcome.equals(ReplayReport.Outcome.FAILED.name())) {
				unexplained.add(path + ": " + request.path("error").asString());
			}
			Pattern explanation = explained.get(path);
			request.path("differences").values().forEach(difference -> {
				if (explanation == null || !explanation.matcher(difference.asString()).matches()) {
					unexplained.add(path + " " + difference.asString());
				}
			});
		}
		return unexplained;
	}

	// equal on both sides is not enough: a path that fails on both backends would cover nothing; 206 is a partial page
	private static List<String> unansweredRequests(JsonNode report) {
		List<String> unanswered = new ArrayList<>();
		report.path("requests").values().forEach(request -> {
			if (!ANSWERED.contains(request.path("brokerStatus").asInt())) {
				unanswered.add(request.path("path").asString() + ": " + request.path("brokerStatus").asInt());
			}
		});
		return unanswered;
	}

	private static List<String> differencesOf(JsonNode report, String path) {
		List<String> differences = new ArrayList<>();
		report.path("requests").values().forEach(request -> {
			if (path.equals(request.path("path").asString())) {
				request.path("differences").values().forEach(difference -> differences.add(difference.asString()));
			}
		});
		return differences;
	}

	// carries every field the backfill could fill, so the API-created offering stays unchanged on PostgreSQL
	private ObjectNode completeReference(JsonNode target) {
		ObjectNode reference = jsonMapper.createObjectNode();
		Stream.of("id", "href", NAME, VERSION)
				.filter(field -> target.hasNonNull(field))
				.forEach(field -> reference.set(field, target.get(field)));
		return reference;
	}

	// seeded as the broker-backed hub controller would, without it: hub registration through the API fails on Scorpio
	private URI createImportedHub(URI callback) {
		SubscriptionQuery query = SubscriptionQueryParser.parse(IMPORTED_HUB_QUERY, List.of());
		List<String> entities = List.of(HUB_ENTITY_TYPE);
		TMForumSubscription hub = new TMForumSubscription(UUID.randomUUID().toString());
		hub.setRawQuery(IMPORTED_HUB_QUERY);
		hub.setEventTypes(query.getEventTypes());
		hub.setEntities(entities);
		hub.setQuery(query.getQuery());
		hub.setCallback(callback);
		hub.setFields(query.getFields());
		hub.setSubscription(NgsiLdSubscriptionFactory.build(notificationEndpoint, callback.toString(),
				query.getQuery(), entities, query.getFields()));
		ApplicationContext source = brokerContexts.source();
		GeneralProperties properties = source.getBean(GeneralProperties.class);
		source.getBean(SubscriptionsApiClient.class).createSubscription(
				source.getBean(SubscriptionMapper.class).map(hub.getSubscription())
						.atContext(properties.getContextUrl()),
				properties.getTenant()).block();
		source.getBean(EntityRepository.class).createDomainEntity(hub).block();
		return hub.getId();
	}

	private URI restoredSubscription(URI hubId) {
		TMForumSubscription hub = broker.toDomain(
				broker.entity(hubId).orElseThrow(() -> new AssertionError(hubId + " was not exported.")),
				TMForumSubscription.class);
		Subscription subscription = hub.getSubscription();
		assertTrue(subscription != null && subscription.getId() != null, hubId + " has no subscription.");
		return subscription.getId();
	}

	private Set<URI> notifyingSubscriptions() {
		return broker.subscriptions(PAGE_SIZE).stream()
				.filter(subscription -> notificationEndpoint.toString().equals(subscription.endpoint()))
				.map(SubscriptionStore.BrokerSubscription::id)
				.collect(Collectors.toSet());
	}

	private ObjectNode hub(URI callback, String query) {
		return jsonMapper.createObjectNode().put("callback", callback.toString()).put("query", query);
	}

	// its characteristic's object value has reserved keys, which the broker stores with the tmfEscaped- prefix
	private static void createEscapedProduct(ApplicationContext brokerServerContext) {
		Characteristic characteristic = new Characteristic();
		characteristic.setName("characteristic");
		characteristic.setValueType("object");
		characteristic.setTmfValue(Map.of("id", "nested-id", "valueType", "nested-value-type"));
		Product product = new Product(ESCAPED_PRODUCT_ID);
		product.setName("escaped-product");
		product.setProductCharacteristic(List.of(characteristic));
		brokerServerContext.getBean(EntityRepository.class).createDomainEntity(product).block();
	}

	private EmbeddedServer startServer(int port, String... environments) {
		Map<String, Object> properties = new HashMap<>();
		properties.put(SERVER_PORT_PROPERTY, port);
		properties.put(SERVER_HOST_PROPERTY, LOCALHOST + port);
		properties.put(API_EXTENSION_PROPERTY, false);
		EmbeddedServer server = ApplicationContext.builder()
				.deduceEnvironment(false)
				.deduceCloudEnvironment(false)
				.environments(environments)
				.properties(properties)
				.run(EmbeddedServer.class);
		servers.add(server);
		return server;
	}

	private static void stop(EmbeddedServer server) {
		ApplicationContext context = server.getApplicationContext();
		if (server.isRunning()) {
			server.stop();
		}
		if (context.isRunning()) {
			context.close();
		}
	}

	private URI startCallbackServer() throws IOException {
		callbackServer = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
		callbackServer.createContext("/", this::receiveNotification);
		callbackServer.start();
		return localUrl(callbackServer.getAddress().getPort());
	}

	private void receiveNotification(HttpExchange exchange) throws IOException {
		try (InputStream body = exchange.getRequestBody()) {
			notifications.add(new Notification(exchange.getRequestURI().getPath(),
					new String(body.readAllBytes(), StandardCharsets.UTF_8)));
		}
		exchange.sendResponseHeaders(HTTP_NO_CONTENT, -1);
		exchange.close();
	}

	private Set<String> awaitNotifiedCallbacks(Set<String> expected) throws InterruptedException {
		Set<String> notified = new HashSet<>();
		long deadline = System.nanoTime() + NOTIFICATION_TIMEOUT.toNanos();
		while (!notified.containsAll(expected)) {
			long remaining = deadline - System.nanoTime();
			Notification notification = remaining > 0 ? notifications.poll(remaining, TimeUnit.NANOSECONDS) : null;
			if (notification == null) {
				break;
			}
			if (notification.body().contains(NOTIFY_MARKER)) {
				notified.add(notification.path());
			}
		}
		return notified;
	}

	private JsonNode post(URI server, String path, JsonNode body) throws IOException, InterruptedException {
		return send(HttpRequest.newBuilder(server.resolve(path)).header("Content-Type", JSON)
				.POST(HttpRequest.BodyPublishers.ofString(body.toString())), HTTP_CREATED);
	}

	private void patch(URI server, String path, JsonNode body) throws IOException, InterruptedException {
		send(HttpRequest.newBuilder(server.resolve(path)).header("Content-Type", JSON)
				.method("PATCH", HttpRequest.BodyPublishers.ofString(body.toString())), HTTP_OK);
	}

	private JsonNode send(HttpRequest.Builder request, int expectedStatus) throws IOException, InterruptedException {
		HttpResponse<String> response = httpClient.send(request.header("Accept", JSON).build(),
				HttpResponse.BodyHandlers.ofString());
		assertEquals(expectedStatus, response.statusCode(), response.body());
		return jsonMapper.readTree(response.body());
	}

	private void createRawEntity(String json) throws IOException, InterruptedException {
		GeneralProperties properties = brokerContexts.source().getBean(GeneralProperties.class);
		String brokerUrl = brokerContexts.source().getRequiredProperty(MigrationContexts.NGSI_URL_PROPERTY, String.class);
		HttpRequest.Builder request = HttpRequest.newBuilder(URI.create(brokerUrl + NGSI_ENTITIES_PATH))
				.header("Content-Type", "application/json")
				.header("Link", String.format(CommonConstants.JSON_LD_CONTEXT_LINK_TEMPLATE, properties.getContextUrl()))
				.POST(HttpRequest.BodyPublishers.ofString(json));
		if (properties.getTenant() != null) {
			request.header("NGSILD-Tenant", properties.getTenant());
		}
		HttpResponse<String> response = httpClient.send(request.build(), HttpResponse.BodyHandlers.ofString());
		assertEquals(HTTP_CREATED, response.statusCode(), response.body());
	}

	private void deleteBrokerEntities() {
		NgsiLdTestEntityCleaner cleaner = new NgsiLdTestEntityCleaner(
				brokerContexts.source().getBean(EntitiesApiClient.class),
				brokerContexts.source().getBean(GeneralProperties.class));
		TYPES.forEach(cleaner::deleteAll);
	}

	private void deleteBrokerSubscriptions() {
		broker.subscriptions(PAGE_SIZE).stream()
				.filter(subscription -> notificationEndpoint.toString().equals(subscription.endpoint()))
				.forEach(subscription -> broker.deleteSubscription(subscription.id()));
	}

	private static void clearTarget() throws SQLException {
		try (Connection connection = DriverManager.getConnection(JDBC_URL, DB_CREDENTIAL, DB_CREDENTIAL);
			 PreparedStatement deleteType = connection.prepareStatement(DELETE_TYPE);
			 PreparedStatement deleteOutbox = connection.prepareStatement(DELETE_OUTBOX);
			 ResultSet tableExists = connection.createStatement().executeQuery(ENTITY_TABLE_EXISTS)) {
			// Flyway creates the schema with the first PostgreSQL context, which a fresh cluster has not seen yet
			if (!tableExists.next() || !tableExists.getBoolean(1)) {
				return;
			}
			for (String type : TYPES) {
				deleteType.setString(1, type);
				deleteType.executeUpdate();
			}
			deleteOutbox.executeUpdate();
		}
	}

	private Run run(String command, String... options) {
		List<String> args = new ArrayList<>(List.of(command, brokerTypeOption(), "--types", String.join(",", TYPES)));
		args.addAll(List.of(options));
		return run(args);
	}

	private static Run run(List<String> args) {
		StringWriter out = new StringWriter();
		StringWriter err = new StringWriter();
		int exitCode = new CommandLine(new Application())
				.setOut(new PrintWriter(out))
				.setErr(new PrintWriter(err))
				.execute(args.toArray(String[]::new));
		return new Run(exitCode, out + "\n" + err);
	}

	private static int freePort() throws IOException {
		try (ServerSocket socket = new ServerSocket(0)) {
			return socket.getLocalPort();
		}
	}

	private static URI localUrl(int port) {
		return URI.create(LOCALHOST + port);
	}

	private static String brokerEnvironment() {
		return System.getProperty(BROKER_PROPERTY, BrokerType.ORION_LD.getEnvironment());
	}

	private static String cacheEnvironment() {
		return System.getProperty(CACHE_PROPERTY, DEFAULT_CACHE);
	}

	private static String brokerTypeOption() {
		return "--broker-type=" + brokerEnvironment();
	}

	private record Run(int exitCode, String output) {
	}

	private record Notification(String path, String body) {
	}

	private record RawRecord(String type, String attribute, boolean versioned) {

		// versioned types get a version, so only the pre-88 offering shows the seeded one
		String json() {
			String version = versioned
					? String.format(", \"version\": {\"type\": \"Property\", \"value\": \"%s\"}", RAW_VERSION)
					: "";
			return String.format("{\"id\": \"%s\", \"type\": \"%s\", \"%s\": {\"type\": \"Property\", \"value\": \"%s\"}%s}",
					String.format(RAW_ID_TEMPLATE, type), type, attribute, String.format(RAW_NAME_TEMPLATE, type),
					version);
		}
	}
}
