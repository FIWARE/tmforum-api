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

@Slf4j
@Singleton
public class BrokerBackedEntitiesRepository extends NgsiLdBaseRepository implements EntitiesRepository {

	public BrokerBackedEntitiesRepository(GeneralProperties generalProperties, EntitiesApi entitiesApi) {
		super(generalProperties, entitiesApi);
	}

	@Override
	public Single<List<EntityVO>> getEntities(List<URI> entityIds) {

		return Maybe.zip(
				entityIds.stream().map(this::retrieveEntityById).toList(),
				evos -> Arrays.stream(evos).map(EntityVO.class::cast).toList()).toSingle(new ArrayList<>());

		// only an option in the future, when the brokers support the spec...

//		if (entityIds == null || entityIds.isEmpty()) {
//			return Single.just(List.of());
//		}
//
//		String idPattern = entityIds.stream().map(URI::toString).collect(Collectors.joining("|", "(", ")"));
//		return entitiesApi.queryEntities(generalProperties.getTenant(), null, idPattern, null, null, null, null, null, null, null, null, null, null, getLinkHeader())
//				.map(entityVOS -> entityVOS.stream().toList());

	}
}
