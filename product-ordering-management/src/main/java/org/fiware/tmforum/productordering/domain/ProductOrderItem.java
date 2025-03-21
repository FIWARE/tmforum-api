package org.fiware.tmforum.productordering.domain;

import lombok.Data;
import org.fiware.tmforum.common.domain.Entity;
import org.fiware.tmforum.common.domain.ReferenceValue;
import org.fiware.tmforum.product.ProductOfferingRefValue;
import org.fiware.tmforum.product.ProductRefOrValue;

import java.util.List;

@Data
public class ProductOrderItem extends Entity {

	private String itemId;
	private Integer quantity;
	private OrderItemAction action;
	private ReferenceValue appointment;
	private ReferenceValue billingAccount;
	private List<OrderPrice> itemPrice;
	private List<OrderTerm> itemTerm;
	private List<OrderPrice> itemTotalPrice;
	private List<ReferenceValue> payment;
	private ProductRefOrValue product;
	private ProductOfferingRefValue productOffering;
	private ProductOfferingQualificationItemRef productOfferingQualificationItem;
	private List<ProductOrderItem> productOrderItem;
	private List<OrderItemRelationship> productOrderItemRelationship;
	private List<ReferenceValue> qualification;
	private QuoteItemRef quoteItem;
	private ProductOrderItemState state;
}
