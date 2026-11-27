package org.fiware.tmforum.accesspolicy;

import io.micronaut.cache.AsyncCache;
import io.micronaut.cache.CacheManager;
import io.micronaut.context.annotation.Context;
import io.micronaut.context.annotation.Requires;
import io.micronaut.context.exceptions.ConfigurationException;
import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import org.fiware.tmforum.accesspolicy.AccessDecision.Source;
import reactor.core.publisher.Mono;

import java.time.Clock;
import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

/**
 * The {@value #CACHE_NAME} cache in front of {@link PolicyDecisionPoint}, keyed by {@link DecisionKey}. Eager, so a
 * deployment without a {@value #CACHE_NAME} cache fails at startup instead of on the first filtered request.
 */
@Slf4j
@Context
@Requires(property = AccessPolicyProperties.ENABLED_PROPERTY, value = "true")
public class DecisionCache {

	public static final String CACHE_NAME = "decisions";
	/**
	 * The {@code redis.caches.decisions.value-serializer} every redis profile names: JSON through the app's mapper.
	 * A name that resolves to no bean silently falls back to Java deserialization of Redis data, so copy it exactly.
	 */
	public static final String VALUE_SERIALIZER = "io.micronaut.jackson.serialize.JacksonObjectSerializer";
	private static final String NO_CACHE_CONFIGURED = "No cache configured for name: ";

	private final AccessPolicyProperties properties;
	private final PolicyDecisionPoint policyDecisionPoint;
	private final AsyncCache<?> cache;
	private final Clock clock;
	// concurrent misses on one key share a single PDP call
	private final Map<DecisionKey, Mono<AccessDecision>> inFlight = new ConcurrentHashMap<>();

	/** Takes the guard first, so an invalid configuration fails on its message before the PDP client is built. */
	@Inject
	public DecisionCache(AccessPolicyConfigurationGuard guard, AccessPolicyProperties properties,
			PolicyDecisionPoint policyDecisionPoint, CacheManager<?> cacheManager) {
		this(properties, policyDecisionPoint, cacheManager, Clock.systemUTC());
	}

	DecisionCache(AccessPolicyProperties properties, PolicyDecisionPoint policyDecisionPoint,
			CacheManager<?> cacheManager, Clock clock) {
		this.properties = properties;
		this.policyDecisionPoint = policyDecisionPoint;
		// Caffeine's dynamic cache manager would otherwise create an unconfigured cache silently
		if (!cacheManager.getCacheNames().contains(CACHE_NAME)) {
			throw new ConfigurationException(NO_CACHE_CONFIGURED + CACHE_NAME);
		}
		this.cache = cacheManager.getCache(CACHE_NAME).async();
		this.clock = clock;
	}

	/**
	 * The PDP verdict, served from the cache for {@code min(token exp, decision-ttl)} after the call that produced it.
	 * Never errors or completes empty, like the PDP client: only a policy verdict is cached and a failing cache is a miss.
	 */
	public Mono<AccessDecision> decide(String policyUid, Map<String, Object> policy, String scope, String action,
			ValidationRequest.Resource resource, Subject subject) {
		Objects.requireNonNull(subject, "subject");
		Mono<AccessDecision> askPdp = Mono.defer(() -> policyDecisionPoint.decide(policyUid,
				ValidationRequest.of(policy, scope, action, resource, subject)));
		DecisionKey key;
		try {
			key = DecisionKey.of(policyUid, policy, scope, action, resource, subject);
		} catch (RuntimeException e) {
			log.warn("No decision cache key for policy {}, asking the PDP uncached: {}", policyUid, e.toString());
			return askPdp;
		}
		return inFlight.computeIfAbsent(key, k -> coalesced(k, lookup(k, policyUid, subject, askPdp)));
	}

	private Mono<AccessDecision> coalesced(DecisionKey key, Mono<AccessDecision> lookup) {
		AtomicReference<Mono<AccessDecision>> self = new AtomicReference<>();
		// two-argument remove, so a late finish never drops a newer lookup of the same key
		Mono<AccessDecision> shared = lookup.doFinally(signal -> inFlight.remove(key, self.get())).cache();
		self.set(shared);
		return shared;
	}

	private Mono<AccessDecision> lookup(DecisionKey key, String policyUid, Subject subject, Mono<AccessDecision> askPdp) {
		return cached(key, policyUid)
				.filter(entry -> entry.isValidAt(clock.instant()))
				.map(CachedDecision::decision)
				.switchIfEmpty(Mono.defer(() -> {
					// taken before the call, so the staleness bound also covers the PDP's answer time
					Instant asked = clock.instant();
					return askPdp.flatMap(decision -> store(key, decision, asked, subject).thenReturn(decision));
				}));
	}

	private Mono<CachedDecision> cached(DecisionKey key, String policyUid) {
		return Mono.defer(() -> Mono.fromFuture(cache.get(key, CachedDecision.class)))
				.flatMap(Mono::justOrEmpty)
				.onErrorResume(error -> {
					log.warn("Reading the decision cache for policy {} failed, asking the PDP: {}", policyUid,
							error.toString());
					return Mono.empty();
				});
	}

	// only a PDP verdict is cached: under fail-open a cached failure would be a grant that outlives a revocation
	private Mono<Void> store(DecisionKey key, AccessDecision decision, Instant asked, Subject subject) {
		Instant validUntil = validUntil(asked, subject);
		if (decision.source() != Source.POLICY || !validUntil.isAfter(asked)) {
			return Mono.empty();
		}
		CachedDecision entry = new CachedDecision(decision, validUntil.toEpochMilli());
		return Mono.defer(() -> Mono.fromFuture(cache.put(key, entry)))
				.onErrorResume(error -> {
					log.warn("Writing the decision cache for policy {} failed: {}", decision.policyUid(),
							error.toString());
					return Mono.empty();
				})
				.then();
	}

	private Instant validUntil(Instant asked, Subject subject) {
		Instant ttlBound = asked.plus(properties.getDecisionTtl());
		Instant tokenExpiry = subject.expiresAt();
		return tokenExpiry != null && tokenExpiry.isBefore(ttlBound) ? tokenExpiry : ttlBound;
	}
}
