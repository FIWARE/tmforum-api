package org.fiware.tmforum.service;

import lombok.Data;
import org.fiware.tmforum.common.domain.TimePeriod;

import java.net.URI;

@Data
public class CharacteristicSpecificationRelationship {

    private String characteristicSpecificationId;
    private String name;
    private URI parentSpecificationHref;
    private String parentSpecificationId;
    private String relationshipType;
    private TimePeriod validFor;
}
