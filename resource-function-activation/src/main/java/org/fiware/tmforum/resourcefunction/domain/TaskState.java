package org.fiware.tmforum.resourcefunction.domain;

public enum TaskState {

    ACKNOWLEDGED("acknowledged"),
    TERMINATEDWITHERROR("terminatedWithError"),
    INPROGRESS("inProgress"),
    DONE("done");

    private final String value;

    TaskState(String value) {
        this.value = value;
    }
}
