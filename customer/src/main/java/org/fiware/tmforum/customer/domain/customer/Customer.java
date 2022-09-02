package org.fiware.tmforum.customer.domain.customer;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.fiware.customer.model.RelatedPartyVO;
import org.fiware.tmforum.common.domain.EntityWithId;
import org.fiware.tmforum.customer.domain.ContactMedium;
import org.fiware.tmforum.customer.domain.RelatedParty;
import org.fiware.tmforum.mapping.annotations.AttributeGetter;
import org.fiware.tmforum.mapping.annotations.AttributeSetter;
import org.fiware.tmforum.mapping.annotations.AttributeType;
import org.fiware.tmforum.mapping.annotations.MappingEnabled;

import java.net.URL;
import java.util.List;

@EqualsAndHashCode(callSuper = true)
@MappingEnabled(entityType = Customer.TYPE_CUSTOMER)
public class Customer extends EntityWithId {

    public static final String TYPE_CUSTOMER = "customer";

    @Getter(onMethod = @__({@AttributeGetter(value = AttributeType.PROPERTY, targetName = "href")}))
    @Setter(onMethod = @__({@AttributeSetter(value = AttributeType.PROPERTY, targetName = "href")}))
    private URL href;

    @Getter(onMethod = @__({@AttributeGetter(value = AttributeType.PROPERTY, targetName = "name")}))
    @Setter(onMethod = @__({@AttributeSetter(value = AttributeType.PROPERTY, targetName = "name")}))
    private String name;

    @Getter(onMethod = @__({@AttributeGetter(value = AttributeType.PROPERTY, targetName = "status")}))
    @Setter(onMethod = @__({@AttributeSetter(value = AttributeType.PROPERTY, targetName = "status")}))
    private String status;

    @Getter(onMethod = @__({@AttributeGetter(value = AttributeType.PROPERTY, targetName = "statusReason")}))
    @Setter(onMethod = @__({@AttributeSetter(value = AttributeType.PROPERTY, targetName = "statusReason")}))
    private String statusReason;

    // 	private java.util.List<AccountRefVO> account;

    // 	private java.util.List<AgreementRefVO> agreement;

    // 	private java.util.List<CharacteristicVO> characteristic;

    @Getter(onMethod = @__({@AttributeGetter(value = AttributeType.PROPERTY_LIST, targetName = "contactMedium")}))
    @Setter(onMethod = @__({@AttributeSetter(value = AttributeType.PROPERTY_LIST, targetName = "contactMedium", targetClass = ContactMedium.class)}))
    private List<ContactMedium> contactMedium;

    // 	private java.util.List<CreditProfileVO> creditProfile;

    //  private RelatedPartyVO engagedParty;

    // 	private java.util.List<PaymentMethodRefVO> paymentMethod;

    @Getter(onMethod = @__({@AttributeGetter(value = AttributeType.RELATIONSHIP_LIST, targetName = "relatedParty")}))
    @Setter(onMethod = @__({@AttributeSetter(value = AttributeType.RELATIONSHIP_LIST, targetName = "relatedParty", targetClass = RelatedParty.class, fromProperties = true)}))
    private List<RelatedParty> relatedParty;

    // 	private TimePeriodVO validFor;



    public Customer(String id) {
        super(TYPE_CUSTOMER, id);
    }
}
