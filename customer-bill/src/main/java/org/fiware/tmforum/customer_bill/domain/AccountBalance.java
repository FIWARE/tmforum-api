package org.fiware.tmforum.customer_bill.domain;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.fiware.tmforum.common.domain.Entity;

@Data
@EqualsAndHashCode(callSuper = true)
public class AccountBalance extends Entity {

    private String balanceType;
    private Money amount;
    private TimePeriod validFor;

}
