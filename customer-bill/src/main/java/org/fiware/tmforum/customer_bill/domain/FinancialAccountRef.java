package org.fiware.tmforum.customer_bill.domain;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.fiware.tmforum.common.domain.Entity;
import org.fiware.tmforum.common.domain.EntityWithId;
import org.fiware.tmforum.mapping.annotations.AttributeGetter;
import org.fiware.tmforum.mapping.annotations.AttributeSetter;
import org.fiware.tmforum.mapping.annotations.AttributeType;
import org.fiware.tmforum.mapping.annotations.MappingEnabled;

import java.net.URL;
import java.util.List;

@MappingEnabled(entityType = FinancialAccountRef.TYPE_FINANCIAL_ACCOUNT_REF)
@EqualsAndHashCode(callSuper = true)
public class FinancialAccountRef extends EntityWithId {

    public static final String TYPE_FINANCIAL_ACCOUNT_REF = "financial-account-ref";

    @Getter(onMethod = @__({@AttributeGetter(value = AttributeType.PROPERTY, targetName = "href")}))
    @Setter(onMethod = @__({@AttributeSetter(value = AttributeType.PROPERTY, targetName = "href")}))
    private URL href;

    @Getter(onMethod = @__({@AttributeGetter(value = AttributeType.PROPERTY, targetName = "name")}))
    @Setter(onMethod = @__({@AttributeSetter(value = AttributeType.PROPERTY, targetName = "name")}))
    private String name;

    @Getter(onMethod = @__({@AttributeGetter(value = AttributeType.RELATIONSHIP_LIST, targetName = "accountBalance")}))
    @Setter(onMethod = @__({@AttributeSetter(value = AttributeType.RELATIONSHIP_LIST, targetName = "accountBalance", targetClass = AccountBalance.class)}))
    private List<AccountBalance> accountBalance;

    @Getter(onMethod = @__({@AttributeGetter(value = AttributeType.PROPERTY, targetName = "atReferredType")}))
    @Setter(onMethod = @__({@AttributeSetter(value = AttributeType.PROPERTY, targetName = "atReferredType")}))
    private String atReferredType;

    public FinancialAccountRef(String id) {
        super(FinancialAccountRef.TYPE_FINANCIAL_ACCOUNT_REF, id);
    }
}
