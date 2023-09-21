package org.fiware.tmforum.resource;

import com.fasterxml.jackson.annotation.JsonValue;

public enum ResourceStatusType {

    STANDBY("standby"),
    ALARM("alarm"),
    AVAILABLE("available"),
    RESERVED("reserved"),
    UNKNOWN("unknown"),
    SUSPENDED("suspended");

    private final String value;

    ResourceStatusType(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }
}

