package org.fiware.tmforum.serviceinventory.domain;

import com.fasterxml.jackson.annotation.JsonValue;

public enum OrderItemActionType {

    ADD("add"),
    MODIFY("modify"),
    DELETE("delete"),
    NOCHANGE("noChange");

    private final String value;

    OrderItemActionType(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }
}
