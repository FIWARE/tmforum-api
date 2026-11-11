package org.fiware.tmforum.storage.postgresql;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.time.Duration;
import java.util.stream.Stream;

import static org.fiware.tmforum.storage.postgresql.OutboxBackoff.FIRST_ATTEMPT;
import static org.fiware.tmforum.storage.postgresql.OutboxBackoff.MAX_BACKOFF_EXPONENT;
import static org.junit.jupiter.api.Assertions.assertEquals;

class OutboxBackoffTest {

    private static final Duration BASE = Duration.ofSeconds(1);
    private static final Duration MAX = Duration.ofHours(1);
    // large enough that only the exponent cap limits the delay
    private static final Duration UNLIMITED = Duration.ofSeconds(Long.MAX_VALUE);
    private static final Duration HUGE_BASE = Duration.ofSeconds(Long.MAX_VALUE / 2);
    private static final int ONE_HOUR_EXCEEDED = 12;
    private static final int BELOW_ONE_HOUR = ONE_HOUR_EXCEEDED - 1;

    static Stream<Arguments> delays() {
        return Stream.of(
                Arguments.of(BASE, MAX, 0, BASE),
                Arguments.of(BASE, MAX, 1, BASE.multipliedBy(2)),
                Arguments.of(BASE, MAX, ONE_HOUR_EXCEEDED, MAX),
                Arguments.of(BASE, UNLIMITED, MAX_BACKOFF_EXPONENT - 1, BASE.multipliedBy(1L << (MAX_BACKOFF_EXPONENT - 1))),
                Arguments.of(BASE, UNLIMITED, MAX_BACKOFF_EXPONENT, BASE.multipliedBy(1L << MAX_BACKOFF_EXPONENT)),
                Arguments.of(BASE, UNLIMITED, 100, BASE.multipliedBy(1L << MAX_BACKOFF_EXPONENT)),
                Arguments.of(BASE, MAX, 100, MAX),
                Arguments.of(BASE, MAX, 10_000, MAX),
                Arguments.of(BASE, MAX, Integer.MAX_VALUE, MAX),
                Arguments.of(HUGE_BASE, UNLIMITED, Integer.MAX_VALUE, UNLIMITED));
    }

    @ParameterizedTest
    @MethodSource("delays")
    void delayGrowsExponentiallyUpToTheMaximum(Duration base, Duration max, int attempts, Duration expected) {
        assertEquals(expected, new OutboxBackoff(base, max).delay(attempts),
                "The delay should be base * 2^min(attempts, cap), at most the maximum.");
    }

    static Stream<Arguments> longestDelayReached() {
        return Stream.of(
                Arguments.of(BASE, MAX, 0, false),
                Arguments.of(BASE, MAX, FIRST_ATTEMPT, false),
                Arguments.of(BASE, MAX, BELOW_ONE_HOUR, false),
                Arguments.of(BASE, MAX, ONE_HOUR_EXCEEDED, true),
                Arguments.of(BASE, MAX, ONE_HOUR_EXCEEDED + 1, false),
                Arguments.of(BASE, MAX, Integer.MAX_VALUE, false),
                // max-backoff beyond the exponent cap: the capped delay is the longest one
                Arguments.of(BASE, UNLIMITED, MAX_BACKOFF_EXPONENT - 1, false),
                Arguments.of(BASE, UNLIMITED, MAX_BACKOFF_EXPONENT, true),
                Arguments.of(BASE, UNLIMITED, MAX_BACKOFF_EXPONENT + 1, false),
                // poll interval at or above max-backoff: every delay is the longest one, the first attempt reports it
                Arguments.of(MAX, MAX, FIRST_ATTEMPT, true),
                Arguments.of(MAX, MAX, FIRST_ATTEMPT + 1, false),
                Arguments.of(MAX.multipliedBy(2), MAX, FIRST_ATTEMPT, true),
                Arguments.of(MAX.multipliedBy(2), MAX, FIRST_ATTEMPT + 1, false));
    }

    @ParameterizedTest
    @MethodSource("longestDelayReached")
    void firstAtLongestDelayHoldsForExactlyOneAttemptCount(Duration base, Duration max, int attempts,
                                                           boolean expected) {
        assertEquals(expected, new OutboxBackoff(base, max).firstAtLongestDelay(attempts),
                "Only the first attempt count whose delay is the longest one should report it.");
    }
}
