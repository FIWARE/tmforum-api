package org.fiware.tmforum.storage;

/**
 * Why a storage operation failed. Callers branch on it instead of on backend-specific errors.
 */
public enum StorageErrorReason {
    /** The addressed entity does not exist. */
    NOT_FOUND,
    /** An entity with the id already exists. */
    CONFLICT,
    /** The backend rejected the content of the entity. */
    INVALID_DATA,
    /** The backend wrote only some of the attributes of an update. */
    PARTIAL_UPDATE,
    /** The backend gave no answer. */
    UNAVAILABLE,
    /** The backend answered with an error that has no other reason. */
    UNKNOWN
}
