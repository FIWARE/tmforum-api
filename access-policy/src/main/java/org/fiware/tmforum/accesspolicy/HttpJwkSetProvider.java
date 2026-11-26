package org.fiware.tmforum.accesspolicy;

import com.nimbusds.jose.jwk.JWKSet;
import io.micronaut.context.annotation.Requires;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.MediaType;
import io.micronaut.http.client.HttpClient;
import io.micronaut.http.client.annotation.Client;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.text.ParseException;
import java.time.Duration;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

/**
 * Fetches the configured {@code jwt.jwks-uri} lazily and caches it. Once a key set was fetched, a failing endpoint
 * never takes it away: the last good set is served and the fetch retried every {@link #JWKS_MIN_REFRESH_INTERVAL}.
 */
@Slf4j
@Singleton
@Requires(property = AccessPolicyProperties.ENABLED_PROPERTY, value = "true")
public class HttpJwkSetProvider implements JwkSetProvider {

	static final Duration JWKS_REFRESH_INTERVAL = Duration.ofMinutes(5);
	/** Lifetime of a failure or a fallback, and the minimum distance between two fetches forced by {@link #refresh()}. */
	static final Duration JWKS_MIN_REFRESH_INTERVAL = Duration.ofSeconds(30);
	static final Duration JWKS_FETCH_TIMEOUT = Duration.ofSeconds(5);

	private static final String JWK_SET_MEDIA_TYPE = "application/jwk-set+json";
	private static final long NEVER_FETCHED = Long.MIN_VALUE;

	private final Supplier<Mono<String>> fetch;
	private final AtomicReference<JWKSet> lastGood = new AtomicReference<>();
	/** Scheduler time in millis of the last fetch, the clock {@link Mono#cache} measures its TTLs with. */
	private final AtomicLong lastFetch = new AtomicLong(NEVER_FETCHED);
	private final AtomicReference<Mono<JWKSet>> cache;

	@Inject
	public HttpJwkSetProvider(AccessPolicyProperties properties, @Client HttpClient httpClient) {
		this(() -> Mono.from(httpClient.retrieve(
				HttpRequest.GET(properties.getJwt().getJwksUri())
						.accept(MediaType.APPLICATION_JSON_TYPE, MediaType.of(JWK_SET_MEDIA_TYPE)),
				String.class)));
	}

	HttpJwkSetProvider(Supplier<Mono<String>> fetch) {
		this.fetch = fetch;
		this.cache = new AtomicReference<>(newCache());
	}

	@Override
	public Mono<JWKSet> current() {
		return cache.get();
	}

	@Override
	public Mono<JWKSet> refresh() {
		Mono<JWKSet> seen = cache.get();
		long last = lastFetch.get();
		if (last != NEVER_FETCHED && now() - last < JWKS_MIN_REFRESH_INTERVAL.toMillis()) {
			return seen;
		}
		Mono<JWKSet> fresh = newCache();
		if (!cache.compareAndSet(seen, fresh)) {
			return cache.get();
		}
		lastFetch.set(now());
		return fresh;
	}

	private Mono<JWKSet> newCache() {
		return Mono.defer(this::fetchOnce)
				.cache(Fetched::ttl, error -> JWKS_MIN_REFRESH_INTERVAL, () -> JWKS_MIN_REFRESH_INTERVAL)
				.map(Fetched::keys);
	}

	private Mono<Fetched> fetchOnce() {
		lastFetch.set(now());
		return Mono.defer(fetch)
				.timeout(JWKS_FETCH_TIMEOUT)
				.map(body -> {
					try {
						return JWKSet.parse(body);
					} catch (ParseException e) {
						throw new IllegalStateException("The JWKS response is not a JWK set.", e);
					}
				})
				.doOnNext(lastGood::set)
				.map(keys -> new Fetched(keys, JWKS_REFRESH_INTERVAL))
				.onErrorResume(this::fallback);
	}

	private Mono<Fetched> fallback(Throwable error) {
		JWKSet previous = lastGood.get();
		if (previous == null) {
			log.warn("Fetching the JWKS failed with {} and no key set was fetched before.", error.getClass().getName());
			return Mono.error(error);
		}
		log.warn("Fetching the JWKS failed with {}, keeping the last fetched key set.", error.getClass().getName());
		return Mono.just(new Fetched(previous, JWKS_MIN_REFRESH_INTERVAL));
	}

	private static long now() {
		return Schedulers.parallel().now(TimeUnit.MILLISECONDS);
	}

	private record Fetched(JWKSet keys, Duration ttl) {
	}
}
