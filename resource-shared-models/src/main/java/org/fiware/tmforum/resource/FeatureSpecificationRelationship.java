package org.fiware.tmforum.resource;

import lombok.Data;
import org.fiware.tmforum.common.domain.TimePeriod;

import java.net.URI;

@Data
public class FeatureSpecificationRelationship {

    private String id;
    private URI href;
    private String featureId;
    private String name;
    private String relationshipType;
    private URI parentSpecificationHref;
    private ResourceSpecificationRef parentSpecificationId;
    private TimePeriod validFor;
    private String atBaseType;
    private URI atSchemaLocation;
    private String atType;
}
