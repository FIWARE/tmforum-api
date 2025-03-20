package org.fiware.tmforum.resourcefunction.domain;

import lombok.Data;

import java.net.URI;

@Data
public class ResourceGraphRelationship {
    private String graphRelationId;
    private URI href;
    private String relationshipType;
    private ResourceGraphRef resourceGraph;
    private String atBaseType;
    private String atSchemaLocation;
    private String atType;
}
