package org.fiware.tmforum.migration.postgresql;

import io.r2dbc.spi.ConnectionFactory;
import org.fiware.tmforum.common.mapping.ForbiddenCharacterEscaper;
import org.fiware.tmforum.migration.postgresql.ReferenceBackfillReport.AttributeResult;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import picocli.CommandLine;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Runs the PostgreSQL reference backfill with the real catalog against the test cluster's PostgreSQL
 * (k3s/postgresql-db, the {@code POSTGRES_*} defaults). It needs no broker, so it runs on every {@code -Dbroker}.
 */
class PostgresReferenceBackfillIT {

	private static final String JDBC_URL = "jdbc:postgresql://localhost:5432/tmforum";
	private static final String DB_CREDENTIAL = "tmforum";
	private static final String OFFERING_TYPE = "product-offering";
	private static final String RESOURCE_CANDIDATE_TYPE = "resource-candidate";
	private static final String CATEGORY_TYPE = "category";
	private static final String SPECIFICATION_TYPE = "product-specification";
	private static final String[] SEEDED_TYPES = {OFFERING_TYPE, RESOURCE_CANDIDATE_TYPE, CATEGORY_TYPE,
			SPECIFICATION_TYPE};
	private static final String SOURCE_ESCAPED_OPTION = "--source-escaped=";
	private static final String SINGLE_LABEL = OFFERING_TYPE + ".resourceCandidate";
	private static final String LIST_LABEL = OFFERING_TYPE + ".category";
	// long before the run, so a touched updated_at shows
	private static final OffsetDateTime SEEDED_AT = OffsetDateTime.of(2020, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC);

	private static final String INSERT = "INSERT INTO tmforum.entity (id, type, data, created_at, updated_at) "
			+ "VALUES (?, ?, ?::jsonb, ?, ?)";
	private static final String SELECT_ROW = "SELECT data::text, updated_at FROM tmforum.entity WHERE id = ?";
	private static final String DELETE_TYPES = "DELETE FROM tmforum.entity WHERE type = ANY(?)";
	private static final String DELETE_OUTBOX = "DELETE FROM tmforum.event_outbox";
	private static final String COUNT_OUTBOX = "SELECT count(*) FROM tmforum.event_outbox";
	private static final String FILL_REF = "SELECT pg_temp.fill_ref(?::jsonb, ?::jsonb, ?, ?, ?)::text";

	private static final String OFFERING = urn(OFFERING_TYPE);
	private static final String RESOURCE_CANDIDATE = urn(RESOURCE_CANDIDATE_TYPE);
	private static final String CATEGORY_ONE = urn(CATEGORY_TYPE);
	private static final String CATEGORY_TWO = urn(CATEGORY_TYPE);
	private static final String SPECIFICATION = urn(SPECIFICATION_TYPE);
	private static final String MISSING = urn(CATEGORY_TYPE);
	private static final Map<String, String> PLACEHOLDERS = Map.of("{O}", OFFERING, "{RC}", RESOURCE_CANDIDATE,
			"{C1}", CATEGORY_ONE, "{C2}", CATEGORY_TWO, "{PS}", SPECIFICATION, "{MISSING}", MISSING);

	// description is no fillable field and a null href is never copied
	private static final Map<String, String> TARGETS = Map.of(
			RESOURCE_CANDIDATE, """
					{"id": "{RC}", "name": "candidate", "href": "http://rc.example.org", "version": "1.0",
					 "description": "not fillable"}""",
			CATEGORY_ONE, """
					{"id": "{C1}", "name": "category one", "href": null, "version": "2.0"}""",
			CATEGORY_TWO, """
					{"id": "{C2}", "name": "category two", "href": "http://c2.example.org", "version": "3.0"}""",
			SPECIFICATION, """
					{"id": "{PS}", "name": "specification", "href": "http://ps.example.org"}""");
	private static final Map<String, String> TARGET_TYPES = Map.of(RESOURCE_CANDIDATE, RESOURCE_CANDIDATE_TYPE,
			CATEGORY_ONE, CATEGORY_TYPE, CATEGORY_TWO, CATEGORY_TYPE, SPECIFICATION, SPECIFICATION_TYPE);

	private static final JsonMapper JSON_MAPPER = JsonMapper.builder().build();
	private static MigrationContexts contexts;
	private static PostgresReferenceBackfill backfill;

	/**
	 * One owner document before and after the backfill; {@code label} is the catalog entry it exercises, if any.
	 */
	record Case(String name, String owner, String expected, String label, long filled, long missing, long changed) {

		@Override
		public String toString() {
			return name;
		}
	}

	static Stream<Case> cases() {
		return Stream.of(
				new Case("absent fields are filled",
						"""
								{"id": "{O}", "resourceCandidate": {"id": "{RC}"}}""",
						"""
								{"id": "{O}", "resourceCandidate": {"id": "{RC}", "name": "candidate",
								 "href": "http://rc.example.org", "version": "1.0"}}""",
						SINGLE_LABEL, 1, 0, 1),
				new Case("a client-sent name is kept",
						"""
								{"id": "{O}", "resourceCandidate": {"id": "{RC}", "name": "client name"}}""",
						"""
								{"id": "{O}", "resourceCandidate": {"id": "{RC}", "name": "client name",
								 "href": "http://rc.example.org", "version": "1.0"}}""",
						SINGLE_LABEL, 1, 0, 1),
				new Case("a missing target leaves the reference unchanged",
						"""
								{"id": "{O}", "resourceCandidate": {"id": "{MISSING}"}}""",
						"""
								{"id": "{O}", "resourceCandidate": {"id": "{MISSING}"}}""",
						SINGLE_LABEL, 0, 1, 0),
				new Case("a list keeps its order and fills mixed entries",
						"""
								{"id": "{O}", "category": [{"id": "{C2}"}, {"id": "{MISSING}"},
								 {"id": "{C1}", "name": "client name"}]}""",
						"""
								{"id": "{O}", "category": [
								 {"id": "{C2}", "name": "category two", "href": "http://c2.example.org", "version": "3.0"},
								 {"id": "{MISSING}"},
								 {"id": "{C1}", "name": "client name", "version": "2.0"}]}""",
						LIST_LABEL, 2, 1, 1),
				new Case("a non-catalog attribute is untouched",
						"""
								{"id": "{O}", "productSpecification": {"id": "{PS}"}}""",
						"""
								{"id": "{O}", "productSpecification": {"id": "{PS}"}}""",
						null, 0, 0, 0));
	}

	@BeforeAll
	static void startTarget() {
		contexts = new MigrationContexts();
		new CommandLine(contexts).parseArgs();
		// runs Flyway, so the schema exists before the first seed
		contexts.startTarget();
		backfill = new PostgresReferenceBackfill(contexts.target().getBean(ConnectionFactory.class),
				ReferenceBackfillCatalog.load());
	}

	@AfterAll
	static void stopTarget() {
		contexts.close();
	}

	@BeforeEach
	@AfterEach
	void clear() throws SQLException {
		// the flag is JVM-wide, a false case must not leak into later tests
		ForbiddenCharacterEscaper.setEnabled(true);
		try (Connection connection = connection();
			 PreparedStatement deleteTypes = connection.prepareStatement(DELETE_TYPES);
			 Statement deleteOutbox = connection.createStatement()) {
			deleteTypes.setArray(1, connection.createArrayOf("text", SEEDED_TYPES));
			deleteTypes.executeUpdate();
			deleteOutbox.executeUpdate(DELETE_OUTBOX);
		}
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("cases")
	void fillsAbsentFieldsOnly(Case testCase) throws SQLException {
		seed(testCase.owner());

		ReferenceBackfillReport dryRun = backfill.run(true);
		assertTrue(dryRun.succeeded(), report(dryRun));
		assertEquals(json(testCase.owner()), row(OFFERING).data(), "A dry run must not write.");

		ReferenceBackfillReport run = backfill.run(false);
		assertTrue(run.succeeded(), report(run));
		Row filled = row(OFFERING);
		assertEquals(json(testCase.expected()), filled.data());
		assertEquals(SEEDED_AT.toInstant(), filled.updatedAt(), "The backfill is no client write.");
		assertEquals(0, outboxCount(), "The backfill must not fire events.");
		for (String target : TARGETS.keySet()) {
			assertEquals(json(TARGETS.get(target)), row(target).data(), "A target must stay unchanged.");
		}

		if (testCase.label() != null) {
			AttributeResult expected = new AttributeResult(testCase.label(), testCase.filled(), testCase.missing(),
					testCase.changed());
			assertEquals(expected, result(run, testCase.label()));
			assertEquals(expected, result(dryRun, testCase.label()), "A dry run must report what a run changes.");
		}

		ReferenceBackfillReport rerun = backfill.run(false);
		assertTrue(rerun.succeeded(), report(rerun));
		assertEquals(filled, row(OFFERING), "A re-run must be a no-op.");
		if (testCase.label() != null) {
			assertEquals(new AttributeResult(testCase.label(), 0, testCase.missing(), 0),
					result(rerun, testCase.label()), "A re-run must be a no-op.");
		}
	}

	// --source-escaped describes broker data; the PostgreSQL-only backfill must accept either value
	@ParameterizedTest(name = "source escaped={0}")
	@ValueSource(booleans = {true, false})
	void backfillCommandRunsAgainstTheDatabase(boolean sourceEscaped) throws SQLException {
		String owner = """
				{"id": "{O}", "resourceCandidate": {"id": "{RC}"}}""";
		seed(owner);
		String escapedOption = SOURCE_ESCAPED_OPTION + sourceEscaped;

		String dryRunOut = runCommand(escapedOption, "--dry-run");
		assertTrue(dryRunOut.contains(SINGLE_LABEL), dryRunOut);
		assertEquals(json(owner), row(OFFERING).data(), "A dry run must not write.");

		runCommand(escapedOption);
		assertEquals("candidate", row(OFFERING).data().path("resourceCandidate").path("name").asString());
	}

	static Stream<ReferenceFillCases.Case> fillCases() {
		return ReferenceFillCases.cases();
	}

	// the broker backfill fills with ReferenceFiller, so both rules must agree case by case
	@ParameterizedTest(name = "{0}")
	@MethodSource("fillCases")
	void fillRefMatchesReferenceFiller(ReferenceFillCases.Case testCase) throws SQLException {
		try (Connection connection = connection();
			 Statement create = connection.createStatement();
			 PreparedStatement fill = connection.prepareStatement(FILL_REF)) {
			create.execute(PostgresReferenceBackfill.CREATE_FILL_REF);
			fill.setString(1, testCase.reference());
			fill.setString(2, testCase.target());
			fill.setArray(3, connection.createArrayOf("text", ReferenceFillCases.FIELDS.stream()
					.map(ReferenceBackfillCatalog.FillableField::referenceKey).toArray()));
			fill.setArray(4, connection.createArrayOf("text", ReferenceFillCases.FIELDS.stream()
					.map(ReferenceBackfillCatalog.FillableField::targetKey).toArray()));
			fill.setArray(5, connection.createArrayOf("boolean", ReferenceFillCases.FIELDS.stream()
					.map(ReferenceBackfillCatalog.FillableField::unwrapSingleElement).toArray()));
			try (ResultSet result = fill.executeQuery()) {
				assertTrue(result.next());
				assertEquals(JSON_MAPPER.readTree(testCase.expected()), JSON_MAPPER.readTree(result.getString(1)));
			}
		}
	}

	private static String runCommand(String... extraArgs) {
		String[] args = Stream.concat(Stream.of("backfill-references", "--target", "postgresql"),
				Stream.of(extraArgs)).toArray(String[]::new);
		StringWriter out = new StringWriter();
		StringWriter err = new StringWriter();
		int exitCode = new CommandLine(new Application())
				.setOut(new PrintWriter(out))
				.setErr(new PrintWriter(err))
				.execute(args);
		assertEquals(CommandLine.ExitCode.OK, exitCode, out + "\n" + err);
		return out.toString();
	}

	private static void seed(String owner) throws SQLException {
		try (Connection connection = connection();
			 PreparedStatement insert = connection.prepareStatement(INSERT)) {
			for (Map.Entry<String, String> target : TARGETS.entrySet()) {
				insert(insert, target.getKey(), TARGET_TYPES.get(target.getKey()), target.getValue());
			}
			insert(insert, OFFERING, OFFERING_TYPE, owner);
		}
	}

	private static void insert(PreparedStatement insert, String id, String type, String data) throws SQLException {
		insert.setString(1, id);
		insert.setString(2, type);
		insert.setString(3, resolve(data));
		insert.setObject(4, SEEDED_AT);
		insert.setObject(5, SEEDED_AT);
		insert.executeUpdate();
	}

	private static Row row(String id) throws SQLException {
		try (Connection connection = connection();
			 PreparedStatement statement = connection.prepareStatement(SELECT_ROW)) {
			statement.setString(1, id);
			try (ResultSet result = statement.executeQuery()) {
				assertTrue(result.next(), id + " is not stored.");
				return new Row(JSON_MAPPER.readTree(result.getString(1)),
						result.getObject(2, OffsetDateTime.class).toInstant());
			}
		}
	}

	private static long outboxCount() throws SQLException {
		try (Connection connection = connection();
			 Statement statement = connection.createStatement();
			 ResultSet result = statement.executeQuery(COUNT_OUTBOX)) {
			result.next();
			return result.getLong(1);
		}
	}

	private static AttributeResult result(ReferenceBackfillReport report, String label) {
		return report.attributes().stream()
				.filter(result -> result.attribute().equals(label))
				.findFirst()
				.orElseThrow(() -> new AssertionError(label + " is missing from the report:\n" + report(report)));
	}

	private static String report(ReferenceBackfillReport report) {
		StringWriter out = new StringWriter();
		report.print(new PrintWriter(out));
		return out.toString();
	}

	private static JsonNode json(String template) {
		return JSON_MAPPER.readTree(resolve(template));
	}

	private static String resolve(String template) {
		String resolved = template;
		for (Map.Entry<String, String> placeholder : PLACEHOLDERS.entrySet()) {
			resolved = resolved.replace(placeholder.getKey(), placeholder.getValue());
		}
		return resolved;
	}

	private static Connection connection() throws SQLException {
		return DriverManager.getConnection(JDBC_URL, DB_CREDENTIAL, DB_CREDENTIAL);
	}

	private static String urn(String type) {
		return String.format("urn:ngsi-ld:%s:%s", type, UUID.randomUUID());
	}

	private record Row(JsonNode data, Instant updatedAt) {
	}
}
