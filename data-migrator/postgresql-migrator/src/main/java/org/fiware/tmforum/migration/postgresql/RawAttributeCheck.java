package org.fiware.tmforum.migration.postgresql;

import io.github.wistefan.mapping.annotations.AttributeSetter;
import org.fiware.ngsi.model.EntityVO;
import org.fiware.tmforum.common.domain.subscription.TMForumSubscription;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * Compares the top-level attributes of a raw broker entity with the top-level keys of its stored document, so an
 * attribute that the domain mapping drops on read is caught (the document diff reads both sides through that mapping).
 */
final class RawAttributeCheck {

	static final String ESCAPE_PREFIX = "tmfEscaped-";
	static final Set<String> SYSTEM_KEYS = Set.of("id", "type", "@context", "createdAt", "modifiedAt", "deletedAt");
	// Step 7 (cleanup-subscriptions) removes it from the stored hubs
	static final String SUBSCRIPTION_ATTRIBUTE = "subscription";
	private static final String SETTER_PREFIX = "set";

	private final Map<String, String> documentKeys;
	private final boolean hub;

	/**
	 * {@code domainClass} is null for a type no class maps; its attribute names are then taken as the document keys.
	 */
	RawAttributeCheck(String type, Class<?> domainClass) {
		this.documentKeys = domainClass == null ? Map.of() : documentKeys(domainClass);
		this.hub = TMForumSubscription.TYPE_TM_FORUM_SUBSCRIPTION.equals(type);
	}

	/**
	 * Attribute names whose document key differs ({@code targetName} → document key). A setter's key is its bean
	 * property when the class declares that field (Lombok setters, e.g. {@code individualLocation} → {@code location}),
	 * and its {@code targetName} for a bridge setter like {@code setEscapedQuery} → {@code query}.
	 */
	static Map<String, String> documentKeys(Class<?> domainClass) {
		Map<String, String> keys = new HashMap<>();
		for (Method method : domainClass.getMethods()) {
			AttributeSetter setter = method.getAnnotation(AttributeSetter.class);
			if (setter == null || setter.targetName().isEmpty() || !method.getName().startsWith(SETTER_PREFIX)) {
				continue;
			}
			String property = decapitalize(method.getName().substring(SETTER_PREFIX.length()));
			String key = hasField(domainClass, property) ? property : setter.targetName();
			if (!key.equals(setter.targetName())) {
				keys.put(setter.targetName(), key);
			}
		}
		return Map.copyOf(keys);
	}

	/**
	 * The entity's top-level attribute names, unescaped, without the NGSI-LD system keys.
	 */
	static Set<String> attributeNames(EntityVO entity) {
		Set<String> names = new TreeSet<>();
		if (entity.getAdditionalProperties() != null) {
			names.addAll(entity.getAdditionalProperties().keySet());
		}
		if (entity.getLocation() != null) {
			names.add(EntityVO.JSON_PROPERTY_LOCATION);
		}
		if (entity.getObservationSpace() != null) {
			names.add(EntityVO.JSON_PROPERTY_OBSERVATION_SPACE);
		}
		if (entity.getOperationSpace() != null) {
			names.add(EntityVO.JSON_PROPERTY_OPERATION_SPACE);
		}
		Set<String> attributes = new TreeSet<>();
		names.stream()
				.map(name -> name.startsWith(ESCAPE_PREFIX) ? name.substring(ESCAPE_PREFIX.length()) : name)
				.filter(name -> !SYSTEM_KEYS.contains(name))
				.forEach(attributes::add);
		return attributes;
	}

	/**
	 * Checks one entity: {@code missing} fails the run, {@code allowedMissing} and {@code targetOnly} are only listed.
	 */
	Result check(Set<String> sourceAttributes, Set<String> targetKeys) {
		Set<String> missing = new TreeSet<>();
		Set<String> allowedMissing = new TreeSet<>();
		Set<String> expectedKeys = new TreeSet<>();
		for (String attribute : sourceAttributes) {
			String key = documentKeys.getOrDefault(attribute, attribute);
			expectedKeys.add(key);
			if (targetKeys.contains(key)) {
				continue;
			}
			if (hub && SUBSCRIPTION_ATTRIBUTE.equals(attribute)) {
				allowedMissing.add(attribute);
			} else {
				missing.add(attribute);
			}
		}
		Set<String> targetOnly = new TreeSet<>(targetKeys);
		targetOnly.removeAll(expectedKeys);
		targetOnly.removeAll(SYSTEM_KEYS);
		return new Result(missing, allowedMissing, targetOnly);
	}

	/**
	 * Attribute names of one entity, by outcome.
	 */
	record Result(Set<String> missing, Set<String> allowedMissing, Set<String> targetOnly) {
	}

	private static boolean hasField(Class<?> type, String name) {
		for (Class<?> current = type; current != null; current = current.getSuperclass()) {
			for (Field field : current.getDeclaredFields()) {
				if (field.getName().equals(name)) {
					return true;
				}
			}
		}
		return false;
	}

	private static String decapitalize(String name) {
		return name.isEmpty() ? name : Character.toLowerCase(name.charAt(0)) + name.substring(1);
	}
}
