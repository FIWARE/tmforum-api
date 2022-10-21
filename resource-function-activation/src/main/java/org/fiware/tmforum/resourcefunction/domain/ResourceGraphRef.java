package org.fiware.tmforum.resourcefunction.domain;

import org.fiware.tmforum.common.domain.RefEntity;
import org.fiware.tmforum.mapping.annotations.MappingEnabled;

import java.util.List;

@MappingEnabled(entityType = ResourceGraph.TYPE_RESOURCE_GRAPH)
public class ResourceGraphRef extends RefEntity {

    public ResourceGraphRef(String id) {
        super(id);
    }

    @Override
    public List<String> getReferencedTypes() {
        return List.of(ResourceGraph.TYPE_RESOURCE_GRAPH);
    }
}
