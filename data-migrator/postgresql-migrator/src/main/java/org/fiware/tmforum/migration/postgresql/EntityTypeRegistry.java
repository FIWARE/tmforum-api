package org.fiware.tmforum.migration.postgresql;

import io.github.classgraph.ClassGraph;
import io.github.classgraph.ScanResult;
import io.github.wistefan.mapping.annotations.MappingEnabled;
import org.fiware.tmforum.common.domain.EntityWithId;
import org.fiware.tmforum.common.domain.subscription.Subscription;

import java.lang.reflect.Modifier;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.stream.Collectors;

/**
 * Maps each stored NGSI-LD entity type to the domain class the migrator reads it into.
 * Only {@link EntityWithId} subclasses count: the {@code *Ref} classes share their target's type but are never stored.
 * A type claimed by more than one class fails construction unless {@link #OVERRIDES} picks one.
 */
public class EntityTypeRegistry {

	static final String BASE_PACKAGE = "org.fiware.tmforum";

	/**
	 * The class to use for a type that several {@code @MappingEnabled} classes declare.
	 */
	static final Map<String, Class<?>> OVERRIDES = Map.of();

	// NGSI-LD subscriptions live in the broker's subscription API, not among its entities, and have no PostgreSQL row
	static final Set<String> EXCLUDED_TYPES = Set.of(Subscription.TYPE_SUBSCRIPTION);

	private final Map<String, Class<?>> classesByType;

	EntityTypeRegistry(Collection<Class<?>> candidates, Map<String, Class<?>> overrides) {
		Map<String, Set<Class<?>>> claims = new TreeMap<>();
		for (Class<?> candidate : candidates) {
			MappingEnabled mapping = candidate.getAnnotation(MappingEnabled.class);
			if (mapping == null || !EntityWithId.class.isAssignableFrom(candidate)
					|| Modifier.isAbstract(candidate.getModifiers())) {
				continue;
			}
			for (String type : mapping.entityType()) {
				if (EXCLUDED_TYPES.contains(type)) {
					continue;
				}
				claims.computeIfAbsent(type, t -> new TreeSet<>((a, b) -> a.getName().compareTo(b.getName())))
						.add(candidate);
			}
		}
		Map<String, Class<?>> resolved = new HashMap<>();
		Map<String, Set<Class<?>>> ambiguous = new TreeMap<>();
		claims.forEach((type, classes) -> {
			if (overrides.containsKey(type)) {
				resolved.put(type, overrides.get(type));
			} else if (classes.size() == 1) {
				resolved.put(type, classes.iterator().next());
			} else {
				ambiguous.put(type, classes);
			}
		});
		if (!ambiguous.isEmpty()) {
			throw new IllegalStateException("Entity types claimed by several classes, add an override: " + ambiguous);
		}
		this.classesByType = Collections.unmodifiableMap(resolved);
	}

	/**
	 * Scans {@value #BASE_PACKAGE} on the classpath for {@code @MappingEnabled} classes.
	 */
	public static EntityTypeRegistry scan() {
		try (ScanResult scan = new ClassGraph().enableAnnotationInfo().acceptPackages(BASE_PACKAGE).scan()) {
			return new EntityTypeRegistry(
					scan.getClassesWithAnnotation(MappingEnabled.class.getName()).loadClasses(), OVERRIDES);
		}
	}

	public Optional<Class<?>> domainClass(String type) {
		return Optional.ofNullable(classesByType.get(type));
	}

	public Set<String> types() {
		return new TreeSet<>(classesByType.keySet());
	}

	/**
	 * The given source types no domain class maps, e.g. the broker's own {@code Subscription}; the import skips them.
	 */
	public Set<String> unmappedTypes(Collection<String> sourceTypes) {
		return sourceTypes.stream()
				.filter(type -> !classesByType.containsKey(type))
				.collect(Collectors.toCollection(TreeSet::new));
	}
}
