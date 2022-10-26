package org.fiware.tmforum.resourcefunction.exception;

import lombok.Getter;

public class ResourceFunctionException extends RuntimeException {

    @Getter
    private final ResourceFunctionExceptionReason catalogExceptionReason;

    public ResourceFunctionException(String message, ResourceFunctionExceptionReason catalogExceptionReason) {
        super(message);
        this.catalogExceptionReason = catalogExceptionReason;
    }

    public ResourceFunctionException(String message, Throwable cause, ResourceFunctionExceptionReason catalogExceptionReason) {
        super(message, cause);
        this.catalogExceptionReason = catalogExceptionReason;
    }
}
