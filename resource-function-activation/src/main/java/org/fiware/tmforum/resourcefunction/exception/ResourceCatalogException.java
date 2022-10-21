package org.fiware.tmforum.resourcefunction.exception;

import lombok.Getter;

public class ResourceCatalogException extends RuntimeException {

    @Getter
    private final ResourceCatalogExceptionReason catalogExceptionReason;

    public ResourceCatalogException(String message, ResourceCatalogExceptionReason catalogExceptionReason) {
        super(message);
        this.catalogExceptionReason = catalogExceptionReason;
    }

    public ResourceCatalogException(String message, Throwable cause, ResourceCatalogExceptionReason catalogExceptionReason) {
        super(message, cause);
        this.catalogExceptionReason = catalogExceptionReason;
    }
}
