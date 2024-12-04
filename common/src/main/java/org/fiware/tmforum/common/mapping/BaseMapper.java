package org.fiware.tmforum.common.mapping;

import org.fiware.tmforum.common.domain.Entity;
import org.mapstruct.AfterMapping;
import org.mapstruct.MappingTarget;

/**
 * Extension for the tmforum-api mappers, to handle unknown properties to extend model-vos.
 */
public abstract class BaseMapper {


	@AfterMapping
	public void afterMappingToEntity(UnknownPreservingBase source, @MappingTarget Entity e) {
		if (source.getAtSchemaLocation() != null && source.getUnknownProperties() != null) {
			source.getUnknownProperties().forEach(e::addAdditionalProperties);
		}
	}

	@AfterMapping
	public void afterMappingFromEntity(Entity source, @MappingTarget UnknownPreservingBase target) {
		if (source.getAtSchemaLocation() != null && source.getAdditionalProperties() != null) {
			source.getAdditionalProperties()
					.forEach(additionalProperty -> target.setUnknownProperties(additionalProperty.getName(), additionalProperty.getValue()));
		}
	}

}
