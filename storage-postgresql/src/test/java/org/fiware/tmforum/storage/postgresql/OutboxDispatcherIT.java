package org.fiware.tmforum.storage.postgresql;

import io.github.wistefan.mapping.UnmappedProperty;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import io.r2dbc.spi.ConnectionFactory;
import jakarta.inject.Inject;
import org.fiware.tmforum.common.domain.Money;
import org.fiware.tmforum.common.notification.EventMapper;
import org.fiware.tmforum.common.notification.EventTypes;
import org.fiware.tmforum.common.notification.TMForumEventHandler;
import org.fiware.tmforum.storage.postgresql.DocumentTestEntities.TestCatalog;
import org.fiware.tmforum.storage.postgresql.DocumentTestEntities.TestNotStoredHolder;
import org.fiware.tmforum.storage.postgresql.DocumentTestEntities.TestResource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;

import java.net.URI;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

import static org.fiware.tmforum.storage.postgresql.PostgresStorage.QUALIFIED_OUTBOX_TABLE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Claim, delivery, settlement and housekeeping of {@link OutboxDispatcher} against the k3s PostgreSQL, driven
 * through {@link OutboxDispatcher#dispatchOnce()} with a mocked {@link TMForumEventHandler}.
 */
@MicronautTest(environments = PostgresStorage.ENVIRONMENT)
class OutboxDispatcherIT {

    private static final String CATALOG_ID = "urn:ngsi-ld:" + TestCatalog.TYPE + ":catalog-1";
    private static final String OTHER_CATALOG_ID = "urn:ngsi-ld:" + TestCatalog.TYPE + ":catalog-2";
    private static final String HOLDER_ID = "urn:ngsi-ld:" + TestNotStoredHolder.TYPE + ":holder-1";
    private static final String RESOURCE_ID = "urn:ngsi-ld:" + TestResource.TYPE + ":resource-1";
    private static final String NAME = "catalog name";
    private static final String UPDATED_NAME = "updated catalog name";
    private static final String EXTENSION_KEY = "customExtension";
    private static final String EXTENSION_VALUE = "custom";
    private static final String CURRENCY = "EUR";
    private static final float AMOUNT = 1.5f;
    private static final Set<String> CATALOGS = Set.of(TestCatalog.TYPE);
    private static final Set<String> HOLDERS = Set.of(TestNotStoredHolder.TYPE);
    private static final Set<String> ALL_TYPES = Set.of(TestCatalog.TYPE, TestNotStoredHolder.TYPE);
    private static final EventMapper EVENT_MAPPER = OutboxTestSupport.eventMapper(Map.of(
            TestCatalog.TYPE, TestCatalog.class, TestNotStoredHolder.TYPE, TestNotStoredHolder.class));
    private static final Duration RETENTION = OutboxTestSupport.PROPERTIES.retention();
    private static final Duration YOUNGER_THAN_RETENTION = Duration.ofDays(1);
    private static final Duration CONCURRENT_DELIVERY_TIMEOUT = Duration.ofSeconds(10);
    // the gauge is read a moment after the rows were inserted
    private static final long AGE_TOLERANCE_SECONDS = 60;

    @Inject
    private PostgresEntityRepository repository;

    @Inject
    private PostgresDocumentMapper documentMapper;

    @Inject
    private ConnectionFactory connectionFactory;

    private OutboxEntityEventPublisher publisher;
    private TMForumEventHandler eventHandler;

    @BeforeEach
    void setUp() {
        OutboxTestSupport.cleanUp(connectionFactory);
        publisher = new OutboxEntityEventPublisher(repository, new EventTypes(ALL_TYPES));
        eventHandler = handler(Mono.empty());
    }

    @ParameterizedTest
    @EnumSource(EventKind.class)
    void deliveredRowIsDeleted(EventKind kind) {
        writeWithEvent(kind);

        assertEquals(1, dispatcher(CATALOGS, eventHandler).dispatchOnce().block(), "The row should be claimed.");

        verifyDelivered(kind);
        assertEquals(0, outboxRows(CATALOG_ID), "A delivered row should be deleted.");
    }

    @ParameterizedTest
    @EnumSource(EventKind.class)
    void failedRowIsRetriedLater(EventKind kind) {
        writeWithEvent(kind);
        OutboxDispatcher dispatcher = dispatcher(CATALOGS, handler(Mono.error(new IllegalStateException("hub down"))));

        assertEquals(1, dispatcher.dispatchOnce().block(), "The row should be claimed.");

        List<Boolean> retryPushed = OutboxTestSupport.query(connectionFactory, "SELECT attempts = 1 AND"
                        + " next_attempt_at > now() AS pushed FROM " + QUALIFIED_OUTBOX_TABLE + " WHERE entity_id = $1",
                row -> row.get("pushed", Boolean.class), CATALOG_ID);
        assertEquals(List.of(true), retryPushed, "A failed row should be kept with one attempt and a later retry.");
        assertEquals(0, dispatcher.dispatchOnce().block(), "A backed-off row should not be due yet.");
    }

    @Test
    void createRowCarriesExtensionsAndNotStoredFields() {
        TestCatalog catalog = catalog(NAME);
        catalog.addAdditionalProperties(EXTENSION_KEY, EXTENSION_VALUE);
        TestNotStoredHolder holder = new TestNotStoredHolder(HOLDER_ID);
        holder.setName(NAME);
        Money balance = new Money();
        balance.setUnit(CURRENCY);
        balance.setTmfValue(AMOUNT);
        holder.setBalance(List.of(balance));
        publisher.createWithEvent(catalog).block();
        publisher.createWithEvent(holder).block();

        assertEquals(2, dispatcher(ALL_TYPES, eventHandler).dispatchOnce().block(), "Both rows should be claimed.");

        ArgumentCaptor<Object> delivered = ArgumentCaptor.forClass(Object.class);
        verify(eventHandler, times(2)).handleCreateEvent(delivered.capture(), anyLong(), any());
        TestCatalog deliveredCatalog = delivered(delivered.getAllValues(), TestCatalog.class);
        assertEquals(List.of(new UnmappedProperty(EXTENSION_KEY, EXTENSION_VALUE)),
                deliveredCatalog.getAdditionalProperties(), "The CreateEvent should carry the extension field.");
        assertEquals(List.of(balance), delivered(delivered.getAllValues(), TestNotStoredHolder.class).getBalance(),
                "The CreateEvent should carry the not-stored field of the request.");
    }

    @Test
    void dispatchersClaimOnlyTheirOwnTypes() {
        publisher.createWithEvent(catalog(NAME)).block();
        TestNotStoredHolder holder = new TestNotStoredHolder(HOLDER_ID);
        holder.setName(NAME);
        publisher.createWithEvent(holder).block();

        assertEquals(1, dispatcher(CATALOGS, eventHandler).dispatchOnce().block(), "Only the catalog row is owned.");
        assertEquals(0, outboxRows(CATALOG_ID), "The catalog row should be delivered.");
        assertEquals(1, outboxRows(HOLDER_ID), "The other deployment's row should be left alone.");

        assertEquals(1, dispatcher(HOLDERS, eventHandler).dispatchOnce().block(), "Only the holder row is owned.");
        assertEquals(0, outboxRows(HOLDER_ID), "The holder row should be delivered by its owner.");
    }

    @Test
    void slowRowDoesNotHoldUpTheRestOfTheBatch() {
        publisher.createWithEvent(catalog(NAME)).block();
        publisher.createWithEvent(catalog(OTHER_CATALOG_ID, NAME)).block();
        Sinks.Empty<Void> otherRowDelivered = Sinks.empty();
        AtomicInteger deliveries = new AtomicInteger();
        TMForumEventHandler blockingHandler = mock(TMForumEventHandler.class);
        // the first delivery completes only once the second one ran, which a one-by-one dispatch never reaches
        when(blockingHandler.handleCreateEvent(any(), anyLong(), any())).thenAnswer(invocation -> {
            if (deliveries.getAndIncrement() == 0) {
                return otherRowDelivered.asMono().timeout(CONCURRENT_DELIVERY_TIMEOUT);
            }
            otherRowDelivered.tryEmitEmpty();
            return Mono.empty();
        });

        assertEquals(2, dispatcher(CATALOGS, blockingHandler).dispatchOnce().block(), "Both rows should be claimed.");

        assertEquals(0, outboxRows(CATALOG_ID) + outboxRows(OTHER_CATALOG_ID),
                "Both rows should be delivered while the first one was still waiting.");
    }

    static Stream<Arguments> leases() {
        return Stream.of(
                Arguments.of(OutboxTestSupport.PROPERTIES.lease(), 0),
                Arguments.of(Duration.ZERO, 1));
    }

    @ParameterizedTest
    @MethodSource("leases")
    void leasedRowIsClaimedAgainOnlyAfterTheLease(Duration lease, int expectedSecondClaims) {
        publisher.createWithEvent(catalog(NAME)).block();
        OutboxDispatcher otherInstance = dispatcher(CATALOGS, eventHandler);
        AtomicInteger secondClaims = new AtomicInteger(-1);
        TMForumEventHandler claimingHandler = mock(TMForumEventHandler.class);
        // while the first instance delivers, a second one polls the same outbox
        when(claimingHandler.handleCreateEvent(any(), anyLong(), any()))
                .thenAnswer(invocation -> otherInstance.dispatchOnce().doOnNext(secondClaims::set).then());

        dispatcher(CATALOGS, claimingHandler, withLease(lease)).dispatchOnce().block();

        assertEquals(expectedSecondClaims, secondClaims.get(),
                "A row should be claimed by another instance only once its lease expired.");
    }

    @Test
    void housekeepingDiscardsExpiredOwnedRowsAndReportsTheRest() {
        insertRow(CATALOG_ID, TestCatalog.TYPE, RETENTION.plusDays(1));
        insertRow(CATALOG_ID, TestCatalog.TYPE, YOUNGER_THAN_RETENTION);
        insertRow(RESOURCE_ID, TestResource.TYPE, RETENTION.plusDays(1));
        MeterRegistry meterRegistry = new SimpleMeterRegistry();

        dispatcher(CATALOGS, eventHandler, OutboxTestSupport.PROPERTIES, meterRegistry).housekeeping().block();

        verifyNoInteractions(eventHandler);
        assertEquals(1, outboxRows(CATALOG_ID), "Only the expired owned row should be discarded.");
        assertEquals(1, outboxRows(RESOURCE_ID), "A row of a type the dispatcher does not own should stay.");
        assertEquals(1.0, meterRegistry.get(OutboxDispatcher.DISCARDED_METRIC)
                .tag(OutboxDispatcher.ENTITY_TYPE_TAG, TestCatalog.TYPE)
                .tag(OutboxDispatcher.EVENT_KIND_TAG, EventKind.CREATE.name())
                .counter().count(), "The discarded row should be counted.");
        assertEquals(1.0, meterRegistry.get(OutboxDispatcher.PENDING_METRIC).gauge().value(),
                "Only the owned pending row should be reported.");
        double oldestAge = meterRegistry.get(OutboxDispatcher.OLDEST_AGE_METRIC).gauge().value();
        long expectedAge = YOUNGER_THAN_RETENTION.toSeconds();
        assertTrue(oldestAge >= expectedAge && oldestAge <= expectedAge + AGE_TOLERANCE_SECONDS,
                "The oldest owned row's age should be reported, was " + oldestAge);
    }

    private void writeWithEvent(EventKind kind) {
        switch (kind) {
            case CREATE -> publisher.createWithEvent(catalog(NAME)).block();
            case UPDATE -> {
                repository.createDomainEntity(catalog(NAME)).block();
                publisher.updateWithEvent(CATALOG_ID, catalog(UPDATED_NAME), TestCatalog.class).block();
            }
            case DELETE -> {
                repository.createDomainEntity(catalog(NAME)).block();
                publisher.deleteWithEvent(URI.create(CATALOG_ID)).block();
            }
        }
    }

    private void verifyDelivered(EventKind kind) {
        switch (kind) {
            case CREATE -> verify(eventHandler).handleCreateEvent(eq(catalog(NAME)), anyLong(), any());
            case UPDATE -> verify(eventHandler).handleUpdateEvent(eq(catalog(UPDATED_NAME)), eq(catalog(NAME)),
                    anyLong(), any());
            case DELETE -> verify(eventHandler).handleDeleteEvent(eq(catalog(NAME)), anyLong(), any());
        }
    }

    private OutboxDispatcher dispatcher(Set<String> types, TMForumEventHandler handler) {
        return dispatcher(types, handler, OutboxTestSupport.PROPERTIES);
    }

    private OutboxDispatcher dispatcher(Set<String> types, TMForumEventHandler handler, OutboxProperties properties) {
        return dispatcher(types, handler, properties, new SimpleMeterRegistry());
    }

    private OutboxDispatcher dispatcher(Set<String> types, TMForumEventHandler handler, OutboxProperties properties,
                                        MeterRegistry meterRegistry) {
        return new OutboxDispatcher(connectionFactory, documentMapper, new EventTypes(types), EVENT_MAPPER, handler,
                properties, meterRegistry);
    }

    private static TMForumEventHandler handler(Mono<Void> outcome) {
        TMForumEventHandler handler = mock(TMForumEventHandler.class);
        when(handler.handleCreateEvent(any(), anyLong(), any())).thenReturn(outcome);
        when(handler.handleUpdateEvent(any(), any(), anyLong(), any())).thenReturn(outcome);
        when(handler.handleDeleteEvent(any(), anyLong(), any())).thenReturn(outcome);
        return handler;
    }

    private static OutboxProperties withLease(Duration lease) {
        OutboxProperties defaults = OutboxTestSupport.PROPERTIES;
        return new OutboxProperties(defaults.enabled(), defaults.pollInterval(), defaults.batchSize(), lease,
                defaults.maxBackoff(), defaults.retention(), defaults.housekeepingInterval());
    }

    private void insertRow(String entityId, String entityType, Duration age) {
        OutboxTestSupport.query(connectionFactory, "INSERT INTO " + QUALIFIED_OUTBOX_TABLE
                        + " (event_kind, entity_id, entity_type, new_state, created_at)"
                        + " VALUES ($1, $2, $3, '{}'::jsonb, $4)", row -> row,
                EventKind.CREATE.name(), entityId, entityType, OffsetDateTime.now().minus(age));
    }

    private long outboxRows(String entityId) {
        return OutboxTestSupport.query(connectionFactory,
                "SELECT count(*) AS rows FROM " + QUALIFIED_OUTBOX_TABLE + " WHERE entity_id = $1",
                row -> row.get("rows", Long.class), entityId).getFirst();
    }

    private static <T> T delivered(List<Object> entities, Class<T> entityClass) {
        return entities.stream().filter(entityClass::isInstance).map(entityClass::cast).findFirst().orElseThrow();
    }

    private static TestCatalog catalog(String name) {
        return catalog(CATALOG_ID, name);
    }

    private static TestCatalog catalog(String id, String name) {
        TestCatalog catalog = new TestCatalog(id);
        catalog.setName(name);
        return catalog;
    }
}
