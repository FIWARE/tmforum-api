package org.fiware.tmforum;

import io.github.wistefan.mapping.annotations.AttributeGetter;
import io.github.wistefan.mapping.annotations.AttributeSetter;
import io.github.wistefan.mapping.annotations.AttributeType;
import org.fiware.tmforum.common.domain.RefEntity;
import org.fiware.tmforum.common.domain.subscription.Subscription;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * References are stored inline and returned as written (ticket 88), so no relationship may resolve its target on read,
 * and every field of a reference has to be written as an embedded sub-property.
 */
class RelationshipsReadFromPropertiesTest {

	// generated API models carry this suffix; a relationship must name the domain class
	private static final String GENERATED_MODEL_SUFFIX = "VO";

	// a pointer to the broker subscription, not a reference payload: reads use only its id, so it is not embedded
	private static final Set<Class<?>> EXEMPT_TARGETS = Set.of(Subscription.class);

	// still dropped on write: Orion-LD merges a multi-instance sub-attribute, so the encoding of the balance list is
	// open (IMPLEMENTATION_PLAN.md, Step 1 result)
	private static final Set<String> EXEMPT_GETTERS = Set.of(
			org.fiware.tmforum.customerbillmanagement.domain.FinancialAccountRef.class.getName()
					+ "#getAccountBalance");

	@Test
	void everyRelationshipIsReadFromProperties() throws Exception {
		TreeSet<String> offenders = new TreeSet<>();
		for (Map.Entry<String, AttributeSetter> relationship : RelationshipTargets.find().entrySet()) {
			if (!relationship.getValue().fromProperties()) {
				offenders.add(relationship.getKey());
			}
		}

		assertTrue(offenders.isEmpty(),
				"Relationship setters without fromProperties = true resolve their target on read:\n"
						+ String.join("\n", offenders));
	}

	@Test
	void relationshipTargetsWriteEveryFieldInline() throws Exception {
		TreeSet<String> offenders = new TreeSet<>();
		for (Class<?> target : targets().values()) {
			if (target.getSimpleName().endsWith(GENERATED_MODEL_SUFFIX)) {
				offenders.add(target.getName() + " is a generated model");
				continue;
			}
			for (Class<?> type = target; type != null && type != Object.class; type = type.getSuperclass()) {
				for (Method method : type.getDeclaredMethods()) {
					AttributeGetter getter = method.getAnnotation(AttributeGetter.class);
					String key = type.getName() + "#" + method.getName();
					if (getter != null && !isEmbeddedProperty(getter) && !EXEMPT_GETTERS.contains(key)) {
						offenders.add(target.getName() + ": " + key);
					}
				}
			}
		}

		assertTrue(offenders.isEmpty(),
				"Relationship target getters that are not PROPERTY with embedProperty = true are not stored inline:\n"
						+ String.join("\n", offenders));
	}

	private static boolean isEmbeddedProperty(AttributeGetter getter) {
		return getter.value() == AttributeType.PROPERTY && getter.embedProperty();
	}

	// every relationship setter's target class plus every RefEntity subclass, by name
	private static TreeMap<String, Class<?>> targets() throws Exception {
		TreeMap<String, Class<?>> targets = new TreeMap<>();
		for (AttributeSetter setter : RelationshipTargets.find().values()) {
			targets.put(setter.targetClass().getName(), setter.targetClass());
		}
		for (Class<?> scanned : RelationshipTargets.classes()) {
			if (RefEntity.class.isAssignableFrom(scanned)) {
				targets.put(scanned.getName(), scanned);
			}
		}
		EXEMPT_TARGETS.forEach(exempt -> targets.remove(exempt.getName()));
		return targets;
	}
}
