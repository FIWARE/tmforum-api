package org.fiware.tmforum.common.domain;

import io.github.wistefan.mapping.annotations.AttributeGetter;
import io.github.wistefan.mapping.annotations.AttributeSetter;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * {@code atReferredType} used to be mapped to the NGSI-LD attribute {@code @referredType} - Coraine
 * rejects that leading "@". It's now mapped to {@code atReferredType}, with {@code legacyNames} kept
 * on the setter so entities written under the old key are still readable.
 */
class RefEntityAtReferredTypeTest {

	@Test
	public void theGetterWritesUnderTheNewNameWithoutALeadingAt() throws NoSuchMethodException {
		Method getter = RefEntity.class.getDeclaredMethod("getAtReferredType");

		AttributeGetter attributeGetter = getter.getAnnotation(AttributeGetter.class);

		assertEquals("atReferredType", attributeGetter.targetName());
	}

	@Test
	public void theSetterReadsTheNewNameAndFallsBackToTheLegacyOne() throws NoSuchMethodException {
		Method setter = RefEntity.class.getDeclaredMethod("setAtReferredType", String.class);

		AttributeSetter attributeSetter = setter.getAnnotation(AttributeSetter.class);

		assertEquals("atReferredType", attributeSetter.targetName());
		assertArrayEquals(new String[] { "@referredType" }, attributeSetter.legacyNames());
	}
}
