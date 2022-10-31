package org.fiware.tmforum.customerbillmanagement.domain;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.fiware.customerbillmanagement.model.PaymentMethodRefVO;
import org.fiware.tmforum.common.domain.AttachmentRefOrValue;
import org.fiware.tmforum.common.domain.EntityWithId;
import org.fiware.tmforum.common.domain.Money;
import org.fiware.tmforum.common.domain.RelatedParty;
import org.fiware.tmforum.common.domain.TimePeriod;
import org.fiware.tmforum.mapping.annotations.AttributeGetter;
import org.fiware.tmforum.mapping.annotations.AttributeSetter;
import org.fiware.tmforum.mapping.annotations.AttributeType;
import org.fiware.tmforum.mapping.annotations.MappingEnabled;

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

	@Getter(onMethod = @__({ @AttributeGetter(value = AttributeType.PROPERTY_LIST, targetName = "amountDue") }))
	@Setter(onMethod = @__({
			@AttributeSetter(value = AttributeType.PROPERTY_LIST, targetName = "amountDue", targetClass = Money.class) }))
	private List<Money> amountDue;

	@Getter(onMethod = @__({ @AttributeGetter(value = AttributeType.PROPERTY_LIST, targetName = "appliedPayment") }))
	@Setter(onMethod = @__({
			@AttributeSetter(value = AttributeType.PROPERTY_LIST, targetName = "appliedPayment", targetClass = AppliedPayment.class) }))
	private List<AppliedPayment> appliedPayment;

	@Getter(onMethod = @__({ @AttributeGetter(value = AttributeType.PROPERTY_LIST, targetName = "billDocument") }))
	@Setter(onMethod = @__({
			@AttributeSetter(value = AttributeType.PROPERTY_LIST, targetName = "billDocument", targetClass = AttachmentRefOrValue.class) }))
	private List<AttachmentRefOrValue> billDocument;

	@Getter(onMethod = @__({ @AttributeGetter(value = AttributeType.RELATIONSHIP, targetName = "billingAccount") }))
	@Setter(onMethod = @__({
			@AttributeSetter(value = AttributeType.RELATIONSHIP, targetName = "billingAccount", targetClass = BillingAccountRef.class) }))
	private BillingAccountRef billingAccount;

	@Getter(onMethod = @__({ @AttributeGetter(value = AttributeType.PROPERTY, targetName = "billingPeriod") }))
	@Setter(onMethod = @__({
			@AttributeSetter(value = AttributeType.PROPERTY, targetName = "billingPeriod", targetClass = TimePeriod.class) }))
	private TimePeriod billingPeriod;

	@Getter(onMethod = @__({ @AttributeGetter(value = AttributeType.RELATIONSHIP, targetName = "financialAccount") }))
	@Setter(onMethod = @__({
			@AttributeSetter(value = AttributeType.RELATIONSHIP, targetName = "financialAccount", targetClass = FinancialAccountRef.class) }))
	private FinancialAccountRef financialAccount;

	@Getter(onMethod = @__({ @AttributeGetter(value = AttributeType.RELATIONSHIP, targetName = "paymentMethod") }))
	@Setter(onMethod = @__({
			@AttributeSetter(value = AttributeType.RELATIONSHIP, targetName = "paymentMethod", targetClass = PaymentMethodRefVO.class) }))
	private PaymentMethodRefVO paymentMethod;

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

	public CustomerBill(String id) {
		super(TYPE_CUSTOMER_BILL, id);
	}
}
