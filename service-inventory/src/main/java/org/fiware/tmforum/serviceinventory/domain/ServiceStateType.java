package org.fiware.tmforum.serviceinventory.domain;

import com.fasterxml.jackson.annotation.JsonValue;

public enum ServiceStateType {

    FEASIBILITYCHECKED("feasibilityChecked"),
    DESIGNED("designed"),
    RESERVED("reserved"),
    INACTIVE("inactive"),
    ACTIVE("active"),
    TERMINATED("terminated");

    private final String value;

    ServiceStateType(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }
}
