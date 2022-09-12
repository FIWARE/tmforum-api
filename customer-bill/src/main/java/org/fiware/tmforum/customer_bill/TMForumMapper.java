package org.fiware.tmforum.customer_bill;

import org.fiware.customer_bill.model.*;
import org.fiware.tmforum.customer_bill.domain.*;
import org.fiware.tmforum.customer_bill.domain.customer_bill.AppliedCustomerBillingRate;
import org.fiware.tmforum.customer_bill.domain.customer_bill.CustomerBill;
import org.fiware.tmforum.customer_bill.domain.customer_bill.CustomerBillOnDemand;
import org.fiware.tmforum.mapping.MappingException;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;

/**
 * Mapper between the internal model and api-domain objects
 */
@Mapper(componentModel = "jsr330")
public interface TMForumMapper {

    String ID_TEMPLATE = "urn:ngsi-ld:%s:%s";

    CustomerBillVO map(CustomerBill customerBill);
    CustomerBill map(CustomerBillVO customerBillVO);

    @Mapping(target = "id", expression = "java(java.lang.String.format(ID_TEMPLATE, \"customer_bill_on_demand\", java.util.UUID.randomUUID()))")
    @Mapping(target = "href", ignore = true)
    CustomerBillOnDemandVO map(CustomerBillOnDemandCreateVO customerBillOnDemandCreateVO);
    CustomerBillOnDemandVO map(CustomerBillOnDemand customerBillOnDemand);
    CustomerBillOnDemand map(CustomerBillOnDemandVO customerBillOnDemandVO);

    AppliedCustomerBillingRate map(AppliedCustomerBillingRateVO appliedCustomerBillingRateVO);

    @Mapping(target = "isBilled", source = "billed")
    AppliedCustomerBillingRateVO map(AppliedCustomerBillingRate appliedCustomerBillingRate);

    TimePeriodVO map(TimePeriod timePeriod);
    TimePeriod map(TimePeriodVO value);

    Money map(MoneyVO moneyVO);
    MoneyVO map(Money money);

    AppliedPayment map(AppliedPaymentVO appliedPaymentVO);
    AppliedPaymentVO map(AppliedPayment appliedPayment);

    PaymentRef map(PaymentRefVO paymentRefVO);
    PaymentRefVO map(PaymentRef paymentRef);

    AttachmentRefOrValue map(AttachmentRefOrValueVO attachmentRefOrValueVO);
    AttachmentRefOrValueVO map(AttachmentRefOrValue attachmentRefOrValue);

    Quantity map(QuantityVO quantityVO);
    QuantityVO map(Quantity quantity);

    BillingAccountRef map(BillingAccountRefVO billingAccountRefVO);
    BillingAccountRefVO map(BillingAccountRef billingAccountRef);

    FinancialAccountRef map(FinancialAccountRefVO financialAccountRefVO);
    FinancialAccountRefVO map(FinancialAccountRef financialAccountRef);

    AccountBalance map(AccountBalanceVO accountBalanceVO);
    AccountBalanceVO map(AccountBalance accountBalance);

    PaymentMethodRef map(PaymentMethodRefVO paymentMethodRefVO);
    PaymentMethodRefVO map(PaymentMethodRef paymentMethodRef);

    RelatedPartyRef map(RelatedPartyRefVO relatedPartyRefVO);
    RelatedPartyRefVO map(RelatedPartyRef relatedPartyRef);

    StateValue map(StateValueVO stateValueVO);
    StateValueVO map(StateValue stateValue);

    StateValues map(StateValuesVO stateValuesVO);
    StateValuesVO map(StateValues stateValues);

    TaxItem map(TaxItemVO taxItemVO);
    TaxItemVO map(TaxItem taxItem);

    BillRef map(BillRefVO billRefVO);
    BillRefVO map(BillRef billRef);

    AppliedBillingTaxRate map(AppliedBillingTaxRateVO appliedBillingTaxRateVO);
    AppliedBillingTaxRateVO map(AppliedBillingTaxRate appliedBillingTaxRate);

    AppliedBillingRateCharacteristic map(AppliedBillingRateCharacteristicVO appliedBillingRateCharacteristicVO);
    AppliedBillingRateCharacteristicVO map(AppliedBillingRateCharacteristic appliedBillingRateCharacteristic);

    ProductRef map(ProductRefVO productRefVO);
    ProductRefVO map(ProductRef productRef);

    default URL map(String value) {
        if (value == null) {
            return null;
        }
        try {
            return new URL(value);
        } catch (MalformedURLException e) {
            throw new MappingException(String.format("%s is not a URL.", value), e);
        }
    }

    default String map(URL value) {
        if (value == null) {
            return null;
        }
        return value.toString();
    }

    default URI mapToURI(String value) {
        if (value == null) {
            return null;
        }
        return URI.create(value);
    }

    default String mapFromURI(URI value) {
        if (value == null) {
            return null;
        }
        return value.toString();
    }

}
