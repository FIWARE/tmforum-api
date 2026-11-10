package org.fiware.tmforum.storage.postgresql;

/**
 * Value of {@code tmforum.event_outbox.event_kind}: the API write an outbox row reports.
 */
public enum EventKind {
    CREATE,
    UPDATE,
    DELETE
}
