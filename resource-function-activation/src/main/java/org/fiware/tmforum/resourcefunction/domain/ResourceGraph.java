package org.fiware.tmforum.resourcefunction.domain;

import lombok.Data;

import java.net.URI;
import java.util.List;

@Data
public class ResourceGraph {

    private String tmfId;
    private URI href;
    private String description;
    private String name;
    private List<Connection> connection;
    private List<ResourceGraphRelationship> graphRelationship;
    private String atBaseType;
    private String atSchemaLocation;
    private String atType;
}
