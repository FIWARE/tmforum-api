package org.fiware.tmforum.storage.postgresql;

import java.time.Duration;

/**
 * Delay before an outbox row is retried: {@code base * 2^min(attempts, MAX_BACKOFF_EXPONENT)}, at most {@code max}.
 * {@code attempts} is unbounded, so the exponent is capped and the delay never overflows.
 */
final class OutboxBackoff {

    // 2^20 times the poll interval is already days; no Duration or PostgreSQL interval overflows below it
    static final int MAX_BACKOFF_EXPONENT = 20;

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
}
