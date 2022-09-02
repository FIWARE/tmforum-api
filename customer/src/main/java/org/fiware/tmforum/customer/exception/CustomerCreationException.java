package org.fiware.tmforum.customer.exception;

/**
 * Exception to be thrown in case a customer could not have been created.
 */
public class CustomerCreationException extends RuntimeException {

    public CustomerCreationException(String message) {
        super(message);
    }

    public CustomerCreationException(String message, Throwable cause) {
        super(message, cause);
    }
}
