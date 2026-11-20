package org.fiware.tmforum.migration.postgresql;

import org.fiware.tmforum.migration.postgresql.PostgresReferenceBackfill.EntryStatements;
import org.fiware.tmforum.migration.postgresql.ReferenceBackfillCatalog.Entry;
import org.fiware.tmforum.migration.postgresql.ReferenceBackfillCatalog.FillableField;
import io.r2dbc.spi.Connection;
import io.r2dbc.spi.ConnectionFactory;
import io.r2dbc.spi.ConnectionFactoryMetadata;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PostgresReferenceBackfillTest {

	private static final Pattern WHITESPACE = Pattern.compile("\\s+");
	private static final Pattern PLACEHOLDER = Pattern.compile("\\$(\\d+)");
	private static final int BINDING_COUNT = 4;
	private static final String FAILING_FACTORY_NAME = "failing";

	private static final List<String> OWNER_TYPES = List.of("owner-type-a", "owner-type-b");
	private static final String ATTRIBUTE = "someReference";
	private static final List<FillableField> FIELDS = List.of(
			new FillableField("refKeyName", "targetKeyName"), new FillableField("refKeyHref", "targetKeyHref"));

	private static final String FILL_REF = "pg_temp.fill_ref(r.ref, t.data, $3::text[], $4::text[])";
	private static final String COUNT_TEMPLATE = "SELECT count(*) FILTER (WHERE t.id IS NOT NULL AND " + FILL_REF
			+ " <> r.ref), count(*) FILTER (WHERE r.ref ->> 'id' IS NOT NULL AND t.id IS NULL), "
			+ "count(DISTINCT o.id) FILTER (WHERE t.id IS NOT NULL AND " + FILL_REF + " <> r.ref) "
			+ "FROM tmforum.entity o %s LEFT JOIN tmforum.entity t ON t.id = r.ref ->> 'id' AND t.is_current "
			+ "WHERE o.type = ANY($1::text[]) AND o.is_current AND jsonb_typeof(o.data -> $2::text) = '%s'";
	private static final String EXPECTED_COUNT_SINGLE = String.format(COUNT_TEMPLATE,
			"CROSS JOIN LATERAL (SELECT o.data -> $2::text AS ref) r", "object");
	private static final String EXPECTED_COUNT_LIST = String.format(COUNT_TEMPLATE,
			"CROSS JOIN LATERAL jsonb_array_elements(o.data -> $2::text) WITH ORDINALITY AS r(ref, ord)", "array");
	private static final String SINGLE_FILL_REF = "pg_temp.fill_ref(e.data -> $2::text, t.data, $3::text[], $4::text[])";
	private static final String EXPECTED_FILL_SINGLE = "UPDATE tmforum.entity e SET data = "
			+ "jsonb_set(e.data, ARRAY[$2::text], " + SINGLE_FILL_REF + ") FROM tmforum.entity t "
			+ "WHERE e.type = ANY($1::text[]) AND e.is_current AND jsonb_typeof(e.data -> $2::text) = 'object' "
			+ "AND t.id = e.data -> $2::text ->> 'id' AND t.is_current "
			+ "AND " + SINGLE_FILL_REF + " <> e.data -> $2::text";
	private static final String EXPECTED_FILL_LIST = "UPDATE tmforum.entity e SET data = "
			+ "jsonb_set(e.data, ARRAY[$2::text], filled.refs) "
			+ "FROM (SELECT o.id, o.version, jsonb_agg(" + FILL_REF + " ORDER BY r.ord) AS refs "
			+ "FROM tmforum.entity o "
			+ "CROSS JOIN LATERAL jsonb_array_elements(o.data -> $2::text) WITH ORDINALITY AS r(ref, ord) "
			+ "LEFT JOIN tmforum.entity t ON t.id = r.ref ->> 'id' AND t.is_current "
			+ "WHERE o.type = ANY($1::text[]) AND o.is_current AND jsonb_typeof(o.data -> $2::text) = 'array' "
			+ "GROUP BY o.id, o.version) filled "
			+ "WHERE e.id = filled.id AND e.version = filled.version AND e.data -> $2::text <> filled.refs";

	static Stream<Arguments> shapes() {
		return Stream.of(
				Arguments.of(false, EXPECTED_COUNT_SINGLE, EXPECTED_FILL_SINGLE),
				Arguments.of(true, EXPECTED_COUNT_LIST, EXPECTED_FILL_LIST));
	}

	@ParameterizedTest(name = "list={0}")
	@MethodSource("shapes")
	void generatesTheStatementsOfTheEntryShape(boolean list, String expectedCount, String expectedFill) {
		EntryStatements statements = PostgresReferenceBackfill.statements(entry(list, OWNER_TYPES, ATTRIBUTE, FIELDS));

		assertEquals(expectedCount, normalized(statements.countSql()));
		assertEquals(expectedFill, normalized(statements.fillSql()));
	}

	@ParameterizedTest(name = "list={0}")
	@ValueSource(booleans = {false, true})
	void bindsOwnerTypesAttributeAndKeysInOrder(boolean list) {
		List<Object> bindings = PostgresReferenceBackfill.statements(entry(list, OWNER_TYPES, ATTRIBUTE, FIELDS))
				.bindings();

		assertEquals(BINDING_COUNT, bindings.size());
		assertArrayEquals(OWNER_TYPES.toArray(String[]::new), (String[]) bindings.get(0));
		assertEquals(ATTRIBUTE, bindings.get(1));
		assertArrayEquals(new String[]{"refKeyName", "refKeyHref"}, (String[]) bindings.get(2));
		assertArrayEquals(new String[]{"targetKeyName", "targetKeyHref"}, (String[]) bindings.get(3));
	}

	@ParameterizedTest(name = "list={0}")
	@ValueSource(booleans = {false, true})
	void interpolatesNoValueIntoTheSql(boolean list) {
		EntryStatements statements = PostgresReferenceBackfill.statements(entry(list, OWNER_TYPES, ATTRIBUTE, FIELDS));
		EntryStatements other = PostgresReferenceBackfill.statements(entry(list, List.of("other-owner"),
				"otherReference", List.of(new FillableField("otherRefKey", "otherTargetKey"))));

		assertEquals(statements.countSql(), other.countSql(), "The SQL text must depend on the entry's shape only.");
		assertEquals(statements.fillSql(), other.fillSql(), "The SQL text must depend on the entry's shape only.");
		for (String sql : List.of(statements.countSql(), statements.fillSql())) {
			Stream.concat(Stream.concat(OWNER_TYPES.stream(), Stream.of(ATTRIBUTE)),
							FIELDS.stream().flatMap(field -> Stream.of(field.referenceKey(), field.targetKey())))
					.forEach(value -> assertFalse(sql.contains(value), value + " is interpolated into " + sql));
			assertTrue(PLACEHOLDER.matcher(sql).results()
							.allMatch(match -> Integer.parseInt(match.group(1)) <= BINDING_COUNT),
					"Every placeholder must have a binding: " + sql);
		}
	}

	@Test
	void skipsEntriesWithoutFillableFields() {
		Entry fillable = entry(false, OWNER_TYPES, ATTRIBUTE, FIELDS);
		Entry unfillable = entry(true, OWNER_TYPES, "unfillableReference", List.of());

		List<EntryStatements> statements = PostgresReferenceBackfill.statements(
				new ReferenceBackfillCatalog(List.of(fillable, unfillable), List.of(), List.of()));

		assertEquals(List.of(fillable), statements.stream().map(EntryStatements::entry).toList());
	}

	@Test
	void coversEveryFillableCatalogEntry() {
		ReferenceBackfillCatalog catalog = ReferenceBackfillCatalog.load();

		assertEquals(catalog.entries().stream().filter(entry -> !entry.fillableFields().isEmpty()).count(),
				PostgresReferenceBackfill.statements(catalog).size());
	}

	@ParameterizedTest(name = "message={0}")
	@NullSource
	@ValueSource(strings = "connection refused")
	void reportsAFailureEvenWithoutAMessage(String message) {
		ConnectionFactory failing = new ConnectionFactory() {
			@Override
			public Publisher<? extends Connection> create() {
				return Mono.error(new IllegalStateException(message));
			}

			@Override
			public ConnectionFactoryMetadata getMetadata() {
				return () -> FAILING_FACTORY_NAME;
			}
		};

		ReferenceBackfillReport report = new PostgresReferenceBackfill(failing, ReferenceBackfillCatalog.load())
				.run(false);

		assertFalse(report.succeeded());
	}

	private static Entry entry(boolean list, List<String> ownerTypes, String attribute, List<FillableField> fields) {
		return new Entry("org.example.Owner", ownerTypes, attribute, list, "org.example.Reference", fields);
	}

	private static String normalized(String sql) {
		return WHITESPACE.matcher(sql).replaceAll(" ").trim();
	}
}
