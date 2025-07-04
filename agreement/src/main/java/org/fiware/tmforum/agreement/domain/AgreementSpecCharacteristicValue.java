package org.fiware.tmforum.agreement.domain;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.fiware.tmforum.common.domain.Entity;
import org.fiware.tmforum.common.domain.TimePeriod;

@Data
@EqualsAndHashCode(callSuper = true)
public class AgreementSpecCharacteristicValue extends Entity {
    private boolean _default;
    private String unitOfMeasure;
    private String valueFrom;
    private String valueTo;
    private String valueType;
    private TimePeriod validFor;
    private Object charValue;
}
