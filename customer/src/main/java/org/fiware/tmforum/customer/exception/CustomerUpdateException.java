package org.fiware.tmforum.customer.exception;

import lombok.Getter;

public class CustomerUpdateException extends RuntimeException {

    @Getter
    private final CustomerExceptionReason customerExceptionReason;

    public CustomerUpdateException(String message, CustomerExceptionReason customerExceptionReason) {
        super(message);
        this.customerExceptionReason = customerExceptionReason;
    }

    public CustomerUpdateException(String message, Throwable cause, CustomerExceptionReason customerExceptionReason) {
        super(message, cause);
        this.customerExceptionReason = customerExceptionReason;
    }

}
