package org.fiware.tmforum.resource;

public enum ResourceAdministrativeState {

    LOCKED("locked"),
    UNLOCKED("unlocked"),
    SHUTDOWN("shutdown");

    private final String value;

    ResourceAdministrativeState(String value) {
        this.value = value;
    }
}
