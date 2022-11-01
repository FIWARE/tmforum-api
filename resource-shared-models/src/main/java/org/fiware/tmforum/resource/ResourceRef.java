package org.fiware.tmforum.resource;

import lombok.EqualsAndHashCode;
import org.fiware.tmforum.common.domain.RefEntity;

import java.util.List;

@EqualsAndHashCode(callSuper = true)
public class ResourceRef extends RefEntity {

    public ResourceRef(String id) {
        super(id);
    }

    @Override
    public List<String> getReferencedTypes() {
        return List.of(Resource.TYPE_RESOURCE);
    }
}
