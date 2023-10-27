package org.fiware.tmforum.product;

import java.util.List;

import org.fiware.tmforum.common.domain.BillingAccountRef;
import org.fiware.tmforum.common.domain.Entity;
import org.fiware.tmforum.common.domain.ReferenceValue;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class ProductPriceValue extends Entity {
    private String description;
	private String name;
	private String priceType;
	private String recurringChargePeriod;
	private String unitOfMeasure;
	private BillingAccountRef billingAccount;
	private Price price;
	private ReferenceValue productOfferingPrice;
	private List<PriceAlteration> productPriceAlteration;
}
