package org.fiware.tmforum.migration.postgresql;

import org.fiware.ngsi.model.EntityVO;

import java.util.List;
import java.util.Set;

/**
 * Where the import reads entities from; {@link BrokerSource} in production, a fake in unit tests.
 */
interface EntitySource {

	/**
	 * One page of the given type, with system attributes; {@code totalCount} is the broker's count of the type.
	 */
	Page page(String type, int offset, int limit);

	/**
	 * Maps the entity like the application's NGSI-LD adapter does. Throws on a mapping failure.
	 */
	<T> T toDomain(EntityVO entity, Class<T> domainClass);

	/**
	 * Every entity type the source holds. Throws when the source cannot list them.
	 */
	Set<String> types();

	/**
	 * {@code listed} is how many entities the broker listed; {@code entities} holds fewer when one vanished before
	 * its re-read by id. Offsets advance by {@code listed}.
	 */
	record Page(List<EntityVO> entities, int listed, long totalCount) {

		Page(List<EntityVO> entities, long totalCount) {
			this(entities, entities.size(), totalCount);
		}
	}
}
