package org.fiware.tmforum.resourcecatalog.domain;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import org.fiware.resourcecatalog.model.TimePeriodVO;
import org.fiware.tmforum.mapping.annotations.AttributeGetter;
import org.fiware.tmforum.mapping.annotations.AttributeSetter;
import org.fiware.tmforum.mapping.annotations.AttributeType;

import java.util.List;

@Data
public class ResourceSpecificationCharacteristic {

    private Boolean configurable;
    private Boolean extensible;
    private Boolean isUnique;
    private String name;
    private String regex;
    private String valueType;
    private String description;
    private Integer maxCardinality;
    private Integer minCardinality;
    // TODO: validate refs
    private List<ResourceSpecificationCharacteristicRelationship> resourceSpecCharRelationship;
    private List<CharacteristicValue> resourceSpecCharacteristicValue;
    private TimePeriodVO validFor;

}
