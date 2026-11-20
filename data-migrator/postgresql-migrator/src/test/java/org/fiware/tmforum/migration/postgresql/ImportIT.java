package org.fiware.tmforum.migration.postgresql;

import org.fiware.ngsi.api.EntitiesApiClient;
import org.fiware.tmforum.common.CommonConstants;
import org.fiware.tmforum.common.configuration.GeneralProperties;
import org.fiware.tmforum.common.domain.subscription.TMForumSubscription;
import org.fiware.tmforum.common.mapping.ForbiddenCharacterEscaper;
import org.fiware.tmforum.common.test.BrokerOnly;
import org.fiware.tmforum.common.test.NgsiLdTestEntityCleaner;
import org.fiware.tmforum.product.Characteristic;
import org.fiware.tmforum.product.Product;
import org.fiware.tmforum.product.ProductOffering;
import org.fiware.tmforum.product.ProductSpecification;
import org.fiware.tmforum.storage.EntityRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import picocli.CommandLine;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Runs {@code import} from the test cluster's broker into its PostgreSQL (k3s/postgresql-db, the {@code POSTGRES_*}
 * defaults); the broker is seeded like the application writes it, plus raw pre-ticket-88 entities.
 */
@BrokerOnly
class ImportIT {

	private static final String JDBC_URL = "jdbc:postgresql://localhost:5432/tmforum";
	private static final String DB_CREDENTIAL = "tmforum";
	private static final String SELECT_ROW =
			"SELECT version, revision, is_current, data::text, created_at, updated_at FROM tmforum.entity WHERE id = ?";
	private static final String DELETE_TYPE = "DELETE FROM tmforum.entity WHERE type = ?";
	private static final String DELETE_OUTBOX = "DELETE FROM tmforum.event_outbox";
	private static final String COUNT_OUTBOX = "SELECT count(*) FROM tmforum.event_outbox";
	private static final String ESCAPE_PREFIX = "tmfEscaped-";
	private static final String SEEDED_VERSION = "0.0.1";
	private static final String GIVEN_VERSION = "1.2";
	private static final String NESTED_ID = "nested-id";
	private static final String NESTED_VALUE_TYPE = "nested-value-type";
	private static final URI HUB_CALLBACK = URI.create("http://hub.example.org/listener");
	private static final String HUB_EVENT_TYPE = "ProductCreateEvent";
	private static final String ENTITIES_PATH = "/ngsi-ld/v1/entities";
	private static final String SYS_ATTRS_QUERY = "?options=sysAttrs";
	private static final String LD_JSON = "application/json";
	private static final int HTTP_CREATED = 201;
	private static final List<String> IMPORTED_TYPES = List.of(Product.TYPE_PRODUCT,
			ProductOffering.TYPE_PRODUCT_OFFERING, ProductSpecification.TYPE_PRODUCT_SPECIFICATION,
			TMForumSubscription.TYPE_TM_FORUM_SUBSCRIPTION);

	private final HttpClient httpClient = HttpClient.newHttpClient();
	private final JsonMapper jsonMapper = JsonMapper.builder().build();
	private MigrationContexts seedContexts;

	@AfterEach
	void cleanUp() throws SQLException {
		if (seedContexts != null) {
			deleteSeededBrokerEntities();
			seedContexts.close();
		}
		clearTarget();
		// a later IT in this JVM must not seed hubs unescaped
		ForbiddenCharacterEscaper.setEnabled(true);
	}

	static Stream<Arguments> hubQueries() {
		return Stream.of(
				Arguments.of(true, "state=active;name=('it''s')", "$filter=state eq 'active';(x)"),
				Arguments.of(true, "price=10%+tax", "a%3Db+c"),
				Arguments.of(false, "price=10%+tax%3D", "a%3Db+c%25"));
	}

	@ParameterizedTest(name = "source-escaped={0}")
	@MethodSource("hubQueries")
	void importsHubQueriesUnchanged(boolean sourceEscaped, String query, String rawQuery) throws Exception {
		startSeedContexts(sourceEscaped);
		TMForumSubscription hub = new TMForumSubscription(urn(TMForumSubscription.TYPE_TM_FORUM_SUBSCRIPTION).toString());
		hub.setCallback(HUB_CALLBACK);
		hub.setEventTypes(List.of(HUB_EVENT_TYPE));
		hub.setEntities(List.of(Product.TYPE_PRODUCT));
		hub.setQuery(query);
		hub.setRawQuery(rawQuery);
		repository().createDomainEntity(hub).block();
		assertEquals(ForbiddenCharacterEscaper.escape(query),
				brokerEntity(hub.getId()).path("query").path("value").asString(),
				"The broker was not seeded in the expected representation.");

		runImport(sourceEscaped, TMForumSubscription.TYPE_TM_FORUM_SUBSCRIPTION);

		Row row = row(hub.getId());
		assertEquals(query, row.data().path("query").asString());
		assertEquals(rawQuery, row.data().path("rawQuery").asString());
		assertEquals("", row.version(), "An unversioned type keeps the column default.");
		assertEquals(0, outboxCount(), "The import must not fire events.");
	}

	@Test
	void importsEntitiesWithVersionsTimestampsAndPlainKeys() throws Exception {
		startSeedContexts(true);
		ProductSpecification specification = new ProductSpecification(urn(ProductSpecification.TYPE_PRODUCT_SPECIFICATION).toString());
		specification.setName("specification");
		specification.setVersion(GIVEN_VERSION);
		repository().createDomainEntity(specification).block();

		Characteristic characteristic = new Characteristic();
		characteristic.setName("characteristic");
		characteristic.setValueType("object");
		characteristic.setTmfValue(Map.of("id", NESTED_ID, "valueType", NESTED_VALUE_TYPE));
		Product product = new Product(urn(Product.TYPE_PRODUCT).toString());
		product.setName("product");
		product.setProductCharacteristic(List.of(characteristic));
		repository().createDomainEntity(product).block();
		assertTrue(brokerEntity(product.getId()).toString().contains(ESCAPE_PREFIX),
				"The seed must carry escaped reserved keys.");

		// written before ticket 88: the relationship carries the target id only, and the offering has no version
		URI offeringId = urn(ProductOffering.TYPE_PRODUCT_OFFERING);
		createRawEntity(String.format("""
				{"id": "%s", "type": "%s",
				 "name": {"type": "Property", "value": "legacy offering"},
				 "productSpecification": {"type": "Relationship", "object": "%s"}}""",
				offeringId, ProductOffering.TYPE_PRODUCT_OFFERING, specification.getId()));

		runImport(true, Product.TYPE_PRODUCT, ProductOffering.TYPE_PRODUCT_OFFERING,
				ProductSpecification.TYPE_PRODUCT_SPECIFICATION);

		for (URI id : List.of(specification.getId(), product.getId(), offeringId)) {
			Row row = row(id);
			JsonNode broker = brokerEntity(id);
			assertEquals(Instant.parse(broker.path("createdAt").asString()), row.createdAt(), id.toString());
			assertEquals(Instant.parse(broker.path("modifiedAt").asString()), row.updatedAt(), id.toString());
			assertEquals(1, row.revision(), id.toString());
			assertTrue(row.current(), id.toString());
			assertFalse(row.data().toString().contains(ESCAPE_PREFIX), id + " kept an escaped key.");
		}

		Row storedSpecification = row(specification.getId());
		assertEquals(GIVEN_VERSION, storedSpecification.version());
		assertEquals(GIVEN_VERSION, storedSpecification.data().path("version").asString());

		Row storedOffering = row(offeringId);
		assertEquals(SEEDED_VERSION, storedOffering.version());
		assertEquals(SEEDED_VERSION, storedOffering.data().path("version").asString());
		assertEquals(Set.of("id"), fieldNames(storedOffering.data().path("productSpecification")),
				"An id-only reference must stay id-only.");
		assertEquals(specification.getId().toString(),
				storedOffering.data().path("productSpecification").path("id").asString());

		Row storedProduct = row(product.getId());
		assertEquals("", storedProduct.version());
		JsonNode storedCharacteristic = storedProduct.data().path("productCharacteristic").path(0);
		assertEquals("object", storedCharacteristic.path("valueType").asString());
		// the domain field name, as the application writes it to PostgreSQL
		JsonNode nested = storedCharacteristic.path("tmfValue");
		assertEquals(NESTED_ID, nested.path("id").asString(), storedProduct.data().toString());
		assertEquals(NESTED_VALUE_TYPE, nested.path("valueType").asString(), storedProduct.data().toString());

		assertEquals(0, outboxCount(), "The import must not fire events.");
	}

	private void startSeedContexts(boolean sourceEscaped) throws SQLException {
		seedContexts = new MigrationContexts();
		new CommandLine(seedContexts).parseArgs(sourceEscapedOption(sourceEscaped));
		seedContexts.start();
		deleteSeededBrokerEntities();
		clearTarget();
	}

	private void deleteSeededBrokerEntities() {
		NgsiLdTestEntityCleaner cleaner = new NgsiLdTestEntityCleaner(
				seedContexts.source().getBean(EntitiesApiClient.class),
				seedContexts.source().getBean(GeneralProperties.class));
		IMPORTED_TYPES.forEach(cleaner::deleteAll);
	}

	private EntityRepository repository() {
		return seedContexts.source().getBean(EntityRepository.class);
	}

	private void runImport(boolean sourceEscaped, String... types) {
		String[] args = {"import", sourceEscapedOption(sourceEscaped), "--types", String.join(",", types)};
		StringWriter out = new StringWriter();
		StringWriter err = new StringWriter();
		int exitCode = new CommandLine(new Application())
				.setOut(new PrintWriter(out))
				.setErr(new PrintWriter(err))
				.execute(args);
		assertEquals(CommandLine.ExitCode.OK, exitCode, out + "\n" + err);
		for (String type : types) {
			assertTrue(out.toString().contains(type), "The report misses " + type + ":\n" + out);
		}
	}

	private void createRawEntity(String json) throws IOException, InterruptedException {
		HttpResponse<String> response = httpClient.send(brokerRequest(ENTITIES_PATH)
						.header("Content-Type", LD_JSON)
						.POST(HttpRequest.BodyPublishers.ofString(json))
						.build(),
				HttpResponse.BodyHandlers.ofString());
		assertEquals(HTTP_CREATED, response.statusCode(), response.body());
	}

	private JsonNode brokerEntity(URI id) throws IOException, InterruptedException {
		HttpResponse<String> response = httpClient.send(
				brokerRequest(ENTITIES_PATH + "/" + id + SYS_ATTRS_QUERY).GET().build(),
				HttpResponse.BodyHandlers.ofString());
		return jsonMapper.readTree(response.body());
	}

	private HttpRequest.Builder brokerRequest(String path) {
		GeneralProperties properties = seedContexts.source().getBean(GeneralProperties.class);
		String brokerUrl = seedContexts.source().getRequiredProperty(MigrationContexts.NGSI_URL_PROPERTY, String.class);
		return HttpRequest.newBuilder(URI.create(brokerUrl + path))
				.header("Accept", LD_JSON)
				.header("Link", String.format(CommonConstants.JSON_LD_CONTEXT_LINK_TEMPLATE, properties.getContextUrl()));
	}

	private Row row(URI id) throws SQLException {
		try (Connection connection = connection();
			 PreparedStatement statement = connection.prepareStatement(SELECT_ROW)) {
			statement.setString(1, id.toString());
			try (ResultSet result = statement.executeQuery()) {
				assertTrue(result.next(), id + " was not imported.");
				Row row = new Row(result.getString(1), result.getLong(2), result.getBoolean(3),
						jsonMapper.readTree(result.getString(4)),
						result.getObject(5, OffsetDateTime.class).toInstant(),
						result.getObject(6, OffsetDateTime.class).toInstant());
				assertFalse(result.next(), id + " has more than one row.");
				return row;
			}
		}
	}

	private long outboxCount() throws SQLException {
		try (Connection connection = connection();
			 Statement statement = connection.createStatement();
			 ResultSet result = statement.executeQuery(COUNT_OUTBOX)) {
			result.next();
			return result.getLong(1);
		}
	}

	private void clearTarget() throws SQLException {
		try (Connection connection = connection();
			 PreparedStatement deleteType = connection.prepareStatement(DELETE_TYPE);
			 Statement deleteOutbox = connection.createStatement()) {
			for (String type : IMPORTED_TYPES) {
				deleteType.setString(1, type);
				deleteType.executeUpdate();
			}
			deleteOutbox.executeUpdate(DELETE_OUTBOX);
		}
	}

	private static Connection connection() throws SQLException {
		return DriverManager.getConnection(JDBC_URL, DB_CREDENTIAL, DB_CREDENTIAL);
	}

	private static Set<String> fieldNames(JsonNode node) {
		return new TreeSet<>(node.propertyNames());
	}

	private static String sourceEscapedOption(boolean sourceEscaped) {
		return "--source-escaped=" + sourceEscaped;
	}

	private static URI urn(String type) {
		return URI.create(String.format("urn:ngsi-ld:%s:%s", type, UUID.randomUUID()));
	}

	private record Row(String version, long revision, boolean current, JsonNode data, Instant createdAt,
					   Instant updatedAt) {
	}
}
