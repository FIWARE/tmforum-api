package org.fiware.tmforum.agreement.domain;

import java.util.List;

import org.fiware.tmforum.common.domain.RefEntity;

import io.github.wistefan.mapping.annotations.AttributeGetter;
import io.github.wistefan.mapping.annotations.AttributeSetter;
import io.github.wistefan.mapping.annotations.AttributeType;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

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
