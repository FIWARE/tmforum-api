package org.fiware.tmforum.resource;

public enum ResourceUsageType {

    IDLE("idle"),
    ACTIVE("active"),
    BUSY("busy");

    private final String value;

    ResourceUsageType(String value) {
        this.value = value;
    }
}
