package org.fiware.tmforum.storage.postgresql;

import io.micronaut.context.annotation.ConfigurationProperties;
import io.micronaut.core.bind.annotation.Bindable;

import java.time.Duration;

/**
 * Settings of {@link OutboxDispatcher} ({@code tmforum.outbox.*}).
 * <p>
 * Delivery is at least once: a row is deleted only after every hub acknowledged it, and a partly failed row is
 * redelivered to all of its hubs with the same {@code eventId}, so consumers deduplicate on it. There is no
 * per-entity order, each row backs off on its own; consumers order by {@code eventTime}, the row's creation time.
 * {@code lease} must exceed the time one batch can take (HTTP client read timeout x callbacks per batch), or another
 * instance claims the rows again while they are still being delivered.
 */
@ConfigurationProperties(OutboxProperties.PREFIX)
public record OutboxProperties(
        @Bindable(defaultValue = "true") boolean enabled,
        @Bindable(defaultValue = OutboxProperties.DEFAULT_POLL_INTERVAL) Duration pollInterval,
        @Bindable(defaultValue = OutboxProperties.DEFAULT_BATCH_SIZE) int batchSize,
        @Bindable(defaultValue = OutboxProperties.DEFAULT_LEASE) Duration lease,
        @Bindable(defaultValue = OutboxProperties.DEFAULT_MAX_BACKOFF) Duration maxBackoff,
        @Bindable(defaultValue = OutboxProperties.DEFAULT_RETENTION) Duration retention,
        @Bindable(defaultValue = OutboxProperties.DEFAULT_HOUSEKEEPING_INTERVAL) Duration housekeepingInterval) {

    public static final String PREFIX = "tmforum.outbox";
    public static final String ENABLED_PROPERTY = PREFIX + ".enabled";
    static final String DEFAULT_POLL_INTERVAL = "1s";
    static final String DEFAULT_BATCH_SIZE = "20";
    static final String DEFAULT_LEASE = "5m";
    static final String DEFAULT_MAX_BACKOFF = "1h";
    static final String DEFAULT_RETENTION = "7d";
    static final String DEFAULT_HOUSEKEEPING_INTERVAL = "1m";
}
