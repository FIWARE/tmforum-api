package org.fiware.tmforum.common.notification;

import reactor.core.publisher.Mono;

import java.net.URI;
import java.util.List;

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

	/**
	 * Deletes the entity and records its DELETE event.
	 *
	 * @param id            the id of the entity to delete
	 * @param acceptedTypes the NGSI-LD types the calling endpoint owns; a backend that can tell the stored type
	 *                      apart only deletes a row of one of them, so an id of another type is a NOT_FOUND
	 */
	Mono<Void> deleteWithEvent(URI id, List<String> acceptedTypes);
}
