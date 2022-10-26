package org.fiware.tmforum.resource;

import lombok.Data;
import org.fiware.tmforum.common.domain.TimePeriod;

import java.net.URI;

@Data
public class ResourceSpecificationCharacteristicRelationship {

    private String characteristicSpecificationId;
    private String name;
    private String relationshipType;
    private String resourceSpecificationHref;
    private ResourceSpecificationRef resourceSpecificationId;
    private TimePeriod validFor;
    private String atBaseType;
    private URI atSchemaLocation;
    private String atType;
}
