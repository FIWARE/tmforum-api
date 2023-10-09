package org.fiware.tmforum.customerbillmanagement.domain;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.fiware.customerbillmanagement.model.PaymentMethodRefVO;
import org.fiware.tmforum.common.domain.AttachmentRefOrValue;
import org.fiware.tmforum.common.domain.BillingAccountRef;
import org.fiware.tmforum.common.domain.EntityWithId;
import org.fiware.tmforum.common.domain.Money;
import org.fiware.tmforum.common.domain.RelatedParty;
import org.fiware.tmforum.common.domain.TaxItem;
import org.fiware.tmforum.common.domain.TimePeriod;
import io.github.wistefan.mapping.annotations.AttributeGetter;
import io.github.wistefan.mapping.annotations.AttributeSetter;
import io.github.wistefan.mapping.annotations.AttributeType;
import io.github.wistefan.mapping.annotations.MappingEnabled;

import java.net.URI;
import java.time.Instant;
import java.util.List;

@EqualsAndHashCode(callSuper = true)
@MappingEnabled(entityType = CustomerBill.TYPE_CUSTOMER_BILL)
public class CustomerBill extends EntityWithId {

	public static final String TYPE_CUSTOMER_BILL = "customer-bill";

	@Getter(onMethod = @__({ @AttributeGetter(value = AttributeType.PROPERTY, targetName = "href") }))
	@Setter(onMethod = @__({ @AttributeSetter(value = AttributeType.PROPERTY, targetName = "href") }))
	private URI href;

	@Getter(onMethod = @__({ @AttributeGetter(value = AttributeType.PROPERTY, targetName = "billDate") }))
	@Setter(onMethod = @__({ @AttributeSetter(value = AttributeType.PROPERTY, targetName = "billDate") }))
	private Instant billDate;

	@Getter(onMethod = @__({ @AttributeGetter(value = AttributeType.PROPERTY, targetName = "billNo") }))
	@Setter(onMethod = @__({ @AttributeSetter(value = AttributeType.PROPERTY, targetName = "billNo") }))
	private String billNo;

	@Getter(onMethod = @__({ @AttributeGetter(value = AttributeType.PROPERTY, targetName = "category") }))
	@Setter(onMethod = @__({ @AttributeSetter(value = AttributeType.PROPERTY, targetName = "category") }))
	private String category;

	@Getter(onMethod = @__({ @AttributeGetter(value = AttributeType.PROPERTY, targetName = "lastUpdate") }))
	@Setter(onMethod = @__({ @AttributeSetter(value = AttributeType.PROPERTY, targetName = "lastUpdate") }))
	private Instant lastUpdate;

	@Getter(onMethod = @__({ @AttributeGetter(value = AttributeType.PROPERTY, targetName = "nextBillDate") }))
	@Setter(onMethod = @__({ @AttributeSetter(value = AttributeType.PROPERTY, targetName = "nextBillDate") }))
	private Instant nextBillDate;

	@Getter(onMethod = @__({ @AttributeGetter(value = AttributeType.PROPERTY, targetName = "paymentDueDate") }))
	@Setter(onMethod = @__({ @AttributeSetter(value = AttributeType.PROPERTY, targetName = "paymentDueDate") }))
	private Instant paymentDueDate;

	@Getter(onMethod = @__({ @AttributeGetter(value = AttributeType.PROPERTY, targetName = "runType") }))
	@Setter(onMethod = @__({ @AttributeSetter(value = AttributeType.PROPERTY, targetName = "runType") }))
	private String runType;

	@Getter(onMethod = @__({ @AttributeGetter(value = AttributeType.PROPERTY, targetName = "amountDue") }))
	@Setter(onMethod = @__({
			@AttributeSetter(value = AttributeType.PROPERTY, targetName = "amountDue", targetClass = Money.class) }))
	private Money amountDue;

	@Getter(onMethod = @__({ @AttributeGetter(value = AttributeType.PROPERTY_LIST, targetName = "appliedPayment") }))
	@Setter(onMethod = @__({
			@AttributeSetter(value = AttributeType.PROPERTY_LIST, targetName = "appliedPayment", targetClass = AppliedPayment.class) }))
	private List<AppliedPayment> appliedPayment;

	@Getter(onMethod = @__({ @AttributeGetter(value = AttributeType.PROPERTY_LIST, targetName = "billDocument") }))
	@Setter(onMethod = @__({
			@AttributeSetter(value = AttributeType.PROPERTY_LIST, targetName = "billDocument", targetClass = AttachmentRefOrValue.class) }))
	private List<AttachmentRefOrValue> billDocument;

	@Getter(onMethod = @__({ @AttributeGetter(value = AttributeType.RELATIONSHIP, targetName = "billingAccount", embedProperty = true) }))
	@Setter(onMethod = @__({
			@AttributeSetter(value = AttributeType.RELATIONSHIP, targetName = "billingAccount", targetClass = BillingAccountRef.class, fromProperties = true) }))
	private BillingAccountRef billingAccount;

	@Getter(onMethod = @__({ @AttributeGetter(value = AttributeType.PROPERTY, targetName = "billingPeriod") }))
	@Setter(onMethod = @__({
			@AttributeSetter(value = AttributeType.PROPERTY, targetName = "billingPeriod", targetClass = TimePeriod.class) }))
	private TimePeriod billingPeriod;

	@Getter(onMethod = @__({ @AttributeGetter(value = AttributeType.RELATIONSHIP, targetName = "financialAccount", embedProperty = true) }))
	@Setter(onMethod = @__({
			@AttributeSetter(value = AttributeType.RELATIONSHIP, targetName = "financialAccount", targetClass = FinancialAccountRef.class, fromProperties = true) }))
	private FinancialAccountRef financialAccount;

	@Getter(onMethod = @__({ @AttributeGetter(value = AttributeType.RELATIONSHIP, targetName = "paymentMethod", embedProperty = true) }))
	@Setter(onMethod = @__({
			@AttributeSetter(value = AttributeType.RELATIONSHIP, targetName = "paymentMethod", targetClass = PaymentMethodRefVO.class, fromProperties = true) }))
	private PaymentMethodRef paymentMethod;

	@Getter(onMethod = @__({ @AttributeGetter(value = AttributeType.RELATIONSHIP_LIST, targetName = "relatedParty") }))
	@Setter(onMethod = @__({
			@AttributeSetter(value = AttributeType.RELATIONSHIP_LIST, targetName = "relatedParty", targetClass = RelatedParty.class) }))
	private List<RelatedParty> relatedParty;

	@Getter(onMethod = @__({ @AttributeGetter(value = AttributeType.PROPERTY, targetName = "remainingAmount") }))
	@Setter(onMethod = @__({
			@AttributeSetter(value = AttributeType.PROPERTY, targetName = "remainingAmount", targetClass = Money.class) }))
	private Money remainingAmount;

	@Getter(onMethod = @__({ @AttributeGetter(value = AttributeType.PROPERTY, targetName = "state") }))
	@Setter(onMethod = @__({
			@AttributeSetter(value = AttributeType.PROPERTY, targetName = "state", targetClass = StateValue.class) }))
	private StateValue state;

	@Getter(onMethod = @__({ @AttributeGetter(value = AttributeType.PROPERTY, targetName = "taxExcludedAmount") }))
	@Setter(onMethod = @__({
			@AttributeSetter(value = AttributeType.PROPERTY, targetName = "taxExcludedAmount", targetClass = Money.class) }))
	private Money taxExcludedAmount;

	@Getter(onMethod = @__({ @AttributeGetter(value = AttributeType.PROPERTY, targetName = "taxIncludedAmount") }))
	@Setter(onMethod = @__({
			@AttributeSetter(value = AttributeType.PROPERTY, targetName = "taxIncludedAmount", targetClass = Money.class) }))
	private Money taxIncludedAmount;

	@Getter(onMethod = @__({ @AttributeGetter(value = AttributeType.PROPERTY_LIST, targetName = "taxItem") }))
	@Setter(onMethod = @__({
			@AttributeSetter(value = AttributeType.PROPERTY_LIST, targetName = "taxItem", targetClass = TaxItem.class) }))
	private List<TaxItem> taxItem;

	public CustomerBill(String id) {
		super(TYPE_CUSTOMER_BILL, id);
	}

	@Override
	public String getEntityState() {
		return state != null ? state.getValue() : null;
	}
}
