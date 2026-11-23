package org.fiware.tmforum.migration.postgresql;

import io.github.wistefan.mapping.annotations.AttributeSetter;
import io.github.wistefan.mapping.annotations.AttributeType;
import io.github.wistefan.mapping.annotations.MappingEnabled;
import org.fiware.tmforum.migration.postgresql.ReferenceBackfillCatalog.DatasetIdRewrite;
import org.fiware.tmforum.migration.postgresql.ReferenceBackfillCatalog.Entry;
import org.fiware.tmforum.migration.postgresql.ReferenceBackfillCatalog.Excluded;
import org.fiware.tmforum.migration.postgresql.ReferenceBackfillCatalog.FillableField;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Pins the reference backfill catalog against the current domain classes and the pre-88 setter list.
 */
class ReferenceBackfillCatalogTest {

	private static final String PRE_88_SETTERS = "non-from-properties-relationships-9570e548.txt";
	private static final String COMMENT_PREFIX = "#";
	private static final int EXPECTED_ENTRIES = 81;
	private static final int EXPECTED_EXCLUDED = 2;
	private static final int EXPECTED_DATASET_ID_REWRITES = 1;
	private static final int EXPECTED_PRE_88_SETTERS = 83;

	private static final ReferenceBackfillCatalog CATALOG = ReferenceBackfillCatalog.load();

	private static EntityTypeRegistry registry;
	private static Set<String> storedListAttributes;

	@BeforeAll
	static void scan() {
		registry = EntityTypeRegistry.scan();
		storedListAttributes = registry.types().stream()
				.map(type -> registry.domainClass(type).orElseThrow())
				.flatMap(domainClass -> BrokerReferenceBackfill.listAttributes(domainClass).stream())
				.collect(Collectors.toSet());
	}

	static Stream<Entry> entries() {
		return CATALOG.entries().stream();
	}

	static Stream<Excluded> excluded() {
		return CATALOG.excluded().stream();
	}

	static Stream<DatasetIdRewrite> datasetIdRewrites() {
		return CATALOG.datasetIdRewrites().stream();
	}

	@ParameterizedTest
	@MethodSource("entries")
	void attributeIsStillARelationshipSetterOfTheReferenceClass(Entry entry) {
		Method setter = attributeSetter(load(entry.ownerClass()), entry.attribute());
		AttributeType expectedType = entry.list() ? AttributeType.RELATIONSHIP_LIST : AttributeType.RELATIONSHIP;
		assertEquals(expectedType, setter.getAnnotation(AttributeSetter.class).value(), entry.attribute());
		assertEquals(load(entry.referenceClass()), referenceType(setter, entry.list()), entry.attribute());
	}

	@ParameterizedTest
	@MethodSource("entries")
	void fillableFieldsExistOnTheReferenceClass(Entry entry) {
		Class<?> referenceClass = load(entry.referenceClass());
		for (FillableField field : entry.fillableFields()) {
			assertTrue(declaresField(referenceClass, field.referenceKey()),
					referenceClass.getName() + " has no field " + field.referenceKey());
		}
	}

	// the fill copies the target value as it is, so only a list into a single field needs the unwrap; some reference
	// classes pick their target type per instance, so a key counts as a list when any stored class stores it as one
	@ParameterizedTest
	@MethodSource("entries")
	void unwrapIsSetExactlyWhereAListTargetFillsASingleField(Entry entry) {
		Class<?> referenceClass = load(entry.referenceClass());
		for (FillableField field : entry.fillableFields()) {
			boolean referenceList = Collection.class.isAssignableFrom(field(referenceClass, field.referenceKey()).getType());
			boolean targetList = storedListAttributes.contains(field.targetKey());
			assertFalse(referenceList && !targetList, field + " fills a list from a single value");
			assertEquals(targetList && !referenceList, field.unwrapSingleElement(), field.toString());
		}
	}

	@ParameterizedTest
	@MethodSource("entries")
	void ownerTypesAreExactlyTheStoredTypesOfTheOwnerClass(Entry entry) {
		assertOwnerTypesComplete(entry.ownerClass(), entry.ownerTypes());
	}

	@ParameterizedTest
	@MethodSource("datasetIdRewrites")
	void datasetIdRewriteIsStillARelationshipListOfTheStoredOwnerTypes(DatasetIdRewrite rewrite) {
		Method setter = attributeSetter(load(rewrite.ownerClass()), rewrite.attribute());
		assertEquals(AttributeType.RELATIONSHIP_LIST, setter.getAnnotation(AttributeSetter.class).value(),
				rewrite.attribute());
		assertEquals(load(rewrite.referenceClass()), referenceType(setter, true), rewrite.attribute());
		assertOwnerTypesComplete(rewrite.ownerClass(), rewrite.ownerTypes());
	}

	@ParameterizedTest
	@MethodSource("excluded")
	void excludedSetterStillSitsOnAClassWithoutMapping(Excluded excluded) {
		Class<?> declaringClass = load(excluded.declaringClass());
		// ticket 88 turned both into PROPERTY setters; only their presence is pinned
		attributeSetter(declaringClass, excluded.attribute());
		assertNull(declaringClass.getAnnotation(MappingEnabled.class), excluded.declaringClass());
	}

	@Test
	void entriesAndExclusionsAreExactlyThePre88Setters() throws IOException {
		Set<String> pinned = pinnedSetters();
		assertEquals(EXPECTED_PRE_88_SETTERS, pinned.size());
		assertEquals(EXPECTED_ENTRIES, CATALOG.entries().size());
		assertEquals(EXPECTED_EXCLUDED, CATALOG.excluded().size());
		assertEquals(EXPECTED_DATASET_ID_REWRITES, CATALOG.datasetIdRewrites().size());

		Set<String> catalogued = Stream.concat(
						CATALOG.entries().stream().map(e -> e.ownerClass() + "#" + e.attribute()),
						CATALOG.excluded().stream().map(e -> e.declaringClass() + "#" + e.attribute()))
				.collect(Collectors.toCollection(TreeSet::new));
		assertEquals(pinned, catalogued);
	}

	private static Set<String> pinnedSetters() throws IOException {
		try (InputStream in = ReferenceBackfillCatalogTest.class.getClassLoader().getResourceAsStream(PRE_88_SETTERS)) {
			if (in == null) {
				fail("Missing " + PRE_88_SETTERS);
			}
			return new String(in.readAllBytes(), StandardCharsets.UTF_8).lines()
					.map(String::strip)
					.filter(line -> !line.isEmpty() && !line.startsWith(COMMENT_PREFIX))
					.collect(Collectors.toCollection(TreeSet::new));
		}
	}

	// Step 4 limits its UPDATE to these types, so a missing subtype would silently get no backfill.
	private static void assertOwnerTypesComplete(String ownerClassName, List<String> ownerTypes) {
		Class<?> ownerClass = load(ownerClassName);
		assertFalse(ownerTypes.isEmpty(), ownerClassName);
		Set<String> storedTypes = registry.types().stream()
				.filter(type -> ownerClass.isAssignableFrom(registry.domainClass(type).orElseThrow()))
				.collect(Collectors.toCollection(TreeSet::new));
		assertEquals(storedTypes, new TreeSet<>(ownerTypes), ownerClassName);
	}

	private static Method attributeSetter(Class<?> owner, String attribute) {
		return Arrays.stream(owner.getMethods())
				.filter(m -> {
					AttributeSetter setter = m.getAnnotation(AttributeSetter.class);
					return setter != null && setter.targetName().equals(attribute);
				})
				.findFirst()
				.orElseThrow(() -> new AssertionError(owner.getName() + " has no attribute setter " + attribute));
	}

	private static Type referenceType(Method setter, boolean list) {
		Type parameter = setter.getGenericParameterTypes()[0];
		if (list && parameter instanceof ParameterizedType parameterized) {
			return parameterized.getActualTypeArguments()[0];
		}
		return parameter;
	}

	private static Field field(Class<?> type, String name) {
		for (Class<?> c = type; c != null; c = c.getSuperclass()) {
			Optional<Field> field = Arrays.stream(c.getDeclaredFields()).filter(f -> f.getName().equals(name)).findFirst();
			if (field.isPresent()) {
				return field.get();
			}
		}
		throw new AssertionError(type.getName() + " has no field " + name);
	}

	private static boolean declaresField(Class<?> type, String name) {
		for (Class<?> c = type; c != null; c = c.getSuperclass()) {
			if (Arrays.stream(c.getDeclaredFields()).anyMatch(f -> f.getName().equals(name))) {
				return true;
			}
		}
		return false;
	}

	private static Class<?> load(String className) {
		try {
			return Class.forName(className);
		} catch (ClassNotFoundException e) {
			throw new AssertionError("Unknown class " + className, e);
		}
	}
}
