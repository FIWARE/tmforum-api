package org.fiware.tmforum.servicecatalog.domain;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.fiware.tmforum.common.domain.RefEntity;
import org.fiware.tmforum.mapping.annotations.AttributeGetter;
import org.fiware.tmforum.mapping.annotations.AttributeSetter;
import org.fiware.tmforum.mapping.annotations.AttributeType;
import org.fiware.tmforum.mapping.annotations.Ignore;

import java.net.URI;
import java.util.List;

@EqualsAndHashCode(callSuper = true)
public class ServiceLevelSpecificationRef extends RefEntity {

    @Getter(onMethod = @__({@AttributeGetter(value = AttributeType.PROPERTY, targetName = "version", embedProperty = true)}))
    @Setter(onMethod = @__({@AttributeSetter(value = AttributeType.PROPERTY, targetName = "version", targetClass = String.class)}))
    private String version;

    public ServiceLevelSpecificationRef(URI id) {
        super(id);
    }

    // TODO: update when TM657 is implemented
    @Override
    @Ignore
    public List<String> getReferencedTypes() {
        return List.of("service-level-specification");
    }
}