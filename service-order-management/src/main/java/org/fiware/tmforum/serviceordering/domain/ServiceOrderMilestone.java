package org.fiware.tmforum.serviceordering.domain;

import lombok.Data;

import java.time.Instant;

/**
 * A milestone associated with a service order, tracking progress through the order lifecycle.
 */
@Data
public class ServiceOrderMilestone {

	private String description;
	private String id;
	private String message;
	private Instant milestoneDate;
	private String name;
	private String status;
}
