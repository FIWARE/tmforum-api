package org.fiware.tmforum.productordering.domain;

import lombok.Data;
import org.fiware.tmforum.common.domain.BillingAccountRef;
import org.fiware.tmforum.common.domain.Entity;
import org.fiware.tmforum.customer.PaymentRef;
import org.fiware.tmforum.product.ProductOfferingRef;
import org.fiware.tmforum.product.ProductRef;

import java.util.List;

@Data
public class ProductOrderItem extends Entity {

	private String id;
	private Integer quantity;
	private OrderItemAction action;
	private AppointmentRef appointment;
	private BillingAccountRef billingAccount;
	private List<OrderPrice> itemPrice;
	private List<OrderTerm> itemTerm;
	private List<OrderPrice> itemTotalPrice;
	private List<PaymentRef> payment;
	private ProductRef product;
	private ProductOfferingRef productOffering;
	private ProductOfferingQualificationItemRef productOfferingQualificationItem;
	private List<ProductOrderItem> productOrderItem;
	private List<OrderItemRelationship> productOrderItemRelationship;
	private List<ProductOfferingQualificationRef> qualification;
	private QuoteItemRef quoteItem;
	private ProductOrderItemState state;

}
