package org.fiware.tmforum.serviceordering.domain;

import lombok.Data;

/**
 * Describes a relationship between service order items.
 */
@Data
public class ServiceOrderItemRelationship {

	private String relationshipType;
	private ReferenceValue orderItem;
}
