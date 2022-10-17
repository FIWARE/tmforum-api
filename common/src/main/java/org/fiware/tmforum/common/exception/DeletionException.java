package org.fiware.tmforum.common.exception;

import lombok.Getter;

public class DeletionException extends RuntimeException{

    @Getter
    private final DeletionExceptionReason reason;

    public DeletionException(String message, DeletionExceptionReason reason) {
        super(message);
        this.reason = reason;
    }

    public DeletionException(String message, Throwable cause, DeletionExceptionReason reason) {
        super(message, cause);
        this.reason = reason;
    }
}
