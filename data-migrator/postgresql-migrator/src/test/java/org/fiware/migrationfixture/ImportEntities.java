package org.fiware.migrationfixture;

import io.github.wistefan.mapping.annotations.MappingEnabled;
import lombok.Getter;
import lombok.Setter;
import org.fiware.tmforum.common.domain.EntityWithId;
import org.fiware.tmforum.common.domain.VersionedEntity;

/**
 * A versioned and an unversioned type for the import tests; outside {@code org.fiware.tmforum}, so no scan sees them.
 */
public final class ImportEntities {

	public static final String VERSIONED_TYPE = "test-versioned";
	public static final String PLAIN_TYPE = "test-plain";

	private ImportEntities() {
	}

	@Getter
	@Setter
	@MappingEnabled(entityType = VERSIONED_TYPE)
	public static class Versioned extends EntityWithId implements VersionedEntity {
		private String version;

		public Versioned(String id) {
			super(VERSIONED_TYPE, id);
		}
	}

	@MappingEnabled(entityType = PLAIN_TYPE)
	public static class Plain extends EntityWithId {
		public Plain(String id) {
			super(PLAIN_TYPE, id);
		}
	}
}
