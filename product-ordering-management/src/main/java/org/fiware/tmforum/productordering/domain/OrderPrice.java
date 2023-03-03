package org.fiware.tmforum.productordering.domain;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.fiware.tmforum.common.domain.BillingAccountRef;
import org.fiware.tmforum.common.domain.Entity;
import org.fiware.tmforum.product.Price;
import org.fiware.tmforum.product.PriceAlteration;

import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
public class OrderPrice extends Entity {

	private String description;
	private String name;
	private String priceType;
	private String recurringChargePeriod;
	private String unitOfMeasure;
	private Price price;
	private List<PriceAlteration> priceAlteration;
	private ProductOfferingPriceRef productOfferingPrice;
	private BillingAccountRef billingAccount;
}
