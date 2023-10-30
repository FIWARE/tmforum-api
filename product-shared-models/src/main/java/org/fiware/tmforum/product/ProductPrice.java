package org.fiware.tmforum.product;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.fiware.tmforum.common.domain.BillingAccountRef;
import org.fiware.tmforum.common.domain.Entity;

import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
public class ProductPrice extends Entity {

	private String description;
	private String name;
	private String priceType;
	private String recurringChargePeriod;
	private String unitOfMeasure;
	private BillingAccountRef billingAccount;
	private Price price;
	private ProductOfferingPriceRefValue productOfferingPrice;
	private List<PriceAlteration> productPriceAlteration;
}
