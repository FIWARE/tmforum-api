package org.fiware.tmforum.resourcefunction.domain;

import com.fasterxml.jackson.annotation.JsonValue;

public enum TaskState {

    ACCEPTED("accepted"),
    TERMINATED_WITH_ERROR("terminatedWithError"),
    IN_PROGRESS("inProgress"),
    DONE("done");

    private final String value;

    TaskState(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }
}
