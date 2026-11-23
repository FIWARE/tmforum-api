package org.fiware.tmforum.migration.postgresql;

import io.micronaut.context.ApplicationContext;
import io.r2dbc.spi.Connection;
import io.r2dbc.spi.ConnectionFactory;
import io.r2dbc.spi.Row;
import org.fiware.tmforum.storage.StorageOperation;
import org.fiware.tmforum.storage.postgresql.DocumentMapper;
import org.fiware.tmforum.storage.postgresql.DocumentMode;
import org.fiware.tmforum.storage.postgresql.PostgresDocumentMapper;
import org.fiware.tmforum.storage.postgresql.PostgresStorage;
import reactor.core.publisher.Flux;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ObjectNode;

import java.net.URI;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

/**
 * Reads {@code tmforum.entity} of the target context for {@code verify}, with its {@link PostgresDocumentMapper}.
 */
class PostgresVerifyTarget implements VerifyTarget {

	static final String COUNT_CURRENT = "SELECT type, count(*) FROM " + PostgresStorage.QUALIFIED_ENTITY_TABLE
			+ " WHERE is_current GROUP BY type";
	// LEFT JOIN, so a stored document without keys still shows up as stored
	static final String TOP_LEVEL_KEYS = "SELECT e.id, k.key FROM " + PostgresStorage.QUALIFIED_ENTITY_TABLE
			+ " e LEFT JOIN LATERAL jsonb_object_keys(e.data) k(key) ON true WHERE e.id = ANY($1::text[]) AND e.is_current";
	static final String DOCUMENTS = "SELECT id, data::text FROM " + PostgresStorage.QUALIFIED_ENTITY_TABLE
			+ " WHERE id = ANY($1::text[]) AND is_current";
	// walks every object member and array element; the CASE guards keep jsonb_each/jsonb_array_elements off scalars;
	// the window count runs before LIMIT, so it is the number of all matching rows, not of the examples
	static final String ESCAPED_KEYS = """
			WITH RECURSIVE node(id, key, value) AS (
			  SELECT e.id, NULL::text, e.data FROM %s e WHERE e.is_current
			  UNION ALL
			  SELECT n.id, child.key, child.value FROM node n
			  CROSS JOIN LATERAL (
			    SELECT o.key, o.value FROM jsonb_each(
			      CASE WHEN jsonb_typeof(n.value) = 'object' THEN n.value ELSE '{}'::jsonb END) o
			    UNION ALL
			    SELECT NULL, a.value FROM jsonb_array_elements(
			      CASE WHEN jsonb_typeof(n.value) = 'array' THEN n.value ELSE '[]'::jsonb END) a
			  ) child
			)
			SELECT id, count(*) OVER () FROM (SELECT DISTINCT id FROM node WHERE starts_with(key, $1)) matched
			ORDER BY id LIMIT $2""".formatted(
			PostgresStorage.QUALIFIED_ENTITY_TABLE);

	private final ConnectionFactory connectionFactory;
	private final DocumentMapper documentMapper;
	private final JsonMapper jsonMapper = JsonMapper.builder().build();

	PostgresVerifyTarget(ApplicationContext target) {
		this.connectionFactory = target.getBean(ConnectionFactory.class);
		this.documentMapper = target.getBean(PostgresDocumentMapper.class).storage();
	}

	@Override
	public Map<String, Long> currentCounts() {
		Map<String, Long> counts = new HashMap<>();
		query(COUNT_CURRENT, List.of(), row -> Map.entry(row.get(0, String.class), row.get(1, Long.class)))
				.forEach(entry -> counts.put(entry.getKey(), entry.getValue()));
		return counts;
	}

	@Override
	public Map<URI, Set<String>> topLevelKeys(Collection<URI> ids) {
		Map<URI, Set<String>> keys = new HashMap<>();
		if (ids.isEmpty()) {
			return keys;
		}
		query(TOP_LEVEL_KEYS, List.of(idArray(ids)), row -> new String[]{row.get(0, String.class),
				row.get(1, String.class)})
				.forEach(idAndKey -> {
					Set<String> entityKeys = keys.computeIfAbsent(URI.create(idAndKey[0]), id -> new HashSet<>());
					if (idAndKey[1] != null) {
						entityKeys.add(idAndKey[1]);
					}
				});
		return keys;
	}

	@Override
	public Map<URI, String> storedDocuments(Collection<URI> ids) {
		Map<URI, String> documents = new HashMap<>();
		if (ids.isEmpty()) {
			return documents;
		}
		query(DOCUMENTS, List.of(idArray(ids)), row -> Map.entry(row.get(0, String.class),
				row.get(1, String.class)))
				.forEach(entry -> documents.put(URI.create(entry.getKey()), entry.getValue()));
		return documents;
	}

	@Override
	public ObjectNode document(Object domainEntity) {
		return (ObjectNode) jsonMapper.readTree(
				documentMapper.toDocument(domainEntity, DocumentMode.FULL, StorageOperation.READ));
	}

	@Override
	public Object domainEntity(String document, Class<?> domainClass) {
		return documentMapper.fromDocument(document, domainClass, StorageOperation.READ);
	}

	@Override
	public EscapedKeyRows rowsWithEscapedKeys(int maxExamples) {
		List<Map.Entry<String, Long>> rows = query(ESCAPED_KEYS, List.of(RawAttributeCheck.ESCAPE_PREFIX, maxExamples),
				row -> Map.entry(row.get(0, String.class), row.get(1, Long.class)));
		long count = rows.isEmpty() ? 0 : rows.getFirst().getValue();
		return new EscapedKeyRows(count, rows.stream().map(Map.Entry::getKey).toList());
	}

	private static String[] idArray(Collection<URI> ids) {
		return ids.stream().map(URI::toString).toArray(String[]::new);
	}

	private <T> List<T> query(String sql, List<Object> bindings, Function<Row, T> mapping) {
		Function<Connection, Flux<T>> run = connection -> Flux.defer(() -> {
			var statement = connection.createStatement(sql);
			for (int i = 0; i < bindings.size(); i++) {
				statement.bind(i, bindings.get(i));
			}
			return Flux.from(statement.execute()).flatMap(result -> result.map((row, metadata) -> mapping.apply(row)));
		});
		List<T> rows = Flux.usingWhen(connectionFactory.create(), run, Connection::close).collectList().block();
		return rows == null ? List.of() : rows;
	}
}
