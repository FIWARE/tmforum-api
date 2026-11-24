package org.fiware.tmforum.migration.postgresql;

import io.github.classgraph.ClassGraph;
import io.github.classgraph.ScanResult;
import io.github.wistefan.mapping.annotations.MappingEnabled;
import org.fiware.migrationfixture.AmbiguousEntities;
import org.fiware.tmforum.common.domain.VersionedEntity;
import org.fiware.tmforum.common.domain.subscription.Subscription;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EntityTypeRegistryTest {

	private static final String TYPE_CONSTANT_PREFIX = "TYPE_";

	private static EntityTypeRegistry registry;

	@BeforeAll
	static void scan() {
		registry = EntityTypeRegistry.scan();
	}

	static Set<String> versionedTypes() {
		return VersionedEntity.VERSIONED_TYPES;
	}

	// read from the classes rather than the registry, so that the test does not only echo the scan
	static Set<String> typeConstants() throws IllegalAccessException {
		Set<String> types = new TreeSet<>();
		try (ScanResult scan = new ClassGraph().enableAnnotationInfo()
				.acceptPackages(EntityTypeRegistry.BASE_PACKAGE).scan()) {
			for (Class<?> mapped : scan.getClassesWithAnnotation(MappingEnabled.class.getName()).loadClasses()) {
				for (Field field : mapped.getDeclaredFields()) {
					if (field.getName().startsWith(TYPE_CONSTANT_PREFIX) && field.getType() == String.class
							&& Modifier.isStatic(field.getModifiers()) && Modifier.isPublic(field.getModifiers())) {
						types.add((String) field.get(null));
					}
				}
			}
		}
		types.removeAll(EntityTypeRegistry.EXCLUDED_TYPES);
		return types;
	}

	@ParameterizedTest
	@MethodSource("versionedTypes")
	void resolvesEveryVersionedType(String type) {
		assertTrue(registry.domainClass(type).isPresent(), "The versioned type " + type + " should be mapped.");
	}

	@ParameterizedTest
	@MethodSource("typeConstants")
	void resolvesEveryTypeConstant(String type) {
		Class<?> domainClass = registry.domainClass(type).orElseThrow();
		assertTrue(Set.of(domainClass.getAnnotation(MappingEnabled.class).entityType()).contains(type),
				domainClass.getName() + " should declare the type " + type + ".");
	}

	@Test
	void ambiguousTypeFails() {
		List<Class<?>> candidates = List.of(AmbiguousEntities.First.class, AmbiguousEntities.Second.class);

		IllegalStateException failure = assertThrows(IllegalStateException.class,
				() -> new EntityTypeRegistry(candidates, Map.of()));
		assertTrue(failure.getMessage().contains(AmbiguousEntities.SHARED_TYPE),
				"The failure should name the ambiguous type.");
	}

	@Test
	void overrideResolvesAmbiguousType() {
		List<Class<?>> candidates = List.of(AmbiguousEntities.First.class, AmbiguousEntities.Second.class);

		EntityTypeRegistry overridden = new EntityTypeRegistry(candidates,
				Map.of(AmbiguousEntities.SHARED_TYPE, AmbiguousEntities.Second.class));

		assertEquals(AmbiguousEntities.Second.class,
				overridden.domainClass(AmbiguousEntities.SHARED_TYPE).orElseThrow());
	}

	@Test
	void reportsUnmappedSourceTypes() {
		String mapped = versionedTypes().iterator().next();

		assertEquals(Set.of(Subscription.TYPE_SUBSCRIPTION),
				registry.unmappedTypes(List.of(mapped, Subscription.TYPE_SUBSCRIPTION)));
		assertFalse(registry.types().contains(Subscription.TYPE_SUBSCRIPTION));
	}
}
