package org.fiware.tmforum.common.repository;

import io.github.wistefan.mapping.EntityVOMapper;
import io.github.wistefan.mapping.JavaObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.fiware.ngsi.api.EntitiesApiClient;
import org.fiware.ngsi.api.SubscriptionsApiClient;
import org.fiware.tmforum.common.configuration.GeneralProperties;
import org.fiware.tmforum.common.exception.TmForumException;
import org.fiware.tmforum.common.exception.TmForumExceptionReason;
import org.fiware.tmforum.common.mapping.NGSIMapper;
import reactor.core.publisher.Mono;

import javax.inject.Singleton;
import java.net.URI;
import java.util.List;

@Slf4j
@Singleton
public class TmForumRepository extends NgsiLdBaseRepository {

	public TmForumRepository(GeneralProperties generalProperties, EntitiesApiClient entitiesApi,
							 SubscriptionsApiClient subscriptionsApi, EntityVOMapper entityVOMapper,
							 NGSIMapper ngsiMapper, JavaObjectMapper javaObjectMapper) {
		super(generalProperties, entitiesApi, subscriptionsApi, javaObjectMapper, ngsiMapper, entityVOMapper);
	}

	public <T> Mono<T> get(URI id, Class<T> entityClass) {
		return retrieveEntityById(id)
				.flatMap(entityVO -> entityVOMapper.fromEntityVO(entityVO, entityClass));
	}

	public <T> Mono<List<T>> findEntities(Integer offset, Integer limit, String entityType, Class<T> entityClass,
			String query) {
		return entitiesApi.queryEntities(generalProperties.getTenant(),
						null,
						null,
						entityType,
						null,
						query,
						null,
						null,
						null,
						null,
						null,
						limit,
						offset,
						null,
						getLinkHeader())
				.map(List::stream)
				.flatMap(entityVOStream -> zipToList(entityVOStream, entityClass))
				.onErrorResume(t -> {
					log.warn("Was not able to list entities.", t);
					throw new TmForumException("Was not able to list entities.", t, TmForumExceptionReason.UNKNOWN);
				});
	}

}
