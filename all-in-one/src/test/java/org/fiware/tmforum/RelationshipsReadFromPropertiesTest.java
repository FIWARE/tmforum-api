package org.fiware.tmforum;

import io.github.wistefan.mapping.annotations.AttributeGetter;
import io.github.wistefan.mapping.annotations.AttributeSetter;
import io.github.wistefan.mapping.annotations.AttributeType;
import org.fiware.tmforum.common.domain.RefEntity;
import org.fiware.tmforum.common.domain.subscription.Subscription;
import org.junit.jupiter.api.Test;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.HashSet;
import java.util.List;
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

	private static final List<Class<? extends Annotation>> MAPPING_ANNOTATIONS =
			List.of(AttributeGetter.class, AttributeSetter.class);

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

	// the mapping library finds its annotated methods through Class#getMethods, so these checks look at them the same way
	@Test
	void relationshipTargetsWriteEveryFieldInline() throws Exception {
		TreeSet<String> offenders = new TreeSet<>();
		for (Class<?> target : targets().values()) {
			if (target.getSimpleName().endsWith(GENERATED_MODEL_SUFFIX)) {
				offenders.add(target.getName() + " is a generated model");
				continue;
			}
			for (Method method : target.getMethods()) {
				AttributeGetter getter = method.getAnnotation(AttributeGetter.class);
				if (getter != null && !isEmbeddedProperty(getter) && !EXEMPT_GETTERS.contains(key(method))) {
					offenders.add(target.getName() + ": " + key(method));
				}
			}
		}

		assertTrue(offenders.isEmpty(),
				"Relationship target getters that are not PROPERTY with embedProperty = true are not stored inline:\n"
						+ String.join("\n", offenders));
	}

	@Test
	void relationshipTargetsWriteEveryFieldTheyRead() throws Exception {
		TreeSet<String> offenders = new TreeSet<>();
		for (Class<?> target : targets().values()) {
			Set<String> written = new HashSet<>();
			for (Method method : target.getMethods()) {
				AttributeGetter getter = method.getAnnotation(AttributeGetter.class);
				if (getter != null) {
					written.add(getter.targetName());
				}
			}
			for (Method method : target.getMethods()) {
				AttributeSetter setter = method.getAnnotation(AttributeSetter.class);
				if (setter != null && !written.contains(setter.targetName())) {
					offenders.add(target.getName() + ": " + setter.targetName());
				}
			}
		}

		assertTrue(offenders.isEmpty(),
				"Relationship target fields that are read but have no @AttributeGetter, so are never stored:\n"
						+ String.join("\n", offenders));
	}

	@Test
	void relationshipTargetOverridesKeepTheMappingAnnotations() throws Exception {
		TreeSet<String> offenders = new TreeSet<>();
		for (Class<?> target : targets().values()) {
			for (Class<?> type = target.getSuperclass(); type != null && type != Object.class;
					type = type.getSuperclass()) {
				for (Method inherited : type.getDeclaredMethods()) {
					if (!Modifier.isPublic(inherited.getModifiers())) {
						continue;
					}
					Method resolved = target.getMethod(inherited.getName(), inherited.getParameterTypes());
					for (Class<? extends Annotation> mapping : MAPPING_ANNOTATIONS) {
						if (inherited.isAnnotationPresent(mapping) && !resolved.isAnnotationPresent(mapping)) {
							offenders.add(target.getName() + ": " + key(resolved) + " hides " + key(inherited));
						}
					}
				}
			}
		}

		assertTrue(offenders.isEmpty(),
				"Overrides without the mapping annotation of the method they override drop the field:\n"
						+ String.join("\n", offenders));
	}

	private static String key(Method method) {
		return method.getDeclaringClass().getName() + "#" + method.getName();
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
