package org.fiware.tmforum.resourcefunction.domain;

import lombok.Getter;
import lombok.Setter;
import org.fiware.tmforum.common.domain.RefEntity;
import org.fiware.tmforum.mapping.annotations.AttributeGetter;
import org.fiware.tmforum.mapping.annotations.AttributeSetter;
import org.fiware.tmforum.mapping.annotations.AttributeType;
import org.fiware.tmforum.mapping.annotations.MappingEnabled;

import java.util.List;

@MappingEnabled(entityType = ResourceFunction.TYPE_RESOURCE_FUNCTION)
public class ResourceFunctionRef extends RefEntity {

    @Getter(onMethod = @__({@AttributeGetter(value = AttributeType.PROPERTY, targetName = "version")}))
    @Setter(onMethod = @__({@AttributeSetter(value = AttributeType.PROPERTY, targetName = "version")}))
    private String version;

    public ResourceFunctionRef(String id) {
        super(id);
    }

    @Override
    public List<String> getReferencedTypes() {
        return List.of(ResourceFunction.TYPE_RESOURCE_FUNCTION);
    }
}
