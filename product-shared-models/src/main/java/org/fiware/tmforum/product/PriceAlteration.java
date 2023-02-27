package org.fiware.tmforum.product;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.fiware.tmforum.common.domain.Entity;

@Data
@EqualsAndHashCode(callSuper = true)
public class PriceAlteration extends Entity {

	private Integer applicationDuration;
	private String description;
	private String name;
	private String priceType;
	private Integer priority;
	private String recurringChargePeriod;
	private String unitOfMeasure;
	private Price price;
	private ProductOfferingPriceRef productOfferingPrice;
}
