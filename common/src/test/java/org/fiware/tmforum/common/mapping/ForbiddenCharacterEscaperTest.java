package org.fiware.tmforum.common.mapping;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class ForbiddenCharacterEscaperTest {

	@AfterEach
	public void resetToDefault() {
		// the flag is a static, process-wide switch - never leak a disabled state into other tests.
		ForbiddenCharacterEscaper.setEnabled(true);
	}

	@Test
	public void nullIsPassedThroughUnchanged() {
		assertNull(ForbiddenCharacterEscaper.escape(null));
		assertNull(ForbiddenCharacterEscaper.unescape(null));
	}

	@Test
	public void forbiddenCharactersAreEscapedAndRoundTrip() {
		String value = "a<b>c\"d'e=f;g(h)i";

		String escaped = ForbiddenCharacterEscaper.escape(value);

		assertNotEquals(value, escaped);
		assertEquals(value, ForbiddenCharacterEscaper.unescape(escaped));
	}

	@Test
	public void aValueWithoutForbiddenCharactersStillRoundTrips() {
		String value = "plainValue123";

		assertEquals(value, ForbiddenCharacterEscaper.unescape(ForbiddenCharacterEscaper.escape(value)));
	}

	@Test
	public void whenDisabledValuesArePassedThroughUnchanged() {
		ForbiddenCharacterEscaper.setEnabled(false);
		String value = "a<b>c=d";

		assertEquals(value, ForbiddenCharacterEscaper.escape(value));
		assertEquals(value, ForbiddenCharacterEscaper.unescape(value));
	}
}
