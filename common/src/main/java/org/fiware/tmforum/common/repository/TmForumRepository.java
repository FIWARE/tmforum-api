package org.fiware.tmforum.common.repository;

import io.github.wistefan.mapping.EntityVOMapper;
import io.github.wistefan.mapping.JavaObjectMapper;
import io.github.wistefan.mapping.annotations.MappingEnabled;
import lombok.extern.slf4j.Slf4j;
import org.fiware.ngsi.api.EntitiesApiClient;
import org.fiware.ngsi.api.SubscriptionsApiClient;
import org.fiware.tmforum.common.configuration.GeneralProperties;
import org.fiware.tmforum.common.exception.TmForumException;
import org.fiware.tmforum.common.exception.TmForumExceptionReason;
import org.fiware.tmforum.common.mapping.IdHelper;
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

        String idString = id.toString();
        String[] parts = idString.split(":", 4);


        if (parts.length < 3) {
            log.warn("Invalid NGSI-LD ID format, expected at least 3 parts: {}", idString);
            return Mono.empty();
        }

        String requestedType = parts[2];

        // Extract entity type from MappingEnabled
        MappingEnabled mappingAnnotation = entityClass.getAnnotation(MappingEnabled.class);

        // prevent NullPointerException
        if (mappingAnnotation == null) {
            log.warn("Class {} missing @MappingEnabled annotation", entityClass.getName());
            return retrieveEntityById(id)
                    .flatMap(entityVO -> entityVOMapper.fromEntityVO(entityVO, entityClass));
        }

        String classType = mappingAnnotation.entityType()[0];

        if (!requestedType.equals(classType)) {
            log.warn("Entity {} has type {} but expected type was {}",
                    id, requestedType, classType);
            return Mono.empty();
        }

        return retrieveEntityById(id)
                .flatMap(entityVO -> entityVOMapper.fromEntityVO(entityVO, entityClass));
    }

    public <T> Mono<List<T>> findEntities(Integer offset, Integer limit, Class<T> entityClass,
                                          String query, String ids, String types) {
        return entitiesApi.queryEntities(generalProperties.getTenant(),
                        ids,
                        null,
                        types,
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


    public <T> Mono<List<T>> findEntities(Integer offset, Integer limit, String entityType, Class<T> entityClass,
                                          String query) {
        return findEntities(offset, limit, entityClass, query, null, entityType);
    }

}
