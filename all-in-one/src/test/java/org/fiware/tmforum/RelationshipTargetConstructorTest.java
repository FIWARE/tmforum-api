package org.fiware.tmforum;

import io.github.wistefan.mapping.annotations.AttributeSetter;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The mapper builds every relationship target it reads from properties through its String constructor, so each
 * target must declare one.
 */
class RelationshipTargetConstructorTest {

	// Pre-existing defects found by ticket 88 Step 4 (see IMPLEMENTATION_PLAN.md): a stored value of these relationships
	// fails the read. The two PaymentMethodRefVO setters name the generated VO instead of the domain PaymentMethodRef.
	private static final Set<String> KNOWN_WITHOUT_STRING_CONSTRUCTOR = Set.of(
			"org.fiware.account.model.PaymentMethodRefVO",
			"org.fiware.customerbillmanagement.model.PaymentMethodRefVO",
			"org.fiware.tmforum.product.RelatedProductOrderItemRef");

	@Test
	void relationshipTargetsDeclareAStringConstructor() throws Exception {
		TreeSet<String> offenders = new TreeSet<>();
		for (Map.Entry<String, AttributeSetter> relationship : RelationshipTargets.find().entrySet()) {
			Class<?> targetClass = relationship.getValue().targetClass();
			if (!KNOWN_WITHOUT_STRING_CONSTRUCTOR.contains(targetClass.getName()) && !hasStringConstructor(targetClass)) {
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
