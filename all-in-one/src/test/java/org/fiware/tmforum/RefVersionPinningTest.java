package org.fiware.tmforum;

import io.github.wistefan.mapping.CacheSerdeableObjectMapper;
import org.fiware.tmforum.common.domain.RefEntity;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A reference with a {@code version} pins its target only through {@code getReferenceVersion}, so every such
 * {@link RefEntity} has to forward it, and the getter must stay out of the stored document.
 */
class RefVersionPinningTest {

	private static final String VERSION_GETTER = "getVersion";
	private static final String VERSION_SETTER = "setVersion";
	private static final String REFERENCE_VERSION_GETTER = "getReferenceVersion";
	private static final String REFERENCE_VERSION_KEY = "referenceVersion";
	private static final String VERSION_KEY = "version";
	private static final String TEST_ID = "urn:ngsi-ld:test:1";
	private static final String TEST_VERSION = "1.0";
	private static final ObjectMapper PLAIN_MAPPER = JsonMapper.builder().build();
	private static final ObjectMapper CACHE_MAPPER = new CacheSerdeableObjectMapper();

	@Test
	void everyVersionedRefForwardsItsVersion() throws Exception {
		TreeSet<String> offenders = new TreeSet<>();
		for (Class<?> ref : versionedRefs()) {
			RefEntity instance = instantiate(ref);
			ref.getMethod(VERSION_SETTER, String.class).invoke(instance, TEST_VERSION);
			if (!TEST_VERSION.equals(instance.getReferenceVersion())) {
				offenders.add(ref.getName());
			}
		}

		assertTrue(offenders.isEmpty(),
				"References with a version that do not override " + REFERENCE_VERSION_GETTER + " pin nothing:\n"
						+ String.join("\n", offenders));
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("serializedRefs")
	void referenceVersionIsNotSerialized(String description, Class<?> ref, ObjectMapper objectMapper)
			throws Exception {
		RefEntity instance = instantiate(ref);
		ref.getMethod(VERSION_SETTER, String.class).invoke(instance, TEST_VERSION);

		JsonNode serialized = objectMapper.readTree(objectMapper.writeValueAsString(instance));

		assertFalse(serialized.has(REFERENCE_VERSION_KEY), "The pin should not be stored: " + description);
		assertEquals(TEST_VERSION, serialized.get(VERSION_KEY).asString(), "The version should be stored: "
				+ description);
	}

	private static Stream<Arguments> serializedRefs() throws Exception {
		return versionedRefs().stream()
				.flatMap(ref -> Stream.of(
						Arguments.of("plain: " + ref.getName(), ref, PLAIN_MAPPER),
						Arguments.of("cache: " + ref.getName(), ref, CACHE_MAPPER)));
	}

	// concrete RefEntity subclasses with a version getter and setter, the ones a client can pin
	private static List<Class<?>> versionedRefs() throws Exception {
		List<Class<?>> refs = new ArrayList<>();
		for (Class<?> scanned : RelationshipTargets.classes()) {
			if (RefEntity.class.isAssignableFrom(scanned) && !Modifier.isAbstract(scanned.getModifiers())
					&& hasMethod(scanned, VERSION_GETTER) && hasMethod(scanned, VERSION_SETTER, String.class)) {
				refs.add(scanned);
			}
		}
		assertFalse(refs.isEmpty(), "No versioned references found.");
		return refs;
	}

	private static boolean hasMethod(Class<?> type, String name, Class<?>... parameterTypes) {
		try {
			type.getMethod(name, parameterTypes);
			return true;
		} catch (NoSuchMethodException e) {
			return false;
		}
	}

	private static RefEntity instantiate(Class<?> ref) throws Exception {
		return (RefEntity) ref.getDeclaredConstructor(String.class).newInstance(TEST_ID);
	}
}
