package org.fiware.tmforum.common.repository;

import io.reactivex.Maybe;
import lombok.extern.slf4j.Slf4j;
import org.fiware.ngsi.api.EntitiesApi;
import org.fiware.ngsi.model.EntityVO;
import org.fiware.tmforum.common.configuration.GeneralProperties;

import javax.inject.Singleton;
import java.net.URI;
import java.util.List;

/**
 * ReferencesRepository implementation backed by the NGSI-LD api.
 */
@Slf4j
@Singleton
public class BrokerBackedReferencesRepository extends NgsiLdBaseRepository implements ReferencesRepository {

	public BrokerBackedReferencesRepository(GeneralProperties generalProperties, EntitiesApi entitiesApi) {
		super(generalProperties, entitiesApi);
	}

	/**
	 * Returns the entity if it exists and has the expected type.
	 *
	 * @param id            id of the entity to check
	 * @param acceptedTypes list of types accepted for the entity
	 * @return a Maybe emitting the entiy, in case it exists.
	 */
	@Override
	public Maybe<EntityVO> referenceExists(String id, List<String> acceptedTypes) {
		return retrieveEntityById(URI.create(id)).filter(e -> acceptedTypes.contains(e.getType()));
	}
}