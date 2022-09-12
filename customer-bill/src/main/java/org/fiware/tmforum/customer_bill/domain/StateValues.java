package org.fiware.tmforum.customer_bill.domain;

public enum StateValues {

    INPROGRESS("inProgress"),
    REJECTED("rejected"),
    DONE("done"),
    TERMINATEDWITHERROR("terminatedWithError");

    private final String value;

    StateValues(String value) {
        this.value = value;
    }
}
