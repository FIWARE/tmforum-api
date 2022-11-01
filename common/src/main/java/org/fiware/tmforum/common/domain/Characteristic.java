package org.fiware.tmforum.common.domain;

import lombok.Data;

import java.net.URI;

@Data
public class Characteristic {

    private String name;
    private String valueType;
    private Object value;
    private String atBaseType;
    private URI atSchemaLocation;
    private String atType;
}
