package org.fiware.tmforum.mapping;

import org.fiware.ngsi.model.EntityVO;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.util.List;

/**
 * Repository interface to provide a retrieval mechanism for entities.
 */
public interface EntitiesRepository {

	Mono<List<EntityVO>> getEntities(List<URI> entityIds);
}
