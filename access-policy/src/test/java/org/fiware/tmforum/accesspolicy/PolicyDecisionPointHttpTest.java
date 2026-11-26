package org.fiware.tmforum.accesspolicy;

import com.nimbusds.jose.util.JSONObjectUtils;
import io.micronaut.context.ApplicationContext;
import org.fiware.tmforum.accesspolicy.AccessDecision.Source;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import reactor.test.StepVerifier;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Stream;

import static org.fiware.tmforum.accesspolicy.ValidationRequestSerializationTest.FULL_POLICY_UID;
import static org.fiware.tmforum.accesspolicy.ValidationRequestSerializationTest.IDENTITY;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Tests the wired {@link PolicyDecisionPoint} over real HTTP against {@link StubPolicyDecisionPoint}.
 */
class PolicyDecisionPointHttpTest {

	private static final String OTHER_POLICY_UID = "urn:uuid:0f1e2d3c-4b5a-4968-8776-655443322110";
	private static final String DENY_REASON = "not a marketplace operator";
	private static final String BASE_PATH = "/base";
	private static final String SERVICE_URL_KEY = PolicyDecisionPoint.SERVICE_PREFIX + ".url";
	private static final String SERVICE_PATH_KEY = PolicyDecisionPoint.SERVICE_PREFIX + ".path";
	private static final String PDP_PATH_KEY = AccessPolicyProperties.PREFIX + ".pdp.path";
	private static final String PDP_URL_KEY = AccessPolicyProperties.PREFIX + ".pdp.url";
	private static final String PDP_TIMEOUT_KEY = AccessPolicyProperties.PREFIX + ".pdp.timeout";
	private static final String JWKS_URI = "https://keycloak.example.org/realms/test/protocol/openid-connect/certs";
	private static final int SERVICE_UNAVAILABLE = 503;
	private static final int EPHEMERAL_PORT = 0;
	private static final int DEFAULT_BACKLOG = 0;
	// generous, so a slow first connection never turns a verdict test into a timeout
	private static final Duration LENIENT_TIMEOUT = Duration.ofSeconds(5);
	private static final Duration SHORT_TIMEOUT = Duration.ofMillis(300);
	private static final Duration STUB_DELAY = SHORT_TIMEOUT.multipliedBy(10);
	private static final Duration FAILURE_SLACK = Duration.ofSeconds(1);
	private static final Duration VERIFY_TIMEOUT = Duration.ofSeconds(10);

	private StubPolicyDecisionPoint stub;

	@BeforeEach
	void startStub() {
		stub = StubPolicyDecisionPoint.start();
	}

	@AfterEach
	void stopStub() {
		stub.close();
	}

	private static ApplicationContext context(Map<String, Object> endpoint, Duration timeout) {
		Map<String, Object> config = new HashMap<>(ValidationRequestSerializationTest.DEPLOYED_JACKSON_CONFIGURATION);
		config.put(AccessPolicyProperties.ENABLED_PROPERTY, "true");
		config.put(AccessPolicyProperties.PREFIX + ".jwt.jwks-uri", JWKS_URI);
		config.put(AccessPolicyProperties.PREFIX + ".jwt.issuer", TestTokens.ISSUER);
		config.put(AccessPolicyProperties.PREFIX + ".jwt.audience", TestTokens.AUDIENCE);
		config.put(PDP_TIMEOUT_KEY, timeout.toMillis() + "ms");
		config.putAll(endpoint);
		return ApplicationContext.run(config);
	}

	private static ValidationRequest request(String policyUid, Subject subject) {
		return ValidationRequest.of(Map.of("uid", policyUid), "catalog", "read",
				new ValidationRequest.Resource("ProductOffering", "urn:ngsi-ld:ProductOffering:42", null, null, null),
				subject);
	}

	private static Stream<Arguments> verdicts() {
		Subject identified = Subject.of(IDENTITY, Map.of(), TestTokens.EXPIRY);
		return Stream.of(
				Arguments.of("permit", FULL_POLICY_UID, identified, AccessDecision.policy(true, null, FULL_POLICY_UID)),
				Arguments.of("deny", OTHER_POLICY_UID, identified,
						AccessDecision.policy(false, DENY_REASON, OTHER_POLICY_UID)),
				Arguments.of("anonymous permit", OTHER_POLICY_UID, Subject.ANONYMOUS,
						AccessDecision.policy(true, null, OTHER_POLICY_UID)),
				Arguments.of("no rule", FULL_POLICY_UID, Subject.ANONYMOUS,
						AccessDecision.policy(false, StubPolicyDecisionPoint.DEFAULT_DENY_REASON, FULL_POLICY_UID)));
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("verdicts")
	void passesOnThePolicyVerdict(String name, String policyUid, Subject subject, AccessDecision expected) {
		stub.permit(FULL_POLICY_UID, IDENTITY)
				.deny(OTHER_POLICY_UID, IDENTITY, DENY_REASON)
				.permit(OTHER_POLICY_UID, null);
		try (ApplicationContext context = context(Map.of(PDP_URL_KEY, stub.url()), LENIENT_TIMEOUT)) {
			PolicyDecisionPoint pdp = context.getBean(PolicyDecisionPoint.class);

			StepVerifier.create(pdp.decide(policyUid, request(policyUid, subject)))
					.expectNext(expected)
					.expectComplete()
					.verify(VERIFY_TIMEOUT);
		}
		assertEquals(1, stub.requestCount());
	}

	private static Stream<Arguments> failures() {
		return Stream.of(
				Arguments.of("503", (Function<StubPolicyDecisionPoint, String>) stub -> {
					stub.failWith(SERVICE_UNAVAILABLE);
					return stub.url();
				}),
				Arguments.of("delay beyond the timeout", (Function<StubPolicyDecisionPoint, String>) stub -> {
					stub.delay(STUB_DELAY);
					return stub.url();
				}),
				Arguments.of("closed port", (Function<StubPolicyDecisionPoint, String>) stub -> closedPortUrl()));
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("failures")
	void tagsAFailureWithinTheTimeout(String name, Function<StubPolicyDecisionPoint, String> pdpUrl) {
		try (ApplicationContext context = context(Map.of(PDP_URL_KEY, pdpUrl.apply(stub)), SHORT_TIMEOUT)) {
			PolicyDecisionPoint pdp = context.getBean(PolicyDecisionPoint.class);

			StepVerifier.create(pdp.decide(FULL_POLICY_UID, request(FULL_POLICY_UID, Subject.ANONYMOUS)))
					.assertNext(decision -> {
						assertEquals(Source.PDP_FAILURE, decision.source());
						assertFalse(decision.permitted());
						assertEquals(FULL_POLICY_UID, decision.policyUid());
					})
					.expectComplete()
					.verify(SHORT_TIMEOUT.plus(FAILURE_SLACK));
		}
	}

	private static Stream<Arguments> endpoints() {
		return Stream.of(
				Arguments.of("pdp.url", AccessPolicyProperties.DEFAULT_PDP_PATH,
						(Function<String, Map<String, Object>>) url -> Map.of(PDP_URL_KEY, url)),
				Arguments.of("pdp.url with base path", BASE_PATH + AccessPolicyProperties.DEFAULT_PDP_PATH,
						(Function<String, Map<String, Object>>) url -> Map.of(PDP_URL_KEY, url + BASE_PATH)),
				Arguments.of("service url", AccessPolicyProperties.DEFAULT_PDP_PATH,
						(Function<String, Map<String, Object>>) url -> Map.of(SERVICE_URL_KEY, url)),
				Arguments.of("service url with service path", BASE_PATH + AccessPolicyProperties.DEFAULT_PDP_PATH,
						(Function<String, Map<String, Object>>) url -> Map.of(SERVICE_URL_KEY, url,
								SERVICE_PATH_KEY, BASE_PATH)),
				Arguments.of("service url, path without slash", AccessPolicyProperties.DEFAULT_PDP_PATH,
						(Function<String, Map<String, Object>>) url -> Map.of(SERVICE_URL_KEY, url,
								PDP_PATH_KEY, "validate")),
				Arguments.of("pdp.url, path without slash", AccessPolicyProperties.DEFAULT_PDP_PATH,
						(Function<String, Map<String, Object>>) url -> Map.of(PDP_URL_KEY, url,
								PDP_PATH_KEY, "validate")));
	}

	// the received body must equal the step-1 fixture, which pins the serialisation on the wire, not only in a mapper
	@ParameterizedTest(name = "{0}")
	@MethodSource("endpoints")
	void reachesThePdpThroughEitherEndpoint(String name, String servedPath,
			Function<String, Map<String, Object>> endpoint) throws IOException, ParseException {
		try (StubPolicyDecisionPoint served = StubPolicyDecisionPoint.start(servedPath)) {
			served.permit(FULL_POLICY_UID, IDENTITY);
			try (ApplicationContext context = context(endpoint.apply(served.url()), LENIENT_TIMEOUT)) {
				PolicyDecisionPoint pdp = context.getBean(PolicyDecisionPoint.class);

				StepVerifier.create(pdp.decide(FULL_POLICY_UID, ValidationRequestSerializationTest.fullRequest()))
						.expectNext(AccessDecision.policy(true, null, FULL_POLICY_UID))
						.expectComplete()
						.verify(VERIFY_TIMEOUT);
			}
			assertEquals(List.of(readFixture(ValidationRequestSerializationTest.FULL_FIXTURE)), served.requests());
		}
	}

	private static Map<String, Object> readFixture(String fixture) throws IOException, ParseException {
		try (InputStream in = PolicyDecisionPointHttpTest.class
				.getResourceAsStream(ValidationRequestSerializationTest.FIXTURE_DIRECTORY + fixture)) {
			assertNotNull(in, fixture);
			return JSONObjectUtils.parse(new String(in.readAllBytes(), StandardCharsets.UTF_8));
		}
	}

	// a port just released on loopback refuses connections, so the call fails fast instead of hanging
	private static String closedPortUrl() {
		try (ServerSocket socket = new ServerSocket(EPHEMERAL_PORT, DEFAULT_BACKLOG, InetAddress.getLoopbackAddress())) {
			return "http://" + socket.getInetAddress().getHostAddress() + ":" + socket.getLocalPort();
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}
}
