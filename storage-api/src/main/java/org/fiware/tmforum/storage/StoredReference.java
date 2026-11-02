package org.fiware.tmforum.storage;

/**
 * Id and stored type of an existing entity, as needed for referential-integrity checks.
 */
public record StoredReference(String id, String type) {
}
