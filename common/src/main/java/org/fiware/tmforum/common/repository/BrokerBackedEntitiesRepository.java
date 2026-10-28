package org.fiware.tmforum.common.repository;

import io.github.wistefan.mapping.EntitiesRepository;
import lombok.extern.slf4j.Slf4j;
import org.fiware.ngsi.api.EntitiesApiClient;
import org.fiware.ngsi.model.EntityVO;
import org.fiware.tmforum.common.configuration.GeneralProperties;
import org.fiware.tmforum.common.mapping.TolerantEntityVOMapper;
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
     * Returns one entity per id. A 404 target becomes a {@link TolerantEntityVOMapper#unresolvedTarget(URI)} stub,
     * so it reads back id-only instead of failing the read; other broker errors propagate.
     */
    @Override
    public Mono<List<EntityVO>> getEntities(List<URI> entityIds) {

        // this can be replaced in the futures, when the brokers properly implement the retrieval of entities with multiple sub-properties, with an idPattern query
        // Currently:
        // * orion-ld: does not properly handle datasetIDs, thus omits such properties and relationships on retrieval
        // * scoprio: declares query parameters as mandatory, that are optional in the spec
        // * stellio: not tested yet
        return Flux.fromIterable(entityIds)
                .flatMapSequential(id -> retrieveEntityById(id)
                        .defaultIfEmpty(TolerantEntityVOMapper.unresolvedTarget(id)))
                .collectList();
    }
}
