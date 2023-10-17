package org.fiware.tmforum.partyrole.domain;

import java.net.URI;
import java.util.List;

import org.fiware.tmforum.common.domain.AgreementRef;
import org.fiware.tmforum.common.domain.Characteristic;
import org.fiware.tmforum.common.domain.ContactMedium;
import org.fiware.tmforum.common.domain.EntityWithId;
import org.fiware.tmforum.common.domain.RelatedParty;
import org.fiware.tmforum.common.domain.TimePeriod;
import org.fiware.tmforum.customer.AccountRef;
import org.fiware.tmforum.customer.CreditProfile;
import org.fiware.tmforum.customer.PaymentMethodRef;

import io.github.wistefan.mapping.annotations.AttributeGetter;
import io.github.wistefan.mapping.annotations.AttributeSetter;
import io.github.wistefan.mapping.annotations.AttributeType;
import io.github.wistefan.mapping.annotations.MappingEnabled;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

@EqualsAndHashCode(callSuper = true)
@MappingEnabled(entityType = PartyRole.TYPE_PR)
public class PartyRole extends EntityWithId{
    public static final String TYPE_PR = "party-role";
    public PartyRole(String id){
        super(PartyRole.TYPE_PR, id);    
    }
     
    @Getter(onMethod = @__({ @AttributeGetter(value = AttributeType.PROPERTY, targetName = "href") }))
    @Setter(onMethod = @__({ @AttributeSetter(value = AttributeType.PROPERTY, targetName = "href") }))
    private URI href;

    @Getter(onMethod = @__({ @AttributeGetter(value = AttributeType.PROPERTY, targetName = "name") }))
	@Setter(onMethod = @__({ @AttributeSetter(value = AttributeType.PROPERTY, targetName = "name") }))
	private String name;

    @Getter(onMethod = @__({ @AttributeGetter(value = AttributeType.PROPERTY, targetName = "status") }))
	@Setter(onMethod = @__({ @AttributeSetter(value = AttributeType.PROPERTY, targetName = "status") }))
	private String status;

    @Getter(onMethod = @__({ @AttributeGetter(value = AttributeType.PROPERTY, targetName = "statusReason") }))
	@Setter(onMethod = @__({ @AttributeSetter(value = AttributeType.PROPERTY, targetName = "statusReason") }))
	private String statusReason;

    @Getter(onMethod = @__({ @AttributeGetter(value = AttributeType.RELATIONSHIP_LIST, targetName = "account") }))
	@Setter(onMethod = @__({ @AttributeSetter(value = AttributeType.RELATIONSHIP_LIST, targetName = "account", targetClass = AccountRef.class) }))
	private List<AccountRef> account;

    @Getter(onMethod = @__({ @AttributeGetter(value = AttributeType.RELATIONSHIP_LIST, targetName = "agreement") }))
	@Setter(onMethod = @__({ @AttributeSetter(value = AttributeType.RELATIONSHIP_LIST, targetName = "agreement", targetClass = AgreementRef.class) }))
	private List<AgreementRef> agreement;

    @Getter(onMethod = @__({ @AttributeGetter(value = AttributeType.PROPERTY_LIST, targetName = "characteristic") }))
	@Setter(onMethod = @__({ @AttributeSetter(value = AttributeType.PROPERTY_LIST, targetName = "characteristic", targetClass = Characteristic.class)}))
	private List<Characteristic> characteristic;
    @Getter(onMethod = @__({ @AttributeGetter(value = AttributeType.PROPERTY_LIST, targetName = "contactMedium") }))
	@Setter(onMethod = @__({ @AttributeSetter(value = AttributeType.PROPERTY_LIST, targetName = "contactMedium", targetClass = ContactMedium.class) }))
	private List<ContactMedium> contactMedium;
    @Getter(onMethod = @__({ @AttributeGetter(value = AttributeType.PROPERTY_LIST, targetName = "creditProfile") }))
	@Setter(onMethod = @__({ @AttributeSetter(value = AttributeType.PROPERTY_LIST, targetName = "creditProfile", targetClass = CreditProfile.class) }))
	private List<CreditProfile> creditProfile;
    @Getter(onMethod = @__({ @AttributeGetter(value = AttributeType.RELATIONSHIP, targetName = "engagedParty") }))
	@Setter(onMethod = @__({ @AttributeSetter(value = AttributeType.RELATIONSHIP, targetName = "engagedParty" , targetClass = RelatedParty.class) }))
	private RelatedParty engagedParty;
    @Getter(onMethod = @__({ @AttributeGetter(value = AttributeType.RELATIONSHIP_LIST, targetName = "paymentMethod") }))
	@Setter(onMethod = @__({ @AttributeSetter(value = AttributeType.RELATIONSHIP_LIST, targetName = "paymentMethod", targetClass = PaymentMethodRef.class) }))
	private List<PaymentMethodRef> paymentMethod;
    @Getter(onMethod = @__({ @AttributeGetter(value = AttributeType.RELATIONSHIP_LIST, targetName = "relatedParty") }))
	@Setter(onMethod = @__({ @AttributeSetter(value = AttributeType.RELATIONSHIP_LIST, targetName = "relatedParty", targetClass = RelatedParty.class) }))
	private List<RelatedParty> relatedParty;
    @Getter(onMethod = @__({ @AttributeGetter(value = AttributeType.PROPERTY, targetName = "validFor") }))
	@Setter(onMethod = @__({ @AttributeSetter(value = AttributeType.PROPERTY, targetName = "validFor") }))
	private TimePeriod validFor; 


        
    }
    

