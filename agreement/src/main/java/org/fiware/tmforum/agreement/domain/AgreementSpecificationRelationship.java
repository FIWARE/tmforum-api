package org.fiware.tmforum.agreement.domain;

import java.util.List;
import lombok.EqualsAndHashCode;
import org.fiware.tmforum.common.domain.RefEntity;
import org.fiware.tmforum.common.domain.TimePeriod;

import lombok.Getter;
import lombok.Setter;
import org.fiware.tmforum.mapping.annotations.AttributeGetter;
import org.fiware.tmforum.mapping.annotations.AttributeSetter;
import org.fiware.tmforum.mapping.annotations.AttributeType;

@EqualsAndHashCode(callSuper = true)
public class AgreementSpecificationRelationship extends RefEntity {
    public AgreementSpecificationRelationship(String id) {
        super(id);
    }

    @Getter(onMethod = @__({ @AttributeGetter(value = AttributeType.PROPERTY, targetName = "relationshipType") }))
    @Setter(onMethod = @__({ @AttributeSetter(value = AttributeType.PROPERTY, targetName = "relationshipType") }))
    private String relationshipType;

    @Getter(onMethod = @__({ @AttributeGetter(value = AttributeType.PROPERTY, targetName = "validFor") }))
    @Setter(onMethod = @__({ @AttributeSetter(value = AttributeType.PROPERTY, targetName = "validFor") }))
    private TimePeriod validFor;

    @Override
    public List<String> getReferencedTypes() {
        return List.of(AgreementSpecification.TYPE_AGSP);
    }
}
