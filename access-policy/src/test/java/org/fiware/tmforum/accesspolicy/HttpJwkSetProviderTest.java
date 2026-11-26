package org.fiware.tmforum.accesspolicy;

import com.nimbusds.jose.jwk.JWKSet;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import reactor.test.scheduler.VirtualTimeScheduler;

import java.io.IOException;
import java.time.Duration;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static org.fiware.tmforum.accesspolicy.HttpJwkSetProvider.JWKS_MIN_REFRESH_INTERVAL;
import static org.fiware.tmforum.accesspolicy.HttpJwkSetProvider.JWKS_REFRESH_INTERVAL;
import static org.fiware.tmforum.accesspolicy.TestTokens.CLOCK;
import static org.fiware.tmforum.accesspolicy.TestTokens.EC_KEY;
import static org.fiware.tmforum.accesspolicy.TestTokens.RSA_KEY;
import static org.fiware.tmforum.accesspolicy.TestTokens.VC_ISSUER;
import static org.fiware.tmforum.accesspolicy.TestTokens.claims;
import static org.fiware.tmforum.accesspolicy.TestTokens.properties;
import static org.fiware.tmforum.accesspolicy.TestTokens.publicSet;
import static org.fiware.tmforum.accesspolicy.TestTokens.sign;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Tests the JWKS cache on virtual time: reuse, rate-limited refresh and the fallback to the last good key set.
 */
class HttpJwkSetProviderTest {

	private static final int CONCURRENT_REFRESHES = 32;
	private static final Duration PAST_MIN_REFRESH = JWKS_MIN_REFRESH_INTERVAL.plusSeconds(1);
	private static final Duration PAST_REFRESH = JWKS_REFRESH_INTERVAL.plusSeconds(1);
	private static final String RSA_SET = publicSet(RSA_KEY).toString();
	private static final String EC_SET = publicSet(EC_KEY).toString();

	private VirtualTimeScheduler virtualTime;

	@BeforeEach
	void installVirtualTime() {
		virtualTime = VirtualTimeScheduler.getOrSet();
	}

	@AfterEach
	void resetVirtualTime() {
		VirtualTimeScheduler.reset();
	}

	/**
	 * Fetch stub answering the given responses in order, then failing; {@link Mono#cache} only reads time
	 * through {@code Schedulers.parallel()}, so it must be built after the virtual scheduler is installed.
	 */
	private static final class ScriptedFetch implements Supplier<Mono<String>> {

		private final Deque<Mono<String>> responses;
		private final AtomicInteger fetches = new AtomicInteger();

		@SafeVarargs
		ScriptedFetch(Mono<String>... responses) {
			this.responses = new ArrayDeque<>(List.of(responses));
		}

		@Override
		public Mono<String> get() {
			fetches.incrementAndGet();
			Mono<String> next = responses.poll();
			return next == null ? down() : next;
		}

		int fetches() {
			return fetches.get();
		}
	}

	private static Mono<String> down() {
		return Mono.error(new IOException("JWKS endpoint down"));
	}

	private static String keyIdOf(JWKSet keys) {
		return keys.getKeys().getFirst().getKeyID();
	}

	@Test
	void reusesTheCachedSet() {
		ScriptedFetch fetch = new ScriptedFetch(Mono.just(RSA_SET));
		HttpJwkSetProvider provider = new HttpJwkSetProvider(fetch);

		StepVerifier.create(provider.current().map(HttpJwkSetProviderTest::keyIdOf))
				.expectNext(RSA_KEY.getKeyID()).verifyComplete();
		virtualTime.advanceTimeBy(JWKS_REFRESH_INTERVAL.minusSeconds(1));
		StepVerifier.create(provider.current().map(HttpJwkSetProviderTest::keyIdOf))
				.expectNext(RSA_KEY.getKeyID()).verifyComplete();

		assertEquals(1, fetch.fetches());
	}

	@Test
	void refetchesAfterTheRefreshInterval() {
		ScriptedFetch fetch = new ScriptedFetch(Mono.just(RSA_SET), Mono.just(EC_SET));
		HttpJwkSetProvider provider = new HttpJwkSetProvider(fetch);

		StepVerifier.create(provider.current()).expectNextCount(1).verifyComplete();
		virtualTime.advanceTimeBy(PAST_REFRESH);

		StepVerifier.create(provider.current().map(HttpJwkSetProviderTest::keyIdOf))
				.expectNext(EC_KEY.getKeyID()).verifyComplete();
		assertEquals(2, fetch.fetches());
	}

	@Test
	void rateLimitsRefresh() {
		ScriptedFetch fetch = new ScriptedFetch(Mono.just(RSA_SET), Mono.just(EC_SET));
		HttpJwkSetProvider provider = new HttpJwkSetProvider(fetch);

		StepVerifier.create(provider.current()).expectNextCount(1).verifyComplete();
		StepVerifier.create(provider.refresh().map(HttpJwkSetProviderTest::keyIdOf))
				.expectNext(RSA_KEY.getKeyID()).verifyComplete();
		assertEquals(1, fetch.fetches());

		virtualTime.advanceTimeBy(PAST_MIN_REFRESH);
		StepVerifier.create(provider.refresh().map(HttpJwkSetProviderTest::keyIdOf))
				.expectNext(EC_KEY.getKeyID()).verifyComplete();
		StepVerifier.create(provider.refresh().map(HttpJwkSetProviderTest::keyIdOf))
				.expectNext(EC_KEY.getKeyID()).verifyComplete();
		assertEquals(2, fetch.fetches());
	}

	private static Stream<Arguments> failedRefetches() {
		return Stream.of(
				Arguments.of("unparsable body", Mono.just("not a JWK set")),
				Arguments.of("empty response", Mono.<String>empty()),
				Arguments.of("endpoint down", down()));
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("failedRefetches")
	void keepsTheLastGoodSetWhenARefreshFails(String failure, Mono<String> refetch) {
		ScriptedFetch fetch = new ScriptedFetch(Mono.just(RSA_SET), refetch);
		HttpJwkSetProvider provider = new HttpJwkSetProvider(fetch);

		StepVerifier.create(provider.current()).expectNextCount(1).verifyComplete();
		virtualTime.advanceTimeBy(PAST_MIN_REFRESH);

		StepVerifier.create(provider.refresh().map(HttpJwkSetProviderTest::keyIdOf))
				.expectNext(RSA_KEY.getKeyID()).verifyComplete();
		assertEquals(2, fetch.fetches());
	}

	@Test
	void concurrentRefreshesFetchOnce() throws InterruptedException {
		ScriptedFetch fetch = new ScriptedFetch(Mono.just(RSA_SET), Mono.just(EC_SET), Mono.just(EC_SET));
		HttpJwkSetProvider provider = new HttpJwkSetProvider(fetch);
		StepVerifier.create(provider.current()).expectNextCount(1).verifyComplete();
		virtualTime.advanceTimeBy(PAST_MIN_REFRESH);

		CountDownLatch start = new CountDownLatch(1);
		List<Thread> callers = IntStream.range(0, CONCURRENT_REFRESHES)
				.mapToObj(i -> Thread.ofPlatform().start(() -> {
					try {
						start.await();
					} catch (InterruptedException e) {
						Thread.currentThread().interrupt();
					}
					provider.refresh().subscribe();
				}))
				.toList();
		start.countDown();
		for (Thread caller : callers) {
			caller.join();
		}

		assertEquals(2, fetch.fetches(), "only one refresh may pass the rate limit");
	}

	@Test
	void errorsWhileNoSetWasEverFetched() {
		ScriptedFetch fetch = new ScriptedFetch();
		HttpJwkSetProvider provider = new HttpJwkSetProvider(fetch);

		StepVerifier.create(provider.current()).expectError(IOException.class).verify();
		StepVerifier.create(provider.current()).expectError(IOException.class).verify();
		assertEquals(1, fetch.fetches(), "a failure is cached, not retried per request");

		virtualTime.advanceTimeBy(PAST_MIN_REFRESH);
		StepVerifier.create(provider.current()).expectError(IOException.class).verify();
		assertEquals(2, fetch.fetches());
	}

	@Test
	void validatesWithTheOldKeyWhenTheRefetchFails() {
		ScriptedFetch fetch = new ScriptedFetch(Mono.just(RSA_SET));
		HttpJwkSetProvider provider = new HttpJwkSetProvider(fetch);
		BearerTokenValidator validator = new BearerTokenValidator(properties(), provider, CLOCK);
		String token = sign(RSA_KEY, claims().build());

		StepVerifier.create(validator.validate(token))
				.expectNextMatches(subject -> VC_ISSUER.equals(subject.identity())).verifyComplete();
		virtualTime.advanceTimeBy(PAST_REFRESH);

		StepVerifier.create(validator.validate(token))
				.expectNextMatches(subject -> VC_ISSUER.equals(subject.identity())).verifyComplete();
		assertEquals(2, fetch.fetches());

		virtualTime.advanceTimeBy(PAST_MIN_REFRESH);
		StepVerifier.create(validator.validate(token))
				.expectNextMatches(subject -> VC_ISSUER.equals(subject.identity())).verifyComplete();
		assertEquals(3, fetch.fetches(), "the fallback is kept only for the minimum refresh interval");
	}
}
