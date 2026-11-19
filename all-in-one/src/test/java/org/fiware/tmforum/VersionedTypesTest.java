package org.fiware.tmforum;

import io.github.wistefan.mapping.annotations.AttributeGetter;
import io.github.wistefan.mapping.annotations.MappingEnabled;
import org.fiware.tmforum.common.domain.EntityWithId;
import org.fiware.tmforum.common.domain.VersionedEntity;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The broker adapter and the backfill migration only know stored type names, PostgreSQL knows the classes; ticket 93
 * needs all three to agree on which types carry a version.
 */
class VersionedTypesTest {

	private static final String VERSION_ATTRIBUTE = "version";
	private static final String BACKFILL_MIGRATION = "/db/migration/tmforum/V3__entity_version_backfill.sql";
	// the type list follows "type IN (", every quoted name in it is a type
	private static final String TYPE_LIST_START = "type IN (";
	private static final Pattern QUOTED_NAME = Pattern.compile("'([^']+)'");

	@Test
	void versionedTypesMatchTheVersionedClasses() throws Exception {
		assertEquals(new TreeSet<>(VersionedEntity.VERSIONED_TYPES), versionedClassTypes(),
				"VersionedEntity.VERSIONED_TYPES should list the stored types of every VersionedEntity class.");
	}

	@Test
	void backfillMigrationCoversTheVersionedTypes() throws IOException {
		assertEquals(new TreeSet<>(VersionedEntity.VERSIONED_TYPES), backfilledTypes(),
				BACKFILL_MIGRATION + " should backfill exactly VersionedEntity.VERSIONED_TYPES.");
	}

	// Class#getMethods, so a version attribute inherited from a superclass counts too
	@Test
	void everyEntityWithAVersionIsVersioned() throws Exception {
		TreeSet<String> offenders = new TreeSet<>();
		for (Class<?> scanned : RelationshipTargets.classes()) {
			if (EntityWithId.class.isAssignableFrom(scanned) && hasVersionAttribute(scanned)
					&& !VersionedEntity.class.isAssignableFrom(scanned)) {
				offenders.add(scanned.getName());
			}
		}

		assertTrue(offenders.isEmpty(),
				"Entities with a version attribute should implement VersionedEntity:\n" + String.join("\n", offenders));
	}

	private static TreeSet<String> versionedClassTypes() throws Exception {
		TreeSet<String> types = new TreeSet<>();
		for (Class<?> scanned : RelationshipTargets.classes()) {
			MappingEnabled mapping = scanned.getAnnotation(MappingEnabled.class);
			if (mapping != null && VersionedEntity.class.isAssignableFrom(scanned)) {
				types.addAll(Arrays.asList(mapping.entityType()));
			}
		}
		return types;
	}

	private static boolean hasVersionAttribute(Class<?> scanned) {
		for (Method method : scanned.getMethods()) {
			AttributeGetter getter = method.getAnnotation(AttributeGetter.class);
			if (getter != null && VERSION_ATTRIBUTE.equals(getter.targetName())) {
				return true;
			}
		}
		return false;
	}

	private static Set<String> backfilledTypes() throws IOException {
		try (InputStream migration = VersionedTypesTest.class.getResourceAsStream(BACKFILL_MIGRATION)) {
			assertNotNull(migration, BACKFILL_MIGRATION + " should be on the classpath.");
			String sql = new String(migration.readAllBytes(), StandardCharsets.UTF_8);
			Matcher names = QUOTED_NAME.matcher(sql.substring(sql.indexOf(TYPE_LIST_START)));
			TreeSet<String> types = new TreeSet<>();
			while (names.find()) {
				types.add(names.group(1));
			}
			return types;
		}
	}
}
