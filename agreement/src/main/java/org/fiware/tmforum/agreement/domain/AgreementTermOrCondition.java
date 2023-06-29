package org.fiware.tmforum.agreement.domain;

import org.fiware.tmforum.common.domain.EntityWithId;
import org.fiware.tmforum.common.domain.TimePeriod;

import io.github.wistefan.mapping.annotations.AttributeGetter;
import io.github.wistefan.mapping.annotations.AttributeSetter;
import io.github.wistefan.mapping.annotations.AttributeType;
import io.github.wistefan.mapping.annotations.MappingEnabled;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

@EqualsAndHashCode(callSuper = true)
@MappingEnabled(entityType = AgreementTermOrCondition.TYPE_AGT)
public class AgreementTermOrCondition extends EntityWithId {
    public static final String TYPE_AGT = "agreementTermOrCondition";

    public AgreementTermOrCondition(String id) {
        super(TYPE_AGT, id);
    }

    @Getter(onMethod = @__({ @AttributeGetter(value = AttributeType.PROPERTY, targetName = "description") }))
    @Setter(onMethod = @__({ @AttributeSetter(value = AttributeType.PROPERTY, targetName = "description") }))
    private String description;

    @Getter(onMethod = @__({ @AttributeGetter(value = AttributeType.PROPERTY, targetName = "validFor") }))
    @Setter(onMethod = @__({ @AttributeSetter(value = AttributeType.PROPERTY, targetName = "validFor") }))
    private TimePeriod validFor;

}
