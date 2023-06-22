package org.fiware.tmforum.service;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.fiware.tmforum.common.domain.RefEntity;
import org.fiware.tmforum.common.domain.TimePeriod;
import io.github.wistefan.mapping.annotations.AttributeGetter;
import io.github.wistefan.mapping.annotations.AttributeSetter;
import io.github.wistefan.mapping.annotations.AttributeType;
import org.fiware.tmforum.servicecatalog.domain.ServiceSpecification;

import java.net.URI;
import java.util.List;

@EqualsAndHashCode(callSuper = true)
public class ServiceSpecificationRelationship extends RefEntity {

    @Getter(onMethod = @__({@AttributeGetter(value = AttributeType.PROPERTY, targetName = "relationshipType", embedProperty = true)}))
    @Setter(onMethod = @__({@AttributeSetter(value = AttributeType.PROPERTY, targetName = "relationshipType", targetClass = String.class)}))
    private String relationshipType;

    @Getter(onMethod = @__({@AttributeGetter(value = AttributeType.PROPERTY, targetName = "role", embedProperty = true)}))
    @Setter(onMethod = @__({@AttributeSetter(value = AttributeType.PROPERTY, targetName = "role", targetClass = String.class)}))
    private String role;

    @Getter(onMethod = @__({@AttributeGetter(value = AttributeType.PROPERTY, targetName = "validFor", embedProperty = true)}))
    @Setter(onMethod = @__({@AttributeSetter(value = AttributeType.PROPERTY, targetName = "validFor", targetClass = TimePeriod.class)}))
    private TimePeriod validFor;

    public ServiceSpecificationRelationship(URI id) {
        super(id);
    }

    @Override
    public List<String> getReferencedTypes() {
        return List.of(ServiceSpecification.TYPE_SERVICE_SPECIFICATION);
    }
}
