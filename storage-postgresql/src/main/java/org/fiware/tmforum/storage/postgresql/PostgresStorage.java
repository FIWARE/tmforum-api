package org.fiware.tmforum.storage.postgresql;

import org.fiware.tmforum.common.CommonConstants;

/**
 * Names shared by the PostgreSQL adapter, its Flyway migrations and its configuration.
 */
public final class PostgresStorage {

    /**
     * Micronaut environment that activates the PostgreSQL adapter.
     */
    public static final String ENVIRONMENT = CommonConstants.POSTGRESQL_ENVIRONMENT;

    /**
     * Schema owned by Flyway; it holds the adapter's tables and the migration history.
     */
    public static final String SCHEMA = "tmforum";

    public static final String ENTITY_TABLE = "entity";

    public static final String QUALIFIED_ENTITY_TABLE = SCHEMA + "." + ENTITY_TABLE;

    /**
     * Partial unique index guaranteeing at most one current row per entity id.
     */
    public static final String ENTITY_CURRENT_INDEX = "entity_current_idx";

    /**
     * Partial index over the current rows carrying the filter and the default order of every list (V4 migration).
     */
    public static final String ENTITY_LIST_INDEX = "entity_current_type_created_idx";

    /**
     * Table of the transactional event outbox (V2 migration).
     */
    public static final String OUTBOX_TABLE = "event_outbox";

    public static final String QUALIFIED_OUTBOX_TABLE = SCHEMA + "." + OUTBOX_TABLE;

    /**
     * Index carrying the dispatcher's claim of the due rows of its own types (V4 migration).
     */
    public static final String OUTBOX_DUE_INDEX = "event_outbox_type_due_idx";

    /**
     * Index carrying the retention sweep over the rows of the dispatcher's own types (V4 migration).
     */
    public static final String OUTBOX_RETENTION_INDEX = "event_outbox_type_created_idx";

    private PostgresStorage() {
    }
}
