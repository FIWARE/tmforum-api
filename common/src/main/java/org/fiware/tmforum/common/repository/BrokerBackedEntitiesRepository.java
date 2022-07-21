package org.fiware.tmforum.common.repository;

import io.reactivex.Maybe;
import io.reactivex.Single;
import lombok.extern.slf4j.Slf4j;
import org.fiware.ngsi.api.EntitiesApi;
import org.fiware.ngsi.model.EntityVO;
import org.fiware.tmforum.common.configuration.GeneralProperties;
import org.fiware.tmforum.mapping.EntitiesRepository;

import javax.inject.Singleton;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Implementation of the {@link  EntitiesRepository} required by the mapping-module. Uses the NGSI-LD api as a storage backend to retrieve the entities.
 */
@Slf4j
@Singleton
public class BrokerBackedEntitiesRepository extends NgsiLdBaseRepository implements EntitiesRepository {

	public BrokerBackedEntitiesRepository(GeneralProperties generalProperties, EntitiesApi entitiesApi) {
		super(generalProperties, entitiesApi);
	}

	@Override
	public Single<List<EntityVO>> getEntities(List<URI> entityIds) {

		// this can be replaced in the futures, when the brokers properly implement the retrieval of entities with multiple sub-properties, with an idPattern query
		// Currently:
		// * orion-ld: does not properly handle datasetIDs, thus omits such properties and relationships on retrieval
		// * scoprio: declares query parameters as mandatory, that are optional in the spec
		// * stellio: not tested yet
		return Maybe.zip(
				entityIds.stream().map(this::retrieveEntityById).toList(),
				eVOs -> Arrays.stream(eVOs).map(EntityVO.class::cast).toList()).toSingle(new ArrayList<>());
	}
}
