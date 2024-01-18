package org.fiware.tmforum.resourcefunction.domain;

import lombok.Data;

import java.net.URI;

@Data
public class Connection {

    private String id;
    private URI href;
    private String associationType;
    private String name;
    private EndpointRef endpoint;
    private String atBaseType;
    private URI atSchemaLocation;
    private String atType;
}
