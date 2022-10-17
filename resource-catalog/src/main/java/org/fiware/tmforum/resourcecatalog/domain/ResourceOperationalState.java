package org.fiware.tmforum.resourcecatalog.domain;

public enum ResourceOperationalState {

    ENABLE("enable"),
    DISABLE("disable");

    private final String value;

    ResourceOperationalState(String value) {
        this.value = value;
    }
}
