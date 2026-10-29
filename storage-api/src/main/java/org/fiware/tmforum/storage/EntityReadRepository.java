package org.fiware.tmforum.storage;

import reactor.core.publisher.Mono;

import java.net.URI;
import java.util.function.Function;

/**
 * Read side of the entity port. Failures are {@link StorageException}s with {@link StorageOperation#READ} for
 * {@code get} and {@link StorageOperation#LIST} for the {@code findEntities*} methods, also when a nested read
 * fails.
 * <p>
 * {@code query} and {@code orderBy} are {@code QueryParser} output (NGSI-LD q syntax). That coupling is
 * transitional until a neutral query model exists (ticket 7).
 */
public interface EntityReadRepository {

    /**
     * Empty, not an error, when the id is unknown, has no type segment ({@code urn:ngsi-ld:<type>:...}) or its
     * type segment is not a type the class is persisted as (today the class's MappingEnabled entity types).
     * Every adapter checks the id's type segment, not the stored type.
     *
     * @throws IllegalArgumentException synchronously, if the backend cannot persist {@code entityClass}; checked
     *                                  only after the id has a type segment, so a malformed id is always empty
     */
    <T> Mono<T> get(URI id, Class<T> entityClass);

    /**
     * Values within {@code ids} and within {@code types} are comma-separated and OR-ed; the two filters and
     * {@code query} are AND-ed. {@code types} is required (NGSI-LD brokers reject a query without a type or
     * other filter); a null {@code ids}, {@code query} or {@code orderBy} means unrestricted or unordered.
     * Without a match the result may complete empty instead of emitting an empty page (the NGSI-LD adapter does).
     */
    <T> Mono<PagedResult<T>> findEntities(Integer offset, Integer limit, Class<T> entityClass,
                                          String query, String ids, String types, String orderBy);

    /**
     * Entities of {@code entityType} matching {@code query}, unordered; the full overload without ids or orderBy.
     */
    default <T> Mono<PagedResult<T>> findEntities(Integer offset, Integer limit, String entityType,
                                                  Class<T> entityClass, String query) {
        return findEntities(offset, limit, entityClass, query, null, entityType, null);
    }

    /**
     * Lists several types in one paged result, mapping each entity to the class {@code typeToClass} returns for
     * its stored type. {@code ids}, {@code types} and {@code query} filter as in {@code findEntities}.
     * <p>
     * Known NGSI-LD adapter defect, not a contract rule (ticket 0): Orion-LD rejects several ids together with
     * several types, so such a call fails with (LIST, UNKNOWN).
     */
    <T> Mono<PagedResult<T>> findEntitiesPolymorphic(Integer offset, Integer limit, String types, String query,
                                                     String ids, String orderBy,
                                                     Function<String, Class<? extends T>> typeToClass);
}
