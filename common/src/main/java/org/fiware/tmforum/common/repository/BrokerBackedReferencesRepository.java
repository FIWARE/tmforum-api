package org.fiware.tmforum.common.repository;

import lombok.extern.slf4j.Slf4j;
import org.fiware.ngsi.api.EntitiesApiClient;
import org.fiware.ngsi.model.EntityVO;
import org.fiware.ngsi.model.PropertyVO;
import org.fiware.tmforum.common.configuration.GeneralProperties;
import org.fiware.tmforum.common.domain.VersionedEntity;
import org.fiware.tmforum.storage.ReferenceReadRepository;
import org.fiware.tmforum.storage.StoredReference;
import reactor.core.publisher.Mono;

import jakarta.inject.Singleton;
import java.net.URI;
import java.util.List;
import java.util.Optional;

/**
 * ReferenceReadRepository implementation backed by the NGSI-LD api. The cache keeps the {@code EntityVO}, the mapping
 * to {@link StoredReference} happens after it.
 */
@Slf4j
@Singleton
public class BrokerBackedReferencesRepository extends NgsiLdBaseRepository implements ReferenceReadRepository {

	private static final String VERSION_ATTRIBUTE = "version";
	private static final String NO_VERSION = "";

	public BrokerBackedReferencesRepository(GeneralProperties generalProperties, EntitiesApiClient entitiesApi) {
		// only used for retrieval, so no such mappers required
		super(generalProperties, entitiesApi, null, null, null);
	}

	@Override
	public Mono<StoredReference> referenceExists(String id, List<String> acceptedTypes) {
		return findReference(id).filter(reference -> acceptedTypes.contains(reference.type()));
	}

	@Override
	public Mono<StoredReference> findReference(String id) {
		return retrieveEntityById(URI.create(id))
				.map(entityVO -> new StoredReference(entityVO.getId().toString(), entityVO.getType(), version(entityVO)));
	}

	// the type decides, as on PostgreSQL, so an extension called version on an unversioned type is not reported
	private static String version(EntityVO entityVO) {
		if (!VersionedEntity.VERSIONED_TYPES.contains(entityVO.getType())) {
			return NO_VERSION;
		}
		return Optional.ofNullable(entityVO.getAdditionalProperties())
				.map(properties -> properties.get(VERSION_ATTRIBUTE))
				.filter(PropertyVO.class::isInstance)
				.map(property -> ((PropertyVO) property).getValue())
				.map(Object::toString)
				.orElse(NO_VERSION);
	}
}
