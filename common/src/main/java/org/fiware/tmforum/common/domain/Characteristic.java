package org.fiware.tmforum.common.domain;

import lombok.Data;

import java.net.URI;

@Data
public class Characteristic {

    private String name;
    private String valueType;
    // if its value, it clashes with the ngsi-ld property "value"
    private Object tmfValue;
    private String atBaseType;
    private URI atSchemaLocation;
    private String atType;
}
