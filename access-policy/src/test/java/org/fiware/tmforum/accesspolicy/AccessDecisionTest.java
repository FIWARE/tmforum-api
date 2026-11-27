package org.fiware.tmforum.accesspolicy;

import io.micronaut.context.ApplicationContext;
import io.micronaut.json.JsonMapper;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.IOException;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Tests that fail-open never hides a PDP failure or an unreadable policy, and that a decision survives the JSON round
 * trip a cache needs.
 */
class AccessDecisionTest {

	private static final String POLICY_UID = "urn:uuid:7e4c2f1a-0b8d-4c3e-9f6a-1d2b3c4d5e6f";
	private static final String FAILURE_REASON = "PDP did not answer within PT2S";
	private static final String UNREADABLE_REASON = "accessPolicy: the policy has no odrl:uid";

	private static ApplicationContext context;
	private static JsonMapper jsonMapper;

	@BeforeAll
	static void startContext() {
		context = ApplicationContext.run(ValidationRequestSerializationTest.DEPLOYED_JACKSON_CONFIGURATION);
		jsonMapper = context.getBean(JsonMapper.class);
	}

	@AfterAll
	static void stopContext() {
		context.close();
	}

	@ParameterizedTest(name = "failOpen={0}")
	@CsvSource({"true, true", "false, false"})
	void failureKeepsItsSource(boolean failOpen, boolean expectedPermitted) {
		AccessDecision decision = AccessDecision.failure(POLICY_UID, FAILURE_REASON, failOpen);

		assertEquals(expectedPermitted, decision.permitted());
		assertEquals(AccessDecision.Source.PDP_FAILURE, decision.source());
		assertEquals(POLICY_UID, decision.policyUid());
		assertEquals(FAILURE_REASON, decision.reason());
	}

	@ParameterizedTest(name = "failOpen={0}")
	@CsvSource({"true, true", "false, false"})
	void unreadableKeepsItsSource(boolean failOpen, boolean expectedPermitted) {
		AccessDecision decision = AccessDecision.unreadable(POLICY_UID, UNREADABLE_REASON, failOpen);

		assertEquals(expectedPermitted, decision.permitted());
		assertEquals(AccessDecision.Source.UNREADABLE_POLICY, decision.source());
		assertEquals(POLICY_UID, decision.policyUid());
		assertEquals(UNREADABLE_REASON, decision.reason());
	}

	private static Stream<Arguments> decisions() {
		return Stream.of(
				Arguments.of(AccessDecision.policy(true, null, POLICY_UID)),
				Arguments.of(AccessDecision.policy(false, "Constraint not satisfied", POLICY_UID)),
				Arguments.of(AccessDecision.failure(POLICY_UID, FAILURE_REASON, true)),
				Arguments.of(AccessDecision.failure(null, FAILURE_REASON, false)),
				Arguments.of(AccessDecision.unreadable(null, UNREADABLE_REASON, true)));
	}

	@ParameterizedTest
	@MethodSource("decisions")
	void roundTripsThroughJson(AccessDecision decision) throws IOException {
		assertEquals(decision, jsonMapper.readValue(jsonMapper.writeValueAsBytes(decision), AccessDecision.class));
	}
}
