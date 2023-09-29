package org.fiware.tmforum.resource;

import com.fasterxml.jackson.annotation.JsonValue;

public enum ResourceAdministrativeState {

    LOCKED("locked"),
    UNLOCKED("unlocked"),
    SHUTDOWN("shutdown");

    private final String value;

    ResourceAdministrativeState(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }
}
