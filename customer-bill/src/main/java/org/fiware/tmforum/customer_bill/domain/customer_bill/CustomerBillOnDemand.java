package org.fiware.tmforum.customer_bill.domain.customer_bill;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.fiware.tmforum.common.domain.EntityWithId;
import org.fiware.tmforum.customer_bill.domain.BillRef;
import org.fiware.tmforum.customer_bill.domain.BillingAccountRef;
import org.fiware.tmforum.customer_bill.domain.RelatedPartyRef;
import org.fiware.tmforum.customer_bill.domain.StateValues;
import org.fiware.tmforum.mapping.annotations.AttributeGetter;
import org.fiware.tmforum.mapping.annotations.AttributeSetter;
import org.fiware.tmforum.mapping.annotations.AttributeType;
import org.fiware.tmforum.mapping.annotations.MappingEnabled;

import java.net.URL;
import java.util.List;

@EqualsAndHashCode(callSuper = true)
@MappingEnabled(entityType = CustomerBillOnDemand.TYPE_CUSTOMER_BILL_ON_DEMAND)
public class CustomerBillOnDemand extends EntityWithId {

    public static final String TYPE_CUSTOMER_BILL_ON_DEMAND = "customer_bill_on_demand";

    @Getter(onMethod = @__({@AttributeGetter(value = AttributeType.PROPERTY, targetName = "href")}))
    @Setter(onMethod = @__({@AttributeSetter(value = AttributeType.PROPERTY, targetName = "href")}))
    private URL href;

    @Getter(onMethod = @__({@AttributeGetter(value = AttributeType.PROPERTY, targetName = "description")}))
    @Setter(onMethod = @__({@AttributeSetter(value = AttributeType.PROPERTY, targetName = "description")}))
    private String description;

    @Getter(onMethod = @__({@AttributeGetter(value = AttributeType.PROPERTY, targetName = "lastUpdate")}))
    @Setter(onMethod = @__({@AttributeSetter(value = AttributeType.PROPERTY, targetName = "lastUpdate")}))
    private String lastUpdate;

    @Getter(onMethod = @__({@AttributeGetter(value = AttributeType.PROPERTY, targetName = "name")}))
    @Setter(onMethod = @__({@AttributeSetter(value = AttributeType.PROPERTY, targetName = "name")}))
    private String name;

    @Getter(onMethod = @__({@AttributeGetter(value = AttributeType.PROPERTY, targetName = "billingAccount")}))
    @Setter(onMethod = @__({@AttributeSetter(value = AttributeType.PROPERTY, targetName = "billingAccount")}))
    private BillingAccountRef billingAccount;

    @Getter(onMethod = @__({@AttributeGetter(value = AttributeType.PROPERTY, targetName = "customerBill")}))
    @Setter(onMethod = @__({@AttributeSetter(value = AttributeType.PROPERTY, targetName = "customerBill")}))
    private BillRef customerBill;

    @Getter(onMethod = @__({@AttributeGetter(value = AttributeType.RELATIONSHIP, targetName = "relatedParty")}))
    @Setter(onMethod = @__({@AttributeSetter(value = AttributeType.RELATIONSHIP, targetName = "relatedParty", targetClass = RelatedPartyRef.class, fromProperties = true)}))
    private RelatedPartyRef relatedParty;

    @Getter(onMethod = @__({@AttributeGetter(value = AttributeType.PROPERTY, targetName = "state")}))
    @Setter(onMethod = @__({@AttributeSetter(value = AttributeType.PROPERTY, targetName = "state")}))
    private StateValues state;

    public CustomerBillOnDemand(String id) {
        super(CustomerBillOnDemand.TYPE_CUSTOMER_BILL_ON_DEMAND, id);
    }
}
