package org.fiware.tmforum.storage.postgresql;

/**
 * Which attributes of a domain object go into its stored document. Null attributes are never written.
 */
public enum DocumentMode {
    /** The whole entity, for a create; the storage variant drops top-level empty lists and objects. */
    FULL,
    /**
     * Only the attributes an update carries, without {@code id} and {@code type}; an emptied list stays {@code []}
     * (the update removes the stored key), the storage variant drops a top-level empty object.
     * Known defect shared with the NGSI-LD adapter (see {@code EntityWriteRepository.updateDomainEntity}): a
     * primitive field cannot be absent, so it is always carried and its default overwrites the stored value.
     */
    CARRIED_ATTRIBUTES
}
