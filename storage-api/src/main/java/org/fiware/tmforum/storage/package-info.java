/**
 * Storage-neutral persistence port of the TMForum APIs. Controllers and services depend only on these
 * types; each backend (today NGSI-LD) provides an adapter implementing them.
 * <p>
 * A {@code Mono<Void>} completes empty on success. Failures are signalled as {@link
 * org.fiware.tmforum.storage.StorageException}, carrying a {@link org.fiware.tmforum.storage.StorageErrorReason}
 * and the failing {@link org.fiware.tmforum.storage.StorageOperation}. References between entities are stored
 * inline, so no foreign-key semantics apply.
 */
package org.fiware.tmforum.storage;
