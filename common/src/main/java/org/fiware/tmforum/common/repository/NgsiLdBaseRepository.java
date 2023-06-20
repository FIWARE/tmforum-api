package org.fiware.tmforum.common.repository;

import io.github.wistefan.mapping.EntityVOMapper;
import io.github.wistefan.mapping.JavaObjectMapper;
import io.micronaut.cache.annotation.CacheInvalidate;
import io.micronaut.cache.annotation.CachePut;
import io.micronaut.cache.annotation.Cacheable;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.client.exceptions.HttpClientResponseException;
import lombok.RequiredArgsConstructor;
import org.fiware.ngsi.api.EntitiesApiClient;
import org.fiware.ngsi.model.EntityFragmentVO;
import org.fiware.ngsi.model.EntityVO;
import org.fiware.tmforum.common.caching.EntityIdKeyGenerator;
import org.fiware.tmforum.common.configuration.GeneralProperties;
import org.fiware.tmforum.common.exception.DeletionException;
import org.fiware.tmforum.common.exception.DeletionExceptionReason;
import org.fiware.tmforum.common.exception.NgsiLdRepositoryException;
import org.fiware.tmforum.common.mapping.NGSIMapper;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * Base-Repository implementation for using the NGSI-LD API as a storage backend. Supports caching and asynchronous retrieval of entities.
 */
@RequiredArgsConstructor
public abstract class NgsiLdBaseRepository {

    /**
     * Name for the entities cache
     */
    private static final String ENTITIES_CACHE_NAME = "entities";

    protected final GeneralProperties generalProperties;
    protected final EntitiesApiClient entitiesApi;
    protected final JavaObjectMapper javaObjectMapper;
    protected final NGSIMapper ngsiMapper;
    protected final EntityVOMapper entityVOMapper;


    protected String getLinkHeader() {
        return String.format("<%s>; rel=\"http://www.w3.org/ns/json-ld#context\"; type=\"application/ld+json", generalProperties.getContextUrl());
    }

    /**
     * Create an entity at the broker and cahce it.
     *
     * @param entityVO     - the entity to be created
     * @param ngsiLDTenant - tenant the entity belongs to
     * @return completable with the result
     */
    @CachePut(value = ENTITIES_CACHE_NAME, keyGenerator = EntityIdKeyGenerator.class)
    public Mono<Void> createEntity(EntityVO entityVO, String ngsiLDTenant) {
        return entitiesApi.createEntity(entityVO, ngsiLDTenant);
    }

    /**
     * Retrieve entity from the broker or from the cache if they are available there.
     *
     * @param entityId id of the entity
     * @return the entity
     */
    @Cacheable(ENTITIES_CACHE_NAME)
    public Mono<EntityVO> retrieveEntityById(URI entityId) {
        return asyncRetrieveEntityById(entityId, generalProperties.getTenant(), null, null, null, getLinkHeader());
    }

    /**
     * Patch an entity, using the "overwrite" option.
     *
     * @param entityId         id of the entity
     * @param entityFragmentVO the entity elements to be updated
     * @return an empty mono
     */
    @CacheInvalidate(value = ENTITIES_CACHE_NAME, keyGenerator = EntityIdKeyGenerator.class)
    public Mono<Void> patchEntity(URI entityId, EntityFragmentVO entityFragmentVO) {
        return entitiesApi.updateEntity(entityId, entityFragmentVO, generalProperties.getTenant(), null);
    }

    /**
     * Create a domain entity
     *
     * @param domainEntity the entity to be created
     * @param <T>          the type of the object
     * @return an empty mono
     */
    public <T> Mono<Void> createDomainEntity(T domainEntity) {
        return createEntity(javaObjectMapper.toEntityVO(domainEntity), generalProperties.getTenant());
    }

    /**
     * Update a domain entity
     *
     * @param id           id of the entity to be updated
     * @param domainEntity the entity to be created
     * @param <T>          the type of the object
     * @return an empty mono
     */
    public <T> Mono<Void> updateDomainEntity(String id, T domainEntity) {
        return patchEntity(URI.create(id), ngsiMapper.map(javaObjectMapper.toEntityVO(domainEntity)));
    }

    /**
     * Delete a domain entity
     *
     * @param id id of the entity to be deleted
     * @return an empty mono
     */
    public Mono<Void> deleteDomainEntity(URI id) {
        return entitiesApi
                .removeEntityById(id, generalProperties.getTenant(), null)
                .onErrorResume(t -> {
                    if (t instanceof HttpClientResponseException e && e.getStatus().equals(HttpStatus.NOT_FOUND)) {
                        throw new DeletionException(String.format("Was not able to delete %s, since it does not exist.", id),
                                DeletionExceptionReason.NOT_FOUND);
                    }
                    throw new DeletionException(String.format("Was not able to delete %s.", id),
                            t,
                            DeletionExceptionReason.UNKNOWN);
                });
    }

    /**
     * Helper method for combining a stream of entites to a single mono.
     *
     * @param entityVOStream stream of entites
     * @param targetClass    target class to map them
     * @param <T>            type of the target
     * @return a mono, emitting a list of mapped entities
     */
    protected <T> Mono<List<T>> zipToList(Stream<EntityVO> entityVOStream, Class<T> targetClass) {
        return Mono.zip(
                entityVOStream.map(entityVO -> entityVOMapper.fromEntityVO(entityVO, targetClass)).toList(),
                oList -> Arrays.stream(oList).map(targetClass::cast).toList()
        );
    }

    /**
     * Uncached call to the broker
     */
    private Mono<EntityVO> asyncRetrieveEntityById(URI entityId, String ngSILDTenant, String attrs, String type, String options, String link) {
        return entitiesApi
                .retrieveEntityById(entityId, ngSILDTenant, attrs, type, options, link)
                .onErrorResume(this::handleClientException);
    }

    private Mono<EntityVO> handleClientException(Throwable e) {
        if (e instanceof HttpClientResponseException httpException && httpException.getStatus().equals(HttpStatus.NOT_FOUND)) {
            return Mono.empty();
        }
        throw new NgsiLdRepositoryException("Was not able to successfully call the broker.", Optional.of(e));
    }

}

