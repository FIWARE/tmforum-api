package org.fiware.tmforum.storage.postgresql;

/**
 * Names shared by the PostgreSQL adapter, its Flyway migrations and its configuration.
 */
public final class PostgresStorage {

    /**
     * Micronaut environment that activates the PostgreSQL adapter.
     */
    public static final String ENVIRONMENT = "postgresql";

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

    private PostgresStorage() {
    }
}
