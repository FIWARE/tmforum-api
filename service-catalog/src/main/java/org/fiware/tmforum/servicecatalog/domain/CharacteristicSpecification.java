package org.fiware.tmforum.servicecatalog.domain;

import lombok.Data;
import org.fiware.tmforum.common.domain.TimePeriod;

import java.net.URI;
import java.util.List;

@Data
public class CharacteristicSpecification {

    private String id;
    private Boolean configurable;
    private String description;
    private Boolean extensible;
    private Boolean isUnique;
    private Integer maxCardinality;
    private Integer minCardinality;
    private String name;
    private String regex;
    private String valueType;
    private List<CharacteristicSpecificationRelationship> charSpecRelationship;
    private List<CharacteristicValueSpecification> characteristicValueSpecification;
    private TimePeriod validFor;
    private String atBaseType;
    private URI atSchemaLocation;
    private String atType;
    private String atValueSchemaLocation;


}
