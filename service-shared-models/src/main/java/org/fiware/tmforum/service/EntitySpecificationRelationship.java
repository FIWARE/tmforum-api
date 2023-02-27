package org.fiware.tmforum.service;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.fiware.tmforum.common.domain.RefEntity;
import org.fiware.tmforum.common.domain.TimePeriod;
import org.fiware.tmforum.mapping.annotations.AttributeGetter;
import org.fiware.tmforum.mapping.annotations.AttributeSetter;
import org.fiware.tmforum.mapping.annotations.AttributeType;

import java.net.URI;
import java.util.List;

@EqualsAndHashCode(callSuper = true)
public class EntitySpecificationRelationship extends RefEntity {


    @Getter(onMethod = @__({@AttributeGetter(value = AttributeType.PROPERTY, targetName = "relationshipType", embedProperty = true)}))
    @Setter(onMethod = @__({@AttributeSetter(value = AttributeType.PROPERTY, targetName = "relationshipType", targetClass = String.class)}))
    private String relationshipType;

    @Getter(onMethod = @__({@AttributeGetter(value = AttributeType.PROPERTY, targetName = "role", embedProperty = true)}))
    @Setter(onMethod = @__({@AttributeSetter(value = AttributeType.PROPERTY, targetName = "role", targetClass = String.class)}))
    private String role;

    @Getter(onMethod = @__({@AttributeGetter(value = AttributeType.RELATIONSHIP, targetName = "associationSpec", embedProperty = true)}))
    @Setter(onMethod = @__({@AttributeSetter(value = AttributeType.RELATIONSHIP, targetName = "associationSpec", targetClass = AssociationSpecificationRef.class)}))
    private AssociationSpecificationRef associationSpec;

    @Getter(onMethod = @__({@AttributeGetter(value = AttributeType.PROPERTY, targetName = "validFor", embedProperty = true)}))
    @Setter(onMethod = @__({@AttributeSetter(value = AttributeType.PROPERTY, targetName = "validFor", targetClass = TimePeriod.class)}))
    private TimePeriod validFor;

    public EntitySpecificationRelationship(URI id) {
        super(id);
    }

    @Override
    public List<String> getReferencedTypes() {
        return List.of(getAtReferredType());
    }
}
