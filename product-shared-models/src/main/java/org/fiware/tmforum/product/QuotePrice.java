package org.fiware.tmforum.product;

import lombok.Data;
import org.fiware.tmforum.common.domain.Entity;

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
