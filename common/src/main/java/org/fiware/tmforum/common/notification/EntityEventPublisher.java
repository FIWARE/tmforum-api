package org.fiware.tmforum.common.notification;

import reactor.core.publisher.Mono;

import java.net.URI;

/**
 * The API's writes together with their TMForum events. Each implementation reads the old state itself, right before
 * its write, so every backend reports the same "state the write replaced".
 */
public interface EntityEventPublisher {

	<T> Mono<Void> createWithEvent(T entity);

	/**
	 * Returns the state after the update; fails like {@code EntityWriteRepository.updateDomainEntity}.
	 */
	<T> Mono<T> updateWithEvent(String id, T patch, Class<T> entityClass);

	Mono<Void> deleteWithEvent(URI id);
}
