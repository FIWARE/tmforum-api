package org.fiware.tmforum.resourcefunction.domain;

import org.fiware.tmforum.common.domain.RefEntity;
import io.github.wistefan.mapping.annotations.MappingEnabled;

import java.util.List;

public class ResourceGraphRef extends RefEntity {

    public ResourceGraphRef(String id) {
        super(id);
    }

    @Override
    public List<String> getReferencedTypes() {
        return List.of(getAtReferredType());
    }
}
