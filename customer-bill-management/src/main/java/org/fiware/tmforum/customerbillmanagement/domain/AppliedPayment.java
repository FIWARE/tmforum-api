package org.fiware.tmforum.customerbillmanagement.domain;

import lombok.Data;
import org.fiware.tmforum.common.domain.Money;
import org.fiware.tmforum.customer.PaymentRef;

import java.net.URI;

@Data
public class AppliedPayment {

	private Money appliedAmount;
	private PaymentRef payment;
	private String atBaseType;
	private URI atSchemaLocation;
	private String atType;
}
