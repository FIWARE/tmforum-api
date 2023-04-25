package org.fiware.tmforum.agreement.domain;

import java.util.List;
import lombok.EqualsAndHashCode;
import org.fiware.tmforum.common.domain.RefEntity;
import lombok.Getter;
import lombok.Setter;
import org.fiware.tmforum.mapping.annotations.AttributeGetter;
import org.fiware.tmforum.mapping.annotations.AttributeSetter;
import org.fiware.tmforum.mapping.annotations.AttributeType;

@EqualsAndHashCode(callSuper = true)
public class AgreementSpecificationRef extends RefEntity {

    public AgreementSpecificationRef(String id) {
        super(id);
    }

    @Getter(onMethod = @__({ @AttributeGetter(value = AttributeType.PROPERTY, targetName = "description") }))
    @Setter(onMethod = @__({ @AttributeSetter(value = AttributeType.PROPERTY, targetName = "description") }))
    private String description;

    @Override
    public List<String> getReferencedTypes() {
        return List.of(AgreementSpecification.TYPE_AGSP);
    }

}
