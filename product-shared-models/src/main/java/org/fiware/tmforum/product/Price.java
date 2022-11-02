package org.fiware.tmforum.product;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.fiware.tmforum.common.domain.Entity;
import org.fiware.tmforum.common.domain.Money;

@Data
@EqualsAndHashCode(callSuper = true)
public class Price extends Entity {

	private Float percentage;
	private Float taxRate;
	private Money dutyFreeAmount;
	private Money taxIncludedAmount;
}
