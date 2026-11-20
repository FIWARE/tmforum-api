package org.fiware.tmforum.migration.postgresql;

import org.fiware.migrationfixture.ImportEntities;
import org.fiware.migrationfixture.ImportEntities.Plain;
import org.fiware.migrationfixture.ImportEntities.Versioned;
import org.fiware.ngsi.model.EntityVO;
import org.fiware.tmforum.common.domain.EntityWithId;
import org.fiware.tmforum.migration.postgresql.ImportReport.TypeReport;
import org.fiware.tmforum.storage.StorageErrorReason;
import org.fiware.tmforum.storage.StorageException;
import org.fiware.tmforum.storage.StorageOperation;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URI;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ImportCommandTest {

	private static final String VERSIONED = ImportEntities.VERSIONED_TYPE;
	private static final String PLAIN = ImportEntities.PLAIN_TYPE;
	private static final String GIVEN_VERSION = "2.1";
	private static final String UNMAPPED_TYPE = "Subscription";
	private static final String ESCAPER_DRIFT = "The forbidden-character escaper is false, but --source-escaped is true.";
	private static final int PAGE_SIZE = 2;
	private static final Instant CREATED_AT = Instant.parse("2024-01-02T03:04:05Z");
	private static final Instant MODIFIED_AT = Instant.parse("2025-06-07T08:09:10Z");

	private final EntityTypeRegistry registry =
			new EntityTypeRegistry(List.of(Versioned.class, Plain.class), Map.of());
	private final FakeSource source = new FakeSource();
	private final FakeTarget target = new FakeTarget();

	static Stream<Arguments> versionSeeding() {
		return Stream.of(
				Arguments.of(VERSIONED, null, ImportCommand.SEED_VERSION, 1),
				Arguments.of(VERSIONED, "", ImportCommand.SEED_VERSION, 1),
				Arguments.of(VERSIONED, GIVEN_VERSION, GIVEN_VERSION, 0),
				Arguments.of(PLAIN, null, null, 0));
	}

	@ParameterizedTest
	@MethodSource("versionSeeding")
	void seedsTheVersionOfVersionedEntitiesWithoutOne(String type, String version, String expectedVersion,
			long expectedSeeded) {
		source.add(type, 1, version);

		ImportReport report = run(options(false, false, false), type);

		assertTrue(report.succeeded());
		assertEquals(expectedSeeded, report.type(type).seededVersion);
		Object written = target.written.getFirst().entity();
		assertEquals(expectedVersion, written instanceof Versioned versioned ? versioned.getVersion() : null);
	}

	static Stream<Arguments> conflicts() {
		return Stream.of(
				// fresh target, an id stored meanwhile: a failure
				Arguments.of(false, 0, true, false, 1, 0, 1),
				// a resumed run over a partly filled target: the stored id is skipped
				Arguments.of(true, 1, true, true, 1, 1, 0),
				Arguments.of(true, 0, false, true, 2, 0, 0),
				// a filled target without --resume: nothing is read or written
				Arguments.of(false, 1, false, false, 0, 0, 0));
	}

	@ParameterizedTest
	@MethodSource("conflicts")
	void handlesConflictsAndFilledTargets(boolean resume, long stored, boolean firstConflicts, boolean succeeds,
			long written, long skipped, long failed) {
		source.add(PLAIN, 2, null);
		target.stored.put(PLAIN, stored);
		if (firstConflicts) {
			target.conflicting.add(source.entities.get(PLAIN).getFirst().getId());
		}

		TypeReport report = run(options(resume, false, false), PLAIN).type(PLAIN);

		assertEquals(succeeds, report.succeeded());
		assertEquals(written, report.written);
		assertEquals(skipped, report.skipped);
		assertEquals(failed, report.failed);
		assertEquals(written, target.written.size());
	}

	@Test
	void passesTheBrokerTimestampsThrough() {
		source.add(PLAIN, 1, null);

		run(options(false, false, false), PLAIN);

		Written written = target.written.getFirst();
		assertEquals(CREATED_AT, written.createdAt());
		assertEquals(MODIFIED_AT, written.updatedAt());
	}

	static Stream<Arguments> paging() {
		return Stream.of(
				// entities, broker cap per request, expected requests
				Arguments.of(0, PAGE_SIZE, 1),
				Arguments.of(1, PAGE_SIZE, 1),
				Arguments.of(4, PAGE_SIZE, 2),
				Arguments.of(5, PAGE_SIZE, 3),
				// a broker returning fewer than requested: advance by what came back
				Arguments.of(5, 1, 5));
	}

	@ParameterizedTest
	@MethodSource("paging")
	void pagesUntilTheBrokerCountIsRead(int entities, int brokerCap, int expectedRequests) {
		source.add(PLAIN, entities, null);
		source.cap = brokerCap;

		TypeReport report = run(options(false, false, false), PLAIN).type(PLAIN);

		assertTrue(report.succeeded());
		assertEquals(expectedRequests, source.requests);
		assertEquals(entities, report.read);
		assertEquals(entities, report.written);
		assertEquals(entities, report.brokerCount);
	}

	@Test
	void writesAnIdReadTwiceOnceAndFailsOnTheGap() {
		source.add(PLAIN, 4, null);
		List<EntityVO> all = source.entities.get(PLAIN);
		// unstable order: the second page repeats b and never returns d
		source.script = offset -> offset == 0 ? List.of(all.get(0), all.get(1)) : List.of(all.get(1), all.get(2));

		TypeReport report = run(options(false, false, false), PLAIN).type(PLAIN);

		assertEquals(4, report.read);
		assertEquals(1, report.duplicateRead);
		assertEquals(3, report.written);
		assertEquals(3, target.written.size());
		assertFalse(report.succeeded());
		assertNotNull(report.error);
		assertTrue(report.error.contains("broker counts 4"), report.error);
	}

	static Stream<Arguments> mappingFailures() {
		return Stream.of(
				Arguments.of(false, 2, false),
				Arguments.of(true, 0, true));
	}

	@ParameterizedTest
	@MethodSource("mappingFailures")
	void countsMappingFailuresAndStopsOnlyWhenFailingFast(boolean failFast, int expectedWritten,
			boolean expectedAborted) {
		source.add(PLAIN, 3, null);
		source.unmappable.add(source.entities.get(PLAIN).getFirst().getId());

		ImportReport report = run(options(false, failFast, false), PLAIN);

		assertFalse(report.succeeded());
		assertEquals(1, report.type(PLAIN).failed);
		assertEquals(expectedWritten, target.written.size());
		assertEquals(expectedAborted, print(report).contains("--fail-fast"));
	}

	@Test
	void writesNothingOnADryRun() {
		source.add(VERSIONED, 2, null);

		ImportReport report = run(options(false, false, true), VERSIONED);

		assertTrue(report.succeeded());
		assertTrue(target.written.isEmpty());
		assertEquals(2, report.type(VERSIONED).written);
		assertEquals(2, report.type(VERSIONED).seededVersion);
	}

	@Test
	void failsAndNamesTheCauseWhenTheEscaperFlagDrifted() {
		source.add(PLAIN, 1, null);

		ImportReport report = run(options(false, false, false), PLAIN);
		report.escaperDrift(ESCAPER_DRIFT);

		assertFalse(report.succeeded());
		String printed = print(report);
		assertTrue(printed.contains("ERROR " + ESCAPER_DRIFT));
		assertTrue(printed.contains("Import FAILED."));
		assertFalse(printed.contains("Import succeeded."));
	}

	@Test
	void listsUnmappedSourceTypesWithoutFailing() {
		source.types = Set.of(PLAIN, UNMAPPED_TYPE);

		ImportReport report = run(options(false, false, false), PLAIN);

		assertTrue(report.succeeded());
		assertEquals(Set.of(UNMAPPED_TYPE), report.unmappedTypes());
		assertTrue(print(report).contains(UNMAPPED_TYPE));
	}

	static Stream<Arguments> requestedTypes() {
		return Stream.of(
				Arguments.of(null, Set.of(VERSIONED, PLAIN)),
				Arguments.of(List.of(), Set.of(VERSIONED, PLAIN)),
				Arguments.of(List.of(PLAIN), Set.of(PLAIN)));
	}

	@ParameterizedTest
	@MethodSource("requestedTypes")
	void resolvesTheRequestedTypes(List<String> requested, Set<String> expected) {
		assertEquals(expected, ImportCommand.resolveTypes(registry, requested));
	}

	@Test
	void rejectsAnUnmappedRequestedType() {
		assertThrows(IllegalArgumentException.class,
				() -> ImportCommand.resolveTypes(registry, List.of(PLAIN, UNMAPPED_TYPE)));
	}

	@Test
	void rejectsANonPositivePageSize() {
		assertThrows(IllegalArgumentException.class, () -> new ImportOptions(0, false, false, false));
	}

	private ImportReport run(ImportOptions options, String type) {
		return new ImportCommand(source, target, registry, options).run(List.of(type));
	}

	private static ImportOptions options(boolean resume, boolean failFast, boolean dryRun) {
		return new ImportOptions(PAGE_SIZE, resume, failFast, dryRun);
	}

	private static String print(ImportReport report) {
		StringWriter out = new StringWriter();
		report.print(new PrintWriter(out));
		return out.toString();
	}

	private record Written(Object entity, Instant createdAt, Instant updatedAt) {
	}

	/**
	 * Slices the added entities by offset and limit, unless a script overrides the page content.
	 */
	private static final class FakeSource implements EntitySource {
		private final Map<String, List<EntityVO>> entities = new HashMap<>();
		private final Map<URI, EntityWithId> domainEntities = new HashMap<>();
		private final Set<URI> unmappable = new HashSet<>();
		private Set<String> types = Set.of();
		private Function<Integer, List<EntityVO>> script;
		private int cap = Integer.MAX_VALUE;
		private int requests;

		void add(String type, int count, String version) {
			IntStream.range(0, count).forEach(i -> {
				EntityWithId domainEntity = type.equals(VERSIONED) ? new Versioned(type + i) : new Plain(type + i);
				if (domainEntity instanceof Versioned versioned) {
					versioned.setVersion(version);
				}
				domainEntities.put(domainEntity.getId(), domainEntity);
				entities.computeIfAbsent(type, t -> new ArrayList<>()).add(new EntityVO()
						.id(domainEntity.getId()).type(type).createdAt(CREATED_AT).modifiedAt(MODIFIED_AT));
			});
		}

		@Override
		public Page page(String type, int offset, int limit) {
			requests++;
			List<EntityVO> all = entities.getOrDefault(type, List.of());
			List<EntityVO> page = script != null ? script.apply(offset)
					: all.subList(Math.min(offset, all.size()), Math.min(offset + Math.min(limit, cap), all.size()));
			return new Page(page, all.size());
		}

		@Override
		public <T> T toDomain(EntityVO entity, Class<T> domainClass) {
			if (unmappable.contains(entity.getId())) {
				throw new IllegalStateException("unmappable " + entity.getId());
			}
			return domainClass.cast(domainEntities.get(entity.getId()));
		}

		@Override
		public Set<String> types() {
			return types;
		}
	}

	private static final class FakeTarget implements EntityTarget {
		private final Map<String, Long> stored = new HashMap<>();
		private final Set<URI> conflicting = new HashSet<>();
		private final List<Written> written = new ArrayList<>();

		@Override
		public long count(String type) {
			return stored.getOrDefault(type, 0L);
		}

		@Override
		public void importEntity(Object entity, Instant createdAt, Instant updatedAt) {
			if (conflicting.contains(((EntityWithId) entity).getId())) {
				throw new StorageException(StorageOperation.CREATE, StorageErrorReason.CONFLICT, "taken");
			}
			written.add(new Written(entity, createdAt, updatedAt));
		}
	}
}
