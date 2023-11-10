package org.fiware.tmforum.resource;

import lombok.EqualsAndHashCode;
import org.fiware.tmforum.common.domain.RefEntity;
import io.github.wistefan.mapping.annotations.MappingEnabled;

import java.util.List;

@MappingEnabled(entityType = "resource-specification")
@EqualsAndHashCode(callSuper = true)
public class ResourceSpecificationRef extends RefEntity {

    public ResourceSpecificationRef(String id) {
        super(id);
    }

    @Override
    public List<String> getReferencedTypes() {
        return List.of("resource-specification");
    }
}
