package org.fiware.tmforum.resource;

public enum ResourceOperationalState {

    ENABLE("enable"),
    DISABLE("disable");

    private final String value;

    ResourceOperationalState(String value) {
        this.value = value;
    }
}
