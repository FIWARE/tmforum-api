package org.fiware.tmforum.account.domain;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.fiware.tmforum.common.domain.EntityWithId;
import io.github.wistefan.mapping.annotations.AttributeGetter;
import io.github.wistefan.mapping.annotations.AttributeSetter;
import io.github.wistefan.mapping.annotations.AttributeType;
import io.github.wistefan.mapping.annotations.MappingEnabled;
import org.fiware.tmforum.common.domain.RelatedParty;
import org.fiware.tmforum.common.domain.Money;
import org.fiware.tmforum.common.domain.TimePeriod;
import org.fiware.tmforum.product.CategoryRef;
import org.fiware.account.model.PaymentMethodRefVO;

import java.net.URI;
import java.time.Instant;
import java.util.List;

@EqualsAndHashCode(callSuper = true)
@MappingEnabled(entityType = BillingAccount.TYPE_BILLINGAC)
public class BillingAccount extends Account {

    public BillingAccount(String id) {
        super(TYPE_BILLINGAC, id);
    }

    public static final String TYPE_BILLINGAC = "billingAccount";

    @Getter(onMethod = @__({ @AttributeGetter(value = AttributeType.PROPERTY, targetName = "paymentStatus") }))
    @Setter(onMethod = @__({ @AttributeSetter(value = AttributeType.PROPERTY, targetName = "paymentStatus") }))
    private String paymentStatus;

    @Getter(onMethod = @__({ @AttributeGetter(value = AttributeType.PROPERTY, targetName = "billStructure") }))
    @Setter(onMethod = @__({ @AttributeSetter(value = AttributeType.PROPERTY, targetName = "billStructure") }))
    private BillStructure billStructure;

    @Getter(onMethod = @__({ @AttributeGetter(value = AttributeType.RELATIONSHIP, targetName = "defaultPaymentMethod", embedProperty = true) }))
    @Setter(onMethod = @__({ @AttributeSetter(value = AttributeType.RELATIONSHIP, targetName = "defaultPaymentMethod", targetClass = PaymentMethodRefVO.class, fromProperties = true) }))
    private PaymentMethodRef defaultPaymentMethod;

    @Getter(onMethod = @__({ @AttributeGetter(value = AttributeType.RELATIONSHIP, targetName = "financialAccount") }))
    @Setter(onMethod = @__({ @AttributeSetter(value = AttributeType.RELATIONSHIP, targetName = "financialAccount", targetClass = FinancialAccountRef.class) }))
    private FinancialAccountRef financialAccount;

    @Getter(onMethod = @__({ @AttributeGetter(value = AttributeType.PROPERTY_LIST, targetName = "paymentPlan") }))
    @Setter(onMethod = @__({ @AttributeSetter(value = AttributeType.PROPERTY_LIST, targetName = "paymentPlan") }))
    private List<PaymentPlan> paymentPlan;


}