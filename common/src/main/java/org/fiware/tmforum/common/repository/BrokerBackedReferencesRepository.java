package org.fiware.tmforum.common.repository;

import lombok.extern.slf4j.Slf4j;
import org.fiware.ngsi.api.EntitiesApiClient;
import org.fiware.tmforum.common.configuration.GeneralProperties;
import org.fiware.tmforum.storage.ReferenceReadRepository;
import org.fiware.tmforum.storage.StoredReference;
import reactor.core.publisher.Mono;

import jakarta.inject.Singleton;
import java.net.URI;
import java.util.List;

/**
 * ReferenceReadRepository implementation backed by the NGSI-LD api. The cache keeps the {@code EntityVO}, the mapping
 * to {@link StoredReference} happens after it.
 */
@Slf4j
@Singleton
public class BrokerBackedReferencesRepository extends NgsiLdBaseRepository implements ReferenceReadRepository {

	public BrokerBackedReferencesRepository(GeneralProperties generalProperties, EntitiesApiClient entitiesApi) {
		// only used for retrieval, so no such mappers required
		super(generalProperties, entitiesApi, null, null, null, null);
	}

	@Override
	public Mono<StoredReference> referenceExists(String id, List<String> acceptedTypes) {
		return findReference(id).filter(reference -> acceptedTypes.contains(reference.type()));
	}

	@Override
	public Mono<StoredReference> findReference(String id) {
		return retrieveEntityById(URI.create(id))
				.map(entityVO -> new StoredReference(entityVO.getId().toString(), entityVO.getType()));
	}
}
