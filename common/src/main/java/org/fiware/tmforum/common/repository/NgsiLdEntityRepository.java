package org.fiware.tmforum.common.repository;

import io.github.wistefan.mapping.EntityVOMapper;
import io.github.wistefan.mapping.JavaObjectMapper;
import io.micronaut.http.HttpResponse;
import lombok.extern.slf4j.Slf4j;
import org.fiware.ngsi.api.EntitiesApiClient;
import org.fiware.ngsi.api.SubscriptionsApiClient;
import org.fiware.tmforum.common.configuration.GeneralProperties;
import org.fiware.tmforum.common.exception.QueryException;
import org.fiware.tmforum.common.mapping.IdHelper;
import org.fiware.tmforum.common.mapping.NGSIMapper;
import org.fiware.tmforum.common.querying.NgsiLdQueryRenderer;
import org.fiware.tmforum.storage.EntityRepository;
import org.fiware.tmforum.storage.PagedResult;
import org.fiware.tmforum.storage.StorageException;
import org.fiware.tmforum.storage.StorageOperation;
import org.fiware.tmforum.storage.query.EntityQuery;
import reactor.core.publisher.Mono;

import jakarta.inject.Singleton;
import java.net.URI;
import java.util.function.Function;
import java.util.stream.Stream;

@Slf4j
@Singleton
public class NgsiLdEntityRepository extends NgsiLdBaseRepository implements EntityRepository, SubscriptionRepository {

    private static final String LIST_FAILED = "Was not able to list entities.";

    private final NgsiLdQueryRenderer queryRenderer;

    public NgsiLdEntityRepository(GeneralProperties generalProperties, EntitiesApiClient entitiesApi,
                                  SubscriptionsApiClient subscriptionsApi, EntityVOMapper entityVOMapper,
                                  NGSIMapper ngsiMapper, JavaObjectMapper javaObjectMapper,
                                  NgsiLdQueryRenderer queryRenderer) {
        super(generalProperties, entitiesApi, subscriptionsApi, javaObjectMapper, ngsiMapper, entityVOMapper);
        this.queryRenderer = queryRenderer;
    }

    @Override
    public <T> Mono<T> get(URI id, Class<T> entityClass) {
        if (!IdHelper.hasTypeSegmentOf(id, entityClass)) {
            log.warn("Id {} has no type segment of {}.", id, entityClass.getSimpleName());
            return Mono.empty();
        }
        return retrieveEntityById(id)
                .flatMap(entityVO -> entityVOMapper.fromEntityVO(entityVO, entityClass));
    }

    @Override
    public <T> Mono<PagedResult<T>> findEntities(Integer offset, Integer limit, Class<T> entityClass,
                                          EntityQuery query, String ids, String types) {
        // deferred, so that a filter the renderer rejects arrives as an error signal like every other list failure
        return Mono.defer(() -> entitiesApi.queryEntities(generalProperties.getTenant(),
                        ids,
                        null,
                        types,
                        null,
                        renderFilter(query),
                        renderOrderBy(query),
                        null,
                        null,
                        null,
                        null,
                        null,
                        limit,
                        offset,
                        true,
                        null,
                        getLinkHeader()))
                .flatMap(response -> zipToList(response.body() != null ? response.body().stream() : Stream.empty(), entityClass)
                        .map(entities -> new PagedResult<>(entities, offset, limit, extractTotalCount(response))))
                .onErrorMap(NgsiLdEntityRepository::listFailure);
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
                                          EntityQuery query, String ids, Function<String, Class<? extends T>> typeToClass) {
        // deferred, so that a filter the renderer rejects arrives as an error signal like every other list failure
        return Mono.defer(() -> entitiesApi.queryEntities(generalProperties.getTenant(),
                        ids,
                        null,
                        types,
                        null,
                        renderFilter(query),
                        renderOrderBy(query),
                        null,
                        null,
                        null,
                        null,
                        null,
                        limit,
                        offset,
                        true,
                        null,
                        getLinkHeader()))
                .flatMap(response -> zipToPolymorphicList(response.body() != null ? response.body().stream() : Stream.empty(), typeToClass)
                        .map(entities -> new PagedResult<>(entities, offset, limit, extractTotalCount(response))))
                .onErrorMap(NgsiLdEntityRepository::listFailure);
    }

    private String renderFilter(EntityQuery query) {
        return query == null ? null : queryRenderer.renderFilter(query);
    }

    private String renderOrderBy(EntityQuery query) {
        return query == null ? null : queryRenderer.renderOrderBy(query);
    }

    // wraps every failure, a nested READ from relationship resolution included, so that a list always renders alike;
    // a filter rejected as the client's error stays a QueryException (400), as on PostgreSQL
    private static Throwable listFailure(Throwable failure) {
        if (failure instanceof QueryException) {
            return failure;
        }
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
