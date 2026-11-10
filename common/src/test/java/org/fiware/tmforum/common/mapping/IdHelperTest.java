package org.fiware.tmforum.common.mapping;

import io.github.wistefan.mapping.annotations.MappingEnabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.net.URI;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * The id/type-segment rule both storage adapters apply on {@code get}.
 */
class IdHelperTest {

	private static final String TYPE = "test-type";
	private static final String SECOND_TYPE = "second-test-type";

	@MappingEnabled(entityType = {TYPE, SECOND_TYPE})
	private static class Mapped {
	}

	private static class NotMapped {
	}

	static Stream<Arguments> ids() {
		return Stream.of(
				Arguments.of("urn:ngsi-ld:" + TYPE + ":1", true),
				Arguments.of("urn:ngsi-ld:" + SECOND_TYPE + ":1", true),
				Arguments.of("urn:ngsi-ld:" + TYPE + ":with:colons", true),
				Arguments.of("urn:ngsi-ld:" + TYPE, true),
				Arguments.of("urn:ngsi-ld:other-type:1", false),
				Arguments.of("urn:ngsi-ld", false),
				Arguments.of("plain-id", false));
	}

	@ParameterizedTest(name = "{0} -> {1}")
	@MethodSource("ids")
	void hasTypeSegmentOf(String id, boolean expected) {
		assertEquals(expected, IdHelper.hasTypeSegmentOf(URI.create(id), Mapped.class));
	}

	@Test
	void anUnmappedClassFailsOnlyForAnIdWithATypeSegment() {
		assertFalse(IdHelper.hasTypeSegmentOf(URI.create("plain-id"), NotMapped.class));
		assertThrows(IllegalArgumentException.class,
				() -> IdHelper.hasTypeSegmentOf(URI.create("urn:ngsi-ld:" + TYPE + ":1"), NotMapped.class));
	}
}
