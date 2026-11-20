package org.fiware.tmforum.storage.postgresql;

import io.micronaut.context.annotation.Requires;
import jakarta.inject.Singleton;
import org.fiware.tmforum.common.notification.EntityEventPublisher;
import org.fiware.tmforum.common.notification.EventTypes;
import reactor.core.publisher.Mono;

import java.net.URI;

/**
 * PostgreSQL path: every write and its outbox row commit together; {@link OutboxDispatcher} delivers the event.
 * Types outside {@link EventTypes} get no row, since no dispatcher would ever claim it.
 */
@Singleton
@Requires(env = PostgresStorage.ENVIRONMENT)
public class OutboxEntityEventPublisher implements EntityEventPublisher {

    private final PostgresEntityRepository repository;
    private final EventTypes eventTypes;

    public OutboxEntityEventPublisher(PostgresEntityRepository repository, EventTypes eventTypes) {
        this.repository = repository;
        this.eventTypes = eventTypes;
    }

    @Override
    public <T> Mono<Void> createWithEvent(T entity) {
        return repository.createWithEvent(entity, eventTypes::contains);
    }

    @Override
    public <T> Mono<T> updateWithEvent(String id, T patch, Class<T> entityClass) {
        return repository.updateWithEvent(id, patch, entityClass, eventTypes::contains);
    }

    @Override
    public Mono<Void> deleteWithEvent(URI id) {
        return repository.deleteWithEvent(id, eventTypes::contains);
    }
}
