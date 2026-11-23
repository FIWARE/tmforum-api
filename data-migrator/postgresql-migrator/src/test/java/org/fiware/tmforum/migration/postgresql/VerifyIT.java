package org.fiware.tmforum.migration.postgresql;

import org.fiware.ngsi.api.EntitiesApiClient;
import org.fiware.tmforum.common.CommonConstants;
import org.fiware.tmforum.common.configuration.GeneralProperties;
import org.fiware.tmforum.common.domain.subscription.TMForumSubscription;
import org.fiware.tmforum.common.mapping.ForbiddenCharacterEscaper;
import org.fiware.tmforum.common.test.BrokerOnly;
import org.fiware.tmforum.common.test.NgsiLdTestEntityCleaner;
import org.fiware.tmforum.product.BundleProductSpecification;
import org.fiware.tmforum.product.Characteristic;
import org.fiware.tmforum.product.Product;
import org.fiware.tmforum.product.ProductOffering;
import org.fiware.tmforum.product.ProductSpecification;
import org.fiware.tmforum.storage.EntityRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
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
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Runs {@code import} and then {@code verify} against the test cluster's broker and PostgreSQL, before and after a
 * manual corruption of the imported rows.
 */
@BrokerOnly
class VerifyIT {

	private static final String JDBC_URL = "jdbc:postgresql://localhost:5432/tmforum";
	private static final String DB_CREDENTIAL = "tmforum";
	/** Set by the failsafe configuration from the build's {@code -Dbroker}. */
	private static final String BROKER_PROPERTY = "broker";
	private static final String DELETE_TYPE = "DELETE FROM tmforum.entity WHERE type = ?";
	private static final String DELETE_OUTBOX = "DELETE FROM tmforum.event_outbox";
	// a filter on a URL-encoded href: the literal %3D must come back as %3D, not as '='
	private static final String HUB_QUERY = "href==\"https://example.org/products?id%3D1\"";
	private static final String HUB_RAW_QUERY = "$filter=href eq 'https://example.org/products?id%3D1'";
	private static final URI HUB_CALLBACK = URI.create("http://hub.example.org/listener");
	private static final String HUB_EVENT_TYPE = "ProductCreateEvent";
	private static final String ENTITIES_PATH = "/ngsi-ld/v1/entities";
	private static final String JSON = "application/json";
	private static final int HTTP_CREATED = 201;
	private static final String SPECIFICATION_NAME = "specification";
	// a catalog entry (product-specification#bundledProductSpecification) whose name the backfill fills
	private static final String BACKFILLED_PATH = "bundledProductSpecification[0].name";
	private static final List<String> TYPES = List.of(Product.TYPE_PRODUCT, ProductOffering.TYPE_PRODUCT_OFFERING,
			ProductSpecification.TYPE_PRODUCT_SPECIFICATION, TMForumSubscription.TYPE_TM_FORUM_SUBSCRIPTION);

	private final HttpClient httpClient = HttpClient.newHttpClient();
	private final JsonMapper jsonMapper = JsonMapper.builder().build();
	private MigrationContexts seedContexts;
	private URI productId;
	private URI hubId;

	@TempDir
	Path reportDirectory;

	@BeforeEach
	void seedAndImport() throws Exception {
		seedContexts = new MigrationContexts();
		new CommandLine(seedContexts).parseArgs(brokerTypeOption());
		seedContexts.start();
		deleteSeededBrokerEntities();
		clearTarget();
		EntityRepository repository = seedContexts.source().getBean(EntityRepository.class);

		ProductSpecification specification = new ProductSpecification(urn(ProductSpecification.TYPE_PRODUCT_SPECIFICATION));
		specification.setName(SPECIFICATION_NAME);
		specification.setVersion("1.0");
		repository.createDomainEntity(specification).block();

		// an id-only reference in a catalog attribute, so import --backfill-references fills its name
		ProductSpecification bundle = new ProductSpecification(urn(ProductSpecification.TYPE_PRODUCT_SPECIFICATION));
		bundle.setName("bundle");
		bundle.setVersion("1.0");
		bundle.setBundledProductSpecification(List.of(new BundleProductSpecification(specification.getId().toString())));
		repository.createDomainEntity(bundle).block();

		Characteristic characteristic = new Characteristic();
		characteristic.setName("characteristic");
		characteristic.setValueType("object");
		characteristic.setTmfValue(Map.of("id", "nested-id", "valueType", "nested-value-type"));
		Product product = new Product(urn(Product.TYPE_PRODUCT));
		product.setName("product");
		product.setProductCharacteristic(List.of(characteristic));
		repository.createDomainEntity(product).block();
		productId = product.getId();

		TMForumSubscription hub = new TMForumSubscription(urn(TMForumSubscription.TYPE_TM_FORUM_SUBSCRIPTION));
		hub.setCallback(HUB_CALLBACK);
		hub.setEventTypes(List.of(HUB_EVENT_TYPE));
		hub.setEntities(List.of(Product.TYPE_PRODUCT));
		hub.setQuery(HUB_QUERY);
		hub.setRawQuery(HUB_RAW_QUERY);
		repository.createDomainEntity(hub).block();
		hubId = hub.getId();

		// written before ticket 88: an id-only reference and no version, so the import seeds one
		createRawEntity(String.format("""
				{"id": "%s", "type": "%s",
				 "name": {"type": "Property", "value": "legacy offering"},
				 "productSpecification": {"type": "Relationship", "object": "%s"}}""",
				urn(ProductOffering.TYPE_PRODUCT_OFFERING), ProductOffering.TYPE_PRODUCT_OFFERING,
				specification.getId()));

		// the runbook's sequence: verify runs after the reference backfill
		assertEquals(CommandLine.ExitCode.OK, run("import", "--backfill-references").exitCode(),
				"The import with the reference backfill must succeed.");
	}

	@AfterEach
	void cleanUp() throws SQLException {
		if (seedContexts != null) {
			deleteSeededBrokerEntities();
			seedContexts.close();
		}
		clearTarget();
		ForbiddenCharacterEscaper.setEnabled(true);
	}

	@Test
	void passesAfterAnImport() {
		Path reportFile = reportDirectory.resolve("verify.json");

		Run verify = run("verify", "--report", reportFile.toString());

		assertEquals(CommandLine.ExitCode.OK, verify.exitCode(), verify.output());
		JsonNode report = jsonMapper.readTree(reportFile.toFile());
		assertTrue(report.path("succeeded").asBoolean(), report.toString());
		assertEquals(1, report.path("types").path(ProductOffering.TYPE_PRODUCT_OFFERING).path("allowedDifferences")
				.path(DocumentDiff.Kind.SEEDED_VERSION.name()).path("count").asLong(), report.toString());
		JsonNode backfilled = report.path("types").path(ProductSpecification.TYPE_PRODUCT_SPECIFICATION)
				.path("allowedDifferences").path(DocumentDiff.Kind.BACKFILLED_REFERENCE_FIELD.name());
		assertEquals(1, backfilled.path("count").asLong(), report.toString());
		assertTrue(backfilled.path("examples").get(0).asString().contains(BACKFILLED_PATH + ": \"" + SPECIFICATION_NAME),
				report.toString());
		for (String type : TYPES) {
			JsonNode typeReport = report.path("types").path(type);
			long expected = type.equals(ProductSpecification.TYPE_PRODUCT_SPECIFICATION) ? 2 : 1;
			assertEquals(expected, typeReport.path("brokerCount").asLong(), type);
			assertEquals(expected, typeReport.path("diffChecked").asLong(), type);
		}
	}

	static Stream<Arguments> corruptions() {
		return Stream.of(
				Arguments.of("deleted row", false, "DELETE FROM tmforum.entity WHERE id = ?", null,
						"count mismatch"),
				Arguments.of("changed field", false,
						"UPDATE tmforum.entity SET data = jsonb_set(data, '{name}', '\"corrupted\"') WHERE id = ?", null,
						"field differences"),
				Arguments.of("removed top-level attribute", false,
						"UPDATE tmforum.entity SET data = data - 'name' WHERE id = ?", null,
						"attribute 'name' missing in the target"),
				Arguments.of("re-escaped query", true,
						"UPDATE tmforum.entity SET data = jsonb_set(data, '{query}', to_jsonb(?::text)) WHERE id = ?",
						URLEncoder.encode(HUB_QUERY, StandardCharsets.UTF_8),
						"escape check: 1 stored hub queries differ"),
				Arguments.of("tmfEscaped- key", false, "UPDATE tmforum.entity SET data = data || "
								+ "'{\"extension\": {\"tmfEscaped-valueType\": \"x\"}}'::jsonb WHERE id = ?", null,
						"escape check: 1 rows hold a tmfEscaped- key"));
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("corruptions")
	void failsNamingTheCause(String description, boolean onHub, String sql, String value, String cause)
			throws SQLException {
		try (Connection connection = connection(); PreparedStatement statement = connection.prepareStatement(sql)) {
			int index = 1;
			if (value != null) {
				statement.setString(index++, value);
			}
			statement.setString(index, (onHub ? hubId : productId).toString());
			assertEquals(1, statement.executeUpdate(), description);
		}

		Run verify = run("verify");

		assertEquals(CommandLine.ExitCode.SOFTWARE, verify.exitCode(), verify.output());
		assertTrue(verify.output().contains(cause), cause + " not in:\n" + verify.output());
	}

	private Run run(String command, String... options) {
		List<String> args = new java.util.ArrayList<>(List.of(command, brokerTypeOption(), "--types",
				String.join(",", TYPES)));
		args.addAll(List.of(options));
		StringWriter out = new StringWriter();
		StringWriter err = new StringWriter();
		int exitCode = new CommandLine(new Application())
				.setOut(new PrintWriter(out))
				.setErr(new PrintWriter(err))
				.execute(args.toArray(String[]::new));
		return new Run(exitCode, out + "\n" + err);
	}

	private void deleteSeededBrokerEntities() {
		NgsiLdTestEntityCleaner cleaner = new NgsiLdTestEntityCleaner(
				seedContexts.source().getBean(EntitiesApiClient.class),
				seedContexts.source().getBean(GeneralProperties.class));
		TYPES.forEach(cleaner::deleteAll);
	}

	private void createRawEntity(String json) throws IOException, InterruptedException {
		GeneralProperties properties = seedContexts.source().getBean(GeneralProperties.class);
		String brokerUrl = seedContexts.source().getRequiredProperty(MigrationContexts.NGSI_URL_PROPERTY, String.class);
		HttpResponse<String> response = httpClient.send(HttpRequest.newBuilder(URI.create(brokerUrl + ENTITIES_PATH))
						.header("Content-Type", JSON)
						.header("Link", String.format(CommonConstants.JSON_LD_CONTEXT_LINK_TEMPLATE,
								properties.getContextUrl()))
						.POST(HttpRequest.BodyPublishers.ofString(json))
						.build(),
				HttpResponse.BodyHandlers.ofString());
		assertEquals(HTTP_CREATED, response.statusCode(), response.body());
	}

	private static void clearTarget() throws SQLException {
		try (Connection connection = connection();
			 PreparedStatement deleteType = connection.prepareStatement(DELETE_TYPE);
			 PreparedStatement deleteOutbox = connection.prepareStatement(DELETE_OUTBOX)) {
			for (String type : TYPES) {
				deleteType.setString(1, type);
				deleteType.executeUpdate();
			}
			deleteOutbox.executeUpdate();
		}
	}

	private static Connection connection() throws SQLException {
		return DriverManager.getConnection(JDBC_URL, DB_CREDENTIAL, DB_CREDENTIAL);
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
