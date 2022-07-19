package org.fiware.tmforum.common.repository;

import io.reactivex.Maybe;
import lombok.extern.slf4j.Slf4j;
import org.fiware.ngsi.api.EntitiesApi;
import org.fiware.ngsi.model.EntityVO;
import org.fiware.tmforum.common.configuration.GeneralProperties;

import javax.inject.Singleton;
import java.net.URI;
import java.util.List;

@Slf4j
@Singleton
public class ReferencesRepository extends NgsiLdBaseRepository {

	public ReferencesRepository(GeneralProperties generalProperties, EntitiesApi entitiesApi) {
		// this repo is only for validating referential integrity and does not change anything, therefor canismajor is not required.
		super(generalProperties, entitiesApi);
	}

	public Maybe<EntityVO> referenceExists(String id, List<String> acceptedTypes) {
		return retrieveEntityById(URI.create(id)).filter(e -> acceptedTypes.contains(e.getType()));
	}
}