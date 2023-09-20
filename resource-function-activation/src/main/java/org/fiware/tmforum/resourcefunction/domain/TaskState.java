package org.fiware.tmforum.resourcefunction.domain;

import lombok.Getter;

public enum TaskState {

    ACKNOWLEDGED("acknowledged"),
    TERMINATEDWITHERROR("terminatedWithError"),
    INPROGRESS("inProgress"),
    DONE("done");

    @Getter
    private final String value;

    TaskState(String value) {
        this.value = value;
    }
}
