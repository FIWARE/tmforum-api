package org.fiware.tmforum.resource;

import lombok.Getter;

public enum ResourceStatusType {

    STANDBY("standby"),
    ALARM("alarm"),
    AVAILABLE("available"),
    RESERVED("reserved"),
    UNKNOWN("unknown"),
    SUSPENDED("suspended");

    @Getter
    private final String value;

    ResourceStatusType(String value) {
        this.value = value;
    }
}

