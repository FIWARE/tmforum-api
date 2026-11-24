package org.fiware.migrationfixture;

import io.github.wistefan.mapping.annotations.MappingEnabled;
import org.fiware.tmforum.common.domain.EntityWithId;

/**
 * Two classes claiming one entity type; outside {@code org.fiware.tmforum}, so the registry's scan does not see them.
 */
public final class AmbiguousEntities {

	public static final String SHARED_TYPE = "shared-type";

	private AmbiguousEntities() {
	}

	@MappingEnabled(entityType = SHARED_TYPE)
	public static class First extends EntityWithId {
		public First(String id) {
			super(SHARED_TYPE, id);
		}
	}

	@MappingEnabled(entityType = SHARED_TYPE)
	public static class Second extends EntityWithId {
		public Second(String id) {
			super(SHARED_TYPE, id);
		}
	}
}
