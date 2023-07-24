package org.fiware.tmforum.customerbillmanagement.domain;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.fiware.tmforum.common.domain.BillingAccountRef;
import org.fiware.tmforum.common.domain.EntityWithId;
import org.fiware.tmforum.common.domain.RelatedParty;
import io.github.wistefan.mapping.annotations.AttributeGetter;
import io.github.wistefan.mapping.annotations.AttributeSetter;
import io.github.wistefan.mapping.annotations.AttributeType;
import io.github.wistefan.mapping.annotations.MappingEnabled;

import java.net.URI;
import java.time.Instant;

@EqualsAndHashCode(callSuper = true)
@MappingEnabled(entityType = CustomerBillOnDemand.TYPE_CUSTOMER_BILL_ON_DEMAND)
public class CustomerBillOnDemand extends EntityWithId {

	public static final String TYPE_CUSTOMER_BILL_ON_DEMAND = "customer-bill-on-demand";

	@Getter(onMethod = @__({ @AttributeGetter(value = AttributeType.PROPERTY, targetName = "href") }))
	@Setter(onMethod = @__({ @AttributeSetter(value = AttributeType.PROPERTY, targetName = "href") }))
	private URI href;

	@Getter(onMethod = @__({ @AttributeGetter(value = AttributeType.PROPERTY, targetName = "description") }))
	@Setter(onMethod = @__({ @AttributeSetter(value = AttributeType.PROPERTY, targetName = "description") }))
	private String description;

	@Getter(onMethod = @__({ @AttributeGetter(value = AttributeType.PROPERTY, targetName = "lastUpdate") }))
	@Setter(onMethod = @__({ @AttributeSetter(value = AttributeType.PROPERTY, targetName = "lastUpdate") }))
	private Instant lastUpdate;

	@Getter(onMethod = @__({ @AttributeGetter(value = AttributeType.PROPERTY, targetName = "name") }))
	@Setter(onMethod = @__({ @AttributeSetter(value = AttributeType.PROPERTY, targetName = "name") }))
	private String name;

	@Getter(onMethod = @__({ @AttributeGetter(value = AttributeType.RELATIONSHIP, targetName = "billingAccount") }))
	@Setter(onMethod = @__({
			@AttributeSetter(value = AttributeType.RELATIONSHIP, targetName = "billingAccount", targetClass = BillingAccountRef.class) }))
	private BillingAccountRef billingAccount;

	@Getter(onMethod = @__({ @AttributeGetter(value = AttributeType.RELATIONSHIP, targetName = "customerBill") }))
	@Setter(onMethod = @__({
			@AttributeSetter(value = AttributeType.RELATIONSHIP, targetName = "customerBill", targetClass = CustomerBillRef.class) }))
	private CustomerBillRef customerBill;

	@Getter(onMethod = @__({ @AttributeGetter(value = AttributeType.RELATIONSHIP, targetName = "relatedParty") }))
	@Setter(onMethod = @__({
			@AttributeSetter(value = AttributeType.RELATIONSHIP, targetName = "relatedParty", targetClass = RelatedParty.class) }))
	private RelatedParty relatedParty;

	@Getter(onMethod = @__({ @AttributeGetter(value = AttributeType.PROPERTY, targetName = "state") }))
	@Setter(onMethod = @__({
			@AttributeSetter(value = AttributeType.PROPERTY, targetName = "state", targetClass = OnDemandStateValue.class) }))
	private OnDemandStateValue state;

	public CustomerBillOnDemand(String id) {
		super(TYPE_CUSTOMER_BILL_ON_DEMAND, id);
	}

	@Override
	public String getEntityState() {
		return state != null ? state.getValue() : null;
	}
}
