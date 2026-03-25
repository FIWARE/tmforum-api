package org.fiware.tmforum.serviceordering.domain;

import lombok.Data;
import org.fiware.tmforum.common.domain.Entity;

import java.util.List;

/**
 * An identified part of the order. A service order is decomposed into one or more order items.
 */
@Data
public class ServiceOrderItem extends Entity {

	private String tmfId;
	private Integer quantity;
	private OrderItemAction action;
	private ServiceOrderItemState state;
	private ReferenceValue appointment;
	private ReferenceValue service;
	private List<ServiceOrderItem> serviceOrderItem;
	private List<ServiceOrderItemRelationship> serviceOrderItemRelationship;
	private List<ServiceOrderItemErrorMessage> errorMessage;
}
