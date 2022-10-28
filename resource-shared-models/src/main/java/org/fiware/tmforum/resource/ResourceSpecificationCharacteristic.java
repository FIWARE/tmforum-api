package org.fiware.tmforum.resource;

import lombok.Data;
import org.fiware.tmforum.common.domain.TimePeriod;

import java.util.List;

@Data
public class ResourceSpecificationCharacteristic {

    private String id;
    private Boolean configurable;
    private Boolean extensible;
    private Boolean isUnique;
    private String name;
    private String regex;
    private String valueType;
    private String description;
    private Integer maxCardinality;
    private Integer minCardinality;
    private List<ResourceSpecificationCharacteristicRelationship> resourceSpecCharRelationship;
    private List<CharacteristicValue> resourceSpecCharacteristicValue;
    private TimePeriod validFor;

}
