package org.fiware.tmforum.customerbillmanagement.domain;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.fiware.tmforum.common.domain.Entity;
import org.fiware.tmforum.common.domain.Money;

@Data
@EqualsAndHashCode(callSuper = true)
public class AppliedBillingTaxRate extends Entity {

	private String taxCategory;
	private Float taxRate;
	private Money taxAmount;

}
