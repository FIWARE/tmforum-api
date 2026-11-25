package org.fiware.tmforum.decoration;

import io.micronaut.context.annotation.Primary;
import io.micronaut.context.annotation.Requires;
import jakarta.inject.Singleton;
import org.fiware.tmforum.storage.EntityRepository;
import org.fiware.tmforum.storage.PagedResult;
import org.fiware.tmforum.storage.StorageAdapter;
import org.fiware.tmforum.storage.query.EntityQuery;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.util.function.Function;

/**
 * Pass-through entity decorator wired the way a real one must be: {@code @Primary}, adapter injected by qualifier.
 */
@Singleton
@Primary
@Requires(property = StorageAdapterQualifierTest.DECORATE_PROPERTY, value = StorageAdapterQualifierTest.DECORATE)
class RecordingEntityRepositoryDecorator implements EntityRepository {

    private final EntityRepository delegate;

    RecordingEntityRepositoryDecorator(@StorageAdapter EntityRepository delegate) {
        this.delegate = delegate;
    }

    EntityRepository delegate() {
        return delegate;
    }

    @Override
    public <T> Mono<T> get(URI id, Class<T> entityClass) {
        return delegate.get(id, entityClass);
    }

    @Override
    public <T> Mono<PagedResult<T>> findEntities(Integer offset, Integer limit, Class<T> entityClass,
                                                 EntityQuery query, String ids, String types) {
        return delegate.findEntities(offset, limit, entityClass, query, ids, types);
    }

    @Override
    public <T> Mono<PagedResult<T>> findEntities(Integer offset, Integer limit, String entityType,
                                                 Class<T> entityClass, EntityQuery query) {
        return delegate.findEntities(offset, limit, entityType, entityClass, query);
    }

    @Override
    public <T> Mono<PagedResult<T>> findEntitiesPolymorphic(Integer offset, Integer limit, String types,
                                                            EntityQuery query, String ids,
                                                            Function<String, Class<? extends T>> typeToClass) {
        return delegate.findEntitiesPolymorphic(offset, limit, types, query, ids, typeToClass);
    }

    @Override
    public <T> Mono<Void> createDomainEntity(T domainEntity) {
        return delegate.createDomainEntity(domainEntity);
    }

    @Override
    public <T> Mono<Void> updateDomainEntity(String id, T domainEntity) {
        return delegate.updateDomainEntity(id, domainEntity);
    }

    @Override
    public Mono<Void> deleteDomainEntity(URI id) {
        return delegate.deleteDomainEntity(id);
    }
}
