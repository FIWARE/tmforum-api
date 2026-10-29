package org.fiware.tmforum.common.repository;

import io.github.wistefan.mapping.EntityVOMapper;
import io.github.wistefan.mapping.JavaObjectMapper;
import io.github.wistefan.mapping.annotations.MappingEnabled;
import io.micronaut.http.HttpResponse;
import lombok.extern.slf4j.Slf4j;
import org.fiware.ngsi.api.EntitiesApiClient;
import org.fiware.ngsi.api.SubscriptionsApiClient;
import org.fiware.tmforum.common.configuration.GeneralProperties;
import org.fiware.tmforum.common.mapping.NGSIMapper;
import org.fiware.tmforum.storage.EntityRepository;
import org.fiware.tmforum.storage.PagedResult;
import org.fiware.tmforum.storage.StorageException;
import org.fiware.tmforum.storage.StorageOperation;
import reactor.core.publisher.Mono;

import jakarta.inject.Singleton;
import java.net.URI;
import java.util.Arrays;
import java.util.function.Function;
import java.util.stream.Stream;

@Slf4j
@Singleton
public class TmForumRepository extends NgsiLdBaseRepository implements EntityRepository, SubscriptionRepository {

    private static final String LIST_FAILED = "Was not able to list entities.";

    public TmForumRepository(GeneralProperties generalProperties, EntitiesApiClient entitiesApi,
                             SubscriptionsApiClient subscriptionsApi, EntityVOMapper entityVOMapper,
                             NGSIMapper ngsiMapper, JavaObjectMapper javaObjectMapper) {
        super(generalProperties, entitiesApi, subscriptionsApi, javaObjectMapper, ngsiMapper, entityVOMapper);
    }

    @Override
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

    @Override
    public <T> Mono<PagedResult<T>> findEntities(Integer offset, Integer limit, Class<T> entityClass,
                                          String query, String ids, String types, String orderBy) {
        return entitiesApi.queryEntities(generalProperties.getTenant(),
                        ids,
                        null,
                        types,
                        null,
                        query,
                        orderBy,
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
                .flatMap(response -> zipToList(response.body() != null ? response.body().stream() : Stream.empty(), entityClass)
                        .map(entities -> new PagedResult<>(entities, offset, limit, extractTotalCount(response))))
                .onErrorMap(TmForumRepository::listFailure);
    }

    /**
     * Query several NGSI-LD entity types in a single broker call (NGSI-LD's {@code type} parameter
     * accepts a comma-separated list with OR semantics), instead of one call per type. This lets the
     * broker apply {@code offset}/{@code limit} and report the count against the combined result set,
     * rather than callers fanning out per-type queries and merging paginated slices themselves.
     * <p>
     * Since the result is a mix of entity types, each entity is mapped to its own domain class via
     * {@code typeToClass} instead of a single fixed class.
     */
    @Override
    public <T> Mono<PagedResult<T>> findEntitiesPolymorphic(Integer offset, Integer limit, String types,
                                          String query, String ids, String orderBy, Function<String, Class<? extends T>> typeToClass) {
        return entitiesApi.queryEntities(generalProperties.getTenant(),
                        ids,
                        null,
                        types,
                        null,
                        query,
                        orderBy,
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
                .flatMap(response -> zipToPolymorphicList(response.body() != null ? response.body().stream() : Stream.empty(), typeToClass)
                        .map(entities -> new PagedResult<>(entities, offset, limit, extractTotalCount(response))))
                .onErrorMap(TmForumRepository::listFailure);
    }

    // wraps every failure, a nested READ from relationship resolution included, so that a list always renders alike
    private static StorageException listFailure(Throwable failure) {
        log.warn(LIST_FAILED, failure);
        return new StorageException(StorageOperation.LIST, BrokerFailures.unknownOrUnavailable(failure), LIST_FAILED,
                failure);
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
