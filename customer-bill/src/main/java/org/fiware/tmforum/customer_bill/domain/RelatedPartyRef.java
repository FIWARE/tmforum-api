package org.fiware.tmforum.customer_bill.domain;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.fiware.tmforum.customer_bill.domain.customer_bill.AppliedCustomerBillingRate;
import org.fiware.tmforum.customer_bill.domain.customer_bill.CustomerBill;
import org.fiware.tmforum.customer_bill.domain.customer_bill.CustomerBillOnDemand;
import org.fiware.tmforum.mapping.annotations.*;

import java.util.List;

@MappingEnabled(entityType = {CustomerBill.TYPE_CUSTOMER_BILL})
@EqualsAndHashCode(callSuper = true)
public class RelatedPartyRef extends RefEntity {

    @Getter(onMethod = @__({@AttributeGetter(value = AttributeType.PROPERTY, targetName = "name", embedProperty = true)}))
    @Setter(onMethod = @__({@AttributeSetter(value = AttributeType.PROPERTY, targetName = "name", targetClass = String.class)}))
    private String name;

    @Getter(onMethod = @__({@AttributeGetter(value = AttributeType.PROPERTY, targetName = "role", embedProperty = true)}))
    @Setter(onMethod = @__({@AttributeSetter(value = AttributeType.PROPERTY, targetName = "role", targetClass = String.class)}))
    private String role;

    public RelatedPartyRef(String id) {
        super(id);
    }

    @Override
    @Ignore
    public List<String> getReferencedTypes() {
        /**
         * TODO: Check if list is correct
         */
        return List.of(
                CustomerBill.TYPE_CUSTOMER_BILL,
                CustomerBillOnDemand.TYPE_CUSTOMER_BILL_ON_DEMAND,
                AppliedCustomerBillingRate.TYPE_APPLIED_CUSTOMER_BILLING_RATE);
    }

}
