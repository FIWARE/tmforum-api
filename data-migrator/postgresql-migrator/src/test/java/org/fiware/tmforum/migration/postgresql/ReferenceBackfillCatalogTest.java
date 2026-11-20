package org.fiware.tmforum.migration.postgresql;

import io.github.wistefan.mapping.annotations.AttributeSetter;
import io.github.wistefan.mapping.annotations.AttributeType;
import io.github.wistefan.mapping.annotations.MappingEnabled;
import org.fiware.tmforum.migration.postgresql.ReferenceBackfillCatalog.Entry;
import org.fiware.tmforum.migration.postgresql.ReferenceBackfillCatalog.Excluded;
import org.fiware.tmforum.migration.postgresql.ReferenceBackfillCatalog.FillableField;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
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
	private static final int EXPECTED_PRE_88_SETTERS = 83;

	private static final ReferenceBackfillCatalog CATALOG = ReferenceBackfillCatalog.load();

	private static EntityTypeRegistry registry;

	@BeforeAll
	static void scan() {
		registry = EntityTypeRegistry.scan();
	}

	static Stream<Entry> entries() {
		return CATALOG.entries().stream();
	}

	static Stream<Excluded> excluded() {
		return CATALOG.excluded().stream();
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

	@ParameterizedTest
	@MethodSource("entries")
	void ownerTypesAreStoredTypesOfTheOwnerClass(Entry entry) {
		Class<?> ownerClass = load(entry.ownerClass());
		assertFalse(entry.ownerTypes().isEmpty(), entry.ownerClass());
		for (String type : entry.ownerTypes()) {
			Class<?> stored = registry.domainClass(type).orElseThrow(() -> new AssertionError("Unknown type " + type));
			assertTrue(ownerClass.isAssignableFrom(stored), type + " is stored as " + stored.getName());
		}
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
