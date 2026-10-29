package org.fiware.tmforum.storage;

/**
 * The backend step that failed. Part of the contract of each port method, because {@code common} renders the
 * HTTP response from it: {@code get}/{@code findReference}/{@code referenceExists} fail with READ,
 * {@code findEntities*} with LIST (also when a nested read fails), and the writes with CREATE/UPDATE/DELETE.
 * The one recorded exception is the NGSI-LD replace-mode update, whose pre-read fails with READ for an absent
 * id or an error status. Only {@code common}'s error rendering reads it; callers branch on the reason.
 */
public enum StorageOperation {
    READ,
    LIST,
    CREATE,
    UPDATE,
    DELETE
}
