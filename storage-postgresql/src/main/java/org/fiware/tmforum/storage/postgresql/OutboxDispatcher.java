package org.fiware.tmforum.storage.postgresql;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.composite.CompositeMeterRegistry;
import io.micronaut.context.annotation.Requires;
import io.micronaut.core.annotation.Nullable;
import io.micronaut.scheduling.annotation.Scheduled;
import io.r2dbc.spi.ConnectionFactory;
import io.r2dbc.spi.Readable;
import io.r2dbc.spi.Result;
import io.r2dbc.spi.Statement;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.fiware.tmforum.common.mapping.EventMapping;
import org.fiware.tmforum.common.notification.EventMapper;
import org.fiware.tmforum.common.notification.EventTypes;
import org.fiware.tmforum.common.notification.TMForumEventHandler;
import org.fiware.tmforum.storage.StorageOperation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;

import static org.fiware.tmforum.storage.postgresql.PostgresStorage.QUALIFIED_OUTBOX_TABLE;

/**
 * Delivers the outbox rows of this deployment's {@link EventTypes} through {@link TMForumEventHandler} and keeps the
 * outbox bounded. A claimed row is leased, not locked, so no transaction is open while hubs are called; see
 * {@link OutboxProperties} for the delivery guarantees.
 */
@Singleton
@Requires(env = PostgresStorage.ENVIRONMENT)
@Requires(property = OutboxProperties.ENABLED_PROPERTY, notEquals = "false")
public class OutboxDispatcher {

    private static final Logger LOGGER = LoggerFactory.getLogger(OutboxDispatcher.class);

    static final String DISCARDED_METRIC = "tmforum.outbox.discarded";
    static final String PENDING_METRIC = "tmforum.outbox.pending";
    static final String OLDEST_AGE_METRIC = "tmforum.outbox.oldest.age";
    static final String ENTITY_TYPE_TAG = "entity_type";
    static final String EVENT_KIND_TAG = "event_kind";

    private static final String ID = "id";
    private static final String EVENT_KIND = "event_kind";
    private static final String ENTITY_ID = "entity_id";
    private static final String ENTITY_TYPE = "entity_type";
    private static final String NEW_STATE = "new_state";
    private static final String OLD_STATE = "old_state";
    private static final String CREATED_AT = "created_at";
    private static final String ATTEMPTS = "attempts";
    private static final String PENDING = "pending";
    private static final String OLDEST_AGE = "oldest_age";
    private static final String MILLISECONDS = " * interval '1 millisecond'";

    private static final String CLAIM = "UPDATE " + QUALIFIED_OUTBOX_TABLE
            + " SET attempts = attempts + 1, next_attempt_at = now() + $1::double precision" + MILLISECONDS
            + " WHERE id IN (SELECT id FROM " + QUALIFIED_OUTBOX_TABLE
            + " WHERE next_attempt_at <= now() AND entity_type = ANY($2) ORDER BY id FOR UPDATE SKIP LOCKED LIMIT $3)"
            + " RETURNING id, event_kind, entity_type, new_state::text AS new_state, old_state::text AS old_state,"
            + " created_at, attempts";
    private static final String SETTLE_DELIVERED = "DELETE FROM " + QUALIFIED_OUTBOX_TABLE + " WHERE id = $1";
    static final String SETTLE_FAILED = "UPDATE " + QUALIFIED_OUTBOX_TABLE
            + " SET next_attempt_at = now() + $2::double precision" + MILLISECONDS + " WHERE id = $1";
    private static final String DISCARD_EXPIRED = "DELETE FROM " + QUALIFIED_OUTBOX_TABLE
            + " WHERE entity_type = ANY($1) AND created_at < now() - $2::double precision" + MILLISECONDS
            + " RETURNING id, event_kind, entity_id, entity_type, attempts";
    private static final String STATISTICS = "SELECT count(*) AS pending,"
            + " COALESCE(EXTRACT(EPOCH FROM now() - min(created_at)), 0)::double precision AS oldest_age FROM "
            + QUALIFIED_OUTBOX_TABLE + " WHERE entity_type = ANY($1)";

    private final ConnectionFactory connectionFactory;
    private final DocumentMapper eventDocuments;
    private final EventTypes eventTypes;
    private final EventMapper eventMapper;
    private final TMForumEventHandler eventHandler;
    private final OutboxProperties properties;
    private final OutboxBackoff backoff;
    private final MeterRegistry meterRegistry;
    private final AtomicLong pending = new AtomicLong();
    private final AtomicLong oldestAgeSeconds = new AtomicLong();

    @Inject
    public OutboxDispatcher(ConnectionFactory connectionFactory, PostgresDocumentMapper documentMapper,
                            EventTypes eventTypes, EventMapper eventMapper, TMForumEventHandler eventHandler,
                            OutboxProperties properties, @Nullable MeterRegistry meterRegistry) {
        this.connectionFactory = connectionFactory;
        this.eventDocuments = documentMapper.event();
        this.eventTypes = eventTypes;
        this.eventMapper = eventMapper;
        this.eventHandler = eventHandler;
        this.properties = properties;
        this.backoff = new OutboxBackoff(properties.pollInterval(), properties.maxBackoff());
        // without metrics enabled, a registry without children drops every measurement
        this.meterRegistry = meterRegistry == null ? new CompositeMeterRegistry() : meterRegistry;
        Gauge.builder(PENDING_METRIC, pending, AtomicLong::get).register(this.meterRegistry);
        Gauge.builder(OLDEST_AGE_METRIC, oldestAgeSeconds, AtomicLong::get).baseUnit("seconds")
                .register(this.meterRegistry);
    }

    @Scheduled(fixedDelay = "${tmforum.outbox.poll-interval:" + OutboxProperties.DEFAULT_POLL_INTERVAL + "}")
    void poll() {
        dispatchOnce().onErrorResume(failure -> {
            LOGGER.warn("Was not able to dispatch the event outbox.", failure);
            return Mono.empty();
        }).block();
    }

    @Scheduled(fixedDelay = "${tmforum.outbox.housekeeping-interval:"
            + OutboxProperties.DEFAULT_HOUSEKEEPING_INTERVAL + "}")
    void scheduledHousekeeping() {
        housekeeping().onErrorResume(failure -> {
            LOGGER.warn("Was not able to clean up the event outbox.", failure);
            return Mono.empty();
        }).block();
    }

    /**
     * Claims one batch of due rows, delivers them concurrently and settles each: deleted when every hub acknowledged,
     * otherwise due again after {@link OutboxBackoff}. Emits the number of claimed rows.
     */
    public Mono<Integer> dispatchOnce() {
        return PostgresStatements.execute(connectionFactory, CLAIM, statement -> statement
                                .bind(0, (double) properties.lease().toMillis())
                                .bind(1, ownedTypes())
                                .bind(2, properties.batchSize()),
                        result -> result.map(ClaimedRow::of))
                // finish the claim, and release its connection, before any hub is called
                .collectList()
                .flatMapMany(Flux::fromIterable)
                // rows are independent and unordered, so a dead callback must not hold up the rest of the batch
                .flatMap(row -> deliverAndSettle(row).thenReturn(row), properties.batchSize())
                .count()
                .map(Math::toIntExact);
    }

    /**
     * Discards the owned rows older than {@code retention} without delivering them (WARN log, counter
     * {@value #DISCARDED_METRIC}), then refreshes the gauges {@value #PENDING_METRIC} and {@value #OLDEST_AGE_METRIC}.
     */
    public Mono<Void> housekeeping() {
        return PostgresStatements.execute(connectionFactory, DISCARD_EXPIRED, statement -> statement
                                .bind(0, ownedTypes())
                                .bind(1, (double) properties.retention().toMillis()),
                        result -> result.map(DiscardedRow::of))
                .doOnNext(this::reportDiscarded)
                .then(PostgresStatements.execute(connectionFactory, STATISTICS,
                                statement -> statement.bind(0, ownedTypes()),
                                result -> result.map(row -> {
                                    pending.set(row.get(PENDING, Long.class));
                                    oldestAgeSeconds.set(Math.round(row.get(OLDEST_AGE, Double.class)));
                                    return row;
                                }))
                        .then());
    }

    // a failing settle statement must not cancel the other rows of the batch; the lease covers its row
    private Mono<Void> deliverAndSettle(ClaimedRow row) {
        return deliver(row)
                .then(Mono.just(true))
                .onErrorResume(failure -> {
                    reportFailedDelivery(row, failure);
                    return Mono.just(false);
                })
                .flatMap(delivered -> delivered
                        ? settle(SETTLE_DELIVERED, statement -> statement.bind(0, row.id()))
                        : settle(SETTLE_FAILED, statement -> statement.bind(0, row.id())
                                .bind(1, (double) backoff.delay(row.attempts()).toMillis())))
                .onErrorResume(failure -> {
                    LOGGER.warn("Was not able to settle outbox row {}, it is due again once its lease of {} expired.",
                            row.id(), properties.lease(), failure);
                    return Mono.empty();
                });
    }

    private Mono<Void> deliver(ClaimedRow row) {
        return Mono.defer(() -> {
            Class<?> rawClass = rawClass(row.entityType());
            long id = row.id();
            return switch (row.kind()) {
                case CREATE -> eventHandler.handleCreateEvent(read(row.newState(), rawClass), id, row.createdAt());
                case UPDATE -> eventHandler.handleUpdateEvent(read(row.newState(), rawClass),
                        read(row.oldState(), rawClass), id, row.createdAt());
                case DELETE -> eventHandler.handleDeleteEvent(read(row.oldState(), rawClass), id, row.createdAt());
            };
        });
    }

    private Class<?> rawClass(String entityType) {
        EventMapping eventMapping = eventMapper.getEntityClassMapping().get(entityType);
        if (eventMapping == null) {
            throw new IllegalStateException(String.format("No event mapping for type %s.", entityType));
        }
        return eventMapping.rawClass();
    }

    private Object read(String document, Class<?> rawClass) {
        return eventDocuments.fromDocument(document, rawClass, StorageOperation.READ);
    }

    private Mono<Void> settle(String sql, Function<Statement, Statement> binder) {
        return PostgresStatements.execute(connectionFactory, sql, binder, Result::getRowsUpdated).then();
    }

    // every row fails until PG can look up hubs (ticket 7), so only a row reaching max-backoff gets one WARN
    private void reportFailedDelivery(ClaimedRow row, Throwable failure) {
        if (backoff.firstAtMaximum(row.attempts())) {
            LOGGER.warn("Delivery of outbox row {} ({} of {}) failed {} times, retrying every {} until it is older"
                            + " than the retention of {}.", row.id(), row.kind(), row.entityType(), row.attempts(),
                    properties.maxBackoff(), properties.retention(), failure);
        } else {
            LOGGER.debug("Delivery of outbox row {} failed, retrying later.", row.id(), failure);
        }
    }

    private void reportDiscarded(DiscardedRow row) {
        LOGGER.warn("Discarded outbox row {} ({} of {} {}) after {} attempts, it is older than the retention of {}.",
                row.id(), row.kind(), row.entityType(), row.entityId(), row.attempts(), properties.retention());
        Counter.builder(DISCARDED_METRIC)
                .tag(ENTITY_TYPE_TAG, row.entityType())
                .tag(EVENT_KIND_TAG, row.kind())
                .register(meterRegistry)
                .increment();
    }

    private String[] ownedTypes() {
        return eventTypes.types().toArray(String[]::new);
    }

    private record ClaimedRow(long id, EventKind kind, String entityType, String newState, String oldState,
                              Instant createdAt, int attempts) {

        static ClaimedRow of(Readable row) {
            return new ClaimedRow(row.get(ID, Long.class), EventKind.valueOf(row.get(EVENT_KIND, String.class)),
                    row.get(ENTITY_TYPE, String.class), row.get(NEW_STATE, String.class),
                    row.get(OLD_STATE, String.class), row.get(CREATED_AT, OffsetDateTime.class).toInstant(),
                    row.get(ATTEMPTS, Integer.class));
        }
    }

    private record DiscardedRow(long id, String kind, String entityId, String entityType, int attempts) {

        static DiscardedRow of(Readable row) {
            return new DiscardedRow(row.get(ID, Long.class), row.get(EVENT_KIND, String.class),
                    row.get(ENTITY_ID, String.class), row.get(ENTITY_TYPE, String.class),
                    row.get(ATTEMPTS, Integer.class));
        }
    }
}
