package org.fiware.tmforum;

import io.github.wistefan.mapping.annotations.AttributeSetter;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.TreeSet;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The mapper builds every relationship target it reads from properties through its String constructor, so each
 * target must declare one.
 */
class RelationshipTargetConstructorTest {

	@Test
	void relationshipTargetsDeclareAStringConstructor() throws Exception {
		TreeSet<String> offenders = new TreeSet<>();
		for (Map.Entry<String, AttributeSetter> relationship : RelationshipTargets.find().entrySet()) {
			Class<?> targetClass = relationship.getValue().targetClass();
			if (!hasStringConstructor(targetClass)) {
				offenders.add(relationship.getKey() + " -> " + targetClass.getName());
			}
		}

		assertTrue(offenders.isEmpty(),
				"Relationship targets without a String constructor cannot be read:\n"
						+ String.join("\n", offenders));
	}

	static boolean hasStringConstructor(Class<?> targetClass) {
		try {
			targetClass.getDeclaredConstructor(String.class);
			return true;
		} catch (NoSuchMethodException e) {
			return false;
		}
	}
}
