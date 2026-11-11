package org.fiware.tmforum.storage.postgresql;

import java.time.Duration;

/**
 * Delay before an outbox row is retried: {@code base * 2^min(attempts, MAX_BACKOFF_EXPONENT)}, at most {@code max}.
 * {@code attempts} is unbounded, so the exponent is capped and the delay never overflows.
 */
final class OutboxBackoff {

    // 2^20 times the poll interval is already days; no Duration or PostgreSQL interval overflows below it
    static final int MAX_BACKOFF_EXPONENT = 20;
    // the claim counts an attempt before delivering, so a failed row has at least this many
    static final int FIRST_ATTEMPT = 1;

    private final Duration base;
    private final Duration max;

    OutboxBackoff(Duration base, Duration max) {
        this.base = base;
        this.max = max;
    }

    Duration delay(int attempts) {
        int exponent = Math.clamp(attempts, 0, MAX_BACKOFF_EXPONENT);
        try {
            Duration delay = base.multipliedBy(1L << exponent);
            return delay.compareTo(max) > 0 ? max : delay;
        } catch (ArithmeticException overflow) {
            return max;
        }
    }

    /** The delay every retry ends up at: {@code max}, or the capped exponent's delay when that is shorter. */
    Duration longestDelay() {
        return delay(Integer.MAX_VALUE);
    }

    /**
     * True for exactly one attempt count {@code >= FIRST_ATTEMPT}: the first whose delay is {@link #longestDelay()},
     * whichever of {@code max} or the exponent cap bounds it.
     */
    boolean firstAtLongestDelay(int attempts) {
        Duration longest = longestDelay();
        return attempts >= FIRST_ATTEMPT && delay(attempts).equals(longest)
                && (attempts == FIRST_ATTEMPT || !delay(attempts - 1).equals(longest));
    }
}
