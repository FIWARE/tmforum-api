package org.fiware.tmforum.customer.exception;

import lombok.Getter;

public class CustomerDeletionException extends RuntimeException {

    @Getter
    private final CustomerExceptionReason customerExceptionReason;

    public CustomerDeletionException(String message, CustomerExceptionReason customerExceptionReason) {
        super(message);
        this.customerExceptionReason = customerExceptionReason;
    }

    public CustomerDeletionException(String message, Throwable cause, CustomerExceptionReason customerExceptionReason) {
        super(message, cause);
        this.customerExceptionReason = customerExceptionReason;
    }
}
