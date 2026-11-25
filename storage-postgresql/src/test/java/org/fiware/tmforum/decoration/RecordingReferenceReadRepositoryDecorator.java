package org.fiware.tmforum.decoration;

import io.micronaut.context.annotation.Primary;
import io.micronaut.context.annotation.Requires;
import jakarta.inject.Singleton;
import org.fiware.tmforum.storage.ReferenceReadRepository;
import org.fiware.tmforum.storage.StorageAdapter;
import org.fiware.tmforum.storage.StoredReference;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Collection;
import java.util.List;

/**
 * Pass-through reference decorator, the {@link ReferenceReadRepository} twin of {@link RecordingEntityRepositoryDecorator}.
 */
@Singleton
@Primary
@Requires(property = StorageAdapterQualifierTest.DECORATE_PROPERTY, value = StorageAdapterQualifierTest.DECORATE)
class RecordingReferenceReadRepositoryDecorator implements ReferenceReadRepository {

    private final ReferenceReadRepository delegate;

    RecordingReferenceReadRepositoryDecorator(@StorageAdapter ReferenceReadRepository delegate) {
        this.delegate = delegate;
    }

    ReferenceReadRepository delegate() {
        return delegate;
    }

    @Override
    public Mono<StoredReference> referenceExists(String id, List<String> acceptedTypes) {
        return delegate.referenceExists(id, acceptedTypes);
    }

    @Override
    public Mono<StoredReference> findReference(String id) {
        return delegate.findReference(id);
    }

    @Override
    public Flux<StoredReference> findReferences(Collection<String> ids) {
        return delegate.findReferences(ids);
    }
}
