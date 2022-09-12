package org.fiware.tmforum.customer_bill.domain;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.fiware.customer_bill.model.MoneyVO;
import org.fiware.customer_bill.model.PaymentRefVO;
import org.fiware.tmforum.common.domain.Entity;

@Data
@EqualsAndHashCode(callSuper = true)
public class AppliedPayment extends Entity {

    private Money appliedAmount;
    private PaymentRef payment;

}
