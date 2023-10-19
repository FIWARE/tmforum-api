package org.fiware.tmforum.account.domain;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.fiware.tmforum.common.domain.Entity;
import org.fiware.tmforum.common.domain.Money;
import org.fiware.tmforum.common.domain.TimePeriod;

import java.net.URI;

@Data
@EqualsAndHashCode(callSuper = true)
public class PaymentPlan extends Entity {

    private Integer numberOfPayments;
    private String paymentFrequency;
    private String planType;
    private Integer priority;
    private String status;
    private PaymentMethodRef paymentMethod;
    private Money totalAmount;
    private TimePeriod validFor;
    private String atBaseType;
    private URI atSchemaLocation;
    private String atType;
}