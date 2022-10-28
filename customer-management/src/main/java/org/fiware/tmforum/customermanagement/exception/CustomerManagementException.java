package org.fiware.tmforum.customermanagement.exception;

import lombok.Getter;

public class CustomerManagementException extends RuntimeException {

    @Getter
    private final CustomerManagementExceptionReason inventoryExceptionReason;

    public CustomerManagementException(String message, CustomerManagementExceptionReason catalogExceptionReason) {
        super(message);
        this.inventoryExceptionReason = catalogExceptionReason;
    }

    public CustomerManagementException(String message, Throwable cause, CustomerManagementExceptionReason catalogExceptionReason) {
        super(message, cause);
        this.inventoryExceptionReason = catalogExceptionReason;
    }
}
