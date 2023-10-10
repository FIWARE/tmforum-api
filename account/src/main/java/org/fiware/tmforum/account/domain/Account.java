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
import org.fiware.tmforum.common.domain.Money;
import org.fiware.tmforum.product.CategoryRef;

import java.net.URI;
import java.time.Instant;
import java.util.List;

@EqualsAndHashCode(callSuper = true)
//@MappingEnabled(entityType = Account.TYPE_ACCOUNT)
public abstract class Account extends EntityWithId {

    public Account(String type, String id) {
        super(type, id);
    }

    /*public static final String TYPE_ACCOUNT = "account";

    public final String type = TYPE_ACCOUNT;*/

    @Getter(onMethod = @__({@AttributeGetter(value = AttributeType.PROPERTY, targetName = "href")}))
    @Setter(onMethod = @__({@AttributeSetter(value = AttributeType.PROPERTY, targetName = "href")}))
    private URI href;

    @Getter(onMethod = @__({@AttributeGetter(value = AttributeType.PROPERTY, targetName = "accountType")}))
    @Setter(onMethod = @__({@AttributeSetter(value = AttributeType.PROPERTY, targetName = "accountType")}))
    private String accountType;

    @Getter(onMethod = @__({@AttributeGetter(value = AttributeType.PROPERTY, targetName = "description")}))
    @Setter(onMethod = @__({@AttributeSetter(value = AttributeType.PROPERTY, targetName = "description")}))
    private String description;

    @Getter(onMethod = @__({ @AttributeGetter(value = AttributeType.PROPERTY, targetName = "lastModified") }))
    @Setter(onMethod = @__({ @AttributeSetter(value = AttributeType.PROPERTY, targetName = "lastModified") }))
    private Instant lastModified;

    @Getter(onMethod = @__({ @AttributeGetter(value = AttributeType.PROPERTY, targetName = "name") }))
    @Setter(onMethod = @__({ @AttributeSetter(value = AttributeType.PROPERTY, targetName = "name") }))
    private String name;

    @Getter(onMethod = @__({ @AttributeGetter(value = AttributeType.PROPERTY, targetName = "state") }))
    @Setter(onMethod = @__({ @AttributeSetter(value = AttributeType.PROPERTY, targetName = "state") }))
    private String state;

    @Getter(onMethod = @__({ @AttributeGetter(value = AttributeType.RELATIONSHIP_LIST, targetName = "accountBalance") }))
    @Setter(onMethod = @__({ @AttributeSetter(value = AttributeType.RELATIONSHIP_LIST, targetName = "accountBalance") }))
    private List<AccountBalance> accountBalance;

    @Getter(onMethod = @__({ @AttributeGetter(value = AttributeType.RELATIONSHIP_LIST, targetName = "accountRelationship") }))
    @Setter(onMethod = @__({ @AttributeSetter(value = AttributeType.RELATIONSHIP_LIST, targetName = "accountRelationship") }))
    private List<AccountRelationship> accountRelationship;

    @Getter(onMethod = @__({ @AttributeGetter(value = AttributeType.RELATIONSHIP_LIST, targetName = "contact") }))
    @Setter(onMethod = @__({ @AttributeSetter(value = AttributeType.RELATIONSHIP_LIST, targetName = "contact") }))
    private List<Contact> contact;

    @Getter(onMethod = @__({ @AttributeGetter(value = AttributeType.RELATIONSHIP, targetName = "creditLimit") }))
    @Setter(onMethod = @__({ @AttributeSetter(value = AttributeType.RELATIONSHIP, targetName = "creditLimit") }))
    private Money creditLimit;

    @Getter(onMethod = @__({ @AttributeGetter(value = AttributeType.RELATIONSHIP_LIST, targetName = "relatedParty") }))
    @Setter(onMethod = @__({
            @AttributeSetter(value = AttributeType.RELATIONSHIP_LIST, targetName = "relatedParty", targetClass = RelatedParty.class) }))
    private List<RelatedParty> relatedParty;

    @Getter(onMethod = @__({ @AttributeGetter(value = AttributeType.RELATIONSHIP_LIST, targetName = "taxExemption") }))
    @Setter(onMethod = @__({ @AttributeSetter(value = AttributeType.RELATIONSHIP_LIST, targetName = "taxExemption") }))
    private List<AccountTaxExemption> taxExemption;

}