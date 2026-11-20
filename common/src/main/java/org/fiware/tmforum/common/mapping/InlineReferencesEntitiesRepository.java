package org.fiware.tmforum.common.mapping;

import io.github.wistefan.mapping.EntitiesRepository;
import io.github.wistefan.mapping.MappingException;
import jakarta.inject.Singleton;
import org.fiware.ngsi.model.EntityVO;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.util.List;

/**
 * {@link EntitiesRepository} for the mapping library that never resolves a relationship target: references are
 * stored inline and read back from their own sub-properties. A non-empty request means a relationship setter lacks
 * {@code fromProperties = true}, so it fails the read instead of silently returning an id-only reference.
 */
@Singleton
public class InlineReferencesEntitiesRepository implements EntitiesRepository {

	@Override
	public Mono<List<EntityVO>> getEntities(List<URI> entityIds) {
		if (entityIds.isEmpty()) {
			return Mono.just(List.of());
		}
		return Mono.error(new MappingException(String.format(
				"References are read from their stored properties, but resolving %s was requested. "
						+ "The relationship setter needs fromProperties = true.", entityIds)));
	}
}
