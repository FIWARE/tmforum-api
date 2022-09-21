package org.fiware.tmforum.mapping;

import io.reactivex.Single;
import org.fiware.ngsi.model.EntityVO;

import java.net.URI;
import java.util.List;

/**
 * Repository interface to provide a retrieval mechanism for entities.
 */
public interface EntitiesRepository {

	Single<List<EntityVO>> getEntities(List<URI> entityIds);
}
