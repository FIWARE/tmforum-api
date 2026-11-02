package org.fiware.tmforum.common.rest;

import org.fiware.tmforum.common.validation.ReferencedEntity;

import java.net.URI;
import java.util.List;

/**
 * Local reference checked by the reference validation of {@link CharacterizationTestController}.
 */
public class CharacterizationReference implements ReferencedEntity {

	private final URI entityId;

	public CharacterizationReference(URI entityId) {
		this.entityId = entityId;
	}

	@Override
	public List<String> getReferencedTypes() {
		return List.of(CharacterizationRelated.TYPE);
	}

	@Override
	public URI getEntityId() {
		return entityId;
	}
}
