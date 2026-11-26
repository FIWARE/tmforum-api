package org.fiware.tmforum.accesspolicy;

import com.nimbusds.jose.util.JSONObjectUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.text.ParseException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Self-test of the stub's rule lookup, counting and reset, talking plain JDK HTTP so it does not lean on the client
 * under test.
 */
class StubPolicyDecisionPointTest {

	private static final String POLICY_UID = "urn:uuid:7e4c2f1a-0b8d-4c3e-9f6a-1d2b3c4d5e6f";
	private static final String OTHER_POLICY_UID = "urn:uuid:0f1e2d3c-4b5a-4968-8776-655443322110";
	private static final String IDENTITY = "did:web:fancy-marketplace.biz";
	private static final String OTHER_IDENTITY = "did:web:other.org";
	private static final String DENY_REASON = "not a marketplace operator";
	private static final String EXPANDED_UID_KEY = "http://www.w3.org/ns/odrl/2/uid";
	private static final String ID_KEY = "@id";
	private static final int OK = 200;

	private final HttpClient client = HttpClient.newHttpClient();
	private StubPolicyDecisionPoint stub;

	@BeforeEach
	void startStub() {
		stub = StubPolicyDecisionPoint.start();
	}

	@AfterEach
	void stopStub() {
		stub.close();
		client.close();
	}

	private static Map<String, Object> request(Map<String, Object> policy, String identity) {
		Map<String, Object> subject = new HashMap<>();
		if (identity != null) {
			subject.put("identity", identity);
		}
		subject.put("claims", Map.of("vc", List.of()));
		return Map.of("policy", policy, "jsonInput", Map.of("subject", subject));
	}

	private static Map<String, Object> request(String policyUid, String identity) {
		return request(Map.of("uid", policyUid), identity);
	}

	private Map<String, Object> post(Map<String, Object> body) throws IOException, InterruptedException,
			ParseException {
		HttpResponse<String> response = client.send(HttpRequest.newBuilder(
								URI.create(stub.url() + AccessPolicyProperties.DEFAULT_PDP_PATH))
						.header("Content-Type", "application/json")
						.POST(HttpRequest.BodyPublishers.ofString(JSONObjectUtils.toJSONString(body)))
						.build(),
				HttpResponse.BodyHandlers.ofString());
		assertEquals(OK, response.statusCode());
		return JSONObjectUtils.parse(response.body());
	}

	private static Map<String, Object> permitted() {
		return Map.of("permitted", true);
	}

	private static Map<String, Object> denied(String reason) {
		return Map.of("permitted", false, "reason", reason);
	}

	private static Stream<Arguments> lookups() {
		return Stream.of(
				Arguments.of("exact permit", request(POLICY_UID, IDENTITY), permitted()),
				Arguments.of("exact deny", request(OTHER_POLICY_UID, IDENTITY), denied(DENY_REASON)),
				Arguments.of("anonymous rule", request(POLICY_UID, null), permitted()),
				Arguments.of("unknown identity", request(POLICY_UID, OTHER_IDENTITY),
						denied(StubPolicyDecisionPoint.DEFAULT_DENY_REASON)),
				Arguments.of("anonymous rule only for anonymous", request(OTHER_POLICY_UID, null),
						denied(StubPolicyDecisionPoint.DEFAULT_DENY_REASON)));
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("lookups")
	void answersByPolicyAndIdentity(String name, Map<String, Object> request, Map<String, Object> expected)
			throws Exception {
		stub.permit(POLICY_UID, IDENTITY)
				.deny(OTHER_POLICY_UID, IDENTITY, DENY_REASON)
				.permit(POLICY_UID, null);

		assertEquals(expected, post(request));
	}

	@Test
	void defaultAnswerAppliesToUnmatchedRequests() throws Exception {
		stub.defaultAnswer(true);

		assertEquals(permitted(), post(request(POLICY_UID, IDENTITY)));
	}

	private static Stream<Arguments> uidShapes() {
		return Stream.of(
				Arguments.of("expanded IRI with node list",
						Map.of(EXPANDED_UID_KEY, List.of(Map.of(ID_KEY, POLICY_UID))), permitted()),
				Arguments.of("odrl:uid as node", Map.of("odrl:uid", Map.of(ID_KEY, POLICY_UID)), permitted()),
				Arguments.of("odrl:uid as string", Map.of("odrl:uid", POLICY_UID), permitted()),
				Arguments.of("plain uid", Map.of("uid", POLICY_UID), permitted()),
				Arguments.of("top-level @id", Map.of(ID_KEY, POLICY_UID), permitted()),
				Arguments.of("no uid", Map.of("@type", "Policy"),
						denied(StubPolicyDecisionPoint.NO_POLICY_UID_REASON)));
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("uidShapes")
	void readsThePolicyUidFromEveryShape(String name, Map<String, Object> policy, Map<String, Object> expected)
			throws Exception {
		stub.permit(POLICY_UID, IDENTITY).defaultAnswer(true);

		assertEquals(expected, post(request(policy, IDENTITY)));
	}

	@Test
	void countsAndRecordsRequests() throws Exception {
		Map<String, Object> first = request(POLICY_UID, IDENTITY);
		Map<String, Object> second = request(OTHER_POLICY_UID, null);

		post(first);
		post(second);

		assertEquals(2, stub.requestCount());
		assertEquals(List.of(first, second), stub.requests());
	}

	@Test
	void resetDropsRulesAndRequests() throws Exception {
		stub.permit(POLICY_UID, IDENTITY).defaultAnswer(true);
		post(request(POLICY_UID, IDENTITY));

		stub.reset();

		assertEquals(0, stub.requestCount());
		assertEquals(List.of(), stub.requests());
		assertEquals(denied(StubPolicyDecisionPoint.DEFAULT_DENY_REASON), post(request(POLICY_UID, IDENTITY)));
	}
}
