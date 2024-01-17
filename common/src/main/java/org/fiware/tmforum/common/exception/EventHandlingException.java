package org.fiware.tmforum.common.exception;

public class EventHandlingException extends RuntimeException {
    public EventHandlingException() {}

    public EventHandlingException(String message) {
        super(message);
    }
}
