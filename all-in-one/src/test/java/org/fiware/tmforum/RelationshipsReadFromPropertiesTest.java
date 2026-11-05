package org.fiware.tmforum;

import io.github.wistefan.mapping.annotations.AttributeSetter;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.TreeSet;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * References are stored inline and returned as written (ticket 88), so no relationship may resolve its target on read.
 */
class RelationshipsReadFromPropertiesTest {

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
}
