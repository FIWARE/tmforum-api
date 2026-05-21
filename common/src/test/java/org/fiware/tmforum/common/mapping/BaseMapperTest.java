package org.fiware.tmforum.common.mapping;

import io.github.wistefan.mapping.UnmappedProperty;
import org.fiware.tmforum.common.domain.Entity;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.net.URI;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for the schema-driven array rebuild applied in
 * {@link BaseMapper#afterMappingFromEntity}.
 *
 * <p>The mapper undoes the JSON-LD single-element-array compaction that
 * NGSI-LD brokers apply to nested data: when the schema declares an
 * attribute as {@code "type": "array"} but the broker returned a scalar
 * (or any non-list value), it must be wrapped back into a single-element
 * list. The fix walks the value/schema trees in lock-step so the rebuild
 * applies at every depth, not just to top-level attributes.
 */
class BaseMapperTest {

	@DisplayName("coerceToSchema wraps a scalar value into a single-element list when the schema declares the field as array.")
	@Test
	void coerceWrapsScalarIntoSingleElementListAtTopLevel() throws Exception {
		Object coerced = invokeCoerce("\"step-cache\"", "{\"type\":\"array\",\"items\":{\"type\":\"string\"}}");
		assertEquals(List.of("step-cache"), coerced);
	}

	@DisplayName("coerceToSchema keeps an empty list as an empty list.")
	@Test
	void coerceKeepsEmptyList() throws Exception {
		Object coerced = invokeCoerce("[]", "{\"type\":\"array\",\"items\":{\"type\":\"string\"}}");
		assertEquals(List.of(), coerced);
	}

	@DisplayName("coerceToSchema preserves an already correct multi-element array.")
	@Test
	void coercePreservesMultiElementArray() throws Exception {
		Object coerced = invokeCoerce("[\"a\",\"b\"]", "{\"type\":\"array\",\"items\":{\"type\":\"string\"}}");
		assertEquals(List.of("a", "b"), coerced);
	}

	@DisplayName("coerceToSchema rebuilds nested array inside an object: the Blueprint orchestrationPlan shape.")
	@Test
	void coerceRebuildsNestedArrayInsideObject() throws Exception {
		// orchestrationPlan: { steps: [ {dependsOn: []}, {dependsOn: "step-cache"} ], version: "1.0" }
		// Schema for orchestrationPlan describes steps[].dependsOn as array of strings.
		String schemaJson = "{"
				+ "\"type\":\"object\","
				+ "\"properties\":{"
				+   "\"steps\":{"
				+     "\"type\":\"array\","
				+     "\"items\":{"
				+       "\"type\":\"object\","
				+       "\"properties\":{"
				+         "\"id\":{\"type\":\"string\"},"
				+         "\"dependsOn\":{\"type\":\"array\",\"items\":{\"type\":\"string\"}}"
				+       "}"
				+     "}"
				+   "},"
				+   "\"version\":{\"type\":\"string\"}"
				+ "}"
				+ "}";

		Map<String, Object> step1 = new LinkedHashMap<>();
		step1.put("id", "step-cache");
		step1.put("dependsOn", List.of()); // already empty list, stays
		Map<String, Object> step2 = new LinkedHashMap<>();
		step2.put("id", "step-web");
		step2.put("dependsOn", "step-cache"); // broker compacted single-element list to scalar
		Map<String, Object> orchestrationPlan = new LinkedHashMap<>();
		orchestrationPlan.put("steps", List.of(step1, step2));
		orchestrationPlan.put("version", "1.0");

		Object coerced = invokeCoerceWithRawValue(orchestrationPlan, schemaJson);
		assertInstanceOf(Map.class, coerced);
		Map<?, ?> coercedMap = (Map<?, ?>) coerced;
		List<?> rebuiltSteps = (List<?>) coercedMap.get("steps");
		assertEquals(List.of(), ((Map<?, ?>) rebuiltSteps.get(0)).get("dependsOn"));
		assertEquals(List.of("step-cache"), ((Map<?, ?>) rebuiltSteps.get(1)).get("dependsOn"),
				"single-element list compacted to a scalar must be rebuilt at any nesting depth");
		assertEquals("1.0", coercedMap.get("version"));
	}

	private static Object invokeCoerce(String valueJson, String schemaJson) throws Exception {
		com.fasterxml.jackson.databind.ObjectMapper om = new com.fasterxml.jackson.databind.ObjectMapper();
		Object value = om.readValue(valueJson, Object.class);
		return invokeCoerceWithRawValue(value, schemaJson);
	}

	private static Object invokeCoerceWithRawValue(Object value, String schemaJson) throws Exception {
		com.fasterxml.jackson.databind.ObjectMapper om = new com.fasterxml.jackson.databind.ObjectMapper();
		com.fasterxml.jackson.databind.JsonNode schemaNode = om.readTree(schemaJson);
		Method m = BaseMapper.class.getDeclaredMethod("coerceToSchema", Object.class, com.fasterxml.jackson.databind.JsonNode.class);
		m.setAccessible(true);
		return m.invoke(null, value, schemaNode);
	}
}
