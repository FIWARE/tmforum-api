package org.fiware.tmforum.customer.exception;

import lombok.Getter;

/**
 * Exception to be thrown in case a customer could not have been created.
 */
public class CustomerCreationException extends RuntimeException {

    @Getter
    private final CustomerExceptionReason customerExceptionReason;

    public CustomerCreationException(String message, CustomerExceptionReason customerExceptionReason) {
        super(message);
        this.customerExceptionReason = customerExceptionReason;
    }

    public CustomerCreationException(String message, Throwable cause, CustomerExceptionReason customerExceptionReason) {
        super(message, cause);
        this.customerExceptionReason = customerExceptionReason;
    }
}
