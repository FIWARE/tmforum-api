package org.fiware.tmforum.accesspolicy;

import io.micronaut.cache.AsyncCache;
import io.micronaut.cache.CacheManager;
import io.micronaut.cache.SyncCache;
import io.micronaut.context.ApplicationContext;
import io.micronaut.context.exceptions.ConfigurationException;
import io.micronaut.http.context.ServerRequestContext;
import org.fiware.tmforum.accesspolicy.AccessDecision.Source;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import reactor.core.publisher.Flux;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static org.fiware.tmforum.accesspolicy.ValidationRequestSerializationTest.IDENTITY;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Tests {@link DecisionCache} over Caffeine and the real {@link PolicyDecisionPoint} against
 * {@link StubPolicyDecisionPoint}, with expiry driven by a settable clock.
 */
class DecisionCacheTest {

	private static final String POLICY_UID = "urn:uuid:5d4c3b2a-1f0e-4d9c-8b7a-695847362514";
	private static final String SCOPE = "catalog";
	private static final String ACTION = "read";
	private static final String DENY_REASON = "not a member";
	private static final String JWKS_URI = "https://keycloak.example.org/realms/test/protocol/openid-connect/certs";
	private static final Duration DECISION_TTL = Duration.ofSeconds(60);
	private static final Duration PDP_TIMEOUT = Duration.ofSeconds(5);
	private static final Duration VERIFY_TIMEOUT = Duration.ofSeconds(20);
	// holds every PDP answer back, so all concurrent misses of one key overlap
	private static final Duration PDP_DELAY = Duration.ofMillis(300);
	private static final Duration SHORT_TOKEN_LIFETIME = Duration.ofSeconds(10);
	private static final Duration JUST_BEFORE = Duration.ofMillis(1);
	private static final Instant START = TestTokens.NOW;
	private static final int RESOURCE_COUNT = 100;
	private static final int POLICY_COUNT = 6;
	private static final int DISTINCT_RESOURCES = 3;
	private static final int ROUNDS = 2;
	private static final int SERVICE_UNAVAILABLE = 503;
	private static final Map<String, Object> CREDENTIAL_A = Map.of("type", "MembershipCredential", "level", "gold");
	private static final Map<String, Object> CREDENTIAL_B = Map.of("type", "MembershipCredential", "level", "basic");

	private StubPolicyDecisionPoint stub;
	private final SettableClock clock = new SettableClock(START);

	@BeforeEach
	void startStub() {
		stub = StubPolicyDecisionPoint.start();
	}

	@AfterEach
	void stopStub() {
		stub.close();
	}

	private Map<String, Object> enabledConfiguration(boolean failOpen) {
		Map<String, Object> config = new HashMap<>(ValidationRequestSerializationTest.DEPLOYED_JACKSON_CONFIGURATION);
		config.put(AccessPolicyProperties.ENABLED_PROPERTY, "true");
		config.put(AccessPolicyProperties.PREFIX + ".jwt.jwks-uri", JWKS_URI);
		config.put(AccessPolicyProperties.PREFIX + ".jwt.issuer", TestTokens.ISSUER);
		config.put(AccessPolicyProperties.PREFIX + ".pdp.url", stub.url());
		config.put(AccessPolicyProperties.PREFIX + ".pdp.timeout", PDP_TIMEOUT.toSeconds() + "s");
		config.put(AccessPolicyProperties.PREFIX + ".decision-ttl", DECISION_TTL.toSeconds() + "s");
		config.put(AccessPolicyProperties.PREFIX + ".fail-open", String.valueOf(failOpen));
		return config;
	}

	private ApplicationContext context(boolean failOpen) {
		Map<String, Object> config = enabledConfiguration(failOpen);
		config.putAll(TestCaches.DECISIONS);
		return ApplicationContext.run(config);
	}

	private DecisionCache clocked(ApplicationContext context) {
		return new DecisionCache(context.getBean(AccessPolicyProperties.class),
				context.getBean(PolicyDecisionPoint.class), context.getBean(CacheManager.class), clock);
	}

	private static Map<String, Object> subjectOnlyPolicy(String uid) {
		return Map.of("uid", uid, "permission", List.of(Map.of("action", ACTION,
				"constraint", Map.of("leftOperand", "vc:country", "operator", "eq", "rightOperand", "DE"))));
	}

	private static Map<String, Object> resourceConstrainedPolicy(String uid) {
		return Map.of("uid", uid, "permission", List.of(Map.of("action", ACTION,
				"target", "urn:ngsi-ld:Catalog:partner-catalog")));
	}

	private static ValidationRequest.Resource resource(int index) {
		return new ValidationRequest.Resource("ProductOffering", "urn:ngsi-ld:ProductOffering:" + index, null, null, null);
	}

	private static Subject subject(Map<String, Object> credential, Instant expiresAt) {
		return Subject.of(IDENTITY, Map.of(ValidationRequest.VERIFIABLE_CREDENTIAL_CLAIM, List.of(credential)),
				expiresAt);
	}

	private static Subject subject() {
		return subject(CREDENTIAL_A, TestTokens.EXPIRY);
	}

	private static AccessDecision decide(DecisionCache cache, Map<String, Object> policy, int resource,
			Subject subject) {
		return cache.decide(POLICY_UID, policy, SCOPE, ACTION, resource(resource), subject).block(VERIFY_TIMEOUT);
	}

	private static String policyUid(int index) {
		return "urn:policy:" + index;
	}

	@Test
	void concurrentMissesShareOneCallPerPolicy() {
		stub.defaultAnswer(true).delay(PDP_DELAY);
		try (ApplicationContext context = context(false)) {
			DecisionCache cache = clocked(context);
			Subject subject = subject();

			List<AccessDecision> decisions = Flux.range(0, RESOURCE_COUNT)
					.flatMap(i -> cache.decide(policyUid(i % POLICY_COUNT), subjectOnlyPolicy(policyUid(i % POLICY_COUNT)),
							SCOPE, ACTION, resource(i), subject))
					.collectList()
					.block(VERIFY_TIMEOUT);

			assertEquals(RESOURCE_COUNT, decisions.size());
			assertTrue(decisions.stream().allMatch(AccessDecision::permitted), decisions.toString());
		}
		assertEquals(POLICY_COUNT, stub.requestCount(), "One PDP call per policy, however many resources share it.");
	}

	@Test
	void anotherCredentialSetOfTheSameIdentityGetsItsOwnVerdict() {
		Map<String, Object> policy = subjectOnlyPolicy(POLICY_UID);
		stub.permit(POLICY_UID, IDENTITY);
		try (ApplicationContext context = context(false)) {
			DecisionCache cache = clocked(context);
			Subject gold = subject(CREDENTIAL_A, TestTokens.EXPIRY);
			Subject basic = subject(CREDENTIAL_B, TestTokens.EXPIRY);

			assertEquals(AccessDecision.policy(true, null, POLICY_UID), decide(cache, policy, 0, gold));
			stub.deny(POLICY_UID, IDENTITY, DENY_REASON);
			assertEquals(AccessDecision.policy(false, DENY_REASON, POLICY_UID), decide(cache, policy, 0, basic));
			assertEquals(AccessDecision.policy(true, null, POLICY_UID), decide(cache, policy, 0, gold),
					"The first credential set keeps its cached verdict.");
		}
		assertEquals(2, stub.requestCount());
	}

	private static Stream<Arguments> resourceSharing() {
		return Stream.of(
				Arguments.of("resource-constrained", resourceConstrainedPolicy(POLICY_UID), true, DISTINCT_RESOURCES),
				Arguments.of("subject-only", subjectOnlyPolicy(POLICY_UID), false, 1));
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("resourceSharing")
	void onlyAResourceConstrainedPolicyIsDecidedPerResource(String name, Map<String, Object> policy,
			boolean constrained, int expectedCalls) {
		assertEquals(constrained, ResourceConstraintDetector.constrainsResource(policy), "fixture");
		stub.permit(POLICY_UID, IDENTITY);
		try (ApplicationContext context = context(false)) {
			DecisionCache cache = clocked(context);
			Subject subject = subject();
			for (int round = 0; round < ROUNDS; round++) {
				for (int resource = 0; resource < DISTINCT_RESOURCES; resource++) {
					assertTrue(decide(cache, policy, resource, subject).permitted());
				}
			}
		}
		assertEquals(expectedCalls, stub.requestCount());
	}

	private static Stream<Arguments> expiries() {
		return Stream.of(
				Arguments.of("decision-ttl bounds a widen", TestTokens.TOKEN_LIFETIME, DECISION_TTL, false),
				Arguments.of("decision-ttl bounds a narrow", TestTokens.TOKEN_LIFETIME, DECISION_TTL, true),
				Arguments.of("token expiry bounds a widen", SHORT_TOKEN_LIFETIME, SHORT_TOKEN_LIFETIME, false),
				Arguments.of("token expiry bounds a narrow", SHORT_TOKEN_LIFETIME, SHORT_TOKEN_LIFETIME, true));
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("expiries")
	void aChangedVerdictTakesEffectAtTheEntryBound(String name, Duration tokenLifetime, Duration bound,
			boolean initiallyPermitted) {
		Map<String, Object> policy = subjectOnlyPolicy(POLICY_UID);
		AccessDecision initial = verdict(initiallyPermitted);
		AccessDecision flipped = verdict(!initiallyPermitted);
		rule(initiallyPermitted);
		try (ApplicationContext context = context(false)) {
			DecisionCache cache = clocked(context);
			Subject subject = subject(CREDENTIAL_A, START.plus(tokenLifetime));

			assertEquals(initial, decide(cache, policy, 0, subject));
			rule(!initiallyPermitted);
			clock.set(START.plus(bound).minus(JUST_BEFORE));
			assertEquals(initial, decide(cache, policy, 0, subject), "The entry answers until its bound.");
			assertEquals(1, stub.requestCount());

			clock.set(START.plus(bound));
			assertEquals(flipped, decide(cache, policy, 0, subject), "The entry no longer answers at its bound.");
		}
		assertEquals(2, stub.requestCount());
	}

	private void rule(boolean permitted) {
		if (permitted) {
			stub.permit(POLICY_UID, IDENTITY);
		} else {
			stub.deny(POLICY_UID, IDENTITY, DENY_REASON);
		}
	}

	private static AccessDecision verdict(boolean permitted) {
		return permitted ? AccessDecision.policy(true, null, POLICY_UID)
				: AccessDecision.policy(false, DENY_REASON, POLICY_UID);
	}

	@ParameterizedTest(name = "fail-open {0}")
	@ValueSource(booleans = {true, false})
	void aPdpFailureIsNotCached(boolean failOpen) {
		Map<String, Object> policy = subjectOnlyPolicy(POLICY_UID);
		stub.failWith(SERVICE_UNAVAILABLE);
		try (ApplicationContext context = context(failOpen)) {
			DecisionCache cache = clocked(context);
			Subject subject = subject();
			for (int round = 0; round < ROUNDS; round++) {
				AccessDecision failure = decide(cache, policy, 0, subject);
				assertEquals(Source.PDP_FAILURE, failure.source());
				assertEquals(failOpen, failure.permitted());
			}
			assertEquals(ROUNDS, stub.requestCount(), "Every failure goes back to the PDP.");

			stub.reset();
			stub.permit(POLICY_UID, IDENTITY);
			assertEquals(AccessDecision.policy(true, null, POLICY_UID), decide(cache, policy, 0, subject));
		}
	}

	@Test
	void anExpiredTokenIsNotCached() {
		Map<String, Object> policy = subjectOnlyPolicy(POLICY_UID);
		stub.permit(POLICY_UID, IDENTITY);
		try (ApplicationContext context = context(false)) {
			DecisionCache cache = clocked(context);
			Subject expired = subject(CREDENTIAL_A, START.minus(JUST_BEFORE));
			for (int round = 0; round < ROUNDS; round++) {
				assertTrue(decide(cache, policy, 0, expired).permitted());
			}
		}
		assertEquals(ROUNDS, stub.requestCount());
	}

	private static Stream<Arguments> brokenCaches() {
		CompletableFuture<Object> failed = CompletableFuture.failedFuture(new IllegalStateException("cache down"));
		CompletableFuture<Optional<Object>> miss = CompletableFuture.completedFuture(Optional.empty());
		CompletableFuture<Boolean> stored = CompletableFuture.completedFuture(true);
		return Stream.of(
				Arguments.of("get fails", (Consumer<AsyncCache<Object>>) cache -> {
					doReturn(failed).when(cache).get(any(), eq(CachedDecision.class));
					doReturn(stored).when(cache).put(any(), any());
				}),
				Arguments.of("get throws", (Consumer<AsyncCache<Object>>) cache -> {
					doThrow(new IllegalStateException("cache down")).when(cache).get(any(), eq(CachedDecision.class));
					doReturn(stored).when(cache).put(any(), any());
				}),
				Arguments.of("put fails", (Consumer<AsyncCache<Object>>) cache -> {
					doReturn(miss).when(cache).get(any(), eq(CachedDecision.class));
					doReturn(failed).when(cache).put(any(), any());
				}),
				Arguments.of("put throws", (Consumer<AsyncCache<Object>>) cache -> {
					doReturn(miss).when(cache).get(any(), eq(CachedDecision.class));
					doThrow(new IllegalStateException("cache down")).when(cache).put(any(), any());
				}));
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("brokenCaches")
	@SuppressWarnings("unchecked")
	void aFailingCacheStillYieldsThePdpVerdict(String name, Consumer<AsyncCache<Object>> breakage) {
		CacheManager<Object> cacheManager = mock(CacheManager.class);
		SyncCache<Object> syncCache = mock(SyncCache.class);
		AsyncCache<Object> asyncCache = mock(AsyncCache.class);
		when(cacheManager.getCacheNames()).thenReturn(Set.of(DecisionCache.CACHE_NAME));
		when(cacheManager.getCache(DecisionCache.CACHE_NAME)).thenReturn(syncCache);
		when(syncCache.async()).thenReturn(asyncCache);
		breakage.accept(asyncCache);
		Map<String, Object> policy = subjectOnlyPolicy(POLICY_UID);
		stub.permit(POLICY_UID, IDENTITY);
		try (ApplicationContext context = context(false)) {
			DecisionCache cache = new DecisionCache(context.getBean(AccessPolicyProperties.class),
					context.getBean(PolicyDecisionPoint.class), cacheManager, clock);
			for (int round = 0; round < ROUNDS; round++) {
				assertEquals(AccessDecision.policy(true, null, POLICY_UID), decide(cache, policy, 0, subject()));
			}
		}
		assertEquals(ROUNDS, stub.requestCount());
	}

	@Test
	void decidesWithoutARequestContext() {
		Map<String, Object> policy = subjectOnlyPolicy(POLICY_UID);
		stub.permit(POLICY_UID, IDENTITY);
		try (ApplicationContext context = context(false)) {
			DecisionCache cache = context.getBean(DecisionCache.class);
			assertTrue(ServerRequestContext.currentRequest().isEmpty(), "precondition");
			// the bean runs on the system clock, so the token must outlive the real now
			Subject subject = subject(CREDENTIAL_A, Instant.now().plus(DECISION_TTL));
			IntStream.range(0, ROUNDS).forEach(round ->
					assertEquals(AccessDecision.policy(true, null, POLICY_UID), decide(cache, policy, 0, subject)));
		}
		assertEquals(1, stub.requestCount(), "The key comes from the parameters alone.");
	}

	@Test
	void anEnabledContextWithoutTheCacheFailsToStart() {
		Throwable thrown = assertThrows(Throwable.class, () -> {
			try (ApplicationContext context = ApplicationContext.run(enabledConfiguration(false))) {
				context.getBean(DecisionCache.class);
			}
		});
		boolean named = false;
		for (Throwable cause = thrown; cause != null; cause = cause.getCause()) {
			named |= cause instanceof ConfigurationException && cause.getMessage().contains(DecisionCache.CACHE_NAME);
		}
		assertTrue(named, "The startup should fail on the missing cache, but failed with: " + thrown);
	}

	/** A clock the test moves by hand. */
	private static final class SettableClock extends Clock {

		private volatile Instant now;

		SettableClock(Instant now) {
			this.now = now;
		}

		void set(Instant now) {
			this.now = now;
		}

		@Override
		public ZoneId getZone() {
			return ZoneOffset.UTC;
		}

		@Override
		public Clock withZone(ZoneId zone) {
			throw new UnsupportedOperationException();
		}

		@Override
		public Instant instant() {
			return now;
		}
	}
}
