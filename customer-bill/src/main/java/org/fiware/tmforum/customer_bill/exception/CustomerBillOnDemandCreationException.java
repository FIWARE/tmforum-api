package org.fiware.tmforum.customer_bill.exception;

/**
 * Exception to be thrown in case a customer bill on demand could not have been created.
 */
public class CustomerBillOnDemandCreationException extends RuntimeException {

    public CustomerBillOnDemandCreationException(String message) {
        super(message);
    }

    public CustomerBillOnDemandCreationException(String message, Throwable cause) {
        super(message, cause);
    }
}
