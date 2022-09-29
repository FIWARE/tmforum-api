package org.fiware.tmforum.common.repository;

import org.fiware.ngsi.model.EntityVO;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * Repostiory interface , focusing on the existence of entities.
 */
public interface ReferencesRepository {

	/**
	 * Returns the entity if it exists and has the expected type.
	 *
	 * @param id            id of the entity to check
	 * @param acceptedTypes list of types accepted for the entity
	 * @return a Mono emitting the entity, in case it exists.
	 */
	Mono<EntityVO> referenceExists(String id, List<String> acceptedTypes);
}
