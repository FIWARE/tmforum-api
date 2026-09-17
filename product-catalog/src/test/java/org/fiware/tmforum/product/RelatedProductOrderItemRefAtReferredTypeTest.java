package org.fiware.tmforum.product;

import io.github.wistefan.mapping.annotations.AttributeGetter;
import io.github.wistefan.mapping.annotations.AttributeSetter;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Same fix as {@code RefEntity.atReferredType} (see {@code RefEntityAtReferredTypeTest} in common),
 * applied here because {@link RelatedProductOrderItemRef} declares its own {@code atReferredType}
 * mapping rather than inheriting it.
 */
class RelatedProductOrderItemRefAtReferredTypeTest {

	@Test
	public void theGetterWritesUnderTheNewNameWithoutALeadingAt() throws NoSuchMethodException {
		Method getter = RelatedProductOrderItemRef.class.getDeclaredMethod("getAtReferredType");

		AttributeGetter attributeGetter = getter.getAnnotation(AttributeGetter.class);

		assertEquals("atReferredType", attributeGetter.targetName());
	}

	@Test
	public void theSetterReadsTheNewNameAndFallsBackToTheLegacyOne() throws NoSuchMethodException {
		Method setter = RelatedProductOrderItemRef.class.getDeclaredMethod("setAtReferredType", String.class);

		AttributeSetter attributeSetter = setter.getAnnotation(AttributeSetter.class);

		assertEquals("atReferredType", attributeSetter.targetName());
		assertArrayEquals(new String[] { "@referredType" }, attributeSetter.legacyNames());
	}
}
