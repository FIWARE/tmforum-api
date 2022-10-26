package org.fiware.tmforum.resourceinventory.exception;

import lombok.Getter;

public class ResourceInventoryException extends RuntimeException {

    @Getter
    private final ResourceInventoryExceptionReason inventoryExceptionReason;

    public ResourceInventoryException(String message, ResourceInventoryExceptionReason catalogExceptionReason) {
        super(message);
        this.inventoryExceptionReason = catalogExceptionReason;
    }

    public ResourceInventoryException(String message, Throwable cause, ResourceInventoryExceptionReason catalogExceptionReason) {
        super(message, cause);
        this.inventoryExceptionReason = catalogExceptionReason;
    }
}
