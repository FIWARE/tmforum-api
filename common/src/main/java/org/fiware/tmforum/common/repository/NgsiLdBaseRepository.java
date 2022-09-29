package org.fiware.tmforum.common.repository;

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
import org.fiware.tmforum.common.exception.NgsiLdRepositoryException;
import reactor.core.publisher.Mono;

import java.net.URI;

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
     * @return the entity.
     */
    @CacheInvalidate(value = ENTITIES_CACHE_NAME, keyGenerator = EntityIdKeyGenerator.class)
    public Mono<Void> patchEntity(URI entityId, EntityFragmentVO entityFragmentVO) {
        return entitiesApi.updateEntity(entityId, entityFragmentVO, generalProperties.getTenant(), null);
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
        throw new NgsiLdRepositoryException("Was not able to successfully call the broker.", e);
    }

}

