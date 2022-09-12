package org.fiware.tmforum.customer_bill.domain;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.fiware.tmforum.common.domain.Entity;

@Data
@EqualsAndHashCode(callSuper = true)
public class AppliedBillingTaxRate extends Entity {

    private String taxCategory;
    private Float taxRate;
    private Money taxAmount;

}
