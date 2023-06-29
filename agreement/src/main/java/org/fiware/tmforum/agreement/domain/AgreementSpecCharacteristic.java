package org.fiware.tmforum.agreement.domain;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

import org.fiware.tmforum.common.domain.Entity;
import org.fiware.tmforum.common.domain.TimePeriod;

@Data
@EqualsAndHashCode(callSuper = true)
public class AgreementSpecCharacteristic extends Entity {

    private boolean configurable;
    private String description;
    private String name;
    private String valueType;
    private List<AgreementSpecCharacteristicValue> specCharacteristicValue;
    private TimePeriod validFor;

}
