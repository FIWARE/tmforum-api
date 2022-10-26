package org.fiware.tmforum.resourcecatalog.domain;

import lombok.Data;
import org.fiware.tmforum.common.domain.TimePeriod;
import org.fiware.tmforum.resource.ResourceSpecificationCharacteristic;

import java.net.URI;
import java.util.List;

@Data
public class ResourceSpecificationRelationship {

    private ResourceSpecificationRef id;
    private String href;
    private Integer defaultQuantity;
    private Integer maximumQuantity;
    private Integer minimumQuantity;
    private String name;
    private String relationshipType;
    private String role;
    private List<ResourceSpecificationCharacteristic> characteristic;
    private TimePeriod validFor;
    private String atBaseType;
    private URI atSchemaLocation;
    private String atType;
}
