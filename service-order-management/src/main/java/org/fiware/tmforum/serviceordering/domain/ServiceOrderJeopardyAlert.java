package org.fiware.tmforum.serviceordering.domain;

import lombok.Data;

import java.time.Instant;

/**
 * A jeopardy alert associated with a service order, indicating a risk of not meeting a commitment.
 */
@Data
public class ServiceOrderJeopardyAlert {

	private Instant alertDate;
	private String exception;
	private String id;
	private String jeopardyType;
	private String message;
	private String name;
}
