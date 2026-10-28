package org.fiware.tmforum;

import io.github.wistefan.mapping.CacheSerdeableObjectMapper;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Guards the String constructors the non-strict read path needs ({@link RelationshipTargetConstructorTest}) against
 * Jackson's creator detection: a public single-String constructor can be picked up as a delegating creator, which
 * would change how the domain classes are (de)serialized on the paths that use them directly, e.g. the redis cache.
 */
class RelationshipTargetSerializationTest {

	private static final String ID_FIELD = "id";
	private static final String TEST_ID = "urn:ngsi-ld:test:1";
	private static final ObjectMapper PLAIN_MAPPER = JsonMapper.builder().build();
	// the mapper the cached entities are (de)serialized with
	private static final ObjectMapper CACHE_MAPPER = new CacheSerdeableObjectMapper();

	private static Stream<Arguments> fetchedRelationshipTargets() throws Exception {
		TreeMap<String, Class<?>> distinctTargets = new TreeMap<>();
		for (Class<?> targetClass : FetchedRelationshipTargets.find().values()) {
			distinctTargets.put(targetClass.getName(), targetClass);
		}
		return distinctTargets.entrySet().stream()
				.flatMap(target -> Stream.of(
						Arguments.of("plain: " + target.getKey(), target.getValue(), PLAIN_MAPPER),
						Arguments.of("cache: " + target.getKey(), target.getValue(), CACHE_MAPPER)));
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("fetchedRelationshipTargets")
	void relationshipTargetsAreSerializedAsObjects(String description, Class<?> targetClass, ObjectMapper objectMapper)
			throws Exception {
		Object target = targetClass.getDeclaredConstructor(String.class).newInstance(TEST_ID);

		JsonNode serialized = objectMapper.readTree(objectMapper.writeValueAsString(target));

		assertTrue(serialized.isObject(), "The reference should be serialized as an object: " + description);
		assertEquals(TEST_ID, serialized.get(ID_FIELD).asString(), "The id should be serialized: " + description);
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("fetchedRelationshipTargets")
	void relationshipTargetsAreDeserializedFromObjects(String description, Class<?> targetClass,
			ObjectMapper objectMapper) throws Exception {
		Object target = targetClass.getDeclaredConstructor(String.class).newInstance(TEST_ID);
		String json = objectMapper.writeValueAsString(target);

		Object readBack = objectMapper.readValue(json, targetClass);

		// the id constructor, not the String one, has to stay the creator for a JSON object
		assertEquals(objectMapper.readTree(json), objectMapper.readTree(objectMapper.writeValueAsString(readBack)),
				"The reference should round-trip unchanged: " + description);
	}
}
