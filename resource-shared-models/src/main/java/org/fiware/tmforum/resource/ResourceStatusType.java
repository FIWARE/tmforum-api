package org.fiware.tmforum.resource;

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
}

