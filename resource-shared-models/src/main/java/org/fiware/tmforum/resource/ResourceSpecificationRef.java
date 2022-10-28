package org.fiware.tmforum.resource;

import lombok.EqualsAndHashCode;
import org.fiware.tmforum.common.domain.RefEntity;
import org.fiware.tmforum.mapping.annotations.MappingEnabled;

import java.net.URI;
import java.util.List;

@MappingEnabled(entityType = "resource-specification")
@EqualsAndHashCode(callSuper = true)
public class ResourceSpecificationRef extends RefEntity {

    public ResourceSpecificationRef(URI id) {
        super(id);
    }

    @Override
    public List<String> getReferencedTypes() {
        return List.of("resource-specification");
    }
}
