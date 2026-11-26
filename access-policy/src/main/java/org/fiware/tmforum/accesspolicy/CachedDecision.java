package org.fiware.tmforum.accesspolicy;

import java.time.Instant;

/**
 * A cached PDP verdict and the instant it stops answering, {@code min(token exp, decision-ttl)}. Plain components, so
 * Jackson binds it for the Redis backend.
 */
public record CachedDecision(AccessDecision decision, long validUntilEpochMilli) {

	/** False from {@code validUntilEpochMilli} on, so an entry never answers at its own bound. */
	public boolean isValidAt(Instant now) {
		return now.toEpochMilli() < validUntilEpochMilli;
	}
}
