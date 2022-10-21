package org.fiware.tmforum.resourcefunction.domain;

import lombok.Getter;
import lombok.Setter;
import org.fiware.tmforum.common.domain.RefEntity;
import org.fiware.tmforum.mapping.annotations.AttributeGetter;
import org.fiware.tmforum.mapping.annotations.AttributeSetter;
import org.fiware.tmforum.mapping.annotations.AttributeType;
import org.fiware.tmforum.mapping.annotations.MappingEnabled;

import java.util.List;

@MappingEnabled(entityType = ResourceGraph.TYPE_RESOURCE_GRAPH)
public class ResourceGraphRelationship extends RefEntity {

    public ResourceGraphRelationship(String id) {
        super(id);
    }

    @Getter(onMethod = @__({@AttributeGetter(value = AttributeType.PROPERTY, targetName = "relationshipType", embedProperty = true)}))
    @Setter(onMethod = @__({@AttributeSetter(value = AttributeType.PROPERTY, targetName = "relationshipType", fromProperties = true)}))
    private String relationshipType;

    @Override
    public List<String> getReferencedTypes() {
        return List.of(ResourceGraph.TYPE_RESOURCE_GRAPH);
    }
}
