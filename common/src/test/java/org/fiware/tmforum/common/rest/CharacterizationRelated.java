package org.fiware.tmforum.common.rest;

import io.github.wistefan.mapping.annotations.MappingEnabled;
import org.fiware.tmforum.common.domain.EntityWithId;

/**
 * Related entity of {@link CharacterizationEntity}, written by the related-entity handling and looked up on read.
 */
@MappingEnabled(entityType = CharacterizationRelated.TYPE)
public class CharacterizationRelated extends EntityWithId {

	public static final String TYPE = "characterization-related";

	public CharacterizationRelated(String id) {
		super(TYPE, id);
	}
}
