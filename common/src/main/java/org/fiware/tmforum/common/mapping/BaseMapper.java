package org.fiware.tmforum.common.mapping;

import org.fiware.tmforum.common.domain.Entity;
import org.mapstruct.AfterMapping;
import org.mapstruct.MappingTarget;

import javax.inject.Inject;

public abstract class BaseMapper {

    @Inject
    private EntityExtender entityExtender;

    @AfterMapping
    public void afterMappingToEntity(Object source, @MappingTarget Entity e) {
        if (e.getAtSchemaLocation() != null) {
            entityExtender.handleExtension(source, e);
        }
    }

    @AfterMapping
    public void afterMappingFromEntity(Entity source, @MappingTarget Object target) {
        if (source.getAtSchemaLocation() != null) {
            entityExtender.storeExtensions(source);
        }
    }
}
