package org.fiware.tmforum.storage.postgresql;

/**
 * Which attributes of a domain object go into its stored document. Null attributes are never written.
 */
public enum DocumentMode {
    /** The whole entity, for a create. */
    FULL,
    /**
     * Only the attributes an update carries, without {@code id} and {@code type}; an emptied list stays {@code []}.
     * A primitive field (e.g. {@code Agreement.documentNumber}) cannot be absent, so it is always carried, its
     * default included; the NGSI-LD adapter writes it the same way.
     */
    CARRIED_ATTRIBUTES
}
