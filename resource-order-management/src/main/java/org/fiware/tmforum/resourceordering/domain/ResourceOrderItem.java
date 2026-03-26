package org.fiware.tmforum.resourceordering.domain;

import lombok.Data;
import org.fiware.tmforum.common.domain.Entity;
import org.fiware.tmforum.common.domain.ReferenceValue;

import java.util.List;

/**
 * An identified part of the order. A resource order is decomposed into one or more order items.
 */
@Data
public class ResourceOrderItem extends Entity {

	private String tmfId;
	private Integer quantity;
	private OrderItemAction action;
	private ResourceOrderItemState state;
	private ReferenceValue appointment;
	private List<ResourceOrderItemRelationship> orderItemRelationship;
	private ResourceRefOrValueForOrder resource;
	private ReferenceValue resourceSpecification;
}
