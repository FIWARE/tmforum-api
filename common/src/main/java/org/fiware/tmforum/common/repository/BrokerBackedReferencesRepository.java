package org.fiware.tmforum.common.repository;

import lombok.extern.slf4j.Slf4j;
import org.fiware.ngsi.api.EntitiesApiClient;
import org.fiware.ngsi.model.EntityVO;
import org.fiware.tmforum.common.configuration.GeneralProperties;
import reactor.core.publisher.Mono;

import javax.inject.Singleton;
import java.net.URI;
import java.util.List;

/**
 * ReferencesRepository implementation backed by the NGSI-LD api.
 */
@Slf4j
@Singleton
public class BrokerBackedReferencesRepository extends NgsiLdBaseRepository implements ReferencesRepository {

	public BrokerBackedReferencesRepository(GeneralProperties generalProperties, EntitiesApiClient entitiesApi) {
		// only used for retrieval, so no such mappers required
		super(generalProperties, entitiesApi, null, null, null);
	}

	/**
	 * Returns the entity if it exists and has the expected type.
	 *
	 * @param id            id of the entity to check
	 * @param acceptedTypes list of types accepted for the entity
	 * @return a Maybe emitting the entiy, in case it exists.
	 */
	@Override
	public Mono<EntityVO> referenceExists(String id, List<String> acceptedTypes) {
		return retrieveEntityById(URI.create(id)).filter(e -> acceptedTypes.contains(e.getType()));
	}
}