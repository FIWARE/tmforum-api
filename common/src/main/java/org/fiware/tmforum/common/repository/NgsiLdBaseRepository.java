package org.fiware.tmforum.common.repository;

import io.micronaut.cache.annotation.CachePut;
import io.micronaut.cache.annotation.Cacheable;
import io.reactivex.Completable;
import io.reactivex.Maybe;
import lombok.RequiredArgsConstructor;
import org.fiware.ngsi.api.EntitiesApi;
import org.fiware.ngsi.model.EntityVO;
import org.fiware.tmforum.common.caching.EntityIdKeyGenerator;
import org.fiware.tmforum.common.configuration.GeneralProperties;

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
	protected final EntitiesApi entitiesApi;

	protected String getLinkHeader() {
		return String.format("<%s>; rel=\"http://www.w3.org/ns/json-ld#context\"; type=\"application/ld+json", generalProperties.getContextUrl());
	}

	/**
	 * Create an entity at the broker and cahce it.
	 * @param entityVO - the entity to be created
	 * @param ngsiLDTenant - tenant the entity belongs to
	 * @return completable with the result
	 */
	@CachePut(value = ENTITIES_CACHE_NAME, keyGenerator = EntityIdKeyGenerator.class)
	public Completable createEntity(EntityVO entityVO, String ngsiLDTenant) {
		return entitiesApi.createEntity(entityVO, ngsiLDTenant);
	}

	/**
	 * Retrieve entity from the broker or from the cache if they are available there.
	 * @param entityId id of the entity
	 * @return the entity
	 */
	@Cacheable(ENTITIES_CACHE_NAME)
	public Maybe<EntityVO> retrieveEntityById(URI entityId) {
		return asyncRetrieveEntityById(entityId, generalProperties.getTenant(), null, null, null, getLinkHeader());
	}

	/**
	 * Uncached call to the broker
	 */
	private Maybe<EntityVO> asyncRetrieveEntityById(URI entityId, String ngSILDTenant, String attrs, String type, String options, String link) {
		return entitiesApi.retrieveEntityById(entityId, ngSILDTenant, attrs, type, options, link);
	}

}

