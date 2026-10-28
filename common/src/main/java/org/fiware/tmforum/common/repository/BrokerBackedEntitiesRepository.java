package org.fiware.tmforum.common.repository;

import io.github.wistefan.mapping.EntitiesRepository;
import lombok.extern.slf4j.Slf4j;
import org.fiware.ngsi.api.EntitiesApiClient;
import org.fiware.ngsi.model.EntityVO;
import org.fiware.tmforum.common.configuration.GeneralProperties;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import jakarta.inject.Singleton;
import java.net.URI;
import java.util.List;

/**
 * Implementation of the {@link  EntitiesRepository} required by the mapping-module. Uses the NGSI-LD api as a storage backend to retrieve the entities.
 */
@Slf4j
@Singleton
public class BrokerBackedEntitiesRepository extends NgsiLdBaseRepository implements EntitiesRepository {

    public BrokerBackedEntitiesRepository(GeneralProperties generalProperties, EntitiesApiClient entitiesApi) {
        // only used for retrieval, so no such mappers required
        super(generalProperties, entitiesApi, null, null, null, null);
    }

    /**
     * Returns the entities that exist, in input order. Missing ones are omitted, so the mapper reads them id-only.
     */
    @Override
    public Mono<List<EntityVO>> getEntities(List<URI> entityIds) {

        // this can be replaced in the futures, when the brokers properly implement the retrieval of entities with multiple sub-properties, with an idPattern query
        // Currently:
        // * orion-ld: does not properly handle datasetIDs, thus omits such properties and relationships on retrieval
        // * scoprio: declares query parameters as mandatory, that are optional in the spec
        // * stellio: not tested yet
        // a missing target (404) is left out instead of emptying the whole result; other broker errors propagate
        return Flux.fromIterable(entityIds)
                .flatMapSequential(this::retrieveEntityById)
                .collectList();
    }
}
