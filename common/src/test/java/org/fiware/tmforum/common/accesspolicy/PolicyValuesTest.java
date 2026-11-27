package org.fiware.tmforum.common.accesspolicy;

import org.fiware.tmforum.accesspolicy.CarriedPolicy;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

class PolicyValuesTest {

	private static final ObjectMapper OBJECT_MAPPER = JsonMapper.builder().build();
	private static final String LOCATION = "productOfferingTerm[0].accessPolicy";
	private static final String UID = "urn:policy:1";
	private static final String POLICY_JSON = "{\"odrl:uid\":\"" + UID + "\",\"odrl:permission\":{\"dspace:scope\":\"catalog\"}}";
	private static final Map<String, Object> POLICY_MAP =
			Map.of("odrl:uid", UID, "odrl:permission", Map.of("dspace:scope", "catalog"));

	/** A bean Jackson cannot serialize, standing in for any value that fails conversion. */
	public static class Unconvertible {
		public String getUid() {
			throw new IllegalStateException("broken getter");
		}
	}

	/** A plain object whose properties form a policy. */
	public record PolicyBean(String uid) {
	}

	static Stream<Arguments> convertibleValues() {
		return Stream.of(
				Arguments.of("a map is passed through", POLICY_MAP, POLICY_MAP),
				Arguments.of("a JsonNode becomes a map", OBJECT_MAPPER.readTree(POLICY_JSON), POLICY_MAP),
				Arguments.of("a bean becomes a map", new PolicyBean(UID), Map.of("uid", UID)));
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("convertibleValues")
	void convertsToAJsonMap(String description, Object value, Map<String, Object> expected) {
		CarriedPolicy carried = PolicyValues.carried(LOCATION, value, OBJECT_MAPPER);

		assertEquals(new CarriedPolicy(LOCATION, expected), carried, description);
	}

	static Stream<Arguments> rawValues() {
		return Stream.of(
				Arguments.of("a string", "not a policy"),
				Arguments.of("a list", List.of(POLICY_MAP)),
				Arguments.of("an unconvertible value", new Unconvertible()),
				Arguments.of("null", null));
	}

	@ParameterizedTest(name = "{0} stays raw")
	@MethodSource("rawValues")
	void keepsTheRawValueWhenItIsNoObject(String description, Object value) {
		CarriedPolicy carried = PolicyValues.carried(LOCATION, value, OBJECT_MAPPER);

		assertEquals(LOCATION, carried.location(), description);
		assertSame(value, carried.value(), description);
	}
}
