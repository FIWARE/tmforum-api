package org.fiware.tmforum.servicecatalog.domain;

import lombok.Data;
import org.fiware.tmforum.common.domain.TimePeriod;

import java.net.URI;

@Data
public class FeatureSpecificationRelationship {

    private String featureId;
    private String name;
    private String relationshipType;
    private URI parentSpecificationHref;
    private String parentSpecificationId;
    private TimePeriod validFor;
    private String atBaseType;
    private URI atSchemaLocation;
    private String atType;
}
