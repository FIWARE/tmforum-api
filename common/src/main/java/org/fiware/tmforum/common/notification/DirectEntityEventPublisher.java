package org.fiware.tmforum.common.notification;

import io.micronaut.context.annotation.Requires;
import jakarta.inject.Singleton;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.fiware.tmforum.common.CommonConstants;
import org.fiware.tmforum.storage.EntityRepository;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.util.Optional;

/**
 * Broker path: writes, then fires the event without waiting for it. DeleteEvents are not fired here, the broker's
 * NGSI-LD subscriptions deliver them through {@link NgsiLdEventHandler}.
 */
@Slf4j
@Singleton
@RequiredArgsConstructor
@Requires(notEnv = CommonConstants.POSTGRESQL_ENVIRONMENT)
public class DirectEntityEventPublisher implements EntityEventPublisher {

	private final EntityRepository repository;
	private final TMForumEventHandler eventHandler;

	@Override
	public <T> Mono<Void> createWithEvent(T entity) {
		return repository.createDomainEntity(entity)
				.doOnSuccess(v -> fireAndForget(eventHandler.handleCreateEvent(entity), "create"));
	}

	@Override
	public <T> Mono<T> updateWithEvent(String id, T patch, Class<T> entityClass) {
		URI idUri = URI.create(id);
		return repository.get(idUri, entityClass)
				.map(Optional::of)
				.defaultIfEmpty(Optional.empty())
				.flatMap(oldState -> repository.updateDomainEntity(id, patch)
						.then(repository.get(idUri, entityClass))
						.doOnNext(newState -> oldState.ifPresent(old ->
								fireAndForget(eventHandler.handleUpdateEvent(newState, old), "update"))));
	}

	@Override
	public Mono<Void> deleteWithEvent(URI id) {
		return repository.deleteDomainEntity(id);
	}

	private static void fireAndForget(Mono<Void> event, String kind) {
		event.subscribe(null, e -> log.warn("Was not able to handle the {} event.", kind, e));
	}
}
