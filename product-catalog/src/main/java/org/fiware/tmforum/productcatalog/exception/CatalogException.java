package org.fiware.tmforum.productcatalog.exception;

import lombok.Getter;

public class CatalogException extends RuntimeException {

    @Getter
    private final CatalogExceptionReason catalogExceptionReason;

    public CatalogException(String message, CatalogExceptionReason catalogExceptionReason) {
        super(message);
        this.catalogExceptionReason = catalogExceptionReason;
    }

    public CatalogException(String message, Throwable cause, CatalogExceptionReason catalogExceptionReason) {
        super(message, cause);
        this.catalogExceptionReason = catalogExceptionReason;
    }
}
