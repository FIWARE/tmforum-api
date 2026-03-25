package org.fiware.tmforum.serviceordering.domain;

import lombok.Data;

import java.net.URI;
import java.time.Instant;

/**
 * An error message associated with a service order.
 */
@Data
public class ServiceOrderErrorMessage {

	private String code;
	private String message;
	private String reason;
	private URI referenceError;
	private String status;
	private Instant timestamp;
}
