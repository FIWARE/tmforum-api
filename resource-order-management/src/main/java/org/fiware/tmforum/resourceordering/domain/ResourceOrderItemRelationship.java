package org.fiware.tmforum.resourceordering.domain;

import lombok.Data;

/**
 * Describes a relationship between resource order items.
 */
@Data
public class ResourceOrderItemRelationship {

	private String relationshipType;
	private ReferenceValue orderItem;
}
