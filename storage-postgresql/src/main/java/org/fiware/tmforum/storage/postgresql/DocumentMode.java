package org.fiware.tmforum.storage.postgresql;

/**
 * Which attributes of a domain object go into its stored document. Null attributes are never written.
 */
public enum DocumentMode {
    /** The whole entity, for a create; the storage variant drops declared top-level empty lists and objects. */
    FULL,
    /**
     * Only the attributes an update carries, without {@code id} and {@code type}. The storage variant writes an
     * emptied declared list as {@code null} (the update removes the stored key) and drops a declared empty object.
     * Known defect shared with the NGSI-LD adapter (see {@code EntityWriteRepository.updateDomainEntity}): a
     * primitive field cannot be absent, so it is always carried and its default overwrites the stored value.
     */
    CARRIED_ATTRIBUTES
}
