package org.fiware.tmforum.storage.postgresql;

import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import io.r2dbc.spi.ConnectionFactory;
import io.r2dbc.spi.Readable;
import jakarta.inject.Inject;
import org.fiware.tmforum.common.notification.EventTypes;
import org.fiware.tmforum.storage.StorageErrorReason;
import org.fiware.tmforum.storage.StorageException;
import org.fiware.tmforum.storage.postgresql.DocumentTestEntities.TestCatalog;
import org.fiware.tmforum.storage.postgresql.DocumentTestEntities.TestResource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.util.List;
import java.util.Set;

import static org.fiware.tmforum.storage.postgresql.PostgresStorage.QUALIFIED_OUTBOX_TABLE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * The entity write and its outbox row commit or roll back together, and only types the deployment owns get a row.
 */
@MicronautTest(environments = PostgresStorage.ENVIRONMENT)
class OutboxEntityEventPublisherIT {

    private static final String CATALOG_ID = "urn:ngsi-ld:" + TestCatalog.TYPE + ":catalog-1";
    private static final String RESOURCE_ID = "urn:ngsi-ld:" + TestResource.TYPE + ":resource-1";
    private static final String NAME = "catalog name";
    private static final String UPDATED_NAME = "updated catalog name";
    private static final String REJECTING_CONSTRAINT = "outbox_it_rejects_entity";
    private static final String SELECT_ROWS = "SELECT event_kind, entity_type, new_state->>'name' AS new_name,"
            + " old_state->>'name' AS old_name FROM " + QUALIFIED_OUTBOX_TABLE + " WHERE entity_id = $1";

    @Inject
    private PostgresEntityRepository repository;

    @Inject
    private ConnectionFactory connectionFactory;

    private OutboxEntityEventPublisher publisher;

    @BeforeEach
    void setUp() {
        dropRejection();
        OutboxTestSupport.cleanUp(connectionFactory);
        publisher = new OutboxEntityEventPublisher(repository, new EventTypes(Set.of(TestCatalog.TYPE)));
    }

    @AfterEach
    void dropRejection() {
        OutboxTestSupport.query(connectionFactory, "ALTER TABLE " + QUALIFIED_OUTBOX_TABLE
                + " DROP CONSTRAINT IF EXISTS " + REJECTING_CONSTRAINT, row -> row);
    }

    @Test
    void createCommitsEntityAndCreateRow() {
        publisher.createWithEvent(catalog(NAME)).block();

        assertNotNull(stored(), "The entity should be stored.");
        assertEquals(List.of(new OutboxRow(EventKind.CREATE.name(), TestCatalog.TYPE, NAME, null)), rows(CATALOG_ID),
                "The create should leave one CREATE row carrying the new state only.");
    }

    @Test
    void updateCommitsMergeAndUpdateRowWithBothStates() {
        repository.createDomainEntity(catalog(NAME)).block();

        TestCatalog updated = publisher.updateWithEvent(CATALOG_ID, catalog(UPDATED_NAME), TestCatalog.class).block();

        assertEquals(UPDATED_NAME, updated.getName(), "The new state should be returned.");
        assertEquals(UPDATED_NAME, stored().getName(), "The merge should be stored.");
        assertEquals(List.of(new OutboxRow(EventKind.UPDATE.name(), TestCatalog.TYPE, UPDATED_NAME, NAME)),
                rows(CATALOG_ID), "The update should leave one UPDATE row with the replaced and the new state.");
    }

    @Test
    void deleteCommitsDeletionAndDeleteRowWithOldState() {
        repository.createDomainEntity(catalog(NAME)).block();

        publisher.deleteWithEvent(URI.create(CATALOG_ID)).block();

        assertNull(stored(), "The entity should be deleted.");
        assertEquals(List.of(new OutboxRow(EventKind.DELETE.name(), TestCatalog.TYPE, null, NAME)), rows(CATALOG_ID),
                "The delete should leave one DELETE row carrying the old state only.");
    }

    @ParameterizedTest
    @EnumSource(value = EventKind.class, names = {"UPDATE", "DELETE"})
    void writeOfMissingEntityIsNotFoundWithoutRow(EventKind kind) {
        StorageException failure = assertThrows(StorageException.class, () -> write(kind).block(),
                "A missing entity should fail the write.");

        assertEquals(StorageErrorReason.NOT_FOUND, failure.reason(), "A missing entity should be NOT_FOUND.");
        assertEquals(List.of(), rows(CATALOG_ID), "A failed write should leave no outbox row.");
    }

    @ParameterizedTest
    @EnumSource(EventKind.class)
    void failingOutboxInsertRollsBackTheWrite(EventKind kind) {
        if (kind != EventKind.CREATE) {
            repository.createDomainEntity(catalog(NAME)).block();
        }
        OutboxTestSupport.query(connectionFactory, "ALTER TABLE " + QUALIFIED_OUTBOX_TABLE + " ADD CONSTRAINT "
                + REJECTING_CONSTRAINT + " CHECK (entity_id <> '" + CATALOG_ID + "') NOT VALID", row -> row);

        assertThrows(StorageException.class, () -> write(kind).block(), "The rejected outbox insert should fail.");

        TestCatalog stored = stored();
        if (kind == EventKind.CREATE) {
            assertNull(stored, "The create should be rolled back with its outbox row.");
        } else {
            assertEquals(NAME, stored.getName(), "The " + kind + " should be rolled back with its outbox row.");
        }
        assertEquals(List.of(), rows(CATALOG_ID), "No outbox row should be left.");
    }

    @Test
    void writesOfAnUnownedTypeLeaveNoRow() {
        TestResource resource = new TestResource(RESOURCE_ID);
        resource.setName(NAME);
        TestResource patch = new TestResource(RESOURCE_ID);
        patch.setName(UPDATED_NAME);

        publisher.createWithEvent(resource).block();
        publisher.updateWithEvent(RESOURCE_ID, patch, TestResource.class).block();
        assertEquals(UPDATED_NAME, repository.get(URI.create(RESOURCE_ID), TestResource.class).block().getName(),
                "The update of an unowned type should still be stored.");
        publisher.deleteWithEvent(URI.create(RESOURCE_ID)).block();

        assertNull(repository.get(URI.create(RESOURCE_ID), TestResource.class).block(),
                "The delete of an unowned type should still be applied.");
        assertEquals(List.of(), rows(RESOURCE_ID), "A type no dispatcher claims should get no outbox row.");
    }

    private Mono<?> write(EventKind kind) {
        return switch (kind) {
            case CREATE -> publisher.createWithEvent(catalog(NAME));
            case UPDATE -> publisher.updateWithEvent(CATALOG_ID, catalog(UPDATED_NAME), TestCatalog.class);
            case DELETE -> publisher.deleteWithEvent(URI.create(CATALOG_ID));
        };
    }

    private TestCatalog stored() {
        return repository.get(URI.create(CATALOG_ID), TestCatalog.class).block();
    }

    private List<OutboxRow> rows(String entityId) {
        return OutboxTestSupport.query(connectionFactory, SELECT_ROWS, OutboxRow::of, entityId);
    }

    private static TestCatalog catalog(String name) {
        TestCatalog catalog = new TestCatalog(CATALOG_ID);
        catalog.setName(name);
        return catalog;
    }

    private record OutboxRow(String kind, String entityType, String newName, String oldName) {

        static OutboxRow of(Readable row) {
            return new OutboxRow(row.get("event_kind", String.class), row.get("entity_type", String.class),
                    row.get("new_name", String.class), row.get("old_name", String.class));
        }
    }
}
