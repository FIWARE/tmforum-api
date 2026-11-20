package org.fiware.tmforum.migration.postgresql;

import io.r2dbc.spi.Connection;
import io.r2dbc.spi.ConnectionFactory;
import io.r2dbc.spi.Result;
import io.r2dbc.spi.Statement;
import org.fiware.tmforum.migration.postgresql.ReferenceBackfillCatalog.Entry;
import org.fiware.tmforum.migration.postgresql.ReferenceBackfillCatalog.FillableField;
import org.fiware.tmforum.migration.postgresql.ReferenceBackfillReport.AttributeResult;
import org.fiware.tmforum.storage.postgresql.PostgresStorage;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * Fills absent fields of the catalog's stored references from their local targets, in one transaction of one
 * statement pair per catalog entry. A stored field is never replaced, a reference without a current local target is
 * kept, and neither {@code updated_at} nor the outbox is touched: this is a migration, not a client write.
 * <p>
 * Every value (owner types, attribute, keys) is a bound parameter, so the SQL text depends on the entry's shape only.
 */
class PostgresReferenceBackfill {

	private static final String ENTITY = PostgresStorage.QUALIFIED_ENTITY_TABLE;

	/**
	 * {@code ref} plus every {@code target} key that {@code ref} lacks and {@code target} holds non-null; anything
	 * but an object reference with a target comes back unchanged.
	 */
	static final String CREATE_FILL_REF = """
			CREATE OR REPLACE FUNCTION pg_temp.fill_ref(ref jsonb, target jsonb, ref_keys text[], target_keys text[])
			RETURNS jsonb LANGUAGE sql IMMUTABLE AS $fill$
			SELECT CASE WHEN jsonb_typeof(ref) = 'object' AND target IS NOT NULL
				THEN ref || coalesce((SELECT jsonb_object_agg(k.ref_key, target -> k.target_key)
					FROM unnest(ref_keys, target_keys) AS k(ref_key, target_key)
					WHERE ref -> k.ref_key IS NULL
						AND coalesce(jsonb_typeof(target -> k.target_key), 'null') <> 'null'), '{}'::jsonb)
				ELSE ref END
			$fill$""";

	static final String DROP_FILL_REF = "DROP FUNCTION pg_temp.fill_ref(jsonb, jsonb, text[], text[])";

	// $1 owner types, $2 attribute, $3 reference keys, $4 target keys
	private static final String FILL = "pg_temp.fill_ref(%s, t.data, $3::text[], $4::text[])";
	private static final String OWNER_FILTER = "%1$s.type = ANY($1::text[]) AND %1$s.is_current "
			+ "AND jsonb_typeof(%1$s.data -> $2::text) = '%2$s'";
	private static final String TARGET_JOIN = "t.id = %s ->> 'id' AND t.is_current";

	private static final String SINGLE_REFERENCES = "CROSS JOIN LATERAL (SELECT o.data -> $2::text AS ref) r";
	private static final String LIST_REFERENCES =
			"CROSS JOIN LATERAL jsonb_array_elements(o.data -> $2::text) WITH ORDINALITY AS r(ref, ord)";
	private static final String JSON_OBJECT = "object";
	private static final String JSON_ARRAY = "array";

	// filled references, references whose target is not stored (external or deleted), entities with a change
	private static final String COUNT = """
			SELECT count(*) FILTER (WHERE t.id IS NOT NULL AND %1$s <> r.ref),
				count(*) FILTER (WHERE r.ref ->> 'id' IS NOT NULL AND t.id IS NULL),
				count(DISTINCT o.id) FILTER (WHERE t.id IS NOT NULL AND %1$s <> r.ref)
			FROM %2$s o %3$s
			LEFT JOIN %2$s t ON %4$s
			WHERE %5$s""";

	private static final String UPDATE_SINGLE = """
			UPDATE %1$s e SET data = jsonb_set(e.data, ARRAY[$2::text], %2$s)
			FROM %1$s t
			WHERE %3$s AND %4$s AND %2$s <> e.data -> $2::text""";

	private static final String UPDATE_LIST = """
			UPDATE %1$s e SET data = jsonb_set(e.data, ARRAY[$2::text], filled.refs)
			FROM (SELECT o.id, o.version, jsonb_agg(%2$s ORDER BY r.ord) AS refs
				FROM %1$s o %3$s
				LEFT JOIN %1$s t ON %4$s
				WHERE %5$s
				GROUP BY o.id, o.version) filled
			WHERE e.id = filled.id AND e.version = filled.version AND e.data -> $2::text <> filled.refs""";

	static final String COUNT_SINGLE = String.format(COUNT, fill("r.ref"), ENTITY, SINGLE_REFERENCES,
			targetJoin("r.ref"), ownerFilter("o", JSON_OBJECT));
	static final String COUNT_LIST = String.format(COUNT, fill("r.ref"), ENTITY, LIST_REFERENCES,
			targetJoin("r.ref"), ownerFilter("o", JSON_ARRAY));
	static final String FILL_SINGLE = String.format(UPDATE_SINGLE, ENTITY, fill("e.data -> $2::text"),
			ownerFilter("e", JSON_OBJECT), targetJoin("e.data -> $2::text"));
	static final String FILL_LIST = String.format(UPDATE_LIST, ENTITY, fill("r.ref"), LIST_REFERENCES,
			targetJoin("r.ref"), ownerFilter("o", JSON_ARRAY));

	private final ConnectionFactory connectionFactory;
	private final List<EntryStatements> statements;

	PostgresReferenceBackfill(ConnectionFactory connectionFactory, ReferenceBackfillCatalog catalog) {
		this.connectionFactory = connectionFactory;
		this.statements = statements(catalog);
	}

	/**
	 * The count and fill statements of one catalog entry; {@code bindings} are {@code $1..$4} in order.
	 */
	record EntryStatements(Entry entry, String countSql, String fillSql, List<Object> bindings) {
	}

	/**
	 * Statements for every entry with a fillable field; the others cannot change anything.
	 */
	static List<EntryStatements> statements(ReferenceBackfillCatalog catalog) {
		return catalog.entries().stream()
				.filter(entry -> !entry.fillableFields().isEmpty())
				.map(PostgresReferenceBackfill::statements)
				.toList();
	}

	static EntryStatements statements(Entry entry) {
		List<Object> bindings = List.of(
				entry.ownerTypes().toArray(String[]::new),
				entry.attribute(),
				entry.fillableFields().stream().map(FillableField::referenceKey).toArray(String[]::new),
				entry.fillableFields().stream().map(FillableField::targetKey).toArray(String[]::new));
		return entry.list()
				? new EntryStatements(entry, COUNT_LIST, FILL_LIST, bindings)
				: new EntryStatements(entry, COUNT_SINGLE, FILL_SINGLE, bindings);
	}

	/**
	 * Runs the backfill, or on a dry run only counts and rolls back. Never throws: a failure rolls the whole
	 * transaction back and is recorded in the report.
	 */
	ReferenceBackfillReport run(boolean dryRun) {
		ReferenceBackfillReport report = new ReferenceBackfillReport(dryRun);
		try {
			List<AttributeResult> results = Mono.usingWhen(connectionFactory.create(),
					connection -> backfill(connection, dryRun),
					Connection::close,
					(connection, failure) -> rollbackAndClose(connection),
					PostgresReferenceBackfill::rollbackAndClose).block();
			report.add(results == null ? List.of() : results);
		} catch (RuntimeException e) {
			report.fail(e.getMessage());
		}
		return report;
	}

	private Mono<List<AttributeResult>> backfill(Connection connection, boolean dryRun) {
		return Mono.from(connection.beginTransaction())
				.then(execute(connection, CREATE_FILL_REF, List.of()).then())
				.thenMany(Flux.fromIterable(statements).concatMap(entry -> backfill(connection, entry, dryRun)))
				.collectList()
				.flatMap(results -> finish(connection, dryRun).thenReturn(results));
	}

	private Mono<AttributeResult> backfill(Connection connection, EntryStatements entry, boolean dryRun) {
		Mono<AttributeResult> counted = execute(connection, entry.countSql(), entry.bindings())
				.flatMap(result -> result.map(row -> new AttributeResult(label(entry.entry()),
						row.get(0, Long.class), row.get(1, Long.class), row.get(2, Long.class))))
				.single();
		if (dryRun) {
			return counted;
		}
		return counted.flatMap(result -> execute(connection, entry.fillSql(), entry.bindings())
				.flatMap(Result::getRowsUpdated)
				.reduce(0L, Long::sum)
				.map(result::withChangedEntities));
	}

	// a dry run discards the helper with everything else
	private static Mono<Void> finish(Connection connection, boolean dryRun) {
		if (dryRun) {
			return Mono.from(connection.rollbackTransaction());
		}
		return execute(connection, DROP_FILL_REF, List.of())
				.then(Mono.from(connection.commitTransaction()));
	}

	private static Flux<Result> execute(Connection connection, String sql, List<Object> bindings) {
		return Flux.defer(() -> {
			Statement statement = connection.createStatement(sql);
			for (int i = 0; i < bindings.size(); i++) {
				statement.bind(i, bindings.get(i));
			}
			return Flux.from(statement.execute());
		});
	}

	// the original failure is what the report shows, a failing rollback must not replace it
	private static Mono<Void> rollbackAndClose(Connection connection) {
		return Mono.from(connection.rollbackTransaction())
				.onErrorResume(rollbackFailure -> Mono.empty())
				.then(Mono.from(connection.close()));
	}

	static String label(Entry entry) {
		return String.join(",", entry.ownerTypes()) + "." + entry.attribute();
	}

	private static String fill(String reference) {
		return String.format(FILL, reference);
	}

	private static String ownerFilter(String alias, String jsonType) {
		return String.format(OWNER_FILTER, alias, jsonType);
	}

	private static String targetJoin(String reference) {
		return String.format(TARGET_JOIN, reference);
	}
}
