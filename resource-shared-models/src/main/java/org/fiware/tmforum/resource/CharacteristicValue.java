package org.fiware.tmforum.resource;

import lombok.Data;
import org.fiware.tmforum.common.domain.TimePeriod;

import java.net.URI;

@Data
public class CharacteristicValue {

    private Boolean isDefault;
    private String rangeInterval;
    private String regex;
    private String unitOfMeasure;
    private Integer valueFrom;
    private Integer valueTo;
    private String valueType;
    private Object charValue;
    private TimePeriod validFor;
    private String atBaseType;
    private URI atSchemaLocation;
    private String atType;
}
