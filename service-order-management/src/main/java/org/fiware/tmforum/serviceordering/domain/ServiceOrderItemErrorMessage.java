package org.fiware.tmforum.serviceordering.domain;

import lombok.Data;

import java.net.URI;

/**
 * An error message associated with a service order item.
 */
@Data
public class ServiceOrderItemErrorMessage {

	private String code;
	private String message;
	private String reason;
	private URI referenceError;
	private String status;
}
