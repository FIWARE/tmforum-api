package org.fiware.tmforum.migration.postgresql;

import org.fiware.migrationfixture.ImportEntities;
import org.fiware.migrationfixture.ImportEntities.Plain;
import org.fiware.migrationfixture.ImportEntities.Versioned;
import org.fiware.ngsi.model.EntityVO;
import org.fiware.ngsi.model.PropertyVO;
import org.fiware.tmforum.common.domain.subscription.TMForumSubscription;
import org.fiware.tmforum.migration.postgresql.VerifyReport.TypeReport;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ObjectNode;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VerifyCommandTest {

	private static final String PLAIN = ImportEntities.PLAIN_TYPE;
	private static final String VERSIONED = ImportEntities.VERSIONED_TYPE;
	private static final String HUB = TMForumSubscription.TYPE_TM_FORUM_SUBSCRIPTION;
	private static final String NAME = "name";
	private static final String QUERY = "query";
	private static final int PAGE_SIZE = 2;
	private static final int SAMPLE_SIZE = 3;
	private static final long SEED = 42;
	private static final int FULL_CHECK_THRESHOLD = 5;
	private static final JsonMapper MAPPER = JsonMapper.builder().build();
	private static final ReferenceBackfillCatalog EMPTY_CATALOG =
			new ReferenceBackfillCatalog(List.of(), List.of(), List.of());

	private final EntityTypeRegistry registry =
			new EntityTypeRegistry(List.of(Versioned.class, Plain.class, TMForumSubscription.class), Map.of());
	private final FakeSource source = new FakeSource();
	private final FakeTarget target = new FakeTarget();

	@Test
	void passesAnExactCopy() {
		IntStream.range(0, 3).forEach(i -> copy(entity(PLAIN, i, NAME, "n" + i)));

		VerifyReport report = run(PLAIN);

		assertTrue(report.succeeded(), report.failures().toString());
		TypeReport type = report.type(PLAIN);
		assertEquals(3, type.brokerCount);
		assertEquals(3, type.targetCount);
		assertEquals(3, type.rawChecked);
		assertEquals(3, type.diffChecked);
		assertTrue(type.fullCheck);
	}

	@Test
	void listsTheSeededVersionWithoutFailing() {
		EntityVO entity = entity(VERSIONED, 0, NAME, "n");
		source.add(entity);
		target.store(entity, "{\"id\":\"" + entity.getId() + "\",\"name\":\"n\",\"version\":\"0.0.1\"}");

		VerifyReport report = run(VERSIONED);

		assertTrue(report.succeeded(), report.failures().toString());
		assertEquals(1, report.type(VERSIONED).allowedDifferences.get(DocumentDiff.Kind.SEEDED_VERSION).count);
		assertEquals(1, report.type(VERSIONED).targetOnlyKeys.get(DocumentDiff.VERSION_KEY));
	}

	static Stream<Arguments> corruptions() {
		return Stream.of(
				Arguments.of("deleted row", (Corruption) (target, entity) -> target.documents.remove(entity.getId()),
						List.of("count mismatch", "missing in the target")),
				Arguments.of("changed field", (Corruption) (target, entity) -> target.documents.put(entity.getId(),
						"{\"id\":\"" + entity.getId() + "\",\"name\":\"changed\"}"), List.of("field differences")),
				Arguments.of("removed attribute", (Corruption) (target, entity) -> target.documents.put(entity.getId(),
						"{\"id\":\"" + entity.getId() + "\"}"), List.of("attribute 'name' missing")),
				Arguments.of("escaped key", (Corruption) (target, entity) -> target.escapedKeyIds.add(
						entity.getId().toString()), List.of("escape check")));
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("corruptions")
	void failsNamingTheCause(String description, Corruption corruption, List<String> causes) {
		IntStream.range(0, 2).forEach(i -> copy(entity(PLAIN, i, NAME, "n" + i)));
		corruption.apply(target, source.entities.get(PLAIN).getFirst());

		VerifyReport report = run(PLAIN);

		assertFalse(report.succeeded(), description);
		String failures = String.join("\n", report.failures());
		causes.forEach(cause -> assertTrue(failures.contains(cause), cause + " not in:\n" + failures));
	}

	@Test
	void reportsEveryEscapedKeyRowWithBoundedExamples() {
		int rows = VerifyReport.MAX_EXAMPLES * 5;
		IntStream.range(0, rows).forEach(i -> target.escapedKeyIds.add("urn:ngsi-ld:escaped:" + i));

		VerifyReport report = run(PLAIN);
		ObjectNode escaped = (ObjectNode) report.toJson(MAPPER).get("escapedKeyRows");

		assertEquals(rows, escaped.get("count").asLong());
		assertEquals(VerifyReport.MAX_EXAMPLES, escaped.get("examples").size());
		assertTrue(String.join("\n", report.failures()).contains("escape check: " + rows + " rows"),
				report.failures().toString());
	}

	@Test
	void diffsOnlyASampleOfALargeType() {
		IntStream.range(0, FULL_CHECK_THRESHOLD + 1).forEach(i -> copy(entity(PLAIN, i, NAME, "n" + i)));

		VerifyReport report = run(PLAIN);

		assertTrue(report.succeeded(), report.failures().toString());
		assertFalse(report.type(PLAIN).fullCheck);
		assertEquals(SAMPLE_SIZE, report.type(PLAIN).diffChecked);
		assertEquals(FULL_CHECK_THRESHOLD + 1, report.type(PLAIN).rawChecked);
	}

	static Stream<Arguments> hubQueries() {
		return Stream.of(
				// the broker holds URLEncoder output; a literal %3D was encoded to %253D
				Arguments.of(true, "a%253Db%2Bc", "a%3Db+c", true),
				Arguments.of(true, "state%3Dactive", "state=active", true),
				Arguments.of(true, "state%3Dactive", "state%3Dactive", false),
				Arguments.of(false, "price=10%+tax%3D", "price=10%+tax%3D", true),
				Arguments.of(false, "price=10%+tax%3D", "price=10% tax=", false));
	}

	@ParameterizedTest(name = "source-escaped={0}, broker {1}, stored {2}")
	@MethodSource("hubQueries")
	void checksStoredHubQueriesAgainstTheBroker(boolean sourceEscaped, String brokerQuery, String storedQuery,
			boolean passes) {
		EntityVO hub = entity(HUB, 0, QUERY, brokerQuery);
		source.add(hub);
		target.store(hub, "{\"id\":\"" + hub.getId() + "\",\"query\":\"" + storedQuery + "\"}");

		VerifyReport report = new VerifyCommand(source, target, registry, EMPTY_CATALOG,
				new VerifyOptions(PAGE_SIZE, SAMPLE_SIZE, SEED, 0, sourceEscaped)).run(List.of(HUB));

		assertEquals(passes ? 0 : 1, report.type(HUB).hubQueryMismatches.count, report.failures().toString());
	}

	@Test
	void printsAndSerializesTheFailures() {
		copy(entity(PLAIN, 0, NAME, "n"));
		target.counts.put(PLAIN, 2L);

		VerifyReport report = run(PLAIN);
		StringWriter out = new StringWriter();
		report.print(new PrintWriter(out));
		ObjectNode json = report.toJson(MAPPER);

		assertTrue(out.toString().contains("ERROR " + PLAIN + ": count mismatch"), out.toString());
		assertTrue(out.toString().contains("Verify FAILED."), out.toString());
		assertFalse(json.get("succeeded").asBoolean());
		assertEquals(SEED, json.get("seed").asLong());
		assertEquals(2, json.path("types").path(PLAIN).path("targetCount").asLong());
	}

	private VerifyReport run(String type) {
		return new VerifyCommand(source, target, registry, EMPTY_CATALOG,
				new VerifyOptions(PAGE_SIZE, SAMPLE_SIZE, SEED, FULL_CHECK_THRESHOLD, true)).run(List.of(type));
	}

	private void copy(EntityVO entity) {
		source.add(entity);
		target.store(entity, FakeSource.document(entity).toString());
	}

	private static EntityVO entity(String type, int index, String attribute, String value) {
		EntityVO entity = new EntityVO().id(URI.create(String.format("urn:ngsi-ld:%s:%d", type, index))).type(type);
		entity.setAdditionalProperties(attribute, new PropertyVO().value(value));
		return entity;
	}

	@FunctionalInterface
	private interface Corruption {
		void apply(FakeTarget target, EntityVO entity);
	}

	// a domain object stand-in: the document itself
	private record Document(ObjectNode json) {
	}

	private static final class FakeSource implements EntitySource {
		final Map<String, List<EntityVO>> entities = new LinkedHashMap<>();

		void add(EntityVO entity) {
			entities.computeIfAbsent(entity.getType(), type -> new ArrayList<>()).add(entity);
		}

		@Override
		public Page page(String type, int offset, int limit) {
			List<EntityVO> all = entities.getOrDefault(type, List.of());
			return new Page(all.subList(Math.min(offset, all.size()), Math.min(offset + limit, all.size())), all.size());
		}

		@Override
		public <T> T toDomain(EntityVO entity, Class<T> domainClass) {
			@SuppressWarnings("unchecked")
			T document = (T) new Document(document(entity));
			return document;
		}

		@Override
		public Set<String> types() {
			return entities.keySet();
		}

		static ObjectNode document(EntityVO entity) {
			ObjectNode json = MAPPER.createObjectNode();
			json.put("id", entity.getId().toString());
			entity.getAdditionalProperties().forEach((name, property) ->
					json.put(name, String.valueOf(((PropertyVO) property).getValue())));
			return json;
		}
	}

	private static final class FakeTarget implements VerifyTarget {
		final Map<URI, String> documents = new HashMap<>();
		final Map<URI, String> types = new HashMap<>();
		final Map<String, Long> counts = new HashMap<>();
		final List<String> escapedKeyIds = new ArrayList<>();

		void store(EntityVO entity, String document) {
			documents.put(entity.getId(), document);
			types.put(entity.getId(), entity.getType());
		}

		@Override
		public Map<String, Long> currentCounts() {
			Map<String, Long> current = new HashMap<>();
			documents.keySet().forEach(id -> current.merge(types.get(id), 1L, Long::sum));
			current.putAll(counts);
			return current;
		}

		@Override
		public Map<URI, Set<String>> topLevelKeys(Collection<URI> ids) {
			Map<URI, Set<String>> keys = new HashMap<>();
			ids.stream().filter(documents::containsKey).forEach(id ->
					keys.put(id, Set.copyOf(MAPPER.readTree(documents.get(id)).propertyNames())));
			return keys;
		}

		@Override
		public Map<URI, String> storedDocuments(Collection<URI> ids) {
			Map<URI, String> stored = new HashMap<>();
			ids.stream().filter(documents::containsKey).forEach(id -> stored.put(id, documents.get(id)));
			return stored;
		}

		@Override
		public ObjectNode document(Object domainEntity) {
			return ((Document) domainEntity).json();
		}

		@Override
		public Object domainEntity(String document, Class<?> domainClass) {
			return new Document((ObjectNode) MAPPER.readTree(document));
		}

		@Override
		public EscapedKeyRows rowsWithEscapedKeys(int maxExamples) {
			return new EscapedKeyRows(escapedKeyIds.size(), escapedKeyIds.stream().limit(maxExamples).toList());
		}
	}
}
