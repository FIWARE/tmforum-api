package org.fiware.tmforum.resource;

import com.fasterxml.jackson.annotation.JsonValue;

public enum ResourceUsageType {

    IDLE("idle"),
    ACTIVE("active"),
    BUSY("busy");

    private final String value;

    ResourceUsageType(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }
}
