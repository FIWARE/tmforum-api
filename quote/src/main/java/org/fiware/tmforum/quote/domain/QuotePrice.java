package org.fiware.tmforum.quote.domain;

import lombok.Data;
import org.fiware.tmforum.common.domain.Entity;
import org.fiware.tmforum.product.Price;
import org.fiware.tmforum.product.PriceAlteration;
import org.fiware.tmforum.product.ProductOfferingPriceRef;

import java.util.List;

@Data
public class QuotePrice extends Entity {

	private String description;
	private String name;
	private String priceType;
	private String recurringChargePeriod;
	private String unitOfMeasure;
	private Price price;
	private List<PriceAlteration> priceAlteration;
	private ProductOfferingPriceRef productOfferingPrice;
}
