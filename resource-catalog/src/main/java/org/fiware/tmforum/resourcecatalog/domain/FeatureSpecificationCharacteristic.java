package org.fiware.tmforum.resourcecatalog.domain;

import lombok.Data;
import org.fiware.tmforum.common.domain.TimePeriod;

import java.util.List;


@Data
public class FeatureSpecificationCharacteristic {

    private Boolean configurable;
    private Boolean extensible;
    private Boolean isUnique;
    private Integer maxCardinality;
    private Integer minCardinality;
    private String name;
    private String regex;
    private String valueType;
    private List<CharacteristicValue> featureSpecCharacteristicValue;
    private List<FeatureSpecificationCharacteristicRelationship> featureSpecCharRelationship;
    private TimePeriod validFor;
}
