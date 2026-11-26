package org.fiware.tmforum.accesspolicy;

import io.micronaut.context.ApplicationContext;
import io.micronaut.core.serialize.ObjectSerializer;
import io.micronaut.core.serialize.exceptions.SerializationException;
import io.micronaut.core.type.Argument;
import io.micronaut.json.JsonMapper;
import io.micronaut.json.tree.JsonNode;
import org.fiware.tmforum.accesspolicy.AccessDecision.Source;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.Arrays;
import java.util.Map;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Round-trips {@link CachedDecision} through {@link DecisionCache#VALUE_SERIALIZER}, resolved from a context the way
 * the Redis cache resolves its {@code value-serializer}, with the Jackson settings every deployed module carries.
 */
class CachedDecisionSerializationTest {

	private static final Map<String, Object> DEPLOYED_JACKSON_CONFIGURATION = Map.of(
			"jackson.serialization-inclusion", "non_null",
			"jackson.mapper-features.sort-properties-alphabetically", false,
			"jackson.deserialization-features.fail-on-null-for-primitives", false,
			"jackson.date-time-features.write-dates-as-timestamps", false);

	private static final Argument<CachedDecision> CACHED_DECISION = Argument.of(CachedDecision.class);
	private static final long VALID_UNTIL = 1_795_694_400_000L;
	private static final String POLICY_UID = "urn:uuid:7e4c2f1a-0b8d-4c3e-9f6a-1d2b3c4d5e6f";
	private static final String REASON = "Constraint vc:country \"DE\" not satisfied <äöü>";

	private static ApplicationContext context;
	private static ObjectSerializer serializer;
	private static JsonMapper jsonMapper;

	@BeforeAll
	static void startContext() throws ClassNotFoundException {
		context = ApplicationContext.run(DEPLOYED_JACKSON_CONFIGURATION);
		Class<? extends ObjectSerializer> serializerType =
				Class.forName(DecisionCache.VALUE_SERIALIZER).asSubclass(ObjectSerializer.class);
		// a missing bean would let the Redis cache fall back to JdkSerializer without an error
		assertTrue(context.containsBean(serializerType), "The value serializer must be a bean in every app.");
		serializer = context.findOrInstantiateBean(serializerType).orElseThrow();
		jsonMapper = context.getBean(JsonMapper.class);
	}

	@AfterAll
	static void stopContext() {
		context.close();
	}

	static Stream<Arguments> decisions() {
		return Arrays.stream(Source.values()).flatMap(source -> Stream.of(
				Arguments.of(new AccessDecision(true, REASON, POLICY_UID, source)),
				Arguments.of(new AccessDecision(false, REASON, POLICY_UID, source)),
				Arguments.of(new AccessDecision(true, null, POLICY_UID, source)),
				Arguments.of(new AccessDecision(false, REASON, null, source)),
				Arguments.of(new AccessDecision(false, null, null, source))));
	}

	@ParameterizedTest
	@MethodSource("decisions")
	void roundTrips(AccessDecision decision) {
		CachedDecision entry = new CachedDecision(decision, VALID_UNTIL);

		byte[] stored = serializer.serialize(entry).orElseThrow();

		assertEquals(entry, serializer.deserialize(stored, CACHED_DECISION).orElseThrow());
	}

	@Test
	void storesPlainJson() throws IOException {
		CachedDecision entry = new CachedDecision(AccessDecision.policy(true, null, POLICY_UID), VALID_UNTIL);

		JsonNode stored = jsonMapper.readValue(serializer.serialize(entry).orElseThrow(), JsonNode.class);

		JsonNode expected = jsonMapper.writeValueToTree(Map.of(
				"decision", Map.of("permitted", true, "policyUid", POLICY_UID, "source", Source.POLICY.name()),
				"validUntilEpochMilli", VALID_UNTIL));
		assertEquals(expected, stored);
	}

	@Test
	void rejectsJavaSerializedData() throws IOException {
		ByteArrayOutputStream javaSerialized = new ByteArrayOutputStream();
		try (ObjectOutputStream out = new ObjectOutputStream(javaSerialized)) {
			out.writeObject(POLICY_UID);
		}

		assertThrows(SerializationException.class,
				() -> serializer.deserialize(javaSerialized.toByteArray(), CACHED_DECISION));
	}
}
