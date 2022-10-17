package org.fiware.tmforum.productcatalog.exception;

import lombok.Getter;

public class ProductCatalogException extends RuntimeException {

    @Getter
    private final ProductCatalogExceptionReason catalogExceptionReason;

    public ProductCatalogException(String message, ProductCatalogExceptionReason catalogExceptionReason) {
        super(message);
        this.catalogExceptionReason = catalogExceptionReason;
    }

    public ProductCatalogException(String message, Throwable cause, ProductCatalogExceptionReason catalogExceptionReason) {
        super(message, cause);
        this.catalogExceptionReason = catalogExceptionReason;
    }
}
