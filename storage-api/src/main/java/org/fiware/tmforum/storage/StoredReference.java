package org.fiware.tmforum.storage;

/**
 * Id, stored type and current version of an existing entity, as needed for referential-integrity checks.
 * {@code version} is the current version label, {@code ""} for an unversioned type, never null.
 */
public record StoredReference(String id, String type, String version) {

    private static final String NO_VERSION = "";

    public StoredReference {
        version = version == null ? NO_VERSION : version;
    }
}
