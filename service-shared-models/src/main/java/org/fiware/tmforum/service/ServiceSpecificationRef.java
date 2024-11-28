package org.fiware.tmforum.service;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.fiware.tmforum.common.domain.RefEntity;
import io.github.wistefan.mapping.annotations.AttributeGetter;
import io.github.wistefan.mapping.annotations.AttributeSetter;
import io.github.wistefan.mapping.annotations.AttributeType;
import io.github.wistefan.mapping.annotations.Ignore;
import io.github.wistefan.mapping.annotations.MappingEnabled;

import org.fiware.tmforum.servicecatalog.domain.ServiceSpecification;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

@EqualsAndHashCode(callSuper = true)
@MappingEnabled(entityType = ServiceSpecification.TYPE_SERVICE_SPECIFICATION)
public class ServiceSpecificationRef extends RefEntity {

    @Getter(onMethod = @__({@AttributeGetter(value = AttributeType.PROPERTY, targetName = "version", embedProperty = true)}))
    @Setter(onMethod = @__({@AttributeSetter(value = AttributeType.PROPERTY, targetName = "version", targetClass = String.class)}))
    private String version;

    public ServiceSpecificationRef(@JsonProperty("id") String id) {
        super(id);
    }

    @Override
    @JsonIgnore
    public List<String> getReferencedTypes() {
        return new ArrayList<>(List.of(ServiceSpecification.TYPE_SERVICE_SPECIFICATION));
    }

}
