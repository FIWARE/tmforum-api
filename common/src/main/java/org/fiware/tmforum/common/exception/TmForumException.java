package org.fiware.tmforum.common.exception;

import lombok.Getter;

public class TmForumException extends RuntimeException {

    @Getter
    private final TmForumExceptionReason exceptionReason;

    public TmForumException(String message, TmForumExceptionReason catalogExceptionReason) {
        super(message);
        this.exceptionReason = catalogExceptionReason;
    }

    public TmForumException(String message, Throwable cause, TmForumExceptionReason catalogExceptionReason) {
        super(message, cause);
        this.exceptionReason = catalogExceptionReason;
    }
}
