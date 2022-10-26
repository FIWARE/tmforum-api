package org.fiware.tmforum.resourcefunction.domain;

import lombok.Data;

import java.net.URI;
import java.util.List;

@Data
public class Connection {

    private String id;
    private String associationType;
    private String name;
    private List<EndpointRef> endpoint;
    private String atBaseType;
    private URI atSchemaLocation;
    private String atType;
}
