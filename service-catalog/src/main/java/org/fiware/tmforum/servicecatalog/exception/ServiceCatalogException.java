package org.fiware.tmforum.servicecatalog.exception;

import lombok.Getter;

public class ServiceCatalogException extends RuntimeException {

    @Getter
    private final ServiceCatalogExceptionReason inventoryExceptionReason;

    public ServiceCatalogException(String message, ServiceCatalogExceptionReason catalogExceptionReason) {
        super(message);
        this.inventoryExceptionReason = catalogExceptionReason;
    }

    public ServiceCatalogException(String message, Throwable cause, ServiceCatalogExceptionReason catalogExceptionReason) {
        super(message, cause);
        this.inventoryExceptionReason = catalogExceptionReason;
    }
}
