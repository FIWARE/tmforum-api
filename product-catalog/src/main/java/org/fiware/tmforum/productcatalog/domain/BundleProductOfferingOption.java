package org.fiware.tmforum.productcatalog.domain;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.fiware.tmforum.common.domain.Entity;


@Data
@EqualsAndHashCode(callSuper = true)
public class BundleProductOfferingOption extends Entity {

	private int numberRelOfferDefault;
	private int numberRelOfferLowerLimit;
	private int numberRelOfferUpperLimit;
}
