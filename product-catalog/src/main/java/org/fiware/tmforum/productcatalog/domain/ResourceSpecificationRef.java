package org.fiware.tmforum.productcatalog.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.fiware.tmforum.common.domain.RefEntity;
import io.github.wistefan.mapping.annotations.AttributeGetter;
import io.github.wistefan.mapping.annotations.AttributeSetter;
import io.github.wistefan.mapping.annotations.AttributeType;
import io.github.wistefan.mapping.annotations.MappingEnabled;
import org.fiware.tmforum.resource.ResourceSpecification;

import java.util.ArrayList;
import java.util.List;

@MappingEnabled(entityType = ResourceSpecification.TYPE_RESOURCE_SPECIFICATION)
@EqualsAndHashCode(callSuper = true)
public class ResourceSpecificationRef extends RefEntity {

    @Getter(onMethod = @__({
            @AttributeGetter(value = AttributeType.PROPERTY, targetName = "version", embedProperty = true)}))
    @Setter(onMethod = @__({
            @AttributeSetter(value = AttributeType.PROPERTY, targetName = "version", fromProperties = true)}))
    private String version;

    public ResourceSpecificationRef(@JsonProperty("id") String id) {
        super(id);
    }

    @Override
    @JsonIgnore
    public List<String> getReferencedTypes() {
        return new ArrayList<>(List.of(ResourceSpecification.TYPE_RESOURCE_SPECIFICATION));
    }
}
