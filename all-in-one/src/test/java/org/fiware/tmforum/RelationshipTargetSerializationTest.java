package org.fiware.tmforum;

import io.github.wistefan.mapping.CacheSerdeableObjectMapper;
import io.github.wistefan.mapping.annotations.AttributeSetter;
import org.fiware.tmforum.product.RelatedProductOrderItemRef;
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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Guards the String constructors the relationship read path needs ({@link RelationshipTargetConstructorTest}) against
 * Jackson's creator detection: a public single-String constructor can be picked up as a delegating creator, which
 * would change how the domain classes are (de)serialized on the paths that use them directly, e.g. the redis cache.
 */
class RelationshipTargetSerializationTest {

	private static final String ID_FIELD = "id";
	// targets that name their id field after the TMForum schema instead of "id"
	private static final Map<Class<?>, String> ID_FIELD_OVERRIDES =
			Map.of(RelatedProductOrderItemRef.class, "productOrderId");
	private static final String TEST_ID = "urn:ngsi-ld:test:1";
	private static final ObjectMapper PLAIN_MAPPER = JsonMapper.builder().build();
	// the mapper the cached entities are (de)serialized with
	private static final ObjectMapper CACHE_MAPPER = new CacheSerdeableObjectMapper();

	private static Stream<Arguments> relationshipTargets() throws Exception {
		TreeMap<String, Class<?>> distinctTargets = new TreeMap<>();
		for (AttributeSetter setter : RelationshipTargets.find().values()) {
			// a target without one is reported by RelationshipTargetConstructorTest
			if (RelationshipTargetConstructorTest.hasStringConstructor(setter.targetClass())) {
				distinctTargets.put(setter.targetClass().getName(), setter.targetClass());
			}
		}
		return distinctTargets.entrySet().stream()
				.flatMap(target -> Stream.of(
						Arguments.of("plain: " + target.getKey(), target.getValue(), PLAIN_MAPPER),
						Arguments.of("cache: " + target.getKey(), target.getValue(), CACHE_MAPPER)));
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("relationshipTargets")
	void relationshipTargetsAreSerializedAsObjects(String description, Class<?> targetClass, ObjectMapper objectMapper)
			throws Exception {
		Object target = targetClass.getDeclaredConstructor(String.class).newInstance(TEST_ID);

		JsonNode serialized = objectMapper.readTree(objectMapper.writeValueAsString(target));

		assertTrue(serialized.isObject(), "The reference should be serialized as an object: " + description);
		JsonNode id = serialized.get(ID_FIELD_OVERRIDES.getOrDefault(targetClass, ID_FIELD));
		assertNotNull(id, "The id should be serialized: " + description);
		assertEquals(TEST_ID, id.asString(), "The id should be serialized: " + description);
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("relationshipTargets")
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
