package org.fiware.tmforum;

import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.TreeSet;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Guards the non-strict read path: the mapper builds an unresolvable relationship target through its String
 * constructor, so every target that is fetched on read (not {@code fromProperties}) must declare one.
 */
class RelationshipTargetConstructorTest {

	@Test
	void fetchedRelationshipTargetsDeclareAStringConstructor() throws Exception {
		TreeSet<String> offenders = new TreeSet<>();
		for (Map.Entry<String, Class<?>> relationship : FetchedRelationshipTargets.find().entrySet()) {
			if (!hasStringConstructor(relationship.getValue())) {
				offenders.add(relationship.getKey() + " -> " + relationship.getValue().getName());
			}
		}

		assertTrue(offenders.isEmpty(),
				"Relationship targets without a String constructor cannot be read id-only:\n"
						+ String.join("\n", offenders));
	}

	private static boolean hasStringConstructor(Class<?> targetClass) {
		try {
			targetClass.getDeclaredConstructor(String.class);
			return true;
		} catch (NoSuchMethodException e) {
			return false;
		}
	}
}
