package org.fiware.tmforum.customer.exception;

public class CustomerListException extends RuntimeException {

    public CustomerListException(String message) {
        super(message);
    }

    public CustomerListException(String message, Throwable cause) {
        super(message, cause);
    }
}
