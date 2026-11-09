package org.fiware.tmforum.storage.postgresql;

/**
 * Which attributes of a domain object go into its stored document. Null attributes are never written.
 */
public enum DocumentMode {
    /** The whole entity, for a create. */
    FULL,
    /** Only the attributes an update carries, without {@code id} and {@code type}; an emptied list stays {@code []}. */
    CARRIED_ATTRIBUTES
}
