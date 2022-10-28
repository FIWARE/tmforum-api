package org.fiware.tmforum.servicecatalog.domain;

import lombok.Data;
import org.fiware.tmforum.common.domain.TimePeriod;

import java.net.URI;

@Data
public class CharacteristicValueSpecification {

    private Boolean isDefault;
    private String rangeInterval;
    private String regex;
    private String unitOfMeasure;
    private Integer valueFrom;
    private Integer valueTo;
    private String valueType;
    private TimePeriod validFor;
    private Object value;
    private String atBaseType;
    private URI atSchemaLocation;
    private String atType;
}
