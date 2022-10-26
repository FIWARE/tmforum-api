package org.fiware.tmforum.servicecatalog.domain;

import lombok.Data;
import org.fiware.tmforum.common.domain.TimePeriod;

import java.net.URI;

@Data
public class ServiceSpecificationRelationship {

    private ServiceSpecificationRef id;
    private String href;
    private String name;
    private String relationshipType;
    private String role;
    private TimePeriod validFor;
    private String atBaseType;
    private URI atSchemaLocation;
    private String atType;
    private String atReferredType;
}
