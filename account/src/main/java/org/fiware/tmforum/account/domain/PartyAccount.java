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
import org.fiware.tmforum.common.domain.TimePeriod;
import org.fiware.tmforum.product.CategoryRef;

import java.net.URI;
import java.time.Instant;
import java.util.List;

@EqualsAndHashCode(callSuper = true)
@MappingEnabled(entityType = PartyAccount.TYPE_PARTYAC)
public class PartyAccount extends Account {

    public PartyAccount(String id) {
        super(TYPE, id);
    }

    public static final String TYPE_PARTYAC = "partyAccount";
    public static final String TYPE = TYPE_PARTYAC;

    @Getter(onMethod = @__({ @AttributeGetter(value = AttributeType.PROPERTY, targetName = "paymentStatus") }))
    @Setter(onMethod = @__({ @AttributeSetter(value = AttributeType.PROPERTY, targetName = "paymentStatus") }))
    private String paymentStatus;

    @Getter(onMethod = @__({ @AttributeGetter(value = AttributeType.RELATIONSHIP, targetName = "billStructure") }))
    @Setter(onMethod = @__({ @AttributeSetter(value = AttributeType.RELATIONSHIP, targetName = "billStructure") }))
    private BillStructure billStructure;

    @Getter(onMethod = @__({ @AttributeGetter(value = AttributeType.RELATIONSHIP, targetName = "defaultPaymentMethod") }))
    @Setter(onMethod = @__({ @AttributeSetter(value = AttributeType.RELATIONSHIP, targetName = "defaultPaymentMethod") }))
    private PaymentMethodRef defaultPaymentMethod;

    @Getter(onMethod = @__({ @AttributeGetter(value = AttributeType.RELATIONSHIP, targetName = "financialAccount") }))
    @Setter(onMethod = @__({ @AttributeSetter(value = AttributeType.RELATIONSHIP, targetName = "financialAccount") }))
    private FinancialAccountRef financialAccount;

    @Getter(onMethod = @__({ @AttributeGetter(value = AttributeType.RELATIONSHIP_LIST, targetName = "paymentPlan") }))
    @Setter(onMethod = @__({ @AttributeSetter(value = AttributeType.RELATIONSHIP_LIST, targetName = "paymentPlan") }))
    private List<PaymentPlan> paymentPlan;

}