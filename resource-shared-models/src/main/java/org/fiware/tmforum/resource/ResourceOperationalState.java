package org.fiware.tmforum.resource;

import com.fasterxml.jackson.annotation.JsonValue;

public enum ResourceOperationalState {

    ENABLE("enable"),
    DISABLE("disable");

    private final String value;

    ResourceOperationalState(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }
}
