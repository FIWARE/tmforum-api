package org.fiware.tmforum.common.repository;

import io.github.wistefan.mapping.EntityVOMapper;
import io.github.wistefan.mapping.JavaObjectMapper;
import io.github.wistefan.mapping.annotations.MappingEnabled;
import io.micronaut.http.HttpResponse;
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
import java.util.Arrays;

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

        if (mappingAnnotation == null) {
            throw new IllegalArgumentException(
                    String.format("Class %s missing @MappingEnabled annotation", entityClass.getName()));
        }

        String[] classType = mappingAnnotation.entityType();

        if (!Arrays.asList(classType).contains(requestedType)) {
            log.warn("Entity {} has type {} but expected type was {}",
                    id, requestedType, classType);
            return Mono.empty();
        }

        return retrieveEntityById(id)
                .flatMap(entityVO -> entityVOMapper.fromEntityVO(entityVO, entityClass));
    }

    public <T> Mono<PagedResult<T>> findEntities(Integer offset, Integer limit, Class<T> entityClass,
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
                        true,
                        null,
                        getLinkHeader())
                .flatMap(response -> zipToList(response.body().stream(), entityClass)
                        .map(entities -> new PagedResult<>(entities, offset, limit, extractTotalCount(response))))
                .onErrorResume(t -> {
                    log.warn("Was not able to list entities.", t);
                    throw new TmForumException("Was not able to list entities.", t, TmForumExceptionReason.UNKNOWN);
                });
    }


    public <T> Mono<PagedResult<T>> findEntities(Integer offset, Integer limit, String entityType, Class<T> entityClass,
                                          String query) {
        return findEntities(offset, limit, entityClass, query, null, entityType);
    }

    Integer extractTotalCount(HttpResponse<?> response) {
        String headerName = generalProperties.getCountHeader();
        if (headerName == null) {
            return null;
        }
        String headerValue = response.header(headerName);
        if (headerValue == null) {
            return null;
        }
        try {
            return Integer.parseInt(headerValue);
        } catch (NumberFormatException e) {
            log.warn("Total count header {} did not contain a valid integer: {}", headerName, headerValue);
            return null;
        }
    }

}
